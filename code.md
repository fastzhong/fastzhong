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

#  Command Line Application 

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
         log.info("UobCmdBootApplication Application: starting");
         SpringApplication.run(UobCmdBootApplication.class, args);
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

