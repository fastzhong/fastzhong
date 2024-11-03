# Component Test

```java
package com.uob.gwb.pbp.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class Pain001ProcessingE2ETest {

    @Autowired
    private DataSource paymentSaveDataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_FILE_PATH = "src/test/resources/pain001-test.json";
    private static final String INBOUND_DIR = "target/test-inbound";
    private static final String BACKUP_DIR = "target/test-backup";
    private static final String ERROR_DIR = "target/test-error";

    @BeforeAll
    static void setupDirectories() throws IOException {
        // Create test directories
        createDirectories();
    }

    @BeforeEach
    void setup() throws IOException {
        // Clean up directories
        cleanupDirectories();
        
        // Copy test file to inbound directory
        copyTestFile();
        
        // Clean and initialize database
        cleanDatabase();
        initializeDatabase();
    }

    @Test
    void shouldProcessPain001FileSuccessfully() throws Exception {
        // Wait for file processing
        Thread.sleep(2000); // Adjust based on your processing time

        // Verify database state
        verifyBulkTransactions();
        verifyTransactionInstructions();
        verifyParties();
        verifyContacts();
        verifyCharges();
        verifyTaxInstructions();
    }

    private static void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(INBOUND_DIR));
        Files.createDirectories(Paths.get(BACKUP_DIR));
        Files.createDirectories(Paths.get(ERROR_DIR));
    }

    private static void cleanupDirectories() throws IOException {
        FileSystemUtils.deleteRecursively(Paths.get(INBOUND_DIR));
        FileSystemUtils.deleteRecursively(Paths.get(BACKUP_DIR));
        FileSystemUtils.deleteRecursively(Paths.get(ERROR_DIR));
        createDirectories();
    }

    private void copyTestFile() throws IOException {
        Path source = Paths.get(TEST_FILE_PATH);
        Path target = Paths.get(INBOUND_DIR).resolve("payment_Auth.json");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        
        // Create done file
        Files.createFile(Paths.get(INBOUND_DIR).resolve("payment_Auth.xml.done"));
    }

    private void verifyBulkTransactions() {
        List<Map<String, Object>> bulkTxns = jdbcTemplate.queryForList(
            "SELECT * FROM PWS_BULK_TRANSACTIONS");
        
        assertThat(bulkTxns).isNotEmpty();
        // Add more specific assertions based on your test data
        Map<String, Object> firstTxn = bulkTxns.get(0);
        assertThat(firstTxn.get("STATUS")).isEqualTo("PENDING");
        // Add more assertions
    }

    private void verifyTransactionInstructions() {
        List<Map<String, Object>> instructions = jdbcTemplate.queryForList(
            "SELECT * FROM PWS_BULK_TRANSACTION_INSTRUCTIONS");
        
        assertThat(instructions).isNotEmpty();
        // Add more specific assertions
    }

    private void verifyParties() {
        List<Map<String, Object>> parties = jdbcTemplate.queryForList(
            "SELECT * FROM PWS_PARTIES");
        
        assertThat(parties).isNotEmpty();
        // Add more specific assertions
    }

    private void verifyContacts() {
        List<Map<String, Object>> contacts = jdbcTemplate.queryForList(
            "SELECT * FROM PWS_PARTY_CONTACTS");
        
        assertThat(contacts).isNotEmpty();
        // Add more specific assertions
    }

    private void verifyCharges() {
        List<Map<String, Object>> charges = jdbcTemplate.queryForList(
            "SELECT * FROM PWS_TRANSACTION_CHARGES");
        
        assertThat(charges).isNotEmpty();
        // Add more specific assertions
    }

    private void verifyTaxInstructions() {
        List<Map<String, Object>> taxes = jdbcTemplate.queryForList(
            "SELECT * FROM PWS_TAX_INSTRUCTIONS");
        
        assertThat(taxes).isNotEmpty();
        // Add more specific assertions
    }
}
```


# config

```yml
spring:
  profiles:
    active: test
  
  datasource:
    default:
      driver-class-name: org.h2.Driver
      url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
      username: sa
      password: 

    payment-save:
      driver-class-name: org.h2.Driver
      url: jdbc:h2:mem:paymentdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
      username: sa
      password: 

bulk-routes:
  - route-name: CUSTOMER_SUBMITTED_TRANSFORMED
    processing-type: INBOUND
    source-type: FILE
    enabled: true
    steps:
      - pain001-validation
      - payment-debulk
      - payment-validation
      - payment-enrichment
      - payment-save
    file-source:
      directoryName: target/test-inbound
      antInclude: "*_Auth.json"
      antExclude:
      charset: utf-8
      doneFileName: "${file:name:noext}.xml.done"
      delay: 1000
      sortBy: file:modified
      maxMessagesPerPoll: 1
      noop: false
      recursive: false
      move: target/test-backup
      moveFailed: target/test-error
      readLock: rename
      readLockTimeout: 10000
      readLockInterval: 1000
      readLockLoggingLevel: WARN
```

```java
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public DataSource defaultDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:schema-batch.sql")
            .addScript("classpath:schema-payment.sql")
            .build();
    }

    @Bean
    public DataSource paymentSaveDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:schema-payment.sql")
            .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource paymentSaveDataSource) {
        return new JdbcTemplate(paymentSaveDataSource);
    }
}
```

# SQL

## spring batch 

```sql
-- Spring Batch tables
CREATE TABLE BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL
);

CREATE TABLE BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP
);
```

## pws

```sql
-- PWS tables
CREATE TABLE PWS_BULK_TRANSACTIONS (
    BK_TRANSACTION_ID NUMBER(19) PRIMARY KEY,
    TRANSACTION_ID NUMBER(19),
    FILE_UPLOAD_ID NUMBER(19),
    RECIPIENTS_REFERENCE VARCHAR2(255),
    RECIPIENTS_DESCRIPTION VARCHAR2(255),
    FATE_FILE_NAME VARCHAR2(255),
    FATE_FILE_PATH VARCHAR2(255),
    COMBINE_DEBIT VARCHAR2(255),
    STATUS VARCHAR2(50),
    CHANGE_TOKEN NUMBER(19),
    ERROR_DETAIL VARCHAR2(4000),
    FINAL_FATE_UPDATED_DATE TIMESTAMP,
    ACK_FILE_PATH VARCHAR2(255),
    ACK_UPDATED_DATE TIMESTAMP,
    TRANSFER_DATE TIMESTAMP,
    USER_COMMENTS VARCHAR2(255),
    DMP_BATCH_NUMBER VARCHAR2(255),
    REJECT_CODE VARCHAR2(50),
    BATCH_BOOKING VARCHAR2(50),
    CHARGE_OPTIONS VARCHAR2(50),
    PAYROLL_OPTIONS VARCHAR2(50),
    BANK_REFERENCE_ID VARCHAR2(255)
);

CREATE TABLE PWS_BULK_TRANSACTION_INSTRUCTIONS (
    TRANSACTION_INSTRUCTIONS_ID NUMBER(19) PRIMARY KEY,
    BANK_REFERENCE_ID VARCHAR2(255),
    CHILD_BANK_REFERENCE_ID VARCHAR2(255),
    TRANSACTION_ID NUMBER(19),
    CHILD_TRANSACTION_INSTRUCTIONS_ID NUMBER(19),
    VALUE_DATE TIMESTAMP,
    SETTLEMENT_DATE TIMESTAMP,
    PAYMENT_CODE_ID VARCHAR2(50),
    PAYMENT_DETAILS VARCHAR2(4000),
    RAIL_CODE VARCHAR2(50),
    IS_RECURRING VARCHAR2(1),
    IS_PRE_APPROVED VARCHAR2(1),
    CUSTOMER_REFERENCE VARCHAR2(255),
    REMARKS_FOR_APPROVAL VARCHAR2(255),
    USER_COMMENTS VARCHAR2(255),
    DUPLICATION_FLAG VARCHAR2(1),
    REJECT_CODE VARCHAR2(50),
    TRANSACTION_CURRENCY VARCHAR2(3),
    TRANSACTION_AMOUNT NUMBER(19,2),
    EQUIVALENT_CURRENCY VARCHAR2(3),
    EQUIVALENT_AMOUNT NUMBER(19,2),
    DESTINATION_COUNTRY VARCHAR2(2),
    DESTINATION_BANK_NAME VARCHAR2(255),
    FX_FLAG VARCHAR2(1),
    CHARGE_OPTIONS VARCHAR2(50),
    TRANSFER_SPEED NUMBER(19),
    IS_NEW NUMBER(1),
    IS_BULK_NEW NUMBER(1),
    BOP_PURPOSE_CODE VARCHAR2(50),
    ADDITIONAL_PURPOSE_CODE VARCHAR2(50),
    APPROVAL_CODE VARCHAR2(50),
    CUSTOMER_TRANSACTION_STATUS VARCHAR2(50),
    PROCESSING_STATUS VARCHAR2(50),
    REJECT_REASON VARCHAR2(255),
    ORIGINAL_VALUE_DATE TIMESTAMP,
    DMP_TRANS_REF VARCHAR2(255),
    CHILD_TEMPLATE_ID NUMBER(19),
    INITIATION_TIME TIMESTAMP
);

CREATE TABLE PWS_PARTIES (
    PARTY_ID NUMBER(19) PRIMARY KEY,
    BANK_REFERENCE_ID VARCHAR2(255),
    PARTY_TYPE VARCHAR2(50),
    NAME VARCHAR2(255)
);

CREATE TABLE PWS_PARTY_CONTACTS (
    PARTY_CONTACT_ID NUMBER(19) PRIMARY KEY,
    PARTY_ID NUMBER(19),
    BANK_REFERENCE_ID VARCHAR2(255),
    PARTY_TYPE VARCHAR2(50),
    CONTACT_TYPE VARCHAR2(50),
    CONTACT_VALUE VARCHAR2(255)
);

CREATE TABLE PWS_TAX_INSTRUCTIONS (
    TAX_INSTRUCTION_ID NUMBER(19) PRIMARY KEY,
    BANK_REFERENCE_ID VARCHAR2(255),
    TAX_TYPE VARCHAR2(50),
    TAX_AMOUNT NUMBER(19,2)
);

CREATE TABLE PWS_TRANSACTION_CHARGES (
    CHARGE_ID NUMBER(19) PRIMARY KEY,
    BANK_REFERENCE_ID VARCHAR2(255),
    CHARGE_TYPE VARCHAR2(50),
    CHARGE_AMOUNT NUMBER(19,2)
);

CREATE TABLE PWS_TRANSACTION_ADVICES (
    ADVICE_ID NUMBER(19) PRIMARY KEY,
    BANK_REFERENCE_ID VARCHAR2(255),
    ADVICE_TYPE VARCHAR2(50),
    ADVICE_DETAILS VARCHAR2(4000)
);

-- Sequences
CREATE SEQUENCE SEQ_BANK_REF_NO START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_BULK_TXN_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_TXN_INSTRUCTION_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_PARTY_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_PARTY_CONTACT_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_TAX_INSTRUCTION_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_CHARGE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_ADVICE_ID START WITH 1 INCREMENT BY 1;
```
