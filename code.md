---
title: '☕️ CEW'
date: 2001-01-01
robots: 'noindex,nofollow'
xml: false
---

<!--more-->

# Rule & RuleTemplate

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

-   We're using ExecutorType.BATCH to enable JDBC batching.
-   We flush and clear the session every BATCH_SIZE (1000 in this example) inserts to manage memory usage.
-   The @Transactional annotation ensures that the entire operation is wrapped in a database transaction.
-   Error handling is implemented to catch and log any issues during the insert process.

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
# the maximum amount of time that a connection can sit idle in the pool before being eligible for eviction.
spring.datasource.hikari.idle-timeout=600000
# Connections will be retired after 30 minutes (1,800,000 ms), regardless of their activity. This is to avoid using stale connections that could have been closed by the database.
spring.datasource.hikari.max-lifetime=1800000

spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048

spring.datasource.hikari.data-source-properties.defaultExecuteBatch=100
spring.datasource.hikari.data-source-properties.defaultBatchValue=100
```

For the other optimizations (NOLOGGING, disabling indexes, changing system parameters), I would recommend:

-   Discuss with your database administrator and other application owners before implementing.
-   If possible, consider setting up a separate schema or database instance for your bulk operations.
-   If you must use these in a shared environment, implement them in a controlled maintenance window where other applications are not actively using the database.

-   session wise tuning

Disable logging during bulk insert:
Before the bulk insert: ALTER TABLE your_table NOLOGGING;
After the bulk insert: ALTER TABLE your_table LOGGING;

Increase SORT_AREA_SIZE (if your insert involves sorting): ALTER SYSTEM SET db_file_multiblock_read_count = 128 SCOPE=BOTH;

Consider ALTER SESSION SET OPTIMIZER_MODE=FIRST_ROWS_N for prioritizing the first set of rows returned, beneficial for large volume inserts.

-   system wise tuning

Disable indexes before insert and rebuild after:
ALTER INDEX your_index UNUSABLE;
-- Perform bulk insert
ALTER INDEX your_index REBUILD;

Consider ALTER SESSION SET OPTIMIZER_MODE=FIRST_ROWS_N for prioritizing the first set of rows returned, beneficial for large volume inserts.

Partitioning large tables can significantly enhance performance by reducing the amount of data scanned during inserts.

Ensure that the Oracle redo log files are sufficiently large and the database is configured to handle large transactions efficiently. If you're not in archivelog mode, large bulk operations will be faster. Disable logging during bulk insert:
Before the bulk insert: ALTER TABLE your_table NOLOGGING;
After the bulk insert: ALTER TABLE your_table LOGGING;

Use Oracle’s Resource Manager to allocate appropriate resources to the bulk insert process without affecting other applications.

Ensure that the temporary tablespaces have sufficient space and are optimized for large sort operations.

1. Performance
   PL/SQL Bulk Insert: Oracle's PL/SQL is highly optimized for bulk operations. The FORALL statement and the ability to work directly within the database engine allows for efficient handling of large datasets. It minimizes context switching between the application and the database, which can significantly reduce overhead and improve performance.
   MyBatis Batch Insert: While MyBatis supports batch processing, it operates on the application side, which may involve more overhead due to network round trips and context switching between the application and the database. Although MyBatis can be tuned for batch operations, it generally won't match the efficiency of a well-designed PL/SQL bulk insert, especially with very large datasets.
2. Scalability
   PL/SQL Bulk Insert: PL/SQL can handle very large volumes of data natively within the Oracle database. The Oracle database is designed to manage large transactions and can handle millions of records efficiently when using bulk operations.
   MyBatis Batch Insert: MyBatis can be configured to handle large volumes, but as the data size grows, it may struggle with memory consumption and network latency. This approach also increases the load on the application server.
3. Resource Management
   PL/SQL Bulk Insert: By offloading the bulk of the processing to the database, you reduce the memory and CPU load on your application server. Oracle's database engine is optimized to manage its own resources efficiently during bulk operations.
   MyBatis Batch Insert: Batch operations in MyBatis require careful management of resources like memory and database connections. If not configured properly, you may run into issues with memory consumption and connection pool exhaustion.
4. Error Handling
   PL/SQL Bulk Insert: Error handling in PL/SQL is more fine-grained and can be managed directly within the stored procedure. You can handle specific exceptions and manage transactions more effectively, ensuring that large operations can be rolled back or partially committed as needed.
   MyBatis Batch Insert: MyBatis does provide mechanisms for transaction management, but handling errors across large batches of data can be more complex, especially if you need to retry failed batches or handle partial failures.
5. Network Overhead
   PL/SQL Bulk Insert: Since the data is processed within the database, there is minimal network overhead. The bulk of the data transfer occurs once when sending the array of data to the database.
   MyBatis Batch Insert: Each batch of records requires a network round trip between the application and the database. This can become a bottleneck with very large datasets.

Conclusion

For very large datasets, particularly when dealing with more than 100K records, the Oracle PL/SQL bulk insert approach is generally more efficient, scalable, and easier to manage in terms of performance and resource utilization. It leverages Oracle's internal optimizations for handling large volumes of data, which is ideal for high-performance data insertion tasks.

## Oracle PL/SQL Bulk Insert

```sql
CREATE OR REPLACE TYPE your_table_row_type AS OBJECT (
    field1 VARCHAR2(100),
    field2 NUMBER,
    field3 DATE
    -- Other fields as necessary
);
/

CREATE OR REPLACE TYPE your_table_type AS TABLE OF your_table_row_type;
/

CREATE OR REPLACE PROCEDURE bulk_insert_your_data (
    p_data IN your_table_type -- This is a PL/SQL table type of your data objects
) AS
BEGIN
    -- Perform the bulk insert using the FORALL statement
    FORALL i IN p_data.FIRST .. p_data.LAST
        INSERT INTO your_table_name (
            column1, column2, column3 -- List all relevant columns
        ) VALUES (
            p_data(i).field1, p_data(i).field2, p_data(i).field3 -- Map to your data object fields
        );

    -- Optionally, handle exceptions, logging, or other business logic here
    COMMIT; -- Commit after bulk insert
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK; -- Rollback if any error occurs
        RAISE;    -- Rethrow the exception to the calling environment
END;
/
```

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnType;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.List;
import java.util.Map;

@Service
public class BulkInsertService {

    private final SimpleJdbcCall jdbcCall;

    @Autowired
    public BulkInsertService(DataSource dataSource) {
        this.jdbcCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("bulk_insert_your_data")
                .declareParameters(
                        new SqlParameter("p_data", java.sql.Types.ARRAY, "YOUR_TABLE_TYPE") // Use your custom Oracle type here
                );
    }

    public void insertLargeDataset(List<YourDataObject> dataList) throws SQLException {
        try (Connection connection = jdbcCall.getJdbcTemplate().getDataSource().getConnection()) {
            Array array = connection.createArrayOf("YOUR_TABLE_TYPE", dataList.toArray()); // Create the array type
            Map<String, Object> inParams = Map.of("p_data", array);

            jdbcCall.execute(inParams); // Call the PL/SQL procedure with the array as input
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

```java
// PaymentProcessingApplication.java
package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PaymentProcessingApplication {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job preDbInsertionJob;

    @Autowired
    private Job dbBatchInsertionJob;

    @Autowired
    private Job postDbInsertionJob;

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessingApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            if (args.length < 1) {
                System.out.println("Usage: java -jar payment-processor.jar <stage>");
                System.out.println("Stages: pre-db, db-batch, post-db");
                return;
            }

            String stage = args[0];
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            switch (stage) {
                case "pre-db":
                    jobLauncher.run(preDbInsertionJob, params);
                    break;
                case "db-batch":
                    jobLauncher.run(dbBatchInsertionJob, params);
                    break;
                case "post-db":
                    jobLauncher.run(postDbInsertionJob, params);
                    break;
                default:
                    System.out.println("Invalid stage. Use pre-db, db-batch, or post-db.");
            }
        };
    }
}

// application-pre-db.properties
spring.profiles.active=pre-db
camel.springboot.main-run-controller=true
input.directory=/path/to/nas/directory
file.pattern=payment_*.json

// application-db-batch.properties
spring.profiles.active=db-batch
spring.batch.job.enabled=false

// application-post-db.properties
spring.profiles.active=post-db
archive.directory=/path/to/archive/directory

// application.properties (common settings)
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XE
spring.datasource.username=your_username
spring.datasource.password=your_password

error.directory=/path/to/error/directory

spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin

spring.jpa.hibernate.ddl-auto=update
```

```bash
// run.sh
#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <stage>"
    echo "Stages: pre-db, db-batch, post-db"
    exit 1
fi

stage=$1

case $stage in
    pre-db|db-batch|post-db)
        java -jar -Dspring.profiles.active=$stage payment-processor.jar $stage
        ;;
    *)
        echo "Invalid stage. Use pre-db, db-batch, or post-db."
        exit 1
        ;;
esac
```

```java
// PaymentFileStatus.java
package com.example.payment.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_file_status")
public class PaymentFileStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Column
    private String errorMessage;

    // Getters and setters
}

// PaymentFileStatusRepository.java
package com.example.payment.repository;

import com.example.payment.model.PaymentFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentFileStatusRepository extends JpaRepository<PaymentFileStatus, Long> {
    PaymentFileStatus findByFileName(String fileName);
}

// CamelConfig.java (updated)
package com.example.payment.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CamelConfig {

    @Bean
    public RouteBuilder routeBuilder(DataSource dataSource) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                JdbcMessageIdRepository repo = new JdbcMessageIdRepository(dataSource, "CAMEL_MESSAGEPROCESSED");

                errorHandler(deadLetterChannel("direct:errorHandler")
                    .useOriginalMessage()
                    .maximumRedeliveries(3)
                    .redeliveryDelay(1000)
                    .backOffMultiplier(2)
                    .retryAttemptedLogLevel(LoggingLevel.WARN));

                from("file:{{input.directory}}?include={{file.pattern}}&readLock=idempotent&idempotentRepository=#jdbcMessageIdRepo&readLockRemoveOnCommit=true")
                    .routeId("filePollingRoute")
                    .log("New file detected: ${header.CamelFileName}")
                    .to("direct:preDbInsertion");

                from("direct:preDbInsertion")
                    .routeId("preDbInsertionRoute")
                    .log("Triggering Pre-DB Insertion Job: ${header.CamelFileName}")
                    .to("spring-batch:preDbInsertionJob");

                from("jms:queue:dbBatchInsertionQueue")
                    .routeId("dbBatchInsertionRoute")
                    .log("Triggering DB Batch Insertion Job")
                    .to("spring-batch:dbBatchInsertionJob");

                from("jms:queue:postDbInsertionQueue")
                    .routeId("postDbInsertionRoute")
                    .log("Triggering Post-DB Insertion Job")
                    .to("spring-batch:postDbInsertionJob");

                from("direct:errorHandler")
                    .routeId("errorHandlerRoute")
                    .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                    .to("bean:errorHandler?method=handleError");
            }
        };
    }

    @Bean
    public JdbcMessageIdRepository jdbcMessageIdRepo(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, "CAMEL_MESSAGEPROCESSED");
    }
}

// ErrorHandler.java
package com.example.payment.service;

import com.example.payment.model.PaymentFileStatus;
import com.example.payment.repository.PaymentFileStatusRepository;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;

@Service
public class ErrorHandler {

    private final PaymentFileStatusRepository statusRepository;
    private final String errorDirectory;

    public ErrorHandler(PaymentFileStatusRepository statusRepository,
                        @Value("${error.directory}") String errorDirectory) {
        this.statusRepository = statusRepository;
        this.errorDirectory = errorDirectory;
    }

    public void handleError(Exchange exchange) {
        String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        // Update status in database
        PaymentFileStatus status = statusRepository.findByFileName(fileName);
        if (status == null) {
            status = new PaymentFileStatus();
            status.setFileName(fileName);
        }
        status.setStatus("ERROR");
        status.setLastUpdated(LocalDateTime.now());
        status.setErrorMessage(cause.getMessage());
        statusRepository.save(status);

        // Move file to error directory
        File sourceFile = new File(exchange.getIn().getHeader("CamelFileAbsolutePath", String.class));
        File destFile = new File(errorDirectory, fileName);
        sourceFile.renameTo(destFile);
    }
}

// PreDbInsertionReader.java (updated)
package com.example.payment.batch;

import com.example.payment.model.PaymentFileStatus;
import com.example.payment.repository.PaymentFileStatusRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;

@Component
public class PreDbInsertionReader implements ItemReader<String> {

    private final LockRegistry lockRegistry;
    private final String filePath;
    private final PaymentFileStatusRepository statusRepository;

    public PreDbInsertionReader(LockRegistry lockRegistry,
                                PaymentFileStatusRepository statusRepository,
                                @Value("#{jobParameters['filePath']}") String filePath) {
        this.lockRegistry = lockRegistry;
        this.statusRepository = statusRepository;
        this.filePath = filePath;
    }

    @Override
    public String read() throws Exception {
        Lock lock = lockRegistry.obtain(filePath);
        if (lock.tryLock()) {
            try {
                // Update status to PROCESSING
                String fileName = new File(filePath).getName();
                PaymentFileStatus status = statusRepository.findByFileName(fileName);
                if (status == null) {
                    status = new PaymentFileStatus();
                    status.setFileName(fileName);
                }
                status.setStatus("PROCESSING");
                status.setLastUpdated(LocalDateTime.now());
                statusRepository.save(status);

                return filePath;
            } catch (Exception e) {
                lock.unlock();
                throw e;
            }
        }
        return null;
    }
}

// PreDbInsertionWriter.java (updated)
package com.example.payment.batch;

import com.example.payment.model.Pan001Payment;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PreDbInsertionWriter implements ItemWriter<Pan001Payment> {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Override
    public void write(List<? extends Pan001Payment> items) throws Exception {
        for (Pan001Payment payment : items) {
            jmsTemplate.convertAndSend("dbBatchInsertionQueue", payment);
        }
    }
}

// DbBatchInsertionWriter.java (updated)
package com.example.payment.batch;

import com.example.payment.model.PaymentTransaction;
import com.example.payment.model.PaymentFileStatus;
import com.example.payment.repository.PaymentFileStatusRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DbBatchInsertionWriter implements ItemWriter<PaymentTransaction> {

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private PaymentFileStatusRepository statusRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Override
    public void write(List<? extends PaymentTransaction> items) throws Exception {
        transactionRepository.saveAll(items);

        // Update status to PROCESSED
        String fileName = items.get(0).getFileName(); // Assuming fileName is stored in PaymentTransaction
        PaymentFileStatus status = statusRepository.findByFileName(fileName);
        status.setStatus("PROCESSED");
        status.setLastUpdated(LocalDateTime.now());
        statusRepository.save(status);

        // Send message to post-processing queue
        jmsTemplate.convertAndSend("postDbInsertionQueue", fileName);
    }
}

// PostDbInsertionWriter.java (updated)
package com.example.payment.batch;

import com.example.payment.model.PaymentFileStatus;
import com.example.payment.repository.PaymentFileStatusRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Component
public class PostDbInsertionWriter implements ItemWriter<String> {

    private final LockRegistry lockRegistry;
    private final String filePath;
    private final PaymentFileStatusRepository statusRepository;
    private final String archiveDirectory;

    public PostDbInsertionWriter(LockRegistry lockRegistry,
                                 PaymentFileStatusRepository statusRepository,
                                 @Value("#{jobParameters['filePath']}") String filePath,
                                 @Value("${archive.directory}") String archiveDirectory) {
        this.lockRegistry = lockRegistry;
        this.statusRepository = statusRepository;
        this.filePath = filePath;
        this.archiveDirectory = archiveDirectory;
    }

    @Override
    public void write(List<? extends String> items) throws Exception {
        String fileName = new File(filePath).getName();

        // Update status to COMPLETED
        PaymentFileStatus status = statusRepository.findByFileName(fileName);
        status.setStatus("COMPLETED");
        status.setLastUpdated(LocalDateTime.now());
        statusRepository.save(status);

        // Move file to archive directory
        File sourceFile = new File(filePath);
        File destFile = new File(archiveDirectory, fileName);
        sourceFile.renameTo(destFile);

        // Release the lock
        Lock lock = lockRegistry.obtain(filePath);
        lock.unlock();
    }
}

// application.properties (updated)
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XE
spring.datasource.username=your_username
spring.datasource.password=your_password

input.directory=/path/to/nas/directory
error.directory=/path/to/error/directory
archive.directory=/path/to/archive/directory
file.pattern=payment_*.json

spring.batch.job.enabled=false

spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin

spring.jpa.hibernate.ddl-auto=update
```

```java
// PaymentFileStatus.java
package com.example.payment.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_file_status")
public class PaymentFileStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fileName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String stage;

    @Column(nullable = false)
    private String customer;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String errorMessage;

    @Column
    private String csvFileName;

    // Getters and setters
}

-- SQL schema for payment_file_status table
CREATE TABLE payment_file_status (
    id NUMBER GENERATED ALWAYS AS IDENTITY,
    file_name VARCHAR2(255) NOT NULL UNIQUE,
    status VARCHAR2(50) NOT NULL,
    stage VARCHAR2(50) NOT NULL,
    customer VARCHAR2(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    error_message VARCHAR2(4000),
    csv_file_name VARCHAR2(255),
    CONSTRAINT pk_payment_file_status PRIMARY KEY (id)
);

CREATE INDEX idx_payment_file_status_file_name ON payment_file_status (file_name);
CREATE INDEX idx_payment_file_status_customer ON payment_file_status (customer);

// PreDbInsertionConfig.java
package com.example.payment.config;

import com.example.payment.batch.PreDbInsertionProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PreDbInsertionConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public PreDbInsertionConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job preDbInsertionJobForCustomer1(
            @Qualifier("preDbInsertionStepForCustomer1") Step preDbInsertionStepForCustomer1) {
        return jobBuilderFactory.get("preDbInsertionJobForCustomer1")
                .flow(preDbInsertionStepForCustomer1)
                .end()
                .build();
    }

    @Bean
    public Step preDbInsertionStepForCustomer1(
            PreDbInsertionReader reader,
            @Qualifier("preDbInsertionProcessorForCustomer1") PreDbInsertionProcessor processor,
            PreDbInsertionWriter writer) {
        return stepBuilderFactory.get("preDbInsertionStepForCustomer1")
                .<String, Pan001Payment>chunk(10)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public PreDbInsertionProcessor preDbInsertionProcessorForCustomer1() {
        return new PreDbInsertionProcessor("Customer1");
    }

    // Add more beans for other customers as needed
}

// PreDbInsertionProcessor.java
package com.example.payment.batch;

import com.example.payment.model.Pan001Payment;
import org.springframework.batch.item.ItemProcessor;

public class PreDbInsertionProcessor implements ItemProcessor<String, Pan001Payment> {

    private final String customer;

    public PreDbInsertionProcessor(String customer) {
        this.customer = customer;
    }

    @Override
    public Pan001Payment process(String item) throws Exception {
        Pan001Payment payment = parsePayment(item);
        validatePayment(payment);
        enrichPayment(payment);
        return payment;
    }

    private Pan001Payment parsePayment(String item) {
        // Parse the payment based on customer-specific format
        // ...
    }

    private void validatePayment(Pan001Payment payment) {
        // Perform customer-specific validation
        // ...
    }

    private void enrichPayment(Pan001Payment payment) {
        // Perform customer-specific enrichment
        // ...
    }
}

// PreDbInsertionWriter.java
package com.example.payment.batch;

import com.example.payment.model.Pan001Payment;
import com.example.payment.model.PaymentFileStatus;
import com.example.payment.repository.PaymentFileStatusRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PreDbInsertionWriter implements ItemWriter<Pan001Payment> {

    private final PaymentFileStatusRepository statusRepository;
    private final String outputDirectory;

    public PreDbInsertionWriter(PaymentFileStatusRepository statusRepository,
                                @Value("${output.directory}") String outputDirectory) {
        this.statusRepository = statusRepository;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void write(List<? extends Pan001Payment> items) throws Exception {
        if (items.isEmpty()) return;

        String originalFileName = items.get(0).getOriginalFileName();
        String csvFileName = originalFileName.replace(".json", ".csv");
        Path csvPath = Paths.get(outputDirectory, csvFileName);

        try (FileWriter writer = new FileWriter(csvPath.toFile())) {
            // Write CSV header
            writer.write("id,amount,currency,paymentDate\n");

            for (Pan001Payment payment : items) {
                // Write payment data to CSV
                writer.write(String.format("%s,%f,%s,%s\n",
                        payment.getId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getPaymentDate()));
            }
        }

        // Update status in database
        PaymentFileStatus status = statusRepository.findByFileName(originalFileName);
        status.setStatus("PRE_DB_COMPLETED");
        status.setStage("DB_BATCH_INSERTION");
        status.setUpdatedAt(LocalDateTime.now());
        status.setCsvFileName(csvFileName);
        statusRepository.save(status);
    }
}

// CamelConfig.java (updated)
package com.example.payment.config;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:{{input.directory}}?include={{file.pattern}}&readLock=changed")
                    .routeId("jsonFilePollingRoute")
                    .log("New JSON file detected: ${header.CamelFileName}")
                    .to("direct:preDbInsertion");

                from("direct:preDbInsertion")
                    .routeId("preDbInsertionRoute")
                    .log("Triggering Pre-DB Insertion Job: ${header.CamelFileName}")
                    .to("spring-batch:preDbInsertionJobForCustomer1");

                from("file:{{output.directory}}?include=*.csv&readLock=changed")
                    .routeId("csvFilePollingRoute")
                    .log("New CSV file detected: ${header.CamelFileName}")
                    .to("direct:dbBatchInsertion");

                from("direct:dbBatchInsertion")
                    .routeId("dbBatchInsertionRoute")
                    .log("Triggering DB Batch Insertion Job: ${header.CamelFileName}")
                    .to("spring-batch:dbBatchInsertionJob");

                // ... (rest of the routes remain the same)
            }
        };
    }
}

// application.properties (updated)
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XE
spring.datasource.username=your_username
spring.datasource.password=your_password

input.directory=/path/to/nas/directory
output.directory=/path/to/output/directory
error.directory=/path/to/error/directory
archive.directory=/path/to/archive/directory
file.pattern=payment_*.json

spring.batch.job.enabled=false

spring.jpa.hibernate.ddl-auto=update

// PaymentProcessingApplication.java (updated)
package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PaymentProcessingApplication {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("preDbInsertionJobForCustomer1")
    private Job preDbInsertionJobForCustomer1;

    @Autowired
    private Job dbBatchInsertionJob;

    @Autowired
    private Job postDbInsertionJob;

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessingApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            if (args.length < 2) {
                System.out.println("Usage: java -jar payment-processor.jar <stage> <customer>");
                System.out.println("Stages: pre-db, db-batch, post-db");
                System.out.println("Customers: customer1, customer2, ...");
                return;
            }

            String stage = args[0];
            String customer = args[1];
            JobParameters params = new JobParametersBuilder()
                    .addString("customer", customer)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            switch (stage) {
                case "pre-db":
                    if ("customer1".equals(customer)) {
                        jobLauncher.run(preDbInsertionJobForCustomer1, params);
                    } else {
                        System.out.println("Unsupported customer for pre-db stage: " + customer);
                    }
                    break;
                case "db-batch":
                    jobLauncher.run(dbBatchInsertionJob, params);
                    break;
                case "post-db":
                    jobLauncher.run(postDbInsertionJob, params);
                    break;
                default:
                    System.out.println("Invalid stage. Use pre-db, db-batch, or post-db.");
            }
        };
    }
}
```

This integration provides several benefits:

Separation of Concerns: Camel handles file monitoring and routing, while Spring Batch manages the complex data processing tasks.
-   Scalability: Multiple instances of the application can run on different nodes, each polling for files and triggering jobs as needed.
-   Flexibility: It's easy to add new routes for different file types or customers, and to trigger different jobs based on file characteristics.
-   Reliability: The use of file locks and idempotent repositories (configured elsewhere) ensures that files are processed exactly once, even in distributed environments.

To summarize, Camel acts as the orchestrator, detecting new files and triggering the appropriate Spring Batch jobs. This separation allows for efficient file handling (Camel's strength) combined with robust, scalable data processing (Spring Batch's strength).
