---
title: '☕️ CEW'
date: 2001-01-01
robots: 'noindex,nofollow'
xml: false
---

<!--more-->

##

```java
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProcessContextHolder {
    private final Map<String, Map<String, Object>> contexts = new ConcurrentHashMap<>();

    public void setContext(String fileId, String key, Object value) {
        contexts.computeIfAbsent(fileId, id -> new HashMap<>())
                .put(key, value);
    }

    public Object getContext(String fileId, String key) {
        return contexts.getOrDefault(fileId, new HashMap<>())
                .get(key);
    }

    public void clearContext(String fileId) {
        contexts.remove(fileId);
    }
}
```

## Process

```java
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentFileProcessingService {
    private final ProcessContextHolder contextHolder;

    public PaymentFileProcessingService(ProcessContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    public List<Message<Payment>> processFile(Message<String> message) {
        String fileId = message.getHeaders().getId().toString();
        String fileContent = message.getPayload();

        try {
            // Parse the file content to extract common information and payments
            List<Payment> payments = parsePayments(fileContent);
            storeCommonInformation(fileId, fileContent);

            // Create messages for each payment
            return payments.stream()
                    .map(payment -> MessageBuilder.withPayload(payment)
                            .setHeader("fileId", fileId)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Handle exceptions as needed
            throw e; // Rethrow or log the exception
        } finally {
            // Clean up the context for the specific file ID
            contextHolder.clearContext(fileId);
        }
    }

    private void storeCommonInformation(String fileId, String fileContent) {
        // TODO: Extract common information from the file content and store it in the context
        contextHolder.setContext(fileId, "commonInfo", fileContent);
    }

    private List<Payment> parsePayments(String fileContent) {
        // TODO: Implement your logic to parse the file content and return a list of Payment objects
        return List.of(); // Placeholder
    }
}
```

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
