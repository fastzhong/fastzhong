```java
@Configuration
 @EnableIntegration
 @Slf4j
 @RequiredArgsConstructor
 public class DmpFlowConfig {

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
@Service
 @RequiredArgsConstructor
 @Slf4j
 public class ProcessingHandler {

     private final ProcessContext processContext;
     private final ObjectMapper objectMapper;
     private final AuthFileService authFileService;
     private final PwsProcessService pwsService;
     private final NotificationService notificationService;
     private final FileArchiveService fileBackService;

     public Message<List<PwsMessage>> processAuthFile(Message<AuthPain001> message) {
         log.info("processAuthFile started");
         MessageHeaders headers = message.getHeaders();
         String fileName = (String)headers.get("file_name");
         List<HandlerName> processors = (List<HandlerName>)processContext.getContext(fileName, "processors");
         processors.add(HandlerName.processAuthFile);
         processContext.setContext(fileName, "processors", processors);
         AuthPain001 authPain001 = message.getPayload();
         String status = authFileService.getAuthFileStatus(authPain001);
         log.info("auth file status: {}", status);
         if (AUTH_STATUS02.equals(status)) {
             log.warn("reject auth file: {}", "path");
             File file = (File) Objects.requireNonNull(headers.get("file_originalFile"));
             throw new AuthFileRejectedException(file.getAbsolutePath());
         }
         log.info("retrieve info for {}", fileName);
         /* ToDo
            retrieve the file upload information "pws_file_upload"
         */
         log.info("mapping pain001 to pws message");
         /* ToDo
            flatten payment (debtor, value date) -> PwsMessage
            charge type
          */
         PwsMessage pwsMessage1 = new PwsMessage();
         PwsMessage pwsMessage2 = new PwsMessage();
         List<PwsMessage> pwsMessages = new ArrayList<>();
         pwsMessages.add(pwsMessage1);
         pwsMessages.add(pwsMessage2);
         return MessageBuilder.withPayload(pwsMessages).copyHeaders(headers).build();
     }

     public Message<NotificationMessage> processPwsMessage(Message<List<PwsMessage>> message) {
         log.info("processPwsMessage started");
         MessageHeaders headers = message.getHeaders();
         String fileName = (String)headers.get("file_name");
         List<HandlerName> processors = (List<HandlerName>)processContext.getContext(fileName, "processors");
         processors.add(HandlerName.processPwsMessage);
         processContext.setContext(fileName, "processors", processors);
         List<PwsMessage> pwsMessages = message.getPayload();
         log.info("splitting started");
         /* ToDo: splitting
           1. totalTransferAmount computation: debtor account & value date
           2. derive product, resource id, feature id
           3. splittingKey: product, charge type, debtor account, value date (xml/uff: NRA/RA)
           4. validation
           4. bulk level computation:
           5. generate pws bulk & child instructions and save
        */

         pwsService.computeTotalAmount(pwsMessages);
         pwsService.deriveProduct(pwsMessages);
         pwsService.createSplittingKey(pwsMessages);

         log.info("validation started");
         pwsService.validatePws(pwsMessages);

         pwsService.computeBulk(pwsMessages);

         log.info("persisting started");
         pwsService.savePws(pwsMessages);

         NotificationMessage notificationMessage = new NotificationMessage();
         return MessageBuilder.withPayload(notificationMessage).copyHeaders(headers).build();
     }

     public Message<?> sendNotification(Message<NotificationMessage> message) {
         log.info("sendNotification started");
         MessageHeaders headers = message.getHeaders();
         String fileName = (String)headers.get("file_name");
         List<HandlerName> processors = (List<HandlerName>)processContext.getContext(fileName, "processors");
         processors.add(HandlerName.sendNotification);
         processContext.setContext(fileName, "processors", processors);
         /* ToDo */
         FileBackupMessage msg = new FileBackupMessage();
         return MessageBuilder.withPayload(msg).build();

     }

     public void backupFile(Message<FileBackupMessage> message) {
         log.info("backupFile started");
         MessageHeaders headers = message.getHeaders();
         String fileName = (String)headers.get("file_name");
         List<HandlerName> processors = (List<HandlerName>)processContext.getContext(fileName, "processors");
         processors.add(HandlerName.backupFile);
         processContext.setContext(fileName, "processors", processors);
         // ToDo: wait for the last pwsMessage
         File file = (File) Objects.requireNonNull(headers.get("file_originalFile"));
         fileBackService.backupAuthFile(file);
     }

 }
```

```java
public class ProcessingHandlerErrorAdvice extends AbstractRequestHandlerAdvice  {

    private final ProcessContext processContext;
    private final MessageChannel notificationInputChannel;

    private volatile long sendTimeout = 1000000L;

    @Override
    protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        NotificationMessage notificationMessage = new NotificationMessage();
        try {
            return callback.execute();
        } catch (AuthFileRejectedException e) {
            // ToDo
        } catch (RejectFileOnErrorException e) {
            // ToDo
        } catch (Exception e) {
            // ToDo
        }
        Message<NotificationMessage> messageToSend = MessageBuilder.withPayload(notificationMessage).copyHeaders(headers).build();
        boolean sent = notificationInputChannel.send(messageToSend, sendTimeout);
        if (!sent) {
            log.error("Failed to send error message to notificationInputChannel");
            throw new MessageDeliveryException(message, "Failed to send message to channel '" + notificationInputChannel + "' within timeout: " + sendTimeout);
        }
        return null;
    }
}
```

