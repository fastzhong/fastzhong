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
```
