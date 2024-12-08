## processing
```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StreamingPain001Processor {
    private final ObjectMapper objectMapper;
    private final Pain001ProcessService pain001ProcessService;

    public StreamingPain001Processor(ObjectMapper objectMapper, Pain001ProcessService pain001ProcessService) {
        this.objectMapper = objectMapper;
        this.pain001ProcessService = pain001ProcessService;
    }

    public List<PaymentInformation> processPain001(File file) {
        try (JsonParser parser = objectMapper.getFactory().createParser(file)) {
            // Navigate to CustomerCreditTransferInitiation
            while (parser.nextToken() != null) {
                String fieldName = parser.currentName();
                if ("customerCreditTransferInitiation".equals(fieldName)) {
                    parser.nextToken(); // START_OBJECT
                    return parseCustomerCreditTransferInitiation(parser);
                }
            }
            
            // If we get here, we didn't find CustomerCreditTransferInitiation
            SourceProcessStatus error = new SourceProcessStatus(
                SourceProcessStatus.Result.SourceError,
                "CustomerCreditTransferInitiation not found in file"
            );
            handleSourceProcessResultAndStop(error);
            return null;

        } catch (IOException e) {
            log.error("Error on parsing pain001 json file: {}", file.getName(), e);
            SourceProcessStatus jsonError = new SourceProcessStatus(
                SourceProcessStatus.Result.SourceError,
                getShortErrorMessage(e)
            );
            handleSourceProcessResultAndStop(jsonError);
            return null;
        }
    }

    private List<PaymentInformation> parseCustomerCreditTransferInitiation(JsonParser parser) throws IOException {
        GroupHeaderDTO groupHeader = null;
        String debtorName = null;
        List<PaymentInformation> paymentInfos = new ArrayList<>();
        boolean preMappingDone = false;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            if (fieldName == null) continue;

            switch (fieldName) {
                case "groupHeader":
                    groupHeader = parseAndProcessGroupHeader(parser);
                    if (groupHeader == null) return null;
                    break;

                case "paymentInformation":
                    List<PaymentInformation> parsedPayments = parsePaymentInformations(
                        parser, groupHeader, preMappingDone);
                    if (parsedPayments == null) return null;
                    paymentInfos.addAll(parsedPayments);
                    preMappingDone = true;
                    break;
            }
        }

        return validateAndProcessPayments(paymentInfos);
    }

    private GroupHeaderDTO parseAndProcessGroupHeader(JsonParser parser) throws IOException {
        parser.nextToken();
        GroupHeaderDTO groupHeader = objectMapper.readValue(parser, GroupHeaderDTO.class);
        
        SourceProcessStatus headerResult = pain001ProcessService.processPain001GroupHeader(groupHeader);
        if (handleSourceProcessResultAndStop(headerResult)) {
            return null;
        }
        
        return groupHeader;
    }

    private List<PaymentInformation> parsePaymentInformations(
            JsonParser parser, 
            GroupHeaderDTO groupHeader,
            boolean preMappingDone) throws IOException {
            
        List<PaymentInformation> paymentInfos = new ArrayList<>();
        parser.nextToken(); // START_ARRAY

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            // Read one PaymentInformationDTO
            PaymentInformationDTO paymentInfoDTO = objectMapper.readValue(parser, PaymentInformationDTO.class);

            // Get debtor name and do pre-mapping if not done yet
            if (!preMappingDone) {
                String debtorName = paymentInfoDTO.getDebtor().getName();
                SourceProcessStatus preMappingResult = 
                    pain001ProcessService.processPrePain001BoMapping(groupHeader, debtorName);
                if (handleSourceProcessResultAndStop(preMappingResult)) {
                    return null;
                }
            }

            // Map directly to PaymentInformation
            PaymentInformation paymentInfo = pain001ProcessService.mapToPaymentInformation(paymentInfoDTO);
            if (paymentInfo != null) {
                paymentInfos.add(paymentInfo);
            }
        }

        return paymentInfos;
    }

    private List<PaymentInformation> validateAndProcessPayments(List<PaymentInformation> paymentInfos) {
        // Validate we have payments after mapping
        if (CollectionUtils.isEmpty(paymentInfos)) {
            SourceProcessStatus emptyPaymentError = new SourceProcessStatus(
                SourceProcessStatus.Result.SourceError,
                "No payments to process after mapping"
            );
            handleSourceProcessResultAndStop(emptyPaymentError);
            return null;
        }

        // Perform mapping validation
        SourceProcessStatus mappingValidationResult = 
            pain001ProcessService.processPain001BoMappingValidation(paymentInfos);
        if (handleSourceProcessResultAndStop(mappingValidationResult)) {
            return null;
        }

        // Post mapping processing
        SourceProcessStatus postMappingResult = 
            pain001ProcessService.processPostPain001BoMapping(paymentInfos);
        if (handleSourceProcessResultAndStop(postMappingResult)) {
            return null;
        }

        return paymentInfos;
    }

    // Convenience method to process using file path
    public List<PaymentInformation> processPain001(String filePath) {
        return processPain001(new File(filePath));
    }
}
```

## mapper

## testing

```java
public class Pain001MappingTest {

     private static final String cu10 = "THISE0511202492_Auth_CU10.json";
     private static final String cu11 = "THISE05118202402_Auth_CU11.json";
     private static final String cu13 = "THISE02508202406_Auth_CU13.json";
     private static final String cu27 = "THISE14119200007_Auth_CU27.json";

     @Autowired
     private PaymentMappingServiceImpl paymentMappingService;

     @Mock
     protected StepExecution stepExecution;

     @Mock
     protected JobExecution jobExecution;

     private ExecutionContext stepContext;
     private ExecutionContext jobContext;
     private PwsFileUpload fileUpload = createMockFileUpload();
     private Pain001InboundProcessingResult result;

     private ObjectMapper objectMapper;

     @BeforeEach
     void setUp() {
         objectMapper = new ObjectMapper();
         // ToDo: more configuration
         objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
         objectMapper.registerModule(new JavaTimeModule());

         // Setup Context
         setUpExecutionContexts();

         paymentMappingService.beforeStep(stepExecution);
     }

     @Test
    @DisplayName("CU10 - Test Payroll Inter Account Funds Transfer mapping")
    void testCu10MappingToBo() throws Exception {
        // Given
        Pain001 pain001 = loadJsonFile(cu10);
        GroupHeaderDTO groupHeaderDTO = getGroupHeader(pain001);
        PaymentInformationDTO paymentDTO = getPaymentInformation(pain001);

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        
        // Verify PaymentInformation
        assertEquals(DmpBulkStatus.fromValue("01"), result.getDmpBulkStatus());
        
        // Verify PwsTransactions
        PwsTransactions pwsTxn = result.getPwsTransactions();
        assertNotNull(pwsTxn);
        assertEquals("Payroll-Inter-Account-Funds-Transfer", pwsTxn.getResourceId());
        assertEquals("Bulk-File-Upload-Executive", pwsTxn.getFeatureId());
        assertEquals("00471048801", pwsTxn.getAccountNumber());
        assertEquals("ABC COMPANY LIMITED.", pwsTxn.getCompanyName());
        assertEquals("SUBMITTED", pwsTxn.getCaptureStatus());
        assertEquals("PENDING_VERIFICATION", pwsTxn.getCustomerTransactionStatus());
        
        // Verify CreditTransferTransactions
        List<CreditTransferTransaction> creditTransfers = result.getCreditTransferTransactionList();
        assertNotNull(creditTransfers);
        assertEquals(2, creditTransfers.size());
        
        // Verify first transaction
        CreditTransferTransaction firstTxn = creditTransfers.get(0);
        assertEquals(1, firstTxn.getDmpLineNo());
        assertEquals(DmpTransactionStatus.fromValue("01"), firstTxn.getDmpTransactionStatus());
        assertEquals(new BigDecimal("200"), firstTxn.getPwsBulkTransactionInstructions().getTransactionAmount());
        assertEquals("NaME MR.AeArs", firstTxn.getCreditor().getPwsParties().getPartyName());

        // Verify second transaction
        CreditTransferTransaction secondTxn = creditTransfers.get(1);
        assertEquals(2, secondTxn.getDmpLineNo());
        assertEquals(new BigDecimal("450"), secondTxn.getPwsBulkTransactionInstructions().getTransactionAmount());
        assertEquals("NsME MR.C4qCU", secondTxn.getCreditor().getPwsParties().getPartyName());
    }

    @Test
    @DisplayName("CU11 - Test Inter Account Funds Transfer with Tax Information")
    void testCu11MappingToBo() throws Exception {
        // Given
        Pain001 pain001 = loadJsonFile(cu11);
        GroupHeaderDTO groupHeaderDTO = getGroupHeader(pain001);
        PaymentInformationDTO paymentDTO = getPaymentInformation(pain001);

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        
        // Verify basic transaction info
        assertEquals("Inter-Account-Funds-Transfer", result.getPwsTransactions().getResourceId());
        assertEquals("Bulk-File-Upload", result.getPwsTransactions().getFeatureId());
        
        // Verify transactions
        List<CreditTransferTransaction> transactions = result.getCreditTransferTransactionList();
        assertEquals(5, transactions.size());
        
        // Verify first transaction's tax information
        CreditTransferTransaction firstTxn = transactions.get(0);
        TaxInformation taxInfo = firstTxn.getTaxInformation();
        assertNotNull(taxInfo);
        assertEquals(3, taxInfo.getInstructionList().size());
        
        PwsTaxInstructions firstTaxInstruction = taxInfo.getInstructionList().get(0);
        assertEquals("234987", firstTaxInstruction.getTaxPayerId());
        assertEquals("53", firstTaxInstruction.getTaxType());
        assertEquals("T1", firstTaxInstruction.getTypeOfIncome());
        assertEquals(new BigDecimal("1.23"), firstTaxInstruction.getTaxRateInPercentage());
        
        // Verify email notification
        assertEquals("EMAIL", firstTxn.getAdvice().getDeliveryMethod());
        assertEquals("mgminterA@thaimail.com", firstTxn.getAdvice().getDeliveryAddress());
    }

    @Test
    @DisplayName("CU13 - Test Payroll Inter Account Fund Transfer with Multiple Recipients")
    void testCu13MappingToBo() throws Exception {
        // Given
        Pain001 pain001 = loadJsonFile(cu13);
        GroupHeaderDTO groupHeaderDTO = getGroupHeader(pain001);
        PaymentInformationDTO paymentDTO = getPaymentInformation(pain001);

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        
        // Verify payment information
        PwsTransactions pwsTxn = result.getPwsTransactions();
        assertEquals("Payroll-Inter-Account-Fund-Transfer", pwsTxn.getResourceId());
        assertEquals("Bulk-File-Upload-Executive", pwsTxn.getFeatureId());
        assertEquals("0471859030", pwsTxn.getAccountNumber());
        assertEquals("บริษัท987", pwsTxn.getCompanyName());
        
        // Verify transactions
        List<CreditTransferTransaction> transactions = result.getCreditTransferTransactionList();
        assertEquals(4, transactions.size());
        
        // Verify amounts
        assertEquals(new BigDecimal("4500.5"), transactions.get(0).getPwsBulkTransactionInstructions().getTransactionAmount());
        assertEquals(new BigDecimal("1000000"), transactions.get(1).getPwsBulkTransactionInstructions().getTransactionAmount());
        assertEquals(new BigDecimal("0.12"), transactions.get(2).getPwsBulkTransactionInstructions().getTransactionAmount());
        assertEquals(new BigDecimal("0.02"), transactions.get(3).getPwsBulkTransactionInstructions().getTransactionAmount());
        
        // Verify beneficiary names
        assertEquals("ชื่อ1", transactions.get(0).getCreditor().getPwsParties().getPartyName());
        assertEquals("ชื่อ2", transactions.get(1).getCreditor().getPwsParties().getPartyName());
        assertEquals("EmployeeName3", transactions.get(2).getCreditor().getPwsParties().getPartyName());
        assertEquals("EmployeeName4", transactions.get(3).getCreditor().getPwsParties().getPartyName());
    }

    @Test
    @DisplayName("CU27 - Test Inter Account Funds Transfer with Complex Tax Structure")
    void testCu27MappingToBo() throws Exception {
        // Given
        Pain001 pain001 = loadJsonFile(cu27);
        GroupHeaderDTO groupHeaderDTO = getGroupHeader(pain001);
        PaymentInformationDTO paymentDTO = getPaymentInformation(pain001);

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        
        // Verify basic transaction info
        PwsTransactions pwsTxn = result.getPwsTransactions();
        assertEquals("Inter-Account-Funds-Transfer", pwsTxn.getResourceId());
        assertEquals("Bulk-File-Upload", pwsTxn.getFeatureId());
        assertEquals("7013630716", pwsTxn.getAccountNumber());
        
        // Verify transactions
        List<CreditTransferTransaction> transactions = result.getCreditTransferTransactionList();
        assertEquals(2, transactions.size());
        
        // Verify first transaction tax details
        CreditTransferTransaction firstTxn = transactions.get(0);
        TaxInformation firstTaxInfo = firstTxn.getTaxInformation();
        assertNotNull(firstTaxInfo);
        assertEquals(5, firstTaxInfo.getInstructionList().size());
        
        // Verify specific tax instruction details
        PwsTaxInstructions taxInstruction = firstTaxInfo.getInstructionList().get(0);
        assertEquals("3100100477538", taxInstruction.getTaxPayerId());
        assertEquals("1", taxInstruction.getTaxPaymentCondition());
        assertEquals("03", taxInstruction.getTypeOfIncome());
        assertEquals("53", taxInstruction.getTaxType());
        assertEquals(new BigDecimal("2"), taxInstruction.getTaxRateInPercentage());
        assertEquals(new BigDecimal("10.11"), taxInstruction.getTaxableAmount());
        assertEquals(new BigDecimal("0.20"), taxInstruction.getTaxAmount());
        
        // Verify proxy information
        assertEquals("M", transactions.get(0).getPwsBulkTransactionInstructions().getPaymentCodeId());
        assertEquals("I", transactions.get(1).getPwsBulkTransactionInstructions().getPaymentCodeId());
    }

     private PwsFileUpload createMockFileUpload() {
         PwsFileUpload fileUpload = new PwsFileUpload();
         fileUpload.setFileUploadId(1L);
         fileUpload.setFileReferenceId("THISE05118202402");
         fileUpload.setChargeOption("OUR");
         fileUpload.setPayrollOption("STANDARD");
         return fileUpload;
     }

     private void setUpExecutionContexts() {
         stepContext = new ExecutionContext();
         jobContext = new ExecutionContext();
         result = new Pain001InboundProcessingResult();
         jobContext.put(ContextKey.bankEntity, UOBT);
         jobContext.put(ContextKey.fileUpload, fileUpload);
         jobContext.put(ContextKey.userId, 123L);
         jobContext.put(ContextKey.companyId, 456L);
         jobContext.put(ContextKey.result, result);

         lenient().when(stepExecution.getExecutionContext()).thenReturn(stepContext);
         lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
         lenient().when(jobExecution.getExecutionContext()).thenReturn(jobContext);
     }

     private Pain001 loadJsonFile(String filename) throws Exception {
        ClassPathResource jsonResource = new ClassPathResource("/dmpAuth/" + filename);
        String jsonContent = Files.readString(Path.of(jsonResource.getURI()));
        return objectMapper.readValue(jsonContent, Pain001.class);
    }

    private GroupHeaderDTO getGroupHeader(Pain001 pain001) {
        return pain001.getBusinessDocument()
                .getCustomerCreditTransferInitiation()
                .getGroupHeader();
    }

    private PaymentInformationDTO getPaymentInformation(Pain001 pain001) {
        return pain001.getBusinessDocument()
                .getCustomerCreditTransferInitiation()
                .getPaymentInformation()
                .get(0);
    }

 }
```

@SpringBootTest
@ActiveProfiles("test")
class Pain001MappingServiceIntegrationTest {

    @Autowired
    private PaymentMappingServiceImpl paymentMappingService;

    @Mock
    private PwsFileUpload mockFileUpload;

    @BeforeEach
    void setup() {
        mockFileUpload = createMockFileUpload();
        ReflectionTestUtils.setField(paymentMappingService, "fileUpload", mockFileUpload);
    }

    private PwsFileUpload createMockFileUpload() {
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setFileUploadId(1L);
        fileUpload.setFileReferenceId("THISE05118202402");
        fileUpload.setChargeOption("OUR");
        fileUpload.setPayrollOption("STANDARD");
        return fileUpload;
    }

    @Test
    void testPain001PaymentToBo_WithRealJsonData() throws Exception {
        // Given
        ClassPathResource jsonResource = new ClassPathResource("pain001-sample.json");
        String jsonContent = Files.readString(Path.of(jsonResource.getURI()));

        ObjectMapper objectMapper = new ObjectMapper();
        Pain001 pain001 = objectMapper.readValue(jsonContent, Pain001.class);
        
        GroupHeaderDTO groupHeaderDTO = pain001.getBusinessDocument()
            .getCustomerCreditTransferInitiation()
            .getGroupHeader();
        
        PaymentInformationDTO paymentDTO = pain001.getBusinessDocument()
            .getCustomerCreditTransferInitiation()
            .getPaymentInformation()
            .get(0);

        // When
        PaymentInformation result = paymentMappingService.pain001PaymentToBo(groupHeaderDTO, paymentDTO);

        // Then
        assertNotNull(result);
        
        // Verify PaymentInformation
        assertEquals(DmpBulkStatus.fromValue("01"), result.getDmpBulkStatus());
        
        // Verify PwsTransactions
        PwsTransactions pwsTxn = result.getPwsTransactions();
        assertNotNull(pwsTxn);
        assertEquals("Inter-Account-Funds-Transfer", pwsTxn.getResourceId());
        assertEquals("Bulk-File-Upload", pwsTxn.getFeatureId());
        assertEquals("0987654321", pwsTxn.getAccountNumber());
        assertEquals("ชื่อ 00", pwsTxn.getCompanyName());
        assertEquals("SUBMITTED", pwsTxn.getCaptureStatus());
        assertEquals("PENDING_VERIFICATION", pwsTxn.getCustomerTransactionStatus());
        assertNotNull(pwsTxn.getInitiationTime());
        
        // Verify PwsBulkTransactions
        PwsBulkTransactions pwsBulkTxn = result.getPwsBulkTransactions();
        assertNotNull(pwsBulkTxn);
        assertEquals(mockFileUpload.getFileUploadId(), pwsBulkTxn.getFileUploadId());
        assertEquals("OUR", pwsBulkTxn.getChargeOptions());
        assertEquals("STANDARD", pwsBulkTxn.getPayrollOptions());
        assertEquals("PENDING_VERIFICATION", pwsBulkTxn.getStatus());
        assertNotNull(pwsBulkTxn.getTransferDate());
        assertEquals("2007-02-23", new SimpleDateFormat("yyyy-MM-dd").format(pwsBulkTxn.getTransferDate()));

        // Verify CreditTransferTransactions
        List<CreditTransferTransaction> creditTransfers = result.getCreditTransferTransactionList();
        assertNotNull(creditTransfers);
        assertEquals(5, creditTransfers.size());
        
        // Verify first transaction details
        CreditTransferTransaction firstTxn = creditTransfers.get(0);
        assertEquals(1, firstTxn.getDmpLineNo());
        assertEquals(DmpTransactionStatus.fromValue("01"), firstTxn.getDmpTransactionStatus());
        
        // Verify PwsBulkTransactionInstructions
        PwsBulkTransactionInstructions instructions = firstTxn.getPwsBulkTransactionInstructions();
        assertNotNull(instructions);
        assertEquals("THB", instructions.getTransactionCurrency());
        assertEquals(new BigDecimal("2490.68"), instructions.getTransactionAmount());
        assertEquals("รายละเอียดการชำระเงิน12345", instructions.getPaymentDetails());
        assertEquals("อ้างอิง000000", instructions.getCustomerReference());
        assertEquals("PENDING_VERIFICATION", instructions.getCustomerTransactionStatus());
        assertEquals("OUR", instructions.getChargeOptions());
        assertNotNull(instructions.getOriginalValueDate());
        assertNotNull(instructions.getInitiationTime());
        
        // Verify Creditor
        Creditor creditor = firstTxn.getCreditor();
        assertNotNull(creditor);
        PwsParties parties = creditor.getPwsParties();
        assertNotNull(parties);
        assertEquals("บริษัท11111", parties.getPartyName());
        assertEquals("00972185011", parties.getPartyAccountNumber());
        
        // Verify Tax Information
        TaxInformation taxInfo = firstTxn.getTaxInformation();
        assertNotNull(taxInfo);
        List<PwsTaxInstructions> taxInstructions = taxInfo.getInstructionList();
        assertNotNull(taxInstructions);
        assertEquals(3, taxInstructions.size());
        
        // Verify first tax instruction
        PwsTaxInstructions firstTaxInstruction = taxInstructions.get(0);
        assertEquals("234987", firstTaxInstruction.getTaxPayerId());
        assertEquals("53", firstTaxInstruction.getTaxType());
        assertEquals("หักภาษี ณ ที่จ่าย (Withhold at source)       1/R  ", firstTaxInstruction.getTaxPaymentCondition());
        assertEquals("T1", firstTaxInstruction.getTypeOfIncome());
        assertEquals("คำอธิบาย1", firstTaxInstruction.getTaxDescription());
        assertEquals(new BigDecimal("1.23"), firstTaxInstruction.getTaxRateInPercentage());
        assertEquals(new BigDecimal("98.76"), firstTaxInstruction.getTaxableAmount());
        assertEquals(new BigDecimal("0.12"), firstTaxInstruction.getTaxAmount());
        
        // Verify Transaction Advice
        PwsTransactionAdvices advice = firstTxn.getAdvice();
        assertNotNull(advice);
        assertEquals("EMAIL", advice.getDeliveryMethod());
        assertEquals("mgminterA@thaimail.com", advice.getDeliveryAddress());
        
        // Verify remaining transactions
        verifyTransaction(creditTransfers.get(1), "บริษัท22222", "mgminterB@thaimail.com", 4);
        verifyTransaction(creditTransfers.get(2), "บริษัท33333", "mgminterC@thaimail.com", 7);
        verifyTransaction(creditTransfers.get(3), "บริษัท44444", "mgminterD@thaimail.com", 10);
        verifyTransaction(creditTransfers.get(4), "บริษัท55555", "mgminterE@thaimail.com", 13);
    }

    private void verifyTransaction(CreditTransferTransaction txn, String expectedPartyName, 
                                 String expectedEmail, int expectedLineNo) {
        assertNotNull(txn);
        assertEquals(expectedLineNo, txn.getDmpLineNo());
        assertEquals(DmpTransactionStatus.fromValue("01"), txn.getDmpTransactionStatus());
        
        assertNotNull(txn.getCreditor());
        assertNotNull(txn.getCreditor().getPwsParties());
        assertEquals(expectedPartyName, txn.getCreditor().getPwsParties().getPartyName());
        
        assertNotNull(txn.getAdvice());
        assertEquals("EMAIL", txn.getAdvice().getDeliveryMethod());
        assertEquals(expectedEmail, txn.getAdvice().getDeliveryAddress());
        
        assertNotNull(txn.getPwsBulkTransactionInstructions());
        assertEquals("THB", txn.getPwsBulkTransactionInstructions().getTransactionCurrency());
        assertEquals(new BigDecimal("2490.68"), txn.getPwsBulkTransactionInstructions().getTransactionAmount());
        assertEquals("PENDING_VERIFICATION", txn.getPwsBulkTransactionInstructions().getCustomerTransactionStatus());
    }
}
