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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApprovalStatusServiceImplTest {

    @Mock
    private TransactionWorkflowDAO transactionWorkflowDAO;

    @InjectMocks
    private ApprovalStatusServiceImpl service;

    private FilterParams filterParams;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        filterParams = new FilterParams();
        filterParams.setSortFieldWithDirection("bothTrans.orderBy");
        executorService = mock(ExecutorService.class);
        ReflectionTestUtils.setField(service, "executorService", executorService);
    }

    @Nested
    @DisplayName("Basic Validation Tests")
    class BasicValidationTests {
        // ... [Previous validation tests remain the same]
    }

    @Nested
    @DisplayName("Child Transaction Tests")
    class ChildTransactionTests {
        // ... [Previous child transaction tests remain the same]

        @Test
        @DisplayName("Should handle null child flag")
        void shouldHandleNullChildFlag() {
            // Given
            filterParams.setIsChannelAdmin("Y");
            filterParams.setIsChild(null);
            filterParams.setBulkAccountBasedOnResourceFeatureList(List.of("ACCOUNT1"));

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnList(any());
        }

        @Test
        @DisplayName("Should handle large transaction lists")
        void shouldHandleLargeTransactionLists() {
            // Given
            setupChildTransactionScenario();
            List<ApprovalStatusTxn> largeList = createLargeTransactionList(1000);
            
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenReturn(largeList);
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(any()))
                .thenReturn(1000);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).hasSize(1000);
            verify(transactionWorkflowDAO).getBulkApprovalStatusTxnList(any());
            verify(transactionWorkflowDAO).getBulkApprovalStatusTxnCount(any());
        }
    }

    @Nested
    @DisplayName("Parent Transaction Tests")
    class ParentTransactionTests {
        // ... [Previous parent transaction tests remain the same]

        @Test
        @DisplayName("Should handle charges with zero amounts")
        void shouldHandleChargesWithZeroAmounts() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            List<PwsTransactionCharges> charges = createZeroAmountCharges();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getFxContracts(any()))
                .thenReturn(Collections.emptyList());
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(charges);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .isNotEmpty()
                .allSatisfy(txn -> assertThat(txn.getTotalFeeAmount()).isEqualTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should handle duplicate transaction IDs in charges")
        void shouldHandleDuplicateTransactionIdsInCharges() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            List<PwsTransactionCharges> charges = createDuplicateCharges();
            
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
                .allSatisfy(txn -> 
                    assertThat(txn.getTotalFeeAmount())
                        .isEqualTo(BigDecimal.valueOf(20))); // Sum of duplicate charges
        }
    }

    @Nested
    @DisplayName("Concurrent Execution Tests")
    class ConcurrentExecutionTests {
        
        @Test
        @DisplayName("Should handle concurrent updates to same transaction")
        void shouldHandleConcurrentUpdatesToSameTransaction() {
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
                .thenAnswer(inv -> {
                    Thread.sleep(100); // Simulate delay
                    return createSampleCharges();
                });

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .isNotEmpty()
                .allSatisfy(txn -> {
                    assertThat(txn.getFxType()).isNotNull();
                    assertThat(txn.getTotalFeeAmount()).isNotNull();
                });
        }

        @Test
        @DisplayName("Should handle executor service shutdown during processing")
        void shouldHandleExecutorServiceShutdownDuringProcessing() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(executorService.isShutdown()).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed to process");
        }
    }

    // Helper Methods
    // ... [Previous helper methods remain the same]

    private List<PwsTransactionCharges> createZeroAmountCharges() {
        List<PwsTransactionCharges> charges = new ArrayList<>();
        PwsTransactionCharges charge = new PwsTransactionCharges();
        charge.setTransactionId(1L);
        charge.setFeesAmount(BigDecimal.ZERO);
        charge.setFeesCurrency("USD");
        charges.add(charge);
        return charges;
    }

    private List<PwsTransactionCharges> createDuplicateCharges() {
        List<PwsTransactionCharges> charges = new ArrayList<>();
        PwsTransactionCharges charge1 = new PwsTransactionCharges();
        charge1.setTransactionId(1L);
        charge1.setFeesAmount(BigDecimal.TEN);
        charge1.setFeesCurrency("USD");
        
        PwsTransactionCharges charge2 = new PwsTransactionCharges();
        charge2.setTransactionId(1L);
        charge2.setFeesAmount(BigDecimal.TEN);
        charge2.setFeesCurrency("USD");
        
        charges.add(charge1);
        charges.add(charge2);
        return charges;
    }

    private List<ApprovalStatusTxn> createLargeTransactionList(int size) {
        List<ApprovalStatusTxn> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ApprovalStatusTxn txn = new ApprovalStatusTxn();
            txn.setTransactionId(String.valueOf(i));
            list.add(txn);
        }
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

    @Mock
    private TransactionWorkflowDAO transactionWorkflowDAO;
    
    @Mock
    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;
    
    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ApprovalStatusV2ServiceImpl service;

    private FilterParams filterParams;
    private TransactionsLookUpReq request;
    private CompanyAndAccountsForUserResourceFeaturesResp response;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        filterParams = new FilterParams();
        filterParams.setSortFieldWithDirection("bothTrans.orderBy");
        
        request = new TransactionsLookUpReq();
        Map<String, Object> props = new HashMap<>();
        props.put("limit", "10");
        request.setAdditionalProperties(props);
        request.setRequestId("REQ123");
        
        response = new CompanyAndAccountsForUserResourceFeaturesResp();
        
        executorService = mock(ExecutorService.class);
        ReflectionTestUtils.setField(service, "executorService", executorService);
    }

    @Nested
    @DisplayName("Single Transaction Tests")
    class SingleTransactionTests {

        @Test
        @DisplayName("Should process single transactions successfully")
        void shouldProcessSingleTransactionsSuccessfully() {
            // Given
            List<ApprovalStatusTxn> txnList = createSampleTransactionList();
            when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
                .thenReturn(txnList);

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "SINGLE", filterParams, request, response, "N");

            // Then
            assertThat(result).isNotNull();
            verify(transactionWorkflowV2DAO).getApprovalStatusTxnList(any());
        }

        @Test
        @DisplayName("Should handle empty result for single transactions")
        void shouldHandleEmptyResultForSingleTransactions() {
            // Given
            when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
                .thenReturn(Collections.emptyList());

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "SINGLE", filterParams, request, response, "N");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Both Transaction Tests")
    class BothTransactionTests {

        @Test
        @DisplayName("Should process both transactions successfully")
        void shouldProcessBothTransactionsSuccessfully() {
            // Given
            setupBothTransactionScenario();
            List<ApprovalStatusTxn> txnList = createSampleTransactionList();
            List<ApprovalStatusTxn> fxContracts = createSampleFxContracts();
            
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
                .thenReturn(txnList);
            when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowV2DAO.getFxContracts(any()))
                .thenReturn(fxContracts);

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTransactions())
                .hasSize(2)
                .allSatisfy(txn -> {
                    assertThat(txn.getCount()).isEqualTo(BigDecimal.valueOf(10));
                    assertThat(txn.getFxType()).isNotNull();
                });
        }

        @Test
        @DisplayName("Should handle concurrent execution for both transactions")
        void shouldHandleConcurrentExecutionForBothTransactions() {
            // Given
            setupBothTransactionScenario();
            List<ApprovalStatusTxn> txnList = createSampleTransactionList();
            
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
                .thenReturn(txnList);
            when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
                .thenAnswer(inv -> {
                    Thread.sleep(100);
                    return 10;
                });
            when(transactionWorkflowV2DAO.getFxContracts(any()))
                .thenReturn(createSampleFxContracts());

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N");

            // Then
            assertThat(result).isNotNull();
            verify(transactionWorkflowV2DAO).getV2BulkBothApprovalStatusTxnCount(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle timeout in both transactions")
        void shouldHandleTimeoutInBothTransactions() {
            // Given
            setupBothTransactionScenario();
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), any()))
                .thenAnswer(inv -> {
                    Thread.sleep(8000); // Simulate timeout
                    return null;
                });

            // When/Then
            assertThatThrownBy(() -> service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N"))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Query timeout occurred");
        }

        @Test
        @DisplayName("Should handle missing limit in request")
        void shouldHandleMissingLimitInRequest() {
            // Given
            setupBothTransactionScenario();
            request.setAdditionalProperties(new HashMap<>());

            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle invalid transaction type")
        void shouldHandleInvalidTransactionType() {
            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "INVALID", filterParams, request, response, "N");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle null response object")
        void shouldHandleNullResponseObject() {
            // When
            ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, null, "N");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Concurrent Execution Tests")
    class ConcurrentExecutionTests {

        @Test
        @DisplayName("Should handle executor service shutdown")
        void shouldHandleExecutorServiceShutdown() {
            // Given
            setupBothTransactionScenario();
            List<ApprovalStatusTxn> txnList = createSampleTransactionList();
            
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), any()))
                .thenReturn(txnList);
            when(executorService.isShutdown()).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N"))
                .isInstanceOf(TransactionProcessingException.class);
        }

        @Test
        @DisplayName("Should handle interrupted execution")
        void shouldHandleInterruptedExecution() {
            // Given
            setupBothTransactionScenario();
            when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), any()))
                .thenAnswer(inv -> {
                    Thread.currentThread().interrupt();
                    return null;
                });

            // When/Then
            assertThatThrownBy(() -> service.setApprovalStatusTxnList(
                "BOTH", filterParams, request, response, "N"))
                .isInstanceOf(TransactionProcessingException.class);
            
            // Clear interrupted state
            Thread.interrupted();
        }
    }

    // Helper methods
    private void setupBothTransactionScenario() {
        filterParams.setSingleAccountBasedOnResourceFeatureList(List.of("ACC1"));
        filterParams.setBulkAccountBasedOnResourceFeatureList(List.of("ACC2"));
    }

    private List<ApprovalStatusTxn> createSampleTransactionList() {
        List<ApprovalStatusTxn> list = new ArrayList<>();
        ApprovalStatusTxn txn1 = new ApprovalStatusTxn();
        txn1.setTransactionId("1");
        ApprovalStatusTxn txn2 = new ApprovalStatusTxn();
        txn2.setTransactionId("2");
        list.add(txn1);
        list.add(txn2);
        return list;
    }

    private List<ApprovalStatusTxn> createSampleFxContracts() {
        List<ApprovalStatusTxn> list = new ArrayList<>();
        ApprovalStatusTxn fx1 = new ApprovalStatusTxn();
        fx1.setTransactionId("1");
        fx1.setFxType("TYPE1");
        fx1.setFxFlag("FLAG1");
        fx1.setBookingRefId("BOOKING1");
        fx1.setEarmarkId("EARMARK1");
        list.add(fx1);
        return list;
    }

    private List<ApprovalStatusTxn> createLargeTransactionList(int size) {
        List<ApprovalStatusTxn> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ApprovalStatusTxn txn = new ApprovalStatusTxn();
            txn.setTransactionId(String.valueOf(i));
            list.add(txn);
        }
        return list;
    }
}
```
