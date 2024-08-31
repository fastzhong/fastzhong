---
title: '☕️ CEW'
date: 2001-01-01
robots: 'noindex,nofollow'
xml: false
---

<!--more-->

## RuleTemplate

```java
public interface DecisionMatrixRow {

    Long getId();

    Map<String, String> getFieldConditionColumns();

    Map<String, String> getBusinessConditionColumns();

    Map<String, String> getFieldActionColumns();

    Map<String, String> getBusinessActionColumns();

}
```

```java
public class GenericRuleTemplate implements RuleTemplate {

    @Override
    public String generateRule(DecisionMatrixRow row) {

        StringBuilder rule = new StringBuilder();
        rule.append("rule \"Rule_").append(row.getId()).append("\"\n");
        rule.append("when\n");
        rule.append("    $entity : Entity(");

        // Add field conditions
        Map<String, String> fieldConditionCols = row.getFieldConditionColumns();
        final AtomicBoolean firstCondition = new AtomicBoolean(true);
        fieldConditionCols.keySet().forEach(k -> {
           if (StringUtils.isNotEmpty(fieldConditionCols.get(k))) {
               if (!firstCondition.get()) {
                   rule.append(", ");
               }
               rule.append(k).append(" ").append(fieldConditionCols.get(k));
               firstCondition.set(false);
           }
        });

        // Add business rule condition
        Map<String, String> businessConditionCols = row.getBusinessConditionColumns();
        firstCondition.set(true);
        businessConditionCols.keySet().forEach(k -> {
            if (StringUtils.isNotEmpty(businessConditionCols.get(k))) {
                if (!firstCondition.get()) {
                    rule.append(", ");
                }
                rule.append(businessConditionCols.get(k)).append(", ");
                firstCondition.set(false);
            }
        });

        rule.append(")\n");
        rule.append("then\n");

        // Add field actions
        Map<String, String> fieldActionCols = row.getFieldActionColumns();
        fieldActionCols.keySet().forEach(k -> {
            if (StringUtils.isNotEmpty(fieldActionCols.get(k))) {
                rule.append("    $entity.set").append(k)
                        .append("(").append(fieldActionCols.get(k)).append(");\n");
            }
        });

        // Add generic actions
        Map<String, String> businessActionCols = row.getBusinessActionColumns();
        businessActionCols.keySet().forEach(k -> {
            if (StringUtils.isNotEmpty(businessActionCols.get(k))) {
                rule.append(businessActionCols.get(k)).append("\n");
            }
        });

        rule.append("end\n");
        return rule.toString();
    }

}
```

```java
public static String columnToProperty(String columnName) {
    if (columnName == null || columnName.isEmpty()) {
        return columnName;
    }

    StringBuilder result = new StringBuilder();
    boolean nextUpperCase = false;
    
    for (int i = 0; i < columnName.length(); i++) {
        char c = columnName.charAt(i);
        if (c == '_') {
            nextUpperCase = true;
        } else {
            if (nextUpperCase) {
                result.append(Character.toUpperCase(c));
                nextUpperCase = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
    }

    return result.toString();
}
```

```txt
rule "Compute TotalTransferAmount"
when
    $payments : List(size > 0) from collect(PaymentDto(creditorAccountNo == $creditorAccountNo, valueDate == $valueDate))
then
    BigDecimal totalAmount = $payments.stream()
                                       .map(PaymentDto::getPaymentAmount)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add);

    for (PaymentDto payment : $payments) {
        payment.setTotalTransferAmount(totalAmount);
    }
end

rule "Compute BulkAmount and BulkSize"
when
    $payments : List(size > 0) from collect(PaymentDto(splittingKey == $splittingKey))
then
    BigDecimal bulkAmount = $payments.stream()
                                      .map(PaymentDto::getPaymentAmount)
                                      .reduce(BigDecimal.ZERO, BigDecimal::add);

    int bulkSize = $payments.size();

    for (PaymentDto payment : $payments) {
        payment.setBulkAmount(bulkAmount);
        payment.setBulkSize(bulkSize);
    }
end

```

# Bulk Insert & Optimization 

Key points:

-    We're using ExecutorType.BATCH to enable JDBC batching.
-    We flush and clear the session every BATCH_SIZE (1000 in this example) inserts to manage memory usage.
-    The @Transactional annotation ensures that the entire operation is wrapped in a database transaction.
-    Error handling is implemented to catch and log any issues during the insert process.

```java
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BulkInsertService {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    private static final int BATCH_SIZE = 1000;

    @Transactional
    public void insertLargeDataset(List<YourDataObject> dataList) {
        SqlSessionFactory sqlSessionFactory = sqlSessionTemplate.getSqlSessionFactory();
        
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            // Set session-specific parameters
            session.update("ALTER SESSION SET SORT_AREA_SIZE = 1048576");

            YourMapper mapper = session.getMapper(YourMapper.class);
            
            for (int i = 0; i < dataList.size(); i++) {
                mapper.insert(dataList.get(i));
                
                if (i > 0 && i % BATCH_SIZE == 0) {
                    session.flushStatements();
                    session.clearCache();
                }
            }
            
            session.flushStatements();
        } catch (Exception e) {
            // Log the error and potentially implement a retry mechanism
            throw new RuntimeException("Error during bulk insert", e);
        }
    }
}
```

```java
public interface YourMapper {
    void insert(YourDataObject data);
}
```

Use the APPEND hint in your INSERT statements:
```xml
<insert id="insert" parameterType="YourDataObject">
    INSERT /*+APPEND*/ INTO your_table (column1, column2, ...)
    VALUES (#{property1}, #{property2}, ...)
</insert>
```

```java    
@Autowired
private BulkInsertService bulkInsertService;

public void performLargeInsert(List<YourDataObject> largeDataset) {
    bulkInsertService.insertLargeDataset(largeDataset);
}
```

HikariCP properties:
maximum-pool-size: 20 is a good starting point for high-load scenarios. Adjust based on your server's capabilities.
minimum-idle: 10 keeps a good number of connections ready.
connection-timeout: 30000 ms (30 seconds) is usually sufficient.
idle-timeout: 600000 ms (10 minutes) before an idle connection is removed.
max-lifetime: 1800000 ms (30 minutes) maximum lifetime of a connection in the pool.

```text
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048

spring.datasource.hikari.data-source-properties.defaultExecuteBatch=100
spring.datasource.hikari.data-source-properties.defaultBatchValue=100
```

For the other optimizations (NOLOGGING, disabling indexes, changing system parameters), I would recommend:

- Discuss with your database administrator and other application owners before implementing.
- If possible, consider setting up a separate schema or database instance for your bulk operations.
- If you must use these in a shared environment, implement them in a controlled maintenance window where other applications are not actively using the database.

Remember, the most important aspect is to test thoroughly in an environment that mirrors your production setup, and to communicate and coordinate with other teams that share the database resources.

Disable logging during bulk insert:
Before the bulk insert: ALTER TABLE your_table NOLOGGING;
After the bulk insert: ALTER TABLE your_table LOGGING;

Increase SORT_AREA_SIZE (if your insert involves sorting): ALTER SYSTEM SET db_file_multiblock_read_count = 128 SCOPE=BOTH;

Disable indexes before insert and rebuild after:
ALTER INDEX your_index UNUSABLE;
-- Perform bulk insert
ALTER INDEX your_index REBUILD;



# Http Client
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
            if (clientRequest.headers().containsKey("Authorization")) {
                String jwtToken = clientRequest.headers().getFirst("Authorization").replace("Bearer ", "");
                System.out.println("JWT Token: " + jwtToken);
            }
            System.out.println("Request: " + clientRequest.method() + " " + clientRequest.url());
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            System.out.println("Response Status: " + clientResponse.statusCode());
            return Mono.just(clientResponse);
        }));
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

    // Inject the configured WebClient
    public MyService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Example method to perform an HTTP POST request
    public Mono<ServiceResponse> createCompanyInfo(CompanyInfoRequest companyInfoRequest) {
        return webClient.post()
                .uri("/company")
                .body(Mono.just(companyInfoRequest), CompanyInfoRequest.class)
                .retrieve()
                .bodyToMono(ServiceResponse.class)
                .doOnError(e -> System.out.println("Error: " + e.getMessage()));
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

#  Command Line Application 

export SERVICE_A_URL=http://prod-service-a1.example.com,http://prod-service-a2.example.com
export SERVICE_B_URL=http://prod-service-b1.example.com,http://prod-service-b2.example.com
export SERVICE_C_URL=http://prod-service-c1.example.com,http://prod-service-c2.example.com

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
         String profile = Arrays.stream(args)
        .filter(arg -> arg.startsWith("--spring.profiles.active="))
        .findFirst()
        .orElse("--spring.profiles.active=flow1");
    
        SpringApplication app = new SpringApplication(UobCmdBootApplication.class);
        app.setAdditionalProfiles(profile.split("=")[1]);
        app.run(args);
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
 }

```

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

    @PreDestroy
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

to improve reliability

1. implement retry:

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

2. Implement a dead letter channel:

```java
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
```

3. Update your main flow to use this:

```java
@Bean
public IntegrationFlow fileProcessingFlow() {
    return IntegrationFlow.from("fileProcessingInputChannel")
        .handle(processingHandler, "processFile", e -> e
            .advice(retryAdvice())
            .handleMessageProcessingError(error -> error.sendTo("deadLetterChannel")))
        // Rest of your flow
        .get();
}

@Bean
public Advice retryAdvice() {
    RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
    advice.setRetryTemplate(retryTemplate());
    return advice;
}
```

4. For automatic restarts, you can use a process manager like systemd on Linux or a tool like Supervisor. Here's a sample systemd service file:

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

6. Proper File Lock Handling: Ensure that file locks are released in case of application shutdown:

```java
@Component
public class ShutdownHook {
    @Autowired
    private FileLockService fileLockService;

    @PreDestroy
    public void onShutdown() {
        fileLockService.releaseAllLocks();
    }
}
```

