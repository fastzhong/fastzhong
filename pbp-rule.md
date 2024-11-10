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
public String generateRule(DecisionMatrixRow row) {
    return """
        global ExecutionContext context;
        global TransactionValidationHelper validationHelper;
        global AtomicBoolean shouldStop;
        
        rule "%s"
        // ... rest of your rule template
        """.formatted(row.getRuleName());
}

public void updateRules(List<DecisionMatrixRow> decisionMatrix) {
        List<String> rules = decisionMatrix.stream()
                .map(ruleTemplate::generateRule)
                .collect(Collectors.toList());

        // Add a rule to set the global variable when an error is encountered
        rules.add(
            "rule \"Set Stop Flag On Error\"\n" +
            "when\n" +
            "    $tx : PwsTransactions(authorizationStatus == \"REJECTED\" || processingStatus == \"ERROR\")\n" +
            "then\n" +
            "    drools.getKieRuntime().setGlobal(\"shouldStop\", new AtomicBoolean(true));\n" +
            "end"
        );

        ruleEngine.updateRules(rules);
    }
```

