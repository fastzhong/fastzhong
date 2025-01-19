```java
import static com.uob.gwb.txn.common.GenericConstants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.transaction.common.domain.*;
import com.uob.gwb.transaction.common.domain.Feature;
import com.uob.gwb.transaction.common.model.*;
import com.uob.gwb.transaction.common.model.AccountResource;
import com.uob.gwb.transaction.common.model.Transactions;
import com.uob.gwb.transaction.common.model.ValidationErrors;
import com.uob.gwb.transaction.common.model.ueqs.ResourceDetails;
import com.uob.gwb.transaction.common.model.ueqs.Subscription;
import com.uob.gwb.transaction.common.service.CamelService;
import com.uob.gwb.transaction.common.service.EntitlementService;
import com.uob.gwb.transaction.common.util.HttpUtils;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.txn.common.GenericConstants;
import com.uob.gwb.txn.config.TransactionWorkFlowConfig;
import com.uob.gwb.txn.data.mapper.ApprovalStatusListMapper;
import com.uob.gwb.txn.data.mapper.ResourceCompanyAndAccountsforResourcesMapper;
import com.uob.gwb.txn.data.mapper.TransactionEntityDomainMapper;
import com.uob.gwb.txn.domain.*;
import com.uob.gwb.txn.domain.Features;
import com.uob.gwb.txn.model.*;
import com.uob.gwb.txn.model.ConfigDetail;
import com.uob.gwb.txn.model.GenericLookUp;
import com.uob.gwb.txn.model.Resource;
import com.uob.gwb.txn.model.ViewCounterParty;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowDAO;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowV2DAO;
import com.uob.gwb.txn.utils.TransactionWorkflowUtils;
import com.uob.gwb.txn.utils.TxnUtils;
import com.uob.ufw.core.exception.ApplicationException;
import com.uob.ufw.core.model.ApiDataModel;
import com.uob.ufw.core.util.JsonUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ApprovalStatusService")
@AllArgsConstructor
public class ApprovalStatusServiceImpl implements ApprovalStatusService {

    private EntitlementService entitlementService;

    private TransactionWorkflowDAO transactionWorkflowDAO;

    private ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper;

    private ApprovalStatusListMapper approvalStatusListMapper;

    private TransactionWorkFlowConfig transactionWorkFlowConfig;

    private TransactionUtils transactionUtils;
    private HttpUtils httpUtils;
    private CamelService camelService;
    private ObjectMapper objectMapper;
    private TransactionWorkflowUtils transactionWorkflowUtils;
    private TransactionWorkflowEnquiryCommonService commonService;
    private TransactionEntityDomainMapper transactionEntityDomainMapper;
    private EntitlementsResourceService entitlementsResourceService;
    private TxnUtils txnUtils;


    private ApprovalStatusLookUpResp getApprovalStatusTxnList(String transactionType, FilterParams filterParams,
            TransactionsLookUpReq request, CompanyAndAccountsForResourceFeaturesResp response) {
        List<ApprovalStatusTxn> approvalStatusTxnList = new ArrayList<>();

        if (SINGLE.equalsIgnoreCase(transactionType)) {
            log.info("getApprovalStatusTxnList SINGLE");
            approvalStatusTxnList.addAll(getSingleApprovalStatus(filterParams));
        } else if (BULK.equalsIgnoreCase(transactionType)) {
            log.info("getApprovalStatusTxnList BULK");
            approvalStatusTxnList.addAll(getBulkApprovalStatus(filterParams));
        } else if (BOTH.equalsIgnoreCase(transactionType)) {
            log.info("getApprovalStatusTxnList BOTH");
            approvalStatusTxnList.addAll(getBothApprovalStatus(filterParams));
        }
        log.debug("ApprovalStatusServiceImpl::approvalStatusTxnList:: size{} ", approvalStatusTxnList.size());

        return setApprovalStatusLookUpResp(approvalStatusTxnList, response, transactionType, request);

    }


    protected List<ApprovalStatusTxn> getBulkApprovalStatus(FilterParams filterParams) {
        List<ApprovalStatusTxn> approvalStatusList = new ArrayList<>();

        var isChild = filterParams.getIsChild();
        if (YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
                || CollectionUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
            if (YES.equalsIgnoreCase(isChild)) {

                filterParams.setIsChildY(YES);
                approvalStatusList = transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams);
                if (ObjectUtils.isNotEmpty(approvalStatusList)) {
                    int count = transactionWorkflowDAO.getBulkApprovalStatusTxnCount(filterParams);
                    log.info("Total number of transaction Count for Bulk Transaction {}", count);
                    approvalStatusList.stream().findFirst().ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));
                }

            } else {
                filterParams.setIsChildN(YES);
                approvalStatusList = transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams);

                if (ObjectUtils.isNotEmpty(approvalStatusList)) {
                    int count = transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams);
                    var transIds = approvalStatusList.stream().map(app -> app.getTransactionId()).toList();
                    List<PwsTransactionCharges> chargesDetail = transactionWorkflowDAO.getChargesDetail(transIds);
                    List<ApprovalStatusTxn> fxContracts = transactionWorkflowDAO.getFxContracts(transIds);
                    Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
                    fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
                    Map<Long, List<PwsTransactionCharges>> chargeFeeAmountMap = chargesDetail.stream()
                            .collect(Collectors.toMap(charge -> charge.getTransactionId(), List::of, ListUtils::union));
                    for (var trans : chargeFeeAmountMap.entrySet()) {
                        var transId = trans.getKey().toString();
                        var totalAmount = trans.getValue()
                                .stream()
                                .map(PwsTransactionCharges::getFeesAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        var pwsFirstCharges = trans.getValue().stream().findFirst().orElse(new PwsTransactionCharges());
                        var transCurrency = pwsFirstCharges.getFeesCurrency();
                        approvalStatusList.stream()
                                .filter(approve -> approve.getTransactionId().equalsIgnoreCase(transId))
                                .forEach(approv -> {
                                    approv.setFeesCurrency(transCurrency);
                                    approv.setTotalFeeAmount(totalAmount);
                                    ApprovalStatusTxn fx = fxContractMap.get(transId);
                                    approv.setFxType(fx.getFxType());
                                    approv.setFxFlag(fx.getFxFlag());
                                    approv.setBookingRefId(fx.getBookingRefId());
                                    approv.setEarmarkId(fx.getEarmarkId());
                                });
                    }
                    approvalStatusList.stream().findFirst().ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));
                }
            }
        }
        return approvalStatusList;
    }

    protected List<ApprovalStatusTxn> getSingleApprovalStatus(FilterParams filterParams) {
        List<ApprovalStatusTxn> approvalStatusList = new ArrayList<>();
        if (YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
                || CollectionUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
            approvalStatusList = transactionWorkflowDAO.getApprovalStatusTxnList(filterParams);
            if (ObjectUtils.isNotEmpty(approvalStatusList)) {
                int count = transactionWorkflowDAO.getApprovalStatusTxnCount(filterParams);
                log.info("Total number of transaction Count for Single Transaction {}", count);
                approvalStatusList.stream().findFirst().ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));
            }
        }
        return approvalStatusList;
    }

    protected List<ApprovalStatusTxn> getBothApprovalStatus(FilterParams filterParams) {

        List<ApprovalStatusTxn> approvalStatusList = new ArrayList<>();
        if (YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
                || ObjectUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())
                || ObjectUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {

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
            approvalStatusList.addAll(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(filterParams));
            if (ObjectUtils.isNotEmpty(approvalStatusList)) {
                int count = transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(filterParams);
                log.info("Total number of transaction Count for Both Transaction {}", count);
                var transIds = approvalStatusList.stream().map(app -> app.getTransactionId()).toList();
                List<ApprovalStatusTxn> fxContracts = transactionWorkflowDAO.getFxContracts(transIds);
                Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
                fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
                approvalStatusList.stream().forEach(approve -> {
                    approve.setCount(BigDecimal.valueOf(count));
                    ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
                    approve.setFxType(fx.getFxType());
                    approve.setFxFlag(fx.getFxFlag());
                    approve.setBookingRefId(fx.getBookingRefId());
                    approve.setEarmarkId(fx.getEarmarkId());
                });
            }
        }

        return approvalStatusList;
    }

}
```

```java

package com.uob.gwb.txn.service;

import static com.uob.gwb.txn.common.GenericConstants.*;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.transaction.common.domain.*;
import com.uob.gwb.transaction.common.domain.v1.CompanyAccounts;
import com.uob.gwb.transaction.common.domain.v1.CompanyAndAccountsForResource;
import com.uob.gwb.transaction.common.domain.v1.CompanyAndAccountsForUserResourceFeaturesResp;
import com.uob.gwb.transaction.common.domain.v1.ResourceFeatureAndCompanies;
import com.uob.gwb.transaction.common.model.BeneDetails;
import com.uob.gwb.transaction.common.service.CamelService;
import com.uob.gwb.transaction.common.service.EntitlementService;
import com.uob.gwb.transaction.common.util.HttpUtils;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.txn.common.GenericConstants;
import com.uob.gwb.txn.config.TransactionWorkFlowConfig;
import com.uob.gwb.txn.data.mapper.ApprovalStatusListMapper;
import com.uob.gwb.txn.data.mapper.ResourceCompanyAndAccountsforResourcesMapper;
import com.uob.gwb.txn.data.mapper.TransactionEntityDomainMapper;
import com.uob.gwb.txn.domain.ApprovalStatusTxn;
import com.uob.gwb.txn.domain.FilterParams;
import com.uob.gwb.txn.model.*;
import com.uob.gwb.txn.model.GenericLookUp;
import com.uob.gwb.txn.model.Resource;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowDAO;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowV2DAO;
import com.uob.gwb.txn.utils.TransactionWorkflowUtils;
import com.uob.gwb.txn.utils.TxnUtils;
import com.uob.ufw.core.exception.ApplicationException;
import com.uob.ufw.core.util.JsonUtil;
import java.math.BigDecimal;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ApprovalStatusV2Service")
public class ApprovalStatusV2ServiceImpl extends ApprovalStatusServiceImpl implements ApprovalStatusV2Service {

    private EntitlementService entitlementService;
    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;
    private ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper;
    private ApprovalStatusListMapper approvalStatusListMapper;
    private TransactionUtils transactionUtils;
    private ObjectMapper objectMapper;
    private TransactionWorkflowUtils transactionWorkflowUtils;
    private TransactionEntityDomainMapper transactionEntityDomainMapper;

    public ApprovalStatusV2ServiceImpl(EntitlementService entitlementService,
            TransactionWorkflowDAO transactionWorkflowDAO, TransactionWorkflowV2DAO transactionWorkflowV2DAO,
            ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper,
            ApprovalStatusListMapper approvalStatusListMapper, TransactionWorkFlowConfig transactionWorkFlowConfig,
            TransactionUtils transactionUtils, HttpUtils httpUtils, CamelService camelService,
            ObjectMapper objectMapper, TransactionWorkflowUtils transactionWorkflowUtils,
            TransactionWorkflowEnquiryCommonService commonService,
            TransactionEntityDomainMapper transactionEntityDomainMapper,
            EntitlementsResourceService entitlementsResourceService, TxnUtils txnUtils) {
        super(entitlementService, transactionWorkflowDAO, transactionWorkflowV2DAO,
                resourceCompanyAndAccountsforResourcesMapper, approvalStatusListMapper, transactionWorkFlowConfig,
                transactionUtils, httpUtils, camelService, objectMapper, transactionWorkflowUtils, commonService,
                transactionEntityDomainMapper, entitlementsResourceService, txnUtils);
        this.entitlementService = entitlementService;
        this.transactionWorkflowV2DAO = transactionWorkflowV2DAO;
        this.resourceCompanyAndAccountsforResourcesMapper = resourceCompanyAndAccountsforResourcesMapper;
        this.approvalStatusListMapper = approvalStatusListMapper;
        this.transactionUtils = transactionUtils;
        this.objectMapper = objectMapper;
        this.transactionWorkflowUtils = transactionWorkflowUtils;
        this.transactionEntityDomainMapper = transactionEntityDomainMapper;
    }

    private ApprovalStatusLookUpResp setApprovalStatusTxnList(String transactionType, FilterParams filterParams,
            TransactionsLookUpReq request, CompanyAndAccountsForUserResourceFeaturesResp response, String isChild) {
        ApprovalStatusLookUpResp approvalStatusLookUpResp = null;
        List<ApprovalStatusTxn> approvalStatusTxnList = new ArrayList<>();
        if (SINGLE.equalsIgnoreCase(transactionType)) {
            approvalStatusTxnList.addAll(transactionWorkflowV2DAO.getApprovalStatusTxnList(filterParams));
        } else if (BULK.equalsIgnoreCase(transactionType)) {
            approvalStatusTxnList.addAll(super.getBulkApprovalStatus(filterParams));
        } else if (BOTH.equalsIgnoreCase(transactionType)) {

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

            approvalStatusTxnList
                    .addAll(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(filterParams, PAYEE_TAX));
            if (isNotEmpty(approvalStatusTxnList)) {
                List<String> transIds = approvalStatusTxnList.stream().map(app -> app.getTransactionId()).toList();
                int count = transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(filterParams);
                Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
                List<ApprovalStatusTxn> fxContracts = transactionWorkflowV2DAO.getFxContracts(transIds);
                fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
                approvalStatusTxnList.stream().forEach(approve -> {
                    ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
                    approve.setFxType(fx.getFxType());
                    approve.setFxFlag(fx.getFxFlag());
                    approve.setEarmarkId(fx.getEarmarkId());
                    approve.setBookingRefId(fx.getBookingRefId());
                    approve.setCount(BigDecimal.valueOf(count));
                });
            }
        }
        log.debug("ApprovalStatusV2ServiceImpl::approvalStatusTxnList:: {} ", approvalStatusTxnList);

        if (isNotEmpty(approvalStatusTxnList)) {
            approvalStatusLookUpResp = setApprovalStatusLookUpResp(approvalStatusTxnList, response, transactionType,
                    isChild, request);
        }

        return approvalStatusLookUpResp;
    }

}
```
