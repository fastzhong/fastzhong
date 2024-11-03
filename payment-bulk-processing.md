## pain001Service

```java
@ExtendWith(MockitoExtension.class)
class Pain001ServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentUtils paymentUtils;

    @Mock
    private Pain001ToBoMapper pain001ToBoMapper;

    @Mock
    private Pain001ToBoService pain001ToBo;

    @Mock
    private PaymentInformationValidation paymentInfoValidation;

    @Mock
    private PaymentInformationEnrichment paymentInfoEnrichment;

    @Mock
    private PaymentDebulkService paymentDebulkService;

    @Mock
    private PaymentSave paymentSave;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private ExecutionContext jobExecutionContext;

    @Mock
    private ExecutionContext stepExecutionContext;

    @InjectMocks
    private Pain001ServiceImpl pain001Service;

    @BeforeEach
    void setUp() {
        // Setup step execution context
        when(stepExecution.getExecutionContext()).thenReturn(stepExecutionContext);
        when(stepExecution.getJobExecution()).thenReturn(mock(JobExecution.class));
        when(stepExecution.getJobExecution().getExecutionContext()).thenReturn(jobExecutionContext);
        ReflectionTestUtils.setField(pain001Service, "stepExecution", stepExecution);
    }

    @Test
    void debulk_ShouldProcessAndUpdateResult() {
        // Given
        List<PaymentInformation> inputPayments = Collections.singletonList(createValidPaymentInfo());
        List<PaymentInformation> debulkedPayments = Arrays.asList(
            createValidPaymentInfo(),
            createValidPaymentInfo()
        );
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();

        when(paymentDebulkService.debulkPaymentInformations(inputPayments)).thenReturn(debulkedPayments);
        when(jobExecutionContext.get("result", Pain001InboundProcessingResult.class)).thenReturn(result);

        // When
        List<PaymentInformation> actualResult = pain001Service.debulk(inputPayments);

        // Then
        verify(paymentDebulkService).beforeStep(stepExecution);
        verify(paymentDebulkService).debulkPaymentInformations(inputPayments);
        assertEquals(debulkedPayments.size(), result.getPaymentDebulkTotal());
        assertEquals(debulkedPayments, actualResult);
    }

    @Test
    void save_WhenValidPayment_ShouldSaveSuccessfully() {
        // Given
        PaymentInformation validPayment = createValidPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(validPayment);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave).savePaymentInformation(
            eq(validPayment),
            eq(stepExecutionContext),
            eq(jobExecutionContext)
        );
        verifyNoMoreInteractions(paymentUtils);
    }

    @Test
    void save_WhenInvalidPayment_ShouldSkip() {
        // Given
        PaymentInformation invalidPayment = createInvalidPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(invalidPayment);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave, never()).savePaymentInformation(
            any(PaymentInformation.class),
            any(ExecutionContext.class),
            any(ExecutionContext.class)
        );
    }

    @Test
    void save_WhenSaveThrowsException_ShouldHandleError() {
        // Given
        PaymentInformation validPayment = createValidPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(validPayment);
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord saveRecord = new PwsSaveRecord();

        doThrow(new RuntimeException("Save failed"))
            .when(paymentSave)
            .savePaymentInformation(any(), any(), any());

        when(jobExecutionContext.get("result", Pain001InboundProcessingResult.class))
            .thenReturn(result);
        when(paymentUtils.createPwsSaveRecord(anyLong(), anyString()))
            .thenReturn(saveRecord);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave).savePaymentInformation(
            eq(validPayment),
            eq(stepExecutionContext),
            eq(jobExecutionContext)
        );
        verify(paymentUtils).createPwsSaveRecord(
            validPayment.getPwsTransactions().getTransactionId(),
            validPayment.getPwsBulkTransactions().getDmpBatchNumber()
        );
        verify(paymentUtils).updatePaymentSavedError(result, saveRecord);
    }

    @Test
    void save_WhenRejectedPayment_ShouldSkip() {
        // Given
        PaymentInformation rejectedPayment = createRejectedPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(rejectedPayment);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave, never()).savePaymentInformation(
            any(PaymentInformation.class),
            any(ExecutionContext.class),
            any(ExecutionContext.class)
        );
    }

    // Helper methods
    private PaymentInformation createValidPaymentInfo() {
        PaymentInformation payment = new PaymentInformation();
        payment.setDmpBulkStatus(DmpBulkStatus.ACCEPTED);
        
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);
        payment.setPwsTransactions(pwsTransactions);
        
        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");
        payment.setPwsBulkTransactions(pwsBulkTransactions);
        
        return payment;
    }

    private PaymentInformation createInvalidPaymentInfo() {
        PaymentInformation payment = createValidPaymentInfo();
        payment.addValidationError("ERR001", "Invalid payment");
        return payment;
    }

    private PaymentInformation createRejectedPaymentInfo() {
        PaymentInformation payment = createValidPaymentInfo();
        payment.setDmpBulkStatus(DmpBulkStatus.REJECTED);
        return payment;
    }
}
```

## debulk

```java
@Service
public class PaymentDebulkService {

    private static final BigDecimal BAHTNET_THRESHOLD = new BigDecimal("2000000.00");
    private static final String SMART_RESOURCE_ID = "smart";
    private static final String BAHTNET_RESOURCE_ID = "bahtnet";
    private static final String SMART_SAME_DAY_RESOURCE_ID = "smart-same-day";

    public List<PaymentInformation> debulkPaymentInformations(List<PaymentInformation> paymentInfos) {
        List<PaymentInformation> debulkedPayments = new ArrayList<>();
        
        for (PaymentInformation paymentInfo : paymentInfos) {
            // Skip if payment info is invalid
            if (!paymentInfo.isValid() || !paymentInfo.isAllChildTxnsValid()) {
                debulkedPayments.add(paymentInfo);
                continue;
            }

            PwsTransactions pwsTransaction = paymentInfo.getPwsTransactions();
            
            // Skip if not SMART payment
            if (!SMART_RESOURCE_ID.equalsIgnoreCase(pwsTransaction.getResourceId())) {
                debulkedPayments.add(paymentInfo);
                continue;
            }

            // Process each child transaction
            List<CreditTransferTransaction> originalChildTxns = paymentInfo.getCreditTransferTransactionList();
            if (originalChildTxns != null) {
                Map<String, List<CreditTransferTransaction>> groupedTxns = new HashMap<>();
                
                // Group transactions based on amount threshold
                for (CreditTransferTransaction childTxn : originalChildTxns) {
                    BigDecimal txnAmount = pwsTransaction.getTransactionTotalAmount();
                    String targetResourceId = determineResourceId(txnAmount);
                    groupedTxns.computeIfAbsent(targetResourceId, k -> new ArrayList<>()).add(childTxn);
                }

                // Create new payment information for each group
                for (Map.Entry<String, List<CreditTransferTransaction>> entry : groupedTxns.entrySet()) {
                    String resourceId = entry.getKey();
                    List<CreditTransferTransaction> groupTxns = entry.getValue();
                    
                    PaymentInformation newPaymentInfo = createNewPaymentInfo(paymentInfo, resourceId, groupTxns);
                    debulkedPayments.add(newPaymentInfo);
                }
            }
        }
        
        return debulkedPayments;
    }

    private String determineResourceId(BigDecimal amount) {
        if (amount.compareTo(BAHTNET_THRESHOLD) >= 0) {
            return BAHTNET_RESOURCE_ID;
        }
        return SMART_SAME_DAY_RESOURCE_ID;
    }

    private PaymentInformation createNewPaymentInfo(
            PaymentInformation originalPayment,
            String resourceId,
            List<CreditTransferTransaction> transactions) {
        
        PaymentInformation newPaymentInfo = new PaymentInformation();
        
        // Copy bulk transaction details
        newPaymentInfo.setPwsBulkTransactions(originalPayment.getPwsBulkTransactions());
        
        // Create new PwsTransactions with updated details
        PwsTransactions originalPwsTxn = originalPayment.getPwsTransactions();
        PwsTransactions newPwsTxn = new PwsTransactions();
        
        // Copy all properties from original transaction
        BeanUtils.copyProperties(originalPwsTxn, newPwsTxn);
        
        // Update specific fields
        newPwsTxn.setResourceId(resourceId);
        newPwsTxn.setTotalChild(transactions.size());
        newPwsTxn.setTotalAmount(calculateTotalAmount(transactions));
        newPwsTxn.setTransactionTotalAmount(calculateTotalAmount(transactions));
        
        // Set the new transactions
        newPaymentInfo.setPwsTransactions(newPwsTxn);
        newPaymentInfo.setCreditTransferTransactionList(transactions);
        
        // Copy validation status
        newPaymentInfo.setDmpBulkStatus(originalPayment.getDmpBulkStatus());
        
        return newPaymentInfo;
    }

    private BigDecimal calculateTotalAmount(List<CreditTransferTransaction> transactions) {
        return transactions.stream()
                .map(txn -> txn.getPwsBulkTransactionInstructions().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentDebulkServiceTest {

    @InjectMocks
    private PaymentDebulkService paymentDebulkService;

    private static final BigDecimal AMOUNT_3M = new BigDecimal("3000000.00");
    private static final BigDecimal AMOUNT_1M = new BigDecimal("1000000.00");
    private static final String SMART = "smart";
    private static final String BAHTNET = "bahtnet";
    private static final String SMART_SAME_DAY = "smart-same-day";

    @Test
    void whenNoPayments_thenReturnEmptyList() {
        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void whenPaymentIsInvalid_thenReturnOriginalPayment() {
        PaymentInformation invalidPayment = createPaymentInfo(SMART, AMOUNT_1M);
        invalidPayment.addValidationError("ERR001", "Invalid payment");

        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(
            Collections.singletonList(invalidPayment)
        );

        assertEquals(1, result.size());
        assertFalse(result.get(0).isValid());
        assertEquals("ERR001", result.get(0).getValidationResults().get(0).getErrorCode());
    }

    @Test
    void whenNonSmartPayment_thenReturnOriginalPayment() {
        PaymentInformation nonSmartPayment = createPaymentInfo("other", AMOUNT_1M);

        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(
            Collections.singletonList(nonSmartPayment)
        );

        assertEquals(1, result.size());
        assertEquals("other", result.get(0).getPwsTransactions().getResourceId());
    }

    @Test
    void whenAllTransactionsUnder2M_thenAllSmartSameDay() {
        PaymentInformation payment = createPaymentInfo(SMART, AMOUNT_1M);
        addChildTransactions(payment, AMOUNT_1M, AMOUNT_1M);

        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(
            Collections.singletonList(payment)
        );

        assertEquals(1, result.size());
        assertEquals(SMART_SAME_DAY, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
    }

    @Test
    void whenAllTransactionsOver2M_thenAllBahtnet() {
        PaymentInformation payment = createPaymentInfo(SMART, AMOUNT_3M);
        addChildTransactions(payment, AMOUNT_3M, AMOUNT_3M);

        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(
            Collections.singletonList(payment)
        );

        assertEquals(1, result.size());
        assertEquals(BAHTNET, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
    }

    @Test
    void whenMixedTransactions_thenSplitIntoTwoGroups() {
        PaymentInformation payment = createPaymentInfo(SMART, AMOUNT_3M.add(AMOUNT_1M));
        addChildTransactions(payment, AMOUNT_3M, AMOUNT_1M);

        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(
            Collections.singletonList(payment)
        );

        assertEquals(2, result.size());
        
        PaymentInformation bahtnetPayment = result.stream()
            .filter(p -> BAHTNET.equals(p.getPwsTransactions().getResourceId()))
            .findFirst()
            .orElseThrow();
        
        PaymentInformation smartPayment = result.stream()
            .filter(p -> SMART_SAME_DAY.equals(p.getPwsTransactions().getResourceId()))
            .findFirst()
            .orElseThrow();

        assertEquals(1, bahtnetPayment.getCreditTransferTransactionList().size());
        assertEquals(1, smartPayment.getCreditTransferTransactionList().size());
        assertEquals(AMOUNT_3M, bahtnetPayment.getPwsTransactions().getTransactionTotalAmount());
        assertEquals(AMOUNT_1M, smartPayment.getPwsTransactions().getTransactionTotalAmount());
    }

    @Test
    void verifyTotalAmountCalculation() {
        PaymentInformation payment = createPaymentInfo(SMART, AMOUNT_3M.add(AMOUNT_1M));
        addChildTransactions(payment, AMOUNT_3M, AMOUNT_1M);

        List<PaymentInformation> result = paymentDebulkService.debulkPaymentInformations(
            Collections.singletonList(payment)
        );

        BigDecimal totalAmount = result.stream()
            .map(p -> p.getPwsTransactions().getTransactionTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(AMOUNT_3M.add(AMOUNT_1M), totalAmount);
    }

    // Helper methods to create test data
    private PaymentInformation createPaymentInfo(String resourceId, BigDecimal amount) {
        PaymentInformation paymentInfo = new PaymentInformation();
        
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);
        pwsTransactions.setResourceId(resourceId);
        pwsTransactions.setTransactionTotalAmount(amount);
        pwsTransactions.setInitiationTime(Timestamp.from(Instant.now()));
        pwsTransactions.setCompanyId(1L);
        
        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(new PwsBulkTransactions());
        
        return paymentInfo;
    }

    private void addChildTransactions(PaymentInformation paymentInfo, BigDecimal... amounts) {
        List<CreditTransferTransaction> childTransactions = Arrays.stream(amounts)
            .map(this::createChildTransaction)
            .toList();
        
        paymentInfo.setCreditTransferTransactionList(childTransactions);
        paymentInfo.getPwsTransactions().setTotalChild(childTransactions.size());
    }

    private CreditTransferTransaction createChildTransaction(BigDecimal amount) {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setAmount(amount);
        transaction.setPwsBulkTransactionInstructions(instructions);
        
        PwsPartyBanks partyBank = new PwsPartyBanks();
        partyBank.setBankId(1L);
        transaction.setPartyBank(partyBank);
        
        return transaction;
    }
}
```

## save

```java
@ExtendWith(MockitoExtension.class)
class PaymentSaveImplTest {

    @Mock
    private AppConfig config;
    
    @Mock
    private RetryTemplate retryTemplate;
    
    @Mock
    private SqlSessionTemplate paymentSaveSqlSessionTemplate;
    
    @Mock
    private PaymentUtils paymentUtils;
    
    @Mock
    private PwsSaveDao pwsSaveDao;
    
    @Mock
    private SqlSessionFactory sqlSessionFactory;
    
    @Mock
    private SqlSession sqlSession;

    @InjectMocks
    private PaymentSaveImpl paymentSave;

    private PaymentInformation paymentInfo;
    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private Pain001InboundProcessingResult result;

    @BeforeEach
    void setUp() {
        // Setup basic test data
        paymentInfo = new PaymentInformation();
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        jobContext.put("result", result);

        // Setup SQL Session behavior
        when(paymentSaveSqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        when(sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(sqlSession);
        when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);
    }

    @Test
    void savePaymentInformation_SuccessfulSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);
        
        setupValidPaymentInfo();
        setupSuccessfulBulkSave();
        setupSuccessfulChildTransactionSave();

        // Act
        paymentSave.savePaymentInformation(paymentInfo, stepContext, jobContext);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(anyLong(), anyString());
        verify(paymentUtils).updatePaymentSaved(eq(result), any());
        verify(pwsSaveDao).insertPwsTransactions(any());
        verify(pwsSaveDao).insertPwsBulkTransactions(any());
        verifyBatchInsertsCalled();
    }

    @Test
    void savePaymentInformation_FailedBatchSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);
        
        setupValidPaymentInfo();
        setupSuccessfulBulkSave();
        setupFailedChildTransactionSave();

        // Act
        paymentSave.savePaymentInformation(paymentInfo, stepContext, jobContext);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(anyLong(), anyString());
        verify(paymentUtils).updatePaymentSavedError(eq(result), any());
        verify(sqlSession, times(1)).rollback();
    }

    @Test
    void saveBulkPayment_Success() {
        // Arrange
        setupValidPaymentInfo();
        setupSuccessfulBulkSave();

        // Act
        long txnId = paymentSave.saveBulkPayment(paymentInfo);

        // Assert
        assertEquals(1L, txnId);
        verify(pwsSaveDao).getBankRefSequenceNum();
        verify(pwsSaveDao).insertPwsTransactions(any());
        verify(pwsSaveDao).insertPwsBulkTransactions(any());
    }

    private void setupValidPaymentInfo() {
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);
        
        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");
        
        List<CreditTransferTransaction> transactions = new ArrayList<>();
        transactions.add(createValidTransaction());
        transactions.add(createValidTransaction());
        
        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);
        paymentInfo.setCreditTransferTransactionList(transactions);
    }

    private CreditTransferTransaction createValidTransaction() {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        transaction.setDmpTransactionStatus(DmpTransactionStatus.APPROVED);
        
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        Party party = new Party();
        party.setPwsParties(new PwsParties());
        
        transaction.setPwsBulkTransactionInstructions(instructions);
        transaction.setParty(party);
        
        return transaction;
    }

    private void setupSuccessfulBulkSave() {
        when(pwsSaveDao.getBankRefSequenceNum()).thenReturn(1);
        when(pwsSaveDao.insertPwsTransactions(any())).thenReturn(1L);
        when(pwsSaveDao.insertPwsBulkTransactions(any())).thenReturn(1L);
    }

    private void setupSuccessfulChildTransactionSave() {
        doAnswer(invocation -> {
            RetryCallback<Object, RuntimeException> callback = invocation.getArgument(0);
            return callback.doWithRetry(null);
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }

    private void setupFailedChildTransactionSave() {
        doAnswer(invocation -> {
            throw new BulkProcessingException("Failed to insert", new RuntimeException());
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }

    private void verifyBatchInsertsCalled() {
        verify(sqlSession, atLeastOnce()).commit();
        verify(sqlSession, never()).rollback();
        verify(sqlSession, atLeastOnce()).close();
    }
}
```
