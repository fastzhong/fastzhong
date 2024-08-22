---
title: '☕️ CEW'
date: 2001-01-01
robots: 'noindex,nofollow'
xml: false
---

<!--more-->

## RuleTemplate

```java
public class GenericRuleTemplate implements RuleTemplate {

    @Override
    public String generateRule(DecisionMatrixRow row) {
        StringBuilder rule = new StringBuilder();
        rule.append("rule \"Rule_").append(row.getId()).append("\"\n");
        rule.append("when\n");
        rule.append("    $entity : Entity(");

        // Add simplified conditions
        for (SimplifiedConditionColumn condition : simplifiedConditionColumns) {
            String conditionValue = condition.getConditionExtractor().apply(row);
            if (conditionValue != null) {
                rule.append(condition.getVariableName()).append(" ").append(conditionValue).append(", ");
            }
        }

        // Add generic conditions
        for (GenericConditionColumn condition : genericConditionColumns) {
            String conditionExpression = condition.getConditionExtractor().apply(row);
            if (conditionExpression != null) {
                rule.append(conditionExpression).append(", ");
            }
        }

        rule.append(")\n");
        rule.append("then\n");

        // Add simplified actions
        for (SimplifiedActionColumn action : simplifiedActionColumns) {
            String actionValue = action.getActionExtractor().apply(row);
            if (actionValue != null) {
                rule.append("    $entity.set").append(capitalize(action.getVariableName()))
                    .append("(").append(actionValue).append(");\n");
            }
        }

        // Add generic actions
        for (GenericActionColumn action : genericActionColumns) {
            String actionExpression = action.getActionExtractor().apply(row);
            if (actionExpression != null) {
                rule.append(actionExpression).append("\n");
            }
        }

        rule.append("end\n");
        return rule.toString();
    }

    @Override
    public String generateValidationRule(DecisionMatrixRow row) {
        StringBuilder rule = new StringBuilder();
        rule.append("rule \"ValidationRule_").append(row.getId()).append("\"\n");
        rule.append("when\n");
        rule.ap
```

```java
@Service
public class DroolsRuleGeneratorService {

    public <T extends DecisionMatrixRow> String generateRules(List<T> rows, RuleTemplate template) {
        return rows.stream()
                .map(template::generateRule)
                .collect(Collectors.joining("\n"));
    }

    public <T extends DecisionMatrixRow> String generateValidationRules(List<T> rows, RuleTemplate template) {
        return rows.stream()
                .map(template::generateValidationRule)
                .collect(Collectors.joining("\n"));
    }
}
```

```java
@Mapper
public interface DecisionMatrixMapper {
    List<RiskDecisionMatrixRow> getAllRiskDecisionMatrixRows();
    List<ProductDecisionMatrixRow> getAllProductDecisionMatrixRows();
}
```

```java
public interface RiskDecisionMatrixDAO {
    List<RiskDecisionMatrixRow> getAll();
    RiskDecisionMatrixRow findById(Long id);
    void insert(RiskDecisionMatrixRow riskDecisionMatrixRow);
    void update(RiskDecisionMatrixRow riskDecisionMatrixRow);
    void delete(Long id);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.dao.RiskDecisionMatrixDAO">

    <resultMap id="RiskDecisionMatrixResultMap" type="RiskDecisionMatrixRow">
        <id column="id" property="id" />
        <result column="condition_age" property="conditionAge" />
        <result column="condition_income" property="conditionIncome" />
        <result column="condition_credit_score" property="conditionCreditScore" />
        <result column="action_risk_score" property="actionRiskScore" />
        <result column="action_approval_status" property="actionApprovalStatus" />
    </resultMap>

    <select id="getAll" resultMap="RiskDecisionMatrixResultMap">
        SELECT * FROM RiskDecisionMatrix
    </select>

    <select id="findById" resultMap="RiskDecisionMatrixResultMap">
        SELECT * FROM RiskDecisionMatrix WHERE id = #{id}
    </select>

    <insert id="insert">
        INSERT INTO RiskDecisionMatrix (id, condition_age, condition_income, condition_credit_score, action_risk_score, action_approval_status)
        VALUES (#{id}, #{conditionAge}, #{conditionIncome}, #{conditionCreditScore}, #{actionRiskScore}, #{actionApprovalStatus})
    </insert>

    <update id="update">
        UPDATE RiskDecisionMatrix
        SET condition_age = #{conditionAge},
            condition_income = #{conditionIncome},
            condition_credit_score = #{conditionCreditScore},
            action_risk_score = #{actionRiskScore},
            action_approval_status = #{actionApprovalStatus}
        WHERE id = #{id}
    </update>

    <delete id="delete">
        DELETE FROM RiskDecisionMatrix WHERE id = #{id}
    </delete>

</mapper>
```

```java

public interface DecisionMatrixRow {
    Long getId();

    Map<String, String> getSimplifiedConditionColumns();

    Map<String, String> getGenericConditionColumns();

    Map<String, String> getSimplifiedActionColumns();

    Map<String, String> getGenericActionColumns();
}


public class RiskDecisionMatrixRow implements DecisionMatrixRow {
    private Long id;
    private String conditionAge;
    private String conditionIncome;
    private String conditionCreditScore;
    private String genericCondition;

    private String actionRiskScore;
    private String actionApprovalStatus;
    private String genericAction;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Map<String, String> getSimplifiedConditionColumns() {
        Map<String, String> simplifiedConditions = new HashMap<>();
        simplifiedConditions.put("age", conditionAge);
        simplifiedConditions.put("income", conditionIncome);
        simplifiedConditions.put("creditScore", conditionCreditScore);
        return simplifiedConditions;
    }

    @Override
    public Map<String, String> getGenericConditionColumns() {
        Map<String, String> genericConditions = new HashMap<>();
        genericConditions.put("genericCondition", genericCondition);
        return genericConditions;
    }

    @Override
    public Map<String, String> getSimplifiedActionColumns() {
        Map<String, String> simplifiedActions = new HashMap<>();
        simplifiedActions.put("riskScore", actionRiskScore);
        simplifiedActions.put("approvalStatus", actionApprovalStatus);
        return simplifiedActions;
    }

    @Override
    public Map<String, String> getGenericActionColumns() {
        Map<String, String> genericActions = new HashMap<>();
        genericActions.put("genericAction", genericAction);
        return genericActions;
    }

    // Getters and setters for individual fields if needed
}
```

```java
@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer(KieFileSystem kfs) {
        KieServices kieServices = KieServices.Factory.get();
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();
        if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new RuntimeException("Error in Drools configuration: " + kieBuilder.getResults().toString());
        }
        ReleaseId krDefaultReleaseId = kieServices.getRepository().getDefaultReleaseId();
        return kieServices.newKieContainer(krDefaultReleaseId);
    }

    @Bean
    public KieSession kieSession(KieContainer kieContainer) {
        return kieContainer.newKieSession();
    }

    @Bean
    public KModuleBeanFactoryPostProcessor kiePostProcessor() {
        return new KModuleBeanFactoryPostProcessor();
    }
}
```

```java
@Service
public class LoanApprovalService {

    @Autowired
    private DecisionMatrixMapper decisionMatrixMapper;

    @Autowired
    private DroolsRuleGeneratorService ruleGeneratorService;

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private KieFileSystem kieFileSystem;

    @PostConstruct
    public void initializeDroolsRules() {
        // Load the decision matrix rows from the database
        List<DecisionMatrixRow> rows = decisionMatrixMapper.getAllDecisionMatrixRows();
        
        // Generate Drools rules using the GenericRuleTemplate
        String rules = ruleGeneratorService.generateRules(rows, new GenericRuleTemplate());
        
        // Write rules directly to the KieFileSystem in-memory
        kieFileSystem.write("src/main/resources/rules/generated_rules.drl", rules);
        
        // Compile the rules and load them into the KieContainer
        KieBuilder kieBuilder = kieContainer.getKieServices().newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        
        // Check for any errors in the rule compilation process
        if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new RuntimeException("Error compiling generated rules: " + kieBuilder.getResults());
        }
    }

    public void processLoanApplication(Applicant applicant) {
        // Create a new session, insert the applicant, and fire all rules
        KieSession kieSession = kieContainer.newKieSession();
        kieSession.insert(applicant);
        kieSession.fireAllRules();
        kieSession.dispose();
    }
}
```

```txt
rule "Compute TotalTransferAmount"
when
    $payments : List(size > 0) from collect(PaymentDto(creditorAccountNo == $creditorAccountNo, valueDate == $valueDate))
then
    BigDecimal totalAmount = $payments.stream()
                                       .map(PaymentDto::getPaymentAmount)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add);

    for (PaymentDto payment : $payments) {
        payment.setTotalTransferAmount(totalAmount);
    }
end
```

```txt
rule "Derive ResourceId and FeatureId"
when
    $payment : PaymentDto(paymentChannel == "ONLINE", fileType == "XML", creditorBankBic != "24", totalTransferAmount > 2000000)
then
    $payment.setResourceId("RES1");
    $payment.setFeatureId("FEAT1");
end
```

```txt
rule "Compute BulkAmount and BulkSize"
when
    $payments : List(size > 0) from collect(PaymentDto(splittingKey == $splittingKey))
then
    BigDecimal bulkAmount = $payments.stream()
                                      .map(PaymentDto::getPaymentAmount)
                                      .reduce(BigDecimal.ZERO, BigDecimal::add);

    int bulkSize = $payments.size();

    for (PaymentDto payment : $payments) {
        payment.setBulkAmount(bulkAmount);
        payment.setBulkSize(bulkSize);
    }
end
```

