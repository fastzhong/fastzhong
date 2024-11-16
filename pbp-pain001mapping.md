## mapper
```java
@Mapping(target = "deliveryMethod", source = "relatedRemittanceInformation[0].remittanceLocationDetails[0].method")
     @Mapping(target = "deliveryAddress", source = "relatedRemittanceInformation[0].remittanceLocationDetails[0].electronicAddress")
     PwsTransactionAdvices mapToPwsTransactionAdvices(CreditTransferTransactionInformationDTO childDTO);
```

## testing

```java
@SpringBootTest
class Pain001MappingServiceIntegrationTest {

    @Autowired
    private PaymentMappingServiceImpl paymentMappingService;

    @Test
    void testPain001PaymentToBo_WithRealJsonData() throws Exception {
        // Given
        String jsonContent = "{\n" +
            "  \"businessDocument\": {\n" +
            "    \"customerCreditTransferInitiation\": {\n" +
            "      \"groupHeader\": {\n" +
            // ... (using the JSON you provided, removed for brevity)
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

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
        assertEquals("PENDING_SUBMIT", pwsTxn.getCaptureStatus());
        assertEquals("NEW", pwsTxn.getCustomerTransactionStatus());
        
        // Verify PwsBulkTransactions
        PwsBulkTransactions pwsBulkTxn = result.getPwsBulkTransactions();
        assertNotNull(pwsBulkTxn);
        assertNotNull(pwsBulkTxn.getTransferDate());
        assertEquals("2007-02-23", new SimpleDateFormat("yyyy-MM-dd").format(pwsBulkTxn.getTransferDate()));

        // Verify CreditTransferTransactions
        List<CreditTransferTransaction> creditTransfers = result.getCreditTransferTransactionList();
        assertNotNull(creditTransfers);
        assertEquals(5, creditTransfers.size());
        
        // Verify first transaction details
        CreditTransferTransaction firstTxn = creditTransfers.get(0);
        
        // Verify PwsBulkTransactionInstructions
        PwsBulkTransactionInstructions instructions = firstTxn.getPwsBulkTransactionInstructions();
        assertNotNull(instructions);
        assertEquals("THB", instructions.getTransactionCurrency());
        assertEquals(new BigDecimal("2490.68"), instructions.getTransactionAmount());
        assertEquals("รายละเอียดการชำระเงิน12345", instructions.getPaymentDetails());
        assertEquals("อ้างอิง000000", instructions.getCustomerReference());
        
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
        
        // Verify remaining transactions have expected values
        assertEquals("บริษัท22222", creditTransfers.get(1).getCreditor().getPwsParties().getPartyName());
        assertEquals("บริษัท33333", creditTransfers.get(2).getCreditor().getPwsParties().getPartyName());
        assertEquals("บริษัท44444", creditTransfers.get(3).getCreditor().getPwsParties().getPartyName());
        assertEquals("บริษัท55555", creditTransfers.get(4).getCreditor().getPwsParties().getPartyName());
    }
}
```

# mapper2
```java
// PwsBulkTransactions
     @Mapping(target = "fileUpload", source = "fileUpload.fileReferenceId")
     @Mapping(target = "status", constant = "PENDING_VERIFICATION")
     @Mapping(target = "dmpBatchNumber", source = "paymentDTO.DMPBatchRef")
     @Mapping(target = "fileUpload", source = "fileUpload.fileReferenceId")
     @Mapping(target = "chargeOptions", source = "fileUpload.chargeOption")
     @Mapping(target = "payrollOptions", source = "fileUpload.payrollOption")
     PwsBulkTransactions mapToPwsBulkTransactions(PaymentInformationDTO paymentDTO, PwsFileUpload fileUpload);
```

