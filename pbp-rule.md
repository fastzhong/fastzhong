```java
@Service
@RequiredArgsConstructor
public class PwsTransactionsValidation implements DecisionMatrixService<PwsTransactions> {
    private final GenericRuleTemplate ruleTemplate;
    private final RuleEngine ruleEngine;

    @Override
    public void updateRules(List<DecisionMatrixRow> decisionMatrix) {
        List<String> rules = decisionMatrix.stream()
                .map(ruleTemplate::generateRule)
                .collect(Collectors.toList());

        // Add a rule to set the global variable when an error is encountered
        rules.add(
            "rule \"Set Stop Flag On Error\"\n" +
            "when\n" +
            "    $tx : PwsTransactions(authorizationStatus == \"REJECTED\" || processingStatus == \"ERROR\")\n" +
            "then\n" +
            "    drools.getKieRuntime().setGlobal(\"shouldStop\", new AtomicBoolean(true));\n" +
            "end"
        );

        ruleEngine.updateRules(rules);
    }

    @Override
    public List<ValidationResult> applyRules(List<PwsTransactions> transactions, boolean errorOnFail) {
        ruleEngine.fireRules(transactions, errorOnFail);
        return transactions.stream()
                .map(this::validateSingle)
                .collect(Collectors.toList());
    }

    private ValidationResult validateSingle(PwsTransactions transaction) {
        ValidationResult result = new ValidationResult();
        
        if ("REJECTED".equals(transaction.getAuthorizationStatus())) {
            result.addError("Transaction rejected: " + transaction.getRejectReason());
        }
        
        if ("ERROR".equals(transaction.getProcessingStatus())) {
            result.addError("Processing error occurred");
        }

        return result;
    }
}

// Example usage in a service
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final PwsTransactionsValidation validator;

    public void processBatchTransactions(List<PwsTransactions> transactions, boolean errorOnFail) {
        List<ValidationResult> results = validator.applyRules(transactions, errorOnFail);
        
        for (int i = 0; i < results.size(); i++) {
            PwsTransactions transaction = transactions.get(i);
            ValidationResult result = results.get(i);
            
            if (result.isValid()) {
                processValidTransaction(transaction);
            } else {
                handleInvalidTransaction(transaction, result);
                if (errorOnFail) {
                    System.out.println("Stopping processing due to error.");
                    break;
                }
            }
        }
    }

    // ... processValidTransaction and handleInvalidTransaction methods ...
}
```

```java
class TransactionServiceTest {

    @Mock
    private PwsTransactionsValidation validator;

    @Mock
    private RuleEngine ruleEngine;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionService(validator);

        // Set up the rule engine with a simple rule
        String rule = 
            "rule \"Validate Transaction Amount\"\n" +
            "when\n" +
            "    $tx : PwsTransactions(transactionTotalAmount > 10000)\n" +
            "then\n" +
            "    $tx.setAuthorizationStatus(\"REJECTED\");\n" +
            "    $tx.setRejectReason(\"Amount exceeds limit\");\n" +
            "end";

        List<String> rules = Arrays.asList(rule);
        when(validator.getRuleEngine()).thenReturn(ruleEngine);
        doNothing().when(ruleEngine).updateRules(rules);
        when(validator.getRules()).thenReturn(rules);
    }

    @Test
    void testProcessBatchTransactions_ErrorOnFail() {
        // Create test transactions
        PwsTransactions validTx = new PwsTransactions();
        validTx.setTransactionId(1L);
        validTx.setTransactionTotalAmount(new BigDecimal(5000));

        PwsTransactions invalidTx = new PwsTransactions();
        invalidTx.setTransactionId(2L);
        invalidTx.setTransactionTotalAmount(new BigDecimal(15000));

        List<PwsTransactions> transactions = Arrays.asList(validTx, invalidTx);

        // Mock validator behavior
        when(validator.applyRules(eq(transactions), eq(true)))
            .thenAnswer(invocation -> {
                ruleEngine.fireRules(transactions, true);
                return transactions.stream()
                    .map(tx -> {
                        ValidationResult result = new ValidationResult();
                        if ("REJECTED".equals(tx.getAuthorizationStatus())) {
                            result.addError(tx.getRejectReason());
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
            });

        // Execute the method under test
        transactionService.processBatchTransactions(transactions, true);

        // Verify that processing stopped after the invalid transaction
        verify(validator, times(1)).applyRules(eq(transactions), eq(true));
        verify(transactionService, times(1)).processValidTransaction(validTx);
        verify(transactionService, times(1)).handleInvalidTransaction(eq(invalidTx), any(ValidationResult.class));
        verify(transactionService, never()).processValidTransaction(invalidTx);
    }

    @Test
    void testProcessBatchTransactions_ContinueOnError() {
        // Create test transactions
        PwsTransactions validTx1 = new PwsTransactions();
        validTx1.setTransactionId(1L);
        validTx1.setTransactionTotalAmount(new BigDecimal(5000));

        PwsTransactions invalidTx = new PwsTransactions();
        invalidTx.setTransactionId(2L);
        invalidTx.setTransactionTotalAmount(new BigDecimal(15000));

        PwsTransactions validTx2 = new PwsTransactions();
        validTx2.setTransactionId(3L);
        validTx2.setTransactionTotalAmount(new BigDecimal(7000));

        List<PwsTransactions> transactions = Arrays.asList(validTx1, invalidTx, validTx2);

        // Mock validator behavior
        when(validator.applyRules(eq(transactions), eq(false)))
            .thenAnswer(invocation -> {
                ruleEngine.fireRules(transactions, false);
                return transactions.stream()
                    .map(tx -> {
                        ValidationResult result = new ValidationResult();
                        if ("REJECTED".equals(tx.getAuthorizationStatus())) {
                            result.addError(tx.getRejectReason());
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
            });

        // Execute the method under test
        transactionService.processBatchTransactions(transactions, false);

        // Verify that all transactions were processed
        verify(validator, times(1)).applyRules(eq(transactions), eq(false));
        verify(transactionService, times(1)).processValidTransaction(validTx1);
        verify(transactionService, times(1)).handleInvalidTransaction(eq(invalidTx), any(ValidationResult.class));
        verify(transactionService, times(1)).processValidTransaction(validTx2);
    }
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

