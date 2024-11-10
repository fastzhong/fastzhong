## Validation Decision Matrix 

```java
package com.uob.gwb.pbp.rule;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class ValidationDecisionMatrixRow {
    private Long id;
    
    // Store conditions and results in maps for dynamic access
    private Map<ValidationStaticConditionColumn, String> staticConditions = new HashMap<>();
    private Map<ValidationDynamicConditionColumn, String> dynamicConditions = new HashMap<>();
    private Map<ValidationResultColumn, String> validationResults = new HashMap<>();
    
    public void setStaticCondition(ValidationStaticConditionColumn column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            staticConditions.put(column, value.trim());
        }
    }
    
    public String getStaticCondition(ValidationStaticConditionColumn column) {
        return staticConditions.get(column);
    }
    
    public void setDynamicCondition(ValidationDynamicConditionColumn column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            dynamicConditions.put(column, value.trim());
        }
    }
    
    public String getDynamicCondition(ValidationDynamicConditionColumn column) {
        return dynamicConditions.get(column);
    }
    
    public void setValidationResult(ValidationResultColumn column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            validationResults.put(column, value.trim());
        }
    }
    
    public String getValidationResult(ValidationResultColumn column) {
        return validationResults.get(column);
    }
}
```

```java
@Slf4j
@Component
public class TransactionValidationDecisionMatrix {

    private final PwsQueryDao pwsQueryDao;
    private List<ValidationDecisionMatrixRow> rows;

    public TransactionValidationDecisionMatrix(PwsQueryDao pwsQueryDao) {
        this.pwsQueryDao = pwsQueryDao;
        this.refresh();
    }

    public void refresh() {
        List<PwsTransactionValidationRules> dbRules = pwsQueryDao.getAllTransactionValidationRules();
        if (dbRules == null || dbRules.isEmpty()) {
            throw new IllegalStateException("No validation rules found in database");
        }

        this.rows = dbRules.stream()
                .map(this::convertToMatrixRow)
                .collect(Collectors.toList());
        
        log.info("Loaded {} validation rules from database", rows.size());
    }

    private String buildStaticConditions(ValidationDecisionMatrixRow row) {
        List<String> conditions = new ArrayList<>();
        
        for (ValidationStaticConditionColumn col : ValidationStaticConditionColumn.values()) {
            String value = row.getStaticCondition(col);
            if (StringUtils.isNotBlank(value)) {
                if (col == ValidationStaticConditionColumn.Field) {
                    String fieldValue = row.getStaticCondition(ValidationStaticConditionColumn.FieldValue);
                    if (StringUtils.isNotBlank(fieldValue)) {
                        conditions.add(String.format("transaction.%s == \"%s\"", value, fieldValue));
                    }
                    continue;
                }
                if (col != ValidationStaticConditionColumn.FieldValue) {
                    conditions.add(String.format("%s == \"%s\"", columnToProperty(col.getColumnName()), value));
                }
            }
        }
        
        return String.join(",\n        ", conditions);
    }

    private String buildDynamicConditions(ValidationDecisionMatrixRow row) {
        List<String> conditions = new ArrayList<>();

        // Process simple conditions
        String condition1 = row.getDynamicCondition(ValidationDynamicConditionColumn.SimpleCon1);
        if (StringUtils.isNotBlank(condition1)) {
            conditions.add(String.format("transaction.%s", condition1));
        }

        String condition2 = row.getDynamicCondition(ValidationDynamicConditionColumn.SimpleCon2);
        if (StringUtils.isNotBlank(condition2)) {
            conditions.add(String.format("transaction.%s", condition2));
        }

        String businessRule = row.getDynamicCondition(ValidationDynamicConditionColumn.BusinessRule);
        if (StringUtils.isNotBlank(businessRule)) {
            conditions.add(String.format("eval(validationHelper.%s)", businessRule));
        }

        return String.join(" && ", conditions);
    }

    public String generateRules() {
        StringBuilder rule = new StringBuilder();

        // Add imports and globals
        rule.append("import com.uob.gwb.pbp.bo.validation.TransactionValidationRecord;\n");
        rule.append("import com.uob.gwb.pbp.bo.CreditTransferTransaction;\n");
        rule.append("import org.springframework.batch.item.ExecutionContext;\n");
        rule.append("import java.util.concurrent.atomic.AtomicBoolean;\n\n");

        rule.append("global ExecutionContext context;\n");
        rule.append("global TransactionValidationHelper validationHelper;\n");
        rule.append("global AtomicBoolean shouldStop;\n\n");

        for (ValidationDecisionMatrixRow row : rows) {
            String condFlag = row.getValidationResult(ValidationResultColumn.CondFlag);
            
            // Skip if condition flag is 0 (don't fire)
            if ("0".equals(condFlag)) {
                continue;
            }

            // Rule header
            rule.append("rule \"Rule_").append(row.getId()).append("\"\n");
            rule.append("when\n");
            rule.append("    $record: TransactionValidationRecord(\n");

            // Add static conditions
            String staticConds = buildStaticConditions(row);
            if (StringUtils.isNotBlank(staticConds)) {
                rule.append("        ").append(staticConds).append(",\n");
            }

            // Ensure transaction is not null
            rule.append("        transaction != null\n    )\n");

            // Add dynamic conditions
            String dynamicConds = buildDynamicConditions(row);
            if (StringUtils.isNotBlank(dynamicConds)) {
                rule.append("    and ").append(dynamicConds).append("\n");
            }

            // Rule actions
            rule.append("then\n");
            
            // Handle condition flag (-1 for NOT matching)
            if ("-1".equals(condFlag)) {
                rule.append("    // Fire when NOT matching\n");
            }

            // Add validation error
            rule.append("    $record.getTransaction().addValidationError(")
                .append("\"").append(row.getValidationResult(ValidationResultColumn.ErrorCode)).append("\", ")
                .append("\"").append(row.getValidationResult(ValidationResultColumn.ErrorMessage)).append("\"")
                .append(");\n");

            // Add stop logic if action is STOP
            if ("STOP".equalsIgnoreCase(row.getValidationResult(ValidationResultColumn.Action))) {
                rule.append("    if (shouldStop != null) {\n");
                rule.append("        shouldStop.set(true);\n");
                rule.append("    }\n");
            }

            rule.append("end\n\n");
        }

        return rule.toString();
    }

    private ValidationDecisionMatrixRow convertToMatrixRow(PwsTransactionValidationRules dbRule) {
        ValidationDecisionMatrixRow row = new ValidationDecisionMatrixRow();
        row.setId(dbRule.getId());

        // Set static conditions dynamically
        for (ValidationStaticConditionColumn col : ValidationStaticConditionColumn.values()) {
            String propertyName = columnToProperty(col.getColumnName());
            String value = null;
            try {
                value = (String) PropertyUtils.getProperty(dbRule, propertyName);
                row.setStaticCondition(col, value);
            } catch (Exception e) {
                log.warn("Failed to get property {} from rule {}", propertyName, dbRule.getId());
            }
        }

        // Set dynamic conditions dynamically
        for (ValidationDynamicConditionColumn col : ValidationDynamicConditionColumn.values()) {
            String propertyName = columnToProperty(col.getColumnName());
            String value = null;
            try {
                value = (String) PropertyUtils.getProperty(dbRule, propertyName);
                row.setDynamicCondition(col, value);
            } catch (Exception e) {
                log.warn("Failed to get property {} from rule {}", propertyName, dbRule.getId());
            }
        }

        // Set validation results dynamically
        for (ValidationResultColumn col : ValidationResultColumn.values()) {
            String propertyName = columnToProperty(col.getColumnName());
            String value = null;
            try {
                value = (String) PropertyUtils.getProperty(dbRule, propertyName);
                row.setValidationResult(col, value);
            } catch (Exception e) {
                log.warn("Failed to get property {} from rule {}", propertyName, dbRule.getId());
            }
        }

        return row;
    }
}
```


## ValidationRuleEngine

```java
package com.uob.gwb.pbp.rule;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.batch.item.ExecutionContext;

@Slf4j
public class ValidationRuleEngine {
    public final static String SHOULD_STOP = "shouldStop";
    public final static String EXECUTION_CONTEXT = "context";
    public final static String VALIDATION_HELPER = "validationHelper";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final KieServices kieServices;
    private KieContainer kieContainer;

    public ValidationRuleEngine() {
        this.kieServices = KieServices.Factory.get();
    }

    public void updateRules(List<String> rules) {
        lock.writeLock().lock();
        try {
            KieModuleModel kieModuleModel = kieServices.newKieModuleModel();
            KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel("KBase")
                    .setDefault(true)
                    .setEventProcessingMode(org.kie.api.conf.EventProcessingOption.STREAM);

            kieBaseModel.newKieSessionModel("KSession")
                    .setDefault(true);

            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            kieFileSystem.writeKModuleXML(kieModuleModel.toXML());

            for (int i = 0; i < rules.size(); i++) {
                String ruleName = "Rule_" + i;
                String ruleContent = rules.get(i);
                String path = "src/main/resources/rules/" + ruleName + ".drl";
                kieFileSystem.write(path, ruleContent);
            }

            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                log.error("Errors during rule compilation: {}", kieBuilder.getResults().getMessages());
                throw new RuntimeException("Error compiling rules: " + kieBuilder.getResults().getMessages());
            }

            kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void fireRules(List<?> facts, ExecutionContext context, TransactionValidationHelper helper) {
        lock.readLock().lock();
        try {
            if (kieContainer == null) {
                throw new IllegalStateException("Rules have not been initialized. Call updateRules() first.");
            }

            KieSession kieSession = kieContainer.newKieSession();
            try {
                // Set globals
                kieSession.setGlobal(EXECUTION_CONTEXT, context);
                kieSession.setGlobal(VALIDATION_HELPER, helper);
                
                // Insert facts
                for (Object fact : facts) {
                    kieSession.insert(fact);
                }
                kieSession.fireAllRules();
            } finally {
                kieSession.dispose();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean fireRulesShouldStop(List<?> facts, ExecutionContext context, 
            TransactionValidationHelper helper, boolean stopOnError) {
        lock.readLock().lock();
        boolean stop = false;
        try {
            if (kieContainer == null) {
                throw new IllegalStateException("Rules have not been initialized. Call updateRules() first.");
            }

            KieSession kieSession = kieContainer.newKieSession();
            try {
                // Set globals
                AtomicBoolean shouldStop = new AtomicBoolean(false);
                kieSession.setGlobal(SHOULD_STOP, shouldStop);
                kieSession.setGlobal(EXECUTION_CONTEXT, context);
                kieSession.setGlobal(VALIDATION_HELPER, helper);

                for (Object fact : facts) {
                    kieSession.insert(fact);
                    kieSession.fireAllRules();
                    if (stopOnError && shouldStop.get()) {
                        stop = true;
                        break;
                    }
                }
            } finally {
                kieSession.dispose();
            }
        } finally {
            lock.readLock().unlock();
        }
        return stop;
    }
}
```

## Validation Matrix Service

```java
@RequiredArgsConstructor
@Slf4j
@Service("transactionDecisionMatrixService")
public class TransactionDecisionMatrixServiceImpl implements DecisionMatrixService<TransactionValidationRecord> {

    private final TransactionValidationDecisionMatrix txnValidationMatrix;
    private final TransactionValidationHelper txnValidationHelper;
    private final ValidationRuleEngine validationRuleEngine;

    @PostConstruct
    public void init() {
        refreshRules();
    }

    @Override
    public void refreshRules() {
        try {
            // Reload rules from database
            txnValidationMatrix.refresh();
            
            // Generate Drools rules
            String rules = txnValidationMatrix.generateRules();
            
            // Update rule engine
            validationRuleEngine.refreshRules(rules);
            
            log.info("Successfully refreshed validation rules");
        } catch (Exception e) {
            log.error("Failed to refresh validation rules", e);
            throw new RuntimeException("Failed to refresh validation rules", e);
        }
    }

    @Override
    public void applyRules(List<TransactionValidationRecord> txnValidationRecords, ExecutionContext context) {
        validationRuleEngine.fireRules(txnValidationRecords, context, txnValidationHelper);
    }

    @Override
    public boolean applyRulesShouldStop(List<TransactionValidationRecord> txnValidationRecords,
            ExecutionContext context, boolean stopOnError) {
        return validationRuleEngine.fireRulesShouldStop(txnValidationRecords, context, txnValidationHelper, stopOnError);
    }
}
```

## rule mapper

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.uob.gwb.pbp.dao.PwsQueryDao">

    <select id="getAllTransactionValidationRules" resultMap="PwsTransactionValidationRulesMap">
        SELECT 
            id,
            country_code,
            channel,
            bank_entity_id,
            source_format,
            resource_id,
            feature_id,
            field,
            field_value,
            condition1,
            condition2,
            business_rule,
            condition_flag,
            action,
            error_code,
            error_message
        FROM pws_transaction_validation_rules
        ORDER BY id
    </select>

    <resultMap id="PwsTransactionValidationRulesMap" type="com.uob.gwb.pbp.po.PwsTransactionValidationRules">
        <id column="id" property="id" />
        
        <!-- Static Conditions -->
        <result column="country_code" property="countryCode" />
        <result column="channel" property="channel" />
        <result column="bank_entity_id" property="bankEntityId" />
        <result column="source_format" property="sourceFormat" />
        <result column="resource_id" property="resourceId" />
        <result column="feature_id" property="featureId" />
        <result column="field" property="field" />
        <result column="field_value" property="fieldValue" />
        
        <!-- Dynamic Conditions -->
        <result column="condition1" property="condition1" />
        <result column="condition2" property="condition2" />
        <result column="business_rule" property="businessRule" />
        
        <!-- Validation Results -->
        <result column="condition_flag" property="condition_flag" />
        <result column="action" property="action" />
        <result column="error_code" property="errorCode" />
        <result column="error_message" property="errorMessage" />
    </resultMap>

</mapper>
```

```sql
-- Create sequence for ID generation
CREATE SEQUENCE PWS_TXN_VAL_RULES_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Create the main table
CREATE TABLE pws_transaction_validation_rules (
    id NUMBER(19) DEFAULT PWS_TXN_VAL_RULES_SEQ.NEXTVAL PRIMARY KEY,
    
    -- Static Conditions
    country_code VARCHAR2(3),
    channel VARCHAR2(50),
    bank_entity_id VARCHAR2(50),
    source_format VARCHAR2(50),
    resource_id VARCHAR2(50),
    feature_id VARCHAR2(50),
    field VARCHAR2(100),
    field_value VARCHAR2(255),
    
    -- Dynamic Conditions
    condition1 VARCHAR2(255),
    condition2 VARCHAR2(255),
    business_rule VARCHAR2(255),
    
    -- Validation Results
    condition_flag CHAR(1),
    action VARCHAR2(50),
    error_code VARCHAR2(50),
    error_message VARCHAR2(255),
    
    -- Audit Fields
    created_by VARCHAR2(50),
    created_date TIMESTAMP DEFAULT SYSTIMESTAMP,
    updated_by VARCHAR2(50),
    updated_date TIMESTAMP DEFAULT SYSTIMESTAMP
)
TABLESPACE USERS;

-- Create indexes for better query performance
CREATE INDEX idx_pws_txn_val_static 
    ON pws_transaction_validation_rules(country_code, channel, bank_entity_id, source_format)
    TABLESPACE USERS;

CREATE INDEX idx_pws_txn_val_feature 
    ON pws_transaction_validation_rules(resource_id, feature_id)
    TABLESPACE USERS;

-- Create trigger for updating the updated_date
CREATE OR REPLACE TRIGGER trg_pws_txn_val_rules_upd
    BEFORE UPDATE ON pws_transaction_validation_rules
    FOR EACH ROW
BEGIN
    :NEW.updated_date := SYSTIMESTAMP;
END;
/

-- Comments for documentation
COMMENT ON TABLE pws_transaction_validation_rules IS 'Stores payment transaction validation rules and conditions';
COMMENT ON COLUMN pws_transaction_validation_rules.id IS 'Primary key';
COMMENT ON COLUMN pws_transaction_validation_rules.country_code IS 'ISO country code for rule application';
COMMENT ON COLUMN pws_transaction_validation_rules.channel IS 'Payment channel (e.g., MOBILE, INTERNET)';
COMMENT ON COLUMN pws_transaction_validation_rules.bank_entity_id IS 'Bank entity identifier';
COMMENT ON COLUMN pws_transaction_validation_rules.source_format IS 'Source format of the payment (e.g., PAIN001)';
COMMENT ON COLUMN pws_transaction_validation_rules.resource_id IS 'Resource identifier';
COMMENT ON COLUMN pws_transaction_validation_rules.feature_id IS 'Feature identifier';
COMMENT ON COLUMN pws_transaction_validation_rules.field IS 'Transaction field name for validation';
COMMENT ON COLUMN pws_transaction_validation_rules.field_value IS 'Expected value for the field';
COMMENT ON COLUMN pws_transaction_validation_rules.condition1 IS 'First dynamic condition expression';
COMMENT ON COLUMN pws_transaction_validation_rules.condition2 IS 'Second dynamic condition expression';
COMMENT ON COLUMN pws_transaction_validation_rules.business_rule IS 'Business rule expression';
COMMENT ON COLUMN pws_transaction_validation_rules.condition_flag IS '1-fire when matching, 0-fire when not matching';
COMMENT ON COLUMN pws_transaction_validation_rules.action IS 'Action to take (e.g., STOP, WARN)';
COMMENT ON COLUMN pws_transaction_validation_rules.error_code IS 'Error code when rule fires';
COMMENT ON COLUMN pws_transaction_validation_rules.error_message IS 'Error message when rule fires';

-- Create synonyms if needed (assuming appropriate privileges)
CREATE PUBLIC SYNONYM pws_txn_val_rules FOR pws_transaction_validation_rules;

-- Grants (adjust according to your security requirements)
GRANT SELECT, INSERT, UPDATE, DELETE ON pws_transaction_validation_rules TO your_app_user;
GRANT SELECT ON PWS_TXN_VAL_RULES_SEQ TO your_app_user;
```

```sql
-- Example validation rules
INSERT INTO pws_transaction_validation_rules (
    country_code, channel, bank_entity_id, source_format, 
    field, field_value, condition1, action, error_code, error_message, 
    condition_flag, created_by
) VALUES (
    'SG', 'MOBILE', NULL, 'PAIN001',
    'transactionType', 'FAST', 'amount > 200000',
    'STOP', 'FAST_LIMIT_EXCEEDED', 'FAST payment limit exceeded for mobile channel',
    '1', 'SYSTEM'
);

INSERT INTO pws_transaction_validation_rules (
    country_code, channel, bank_entity_id, source_format,
    business_rule, action, error_code, error_message,
    condition_flag, created_by
) VALUES (
    'MY', NULL, NULL, NULL,
    'checkCrossBorderPayment(context, transaction)', 
    'WARN', 'CROSS_BORDER_CHECK', 'Please verify cross-border payment details',
    '1', 'SYSTEM'
);

INSERT INTO pws_transaction_validation_rules (
    country_code, channel, bank_entity_id, source_format,
    condition1, action, error_code, error_message,
    condition_flag, created_by
) VALUES (
    NULL, NULL, 'UOB', NULL,
    'amount < 100', 
    'STOP', 'MIN_AMOUNT', 'Amount below minimum threshold',
    '1', 'SYSTEM'
);

COMMIT;

-- Verify the inserts
SELECT id, country_code, channel, action, error_code 
FROM pws_transaction_validation_rules 
ORDER BY id;
```
