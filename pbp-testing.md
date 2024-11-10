```java
@ExtendWith(MockitoExtension.class)
class PaymentSaveServiceTest {
    @Mock
    private AppConfig config;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private SqlSessionTemplate paymentSaveSqlSessionTemplate;

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @Mock
    private SqlSession sqlSession;

    @Mock
    private PwsSaveDao pwsSaveDao;

    @Mock
    private PaymentUtils paymentUtils;

    @Mock
    private TransactionUtils transactionUtils;

    @Mock
    protected StepExecution stepExecution;

    @Mock
    protected JobExecution jobExecution;

    @InjectMocks
    private PaymentSaveServiceImpl paymentSave;

    private PaymentInformation paymentInfo;
    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private BankRefMetaData bankRefMetaData;
    private Pain001InboundProcessingResult result;
    private PwsSaveRecord mockRecord;

    @BeforeEach
    void setUp() {
        // Initialize contexts and result
        paymentInfo = new PaymentInformation();
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        result.setPaymentSaved(new ArrayList<>());
        result.setPaymentSavedError(new ArrayList<>());
        jobContext.put("result", result);
        mockRecord = new PwsSaveRecord("123", "dmpRef123");

        // Setup step execution
        lenient().when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(jobContext);

        // Setup SQL Session behavior
        lenient().when(paymentSaveSqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        lenient().when(sqlSessionFactory.openSession(any(ExecutorType.class))).thenReturn(sqlSession);
        lenient().when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);

        // Setup PaymentUtils behavior
        lenient().when(paymentUtils.createPwsSaveRecord(anyLong(), anyString())).thenReturn(mockRecord);

        // Setup bank reference metadata
        paymentSave.beforeStep(stepExecution);
        bankRefMetaData = new BankRefMetaData("TH", "I", "SE", "2411");
        paymentSave.setBankRefMetadata(bankRefMetaData);

        // Mock method execution for batch inserts
        try {
            Method mockMethod = mock(Method.class);
            lenient().when(pwsSaveDao.getClass().getMethod(anyString(), any(Class.class))).thenReturn(mockMethod);
            lenient().when(mockMethod.invoke(any(), any())).thenReturn(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void savePaymentInformation_SuccessfulSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);
        setupValidPaymentInfo(batchSize);
        setupSuccessfulBulkSave();
        setupChildTxnBatchBankRefSeq(batchSize);
        
        // Setup successful retry template execution
        doAnswer(invocation -> {
            RetryCallback<Object, RuntimeException> callback = invocation.getArgument(0);
            return callback.doWithRetry(null);
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));

        // Mock successful batch operations
        doNothing().when(sqlSession).commit();
        doNothing().when(sqlSession).close();

        // Act
        paymentSave.savePaymentInformation(paymentInfo);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(eq(paymentInfo.getPwsTransactions().getTransactionId()),
                eq(paymentInfo.getPwsBulkTransactions().getDmpBatchNumber()));
        verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
        verify(pwsSaveDao).insertPwsTransactions(any(PwsTransactions.class));
        verify(pwsSaveDao).insertPwsBulkTransactions(any(PwsBulkTransactions.class));
        verify(sqlSession, atLeastOnce()).commit();
        verify(sqlSession, never()).rollback();
        verify(sqlSession, atLeastOnce()).close();
    }

    @Test
    void savePaymentInformation_FailedBatchSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);
        setupValidPaymentInfo(batchSize);
        setupSuccessfulBulkSave();
        setupChildTxnBatchBankRefSeq(batchSize);

        // Setup failed retry template execution
        doAnswer(invocation -> {
            throw new BulkProcessingException("Failed to insert", new RuntimeException());
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));

        // Mock failed batch operations
        doNothing().when(sqlSession).rollback();
        doNothing().when(sqlSession).close();

        // Act
        paymentSave.savePaymentInformation(paymentInfo);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(anyLong(), anyString());
        verify(paymentUtils).updatePaymentSavedError(eq(result), eq(mockRecord));
        verify(paymentUtils, never()).updatePaymentSaved(any(), any());
    }

    private void setupValidPaymentInfo(int size) {
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);

        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");

        List<CreditTransferTransaction> transactions = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            CreditTransferTransaction txn = createValidTransaction(i);
            txn.setDmpTransactionStatus(DmpTransactionStatus.APPROVED);
            transactions.add(txn);
        }

        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);
        paymentInfo.setCreditTransferTransactionList(transactions);
    }

    private CreditTransferTransaction createValidTransaction(int id) {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        transaction.setDmpTransactionStatus(DmpTransactionStatus.APPROVED);

        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        Party party = new Party();
        PwsParties pwsParties = new PwsParties();
        pwsParties.setPartyId((long) id);
        party.setPwsParties(pwsParties);

        transaction.setPwsBulkTransactionInstructions(instructions);
        transaction.setParty(party);

        return transaction;
    }

    private void setupSuccessfulBulkSave() {
        when(pwsSaveDao.getBankRefSequenceNum()).thenReturn(1);
        when(pwsSaveDao.insertPwsTransactions(any())).thenReturn(1L);
        when(pwsSaveDao.insertPwsBulkTransactions(any())).thenReturn(1L);
    }

    private void setupChildTxnBatchBankRefSeq(int size) {
        List<Integer> seqNums = IntStream.rangeClosed(1, size).boxed().collect(Collectors.toList());
        when(pwsSaveDao.getBatchBankRefSequenceNum(size)).thenReturn(seqNums);
    }
}
```

```java
package com.uob.gwb.pbp.rule;

import com.uob.gwb.pbp.dao.PwsQueryDao;
import com.uob.gwb.pbp.po.PwsTransactionValidationRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionValidationDecisionMatrixTest {

    @Mock
    private PwsQueryDao pwsQueryDao;

    private TransactionValidationDecisionMatrix matrix;

    @BeforeEach
    void setUp() {
        matrix = new TransactionValidationDecisionMatrix(pwsQueryDao);
    }

    @Test
    void shouldGenerateSimpleValidationRule() {
        // Given
        PwsTransactionValidationRules rule = new PwsTransactionValidationRules();
        rule.setId(1L);
        rule.setCountryCode("countryCode == \"SG\"");
        rule.setChannel("channel == \"MOBILE\"");
        rule.setCondition_flag("1");
        rule.setErrorCode("ERR001");
        rule.setErrorMessage("Test Error");
        rule.setAction("STOP");

        when(pwsQueryDao.getAllTransactionValidationRules()).thenReturn(Arrays.asList(rule));

        // When
        matrix.refresh();
        String generatedRules = matrix.generateRules();

        // Then
        assertThat(generatedRules)
            .contains("rule \"Rule_1\"")
            .contains("countryCode == \"SG\"")
            .contains("channel == \"MOBILE\"")
            .contains("ERR001")
            .contains("Test Error")
            .contains("shouldStop.set(true)");
    }

    @Test
    void shouldGenerateNegativeMatchingRule() {
        // Given
        PwsTransactionValidationRules rule = new PwsTransactionValidationRules();
        rule.setId(2L);
        rule.setField("amount");
        rule.setFieldValue(">= 1000");
        rule.setCondition_flag("-1");
        rule.setErrorCode("ERR002");
        rule.setErrorMessage("Amount validation failed");

        when(pwsQueryDao.getAllTransactionValidationRules()).thenReturn(Arrays.asList(rule));

        // When
        matrix.refresh();
        String generatedRules = matrix.generateRules();

        // Then
        assertThat(generatedRules)
            .contains("rule \"Rule_2\"")
            .contains("not (")
            .contains("transaction.amount >= 1000")
            .contains("ERR002")
            .contains("Amount validation failed");
    }

    @Test
    void shouldGenerateBusinessRuleValidation() {
        // Given
        PwsTransactionValidationRules rule = new PwsTransactionValidationRules();
        rule.setId(3L);
        rule.setBusinessRule("validationHelper.checkDailyLimit(context, transaction.getCustomerId())");
        rule.setCondition_flag("1");
        rule.setErrorCode("ERR003");
        rule.setErrorMessage("Daily limit exceeded");
        rule.setAction("STOP");

        when(pwsQueryDao.getAllTransactionValidationRules()).thenReturn(Arrays.asList(rule));

        // When
        matrix.refresh();
        String generatedRules = matrix.generateRules();

        // Then
        assertThat(generatedRules)
            .contains("rule \"Rule_3\"")
            .contains("eval(validationHelper.checkDailyLimit")
            .contains("ERR003")
            .contains("Daily limit exceeded");
    }

    @Test
    void shouldSkipDisabledRules() {
        // Given
        PwsTransactionValidationRules rule = new PwsTransactionValidationRules();
        rule.setId(4L);
        rule.setCondition_flag("0");
        rule.setErrorCode("ERR004");

        when(pwsQueryDao.getAllTransactionValidationRules()).thenReturn(Arrays.asList(rule));

        // When
        matrix.refresh();
        String generatedRules = matrix.generateRules();

        // Then
        assertThat(generatedRules).doesNotContain("ERR004");
    }

    @Test
    void shouldThrowExceptionWhenNoRulesFound() {
        // Given
        when(pwsQueryDao.getAllTransactionValidationRules()).thenReturn(Arrays.asList());

        // When/Then
        assertThatThrownBy(() -> matrix.refresh())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No validation rules found");
    }
}
```

```java
package com.uob.gwb.pbp.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidationRuleEngineTest {

    @Mock
    private TransactionValidationHelper validationHelper;

    @Mock
    private ExecutionContext executionContext;

    private ValidationRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ValidationRuleEngine();
    }

    @Test
    void shouldRefreshAndFireRules() {
        // Given
        String engineName = "test-engine";
        String rules = """
            package rules;
            import com.uob.gwb.pbp.bo.validation.TransactionValidationRecord;
            global ExecutionContext context;
            global TransactionValidationHelper validationHelper;
            
            rule "Test Rule"
            when
                $record: TransactionValidationRecord(countryCode == "SG")
            then
                $record.getTransaction().addValidationError("TEST001", "Test Error");
            end
            """;

        TransactionValidationRecord record = new TransactionValidationRecord();
        record.setCountryCode("SG");
        record.setTransaction(new CreditTransferTransaction());

        // When
        engine.refreshRules(engineName, rules);
        engine.fireRules(engineName, Arrays.asList(record), executionContext, validationHelper);

        // Then
        assertThat(record.getTransaction().getPaymentValidationResults())
            .hasSize(1)
            .extracting("errorCode")
            .containsExactly("TEST001");
    }

    @Test
    void shouldHandleStopCondition() {
        // Given
        String engineName = "test-engine";
        String rules = """
            package rules;
            import com.uob.gwb.pbp.bo.validation.TransactionValidationRecord;
            global ExecutionContext context;
            global TransactionValidationHelper validationHelper;
            global AtomicBoolean shouldStop;
            
            rule "Stop Rule"
            when
                $record: TransactionValidationRecord(countryCode == "SG")
            then
                $record.getTransaction().addValidationError("STOP001", "Stop Error");
                shouldStop.set(true);
            end
            """;

        TransactionValidationRecord record = new TransactionValidationRecord();
        record.setCountryCode("SG");
        record.setTransaction(new CreditTransferTransaction());

        // When
        engine.refreshRules(engineName, rules);
        boolean stopped = engine.fireRulesShouldStop(engineName, Arrays.asList(record), 
            executionContext, validationHelper, true);

        // Then
        assertThat(stopped).isTrue();
        assertThat(record.getTransaction().getPaymentValidationResults())
            .hasSize(1)
            .extracting("errorCode")
            .containsExactly("STOP001");
    }

    @Test
    void shouldHandleInvalidRules() {
        // Given
        String engineName = "test-engine";
        String invalidRules = "invalid drools syntax";

        // When/Then
        assertThatThrownBy(() -> engine.refreshRules(engineName, invalidRules))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error compiling rules");
    }

    @Test
    void shouldHandleMultipleEngineInstances() {
        // Given
        String engine1 = "engine1";
        String engine2 = "engine2";
        String rules1 = """
            package rules;
            rule "Rule1" when then end
            """;
        String rules2 = """
            package rules;
            rule "Rule2" when then end
            """;

        // When
        engine.refreshRules(engine1, rules1);
        engine.refreshRules(engine2, rules2);

        // Then
        assertThat(engine.hasEngine(engine1)).isTrue();
        assertThat(engine.hasEngine(engine2)).isTrue();
        assertThat(engine.getEngineNames()).containsExactlyInAnyOrder(engine1, engine2);
    }

    @Test
    void shouldCleanupEngineInstance() {
        // Given
        String engineName = "test-engine";
        String rules = "package rules;\nrule \"Test\" when then end";
        engine.refreshRules(engineName, rules);

        // When
        engine.removeEngine(engineName);

        // Then
        assertThat(engine.hasEngine(engineName)).isFalse();
    }
}
```

```java
package com.uob.gwb.pbp.service.impl;

import com.uob.gwb.pbp.rule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionDecisionMatrixServiceImplTest {

    @Mock
    private TransactionValidationDecisionMatrix txnValidationMatrix;

    @Mock
    private TransactionValidationHelper txnValidationHelper;

    @Mock
    private ValidationRuleEngine validationRuleEngine;

    @Mock
    private ExecutionContext executionContext;

    private TransactionDecisionMatrixServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionDecisionMatrixServiceImpl(
            txnValidationMatrix, 
            txnValidationHelper, 
            validationRuleEngine
        );
    }

    @Test
    void shouldRefreshRulesSuccessfully() {
        // Given
        String generatedRules = "package rules;\nrule \"Test\" when then end";
        when(txnValidationMatrix.generateRules()).thenReturn(generatedRules);

        // When
        service.refreshRules();

        // Then
        verify(txnValidationMatrix).refresh();
        verify(validationRuleEngine).refreshRules(
            eq("transaction-validation"), 
            eq(generatedRules)
        );
    }

    @Test
    void shouldHandleRefreshRulesFailure() {
        // Given
        doThrow(new RuntimeException("Database error"))
            .when(txnValidationMatrix).refresh();

        // When/Then
        assertThatThrownBy(() -> service.refreshRules())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to refresh validation rules");
    }

    @Test
    void shouldApplyRulesSuccessfully() {
        // Given
        List<TransactionValidationRecord> records = Arrays.asList(
            new TransactionValidationRecord()
        );

        // When
        service.applyRules(records, executionContext);

        // Then
        verify(validationRuleEngine).fireRules(
            eq("transaction-validation"),
            eq(records),
            eq(executionContext),
            eq(txnValidationHelper)
        );
    }

    @Test
    void shouldHandleStopCondition() {
        // Given
        List<TransactionValidationRecord> records = Arrays.asList(
            new TransactionValidationRecord()
        );
        when(validationRuleEngine.fireRulesShouldStop(
            any(), any(), any(), any(), anyBoolean()
        )).thenReturn(true);

        // When
        boolean stopped = service.applyRulesShouldStop(records, executionContext, true);

        // Then
        assertThat(stopped).isTrue();
        verify(validationRuleEngine).fireRulesShouldStop(
            eq("transaction-validation"),
            eq(records),
            eq(executionContext),
            eq(txnValidationHelper),
            eq(true)
        );
    }

    @Test
    void shouldInitializeOnStartup() {
        // Given
        String generatedRules = "package rules;\nrule \"Test\" when then end";
        when(txnValidationMatrix.generateRules()).thenReturn(generatedRules);

        // When
        service.init();

        // Then
        verify(txnValidationMatrix).refresh();
        verify(validationRuleEngine).refreshRules(
            eq("transaction-validation"), 
            eq(generatedRules)
        );
    }
}
```
