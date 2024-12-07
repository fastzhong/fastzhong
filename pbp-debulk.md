public List<PaymentInformation> debulk(List<PaymentInformation> paymentInfos) {
    log.info("paymentDebulkServiceTH starts ...");
    List<PaymentInformation> debulkedPayments = new ArrayList<>();

    for (PaymentInformation paymentInfo : paymentInfos) {
        // Skip if payment info is invalid
        if (paymentInfo.isDmpRejected()) {
            debulkedPayments.add(paymentInfo);
            continue;
        }

        PwsTransactions pwsTransaction = paymentInfo.getPwsTransactions();

        // Skip if not SMART payment
        if (!Resource.SMART.id.equals(pwsTransaction.getResourceId())) {
            debulkedPayments.add(paymentInfo);
            continue;
        }

        // Step 1: Split by bahtnet/smart-day first
        List<PaymentInformation> splitByType = splitByResourceType(paymentInfo);
        
        // Step 2: Further split by max transactions or amount
        for (PaymentInformation splitPayment : splitByType) {
            if (appConfig.getDebulk().isMaxTransactionsEnabled()) {
                debulkedPayments.addAll(splitByMaxTransactions(splitPayment));
            } else if (appConfig.getDebulk().isMaxAmountEnabled()) {
                debulkedPayments.addAll(splitByMaxAmount(splitPayment));
            } else {
                debulkedPayments.add(splitPayment);
            }
        }
    }

    return debulkedPayments;
}

private List<PaymentInformation> splitByResourceType(PaymentInformation paymentInfo) {
    List<PaymentInformation> splitPayments = new ArrayList<>();
    List<CreditTransferTransaction> originalChildTxns = paymentInfo.getCreditTransferTransactionList();
    
    if (originalChildTxns != null) {
        Map<String, List<CreditTransferTransaction>> groupedTxns = new HashMap<>();

        // Group transactions based on amount threshold
        for (CreditTransferTransaction childTxn : originalChildTxns) {
            BigDecimal txnAmount = childTxn.getPwsBulkTransactionInstructions().getTransactionAmount();
            String bankCode = getCreditorBankCode();
            String targetResourceId = determineResourceIdForSmart(bankCode, txnAmount);
            groupedTxns.computeIfAbsent(targetResourceId, k -> new ArrayList<>()).add(childTxn);
        }

        // Create new payment information for each group
        for (Map.Entry<String, List<CreditTransferTransaction>> entry : groupedTxns.entrySet()) {
            String resourceId = entry.getKey();
            List<CreditTransferTransaction> groupTxns = entry.getValue();
            PaymentInformation newPaymentInfo = createNewPaymentInfo(paymentInfo, resourceId, groupTxns);
            splitPayments.add(newPaymentInfo);
        }
    }
    
    return splitPayments;
}

private List<PaymentInformation> splitByMaxTransactions(PaymentInformation paymentInfo) {
    List<PaymentInformation> splitPayments = new ArrayList<>();
    List<CreditTransferTransaction> transactions = paymentInfo.getCreditTransferTransactionList();
    
    for (int i = 0; i < transactions.size(); i += appConfig.getDebulk().getMaxTransactionsPerBatch()) {
        int end = Math.min(i + appConfig.getDebulk().getMaxTransactionsPerBatch(), transactions.size());
        List<CreditTransferTransaction> batch = transactions.subList(i, end);
        
        PaymentInformation newPayment = createNewPaymentInfo(
            paymentInfo, 
            paymentInfo.getPwsTransactions().getResourceId(), 
            batch
        );
        splitPayments.add(newPayment);
    }
    
    return splitPayments;
}

private List<PaymentInformation> splitByMaxAmount(PaymentInformation paymentInfo) {
    List<PaymentInformation> splitPayments = new ArrayList<>();
    List<CreditTransferTransaction> transactions = paymentInfo.getCreditTransferTransactionList();
    List<CreditTransferTransaction> currentBatch = new ArrayList<>();
    BigDecimal currentAmount = BigDecimal.ZERO;
    
    for (CreditTransferTransaction txn : transactions) {
        BigDecimal txnAmount = txn.getPwsBulkTransactionInstructions().getTransactionAmount();
        
        if (currentAmount.add(txnAmount).compareTo(appConfig.getDebulk().getMaxAmountPerBatch()) > 0 
            && !currentBatch.isEmpty()) {
            PaymentInformation newPayment = createNewPaymentInfo(
                paymentInfo, 
                paymentInfo.getPwsTransactions().getResourceId(), 
                currentBatch
            );
            splitPayments.add(newPayment);
            currentBatch = new ArrayList<>();
            currentAmount = BigDecimal.ZERO;
        }
        
        currentBatch.add(txn);
        currentAmount = currentAmount.add(txnAmount);
    }
    
    if (!currentBatch.isEmpty()) {
        PaymentInformation newPayment = createNewPaymentInfo(
            paymentInfo, 
            paymentInfo.getPwsTransactions().getResourceId(), 
            currentBatch
        );
        splitPayments.add(newPayment);
    }
    
    return splitPayments;
}

// Existing methods remain the same
private String getCreditorBankCode() {
    return "024";
}

private String determineResourceIdForSmart(String bankCode, BigDecimal amount) {
    if (!appConfig.getDebulk().getDebulkSmart().getBankCode().equals(bankCode)
            && amount.compareTo(appConfig.getDebulk().getDebulkSmart().getBahtnetThreshold()) > 0) {
        return Resource.BAHTNET.id;
    }
    if (appConfig.getDebulk().getDebulkSmart().getSourceFormat().contains(getSourceFormat())) {
        return Resource.SMART_NEXT_DAY.id;
    }
    return Resource.SMART_SAME_DAY.id;
}

private PaymentInformation createNewPaymentInfo(PaymentInformation originalPayment, String resourceId,
        List<CreditTransferTransaction> transactions) {
    PaymentInformation newPaymentInfo = new PaymentInformation();
    newPaymentInfo.setDmpBulkStatus(originalPayment.getDmpBulkStatus());
    newPaymentInfo.setPwsBulkTransactions(originalPayment.getPwsBulkTransactions());

    PwsTransactions originalPwsTxn = originalPayment.getPwsTransactions();
    PwsTransactions newPwsTxn = new PwsTransactions();
    BeanUtils.copyProperties(originalPwsTxn, newPwsTxn);
    newPwsTxn.setResourceId(resourceId);
    newPaymentInfo.setPwsTransactions(newPwsTxn);
    newPaymentInfo.setCreditTransferTransactionList(transactions);

    return newPaymentInfo;
}
