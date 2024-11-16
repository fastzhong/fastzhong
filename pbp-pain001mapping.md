## mapper

## testing

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
