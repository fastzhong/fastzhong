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

## Rules 

```java
package com.uob.gwb.pbp.rule;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GenericValidationRuleTemplate implements ValidationRuleTemplate {

    public static String columnToProperty(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    private String buildStaticConditions(ValidationDecisionMatrixRow row) {
        List<String> conditions = new ArrayList<>();

        // Process each static condition column
        for (ValidationStaticConditionColumn col : ValidationStaticConditionColumn.values()) {
            String value = row.getStaticCondition(col);
            if (StringUtils.isNotBlank(value)) {
                String property = columnToProperty(col.getColumnName());
                
                // Handle Field and FieldValue special case
                if (col == ValidationStaticConditionColumn.Field) {
                    String fieldValue = row.getStaticCondition(ValidationStaticConditionColumn.FieldValue);
                    if (StringUtils.isNotBlank(fieldValue)) {
                        conditions.add(String.format("transaction.%s == \"%s\"", value, fieldValue));
                    }
                    continue;
                }
                
                if (col != ValidationStaticConditionColumn.FieldValue) {
                    conditions.add(String.format("%s == \"%s\"", property, value));
                }
            }
        }

        return String.join(", ", conditions);
    }

    private String buildDynamicConditions(ValidationDecisionMatrixRow row) {
        List<String> conditions = new ArrayList<>();

        // Process simple conditions
        String simpleCond1 = row.getDynamicCondition(ValidationDynamicConditionColumn.SimpleCon1);
        if (StringUtils.isNotBlank(simpleCond1)) {
            conditions.add(String.format("transaction.%s", simpleCond1));
        }

        String simpleCond2 = row.getDynamicCondition(ValidationDynamicConditionColumn.SimpleCon2);
        if (StringUtils.isNotBlank(simpleCond2)) {
            conditions.add(String.format("transaction.%s", simpleCond2));
        }

        // Process business rule condition
        String businessRule = row.getDynamicCondition(ValidationDynamicConditionColumn.BusinessRule);
        if (StringUtils.isNotBlank(businessRule)) {
            conditions.add(String.format("eval(validationHelper.%s)", businessRule));
        }

        return String.join(" && ", conditions);
    }

    @Override
    public String generateRule(ValidationDecisionMatrixRow row) {
        StringBuilder rule = new StringBuilder();

        // Add global declarations
        rule.append("import com.uob.gwb.pbp.bo.validation.TransactionValidationRecord;\n");
        rule.append("import com.uob.gwb.pbp.bo.CreditTransferTransaction;\n");
        rule.append("import org.springframework.batch.item.ExecutionContext;\n");
        rule.append("import java.util.concurrent.atomic.AtomicBoolean;\n\n");
        
        rule.append("global ExecutionContext context;\n");
        rule.append("global TransactionValidationHelper validationHelper;\n");
        rule.append("global AtomicBoolean shouldStop;\n\n");

        // Rule header
        rule.append("rule \"Rule_").append(row.getId()).append("\"\n");
        rule.append("when\n");
        
        // Start condition building
        rule.append("    $record: TransactionValidationRecord(\n");
        
        // Add static conditions
        String staticConds = buildStaticConditions(row);
        if (StringUtils.isNotBlank(staticConds)) {
            rule.append("        ").append(staticConds).append(",\n");
        }
        
        // Ensure transaction is not null
        rule.append("        transaction != null");
        
        // Close basic conditions
        rule.append("\n    )\n");
        
        // Add dynamic conditions if present
        String dynamicConds = buildDynamicConditions(row);
        if (StringUtils.isNotBlank(dynamicConds)) {
            rule.append("    and ").append(dynamicConds).append("\n");
        }
        
        // Rule actions
        rule.append("then\n");
        
        // Add validation error if condition flag matches
        if ("1".equals(row.getValidationResult(ValidationResultColumn.CondFlag))) {
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
        }
        
        rule.append("end\n");
        
        return rule.toString();
    }
}
```

