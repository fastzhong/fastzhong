# ApprovalStatusServiceImpl

## code 

```java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static com.uob.gwb.txn.common.GenericConstants.YES;

/**
 * Service implementation for processing approval status transactions with concurrent execution.
 * This implementation focuses on optimizing performance through parallel query execution.
 */
@Slf4j
@Service("ApprovalStatusService")
@RequiredArgsConstructor
public class ApprovalStatusServiceImpl implements ApprovalStatusService {
    
    // Number of threads for concurrent execution
    private static final int THREAD_POOL_SIZE = 4;
    // Maximum time to wait for database queries
    private static final int QUERY_TIMEOUT_SECONDS = 7;

    // Required dependencies
    private final TransactionWorkflowDAO transactionWorkflowDAO;

    /**
     * Thread pool executor for managing concurrent database queries.
     * Fixed size pool is used to prevent resource exhaustion.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * Cleanup method to properly shutdown the executor service.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Initiating executor service shutdown");
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                log.warn("Executor service did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during executor service shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Main method for processing bulk approval status requests.
     * Routes requests to appropriate processing method based on child/parent status.
     */
    protected List<ApprovalStatusTxn> getBulkApprovalStatus(FilterParams filterParams) {
        if (!shouldProcessBulkApproval(filterParams)) {
            return new ArrayList<>();
        }

        try {
            return YES.equalsIgnoreCase(filterParams.getIsChild())
                ? processChildTransactions(filterParams)
                : processParentTransactions(filterParams);
        } catch (Exception e) {
            log.error("Error in bulk approval processing: {}", e.getMessage(), e);
            throw new TransactionProcessingException("Failed to process bulk approval status", e);
        }
    }

    /**
     * Processes child transactions with concurrent execution of count query.
     */
    private List<ApprovalStatusTxn> processChildTransactions(FilterParams filterParams) {
        try {
            filterParams.setIsChildY(YES);

            // Execute main transaction list query
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams), 
                           executorService);

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            // Execute count query concurrently
            CompletableFuture<Integer> countFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkApprovalStatusTxnCount(filterParams),
                           executorService);

            int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Total number of child transactions: {}", count);
            
            // Update transaction count
            approvalStatusList.stream()
                .findFirst()
                .ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));

            return approvalStatusList;

        } catch (TimeoutException e) {
            throw new TransactionProcessingException("Query timeout occurred", e);
        } catch (Exception e) {
            throw new TransactionProcessingException("Failed to process child transactions", e);
        }
    }

    /**
     * Processes parent transactions with concurrent execution of count, FX contracts,
     * and charges queries.
     */
    private List<ApprovalStatusTxn> processParentTransactions(FilterParams filterParams) {
        try {
            filterParams.setIsChildN(YES);

            // Execute main transaction list query
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams), 
                           executorService);

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            List<String> transIds = extractTransactionIds(approvalStatusList);

            // Execute all supporting queries concurrently
            CompletableFuture<Integer> countFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams),
                           executorService);

            CompletableFuture<List<PwsTransactionCharges>> chargesFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getChargesDetail(transIds),
                           executorService);

            CompletableFuture<List<ApprovalStatusTxn>> fxContractsFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getFxContracts(transIds),
                           executorService);

            // Wait for all futures to complete
            int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<PwsTransactionCharges> chargesDetail = chargesFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<ApprovalStatusTxn> fxContracts = fxContractsFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Update approval status list with results
            updateParentTransactions(approvalStatusList, count, chargesDetail, fxContracts);
            return approvalStatusList;

        } catch (TimeoutException e) {
            throw new TransactionProcessingException("Query timeout occurred", e);
        } catch (Exception e) {
            throw new TransactionProcessingException("Failed to process parent transactions", e);
        }
    }

    /**
     * Updates parent transactions with charge and FX contract details.
     */
    private void updateParentTransactions(
            List<ApprovalStatusTxn> approvalStatusList,
            int count,
            List<PwsTransactionCharges> chargesDetail,
            List<ApprovalStatusTxn> fxContracts) {
        
        // Create FX contracts map for easier lookup
        Map<String, ApprovalStatusTxn> fxContractMap = fxContracts.stream()
            .collect(Collectors.toMap(
                ApprovalStatusTxn::getTransactionId,
                Function.identity(),
                (existing, replacement) -> existing
            ));

        // Process charges by transaction ID
        Map<Long, List<PwsTransactionCharges>> chargesMap = chargesDetail.stream()
            .collect(Collectors.groupingBy(PwsTransactionCharges::getTransactionId));

        // Update each transaction with its details
        BigDecimal countValue = BigDecimal.valueOf(count);
        approvalStatusList.forEach(approve -> {
            approve.setCount(countValue);
            
            // Update FX contract details
            ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
            if (fx != null) {
                approve.setFxType(fx.getFxType());
                approve.setFxFlag(fx.getFxFlag());
                approve.setBookingRefId(fx.getBookingRefId());
                approve.setEarmarkId(fx.getEarmarkId());
            }

            // Update charges details
            Long transId = Long.valueOf(approve.getTransactionId());
            List<PwsTransactionCharges> charges = chargesMap.get(transId);
            if (charges != null && !charges.isEmpty()) {
                updateChargesDetails(approve, charges);
            }
        });
    }

    /**
     * Updates a transaction with its charge details.
     */
    private void updateChargesDetails(
            ApprovalStatusTxn approve,
            List<PwsTransactionCharges> charges) {
        
        BigDecimal totalAmount = charges.stream()
            .map(PwsTransactionCharges::getFeesAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        String transCurrency = charges.get(0).getFeesCurrency();
        
        approve.setFeesCurrency(transCurrency);
        approve.setTotalFeeAmount(totalAmount);
    }

    /**
     * Extracts transaction IDs from approval status list.
     */
    private List<String> extractTransactionIds(List<ApprovalStatusTxn> approvalStatusList) {
        return approvalStatusList.stream()
            .map(ApprovalStatusTxn::getTransactionId)
            .collect(Collectors.toList());
    }

    /**
     * Checks if bulk approval processing should proceed.
     */
    private boolean shouldProcessBulkApproval(FilterParams filterParams) {
        return YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
            || CollectionUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList());
    }
}
```

# unit test

```java
@ExtendWith(MockitoExtension.class)
class ApprovalStatusServiceImplTest {
    // ... existing fields and setup remain the same ...

    @Nested
    @DisplayName("Child Transaction Edge Cases")
    class ChildTransactionEdgeCases {
        
        @Test
        @DisplayName("Should handle concurrent query failures")
        void shouldHandleConcurrentQueryFailures() {
            // Given
            setupChildTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(any()))
                .thenThrow(new RuntimeException("Count query failed"));

            // When/Then
            assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed to process child transactions");
        }

        @Test
        @DisplayName("Should handle interrupted execution")
        void shouldHandleInterruptedExecution() {
            // Given
            setupChildTransactionScenario();
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenAnswer(inv -> {
                    Thread.currentThread().interrupt();
                    return null;
                });

            // When/Then
            assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
                .isInstanceOf(TransactionProcessingException.class);
            
            // Clear interrupted state
            Thread.interrupted();
        }

        @Test
        @DisplayName("Should handle zero count result")
        void shouldHandleZeroCountResult() {
            // Given
            setupChildTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(any()))
                .thenReturn(0);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .isNotEmpty()
                .first()
                .satisfies(txn -> assertThat(txn.getCount()).isEqualTo(BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("Parent Transaction Edge Cases")
    class ParentTransactionEdgeCases {

        @Test
        @DisplayName("Should handle empty charges list")
        void shouldHandleEmptyChargesList() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            List<ApprovalStatusTxn> fxContracts = createSampleFxContracts();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getFxContracts(any()))
                .thenReturn(fxContracts);
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(Collections.emptyList());

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .isNotEmpty()
                .allSatisfy(txn -> {
                    assertThat(txn.getTotalFeeAmount()).isNull();
                    assertThat(txn.getFeesCurrency()).isNull();
                });
        }

        @Test
        @DisplayName("Should handle mismatched transaction IDs")
        void shouldHandleMismatchedTransactionIds() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            List<ApprovalStatusTxn> fxContracts = createMismatchedFxContracts();
            List<PwsTransactionCharges> charges = createMismatchedCharges();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getFxContracts(any()))
                .thenReturn(fxContracts);
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(charges);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .isNotEmpty()
                .allSatisfy(txn -> {
                    assertThat(txn.getFxType()).isNull();
                    assertThat(txn.getTotalFeeAmount()).isNull();
                });
        }

        @Test
        @DisplayName("Should handle partial query failures")
        void shouldHandlePartialQueryFailures() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getFxContracts(any()))
                .thenThrow(new RuntimeException("FX query failed"));
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(createSampleCharges());

            // When/Then
            assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed to process parent transactions");
        }
    }

    @Nested
    @DisplayName("Special Condition Tests")
    class SpecialConditionTests {

        @Test
        @DisplayName("Should handle missing channel admin flag")
        void shouldHandleMissingChannelAdminFlag() {
            // Given
            filterParams.setIsChannelAdmin(null);
            filterParams.setBulkAccountBasedOnResourceFeatureList(null);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle invalid child flag")
        void shouldHandleInvalidChildFlag() {
            // Given
            setupParentTransactionScenario();
            filterParams.setIsChild("INVALID");

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnList(any());
        }

        @Test
        @DisplayName("Should handle transaction ID conversion error")
        void shouldHandleTransactionIdConversionError() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createInvalidTransactionList();
            List<PwsTransactionCharges> charges = createSampleCharges();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(charges);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .isNotEmpty()
                .allSatisfy(txn -> assertThat(txn.getTotalFeeAmount()).isNull());
        }
    }

    // Additional helper methods
    private List<ApprovalStatusTxn> createMismatchedFxContracts() {
        List<ApprovalStatusTxn> list = new ArrayList<>();
        ApprovalStatusTxn fx = new ApprovalStatusTxn();
        fx.setTransactionId("999"); // Mismatched ID
        fx.setFxType("TYPE1");
        list.add(fx);
        return list;
    }

    private List<PwsTransactionCharges> createMismatchedCharges() {
        List<PwsTransactionCharges> list = new ArrayList<>();
        PwsTransactionCharges charge = new PwsTransactionCharges();
        charge.setTransactionId(999L); // Mismatched ID
        charge.setFeesAmount(BigDecimal.TEN);
        list.add(charge);
        return list;
    }

    private List<ApprovalStatusTxn> createInvalidTransactionList() {
        List<ApprovalStatusTxn> list = new ArrayList<>();
        ApprovalStatusTxn txn = new ApprovalStatusTxn();
        txn.setTransactionId("INVALID"); // Non-numeric ID
        list.add(txn);
        return list;
    }
}
```

# ApprovalStatusV2ServiceImpl

## code

```java
@Slf4j
@Service("ApprovalStatusV2Service")
public class ApprovalStatusV2ServiceImpl extends ApprovalStatusServiceImpl implements ApprovalStatusV2Service {
    
    private static final int QUERY_TIMEOUT_SECONDS = 7;
    private static final int THREAD_POOL_SIZE = 4;
    private static final int BATCH_SIZE = 1000;

    // Thread pool configuration with custom rejection handler
    private final ExecutorService executorService = new ThreadPoolExecutor(
        THREAD_POOL_SIZE, 
        THREAD_POOL_SIZE,
        0L, 
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy() // Fallback strategy
    );

    // Cache for frequently accessed data
    private final LoadingCache<String, Integer> transactionCountCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(new CacheLoader<>() {
            @Override
            public Integer load(String key) {
                return 0; // Default value
            }
        });

    @PreDestroy
    public void cleanup() {
        try {
            log.info("Initiating executor service shutdown");
            executorService.shutdown();
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                log.warn("Executor service did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during executor service shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ApprovalStatusLookUpResp setApprovalStatusTxnList(
            String transactionType,
            FilterParams filterParams,
            TransactionsLookUpReq request,
            CompanyAndAccountsForUserResourceFeaturesResp response,
            String isChild) {
        
        MDC.put("transactionType", transactionType);
        MDC.put("requestId", request.getRequestId());
        
        try {
            validateInput(transactionType, filterParams, request);

            List<ApprovalStatusTxn> approvalStatusTxnList = processTransactionsByType(
                transactionType, filterParams, request);

            if (ObjectUtils.isEmpty(approvalStatusTxnList)) {
                log.debug("No transactions found for type: {}", transactionType);
                return null;
            }

            return setApprovalStatusLookUpResp(
                approvalStatusTxnList, 
                response, 
                transactionType, 
                isChild, 
                request
            );
            
        } catch (ValidationException e) {
            log.error("Validation error in approval status processing", e);
            throw e;
        } catch (Exception e) {
            log.error("Error processing approval status transactions for type: {}", transactionType, e);
            throw new TransactionProcessingException(
                String.format("Failed to process approval status transactions for type: %s", transactionType), 
                e
            );
        } finally {
            MDC.clear();
        }
    }

    private void validateInput(String transactionType, FilterParams filterParams, TransactionsLookUpReq request) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(transactionType)) {
            errors.add("Transaction type cannot be empty");
        }

        if (filterParams == null) {
            errors.add("Filter parameters cannot be null");
        }

        if (request == null || request.getAdditionalProperties() == null) {
            errors.add("Request or its properties cannot be null");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Input validation failed: " + String.join(", ", errors));
        }
    }

    private <T> CompletableFuture<T> executeWithRetry(Supplier<T> supplier, String operationName) {
        return CompletableFuture.supplyAsync(() -> {
            int maxRetries = 3;
            int retryCount = 0;
            while (true) {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        log.error("Operation {} failed after {} retries", operationName, maxRetries, e);
                        throw new TransactionProcessingException(
                            String.format("Operation %s failed after retries", operationName), e);
                    }
                    log.warn("Retry {} for operation {}", retryCount, operationName);
                    try {
                        Thread.sleep(100L * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new TransactionProcessingException("Interrupted during retry", ie);
                    }
                }
            }
        }, executorService);
    }

    private List<ApprovalStatusTxn> processSingleTransactions(FilterParams filterParams) {
        String cacheKey = buildCacheKey(filterParams);
        
        try {
            // Execute queries with retry mechanism
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = executeWithRetry(
                () -> transactionWorkflowV2DAO.getApprovalStatusTxnList(filterParams),
                "getApprovalStatusTxnList"
            );

            CompletableFuture<Integer> countFuture = executeWithRetry(
                () -> transactionWorkflowV2DAO.getApprovalStatusTxnCount(filterParams),
                "getApprovalStatusTxnCount"
            );

            // Wait for results with timeout
            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            // Process count and update cache
            int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            transactionCountCache.put(cacheKey, count);
            
            log.info("Total number of transaction Count for Single Transaction {}", count);
            updateTransactionCount(approvalStatusList, count);

            return approvalStatusList;

        } catch (TimeoutException e) {
            log.error("Timeout while processing single transactions", e);
            throw new TransactionProcessingException("Operation timed out for single transactions", e);
        } catch (Exception e) {
            log.error("Error processing single transactions", e);
            throw new TransactionProcessingException("Failed to process single transactions", e);
        }
    }

    private List<ApprovalStatusTxn> processBothTransactions(
            FilterParams filterParams,
            TransactionsLookUpReq request) {
        
        try {
            prepareBothTransactionsParams(filterParams, request);

            // Execute all queries concurrently with retry
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = executeWithRetry(
                () -> transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(filterParams, PAYEE_TAX),
                "getBulkBothApprovalStatusTxnList"
            );

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            List<String> transIds = extractTransactionIds(approvalStatusList);

            // Process in batches if needed
            List<List<String>> transIdBatches = Lists.partition(transIds, BATCH_SIZE);
            
            // Execute remaining queries concurrently
            CompletableFuture<Integer> countFuture = executeWithRetry(
                () -> transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(filterParams),
                "getV2BulkBothApprovalStatusTxnCount"
            );

            // Process FX contracts in batches
            CompletableFuture<Map<String, ApprovalStatusTxn>> fxContractsFuture = processFxContractsInBatches(transIdBatches);

            // Wait for all futures with timeout
            int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Map<String, ApprovalStatusTxn> fxContractMap = fxContractsFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            updateBothTransactions(approvalStatusList, count, fxContractMap);
            return approvalStatusList;

        } catch (Exception e) {
            handleProcessingError(e, "both");
            throw new TransactionProcessingException("Failed to process both transactions", e);
        }
    }

    private CompletableFuture<Map<String, ApprovalStatusTxn>> processFxContractsInBatches(List<List<String>> transIdBatches) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return transIdBatches.parallelStream()
                    .map(batch -> {
                        try {
                            return transactionWorkflowV2DAO.getFxContracts(batch);
                        } catch (Exception e) {
                            log.error("Error processing FX contracts batch", e);
                            return Collections.<ApprovalStatusTxn>emptyList();
                        }
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toMap(
                        ApprovalStatusTxn::getTransactionId,
                        Function.identity(),
                        (existing, replacement) -> existing
                    ));
            } catch (Exception e) {
                log.error("Error in FX contracts batch processing", e);
                return Collections.emptyMap();
            }
        }, executorService);
    }

    private void handleProcessingError(Exception e, String transactionType) {
        if (e instanceof TimeoutException) {
            log.error("Timeout while processing {} transactions", transactionType, e);
            // Consider partial results or fallback strategy
        } else if (e instanceof CompletionException) {
            log.error("Error in concurrent execution for {} transactions", transactionType, e.getCause());
        } else {
            log.error("Unexpected error processing {} transactions", transactionType, e);
        }
    }

    @VisibleForTesting
    String buildCacheKey(FilterParams filterParams) {
        return String.format("%s_%s_%s",
            filterParams.getCompanyId(),
            filterParams.getAccountNumber(),
            filterParams.getDateFrom()
        );
    }

    @VisibleForTesting
    void updateTransactionCount(List<ApprovalStatusTxn> transactions, int count) {
        BigDecimal countValue = BigDecimal.valueOf(count);
        transactions.forEach(txn -> txn.setCount(countValue));
    }
}
```

## unit test

```java
@ExtendWith(MockitoExtension.class)
class ApprovalStatusV2ServiceImplTest {
    // ... previous code remains the same ...

    @Nested
    @DisplayName("Cache Tests")
    class CacheTests {
        
        @Test
        @DisplayName("Should use cached count for repeated calls")
        void shouldUseCachedCountForRepeatedCalls() {
            // Given
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowV2DAO.getApprovalStatusTxnCount(any()))
                .thenReturn(10);

            // When
            service.setApprovalStatusTxnList("SINGLE", filterParams, request, response, "N");
            service.setApprovalStatusTxnList("SINGLE", filterParams, request, response, "N");

            // Then
            verify(transactionWorkflowV2DAO, times(1)).getApprovalStatusTxnCount(any());
        }

        @Test
        @DisplayName("Should build correct cache key")
        void shouldBuildCorrectCacheKey() {
            // Given
            filterParams.setCompanyId("COMP1");
            filterParams.setAccountNumber("ACC1");
            filterParams.setDateFrom("2024-01-01");

            // When
            String cacheKey = service.buildCacheKey(filterParams);

            // Then
            assertThat(cacheKey).isEqualTo("COMP1_ACC1_2024-01-01");
        }
    }

    @Nested
    @DisplayName("Retry Mechanism Tests")
    class RetryMechanismTests {

        @Test
        @DisplayName("Should retry failed operation with exponential backoff")
        void shouldRetryFailedOperationWithExponentialBackoff() {
            // Given
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
                .thenThrow(new RuntimeException("First attempt"))
                .thenThrow(new RuntimeException("Second attempt"))
                .thenReturn(expectedList);

            // When
            long startTime = System.currentTimeMillis();
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "SINGLE", filterParams, request, response, "N");
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(result).isNotNull();
            assertThat(duration).isGreaterThan(300); // Verify backoff delay
            verify(transactionWorkflowV2DAO, times(3)).getApprovalStatusTxnList(any());
        }

        @Test
        @DisplayName("Should handle max retry exhaustion")
        void shouldHandleMaxRetryExhaustion() {
            // Given
            when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
                .thenThrow(new RuntimeException("Persistent error"));

            // When/Then
            assertThatThrownBy(() -> service.setApprovalStatusTxnList(
                "SINGLE", filterParams, request, response, "N"))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed after retries");
        }
    }

    @Nested
    @DisplayName("Batch Processing Tests")
    class BatchProcessingTests {

        @Test
        @DisplayName("Should handle partial batch processing failure")
        void shouldHandlePartialBatchProcessingFailure() {
            // Given
            setupBothTransactionScenario();
            List<ApprovalStatusTxn> largeList = createLargeTransactionList(2000);
            
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
                .thenReturn(largeList);
            when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
                .thenReturn(2000);
            when(transactionWorkflowV2DAO.getFxContracts(any()))
                .thenThrow(new RuntimeException("Batch failure"))
                .thenReturn(createSampleFxContracts());

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N");

            // Then
            assertThat(result).isNotNull();
            verify(transactionWorkflowV2DAO, atLeast(2)).getFxContracts(any());
        }

        @Test
        @DisplayName("Should process empty batches correctly")
        void shouldProcessEmptyBatchesCorrectly() {
            // Given
            setupBothTransactionScenario();
            List<ApprovalStatusTxn> emptyList = Collections.emptyList();
            
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
                .thenReturn(emptyList);

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N");

            // Then
            assertThat(result).isNull();
            verify(transactionWorkflowV2DAO, never()).getFxContracts(any());
        }
    }

    @Nested
    @DisplayName("Concurrent Processing Tests")
    class ConcurrentProcessingTests {

        @Test
        @DisplayName("Should handle concurrent request overflow")
        void shouldHandleConcurrentRequestOverflow() {
            // Given
            when(executorService.submit(any(Callable.class)))
                .thenThrow(new RejectedExecutionException())
                .thenReturn(CompletableFuture.completedFuture(createSampleTransactionList()));

            // When/Then
            assertThatThrownBy(() -> service.setApprovalStatusTxnList(
                "SINGLE", filterParams, request, response, "N"))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed to process");
        }

        @Test
        @DisplayName("Should handle thread interruption during processing")
        void shouldHandleThreadInterruptionDuringProcessing() {
            // Given
            when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
                .thenAnswer(inv -> {
                    Thread.currentThread().interrupt();
                    return null;
                });

            // When/Then
            assertThatThrownBy(() -> service.setApprovalStatusTxnList(
                "SINGLE", filterParams, request, response, "N"))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Interrupted during processing");
        }
    }

    @Test
    @DisplayName("Should handle null FX contracts in both transactions")
    void shouldHandleNullFxContractsInBothTransactions() {
        // Given
        setupBothTransactionScenario();
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        
        when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
            .thenReturn(txnList);
        when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
            .thenReturn(10);
        when(transactionWorkflowV2DAO.getFxContracts(any()))
            .thenReturn(null);

        // When
        ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
            "BOTH", filterParams, request, response, "N");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactions())
            .allMatch(txn -> txn.getFxType() == null && txn.getFxFlag() == null);
    }

    @Test
    @DisplayName("Should handle invalid transaction type")
    void shouldHandleInvalidTransactionType() {
        // When
        ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
            "INVALID_TYPE", filterParams, request, response, "N");

        // Then
        assertThat(result).isNull();
        verifyNoInteractions(transactionWorkflowV2DAO);
    }

    @Test
    @DisplayName("Should preserve MDC context")
    void shouldPreserveMdcContext() {
        // Given
        String originalValue = MDC.get("existingKey");
        MDC.put("existingKey", "value");

        // When
        try {
            service.setApprovalStatusTxnList("SINGLE", filterParams, request, response, "N");
        } finally {
            // Then
            assertThat(MDC.get("existingKey")).isEqualTo("value");
            if (originalValue != null) {
                MDC.put("existingKey", originalValue);
            } else {
                MDC.remove("existingKey");
            }
        }
    }
}
```
