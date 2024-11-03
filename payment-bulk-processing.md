## pain001Service

```java
@ExtendWith(MockitoExtension.class)
class Pain001ServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentUtils paymentUtils;

    @Mock
    private Pain001ToBoMapper pain001ToBoMapper;

    @Mock
    private Pain001ToBoService pain001ToBo;

    @Mock
    private PaymentInformationValidation paymentInfoValidation;

    @Mock
    private PaymentInformationEnrichment paymentInfoEnrichment;

    @Mock
    private PaymentDebulkService paymentDebulkService;

    @Mock
    private PaymentSave paymentSave;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private ExecutionContext jobExecutionContext;

    @Mock
    private ExecutionContext stepExecutionContext;

    @InjectMocks
    private Pain001ServiceImpl pain001Service;

    @BeforeEach
    void setUp() {
        // Setup step execution context
        when(stepExecution.getExecutionContext()).thenReturn(stepExecutionContext);
        when(stepExecution.getJobExecution()).thenReturn(mock(JobExecution.class));
        when(stepExecution.getJobExecution().getExecutionContext()).thenReturn(jobExecutionContext);
        ReflectionTestUtils.setField(pain001Service, "stepExecution", stepExecution);
    }

    @Test
    void debulk_ShouldProcessAndUpdateResult() {
        // Given
        List<PaymentInformation> inputPayments = Collections.singletonList(createValidPaymentInfo());
        List<PaymentInformation> debulkedPayments = Arrays.asList(
            createValidPaymentInfo(),
            createValidPaymentInfo()
        );
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();

        when(paymentDebulkService.debulkPaymentInformations(inputPayments)).thenReturn(debulkedPayments);
        when(jobExecutionContext.get("result", Pain001InboundProcessingResult.class)).thenReturn(result);

        // When
        List<PaymentInformation> actualResult = pain001Service.debulk(inputPayments);

        // Then
        verify(paymentDebulkService).beforeStep(stepExecution);
        verify(paymentDebulkService).debulkPaymentInformations(inputPayments);
        assertEquals(debulkedPayments.size(), result.getPaymentDebulkTotal());
        assertEquals(debulkedPayments, actualResult);
    }

    @Test
    void save_WhenValidPayment_ShouldSaveSuccessfully() {
        // Given
        PaymentInformation validPayment = createValidPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(validPayment);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave).savePaymentInformation(
            eq(validPayment),
            eq(stepExecutionContext),
            eq(jobExecutionContext)
        );
        verifyNoMoreInteractions(paymentUtils);
    }

    @Test
    void save_WhenInvalidPayment_ShouldSkip() {
        // Given
        PaymentInformation invalidPayment = createInvalidPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(invalidPayment);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave, never()).savePaymentInformation(
            any(PaymentInformation.class),
            any(ExecutionContext.class),
            any(ExecutionContext.class)
        );
    }

    @Test
    void save_WhenSaveThrowsException_ShouldHandleError() {
        // Given
        PaymentInformation validPayment = createValidPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(validPayment);
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord saveRecord = new PwsSaveRecord();

        doThrow(new RuntimeException("Save failed"))
            .when(paymentSave)
            .savePaymentInformation(any(), any(), any());

        when(jobExecutionContext.get("result", Pain001InboundProcessingResult.class))
            .thenReturn(result);
        when(paymentUtils.createPwsSaveRecord(anyLong(), anyString()))
            .thenReturn(saveRecord);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave).savePaymentInformation(
            eq(validPayment),
            eq(stepExecutionContext),
            eq(jobExecutionContext)
        );
        verify(paymentUtils).createPwsSaveRecord(
            validPayment.getPwsTransactions().getTransactionId(),
            validPayment.getPwsBulkTransactions().getDmpBatchNumber()
        );
        verify(paymentUtils).updatePaymentSavedError(result, saveRecord);
    }

    @Test
    void save_WhenRejectedPayment_ShouldSkip() {
        // Given
        PaymentInformation rejectedPayment = createRejectedPaymentInfo();
        List<PaymentInformation> payments = Collections.singletonList(rejectedPayment);

        // When
        pain001Service.save(payments);

        // Then
        verify(paymentSave, never()).savePaymentInformation(
            any(PaymentInformation.class),
            any(ExecutionContext.class),
            any(ExecutionContext.class)
        );
    }

    // Helper methods
    private PaymentInformation createValidPaymentInfo() {
        PaymentInformation payment = new PaymentInformation();
        payment.setDmpBulkStatus(DmpBulkStatus.ACCEPTED);
        
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);
        payment.setPwsTransactions(pwsTransactions);
        
        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");
        payment.setPwsBulkTransactions(pwsBulkTransactions);
        
        return payment;
    }

    private PaymentInformation createInvalidPaymentInfo() {
        PaymentInformation payment = createValidPaymentInfo();
        payment.addValidationError("ERR001", "Invalid payment");
        return payment;
    }

    private PaymentInformation createRejectedPaymentInfo() {
        PaymentInformation payment = createValidPaymentInfo();
        payment.setDmpBulkStatus(DmpBulkStatus.REJECTED);
        return payment;
    }
}
```

## save

```java
@ExtendWith(MockitoExtension.class)
class PaymentSaveImplTest {

    @Mock
    private AppConfig config;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private SqlSessionTemplate paymentSaveSqlSessionTemplate;

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @Mock
    private SqlSession sqlSession;

    @Mock
    private PwsSaveDao pwsSaveDao;

    @Mock
    private PaymentUtils paymentUtils;

    @Mock
    private TransactionUtils transactionUtils;

    @InjectMocks
    private PaymentSaveImpl paymentSave;

    private PaymentInformation paymentInfo;
    private ExecutionContext stepContext;
    private ExecutionContext jobContext;
    private Pain001InboundProcessingResult result;
    private PwsSaveRecord mockRecord;

    @BeforeEach
    void setUp() {
        paymentInfo = new PaymentInformation();
        stepContext = new ExecutionContext();
        jobContext = new ExecutionContext();
        result = new Pain001InboundProcessingResult();
        result.setPaymentSaved(new ArrayList<>());
        result.setPaymentSavedError(new ArrayList<>());
        jobContext.put("result", result);
        mockRecord = new PwsSaveRecord("encrypted_1", "BATCH001");

        // Setup SQL Session behavior
        when(paymentSaveSqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        when(sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(sqlSession);
        when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);
        
        // Setup PaymentUtils behavior
        when(paymentUtils.createPwsSaveRecord(anyLong(), anyString())).thenReturn(mockRecord);
        doNothing().when(paymentUtils).updatePaymentSaved(any(), any());
        doNothing().when(paymentUtils).updatePaymentSavedError(any(), any());
    }

    @Test
    void savePaymentInformation_SuccessfulSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);

        setupValidPaymentInfo();
        setupSuccessfulBulkSave();
        setupSuccessfulChildTransactionSave();

        // Act
        paymentSave.savePaymentInformation(paymentInfo, stepContext, jobContext);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(
            eq(paymentInfo.getPwsTransactions().getTransactionId()),
            eq(paymentInfo.getPwsBulkTransactions().getDmpBatchNumber())
        );
        verify(paymentUtils).updatePaymentSaved(eq(result), eq(mockRecord));
        verify(pwsSaveDao).insertPwsTransactions(any(PwsTransactions.class));
        verify(pwsSaveDao).insertPwsBulkTransactions(any(PwsBulkTransactions.class));
        verifyBatchInsertsCalled();
    }

    @Test
    void savePaymentInformation_FailedBatchSave() {
        // Arrange
        int batchSize = 2;
        when(config.getBatchInsertSize()).thenReturn(batchSize);

        setupValidPaymentInfo();
        setupSuccessfulBulkSave();
        setupFailedChildTransactionSave();

        // Act
        paymentSave.savePaymentInformation(paymentInfo, stepContext, jobContext);

        // Assert
        verify(paymentUtils).createPwsSaveRecord(
            eq(paymentInfo.getPwsTransactions().getTransactionId()),
            eq(paymentInfo.getPwsBulkTransactions().getDmpBatchNumber())
        );
        verify(paymentUtils).updatePaymentSavedError(eq(result), eq(mockRecord));
        verify(sqlSession, times(1)).rollback();
    }

    @Test
    void saveBulkPayment_Success() {
        // Arrange
        setupValidPaymentInfo();
        setupSuccessfulBulkSave();

        // Act
        long txnId = paymentSave.saveBulkPayment(paymentInfo);

        // Assert
        assertEquals(1L, txnId);
        verify(pwsSaveDao).getBankRefSequenceNum();
        verify(pwsSaveDao).insertPwsTransactions(any(PwsTransactions.class));
        verify(pwsSaveDao).insertPwsBulkTransactions(any(PwsBulkTransactions.class));
    }

    private void setupValidPaymentInfo() {
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(1L);

        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");

        List<CreditTransferTransaction> transactions = new ArrayList<>();
        transactions.add(createValidTransaction());
        transactions.add(createValidTransaction());

        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);
        paymentInfo.setCreditTransferTransactionList(transactions);
    }

    private CreditTransferTransaction createValidTransaction() {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        transaction.setDmpTransactionStatus(DmpTransactionStatus.APPROVED);

        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        Party party = new Party();
        party.setPwsParties(new PwsParties());

        transaction.setPwsBulkTransactionInstructions(instructions);
        transaction.setParty(party);

        return transaction;
    }

    private void setupSuccessfulBulkSave() {
        when(pwsSaveDao.getBankRefSequenceNum()).thenReturn(1);
        when(pwsSaveDao.insertPwsTransactions(any())).thenReturn(1L);
        when(pwsSaveDao.insertPwsBulkTransactions(any())).thenReturn(1L);
    }

    private void setupSuccessfulChildTransactionSave() {
        doAnswer(invocation -> {
            RetryCallback<Object, RuntimeException> callback = invocation.getArgument(0);
            return callback.doWithRetry(null);
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }

    private void setupFailedChildTransactionSave() {
        doAnswer(invocation -> {
            throw new BulkProcessingException("Failed to insert", new RuntimeException());
        }).when(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }

    private void verifyBatchInsertsCalled() {
        verify(sqlSession, atLeastOnce()).commit();
        verify(sqlSession, never()).rollback();
        verify(sqlSession, atLeastOnce()).close();
    }
}
```
