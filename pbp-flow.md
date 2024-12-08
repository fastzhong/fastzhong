# flow builder

```txt
file://c:/CEW/tmp/dmp/incoming?antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=${file:name.noext}.xml.done&maxMessagesPerPoll=1&move=c:/CEW/tmp/dmp/backup&moveFailed=c:/CEW/tmp/dmp/error&noop=false&readLock=rename&readLockTimeout=60000&sortBy=file:modified
```

```java
@ExtendWith(MockitoExtension.class)
class BulkProcessingFlowBuilderTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private BulkRoutesConfig bulkRoutesConfig;

    @Mock
    private ObjectMapper objMapper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private Pain001InboundService pain001InboundService;

    @InjectMocks
    private BulkProcessingFlowBuilder flowBuilder;

    @Test
    void testConfigure_withEnabledRoutes() throws Exception {
        BulkRoute bulkRoute = new BulkRoute();
        bulkRoute.setEnabled(true);
        bulkRoute.setProcessingType(ProcessingType.INBOUND);
        bulkRoute.setRouteName("TestRoute");
        bulkRoute.setSourceType(SourceType.FILE);
        bulkRoute.setFileSource(createTestFileSource());

        when(bulkRoutesConfig.getRoutes()).thenReturn(Collections.singletonList(bulkRoute));

        flowBuilder.configure();

        // Verify the route was built correctly
        verify(bulkRoutesConfig, times(1)).getRoutes();
    }

    @Test
    void testConfigure_withDisabledRoutes() throws Exception {
        BulkRoute bulkRoute = new BulkRoute();
        bulkRoute.setEnabled(false);

        when(bulkRoutesConfig.getRoutes()).thenReturn(Collections.singletonList(bulkRoute));

        flowBuilder.configure();

        // Verify no further methods were invoked
        verify(bulkRoutesConfig, times(1)).getRoutes();
        verifyNoInteractions(appConfig, objMapper, jobRepository, jobLauncher, platformTransactionManager);
    }

    @Test
    void testBuildInboundFromUri_validFileSource() {
        BulkRoute bulkRoute = new BulkRoute();
        bulkRoute.setSourceType(SourceType.FILE);
        bulkRoute.setFileSource(createTestFileSource());

        String uri = flowBuilder.buildInboundFromUri(bulkRoute);

        assertNotNull(uri);
        assertTrue(uri.contains("file:"));
    }

    @Test
    void testBuildInboundFromUri_unsupportedSourceType() {
        BulkRoute bulkRoute = new BulkRoute();
        bulkRoute.setSourceType(SourceType.UNKNOWN);

        Exception exception = assertThrows(BulkProcessingException.class,
                () -> flowBuilder.buildInboundFromUri(bulkRoute));

        assertTrue(exception.getMessage().contains("source type not supported"));
    }

    @Test
    void testCreateInboundJobParameters_missingRouteContext() {
        Exchange exchange = mock(Exchange.class);
        when(exchange.getProperty("routeContext", InboundContext.class)).thenReturn(null);

        BulkRoute bulkRoute = new BulkRoute();
        bulkRoute.setRouteName("TestRoute");

        Exception exception = assertThrows(BulkProcessingException.class,
                () -> flowBuilder.createInboundJobParameters(bulkRoute, exchange));

        assertTrue(exception.getMessage().contains("routeContext is missing"));
    }

    @Test
    void testPrepareInboundContext_withFileSource() {
        BulkRoute bulkRoute = new BulkRoute();
        bulkRoute.setSourceType(SourceType.FILE);

        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Exchange.FILE_PATH, String.class)).thenReturn("/test/path");
        when(message.getHeader(Exchange.FILE_NAME, String.class)).thenReturn("test.json");

        flowBuilder.prepareInboundContext(bulkRoute, exchange);

        verify(exchange, times(1)).setProperty(eq(ContextKey.routeContext), any(InboundContext.class));
    }

    @Test
    void testHandleInboundJobExecution_withCompletedStatus() {
        BulkRoute bulkRoute = new BulkRoute();
        Exchange exchange = mock(Exchange.class);
        JobExecution jobExecution = mock(JobExecution.class);

        when(exchange.getProperty("routeContext", InboundContext.class)).thenReturn(new InboundContext());
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        flowBuilder.handleInboundJobExecution(bulkRoute, exchange, jobExecution);

        verify(exchange.getIn(), times(1)).setHeader(eq("exitStatus"), eq(ExitStatus.COMPLETED));
    }

    private BulkRoutesConfig.FileSource createTestFileSource() {
        BulkRoutesConfig.FileSource fileSource = new BulkRoutesConfig.FileSource();
        fileSource.setDirectoryName("/input");
        fileSource.setAntInclude("*.json");
        fileSource.setAntExclude("*.tmp");
        fileSource.setCharset("UTF-8");
        fileSource.setDoneFileName(".done");
        fileSource.setDelay(1000L);
        fileSource.setSortBy("file:name");
        fileSource.setMaxMessagesPerPoll(10);
        fileSource.setNoop(true);
        fileSource.setRecursive(false);
        fileSource.setMove("/processed");
        fileSource.setMoveFailed("/failed");
        fileSource.setReadLock("none");
        fileSource.setReadLockTimeout(1000L);
        fileSource.setReadLockInterval(500L);
        return fileSource;
    }
}

```
