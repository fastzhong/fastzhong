```java
from(buildInboundFromUri(bulkRoute)).routeId(bulkRoute.getRouteName()).process(exchange -> {
    // Create InboundContext
    createInboundContext(bulkRoute, exchange);

    // Create and run the batch job
    JobParameters jobParameters = createInboundJobParameters(bulkRoute, exchange);

    // Create a CountDownLatch for job completion notification
    CountDownLatch countDownLatch = new CountDownLatch(1);
    
    // Create the batch job with the latch
    Job job = createJob(bulkRoute, exchange, paymentContext, countDownLatch);
    JobExecution jobExecution = jobLauncher.run(job, jobParameters);

    // Wait for completion with timeout
    boolean completed = countDownLatch.await(getJobTimeoutSeconds(bulkRoute), TimeUnit.SECONDS);
    
    if (!completed) {
        log.warn("Job execution timed out for file: {}", 
                 exchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
        handleInboundClose(bulkRoute, exchange, ExitStatus.FAILED);
    } else {
        // Process the job results in the main thread using the jobExecution we already have
        handleInboundJobExecution(bulkRoute, exchange, jobExecution);
    }
});
```

```java
protected Job createJob(AppConfig.BulkRoute bulkRoute, Exchange exchange, PaymentContext paymentContext, CountDownLatch countDownLatch) {
    JobBuilder jobBuilder = new JobBuilder("BatchJob_" + bulkRoute.getRouteName(), jobRepository);
    
    jobBuilder.listener(new JobExecutionListener() {
        public void beforeJob(JobExecution jobExecution) {
            ExecutionContext executionContext = jobExecution.getExecutionContext();
            if (bulkRoute.getProcessingType() == AppConfig.ProcessingType.INBOUND) {
                // Inbound
                InboundContext routeContext = exchange.getProperty(ContextKey.routeContext, InboundContext.class);
                executionContext.put(ContextKey.routeName, bulkRoute.getRouteName());
                executionContext.put(ContextKey.routeConfig, bulkRoute);
                executionContext.put(ContextKey.country, routeContext.getCountry());
                executionContext.put(ContextKey.channel, bulkRoute.getChannel());
                executionContext.put(ContextKey.bankEntity, bulkRoute.getBankEntity());
                executionContext.put(ContextKey.sourcePath, routeContext.getSourcePath());
                executionContext.put(ContextKey.sourceName, routeContext.getSourceName());
                executionContext.put(ContextKey.sourceFormat, "");
                executionContext.put(ContextKey.userId, 0L);
                executionContext.put(ContextKey.companyId, 0L);
                BankRefMetaData bankRefMetaData = new BankRefMetaData(appConfig.getCountry().name(),
                        bulkRoute.getChannel().prefix, bulkRoute.getRequestType().prefix,
                        LocalDateTime.now().format(Constants.BANK_REF_YY_MM));
                executionContext.put(ContextKey.bankRefMetaData, bankRefMetaData);
                executionContext.put(ContextKey.result, new Pain001InboundProcessingResult());
            } else {
                // ToDo: Outbound
                throw new BulkProcessingException("flow type not supported",
                        new Throwable("Unsupported flow: " + bulkRoute.getProcessingType()));
            }
            log.info("Job started successfully");
        }

        public void afterJob(JobExecution jobExecution) {
            if (jobExecution.getExitStatus() == ExitStatus.COMPLETED) {
                log.info("Job completed successfully: {}", jobExecution.getJobInstance().getJobName());
            } else {
                log.error("Job: {}", jobExecution.getJobInstance().getJobName());
                log.error("Job result: {}", jobExecution.getExitStatus());
                log.error("Job errorMessage: {}", jobExecution.getExitStatus().getExitDescription());
            }
            
            // Signal completion to the main thread
            countDownLatch.countDown();
        }
    });

    List<String> stepNames = bulkRoute.getSteps();
    if (stepNames == null || stepNames.isEmpty()) {
        throw new BulkProcessingException("No steps defined for route: " + bulkRoute.getRouteName(),
                new Throwable("no steps defined"));
    }

    Step firstStep = createStepForName(stepNames.get(0), bulkRoute, paymentContext);
    SimpleJobBuilder simpleJobBuilder = jobBuilder.start(firstStep);

    for (int i = 1; i < stepNames.size(); i++) {
        Step step = createStepForName(stepNames.get(i), bulkRoute, paymentContext);
        simpleJobBuilder.next(step);
    }
    
    return simpleJobBuilder.build();
}
```

```java
public static class BulkRoute {
    // existing fields
    private long jobTimeoutSeconds = 300; // Default 5 minutes
    
    // getters and setters
    public long getJobTimeoutSeconds() {
        return jobTimeoutSeconds;
    }
    
    public void setJobTimeoutSeconds(long jobTimeoutSeconds) {
        this.jobTimeoutSeconds = jobTimeoutSeconds;
    }
}
```



