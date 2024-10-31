# Srping Retry

```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
    <version>1.3.1</version>
</dependency>
```

```yml
app:
  retry:
    maxAttempts: 3
    backoff:
      initialInterval: 500     # in milliseconds
      multiplier: 2.0
      maxInterval: 5000        # in milliseconds
```

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.retry")
public class RetryConfigProperties {
    private int maxAttempts;
    private BackoffProperties backoff = new BackoffProperties();

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BackoffProperties getBackoff() {
        return backoff;
    }

    public void setBackoff(BackoffProperties backoff) {
        this.backoff = backoff;
    }

    public static class BackoffProperties {
        private long initialInterval;
        private double multiplier;
        private long maxInterval;

        public long getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(long initialInterval) {
            this.initialInterval = initialInterval;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxInterval() {
            return maxInterval;
        }

        public void setMaxInterval(long maxInterval) {
            this.maxInterval = maxInterval;
        }
    }
}
```


# WebClient & Resilient4J

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

```properties
resilience4j.circuitbreaker.instances.externalService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.externalService.waitDurationInOpenState=5000
resilience4j.circuitbreaker.instances.externalService.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.externalService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.externalService.minimumNumberOfCalls=5
```

export SERVICE_A_URL=http://prod-service-a1.example.com,http://prod-service-a2.example.com
export SERVICE_B_URL=http://prod-service-b1.example.com,http://prod-service-b2.example.com
export SERVICE_C_URL=http://prod-service-c1.example.com,http://prod-service-c2.example.com

```java
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration {

    @Value("${SERVICE_A_URL:http://localhost:8080}")
    private String serviceAUrl;

    @Value("${SERVICE_B_URL:http://localhost:8081}")
    private String serviceBUrl;

    @Value("${SERVICE_C_URL:http://localhost:8082}")
    private String serviceCUrl;

    @Bean
    public WebClient webClient(ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction) {
        // Create a ConnectionProvider to manage connection pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("customConnectionProvider")
                .maxConnections(100) // Max number of connections
                .pendingAcquireTimeout(Duration.ofMillis(5000)) // Timeout for acquiring connection
                .maxIdleTime(Duration.ofSeconds(20)) // Max idle time before a connection is closed
                .build();

        // Create a Reactor Netty HttpClient with timeouts and connection pooling
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // Connection timeout
                .doOnConnected(connection ->
                    connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                             .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)))
                .observe((connection, newState) -> {
                    if (newState == ConnectionObserver.State.DISCONNECTED) {
                        // Handle disconnects if needed
                    }
                })
                .responseTimeout(Duration.ofSeconds(10)) // Response timeout
                .wiretap("reactor.netty.http.client.HttpClient",
                          io.netty.handler.logging.LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL) // Enable logging
                .metrics(true) // Enable metrics for performance monitoring
                .compress(true) // Enable response compression
                .keepAlive(true); // Enable HTTP Keep-Alive

        // Build the WebClient
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(retryFilter()) // Add retry filter
                .filter(loggingFilter()) // Add logging filter
                .filter(loadBalancerExchangeFilterFunction) // Add load balancer filter
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // Set max memory size for responses (16MB)
                        .build())
                .defaultHeader("User-Agent", "Spring WebClient") // Set a default User-Agent header
                .build();
    }

    private ExchangeFilterFunction retryFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // Add your retry logic here
            // For example, use RetryBackoffSpec for sophisticated retry policies
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // if debugging
            if (clientRequest.headers().containsKey("Authorization")) {
                String jwtToken = clientRequest.headers().getFirst("Authorization").replace("Bearer ", "");
                System.out.println("JWT Token: " + jwtToken);
            }
            System.out.println("Request: " + clientRequest.method() + " " + clientRequest.url());
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            // if debugging
            System.out.println("Response Status: " + clientResponse.statusCode());
            return Mono.just(clientResponse);
        }));
    }

    private boolean isDebugging() {
        return true; // Implement your logic for enabling/disabling logging
    }

    // Example method to construct WebClient with service-specific URL
    private WebClient serviceClient(String serviceUrl) {
        return webClient(null).mutate()
                .baseUrl(serviceUrl)
                .defaultHeader("Authorization", "Bearer " + generateJwtToken()) // Add JWT token to header
                .build();
    }

    private String generateJwtToken() {
        // Logic to generate JWT token
        return "your_jwt_token";
    }


}

```

```java
import reactor.core.publisher.Mono;

public class MyService {

    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Inject the configured WebClient
    public MyService(WebClient webClient CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    // Example method to perform an HTTP POST request
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallback")
    @Retry(name = "externalService")
    @TimeLimiter(name = "externalService")
    public Mono<ServiceResponse> callExternalService(CompanyInfoRequest companyInfoRequest) {
        return webClient.post()
                .uri("/company")
                .body(Mono.just(companyInfoRequest), CompanyInfoRequest.class)
                .retrieve()
                .bodyToMono(ServiceResponse.class)
                .doOnError(e -> System.out.println("Error: " + e.getMessage()));
    }

    public Mono<String> fallback(Exception e) {
        // Log or handle the exception
        System.out.println("Fallback executed due to: " + throwable.getMessage());
        // Return a fallback response or alternative value
        return Mono.just(new ServiceResponse("Fallback response"));
    }

    // Example usage in a service class
    public Mono<CompanyInfoResponse> getCompanyInfo(String companyId) {
        return serviceClient(serviceAUrl)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/company/{id}")
                        .build(companyId))
                .retrieve()
                .bodyToMono(CompanyInfoResponse.class)
                .doOnError(e -> System.out.println("Error: " + e.getMessage()));
    }
}

```

```yaml
resilience4j:
    retry:
        instances:
            externalService:
                max-attempts: 3
                wait-duration: 500ms
                enable-exponential-backoff: true
                exponential-backoff-multiplier: 1.5
                retry-exceptions:
                    - org.springframework.web.reactive.function.client.WebClientResponseException
                ignore-exceptions:
                    - java.lang.IllegalArgumentException

    circuitbreaker:
        instances:
            externalService:
                register-health-indicator: true
                sliding-window-type: COUNT_BASED
                sliding-window-size: 50
                failure-rate-threshold: 50
                wait-duration-in-open-state: 30s
                permitted-number-of-calls-in-half-open-state: 10
                minimum-number-of-calls: 10

    timelimiter:
        instances:
            externalService:
                timeout-duration: 2s
                cancel-running-future: true
```

```java
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(WebClientResponseException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
    }

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(50)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(10)
                .minimumNumberOfCalls(10)
                .build();
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .cancelRunningFuture(true)
                .build();
    }
}
```

```java
@Configuration
@EnableRetry
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(5000); // 5 seconds

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }
}
```

# Command Line Application & Spring Active Profile

This approach will load properties in the following order:

application.properties (default shared properties)
application-{env}.properties (environment-specific properties)
application-{flow}.properties (flow-specific properties)

```bash
> java -Denv=dev -Dflow=flow1 -Dconfig.path=/path/to/your/config -jar your-application.jar
```

```java
@SpringBootApplication
 @Configuration
 @Slf4j
 public class UobCmdBootApplication implements CommandLineRunner {

     @Autowired
     ConfigurableApplicationContext context;

     @Autowired
     PfpProperties pfpProperties;

     @Autowired
     AtomicBoolean runOnce;

     @Autowired
     CountDownLatch latch;

     @Autowired
     FilePollingAdapter filePollingAdapter;

     public static void main(String[] args) {
        String env = System.getProperty("env", "dev");
        String flow = System.getProperty("flow", "flow1");
        String configPath = System.getProperty("config.path", "/etc/myapp/config")
        SpringApplication app = new SpringApplication(UobCmdBootApplication.class)
            .properties("spring.config.additional-location=" + configPath + "/")
            .profiles(env, flow)
            .build()
            .run(args);
     }

     @Override
     public void run(String... args) throws Exception {
         log.info("DMP Auth file processing path: {}", pfpProperties.getDmpAuthProcessingFilePath());
         log.info("polling {} mins", pfpProperties.getDmpAuthFilePolling());
         boolean runOnceFlag = Arrays.asList(args).contains("--once");
         runOnce.getAndSet(runOnceFlag);
         filePollingAdapter.startFilePolling();

         if (runOnceFlag) {
             // run once and exit
             log.info("run once only ...");
             latch.await();
             filePollingAdapter.stopFilePolling();
             SpringApplication.exit(context, () -> 0);
         } else {
             log.info("continuously running ...");
             Thread.currentThread().join();
         }
     }

    @Autowired
    private FileLockService fileLockService;

    @PreDestroy
    public void onShutdown() {
        fileLockService.releaseAllLocks();
    }
 }

```

For automatic restarts, you can use a process manager like systemd on Linux or a tool like Supervisor. Here's a sample systemd service file:

```txt
[Unit]
Description=UOB CMD Boot Application
After=network.target

[Service]
ExecStart=/usr/bin/java -jar /path/to/your/application.jar
Restart=always
User=youruser

[Install]
WantedBy=multi-user.target
```

# NAS & Non-blocking IO

## File Polling

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class FilePollingAdapter {

    private final ProcessContext processContext;
    private final PollerMetadata poller;
    private final ObjectMapper objectMapper;
    private final FileReadingMessageSource fileReadingMessageSource;
    private final MessageChannel fileProcessingInputChannel;
    private final FileLockService fileLockService;
    private final IntegrationFlowContext flowContext;

    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private IntegrationFlowContext.IntegrationFlowRegistration filePollingFlowRegistration;

    @ServiceActivator(inputChannel = "startFilePollingChannel")
    public void startFilePolling() {
        if (isPolling.compareAndSet(false, true)) {
            IntegrationFlow filePollingFlow = IntegrationFlow
                    .from(fileReadingMessageSource, config -> config.poller(poller))
                    .filter(File.class, file -> {
                        if (fileLockService.tryLock(file.getAbsolutePath())) {
                            return true;
                        } else {
                            log.info("failed to create lock for file: {}", file.getAbsolutePath());
                            return false;
                        }
                    })
                    .handle(this, HandlerName.readFile.name())
                    .channel(fileProcessingInputChannel)
                    .get();
            filePollingFlowRegistration = flowContext.registration(filePollingFlow).register();
            log.info("File Polling started");
        } else {
            log.info("File Polling already running");
        }
    }

    @ServiceActivator(inputChannel = "stopFilePollingChannel")
    public void stopFilePolling() {
        if (isPolling.compareAndSet(true, false)) {
            if (filePollingFlowRegistration != null) {
                flowContext.remove(filePollingFlowRegistration.getId());
                filePollingFlowRegistration = null;
            }
            log.info("File Polling stopped");
        } else {
            log.info("File Polling already stopped");
        }
    }

    public Message<?> readFile(Message<?> message) {
        log.info("readFile started");
        MessageHeaders headers = message.getHeaders();
        String fileName = (String)headers.get("file_name");
        List<HandlerName> processors = new ArrayList<>();
        processors.add(HandlerName.readFile);
        processContext.setContext(fileName, "processors", processors);
        File file = (File) message.getPayload();
        String jsonString = "";
        try {
            jsonString = new String(Files.readAllBytes(file.toPath()));
            log.info("successful to read file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("failed to read file: {}", file.getAbsolutePath(), e);
            return null;
        }
        AuthPain001 authPain001 = null;
        try {
            authPain001 = objectMapper.readValue(jsonString, AuthPain001.class);
            log.info("successful to convert json from: {}", file.getAbsolutePath());
        } catch (JsonProcessingException e) {
            log.error("failed to read file: {}", file.getAbsolutePath(), e);
            return null;
        }

        return MessageBuilder.withPayload(authPain001).copyHeaders(headers).build();
    }

}
```

## File Lock

```java
@Service
public class FileLockService {
    private final ConcurrentMap<String, FileLock> locks = new ConcurrentHashMap<>();

    public boolean tryLock(String filePath) {
        try {
            FileChannel channel = FileChannel.open(Paths.get(filePath), StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock != null) {
                locks.put(filePath, lock);
                return true;
            }
        } catch (IOException e) {
            // Log error
        }
        return false;
    }

    public void releaseLock(String filePath) {
        FileLock lock = locks.remove(filePath);
        if (lock != null) {
            try {
                lock.release();
                lock.channel().close();
            } catch (IOException e) {
                // Log error
            }
        }
    }

    public void releaseAllLocks() {
        locks.forEach((path, lock) -> {
            try {
                lock.release();
                lock.channel().close();
            } catch (IOException e) {
                // Log error
            }
        });
        locks.clear();
    }
}
```

## File Status Tracking

```java
public class FileProcessingStatus {

    private String fileName;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private LocalDateTime lastUpdated;

    // Constructors, getters, and setters
}

public enum ProcessingStatus {
    PENDING, PROCESSING, COMPLETED, ERROR
}

@Mapper
public interface FileProcessingStatusMapper {
    @Select("SELECT * FROM file_processing_status WHERE file_name = #{fileName}")
    FileProcessingStatus findByFileName(String fileName);

    @Insert("INSERT INTO file_processing_status (file_name, status, last_updated) " +
            "VALUES (#{fileName}, #{status}, #{lastUpdated})")
    void insert(FileProcessingStatus status);

    @Update("UPDATE file_processing_status SET status = #{status}, last_updated = #{lastUpdated} " +
            "WHERE file_name = #{fileName}")
    void update(FileProcessingStatus status);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.yourpackage.FileProcessingStatusMapper">
    <select id="findByFileName" resultType="com.yourpackage.FileProcessingStatus">
        SELECT * FROM file_processing_status WHERE file_name = #{fileName}
    </select>

    <insert id="insert" parameterType="com.yourpackage.FileProcessingStatus">
        INSERT INTO file_processing_status (file_name, status, last_updated)
        VALUES (#{fileName}, #{status}, #{lastUpdated})
    </insert>

    <update id="update" parameterType="com.yourpackage.FileProcessingStatus">
        UPDATE file_processing_status
        SET status = #{status}, last_updated = #{lastUpdated}
        WHERE file_name = #{fileName}
    </update>
</mapper>
```

```java
@Service
@Transactional
public class FileProcessingService {
    private final FileProcessingStatusRepository statusRepository;
    private final FileLockService fileLockService;

    public FileProcessingService(FileProcessingStatusRepository statusRepository, FileLockService fileLockService) {
        this.statusRepository = statusRepository;
        this.fileLockService = fileLockService;
    }

    public void processFile(File file) {
        String fileName = file.getName();
        if (!fileLockService.tryLock(file.getPath())) {
            throw new RuntimeException("Unable to acquire lock for file: " + fileName);
        }

        try {
            FileProcessingStatus status = statusRepository.findById(fileName)
                .orElse(new FileProcessingStatus(fileName, ProcessingStatus.PENDING, LocalDateTime.now()));

            if (status.getStatus() != ProcessingStatus.PENDING) {
                return; // File already processed or being processed
            }

            status.setStatus(ProcessingStatus.PROCESSING);
            status.setLastUpdated(LocalDateTime.now());
            statusRepository.save(status);

            // Process the file
            // ...

            status.setStatus(ProcessingStatus.COMPLETED);
            status.setLastUpdated(LocalDateTime.now());
            statusRepository.save(status);
        } catch (Exception e) {
            FileProcessingStatus status = statusRepository.findById(fileName)
                .orElseThrow(() -> new RuntimeException("Status not found for file: " + fileName));
            status.setStatus(ProcessingStatus.ERROR);
            status.setLastUpdated(LocalDateTime.now());
            statusRepository.save(status);
            throw e;
        } finally {
            fileLockService.releaseLock(file.getPath());
        }
    }
}
```

# Process flow & DLQ flow

## Process Flow

```java

@Configuration
 @EnableIntegration
 @Slf4j
 @RequiredArgsConstructor
@Profile("DmpFlow")
 public class DmpIntegrationConfig {

     private final PfpProperties pfpProperties;

     @Bean
     public ObjectMapper objectMapper() {
         ObjectMapper mapper = new ObjectMapper();
         mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
         return mapper;
     }

     @Bean
     public AtomicBoolean runOnce() {
         return new AtomicBoolean(false);
     }

     @Bean
     public CountDownLatch runOnceLatch() {
         return new CountDownLatch(1);
     }

     @Bean
     public MessageChannel startFilePollingChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel stopFilePollingChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel fileProcessingInputChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel pwsProcessingInputChannel() {
         return new DirectChannel();
     } // ToDo: change to multithreading

     @Bean
     public MessageChannel notificationInputChannel() {
         return new DirectChannel();
     }

     @Bean
     public MessageChannel fileBackupInputChannel() {
         return new DirectChannel();
     }

     @Bean(name = PollerMetadata.DEFAULT_POLLER)
     public PollerMetadata poller() {
         return Pollers.fixedDelay(Duration.ofMinutes(pfpProperties.getDmpAuthFilePolling())).getObject();
     }

     @Bean
     public FileReadingMessageSource fileReadingMessageSource() {
         FileReadingMessageSource source = new FileReadingMessageSource();
         source.setDirectory(new File(pfpProperties.getDmpAuthProcessingFilePath()));
         source.setFilter(new SimplePatternFileListFilter(pfpProperties.getDmpAuthProcessingFilePattern()));
         source.setUseWatchService(true);
         source.setWatchEvents(FileReadingMessageSource.WatchEventType.CREATE,
                 FileReadingMessageSource.WatchEventType.MODIFY);
         return source;
     }

     @Bean
     public IntegrationFlow fileProcessingFlow(ProcessingHandler processingHandler, ProcessingHandlerErrorAdvice advice) {
         return IntegrationFlow.from("fileProcessingInputChannel")
                 .handle(processingHandler, HandlerName.processAuthFile.name(), e -> e.advice(advice))
                 .channel("pwsProcessingInputChannel")
                 .handle(processingHandler, HandlerName.processPwsMessage.name(), e -> e.advice(advice))
                 .channel("notificationInputChannel")
                 .handle(processingHandler, HandlerName.sendNotification.name())
                 .channel("fileBackupInputChannel")
                 .handle(processingHandler, HandlerName.backupFile.name())
                 .get();
     }

 }
```

## DLQ flow

```xml
<dependency>
    <groupId>com.ibm.mq</groupId>
    <artifactId>mq-jms-spring-boot-starter</artifactId>
    <version>2.6.4</version>
</dependency>
```

```propertis
ibm.mq.queueManager=YOUR_QUEUE_MANAGER
ibm.mq.channel=YOUR_CHANNEL
ibm.mq.connName=YOUR_CONNECTION_NAME(1414)
ibm.mq.user=YOUR_USERNAME
ibm.mq.password=YOUR_PASSWORD
ibm.mq.queue=YOUR_DLQ_QUEUE_NAME
```

```java
@Configuration
public class MQConfig {
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public MessageChannel deadLetterChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow deadLetterFlow() {
        return IntegrationFlow.from("deadLetterChannel")
            .<Message<?>>handle((payload, headers) -> {
                // Log the error, potentially move the file to an error directory
                return null;
            })
            .get();
    }
}
```

```java
@Service
public class ErrorHandlingService {
    private final JmsTemplate jmsTemplate;

    public ErrorHandlingService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendToDeadLetterQueue(ErrorMessage errorMessage) {
        jmsTemplate.convertAndSend("YOUR_DLQ_QUEUE_NAME", errorMessage);
    }
}

@Bean
public IntegrationFlow fileProcessingFlow() {
    return IntegrationFlow.from("fileProcessingInputChannel")
        .handle(processingHandler, "processFile", e -> e
            .advice(retryAdvice())
            .handleMessageProcessingError(error -> error.sendTo("deadLetterChannel")))
        // Rest of your flow
        .get();
}
```

## Spring Batch Process

Key Considerations:

-   File Synchronization: To prevent multiple instances from processing the same CSV files, you can use a file locking mechanism or database coordination.
-   Transaction Consistency: Using Spring Batch’s transaction management, we can ensure that all steps of the batch are either fully processed or rolled back in case of failure.
-   Handling Multiple CSVs per Batch: You need to group the CSVs of a single batch (with the same prefix) and process them together.
-   Notification on Completion or Failure: You can use a listener to send a success or failure message to IBM MQ.
-   Concurrency: Running multiple batch processes on different nodes for different customers and batches should be controlled to prevent race conditions.

```text
src/main/java/com/yourcompany/batchprocessing/
├── BatchApplication.java
├── config/
│   ├── BatchConfig.java
│   ├── DatabaseConfig.java
│   └── MQConfig.java
├── model/
│   ├── PaymentTransaction.java
│   ├── PaymentInstruction.java
│   ├── PaymentPayee.java
│   ├── PaymentAdvice.java
│   └── PaymentCharge.java
├── mapper/
│   ├── PaymentTransactionMapper.java
│   ├── PaymentInstructionMapper.java
│   ├── PaymentPayeeMapper.java
│   ├── PaymentAdviceMapper.java
│   └── PaymentChargeMapper.java
├── processor/
│   ├── PaymentTransactionProcessor.java
│   ├── PaymentInstructionProcessor.java
│   ├── PaymentPayeeProcessor.java
│   ├── PaymentAdviceProcessor.java
│   └── PaymentChargeProcessor.java
├── reader/
│   └── MultiResourceItemReader.java
├── writer/
│   └── MyBatisBatchItemWriter.java
├── listener/
│   ├── JobCompletionNotificationListener.java
│   └── StepExecutionListener.java
└── service/
    ├── BatchStatusService.java
    └── NotificationService.java
```

```java
@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication implements CommandLineRunner {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importPaymentDataJob;

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String customerFolder = System.getProperty("customer.folder", "customer1");
        String inputPath = "/path/to/nas/" + customerFolder + "/";

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("inputPath", inputPath)
                .addDate("date", new Date())
                .toJobParameters();

        jobLauncher.run(importPaymentDataJob, jobParameters);
    }
}
```

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.size:1000}")
    private int chunkSize;

    @Bean
    public Job importPaymentDataJob(Step importPaymentTransactionsStep,
                                    Step importPaymentInstructionsStep,
                                    Step importPaymentPayeesStep,
                                    Step importPaymentAdvicesStep,
                                    Step importPaymentChargesStep,
                                    JobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("importPaymentDataJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(importPaymentTransactionsStep)
                .next(importPaymentInstructionsStep)
                .next(importPaymentPayeesStep)
                .next(importPaymentAdvicesStep)
                .next(importPaymentChargesStep)
                .end()
                .build();
    }

    @Bean
    public Step importPaymentTransactionsStep(MyBatisBatchItemWriter<PaymentTransaction> writer) {
        return stepBuilderFactory.get("importPaymentTransactionsStep")
                .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
                .reader(multiResourceItemReader("paymentTransactions"))
                .processor(new PaymentTransactionProcessor())
                .writer(writer)
                .listener(new StepExecutionListener())
                .build();
    }

    // Similar step beans for other payment types...

    @Bean
    @StepScope
    public MultiResourceItemReader<PaymentTransaction> multiResourceItemReader(@Value("#{jobParameters['inputPath']}") String inputPath) {
        return new MultiResourceItemReader<>(inputPath, "batch*_paymentTransactions.csv", new PaymentTransactionMapper());
    }

    // Similar MultiResourceItemReader beans for other payment types...
}
```

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.size:1000}")
    private int chunkSize;

    // Define the Job that coordinates the different steps
    @Bean
    public Job importPaymentDataJob(JobCompletionNotificationListener listener,
                                    Step importPaymentTransactionsStep,
                                    Step importPaymentInstructionsStep,
                                    Step importPaymentPayeesStep,
                                    Step importPaymentAdvicesStep,
                                    Step importPaymentChargesStep) {
        return jobBuilderFactory.get("importPaymentDataJob")
                .listener(listener)
                .start(importPaymentTransactionsStep(paymentTransactionWriter()))  // First Step
                .next(importPaymentInstructionsStep(paymentInstructionWriter()))    // Second Step
                .next(importPaymentPayeesStep(paymentPayeeWriter()))
                .end()
                .build();
    }

    // Define the steps for each CSV file processing
    @Bean
    public Step importPaymentTransactionsStep(MyBatisBatchItemWriter<PaymentTransaction> writer) {
        return stepBuilderFactory.get("importPaymentTransactionsStep")
                .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
                .reader(multiResourceItemReader("paymentTransactions"))
                .processor(new PaymentTransactionProcessor())
                .writer(writer)
                .listener(new StepExecutionListener())
                .build();
    }

    // Similarly define step beans for other payment files (instructions, payees, advices, charges)
    @Bean
    public Step importPaymentInstructionsStep(MyBatisBatchItemWriter<PaymentInstruction> writer) {
        return stepBuilderFactory.get("importPaymentInstructionsStep")
                .<PaymentInstruction, PaymentInstruction>chunk(chunkSize)
                .reader(multiResourceItemReader("paymentInstructions"))
                .processor(new PaymentInstructionProcessor())
                .writer(writer)
                .listener(new StepExecutionListener())
                .build();
    }

    // Add steps for payees, advices, charges...
}
```

```sql
CREATE TABLE csv_file_process_status (
    id NUMBER PRIMARY KEY,
    file_name VARCHAR2(255) UNIQUE,
    process_status VARCHAR2(50),
    lock_flag NUMBER(1),  -- 0: unlocked, 1: locked
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

```java
@Service
public class FileProcessingLockService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Acquire the lock for a file
    public boolean acquireLock(String fileName) {
        String sql = "UPDATE csv_file_process_status SET lock_flag = 1 WHERE file_name = ? AND lock_flag = 0";
        int rowsUpdated = jdbcTemplate.update(sql, fileName);
        return rowsUpdated > 0;  // If rowsUpdated > 0, the lock was successfully acquired
    }

    // Release the lock for a file
    public void releaseLock(String fileName) {
        String sql = "UPDATE csv_file_process_status SET lock_flag = 0 WHERE file_name = ?";
        jdbcTemplate.update(sql, fileName);
    }

    // Update the process status
    public void updateProcessStatus(String fileName, String status) {
        String sql = "UPDATE csv_file_process_status SET process_status = ?, last_updated = CURRENT_TIMESTAMP WHERE file_name = ?";
        jdbcTemplate.update(sql, status, fileName);
    }

    // Check if file is being processed
    public boolean isFileLocked(String fileName) {
        String sql = "SELECT lock_flag FROM csv_file_process_status WHERE file_name = ?";
        Integer lockFlag = jdbcTemplate.queryForObject(sql, new Object[]{fileName}, Integer.class);
        return lockFlag != null && lockFlag == 1;
    }
}

```

```java
@Bean
public Step importPaymentTransactionsStep(JdbcBatchItemWriter<PaymentTransaction> writer, FileProcessingLockService lockService) {
    return stepBuilderFactory.get("importPaymentTransactionsStep")
            .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
            .reader(paymentTransactionReader())
            .processor(new PaymentTransactionProcessor())
            .writer(new PaymentTransactionWriter())
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    String fileName = stepExecution.getJobParameters().getString("fileName");
                    if (!lockService.acquireLock(fileName)) {
                        throw new IllegalStateException("Unable to acquire lock for file: " + fileName);
                    }
                    lockService.updateProcessStatus(fileName, "IN_PROGRESS");
                }

                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    String fileName = stepExecution.getJobParameters().getString("fileName");
                    if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                        lockService.updateProcessStatus(fileName, "COMPLETED");
                    } else {
                        lockService.updateProcessStatus(fileName, "FAILED");
                    }
                    lockService.releaseLock(fileName);
                    return stepExecution.getExitStatus();
                }
            })
            .build();
}

```

```java
@Bean
@StepScope
public MultiResourceItemReader<PaymentTransaction> multiResourceItemReader(@Value("#{jobParameters['inputPath']}") String inputPath) {
    MultiResourceItemReader<PaymentTransaction> reader = new MultiResourceItemReader<>();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    try {
        Resource[] resources = resolver.getResources(inputPath + "/batch*_paymentTransactions.csv");
        reader.setResources(resources);
    } catch (IOException e) {
        throw new RuntimeException("Error reading CSV files", e);
    }

    reader.setDelegate(paymentTransactionReader()); // Define the CSV reader for PaymentTransaction
    return reader;
}

@Bean
@StepScope
public FlatFileItemReader<PaymentTransaction> paymentTransactionReader() {
    return new FlatFileItemReaderBuilder<PaymentTransaction>()
            .name("paymentTransactionReader")
            .delimited()
            .names(new String[] {"transactionId", "amount", "currency", "date"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<PaymentTransaction>() {{
                setTargetType(PaymentTransaction.class);
            }})
            .build();
}

// Similar reader configuration for instructions, payees, advices, charges
```

1. importPaymentTransactionsStep: Insert the payment transactions, store paymentTransactionId in the StepExecutionContext.
2. importPaymentInstructionsStep: Insert payment instructions, retrieve paymentTransactionId from the StepExecutionContext, and store paymentInstructionId in the StepExecutionContext.
3. importPaymentPayeesStep: Retrieve both paymentTransactionId and paymentInstructionId from the StepExecutionContext and use them for inserting payee information.

```java
public class PaymentTransactionProcessor implements ItemProcessor<PaymentTransaction, PaymentTransaction> {

    @Override
    public PaymentTransaction process(PaymentTransaction item) {
        // Any transformation logic for PaymentTransaction
        return item;
    }
}

public class PaymentInstructionWriter extends JdbcBatchItemWriter<PaymentInstruction> {

    @Override
    public void write(List<? extends PaymentInstruction> items) throws Exception {
        super.write(items);

        // Retrieve the StepExecution and StepExecutionContext
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

        // Assuming transactionId is already present in the context
        List<Long> transactionIds = (List<Long>) stepExecutionContext.get("transactionIds");

        // Store the generated instructionIds in the StepExecutionContext
        List<Long> instructionIds = items.stream()
                .map(PaymentInstruction::getInstructionId)  // Assuming getInstructionId is the method to get the generated ID
                .collect(Collectors.toList());

        // Save the instructionIds in the context
        stepExecutionContext.put("instructionIds", instructionIds);
    }
}

public class PaymentPayeeProcessor implements ItemProcessor<PaymentPayee, PaymentPayee> {

    @Override
    public PaymentPayee process(PaymentPayee item) throws Exception {
        // Retrieve StepExecutionContext
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

        // Get transactionIds and instructionIds from the context
        List<Long> transactionIds = (List<Long>) stepExecutionContext.get("transactionIds");
        List<Long> instructionIds = (List<Long>) stepExecutionContext.get("instructionIds");

        // Assign transactionId and instructionId to the Payee
        item.setTransactionId(transactionIds.get(0));  // Assuming single transaction scenario
        item.setInstructionId(instructionIds.get(0)); // Assign corresponding instructionId

        return item;
    }
}

public class PaymentPayeeWriter extends JdbcBatchItemWriter<PaymentPayee> {
    // No special changes required here, as the payees with their transactionId and instructionId
    // will be written directly to the database.
}

@Bean
public Step importPaymentPayeesStep(JdbcBatchItemWriter<PaymentPayee> writer) {
    return stepBuilderFactory.get("importPaymentPayeesStep")
            .<PaymentPayee, PaymentPayee>chunk(chunkSize)
            .reader(paymentPayeeReader())  // Custom reader to read payee data
            .processor(new PaymentPayeeProcessor())  // Processor retrieves IDs from context
            .writer(new PaymentPayeeWriter())  // Standard writer for payees
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    // Optionally add some logging or initialization logic
                }

                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    // Optionally handle post-step logic
                    return stepExecution.getExitStatus();
                }
            })
            .build();
}

```

```java
@Bean
public MyBatisBatchItemWriter<PaymentTransaction> paymentTransactionWriter(SqlSessionFactory sqlSessionFactory) {
    return new MyBatisBatchItemWriterBuilder<PaymentTransaction>()
            .sqlSessionFactory(sqlSessionFactory)
            .statementId("insertPaymentTransaction") // Define your MyBatis insert statement
            .build();
}

// Similarly, define writers for instructions, payees, advices, charges
```

```java
@Configuration
@MapperScan("com.yourcompany.batchprocessing.mapper")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        // Configure your Oracle DataSource
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean
    public MyBatisBatchItemWriter<PaymentTransaction> paymentTransactionWriter(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisBatchItemWriter<>(sqlSessionFactory, PaymentTransactionMapper.class, "insertPaymentTransaction");
    }

    // Similar writer beans for other payment types...
}
```

```java
@Configuration
@MapperScan("com.yourcompany.batchprocessing.mapper")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        // Configure your Oracle DataSource
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Bean
    public MyBatisBatchItemWriter<PaymentTransaction> paymentTransactionWriter(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisBatchItemWriter<>(sqlSessionFactory, PaymentTransactionMapper.class, "insertPaymentTransaction");
    }

    // Similar writer beans for other payment types...
}
```

```java
@Service
public class BatchStatusService {

    @Autowired
    private BatchStatusMapper batchStatusMapper;

    public void updateBatchStatus(String batchId, String status) {
        batchStatusMapper.updateBatchStatus(batchId, status);
    }
}
```

```java
@Service
public class NotificationService {

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendNotification(String message) {
        jmsTemplate.convertAndSend("NOTIFICATION_QUEUE", message);
    }
}
```

```java
@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    @Autowired
    private BatchStatusService batchStatusService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchStatusService.updateBatchStatus(jobExecution.getJobParameters().getString("inputPath"), "COMPLETED");
            notificationService.sendNotification("Batch processing completed successfully");
        } else {
            batchStatusService.updateBatchStatus(jobExecution.getJobParameters().getString("inputPath"), "FAILED");
            notificationService.sendNotification("Batch processing failed");
        }
    }
}
```

```java
public class StepExecutionListener implements org.springframework.batch.core.StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Log step start
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Log step completion
        return null;
    }
}
```

Camel:

```xml
<!-- Apache Camel dependencies -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-file-starter</artifactId>
</dependency>

<!-- Spring Batch dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

```java
@Configuration
public class CamelFilePollingRoute extends RouteBuilder {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importPaymentDataJob;

    @Autowired
    private DatabaseFileLockService fileLockService;

    @Value("${input.directory.path}")
    private String inputDirectory;

    @Override
    public void configure() throws Exception {
        from("file://" + inputDirectory + "?noop=true&include=batch.*_paymentTransactions.csv")
            .routeId("filePollingRoute")
            .process(this::lockAndLaunchJob)
            .log("Processing file ${header.CamelFileName}")
            .end();
    }

    private void lockAndLaunchJob(Exchange exchange) {
        File file = exchange.getIn().getBody(File.class);
        String fileName = file.getName();

        // Use DatabaseFileLockService to lock the file
        if (fileLockService.lockFile(fileName)) {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                    .addString("inputPath", file.getAbsolutePath())
                    .addLong("time", System.currentTimeMillis())  // Ensure unique job parameters
                    .toJobParameters();

                jobLauncher.run(importPaymentDataJob, jobParameters);
                fileLockService.unlockFile(fileName, "COMPLETED");
            } catch (JobExecutionException e) {
                fileLockService.unlockFile(fileName, "FAILED");
                exchange.setException(e);  // Handle exception in the route
            }
        } else {
            // Skip the file if it is already being processed
            log.info("File {} is already being processed or completed, skipping.", fileName);
        }
    }
}
```

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.size:1000}")
    private int chunkSize;

    @Bean
    public Job importPaymentDataJob(Step importPaymentTransactionsStep,
                                    Step importPaymentInstructionsStep,
                                    Step importPaymentPayeesStep,
                                    Step importPaymentAdvicesStep,
                                    Step importPaymentChargesStep) {
        return jobBuilderFactory.get("importPaymentDataJob")
                .incrementer(new RunIdIncrementer())
                .flow(importPaymentTransactionsStep)
                .next(importPaymentInstructionsStep)
                .next(importPaymentPayeesStep)
                .next(importPaymentAdvicesStep)
                .next(importPaymentChargesStep)
                .end()
                .build();
    }

    @Bean
    public Step importPaymentTransactionsStep(JdbcBatchItemWriter<PaymentTransaction> writer) {
        return stepBuilderFactory.get("importPaymentTransactionsStep")
                .<PaymentTransaction, PaymentTransaction>chunk(chunkSize)
                .reader(paymentTransactionReader(null))  // Use the CSV reader
                .processor(new PaymentTransactionProcessor()) // Custom processor
                .writer(writer)  // Custom writer
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PaymentTransaction> paymentTransactionReader(
            @Value("#{jobParameters['inputPath']}") String inputPath) {
        return new FlatFileItemReaderBuilder<PaymentTransaction>()
                .name("paymentTransactionReader")
                .resource(new FileSystemResource(inputPath))
                .delimited()
                .names(new String[]{"column1", "column2", "column3"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(PaymentTransaction.class);
                }})
                .build();
    }
}
```

Large-Volume Data Insertion Optimization: For large-volume data insertion (e.g., in importPaymentInstructionsStep, importPaymentPayeesStep), Spring Batch provides several mechanisms to optimize data processing and reduce resource contention:

Chunk-based processing: Spring Batch processes data in chunks to optimize memory usage.
Batch Inserts: Spring Batch can batch the database inserts to reduce round trips between the application and the database.
Paging: Use paging techniques when reading large datasets.
Parallel Processing: You can split data across multiple threads or nodes for faster processing.
Partitioning: This allows splitting the data into partitions, where each partition can be processed independently on different threads or nodes.

1. Database Lock (Avoiding File Lock)
   Spring Batch uses the JobRepository to coordinate job executions across multiple instances and nodes. Ensure that the JobRepository is properly configured to use your Oracle database. Spring Batch handles locking at the job level using this repository, so you don’t need to manually implement file locking or database locks.

```java
@Configuration
public class BatchConfig {

    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionManager(transactionManager);
        factoryBean.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED"); // Adjust as necessary
        factoryBean.setDatabaseType("ORACLE"); // Specify Oracle as the database type
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

2. Large-Volume Data Insertion (Optimizing importPaymentInstructionsStep and importPaymentPayeesStep)
   Here’s how you can optimize your large-volume data insertion steps:

A. Chunk-based Processing and Batch Inserts
You can use chunk-based processing with a large chunk size to process multiple records in a single transaction and perform batch inserts. Below is an example of how you can configure chunk-based processing for importPaymentInstructionsStep.

```java
@Bean
public Step importPaymentInstructionsStep(JdbcBatchItemWriter<PaymentInstruction> writer) {
    return stepBuilderFactory.get("importPaymentInstructionsStep")
            .<PaymentInstruction, PaymentInstruction>chunk(1000)  // Define a suitable chunk size
            .reader(paymentInstructionReader(null))
            .processor(new PaymentInstructionProcessor())
            .writer(writer)
            .build();
}

@Bean
@StepScope
public FlatFileItemReader<PaymentInstruction> paymentInstructionReader(
        @Value("#{jobParameters['inputPath']}") String inputPath) {
    return new FlatFileItemReaderBuilder<PaymentInstruction>()
            .name("paymentInstructionReader")
            .resource(new FileSystemResource(inputPath))
            .delimited()
            .names(new String[]{"transactionId", "instructionData", "otherColumns"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(PaymentInstruction.class);
            }})
            .build();
}

@Bean
public JdbcBatchItemWriter<PaymentInstruction> paymentInstructionWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<PaymentInstruction>()
            .dataSource(dataSource)
            .sql("INSERT INTO payment_instruction (transaction_id, instruction_data, other_columns) "
                    + "VALUES (:transactionId, :instructionData, :otherColumns)")
            .beanMapped()
            .build();
}
```

Chunk Size: The chunk(1000) defines that 1000 rows will be processed per transaction. You can adjust this value based on your memory and database performance.
Batch Inserts: JdbcBatchItemWriter automatically batches SQL inserts for efficiency.
B. Parallel Processing with Task Executors
You can speed up processing by running multiple threads for each step. Spring Batch provides a TaskExecutor to execute steps in parallel.

```java
@Bean
public Step importPaymentInstructionsStep(JdbcBatchItemWriter<PaymentInstruction> writer, TaskExecutor taskExecutor) {
    return stepBuilderFactory.get("importPaymentInstructionsStep")
            .<PaymentInstruction, PaymentInstruction>chunk(1000)
            .reader(paymentInstructionReader(null))
            .processor(new PaymentInstructionProcessor())
            .writer(writer)
            .taskExecutor(taskExecutor)  // Enable parallel execution
            .throttleLimit(10)  // Limit the number of concurrent threads
            .build();
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("batch-thread-");
    executor.initialize();
    return executor;
}
```

TaskExecutor: This allows the step to run with multiple threads, speeding up data processing.
Throttle Limit: Defines the maximum number of concurrent threads. Adjust this according to your system's capacity.
C. Partitioning for Large Data Volumes
Spring Batch allows partitioning, where you can split the data into multiple partitions, each processed by a separate thread or even a separate JVM.

```java
@Bean
public Step masterStep(Step slaveStep) {
    return stepBuilderFactory.get("masterStep")
            .partitioner(slaveStep.getName(), new MultiResourcePartitioner())
            .step(slaveStep)
            .taskExecutor(taskExecutor())  // Parallel processing
            .build();
}

@Bean
public Step slaveStep(JdbcBatchItemWriter<PaymentInstruction> writer) {
    return stepBuilderFactory.get("slaveStep")
            .<PaymentInstruction, PaymentInstruction>chunk(1000)
            .reader(paymentInstructionReader(null))
            .processor(new PaymentInstructionProcessor())
            .writer(writer)
            .build();
}

@Bean
@StepScope
public MultiResourcePartitioner partitioner(@Value("#{jobParameters['inputPath']}") String inputPath) {
    MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
    partitioner.setResources(new FileSystemResource(inputPath).getFile().listFiles());  // Partition by file
    return partitioner;
}
```

Master-Slave Step: The master step divides the data into partitions and delegates the work to slave steps.
Partitioner: Here, files are partitioned, and each partition is processed in parallel. You can also partition data by ranges (e.g., ranges of IDs).

Summary:
Database Locking: Spring Batch's JobRepository ensures that jobs are locked across distributed nodes. You don't need file locks, and jobs are coordinated using Oracle database tables (BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION).

Optimizing Large-Volume Data Insertions:

Chunk-based processing ensures memory efficiency and batches data inserts.
Task Executors allow parallel processing of steps for faster data insertion.
Partitioning divides the data into smaller chunks, each processed independently, and can be run on different threads or even nodes.
By combining these techniques, you can scale up your batch processing and handle large CSV files more efficiently.

# Spring Batch

```bash
// run.sh
#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <flow> <stage>"
    echo "Stages: pre-db, db-insert, post-db"
    exit 1
fi

stage=$1

case $stage in
    pre-db|db-batch|post-db)
        java -jar -Dspring.profiles.active=$stage payment-processor.jar $stage
        ;;
    *)
        echo "Invalid stage. Use pre-db, db-insert, or post-db."
        exit 1
        ;;
esac
```

```sql
-- SQL schema for payment_file_status table
CREATE TABLE payment_file_status (
    id NUMBER GENERATED ALWAYS AS IDENTITY,
    file_name VARCHAR2(255) NOT NULL UNIQUE,
    flow VARCHAR2(100) NOT NULL,
    stage VARCHAR2(50) NOT NULL
    status VARCHAR2(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    error_message VARCHAR2(4000),
    csv_file_name VARCHAR2(255),
    CONSTRAINT pk_payment_file_status PRIMARY KEY (id)
);
```

```java
public enum Stage {
    PRE_DB,
    DB_INSERT,
    POST_DB
}
```

```java
package com.example.payment;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

@SpringBootApplication
public class PaymentProcessingApplication {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        String env = System.getProperty("env", "dev");
        String flow = System.getProperty("flow", "flow1");
        String configPath = System.getProperty("config.path", "/etc/myapp/config");

        SpringApplication app = new SpringApplication(PaymentProcessingApplication.class);
        app.setAdditionalProfiles(env);
        app.setDefaultProperties(Map.of(
            "spring.config.additional-location", configPath + "/",
            "flow", flow
        ));
        app.run(args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            if (args.length < 1) {
                System.out.println("Usage: java -jar payment-processor.jar <stage>");
                return;
            }

            String flow = context.getEnvironment().getProperty("flow");
            Stage stage = Stage.valueOf(args[0].toUpperCase());

            JobParameters params = new JobParametersBuilder()
                    .addString("flow", flow)
                    .addString("stage", stage.name())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            String jobName = stage.name().toLowerCase() + "Job" + flow;
            Job job = (Job) context.getBean(jobName);
            jobLauncher.run(job, params);
        };
    }

    @Bean
    public CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                // Configure Camel context before start if needed
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // Add shutdown hook to remove Camel file locks
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        System.out.println("Removing Camel file locks...");
                        try {
                            camelContext.getRoutes().forEach(route -> {
                                if (route.getEndpoint() instanceof org.apache.camel.component.file.GenericFileEndpoint) {
                                    org.apache.camel.component.file.GenericFileEndpoint endpoint =
                                        (org.apache.camel.component.file.GenericFileEndpoint) route.getEndpoint();
                                    endpoint.getGenericFileOperations().releaseAllLocksForPath(endpoint.getConfiguration().getDirectory());
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Error removing Camel file locks: " + e.getMessage());
                        }
                    }
                });
            }
        };
    }
}
```

```java
package com.example.payment.config;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.payment.service.PaymentFileStatusService;
import com.example.payment.Stage;

import javax.sql.DataSource;

@Configuration
public class CamelConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentFileStatusService statusService;

    @Value("${stage}")
    private Stage stage;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Bean
    public JdbcMessageIdRepository jdbcMessageIdRepo() {
        return new JdbcMessageIdRepository(dataSource, "PROCESSED_FILES");
    }

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("direct:errorHandler")
                    .useOriginalMessage()
                    .maximumRedeliveries(3)
                    .redeliveryDelay(1000)
                    .backOffMultiplier(2)
                    .retryAttemptedLogLevel(LoggingLevel.WARN));

                switch (stage) {
                    case PRE_DB:
                        configurePreDbStage();
                        break;
                    case DB_INSERT:
                        configureDbInsertStage();
                        break;
                    case POST_DB:
                        configurePostDbStage();
                        break;
                }

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .process(this::handleProcessingError);
            }

            private void configurePreDbStage() {
                from("file:{{input.directory}}?include={{file.pattern}}&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("preDbInsertionRoute")
                    .log("Pre-DB Insertion: ${header.CamelFileName}")
                    .process(this::initializeFileStatus)
                    .to("spring-batch:preDbJob" + activeProfile)
                    .process(this::completePreDbProcessing)
                    .to("file:{{output.directory}}");
            }

            private void configureDbInsertStage() {
                from("file:{{db.input.directory}}?include=*.csv&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("dbInsertionRoute")
                    .log("DB Insertion: ${header.CamelFileName}")
                    .process(this::updateStatusToDbInserting)
                    .to("spring-batch:dbInsertJob" + activeProfile)
                    .process(this::completeDbInsertion)
                    .to("file:{{db.output.directory}}");
            }

            private void configurePostDbStage() {
                from("file:{{post.input.directory}}?include=*.txt&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("postDbProcessingRoute")
                    .log("Post-DB Processing: ${header.CamelFileName}")
                    .process(this::updateStatusToPostDbProcessing)
                    .to("spring-batch:postDbJob" + activeProfile)
                    .process(this::completePostDbProcessing);
            }

            // ... (status update methods remain the same)
        };
    }
}
```

```java
@Configuration
public class CamelConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentFileStatusService statusService;

    @Value("${stage}")
    private Stage stage;

    @Value("${flow}")
    private String flow;

    @Bean
    public JdbcMessageIdRepository jdbcMessageIdRepo() {
        return new JdbcMessageIdRepository(dataSource, "PROCESSED_FILES");
    }

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("direct:errorHandler")
                    .useOriginalMessage()
                    .maximumRedeliveries(3)
                    .redeliveryDelay(1000)
                    .backOffMultiplier(2)
                    .retryAttemptedLogLevel(LoggingLevel.WARN));

                switch (stage) {
                    case PRE_DB:
                        configurePreDbStage();
                        break;
                    case DB_INSERT:
                        configureDbInsertStage();
                        break;
                    case POST_DB:
                        configurePostDbStage();
                        break;
                }

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .process(this::handleProcessingError);
            }

            private void configurePreDbStage() {
                from("file:{{" + flow + ".input.directory}}?include={{" + flow + ".file.pattern}}&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("preDbInsertionRoute")
                    .log("Pre-DB Insertion: ${header.CamelFileName}")
                    .process(this::initializeFileStatus)
                    .to("spring-batch:preDbJob" + flow)
                    .process(this::completePreDbProcessing)
                    .to("file:{{" + flow + ".output.directory}}");
            }

            private void configureDbInsertStage() {
                from("file:{{db.input.directory}}?include=*.csv&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("dbInsertionRoute")
                    .log("DB Insertion: ${header.CamelFileName}")
                    .process(this::updateStatusToDbInserting)
                    .to("spring-batch:dbInsertionJob")
                    .process(this::completeDbInsertion)
                    .to("file:{{db.output.directory}}");
            }

            private void configurePostDbStage() {
                from("file:{{post.input.directory}}?include=*.txt&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
                    .routeId("postDbProcessingRoute")
                    .log("Post-DB Processing: ${header.CamelFileName}")
                    .process(this::updateStatusToPostDbProcessing)
                    .to("spring-batch:postDbJob")
                    .process(this::completePostDbProcessing);
            }

            private void completePreDbProcessing(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.PRE_DB, "COMPLETED");
            }

            private void updateStatusToDbInserting(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.DB_INSERT, "PROCESSING");
            }

            private void completeDbInsertion(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.DB_INSERT, "COMPLETED");
            }

            private void updateStatusToPostDbProcessing(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.POST_DB, "PROCESSING");
            }

            private void completePostDbProcessing(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatus(fileName, Stage.POST_DB, "COMPLETED");
            }

            private void handleProcessingError(Exchange exchange) {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                statusService.updateStatusToError(fileName, stage, cause.getMessage());
            }
        };
    }
}
```

```java
package com.example.payment.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import com.example.payment.tasklet.*;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("spring_batch");
    }

    // Common DB Insertion Job
    @Bean
    public Job dbInsertionJob(
            Step insertPaymentTransactionStep,
            Step insertPaymentBulkInformationStep,
            Step insertPaymentInstructionStep) {
        return jobBuilderFactory.get("dbInsertionJob")
                .start(insertPaymentTransactionStep)
                .next(insertPaymentBulkInformationStep)
                .next(insertPaymentInstructionStep)
                .build();
    }

    @Bean
    public Step insertPaymentTransactionStep(InsertPaymentTransactionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentTransactionStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentBulkInformationStep(InsertPaymentBulkInformationTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentBulkInformationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentInstructionStep(InsertPaymentInstructionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentInstructionStep")
                .tasklet(tasklet)
                .build();
    }

    // Common Post-DB Job
    @Bean
    public Job postDbJob(Step sendNotificationStep, Step archiveFileStep) {
        return jobBuilderFactory.get("postDbJob")
                .start(sendNotificationStep)
                .next(archiveFileStep)
                .build();
    }

    @Bean
    public Step sendNotificationStep(SendNotificationTasklet tasklet) {
        return stepBuilderFactory.get("sendNotificationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step archiveFileStep(ArchiveFileTasklet tasklet) {
        return stepBuilderFactory.get("archiveFileStep")
                .tasklet(tasklet)
                .build();
    }
}
```

```java
package com.example.payment.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import com.example.payment.tasklet.*;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("spring_batch");
    }

    // Common DB Insertion Job
    @Bean
    public Job dbInsertionJob(
            Step insertPaymentTransactionStep,
            Step insertPaymentBulkInformationStep,
            Step insertPaymentInstructionStep) {
        return jobBuilderFactory.get("dbInsertionJob")
                .start(insertPaymentTransactionStep)
                .next(insertPaymentBulkInformationStep)
                .next(insertPaymentInstructionStep)
                .build();
    }

    @Bean
    public Step insertPaymentTransactionStep(InsertPaymentTransactionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentTransactionStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentBulkInformationStep(InsertPaymentBulkInformationTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentBulkInformationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step insertPaymentInstructionStep(InsertPaymentInstructionTasklet tasklet) {
        return stepBuilderFactory.get("insertPaymentInstructionStep")
                .tasklet(tasklet)
                .build();
    }

    // Common Post-DB Job
    @Bean
    public Job postDbJob(Step sendNotificationStep, Step archiveFileStep) {
        return jobBuilderFactory.get("postDbJob")
                .start(sendNotificationStep)
                .next(archiveFileStep)
                .build();
    }

    @Bean
    public Step sendNotificationStep(SendNotificationTasklet tasklet) {
        return stepBuilderFactory.get("sendNotificationStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step archiveFileStep(ArchiveFileTasklet tasklet) {
        return stepBuilderFactory.get("archiveFileStep")
                .tasklet(tasklet)
                .build();
    }
}
```

```java
@Configuration
@ConditionalOnProperty(name = "flow", havingValue = "flow1")
public class BatchConfigFlow1 {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job preDbJobFlow1(Step validateFlow1Step, Step enrichFlow1Step) {
        return jobBuilderFactory.get("preDbJobFlow1")
                .start(validateFlow1Step)
                .next(enrichFlow1Step)
                .build();
    }

    @Bean
    public Step validateFlow1Step(ValidateFlow1Tasklet tasklet) {
        return stepBuilderFactory.get("validateFlow1Step")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step enrichFlow1Step(EnrichFlow1Tasklet tasklet) {
        return stepBuilderFactory.get("enrichFlow1Step")
                .tasklet(tasklet)
                .build();
    }
}

@Configuration
@ConditionalOnProperty(name = "flow", havingValue = "flow2")
public class BatchConfigFlow2 {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job preDbJobFlow2(Step validateFlow2Step, Step enrichFlow2Step) {
        return jobBuilderFactory.get("preDbJobFlow2")
                .start(validateFlow2Step)
                .next(enrichFlow2Step)
                .build();
    }

    @Bean
    public Step validateFlow2Step(ValidateFlow2Tasklet tasklet) {
        return stepBuilderFactory.get("validateFlow2Step")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step enrichFlow2Step(EnrichFlow2Tasklet tasklet) {
        return stepBuilderFactory.get("enrichFlow2Step")
                .tasklet(tasklet)
                .build();
    }
}
```

```java
@Component
public class InsertPaymentTransactionTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to insert payment transaction
        return RepeatStatus.FINISHED;
    }
}

@Component
public class InsertPaymentBulkInformationTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to insert payment bulk information
        return RepeatStatus.FINISHED;
    }
}

@Component
public class InsertPaymentInstructionTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to insert payment instruction
        return RepeatStatus.FINISHED;
    }
}

@Component
public class SendNotificationTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to send notification
        return RepeatStatus.FINISHED;
    }
}

@Component
public class ArchiveFileTasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement logic to archive file
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow1")
public class ValidateFlow1Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow1-specific validation logic
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow1")
public class EnrichFlow1Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow1-specific enrichment logic
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow2")
public class ValidateFlow2Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow2-specific validation logic
        return RepeatStatus.FINISHED;
    }
}

@Component
@ConditionalOnProperty(name = "flow", havingValue = "flow2")
public class EnrichFlow2Tasklet implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Implement Flow2-specific enrichment logic
        return RepeatStatus.FINISHED;
    }
}
```

Key points in this implementation:

File Locking and Idempotent Processing:

We use readLock=idempotent along with idempotent=true and idempotentRepository=#jdbcMessageIdRepo.
This ensures that each file is processed only once and remains locked during processing.

Lock Management:

readLockRemoveOnCommit=true: Releases the lock when processing completes successfully.
readLockRemoveOnRollback=true: Releases the lock if an error occurs during processing.

Status Tracking:

We use processor methods to update the file status in the database at different stages of processing.

Error Handling:

The onException clause handles any errors during processing, updating the file status accordingly.

Spring Batch Integration:

The to("spring-batch:preDbInsertionJob") step triggers the Spring Batch job.
The Camel route handles the overall flow, including pre and post-processing steps.

Benefits of this approach:

Simplified Locking: We rely on Camel's built-in locking mechanism, which covers both file access and processing uniqueness.
Reduced Complexity: By eliminating the separate database lock for batch jobs, we simplify our code and reduce potential points of failure.
Consistent Status Tracking: File status is updated within the Camel route, providing a clear picture of the processing lifecycle.
Automatic Lock Release: Locks are automatically released on both successful completion and errors, reducing the risk of stuck locks.

This integration provides several benefits:

Separation of Concerns: Camel handles file monitoring and routing, while Spring Batch manages the complex data processing tasks.

-   Scalability: Multiple instances of the application can run on different nodes, each polling for files and triggering jobs as needed.
-   Flexibility: It's easy to add new routes for different file types or customers, and to trigger different jobs based on file characteristics.
-   Reliability: The use of file locks and idempotent repositories (configured elsewhere) ensures that files are processed exactly once, even in distributed environments.

To summarize, Camel acts as the orchestrator, detecting new files and triggering the appropriate Spring Batch jobs. This separation allows for efficient file handling (Camel's strength) combined with robust, scalable data processing (Spring Batch's strength).

# DLQ

This implementation provides the following benefits:

Files that fail processing multiple times are moved to a dead letter queue.
The system keeps track of retry attempts for each file.
Successfully processed files have their retry count reset.
A monitoring service regularly checks the DLQ and notifies operations if there are files needing attention.
A REST endpoint allows for manual reprocessing of files in the DLQ.

To further improve this system:

Implement more detailed logging for files moved to the DLQ.
Create a user interface for managing the DLQ, showing file details and processing history.
Implement analytics to identify patterns in files that end up in the DLQ.
Add the ability to automatically reprocess DLQ files during off-peak hours.

This DLQ implementation enhances your system's reliability by preventing problematic files from blocking the entire process while still allowing for their eventual processing through manual intervention.

1.First, let's add a new configuration for the dead letter queue:

```java
@Configuration
public class DlqConfig {

    @Value("${dlq.retry.count}")
    private int retryCount;

    @Value("${dlq.directory}")
    private String dlqDirectory;

    @Bean
    public DeadLetterQueueHandler dlqHandler() {
        return new DeadLetterQueueHandler(retryCount, dlqDirectory);
    }
}
```

2. Next, create a DeadLetterQueueHandler class:

```java
@Component
public class DeadLetterQueueHandler {

    private final int retryCount;
    private final String dlqDirectory;
    private final Map<String, Integer> fileRetryCount = new ConcurrentHashMap<>();

    public DeadLetterQueueHandler(int retryCount, String dlqDirectory) {
        this.retryCount = retryCount;
        this.dlqDirectory = dlqDirectory;
    }

    public boolean shouldMoveToDeadLetterQueue(String fileName) {
        int count = fileRetryCount.compute(fileName, (k, v) -> (v == null) ? 1 : v + 1);
        return count > retryCount;
    }

    public void moveToDeadLetterQueue(File file) throws IOException {
        File dlqFile = new File(dlqDirectory, file.getName());
        Files.move(file.toPath(), dlqFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        fileRetryCount.remove(file.getName());
    }

    public void resetRetryCount(String fileName) {
        fileRetryCount.remove(fileName);
    }
}
```

3. Update the CamelConfig to use the DeadLetterQueueHandler:

```java
@Configuration
public class CamelConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentFileStatusService statusService;

    @Autowired
    private DeadLetterQueueHandler dlqHandler;

    // ... other fields and methods

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException(Exception.class)
                    .handled(true)
                    .process(this::handleProcessingError)
                    .choice()
                        .when(method(dlqHandler, "shouldMoveToDeadLetterQueue(${header.CamelFileName})"))
                            .process(exchange -> {
                                File file = exchange.getIn().getBody(File.class);
                                dlqHandler.moveToDeadLetterQueue(file);
                                statusService.updateStatus(file.getName(), Stage.ERROR, "Moved to DLQ after multiple failures");
                            })
                        .otherwise()
                            .to("direct:${header.CamelFileNameOnly}")
                    .end();

                // ... rest of the route configurations

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .process(this::handleProcessingError);
            }

            private void handleProcessingError(Exchange exchange) {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                statusService.updateStatusToError(fileName, exchange.getFromRouteId(), cause.getMessage());
            }

            // ... other methods
        };
    }
}
```

4. Modify each stage's route to handle retries and DLQ:

```java
private void configurePreDbStage() {
    from("file:{{" + flow + ".input.directory}}?include={{" + flow + ".file.pattern}}&readLock=idempotent&idempotent=true&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true&readLockRemoveOnRollback=true")
        .routeId("preDbInsertionRoute")
        .log("Pre-DB Insertion: ${header.CamelFileName}")
        .process(this::initializeFileStatus)
        .doTry()
            .to("spring-batch:preDbJob" + flow)
            .process(this::completePreDbProcessing)
            .to("file:{{" + flow + ".output.directory}}")
            .process(exchange -> dlqHandler.resetRetryCount(exchange.getIn().getHeader("CamelFileName", String.class)))
        .doCatch(Exception.class)
            .to("direct:${header.CamelFileNameOnly}")
        .end();
}
```

5.

```properties
dlq.retry.count=3
dlq.directory=/path/to/dlq/directory
```

6. Create a DLQ monitoring service:

```java
@Service
public class DlqMonitoringService {

    private final String dlqDirectory;

    public DlqMonitoringService(@Value("${dlq.directory}") String dlqDirectory) {
        this.dlqDirectory = dlqDirectory;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void monitorDlq() {
        File dlqDir = new File(dlqDirectory);
        File[] dlqFiles = dlqDir.listFiles();
        if (dlqFiles != null && dlqFiles.length > 0) {
            // Send notification to operations team
            sendNotification("Files in DLQ: " + dlqFiles.length);
        }
    }

    private void sendNotification(String message) {
        // Implement notification logic (e.g., email, Slack message, etc.)
    }
}
```

7. Add a controller for manual intervention:

```java
@RestController
@RequestMapping("/dlq")
public class DlqController {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private PaymentFileStatusService statusService;

    @PostMapping("/retry/{fileName}")
    public ResponseEntity<String> retryFile(@PathVariable String fileName) throws Exception {
        File dlqFile = new File(dlqDirectory, fileName);
        if (!dlqFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Move file back to the input directory
        File inputFile = new File(inputDirectory, fileName);
        Files.move(dlqFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Reset file status
        statusService.resetStatus(fileName);

        // Trigger processing
        camelContext.getRouteController().startRoute("preDbInsertionRoute");

        return ResponseEntity.ok("File queued for reprocessing");
    }
}
```

```java
private File fileTransformer(String fileName, String localDirectory, Document document)
            throws TransformerException {
        DOMSource dom = new DOMSource(document);
        Transformer transformer = newDefaultInstance().newTransformer();
        File signatureFile = archiveFileUtils.fileWithDirectory(localDirectory, fileName);
        StreamResult result = new StreamResult(signatureFile);
        transformer.transform(dom, result);
        return signatureFile;
    }
```

# Multilingual File Name for File Upload/Download

Ensure your Spring Boot application is configured to use UTF-8 encoding globally:
```yml
server:
  servlet:
    encoding:
      charset: UTF-8
      force: true
```

Invalid Character Handling: Instead of trying to transliterate or remove non-Latin characters, we now only replace characters that are typically invalid in most file systems (like /, , :, *, ?, ", <, >, |).

Length Limitation: We still trim the filename to a maximum length, but we do so carefully to avoid cutting in the middle of a multi-byte character (which is common in Asian scripts).


File System Considerations: Ensure your Linux file system is configured to use UTF-8. Most modern Linux distributions do this by default. For the ext4 file system (commonly used in Linux), file names are stored as byte sequences, which works well with UTF-8 encoded Asian characters.


Database Storage: Make sure your Oracle database columns for storing file names use a character set that supports Asian languages, such as AL32UTF8 (which is Oracle's implementation of UTF-8).

```java
import org.springframework.stereotype.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class FileUtil {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    public String sanitizeFilename(String filename) {
        // Replace invalid characters with underscore
        String sanitized = INVALID_CHARS.matcher(filename).replaceAll("_");
        
        // Trim to max length, being careful not to cut in the middle of a multi-byte character
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            byte[] bytes = sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length > MAX_FILENAME_LENGTH) {
                sanitized = new String(bytes, 0, MAX_FILENAME_LENGTH, java.nio.charset.StandardCharsets.UTF_8);
                // Ensure we don't end with an incomplete multi-byte character
                while (!sanitized.isEmpty() && !Character.isValidCodePoint(sanitized.charAt(sanitized.length() - 1))) {
                    sanitized = sanitized.substring(0, sanitized.length() - 1);
                }
            }
        }
        
        return sanitized.isEmpty() ? "file" : sanitized;
    }

    public String getUniqueFilename(Path directory, String filename) throws IOException {
        String baseName = filename;
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        }

        Path filePath = directory.resolve(filename);
        int counter = 1;

        while (Files.exists(filePath)) {
            String newName = String.format("%s(%d)%s", baseName, counter++, extension);
            filePath = directory.resolve(newName);
        }

        return filePath.getFileName().toString();
    }

    public String storeFile(Path directory, String originalFilename, byte[] content) throws IOException {
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String uniqueFilename = getUniqueFilename(directory, sanitizedFilename);
        
        Path filePath = directory.resolve(uniqueFilename);
        Files.write(filePath, content);
        
        return uniqueFilename;
    }
}
```

When serving files for download, use UTF-8 encoding for the file name in the Content-Disposition header:
```java
@GetMapping("/download/{storedFilename}")
public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String storedFilename) throws IOException {
    byte[] data = fileService.getFile(storedFilename);
    String originalFilename = fileService.getOriginalFilename(storedFilename);
    ByteArrayResource resource = new ByteArrayResource(data);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment;filename*=UTF-8''" + URLEncoder.encode(originalFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20"))
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(data.length)
        .body(resource);
}
```
