# rule mapper

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

# test

# rule 

```java
```

