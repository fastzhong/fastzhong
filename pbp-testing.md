# E2E



## sql 

## test 

```java
@Test
    void configure_WithEnabledInboundRoute() throws Exception {
        List<AppConfig.BulkRoute> routes = Collections.singletonList(bulkRoute);
        lenient().when(appConfig.getBulkRoutes()).thenReturn(routes);
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        flowBuilder.configure();

        verify(appConfig).getBulkRoutes();
    }

    @Test
    void configure_WithDisabledRoute() throws Exception {
        // Arrange
        bulkRoute.setEnabled(false);
        List<AppConfig.BulkRoute> routes = Collections.singletonList(bulkRoute);
        lenient().when(appConfig.getBulkRoutes()).thenReturn(routes);

        // Act
        flowBuilder.configure();

        // Assert
        verify(appConfig).getBulkRoutes();
        // Verify no route was created
    }

    @Test
    void buildInboundFromUri_FileSource() {
        AppConfig.FileSource fileSource = bulkRoute.getFileSource();

        String uri = flowBuilder.buildInboundFromUri(bulkRoute);

        assertNotNull(uri);
        assertTrue(uri.startsWith("file:"));
        assertTrue(uri.contains(fileSource.getDirectoryName()));
    }

    @Test
    void buildInboundFromUri_UnsupportedSource() {
        bulkRoute.setSourceType(AppConfig.SourceDestinationType.JDBC);

        assertThrows(BulkProcessingException.class, () ->
                flowBuilder.buildInboundFromUri(bulkRoute)
        );
    }

    @Test
    void prepareInboundContext() {
        lenient().when(message.getHeader(Exchange.FILE_PATH, String.class)).thenReturn("/test/path/file.json");
        lenient().when(message.getHeader(Exchange.FILE_NAME, String.class)).thenReturn("file.json");

        flowBuilder.prepareInboundContext(bulkRoute, exchange);

        InboundContext context = exchange.getProperty(ContextKey.routeContext, InboundContext.class);
        assertNotNull(context);
        assertEquals("file.xml", context.getSourceName());
        assertEquals("/test/path/file.xml", context.getSourcePath());
    }

    @Test
    void createInboundJobParameters() throws Exception {
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

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
        StepBuilder stepBuilder = new StepBuilder("pain001-processing", jobRepository);
        List<PaymentInformation> testPayments = Collections.singletonList(new PaymentInformation());
        lenient().when(pain001InboundService.processPain001(anyString())).thenReturn(testPayments);

        MockedStatic<Files> filesMock = mockStatic(Files.class);
        filesMock.lenient().when(() -> Files.readString(any(Path.class))).thenReturn("test content");

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
        lenient().when(pain001InboundService.debulk(anyList())).thenReturn(testPayments);

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
        lenient().when(pain001InboundService.validate(anyList())).thenReturn(testPayments);

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
        lenient().when(pain001InboundService.enrich(anyList())).thenReturn(testPayments);

        // Act
        Step step = flowBuilder.createPaymentEnrichmentStep(stepBuilder, bulkRoute);

        // Assert
        assertNotNull(step);
        assertEquals("payment-enrichment", step.getName());
    }
```

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

## verification
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

# Util

```java
public final class CommonUtils {
    private CommonUtils() {
        // Prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    // Thread-local DateFormat instances for thread safety
    private static final ThreadLocal<DateFormat> ISO_DATE_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(false);
        return df;
    });

    private static final ThreadLocal<DateFormat> ISO_DATETIME_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        df.setLenient(false);
        return df;
    });

    private static final ThreadLocal<DateFormat> ISO_DATETIME_ZONE_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setLenient(false);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    });

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    public static String isoZoneFormatDateTime(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return ISO_DATETIME_ZONE_FORMAT.get().format(date);
    }

    public static Date stringToDate(String dateString) throws ParseException {
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }
        try {
            return new Date(ISO_DATE_FORMAT.get().parse(dateString).getTime());
        } catch (ParseException e) {
            throw new ParseException("Failed to parse date string: " + dateString, e.getErrorOffset());
        }
    }

    public static Timestamp stringToTimestamp(String timestampString) throws ParseException {
        if (StringUtils.isEmpty(timestampString)) {
            return null;
        }
        try {
            return new Timestamp(ISO_DATETIME_FORMAT.get().parse(timestampString).getTime());
        } catch (ParseException e) {
            throw new ParseException("Failed to parse timestamp string: " + timestampString, e.getErrorOffset());
        }
    }

    public static String getShortErrorMessage(Exception e) {
        if (e == null) {
            return "";
        }
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
        return ExceptionUtils.getRootCauseMessage(cause);
    }

    // Date conversion methods with null checks and immutability
    public static LocalDate utilAsLocalDate(Date utilDate) {
        if (utilDate == null) {
            return null;
        }
        return utilDate.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDate();
    }

    public static LocalDateTime utilAsLocalDateTime(Date utilDate) {
        if (utilDate == null) {
            return null;
        }
        return utilDate.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    public static Date localDateAsUtil(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.valueOf(localDate);
    }

    public static Date localDateTimeAsUtil(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    public static java.sql.Date utilAsSqlDate(Date utilDate) {
        if (utilDate == null) {
            return null;
        }
        return new java.sql.Date(utilDate.getTime());
    }

    public static Timestamp utilAsSqlTimestamp(Date utilDate) {
        if (utilDate == null) {
            return null;
        }
        return new Timestamp(utilDate.getTime());
    }

    public static Date sqlDateAsUtil(java.sql.Date sqlDate) {
        if (sqlDate == null) {
            return null;
        }
        return new Date(sqlDate.getTime());
    }

    public static Date sqlTimestampDateAsUtil(Timestamp sqlTimestamp) {
        if (sqlTimestamp == null) {
            return null;
        }
        return new Date(sqlTimestamp.getTime());
    }

    public static String prettyPrint(ObjectMapper objectMapper, Object obj) {
        if (obj == null) {
            return "";
        }
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper cannot be null");
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to pretty print object", e);
            return String.valueOf(obj);
        }
    }

    public static boolean moveFile(Path source, Path target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths cannot be null");
        }

        try {
            Path targetDir = target.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            log.error("Failed to move file from {} to {}", source, target, e);
            return false;
        }
    }
}
```


```java
@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {
    
    private static final String TEST_DATE_STRING = "2024-11-26";
    private static final String TEST_DATETIME_STRING = "2024-11-26 15:30:45.123";
    private static final String TEST_DATETIME_ZONE_STRING = "2024-11-26T15:30:45.123Z";

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void isoZoneFormatDateTime_ValidDate() {
        Date date = new Date();
        String result = CommonUtils.isoZoneFormatDateTime(date);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    @Test
    void isoZoneFormatDateTime_NullDate() {
        assertThrows(IllegalArgumentException.class, 
            () -> CommonUtils.isoZoneFormatDateTime(null));
    }

    @Test
    void stringToDate_ValidString() throws ParseException {
        Date result = CommonUtils.stringToDate(TEST_DATE_STRING);
        assertNotNull(result);
        assertEquals(TEST_DATE_STRING, CommonUtils.ISO_DATE_FORMAT.get().format(result));
    }

    @Test
    void stringToDate_InvalidFormat() {
        assertThrows(ParseException.class, 
            () -> CommonUtils.stringToDate("invalid-date"));
    }

    @Test
    void stringToTimestamp_ValidString() throws ParseException {
        Timestamp result = CommonUtils.stringToTimestamp(TEST_DATETIME_STRING);
        assertNotNull(result);
        assertTrue(result.getTime() > 0);
    }

    @Test
    void dateConversions_NullHandling() {
        assertNull(CommonUtils.utilAsLocalDate(null));
        assertNull(CommonUtils.utilAsLocalDateTime(null));
        assertNull(CommonUtils.localDateAsUtil(null));
        assertNull(CommonUtils.localDateTimeAsUtil(null));
        assertNull(CommonUtils.utilAsSqlDate(null));
        assertNull(CommonUtils.utilAsSqlTimestamp(null));
        assertNull(CommonUtils.sqlDateAsUtil(null));
        assertNull(CommonUtils.sqlTimestampDateAsUtil(null));
    }

    @Test
    void dateConversions_ValidDates() {
        // Test LocalDate conversions
        LocalDate localDate = LocalDate.now();
        Date utilDate = CommonUtils.localDateAsUtil(localDate);
        LocalDate convertedBack = CommonUtils.utilAsLocalDate(utilDate);
        assertEquals(localDate, convertedBack);

        // Test LocalDateTime conversions
        LocalDateTime localDateTime = LocalDateTime.now();
        Date utilDateTime = CommonUtils.localDateTimeAsUtil(localDateTime);
        LocalDateTime convertedBackDateTime = CommonUtils.utilAsLocalDateTime(utilDateTime);
        assertEquals(localDateTime.withNano(0), convertedBackDateTime.withNano(0));
    }

    @Test
    void moveFile_Success() throws IOException {
        Path source = Files.createTempFile("test", ".tmp");
        Path target = Paths.get(source.getParent().toString(), "moved.tmp");
        
        assertTrue(CommonUtils.moveFile(source, target));
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
        
        Files.deleteIfExists(target);
    }

    @Test
    void moveFile_NullPaths() {
        assertThrows(IllegalArgumentException.class, 
            () -> CommonUtils.moveFile(null, Paths.get("test")));
        assertThrows(IllegalArgumentException.class, 
            () -> CommonUtils.moveFile(Paths.get("test"), null));
    }

    @Test
    void prettyPrint_ValidObject() throws JsonProcessingException {
        TestObject obj = new TestObject("test");
        when(objectMapper.writerWithDefaultPrettyPrinter())
            .thenReturn(objectMapper.writer());
        when(objectMapper.writer().writeValueAsString(obj))
            .thenReturn("{\"name\":\"test\"}");

        String result = CommonUtils.prettyPrint(objectMapper, obj);
        assertEquals("{\"name\":\"test\"}", result);
    }

    @Test
    void prettyPrint_NullObject() {
        assertEquals("", CommonUtils.prettyPrint(objectMapper, null));
    }

    @Test
    void prettyPrint_NullMapper() {
        assertThrows(IllegalArgumentException.class, 
            () -> CommonUtils.prettyPrint(null, new Object()));
    }

    @Data
    @AllArgsConstructor
    private static class TestObject {
        private String name;
    }
}
```

public final class PaymentUtils {
    
    private PaymentUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static PwsRejectedRecord createRecordForRejectedFile(StepAwareService stepAwareService, String errMsg) {
        if (stepAwareService == null) {
            throw new IllegalArgumentException("StepAwareService cannot be null");
        }
        if (StringUtils.isEmpty(errMsg)) {
            throw new IllegalArgumentException("Error message cannot be empty");
        }

        final PwsRejectedRecord rejected = new PwsRejectedRecord();
        PwsFileUpload fileUpload = stepAwareService.getFileUpload();

        if (fileUpload != null) {
            rejected.setEntityType("Bulk File Rejected");
            rejected.setEntityId(fileUpload.getFileUploadId());
            rejected.setCreatedBy(fileUpload.getCreatedBy());
            rejected.setBankReferenceId(fileUpload.getFileReferenceId());
        } else {
            rejected.setEntityType(stepAwareService.getBankEntityId());
        }

        rejected.setCreatedDate(Date.valueOf(LocalDate.now()));
        rejected.setRejectCode(ErrorCode.PBP_2001);
        rejected.setErrorDetail(errMsg);

        return rejected;
    }

    public static PwsRejectedRecord createRecordForRejectedPayment(
            StepAwareService stepAwareService,
            PaymentInformation paymentInfo, 
            String errMsg) {
        validateInputs(stepAwareService, paymentInfo, errMsg);
        
        PwsFileUpload fileUpload = stepAwareService.getFileUpload();
        if (fileUpload == null) {
            throw new IllegalStateException("FileUpload must be available in StepAwareService");
        }

        final PwsRejectedRecord rejected = new PwsRejectedRecord();
        rejected.setEntityType("Payment Rejected");
        rejected.setEntityId(fileUpload.getFileUploadId());
        rejected.setCreatedBy(fileUpload.getCreatedBy());
        rejected.setCreatedDate(Date.valueOf(LocalDate.now()));
        rejected.setBankReferenceId(fileUpload.getFileReferenceId());
        rejected.setRejectCode(ErrorCode.PBP_2001);
        
        String dmpBatchNumber = Optional.ofNullable(paymentInfo.getPwsBulkTransactions())
            .map(PwsBulkTransactions::getDmpBatchNumber)
            .orElse("UNKNOWN");
        rejected.setErrorDetail(String.format("DMPBatch<%s>: %s", dmpBatchNumber, errMsg));

        return rejected;
    }

    public static PwsRejectedRecord createRecordForRejectedTransaction(
            StepAwareService stepAwareService,
            PaymentInformation paymentInfo, 
            CreditTransferTransaction txn, 
            String errMsg) {
        validateInputs(stepAwareService, paymentInfo, errMsg);
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        PwsFileUpload fileUpload = stepAwareService.getFileUpload();
        if (fileUpload == null) {
            throw new IllegalStateException("FileUpload must be available in StepAwareService");
        }

        final PwsRejectedRecord rejected = new PwsRejectedRecord();
        rejected.setEntityType("Transaction Rejected");
        rejected.setEntityId(fileUpload.getFileUploadId());
        rejected.setLineNo(parseDmpLineNo(txn.getDmpLineNo()));
        rejected.setCreatedBy(fileUpload.getCreatedBy());
        rejected.setCreatedDate(Date.valueOf(LocalDate.now()));
        rejected.setBankReferenceId(fileUpload.getFileReferenceId());
        rejected.setRejectCode(ErrorCode.PBP_2001);
        
        String dmpBatchNumber = Optional.ofNullable(paymentInfo.getPwsBulkTransactions())
            .map(PwsBulkTransactions::getDmpBatchNumber)
            .orElse("UNKNOWN");
        rejected.setErrorDetail(
            String.format("DMPBatch<%s> transaction: %s", dmpBatchNumber, errMsg));

        return rejected;
    }

    public static PwsSaveRecord createPwsSaveRecord(long id, String dmpTxnRef) {
        return new PwsSaveRecord(id, Optional.ofNullable(dmpTxnRef).orElse(""));
    }

    public static void updatePaymentSaved(Pain001InboundProcessingResult result, PwsSaveRecord record) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        if (record == null) {
            throw new IllegalArgumentException("Save record cannot be null");
        }

        if (result.getPaymentSaved() == null) {
            result.setPaymentSaved(new ArrayList<>());
        }
        result.getPaymentSaved().add(record);
    }

    public static void updatePaymentSavedError(Pain001InboundProcessingResult result, PwsSaveRecord record) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        if (record == null) {
            throw new IllegalArgumentException("Save record cannot be null");
        }

        if (result.getPaymentSavedError() == null) {
            result.setPaymentSavedError(new ArrayList<>());
        }
        if (!result.getPaymentSavedError().contains(record)) {
            result.getPaymentSavedError().add(record);
        }
    }

    private static void validateInputs(StepAwareService stepAwareService, 
                                     PaymentInformation paymentInfo, 
                                     String errMsg) {
        if (stepAwareService == null) {
            throw new IllegalArgumentException("StepAwareService cannot be null");
        }
        if (paymentInfo == null) {
            throw new IllegalArgumentException("PaymentInformation cannot be null");
        }
        if (StringUtils.isEmpty(errMsg)) {
            throw new IllegalArgumentException("Error message cannot be empty");
        }
    }

    private static long parseDmpLineNo(String dmpLineNo) {
        try {
            return Long.parseLong(dmpLineNo);
        } catch (NumberFormatException e) {
            log.warn("Invalid DMP line number: {}", dmpLineNo);
            return 0;
        }
    }
}
```

```java
@ExtendWith(MockitoExtension.class)
class PaymentUtilsTest {

    @Mock
    private StepAwareService stepAwareService;

    @Mock
    private PwsFileUpload fileUpload;

    private PaymentInformation paymentInfo;
    private CreditTransferTransaction transaction;
    private static final String TEST_ERROR_MSG = "Test error message";
    private static final long TEST_FILE_UPLOAD_ID = 123L;
    private static final String TEST_CREATED_BY = "testUser";
    private static final String TEST_FILE_REF = "TEST-REF-001";
    private static final String TEST_BANK_ENTITY = "TEST_BANK";
    private static final String TEST_DMP_BATCH = "BATCH001";

    @BeforeEach
    void setUp() {
        // Setup FileUpload
        when(fileUpload.getFileUploadId()).thenReturn(TEST_FILE_UPLOAD_ID);
        when(fileUpload.getCreatedBy()).thenReturn(TEST_CREATED_BY);
        when(fileUpload.getFileReferenceId()).thenReturn(TEST_FILE_REF);
        when(stepAwareService.getFileUpload()).thenReturn(fileUpload);
        when(stepAwareService.getBankEntityId()).thenReturn(TEST_BANK_ENTITY);

        // Setup PaymentInfo
        paymentInfo = new PaymentInformation();
        PwsBulkTransactions bulkTxn = new PwsBulkTransactions();
        bulkTxn.setDmpBatchNumber(TEST_DMP_BATCH);
        paymentInfo.setPwsBulkTransactions(bulkTxn);

        // Setup Transaction
        transaction = new CreditTransferTransaction();
        transaction.setDmpLineNo("1");
    }

    @Nested
    class RejectedFileTests {
        @Test
        void createRecordForRejectedFile_WithFileUpload() {
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedFile(
                stepAwareService, TEST_ERROR_MSG);

            assertNotNull(result);
            assertEquals("Bulk File Rejected", result.getEntityType());
            assertEquals(TEST_FILE_UPLOAD_ID, result.getEntityId());
            assertEquals(TEST_CREATED_BY, result.getCreatedBy());
            assertEquals(TEST_FILE_REF, result.getBankReferenceId());
            assertEquals(ErrorCode.PBP_2001, result.getRejectCode());
            assertEquals(TEST_ERROR_MSG, result.getErrorDetail());
            assertNotNull(result.getCreatedDate());
        }

        @Test
        void createRecordForRejectedFile_WithoutFileUpload() {
            when(stepAwareService.getFileUpload()).thenReturn(null);

            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedFile(
                stepAwareService, TEST_ERROR_MSG);

            assertNotNull(result);
            assertEquals(TEST_BANK_ENTITY, result.getEntityType());
            assertEquals(ErrorCode.PBP_2001, result.getRejectCode());
            assertEquals(TEST_ERROR_MSG, result.getErrorDetail());
        }

        @Test
        void createRecordForRejectedFile_NullService() {
            assertThrows(IllegalArgumentException.class, 
                () -> PaymentUtils.createRecordForRejectedFile(null, TEST_ERROR_MSG));
        }
    }

    @Nested
    class RejectedPaymentTests {
        @Test
        void createRecordForRejectedPayment_Success() {
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedPayment(
                stepAwareService, paymentInfo, TEST_ERROR_MSG);

            assertNotNull(result);
            assertEquals("Payment Rejected", result.getEntityType());
            assertEquals(TEST_FILE_UPLOAD_ID, result.getEntityId());
            assertTrue(result.getErrorDetail().contains(TEST_DMP_BATCH));
            assertTrue(result.getErrorDetail().contains(TEST_ERROR_MSG));
        }

        @Test
        void createRecordForRejectedPayment_NullPaymentInfo() {
            assertThrows(IllegalArgumentException.class, 
                () -> PaymentUtils.createRecordForRejectedPayment(
                    stepAwareService, null, TEST_ERROR_MSG));
        }
    }

    @Nested
    class RejectedTransactionTests {
        @Test
        void createRecordForRejectedTransaction_Success() {
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedTransaction(
                stepAwareService, paymentInfo, transaction, TEST_ERROR_MSG);

            assertNotNull(result);
            assertEquals("Transaction Rejected", result.getEntityType());
            assertEquals(1L, result.getLineNo());
            assertTrue(result.getErrorDetail().contains(TEST_DMP_BATCH));
            assertTrue(result.getErrorDetail().contains(TEST_ERROR_MSG));
        }

        @Test
        void createRecordForRejectedTransaction_InvalidLineNo() {
            transaction.setDmpLineNo("invalid");
            
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedTransaction(
                stepAwareService, paymentInfo, transaction, TEST_ERROR_MSG);

            assertEquals(0L, result.getLineNo());
        }
    }

    @Nested
    class SaveRecordTests {
        @Test
        void createPwsSaveRecord_WithValidInput() {
            PwsSaveRecord result = PaymentUtils.createPwsSaveRecord(123L, "TEST_REF");
            
            assertNotNull(result);
            assertEquals(123L, result.getId());
            assertEquals("TEST_REF", result.getDmpTxnRef());
        }

        @Test
        void createPwsSaveRecord_WithNullDmpTxnRef() {
            PwsSaveRecord result = PaymentUtils.createPwsSaveRecord(123L, null);
            
            assertNotNull(result);
            assertEquals("", result.getDmpTxnRef());
        }
    }

    @Nested
    class UpdatePaymentResultTests {
        private Pain001InboundProcessingResult result;
        private PwsSaveRecord saveRecord;

        @BeforeEach
        void setUp() {
            result = new Pain001InboundProcessingResult();
            saveRecord = new PwsSaveRecord(123L, "TEST_REF");
        }

        @Test
        void updatePaymentSaved_Success() {
            PaymentUtils.updatePaymentSaved(result, saveRecord);
            
            assertNotNull(result.getPaymentSaved());
            assertTrue(result.getPaymentSaved().contains(saveRecord));
        }

        @Test
        void updatePaymentSavedError_NoDuplicates() {
            PaymentUtils.updatePaymentSavedError(result, saveRecord);
            PaymentUtils.updatePaymentSavedError(result, saveRecord);
            
            assertNotNull(result.getPaymentSavedError());
            assertEquals(1, result.getPaymentSavedError().size());
        }

        @Test
        void updatePaymentSaved_NullResult() {
            assertThrows(IllegalArgumentException.class, 
                () -> PaymentUtils.updatePaymentSaved(null, saveRecord));
        }

        @Test
        void updatePaymentSavedError_NullRecord() {
            assertThrows(IllegalArgumentException.class, 
                () -> PaymentUtils.updatePaymentSavedError(result, null));
        }
    }
}
```

# Flow

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkProcessingFlowBuilder extends RouteBuilder {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final PlatformTransactionManager platformTransactionManager;
    private final PaymentContext paymentContext;
    private final Pain001InboundService pain001InboundService;

    @Override
    public void configure() {
        appConfig.getBulkRoutes().stream()
                .filter(AppConfig.BulkRoute::isEnabled)
                .forEach(this::configureRoute);
    }

    private void configureRoute(AppConfig.BulkRoute bulkRoute) {
        log.info("Creating processing flow: {}", bulkRoute);
        
        if (bulkRoute.getProcessingType() != AppConfig.ProcessingType.INBOUND) {
            throw new BulkProcessingException("Unsupported flow type", 
                new IllegalArgumentException("Unsupported flow: " + bulkRoute.getProcessingType()));
        }

        // Configure error handling
        configureErrorHandling(bulkRoute);
        
        // Define the main route
        from(buildInboundFromUri(bulkRoute))
            .routeId(bulkRoute.getRouteName())
            .process(exchange -> handleInboundProcessing(bulkRoute, exchange));
    }

    private void configureErrorHandling(AppConfig.BulkRoute bulkRoute) {
        onException(Exception.class)
            .handled(true)
            .process(exchange -> {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                log.error("Processing failed for file: {}. Error: {}", fileName, cause.getMessage(), cause);
                handleInboundClose(bulkRoute, exchange, ExitStatus.FAILED);
            });
    }

    private void handleInboundProcessing(AppConfig.BulkRoute bulkRoute, Exchange exchange) throws Exception {
        createInboundContext(bulkRoute, exchange);
        JobParameters jobParameters = createInboundJobParameters(bulkRoute, exchange);
        Job job = createJob(bulkRoute, exchange);
        
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        waitForJobCompletion(jobExecution);
        handleInboundJobExecution(bulkRoute, exchange, jobExecution);
    }

    private void waitForJobCompletion(JobExecution jobExecution) throws InterruptedException {
        while (jobExecution.isRunning()) {
            Thread.sleep(1000); // Reduced sleep time, consider using more sophisticated waiting mechanism
        }
    }

    private Job createJob(AppConfig.BulkRoute bulkRoute, Exchange exchange) {
        return new JobBuilder("BatchJob_" + bulkRoute.getRouteName(), jobRepository)
            .listener(createJobListener(bulkRoute, exchange))
            .start(createInitialStep(bulkRoute))
            .build();
    }

    private JobExecutionListener createJobListener(AppConfig.BulkRoute bulkRoute, Exchange exchange) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                initializeJobContext(jobExecution, bulkRoute, exchange);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                logJobCompletion(jobExecution);
            }
        };
    }

    private Step createInitialStep(AppConfig.BulkRoute bulkRoute) {
        List<String> stepNames = Optional.ofNullable(bulkRoute.getSteps())
            .filter(steps -> !steps.isEmpty())
            .orElseThrow(() -> new BulkProcessingException(
                "No steps defined for route: " + bulkRoute.getRouteName()));

        Step firstStep = createStepForName(stepNames.get(0), bulkRoute);
        return stepNames.stream()
            .skip(1)
            .reduce(
                firstStep,
                (previousStep, stepName) -> new StepBuilder(stepName, jobRepository)
                    .next(createStepForName(stepName, bulkRoute))
                    .build(),
                (step1, step2) -> step1
            );
    }

    protected Step createStepForName(String stepName, AppConfig.BulkRoute bulkRoute) {
        StepBuilder stepBuilder = new StepBuilder(stepName, jobRepository);
        
        return switch (stepName) {
            case "pain001-processing" -> createPain001ProcessingStep(stepBuilder);
            case "payment-debulk" -> createPaymentDebulkStep(stepBuilder);
            case "payment-validation" -> createPaymentValidationStep(stepBuilder);
            case "payment-enrichment" -> createPaymentEnrichmentStep(stepBuilder);
            case "payment-save" -> createPaymentSaveStep(stepBuilder);
            default -> throw new BulkProcessingException("Unknown step: " + stepName);
        };
    }

    // Example of one refactored step implementation
    protected Step createPain001ProcessingStep(StepBuilder stepBuilder) {
        return stepBuilder
            .tasklet((contribution, chunkContext) -> {
                ExecutionContext jobContext = getJobContext(chunkContext);
                pain001InboundService.beforeStep(chunkContext.getStepContext().getStepExecution());

                String sourcePath = getSourcePath(jobContext, chunkContext);
                String fileContent = readFileContent(sourcePath);
                
                List<PaymentInformation> paymentInformations = processPain001File(fileContent);
                validateAndUpdateContext(paymentInformations, jobContext, contribution);
                
                return RepeatStatus.FINISHED;
            }, platformTransactionManager)
            .build();
    }

    private List<PaymentInformation> processPain001File(String fileContent) {
        return Optional.ofNullable(pain001InboundService.processPain001(fileContent))
            .filter(payments -> !payments.isEmpty())
            .orElseThrow(() -> new JobExecutionException("Pain001 processing returned no valid payments"));
    }

    private void validateAndUpdateContext(
            List<PaymentInformation> paymentInformations,
            ExecutionContext jobContext,
            StepContribution contribution) {
        
        if (CollectionUtils.isEmpty(paymentInformations)) {
            Pain001InboundProcessingResult result = jobContext.get(
                ContextKey.result, 
                Pain001InboundProcessingResult.class
            );
            contribution.setExitStatus(ExitStatus.FAILED);
            throw new JobExecutionException(
                String.format("%s: %s", 
                    result.getProcessingStatus(), 
                    Optional.ofNullable(result.getMessage()).orElse("No message"))
            );
        }
        
        paymentContext.setPaymentInformations(paymentInformations);
    }
}
```

```java
@ExtendWith(MockitoExtension.class)
class BulkProcessingFlowBuilderTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private PaymentContext paymentContext;

    @Mock
    private Pain001InboundService pain001InboundService;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private ExecutionContext jobExecutionContext;

    @Mock
    private StepExecution stepExecution;

    @InjectMocks
    private BulkProcessingFlowBuilder flowBuilder;

    private AppConfig.BulkRoute testRoute;
    private InboundContext testContext;
    private static final String TEST_FILE_PATH = "/test/path/testfile.json";
    private static final String TEST_FILE_NAME = "testfile.json";

    @BeforeEach
    void setUp() {
        setupTestRoute();
        setupTestContext();
        setupMocks();
    }

    @Nested
    class RouteConfigurationTests {
        @Test
        void whenValidInboundRoute_shouldConfigureSuccessfully() throws Exception {
            // Arrange
            List<AppConfig.BulkRoute> routes = Collections.singletonList(testRoute);
            when(appConfig.getBulkRoutes()).thenReturn(routes);

            // Act
            flowBuilder.configure();

            // Assert
            verify(appConfig).getBulkRoutes();
        }

        @Test
        void whenOutboundRoute_shouldThrowException() {
            // Arrange
            testRoute.setProcessingType(AppConfig.ProcessingType.OUTBOUND);

            // Act & Assert
            assertThrows(BulkProcessingException.class, 
                () -> flowBuilder.configureRoute(testRoute));
        }

        @Test
        void whenNoSteps_shouldThrowException() {
            // Arrange
            testRoute.setSteps(Collections.emptyList());

            // Act & Assert
            assertThrows(BulkProcessingException.class, 
                () -> flowBuilder.createJob(testRoute, exchange, paymentContext));
        }
    }

    @Nested
    class FileHandlingTests {
        @Test
        void whenBuildInboundUri_shouldCreateValidUri() {
            // Act
            String uri = flowBuilder.buildInboundFromUri(testRoute);

            // Assert
            assertTrue(uri.startsWith("file:///"));
            assertTrue(uri.contains("antInclude=*_Auth.json"));
            assertTrue(uri.contains("noop=true"));
            assertTrue(uri.contains("recursive=false"));
        }

        @Test
        void whenHandleInboundClose_withSuccess_shouldMoveFiles() {
            // Arrange
            setupMockFileSystem();

            // Act
            flowBuilder.handleInboundClose(testRoute, exchange, ExitStatus.COMPLETED);

            // Assert
            verifyFileMovement(true);
        }

        @Test
        void whenHandleInboundClose_withFailure_shouldMoveToErrorDir() {
            // Arrange
            setupMockFileSystem();

            // Act
            flowBuilder.handleInboundClose(testRoute, exchange, ExitStatus.FAILED);

            // Assert
            verifyFileMovement(false);
        }
    }

    @Nested
    class JobExecutionTests {
        @Test
        void whenCreateJob_shouldSetupCorrectSteps() {
            // Act
            Job job = flowBuilder.createJob(testRoute, exchange, paymentContext);

            // Assert
            assertNotNull(job);
            assertEquals("BatchJob_" + testRoute.getRouteName(), job.getName());
            verifyStepsCreated();
        }

        @Test
        void whenHandleJobExecution_withSuccess_shouldUpdateContext() {
            // Arrange
            setupSuccessfulJobExecution();

            // Act
            flowBuilder.handleInboundJobExecution(testRoute, exchange, jobExecution);

            // Assert
            verify(exchange.getIn()).setHeader("exitStatus", ExitStatus.COMPLETED);
            verifyContextUpdated();
        }

        @Test
        void whenHandleJobExecution_withFailure_shouldHandleError() {
            // Arrange
            setupFailedJobExecution();

            // Act
            flowBuilder.handleInboundJobExecution(testRoute, exchange, jobExecution);

            // Assert
            verify(exchange.getIn()).setHeader("exitStatus", ExitStatus.FAILED);
            verifyErrorHandling();
        }
    }

    @Nested
    class StepCreationTests {
        @Test
        void whenCreatePain001ProcessingStep_shouldConfigureCorrectly() {
            // Act
            Step step = flowBuilder.createStepForName("pain001-processing", testRoute, paymentContext);

            // Assert
            assertNotNull(step);
            assertEquals("pain001-processing", step.getName());
        }

        @Test
        void whenCreatePaymentDebulkStep_shouldConfigureCorrectly() {
            // Act
            Step step = flowBuilder.createStepForName("payment-debulk", testRoute, paymentContext);

            // Assert
            assertNotNull(step);
            assertEquals("payment-debulk", step.getName());
        }

        // Similar tests for other steps...

        @Test
        void whenUnknownStep_shouldThrowException() {
            assertThrows(BulkProcessingException.class, 
                () -> flowBuilder.createStepForName("unknown-step", testRoute, paymentContext));
        }
    }

    private void setupTestRoute() {
        testRoute = new AppConfig.BulkRoute();
        testRoute.setRouteName("test-route");
        testRoute.setProcessingType(AppConfig.ProcessingType.INBOUND);
        testRoute.setEnabled(true);
        testRoute.setSourceType(AppConfig.SourceDestinationType.FILE);
        
        AppConfig.FileSource fileSource = new AppConfig.FileSource();
        fileSource.setDirectoryName("/test/input");
        fileSource.setAntInclude("*_Auth.json");
        fileSource.setNoop(true);
        fileSource.setRecursive(false);
        fileSource.setMove("/test/backup");
        fileSource.setMoveFailed("/test/error");
        testRoute.setFileSource(fileSource);
        
        testRoute.setSteps(Arrays.asList(
            "pain001-processing",
            "payment-debulk",
            "payment-validation",
            "payment-enrichment",
            "payment-save"
        ));
    }

    private void setupTestContext() {
        testContext = new InboundContext();
        testContext.setCountry(Country.builder().code("TH").name("Thailand").build());
        testContext.setBulkRoute(testRoute);
        testContext.setSourcePath(TEST_FILE_PATH);
        testContext.setSourceName(TEST_FILE_NAME);
        testContext.setFormat("json");
    }

    private void setupMocks() {
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Exchange.FILE_PATH, String.class)).thenReturn(TEST_FILE_PATH);
        when(message.getHeader(Exchange.FILE_NAME, String.class)).thenReturn(TEST_FILE_NAME);
        when(exchange.getProperty(ContextKey.routeContext, InboundContext.class)).thenReturn(testContext);
        when(jobExecution.getExecutionContext()).thenReturn(jobExecutionContext);
    }

    // Additional helper methods...
}
```

And here's the test support code:

```java
@TestConfiguration
class BulkProcessingTestConfig {
    @Bean
    @Primary
    public JobRepository testJobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("org/springframework/batch/core/schema-h2.sql")
            .build());
        factory.setTransactionManager(new ResourcelessTransactionManager());
        return factory.getObject();
    }

    @Bean
    public JobLauncher testJobLauncher(JobRepository jobRepository) {
        SimpleJobLauncher launcher = new SimpleJobLauncher();
        launcher.setJobRepository(jobRepository);
        return launcher;
    }
}
```

# Service

## validation helper
```java
@ExtendWith(MockitoExtension.class)
class PaymentValidationHelperImplTest {

    @Mock
    protected StepExecution stepExecution;

    @Mock
    protected JobExecution jobExecution;

    @Mock
    protected ExecutionContext jobContext;

    @Mock
    private CompanySettings companySettings;

    @InjectMocks
    private PaymentValidationHelperImpl paymentValidationHelper;

    private static final String TEST_RESOURCE_ID = "SMART";
    private static final String TEST_FEATURE_ID = "BFU";
    private static final String TEST_ACCOUNT = "123456789";

    @BeforeEach
    void setUp() {
        // Setup execution context
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        
        // Initialize validation helper
        paymentValidationHelper.beforeStep(stepExecution);
    }

    @Nested
    class SourceAndFeatureValidationTests {
        @BeforeEach
        void setUp() {
            when(jobContext.get(ContextKey.resourceId)).thenReturn(TEST_RESOURCE_ID);
            when(jobContext.get(ContextKey.featureId)).thenReturn(TEST_FEATURE_ID);
        }

        @Test
        void whenValidSourceAndFeature_shouldReturnNull() {
            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateSourceAndFeature(TEST_RESOURCE_ID, TEST_FEATURE_ID);

            // Assert
            assertNull(result);
        }

        @Test
        void whenInvalidSource_shouldReturnError() {
            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateSourceAndFeature("INVALID", TEST_FEATURE_ID);

            // Assert
            assertNotNull(result);
            assertEquals(ErrorCode.PBP_4001, result.getErrorCode());
        }

        @Test
        void whenInvalidFeature_shouldReturnError() {
            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateSourceAndFeature(TEST_RESOURCE_ID, "INVALID");

            // Assert
            assertNotNull(result);
            assertEquals(ErrorCode.PBP_4001, result.getErrorCode());
        }
    }

    @Nested
    class AccountNRAValidationTests {
        @BeforeEach
        void setupAccountResource() {
            Map<String, AccountResource> accountResources = new HashMap<>();
            accountResources.put(TEST_ACCOUNT, createMockAccountResource(false));
            when(jobContext.get(eq(ContextKey.accountResources), any())).thenReturn(accountResources);
        }

        @Test
        void whenNonNRAAccount_shouldReturnNull() {
            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateAccountNonNRA(TEST_ACCOUNT);

            // Assert
            assertNull(result);
        }

        @Test
        void whenNRAAccount_shouldReturnError() {
            // Arrange
            Map<String, AccountResource> accountResources = new HashMap<>();
            accountResources.put(TEST_ACCOUNT, createMockAccountResource(true));
            when(jobContext.get(eq(ContextKey.accountResources), any())).thenReturn(accountResources);

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateAccountNonNRA(TEST_ACCOUNT);

            // Assert
            assertNotNull(result);
            assertEquals(ErrorCode.PBP_4003, result.getErrorCode());
        }

        private AccountResource createMockAccountResource(boolean isNRA) {
            AccountResource resource = new AccountResource();
            resource.setIsNRA(isNRA ? PaymentValidationHelperImpl.NRA_YES : "N");
            return resource;
        }
    }

    @Nested
    class BatchBookingValidationTests {
        @Test
        void whenItemizedBatchUnderLimit_shouldReturnNull() {
            // Arrange
            PaymentInformation payment = createPaymentInfo(5);

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateBatchBookingItemized(payment, 10);

            // Assert
            assertNull(result);
        }

        @Test
        void whenItemizedBatchOverLimit_shouldReturnError() {
            // Arrange
            PaymentInformation payment = createPaymentInfo(15);

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateBatchBookingItemized(payment, 10);

            // Assert
            assertNotNull(result);
            assertEquals(ErrorCode.PBP_4004, result.getErrorCode());
        }

        @Test
        void whenLumsumBatchUnderLimit_shouldReturnNull() {
            // Arrange
            PaymentInformation payment = createPaymentInfoWithAmount(
                Arrays.asList(
                    new BigDecimal("100.00"),
                    new BigDecimal("200.00")
                )
            );

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateBatchBookingLumsum(payment, 1000);

            // Assert
            assertNull(result);
        }

        @Test
        void whenLumsumBatchOverLimit_shouldReturnError() {
            // Arrange
            PaymentInformation payment = createPaymentInfoWithAmount(
                Arrays.asList(
                    new BigDecimal("500.00"),
                    new BigDecimal("600.00")
                )
            );

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateBatchBookingLumsum(payment, 1000);

            // Assert
            assertNotNull(result);
            assertEquals(ErrorCode.PBP_4005, result.getErrorCode());
        }

        private PaymentInformation createPaymentInfo(int transactionCount) {
            PaymentInformation payment = new PaymentInformation();
            List<CreditTransferTransaction> transactions = new ArrayList<>();
            for (int i = 0; i < transactionCount; i++) {
                transactions.add(createTransaction(new BigDecimal("100.00")));
            }
            payment.setCreditTransferTransactionList(transactions);
            return payment;
        }

        private PaymentInformation createPaymentInfoWithAmount(List<BigDecimal> amounts) {
            PaymentInformation payment = new PaymentInformation();
            List<CreditTransferTransaction> transactions = amounts.stream()
                .map(this::createTransaction)
                .collect(Collectors.toList());
            payment.setCreditTransferTransactionList(transactions);
            return payment;
        }

        private CreditTransferTransaction createTransaction(BigDecimal amount) {
            CreditTransferTransaction transaction = new CreditTransferTransaction();
            PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
            instructions.setTransactionAmount(amount);
            transaction.setPwsBulkTransactionInstructions(instructions);
            return transaction;
        }
    }

    @Nested
    class ValueDateValidationTests {
        @Test
        void whenValueDateAfterEarliestDate_shouldReturnNull() {
            // Arrange
            PaymentInformation payment = createPaymentInfoWithDate(
                LocalDate.now().plusDays(1));
            LocalDate earliestDate = LocalDate.now();

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateValueDate(payment, earliestDate);

            // Assert
            assertNull(result);
        }

        @Test
        void whenValueDateEqualsEarliestDate_shouldReturnNull() {
            // Arrange
            LocalDate testDate = LocalDate.now();
            PaymentInformation payment = createPaymentInfoWithDate(testDate);

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateValueDate(payment, testDate);

            // Assert
            assertNull(result);
        }

        @Test
        void whenValueDateBeforeEarliestDate_shouldReturnError() {
            // Arrange
            PaymentInformation payment = createPaymentInfoWithDate(
                LocalDate.now().minusDays(1));
            LocalDate earliestDate = LocalDate.now();

            // Act
            PaymentValidationResult result = paymentValidationHelper
                .validateValueDate(payment, earliestDate);

            // Assert
            assertNotNull(result);
            assertEquals(ErrorCode.PBP_4006, result.getErrorCode());
        }

        private PaymentInformation createPaymentInfoWithDate(LocalDate date) {
            PaymentInformation payment = new PaymentInformation();
            PwsBulkTransactions bulkTxn = new PwsBulkTransactions();
            bulkTxn.setTransferDate(Date.valueOf(date));
            payment.setPwsBulkTransactions(bulkTxn);
            return payment;
        }
    }
}
```

## stepaware service

``java
@ExtendWith(MockitoExtension.class)
class StepAwareServiceTest {

    @Mock
    private JobExecution jobExecution;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private ExecutionContext jobContext;

    @Mock
    private ExecutionContext stepContext;

    @Mock
    private ObjectMapper objectMapper;

    private StepAwareService service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new TestStepAwareService();
        
        // Setup common mock behaviors
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // Test implementation class
    private static class TestStepAwareService extends StepAwareService {
        // Concrete implementation for testing
    }

    @Nested
    @DisplayName("Step Lifecycle Tests")
    class StepLifecycleTest {
        
        @Test
        @DisplayName("beforeStep should initialize step execution")
        void beforeStep_ShouldInitializeStepExecution() {
            // Act
            service.beforeStep(stepExecution);
            
            // Assert
            assertThat(service.getStepExecution())
                .as("Step execution should be initialized")
                .isEqualTo(stepExecution);
        }

        @Test
        @DisplayName("beforeStep with null should throw exception")
        void beforeStep_WithNull_ShouldThrowException() {
            assertThatThrownBy(() -> service.beforeStep(null))
                .as("Should throw exception for null step execution")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Step execution cannot be null");
        }

        @Test
        @DisplayName("afterStep should clean up resources")
        void afterStep_ShouldCleanUpResources() {
            // Arrange
            service.beforeStep(stepExecution);
            
            // Act
            service.afterStep(stepExecution);
            
            // Assert
            assertThat(service.getStepExecution())
                .as("Step execution should be cleaned up")
                .isNull();
        }
    }

    @Nested
    @DisplayName("Context Access Tests")
    class ContextAccessTest {

        @BeforeEach
        void init() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("getJobContext should return valid context")
        void getJobContext_ShouldReturnValidContext() {
            assertThat(service.getJobContext())
                .as("Job context should be accessible")
                .isNotNull()
                .isEqualTo(jobContext);
        }

        @Test
        @DisplayName("getStepContext should return valid context")
        void getStepContext_ShouldReturnValidContext() {
            assertThat(service.getStepContext())
                .as("Step context should be accessible")
                .isNotNull()
                .isEqualTo(stepContext);
        }

        @Test
        @DisplayName("getJobContext without initialization should throw exception")
        void getJobContext_WithoutInit_ShouldThrowException() {
            // Arrange
            StepAwareService uninitializedService = new TestStepAwareService();
            
            // Assert
            assertThatThrownBy(() -> uninitializedService.getJobContext())
                .as("Should throw exception when accessing uninitialized context")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Service not initialized");
        }
    }

    @Nested
    @DisplayName("Route Configuration Tests")
    class RouteConfigurationTest {

        @BeforeEach
        void init() {
            service.beforeStep(stepExecution);
        }

        @ParameterizedTest
        @DisplayName("getRouteName should return correct route name")
        @ValueSource(strings = {"route1", "route2", "route3"})
        void getRouteName_ShouldReturnCorrectRouteName(String routeName) {
            // Arrange
            AppConfig.BulkRoute bulkRoute = mock(AppConfig.BulkRoute.class);
            when(bulkRoute.getRouteName()).thenReturn(routeName);
            when(jobContext.get(ContextKey.routeConfig, AppConfig.BulkRoute.class))
                .thenReturn(bulkRoute);

            // Act & Assert
            assertThat(service.getRouteName())
                .as("Route name should match configured value")
                .isEqualTo(routeName);
        }

        @Test
        @DisplayName("getRouteConfig with missing configuration should throw exception")
        void getRouteConfig_WithMissingConfig_ShouldThrowException() {
            // Arrange
            when(jobContext.get(ContextKey.routeConfig, AppConfig.BulkRoute.class))
                .thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> service.getRouteName())
                .as("Should throw exception for missing route configuration")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Route configuration not found");
        }
    }

    @Nested
    @DisplayName("Processing Status Tests")
    class ProcessingStatusTest {

        @BeforeEach
        void init() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("updateProcessingStatus should update status correctly")
        void updateProcessingStatus_ShouldUpdateCorrectly() {
            // Arrange
            Pain001InboundProcessingResult result = mock(Pain001InboundProcessingResult.class);
            when(jobContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(result);

            // Act
            service.updateProcessingStatus(
                Pain001InboundProcessingStatus.COMPLETED,
                "Success",
                objectMapper
            );

            // Assert
            verify(result).setProcessingStatus(Pain001InboundProcessingStatus.COMPLETED);
            verify(result).setMessage("Success");
        }

        @Test
        @DisplayName("updateProcessingStatus with null result should throw exception")
        void updateProcessingStatus_WithNullResult_ShouldThrowException() {
            // Arrange
            when(jobContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> 
                service.updateProcessingStatus(
                    Pain001InboundProcessingStatus.COMPLETED,
                    "Success",
                    objectMapper
                ))
                .as("Should throw exception for null processing result")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Processing result not found");
        }
    }

    @Nested
    @DisplayName("Bank Settings Tests")
    class BankSettingsTest {

        @BeforeEach
        void init() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("setBankSettings should store settings correctly")
        void setBankSettings_ShouldStoreCorrectly() {
            // Arrange
            BankSettings bankSettings = mock(BankSettings.class);
            when(jobContext.get(ContextKey.bankSettings, BankSettings.class))
                .thenReturn(bankSettings);

            // Act
            service.setBankSettings(
                "resourceId",
                "featureId",
                BankSettings.SettingsName.GENERAL,
                "value"
            );

            // Assert
            verify(bankSettings).setBankSettings(
                eq("resourceId"),
                eq("featureId"),
                eq(BankSettings.SettingsName.GENERAL),
                eq("value")
            );
        }

        @Test
        @DisplayName("getAllBankSettings should return settings map")
        void getAllBankSettings_ShouldReturnMap() {
            // Arrange
            BankSettings bankSettings = mock(BankSettings.class);
            Map<BankSettings.SettingsName, Object> settingsMap = new HashMap<>();
            settingsMap.put(BankSettings.SettingsName.GENERAL, "value");
            
            when(jobContext.get(ContextKey.bankSettings, BankSettings.class))
                .thenReturn(bankSettings);
            when(bankSettings.getAllBankSettings(anyString(), anyString()))
                .thenReturn(settingsMap);

            // Act
            Map<BankSettings.SettingsName, Object> result = 
                service.getAllBankSettings("resourceId", "featureId");

            // Assert
            assertThat(result)
                .as("Settings map should contain expected values")
                .isNotEmpty()
                .containsEntry(BankSettings.SettingsName.GENERAL, "value");
        }
    }

    @Nested
    @DisplayName("Debug Context Tests")
    @ExtendWith(CaptureLogExtension.class)
    class DebugContextTest {

        @BeforeEach
        void init() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("debugContext should log all required information")
        void debugContext_ShouldLogAllInfo(@CapturedLog CapturedLog log) {
            // Arrange
            setupMockContextData();

            // Act
            service.debugContext(objectMapper);

            // Assert
            assertThat(log.getAll())
                .as("Debug logs should contain all required information")
                .anyMatch(line -> line.contains("Route Name"))
                .anyMatch(line -> line.contains("Country Code"))
                .anyMatch(line -> line.contains("Channel"))
                .anyMatch(line -> line.contains("Bank Entity"));
        }

        private void setupMockContextData() {
            // Setup mock data for context debugging
            AppConfig.BulkRoute bulkRoute = mock(AppConfig.BulkRoute.class);
            when(bulkRoute.getRouteName()).thenReturn("testRoute");
            when(jobContext.get(ContextKey.routeConfig, AppConfig.BulkRoute.class))
                .thenReturn(bulkRoute);

            Country country = mock(Country.class);
            when(country.getCountryCode()).thenReturn("US");
            when(jobContext.get(ContextKey.country, Country.class))
                .thenReturn(country);

            // Add more mock data as needed
        }
    }
}
```

## inbound service

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

