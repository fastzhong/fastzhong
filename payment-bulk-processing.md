## debulk

```java
@Service
public class PaymentDebulkService {

    private static final BigDecimal BAHTNET_THRESHOLD = new BigDecimal("2000000.00");
    private static final String SMART_RESOURCE_ID = "smart";
    private static final String BAHTNET_RESOURCE_ID = "bahtnet";
    private static final String SMART_SAME_DAY_RESOURCE_ID = "smart-same-day";

    public List<PaymentInformation> debulkPaymentInformations(List<PaymentInformation> paymentInfos) {
        List<PaymentInformation> debulkedPayments = new ArrayList<>();
        
        for (PaymentInformation paymentInfo : paymentInfos) {
            // Skip if payment info is invalid
            if (!paymentInfo.isValid() || !paymentInfo.isAllChildTxnsValid()) {
                debulkedPayments.add(paymentInfo);
                continue;
            }

            PwsTransactions pwsTransaction = paymentInfo.getPwsTransactions();
            
            // Skip if not SMART payment
            if (!SMART_RESOURCE_ID.equalsIgnoreCase(pwsTransaction.getResourceId())) {
                debulkedPayments.add(paymentInfo);
                continue;
            }

            // Process each child transaction
            List<CreditTransferTransaction> originalChildTxns = paymentInfo.getCreditTransferTransactionList();
            if (originalChildTxns != null) {
                Map<String, List<CreditTransferTransaction>> groupedTxns = new HashMap<>();
                
                // Group transactions based on amount threshold
                for (CreditTransferTransaction childTxn : originalChildTxns) {
                    BigDecimal txnAmount = pwsTransaction.getTransactionTotalAmount();
                    String targetResourceId = determineResourceId(txnAmount);
                    groupedTxns.computeIfAbsent(targetResourceId, k -> new ArrayList<>()).add(childTxn);
                }

                // Create new payment information for each group
                for (Map.Entry<String, List<CreditTransferTransaction>> entry : groupedTxns.entrySet()) {
                    String resourceId = entry.getKey();
                    List<CreditTransferTransaction> groupTxns = entry.getValue();
                    
                    PaymentInformation newPaymentInfo = createNewPaymentInfo(paymentInfo, resourceId, groupTxns);
                    debulkedPayments.add(newPaymentInfo);
                }
            }
        }
        
        return debulkedPayments;
    }

    private String determineResourceId(BigDecimal amount) {
        if (amount.compareTo(BAHTNET_THRESHOLD) >= 0) {
            return BAHTNET_RESOURCE_ID;
        }
        return SMART_SAME_DAY_RESOURCE_ID;
    }

    private PaymentInformation createNewPaymentInfo(
            PaymentInformation originalPayment,
            String resourceId,
            List<CreditTransferTransaction> transactions) {
        
        PaymentInformation newPaymentInfo = new PaymentInformation();
        
        // Copy bulk transaction details
        newPaymentInfo.setPwsBulkTransactions(originalPayment.getPwsBulkTransactions());
        
        // Create new PwsTransactions with updated details
        PwsTransactions originalPwsTxn = originalPayment.getPwsTransactions();
        PwsTransactions newPwsTxn = new PwsTransactions();
        
        // Copy all properties from original transaction
        BeanUtils.copyProperties(originalPwsTxn, newPwsTxn);
        
        // Update specific fields
        newPwsTxn.setResourceId(resourceId);
        newPwsTxn.setTotalChild(transactions.size());
        newPwsTxn.setTotalAmount(calculateTotalAmount(transactions));
        newPwsTxn.setTransactionTotalAmount(calculateTotalAmount(transactions));
        
        // Set the new transactions
        newPaymentInfo.setPwsTransactions(newPwsTxn);
        newPaymentInfo.setCreditTransferTransactionList(transactions);
        
        // Copy validation status
        newPaymentInfo.setDmpBulkStatus(originalPayment.getDmpBulkStatus());
        
        return newPaymentInfo;
    }

    private BigDecimal calculateTotalAmount(List<CreditTransferTransaction> transactions) {
        return transactions.stream()
                .map(txn -> txn.getPwsBulkTransactionInstructions().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

