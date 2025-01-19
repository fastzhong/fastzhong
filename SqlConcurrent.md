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

## unit test

```java
    
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
```
