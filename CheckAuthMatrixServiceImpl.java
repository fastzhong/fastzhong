 /*
 * Copyright (c) United Overseas Bank Limited Co.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * United Overseas Bank Limited Co. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * United Overseas Bank Limited Co.
 */
package com.uob.gwb.tws.service;

import static com.uob.gwb.tws.common.GenericConstants.*;
import static com.uob.gwb.tws.common.TransactionPredicate.isBulkOnlineOrBulkFileUpload;
import static com.uob.gwb.tws.common.TransactionPredicate.isBulkOnlineOrBulkFileUploadPayroll;
import static com.uob.gwb.tws.common.TransactionPredicate.isValidPendinReworkTransaction;

import com.uob.gwb.transaction.common.config.EntitlementsFeaturesConfiguration;
import com.uob.gwb.tws.common.ErrorCodeConstants;
import com.uob.gwb.tws.common.GenericConstants;
import com.uob.gwb.tws.common.GenericException;
import com.uob.gwb.tws.common.TwsUtils;
import com.uob.gwb.tws.config.SubmitTransactionConfiguration;
import com.uob.gwb.tws.domain.GebUserIdGroupId;
import com.uob.gwb.tws.domain.RoleActionInfo;
import com.uob.gwb.tws.domain.TransactionInfo;
import com.uob.gwb.tws.model.CheckAuthMatrixReq;
import com.uob.gwb.tws.model.CheckAuthMatrixResp;
import com.uob.gwb.tws.model.Transactions;
import com.uob.gwb.tws.model.TransactionsSchema;
import com.uob.trx.auth.matrix.MatrixContext;
import com.uob.trx.auth.matrix.MatrixProcessingResult;
import com.uob.trx.auth.matrix.domain.entitlement.Role;
import com.uob.trx.auth.matrix.exception.NoRulesMatchedException;
import com.uob.trx.auth.matrix.service.authz.AuthMatrixService;
import com.uob.ufw.core.exception.ApplicationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("CheckAuthMatrixService")
@Slf4j
@RequiredArgsConstructor
public class CheckAuthMatrixServiceImpl implements CheckAuthMatrixService {
    @Value("${country}")
    private String country;
    private final TwsUtils twsUtils;

    private final AuthMatrixService authMatrixService;

    private final InsertAuditTrailService insertAuditTrailService;

    private final SubmitTransactionConfiguration submitTransactionConfiguration;

    private final EntitlementsFeaturesConfiguration entitlementsFeaturesConfiguration;

    @Override
    public CheckAuthMatrixResp checkAuthMatrixForActions(CheckAuthMatrixReq checkAuthMatrixReq) {
        log.info("CheckAuthMatrixServiceImpl >> method - checkAuthMatrixForActions >> Start"
                + checkAuthMatrixReq.toString());
        // Step1 : Scan the request attributes
        Long userId = twsUtils.getDecryptedUserId(checkAuthMatrixReq.getUserId());
        String bankEntityId = twsUtils.getDecryptedBankEntityId(checkAuthMatrixReq.getAdditionalPropValue(ENTITY_TEXT));

        Map<String, String> transactionIdMap = new HashMap<>();

        List<String> encryTransactionIds = validateMaxThresholdTxnsLimit(checkAuthMatrixReq);

        List<String> transactionIds = twsUtils.getDecryptedTransactionIds(encryTransactionIds, transactionIdMap);

        Boolean isMultiTransaction = false;
        if (encryTransactionIds.size() > 1) {
            isMultiTransaction = true;
        }

        List<String> changeTokens = new ArrayList<>();
        List<String> customerStatusList = new ArrayList<>();
        customerStatusList.add(PENDING_VERIFICATION);
        customerStatusList.add(PENDING);
        customerStatusList.add(NEW);
        customerStatusList.add(PENDING_SUBMIT);
        customerStatusList.add(PENDING_AUTHORIZATION);
        customerStatusList.add(PENDING_SEND);
        customerStatusList.add(PENDING_REWORK);
        customerStatusList.add(DRAFT);
        customerStatusList.add(REJECTED);
        customerStatusList.add(REJECTED_BY_BANK_ACTION);
        customerStatusList.add(SUCCESSFUL_STATUS);
        customerStatusList.add(UNSUCCESSFUL_STATUS);

        // Step2: Call the DB to get transaction details corresponding to the txnId
        List<TransactionInfo> transactionInfos = submitTransactionConfiguration.getTransactionInfos(bankEntityId,
                changeTokens, transactionIds, customerStatusList);

        // Add other if conditions for RPP-RTP or others apart from RppMandate for
        // future stories.
        // Step2a: Merge all transaction details from all resources
        List<TransactionInfo> transactionInfoList = new ArrayList<>();
        transactionInfoList.addAll(transactionInfos);

        // Step2b: Collect list of all below unique data from transaction tables
        // response
        Set<String> cewGroupIds = new HashSet<>();
        Set<String> cewCompanyIds = new HashSet<>();
        Set<String> gebCompanyIds = new HashSet<>();
        Set<String> accountNumbers = new HashSet<>();
        Set<String> resourceIds = new HashSet<>();
        Set<String> featureIds = new HashSet<>();

        Map<String, TransactionInfo> txnIdRequestMap = new HashMap<>();
        List<RoleActionInfo> roleActionInfoList = new ArrayList<>();
        // Step 3: get Roles/actions from DB for a given combination of Resource,
        // Feature , Groups ,
        // companies
        submitTransactionConfiguration.extractRoles(bankEntityId, userId, transactionInfoList, cewGroupIds,
                cewCompanyIds, accountNumbers, resourceIds, featureIds, txnIdRequestMap, roleActionInfoList);

        // Step4: Get the GEBUserID corresponding to a given CEWUserId & also get the
        // corresponding
        // authGroup
        Map<String, Boolean> gebGebCompanyIdbulkAuthorizeLimitMap = new HashMap<>();
        Map<String, Boolean> cewCompanyIdbulkAuthorizeLimitMap = new HashMap<>();
        List<GebUserIdGroupId> gebUserIdGroupIdList = submitTransactionConfiguration.getGEBUserIdAuthGroupInfo(userId,
                bankEntityId, cewGroupIds, cewCompanyIds, gebCompanyIds, gebGebCompanyIdbulkAuthorizeLimitMap,
                cewCompanyIdbulkAuthorizeLimitMap);

        // Step5: Get the approveOwnTransaction boolean value for a given companyId
        Map<String, Boolean> gebGebCompanyIdApproveOwnTxnMap = new HashMap<>();
        Map<String, Boolean> cewCompanyIdApproveOwnTxnMap = new HashMap<>();
        submitTransactionConfiguration.populateAuthorizeOwnTxnMap(cewCompanyIds, gebCompanyIds,
                gebGebCompanyIdApproveOwnTxnMap, cewCompanyIdApproveOwnTxnMap);
        // Step6: Call authMatrix for submit event so that we get the
        // authorization_status column
        // updated
        List<Transactions> transactionsResponseList;
        Map<String, Boolean> txnIdSubmitStatusMap = new HashMap<>();
        transactionsResponseList = getActionFromAuthMatrix(transactionInfoList, roleActionInfoList,
                cewCompanyIdApproveOwnTxnMap, gebUserIdGroupIdList, userId, txnIdSubmitStatusMap, isMultiTransaction,
                cewCompanyIdbulkAuthorizeLimitMap);

        twsUtils.restoreEncryptedTrnId(transactionIdMap, transactionsResponseList);
        CheckAuthMatrixResp checkAuthMatrixResp = new CheckAuthMatrixResp();
        TransactionsSchema transactionsSchema = new TransactionsSchema();
        transactionsSchema.setTransactions(transactionsResponseList);
        checkAuthMatrixResp.setUserTransactionActions(transactionsSchema);
        log.info("CheckAuthMatrixServiceImpl >> method - checkAuthMatrixForActions >> END"
                + checkAuthMatrixResp.toString());
        return checkAuthMatrixResp;
    }

    /**
     * @param submitTxnMandateInfoList
     * @param roleActionInfoList
     * @param cewCompanyIdApproveOwnTxnMap
     * @param gebUserIdGroupIdList
     * @param userId
     * @param txnIdSubmitStatusMap
     * @return
     * @desc Prepare the data for authMatrix library and also call it
     */
    protected List<Transactions> getActionFromAuthMatrix(List<TransactionInfo> submitTxnMandateInfoList,
            List<RoleActionInfo> roleActionInfoList, Map<String, Boolean> cewCompanyIdApproveOwnTxnMap,
            List<GebUserIdGroupId> gebUserIdGroupIdList, Long userId, Map<String, Boolean> txnIdSubmitStatusMap,
            Boolean isMultiTransaction, Map<String, Boolean> cewCompanyIdbulkAuthorizeLimitMap) {
        List<Transactions> transactionsResponseList = new ArrayList<>();
        Map<Integer, Set<String>> roleIdActionListMap = new HashMap<>();
        Map<Integer, String> roleIdNameMap = new HashMap<>();
        Map<String, Set<Integer>> transactionRoleMap = new HashMap<>();
        List<String> customerStatus = new ArrayList<>();
        customerStatus.add(REJECTED);
        customerStatus.add(REJECTED_BY_BANK_ACTION);
        customerStatus.add(SUCCESSFUL_STATUS);
        customerStatus.add(UNSUCCESSFUL_STATUS);

        // Collect the roleId list for a given combination of userId, compId,
        // groupId,ResourceId,FeatureId & accountNumber
        // Collect the roleId to roleName & actionList mapping
        for (RoleActionInfo eachRoleActionInfo : roleActionInfoList) {
            String roleActionKey = submitTransactionConfiguration.getRoleActionKey(eachRoleActionInfo);

            Set<String> actionList = roleIdActionListMap.get(eachRoleActionInfo.getRoleId());
            if (actionList == null) {
                actionList = new HashSet<>();
            }
            actionList.add(eachRoleActionInfo.getActionId());

            Set<Integer> roleIdList = transactionRoleMap.get(roleActionKey);
            if (roleIdList == null) {
                roleIdList = new HashSet<>();
            }
            roleIdList.add(eachRoleActionInfo.getRoleId());

            roleIdActionListMap.put(eachRoleActionInfo.getRoleId(), actionList);
            roleIdNameMap.put(eachRoleActionInfo.getRoleId(), eachRoleActionInfo.getRoleName());
            transactionRoleMap.put(roleActionKey, roleIdList);
        }

        // Collect the authorizationGroup for a given combination of gebUserId,
        // cewCompId & cewGroupId
        Map<String, GebUserIdGroupId> userCompGrpAuthGrpMap = new HashMap<>();
        for (GebUserIdGroupId eachGebUserIdGroupId : gebUserIdGroupIdList) {
            String userCompGrpAuthGrpKey = eachGebUserIdGroupId.getCewUserId() + UNDERSCORE_TEXT
                    + eachGebUserIdGroupId.getCewCompanyId() + UNDERSCORE_TEXT + eachGebUserIdGroupId.getCewGroupId();

            userCompGrpAuthGrpMap.put(userCompGrpAuthGrpKey, eachGebUserIdGroupId);
        }

        // Prepare the roleList & Call the authMatrix api with the collected data
        for (TransactionInfo eachSubmitTxnMandateInfo : submitTxnMandateInfoList) {
            List<Role> currentRoles = submitTransactionConfiguration.prepareRoleList(userId, eachSubmitTxnMandateInfo,
                    transactionRoleMap, roleIdNameMap, roleIdActionListMap);
            Transactions transactionsObject = new Transactions();
            if (!customerStatus.contains(eachSubmitTxnMandateInfo.getCustomerStatusCode())) {
                transactionsObject = callAuthMatrix(userId, eachSubmitTxnMandateInfo, userCompGrpAuthGrpMap,
                        currentRoles, txnIdSubmitStatusMap, cewCompanyIdApproveOwnTxnMap, isMultiTransaction,
                        cewCompanyIdbulkAuthorizeLimitMap);
            } else {
                transactionsObject.setTransactionId(eachSubmitTxnMandateInfo.getTransactionId());
                transactionsObject.setStatusMessage(SUCCESS_STATUS_MESSAGE);
            }
            transactionsObject.setUserCompanyActions(getUserTransactionActionList(roleActionInfoList));
            transactionsObject.setActions(
                    getFinalActionList(transactionsObject.getUserCompanyActions(), transactionsObject.getActions()));
            transactionsResponseList.add(transactionsObject);
        }

        return transactionsResponseList;
    }

    /**
     * @param userId
     * @param eachSubmitTxnMandateInfo
     * @param userCompGrpAuthGrpMap
     * @param currentRoles
     * @param txnIdSubmitStatusMap
     * @param cewCompanyIdApproveOwnTxnMap
     * @return
     * @desc Call the authMatrix api with the collected data
     */
    protected Transactions callAuthMatrix(Long userId, TransactionInfo eachSubmitTxnMandateInfo,
            Map<String, GebUserIdGroupId> userCompGrpAuthGrpMap, List<Role> currentRoles,
            Map<String, Boolean> txnIdSubmitStatusMap, Map<String, Boolean> cewCompanyIdApproveOwnTxnMap,
            Boolean isMultiTransaction, Map<String, Boolean> cewCompanyIdbulkAuthorizeLimitMap) {
        String statusMessage = null;
        MatrixProcessingResult result = null;
        MatrixContext matrixContext = null;

        GebUserIdGroupId gebUserIdGroupId = userCompGrpAuthGrpMap
                .get(userId + UNDERSCORE_TEXT + eachSubmitTxnMandateInfo.getCompanyId() + UNDERSCORE_TEXT
                        + eachSubmitTxnMandateInfo.getCompanyGroupId());

        if (ObjectUtils.isEmpty(gebUserIdGroupId)) {
            log.error("No GEB CEW relationship userIds found");
            statusMessage = GenericConstants.FAILURE_STATUS_MESSAGE;
            txnIdSubmitStatusMap.put(eachSubmitTxnMandateInfo.getTransactionId(), false);
        }

        Boolean isApproveOwnTransaction = cewCompanyIdApproveOwnTxnMap.get(eachSubmitTxnMandateInfo.getCompanyId());
        if (isApproveOwnTransaction == null) {
            isApproveOwnTransaction = false;
        }
        log.info("isApproveOwnTransaction: {}", isApproveOwnTransaction);

        Boolean isbulkAuthorizeLimit = cewCompanyIdbulkAuthorizeLimitMap.get(eachSubmitTxnMandateInfo.getCompanyId());
        log.info("isbulkAuthorizeLimit: {}", isbulkAuthorizeLimit);

        if (isbulkAuthorizeLimit == null) {
            isbulkAuthorizeLimit = false;
        }

        try {
            if (entitlementsFeaturesConfiguration.isValidSingleTransaction(eachSubmitTxnMandateInfo.getFeatureId())) {
                matrixContext = populateSingleMatrixContextDtls(userId, eachSubmitTxnMandateInfo, currentRoles,
                        gebUserIdGroupId, isApproveOwnTransaction);
            } else if (isBulkOnlineOrBulkFileUpload.or(isBulkOnlineOrBulkFileUploadPayroll)
                    .test(eachSubmitTxnMandateInfo)) {
                log.info("callAuthMatrix");
                if (Boolean.TRUE.equals(isbulkAuthorizeLimit)) {
                    log.info("callAuthMatrix >>  isbulkAuthorizeLimit {} >> populateBulkMatrixContextWithMaximumAmt",
                            isbulkAuthorizeLimit);
                    matrixContext = populateBulkMatrixContextWithMaximumAmt(userId, eachSubmitTxnMandateInfo,
                            currentRoles, gebUserIdGroupId, isApproveOwnTransaction);
                } else {
                    log.info("callAuthMatrix >>  isbulkAuthorizeLimit {} >> populateBulkMatrixContextWithTotalAmt",
                            isbulkAuthorizeLimit);
                    matrixContext = populateBulkMatrixContextWithTotalAmt(userId, eachSubmitTxnMandateInfo,
                            currentRoles, gebUserIdGroupId, isApproveOwnTransaction);
                }
            }

            log.info("callAuthMatrix >>  matrixContext {} >> ", matrixContext);

            result = authMatrixService.checkAuthMatrixForAllowedActions(matrixContext);
            if (result == null) {
                throw new GenericException("Authmatrix submit call returned null for transactionId "
                        + eachSubmitTxnMandateInfo.getTransactionId());
            } else {
                statusMessage = SUCCESS_STATUS_MESSAGE;
                txnIdSubmitStatusMap.put(eachSubmitTxnMandateInfo.getTransactionId(), true);
            }
        } catch (NoRulesMatchedException ex) {
            log.error("No rules matched the account or company or group details provided ", ex);
            if (Boolean.TRUE.equals(isMultiTransaction)) {
                statusMessage = FAILURE_STATUS_MESSAGE;
                txnIdSubmitStatusMap.put(eachSubmitTxnMandateInfo.getTransactionId(), false);
            }
        } catch (Exception ex) {
            log.error("Error in auth matrix check ", ex);
            statusMessage = FAILURE_STATUS_MESSAGE;
            txnIdSubmitStatusMap.put(eachSubmitTxnMandateInfo.getTransactionId(), false);
        }
        return getTransactionsObject(eachSubmitTxnMandateInfo, result, statusMessage, isMultiTransaction);
    }

    private MatrixContext populateBulkMatrixContextWithTotalAmt(Long userId, TransactionInfo eachSubmitTxnMandateInfo,
            List<Role> currentRoles, GebUserIdGroupId gebUserIdGroupId, Boolean isApproveOwnTransaction) {
        return new MatrixContext.Builder().withUser(userId)
                .withUsersCompany(eachSubmitTxnMandateInfo.getCompanyId(), null,
                        eachSubmitTxnMandateInfo.getCompanyGroupId())
                .withRoles(currentRoles)
                .withAllowApproveOwnTrx(isApproveOwnTransaction)
                .withAuthorizationGroups(new ArrayList<>(Arrays.asList(gebUserIdGroupId.getAuthGroup())))
                .withTransactionDetails(eachSubmitTxnMandateInfo.getTransactionId(),
                        eachSubmitTxnMandateInfo.getTransactionCurrency(),
                        new BigDecimal(eachSubmitTxnMandateInfo.getTotalAmount()))
                .withBulkProcessing(true, new BigDecimal(eachSubmitTxnMandateInfo.getTotalAmount()))
                .withAlternateUserId(Long.parseLong(gebUserIdGroupId.getGebUserId()))
                .withUsersCompanyAlternateId(gebUserIdGroupId.getGebCompanyId())
                .withCountry(country)
                .build();
    }

    private MatrixContext populateBulkMatrixContextWithMaximumAmt(Long userId, TransactionInfo eachSubmitTxnMandateInfo,
            List<Role> currentRoles, GebUserIdGroupId gebUserIdGroupId, Boolean isApproveOwnTransaction) {
        return new MatrixContext.Builder().withUser(userId)
                .withUsersCompany(eachSubmitTxnMandateInfo.getCompanyId(), null,
                        eachSubmitTxnMandateInfo.getCompanyGroupId())
                .withRoles(currentRoles)
                .withAllowApproveOwnTrx(isApproveOwnTransaction)
                .withAuthorizationGroups(new ArrayList<>(Arrays.asList(gebUserIdGroupId.getAuthGroup())))
                .withTransactionDetails(eachSubmitTxnMandateInfo.getTransactionId(),
                        eachSubmitTxnMandateInfo.getTransactionCurrency(),
                        new BigDecimal(eachSubmitTxnMandateInfo.getMaxAmount()))
                .withBulkProcessing(true, new BigDecimal(eachSubmitTxnMandateInfo.getTotalAmount()))
                .withAlternateUserId(Long.parseLong(gebUserIdGroupId.getGebUserId()))
                .withUsersCompanyAlternateId(gebUserIdGroupId.getGebCompanyId())
                .withCountry("MY")
                .build();
    }

    private MatrixContext populateSingleMatrixContextDtls(Long userId, TransactionInfo eachSubmitTxnMandateInfo,
            List<Role> currentRoles, GebUserIdGroupId gebUserIdGroupId, Boolean isApproveOwnTransaction) {
        return new MatrixContext.Builder().withUser(userId)
                .withUsersCompany(eachSubmitTxnMandateInfo.getCompanyId(), null,
                        eachSubmitTxnMandateInfo.getCompanyGroupId())
                .withRoles(currentRoles)
                .withAllowApproveOwnTrx(isApproveOwnTransaction)
                .withAuthorizationGroups(new ArrayList<>(Arrays.asList(gebUserIdGroupId.getAuthGroup())))
                .withTransactionDetails(eachSubmitTxnMandateInfo.getTransactionId(),
                        eachSubmitTxnMandateInfo.getTransactionCurrency(),
                        eachSubmitTxnMandateInfo.getTransactionAmount())
                .withAlternateUserId(Long.parseLong(gebUserIdGroupId.getGebUserId()))
                .withUsersCompanyAlternateId(gebUserIdGroupId.getGebCompanyId())
                .withCountry("MY")
                .build();
    }

    /**
     * @param submitTxnMandateInfo
     * @param statusMessage
     * @return
     * @desc Get the transactionObject and prepare the statusMessage
     */
    protected Transactions getTransactionsObject(TransactionInfo submitTxnMandateInfo, MatrixProcessingResult result,
            String statusMessage, Boolean isMultiTransaction) {
        Transactions transactionsObject = new Transactions();
        transactionsObject.setTransactionId(submitTxnMandateInfo.getTransactionId());
        if (result != null && result.getAllowedActionsForUser() != null) {
            transactionsObject.getActions().addAll(result.getAllowedActionsForUser());
        }
        Predicate<String> predicate = action -> VIEW_ACTION.equals(action) || AUTHORIZE_ACTION.equals(action)
                || VERIFY_ACTION.equals(action) || SEND_TO_BANK_ACTION.equals(action) || CREATE_ACTION.equals(action);
        boolean isTrue = transactionsObject.getActions().stream().filter(predicate).count() < 1
                && Boolean.TRUE.equals(isMultiTransaction);
        if (transactionsObject.getActions().isEmpty() || isTrue) {
            statusMessage = FAILURE_STATUS_MESSAGE;
        }
        statusMessage = inValidCustomerTransactionStatus(submitTxnMandateInfo, statusMessage, isMultiTransaction);
        log.info("before dailyLimitCheck, the DailyLimitExceeded is {}", transactionsObject.getDailyLimitExceeded());
        statusMessage = dailyLimitCheck(result, transactionsObject, statusMessage);
        log.info("after dailyLimitCheck, the DailyLimitExceeded is {}", transactionsObject.getDailyLimitExceeded());
        if (YES_TEXT.equals(transactionsObject.getDailyLimitExceeded())) {
            statusMessage = FAILURE_STATUS_MESSAGE;
        }
        /* Changes regarding UST CEW-1050 */
        if (result != null && result.getMatrixContext() != null
                && result.getMatrixContext().getAdditionalContextData().get(IS_LAST_AUTHORIZER) != null) {
            String isLastAuth = result.getMatrixContext().getAdditionalContextData().get(IS_LAST_AUTHORIZER).toString();
            transactionsObject.setIsLastAuthoriser(isLastAuth);
        }
        transactionsObject.setStatusMessage(statusMessage);
        return transactionsObject;
    }

    private String dailyLimitCheck(MatrixProcessingResult result, Transactions transactionsObject,
            String statusMessage) {
        if (result != null && result.getMatrixContext() != null
                && result.getMatrixContext().getAdditionalContextData().get("LIMIT_AVLBL") != null) {
            String dailyAuthLimitFlag = result.getMatrixContext()
                    .getAdditionalContextData()
                    .get("LIMIT_AVLBL")
                    .toString();
            dailyAuthLimitFlag = NO_TEXT.equals(dailyAuthLimitFlag) ? YES_TEXT : NO_TEXT;

            if (YES_TEXT.equals(dailyAuthLimitFlag)) {
                statusMessage = FAILURE_STATUS_MESSAGE;
            }
            transactionsObject.setDailyLimitExceeded(dailyAuthLimitFlag);

        } else {
            log.warn("result.getMatrixContext() or result or LIMIT_AVLBL value is is null");
            transactionsObject.setDailyLimitExceeded(NO_TEXT);
        }
        return statusMessage;
    }

    private String inValidCustomerTransactionStatus(TransactionInfo submitTxnMandateInfo, String statusMessage,
            Boolean isMultiTransaction) {
        if (Boolean.TRUE.equals(isMultiTransaction) && (DRAFT.equals(submitTxnMandateInfo.getCustomerStatusCode())
                || PENDING_REWORK.equals(submitTxnMandateInfo.getCustomerStatusCode()))) {
            statusMessage = FAILURE_STATUS_MESSAGE;
        }
        if (Boolean.TRUE.equals(isMultiTransaction)
                && PENDING_REWORK.equals(submitTxnMandateInfo.getCustomerStatusCode())
                && isValidPendinReworkTransaction.test(submitTxnMandateInfo)) {
            statusMessage = SUCCESS_STATUS_MESSAGE;
        }

        return statusMessage;
    }

    private List<String> validateMaxThresholdTxnsLimit(CheckAuthMatrixReq checkAuthMatrixReq) {
        List<String> encryTransactionIds = checkAuthMatrixReq.getTransactionIds();
        try {
            if (CollectionUtils.isEmpty(encryTransactionIds)
                    || encryTransactionIds.contains(GenericConstants.EMPTY_TEXT)
                    || encryTransactionIds.size() > GenericConstants.TRANSACTION_MAX_SIZE)
                throw new GenericException(
                        "Transaction list is not in Range/Valid, Size is " + encryTransactionIds.size());
        } catch (Exception ex) {
            log.error("Exception occurred while validating transaction threshold limit", ex);
            throw new ApplicationException(ErrorCodeConstants.CEW_2071);
        }
        return encryTransactionIds;
    }

    @Transactional
    public void updateRejectedStatus(TransactionInfo eachSubmitTxnMandateInfo, Long userId) {
        submitTransactionConfiguration.updateRejectedStatusByTransactionId(eachSubmitTxnMandateInfo.getTransactionId(),
                REJECTED, REJECTED);
        insertAuditTrailService.insertAuditTrails(eachSubmitTxnMandateInfo, userId, REJECTED);
    }

    protected List<String> getUserTransactionActionList(List<RoleActionInfo> roleActionInfoList) {
        List<String> userActionList = new ArrayList<>();
        for (RoleActionInfo roleActionInfo : roleActionInfoList) {
            userActionList.add(roleActionInfo.getActionId());
        }
        log.info("CheckAuthMatrixServiceImpl >> getUserTransactionActionList >> userActionList:{}", userActionList);
        List<String> userActionListWithoutDuplicates = new ArrayList<>(new HashSet<>(userActionList));
        log.info("CheckAuthMatrixServiceImpl >> getUserTransactionActionList >> userActionListWithoutDuplicates:{}",
                userActionListWithoutDuplicates);
        return userActionListWithoutDuplicates;
    }

    protected List<String> getFinalActionList(List<String> userTransactionActionList, List<String> actionList) {
        if (userTransactionActionList != null && userTransactionActionList.contains(CREATE_ACTION)) {
            if (actionList == null) {
                actionList = new ArrayList<>();
            }
            actionList.add(CREATE_ACTION);
        }
        log.info("CheckAuthMatrixServiceImpl >> getActionList >> actionList:{}", actionList);
        List<String> actionListWithoutDuplicates = new ArrayList<>(new HashSet<>(actionList));
        log.info("CheckAuthMatrixServiceImpl >> getActionList >> actionListWithoutDuplicates:{}",
                actionListWithoutDuplicates);
        return actionListWithoutDuplicates;
    }
}
