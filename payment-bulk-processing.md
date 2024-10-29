# BatchConfig
```java
@EnableBatchProcessing
public class BatchConfig {

    @Value("${spring.batch.jdbc.initialize-schema:false}")
    private boolean initializeSchema;

    @Bean
    public PlatformTransactionManager transactionManager(@Qualifier("defaultDataSource") DataSource dataSource) {
        return initializeSchema ? new DataSourceTransactionManager(dataSource) : new ResourcelessTransactionManager();
    }

    @Bean
    public JobRepository jobRepository(PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
        factory.setTablePrefix("batch_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobExplorer jobExplorer(PlatformTransactionManager transactionManager) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setTransactionManager(transactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();

    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
```

# flow builder 
```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BulkProcessingFlowBuilder extends RouteBuilder {

    private final BulkRoutesConfig bulkRoutesConfig;
    private final ObjectMapper mapper;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final PlatformTransactionManager platformTransactionManager;

    private final Pain001Service pain001Service;

    @Override
    public void configure() throws Exception {
        for (RouteConfig routeConfig : bulkRoutesConfig.getRoutes()) {
            if (routeConfig.isEnabled()) {
                configureRoute(routeConfig);
            }
        }
    }

    private void configureRoute(RouteConfig routeConfig) throws Exception {
        log.info("creating processing flow: {}", routeConfig.toString());
        if (routeConfig.getProcessingType() == ProcessingType.INBOUND) {
            onException(BulkProcessingException.class).handled(true).process(exchange -> {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                log.error("Processing failed for file: {}. Error: {}", fileName, cause.getMessage(), cause);
                updateProcessingStatus(routeConfig, exchange, ProcessingStatus.FAILED);
            });
            // define the route
            from(buildInboundFromUri(routeConfig)).routeId(routeConfig.getRouteName())
                    .process(exchange -> prepareInboundContext(routeConfig, exchange))
                    .process(exchange -> {
                        // Create and run the batch job
                        Job job = createJob(routeConfig, exchange);
                        JobParameters jobParameters = createInboundJobParameters(routeConfig, exchange);
                        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                        handleInboundJobExecution(routeConfig, exchange, jobExecution);
                    });
        } else {
            // OUTBOUND
            throw new BulkProcessingException("outbound flow yet to implement",
                    new Throwable("outbound flow yet to implement"));
        }
    }

    private String buildInboundFromUri(RouteConfig routeConfig) {
        switch (routeConfig.getSourceType()) {
            case FILE :
                BulkRoutesConfig.FileSource f = routeConfig.getFileSource();
                String fileUri = "file:%s?antInclude=%s" + "&antExclude=%s" + "&charset=%s" + "&doneFileName=%s"
                        + "&delay=%d" + "&sortBy=%s" + "&maxMessagesPerPoll=%d" + "&noop=%b" + "&recursive=%b"
                        + "&move=%s" + "&moveFailed=%s" // This handles failed file movement automatically
                        + "&readLock=%s" + "&readLockTimeout=%d" + "&readLockLoggingLevel=%s";

                String uri = String.format(fileUri, f.getDirectoryName(), f.getAntInclude(), f.getAntExclude(),
                        f.getCharset(), f.getDoneFileName(), f.getDelay(), f.getSortBy(), f.getMaxMessagesPerPoll(),
                        f.isNoop(), f.isRecursive(), f.getMove(), f.getMoveFailed(), f.getReadLock(),
                        f.getReadLockTimeout(), f.getReadLockInterval());

                log.info("inboundFromUri: " + uri);
                return uri;
            default :
                log.error("Unsupported source type: {}", routeConfig.getSourceType());
                throw new BulkProcessingException("Unsupported source type",
                        new Throwable("Unsupported source type: " + routeConfig.getSourceType()));
        }
    }

    // Inbound Job
    private void prepareInboundContext(RouteConfig routeConfig, Exchange exchange) {
        // Create execution context with necessary data
        log.info("prepare inbound context ...");
        InboundContext routeContext = new InboundContext();
        routeContext.setRouteConfig(routeConfig);
        routeContext.setFileName((String) exchange.getIn().getHeader(Exchange.FILE_NAME));
        routeContext.setFormat("json");
        LocalDateTime now = LocalDateTime.now();
        routeContext.setStart(now.atZone(ZoneId.systemDefault()) // timezone
                .toInstant()
                .toEpochMilli());
        exchange.setProperty("routeContext", routeContext);
    }

    private JobParameters createInboundJobParameters(RouteConfig routeConfig, Exchange exchange) {
        log.info("create inbound job parameters ...");
        try {
            InboundContext routeContext = exchange.getProperty("routeContext", InboundContext.class);
            return new JobParametersBuilder().addString("routeName", routeConfig.getRouteName())
                    .addString("routeConfig", mapper.writeValueAsString(routeConfig))
                    .addString("sourceFilePath", routeContext.getSourceFilePath())
                    .addString("fileName", routeContext.getFileName())
                    .toJobParameters();
        } catch (JsonProcessingException e) {
            throw new BulkProcessingException("Error creating job parameters", e);
        }
    }

    private void handleInboundJobExecution(RouteConfig routeConfig, Exchange exchange, JobExecution jobExecution) {
        InboundContext routeContext = exchange.getProperty("routeContext", InboundContext.class);
        ProcessingStatus status = jobExecution.getStatus().equals(BatchStatus.COMPLETED)
                ? ProcessingStatus.SUCCESS
                : ProcessingStatus.FAILED;
        if (jobExecution.getExecutionContext().containsKey("result")) {
            if (ObjectUtils.isNotEmpty(routeContext)) {
                routeContext.setPain001InboundProcessingResult(
                        jobExecution.getExecutionContext().get("result", Pain001InboundProcessingResult.class));
                LocalDateTime now = LocalDateTime.now();
                routeContext.setEnd(now.atZone(ZoneId.systemDefault()) // timezone
                        .toInstant()
                        .toEpochMilli());
            }
        }
        exchange.getIn().setHeader("processingStatus", status);

        log.info("routeConfig: {}", routeContext.getRouteConfig());
        log.info("sourceFilePath: {}", routeContext.getSourceFilePath());
        log.info("fileName: {}", routeContext.getFileName());
        log.info("format: {}", routeContext.getFormat());
        log.info("start: {}",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(routeContext.getStart()), ZoneId.systemDefault()));
        log.info("end: {}",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(routeContext.getEnd()), ZoneId.systemDefault()));
        log.info("result: {}", routeContext.getPain001InboundProcessingResult());

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            updateProcessingStatus(routeConfig, exchange, ProcessingStatus.SUCCESS);
        } else {
            ExitStatus exitStatus = jobExecution.getExitStatus();
            String errorMessage = exitStatus.getExitDescription();
            exchange.setProperty("errorMessage", errorMessage);
            updateProcessingStatus(routeConfig, exchange, ProcessingStatus.FAILED);
        }
    }
    // Outbound Job
    private String buildOutboundToUri(RouteConfig routeConfig) {
        switch (routeConfig.getDestinationType()) {
            case FILE :
                BulkRoutesConfig.FileDestination f = routeConfig.getFileDestination();
                String fileUri = "file:%s?fileName=%s" + "&tempFileName=%s" + "&doneFileName=%s" + "&autoCreate=%b"
                        + "&fileExist=%s" + "&moveExisting=%s" + "&eagerDeleteTargetFile=%b" + "&delete=%b"
                        + "&chmod=%s";

                String uri = String.format(fileUri, f.getFileName(), f.getTempFileName(), f.getDoneFileName(),
                        f.isAutoCreate(), f.getFileExist(), f.getMoveExisting(), f.isEagerDeleteTargetFile(),
                        f.isDelete(), f.getChmod());

                log.info("outboundToUri: " + uri);
                return uri;
            default :
                log.error("Unsupported source type: {}", routeConfig.getSourceType());
                throw new BulkProcessingException("Unsupported source type",
                        new Throwable("Unsupported source type: " + routeConfig.getSourceType()));
        }
    }

    private void updateProcessingStatus(RouteConfig routeConfig, Exchange exchange, ProcessingStatus status) {
        // ToDo: update status
        // ToDo: notification
    }

    private Job createJob(RouteConfig routeConfig, Exchange exchange) {
        JobBuilder jobBuilder = new JobBuilder(routeConfig.getRouteName() + "Job", jobRepository);
        jobBuilder.listener(new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                ExecutionContext executionContext = jobExecution.getExecutionContext();
                if (routeConfig.getProcessingType() == ProcessingType.INBOUND) {
                    // INBOUND
                    InboundContext routeContext = exchange.getProperty("routeContext", InboundContext.class);
                    executionContext.put("routeConfig", routeConfig);
                    executionContext.put("sourceFilePath", routeContext.getSourceFilePath());
                    executionContext.put("fileName", routeContext.getFileName());
                    executionContext.put("result", new Pain001InboundProcessingResult());
                } else {
                    // ToDo: OUTBOUND
                }
                log.info("Job started successfully");
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    log.info("Job completed successfully: {}", jobExecution.getJobInstance().getJobName());
                } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                    log.error("Job failed: {}", jobExecution.getJobInstance().getJobName());
                    log.error("errorMessage: {}", jobExecution.getExitStatus().getExitDescription());
                }
            }
        });

        List<String> stepNames = routeConfig.getSteps();
        if (stepNames == null || stepNames.isEmpty()) {
            throw new BulkProcessingException("No steps defined for route: " + routeConfig.getRouteName(),
                    new Throwable("no steps defined"));
        }

        Step firstStep = createStepForName(stepNames.get(0), routeConfig);
        SimpleJobBuilder simpleJobBuilder = jobBuilder.start(firstStep);

        for (int i = 1; i < stepNames.size(); i++) {
            Step step = createStepForName(stepNames.get(i), routeConfig);
            simpleJobBuilder.next(step);
        }

        return simpleJobBuilder.build();
    }

    private Step createStepForName(String stepName, RouteConfig routeConfig) {

        log.info("Creating step: {} for route: {}", stepName, routeConfig.getRouteName());

        StepBuilder stepBuilder = new StepBuilder(stepName, jobRepository);
        switch (stepName) {
            case "pain001-validation" :
                return createPain001ValidationStep(stepBuilder, routeConfig);
            case "payment-debulk" :
                return createPaymentDebulkStep(stepBuilder, routeConfig);
            case "payment-validation" :
                return createPaymentValidationStep(stepBuilder, routeConfig);
            case "payment-enrichment" :
                return createPaymentEnrichmentStep(stepBuilder, routeConfig);
            default :
                throw new BulkProcessingException("Unknown step: " + stepName, new Throwable("Unknown step"));
        }
    }

    // implement steps

    // pain001 file processing (tasklet)
    private Step createPain001ValidationStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        log.info("createPain001ValidationStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            pain001Service.beforeStep(chunkContext.getStepContext().getStepExecution());
            String sourceFilePath = jobContext.getString("sourceFilePath");
            String fileContent = Files.readString(Paths.get(sourceFilePath));
            if (StringUtils.isBlank(fileContent)) {
                throw new BulkProcessingException("File content not found", new Throwable("File content not found"));
            }
            // convert to business objects for further processing
            List<PaymentInformation> paymentInformations = pain001Service.validateJson(fileContent);
            jobContext.put("paymentInformations", paymentInformations);

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    // createPaymentSplittingStep
    @SuppressWarnings("unchecked")
    private Step createPaymentDebulkStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        log.info("createPaymentDebulkStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // debulk payment
            pain001Service.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInformations = (List<PaymentInformation>) jobContext
                    .get("paymentInformations");
            List<PaymentInformation> debulkPaymentInformations = pain001Service.debulk(paymentInformations);
            jobContext.put("paymentInformations", debulkPaymentInformations);

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    @SuppressWarnings("unchecked")
    private Step createPaymentValidationStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        log.info("createPaymentValidationStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // validate payment
            pain001Service.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInformations = (List<PaymentInformation>) jobContext
                    .get("paymentInformations");
            List<PaymentInformation> validatedPaymentInformations = pain001Service.validate(paymentInformations);
            jobContext.put("paymentInformations", validatedPaymentInformations);

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    @SuppressWarnings("unchecked")
    private Step createPaymentEnrichmentStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        log.info("createPaymentEnrichmentStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // enrich payment
            pain001Service.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInformations = (List<PaymentInformation>) jobContext
                    .get("paymentInformations");
            List<PaymentInformation> enrichedPaymentInformations = pain001Service.enrich(paymentInformations);
            jobContext.put("paymentInformations", enrichedPaymentInformations);

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

    @SuppressWarnings("unchecked")
    private Step createPaymentSaveStep(StepBuilder stepBuilder, RouteConfig routeConfig) {
        log.info("createPaymentSaveStep ...");
        return stepBuilder.tasklet((contribution, chunkContext) -> {
            ExecutionContext jobContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

            // save payment
            pain001Service.beforeStep(chunkContext.getStepContext().getStepExecution());
            List<PaymentInformation> paymentInformations = (List<PaymentInformation>) jobContext
                    .get("paymentInformations");
            pain001Service.save(paymentInformations);

            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }
}
 
```

# pain001Service
```java
@RequiredArgsConstructor
@Slf4j
@Service
public class Pain001ServiceImpl extends StepAwareService implements Pain001Service {

    private final ObjectMapper objectMapper;

    @Override
    public List<PaymentInformation> validatePain001(String json) {
        Pain001 pain001 = null;
        try {
            pain001 = objectMapper.readValue(json, Pain001.class);
        } catch (JsonProcessingException e) {
            throw new BulkProcessingException("Error on parsing pain001 json", e);
        }
        String fileStatus = pain001.getBusinessDocument()
                .getCustomerCreditTransferInitiation()
                .getGroupHeader()
                .getFilestatus();
        // ToDo: stop at fileStatus = 02
        // ToDo: map json to bo
        return null;
    }

    @Override
    public List<PaymentInformation> debulk(List<PaymentInformation> paymentInformations) {
        // ToDo: debulk payment
        return null;
    }

    @Override
    public List<PaymentInformation> validate(List<PaymentInformation> paymentInformations) {
        // ToDo: validate payment
        return null;
    }

    @Override
    public List<PaymentInformation> enrich(List<PaymentInformation> paymentInformations) {
        // ToDo: enrich payment
        return null;
    }

    @Value("${batch.size:1000}")
    private int batchSize;

    @Value("${retry.max.attempts:3}")
    private int maxRetryAttempts;

    @Value("${retry.delay.ms:1000}")
    private long retryDelayMs;

    @Override
    @Transactional
    public void save(List<PaymentInformation> paymentInformations) {
        for (PaymentInformation paymentInfo : paymentInformations) {
            if (!paymentInfo.isValid()) {
                log.warn("Skipping invalid payment information");
                continue;
            }

            try {
                processPaymentInformation(paymentInfo);
            } catch (Exception e) {
                log.error("Error processing payment information: ", e);
                paymentInfo.addValidationError("SAVE_ERROR", "Failed to save payment: " + e.getMessage());
            }
        }
    }

    private void processPaymentInformation(PaymentInformation paymentInfo) {
        // Save parent payment first
        String parentBankRefId = saveParentPayment(paymentInfo);

        // Process credit transfers in batches
        List<CreditTransferTransaction> allTransactions = paymentInfo.getCreditTransferTransactionList();
        if (allTransactions == null || allTransactions.isEmpty()) {
            return;
        }

        // Calculate total batches
        int totalTransactions = allTransactions.size();
        int totalBatches = (totalTransactions + batchSize - 1) / batchSize;
        log.info("Processing {} transactions in {} batches", totalTransactions, totalBatches);

        // Process each batch sequentially
        for (int i = 0; i < totalBatches; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, totalTransactions);
            List<CreditTransferTransaction> batch = allTransactions.subList(fromIndex, toIndex);
            
            try {
                processCreditTransferBatch(batch, parentBankRefId, i + 1);
                log.info("Completed batch {}/{}", i + 1, totalBatches);
            } catch (Exception e) {
                log.error("Error processing batch {}: ", i + 1, e);
                // Continue processing other batches despite errors
            }
        }
    }

    private String saveParentPayment(PaymentInformation paymentInfo) {
        long bankRefSeqNo = paymentDao.getBankRefSeqNo();
        String bankReferenceId = generateBankReferenceId(bankRefSeqNo);

        if (paymentInfo.getPwsTransactions() != null) {
            paymentInfo.getPwsTransactions().setBankReferenceId(bankReferenceId);
            long txnId = paymentDao.insertPwsTransactions(paymentInfo.getPwsTransactions());
            paymentInfo.getPwsTransactions().setTxnId(txnId);
        }

        if (paymentInfo.getPwsBulkTransactions() != null) {
            paymentInfo.getPwsBulkTransactions().setBankReferenceId(bankReferenceId);
            long bulkTxnId = paymentDao.insertPwsBulkTransactions(paymentInfo.getPwsBulkTransactions());
            paymentInfo.getPwsBulkTransactions().setTxnId(bulkTxnId);
        }

        return bankReferenceId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processCreditTransferBatch(
            List<CreditTransferTransaction> batch, 
            String parentBankRefId, 
            int batchNumber) {
        
        try {
            // Pre-generate bank reference IDs for the batch
            List<String> bankRefIds = generateBankReferenceIds(batch.size());

            // Prepare all batch collections
            BatchCollections batchCollections = new BatchCollections();

            // Collect all records for batch insert
            for (int i = 0; i < batch.size(); i++) {
                CreditTransferTransaction creditTransfer = batch.get(i);
                if (!creditTransfer.isValid()) {
                    continue;
                }

                String childBankRefId = bankRefIds.get(i);
                collectBatchRecords(creditTransfer, childBankRefId, parentBankRefId, batchCollections);
            }

            // Execute all batch inserts in optimal order
            executeBatchInsertsInOrder(batchCollections);
            
        } catch (Exception e) {
            log.error("Error processing batch {}: ", batchNumber, e);
            throw new PaymentProcessingException("Failed to process payment batch " + batchNumber, e);
        }
    }

    private List<String> generateBankReferenceIds(int count) {
        List<String> bankRefIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long seqNo = paymentDao.getBankRefSeqNo();
            bankRefIds.add(generateBankReferenceId(seqNo));
        }
        return bankRefIds;
    }

    @Data
    private static class BatchCollections {
        private final List<PwsBulkTransactionInstructions> instructions = new ArrayList<>();
        private final List<PwsParties> parties = new ArrayList<>();
        private final List<PwsPartyContracts> contacts = new ArrayList<>();
        private final List<PwsTaxInstruction> taxInstructions = new ArrayList<>();
        private final List<PwsTransactionAdvices> advices = new ArrayList<>();
        private final List<PwsTransactionCharges> charges = new ArrayList<>();
    }

    private void collectBatchRecords(
            CreditTransferTransaction creditTransfer,
            String childBankRefId,
            String parentBankRefId,
            BatchCollections collections) {

        // Collect transaction instructions
        if (creditTransfer.getPwsBulkTransactionInstructions() != null) {
            PwsBulkTransactionInstructions instructions = creditTransfer.getPwsBulkTransactionInstructions();
            instructions.setBankReferenceId(childBankRefId);
            instructions.setParentBankReferenceId(parentBankRefId);
            collections.getInstructions().add(instructions);
        }

        // Collect parties and contacts
        if (creditTransfer.getPwsPartiesList() != null) {
            for (PwsParties party : creditTransfer.getPwsPartiesList()) {
                party.setBankReferenceId(childBankRefId);
                collections.getParties().add(party);

                if (creditTransfer.getPwsPartyContactList() != null) {
                    for (PwsPartyContracts contact : creditTransfer.getPwsPartyContactList()) {
                        if (contact.getPartyType().equals(party.getPartyType())) {
                            contact.setBankReferenceId(childBankRefId);
                            collections.getContacts().add(contact);
                        }
                    }
                }
            }
        }

        // Collect other related records
        collectRelatedRecords(creditTransfer, childBankRefId, collections);
    }

    private void collectRelatedRecords(
            CreditTransferTransaction creditTransfer,
            String childBankRefId,
            BatchCollections collections) {
            
        if (creditTransfer.getPwsTaxInstructionList() != null) {
            for (PwsTaxInstruction tax : creditTransfer.getPwsTaxInstructionList()) {
                tax.setBankReferenceId(childBankRefId);
                collections.getTaxInstructions().add(tax);
            }
        }

        if (creditTransfer.getPwsTransactionAdvices() != null) {
            PwsTransactionAdvices advice = creditTransfer.getPwsTransactionAdvices();
            advice.setBankReferenceId(childBankRefId);
            collections.getAdvices().add(advice);
        }

        if (creditTransfer.getPwsTransactionCharges() != null) {
            PwsTransactionCharges charges = creditTransfer.getPwsTransactionCharges();
            charges.setBankReferenceId(childBankRefId);
            collections.getCharges().add(charges);
        }
    }

    private void executeBatchInsertsInOrder(BatchCollections collections) {
        try {
            // 1. Insert Instructions
            if (!collections.getInstructions().isEmpty()) {
                retryTemplate.execute(context -> {
                    log.info("Attempting to insert bulk transaction instructions. Attempt: {}", context.getRetryCount() + 1);
                    paymentDao.batchInsertInstructions(collections.getInstructions());
                    return null;
                }, context -> {
                    log.error("Failed to insert bulk transaction instructions after all retries", context.getLastThrowable());
                    throw new PaymentProcessingException("Failed to insert instructions after retries", context.getLastThrowable());
                });
            }

            // 2. Insert Parties and handle their Contacts
            Map<String, Long> partyIdsByRef = new HashMap<>();
            if (!collections.getParties().isEmpty()) {
                retryTemplate.execute(context -> {
                    log.info("Attempting to insert parties. Attempt: {}", context.getRetryCount() + 1);
                    List<PwsParties> insertedParties = paymentDao.batchInsertPartiesWithReturn(collections.getParties());
                    // Store party IDs for contact processing
                    for (PwsParties party : insertedParties) {
                        partyIdsByRef.put(party.getBankReferenceId() + "_" + party.getPartyType(), party.getPartyId());
                    }
                    return null;
                }, context -> {
                    log.error("Failed to insert parties after all retries", context.getLastThrowable());
                    throw new PaymentProcessingException("Failed to insert parties after retries", context.getLastThrowable());
                });

                // Process contacts for each party type
                for (Map.Entry<String, List<PwsPartyContracts>> entry : collections.getContactsByPartyType().entrySet()) {
                    String partyType = entry.getKey();
                    List<PwsPartyContracts> contacts = entry.getValue();
                    
                    // Set party IDs for contacts
                    for (PwsPartyContracts contact : contacts) {
                        String partyKey = contact.getBankReferenceId() + "_" + partyType;
                        Long partyId = partyIdsByRef.get(partyKey);
                        if (partyId != null) {
                            contact.setPartyId(partyId);
                        } else {
                            log.warn("No party ID found for contact: {}", partyKey);
                        }
                    }

                    // Insert contacts with retry
                    final String currentPartyType = partyType;
                    retryTemplate.execute(context -> {
                        log.info("Attempting to insert contacts for party type {}. Attempt: {}", 
                            currentPartyType, context.getRetryCount() + 1);
                        paymentDao.batchInsertContacts(contacts);
                        return null;
                    }, context -> {
                        log.error("Failed to insert contacts for party type {} after all retries", 
                            currentPartyType, context.getLastThrowable());
                        throw new PaymentProcessingException(
                            "Failed to insert contacts for party type " + currentPartyType, 
                            context.getLastThrowable());
                    });
                }
            }

            // 3. Insert remaining related records
            if (!collections.getTaxInstructions().isEmpty()) {
                retryTemplate.execute(context -> {
                    log.info("Attempting to insert tax instructions. Attempt: {}", context.getRetryCount() + 1);
                    paymentDao.batchInsertTaxInstructions(collections.getTaxInstructions());
                    return null;
                }, context -> {
                    log.error("Failed to insert tax instructions after all retries", context.getLastThrowable());
                    throw new PaymentProcessingException("Failed to insert tax instructions after retries", 
                        context.getLastThrowable());
                });
            }

            if (!collections.getAdvices().isEmpty()) {
                retryTemplate.execute(context -> {
                    log.info("Attempting to insert transaction advices. Attempt: {}", context.getRetryCount() + 1);
                    paymentDao.batchInsertAdvices(collections.getAdvices());
                    return null;
                }, context -> {
                    log.error("Failed to insert transaction advices after all retries", context.getLastThrowable());
                    throw new PaymentProcessingException("Failed to insert advices after retries", 
                        context.getLastThrowable());
                });
            }

            if (!collections.getCharges().isEmpty()) {
                retryTemplate.execute(context -> {
                    log.info("Attempting to insert transaction charges. Attempt: {}", context.getRetryCount() + 1);
                    paymentDao.batchInsertCharges(collections.getCharges());
                    return null;
                }, context -> {
                    log.error("Failed to insert transaction charges after all retries", context.getLastThrowable());
                    throw new PaymentProcessingException("Failed to insert charges after retries", 
                        context.getLastThrowable());
                });
            }

        } catch (Exception e) {
            log.error("Failed to execute batch inserts", e);
            throw new PaymentProcessingException("Failed to execute batch inserts", e);
        }
    }
    
    // Add these methods to PaymentDao
    private void insertBulkTransactionInstructionsBatch(List<PwsBulkTransactionInstructions> batch) {
        SqlSession sqlSession = getSqlSession();
        try {
            BatchExecutor executor = new BatchExecutor(sqlSession);
            executor.executeBatch("insertPwsBulkTransactionInstructions", batch, batchSize);
        } finally {
            sqlSession.close();
        }
    }

    private void insertPartiesBatch(List<PwsParties> batch) {
        SqlSession sqlSession = getSqlSession();
        try {
            BatchExecutor executor = new BatchExecutor(sqlSession);
            executor.executeBatch("insertPwsParties", batch, batchSize);
        } finally {
            sqlSession.close();
        }
    }

    // Similar batch insert methods for other entities...

    private String generateBankReferenceId(long seqNo) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s%s%011d", 
            bankReferencePrefix,
            now.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            seqNo);
    }

}

```

# pain001
```java
@NoArgsConstructor
@Data
public class Pain001 {
    @JsonProperty("businessDocument")
    private BusinessDocumentDTO businessDocument;
}

@NoArgsConstructor
@Data
public class BusinessDocumentDTO {
    @JsonProperty("customerCreditTransferInitiation")
    private CustomerCreditTransferInitiationDTO customerCreditTransferInitiation;
}

@NoArgsConstructor
 @Data
 public class CustomerCreditTransferInitiationDTO {
     @JsonProperty("groupHeader")
     private GroupHeaderDTO groupHeader;
     @JsonProperty("paymentInformation")
     private List<PaymentInformationDTO> paymentInformation;
 }
```

# payment info
```java

@Data
public class PaymentInformantion {

    PwsTransactions pwsTransactions;
    PwsBulkTransactions pwsBulkTransactions;

    List<CreditTransferTransaction> creditTransferTransactions;

}
```

# credit transfer txn info
```java
 @Data
 public class CreditTransferTransaction {

     PwsBulkTransactionInstructions pwsBulkTransactionInstructions;
     PwsParties pwsParties;
     PwsTransactionAdvices pwsTransactionAdvices;
     PwsTransactionCharges pwsTransactionCharges;
     
}
```

# mapper
```java
package com.example.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class Pain001Mapper {

    /**
     * Maps Pain001 DTO to a list of PaymentInformation business objects
     */
    public List<PaymentInformation> mapToBOs(Pain001 pain001) {
        List<PaymentInformation> paymentInfoList = new ArrayList<>();
        
        try {
            List<PaymentInformationDTO> paymentInfoDTOs = pain001.getBusinessDocument()
                .getCustomerCreditTransferInitiation()
                .getPaymentInformation();

            // Map each payment information entry
            for (PaymentInformationDTO paymentInfoDTO : paymentInfoDTOs) {
                PaymentInformation paymentInfo = mapSinglePaymentInformation(paymentInfoDTO);
                paymentInfoList.add(paymentInfo);
            }
        } catch (Exception e) {
            log.error("Error mapping Pain001 to BOs: ", e);
            // Create an error payment information object
            PaymentInformation errorPaymentInfo = new PaymentInformation();
            errorPaymentInfo.addValidationError("MAPPING_ERROR", "Failed to map Pain001 to business objects: " + e.getMessage());
            paymentInfoList.add(errorPaymentInfo);
        }

        return paymentInfoList;
    }

    /**
     * Maps a single PaymentInformationDTO to PaymentInformation business object
     */
    private PaymentInformation mapSinglePaymentInformation(PaymentInformationDTO paymentInfoDTO) {
        PaymentInformation paymentInfo = new PaymentInformation();
        
        try {
            // Map PwsBulkTransactions
            PwsBulkTransactions pwsBulkTransactions = mapToPwsBulkTransactions(paymentInfoDTO);
            paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);

            // Map Credit Transfer Transactions
            List<CreditTransferTransaction> creditTransferList = new ArrayList<>();
            for (CreditTransferTransactionInformationDTO txnDTO : 
                    paymentInfoDTO.getCreditTransferTransactionInformation()) {
                creditTransferList.add(mapToCreditTransferTransaction(txnDTO, paymentInfoDTO));
            }
            paymentInfo.setCreditTransferTransactionList(creditTransferList);

            // Validate the mapped payment information
            validatePaymentInformation(paymentInfo, paymentInfoDTO);

        } catch (Exception e) {
            log.error("Error mapping single payment information: ", e);
            paymentInfo.addValidationError("MAPPING_ERROR", 
                "Failed to map payment information: " + e.getMessage());
        }

        return paymentInfo;
    }

    /**
     * Maps PaymentInformationDTO to PwsBulkTransactions
     */
    private PwsBulkTransactions mapToPwsBulkTransactions(PaymentInformationDTO paymentInfoDTO) {
        PwsBulkTransactions bulkTxn = new PwsBulkTransactions();
        
        bulkTxn.setDmpBatchNumber(paymentInfoDTO.getDmpBatchRef());
        bulkTxn.setBatchBooking(paymentInfoDTO.getBatchBooking());
        bulkTxn.setStatus(mapStatus(paymentInfoDTO.getBulkstatus()));
        bulkTxn.setRecipientsReference(paymentInfoDTO.getPaymentInformationIdentification());
        
        // Set control sum and number of transactions
        if (paymentInfoDTO.getControlSum() != null) {
            bulkTxn.setCombineDebit(paymentInfoDTO.getControlSum());
        }
        
        // Set transfer date from requested execution date
        if (paymentInfoDTO.getRequestedExecutionDate() != null) {
            bulkTxn.setTransferDate(parseDate(paymentInfoDTO.getRequestedExecutionDate().getDate()));
        }

        return bulkTxn;
    }

    /**
     * Maps CreditTransferTransactionInformationDTO to CreditTransferTransaction
     */
    private CreditTransferTransaction mapToCreditTransferTransaction(
            CreditTransferTransactionInformationDTO txnDTO,
            PaymentInformationDTO paymentInfoDTO) {
        
        CreditTransferTransaction creditTransfer = new CreditTransferTransaction();

        try {
            // Map bulk transaction instructions
            PwsBulkTransactionInstructions bulkInstructions = 
                mapToPwsBulkTransactionInstructions(txnDTO, paymentInfoDTO);
            creditTransfer.setPwsBulkTransactionInstructions(bulkInstructions);

            // Map parties
            creditTransfer.setPwsPartiesList(mapToParties(txnDTO, paymentInfoDTO));

            // Map transaction advices
            creditTransfer.setPwsTransactionAdvices(mapToTransactionAdvices(txnDTO));

            // Map transaction charges
            creditTransfer.setPwsTransactionCharges(mapToTransactionCharges(txnDTO));

            // Map tax instructions if available
            if (txnDTO.getSupplementaryData() != null) {
                creditTransfer.setPwsTaxInstructionList(mapToTaxInstructions(txnDTO.getSupplementaryData()));
            }

        } catch (Exception e) {
            log.error("Error mapping credit transfer transaction: ", e);
            creditTransfer.addValidationError("MAPPING_ERROR", 
                "Failed to map credit transfer: " + e.getMessage());
        }

        return creditTransfer;
    }

    /**
     * Maps transaction DTOs to Party list
     */
    private List<PwsParties> mapToParties(
            CreditTransferTransactionInformationDTO txnDTO,
            PaymentInformationDTO paymentInfoDTO) {
        
        List<PwsParties> parties = new ArrayList<>();

        // Map Debtor Party
        if (paymentInfoDTO.getDebtor() != null) {
            PwsParties debtorParty = new PwsParties();
            debtorParty.setPartyType("DBTR");
            debtorParty.setName(paymentInfoDTO.getDebtor().getName());
            // Map other debtor details...
            parties.add(debtorParty);
        }

        // Map Creditor Party
        if (txnDTO.getCreditor() != null) {
            PwsParties creditorParty = new PwsParties();
            creditorParty.setPartyType("CDTR");
            creditorParty.setName(txnDTO.getCreditor().getName());
            // Map other creditor details...
            parties.add(creditorParty);
        }

        return parties;
    }

    /**
     * Maps transaction DTO to PwsTransactionAdvices
     */
    private PwsTransactionAdvices mapToTransactionAdvices(CreditTransferTransactionInformationDTO txnDTO) {
        PwsTransactionAdvices advices = new PwsTransactionAdvices();
        
        if (txnDTO.getInstructionForCreditorAgent() != null && !txnDTO.getInstructionForCreditorAgent().isEmpty()) {
            // Map advice details from instructions
            InstructionForCreditorAgentDTO instruction = txnDTO.getInstructionForCreditorAgent().get(0);
            // Map instruction details to advices...
        }

        return advices;
    }

    /**
     * Maps transaction DTO to PwsTransactionCharges
     */
    private PwsTransactionCharges mapToTransactionCharges(CreditTransferTransactionInformationDTO txnDTO) {
        PwsTransactionCharges charges = new PwsTransactionCharges();
        charges.setChargeBearer(txnDTO.getChargeBearer());
        return charges;
    }

    /**
     * Helper method to map status codes
     */
    private String mapStatus(String status) {
        // Map status codes between systems
        return Optional.ofNullable(status)
                .map(s -> switch (s) {
                    case "ACTC" -> "ACCEPTED";
                    case "PDNG" -> "PENDING";
                    case "RJCT" -> "REJECTED";
                    case "ACCP" -> "ACCEPTED";
                    case "ACSP" -> "PROCESSING";
                    case "ACSC" -> "COMPLETED";
                    case "ACWC" -> "COMPLETED_WITH_WARNING";
                    default -> "UNKNOWN";
                })
                .orElse("UNKNOWN");
    }

    /**
     * Helper method to parse date string to Date
     */
    private java.util.Date parseDate(String dateStr) {
        try {
            return java.sql.Date.valueOf(dateStr);
        } catch (Exception e) {
            log.error("Error parsing date: {}", dateStr, e);
            return null;
        }
    }

    /**
     * Helper method to parse date string to Timestamp
     */
    private Timestamp parseTimestamp(String dateStr) {
        try {
            return Timestamp.valueOf(LocalDateTime.parse(dateStr));
        } catch (Exception e) {
            log.error("Error parsing timestamp: {}", dateStr, e);
            return null;
        }
    }

    /**
     * Validates the mapped payment information against the DTO
     */
    private void validatePaymentInformation(
            PaymentInformation paymentInfo, 
            PaymentInformationDTO paymentInfoDTO) {
        
        // Validate control sum
        if (paymentInfoDTO.getControlSum() != null) {
            BigDecimal controlSum = new BigDecimal(paymentInfoDTO.getControlSum());
            BigDecimal actualSum = calculateActualSum(paymentInfo.getCreditTransferTransactionList());
            
            if (controlSum.compareTo(actualSum) != 0) {
                paymentInfo.addValidationError("CONTROL_SUM_MISMATCH", 
                    "Control sum does not match sum of transactions");
            }
        }

        // Validate number of transactions
        if (paymentInfoDTO.getNumberOfTransactions() != null) {
            int expectedCount = Integer.parseInt(paymentInfoDTO.getNumberOfTransactions());
            int actualCount = paymentInfo.getCreditTransferTransactionList().size();
            
            if (expectedCount != actualCount) {
                paymentInfo.addValidationError("TRANSACTION_COUNT_MISMATCH", 
                    "Number of transactions does not match expected count");
            }
        }
    }

    /**
     * Calculates the actual sum of all transactions
     */
    private BigDecimal calculateActualSum(List<CreditTransferTransaction> transactions) {
        return transactions.stream()
            .map(txn -> txn.getPwsBulkTransactionInstructions().getTransactionAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Maps supplementary data to tax instructions
     */
    private List<PwsTaxInstruction> mapToTaxInstructions(List<SupplementaryDataDTO> supplementaryData) {
        List<PwsTaxInstruction> taxInstructions = new ArrayList<>();
        
        for (SupplementaryDataDTO supData : supplementaryData) {
            if ("TAX".equals(supData.getType())) {
                PwsTaxInstruction taxInstruction = new PwsTaxInstruction();
                // Map tax instruction details
                taxInstructions.add(taxInstruction);
            }
        }
        
        return taxInstructions;
    }
}
```

# Pws Save

```java
@Service
@Slf4j
@Transactional
public class Pain001ServiceImpl implements Pain001Service {

    @Autowired 
    private PaymentDao paymentDao;
    
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    
    @Value("${jdbc.batch.size:1000}")
    private int jdbcBatchSize;

    private final RetryTemplate retryTemplate;

    @PostConstruct
    private void init() {
        this.retryTemplate = createRetryTemplate();
    }

    private void executeBatchInsertsInOrder(BatchCollections collections) {
        Map<String, Long> partyIdsByRef = new HashMap<>();
        
        try {
            // 1. Process bulk transaction instructions
            if (!collections.getInstructions().isEmpty()) {
                executeBatchInsert("insertBulkTransactionInstruction", 
                    collections.getInstructions(), 
                    null);
            }

            // 2. Process parties and store their IDs
            if (!collections.getParties().isEmpty()) {
                List<PwsParties> insertedParties = executeBatchInsertWithIds("insertParty", 
                    collections.getParties());
                
                // Store party IDs for contacts
                for (PwsParties party : insertedParties) {
                    partyIdsByRef.put(party.getBankReferenceId() + "_" + party.getPartyType(), 
                        party.getPartyId());
                }
            }

            // 3. Process contacts with party IDs
            for (Map.Entry<String, List<PwsPartyContracts>> entry : 
                    collections.getContactsByPartyType().entrySet()) {
                List<PwsPartyContracts> contacts = entry.getValue();
                setPartyIdsForContacts(contacts, partyIdsByRef);
                executeBatchInsert("insertPartyContact", contacts, null);
            }

            // 4. Process tax instructions
            if (!collections.getTaxInstructions().isEmpty()) {
                executeBatchInsert("insertTaxInstruction", 
                    collections.getTaxInstructions(), 
                    null);
            }

            // 5. Process transaction advices
            if (!collections.getAdvices().isEmpty()) {
                executeBatchInsert("insertTransactionAdvice", 
                    collections.getAdvices(), 
                    null);
            }

            // 6. Process transaction charges
            if (!collections.getCharges().isEmpty()) {
                executeBatchInsert("insertTransactionCharge", 
                    collections.getCharges(), 
                    null);
            }

        } catch (Exception e) {
            log.error("Failed to execute batch inserts", e);
            throw new PaymentProcessingException("Failed to execute batch inserts", e);
        }
    }

    /**
     * Generic batch insert method
     */
    private <T> void executeBatchInsert(String insertMethod, List<T> records, 
            Consumer<List<T>> postBatchProcessor) {
        retryTemplate.execute(context -> {
            SqlSession session = sqlSessionTemplate.getSqlSessionFactory()
                .openSession(ExecutorType.BATCH);
            
            try {
                PaymentDao mapper = session.getMapper(PaymentDao.class);
                int count = 0;
                List<T> currentBatch = new ArrayList<>();

                for (T record : records) {
                    // Use reflection to call the appropriate insert method
                    Method method = mapper.getClass().getMethod(insertMethod, record.getClass());
                    method.invoke(mapper, record);
                    currentBatch.add(record);
                    count++;
                    
                    if (count % jdbcBatchSize == 0) {
                        session.flushStatements();
                        if (postBatchProcessor != null) {
                            postBatchProcessor.accept(currentBatch);
                        }
                        currentBatch = new ArrayList<>();
                    }
                }
                
                // Process remaining records
                if (!currentBatch.isEmpty()) {
                    session.flushStatements();
                    if (postBatchProcessor != null) {
                        postBatchProcessor.accept(currentBatch);
                    }
                }
                
                session.commit();
                return null;
            } catch (Exception e) {
                session.rollback();
                throw new PaymentProcessingException("Batch insert failed", e);
            } finally {
                session.close();
            }
        });
    }

    /**
     * Generic batch insert method with ID return
     */
    private <T> List<T> executeBatchInsertWithIds(String insertMethod, List<T> records) {
        return retryTemplate.execute(context -> {
            SqlSession session = sqlSessionTemplate.getSqlSessionFactory()
                .openSession(ExecutorType.BATCH);
            List<T> processedRecords = new ArrayList<>();
            
            try {
                PaymentDao mapper = session.getMapper(PaymentDao.class);
                int count = 0;
                List<T> currentBatch = new ArrayList<>();

                for (T record : records) {
                    Method method = mapper.getClass().getMethod(insertMethod, record.getClass());
                    method.invoke(mapper, record);
                    currentBatch.add(record);
                    count++;
                    
                    if (count % jdbcBatchSize == 0) {
                        session.flushStatements();
                        processedRecords.addAll(currentBatch);
                        currentBatch = new ArrayList<>();
                    }
                }
                
                if (!currentBatch.isEmpty()) {
                    session.flushStatements();
                    processedRecords.addAll(currentBatch);
                }
                
                session.commit();
                return processedRecords;
            } catch (Exception e) {
                session.rollback();
                throw new PaymentProcessingException("Batch insert with IDs failed", e);
            } finally {
                session.close();
            }
        });
    }

    private void setPartyIdsForContacts(List<PwsPartyContracts> contacts, 
            Map<String, Long> partyIdsByRef) {
        for (PwsPartyContracts contact : contacts) {
            String partyKey = contact.getBankReferenceId() + "_" + contact.getPartyType();
            Long partyId = partyIdsByRef.get(partyKey);
            if (partyId != null) {
                contact.setPartyId(partyId);
            } else {
                log.warn("No party ID found for contact: {}", partyKey);
            }
        }
    }
}
```


# application.yml
```yml

server:
  port: ${pbp_port:8015}
  servlet:
    context-path: /paymentbulkprocessing
  shutdown: graceful
  tomcat:
    threads:
      min-spare: 100
      max: 200
    connection-timeout: 10s
    accept-count: 200
    max-connections: 600
    accesslog:
      enabled: true
      buffered: false
      rotate: true
      max-days: 90
      encoding: UTF-8
      request-attributes-enabled: true
      pattern: "%{yyyy-MM-dd HH:mm:ss.SSS}t %h %r %s %b [%D ms]"
      directory: ${log_dir}
      prefix: access_log_paymentbulkprocessing
      suffix: .log
  ssl:
    enabled: false
    client-auth:
    key-store:
    key-alias:
    key-store-type:
    key-store-password:
    key-password:
    trust-store:
    trust-store-password:
    trust-store-type:

spring:
  config:
    import: bootstrap.yml,classpath:trancommon.yml,classpath:tran-common-caches.yml,classpath:common-utils.yml
  lifecycle:
    timeout-per-shutdown-phase: 20s
  jackson:
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:8080/auth/realms/oauth2-sample/protocol/openid-connect/certs
  datasource:
    common:
      type: com.zaxxer.hikari.HikariDataSource
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        data-source-properties:
          rewriteBatchedStatement: true
          cachePrepStmts: true
          preStmtCacheSize: 250
          preStmtCacheSqlLimit: 2048
      vault:
        enabled: false
        key-store: /path/to/keystore.jks
        key-store-password: keystorePassword
        key-store-type: JKS
        trust-store: /path/to/truststore.jks
        trust-store-password: truststorePassword
        wallet-location: /path/to/wallet

    # Default datasource for Spring Batch
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

mybatis:
  mapper-locations:
    - classpath:mappers/**/*.xml
  configuration:
    jdbc-type-for-null: VARCHAR
    default-statement-timeout: 5          #public-key-location: file:${ssl_dir}/geb-sg-sso-jwt.pub

management:
  endpoint:
    prometheus:
      enabled: true
    health:
      probes:
        enabled: true
      show-details: ALWAYS
    caches:
      enabled: true
    metrics:
      enabled: true
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - prometheus
          - metrics
          - caches

country: th
country_code: TH

ufw:
  app:
    enable-db-performance-logging: true
    enable-camel-performance-logging: true
    jwt-require-claims: false
    jwt-subject-additional-fields:
      - userSegmentUUID
    jwt-claim-keys:
      - sub
      - iss
      - userSegmentUUID
    enable-entitlements: true
    enable-simple-policy-enforcement: true
    symmetric-crypto:
      secret: password
      salt: 5c0744940b5c369b
    jwt-public-keys:
      - client-id: IDBv2
        client-public-key-path: ${ssl_dir}/geb-${country}-sso-jwt.pub
        key-algorithm: rs256
    async-protocol: cew-mq
    mq-properties:

tranCommon:
  connection-per-route@: 40
  sort-per-route@: asc
  max-total-connections@: 400
  socket-timeout@: 5000
  connect-timeout@: 3000
  request-timeout@: 1000
  initiateService:
    enable@: false
  ueqs:
    defaultEntitlementRole@:
    enable@: true
    url:
      routes@: ${scheme}://${hostname}:${ueqs_port},${scheme}://${hostname}:${ueqs_routes_1}
      maximumFailoverAttempts@: 2
      base-path@: /userentitlementqueryapi
      companyAndAccount@: /api/entitlements/v1/companyAndAccounts/
      resourcesAndFeatures@: /api/entitlements/v1/resourceAndFeaturesFromJWT
      companyAndAccountsForUserResourceFeatures@: /api/entitlements/v3/companyAndAccountsForUserResourceFeatures
      resourceAndFileType@: /api/entitlements/v1/getResourceAndFileType
  rds:
    enable@: true
    url:
      routes@: ${scheme}://${hostname}:${rds_port}
      maximumFailoverAttempts@: 2
      base-path@: /banklookupapi
      v2-country-list@: /api/refData/v2/countries
      payment-codes@: /api/refData/v1/paymentCodes
      v2-banks-list@: /api/refData/v2/banks
  fx:
    enable@: true
    url:
      routes@: ${scheme}://${hostname}:${fx_port}
      maximumFailoverAttempts@: 2
      base-path@: /fxservice
      prebook-contract@: /api/foreignExchange/v1/prebookedContracts
      fxRate@: /api/foreignExchange/v1/fxRate
      computeEquivalentAmount@: /api/foreignExchange/v1/computeEquivalentAmount

bulk-routes:
  - route-name: CUSTOMER_SUBMITTED_TRANSFORMED
    processing-type: INBOUND
    source-type: FILE
    enabled: true
    steps:
      - pain001-validation
      - payment-debulk
      - payment-validation
      - payment-enrichment
      - payment-save
    file-source:
      directoryName: /path/to/inbound/processing
      antInclude: "*_Auth.json"
      antExclude:
      charset: utf-8
      doneFileName: "${file:name:noext}.xml.done"
      delay: 6000
      sortBy: file:modified
      maxMessagesPerPoll: 1
      noop: false
      recursive: false
      move: /path/to/inbound/backup
      moveFailed: /path/to/inbound/error
      readLock: rename
      readLockTimeout: 60000
      readLockInterval: 1000
      readLockLoggingLevel: WARN
    payment-save:
      datasource: datasource-pws-insertion
    notification:
      email:
        host: smtp.example.com
        port: 587
        username: notification@example.com
        password: notification_password


  - route-name: CUSTOMER_AUTHORIZED
    processing-type: OUTBOUND
    destination-type: API
    enabled: false
    steps:
      - payment-load
      - pain001-transform
    api-source:
      http-uri: /path/to/api
    payment-load:
      datasource: datasource-pws-loading
    pain001-transform:
    file-destination:
      directoryName: /path/to/outbound/processing
      fileName: "${header.CamelFileName}"
      tempFileName: "${file:name:noext}.tmp"
      doneFileName: "${file:name:noext}.xml.done"
      autoCreate: true
      fileExist: Override
      moveExisting:
      eagerDeleteTargetFile: false
      delete: true
      chmod: rw-r--r--

retry.max.attempts=3
retry.backoff.period=1000
retry.max.backoff=10000
```

```java
@Value("${retry.backoff.period:1000}")
    private long backoffPeriod;

    @Value("${retry.max.backoff:10000}")
    private long maxBackoff;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(backoffPeriod);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(maxBackoff);

        // Simple retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    // Alternatively, you can use @Retryable annotation
    @Bean
    public RetryOperationsInterceptor retryOperationsInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(backoffPeriod, 2.0, maxBackoff)
                .build();
    }
}
```

# testing
```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@SpringBootTest
@SpringBatchTest
@CamelSpringBootTest
@ActiveProfiles("test")
class BulkProcessingFlowBuilderTest {

    @Value("${test.resources.path:src/test/resources}")
    private String testResourcesPath;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private Pain001Service pain001Service;

    @TempDir
    Path tempDir;

    private Path inboundDir;
    private Path backupDir;
    private Path errorDir;
    private Path testFilesDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create test directories under temp
        inboundDir = tempDir.resolve("inbound");
        backupDir = tempDir.resolve("backup");
        errorDir = tempDir.resolve("error");
        
        Files.createDirectories(inboundDir);
        Files.createDirectories(backupDir);
        Files.createDirectories(errorDir);

        // Load test file from classpath
        testFilesDir = Paths.get(testResourcesPath);
        copyTestFilesToInbound();

        // Configure test route
        BulkRoutesConfig.FileSource fileSource = new BulkRoutesConfig.FileSource();
        fileSource.setDirectoryName(inboundDir.toString());
        fileSource.setAntInclude("*.json");
        fileSource.setMove(backupDir.toString());
        fileSource.setMoveFailed(errorDir.toString());
        fileSource.setReadLock("rename");
        fileSource.setReadLockTimeout(1000);
        fileSource.setMaxMessagesPerPoll(1);

        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setRouteName("TEST_ROUTE");
        routeConfig.setProcessingType(ProcessingType.INBOUND);
        routeConfig.setSourceType(SourceType.FILE);
        routeConfig.setEnabled(true);
        routeConfig.setFileSource(fileSource);
        routeConfig.setSteps(List.of(
            "pain001-validation",
            "payment-debulk",
            "payment-validation",
            "payment-enrichment"
        ));

        BulkRoutesConfig bulkRoutesConfig = new BulkRoutesConfig();
        bulkRoutesConfig.setRoutes(List.of(routeConfig));

        // Mock service responses
        setupMockResponses();
    }

    private void copyTestFilesToInbound() throws IOException {
        // Copy test files from resources to inbound directory
        Path sourceFile = testFilesDir.resolve("pain001.json");
        if (!Files.exists(sourceFile)) {
            throw new IllegalStateException(
                "Test file not found: " + sourceFile + 
                ". Please ensure pain001.json exists in " + testResourcesPath
            );
        }
        
        Path targetFile = inboundDir.resolve("pain001.json");
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied test file from {} to {}", sourceFile, targetFile);
    }

    private void setupMockResponses() {
        when(pain001Service.validatePain001(anyString()))
            .thenReturn(createTestPaymentInformations());
        when(pain001Service.debulk(anyList()))
            .thenAnswer(i -> i.getArgument(0));
        when(pain001Service.validate(anyList()))
            .thenAnswer(i -> i.getArgument(0));
        when(pain001Service.enrich(anyList()))
            .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void shouldProcessPain001FileSuccessfully() throws Exception {
        // Wait for file processing
        await().atMost(30, TimeUnit.SECONDS)
               .until(() -> Files.list(backupDir).count() > 0);

        // Verify file movement
        assertThat(Files.list(inboundDir).count()).isEqualTo(0);
        assertThat(Files.list(backupDir).count()).isEqualTo(1);
        assertThat(Files.list(errorDir).count()).isEqualTo(0);

        // Verify service calls
        verify(pain001Service).validatePain001(anyString());
        verify(pain001Service).debulk(anyList());
        verify(pain001Service).validate(anyList());
        verify(pain001Service).enrich(anyList());

        // Verify job execution
        List<JobExecution> jobExecutions = jobRepository.getJobExecutions(job);
        assertThat(jobExecutions).hasSize(1);
        assertThat(jobExecutions.get(0).getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void shouldHandleInvalidPain001File() throws Exception {
        // Setup error scenario
        when(pain001Service.validatePain001(anyString()))
            .thenThrow(new BulkProcessingException("Invalid PAIN.001 file"));

        // Wait for file processing
        await().atMost(30, TimeUnit.SECONDS)
               .until(() -> Files.list(errorDir).count() > 0);

        // Verify file movement
        assertThat(Files.list(inboundDir).count()).isEqualTo(0);
        assertThat(Files.list(backupDir).count()).isEqualTo(0);
        assertThat(Files.list(errorDir).count()).isEqualTo(1);

        // Verify job execution
        List<JobExecution> jobExecutions = jobRepository.getJobExecutions(job);
        assertThat(jobExecutions).hasSize(1);
        assertThat(jobExecutions.get(0).getStatus()).isEqualTo(BatchStatus.FAILED);
    }

    private List<PaymentInformation> createTestPaymentInformations() {
        PaymentInformation payment = new PaymentInformation();
        // Set test payment data
        payment.setPaymentInformationId("TEST-PAYMENT-001");
        // ... set other fields
        return List.of(payment);
    }
}

@TestConfiguration
class TestConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public BulkProcessingFlowBuilder bulkProcessingFlowBuilder(
            BulkRoutesConfig bulkRoutesConfig,
            ObjectMapper mapper,
            JobRepository jobRepository,
            JobLauncher jobLauncher,
            PlatformTransactionManager transactionManager,
            Pain001Service pain001Service) {
        return new BulkProcessingFlowBuilder(
            bulkRoutesConfig, 
            mapper, 
            jobRepository, 
            jobLauncher, 
            transactionManager, 
            pain001Service
        );
    }
}
```

// application-test.yml
@antArtifact identifier="test-config" type="text/markdown" title="Test Configuration">
```yaml
spring:
  datasource:
    default:
      driver-class-name: org.h2.Driver
      url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
      username: sa
      password: 
  batch:
    jdbc:
      initialize-schema: always
    job:
      enabled: false

bulk-routes:
  - route-name: TEST_ROUTE
    processing-type: INBOUND
    source-type: FILE
    enabled: true
    steps:
      - pain001-validation
      - payment-debulk
      - payment-validation
      - payment-enrichment
    file-source:
      directoryName: ${java.io.tmpdir}/inbound
      antInclude: "*.json"
      charset: utf-8
      delay: 1000
      maxMessagesPerPoll: 1
      move: ${java.io.tmpdir}/backup
      moveFailed: ${java.io.tmpdir}/error
      readLock: rename
      readLockTimeout: 1000

# Can be overridden via system property or environment variable
test:
  resources:
    path: ${TEST_RESOURCES_PATH:src/test/resources}
```

