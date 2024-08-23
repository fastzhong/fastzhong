---
title: '☕️ CEW'
date: 2001-01-01
robots: 'noindex,nofollow'
xml: false
---

<!--more-->

## RuleTemplate

```java
public interface DecisionMatrixRow {

    Long getId();

    Map<String, String> getFieldConditionColumns();

    Map<String, String> getBusinessConditionColumns();

    Map<String, String> getFieldActionColumns();

    Map<String, String> getBusinessActionColumns();

}
```

```java
public class GenericRuleTemplate implements RuleTemplate {

    @Override
    public String generateRule(DecisionMatrixRow row) {

        StringBuilder rule = new StringBuilder();
        rule.append("rule \"Rule_").append(row.getId()).append("\"\n");
        rule.append("when\n");
        rule.append("    $entity : Entity(");

        // Add field conditions
        Map<String, String> fieldConditionCols = row.getFieldConditionColumns();
        final AtomicBoolean firstCondition = new AtomicBoolean(true);
        fieldConditionCols.keySet().forEach(k -> {
           if (StringUtils.isNotEmpty(fieldConditionCols.get(k))) {
               if (!firstCondition.get()) {
                   rule.append(", ");
               }
               rule.append(k).append(" ").append(fieldConditionCols.get(k));
               firstCondition.set(false);
           }
        });

        // Add business rule condition
        Map<String, String> businessConditionCols = row.getBusinessConditionColumns();
        firstCondition.set(true);
        businessConditionCols.keySet().forEach(k -> {
            if (StringUtils.isNotEmpty(businessConditionCols.get(k))) {
                if (!firstCondition.get()) {
                    rule.append(", ");
                }
                rule.append(businessConditionCols.get(k)).append(", ");
                firstCondition.set(false);
            }
        });

        rule.append(")\n");
        rule.append("then\n");

        // Add field actions
        Map<String, String> fieldActionCols = row.getFieldActionColumns();
        fieldActionCols.keySet().forEach(k -> {
            if (StringUtils.isNotEmpty(fieldActionCols.get(k))) {
                rule.append("    $entity.set").append(k)
                        .append("(").append(fieldActionCols.get(k)).append(");\n");
            }
        });

        // Add generic actions
        Map<String, String> businessActionCols = row.getBusinessActionColumns();
        businessActionCols.keySet().forEach(k -> {
            if (StringUtils.isNotEmpty(businessActionCols.get(k))) {
                rule.append(businessActionCols.get(k)).append("\n");
            }
        });

        rule.append("end\n");
        return rule.toString();
    }

}
```
