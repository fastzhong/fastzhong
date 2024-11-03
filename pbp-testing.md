```java
@ExtendWith(MockitoExtension.class)
class PaymentSaveImplTest {

    @Mock
    private AppConfig config;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private SqlSessionTemplate paymentSaveSqlSessionTemplate;

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @Mock
    private SqlSession sqlSession;

    @Mock
    private PwsSaveDao pwsSaveDao;

    @Mock
    private PaymentUtils paymentUtils;

    @InjectMocks
    private PaymentSaveImpl paymentSave;

    private PaymentInformation paymentInfo;
    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private Pain001InboundProcessingResult result;
    private PwsSaveRecord mockRecord;

    @BeforeEach
    void setUp() {
        // Initialize basic test data
        paymentInfo = new PaymentInformation();
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        result.setPaymentSaved(new ArrayList<>());
        result.setPaymentSavedError(new ArrayList<>());
        jobContext.put("result", result);

        // Create mock record
        mockRecord = new PwsSaveRecord("encrypted_1", "BATCH001");

        // Setup SQL Session behavior
        when(paymentSaveSqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        when(sqlSessionFactory.openSession(any(ExecutorType.class))).thenReturn(sqlSession);
        when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);

        // Setup PaymentUtils default behavior
        when(paymentUtils.createPwsSaveRecord(anyLong(), anyString())).thenReturn(mockRecord);
        doNothing().when(paymentUtils).updatePaymentSaved(any(), any());
        doNothing().when(paymentUtils).updatePaymentSavedError(any(), any());
    }

    @Test
    void savePaymentInformation_SuccessfulSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);
        setupValidPaymentInfo();
        setupSuccessfulBulkSave();
        setupSuccessfulChildTransactionSave();

        // Mock mapper method existence for batch insert
        when(pwsSaveDao.getClass().getMethod(anyString(), any(Class.class)))
            .thenReturn(null); // Just to avoid NoSuchMethodException

        // Act
        paymentSave.savePaymentInformation(paymentInfo, stepContext, jobContext);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(anyLong(), anyString());
        verify(paymentUtils).updatePaymentSaved(eq(result), any(PwsSaveRecord.class));
        verify(paymentUtils, never()).updatePaymentSavedError(any(), any());
        verify(pwsSaveDao).insertPwsTransactions(any(PwsTransactions.class));
        verify(pwsSaveDao).insertPwsBulkTransactions(any(PwsBulkTransactions.class));
        verify(sqlSession, never()).rollback();
    }

    @Test
    void savePaymentInformation_FailedBatchSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);
        setupValidPaymentInfo();
        setupSuccessfulBulkSave();
        setupFailedChildTransactionSave();

        // Act
        paymentSave.savePaymentInformation(paymentInfo, stepContext, jobContext);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(anyLong(), anyString());
        verify(paymentUtils, never()).updatePaymentSaved(any(), any());
        verify(paymentUtils).updatePaymentSavedError(eq(result), any(PwsSaveRecord.class));
    }

    private void setupValidPaymentInfo() {
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);

        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");

        List<CreditTransferTransaction> transactions = new ArrayList<>();
        transactions.add(createValidTransaction());
        transactions.add(createValidTransaction());

        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);
        paymentInfo.setCreditTransferTransactionList(transactions);
    }

    private CreditTransferTransaction createValidTransaction() {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        transaction.setDmpTransactionStatus(DmpTransactionStatus.APPROVED);
        
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        Party party = new Party();
        PwsParties pwsParties = new PwsParties();
        party.setPwsParties(pwsParties);

        transaction.setPwsBulkTransactionInstructions(instructions);
        transaction.setParty(party);

        return transaction;
    }

    private void setupSuccessfulBulkSave() {
        when(pwsSaveDao.getBankRefSequenceNum()).thenReturn(1);
        when(pwsSaveDao.insertPwsTransactions(any())).thenReturn(1L);
        when(pwsSaveDao.insertPwsBulkTransactions(any())).thenReturn(1L);
    }

    private void setupSuccessfulChildTransactionSave() {
        doAnswer(invocation -> {
            RetryCallback<Object, RuntimeException> callback = invocation.getArgument(0);
            return callback.doWithRetry(null);
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));

        // Mock successful batch insert
        doNothing().when(sqlSession).commit();
        doNothing().when(sqlSession).close();
    }

    private void setupFailedChildTransactionSave() {
        doThrow(new BulkProcessingException("Failed to insert", new RuntimeException()))
            .when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
        
        doNothing().when(sqlSession).rollback();
        doNothing().when(sqlSession).close();
    }
}
```


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
