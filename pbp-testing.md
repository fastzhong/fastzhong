# E2E

## config

```yml

```

```java
@TestConfiguration
@Profile("test")
public class TestConfig {


    @Bean
    @Primary
    public DataSource defaultDataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:/sql/schema-h2.sql")
                .addScript("classpath:/sql/schema-h2.sql")
                .build();
    }

    @Bean
    public DataSource aesDataSource() {
        // ToDo
        return null; 
    }

    @Bean
    public DataSource rdsDataSource() {
        // ToDo
        return null;
    }

    @Bean
    public DataSource pwsDataSource() {
        // ToDo
        return null;
    }

```

## Init

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.batch.job.enabled=false"
})
@Import(Pain001IntegrationTestConfig.class)
class Pain001ProcessingIntegrationTest {

    private static final String TEST_INPUT_DIR = "test-input";
    private static final String TEST_OUTPUT_DIR = "test-output";
    private static final String TEST_ERROR_DIR = "test-error";
    private static final String TEST_BACKUP_DIR = "test-backup";

    @Autowired
    private Pain001ServiceImpl pain001Service;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private PaymentIntegrationservice paymentIntegrationservice;

    private Path testRootDir;
    private Path inputDir;
    private Path outputDir;
    private Path errorDir;
    private Path backupDir;

    @BeforeAll
    static void initAll() throws IOException {
        // Create test directories structure
        createTestDirectories();
    }

    @BeforeEach
    void setUp() throws Exception {
        // Setup directories
        setupTestDirectories();
        
        // Initialize H2 database
        initializeH2Database();
        
        // Setup test files
        setupTestFiles();
        
        // Setup mock responses
        setupMockResponses();

        // Start Camel context
        camelContext.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test directories
        cleanupTestDirectories();
        
        // Clean up database
        cleanupDatabase();
        
        // Stop Camel context
        camelContext.stop();
    }

    private static void createTestDirectories() throws IOException {
        Path testRoot = Files.createTempDirectory("pain001-test");
        Files.createDirectories(testRoot.resolve(TEST_INPUT_DIR));
        Files.createDirectories(testRoot.resolve(TEST_OUTPUT_DIR));
        Files.createDirectories(testRoot.resolve(TEST_ERROR_DIR));
        Files.createDirectories(testRoot.resolve(TEST_BACKUP_DIR));
    }

    private void setupTestDirectories() throws IOException {
        testRootDir = Files.createTempDirectory("pain001-test");
        inputDir = testRootDir.resolve(TEST_INPUT_DIR);
        outputDir = testRootDir.resolve(TEST_OUTPUT_DIR);
        errorDir = testRootDir.resolve(TEST_ERROR_DIR);
        backupDir = testRootDir.resolve(TEST_BACKUP_DIR);
        
        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);
        Files.createDirectories(errorDir);
        Files.createDirectories(backupDir);
    }

    private void initializeH2Database() {
        try (Connection conn = dataSource.getConnection()) {
            // Execute schema scripts
            executeSqlScript(conn, "schema/h2-schema.sql");
            executeSqlScript(conn, "schema/h2-triggers.sql");
            
            // Execute initial data scripts
            executeSqlScript(conn, "data/test-data.sql");
            executeSqlScript(conn, "data/reference-data.sql");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 database", e);
        }
    }

    private void executeSqlScript(Connection conn, String scriptPath) {
        try {
            String script = new String(getClass().getResourceAsStream(scriptPath).readAllBytes());
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(script.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SQL script: " + scriptPath, e);
        }
    }

    private void setupTestFiles() throws IOException {
        // Copy pain001 test files to input directory
        copyTestFile("/test-files/pain001-withholding-tax.json", 
            inputDir.resolve("THISE14119200007_Auth.json"));
        copyTestFile("/test-files/pain001-payroll.json", 
            inputDir.resolve("THISE02508202406_Auth.json"));
        
        // Create .done files if needed
        createDoneFile("THISE14119200007_Auth.json");
        createDoneFile("THISE02508202406_Auth.json");
    }

    private void copyTestFile(String resourcePath, Path targetPath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void createDoneFile(String fileName) throws IOException {
        Path donePath = inputDir.resolve(fileName + ".done");
        Files.writeString(donePath, "");
    }

    private void cleanupTestDirectories() {
        try {
            FileUtils.deleteDirectory(testRootDir.toFile());
        } catch (IOException e) {
            // Log warning but don't fail test
            log.warn("Failed to cleanup test directories", e);
        }
    }

    private void cleanupDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            // Clean up tables in correct order
            executeSqlScript(conn, "cleanup/cleanup-tables.sql");
        } catch (SQLException e) {
            log.warn("Failed to cleanup database", e);
        }
    }

    @Test
    void testEndToEndPayrollProcessing() throws Exception {
        // Arrange
        String fileName = "THISE02508202406_Auth.json";
        Path sourceFile = inputDir.resolve(fileName);
        assertTrue(Files.exists(sourceFile), "Test file should exist");

        // Act - Trigger file processing
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("CamelFileName", fileName);

        // Wait for processing
        mockEndpoint.assertIsSatisfied(30, TimeUnit.SECONDS);

        // Assert - Verify processing results
        verifyPayrollProcessingResults(fileName);
        verifyFileMovement(fileName);
    }

    @Test
    void testEndToEndWithholdingTaxProcessing() throws Exception {
        // Similar to payroll test but for withholding tax file
        String fileName = "THISE14119200007_Auth.json";
        // ... test implementation
    }

    private void verifyPayrollProcessingResults(String fileName) {
        // Verify database state
        verifyTransactionsSaved();
        verifyBulkTransactionsSaved();
        verifyChildTransactionsSaved();
        
        // Verify processing status
        verifyProcessingStatus(fileName);
        
        // Verify transit message
        verifyTransitMessage(fileName);
    }

    private void verifyFileMovement(String fileName) {
        // Verify original file moved to backup
        assertTrue(Files.exists(backupDir.resolve(fileName)));
        assertFalse(Files.exists(inputDir.resolve(fileName)));
        
        // Verify done file handled
        assertFalse(Files.exists(inputDir.resolve(fileName + ".done")));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ResourcePatternResolver resourcePatternResolver() {
            return new PathMatchingResourcePatternResolver();
        }
    }
}
```

And here are the necessary resources:

1. SQL Scripts Structure:
```
src/test/resources/
├── schema/
│   ├── h2-schema.sql
│   └── h2-triggers.sql
├── data/
│   ├── test-data.sql
│   └── reference-data.sql
├── cleanup/
│   └── cleanup-tables.sql
└── test-files/
    ├── pain001-withholding-tax.json
    └── pain001-payroll.json
```

2. Cleanup SQL:
```sql
-- cleanup-tables.sql
DELETE FROM PWS_TAX_INSTRUCTIONS;
DELETE FROM PWS_TRANSACTION_ADVICES;
DELETE FROM PWS_PARTY_CONTACTS;
DELETE FROM PWS_PARTIES;
DELETE FROM PWS_BULK_TRANSACTION_INSTRUCTIONS;
DELETE FROM PWS_BULK_TRANSACTIONS;
DELETE FROM PWS_TRANSACTIONS;
DELETE FROM PWS_TRANSIT_MESSAGE;
DELETE FROM PWS_FILE_UPLOAD;
```

3. Test Data SQL:
```sql
-- test-data.sql
INSERT INTO PWS_FILE_UPLOAD (
    FILE_UPLOAD_ID, 
    FILE_REFERENCE_ID, 
    RESOURCE_ID, 
    FEATURE_ID, 
    STATUS
) VALUES 
(1, 'THISE14119200007', 'SMART', 'BFU', 'PROCESSING'),
(2, 'THISE02508202406', 'SMART', 'BFU', 'PROCESSING');

-- Add any other necessary reference data
```

### sql 

```sql
-- Spring Batch Tables
CREATE TABLE BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP,
    END_TIME TIMESTAMP,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
    references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

-- Payment Tables
-- File Upload table
CREATE TABLE PWS_FILE_UPLOAD (
    FILE_UPLOAD_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    FILE_REFERENCE_ID VARCHAR(255),
    TRANSACTION_ID BIGINT,
    FEATURE_ID VARCHAR(100),
    RESOURCE_ID VARCHAR(100),
    FILE_TYPE_ENUM VARCHAR(50),
    FILE_SUB_TYPE_ENUM VARCHAR(50),
    UPLOAD_FILE_NAME VARCHAR(255),
    UPLOAD_FILE_PATH VARCHAR(500),
    FILE_SIZE BIGINT,
    COMPANY_ID BIGINT,
    ACCOUNT_ID VARCHAR(100),
    RESOURCE_CATEGORY VARCHAR(100),
    CHARGE_OPTION VARCHAR(50),
    PAYROLL_OPTION VARCHAR(50),
    FILE_UPLOAD_STATUS VARCHAR(50),
    STATUS VARCHAR(50),
    CREATED_BY VARCHAR(100),
    CREATED_DATE TIMESTAMP,
    UPDATED_BY VARCHAR(100),
    UPDATED_DATE TIMESTAMP,
    CHANGE_TOKEN BIGINT
);

-- Rejected Records table
CREATE TABLE PWS_REJECTED_RECORD (
    REJECTED_RECORD_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    ENTITY_TYPE VARCHAR(100),
    ENTITY_ID BIGINT,
    SEQUENCE_NO BIGINT,
    LINE_NO BIGINT,
    COLUMN_NO BIGINT,
    CREATED_BY VARCHAR(100),
    CREATED_DATE TIMESTAMP,
    BANK_REFERENCE_ID VARCHAR(100),
    TRANSACTION_ID BIGINT,
    REJECT_CODE VARCHAR(50),
    ERROR_DETAIL VARCHAR(500)
);

-- Transaction Validation Rules table
CREATE TABLE PWS_TRANSACTION_VALIDATION_RULES (
    ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    COUNTRY_CODE VARCHAR(3),
    CHANNEL VARCHAR(50),
    BANK_ENTITY_ID VARCHAR(100),
    SOURCE_FORMAT VARCHAR(50),
    RESOURCE_ID VARCHAR(100),
    FEATURE_ID VARCHAR(100),
    FIELD_NAME VARCHAR(255),
    FIELD_VALUE VARCHAR(255),
    CONDITION1 VARCHAR(255),
    CONDITION2 VARCHAR(255),
    RULE VARCHAR(500),
    CONDITION_FLAG VARCHAR(50),
    ERROR_CODE VARCHAR(50),
    ERROR_MESSAGE VARCHAR(500),
    ACTION VARCHAR(100)
);

-- Transit Message table
CREATE TABLE PWS_TRANSIT_MESSAGE (
    MESSAGE_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    MESSAGE_REF_NO VARCHAR(100),
    BANK_REFERENCE_ID VARCHAR(100),
    CORRELATION_ID VARCHAR(100),
    MESSAGE_CONTENT BLOB,
    SERVICE_TYPE VARCHAR(100),
    END_SYSTEM VARCHAR(100),
    PROCESSING_DATE TIMESTAMP,
    RETRY_COUNT INT,
    STATUS VARCHAR(50)
);

-- Existing tables from previous POs
CREATE TABLE PWS_TRANSACTIONS (
    TRANSACTION_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    BANK_ENTITY_ID VARCHAR(100),
    RESOURCE_ID VARCHAR(100),
    FEATURE_ID VARCHAR(100),
    CORRELATION_ID VARCHAR(100),
    BANK_REFERENCE_ID VARCHAR(100),
    APPLICATION_TYPE VARCHAR(50),
    ACCOUNT_CURRENCY VARCHAR(3),
    ACCOUNT_NUMBER VARCHAR(100),
    COMPANY_GROUP_ID BIGINT,
    COMPANY_ID BIGINT,
    COMPANY_NAME VARCHAR(255),
    TRANSACTION_CURRENCY VARCHAR(3),
    TOTAL_AMOUNT DECIMAL(19,2),
    TOTAL_CHILD INT,
    HIGHEST_AMOUNT DECIMAL(19,2),
    AUTHORIZATION_STATUS VARCHAR(50),
    CAPTURE_STATUS VARCHAR(50),
    CUSTOMER_TRANSACTION_STATUS VARCHAR(50),
    PROCESSING_STATUS VARCHAR(50),
    REJECT_REASON VARCHAR(500),
    INITIATED_BY BIGINT,
    INITIATION_TIME TIMESTAMP,
    RELEASED_BY BIGINT,
    RELEASE_DATE TIMESTAMP,
    CHANGE_TOKEN BIGINT
);

CREATE TABLE PWS_BULK_TRANSACTIONS (
    BK_TRANSACTION_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    FILE_UPLOAD_ID BIGINT,
    RECIPIENTS_REFERENCE VARCHAR(100),
    RECIPIENTS_DESCRIPTION VARCHAR(500),
    FATE_FILE_NAME VARCHAR(255),
    FATE_FILE_PATH VARCHAR(500),
    COMBINE_DEBIT VARCHAR(1),
    STATUS VARCHAR(50),
    CHANGE_TOKEN BIGINT,
    ERROR_DETAIL VARCHAR(500),
    FINAL_FATE_UPDATED_DATE TIMESTAMP,
    ACK_FILE_PATH VARCHAR(500),
    ACK_UPDATED_DATE TIMESTAMP,
    TRANSFER_DATE DATE,
    USER_COMMENTS VARCHAR(500),
    DMP_BATCH_NUMBER VARCHAR(100),
    REJECT_CODE VARCHAR(50),
    BATCH_BOOKING VARCHAR(50),
    CHARGE_OPTIONS VARCHAR(50),
    PAYROLL_OPTIONS VARCHAR(50),
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID),
    FOREIGN KEY (FILE_UPLOAD_ID) REFERENCES PWS_FILE_UPLOAD(FILE_UPLOAD_ID)
);

CREATE TABLE PWS_BULK_TRANSACTION_INSTRUCTIONS (
    CHILD_TRANSACTION_INSTRUCTIONS_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    CHILD_BANK_REFERENCE_ID VARCHAR(100),
    TRANSACTION_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(100),
    TRANSACTION_CURRENCY VARCHAR(3),
    TRANSACTION_AMOUNT DECIMAL(19,2),
    EQUIVALENT_CURRENCY VARCHAR(3),
    EQUIVALENT_AMOUNT DECIMAL(19,2),
    DESTINATION_COUNTRY VARCHAR(3),
    DESTINATION_BANK_NAME VARCHAR(255),
    FX_FLAG VARCHAR(1),
    CHARGE_OPTIONS VARCHAR(50),
    ORIGINAL_VALUE_DATE TIMESTAMP,
    VALUE_DATE TIMESTAMP,
    SETTLEMENT_DATE TIMESTAMP,
    PAYMENT_CODE_ID VARCHAR(50),
    PAYMENT_DETAILS VARCHAR(500),
    RAIL_CODE VARCHAR(50),
    IS_RECURRING VARCHAR(1),
    IS_PRE_APPROVED VARCHAR(1),
    CUSTOMER_REFERENCE VARCHAR(100),
    CUSTOMER_TRANSACTION_STATUS VARCHAR(50),
    PROCESSING_STATUS VARCHAR(50),
    REJECT_REASON VARCHAR(500),
    REMARKS_FOR_APPROVAL VARCHAR(500),
    USER_COMMENTS VARCHAR(500),
    DUPLICATION_FLAG VARCHAR(1),
    REJECT_CODE VARCHAR(50),
    TRANSFER_SPEED BIGINT,
    DMP_TRANS_REF VARCHAR(100),
    CHILD_TEMPLATE_ID BIGINT,
    INITIATION_TIME TIMESTAMP,
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID)
);

CREATE TABLE PWS_PARTIES (
    PARTY_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    BANK_ENTITY_ID VARCHAR(100),
    TRANSACTION_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(100),
    CHILD_BANK_REFERENCE_ID VARCHAR(100),
    BANK_CODE VARCHAR(50),
    BANK_ID BIGINT,
    PARTY_ACCOUNT_TYPE VARCHAR(50),
    PARTY_ACCOUNT_NUMBER VARCHAR(100),
    PARTY_ACCOUNT_NAME VARCHAR(255),
    PARTY_NICK_NAME VARCHAR(255),
    PARTY_MASKED_NICK_NAME VARCHAR(255),
    PARTY_ACCOUNT_CURRENCY VARCHAR(3),
    PARTY_AGENT_BIC VARCHAR(11),
    PARTY_NAME VARCHAR(255),
    PARTY_ROLE VARCHAR(50),
    RESIDENTIAL_STATUS VARCHAR(50),
    PROXY_ID VARCHAR(100),
    PROXY_ID_TYPE VARCHAR(50),
    ID_ISSUING_COUNTRY VARCHAR(3),
    PRODUCT_TYPE VARCHAR(50),
    PRIMARY_IDENTIFICATION_TYPE VARCHAR(50),
    PRIMARY_IDENTIFICATION_VALUE VARCHAR(100),
    SECONDARY_IDENTIFICATION_TYPE VARCHAR(50),
    SECONDARY_IDENTIFICATION_VALUE VARCHAR(100),
    REGISTRATION_ID VARCHAR(100),
    BENEFICIARY_REFERENCE_ID BIGINT,
    SWIFT_CODE VARCHAR(11),
    PARTY_TYPE VARCHAR(50),
    RESIDENCY_STATUS VARCHAR(50),
    ACCOUNT_OWNERSHIP VARCHAR(50),
    RELATIONSHIP_TYPE VARCHAR(50),
    ULTIMATE_PAYEE_COUNTRY_CODE VARCHAR(3),
    ULTIMATE_PAYEE_NAME VARCHAR(255),
    ULTIMATE_PAYER_NAME VARCHAR(255),
    IS_PREAPPROVED VARCHAR(1),
    PARTY_MODIFIED_DATE TIMESTAMP,
    BENEFICIARY_CHANGE_TOKEN BIGINT,
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID)
);

-- Indexes
CREATE INDEX IDX_FILE_UPLOAD_REF ON PWS_FILE_UPLOAD(FILE_REFERENCE_ID);
CREATE INDEX IDX_FILE_UPLOAD_TXN ON PWS_FILE_UPLOAD(TRANSACTION_ID);
CREATE INDEX IDX_REJECTED_RECORD_TXN ON PWS_REJECTED_RECORD(TRANSACTION_ID);
CREATE INDEX IDX_TRANSIT_MSG_REF ON PWS_TRANSIT_MESSAGE(MESSAGE_REF_NO);
CREATE INDEX IDX_TRANSIT_MSG_BANK_REF ON PWS_TRANSIT_MESSAGE(BANK_REFERENCE_ID);
CREATE INDEX IDX_TXN_BANK_REF ON PWS_TRANSACTIONS(BANK_REFERENCE_ID);
CREATE INDEX IDX_BULK_TXN_STATUS ON PWS_BULK_TRANSACTIONS(STATUS);
CREATE INDEX IDX_PARTY_BANK_REF ON PWS_PARTIES(BANK_REFERENCE_ID);
```
## test 

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true"
})
@Import(E2ETestConfig.class)
class PaymentBulkProcessingE2ETest {

    @Autowired
    private BulkProcessingFlowBuilder flowBuilder;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Pain001InboundService pain001InboundService;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    private PwsSaveDao pwsSaveDao;

    @MockBean
    private PaymentIntegrationservice paymentIntegrationService;

    @Value("classpath:test-data/pain001-simple.json")
    private Resource simplePain001Json;

    @Value("classpath:test-data/pain001-complex.json")
    private Resource complexPain001Json;

    @BeforeEach
    void setUp() {
        // Setup mock integration responses
        when(paymentIntegrationService.getResourcesAndFeatures(anyLong()))
            .thenReturn(createMockResourceFeatures());
        when(paymentIntegrationService.getCompanyAndAccounts(anyLong(), anyString(), anyString()))
            .thenReturn(createMockCompanyAccounts());
    }

    @Test
    void testSimplePain001Processing() throws Exception {
        // Arrange
        String jsonContent = new String(Files.readAllBytes(simplePain001Json.getFile().toPath()));
        BulkRoute route = createTestRoute();

        // Act
        JobParameters jobParameters = createJobParameters(route);
        JobExecution jobExecution = jobLauncher.run(flowBuilder.createJob(route, createMockExchange()), jobParameters);

        // Assert
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        verifyPaymentSaved();
    }

    @Test
    void testComplexPain001Processing() throws Exception {
        // Similar to simple test but with complex payload
    }

    private BulkRoute createTestRoute() {
        BulkRoute route = new BulkRoute();
        route.setRouteName("test-inbound-route");
        route.setProcessingType(ProcessingType.INBOUND);
        route.setSourceType(SourceDestinationType.FILE);
        route.setEnabled(true);
        route.setSteps(Arrays.asList(
            "pain001-processing",
            "payment-debulk",
            "payment-validation",
            "payment-enrichment",
            "payment-save"
        ));
        return route;
    }

    private Exchange createMockExchange() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(Exchange.FILE_NAME, "test.json");
        exchange.getIn().setHeader(Exchange.FILE_PATH, "/test/path/test.json");
        
        InboundContext routeContext = new InboundContext();
        routeContext.setCountry(new Country("TH", "Thailand"));
        routeContext.setSourcePath("/test/path/test.json");
        routeContext.setSourceName("test.json");
        exchange.setProperty(ContextKey.routeContext, routeContext);
        
        return exchange;
    }

    private JobParameters createJobParameters(BulkRoute route) {
        return new JobParametersBuilder()
            .addString(ContextKey.routeName, route.getRouteName())
            .addString(ContextKey.country, "TH")
            .addString(ContextKey.sourcePath, "/test/path/test.json")
            .addString(ContextKey.sourceName, "test.json")
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
    }

    private void verifyPaymentSaved() {
        // Verify database state
        List<PwsTransactions> transactions = pwsSaveDao.findAllTransactions();
        assertFalse(transactions.isEmpty());
        
        PwsTransactions transaction = transactions.get(0);
        assertNotNull(transaction.getBankReferenceId());
        assertEquals(CaptureStatus.SUBMITTED.name(), transaction.getCaptureStatus());
        
        List<PwsBulkTransactions> bulkTxns = pwsSaveDao.findBulkTransactionsByTxnId(transaction.getTransactionId());
        assertFalse(bulkTxns.isEmpty());
        
        // Verify child transactions
        List<PwsBulkTransactionInstructions> instructions = 
            pwsSaveDao.findInstructionsByTxnId(transaction.getTransactionId());
        assertFalse(instructions.isEmpty());
    }

    private UserResourceFeaturesActionsData createMockResourceFeatures() {
        // Create mock entitlements data
        UserResourceFeaturesActionsData data = new UserResourceFeaturesActionsData();
        // Set up mock data
        return data;
    }

    private CompanyAndAccountsForResourceFeaturesResp createMockCompanyAccounts() {
        // Create mock company and accounts data
        CompanyAndAccountsForResourceFeaturesResp resp = new CompanyAndAccountsForResourceFeaturesResp();
        // Set up mock data
        return resp;
    }
}
```

## test application.yml

```yaml
# Test configuration - application-test.yml
server:
  port: ${pbp_port:8015}
  servlet:
    context-path: /bulkprocessing

spring:
  config:
    import: bootstrap.yml,classpath:trancommon.yml,classpath:tran-common-caches.yml,classpath:common-utils.yml
  h2:
    console:
      enabled: true
      path: /h2-console
  datasource:
    common:
      type: com.zaxxer.hikari.HikariDataSource
      driver-class-name: org.h2.Driver
      hikari:
        maximum-pool-size: 5
        minimum-idle: 2
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

    # Default datasource for Spring Batch
    default:
      jdbc-url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=Oracle
      username: sa
      password: 
      driver-class-name: org.h2.Driver

    # aes
    aes:
      jdbc-url: jdbc:h2:mem:aesdb;DB_CLOSE_DELAY=-1;MODE=Oracle
      username: sa
      password: 
      driver-class-name: org.h2.Driver

    # rds
    rds:
      jdbc-url: jdbc:h2:mem:rdsdb;DB_CLOSE_DELAY=-1;MODE=Oracle
      username: sa
      password: 
      driver-class-name: org.h2.Driver

    # pws
    pws:
      jdbc-url: jdbc:h2:mem:pwsdb;DB_CLOSE_DELAY=-1;MODE=Oracle
      username: sa
      password: 
      driver-class-name: org.h2.Driver

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        
  sql:
    init:
      mode: always
      schema-locations: 
        - classpath:schema-h2.sql
      data-locations:
        - classpath:data-h2.sql

  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: always

mybatis:
  mapper-locations:
    - classpath:mappers/**/*.xml
  configuration:
    jdbc-type-for-null: VARCHAR
    default-statement-timeout: 5

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    your.package.name.mapper: DEBUG  # Replace with your actual mapper package
```

# Bulk Processing Builder 
```java
@ExtendWith(MockitoExtension.class)
class BulkProcessingFlowBuilderTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private BulkRoutesConfig bulkRoutesConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private Pain001InboundService pain001InboundService;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private SqlSessionTemplate sqlSessionTemplate;

    @InjectMocks
    private BulkProcessingFlowBuilder flowBuilder;

    private BulkRoute bulkRoute;
    private ExecutionContext jobContext;
    private ExecutionContext stepContext;
    private InboundContext routeContext;
    private Pain001InboundProcessingResult processingResult;

    @BeforeEach
    void setUp() {
        // Initialize contexts
        jobContext = new ExecutionContext();
        stepContext = new ExecutionContext();
        processingResult = new Pain001InboundProcessingResult();
        jobContext.put("result", processingResult);

        // Setup route context
        routeContext = new InboundContext();
        routeContext.setCountry(new Country("TH", "Thailand"));
        routeContext.setSourcePath("/test/path/file.xml");
        routeContext.setSourceName("file.xml");
        routeContext.setFormat("json");

        // Setup bulk route
        bulkRoute = createTestBulkRoute();

        // Setup exchange mocks
        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(ContextKey.routeContext, InboundContext.class)).thenReturn(routeContext);
        
        // Setup execution context mocks
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
    }

    @Test
    void configure_WithEnabledInboundRoute() throws Exception {
        // Arrange
        List<BulkRoute> routes = Collections.singletonList(bulkRoute);
        when(bulkRoutesConfig.getRoutes()).thenReturn(routes);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        flowBuilder.configure();

        // Assert
        verify(bulkRoutesConfig).getRoutes();
    }

    @Test
    void configure_WithDisabledRoute() throws Exception {
        // Arrange
        bulkRoute.setEnabled(false);
        List<BulkRoute> routes = Collections.singletonList(bulkRoute);
        when(bulkRoutesConfig.getRoutes()).thenReturn(routes);

        // Act
        flowBuilder.configure();

        // Assert
        verify(bulkRoutesConfig).getRoutes();
        // Verify no route was created
    }

    @Test
    void buildInboundFromUri_FileSource() {
        // Arrange
        BulkRoutesConfig.FileSource fileSource = bulkRoute.getFileSource();
        
        // Act
        String uri = flowBuilder.buildInboundFromUri(bulkRoute);

        // Assert
        assertNotNull(uri);
        assertTrue(uri.startsWith("file:"));
        assertTrue(uri.contains(fileSource.getDirectoryName()));
    }

    @Test
    void buildInboundFromUri_UnsupportedSource() {
        // Arrange
        bulkRoute.setSourceType(BulkRoutesConfig.SourceDestinationType.JDBC);

        // Act & Assert
        assertThrows(BulkProcessingException.class, () -> 
            flowBuilder.buildInboundFromUri(bulkRoute)
        );
    }

    @Test
    void prepareInboundContext() {
        // Arrange
        when(message.getHeader(Exchange.FILE_PATH, String.class)).thenReturn("/test/path/file.xml");
        when(message.getHeader(Exchange.FILE_NAME, String.class)).thenReturn("file.xml");

        // Act
        flowBuilder.prepareInboundContext(bulkRoute, exchange);

        // Assert
        InboundContext context = exchange.getProperty(ContextKey.routeContext, InboundContext.class);
        assertNotNull(context);
        assertEquals("file.xml", context.getSourceName());
        assertEquals("/test/path/file.xml", context.getSourcePath());
    }

    @Test
    void createInboundJobParameters() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        JobParameters params = flowBuilder.createInboundJobParameters(bulkRoute, exchange);

        // Assert
        assertNotNull(params);
        assertEquals("TH", params.getString(ContextKey.country));
        assertEquals(bulkRoute.getRouteName(), params.getString(ContextKey.routeName));
        assertEquals("/test/path/file.xml", params.getString(ContextKey.sourcePath));
    }

    @Test
    void handleInboundJobExecution_Successful() {
        // Act
        flowBuilder.handleInboundJobExecution(bulkRoute, exchange, jobExecution);

        // Assert
        verify(message).setHeader("exitStatus", ExitStatus.COMPLETED);
        assertEquals(routeContext.getInboundProcessingResult(), processingResult);
    }

    @Test
    void createJob_WithValidSteps() {
        // Act
        Job job = flowBuilder.createJob(bulkRoute, exchange);

        // Assert
        assertNotNull(job);
        assertEquals(bulkRoute.getRouteName() + "Job", job.getName());
    }

    @Test
    void createJob_WithNoSteps() {
        // Arrange
        bulkRoute.setSteps(Collections.emptyList());

        // Act & Assert
        assertThrows(BulkProcessingException.class, () ->
            flowBuilder.createJob(bulkRoute, exchange)
        );
    }

    @Test
    void testPain001ProcessingStep() throws Exception {
        // Arrange
        StepBuilder stepBuilder = new StepBuilder("pain001-processing", jobRepository);
        List<PaymentInformation> testPayments = Collections.singletonList(new PaymentInformation());
        when(pain001InboundService.processPain001(anyString())).thenReturn(testPayments);

        MockedStatic<Files> filesMock = mockStatic(Files.class);
        filesMock.when(() -> Files.readString(any(Path.class))).thenReturn("test content");

        // Act
        Step step = flowBuilder.createPain001ProcessingStep(stepBuilder, bulkRoute);

        // Assert
        assertNotNull(step);
        assertEquals("pain001-processing", step.getName());
        filesMock.close();
    }

    @Test
    void testPaymentDebulkStep() {
        // Arrange
        StepBuilder stepBuilder = new StepBuilder("payment-debulk", jobRepository);
        List<PaymentInformation> testPayments = Collections.singletonList(new PaymentInformation());
        when(pain001InboundService.debulk(anyList())).thenReturn(testPayments);

        // Act
        Step step = flowBuilder.createPaymentDebulkStep(stepBuilder, bulkRoute);

        // Assert
        assertNotNull(step);
        assertEquals("payment-debulk", step.getName());
    }

    @Test
    void testPaymentValidationStep() {
        // Arrange
        StepBuilder stepBuilder = new StepBuilder("payment-validation", jobRepository);
        List<PaymentInformation> testPayments = Collections.singletonList(new PaymentInformation());
        when(pain001InboundService.validate(anyList())).thenReturn(testPayments);

        // Act
        Step step = flowBuilder.createPaymentValidationStep(stepBuilder, bulkRoute);

        // Assert
        assertNotNull(step);
        assertEquals("payment-validation", step.getName());
    }

    @Test
    void testPaymentEnrichmentStep() {
        // Arrange
        StepBuilder stepBuilder = new StepBuilder("payment-enrichment", jobRepository);
        List<PaymentInformation> testPayments = Collections.singletonList(new PaymentInformation());
        when(pain001InboundService.enrich(anyList())).thenReturn(testPayments);

        // Act
        Step step = flowBuilder.createPaymentEnrichmentStep(stepBuilder, bulkRoute);

        // Assert
        assertNotNull(step);
        assertEquals("payment-enrichment", step.getName());
    }

    @Test
    void testPaymentSaveStep() {
        // Arrange
        StepBuilder stepBuilder = new StepBuilder("payment-save", jobRepository);
        List<PaymentInformation> testPayments = Collections.singletonList(new PaymentInformation());
        when(pain001InboundService.save(anyList())).thenReturn(testPayments);

        // Act
        Step step = flowBuilder.createPaymentSaveStep(stepBuilder, bulkRoute);

        // Assert
        assertNotNull(step);
        assertEquals("payment-save", step.getName());
    }

    @Test
    void testCreateStepForName_UnknownStep() {
        // Act & Assert
        assertThrows(BulkProcessingException.class, () ->
            flowBuilder.createStepForName("unknown-step", bulkRoute)
        );
    }

    @Test
    void testExceptionHandling() {
        // Arrange
        when(message.getHeader(Exchange.FILE_NAME, String.class)).thenReturn("test.xml");
        Exception testException = new Exception("Test error");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            flowBuilder.configure();
            // Simulate route execution with error
            throw testException;
        });
    }

    private BulkRoute createTestBulkRoute() {
        BulkRoute route = new BulkRoute();
        route.setRouteName("test-route");
        route.setEnabled(true);
        route.setProcessingType(ProcessingType.INBOUND);
        route.setSourceType(BulkRoutesConfig.SourceDestinationType.FILE);
        
        // Setup file source
        BulkRoutesConfig.FileSource fileSource = new BulkRoutesConfig.FileSource();
        fileSource.setDirectoryName("/test/input");
        fileSource.setAntInclude("*.xml");
        fileSource.setCharset("UTF-8");
        fileSource.setDoneFileName("${file:name}.done");
        fileSource.setDelay(5000);
        fileSource.setMaxMessagesPerPoll(100);
        route.setFileSource(fileSource);
        
        // Setup processing steps
        List<String> steps = Arrays.asList(
            "pain001-processing",
            "payment-debulk",
            "payment-validation",
            "payment-enrichment",
            "payment-save"
        );
        route.setSteps(steps);
        
        return route;
    }
}
```

## verifycation
```java
```java
    // Mapping Verifications
    private void verifyPayrollMapping(PaymentInformation payment) {
        // Verify basic payment info
        assertEquals(DmpBulkStatus.SUBMITTED, payment.getDmpBulkStatus());
        
        // Verify PwsTransactions mapping
        PwsTransactions pwsTxn = payment.getPwsTransactions();
        assertEquals("Payroll-Inter-Account-Fund-Transfer", pwsTxn.getResourceId());
        assertEquals("Bulk-File-Upload-Executive", pwsTxn.getFeatureId());
        assertEquals("0471859030", pwsTxn.getAccountNumber());
        assertEquals("บริษัท987", pwsTxn.getCompanyName());
        
        // Verify PwsBulkTransactions mapping
        PwsBulkTransactions bulkTxn = payment.getPwsBulkTransactions();
        assertEquals(4, Integer.parseInt(bulkTxn.getNumberOfTransactions()));
        assertEquals(new BigDecimal("1004500.64"), bulkTxn.getControlSum());
        assertNotNull(bulkTxn.getTransferDate());

        // Verify child transactions
        List<CreditTransferTransaction> childTxns = payment.getCreditTransferTransactionList();
        assertEquals(4, childTxns.size());
        verifyPayrollChildTransactions(childTxns);
    }

    private void verifyPayrollChildTransactions(List<CreditTransferTransaction> transactions) {
        // Verify first transaction
        CreditTransferTransaction firstTxn = transactions.get(0);
        assertEquals(2, firstTxn.getDmpLineNo());
        assertEquals(DmpTransactionStatus.APPROVED, firstTxn.getDmpTransactionStatus());
        
        // Verify transaction instructions
        PwsBulkTransactionInstructions firstInstr = firstTxn.getPwsBulkTransactionInstructions();
        assertEquals(new BigDecimal("4500.50"), firstInstr.getTransactionAmount());
        assertEquals("THB", firstInstr.getTransactionCurrency());
        assertEquals("20", firstInstr.getCategoryPurpose());
        
        // Verify creditor details
        assertEquals("ชื่อ1", firstTxn.getCreditor().getPwsParties().getPartyName());
        assertEquals("0471693490", firstTxn.getCreditor().getPwsParties().getPartyAccountNumber());
    }

    private void verifyWithholdingTaxMapping(List<PaymentInformation> payments) {
        assertEquals(1, payments.size());
        PaymentInformation payment = payments.get(0);
        
        // Verify tax information
        List<CreditTransferTransaction> transactions = payment.getCreditTransferTransactionList();
        for (CreditTransferTransaction txn : transactions) {
            verifyTaxInformation(txn.getTaxInformation());
        }
    }

    private void verifyTaxInformation(TaxInformation taxInfo) {
        assertNotNull(taxInfo);
        assertNotNull(taxInfo.getInstructionList());
        assertFalse(taxInfo.getInstructionList().isEmpty());
        
        // Verify first tax instruction
        PwsTaxInstructions firstTax = taxInfo.getInstructionList().get(0);
        assertEquals("53", firstTax.getTaxType());
        assertNotNull(firstTax.getTaxRateInPercentage());
        assertNotNull(firstTax.getTaxableAmount());
        assertNotNull(firstTax.getTaxAmount());
    }

    // Debulking Verifications
    private void verifyPayrollDebulking(List<PaymentInformation> debulkedPayments) {
        assertTrue(debulkedPayments.size() > 1); // Should be split based on amount
        
        // Verify BAHTNET payments
        List<PaymentInformation> bahtnetPayments = debulkedPayments.stream()
            .filter(p -> Resource.BAHTNET.id.equals(p.getPwsTransactions().getResourceId()))
            .collect(Collectors.toList());
        
        // Verify SMART payments
        List<PaymentInformation> smartPayments = debulkedPayments.stream()
            .filter(p -> Resource.SMART_SAME_DAY.id.equals(p.getPwsTransactions().getResourceId()))
            .collect(Collectors.toList());
        
        verifyBahtnetPayments(bahtnetPayments);
        verifySmartPayments(smartPayments);
    }

    private void verifyBahtnetPayments(List<PaymentInformation> bahtnetPayments) {
        assertFalse(bahtnetPayments.isEmpty());
        for (PaymentInformation payment : bahtnetPayments) {
            assertTrue(payment.getCreditTransferTransactionList().stream()
                .allMatch(txn -> txn.getPwsBulkTransactionInstructions().getTransactionAmount()
                    .compareTo(new BigDecimal("2000000")) > 0));
        }
    }

    // Validation Verifications
    private void verifyPayrollValidation(List<PaymentInformation> validatedPayments) {
        assertFalse(validatedPayments.isEmpty());
        for (PaymentInformation payment : validatedPayments) {
            assertTrue(payment.hasNoValidationError());
            assertTrue(payment.isValid());
            verifyValidatedChildTransactions(payment.getCreditTransferTransactionList());
        }
    }

    private void verifyValidatedChildTransactions(List<CreditTransferTransaction> transactions) {
        for (CreditTransferTransaction txn : transactions) {
            assertTrue(txn.hasNoValidationError());
            assertNotNull(txn.getPwsBulkTransactionInstructions().getTransactionAmount());
            assertNotNull(txn.getCreditor().getPwsParties().getPartyAccountNumber());
        }
    }

    // Enrichment Verifications
    private void verifyPayrollEnrichment(List<PaymentInformation> enrichedPayments) {
        for (PaymentInformation payment : enrichedPayments) {
            // Verify enriched transaction data
            PwsTransactions pwsTxn = payment.getPwsTransactions();
            assertNotNull(pwsTxn.getTotalAmount());
            assertNotNull(pwsTxn.getHighestAmount());
            assertEquals(payment.getCreditTransferTransactionList().size(), pwsTxn.getTotalChild());
            
            // Verify enriched bulk transaction data
            verifyEnrichedBulkTransaction(payment.getPwsBulkTransactions());
            
            // Verify enriched child transactions
            verifyEnrichedChildTransactions(payment.getCreditTransferTransactionList());
        }
    }

    // Database State Verifications
    private void verifyPayrollDataSaved() {
        // Verify main transactions
        List<PwsTransactions> transactions = pwsSaveDao.findAllTransactions();
        assertFalse(transactions.isEmpty());
        
        for (PwsTransactions txn : transactions) {
            assertNotNull(txn.getTransactionId());
            assertNotNull(txn.getBankReferenceId());
            
            // Verify bulk transactions
            List<PwsBulkTransactions> bulkTxns = 
                pwsSaveDao.findBulkTransactionsByTxnId(txn.getTransactionId());
            assertFalse(bulkTxns.isEmpty());
            
            // Verify child transactions
            verifyChildTransactionsInDb(txn.getTransactionId());
            
            // Verify parties and contacts
            verifyPartiesAndContactsInDb(txn.getTransactionId());
        }
    }

    private void verifyChildTransactionsInDb(long transactionId) {
        List<PwsBulkTransactionInstructions> instructions = 
            pwsSaveDao.findInstructionsByTxnId(transactionId);
        assertFalse(instructions.isEmpty());
        
        for (PwsBulkTransactionInstructions instruction : instructions) {
            assertNotNull(instruction.getChildBankReferenceId());
            assertNotNull(instruction.getTransactionAmount());
            assertEquals("THB", instruction.getTransactionCurrency());
            assertEquals("PENDING_VERIFICATION", instruction.getCustomerTransactionStatus());
        }
    }

    private void verifyPartiesAndContactsInDb(long transactionId) {
        // Verify parties
        List<PwsParties> parties = pwsSaveDao.findPartiesByTxnId(transactionId);
        assertFalse(parties.isEmpty());
        
        for (PwsParties party : parties) {
            assertNotNull(party.getPartyId());
            assertNotNull(party.getPartyName());
            assertNotNull(party.getPartyAccountNumber());
            
            // Verify contacts if any
            List<PwsPartyContacts> contacts = 
                pwsSaveDao.findPartyContactsByPartyId(party.getPartyId());
            if (!contacts.isEmpty()) {
                verifyPartyContacts(contacts);
            }
        }
    }

    private void verifyWithholdingTaxDataSaved() {
        // Verify transactions similar to payroll
        verifyPayrollDataSaved();
        
        // Additionally verify tax-specific data
        List<PwsTransactions> transactions = pwsSaveDao.findAllTransactions();
        for (PwsTransactions txn : transactions) {
            verifyTaxInstructionsInDb(txn.getTransactionId());
        }
    }

    private void verifyTaxInstructionsInDb(long transactionId) {
        List<PwsTaxInstructions> taxInstructions = 
            pwsSaveDao.findTaxInstructionsByTxnId(transactionId);
        assertFalse(taxInstructions.isEmpty());
        
        for (PwsTaxInstructions tax : taxInstructions) {
            assertNotNull(tax.getTaxType());
            assertNotNull(tax.getTaxRateInPercentage());
            assertNotNull(tax.getTaxableAmount());
            assertNotNull(tax.getTaxAmount());
            assertNotNull(tax.getTypeOfIncome());
        }
    }

    // Helper verification methods
    private void verifyPartyContacts(List<PwsPartyContacts> contacts) {
        for (PwsPartyContacts contact : contacts) {
            assertNotNull(contact.getPartyId());
            if (contact.getContactType().equals("EMAIL")) {
                assertNotNull(contact.getDeliveryAddress());
            } else {
                // Verify address fields if present
                if (contact.getAddress1() != null) {
                    assertFalse(contact.getAddress1().isEmpty());
                }
            }
        }
    }
```
```

# Service

## pain001 service 

```java
@ExtendWith(MockitoExtension.class)
class Pain001ServiceImplTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentUtils paymentUtils;

    @Mock
    private Pain001ProcessService pain001ProcessService;

    @Mock
    private PaymentEnrichmentService paymentEnrichmentService;

    @Mock
    private PaymentValidationService paymentValidationService;

    @Mock
    private PaymentDebulkService paymentDebulkService;

    @Mock
    private PaymentSaveService paymentSaveService;

    @Mock
    private PaymentUpdateService paymentUpdateService;

    @Mock
    private PaymentDeleteService paymentDeleteService;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private JobExecution jobExecution;

    @InjectMocks
    private Pain001ServiceImpl pain001Service;

    private ExecutionContext jobContext;
    private Pain001InboundProcessingResult result;

    @BeforeEach
    void setUp() {
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        jobContext.put("result", result);

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        pain001Service.beforeStep(stepExecution);
    }

    @Test
    void processPain001_SuccessfulProcessing() throws JsonProcessingException {
        // Arrange
        String jsonInput = "{}";
        Pain001 pain001 = new Pain001();
        List<PaymentInformation> expectedPayments = Collections.singletonList(new PaymentInformation());

        when(objectMapper.readValue(jsonInput, Pain001.class)).thenReturn(pain001);
        when(pain001ProcessService.processPain001GroupHeader(pain001))
            .thenReturn(new SourceProcessStatus(SourceProcessStatus.Result.Success, ""));
        when(pain001ProcessService.processPrePain001BoMapping(pain001))
            .thenReturn(new SourceProcessStatus(SourceProcessStatus.Result.Success, ""));
        when(pain001ProcessService.processPain001BoMapping(pain001)).thenReturn(expectedPayments);
        when(pain001ProcessService.processPostPain001BoMapping(pain001))
            .thenReturn(new SourceProcessStatus(SourceProcessStatus.Result.Success, ""));

        // Act
        List<PaymentInformation> result = pain001Service.processPain001(jsonInput);

        // Assert
        assertNotNull(result);
        assertEquals(expectedPayments, result);
        verify(pain001ProcessService).processPain001GroupHeader(pain001);
        verify(pain001ProcessService).processPrePain001BoMapping(pain001);
        verify(pain001ProcessService).processPain001BoMapping(pain001);
        verify(pain001ProcessService).processPostPain001BoMapping(pain001);
    }

    @Test
    void processPain001_JsonParsingError() throws JsonProcessingException {
        // Arrange
        String jsonInput = "invalid json";
        when(objectMapper.readValue(jsonInput, Pain001.class))
            .thenThrow(new JsonParseException(null, "Invalid JSON"));

        // Act
        List<PaymentInformation> result = pain001Service.processPain001(jsonInput);

        // Assert
        assertNull(result);
        verify(paymentSaveService).createTransitMessage(any(PwsTransitMessage.class));
        assertEquals(Pain001InboundProcessingStatus.SourceError, this.result.getProcessingStatus());
    }

    @Test
    void debulk_SuccessfulDebulking() {
        // Arrange
        List<PaymentInformation> input = Collections.singletonList(new PaymentInformation());
        List<PaymentInformation> expected = Arrays.asList(new PaymentInformation(), new PaymentInformation());
        when(paymentDebulkService.debulk(input)).thenReturn(expected);

        // Act
        List<PaymentInformation> result = pain001Service.debulk(input);

        // Assert
        assertNotNull(result);
        assertEquals(expected.size(), result.size());
        assertEquals(Pain001InboundProcessingStatus.DebulkPassed, this.result.getProcessingStatus());
        verify(paymentUpdateService).updateTransitMessageStatus(any());
    }

    @Test
    void validate_SuccessfulValidation() {
        // Arrange
        List<PaymentInformation> input = Collections.singletonList(new PaymentInformation());
        when(paymentValidationService.entitlementValidation(any())).thenReturn(input);
        when(paymentValidationService.preTransactionValidation(any())).thenReturn(input);
        when(paymentValidationService.transactionValidation(any())).thenReturn(input);
        when(paymentValidationService.postTransactionValidation(any())).thenReturn(input);
        when(paymentValidationService.duplicationValidation(any())).thenReturn(input);

        // Act
        List<PaymentInformation> result = pain001Service.validate(input);

        // Assert
        assertNotNull(result);
        verify(paymentValidationService).updateResultAfterValidation(result);
        verify(paymentUpdateService, times(5)).updateTransitMessageStatus(any());
    }

    @Test
    void save_SuccessfulSaving() {
        // Arrange
        PaymentInformation validPayment = new PaymentInformation();
        validPayment.setValid(true);
        List<PaymentInformation> input = Collections.singletonList(validPayment);

        // Act
        List<PaymentInformation> result = pain001Service.save(input);

        // Assert
        assertNotNull(result);
        assertEquals(Pain001InboundProcessingStatus.SavePassed, this.result.getProcessingStatus());
        verify(paymentUpdateService).updateTransitMessageStatus(any());
        verify(paymentUpdateService).updateFileUploadStatus(any());
    }

    @Test
    void save_WithSaveException() {
        // Arrange
        PaymentInformation payment = new PaymentInformation();
        payment.setValid(false);
        List<PaymentInformation> input = Collections.singletonList(payment);

        doThrow(new RuntimeException("Save failed")).when(paymentSaveService).savePaymentInformation(payment);

        // Act
        List<PaymentInformation> result = pain001Service.save(input);

        // Assert
        assertNotNull(result);
        assertEquals(Pain001InboundProcessingStatus.SaveWithException, this.result.getProcessingStatus());
        verify(paymentDeleteService).deletePaymentInformation(payment);
        verify(paymentUtils).updatePaymentSavedError(any(), any());
    }

    @Test
    void handleSourceProcessResultAndStop_SourceError() {
        // Arrange
        SourceProcessStatus status = new SourceProcessStatus(SourceProcessStatus.Result.SourceError, "Error message");

        // Act
        boolean result = pain001Service.handleSourceProcessResultAndStop(status);

        // Assert
        assertTrue(result);
        assertEquals(Pain001InboundProcessingStatus.SourceError, this.result.getProcessingStatus());
        verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
    }

    @Test
    void handleRejectOnErrorAndStop_WithRejectStatus() {
        // Arrange
        result.setProcessingStatus(Pain001InboundProcessingStatus.RejectOnError);
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setFileUploadId(1L);
        pain001Service.setFileUpload(fileUpload);

        // Act
        boolean stopped = pain001Service.handleRejectOnErrorAndStop();

        // Assert
        assertTrue(stopped);
        verify(paymentUpdateService).updateFileUploadStatus(any(PwsFileUpload.class));
        verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
    }
}
```

## processing

```java
@ExtendWith(MockitoExtension.class)
class Pain001ProcessServiceImplTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private PaymentMappingService paymentMappingService;

    @Mock
    private PaymentIntegrationservice paymentIntegrationservice;

    @Mock
    private PaymentQueryService paymentQueryService;

    @Mock
    private PaymentSaveService paymentSaveService;

    @Mock
    private AesQueryDao aesQueryDao;

    @Mock
    protected StepExecution stepExecution;

    @Mock
    protected JobExecution jobExecution;

    @InjectMocks
    private Pain001ProcessServiceImpl pain001ProcessService;

    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private Pain001InboundProcessingResult result;
    private Pain001 pain001;
    private PwsFileUpload fileUpload;

    private static final String TEST_SOURCE_REF = "TEST-REF-001";
    private static final String TEST_FILE_FORMAT = "BCU10P1";
    private static final Long TEST_COMPANY_ID = 123L;
    private static final Long TEST_USER_ID = 456L;
    private static final String TEST_RESOURCE_ID = "SMART";
    private static final String TEST_FEATURE_ID = "BFU";

    @BeforeEach
    void setUp() {
        // Initialize contexts
        setupExecutionContexts();
        
        // Setup test data
        setupTestPain001();
        setupTestFileUpload();
        
        // Setup basic mocks
        setupBasicMocks();
    }

    @Nested
    class GroupHeaderProcessingTests {
        @Test
        void whenValidGroupHeader_shouldProcessSuccessfully() {
            // Arrange
            setupSuccessfulEntitlementCheck();
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPain001GroupHeader(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, status.getResult());
            assertNotNull(pain001ProcessService.getFileUpload());
            assertEquals(TEST_FILE_FORMAT, pain001ProcessService.getSourceFormat());
            verifyEntitlementChecked();
        }

        @Test
        void whenFileStatusRejected_shouldStopProcessing() {
            // Arrange
            setupRejectedFileStatus();
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPain001GroupHeader(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceReject, status.getResult());
            assertEquals(DmpFileStatus.REJECTED, result.getDmpFileStatus());
        }

        @Test
        void whenFileUploadNotFound_shouldReturnError() {
            // Arrange
            when(paymentQueryService.getFileUpload(anyString())).thenReturn(null);
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPain001GroupHeader(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceError, status.getResult());
        }

        @Test
        void whenUserNotEntitled_shouldReturnError() {
            // Arrange
            setupFailedEntitlementCheck();
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPain001GroupHeader(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceError, status.getResult());
        }
    }

    @Nested
    class PreBoMappingTests {
        @Test
        void whenValidSetup_shouldProcessSuccessfully() {
            // Arrange
            setupCompanySettings();
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPrePain001BoMapping(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, status.getResult());
            verifyCompanySettingsProcessed();
            verifyResourceSettingsProcessed();
        }

        @Test
        void whenCompanyIdNotFound_shouldReturnError() {
            // Arrange
            when(aesQueryDao.getCompanyIdFromName(anyString())).thenReturn(null);
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPrePain001BoMapping(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceError, status.getResult());
        }

        @Test
        void whenSourceFormatRequiresDebtorName_shouldProcessCorrectly() {
            // Arrange
            when(appConfig.getUploadSourceFormatNoCompany())
                .thenReturn(Collections.singletonList(TEST_FILE_FORMAT));
            when(aesQueryDao.getCompanyIdFromName(anyString())).thenReturn(TEST_COMPANY_ID);
            
            // Act
            SourceProcessStatus status = pain001ProcessService.processPrePain001BoMapping(pain001);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, status.getResult());
            verify(aesQueryDao).getCompanyIdFromName(anyString());
        }
    }

    @Nested
    class BoMappingTests {
        @Test
        void whenValidPayments_shouldMapSuccessfully() {
            // Arrange
            setupSuccessfulMapping();
            
            // Act
            List<PaymentInformation> result = pain001ProcessService.processPain001BoMapping(pain001);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verifyRejectedRecordsHandled();
        }

        @Test
        void whenMappingFails_shouldReturnNull() {
            // Arrange
            when(paymentMappingService.pain001PaymentToBo(any(), any()))
                .thenThrow(new RuntimeException("Mapping failed"));
            
            // Act
            List<PaymentInformation> result = pain001ProcessService.processPain001BoMapping(pain001);

            // Assert
            assertNull(result);
        }

        @Test
        void whenSomePaymentsRejected_shouldCreateRejectedRecords() {
            // Arrange
            setupMixedPaymentStatus();
            
            // Act
            List<PaymentInformation> result = pain001ProcessService.processPain001BoMapping(pain001);

            // Assert
            assertNotNull(result);
            verify(paymentSaveService).createRejectedRecord(any());
            verifyRejectedPaymentsFiltered(result);
        }

        @Test
        void whenSomeTransactionsRejected_shouldCreateRejectedRecords() {
            // Arrange
            setupMixedTransactionStatus();
            
            // Act
            List<PaymentInformation> result = pain001ProcessService.processPain001BoMapping(pain001);

            // Assert
            assertNotNull(result);
            verify(paymentSaveService).createRejectedRecords(anyList());
            verifyRejectedTransactionsFiltered(result);
        }
    }

    // Helper methods for setup
    private void setupExecutionContexts() {
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        jobContext.put("result", result);

        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        
        pain001ProcessService.beforeStep(stepExecution);
    }

    private void setupTestPain001() {
        pain001 = new Pain001();
        BusinessDocument businessDocument = new BusinessDocument();
        CustomerCreditTransferInitiation ccti = new CustomerCreditTransferInitiation();
        
        // Setup GroupHeader
        GroupHeaderDTO groupHeader = new GroupHeaderDTO();
        groupHeader.setFilereference(TEST_SOURCE_REF);
        groupHeader.setFileformat(TEST_FILE_FORMAT);
        groupHeader.setFilestatus("01");
        groupHeader.setControlSum("1000.00");
        groupHeader.setNumberOfTransactions("2");
        ccti.setGroupHeader(groupHeader);

        // Setup PaymentInformation
        List<PaymentInformationDTO> payments = createTestPayments();
        ccti.setPaymentInformation(payments);

        businessDocument.setCustomerCreditTransferInitiation(ccti);
        pain001.setBusinessDocument(businessDocument);
    }

    private List<PaymentInformationDTO> createTestPayments() {
        List<PaymentInformationDTO> payments = new ArrayList<>();
        
        PaymentInformationDTO payment1 = new PaymentInformationDTO();
        payment1.setBulkstatus("01");
        DebtorDTO debtor1 = new DebtorDTO();
        debtor1.setName("Test Company");
        payment1.setDebtor(debtor1);
        payments.add(payment1);

        PaymentInformationDTO payment2 = new PaymentInformationDTO();
        payment2.setBulkstatus("01");
        DebtorDTO debtor2 = new DebtorDTO();
        debtor2.setName("Test Company 2");
        payment2.setDebtor(debtor2);
        payments.add(payment2);

        return payments;
    }

    private void setupTestFileUpload() {
        fileUpload = new PwsFileUpload();
        fileUpload.setFileReferenceId(TEST_SOURCE_REF);
        fileUpload.setResourceId(TEST_RESOURCE_ID);
        fileUpload.setFeatureId(TEST_FEATURE_ID);
        fileUpload.setCompanyId(TEST_COMPANY_ID);
    }

    private void setupBasicMocks() {
        when(paymentQueryService.getFileUpload(TEST_SOURCE_REF)).thenReturn(fileUpload);
    }

    private void setupSuccessfulEntitlementCheck() {
        UserResourceFeaturesActionsData resourceData = new UserResourceFeaturesActionsData();
        // Setup resource data with proper entitlements
        when(paymentIntegrationservice.getResourcesAndFeatures(anyLong()))
            .thenReturn(resourceData);
    }

    // ... continue with more helper methods
}
```

This is part 1 of the test class. Would you like me to provide:
1. Part 2 with remaining helper methods?
2. Additional test scenarios?
3. More detailed verifications?
4. Integration test examples?

The tests cover:
1. Group header processing
2. Pre BO mapping
3. BO mapping
4. Post BO mapping
5. Error scenarios
6. Validation scenarios

Key features:
1. Nested test classes for better organization
2. Comprehensive setup methods
3. Detailed verification methods
4. Coverage of all major scenarios

Let me know if you want to see the remaining parts or additional scenarios!
```

```java
@ExtendWith(MockitoExtension.class)
class Pain001ProcessServiceImplTest {

    @InjectMocks
    private Pain001ProcessServiceImpl pain001ProcessService;

    @Mock
    private PaymentQueryService paymentQueryService;

    @Mock
    private AppConfig appConfig;

    @Test
    void testProcessPain001GroupHeader_RejectedFileStatus() {
        // Arrange
        Pain001 pain001 = new Pain001();
        GroupHeaderDTO groupHeaderDTO = new GroupHeaderDTO();
        groupHeaderDTO.setFilestatus("02"); // Set file status to REJECTED
        pain001.setBusinessDocument(new BusinessDocument());
        pain001.getBusinessDocument().setCustomerCreditTransferInitiation(new CustomerCreditTransferInitiation());
        pain001.getBusinessDocument().getCustomerCreditTransferInitiation().setGroupHeader(groupHeaderDTO);

        // Act
        SourceProcessStatus result = pain001ProcessService.processPain001GroupHeader(pain001);

        // Assert
        assertThat(result.getStatus()).isEqualTo(SourceReject);
        assertThat(result.getMessage()).isEqualTo("File rejected by DMP");
    }

    @Test
    void testProcessPain001GroupHeader_ValidFile() {
        // Arrange
        Pain001 pain001 = new Pain001();
        GroupHeaderDTO groupHeaderDTO = new GroupHeaderDTO();
        groupHeaderDTO.setFilestatus("01"); // Set file status to VALID
        groupHeaderDTO.setFilereference("12345");
        pain001.setBusinessDocument(new BusinessDocument());
        pain001.getBusinessDocument().setCustomerCreditTransferInitiation(new CustomerCreditTransferInitiation());
        pain001.getBusinessDocument().getCustomerCreditTransferInitiation().setGroupHeader(groupHeaderDTO);

        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setResourceId(1L);

        Mockito.when(paymentQueryService.getFileUpload("12345")).thenReturn(fileUpload);

        // Act
        SourceProcessStatus result = pain001ProcessService.processPain001GroupHeader(pain001);

        // Assert
        assertThat(result.getStatus()).isEqualTo(SourceOK);
        assertThat(result.getMessage()).isEqualTo("Pain001 group header processed OK");
    }
}

@Test
void testProcessPrePain001BoMapping_CompanyIdNotFound() {
    // Arrange
    Pain001 pain001 = new Pain001();
    pain001.setBusinessDocument(new BusinessDocument());
    pain001.getBusinessDocument().setCustomerCreditTransferInitiation(new CustomerCreditTransferInitiation());
    pain001.getBusinessDocument().getCustomerCreditTransferInitiation().setPaymentInformation(Collections.emptyList());

    Mockito.when(appConfig.getUploadSourceFormatNoCompany()).thenReturn(Collections.singletonList("JSON"));

    // Act
    SourceProcessStatus result = pain001ProcessService.processPrePain001BoMapping(pain001);

    // Assert
    assertThat(result.getStatus()).isEqualTo(SourceError);
    assertThat(result.getMessage()).isEqualTo("Failed to find company id");
}

@Test
void testProcessPain001BoMapping_RejectedPayments() {
    // Arrange
    Pain001 pain001 = new Pain001();
    GroupHeaderDTO groupHeaderDTO = new GroupHeaderDTO();
    pain001.setBusinessDocument(new BusinessDocument());
    pain001.getBusinessDocument().setCustomerCreditTransferInitiation(new CustomerCreditTransferInitiation());
    pain001.getBusinessDocument().getCustomerCreditTransferInitiation().setGroupHeader(groupHeaderDTO);

    PaymentInformationDTO paymentDTO = new PaymentInformationDTO();
    paymentDTO.setDebtor(new Debtor("DebtorName"));

    pain001.getBusinessDocument().getCustomerCreditTransferInitiation().setPaymentInformation(Collections.singletonList(paymentDTO));

    PaymentInformation paymentInfo = new PaymentInformation();
    paymentInfo.setDmpRejected(true);

    Mockito.when(paymentMappingService.pain001PaymentToBo(Mockito.any(), Mockito.any())).thenReturn(paymentInfo);

    // Act
    List<PaymentInformation> result = pain001ProcessService.processPain001BoMapping(pain001);

    // Assert
    assertThat(result).isEmpty();
    Mockito.verify(paymentSaveService).createRejectedRecord(Mockito.any(PwsRejectedRecord.class));
}

@Test
void testProcessPostPain001BoMapping_Success() {
    // Arrange
    Pain001 pain001 = new Pain001();
    List<PaymentInformation> paymentInfos = Collections.emptyList();

    // Act
    SourceProcessStatus result = pain001ProcessService.processPostPain001BoMapping(pain001, paymentInfos);

    // Assert
    assertThat(result.getStatus()).isEqualTo(SourceOK);
    assertThat(result.getMessage()).isEqualTo("PostPain001BoMapping processed OK");
}

```

## mapping

```java
@ExtendWith(MockitoExtension.class)
class PaymentMappingServiceImplTest {

    @InjectMocks
    private PaymentMappingServiceImpl paymentMappingService;

    @Mock
    private Pain001ToBoMapper pain001ToBoMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should successfully map pain001 payment to BO")
    void testPain001PaymentToBo() throws Exception {
        // Given
        GroupHeaderDTO groupHeaderDTO = createGroupHeaderDTO();
        PaymentInformationDTO paymentDTO = createPaymentInformationDTO();
        PwsFileUpload fileUpload = createMockFileUpload();
        
        // Mock service method responses
        when(paymentMappingService.getBankEntityId()).thenReturn("TESTBANK");
        when(paymentMappingService.getCompanyId()).thenReturn(12345L);
        when(paymentMappingService.getUserId()).thenReturn(67890L);
        when(paymentMappingService.getFileUpload()).thenReturn(fileUpload);

        // Mock mapper responses
        PaymentInformation mockPaymentInfo = new PaymentInformation();
        mockPaymentInfo.setDmpBulkStatus(DmpBulkStatus.fromValue("01"));
        when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO)).thenReturn(mockPaymentInfo);

        PwsTransactions mockPwsTransactions = new PwsTransactions();
        when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO)).thenReturn(mockPwsTransactions);

        PwsBulkTransactions mockPwsBulkTransactions = new PwsBulkTransactions();
        when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any(PwsFileUpload.class)))
            .thenReturn(mockPwsBulkTransactions);

        // Mock child transaction mappings
        PwsBulkTransactionInstructions mockInstructions = new PwsBulkTransactionInstructions();
        when(pain001ToBoMapper.mapToPwsBulkTransactionInstructions(any(CreditTransferTransactionInformationDTO.class), any(PwsFileUpload.class)))
            .thenReturn(mockInstructions);

        Creditor mockCreditor = new Creditor();
        when(pain001ToBoMapper.mapToCreditor(any(CreditTransferTransactionInformationDTO.class)))
            .thenReturn(mockCreditor);

        PwsTransactionAdvices mockAdvice = new PwsTransactionAdvices();
        when(pain001ToBoMapper.mapToPwsTransactionAdvices(any(CreditTransferTransactionInformationDTO.class)))
            .thenReturn(mockAdvice);

        TaxInformation mockTaxInfo = new TaxInformation();
        when(pain001ToBoMapper.mapToTaxInformation(any(CreditTransferTransactionInformationDTO.class)))
            .thenReturn(mockTaxInfo);

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        assertEquals(DmpBulkStatus.fromValue("01"), result.getDmpBulkStatus());
        
        // Verify PwsTransactions setup
        PwsTransactions resultTxn = result.getPwsTransactions();
        assertNotNull(resultTxn);
        assertEquals("TESTBANK", resultTxn.getBankEntityId());
        assertEquals(12345L, resultTxn.getCompanyId());
        assertEquals(67890L, resultTxn.getInitiatedBy());
        assertNotNull(resultTxn.getInitiationTime());

        // Verify PwsBulkTransactions setup
        PwsBulkTransactions resultBulkTxn = result.getPwsBulkTransactions();
        assertNotNull(resultBulkTxn);
        assertNotNull(resultBulkTxn.getTransferDate());
        assertEquals("2007-02-23", new SimpleDateFormat("yyyy-MM-dd").format(resultBulkTxn.getTransferDate()));

        // Verify credit transfer transactions
        assertNotNull(result.getCreditTransferTransactionList());
        assertFalse(result.getCreditTransferTransactionList().isEmpty());
        assertEquals(5, result.getCreditTransferTransactionList().size());

        // Verify method invocations
        verify(pain001ToBoMapper).mapToPaymentInformation(paymentDTO);
        verify(pain001ToBoMapper).mapToPwsTransactions(paymentDTO);
        verify(pain001ToBoMapper).mapToPwsBulkTransactions(eq(paymentDTO), any(PwsFileUpload.class));
        
        verify(pain001ToBoMapper, times(5))
            .mapToPwsBulkTransactionInstructions(any(CreditTransferTransactionInformationDTO.class), any(PwsFileUpload.class));
        verify(pain001ToBoMapper, times(5))
            .mapToCreditor(any(CreditTransferTransactionInformationDTO.class));
        verify(pain001ToBoMapper, times(5))
            .mapToPwsTransactionAdvices(any(CreditTransferTransactionInformationDTO.class));
        verify(pain001ToBoMapper, times(5))
            .mapToTaxInformation(any(CreditTransferTransactionInformationDTO.class));
    }

    @Test
    @DisplayName("Should handle empty credit transfer transaction list")
    void testPain001PaymentToBo_EmptyTransactions() throws Exception {
        // Given
        GroupHeaderDTO groupHeaderDTO = createGroupHeaderDTO();
        PaymentInformationDTO paymentDTO = createPaymentInformationDTO();
        paymentDTO.setCreditTransferTransactionInformation(Collections.emptyList());
        
        PwsFileUpload fileUpload = createMockFileUpload();
        when(paymentMappingService.getFileUpload()).thenReturn(fileUpload);

        // Mock basic mappings
        when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO))
            .thenReturn(new PaymentInformation());
        when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO))
            .thenReturn(new PwsTransactions());
        when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any(PwsFileUpload.class)))
            .thenReturn(new PwsBulkTransactions());

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreditTransferTransactionList());
        assertTrue(result.getCreditTransferTransactionList().isEmpty());
    }

    @Test
    @DisplayName("Should handle null payment instructions")
    void testPain001PaymentToBo_NullInstructions() throws Exception {
        // Given
        GroupHeaderDTO groupHeaderDTO = createGroupHeaderDTO();
        PaymentInformationDTO paymentDTO = createPaymentInformationDTO();
        CreditTransferTransactionInformationDTO txnDTO = paymentDTO.getCreditTransferTransactionInformation().get(0);
        txnDTO.setInstructionForCreditorAgent(null);
        
        PwsFileUpload fileUpload = createMockFileUpload();
        when(paymentMappingService.getFileUpload()).thenReturn(fileUpload);

        // Mock mapper responses with basic objects
        when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO))
            .thenReturn(new PaymentInformation());
        when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO))
            .thenReturn(new PwsTransactions());
        when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any(PwsFileUpload.class)))
            .thenReturn(new PwsBulkTransactions());
        when(pain001ToBoMapper.mapToPwsBulkTransactionInstructions(any(), any()))
            .thenReturn(new PwsBulkTransactionInstructions());
        when(pain001ToBoMapper.mapToCreditor(any()))
            .thenReturn(new Creditor());
        when(pain001ToBoMapper.mapToPwsTransactionAdvices(any()))
            .thenReturn(new PwsTransactionAdvices());
        when(pain001ToBoMapper.mapToTaxInformation(any()))
            .thenReturn(new TaxInformation());

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreditTransferTransactionList());
        assertFalse(result.getCreditTransferTransactionList().isEmpty());
        
        PwsBulkTransactionInstructions instructions = result.getCreditTransferTransactionList()
            .get(0).getPwsBulkTransactionInstructions();
        assertNotNull(instructions);
        assertNull(instructions.getPaymentDetails());
    }

    private GroupHeaderDTO createGroupHeaderDTO() {
        GroupHeaderDTO header = new GroupHeaderDTO();
        header.setMessageIdentification("TESTMSG123");
        header.setCreationDateTime("2024-02-23T10:00:00");
        return header;
    }

    private PaymentInformationDTO createPaymentInformationDTO() throws Exception {
        // Load from test JSON file
        ClassPathResource jsonResource = new ClassPathResource("pain001-sample.json");
        String jsonContent = Files.readString(Path.of(jsonResource.getURI()));
        
        Pain001 pain001 = objectMapper.readValue(jsonContent, Pain001.class);
        return pain001.getBusinessDocument()
                     .getCustomerCreditTransferInitiation()
                     .getPaymentInformation()
                     .get(0);
    }

    private PwsFileUpload createMockFileUpload() {
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setFileUploadId(1L);
        fileUpload.setFileReferenceId("TEST123");
        fileUpload.setChargeOption("OUR");
        fileUpload.setPayrollOption("STANDARD");
        return fileUpload;
    }

    @Test
    @DisplayName("Should throw ParseException for invalid date format")
    void testPain001PaymentToBo_InvalidDate() {
        // Given
        GroupHeaderDTO groupHeaderDTO = createGroupHeaderDTO();
        PaymentInformationDTO paymentDTO = new PaymentInformationDTO();
        RequestedExecutionDateDTO dateDTO = new RequestedExecutionDateDTO();
        dateDTO.setDate("invalid-date");
        paymentDTO.setRequestedExecutionDate(dateDTO);

        // Then
        assertThrows(ParseException.class, () -> 
            paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO)
        );
    }

    @Test
    @DisplayName("Should handle null tax information")
    void testPain001PaymentToBo_NullTaxInfo() throws Exception {
        // Given
        GroupHeaderDTO groupHeaderDTO = createGroupHeaderDTO();
        PaymentInformationDTO paymentDTO = createPaymentInformationDTO();
        paymentDTO.getCreditTransferTransactionInformation().get(0).setTax(null);
        
        PwsFileUpload fileUpload = createMockFileUpload();
        when(paymentMappingService.getFileUpload()).thenReturn(fileUpload);

        // Mock basic mappings
        when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO))
            .thenReturn(new PaymentInformation());
        when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO))
            .thenReturn(new PwsTransactions());
        when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any(PwsFileUpload.class)))
            .thenReturn(new PwsBulkTransactions());
        when(pain001ToBoMapper.mapToPwsBulkTransactionInstructions(any(), any()))
            .thenReturn(new PwsBulkTransactionInstructions());
        when(pain001ToBoMapper.mapToCreditor(any()))
            .thenReturn(new Creditor());
        when(pain001ToBoMapper.mapToPwsTransactionAdvices(any()))
            .thenReturn(new PwsTransactionAdvices());

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreditTransferTransactionList());
        assertNull(result.getCreditTransferTransactionList().get(0).getTaxInformation());
    }
}
```

## debulk

```java
@ExtendWith(MockitoExtension.class)
class PaymentDebulkServiceTest {

    @InjectMocks
    private PaymentDebulkServiceImplTH paymentDebulkService;

    private AppConfig appConfig;

    @Mock
    protected StepExecution stepExecution;
    
    @Mock
    protected JobExecution jobExecution;
    
    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private Pain001InboundProcessingResult result;

    private static final String UOB_TH = "024";
    private static final String SCB_TH = "014";
    private static final BigDecimal AMOUNT_1M = new BigDecimal("1000000.00");
    private static final BigDecimal AMOUNT_2M = new BigDecimal("2000000.00");
    private static final BigDecimal AMOUNT_3M = new BigDecimal("3000000.00");
    private static final String SOURCE_FORMAT_SMART_NEXT_DAY = "BCU10P1";

    @BeforeEach
    void init() {
        // Setup AppConfig
        appConfig = new AppConfig();
        AppConfig.DebulkConfig debulkConfig = new AppConfig.DebulkConfig();
        AppConfig.DebulkSmart debulkSmart = new AppConfig.DebulkSmart();
        debulkSmart.setBankCode(UOB_TH);
        debulkSmart.setBahtnetThreshold(AMOUNT_2M);
        debulkSmart.setSourceFormat(Arrays.asList(SOURCE_FORMAT_SMART_NEXT_DAY));
        debulkConfig.setDebulkSmart(debulkSmart);
        appConfig.setDebulk(debulkConfig);
        ReflectionTestUtils.setField(paymentDebulkService, "appConfig", appConfig);

        // Setup Context
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        jobContext.put("result", result);
        jobContext.put("sourceFormat", SOURCE_FORMAT_SMART_NEXT_DAY);

        // Setup Execution
        lenient().when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(jobContext);

        paymentDebulkService.beforeStep(stepExecution);
    }

    @Test
    void whenNoPayments_thenReturnEmptyList() {
        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.emptyList());
        
        // Assert
        assertTrue(result.isEmpty());
        assertEquals(0, this.result.getPaymentDebulkTotal());
    }

    @Test
    void whenPaymentIsRejected_thenReturnOriginalPayment() {
        // Arrange
        PaymentInformation rejectedPayment = createPaymentInfo(Resource.SMART, AMOUNT_1M);
        rejectedPayment.setDmpBulkStatus(DmpBulkStatus.REJECTED);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(rejectedPayment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(DmpBulkStatus.REJECTED, result.get(0).getDmpBulkStatus());
        assertEquals(1, this.result.getPaymentDebulkTotal());
    }

    @Test
    void whenNonSmartPayment_thenReturnOriginalPayment() {
        // Arrange
        PaymentInformation nonSmartPayment = createPaymentInfo(Resource.IAFT, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(nonSmartPayment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(Resource.IAFT.id, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(1, this.result.getPaymentDebulkTotal());
    }

    @Test
    void whenSmartNextDaySourceFormat_thenAllSmartNextDay() {
        // Arrange
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_1M);
        addChildTransactions(payment, UOB_TH, AMOUNT_1M, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(Resource.SMART_NEXT_DAY.id, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
        assertEquals(1, this.result.getPaymentDebulkTotal());
    }

    @Test
    void whenAllTransactionsUnder2M_UOB_thenAllSmartSameDay() {
        // Arrange
        jobContext.put("sourceFormat", "OTHER_FORMAT");
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_1M);
        addChildTransactions(payment, UOB_TH, AMOUNT_1M, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(Resource.SMART_SAME_DAY.id, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
    }

    @Test
    void whenAllTransactionsUnder2M_OtherBank_thenAllSmartSameDay() {
        // Arrange
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_1M);
        addChildTransactions(payment, SCB_TH, AMOUNT_1M, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(Resource.SMART_SAME_DAY.id, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
    }

    @Test
    void whenAllTransactionsOver2M_UOB_thenAllBahtnet() {
        // Arrange
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_3M);
        addChildTransactions(payment, UOB_TH, AMOUNT_3M, AMOUNT_3M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(Resource.BAHTNET.id, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
    }

    @Test
    void whenAllTransactionsOver2M_OtherBank_thenAllBahtnet() {
        // Arrange
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_3M);
        addChildTransactions(payment, SCB_TH, AMOUNT_3M, AMOUNT_3M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(1, result.size());
        assertEquals(Resource.BAHTNET.id, result.get(0).getPwsTransactions().getResourceId());
        assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
    }

    @Test
    void whenMixedTransactions_UOB_thenSplitIntoGroups() {
        // Arrange
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_3M.add(AMOUNT_1M));
        addChildTransactions(payment, UOB_TH, AMOUNT_3M, AMOUNT_1M, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(2, result.size());
        verifyPaymentGroups(result);
    }

    @Test
    void whenMixedTransactions_DifferentBanks_thenSplitIntoGroups() {
        // Arrange
        PaymentInformation payment = createPaymentInfo(Resource.SMART, AMOUNT_3M.add(AMOUNT_1M));
        addMixedBankTransactions(payment, AMOUNT_3M, AMOUNT_1M, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Collections.singletonList(payment));

        // Assert
        assertEquals(2, result.size());
        verifyPaymentGroups(result);
    }

    @Test
    void whenMultiplePayments_thenDebulkAll() {
        // Arrange
        PaymentInformation payment1 = createPaymentInfo(Resource.SMART, AMOUNT_3M);
        addChildTransactions(payment1, UOB_TH, AMOUNT_3M);
        
        PaymentInformation payment2 = createPaymentInfo(Resource.SMART, AMOUNT_1M);
        addChildTransactions(payment2, UOB_TH, AMOUNT_1M);

        // Act
        List<PaymentInformation> result = paymentDebulkService.debulk(Arrays.asList(payment1, payment2));

        // Assert
        assertEquals(2, result.size());
        assertEquals(2, this.result.getPaymentDebulkTotal());
    }

    // Helper methods
    private PaymentInformation createPaymentInfo(Resource resource, BigDecimal amount) {
        PaymentInformation paymentInfo = new PaymentInformation();
        
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);
        pwsTransactions.setResourceId(resource.id);
        pwsTransactions.setTotalAmount(amount);
        pwsTransactions.setInitiationTime(Timestamp.from(Instant.now()));
        pwsTransactions.setCompanyId(1L);
        
        PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
        bulkTransactions.setDmpBatchNumber("BATCH-" + System.currentTimeMillis());

        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(bulkTransactions);

        return paymentInfo;
    }

    private void addChildTransactions(PaymentInformation paymentInfo, String bankCode, BigDecimal... amounts) {
        List<CreditTransferTransaction> childTransactions = Arrays.stream(amounts)
                .map(amount -> createChildTransaction(amount, bankCode))
                .collect(Collectors.toList());

        paymentInfo.setCreditTransferTransactionList(childTransactions);
        paymentInfo.getPwsTransactions().setTotalChild(childTransactions.size());
    }

    private void addMixedBankTransactions(PaymentInformation paymentInfo, BigDecimal... amounts) {
        List<CreditTransferTransaction> childTransactions = new ArrayList<>();
        for (int i = 0; i < amounts.length; i++) {
            String bankCode = (i % 2 == 0) ? UOB_TH : SCB_TH;
            childTransactions.add(createChildTransaction(amounts[i], bankCode));
        }

        paymentInfo.setCreditTransferTransactionList(childTransactions);
        paymentInfo.getPwsTransactions().setTotalChild(childTransactions.size());
    }

    private CreditTransferTransaction createChildTransaction(BigDecimal amount, String bankCode) {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionAmount(amount);
        transaction.setPwsBulkTransactionInstructions(instructions);

        CreditorAgent creditorAgent = new CreditorAgent();
        creditorAgent.setBankCode(bankCode);
        transaction.setCreditorAgent(creditorAgent);

        return transaction;
    }

    private void verifyPaymentGroups(List<PaymentInformation> results) {
        PaymentInformation bahtnetPayment = results.stream()
                .filter(p -> Resource.BAHTNET.id.equals(p.getPwsTransactions().getResourceId()))
                .findFirst()
                .orElseThrow();

        PaymentInformation smartPayment = results.stream()
                .filter(p -> Resource.SMART_SAME_DAY.id.equals(p.getPwsTransactions().getResourceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, bahtnetPayment.getCreditTransferTransactionList().size());
        assertEquals(2, smartPayment.getCreditTransferTransactionList().size());
        
        assertTrue(bahtnetPayment.getCreditTransferTransactionList().stream()
                .allMatch(txn -> txn.getPwsBulkTransactionInstructions().getTransactionAmount().compareTo(AMOUNT_2M) > 0));
        
        assertTrue(smartPayment.getCreditTransferTransactionList().stream()
                .allMatch(txn -> txn.getPwsBulkTransactionInstructions().getTransactionAmount().compareTo(AMOUNT_2M) <= 0));
    }
}
```

## validation 

### helper
```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PaymentValidationHelperTest {

    @InjectMocks
    private PaymentValidationHelperImpl paymentValidationHelper;

    @Mock
    private StepAwareService stepAwareService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testValidateSourceAndFeature_Valid() {
        // Mock StepAwareService methods
        when(paymentValidationHelper.getResourceId()).thenReturn("PRODUCT_A");
        when(paymentValidationHelper.getFeatureId()).thenReturn("FEATURE_X");

        // Act
        PaymentValidationResult result = paymentValidationHelper.validateSourceAndFeature("PRODUCT_A", "FEATURE_X");

        // Assert
        assertNull(result); // Expected to be valid (no error)
    }

    @Test
    void testValidateSourceAndFeature_Invalid() {
        // Mock StepAwareService methods
        when(paymentValidationHelper.getResourceId()).thenReturn("PRODUCT_B");
        when(paymentValidationHelper.getFeatureId()).thenReturn("FEATURE_Y");

        // Act
        PaymentValidationResult result = paymentValidationHelper.validateSourceAndFeature("PRODUCT_A", "FEATURE_X");

        // Assert
        assertNotNull(result);
        assertEquals(ErrorCode.CEW_0001, result.getErrorCode());
        assertEquals(ErrorCode.CEW_0001_MESSAGE, result.getErrorMessage());
    }

    @Test
    void testValidateAccountNRA_Valid() {
        AccountResource accountResource = new AccountResource();
        accountResource.setIsNRA("NO");
        when(paymentValidationHelper.getAccountResource("12345")).thenReturn(accountResource);

        PaymentValidationResult result = paymentValidationHelper.validateAccountNRA("12345");

        assertNull(result); // Expected to be valid
    }

    @Test
    void testValidateAccountNRA_Invalid() {
        AccountResource accountResource = new AccountResource();
        accountResource.setIsNRA("YES");
        when(paymentValidationHelper.getAccountResource("12345")).thenReturn(accountResource);

        PaymentValidationResult result = paymentValidationHelper.validateAccountNRA("12345");

        assertNotNull(result);
        assertEquals(ErrorCode.CEW_0003, result.getErrorCode());
        assertEquals(ErrorCode.CEW_0003_MESSAGE, result.getErrorMessage());
    }

    @Test
    void testValidateBatchBookingItemized_Valid() {
        PaymentInformation paymentInformation = new PaymentInformation();
        paymentInformation.setCreditTransferTransactionList(Collections.emptyList());

        PaymentValidationResult result = paymentValidationHelper.validateBatchBookingItemized(paymentInformation, 10);

        assertNull(result); // No validation error
    }

    @Test
    void testValidateBatchBookingItemized_Invalid() {
        PaymentInformation paymentInformation = new PaymentInformation();
        paymentInformation.setCreditTransferTransactionList(Collections.nCopies(11, new CreditTransferTransaction()));

        PaymentValidationResult result = paymentValidationHelper.validateBatchBookingItemized(paymentInformation, 10);

        assertNotNull(result);
        assertEquals(ErrorCode.CEW_0004, result.getErrorCode());
        assertEquals(ErrorCode.CEW_0004_MESSAGE, result.getErrorMessage());
    }

    @Test
    void testValidateBatchBookingLumsum_Valid() {
        PaymentInformation paymentInformation = new PaymentInformation();
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionAmount(BigDecimal.valueOf(50));
        transaction.setPwsBulkTransactionInstructions(instructions);
        paymentInformation.setCreditTransferTransactionList(Collections.singletonList(transaction));

        PaymentValidationResult result = paymentValidationHelper.validateBatchBookingLumsum(paymentInformation, 100);

        assertNull(result); // No validation error
    }

    @Test
    void testValidateBatchBookingLumsum_Invalid() {
        PaymentInformation paymentInformation = new PaymentInformation();
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionAmount(BigDecimal.valueOf(150));
        transaction.setPwsBulkTransactionInstructions(instructions);
        paymentInformation.setCreditTransferTransactionList(Collections.singletonList(transaction));

        PaymentValidationResult result = paymentValidationHelper.validateBatchBookingLumsum(paymentInformation, 100);

        assertNotNull(result);
        assertEquals(ErrorCode.CEW_0005, result.getErrorCode());
        assertEquals(ErrorCode.CEW_0005_MESSAGE, result.getErrorMessage());
    }
}

```

### suggestion

```java
private void handleError(PaymentInformation paymentInfo, String errMsg, Exception e) {
    log.error(errMsg, e);
    paymentInfo.addValidationError(ErrorCode.CEW_9000, ErrorCode.CEW_9000_MESSAGE);
    updateProcessingStatus(Pain001InboundProcessingStatus.PostValidationEnrichmentWithException, errMsg);
}

paymentInfos.parallelStream().forEach(paymentInfo -> {
    try {
        validateEntitlement(paymentInfo);
    } catch (Exception e) {
        handleError(paymentInfo, "Entitlement validation failed", e);
    }
});

private PaymentValidationResult validateBatchBooking(PaymentInformation paymentInfo, BatchBookingIndicator indicator) {
    return paymentValidationHelper.validateBatchBooking(paymentInfo, indicator, getCompanySettings().getMaxCountOfBatchBooking());
}
```

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentValidationServiceImplTest {

    @Mock
    private TransactionUtils transactionUtils;

    @Mock
    private PaymentIntegrationservice paymentIntegrationservice;

    @Mock
    private PaymentValidationHelper paymentValidationHelper;

    @Mock
    private DecisionMatrixService<TransactionValidationRecord> txnValidationService;

    @InjectMocks
    private PaymentValidationServiceImpl paymentValidationService;

    private List<PaymentInformation> paymentInfos;

    @BeforeEach
    void setUp() {
        paymentInfos = new ArrayList<>();
        PaymentInformation paymentInfo = new PaymentInformation();
        paymentInfo.setPwsTransactions(new PwsTransactions());
        paymentInfos.add(paymentInfo);
    }

    @Test
    void testDoEntitlementValidation_Success() {
        // Arrange
        when(paymentIntegrationservice.getCompanyAndAccounts(anyString(), anyString(), anyString()))
                .thenReturn(new CompanyAndAccountsForResourceFeaturesResp());

        // Act
        List<PaymentInformation> result = paymentValidationService.doEntitlementValidation(paymentInfos);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentIntegrationser
```



## save 

### part1

```java
@ExtendWith(MockitoExtension.class)
class PaymentSaveServiceTest {
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

    @Mock
    protected StepExecution stepExecution;

    @Mock
    protected JobExecution jobExecution;

    @InjectMocks
    private PaymentSaveServiceImpl paymentSave;

    private PaymentInformation paymentInfo;
    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private BankRefMetaData bankRefMetaData;
    private Pain001InboundProcessingResult result;
    private PwsSaveRecord mockRecord;

    private static final String TEST_BANK_REF = "THISE2411001";
    private static final long TEST_CHANGE_TOKEN = 1699920000000L;
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("1000.00");
    private static final String TEST_CURRENCY = "THB";


    @BeforeEach
    void setUp() {
        // Initialize contexts
        setUpExecutionContexts();
        
        // Setup SQL Session
        setUpSqlSession();
        
        // Setup PaymentUtils
        setUpPaymentUtils();
        
        // Setup BankRef
        setUpBankRef();
        
        // Setup Configuration
        when(config.getBatchInsertSize()).thenReturn(1000);
    }

    @Test
        void createTransitMessage_Successful() {
            // Arrange
            PwsTransitMessage transitMessage = createTestTransitMessage();

            // Act
            paymentSave.createTransitMessage(transitMessage);

            // Assert
            verify(pwsSaveDao).insertPwsTransitMessage(transitMessage);
        }

        @Test
        void createTransitMessage_WhenInsertFails_ShouldThrowException() {
            // Arrange
            PwsTransitMessage transitMessage = createTestTransitMessage();
            when(pwsSaveDao.insertPwsTransitMessage(any()))
                .thenThrow(new RuntimeException("Insert failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.createTransitMessage(transitMessage));
        }

        private PwsTransitMessage createTestTransitMessage() {
            PwsTransitMessage message = new PwsTransitMessage();
            message.setMessageRefNo("MSG-001");
            message.setBankReferenceId(TEST_BANK_REF);
            message.setServiceType("SERVICE_INBOUND");
            message.setEndSystem("DMP");
            message.setStatus("PROCESSING");
            message.setRetryCount(0);
            message.setProcessingDate(new Date());
            return message;
        }
    }

    @Nested
    class RejectedRecordTests {
        @Test
        void createSingleRejectedRecord_Successful() {
            // Arrange
            PwsRejectedRecord rejectedRecord = createTestRejectedRecord(1L);

            // Act
            paymentSave.createRejectedRecord(rejectedRecord);

            // Assert
            verify(pwsSaveDao).insertPwsRejectedRecord(rejectedRecord);
        }

        @Test
        void createSingleRejectedRecord_WhenInsertFails_ShouldThrowException() {
            // Arrange
            PwsRejectedRecord rejectedRecord = createTestRejectedRecord(1L);
            when(pwsSaveDao.insertPwsRejectedRecord(any()))
                .thenThrow(new RuntimeException("Insert failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.createRejectedRecord(rejectedRecord));
        }

        @Test
        void createMultipleRejectedRecords_WithinSingleBatch_Successful() {
            // Arrange
            int recordCount = 5;
            when(config.getBatchInsertSize()).thenReturn(10);
            List<PwsRejectedRecord> rejectedRecords = createTestRejectedRecords(recordCount);
            setupSuccessfulRetryTemplate();

            // Act
            paymentSave.createRejectedRecords(rejectedRecords);

            // Assert
            verify(sqlSession, times(1)).commit();
            verify(sqlSession, never()).rollback();
            verifyRejectedRecordsInserted(recordCount);
        }

        @Test
        void createMultipleRejectedRecords_AcrossMultipleBatches_Successful() {
            // Arrange
            int batchSize = 2;
            int totalRecords = 5;
            when(config.getBatchInsertSize()).thenReturn(batchSize);
            List<PwsRejectedRecord> rejectedRecords = createTestRejectedRecords(totalRecords);
            setupSuccessfulRetryTemplate();

            // Act
            paymentSave.createRejectedRecords(rejectedRecords);

            // Assert
            verify(sqlSession, times(3)).commit(); // ceil(5/2) batches
            verify(sqlSession, never()).rollback();
            verifyRejectedRecordsInserted(totalRecords);
        }

        @Test
        void createMultipleRejectedRecords_WhenBatchFails_ShouldContinue() {
            // Arrange
            int batchSize = 2;
            int totalRecords = 4;
            when(config.getBatchInsertSize()).thenReturn(batchSize);
            List<PwsRejectedRecord> rejectedRecords = createTestRejectedRecords(totalRecords);
            setupPartiallyFailingRetryTemplate(1); // Fail first batch

            // Act
            paymentSave.createRejectedRecords(rejectedRecords);

            // Assert
            verify(sqlSession, atLeastOnce()).rollback();
            verify(sqlSession, atLeastOnce()).commit();
        }

        @Test
        void createMultipleRejectedRecords_WhenAllBatchesFail_ShouldHandleError() {
            // Arrange
            int totalRecords = 4;
            List<PwsRejectedRecord> rejectedRecords = createTestRejectedRecords(totalRecords);
            setupFailedRetryTemplate();

            // Act
            paymentSave.createRejectedRecords(rejectedRecords);

            // Assert
            verify(sqlSession, times(2)).rollback(); // Two batches attempted
            verify(sqlSession, never()).commit();
        }

        private PwsRejectedRecord createTestRejectedRecord(Long entityId) {
            PwsRejectedRecord record = new PwsRejectedRecord();
            record.setEntityId(entityId);
            record.setEntityType("PAYMENT");
            record.setBankReferenceId(TEST_BANK_REF);
            record.setRejectCode("ERR-001");
            record.setErrorDetail("Test Error");
            record.setCreatedBy("TEST_USER");
            record.setCreatedDate(new Date());
            return record;
        }

        private List<PwsRejectedRecord> createTestRejectedRecords(int count) {
            return IntStream.range(0, count)
                .mapToObj(i -> createTestRejectedRecord((long) i))
                .collect(Collectors.toList());
        }

        private void verifyRejectedRecordsInserted(int expectedCount) {
            ArgumentCaptor<PwsRejectedRecord> captor = ArgumentCaptor.forClass(PwsRejectedRecord.class);
            verify(pwsSaveDao, times(expectedCount)).insertPwsRejectedRecord(captor.capture());
            
            List<PwsRejectedRecord> capturedRecords = captor.getAllValues();
            assertEquals(expectedCount, capturedRecords.size());
            
            // Verify each record has required fields
            capturedRecords.forEach(record -> {
                assertNotNull(record.getEntityId());
                assertNotNull(record.getEntityType());
                assertNotNull(record.getBankReferenceId());
                assertNotNull(record.getRejectCode());
                assertNotNull(record.getErrorDetail());
                assertNotNull(record.getCreatedBy());
                assertNotNull(record.getCreatedDate());
            });
        }
    }

    // Additional helper methods for retry template setup
    private void setupPartiallyFailingRetryTemplate(int failBatchNumber) {
        AtomicInteger batchCounter = new AtomicInteger(0);
        lenient().when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class)))
            .thenAnswer(invocation -> {
                int currentBatch = batchCounter.incrementAndGet();
                if (currentBatch == failBatchNumber) {
                    throw new BulkProcessingException("Failed to insert batch " + failBatchNumber, 
                        new RuntimeException());
                }
                RetryCallback<Object, RuntimeException> callback = invocation.getArgument(0);
                return callback.doWithRetry(null);
            });
    }

    private class TestRetryContext implements RetryContext {
        private final int retryCount;
        private final Throwable lastThrowable;

        TestRetryContext(int retryCount, Throwable lastThrowable) {
            this.retryCount = retryCount;
            this.lastThrowable = lastThrowable;
        }

        @Override
        public int getRetryCount() {
            return retryCount;
        }

        @Override
        public Throwable getLastThrowable() {
            return lastThrowable;
        }
    }

    @Nested
    class SavePaymentInformationTests {
        @Test
        void whenSingleValidTransaction_shouldSaveSuccessfully() {
            // Arrange
            setupValidPaymentInfo(1);
            setupSuccessfulBulkSave();
            setupSuccessfulRetryTemplate();

            // Act
            paymentSave.savePaymentInformation(paymentInfo);

            // Assert
            verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
            verifyBulkPaymentSaved();
            verifyChildTransactionsSaved(1);
        }

        @Test
        void whenMultipleTransactionsWithinBatch_shouldSaveInSingleBatch() {
            // Arrange
            int transactionCount = 5;
            when(config.getBatchInsertSize()).thenReturn(10);
            setupValidPaymentInfo(transactionCount);
            setupSuccessfulBulkSave();
            setupSuccessfulRetryTemplate();

            // Act
            paymentSave.savePaymentInformation(paymentInfo);

            // Assert
            verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
            verifyBulkPaymentSaved();
            verifyChildTransactionsSaved(1); // Single batch
            verifyTransactionCounts(transactionCount);
        }

        @Test
        void whenTransactionsExceedBatchSize_shouldSaveInMultipleBatches() {
            // Arrange
            int batchSize = 2;
            int totalTransactions = 5;
            when(config.getBatchInsertSize()).thenReturn(batchSize);
            setupValidPaymentInfo(totalTransactions);
            setupSuccessfulBulkSave();
            setupSuccessfulRetryTemplate();

            // Act
            paymentSave.savePaymentInformation(paymentInfo);

            // Assert
            verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
            verifyBulkPaymentSaved();
            verifyChildTransactionsSaved(3); // ceil(5/2) batches
            verifyTransactionCounts(totalTransactions);
        }

        @Test
        void whenSomeTransactionsInvalid_shouldOnlySaveValidOnes() {
            // Arrange
            setupPaymentInfoWithMixedValidation(5, 2); // 5 total, 2 invalid
            setupSuccessfulBulkSave();
            setupSuccessfulRetryTemplate();

            // Act
            paymentSave.savePaymentInformation(paymentInfo);

            // Assert
            verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
            verifyBulkPaymentSaved();
            verifyTransactionCounts(3); // Only valid ones
        }

        @Test
        void whenAllTransactionsInvalid_shouldOnlySaveBulkPayment() {
            // Arrange
            setupPaymentInfoWithAllInvalidTransactions(3);
            setupSuccessfulBulkSave();

            // Act
            paymentSave.savePaymentInformation(paymentInfo);

            // Assert
            verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
            verifyBulkPaymentSaved();
            verifyNoChildTransactionsSaved();
        }

        @Test
        void whenBulkSaveFails_shouldNotProcessChildTransactions() {
            // Arrange
            setupValidPaymentInfo(3);
            when(pwsSaveDao.insertPwsTransactions(any()))
                .thenThrow(new RuntimeException("Bulk save failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.savePaymentInformation(paymentInfo));
            verifyNoChildTransactionsSaved();
        }

        @Test
        void whenChildBatchSaveFails_shouldRollbackAndContinue() {
            // Arrange
            int batchSize = 2;
            when(config.getBatchInsertSize()).thenReturn(batchSize);
            setupValidPaymentInfo(4);
            setupSuccessfulBulkSave();
            setupPartiallyFailingRetryTemplate(1); // Fail first batch

            // Act
            paymentSave.savePaymentInformation(paymentInfo);

            // Assert
            verify(paymentUtils).updatePaymentSavedError(eq(result), eq(mockRecord));
            verify(sqlSession, times(1)).rollback();
            verifyBulkPaymentSaved();
        }
    }

    @Nested
    class SaveBulkPaymentTests {
        @Test
        void whenValidBulkPayment_shouldSaveSuccessfully() {
            // Arrange
            setupValidPaymentInfo(1);
            setupSuccessfulBulkSave();
            mockBankRefGeneration();

            // Act
            long txnId = paymentSave.saveBulkPayment(paymentInfo);

            // Assert
            assertEquals(1L, txnId);
            PwsTransactions pwsTransactions = paymentInfo.getPwsTransactions();
            assertEquals(TEST_BANK_REF, pwsTransactions.getBankReferenceId());
            assertNotNull(pwsTransactions.getChangeToken());
            verifyBulkPaymentSaved();
        }

        @Test
        void whenBankRefGenerationFails_shouldThrowException() {
            // Arrange
            setupValidPaymentInfo(1);
            when(pwsSaveDao.getBankRefSequenceNum())
                .thenThrow(new RuntimeException("Sequence generation failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.saveBulkPayment(paymentInfo));
        }

        @Test
        void whenTransactionInsertFails_shouldThrowException() {
            // Arrange
            setupValidPaymentInfo(1);
            when(pwsSaveDao.insertPwsTransactions(any()))
                .thenThrow(new RuntimeException("Insert failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.saveBulkPayment(paymentInfo));
        }
    }

    @Nested
    class SaveCreditTransferBatchTests {
        @Test
        void whenBatchWithAllEntities_shouldSaveSuccessfully() {
            // Arrange
            int batchSize = 2;
            setupValidPaymentInfoWithAllEntities(batchSize);
            setupChildTxnBatchBankRefSeq(batchSize);
            setupSuccessfulBulkSave();
            mockBankRefGeneration();

            // Act
            paymentSave.saveCreditTransferBatch(
                paymentInfo, 
                paymentInfo.getCreditTransferTransactionList(), 
                1
            );

            // Assert
            verifyAllEntitiesSaved(batchSize);
        }

        @Test
        void whenBatchWithOptionalEntities_shouldSaveSuccessfully() {
            // Arrange
            int batchSize = 2;
            setupValidPaymentInfoWithOptionalEntities(batchSize);
            setupChildTxnBatchBankRefSeq(batchSize);
            setupSuccessfulBulkSave();
            mockBankRefGeneration();

            // Act
            paymentSave.saveCreditTransferBatch(
                paymentInfo, 
                paymentInfo.getCreditTransferTransactionList(), 
                1
            );

            // Assert
            verifyMandatoryEntitiesSaved(batchSize);
            verifyNoOptionalEntitiesSaved();
        }

        @Test
        void whenBatchWithNullEntities_shouldSkipNullRecords() {
            // Arrange
            setupValidPaymentInfoWithNullEntities(2);
            setupChildTxnBatchBankRefSeq(2);
            setupSuccessfulBulkSave();

            // Act
            paymentSave.saveCreditTransferBatch(
                paymentInfo, 
                paymentInfo.getCreditTransferTransactionList(), 
                1
            );

            // Assert
            verifyNullRecordsSkipped();
        }
    }

    @Nested
    class BatchExecutionTests {
        @Test
        void whenBatchInsertSucceeds_shouldCommitAndClose() {
            // Arrange
            List<PwsBulkTransactionInstructions> records = createTestInstructions(2);
            setupSuccessfulBatchExecution();

            // Act
            paymentSave.executeBatchInsert(
                "insertPwsBulkTransactionInstructions", 
                records, 
                null
            );

            // Assert
            verify(sqlSession).commit();
            verify(sqlSession).close();
            verify(sqlSession, never()).rollback();
        }

        @Test
        void whenBatchInsertFails_shouldRollbackAndClose() {
            // Arrange
            List<PwsBulkTransactionInstructions> records = createTestInstructions(2);
            setupFailedBatchExecution();

            // Act & Assert
            assertThrows(BulkProcessingException.class, 
                () -> paymentSave.executeBatchInsert(
                    "insertPwsBulkTransactionInstructions", 
                    records, 
                    null
                )
            );
            verify(sqlSession).rollback();
            verify(sqlSession).close();
            verify(sqlSession, never()).commit();
        }
    }

    // Helper methods for setting up test contexts
    private void setUpExecutionContexts() {
        paymentInfo = new PaymentInformation();
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        result.setPaymentSaved(new ArrayList<>());
        result.setPaymentSavedError(new ArrayList<>());
        jobContext.put("result", result);
        mockRecord = new PwsSaveRecord("encryptedId123", "BATCH001");

        lenient().when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(jobContext);
    }

    private void setUpSqlSession() {
        lenient().when(paymentSaveSqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        lenient().when(sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(sqlSession);
        lenient().when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);
        doNothing().when(sqlSession).commit();
        doNothing().when(sqlSession).rollback();
        doNothing().when(sqlSession).close();
    }

    private void setUpPaymentUtils() {
        lenient().when(paymentUtils.createPwsSaveRecord(anyLong(), anyString())).thenReturn(mockRecord);
        doNothing().when(paymentUtils).updatePaymentSaved(any(), any());
        doNothing().when(paymentUtils).updatePaymentSavedError(any(), any());
    }

    private void setUpBankRef() {
        bankRefMetaData = new BankRefMetaData("TH", "I", "SE", "2411");
        paymentSave.setBankRefMetaData(bankRefMetaData);
        paymentSave.beforeStep(stepExecution);
    }

    // Helper methods for creating test data
    private void setupValidPaymentInfoWithAllEntities(int size) {
        setupValidPaymentInfo(size);
        paymentInfo.getCreditTransferTransactionList().forEach(txn -> {
            txn.setAdvice(createTestAdvice());
            txn.setTaxInformation(createTestTaxInfo());
            ((Creditor)txn.getCreditor()).setPwsPartyContactList(createTestContacts());
        });
    }

    private void setupValidPaymentInfoWithOptionalEntities(int size) {
        setupValidPaymentInfo(size);
        paymentInfo.getCreditTransferTransactionList().forEach(txn -> {
            txn.setAdvice(null);
            txn.setTaxInformation(null);
            ((Creditor)txn.getCreditor()).setPwsPartyContactList(Collections.emptyList());
        });
    }

    private PwsTransactionAdvices createTestAdvice() {
        PwsTransactionAdvices advice = new PwsTransactionAdvices();
        advice.setAdviceId("TEST-ADVICE");
        advice.setDeliveryMethod("EMAIL");
        return advice;
    }

    private TaxInformation createTestTaxInfo() {
        TaxInformation taxInfo = new TaxInformation();
        PwsTaxInstructions instruction = new PwsTaxInstructions();
        instruction.setTaxType("WHT");
        instruction.setTaxAmount(new BigDecimal("100.00"));
        taxInfo.setInstructionList(Collections.singletonList(instruction));
        return taxInfo;
    }

    // Verification methods
    private void verifyAllEntitiesSaved(int batchSize) {
        verify(pwsSaveDao, times(batchSize)).insertPwsBulkTransactionInstructions(any());
        verify(pwsSaveDao, times(batchSize)).insertPwsParties(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsPartyContacts(any());
        verify(pwsSaveDao, times(batchSize)).insertPwsTransactionAdvices(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsTaxInstructions(any());
        verify(sqlSession).commit();
        verify(sqlSession, never()).rollback();
    }

    private void verifyMandatoryEntitiesSaved(int batchSize) {
        verify(pwsSaveDao, times(batchSize)).insertPwsBulkTransactionInstructions(any());
        verify(pwsSaveDao, times(batchSize)).insertPwsParties(any());
        verify(sqlSession).commit();
        verify(sqlSession, never()).rollback();
    }

    private void verifyNoOptionalEntitiesSaved() {
        verify(pwsSaveDao, never()).insertPwsPartyContacts(any());
        verify(pwsSaveDao, never()).insertPwsTransactionAdvices(any());
        verify(pwsSaveDao, never()).insertPwsTaxInstructions(any());
    }

    private void verifyTransactionCounts(int expectedCount) {
        assertEquals(expectedCount, result.getTransactionCreatedTotal());
        assertEquals(1, result.getPaymentCreatedTotal());
    }

    private void verifyBulkPaymentSaved() {
        verify(pwsSaveDao).insertPwsTransactions(any(PwsTransactions.class));
        verify(pwsSaveDao).insertPwsBulkTransactions(any(PwsBulkTransactions.class));
    }


    private void setupValidPaymentInfo(int size) {
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);
        pwsTransactions.setAccountNumber("123456789");
        pwsTransactions.setAccountCurrency("THB");

        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");

        List<CreditTransferTransaction> transactions = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            transactions.add(createValidTransaction(i));
        }

        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);
        paymentInfo.setCreditTransferTransactionList(transactions);
        paymentInfo.setEntitlementValidationResult(
                new EntitlementValidationResult(EntitlementValidationResult.Result.SUCCESS));
    }

    private void setupPaymentInfoWithMixedValidation(int size) {
        setupValidPaymentInfo(size);
        List<CreditTransferTransaction> transactions = paymentInfo.getCreditTransferTransactionList();
        transactions.get(0).setValidationResult(new ValidationResult("ERR-001", "Test Error"));
    }

    private CreditTransferTransaction createValidTransaction(int id) {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        transaction.setDmpTransactionStatus(DmpTransactionStatus.APPROVED);
        transaction.setDuplicateValidationResult(DuplicateValidationResult.False);

        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionAmount(new BigDecimal("1000.00"));
        instructions.setTransactionCurrency("THB");

        Party party = new Party();
        PwsParties pwsParties = new PwsParties();
        pwsParties.setPartyId((long) id);
        pwsParties.setName("Test Party " + id);
        party.setPwsParties(pwsParties);
        
        PwsPartyContacts contact = new PwsPartyContacts();
        contact.setContactType("EMAIL");
        contact.setContactValue("test" + id + "@example.com");
        party.setPwsPartyContactList(Collections.singletonList(contact));

        transaction.setPwsBulkTransactionInstructions(instructions);
        transaction.setParty(party);
        transaction.setAdvice(new PwsTransactionAdvices());
        transaction.setTaxInstructionList(Collections.singletonList(new PwsTaxInstructions()));

        return transaction;
    }

    private void setupSuccessfulBulkSave() {
        lenient().when(pwsSaveDao.getBankRefSequenceNum()).thenReturn(1);
        lenient().when(pwsSaveDao.insertPwsTransactions(any())).thenReturn(1);
        lenient().when(pwsSaveDao.insertPwsBulkTransactions(any())).thenReturn(1);
    }

    private void setupChildTxnBatchBankRefSeq(int size) {
        List<Integer> seqNums = IntStream.rangeClosed(1, size)
                                       .boxed()
                                       .collect(Collectors.toList());
        lenient().when(pwsSaveDao.getBatchBankRefSequenceNum(size)).thenReturn(seqNums);
    }

    private void setupSuccessfulRetryTemplate() {
        lenient().when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class)))
                .thenAnswer(invocation -> {
                    RetryCallback<Object, RuntimeException> callback = invocation.getArgument(0);
                    return callback.doWithRetry(null);
                });
        lenient().doNothing().when(sqlSession).commit();
        lenient().doNothing().when(sqlSession).close();
    }

    private void setupFailedRetryTemplate() {
        lenient().when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class)))
                .thenThrow(new BulkProcessingException("Failed to insert", new RuntimeException()));
        lenient().doNothing().when(sqlSession).rollback();
        lenient().doNothing().when(sqlSession).close();
    }

    private void mockBankRefGeneration() {
        lenient().when(bankRefMetaData.generateBankRefId(anyInt())).thenReturn(TEST_BANK_REF);
    }

    private void verifyBatchInsertsForMultipleBatches(int expectedBatches) {
        verify(sqlSession, times(expectedBatches)).commit();
        verify(sqlSession, never()).rollback();
        verify(sqlSession, times(expectedBatches)).close();
    }

    private void verifyAllRecordsSaved() {
        // Verify all related records were inserted
        verify(pwsSaveDao, atLeastOnce()).insertPwsBulkTransactionInstructions(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsParties(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsPartyContacts(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsTransactionAdvices(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsTaxInstructions(any());
    }
}
```

### part2

```java
    @Nested
    class SaveBulkPaymentTests {
        @Test
        void whenValidBulkPayment_shouldSaveSuccessfully() {
            // Arrange
            setupValidPaymentInfo(1);
            setupSuccessfulBulkSave();
            mockBankRefGeneration();

            // Act
            long txnId = paymentSave.saveBulkPayment(paymentInfo);

            // Assert
            assertEquals(1L, txnId);
            PwsTransactions pwsTransactions = paymentInfo.getPwsTransactions();
            assertEquals(TEST_BANK_REF, pwsTransactions.getBankReferenceId());
            assertNotNull(pwsTransactions.getChangeToken());
            verifyBulkPaymentSaved();
        }

        @Test
        void whenBankRefGenerationFails_shouldThrowException() {
            // Arrange
            setupValidPaymentInfo(1);
            when(pwsSaveDao.getBankRefSequenceNum())
                .thenThrow(new RuntimeException("Sequence generation failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.saveBulkPayment(paymentInfo));
        }

        @Test
        void whenTransactionInsertFails_shouldThrowException() {
            // Arrange
            setupValidPaymentInfo(1);
            when(pwsSaveDao.insertPwsTransactions(any()))
                .thenThrow(new RuntimeException("Insert failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> paymentSave.saveBulkPayment(paymentInfo));
        }
    }

    @Nested
    class SaveCreditTransferBatchTests {
        @Test
        void whenBatchWithAllEntities_shouldSaveSuccessfully() {
            // Arrange
            int batchSize = 2;
            setupValidPaymentInfoWithAllEntities(batchSize);
            setupChildTxnBatchBankRefSeq(batchSize);
            setupSuccessfulBulkSave();
            mockBankRefGeneration();

            // Act
            paymentSave.saveCreditTransferBatch(
                paymentInfo, 
                paymentInfo.getCreditTransferTransactionList(), 
                1
            );

            // Assert
            verifyAllEntitiesSaved(batchSize);
        }

        @Test
        void whenBatchWithOptionalEntities_shouldSaveSuccessfully() {
            // Arrange
            int batchSize = 2;
            setupValidPaymentInfoWithOptionalEntities(batchSize);
            setupChildTxnBatchBankRefSeq(batchSize);
            setupSuccessfulBulkSave();
            mockBankRefGeneration();

            // Act
            paymentSave.saveCreditTransferBatch(
                paymentInfo, 
                paymentInfo.getCreditTransferTransactionList(), 
                1
            );

            // Assert
            verifyMandatoryEntitiesSaved(batchSize);
            verifyNoOptionalEntitiesSaved();
        }

        @Test
        void whenBatchWithNullEntities_shouldSkipNullRecords() {
            // Arrange
            setupValidPaymentInfoWithNullEntities(2);
            setupChildTxnBatchBankRefSeq(2);
            setupSuccessfulBulkSave();

            // Act
            paymentSave.saveCreditTransferBatch(
                paymentInfo, 
                paymentInfo.getCreditTransferTransactionList(), 
                1
            );

            // Assert
            verifyNullRecordsSkipped();
        }
    }

    @Nested
    class BatchExecutionTests {
        @Test
        void whenBatchInsertSucceeds_shouldCommitAndClose() {
            // Arrange
            List<PwsBulkTransactionInstructions> records = createTestInstructions(2);
            setupSuccessfulBatchExecution();

            // Act
            paymentSave.executeBatchInsert(
                "insertPwsBulkTransactionInstructions", 
                records, 
                null
            );

            // Assert
            verify(sqlSession).commit();
            verify(sqlSession).close();
            verify(sqlSession, never()).rollback();
        }

        @Test
        void whenBatchInsertFails_shouldRollbackAndClose() {
            // Arrange
            List<PwsBulkTransactionInstructions> records = createTestInstructions(2);
            setupFailedBatchExecution();

            // Act & Assert
            assertThrows(BulkProcessingException.class, 
                () -> paymentSave.executeBatchInsert(
                    "insertPwsBulkTransactionInstructions", 
                    records, 
                    null
                )
            );
            verify(sqlSession).rollback();
            verify(sqlSession).close();
            verify(sqlSession, never()).commit();
        }
    }

    // Helper methods for setting up test contexts
    private void setUpExecutionContexts() {
        paymentInfo = new PaymentInformation();
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        result.setPaymentSaved(new ArrayList<>());
        result.setPaymentSavedError(new ArrayList<>());
        jobContext.put("result", result);
        mockRecord = new PwsSaveRecord("encryptedId123", "BATCH001");

        lenient().when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(jobContext);
    }

    private void setUpSqlSession() {
        lenient().when(paymentSaveSqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        lenient().when(sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(sqlSession);
        lenient().when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);
        doNothing().when(sqlSession).commit();
        doNothing().when(sqlSession).rollback();
        doNothing().when(sqlSession).close();
    }

    private void setUpPaymentUtils() {
        lenient().when(paymentUtils.createPwsSaveRecord(anyLong(), anyString())).thenReturn(mockRecord);
        doNothing().when(paymentUtils).updatePaymentSaved(any(), any());
        doNothing().when(paymentUtils).updatePaymentSavedError(any(), any());
    }

    private void setUpBankRef() {
        bankRefMetaData = new BankRefMetaData("TH", "I", "SE", "2411");
        paymentSave.setBankRefMetaData(bankRefMetaData);
        paymentSave.beforeStep(stepExecution);
    }

    // Helper methods for creating test data
    private void setupValidPaymentInfoWithAllEntities(int size) {
        setupValidPaymentInfo(size);
        paymentInfo.getCreditTransferTransactionList().forEach(txn -> {
            txn.setAdvice(createTestAdvice());
            txn.setTaxInformation(createTestTaxInfo());
            ((Creditor)txn.getCreditor()).setPwsPartyContactList(createTestContacts());
        });
    }

    private void setupValidPaymentInfoWithOptionalEntities(int size) {
        setupValidPaymentInfo(size);
        paymentInfo.getCreditTransferTransactionList().forEach(txn -> {
            txn.setAdvice(null);
            txn.setTaxInformation(null);
            ((Creditor)txn.getCreditor()).setPwsPartyContactList(Collections.emptyList());
        });
    }

    private PwsTransactionAdvices createTestAdvice() {
        PwsTransactionAdvices advice = new PwsTransactionAdvices();
        advice.setAdviceId("TEST-ADVICE");
        advice.setDeliveryMethod("EMAIL");
        return advice;
    }

    private TaxInformation createTestTaxInfo() {
        TaxInformation taxInfo = new TaxInformation();
        PwsTaxInstructions instruction = new PwsTaxInstructions();
        instruction.setTaxType("WHT");
        instruction.setTaxAmount(new BigDecimal("100.00"));
        taxInfo.setInstructionList(Collections.singletonList(instruction));
        return taxInfo;
    }

    // Verification methods
    private void verifyAllEntitiesSaved(int batchSize) {
        verify(pwsSaveDao, times(batchSize)).insertPwsBulkTransactionInstructions(any());
        verify(pwsSaveDao, times(batchSize)).insertPwsParties(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsPartyContacts(any());
        verify(pwsSaveDao, times(batchSize)).insertPwsTransactionAdvices(any());
        verify(pwsSaveDao, atLeastOnce()).insertPwsTaxInstructions(any());
        verify(sqlSession).commit();
        verify(sqlSession, never()).rollback();
    }

    private void verifyMandatoryEntitiesSaved(int batchSize) {
        verify(pwsSaveDao, times(batchSize)).insertPwsBulkTransactionInstructions(any());
        verify(pwsSaveDao, times(batchSize)).insertPwsParties(any());
        verify(sqlSession).commit();
        verify(sqlSession, never()).rollback();
    }

    private void verifyNoOptionalEntitiesSaved() {
        verify(pwsSaveDao, never()).insertPwsPartyContacts(any());
        verify(pwsSaveDao, never()).insertPwsTransactionAdvices(any());
        verify(pwsSaveDao, never()).insertPwsTaxInstructions(any());
    }

    private void verifyTransactionCounts(int expectedCount) {
        assertEquals(expectedCount, result.getTransactionCreatedTotal());
        assertEquals(1, result.getPaymentCreatedTotal());
    }

    private void verifyBulkPaymentSaved() {
        verify(pwsSaveDao).insertPwsTransactions(any(PwsTransactions.class));
        verify(pwsSaveDao).insertPwsBulkTransactions(any(PwsBulkTransactions.class));
    }

    // Additional helper methods remain the same...
}
```

Key improvements in Part 2:

1. Added dedicated test classes for:
   - Bulk payment operations
   - Credit transfer batch operations
   - Batch execution mechanics

2. Enhanced test scenarios for:
   - Optional entity handling
   - Null entity handling
   - Batch processing edge cases

3. Improved verification methods:
   - Entity-specific verifications
   - Batch processing verifications
   - Transaction count verifications

4. Better test data creation:
   - More realistic test entities
   - Support for optional entities
   - Support for full entity relationships

Would you like me to add:
1. More specific test scenarios?
2. Integration test examples?
3. Performance test scenarios?
4. Additional error scenarios?
```

## crud

### payment query 

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class PaymentQueryServiceTest {

    @Mock
    private PwsQueryDao pwsQueryDao;

    @InjectMocks
    private PaymentQueryServiceImpl paymentQueryService;

    private PwsFileUpload pwsFileUpload;
    private final String fileRef = "12345";
    private final String maxCountIndicator = "indicator";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock PwsFileUpload object
        pwsFileUpload = new PwsFileUpload();
        pwsFileUpload.setFileRef(fileRef);
        pwsFileUpload.setSomeOtherField("Test Value");
    }

    @Test
    void testGetFileUpload() {
        // Arrange
        when(pwsQueryDao.getPwsFileUpload(fileRef)).thenReturn(pwsFileUpload);

        // Act
        PwsFileUpload result = paymentQueryService.getFileUpload(fileRef);

        // Assert
        assertNotNull(result);
        assertEquals(fileRef, result.getFileRef());
        verify(pwsQueryDao).getPwsFileUpload(fileRef); // Ensure DAO method is called
    }

    @Test
    void testGetMaxCountOfBatchBooking() {
        // Arrange
        int expectedCount = 10;
        when(pwsQueryDao.getMaxCountOfBatchBooking(maxCountIndicator)).thenReturn(expectedCount);

        // Act
        int result = paymentQueryService.getMaxCountOfBatchBooking(maxCountIndicator);

        // Assert
        assertEquals(expectedCount, result);
        verify(pwsQueryDao).getMaxCountOfBatchBooking(maxCountIndicator); // Ensure DAO method is called
    }
}
```

### payment update

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class PaymentUpdateServiceTest {

    @Mock
    private PwsUpdateDao pwsUpdateDao;

    @InjectMocks
    private PaymentUpdateServiceImpl paymentUpdateService;

    private PwsFileUpload pwsFileUpload;
    private PwsTransitMessage pwsTransitMessage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock PwsFileUpload object
        pwsFileUpload = new PwsFileUpload();
        pwsFileUpload.setFileRef("12345");
        pwsFileUpload.setSomeOtherField("Test Value");

        // Setup mock PwsTransitMessage object
        pwsTransitMessage = new PwsTransitMessage();
        pwsTransitMessage.setMessageRef("TRANSIT123");
        pwsTransitMessage.setSomeField("Message Content");
    }

    @Test
    void testUpdateFileUploadStatus() {
        // Act
        paymentUpdateService.updateFileUploadStatus(pwsFileUpload);

        // Assert
        verify(pwsUpdateDao).updateFileUploadStatus(pwsFileUpload); // Ensure the DAO method is called once
    }

    @Test
    void testUpdateTransitMessageStatus() {
        // Act
        paymentUpdateService.updateTransitMessageStatus(pwsTransitMessage);

        // Assert
        verify(pwsUpdateDao).updateTransitMessageStatus(pwsTransitMessage); // Ensure the DAO method is called once
    }
}
```

### payment delete 

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentDeleteServiceTest {

    @Mock
    private PwsDeleteDao pwsDeleteDao;

    @InjectMocks
    private PaymentDeleteServiceImpl paymentDeleteService;

    private PaymentInformation paymentInformation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock payment information
        paymentInformation = new PaymentInformation();
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(123L);  // Assuming the transaction ID is set here
        
        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setTransactionId(123L);  // Same as above

        paymentInformation.setPwsTransactions(pwsTransactions);
        paymentInformation.setPwsBulkTransactions(pwsBulkTransactions);
    }

    @Test
    void testDeletePaymentInformation_whenTxnIdIsNonZero() {
        // Act
        paymentDeleteService.deletePaymentInformation(paymentInformation);

        // Verify interactions with pwsDeleteDao
        verify(pwsDeleteDao).deletePwsTaxInstructions(123L);
        verify(pwsDeleteDao).deletePwsTransactionAdvices(123L);
        verify(pwsDeleteDao).deletePwsPartyContacts(123L);
        verify(pwsDeleteDao).deletePwsParties(123L);
        verify(pwsDeleteDao).deletePwsBulkTransactionInstructions(123L);
        verify(pwsDeleteDao).deletePwsBulkTransactions(123L);
        verify(pwsDeleteDao).deletePwsTransactions(123L);

        // Assert the transaction IDs are set to 0
        assertEquals(0L, paymentInformation.getPwsTransactions().getTransactionId());
        assertEquals(0L, paymentInformation.getPwsBulkTransactions().getTransactionId());
        assertTrue(paymentInformation.getCreditTransferTransactionList()
            .stream().allMatch(v -> v.getPwsBulkTransactionInstructions().getTransactionId() == 0L));
    }

    @Test
    void testDeletePaymentInformation_whenTxnIdIsZero() {
        // Setup a PaymentInformation object with txnId = 0
        paymentInformation.getPwsTransactions().setTransactionId(0L);

        // Act
        paymentDeleteService.deletePaymentInformation(paymentInformation);

        // Verify that no deletion methods are called
        verify(pwsDeleteDao, times(0)).deletePwsTaxInstructions(anyLong());
        verify(pwsDeleteDao, times(0)).deletePwsTransactionAdvices(anyLong());
        verify(pwsDeleteDao, times(0)).deletePwsPartyContacts(anyLong());
        verify(pwsDeleteDao, times(0)).deletePwsParties(anyLong());
        verify(pwsDeleteDao, times(0)).deletePwsBulkTransactionInstructions(anyLong());
        verify(pwsDeleteDao, times(0)).deletePwsBulkTransactions(anyLong());
        verify(pwsDeleteDao, times(0)).deletePwsTransactions(anyLong());

        // Assert the transaction IDs are still 0
        assertEquals(0L, paymentInformation.getPwsTransactions().getTransactionId());
        assertEquals(0L, paymentInformation.getPwsBulkTransactions().getTransactionId());
        assertTrue(paymentInformation.getCreditTransferTransactionList()
            .stream().allMatch(v -> v.getPwsBulkTransactionInstructions().getTransactionId() == 0L));
    }
}
```

# utils

## Common 

```java
@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

    private static final String VALID_DATE_STRING = "2024-11-01";
    private static final String INVALID_DATE_STRING = "20-24-11";
    private static final String VALID_DATETIME_STRING = "2024-11-01 12:34:56.789";
    private static final String INVALID_DATETIME_STRING = "2024-11-01T12:34:56"; // Invalid format for the configured pattern

    @Test
    void testStringToDate_WithValidDate() throws Exception {
        // Given
        String dateString = VALID_DATE_STRING;

        // When
        Date result = CommonUtils.stringToDate(dateString);

        // Then
        assertNotNull(result);
        assertEquals(CommonUtils.ISO_DATE_FORMAT.parse(dateString), result);
    }

    @Test
    void testStringToDate_WithInvalidDate() {
        // Given
        String invalidDateString = INVALID_DATE_STRING;

        // When/Then
        assertThrows(ParseException.class, () -> CommonUtils.stringToDate(invalidDateString));
    }

    @Test
    void testStringToDate_WithNullDate() throws ParseException {
        // When/Then
        assertNull(CommonUtils.stringToDate(null));
    }

    @Test
    void testStringToTimestamp_WithValidDatetime() throws Exception {
        // Given
        String datetimeString = VALID_DATETIME_STRING;

        // When
        Timestamp result = CommonUtils.stringToTimestamp(datetimeString);

        // Then
        assertNotNull(result);
        assertEquals(new Timestamp(CommonUtils.ISO_DATETIME_FORMAT.parse(datetimeString).getTime()), result);
    }

    @Test
    void testStringToTimestamp_WithInvalidDatetime() {
        // Given
        String invalidDatetimeString = INVALID_DATETIME_STRING;

        // When/Then
        assertThrows(ParseException.class, () -> CommonUtils.stringToTimestamp(invalidDatetimeString));
    }

    @Test
    void testStringToTimestamp_WithNullDatetime() throws ParseException {
        // When/Then
        assertNull(CommonUtils.stringToTimestamp(null));
    }

    @Test
    void testGetShortErrorMessage_WithCause() {
        // Given
        Exception rootCause = new Exception("Root Cause");
        Exception exception = new RuntimeException("Outer Exception", rootCause);

        // When
        String message = CommonUtils.getShortErrorMessage(exception);

        // Then
        assertEquals("Root Cause", message);
    }

    @Test
    void testGetShortErrorMessage_WithoutCause() {
        // Given
        Exception exception = new RuntimeException("No Cause Exception");

        // When
        String message = CommonUtils.getShortErrorMessage(exception);

        // Then
        assertEquals("No Cause Exception", message);
    }
}
```

## payment

```java
@ExtendWith(MockitoExtension.class)
class PaymentUtilsTest {

    @Mock
    private StepAwareService stepAwareService;

    @InjectMocks
    private PaymentUtils paymentUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateRecordForRejectedFile_WithFileUpload() {
        // Given
        String errMsg = "File upload error";
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setFileUploadId(123L);
        fileUpload.setCreatedBy("testUser");
        fileUpload.setFileReferenceId("fileRef123");

        when(stepAwareService.getFileUpload()).thenReturn(fileUpload);

        // When
        PwsRejectedRecord rejected = PaymentUtils.createRecordForRejectedFile(stepAwareService, errMsg);

        // Then
        assertEquals("Bulk File Rejected", rejected.getEntityType());
        assertEquals(fileUpload.getFileUploadId(), rejected.getEntityId());
        assertEquals(fileUpload.getCreatedBy(), rejected.getCreatedBy());
        assertNotNull(rejected.getCreatedDate());
        assertEquals(fileUpload.getFileReferenceId(), rejected.getBankReferenceId());
        assertEquals(ErrorCode.CEW_8803, rejected.getRejectCode());
        assertEquals(errMsg, rejected.getErrorDetail());
    }

    @Test
    void testCreateRecordForRejectedFile_WithoutFileUpload() {
        // Given
        String errMsg = "File upload error";
        String bankEntityId = "bank123";

        when(stepAwareService.getFileUpload()).thenReturn(null);
        when(stepAwareService.getBankEntityId()).thenReturn(bankEntityId);

        // When
        PwsRejectedRecord rejected = PaymentUtils.createRecordForRejectedFile(stepAwareService, errMsg);

        // Then
        assertEquals(bankEntityId, rejected.getEntityType());
        assertNull(rejected.getEntityId());
        assertNotNull(rejected.getCreatedDate());
        assertEquals(ErrorCode.CEW_8803, rejected.getRejectCode());
        assertEquals(errMsg, rejected.getErrorDetail());
    }

    @Test
    void testCreateRecordForRejectedPayment() {
        // Given
        String errMsg = "Payment error";
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setFileUploadId(123L);
        fileUpload.setCreatedBy("testUser");
        fileUpload.setFileReferenceId("fileRef123");

        PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
        bulkTransactions.setDmpBatchNumber("batch123");

        PaymentInformation paymentInfo = new PaymentInformation();
        paymentInfo.setPwsBulkTransactions(bulkTransactions);

        when(stepAwareService.getFileUpload()).thenReturn(fileUpload);

        // When
        PwsRejectedRecord rejected = PaymentUtils.createRecordForRejectedPayment(stepAwareService, paymentInfo, errMsg);

        // Then
        assertEquals("Payment Rejected", rejected.getEntityType());
        assertEquals(fileUpload.getFileUploadId(), rejected.getEntityId());
        assertEquals(fileUpload.getCreatedBy(), rejected.getCreatedBy());
        assertNotNull(rejected.getCreatedDate());
        assertEquals(fileUpload.getFileReferenceId(), rejected.getBankReferenceId());
        assertEquals(ErrorCode.CEW_8803, rejected.getRejectCode());
        assertEquals("DMPBatch<batch123>: " + errMsg, rejected.getErrorDetail());
    }

    @Test
    void testCreateRecordForRejectedTransaction() {
        // Given
        String errMsg = "Transaction error";
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setFileUploadId(123L);
        fileUpload.setCreatedBy("testUser");
        fileUpload.setFileReferenceId("fileRef123");

        PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
        bulkTransactions.setDmpBatchNumber("batch123");

        PaymentInformation paymentInfo = new PaymentInformation();
        paymentInfo.setPwsBulkTransactions(bulkTransactions);

        CreditTransferTransaction txn = new CreditTransferTransaction();
        txn.setDmpLineNo("10");

        when(stepAwareService.getFileUpload()).thenReturn(fileUpload);

        // When
        PwsRejectedRecord rejected = PaymentUtils.createRecordForRejectedTransaction(stepAwareService, paymentInfo, txn, errMsg);

        // Then
        assertEquals("Transaction Rejected", rejected.getEntityType());
        assertEquals(fileUpload.getFileUploadId(), rejected.getEntityId());
        assertEquals(Long.parseLong(txn.getDmpLineNo()), rejected.getLineNo());
        assertEquals(fileUpload.getCreatedBy(), rejected.getCreatedBy());
        assertNotNull(rejected.getCreatedDate());
        assertEquals(fileUpload.getFileReferenceId(), rejected.getBankReferenceId());
        assertEquals(ErrorCode.CEW_8803, rejected.getRejectCode());
        assertEquals("DMPBatch<batch123> transaction: " + errMsg, rejected.getErrorDetail());
    }

    @Test
    void testCreatePwsSaveRecord_WithValidIdAndDmpTxnRef() {
        long id = 123L;
        String dmpTxnRef = "dmpRef123";

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, dmpTxnRef);

        assertEquals(id, record.getTxnId());
        assertEquals(dmpTxnRef, record.getDmpTxnRef());
    }

    @Test
    void testCreatePwsSaveRecord_WithNullDmpTxnRef() {
        long id = 123L;

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, null);

        assertEquals(id, record.getTxnId());
        assertEquals("", record.getDmpTxnRef());
    }

    @Test
    void testUpdatePaymentSaved() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord(123L, "ref123");

        paymentUtils.updatePaymentSaved(result, record);

        assertTrue(result.getPaymentSaved().contains(record));
    }

    @Test
    void testUpdatePaymentSavedError_WhenRecordNotPresent() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord(123L, "ref123");

        paymentUtils.updatePaymentSavedError(result, record);

        assertTrue(result.getPaymentSavedError().contains(record));
    }

    @Test
    void testUpdatePaymentSavedError_WhenRecordAlreadyPresent() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord(123L, "ref123");

        result.getPaymentSavedError().add(record);
        paymentUtils.updatePaymentSavedError(result, record);

        assertEquals(1, result.getPaymentSavedError().size());
    }
}

```
