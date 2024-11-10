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
