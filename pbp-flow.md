# flow builder

```txt
file://c:/CEW/tmp/dmp/incoming?antInclude=*_Auth_*.json&charset=utf-8&delay=6000&doneFileName=${file:name.noext}.xml.done&maxMessagesPerPoll=1&move=c:/CEW/tmp/dmp/backup&moveFailed=c:/CEW/tmp/dmp/error&noop=false&readLock=rename&readLockTimeout=60000&sortBy=file:modified
```

```java
public class BulkProcessingFlowBuilder extends RouteBuilder {

    private final AppConfig appConfig;
    private final ObjectMapper objMapper;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final PlatformTransactionManager platformTransactionManager;

    private final Pain001InboundService pain001InboundService;

    @Override
    public void configure() throws Exception {
        for (AppConfig.BulkRoute bulkRoute : appConfig.getBulkRoutes()) {
            if (bulkRoute.isEnabled()) {
                configureRoute(bulkRoute);
            }
        }
    }

    private void configureRoute(AppConfig.BulkRoute bulkRoute) throws Exception {
        log.info("creating processing flow: {}", bulkRoute.toString());
        if (bulkRoute.getProcessingType() == AppConfig.ProcessingType.INBOUND) {
            // Inbound
            onException(Exception.class).handled(true).process(exchange -> {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                log.error("Processing failed for file: {}. Error: {}", fileName, cause.getMessage(), cause);
                updateProcessingStatus(bulkRoute, exchange, ExitStatus.NOOP);
            });
            // define the route
            from(buildInboundFromUri(bulkRoute)).routeId(bulkRoute.getRouteName())
                    .process(exchange -> prepareInboundContext(bulkRoute, exchange))
                    .process(exchange -> {
                        // Create and run the batch job
                        JobParameters jobParameters = createInboundJobParameters(bulkRoute, exchange);
                        Job job = createJob(bulkRoute, exchange);
                        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                        handleInboundJobExecution(bulkRoute, exchange, jobExecution);
                    });
        } else {
            // Outbound
            // ToDo: yet to implement
            throw new BulkProcessingException("flow type not supported",
                    new Throwable("Unsupported flow: " + bulkRoute.getProcessingType()));
        }
    }

    String buildInboundFromUri(AppConfig.BulkRoute bulkRoute) {
        switch (bulkRoute.getSourceType()) {
            case FILE :
                AppConfig.FileSource f = bulkRoute.getFileSource();
                String fileUri = "file:%s?antInclude=%s" + "&antExclude=%s" + "&charset=%s" + "&doneFileName=%s"
                        + "&delay=%d" + "&sortBy=%s" + "&maxMessagesPerPoll=%d" + "&noop=%b" + "&recursive=%b"
                        + "&move=%s" + "&moveFailed=%s" // This handles failed file movement automatically
                        + "&readLock=%s" + "&readLockTimeout=%d" + "&readLockCheckInterval=%d"
                        + "&readLockLoggingLevel=%s";

                String uri = String.format(fileUri, f.getDirectoryName(), f.getAntInclude(), f.getAntExclude(),
                        f.getCharset(), f.getDoneFileName(), f.getDelay(), f.getSortBy(), f.getMaxMessagesPerPoll(),
                        f.isNoop(), f.isRecursive(), f.getMove(), f.getMoveFailed(), f.getReadLock(),
                        f.getReadLockTimeout(), f.getReadLockCheckInterval(), f.getReadLockLoggingLevel());

                log.info("inboundFromUri: " + uri);
                return uri;
            default :
                log.error("Unsupported source type: {}", bulkRoute.getSourceType());
                throw new BulkProcessingException("source type not supported",
                        new Throwable("Unsupported source type: " + bulkRoute.getSourceType()));
        }
    }

    // Inbound Job
    protected void prepareInboundContext(AppConfig.BulkRoute bulkRoute, Exchange exchange) {
        // Create execution context with necessary data
        log.info("prepare inbound context ...");
        InboundContext routeContext = new InboundContext();
        routeContext.setCountry(appConfig.getCountry());
        routeContext.setBulkRoute(bulkRoute);
        routeContext.setSourcePath(exchange.getIn().getHeader(Exchange.FILE_PATH, String.class));
        routeContext.setSourceName(exchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
        routeContext.setFormat("json");
        if (AppConfig.SourceDestinationType.FILE.equals(bulkRoute.getSourceType())) {
            // Clear the message body to free up memory
            exchange.getIn().setBody(null);
        }
        LocalDateTime now = LocalDateTime.now();
        routeContext.setStart(now.atZone(ZoneId.systemDefault()) // timezone
                .toInstant()
                .toEpochMilli());
        exchange.setProperty(ContextKey.routeContext, routeContext);

        log.info("routeContext: {}", routeContext);
    }

    protected JobParameters createInboundJobParameters(AppConfig.BulkRoute bulkRoute, Exchange exchange) {
        log.info("create inbound job parameters ...");
        try {
            InboundContext routeContext = exchange.getProperty("routeContext", InboundContext.class);
            if (ObjectUtils.isEmpty(routeContext)) {
                throw new BulkProcessingException("routeContext is missing from Camel exchange",
                        new Throwable("routeContext is missing from Camel exchange"));
            }
            JobParameters params = new JobParametersBuilder()
                    .addString(ContextKey.country, routeContext.getCountry().countryCode)
                    .addString(ContextKey.routeName, bulkRoute.getRouteName())
                    .addString(ContextKey.routeConfig, objMapper.writeValueAsString(bulkRoute))
                    // channel
                    // bank entity
                    .addString(ContextKey.sourcePath, routeContext.getSourcePath())
                    .addString(ContextKey.sourceName, routeContext.getSourceName())
                    .toJobParameters();
            log.info("job parameters: {}", params);
            return params;
        } catch (JsonProcessingException e) {
            throw new BulkProcessingException("Error creating job parameters", e);
        }

    }

    protected void handleInboundJobExecution(AppConfig.BulkRoute bulkRoute, Exchange exchange,
            JobExecution jobExecution) {
        InboundContext routeContext = exchange.getProperty("routeContext", InboundContext.class);
        if (ObjectUtils.isEmpty(routeContext)) {
            throw new BulkProcessingException("routeContext is missing from Camel exchange",
                    new Throwable("routeContext is missing from Camel exchange"));
        }

        ExitStatus status = jobExecution.getExitStatus();
        exchange.getIn().setHeader("exitStatus", status);
        if (!ExitStatus.COMPLETED.equals(status)) {
            Optional.of(jobExecution.getExitStatus()).ifPresent(v -> routeContext.setErrorMsg(v.getExitDescription()));
        }

        if (jobExecution.getExecutionContext().containsKey(ContextKey.result)) {
            routeContext.setInboundProcessingResult(
                    jobExecution.getExecutionContext().get(ContextKey.result, Pain001InboundProcessingResult.class));

        }
        LocalDateTime now = LocalDateTime.now();
        routeContext.setEnd(now.atZone(ZoneId.systemDefault()) // timezone
                .toInstant()
                .toEpochMilli());

        log.info("routeContext: {}", routeContext);
        updateProcessingStatus(bulkRoute, exchange, status);
    }

    // Outbound Job
    protected String buildOutboundToUri(AppConfig.BulkRoute bulkRoute) {
        switch (bulkRoute.getDestinationType()) {
            case FILE :
                AppConfig.FileDestination f = bulkRoute.getFileDestination();
                String fileUri = "file:%s?fileName=%s" + "&tempFileName=%s" + "&doneFileName=%s" + "&autoCreate=%b"
                        + "&fileExist=%s" + "&moveExisting=%s" + "&eagerDeleteTargetFile=%b" + "&delete=%b"
                        + "&chmod=%s";
                String uri = String.format(fileUri, f.getFileName(), f.getTempFileName(), f.getDoneFileName(),
                        f.isAutoCreate(), f.getFileExist(), f.getMoveExisting(), f.isEagerDeleteTargetFile(),
                        f.isDelete(), f.getChmod());
                log.info("outboundToUri: " + uri);
                return uri;
            default :
                log.error("Unsupported source type: {}", bulkRoute.getSourceType());
                throw new BulkProcessingException("source type not supported",
                        new Throwable("Unsupported source type: " + bulkRoute.getSourceType()));
        }
    }

    protected void updateProcessingStatus(AppConfig.BulkRoute bulkRoute, Exchange exchange, ExitStatus status) {
        InboundContext routeContext = exchange.getProperty(ContextKey.routeContext, InboundContext.class);
        // ToDo: update transit message?
        // ToDo: notification
    }

    protected Job createJob(AppConfig.BulkRoute bulkRoute, Exchange exchange) {
        JobBuilder jobBuilder = new JobBuilder(bulkRoute.getRouteName() + "Job", jobRepository);
        jobBuilder.listener(new JobExecutionListener() {
            public void beforeJob(JobExecution jobExecution) {
                ExecutionContext executionContext = jobExecution.getExecutionContext();
                if (bulkRoute.getProcessingType() == AppConfig.ProcessingType.INBOUND) {
                    // Inbound
                    InboundContext routeContext = exchange.getProperty(ContextKey.routeContext, InboundContext.class);
                    executionContext.put(ContextKey.routeName, bulkRoute.getRouteName());
                    executionContext.put(ContextKey.routeConfig, bulkRoute);
                    executionContext.put(ContextKey.country, routeContext.getCountry());
                    // channel
                    // bank entity
                    executionContext.put(ContextKey.sourcePath, routeContext.getSourcePath());
                    executionContext.put(ContextKey.sourceName, routeContext.getSourceName());
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
            }
        });

        List<String> stepNames = bulkRoute.getSteps();
        if (stepNames == null || stepNames.isEmpty()) {
            throw new BulkProcessingException("No steps defined for route: " + bulkRoute.getRouteName(),
                    new Throwable("no steps defined"));
        }

        Step firstStep = createStepForName(stepNames.get(0), bulkRoute);
        SimpleJobBuilder simpleJobBuilder = jobBuilder.start(firstStep);

        for (int i = 1; i < stepNames.size(); i++) {
            Step step = createStepForName(stepNames.get(i), bulkRoute);
            simpleJobBuilder.next(step);
        }

        return simpleJobBuilder.build();
    }

    protected Step createStepForName(String stepName, AppConfig.BulkRoute bulkRoute) {

        log.info("Creating step: {} for route: {}", stepName, bulkRoute.getRouteName());

        StepBuilder stepBuilder = new StepBuilder(stepName, jobRepository);
        switch (stepName) {
            case "pain001-processing" :
                return createPain001ProcessingStep(stepBuilder, bulkRoute);
            case "payment-debulk" :
                return createPaymentDebulkStep(stepBuilder, bulkRoute);
            case "payment-validation" :
                return createPaymentValidationStep(stepBuilder, bulkRoute);
            case "payment-enrichment" :
                return createPaymentEnrichmentStep(stepBuilder, bulkRoute);
            case "payment-save" :
                return createPaymentSaveStep(stepBuilder, bulkRoute);
            default :
                throw new BulkProcessingException("Unknown step: " + stepName, new Throwable("Unknown step"));
        }
    }

    // implement steps

    // pain001 file processing (tasklet)
    protected Step createPain001ProcessingStep(StepBuilder stepBuilder, AppConfig.BulkRoute bulkRoute) {
        log.info("createPain001ValidationStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            log.info("pain001-processing starts ...");
            pain001InboundService.beforeStep(chunkContext.getStepContext().getStepExecution());
            String sourcePath = Optional.ofNullable(jobContext.getString(ContextKey.sourcePath))
                    .filter(path -> !StringUtils.isEmpty(path))
                    .orElseThrow(() -> new BulkProcessingException("sourcePath is missing from job context",
                            new Throwable("sourcePath is missing from job context: "
                                    + chunkContext.getStepContext().getJobName())));
            String fileContent = Files.readString(Paths.get(sourcePath));
            if (StringUtils.isBlank(fileContent)) {
                throw new BulkProcessingException("File not found", new Throwable("File not found: " + sourcePath));
            }
            // convert to business objects for further processing
            List<PaymentInformation> paymentInformations = pain001InboundService.processPain001(fileContent);
            if (CollectionUtils.isEmpty(paymentInformations)) {
                contribution.setExitStatus(ExitStatus.NOOP);
            } else {
                jobContext.put(ContextKey.paymentInformations, paymentInformations);
            }
            log.info("pain001-processing ends");

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    // createPaymentSplittingStep
    @SuppressWarnings("unchecked")
    protected Step createPaymentDebulkStep(StepBuilder stepBuilder, AppConfig.BulkRoute bulkRoute) {
        log.info("createPaymentDebulkStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // debulk payment
            log.info("payment-debulk starts ...");
            pain001InboundService.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInfos = (List<PaymentInformation>) jobContext
                    .get(ContextKey.paymentInformations);
            List<PaymentInformation> debulked = pain001InboundService.debulk(paymentInfos);
            if (CollectionUtils.isEmpty(debulked)) {
                contribution.setExitStatus(ExitStatus.FAILED);
            } else {
                jobContext.put(ContextKey.paymentInformations, debulked);
            }
            log.info("payment-debulk ends");

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    @SuppressWarnings("unchecked")
    protected Step createPaymentValidationStep(StepBuilder stepBuilder, AppConfig.BulkRoute bulkRoute) {
        log.info("createPaymentValidationStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // validate payment
            log.info("payment-validation starts ...");
            pain001InboundService.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInfos = (List<PaymentInformation>) jobContext
                    .get(ContextKey.paymentInformations);
            List<PaymentInformation> validated = pain001InboundService.validate(paymentInfos);
            if (CollectionUtils.isEmpty(validated)) {
                contribution.setExitStatus(ExitStatus.FAILED);
            } else {
                jobContext.put(ContextKey.paymentInformations, validated);
            }
            log.info("payment-validation ends");

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    @SuppressWarnings("unchecked")
    protected Step createPaymentEnrichmentStep(StepBuilder stepBuilder, AppConfig.BulkRoute bulkRoute) {
        log.info("createPaymentEnrichmentStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // enrich payment
            log.info("payment-enrichment starts ...");
            pain001InboundService.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInfos = (List<PaymentInformation>) jobContext
                    .get(ContextKey.paymentInformations);
            List<PaymentInformation> enriched = pain001InboundService.enrich(paymentInfos);
            if (CollectionUtils.isEmpty(enriched)) {
                contribution.setExitStatus(ExitStatus.FAILED);
            } else {
                jobContext.put(ContextKey.paymentInformations, enriched);
            }
            log.info("payment-enrichment ends");

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    @SuppressWarnings("unchecked")
    protected Step createPaymentSaveStep(StepBuilder stepBuilder, AppConfig.BulkRoute bulkRoute) {
        log.info("createPaymentSaveStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // save payment
            log.info("payment-save starts ...");
            pain001InboundService.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInfos = (List<PaymentInformation>) jobContext
                    .get(ContextKey.paymentInformations);
            List<PaymentInformation> saved = pain001InboundService.save(paymentInfos);
            if (CollectionUtils.isEmpty(saved)) {
                contribution.setExitStatus(ExitStatus.FAILED);
            } else {
                jobContext.put(ContextKey.paymentInformations, saved);
            }
            log.info("payment-save ends");

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }
}

```
