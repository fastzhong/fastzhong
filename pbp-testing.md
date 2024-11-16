# E2E

## config

```java
@TestConfiguration
@EnableBatchProcessing
public class E2ETestConfig {

    @Bean
    @Primary
    public DataSource h2DataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:schema-h2.sql")
            .addScript("classpath:test-data.sql")
            .build();
    }

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath:mappers/**/*.xml"));
        return sessionFactory.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public RetryTemplate retryTemplate(AppConfig appConfig) {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(appConfig.getRetry().getMaxAttempts());
        retryTemplate.setRetryPolicy(retryPolicy);
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(appConfig.getRetry().getBackoff().getInitialInterval());
        backOffPolicy.setMultiplier(appConfig.getRetry().getBackoff().getMultiplier());
        backOffPolicy.setMaxInterval(appConfig.getRetry().getBackoff().getMaxInterval());
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}
```

## sql 

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
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    username: sa
    password: 

bulk-processing:
  country: TH
  retry:
    maxAttempts: 3
    backoff:
      initialInterval: 1000
      multiplier: 2.0
      maxInterval: 5000
  batchInsertSize: 100
  uploadSourceFormatNoCompany:
    - BCU11P2
    - BCU11P4
    - BCU11P6
  debulk:
    debulkSmart:
      bankCode: 024
      bahtnetThreshold: 2000000
      sourceFormat:
        - BCU10P1
        - BCU10P2

bulk-routes:
  - route-name: CUSTOMER_SUBMITTED_TRANSFORMED
    processing-type: INBOUND
    source-type: FILE
    bank-entity: UOBT
    channel: IDB
    request-type: BulkUpload
    enabled: true
    steps:
      - pain001-processing
      - payment-debulk
      - payment-validation
      - payment-enrichment
      - payment-save
    file-source:
      directoryName: /test/inbound
      antInclude: "*_Auth.json"
      charset: utf-8
      doneFileName: "${file:name:noext}.xml.done"
      delay: 1000
      maxMessagesPerPoll: 1
    payment-save:
      datasource: datasource-payment-save
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

## save 
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
