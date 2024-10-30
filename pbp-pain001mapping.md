
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

