
    static {
        ISO_DATE_FORMAT.setLenient(false); // Disable lenient parsing to enforce strict format checking
    }

public class Pain001InboundProcessingResult {
    private List<PwsSaveRecord> paymentSaved = new ArrayList<>();
    private List<PwsSaveRecord> paymentSavedError = new ArrayList<>();
    // Other fields and methods...
}

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentUtilsTest {

    @Mock
    private TransactionUtils txnUtils;

    @InjectMocks
    private PaymentUtils paymentUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreatePwsSaveRecord_WithValidIdAndDmpTxnRef() {
        long id = 123L;
        String dmpTxnRef = "dmpRef123";
        String decryptedId = "decrypted123";
        when(txnUtils.getDecrypted(String.valueOf(id))).thenReturn(decryptedId);

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, dmpTxnRef);

        assertEquals(decryptedId, record.getTxnId());
        assertEquals(dmpTxnRef, record.getDmpTxnRef());
        verify(txnUtils).getDecrypted(String.valueOf(id));
    }

    @Test
    void testCreatePwsSaveRecord_WithZeroId() {
        long id = 0;
        String dmpTxnRef = "dmpRef123";

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, dmpTxnRef);

        assertEquals("", record.getTxnId());
        assertEquals(dmpTxnRef, record.getDmpTxnRef());
        verifyNoInteractions(txnUtils);
    }

    @Test
    void testCreatePwsSaveRecord_WithNullDmpTxnRef() {
        long id = 123L;
        String decryptedId = "decrypted123";
        when(txnUtils.getDecrypted(String.valueOf(id))).thenReturn(decryptedId);

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, null);

        assertEquals(decryptedId, record.getTxnId());
        assertEquals("", record.getDmpTxnRef());
    }

    @Test
    void testUpdatePaymentSaved() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord("txn123", "ref123");

        paymentUtils.updatePaymentSaved(result, record);

        assertTrue(result.getPaymentSaved().contains(record));
    }

    @Test
    void testUpdatePaymentSavedError_WhenRecordNotPresent() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord("txn123", "ref123");

        paymentUtils.updatePaymentSavedError(result, record);

        assertTrue(result.getPaymentSavedError().contains(record));
    }

    @Test
    void testUpdatePaymentSavedError_WhenRecordAlreadyPresent() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord("txn123", "ref123");
        result.getPaymentSavedError().add(record); // Pre-add record to simulate already present

        paymentUtils.updatePaymentSavedError(result, record);

        assertEquals(1, result.getPaymentSavedError().size());
        assertTrue(result.getPaymentSavedError().contains(record));
    }
}

    

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
