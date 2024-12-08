# flow builder

```txt
file://c:/CEW/tmp/dmp/incoming?antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=${file:name.noext}.xml.done&maxMessagesPerPoll=1&move=c:/CEW/tmp/dmp/backup&moveFailed=c:/CEW/tmp/dmp/error&noop=false&readLock=rename&readLockTimeout=60000&sortBy=file:modified
```

```java
package com.example.flow;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BulkProcessingFlowBuilderTest {

    @Mock private AppConfig appConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private JobRepository jobRepository;
    @Mock private JobLauncher jobLauncher;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private PaymentContext paymentContext;
    @Mock private Pain001InboundService pain001InboundService;
    @Mock private Exchange exchange;
    @Mock private Message message;

    private BulkProcessingFlowBuilder flowBuilder;
    private AutoCloseable closeable;
    private Path tempDir;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        flowBuilder = new BulkProcessingFlowBuilder(
            appConfig, objectMapper, jobRepository, jobLauncher,
            transactionManager, paymentContext, pain001InboundService
        );
        tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        
        when(exchange.getIn()).thenReturn(message);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("URI Building Tests")
    class UriBuiluildingTests {
        
        @Test
        @DisplayName("Should build inbound URI for file source")
        void buildInboundFromUri_WithFileSource_ShouldBuildCorrectUri() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            AppConfig.FileSource fileSource = new AppConfig.FileSource();
            fileSource.setDirectoryName("/test/dir");
            fileSource.setAntInclude("*.xml");
            fileSource.setAntExclude("*.tmp");
            fileSource.setCharset("UTF-8");
            fileSource.setDoneFileName("${file:name}.done");
            fileSource.setDelay(5000);
            fileSource.setSortBy("file:modified");
            fileSource.setMaxMessagesPerPoll(1);
            fileSource.setNoop(true);
            fileSource.setRecursive(false);
            
            bulkRoute.setSourceType(AppConfig.SourceDestinationType.FILE);
            bulkRoute.setFileSource(fileSource);

            // Act
            String uri = flowBuilder.buildInboundFromUri(bulkRoute);

            // Assert
            assertTrue(uri.startsWith("file://"));
            assertTrue(uri.contains("/test/dir"));
            assertTrue(uri.contains("antInclude=*.xml"));
            assertTrue(uri.contains("maxMessagesPerPoll=1"));
        }

        @Test
        @DisplayName("Should throw exception for non-noop file source")
        void buildInboundFromUri_WithNonNoopFileSource_ShouldThrowException() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            AppConfig.FileSource fileSource = new AppConfig.FileSource();
            fileSource.setNoop(false);
            bulkRoute.setSourceType(AppConfig.SourceDestinationType.FILE);
            bulkRoute.setFileSource(fileSource);

            // Act & Assert
            assertThrows(BulkProcessingException.class, 
                () -> flowBuilder.buildInboundFromUri(bulkRoute));
        }
    }

    @Nested
    @DisplayName("Context Creation Tests")
    class ContextCreationTests {
        
        @Test
        @DisplayName("Should create inbound context")
        void createInboundContext_WithValidExchange_ShouldCreateContext() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            when(message.getHeader(Exchange.FILE_PATH, String.class))
                .thenReturn("/test/path/file.xml");
            when(message.getHeader(Exchange.FILE_NAME, String.class))
                .thenReturn("file.xml");

            // Act
            flowBuilder.createInboundContext(bulkRoute, exchange);

            // Assert
            verify(exchange).setProperty(eq(ContextKey.routeContext), any(InboundContext.class));
            verify(message).getHeader(Exchange.FILE_PATH, String.class);
            verify(message).getHeader(Exchange.FILE_NAME, String.class);
        }
    }

    @Nested
    @DisplayName("Job Parameter Tests")
    class JobParameterTests {
        
        @Test
        @DisplayName("Should create job parameters from context")
        void createInboundJobParameters_WithValidContext_ShouldCreateParameters() throws Exception {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            bulkRoute.setRouteName("testRoute");
            
            InboundContext routeContext = new InboundContext();
            routeContext.setSourcePath("/test/path");
            routeContext.setSourceName("test.xml");
            
            when(exchange.getProperty("routeContext", InboundContext.class))
                .thenReturn(routeContext);
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

            // Act
            JobParameters params = flowBuilder.createInboundJobParameters(bulkRoute, exchange);

            // Assert
            assertNotNull(params);
            assertEquals("testRoute", params.getString(ContextKey.routeName));
            assertEquals("/test/path", params.getString(ContextKey.sourcePath));
        }
    }

    @Nested
    @DisplayName("Job Execution Tests")
    class JobExecutionTests {
        
        @Test
        @DisplayName("Should handle successful job execution")
        void handleInboundJobExecution_WithSuccessfulExecution_ShouldHandleCorrectly() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            JobExecution jobExecution = mock(JobExecution.class);
            ExecutionContext executionContext = new ExecutionContext();
            InboundContext routeContext = new InboundContext();
            
            when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
            when(jobExecution.getExecutionContext()).thenReturn(executionContext);
            when(exchange.getProperty("routeContext", InboundContext.class))
                .thenReturn(routeContext);

            // Act
            flowBuilder.handleInboundJobExecution(bulkRoute, exchange, jobExecution);

            // Assert
            verify(exchange.getIn()).setHeader("exitStatus", ExitStatus.COMPLETED);
            verify(exchange.getProperty("routeContext", InboundContext.class));
        }
    }

    @Nested
    @DisplayName("Step Creation Tests")
    class StepCreationTests {
        
        @Test
        @DisplayName("Should create pain001 processing step")
        void createPain001ProcessingStep_WithValidConfig_ShouldCreateStep() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            StepBuilder stepBuilder = new StepBuilder("pain001-processing", jobRepository);

            // Act
            Step step = flowBuilder.createPain001ProcessingStep(stepBuilder, bulkRoute, paymentContext);

            // Assert
            assertNotNull(step);
            assertEquals("pain001-processing", step.getName());
        }

        @Test
        @DisplayName("Should create all required steps")
        void createStepForName_WithValidStepNames_ShouldCreateSteps() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            String[] stepNames = {
                "pain001-processing",
                "payment-debulk",
                "payment-validation",
                "payment-enrichment",
                "payment-save"
            };

            // Act & Assert
            Arrays.stream(stepNames).forEach(stepName -> {
                Step step = flowBuilder.createStepForName(stepName, bulkRoute, paymentContext);
                assertNotNull(step);
                assertEquals(stepName, step.getName());
            });
        }

        @Test
        @DisplayName("Should throw exception for unknown step")
        void createStepForName_WithUnknownStep_ShouldThrowException() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();

            // Act & Assert
            assertThrows(BulkProcessingException.class,
                () -> flowBuilder.createStepForName("unknown-step", bulkRoute, paymentContext));
        }
    }

    @Nested
    @DisplayName("Job Creation Tests")
    class JobCreationTests {
        
        @Test
        @DisplayName("Should create job with all steps")
        void createJob_WithValidConfig_ShouldCreateJob() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            bulkRoute.setRouteName("testRoute");
            bulkRoute.setProcessingType(AppConfig.ProcessingType.INBOUND);
            bulkRoute.setSteps(Arrays.asList(
                "pain001-processing",
                "payment-debulk",
                "payment-validation"
            ));
            
            InboundContext routeContext = new InboundContext();
            when(exchange.getProperty(ContextKey.routeContext, InboundContext.class))
                .thenReturn(routeContext);

            // Act
            Job job = flowBuilder.createJob(bulkRoute, exchange, paymentContext);

            // Assert
            assertNotNull(job);
            assertTrue(job.getName().contains("testRoute"));
        }

        @Test
        @DisplayName("Should throw exception for empty steps")
        void createJob_WithEmptySteps_ShouldThrowException() {
            // Arrange
            AppConfig.BulkRoute bulkRoute = new AppConfig.BulkRoute();
            bulkRoute.setRouteName("testRoute");

            // Act & Assert
            assertThrows(BulkProcessingException.class,
                () -> flowBuilder.createJob(bulkRoute, exchange, paymentContext));
        }
    }
}
```
