
The issue with dependency injection due to Java dynamic proxies in Spring Boot 3.2 and Java 17 arises because Spring defaults to JDK dynamic proxies for beans implementing interfaces. This can cause injection errors when the target bean is expected as a concrete class but is proxied as an interface-based proxy.
Solutions:
	1.	Force CGLIB Proxies: Add `spring.aop.proxy-target-class=true` in `application.properties` or annotate with `@EnableAsync(proxyTargetClass = true)` or `@EnableCaching(proxyTargetClass = true)`.
	2.	Inject Interfaces: Instead of injecting the concrete class, inject one of its interfaces.
	3.	Compile with `-parameters`: Ensure bytecode includes parameter names by enabling the `-parameters` option in your compiler.
	4.	Check Dependencies: Ensure compatibility of third-party libraries with Spring Boot 3.x and Java 17.

# ApprovalStatusServiceImpl

## code 

```java
package com.uob.gwb.txn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.transaction.common.service.CamelService;
import com.uob.gwb.transaction.common.service.EntitlementService;
import com.uob.gwb.transaction.common.util.HttpUtils;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.txn.config.TransactionWorkFlowConfig;
import com.uob.gwb.txn.data.mapper.ApprovalStatusListMapper;
import com.uob.gwb.txn.data.mapper.ResourceCompanyAndAccountsforResourcesMapper;
import com.uob.gwb.txn.data.mapper.TransactionEntityDomainMapper;
import com.uob.gwb.txn.domain.ApprovalStatusTxn;
import com.uob.gwb.txn.domain.FilterParams;
import com.uob.gwb.txn.domain.PwsTransactionCharges;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowDAO;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowV2DAO;
import com.uob.gwb.txn.utils.TransactionWorkflowUtils;
import com.uob.gwb.txn.utils.TxnUtils;
import com.uob.ufw.core.exception.ApplicationException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.uob.gwb.txn.common.GenericConstants.*;

@Slf4j
@Service("ApprovalStatusServiceTH")
public class ApprovalStatusServicePerformanceImpl extends ApprovalStatusServiceImpl implements ApprovalStatusService {

    // Number of threads for concurrent execution
    private static final int THREAD_POOL_SIZE = 4;
    // Maximum time to wait for database queries
    private static final int QUERY_TIMEOUT_SECONDS = 7;
    /**
     * Thread pool executor for managing concurrent database queries.
     * Fixed size pool is used to prevent resource exhaustion.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private TransactionWorkflowDAO transactionWorkflowDAO;

    public ApprovalStatusServicePerformanceImpl(EntitlementService entitlementService,
                                                TransactionWorkflowDAO transactionWorkflowDAO,
                                                TransactionWorkflowV2DAO transactionWorkflowV2DAO,
                                                ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper,
                                                ApprovalStatusListMapper approvalStatusListMapper,
                                                TransactionWorkFlowConfig transactionWorkFlowConfig,
                                                TransactionUtils transactionUtils,
                                                HttpUtils httpUtils,
                                                CamelService camelService,
                                                ObjectMapper objectMapper,
                                                TransactionWorkflowUtils transactionWorkflowUtils,
                                                TransactionWorkflowEnquiryCommonService commonService,
                                                TransactionEntityDomainMapper transactionEntityDomainMapper,
                                                EntitlementsResourceService entitlementsResourceService,
                                                TxnUtils txnUtils) {
        super(
                entitlementService,
                transactionWorkflowDAO,
                transactionWorkflowV2DAO,
                resourceCompanyAndAccountsforResourcesMapper,
                approvalStatusListMapper,
                transactionWorkFlowConfig,
                transactionUtils,
                httpUtils,
                camelService,
                objectMapper,
                transactionWorkflowUtils,
                commonService,
                transactionEntityDomainMapper,
                entitlementsResourceService,
                txnUtils);
        this.transactionWorkflowDAO = transactionWorkflowDAO;
    }

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

    // reimplement getSingleApprovalStatus
    @Override
    protected List<ApprovalStatusTxn> getSingleApprovalStatus(FilterParams filterParams) {
        List<ApprovalStatusTxn> approvalStatusList = new ArrayList<>();
        if (YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
                || CollectionUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
            try {
                // Start both queries simultaneously
                CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                        .supplyAsync(() -> transactionWorkflowDAO.getApprovalStatusTxnList(filterParams),
                                executorService);

                CompletableFuture<Integer> countFuture = CompletableFuture
                        .supplyAsync(() -> transactionWorkflowDAO.getApprovalStatusTxnCount(filterParams),
                                executorService);

                // Wait for both futures to complete
                approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (CollectionUtils.isNotEmpty(approvalStatusList)) {
                    int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    log.info("Total number of transaction Count for Single Transaction {}", count);

                    approvalStatusList.stream()
                            .findFirst()
                            .ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));
                }
            } catch (TimeoutException e) {
                log.error("Query timeout occurred: {}", e.getMessage(), e);
                throw new ApplicationException("Query timeout occurred");
            } catch (Exception e) {
                log.error("Failed to process getSingleApprovalStatus: {}", e.getMessage(), e);
                throw new ApplicationException("Failed to process getSingleApprovalStatus");
            }
        }
        return approvalStatusList;
    }

    // reimplement getBulkApprovalStatus
    @Override
    protected List<ApprovalStatusTxn> getBulkApprovalStatus(FilterParams filterParams) {
        if (YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
                || CollectionUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
            try {
                return YES.equalsIgnoreCase(filterParams.getIsChild())
                        ? processChildTransactions(filterParams)
                        : processParentTransactions(filterParams);
            } catch (TimeoutException e) {
                log.error("Query timeout occurred: {}", e.getMessage(), e);
                throw new ApplicationException("Query timeout occurred");
            } catch (Exception e) {
                log.error("Failed to process getBulkApprovalStatus: {}", e.getMessage(), e);
                throw new ApplicationException("Failed to process getBulkApprovalStatus");
            }
        }
        return new ArrayList<>();
    }

    protected List<ApprovalStatusTxn> processChildTransactions(FilterParams filterParams)
            throws ExecutionException, InterruptedException, TimeoutException {
        filterParams.setIsChildY(YES);

        // Start both queries simultaneously
        CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams),
                        executorService);

        CompletableFuture<Integer> countFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkApprovalStatusTxnCount(filterParams),
                        executorService);

        // Wait for both futures to complete
        List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (ObjectUtils.isEmpty(approvalStatusList)) {
            return approvalStatusList;
        }

        // Get count results and update
        int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.info("Total number of transaction Count for Bulk Transaction {}", count);

        approvalStatusList.stream()
                .findFirst()
                .ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));

        return approvalStatusList;
    }

    protected List<ApprovalStatusTxn> processParentTransactions(FilterParams filterParams)
            throws ExecutionException, InterruptedException, TimeoutException {
        filterParams.setIsChildN(YES);

        // Start both queries simultaneously
        CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams),
                        executorService);

        CompletableFuture<Integer> countFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams),
                        executorService);

        // Wait for both futures to complete
        List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (ObjectUtils.isEmpty(approvalStatusList)) {
            return approvalStatusList;
        }

        // Get count results and update
        int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.info("Total number of transaction Count for Bulk Transaction {}", count);

        var transIds = approvalStatusList.stream().map(ApprovalStatusTxn::getTransactionId).toList();

        // Start all supporting queries simultaneously
        CompletableFuture<List<PwsTransactionCharges>> chargesFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getChargesDetail(transIds),
                        executorService);

        CompletableFuture<List<ApprovalStatusTxn>> fxContractsFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getFxContracts(transIds),
                        executorService);

        // Wait for all supporting queries to complete using allOf
        CompletableFuture.allOf(chargesFuture, fxContractsFuture)
                .get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        List<PwsTransactionCharges> chargesDetail = chargesFuture.join();
        List<ApprovalStatusTxn> fxContracts = fxContractsFuture.join();

        Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
        fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
        // Group charges by transaction ID
        Map<Long, List<PwsTransactionCharges>> chargeFeeAmountMap = chargesDetail.stream()
                .collect(Collectors.toMap(charge -> charge.getTransactionId(), List::of, ListUtils::union));
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
            List<PwsTransactionCharges> charges = chargeFeeAmountMap.get(transId);
            if (charges != null && !charges.isEmpty()) {
                BigDecimal totalAmount = charges.stream()
                        .map(PwsTransactionCharges::getFeesAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String transCurrency = charges.get(0).getFeesCurrency();
                approve.setFeesCurrency(transCurrency);
                approve.setTotalFeeAmount(totalAmount);
            }
        });
        return approvalStatusList;
    }

    // reimplement getBothApprovalStatus
    @Override
    protected List<ApprovalStatusTxn> getBothApprovalStatus(FilterParams filterParams) {
        List<ApprovalStatusTxn> approvalStatusList = new ArrayList<>();
        if (YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
                || ObjectUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())
                || ObjectUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {

            final FilterParams finalFilterParams = updateFilterParamsForApprovalStatus(filterParams);

            try {
                // Start both queries simultaneously
                CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                        .supplyAsync(() -> transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(finalFilterParams),
                                executorService);

                CompletableFuture<Integer> countFuture = CompletableFuture
                        .supplyAsync(() -> transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(finalFilterParams),
                                executorService);

                // Wait for both futures to complete
                approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (CollectionUtils.isNotEmpty(approvalStatusList)) {
                    int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    log.info("Total number of transaction Count for Both Transaction {}", count);
                    var transIds = approvalStatusList.stream().map(app -> app.getTransactionId()).toList();
                    List<ApprovalStatusTxn> fxContracts = transactionWorkflowDAO.getFxContracts(transIds);
                    Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
                    fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
                    BigDecimal countValue = BigDecimal.valueOf(count);
                    approvalStatusList.stream().forEach(approve -> {
                        approve.setCount(countValue);
                        ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
                        approve.setFxType(fx.getFxType());
                        approve.setFxFlag(fx.getFxFlag());
                        approve.setBookingRefId(fx.getBookingRefId());
                        approve.setEarmarkId(fx.getEarmarkId());
                    });
                }
            } catch (TimeoutException e) {
                log.error("Query timeout occurred: {}", e.getMessage(), e);
                throw new ApplicationException("Query timeout occurred");
            } catch (Exception e) {
                log.error("Failed to process getBothApprovalStatus: {}", e.getMessage(), e);
                throw new ApplicationException("Failed to process getBothApprovalStatus");
            }
        }
        return approvalStatusList;
    }

    protected FilterParams updateFilterParamsForApprovalStatus(FilterParams filterParams) {
        String orderByWithDirection = filterParams.getSortFieldWithDirection();
        String orderBy = filterParams.getSortFieldWithDirection()
                .substring(filterParams.getSortFieldWithDirection().indexOf(DOT_SEPARATOR) + 1);
        StringBuilder sortBuilder = new StringBuilder();
        if (orderByWithDirection.contains(LOWER)) {
            sortBuilder.append(LOWER).append(OPEN_BRACE);
        }
        sortBuilder.append(BOTH_TRANS).append(DOT_SEPARATOR).append(orderBy.trim());
        filterParams.setSortFieldWithDirection(sortBuilder.toString());

        if (ObjectUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
            filterParams.setIsSingleTransaction(YES);
            filterParams.setSingleFeatureId(SINGLE_TRANSACTION);
        }
        if (ObjectUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
            filterParams.setIsBulkTransaction(YES);
        }

        return filterParams;
    }
}
```

## unit test

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.uob.gwb.txn.common.GenericConstants;
import org.apache.commons.collections.ListUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class ApprovalStatusServicePerformanceImplTest {

    private static final String YES = "Y";
    private static final String NO = "N";

    @Mock
    private TransactionWorkflowDAO transactionWorkflowDAO;
    
    @Mock
    private EntitlementService entitlementService;
    
    @Mock
    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;
    
    @Mock
    private ResourceCompanyAndAccountsforResourcesMapper resourceMapper;
    
    @Mock
    private ApprovalStatusListMapper approvalStatusListMapper;
    
    @Mock
    private TransactionWorkFlowConfig transactionWorkFlowConfig;
    
    @Mock
    private TransactionUtils transactionUtils;
    
    @Mock
    private HttpUtils httpUtils;
    
    @Mock
    private CamelService camelService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private TransactionWorkflowUtils transactionWorkflowUtils;
    
    @Mock
    private TransactionWorkflowEnquiryCommonService commonService;
    
    @Mock
    private TransactionEntityDomainMapper transactionEntityDomainMapper;
    
    @Mock
    private EntitlementsResourceService entitlementsResourceService;
    
    @Mock
    private TxnUtils txnUtils;

    @InjectMocks
    private ApprovalStatusServicePerformanceImpl approvalStatusService;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    @DisplayName("getSingleApprovalStatus - Should return empty list when not channel admin")
    void getSingleApprovalStatus_WhenNotChannelAdmin_ShouldReturnEmptyList() {
        // Arrange
        FilterParams filterParams = new FilterParams();
        filterParams.setIsChannelAdmin(NO);
        filterParams.setSingleAccountBasedOnResourceFeatureList(Collections.emptyList());

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.getSingleApprovalStatus(filterParams);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(transactionWorkflowDAO);
    }

    @Test
    @DisplayName("getSingleApprovalStatus - Should return transactions when channel admin")
    void getSingleApprovalStatus_WhenChannelAdmin_ShouldReturnTransactions() {
        // Arrange
        FilterParams filterParams = new FilterParams();
        filterParams.setIsChannelAdmin(YES);
        
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        when(transactionWorkflowDAO.getApprovalStatusTxnList(filterParams))
            .thenReturn(expectedTxns);
        when(transactionWorkflowDAO.getApprovalStatusTxnCount(filterParams))
            .thenReturn(1);

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.getSingleApprovalStatus(filterParams);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(BigDecimal.ONE, result.get(0).getCount());
        verify(transactionWorkflowDAO).getApprovalStatusTxnList(filterParams);
        verify(transactionWorkflowDAO).getApprovalStatusTxnCount(filterParams);
    }

    @Test
    @DisplayName("getSingleApprovalStatus - Should handle timeout exception")
    void getSingleApprovalStatus_WhenTimeout_ShouldThrowApplicationException() {
        // Arrange
        FilterParams filterParams = new FilterParams();
        filterParams.setIsChannelAdmin(YES);
        
        when(transactionWorkflowDAO.getApprovalStatusTxnList(filterParams))
            .thenThrow(new RuntimeException("Timeout occurred"));

        // Act & Assert
        assertThrows(ApplicationException.class, 
            () -> approvalStatusService.getSingleApprovalStatus(filterParams));
    }

    @Test
    @DisplayName("processParentTransactions - Should process all queries concurrently")
    void processParentTransactions_ShouldProcessQueriesConcurrently() throws Exception {
        // Arrange
        FilterParams filterParams = new FilterParams();
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        List<PwsTransactionCharges> expectedCharges = List.of(createMockCharges("1"));
        List<ApprovalStatusTxn> expectedFxContracts = List.of(createMockFxContract("1"));

        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams))
            .thenReturn(expectedTxns);
        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams))
            .thenReturn(1);
        when(transactionWorkflowDAO.getChargesDetail(anyList()))
            .thenReturn(expectedCharges);
        when(transactionWorkflowDAO.getFxContracts(anyList()))
            .thenReturn(expectedFxContracts);

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.processParentTransactions(filterParams);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(BigDecimal.ONE, result.get(0).getCount());
        assertNotNull(result.get(0).getFxType());
        assertNotNull(result.get(0).getTotalFeeAmount());
        
        verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnList(filterParams);
        verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnCount(filterParams);
        verify(transactionWorkflowDAO).getChargesDetail(anyList());
        verify(transactionWorkflowDAO).getFxContracts(anyList());
    }

    @Test
    @DisplayName("processChildTransactions - Should handle empty result")
    void processChildTransactions_WhenEmptyResult_ShouldReturnEmptyList() throws Exception {
        // Arrange
        FilterParams filterParams = new FilterParams();
        
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams))
            .thenReturn(Collections.emptyList());

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.processChildTransactions(filterParams);

        // Assert
        assertTrue(result.isEmpty());
        verify(transactionWorkflowDAO).getBulkApprovalStatusTxnList(filterParams);
        verify(transactionWorkflowDAO, never()).getBulkApprovalStatusTxnCount(any());
    }

    @Test
    @DisplayName("processChildTransactions - Should process successfully")
    void processChildTransactions_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        FilterParams filterParams = new FilterParams();
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams))
            .thenReturn(expectedTxns);
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(filterParams))
            .thenReturn(1);

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.processChildTransactions(filterParams);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(BigDecimal.ONE, result.get(0).getCount());
    }

    @Test
    @DisplayName("processParentTransactions - Should handle empty FX contracts")
    void processParentTransactions_WhenEmptyFxContracts_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        FilterParams filterParams = new FilterParams();
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        List<PwsTransactionCharges> expectedCharges = List.of(createMockCharges("1"));

        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams))
            .thenReturn(expectedTxns);
        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams))
            .thenReturn(1);
        when(transactionWorkflowDAO.getChargesDetail(anyList()))
            .thenReturn(expectedCharges);
        when(transactionWorkflowDAO.getFxContracts(anyList()))
            .thenReturn(Collections.emptyList());

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.processParentTransactions(filterParams);

        // Assert
        assertFalse(result.isEmpty());
        assertNull(result.get(0).getFxType());
        assertNotNull(result.get(0).getTotalFeeAmount());
    }

    @Test
    @DisplayName("getBothApprovalStatus - Should process both transactions")
    void getBothApprovalStatus_ShouldProcessBothTransactions() {
        // Arrange
        FilterParams filterParams = new FilterParams();
        filterParams.setIsChannelAdmin(YES);
        filterParams.setSortFieldWithDirection("field.asc");
        
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        List<ApprovalStatusTxn> expectedFxContracts = List.of(createMockFxContract("1"));

        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(any()))
            .thenReturn(expectedTxns);
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(any()))
            .thenReturn(1);
        when(transactionWorkflowDAO.getFxContracts(anyList()))
            .thenReturn(expectedFxContracts);

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.getBothApprovalStatus(filterParams);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(BigDecimal.ONE, result.get(0).getCount());
        assertNotNull(result.get(0).getFxType());
    }

    @Test
    @DisplayName("cleanup - Should shutdown executor service gracefully")
    void cleanup_ShouldShutdownExecutorServiceGracefully() throws InterruptedException {
        // Act
        approvalStatusService.cleanup();

        // Wait briefly to allow shutdown to complete
        Thread.sleep(1000);
    }

    @Test
    @DisplayName("updateFilterParamsForApprovalStatus - Should handle all scenarios")
    void updateFilterParamsForApprovalStatus_ShouldHandleAllScenarios() {
        // Arrange
        FilterParams filterParams = new FilterParams();
        filterParams.setSortFieldWithDirection("LOWER(field.asc)");
        filterParams.setSingleAccountBasedOnResourceFeatureList(List.of("feature1"));
        filterParams.setBulkAccountBasedOnResourceFeatureList(List.of("feature2"));

        // Act
        FilterParams result = approvalStatusService.updateFilterParamsForApprovalStatus(filterParams);

        // Assert
        assertTrue(result.getSortFieldWithDirection().startsWith("LOWER("));
        assertEquals(YES, result.getIsSingleTransaction());
        assertEquals(YES, result.getIsBulkTransaction());
        assertEquals("SINGLE_TRANSACTION", result.getSingleFeatureId());
    }

    @Test
    @DisplayName("getBulkApprovalStatus - Should handle all scenarios")
    void getBulkApprovalStatus_ShouldHandleAllScenarios() {
        // Arrange
        FilterParams filterParams = new FilterParams();
        filterParams.setIsChannelAdmin(YES);
        filterParams.setIsChild(YES);
        
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
            .thenReturn(expectedTxns);
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(any()))
            .thenReturn(1);

        // Act
        List<ApprovalStatusTxn> result = approvalStatusService.getBulkApprovalStatus(filterParams);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(BigDecimal.ONE, result.get(0).getCount());
    }

    // Helper methods
    private ApprovalStatusTxn createMockTransaction(String id) {
        ApprovalStatusTxn txn = new ApprovalStatusTxn();
        txn.setTransactionId(id);
        return txn;
    }

    private ApprovalStatusTxn createMockFxContract(String id) {
        ApprovalStatusTxn txn = new ApprovalStatusTxn();
        txn.setTransactionId(id);
        txn.setFxType("SPOT");
        txn.setFxFlag(YES);
        txn.setBookingRefId("BOOK1");
        txn.setEarmarkId("EARK1");
        return txn;
    }

    private PwsTransactionCharges createMockCharges(String id) {
        PwsTransactionCharges charges = new PwsTransactionCharges();
        charges.setTransactionId(Long.valueOf(id));
        charges.setFeesAmount(BigDecimal.TEN);
        charges.setFeesCurrency("USD");
        return charges;
    }
}
```

## code

```java
package com.uob.gwb.txn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.transaction.common.domain.v1.CompanyAndAccountsForUserResourceFeaturesResp;
import com.uob.gwb.transaction.common.service.CamelService;
import com.uob.gwb.transaction.common.service.EntitlementService;
import com.uob.gwb.transaction.common.util.HttpUtils;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.txn.config.TransactionWorkFlowConfig;
import com.uob.gwb.txn.data.mapper.ApprovalStatusListMapper;
import com.uob.gwb.txn.data.mapper.ResourceCompanyAndAccountsforResourcesMapper;
import com.uob.gwb.txn.data.mapper.TransactionEntityDomainMapper;
import com.uob.gwb.txn.domain.ApprovalStatusTxn;
import com.uob.gwb.txn.domain.FilterParams;
import com.uob.gwb.txn.model.ApprovalStatusLookUpResp;
import com.uob.gwb.txn.model.TransactionsLookUpReq;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowDAO;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowV2DAO;
import com.uob.gwb.txn.utils.TransactionWorkflowUtils;
import com.uob.gwb.txn.utils.TxnUtils;
import com.uob.ufw.core.exception.ApplicationException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.uob.gwb.txn.common.GenericConstants.*;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@Service("ApprovalStatusV2ServiceTH")
public class ApprovalStatusV2ServicePerformanceImpl extends ApprovalStatusV2ServiceImpl implements ApprovalStatusV2Service {

    // Number of threads for concurrent execution
    private static final int THREAD_POOL_SIZE = 4;
    // Maximum time to wait for database queries
    private static final int QUERY_TIMEOUT_SECONDS = 7;
    /**
     * Thread pool executor for managing concurrent database queries.
     * Fixed size pool is used to prevent resource exhaustion.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;

    public ApprovalStatusV2ServicePerformanceImpl(EntitlementService entitlementService,
                                                  TransactionWorkflowDAO transactionWorkflowDAO,
                                                  TransactionWorkflowV2DAO transactionWorkflowV2DAO,
                                                  ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper,
                                                  ApprovalStatusListMapper approvalStatusListMapper,
                                                  TransactionWorkFlowConfig transactionWorkFlowConfig,
                                                  TransactionUtils transactionUtils,
                                                  HttpUtils httpUtils,
                                                  CamelService camelService,
                                                  ObjectMapper objectMapper,
                                                  TransactionWorkflowUtils transactionWorkflowUtils,
                                                  TransactionWorkflowEnquiryCommonService commonService,
                                                  TransactionEntityDomainMapper transactionEntityDomainMapper,
                                                  EntitlementsResourceService entitlementsResourceService,
                                                  TxnUtils txnUtils) {
        super(
                entitlementService,
                transactionWorkflowDAO,
                transactionWorkflowV2DAO,
                resourceCompanyAndAccountsforResourcesMapper,
                approvalStatusListMapper,
                transactionWorkFlowConfig,
                transactionUtils,
                httpUtils,
                camelService,
                objectMapper,
                transactionWorkflowUtils,
                commonService,
                transactionEntityDomainMapper,
                entitlementsResourceService,
                txnUtils);
        this.transactionWorkflowV2DAO = transactionWorkflowV2DAO;
    }

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

    // reimplement setApprovalStatusTxnList
    @Override
    protected ApprovalStatusLookUpResp setApprovalStatusTxnList(String transactionType,
                                                                FilterParams filterParams,
                                                                TransactionsLookUpReq request,
                                                                CompanyAndAccountsForUserResourceFeaturesResp response,
                                                                String isChild) {
        List<ApprovalStatusTxn> approvalStatusTxnList = new ArrayList<>();
        if (SINGLE.equalsIgnoreCase(transactionType)) {
            approvalStatusTxnList.addAll(transactionWorkflowV2DAO.getApprovalStatusTxnList(filterParams));
        } else if (BULK.equalsIgnoreCase(transactionType)) {
            approvalStatusTxnList.addAll(super.getBulkApprovalStatus(filterParams));
        } else if (BOTH.equalsIgnoreCase(transactionType)) {
            // reimplement
            final FilterParams finalFilterParams = updateFilterParamsForApprovalStatus(request, filterParams);
            try {
                // Start both queries simultaneously
                CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                        .supplyAsync(() -> transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(finalFilterParams, PAYEE_TAX),
                                executorService);

                CompletableFuture<Integer> countFuture = CompletableFuture
                        .supplyAsync(() -> transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(finalFilterParams),
                                executorService);

                // Wait for both futures to complete
                approvalStatusTxnList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (CollectionUtils.isNotEmpty(approvalStatusTxnList)) {
                    int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    log.info("Total number of transaction Count for Both Transaction {}", count);
                    var transIds = approvalStatusTxnList.stream().map(ApprovalStatusTxn::getTransactionId).toList();
                    List<ApprovalStatusTxn> fxContracts = transactionWorkflowV2DAO.getFxContracts(transIds);
                    Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
                    fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
                    BigDecimal countValue = BigDecimal.valueOf(count);
                    approvalStatusTxnList.stream().forEach(approve -> {
                        approve.setCount(countValue);
                        ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
                        approve.setFxType(fx.getFxType());
                        approve.setFxFlag(fx.getFxFlag());
                        approve.setBookingRefId(fx.getBookingRefId());
                        approve.setEarmarkId(fx.getEarmarkId());
                    });
                }
            } catch (TimeoutException e) {
                log.error("Query timeout occurred: {}", e.getMessage(), e);
                throw new ApplicationException("Query timeout occurred");
            } catch (Exception e) {
                log.error("Failed to process getBothApprovalStatus: {}", e.getMessage(), e);
                throw new ApplicationException("Failed to process getBothApprovalStatus");
            }
        }


        if (CollectionUtils.isNotEmpty(approvalStatusTxnList)) {
            return setApprovalStatusLookUpResp(approvalStatusTxnList, response, transactionType,
                    isChild, request);
        }

        return null;

    }

    protected FilterParams updateFilterParamsForApprovalStatus(TransactionsLookUpReq request, FilterParams filterParams) {
        filterParams.setLimit(String.valueOf(request.getAdditionalProperties().get(LIMIT)));
        String orderBy = filterParams.getSortFieldWithDirection()
                .substring(filterParams.getSortFieldWithDirection().indexOf(DOT_SEPARATOR) + 1);
        String orderByWithDirection = filterParams.getSortFieldWithDirection();

        StringBuilder sortBuilder = new StringBuilder();

        if (orderByWithDirection.contains(LOWER)) {
            sortBuilder.append(LOWER).append(OPEN_BRACE);
        }

        sortBuilder.append(BOTH_TRANS).append(DOT_SEPARATOR).append(orderBy.trim());
        filterParams.setSortFieldWithDirection(sortBuilder.toString());

        if (isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
            filterParams.setSingleFeatureId(SINGLE_TRANSACTION);
            filterParams.setIsSingleTransaction(YES);
        }

        if (isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
            filterParams.setIsBulkTransaction(YES);
        }
        return filterParams;
    }

}
```

## unit test

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@ExtendWith(MockitoExtension.class)
class ApprovalStatusV2ServicePerformanceImplTest {

    private static final String YES = "Y";
    private static final String SINGLE = "SINGLE";
    private static final String BULK = "BULK";
    private static final String BOTH = "BOTH";
    private static final String PAYEE_TAX = "PAYEE_TAX";
    private static final String LIMIT = "limit";

    @Mock
    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;
    
    @Mock
    private EntitlementService entitlementService;
    
    @Mock
    private TransactionWorkflowDAO transactionWorkflowDAO;
    
    @Mock
    private ResourceCompanyAndAccountsforResourcesMapper resourceMapper;
    
    @Mock
    private ApprovalStatusListMapper approvalStatusListMapper;
    
    @Mock
    private TransactionWorkFlowConfig transactionWorkFlowConfig;
    
    @Mock
    private TransactionUtils transactionUtils;
    
    @Mock
    private HttpUtils httpUtils;
    
    @Mock
    private CamelService camelService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private TransactionWorkflowUtils transactionWorkflowUtils;
    
    @Mock
    private TransactionWorkflowEnquiryCommonService commonService;
    
    @Mock
    private TransactionEntityDomainMapper transactionEntityDomainMapper;
    
    @Mock
    private EntitlementsResourceService entitlementsResourceService;
    
    @Mock
    private TxnUtils txnUtils;

    @InjectMocks
    private ApprovalStatusV2ServicePerformanceImpl approvalStatusService;

    private TransactionsLookUpReq request;
    private FilterParams filterParams;
    private CompanyAndAccountsForUserResourceFeaturesResp response;

    @BeforeEach
    void setUp() {
        request = new TransactionsLookUpReq();
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put(LIMIT, "10");
        request.setAdditionalProperties(additionalProperties);

        filterParams = new FilterParams();
        filterParams.setSortFieldWithDirection("field.asc");

        response = new CompanyAndAccountsForUserResourceFeaturesResp();
    }

    @Test
    @DisplayName("setApprovalStatusTxnList - Should handle SINGLE transaction type")
    void setApprovalStatusTxnList_WhenSingle_ShouldProcessSuccessfully() {
        // Arrange
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
            .thenReturn(expectedTxns);
        when(approvalStatusListMapper.mapToApprovalStatusLookUpResp(any(), any(), any(), any(), any()))
            .thenReturn(new ApprovalStatusLookUpResp());

        // Act
        ApprovalStatusLookUpResp result = approvalStatusService.setApprovalStatusTxnList(
            SINGLE, filterParams, request, response, YES);

        // Assert
        assertNotNull(result);
        verify(transactionWorkflowV2DAO).getApprovalStatusTxnList(any());
    }

    @Test
    @DisplayName("setApprovalStatusTxnList - Should handle BOTH transaction type")
    void setApprovalStatusTxnList_WhenBoth_ShouldProcessSuccessfully() {
        // Arrange
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        List<ApprovalStatusTxn> expectedFxContracts = List.of(createMockFxContract("1"));

        when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
            .thenReturn(expectedTxns);
        when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
            .thenReturn(1);
        when(transactionWorkflowV2DAO.getFxContracts(anyList()))
            .thenReturn(expectedFxContracts);
        when(approvalStatusListMapper.mapToApprovalStatusLookUpResp(any(), any(), any(), any(), any()))
            .thenReturn(new ApprovalStatusLookUpResp());

        // Act
        ApprovalStatusLookUpResp result = approvalStatusService.setApprovalStatusTxnList(
            BOTH, filterParams, request, response, YES);

        // Assert
        assertNotNull(result);
        verify(transactionWorkflowV2DAO).getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX));
        verify(transactionWorkflowV2DAO).getV2BulkBothApprovalStatusTxnCount(any());
        verify(transactionWorkflowV2DAO).getFxContracts(anyList());
    }

    @Test
    @DisplayName("setApprovalStatusTxnList - Should handle empty results")
    void setApprovalStatusTxnList_WhenEmptyResults_ShouldReturnNull() {
        // Arrange
        when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
            .thenReturn(Collections.emptyList());

        // Act
        ApprovalStatusLookUpResp result = approvalStatusService.setApprovalStatusTxnList(
            BOTH, filterParams, request, response, YES);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("setApprovalStatusTxnList - Should handle timeout exception")
    void setApprovalStatusTxnList_WhenTimeout_ShouldThrowApplicationException() {
        // Arrange
        when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
            .thenAnswer(invocation -> {
                Thread.sleep(8000); // Simulate timeout
                return null;
            });

        // Act & Assert
        assertThrows(ApplicationException.class, () -> 
            approvalStatusService.setApprovalStatusTxnList(BOTH, filterParams, request, response, YES));
    }

    @Test
    @DisplayName("updateFilterParamsForApprovalStatus - Should handle LOWER in sort field")
    void updateFilterParamsForApprovalStatus_WhenLowerInSortField_ShouldUpdateCorrectly() {
        // Arrange
        filterParams.setSortFieldWithDirection("LOWER(field.asc)");
        filterParams.setSingleAccountBasedOnResourceFeatureList(List.of("feature1"));
        filterParams.setBulkAccountBasedOnResourceFeatureList(List.of("feature2"));

        // Act
        FilterParams result = approvalStatusService.updateFilterParamsForApprovalStatus(request, filterParams);

        // Assert
        assertTrue(result.getSortFieldWithDirection().startsWith("LOWER("));
        assertEquals(YES, result.getIsSingleTransaction());
        assertEquals(YES, result.getIsBulkTransaction());
        assertEquals("SINGLE_TRANSACTION", result.getSingleFeatureId());
    }

    @Test
    @DisplayName("updateFilterParamsForApprovalStatus - Should handle sort field without LOWER")
    void updateFilterParamsForApprovalStatus_WhenNoLower_ShouldUpdateCorrectly() {
        // Arrange
        filterParams.setSortFieldWithDirection("field.desc");
        filterParams.setSingleAccountBasedOnResourceFeatureList(null);
        filterParams.setBulkAccountBasedOnResourceFeatureList(null);

        // Act
        FilterParams result = approvalStatusService.updateFilterParamsForApprovalStatus(request, filterParams);

        // Assert
        assertFalse(result.getSortFieldWithDirection().contains("LOWER"));
        assertNull(result.getIsSingleTransaction());
        assertNull(result.getIsBulkTransaction());
    }

    @Test
    @DisplayName("cleanup - Should shutdown executor service gracefully")
    void cleanup_ShouldShutdownExecutorServiceGracefully() throws InterruptedException {
        // Act
        approvalStatusService.cleanup();

        // Wait briefly to allow shutdown to complete
        Thread.sleep(1000);
    }

    @Test
    @DisplayName("setApprovalStatusTxnList - Should handle BULK transaction type")
    void setApprovalStatusTxnList_WhenBulk_ShouldProcessSuccessfully() {
        // Arrange
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
            .thenReturn(expectedTxns);
        when(approvalStatusListMapper.mapToApprovalStatusLookUpResp(any(), any(), any(), any(), any()))
            .thenReturn(new ApprovalStatusLookUpResp());

        // Act
        ApprovalStatusLookUpResp result = approvalStatusService.setApprovalStatusTxnList(
            BULK, filterParams, request, response, YES);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("setApprovalStatusTxnList - Should handle empty FX contracts")
    void setApprovalStatusTxnList_WhenEmptyFxContracts_ShouldProcessSuccessfully() {
        // Arrange
        List<ApprovalStatusTxn> expectedTxns = List.of(createMockTransaction("1"));
        when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq(PAYEE_TAX)))
            .thenReturn(expectedTxns);
        when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
            .thenReturn(1);
        when(transactionWorkflowV2DAO.getFxContracts(anyList()))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
            approvalStatusService.setApprovalStatusTxnList(BOTH, filterParams, request, response, YES));
    }

    // Helper methods
    private ApprovalStatusTxn createMockTransaction(String id) {
        ApprovalStatusTxn txn = new ApprovalStatusTxn();
        txn.setTransactionId(id);
        return txn;
    }

    private ApprovalStatusTxn createMockFxContract(String id) {
        ApprovalStatusTxn txn = new ApprovalStatusTxn();
        txn.setTransactionId(id);
        txn.setFxType("SPOT");
        txn.setFxFlag(YES);
        txn.setBookingRefId("BOOK1");
        txn.setEarmarkId("EARK1");
        return txn;
    }
}
```
