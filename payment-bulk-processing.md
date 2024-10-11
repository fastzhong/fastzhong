```yml
spring:
  application:
    name: payment-processing-application

  spring:
  application:
    name: payment-processing-application

  datasource:
    common:
      type: com.zaxxer.hikari.HikariDataSource
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        maximum-pool-size: 5
        minimum-idle: 2
        idle-timeout: 300000
        connection-timeout: 20000
        max-lifetime: 1800000
      vault:
        enabled: false
        key-store: /path/to/keystore.jks
        key-store-password: keystorePassword
        key-store-type: JKS
        trust-store: /path/to/truststore.jks
        trust-store-password: truststorePassword
        wallet-location: /path/to/wallet

    # Default datasource for Spring Batch metadata
    default:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/XEPDB1
      username: batch_user
      password: batch_password

    # PWS Insertion datasource
    pws-insertion:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/PWSDB1
      username: pws_insert_user
      password: pws_insert_password

    # PWS Loading datasource
    pws-loading:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/PWSDB2
      username: pws_load_user
      password: pws_load_password

  batch:
    job:
      enabled: false  # Disable auto-start of jobs
    job-repository:
      initialize: false  # Set to true to use database-backed job repository

server:
  port: 8080

logging:
  level:
    root: INFO
    com.example.paymentprocessing: DEBUG
    com.zaxxer.hikari: DEBUG

mybatis:
  config-location: classpath:mybatis-config.xml
  mapper-locations: classpath:mappers/**/*.xml

bulk-routes:
  - route-name: CUSTOMER_SUBMITTED_TRANSFORMED
    processing-type: INBOUND
    source-type: FILE
    enabled: true
    steps:
      - file-validation  
      - group-validation
      - paymentInfo-validation
      - paymentInfo-enrichment
      - transaction-validation
      - transaction-enrichment 
      - pws-computation
      - pws-insertion
      - notification
      - file-archive
    file-source: 
      path: /path/to/inbound/files
      file-patterns: *.json
      interval: 60000  # 60 seconds
    pws-insertion:
      datasource: datasource-pws-insertion
    notification:
      email:
        host: smtp.example.com
        port: 587
        username: notification@example.com
        password: notification_password
    file-archive:
      path: /path/to/archive/folder

  - route-name: CUSTOMER_AUTHORIZED
    processing-type: OUTBOUND
    destination-type: FILE
    enabled: true
    steps:
      - pws-loading
      - pain001-transformation
    pws-loading: 
      datasource: datasource-pws-loading
      cron: 0 0/15 * * * ?  # Every 15 minutes
    pain001-transformation: 
      template-path: /path/to/pain001/template.xml
    file-destination: 
      path: /path/to/outbound/files
```

```java
package com.example.paymentprocessing.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    private final Environment env;

    public DataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.default")
    public DataSource defaultDataSource() {
        return createDataSource("spring.datasource.default");
    }

    @Bean
    @ConfigurationProperties("spring.datasource.pws-insertion")
    public DataSource pwsInsertionDataSource() {
        return createDataSource("spring.datasource.pws-insertion");
    }

    @Bean
    @ConfigurationProperties("spring.datasource.pws-loading")
    public DataSource pwsLoadingDataSource() {
        return createDataSource("spring.datasource.pws-loading");
    }

    private DataSource createDataSource(String prefix) {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        
        // Set common properties
        dataSource.setDriverClassName(env.getProperty("spring.datasource.common.driver-class-name"));
        dataSource.setJdbcUrl(env.getProperty(prefix + ".jdbc-url"));
        dataSource.setUsername(env.getProperty(prefix + ".username"));
        dataSource.setPassword(env.getProperty(prefix + ".password"));
        
        // Set Hikari-specific properties
        dataSource.setMaximumPoolSize(env.getProperty("spring.datasource.common.hikari.maximum-pool-size", Integer.class));
        dataSource.setMinimumIdle(env.getProperty("spring.datasource.common.hikari.minimum-idle", Integer.class));
        dataSource.setIdleTimeout(env.getProperty("spring.datasource.common.hikari.idle-timeout", Long.class));
        dataSource.setConnectionTimeout(env.getProperty("spring.datasource.common.hikari.connection-timeout", Long.class));
        dataSource.setMaxLifetime(env.getProperty("spring.datasource.common.hikari.max-lifetime", Long.class));
        
        // Configure Oracle Vault if enabled
        if (Boolean.parseBoolean(env.getProperty("spring.datasource.common.vault.enabled"))) {
            Properties props = new Properties();
            props.setProperty("oracle.net.wallet_location", env.getProperty("spring.datasource.common.vault.wallet-location"));
            props.setProperty("javax.net.ssl.keyStore", env.getProperty("spring.datasource.common.vault.key-store"));
            props.setProperty("javax.net.ssl.keyStorePassword", env.getProperty("spring.datasource.common.vault.key-store-password"));
            props.setProperty("javax.net.ssl.keyStoreType", env.getProperty("spring.datasource.common.vault.key-store-type"));
            props.setProperty("javax.net.ssl.trustStore", env.getProperty("spring.datasource.common.vault.trust-store"));
            props.setProperty("javax.net.ssl.trustStorePassword", env.getProperty("spring.datasource.common.vault.trust-store-password"));
            
            dataSource.setDataSourceProperties(props);
        }
        
        return dataSource;
    }
}
```

```java
package com.example.paymentprocessing.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = "com.example.paymentprocessing.mapper")
public class MyBatisConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource-pws-insertion")
    public DataSource pwsInsertionDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource-pws-loading")
    public DataSource pwsLoadingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public SqlSessionFactory primarySqlSessionFactory(@Qualifier("primaryDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/**/*.xml"));
        return sessionFactory.getObject();
    }

    @Bean
    public SqlSessionFactory pwsInsertionSqlSessionFactory(@Qualifier("pwsInsertionDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/pwsinsertion/**/*.xml"));
        return sessionFactory.getObject();
    }

    @Bean
    public SqlSessionFactory pwsLoadingSqlSessionFactory(@Qualifier("pwsLoadingDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/pwsloading/**/*.xml"));
        return sessionFactory.getObject();
    }
}
```

```java
package com.example.paymentprocessing.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.paymentprocessing.config.BulkRoutesConfig;
import com.example.paymentprocessing.config.BulkRoutesConfig.RouteConfig;
import com.example.paymentprocessing.config.BulkRoutesConfig.ProcessingType;
import com.example.paymentprocessing.config.BulkRoutesConfig.SourceDestinationType;

@Component
public class DynamicRouteBuilder extends RouteBuilder {

    @Autowired
    private BulkRoutesConfig bulkRoutesConfig;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Map<String, Job> jobs;

    @Override
    public void configure() throws Exception {
        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                configureRoute(routeConfig);
            }
        }
    }

    private void configureRoute(RouteConfig routeConfig) {
        String jobName = routeConfig.getRouteName() + "Job";

        if (routeConfig.getProcessingType() == ProcessingType.INBOUND) {
            from(buildInboundFromUri(routeConfig))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                    Job job = jobs.get(jobName);
                    JobParameters jobParameters = new JobParametersBuilder()
                        .addString("fileName", exchange.getIn().getHeader("CamelFileName", String.class))
                        .addString("routeName", routeConfig.getRouteName())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                    jobLauncher.run(job, jobParameters);
                });
        } else { // OUTBOUND
            from("quartz://pwsLoadTimer?cron=" + routeConfig.getPwsLoading().get("cron"))
                .routeId(routeConfig.getRouteName())
                .process(exchange -> {
                    Job job = jobs.get(jobName);
                    JobParameters jobParameters = new JobParametersBuilder()
                        .addString("routeName", routeConfig.getRouteName())
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                    jobLauncher.run(job, jobParameters);
                })
                .to(buildOutboundToUri(routeConfig));
        }
    }

    private String buildInboundFromUri(RouteConfig routeConfig) {
        switch (routeConfig.getSourceType()) {
            case FILE:
                return String.format("file:%s?include=%s&delay=%d",
                    routeConfig.getFileSource().get("path"),
                    routeConfig.getFileSource().get("file-patterns"),
                    routeConfig.getFileSource().get("interval"));
            case JDBC:
                return "jdbc://select * from source_table"; // Placeholder, adjust as needed
            case MESSAGE:
                return "jms:queue:sourceQueue"; // Placeholder, adjust as needed
            case API:
                return "rest:get:api/source"; // Placeholder, adjust as needed
            default:
                throw new IllegalArgumentException("Unsupported source type: " + routeConfig.getSourceType());
        }
    }

    private String buildOutboundToUri(RouteConfig routeConfig) {
        switch (routeConfig.getDestinationType()) {
            case FILE:
                return String.format("file:%s", routeConfig.getFileDestination().get("path"));
            case JDBC:
                return "jdbc:destinationDataSource"; // Placeholder, adjust as needed
            case MESSAGE:
                return "jms:queue:destinationQueue"; // Placeholder, adjust as needed
            case API:
                return "rest:post:api/destination"; // Placeholder, adjust as needed
            default:
                throw new IllegalArgumentException("Unsupported destination type: " + routeConfig.getDestinationType());
        }
    }
}

package com.example.paymentprocessing.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class Pain001BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private ErrorHandlingListener errorHandlingListener;

    @Bean
    public Job pain001ProcessingJob() {
        return jobBuilderFactory.get("pain001ProcessingJob")
                .start(pain001ProcessingFlow())
                .listener(errorHandlingListener)
                .build();
    }

    @Bean
    public Flow pain001ProcessingFlow() {
        return new FlowBuilder<SimpleFlow>("pain001ProcessingFlow")
                .start(groupValidationStep())
                .next(paymentInfoSplittingStep())
                .next(transactionProcessingStep())
                .next(pwsComputationStep())
                .next(pwsInsertionStep())
                .next(notificationStep())
                .build();
    }

    @Bean
    public Step groupValidationStep() {
        return stepBuilderFactory.get("groupValidationStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement group validation logic
                    boolean validationPassed = true;
                    if (!validationPassed && isErrorOnFileEnabled()) {
                        throw new ValidationException("Group validation failed");
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step paymentInfoSplittingStep() {
        return stepBuilderFactory.get("paymentInfoSplittingStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement payment info splitting logic
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step transactionProcessingStep() {
        FaultTolerantStepBuilder<Transaction, Transaction> builder = stepBuilderFactory.get("transactionProcessingStep")
                .<Transaction, Transaction>chunk(1000)
                .reader(transactionReader())
                .processor(compositeTransactionProcessor())
                .writer(transactionWriter())
                .faultTolerant()
                .retry(RetryableException.class)
                .retryLimit(3)
                .skip(SkippableException.class)
                .skipLimit(10)
                .listener(new TransactionProcessingListener())
                .taskExecutor(taskExecutor())
                .throttleLimit(20);

        if (isErrorOnFileEnabled()) {
            builder.completionPolicy(new ErrorAwareCompletionPolicy());
        }

        return builder.build();
    }

    @Bean
    public Step pwsComputationStep() {
        return stepBuilderFactory.get("pwsComputationStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement PWS computation logic
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step pwsInsertionStep() {
        return stepBuilderFactory.get("pwsInsertionStep")
                .<PwsRecord, PwsRecord>chunk(1000)
                .reader(pwsRecordReader())
                .writer(pwsRecordWriter())
                .faultTolerant()
                .retry(RetryableException.class)
                .retryLimit(3)
                .skip(SkippableException.class)
                .skipLimit(10)
                .taskExecutor(taskExecutor())
                .throttleLimit(10)
                .build();
    }

    @Bean
    public Step notificationStep() {
        return stepBuilderFactory.get("notificationStep")
                .tasklet((contribution, chunkContext) -> {
                    // Implement notification logic
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("pain001-");
        executor.initialize();
        return executor;
    }

    @Bean
    public CompositeItemProcessor<Transaction, Transaction> compositeTransactionProcessor() {
        CompositeItemProcessor<Transaction, Transaction> processor = new CompositeItemProcessor<>();
        processor.setDelegates(Arrays.asList(
            transactionValidator(),
            transactionEnricher()
        ));
        return processor;
    }

    private boolean isErrorOnFileEnabled() {
        // Implement logic to check if errorOnFile setting is enabled
        return true; // Placeholder
    }

    // Other beans (readers, processors, writers) ...
}

class ErrorAwareCompletionPolicy implements CompletionPolicy {
    private boolean errorOccurred = false;

    @Override
    public boolean isComplete(RepeatContext context, RepeatStatus result) {
        return errorOccurred || RepeatStatus.FINISHED.equals(result);
    }

    @Override
    public boolean isComplete(RepeatContext context) {
        return errorOccurred;
    }

    @Override
    public RepeatContext start(RepeatContext parent) {
        return parent;
    }

    @Override
    public void update(RepeatContext context) {
        // This method can be called to set errorOccurred to true when an error is detected
    }

    public void setErrorOccurred(boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }
}

class TransactionProcessingListener implements ItemProcessListener<Transaction, Transaction>,
                                              ItemWriteListener<Transaction> {
    @Autowired
    private ErrorAwareCompletionPolicy completionPolicy;

    @Override
    public void onProcessError(Transaction item, Exception e) {
        if (isErrorOnFileEnabled()) {
            completionPolicy.setErrorOccurred(true);
        }
    }

    @Override
    public void onWriteError(Exception exception, List<? extends Transaction> items) {
        if (isErrorOnFileEnabled()) {
            completionPolicy.setErrorOccurred(true);
        }
    }

    // Other interface methods...
}
    @Bean
    public Map<String, Job> jobs() {
        Map<String, Job> jobs = new HashMap<>();

        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                String jobName = routeConfig.getRouteName() + "Job";
                jobs.put(jobName, createJob(routeConfig));
            }
        }

        return jobs;
    }

    private Job createJob(RouteConfig routeConfig) {
        JobBuilder jobBuilder = jobBuilderFactory.get(routeConfig.getRouteName() + "Job");

        for (String stepName : routeConfig.getSteps()) {
            Step step = steps.get(stepName + "Step");
            if (step != null) {
                jobBuilder = jobBuilder.next(step);
            }
        }

        return jobBuilder.build();
    }

    // Define step beans for each processing step
    @Bean
    public Step fileValidationStep() {
        return stepBuilderFactory.get("fileValidationStep")
            .tasklet((contribution, chunkContext) -> {
                // Implement file validation logic
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    @Bean
    public Step groupValidationStep() {
        // Implement group validation step
    }

    @Bean
    public Step paymentInfoValidationStep() {
        // Implement paymentInfo validation step
    }

    @Bean
    public Step paymentInfoEnrichmentStep() {
        // Implement paymentInfo enrichment step
    }

    @Bean
    public Step transactionValidationStep() {
        // Implement transaction validation step
    }

    @Bean
    public Step transactionEnrichmentStep() {
        // Implement transaction enrichment step
    }

    @Bean
    public Step pwsComputationStep() {
        // Implement PWS computation step
    }

    @Bean
    public Step pwsInsertionStep() {
        // Implement PWS insertion step
    }

    @Bean
    public Step notificationStep() {
        // Implement notification step
    }

    @Bean
    public Step fileArchiveStep() {
        // Implement file archive step
    }

    @Bean
    public Step pwsLoadingStep() {
        return stepBuilderFactory.get("pwsLoadingStep")
            .<PwsPayment, PwsPayment>chunk(10)
            .reader(pwsPaymentReader())
            .processor(pwsPaymentProcessor())
            .writer(pwsPaymentWriter())
            .build();
    }

    @Bean
    public Step pain001TransformationStep() {
        return stepBuilderFactory.get("pain001TransformationStep")
            .<PwsPayment, Pain001Payment>chunk(10)
            .reader(pwsPaymentReader())
            .processor(pain001TransformProcessor())
            .writer(pain001Writer())
            .build();
    }

    // Define readers, processors, and writers
    @Bean
    public JdbcCursorItemReader<PwsPayment> pwsPaymentReader() {
        // Configure to read from PWS tables
    }

    @Bean
    public ItemProcessor<PwsPayment, PwsPayment> pwsPaymentProcessor() {
        // Process PWS payments if needed
    }

    @Bean
    public ItemWriter<PwsPayment> pwsPaymentWriter() {
        // Write processed PWS payments if needed
    }

    @Bean
    public ItemProcessor<PwsPayment, Pain001Payment> pain001TransformProcessor() {
        // Transform PWS payment to Pain001 format
    }

    @Bean
    public ItemWriter<Pain001Payment> pain001Writer() {
        // Write Pain001 payments to the configured destination
    }

    @Value("${spring.batch.job-repository.initialize:false}")
    private boolean shouldInitializeJobRepository;

    private final DataSource dataSource;

    public BatchConfig(DataSource dataSource) {
        // This will be the default datasource if it exists
        this.dataSource = dataSource;
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        // Override to use no datasource if initialization is disabled
        if (shouldInitializeJobRepository) {
            super.setDataSource(dataSource);
        }
    }

    @Override
    protected JobRepository createJobRepository() throws Exception {
        if (shouldInitializeJobRepository) {
            return super.createJobRepository();
        } else {
            MapJobRepositoryFactoryBean factoryBean = new MapJobRepositoryFactoryBean();
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }
    }

    @Override
    protected JobExplorer createJobExplorer() throws Exception {
        if (shouldInitializeJobRepository) {
            return super.createJobExplorer();
        } else {
            MapJobExplorerFactoryBean factoryBean = new MapJobExplorerFactoryBean(createJobRepository());
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }
    }

    @Bean
    public JobLauncher jobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(createJobRepository());
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

}
```
