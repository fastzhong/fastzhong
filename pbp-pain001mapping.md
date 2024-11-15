# mapper1
```java
// Pain001ToBoMapper.java
@Mapper(componentModel = "spring")
public interface Pain001ToBoMapper {
    
    @Mapping(target = "dmpBulkStatus", source = "bulkstatus", qualifiedByName = "mapBulkStatus")
    PaymentInformation mapToPaymentInformation(PaymentInformationDTO paymentDTO);

    @Named("mapBulkStatus")
    default DmpBulkStatus mapBulkStatus(String bulkstatus) {
        return Objects.isNull(bulkstatus) ? null : DmpBulkStatus.fromValue(bulkstatus);
    }

    @Mapping(target = "captureStatus", constant = "PENDING_SUBMIT")
    @Mapping(target = "customerTransactionStatus", constant = "NEW")
    @Mapping(target = "resourceId", source = "product")
    @Mapping(target = "featureId", source = "productfeature")
    @Mapping(target = "accountNumber", source = "debtorAccount.identification.other.identification")
    @Mapping(target = "accountCurrency", source = "debtorAccount.currency")
    @Mapping(target = "companyName", source = "debtor.name")
    @Mapping(target = "initiationTime", expression = "java(new Timestamp(System.currentTimeMillis()))")
    PwsTransactions mapToPwsTransactions(PaymentInformationDTO paymentDTO);

    @Mapping(target = "dmpBatchNumber", source = "DMPBatchRef")
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "changeToken", expression = "java(System.currentTimeMillis())")
    PwsBulkTransactions mapToPwsBulkTransactions(PaymentInformationDTO paymentDTO);

    @Mapping(target = "transactionAmount", source = "amount.instructedAmount.amount")
    @Mapping(target = "transactionCurrency", source = "amount.instructedAmount.currency")
    @Mapping(target = "destinationBankName", source = "creditorAgent.financialInstitutionIdentification.name")
    @Mapping(target = "dmpTransRef", source = "DMPTransRef")
    @Mapping(target = "paymentDetails", source = "instructionForCreditorAgent[0].instructionInformation")
    @Mapping(target = "customerReference", source = "paymentIdentification.endToEndIdentification")
    PwsBulkTransactionInstructions mapToPwsBulkTransactionInstructions(CreditTransferTransactionInformationDTO childDTO);

    @Mapping(target = "pwsParties.partyName", source = "creditor.name")
    @Mapping(target = "pwsParties.partyAccountNumber", source = "creditorAccount.identification.other.identification")
    @Mapping(target = "pwsPartyContactList", source = "creditor.postalAddress.addressLine")
    Creditor mapToCreditor(CreditTransferTransactionInformationDTO childDTO);

    default List<PwsPartyContacts> mapAddressLinesToContacts(List<String> addressLines) {
        if (addressLines == null || addressLines.isEmpty()) {
            return Collections.emptyList();
        }
        
        PwsPartyContacts contacts = new PwsPartyContacts();
        for (int i = 0; i < addressLines.size(); i++) {
            switch (i) {
                case 0: contacts.setAddress1(addressLines.get(i)); break;
                case 1: contacts.setAddress2(addressLines.get(i)); break;
                case 2: contacts.setAddress3(addressLines.get(i)); break;
            }
        }
        return Collections.singletonList(contacts);
    }

    @Mapping(target = "adviceId", source = "creditor.identification.organisationIdentification.other.identification")
    @Mapping(target = "deliveryMethod", source = "relatedRemittanceInformation[0].remittanceLocationDetails[0].method")
    @Mapping(target = "deliveryAddress", source = "relatedRemittanceInformation[0].remittanceLocationDetails[0].electronicAddress")
    PwsTransactionAdvices mapToPwsTransactionAdvices(CreditTransferTransactionInformationDTO childDTO);

    default TaxInformation mapToTaxInformation(CreditTransferTransactionInformationDTO childDTO) {
        if (childDTO.getTax() == null) {
            return null;
        }

        TaxInformation taxInfo = new TaxInformation();
        List<PwsTaxInstructions> instructions = new ArrayList<>();
        
        for (TaxRecordDTO record : childDTO.getTax().getRecord()) {
            if (record.getTaxAmount() != null) {
                PwsTaxInstructions instruction = new PwsTaxInstructions();
                instruction.setTaxPayerId(childDTO.getTax().getCreditor().getTaxIdentification());
                instruction.setTaxType(record.getFormsCode());
                instruction.setTaxPaymentCondition(record.getType());
                instruction.setTypeOfIncome(record.getCategory());
                instruction.setTaxDescription(record.getCategoryDetails());
                instruction.setTaxRateInPercentage(new BigDecimal(record.getTaxAmount().getRate()));
                instruction.setTaxableAmount(new BigDecimal(record.getTaxAmount().getTaxableBaseAmount().getAmount()));
                instruction.setTaxAmount(new BigDecimal(record.getTaxAmount().getTotalAmount().getAmount()));
                instructions.add(instruction);
            }
        }
        
        taxInfo.setInstructionList(instructions);
        return taxInfo;
    }
}
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

