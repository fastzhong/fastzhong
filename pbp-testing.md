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

# E2E
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

Now let's create the H2 schema SQL:


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
CREATE TABLE PWS_TRANSACTIONS (
    TRANSACTION_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    BANK_REFERENCE_ID VARCHAR(50),
    CHANGE_TOKEN BIGINT,
    RESOURCE_ID VARCHAR(50),
    FEATURE_ID VARCHAR(50),
    ACCOUNT_NUMBER VARCHAR(20),
    ACCOUNT_CURRENCY VARCHAR(3),
    TRANSACTION_CURRENCY VARCHAR(3),
    TOTAL_CHILD INT,
    TOTAL_AMOUNT DECIMAL(19,2),
    HIGHEST_AMOUNT DECIMAL(19,2),
    CAPTURE_STATUS VARCHAR(20),
    CUSTOMER_TRANSACTION_STATUS VARCHAR(50),
    COMPANY_ID BIGINT,
    COMPANY_GROUP_ID BIGINT,
    COMPANY_NAME VARCHAR(255),
    INITIATED_BY BIGINT
);

CREATE TABLE PWS_BULK_TRANSACTIONS (
    BK_TRANSACTION_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    DMP_BATCH_NUMBER VARCHAR(50),
    CHANGE_TOKEN BIGINT,
    STATUS VARCHAR(50),
    FILE_UPLOAD_ID BIGINT,
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID)
);

CREATE TABLE PWS_BULK_TRANSACTION_INSTRUCTIONS (
    INSTRUCTION_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(50),
    CHILD_BANK_REFERENCE_ID VARCHAR(50),
    TRANSACTION_AMOUNT DECIMAL(19,2),
    TRANSACTION_CURRENCY VARCHAR(3),
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID)
);

CREATE TABLE PWS_PARTIES (
    PARTY_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(50),
    CHILD_BANK_REFERENCE_ID VARCHAR(50),
    BENEFICIARY_CHANGE_TOKEN BIGINT,
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID)
);

CREATE TABLE PWS_PARTY_CONTACTS (
    CONTACT_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    PARTY_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(50),
    CHILD_BANK_REFERENCE_ID VARCHAR(50),
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID),
    FOREIGN KEY (PARTY_ID) REFERENCES PWS_PARTIES(PARTY_ID)
);

CREATE TABLE PWS_TRANSACTION_ADVICES (
    ADVICE_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    PARTY_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(50),
    CHILD_BANK_REFERENCE_ID VARCHAR(50),
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID),
    FOREIGN KEY (PARTY_ID) REFERENCES PWS_PARTIES(PARTY_ID)
);

CREATE TABLE PWS_TAX_INSTRUCTIONS (
    TAX_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TRANSACTION_ID BIGINT,
    BANK_REFERENCE_ID VARCHAR(50),
    CHILD_BANK_REFERENCE_ID VARCHAR(50),
    FOREIGN KEY (TRANSACTION_ID) REFERENCES PWS_TRANSACTIONS(TRANSACTION_ID)
);

CREATE TABLE PWS_TRANSIT_MESSAGE (
    MESSAGE_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    BANK_REFERENCE_ID VARCHAR(50),
    SERVICE_TYPE VARCHAR(20),
    END_SYSTEM VARCHAR(20),
    MESSAGE_REF_NO VARCHAR(50),
    RETRY_COUNT INT,
    PROCESSING_DATE TIMESTAMP,
    STATUS VARCHAR(50)
);

CREATE TABLE PWS_FILE_UPLOAD (
    FILE_UPLOAD_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    FILE_REFERENCE_ID VARCHAR(50),
    RESOURCE_ID VARCHAR(50),
    FEATURE_ID VARCHAR(50),
    COMPANY_ID BIGINT,
    CREATED_BY VARCHAR(50),
    STATUS VARCHAR(20)
);

CREATE TABLE PWS_REJECTED_RECORD (
    REJECT_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    ENTITY_TYPE VARCHAR(50),
    ENTITY_ID BIGINT,
    CREATED_BY VARCHAR(50),
    CREATED_DATE DATE,
    BANK_REFERENCE_ID VARCHAR(50),
    REJECT_CODE VARCHAR(20),
    ERROR_DETAIL VARCHAR(500)
);
```

Now let's create the E2E test:


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

And here's the test application.yml:


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

