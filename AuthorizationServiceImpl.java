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

import static com.uob.gwb.tws.common.AuthorizationConstants.*;
import static com.uob.gwb.tws.common.GenericConstants.*;
import static com.uob.gwb.tws.common.TransactionPredicate.*;

import com.isprint.am.xmlrpc.clientcore.*;
import com.uob.gwb.transaction.common.config.EntitlementsFeaturesConfiguration;
import com.uob.gwb.transaction.common.domain.PwsResourceConfigurations;
import com.uob.gwb.transaction.common.domain.UserResourceFeaturesActionsData;
import com.uob.gwb.transaction.common.service.EntitlementService;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.tws.aes.dao.SubmitForAuthAesDao;
import com.uob.gwb.tws.common.*;
import com.uob.gwb.tws.config.SubmitTransactionConfiguration;
import com.uob.gwb.tws.config.TransactionSigningConfiguration;
import com.uob.gwb.tws.config.TransactionWorkFlowConfig;
import com.uob.gwb.tws.data.mapper.TransactionModelDomainMapper;
import com.uob.gwb.tws.domain.*;
import com.uob.gwb.tws.domain.TransactionInfo;
import com.uob.gwb.tws.model.*;
import com.uob.gwb.tws.pws.dao.AuthorizationDao;
import com.uob.gwb.tws.pws.dao.ReturnMandateDao;
import com.uob.trx.auth.matrix.event.TransactionEvent;
import com.uob.ufw.core.exception.ApplicationException;
import com.uob.ufw.core.util.JsonUtil;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service("AuthorizationService")
@Slf4j
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private final TransactionSigningConfiguration txnSigningConfiguration;

    private final TransactionModelDomainMapper transactionModelDomainMapper;
    private final ESignValueDateValidationsServiceImpl eSignValueDateValidationsService;
    private final AuthorizationDao authorizationDao;

    private final SubmitForAuthAesDao submitAuthAesDao;

    private final TwsUtils twsUtils;

    private final SubmitTransactionConfiguration submitTransactionConfiguration;

    private final TransactionUtils transactionUtils;

    private final EntitlementService entitlementService;

    private final TransactionMandateUtil transactionMandateUtil;

    private final SubmitPwsTransactions submitPwsTransactions;

    private final UpdateRejectMandateRequestUtil updateRejectMandateRequestUtil;

    private final ReturnMandateDao returnMandateDao;

    private final RejectMandateRequestService rejectMandateRequestService;

    private final GetMultisubmitChallengeCode getMultisubmitChallengeCode;

    private final TransactionProperties transactionProperties;

    private final TransactionWorkFlowConfig transactionWorkFlowConfig;

    private final EntitlementsFeaturesConfiguration entitlementsFeaturesConfiguration;

    private final IDBAuditSaveService iDBAuditSaveService;
    public static final Predicate<AuthorizeReq> isHardTokenType = a -> AuthorizationConstants.HARD_TOKEN
            .equals(a.getTokenType().getValue());

    @Override
    public AuthorizeResp getAuthorize(AuthorizeReq authorizeReq) {
        log.info("AuthorizationServiceImpl >> getAuthorize >> started >> " + JsonUtil.toJsonString(authorizeReq));
        AuthorizeResp authorizeRes = new AuthorizeResp();
        List<TransactionSchema> valueDateFailedTxnSchemaList = new ArrayList<>();
        log.info("****************Started - Validation" + " ApplicationId,ChannelId,BankEntityId***************");
        validateAuthorizeReq(authorizeReq);
        validateHeaders(authorizeReq);
        Long userId = twsUtils.getDecryptedUserId(authorizeReq.getUserId());
        String bankEntityId = getBankEntity(authorizeReq);
        List<String> changeTokens = getChangeTokenList(authorizeReq);
        checkLimitTransactions(authorizeReq);
        List<String> encryptedTrnIds = getEncryptedTrnIds(authorizeReq);
        Map<String, String> transactionIdMap = new HashMap<>();
        List<String> transactionIds = twsUtils.getDecryptedTransactionIds(encryptedTrnIds, transactionIdMap);
        log.info("****************userId ****************:{}", userId.toString());
        String loginUserId = getLoginUserId(userId.toString());

        List<String> customerStatusList = new ArrayList<>();
        List<TransactionInfo> transactionList = submitTransactionConfiguration.getTransactionInfos(bankEntityId,
                changeTokens, transactionIds, customerStatusList);
        setOriginalEncryptionTxnId(transactionList, transactionIdMap);

        String companyGroupName = getCompanyGroupName(transactionList.stream().findFirst());

        AuthTransactionType txnType = getAuthTransactionTypes(authorizeReq, transactionList, transactionIds);

        ChallengeCodeInfo challengeCodeInfo = getChallengeCodeInfo(authorizeReq, transactionList, txnType);

        String resourceId = null;
        if (!transactionList.isEmpty()) {
            resourceId = transactionList.stream()
                    .map(TransactionInfo::getResourceId)
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException("Invalid Resource ID"));
        }
        TransactionIDsInfo transactionIDsInfo = new TransactionIDsInfo();
        transactionIDsInfo.setTransactionIds(transactionIds);
        transactionIDsInfo.setEncryptedTransactionIds(encryptedTrnIds);
        log.info(" Condition check isMockSSOResponse for ESign ");
        if (!transactionProperties.isMockSSOResponse()) {
            ApiProxyFactory apiProxyFactory = txnSigningConfiguration.getApiProxyFactoryInstance();
            if (apiProxyFactory != null && companyGroupName != null) {
                Boolean auth = getAuth(authorizeReq, txnType, companyGroupName, loginUserId, challengeCodeInfo);
                txnType.setVerifyEsignAuthTransaction(auth);
                log.info("**************** After verifyESign****************{}:", auth);
                eSignValidationAndTxnDetails(authorizeReq, authorizeRes, transactionIDsInfo, transactionIdMap, txnType,
                        valueDateFailedTxnSchemaList, resourceId);
            }
        } else {
            getTransactionsResponseList(authorizeReq, authorizeRes, encryptedTrnIds, transactionIdMap, transactionIds,
                    txnType.isMultiTransaction());
        }
        List<PwsResourceConfigurations> configurationList = submitPwsTransactions.getCutOffTime(CUT_OFF_TIME_TEXT);
        transactionList.stream()
                .filter(submitTransactionConfiguration::isValidForUpdate)
                .forEach(txnInfo -> submitTransactionConfiguration.updateTransferDateForBulk(txnInfo,
                        configurationList));
        /* To remove duplicate records from response */
        List<TransactionSchema> uniqueTransactionsResponseList = getTransactionsResponseListDistinctByBankRefId(
                authorizeRes.getTransactions());
        authorizeRes.setTransactions(uniqueTransactionsResponseList);

        iDBAuditSaveService.callIdbAuditSave(transactionIds, userId);
        log.info("AuthorizationServiceImpl >> getAuthorize >> ended");
        return authorizeRes;
    }

    private void validateForValueDateFailedTxnSchemaList(List<TransactionSchema> valueDateFailedTxnSchemaList,
            List<String> encryptedTrnIds, List<String> transactionIds, Map<String, String> transactionIdMap,
            AuthorizeReq authorizeReq) {
        encryptedTrnIds.forEach(encryptedTrnId -> {
            TransactionDetailsInfo transactionDetailsInfo = eSignValueDateValidationsService
                    .checkBorderLineConditionForValueDate(encryptedTrnId);
            if (Objects.nonNull(transactionDetailsInfo)
                    && CollectionUtils.isNotEmpty(transactionDetailsInfo.getValidationErrors())) {
                log.info("ValueDate Validation Failed for transactionID::{} BankReferenceID::{}",
                        transactionDetailsInfo.getTransactionId(), transactionDetailsInfo.getBankReferenceId());
                transactionIds.remove(transactionDetailsInfo.getTransactionId());
                transactionIdMap.remove(transactionDetailsInfo.getTransactionId());
                transactionDetailsInfo.setEncryptedTransactionId(encryptedTrnId);
                TransactionSchema transactionSchema = transactionModelDomainMapper
                        .mapTransactionDetailToTransactionSchema(transactionDetailsInfo);
                transactionSchema.setStatusMessage(FAILURE_STATUS_MESSAGE);
                valueDateFailedTxnSchemaList.add(transactionSchema);
            }
        });

        if (CollectionUtils.isNotEmpty(valueDateFailedTxnSchemaList)) {
            List<TransactionIdSchema> transactionSchemaList = authorizeReq.getTransactionIds()
                    .stream()
                    .filter(transactionIdSchema -> transactionIds
                            .contains(transactionUtils.getDecrypted(transactionIdSchema.getTransactionId())))
                    .toList();
            authorizeReq.setTransactionIds(transactionSchemaList);
        }
    }
    private void validateHeaders(AuthorizeReq authorizeReq) {
        log.info("Validation for ApplicationId,ChannelId,BankEntityId - Started");
        twsUtils.applicationIdValidation(authorizeReq.getAdditionalPropValue(GenericConstants.APPLICATION_ID));
        twsUtils.channelIdValidation(authorizeReq.getAdditionalPropValue(GenericConstants.CHANNEL_ID));
        twsUtils.encryptedBankEntityIdValidation(authorizeReq.getAdditionalPropValue(GenericConstants.ENTITY_TEXT));
        log.info("Validation for ApplicationId,ChannelId,BankEntityId - Ended");
    }

    private Boolean getAuth(AuthorizeReq authorizeReq, AuthTransactionType txnType, String companyGroupName,
            String loginUserId, ChallengeCodeInfo challengeCodeInfo) {
        Boolean auth = false;
        log.info("****************actionType****************");
        if (Boolean.TRUE.equals(txnType.isMultiTransaction())) {
            auth = verifyESign(authorizeReq, companyGroupName, loginUserId,
                    challengeCodeInfo.getMultiSubmitChallengeCodes());
            log.info("****************multiSubmit auth result****************{}:", auth);
        } else if (Boolean.TRUE.equals(txnType.isBulkTransactions())) {
            auth = verifyESign(authorizeReq, companyGroupName, loginUserId, challengeCodeInfo.getBulkChallengeCodes());
            log.info("****************bulk auth result****************{}:", auth);
        } else if (Boolean.TRUE.equals(txnType.isSingleTransaction())) {
            auth = verifyESign(authorizeReq, companyGroupName, loginUserId,
                    challengeCodeInfo.getSingleChallengeCodes());
            log.info("****************Single auth result****************{}:", auth);
        }
        return auth;
    }

    @NotNull
    private ChallengeCodeInfo getChallengeCodeInfo(AuthorizeReq authorizeReq, List<TransactionInfo> transactionList,
            AuthTransactionType txnType) {
        ChallengeCodeInfo challengeCodeInfo = new ChallengeCodeInfo();
        log.info("==============transactionList size is=============={}:", transactionList.size());
        if (Boolean.TRUE.equals(txnType.isSingleTransaction())
                && !REQ_INDICATOR_MULTISUBMIT.equalsIgnoreCase(authorizeReq.getRequestIndicator())) {
            log.info("===isSingleTransaction===={}:", txnType.isSingleTransaction());
            challengeCodeInfo.setSingleChallengeCodes(getChallengeCodes(transactionList));
        } else if (Boolean.TRUE.equals(txnType.isBulkTransactions())
                && !REQ_INDICATOR_MULTISUBMIT.equalsIgnoreCase(authorizeReq.getRequestIndicator())) {
            log.info("===isBulkTransactions===={}:", txnType.isBulkTransactions());
            challengeCodeInfo.setBulkChallengeCodes(getBulkChallengeCodes(transactionList));
        } else if (Boolean.TRUE.equals(txnType.isMultiTransaction())) {
            log.info("===isMultiTransaction===={}:", txnType.isMultiTransaction());
            challengeCodeInfo.setMultiSubmitChallengeCodes(getMultiSubmitChallengeCodes(transactionList));
        }
        return challengeCodeInfo;
    }

    private AuthTransactionType getAuthTransactionTypes(AuthorizeReq authorizeReq,
            List<TransactionInfo> transactionList, List<String> transactionIds) {
        AuthTransactionType txnType = new AuthTransactionType();
        if (!org.springframework.util.CollectionUtils.isEmpty(transactionList)
                && Objects.nonNull(transactionList.get(0).getFeatureId())) {

            if (transactionIds.size() > 1
                    || REQ_INDICATOR_MULTISUBMIT.equalsIgnoreCase(authorizeReq.getRequestIndicator())) {
                txnType.setMultiTransaction(Boolean.TRUE);
            } else if (entitlementsFeaturesConfiguration
                    .isValidSingleTransaction(transactionList.get(0).getFeatureId())) {
                log.info("===isSingleTransaction====True");
                txnType.setSingleTransaction(Boolean.TRUE);
            } else if (isFeaturedIdBulk.test(transactionList.get(0)) || (transactionWorkFlowConfig.getBulkFeatureIds()
                    .contains(transactionList.get(0).getFeatureId()))) {
                log.info("===isBulkTransactions MY====True");
                txnType.setBulkTransactions(Boolean.TRUE);
            }
        }
        return txnType;
    }

    private void eSignValidationAndTxnDetails(AuthorizeReq authorizeReq, AuthorizeResp authorizeRes,
            TransactionIDsInfo transactionIDsInfo, Map<String, String> transactionIdMap, AuthTransactionType txnType,
            List<TransactionSchema> valueDateFailedTxnSchemaList, String resourceId) {
        Boolean auth = txnType.isVerifyEsignAuthTransaction();
        Boolean isMultiTransaction = txnType.isMultiTransaction();
        List<String> encryptedTrnIds = transactionIDsInfo.getEncryptedTransactionIds();

        if (Boolean.TRUE.equals(auth)) {

            if (isNotMYRPPResource.test(resourceId)
                    && !StringUtils.equalsIgnoreCase(ACTION_TYPE_SINGLE_RETURN_TEXT, authorizeReq.getActionType())
                    && !StringUtils.equalsIgnoreCase(ACTION_TYPE_SINGLE_REJECT_TEXT, authorizeReq.getActionType())) {
                validateForValueDateFailedTxnSchemaList(valueDateFailedTxnSchemaList, encryptedTrnIds,
                        transactionIDsInfo.getTransactionIds(), transactionIdMap, authorizeReq);
                encryptedTrnIds = getEncryptedTrnIds(authorizeReq);
                isMultiTransaction = authorizeReq.getTransactionIds().size() > 1;
            }
            if (CollectionUtils.isNotEmpty(authorizeReq.getTransactionIds())) {
                getTransactionsResponseList(authorizeReq, authorizeRes, encryptedTrnIds, transactionIdMap,
                        transactionIDsInfo.getTransactionIds(), isMultiTransaction);
            }
            if (CollectionUtils.isNotEmpty(valueDateFailedTxnSchemaList)) {
                if (Objects.isNull(authorizeRes.getTransactions()))
                    authorizeRes.setTransactions(valueDateFailedTxnSchemaList);
                else
                    authorizeRes.getTransactions().addAll(valueDateFailedTxnSchemaList);

            }
        } else {
            throw new ApplicationException(ErrorCodeConstants.OTP_VALIDATION, "error.authorise.esign.failed");
        }
    }

    private String getCompanyGroupName(Optional<TransactionInfo> transactionInfo) {
        String companyGroupName = null;
        if (transactionInfo.isPresent()) {
            String companyId = transactionInfo.get().getCompanyId();
            companyGroupName = getCompanyGroupName(companyId);
        }
        return companyGroupName;
    }

    public String[] getBulkChallengeCodes(List<TransactionInfo> transactionList) {
        log.info("AuthorizationServiceImpl getBulkChallengeCodes Started");
        String[] bulkChallengeCodes = null;
        try {
            bulkChallengeCodes = new String[]{getMultisubmitChallengeCode.getBulkChallengeCodeOne(transactionList),
                    getMultisubmitChallengeCode.getBulkChallengeCodeTwo(transactionList)};

            log.info("Size of BulkChallengeCodes :- " + bulkChallengeCodes.length);
            log.info("BulkChallengeCodes");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            log.error("Error in getBulkChallengeCodes Generation", ex);
        }
        log.info("AuthorizationServiceImpl getBulkChallengeCodes End");

        return bulkChallengeCodes;
    }

    public String getChallengeCodeByTnxAmount(TransactionInfo transactionInfo) {
        BigDecimal trnAmountMultiplyBy;
        String str;
        if ((RPP_REQUEST_TO_PAY_TEXT.equals(transactionInfo.getResourceId())
                && DIRECTION_INCOMING.equals(transactionInfo.getDirection()))
                || (RPP_REQUEST_TO_PAY_BUNDLE_TEXT.equals(transactionInfo.getResourceId())
                        && DIRECTION_INCOMING.equals(transactionInfo.getDirection()))
                || (RPP_REQUEST_TO_PAY_REFUND_TEXT.equals(transactionInfo.getResourceId())
                        && DIRECTION_OUTGOING.equals(transactionInfo.getDirection()))) {
            log.info(" inside getChallengeCodeByPayableAmount Start with payable amount = {} ",
                    transactionInfo.getPayableAmount().setScale(2, RoundingMode.HALF_UP));
            trnAmountMultiplyBy = transactionInfo.getPayableAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            log.info(" inside getChallengeCodeByTnxAmount Start with transaction amount = {} ",
                    transactionInfo.getTransactionAmount().setScale(2, RoundingMode.HALF_UP));
            trnAmountMultiplyBy = transactionInfo.getTransactionAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        if (trnAmountMultiplyBy.toBigInteger().toString().length() <= 4) {
            str = String.format("%04d", trnAmountMultiplyBy.toBigInteger());
        } else if (trnAmountMultiplyBy.toBigInteger().toString().length() > 4
                && trnAmountMultiplyBy.toBigInteger().toString().length() <= 8) {
            str = trnAmountMultiplyBy.toBigInteger().toString();
        } else {
            str = trnAmountMultiplyBy.toBigInteger().toString().substring(0, 8);
        }
        log.info(" inside getChallengeCodeByTnxAmount END with transaction amount  "
                + transactionInfo.getTransactionAmount() + " and challengeCode1 = " + str);
        return str;
    }

    public String getChallengeCode2(TransactionInfo transactionInfo) {
        log.info("AuthorizationServiceImpl >> getChallengeCode2 >> transaction info");
        return isValidApplicationType.test(transactionInfo)
                ? challengedCodeExistingLogic(transactionInfo)
                : generateChallengedCodeWithAcctNo(transactionInfo);
    }

    private String generateChallengedCodeWithAcctNo(TransactionInfo transactionInfo) {
        return isValidMandateOrRtp.test(transactionInfo)
                ? TransactionMandateUtil.getChanllengeCode(transactionInfo.getCounterPartyAccountNum())
                : challengedCodeExistingLogic(transactionInfo);
    }

    private String challengedCodeExistingLogic(TransactionInfo transactionInfo) {
        return isNotMYRPPResource.test(transactionInfo.getResourceId())
                ? getChallengeCode2SG(transactionInfo)
                : TransactionMandateUtil.getChanllengeCodewithLength(transactionInfo.getBankReferenceNumber());
    }

    public String getChallengeCode2SG(TransactionInfo transactionInfo) {
        log.info("inside getChallengeCode2 STARTED with BankReferenceNumber");
        log.info("inside getChallengeCode2 STARTED with CounterPartyAccountNum");
        String str;

        if (StringUtils.isNotEmpty(transactionInfo.getCounterPartyAccountNum())) {
            log.info("inside generating ChallengeCode2 for RPP-Mandate");
            str = enrichChallengeCode(transactionInfo.getCounterPartyAccountNum());
        } else {
            log.info("inside generating ChallengeCode2 using BankReferenceNumber");
            str = transactionInfo.getBankReferenceNumber()
                    .substring(transactionInfo.getBankReferenceNumber().length() - 8);
        }

        log.info("inside getChallengeCode2 END challengeCode2");
        return str;
    }

    private String enrichChallengeCode(String accountSubStr) {
        // Check for non numeric char and replace it with empty string
        String regExp = "\\D";
        String str = accountSubStr.replaceAll(regExp, "");
        if (str.length() < 4) {
            return StringUtils.leftPad(str, 4, "0");
        } else {
            return str.length() > 8 ? str.substring(0, 8) : str;
        }
    }

    protected Boolean verifyESign(AuthorizeReq authorizeReq, String companyGroupName, String loginUserId,
            String[] challengeCodes) {
        log.info("AuthorizationServiceImpl >> verifyESign >> started");

        ApiProxy proxy = null;
        boolean status = false;

        Map<String, String> params = new HashMap<>();
        params.put("Pki", authorizeReq.getPublicKeyIndex() + "");
        params.put("Ra", authorizeReq.getRandomNumber());
        params.put("DPModuleId", "DPOTP");
        params.put("authMode", "SG");
        params.put("appId", getAppId(authorizeReq));

        String tokenVendor = getTokenVendor(authorizeReq);

        log.info("tokenVendor {}" + tokenVendor);
        log.info("challengeCodes");
        log.info("OTPPP");
        proxy = txnSigningConfiguration.getProxy();
        try {
            if (CollectionUtils.isNotEmpty(authorizeReq.getChallengeCode())
                    && !authorizeReq.getChallengeCode().equals(Arrays.asList(challengeCodes))) {
                log.error(" Challenge code mismatched ");
                throw new ApplicationException(

                        ErrorCodeConstants.CEW_2080, "error.challenge.code.is.mismatch");

            }
            String tokenSerialNumber = getTokenSerialNumber(authorizeReq, proxy, companyGroupName, loginUserId);
            log.info("tokenSerialNumber" + tokenSerialNumber);

            if (tokenSerialNumber != null) {
                /** SyncTokenBlobsBySerialNum is applicable only for HARD TOKEN */
                if (isHardTokenType.test(authorizeReq)) {
                    getSyncTokenBlobsBySerialNum(proxy, tokenVendor, tokenSerialNumber);
                }
                getVerifyDigitalSignatureBySerialNum(proxy, tokenVendor, tokenSerialNumber, challengeCodes,
                        authorizeReq.getOtp(), params);
                status = true;
            }
        } catch (Exception e) {
            log.error("Exception occurred at sso e signature, the Exception details is ", e);
            throw new ApplicationException(ErrorCodeConstants.CEW_2036,
                    "invalid.tokenSerialNumber.for.signature.validation");
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
        log.info("AuthorizationServiceImpl >> verifyESign >> status:{}", status);
        log.info("AuthorizationServiceImpl >> verifyESign >> ended");

        return status;
    }

    protected String getTokenSerialNumber(AuthorizeReq authorizeReq, ApiProxy proxy, String companyGroupName,
            String loginUserId) {
        log.info("AuthorizationServiceImpl >> getTokenSerialNumber >> ended");

        String tokenSerialNumber = null;
        TokenInfoBean tokenInfoBean = new TokenInfoBean();
        Map<String, String> mapData = new HashMap<>();
        String tokenVendor = getTokenVendor(authorizeReq);
        String tokenModel = getTokenModel(authorizeReq);
        try {
            TokenTO[] tokenData = proxy.getTokenService()
                    .getAssignedTokensBySegmentAndUserAndTokenModel("", tokenVendor, tokenModel, "", companyGroupName,
                            loginUserId, tokenInfoBean, mapData);
            tokenSerialNumber = tokenData[0].getSerialNum();
            log.info("tokenSerialNumber" + tokenSerialNumber);
        } catch (Exception e) {
            log.error("Exception occurred for getAssignedTokensBySegmentAndUserAndTokenModel: ", e);
            throw new ApplicationException(ErrorCodeConstants.CEW_2038,
                    "sso.failed.for.assigned.tokens.by.segment.and.user.and.tokenModel");
        }
        log.info("AuthorizationServiceImpl >> getTokenSerialNumber >> ended");

        return tokenSerialNumber;
    }

    protected String getCompanyGroupName(String companyId) {
        log.info("AuthorizationServiceImpl >> getCompanyGroupName >> started");

        String companyGroupName = null;
        companyGroupName = submitAuthAesDao.findCompanyGroupNameByCompanyId(companyId);

        log.info("companyGroupName :{}", companyGroupName);

        return companyGroupName;
    }

    protected String getTokenVendor(AuthorizeReq authorizeReq) {
        log.info("AuthorizationServiceImpl >> getTokenVendor >> started");

        String tokenVendor = null;
        if (authorizeReq.getTokenType() != null) {
            String tokenType = authorizeReq.getTokenType().getValue();
            if (AuthorizationConstants.SOFT_TOKEN.equals(tokenType)) {
                tokenVendor = AuthorizationConstants.SOFT_TOKEN_VENDOR;
            } else if (AuthorizationConstants.HARD_TOKEN.equals(tokenType)) {
                tokenVendor = AuthorizationConstants.HARD_TOKEN_VENDOR;
            } else {
                throw new ApplicationException(ErrorCodeConstants.CEW_2039, "invalid.token.type");
            }
        } else {
            throw new ApplicationException(ErrorCodeConstants.CEW_2040, "error.token.type.is.null");
        }
        log.info("AuthorizationServiceImpl >> getTokenVendor >> ended");

        return tokenVendor;
    }

    protected String getTokenModel(AuthorizeReq authorizeReq) {
        log.info("AuthorizationServiceImpl >> getTokenModel >> started");

        String tokenModel = null;
        if (authorizeReq.getTokenType() != null) {
            String tokenType = authorizeReq.getTokenType().getValue();
            if (AuthorizationConstants.SOFT_TOKEN.equals(tokenType)) {
                tokenModel = AuthorizationConstants.SOFT_TOKEN_MODEL;
            } else if (AuthorizationConstants.HARD_TOKEN.equals(tokenType)) {
                tokenModel = AuthorizationConstants.HARD_TOKEN_MODEL;
            } else {
                throw new ApplicationException(ErrorCodeConstants.CEW_2039, "invalid.token.type");
            }
        } else {
            throw new ApplicationException(ErrorCodeConstants.CEW_2040, "error.token.type.is.null");
        }
        log.info("AuthorizationServiceImpl >> getTokenModel >> ended");

        return tokenModel;
    }

    protected AuthorizeResp getTransactionsResponseListForSubmitActionType(AuthorizeResp authorizeRes,
            String bankEntityId, List<String> changeTokens, List<String> transactionIds, List<String> encryptedTrnIds,
            Long userId, Map<String, String> transactionIdMap, boolean isMultiTransaction) {
        log.info("AuthorizationServiceImpl >> getTransactionsResponseListForSubmitActionType >> started");
        List<String> customerStatusList = new ArrayList<>();
        customerStatusList.add(PENDING_VERIFICATION);
        customerStatusList.add(PENDING);
        customerStatusList.add(DRAFT);
        if (!isMultiTransaction) {
            customerStatusList.add(NEW);
            customerStatusList.add(PENDING_SUBMIT);
        }
        customerStatusList.add(PENDING_AUTHORIZATION);
        customerStatusList.add(PENDING_SEND);
        customerStatusList.add(PENDING_REWORK);

        List<TransactionSchema> transactionsResponseList = submitTransactionConfiguration.submitTransactionInfo(
                bankEntityId, changeTokens, transactionIds, userId, TransactionEvent.AUTHORIZED,
                GenericConstants.AUTHORIZED, isMultiTransaction, customerStatusList, true);

        // Step8: Restore the original encrypted transactionIds
        if (!transactionIdMap.isEmpty())
            restoreEncryptedTransactionId(transactionIdMap, transactionsResponseList);
        checkAllTransactionIsPresent(encryptedTrnIds, transactionsResponseList);
        authorizeRes.setTransactions(transactionsResponseList);
        log.info("AuthorizationServiceImpl >> getTransactionsResponseListForSubmitActionType >> ended");

        return authorizeRes;
    }

    /**
     * @param transactionIdMap
     * @param transactionsResponseList
     * @desc restore the original transactionId for all
     */
    protected void restoreEncryptedTransactionId(Map<String, String> transactionIdMap,
            List<TransactionSchema> transactionsResponseList) {
        log.info("AuthorizationServiceImpl >> restoreEncryptedTransactionId >> started");

        for (TransactionSchema transactionSchema : transactionsResponseList) {
            String originalTransactionId = transactionIdMap.get(transactionSchema.getTransactionId());
            transactionSchema.setTransactionId(originalTransactionId);
        }
        log.info("AuthorizationServiceImpl >> restoreEncryptedTransactionId >> ended");
    }

    private String getLoginUserId(String userId) {
        log.info("AuthorizationServiceImpl >> getLoginUserId >> started >> userId:{} ", userId);

        UserResourceFeaturesActionsData userResourceFeaturesActionsData = entitlementService.getResourcesAndFeatures(
                userId, AuthorizationConstants.ENTITLEMENT_RESOURCE_FEATURES_ROUTE_ENDPOINT,
                transactionUtils.camelHeaders());

        String ssoUserId = userResourceFeaturesActionsData.getSsoUserId();
        log.info("****************name1" + ssoUserId);
        log.info("AuthorizationServiceImpl >> getLoginUserId >> ended >> ssoUserId:{}", ssoUserId);

        return ssoUserId;
    }

    protected ChallengeTO getSyncTokenBlobsBySerialNum(ApiProxy proxy, String tokenVendor, String tokenSerialNumber) {
        log.info("AuthorizationServiceImpl >> getSyncTokenBlobsBySerialNum >> tokenVendor :{} tokenSerialNumber:{}",
                tokenVendor, tokenSerialNumber);
        ChallengeTO challengeTO = null;
        try {
            challengeTO = proxy.getTokenService()
                    .syncTokenBlobsBySerialNum("", "", tokenVendor, tokenSerialNumber, new HashMap<>());

        } catch (Exception e) {
            log.error("Exception occurred at tokenSerialNumber for syncTokenBlobs ", e);
            throw new ApplicationException(ErrorCodeConstants.CEW_2037, "sso.failed.for.syncTokenBlobs");
        }
        log.info("AuthorizationServiceImpl >> getSyncTokenBlobsBySerialNum >> ended ");
        return challengeTO;
    }

    protected ChallengeTO getVerifyDigitalSignatureBySerialNum(ApiProxy proxy, String tokenVendor,
            String tokenSerialNumber, String[] challengeCodes, String signature, Map<String, String> params) {
        log.info("AuthorizationServiceImpl >> getVerifyDigitalSignatureBySerialNum >> started >> signature");
        ChallengeTO challengeTO = null;
        try {
            challengeTO = proxy.getTokenService()
                    .verifyDigitalSignatureBySerialNum("", "", tokenVendor, tokenSerialNumber, challengeCodes,
                            signature, params);
        } catch (Exception e) {
            log.error("Exception occurred at signature Validation ", e);
            throw new ApplicationException(ErrorCodeConstants.CEW_2036, "sso.failed.for.signature.validation");
        }
        log.info("AuthorizationServiceImpl >> getVerifyDigitalSignatureBySerialNum >> ended");
        return challengeTO;
    }

    protected void validateAuthorizeReq(AuthorizeReq authorizeReq) {
        if (authorizeReq != null) {
            validateRandomNumber(authorizeReq);
            validatePublicKey(authorizeReq);
            validateOtp(authorizeReq);
            validateChangeToken(authorizeReq);
            validatePublicKeyIndex(authorizeReq);
        }
    }

    protected void validateRandomNumber(AuthorizeReq authorizeReq) {
        if (StringUtils.isEmpty(authorizeReq.getRandomNumber()) || authorizeReq.getRandomNumber().isBlank()) {
            throw new ApplicationException(ErrorCodeConstants.CEW_2047, "error.random.number.is.null.or.empty");
        }
    }

    protected void validatePublicKey(AuthorizeReq authorizeReq) {
        if (StringUtils.isEmpty(authorizeReq.getPublicKey()) || authorizeReq.getPublicKey().isBlank()) {
            throw new ApplicationException(ErrorCodeConstants.CEW_2046, "error.public.key.is.null.or.empty");
        }
    }

    protected void validateOtp(AuthorizeReq authorizeReq) {
        if (StringUtils.isEmpty(authorizeReq.getOtp()) || authorizeReq.getOtp().isBlank()) {
            throw new ApplicationException(ErrorCodeConstants.CEW_2045, "error.otp.is.null.or.empty");
        }
    }

    protected void validateChangeToken(AuthorizeReq authorizeReq) {
        if (!authorizeReq.getTransactionIds().isEmpty()) {
            Optional<String> changeTokenOpt = authorizeReq.getTransactionIds()
                    .stream()
                    .map(TransactionIdSchema::getChangeToken)
                    .findFirst();
            if (!changeTokenOpt.isPresent() || changeTokenOpt.get().isEmpty()) {
                throw new ApplicationException(ErrorCodeConstants.CEW_2044, "error.change.token.is.null.or.empty");
            }
        }
    }

    protected void validatePublicKeyIndex(AuthorizeReq authorizeReq) {
        if (StringUtils.isEmpty(authorizeReq.getPublicKeyIndex()) || authorizeReq.getPublicKeyIndex().isBlank()) {
            throw new ApplicationException(ErrorCodeConstants.CEW_2048, "error.public.key.index.is.null.or.empty");
        }
    }

    // CEW-779, 781, 783 - Due to tight deadline of just 1 day time to close the
    // story.
    // Hence unable to follow proper coding/design practices.
    // The below codes are copied from return/reject EPs. Need to refactor later.
    private AuthorizeResp getTransactionsResponseListForRejectActionType(AuthorizeReq authorizeReq,
            AuthorizeResp authorizeRes, String bankEntityId, List<String> changeTokens, List<String> transactionIds,
            List<String> encryptedTrnIds, Long userId, Map<String, String> transactionIdMap) {
        log.info("AuthorizationServiceImpl >> getTransactionsResponseListForRejectActionType >> started");
        try {
            List<TransactionInfo> transactionList = null;
            List<String> customerStatusList = new ArrayList<>();
            customerStatusList.add(GenericConstants.PENDING_VERIFICATION);
            customerStatusList.add(GenericConstants.PENDING);
            customerStatusList.add(GenericConstants.PENDING_AUTHORIZATION);
            customerStatusList.add(GenericConstants.PENDING_SEND);
            customerStatusList.add(GenericConstants.PENDING_REWORK);
            if (!CollectionUtils.isEmpty(changeTokens) && !CollectionUtils.isEmpty(transactionIds)) {
                transactionList = rejectMandateRequestService.getValidTransactions(transactionIds, changeTokens,
                        customerStatusList, bankEntityId);
            }
            if (transactionList == null || transactionList.isEmpty()) {
                throw new ApplicationException(ErrorCodeConstants.NO_RECORDS_FOUND);
            }
            if (!CollectionUtils.isEmpty(transactionList) && StringUtils.isNotBlank(authorizeReq.getUserComments())) {
                transactionList.stream().forEach(tr -> tr.setUserComments(authorizeReq.getUserComments()));
            }
            List<TransactionSchema> transactionsResponseList = updateRejectMandateRequestUtil
                    .getUpdateRejectMandateRequest(transactionList, userId, bankEntityId, false);
            updateRejectMandateRequestUtil.populateMQCall(transactionsResponseList);
            restoreEncryptedTransactionId(transactionIdMap, transactionsResponseList);
            checkAllTransactionIsPresent(encryptedTrnIds, transactionsResponseList);
            authorizeRes.setTransactions(transactionsResponseList);
            log.info("AuthorizationServiceImpl >> getTransactionsResponseListForRejectActionType >> ended");
        } catch (Exception ex) {
            log.error("Exception occurred on getUpdateRejectMandateRequest method >> ", ex);
            throw new ApplicationException(ErrorCodeConstants.NO_RECORDS_FOUND);
        }

        return authorizeRes;
    }

    private AuthorizeResp getTransactionsResponseListForReturnActionType(AuthorizeReq authorizeReq,
            AuthorizeResp authorizeRes, String bankEntityId, List<String> transactionIds, List<String> encryptedTrnIds,
            Long userId, Map<String, String> transactionIdMap, boolean isMultiTransaction) {
        log.info("AuthorizationServiceImpl >> getTransactionsResponseListForReturnActionType >> started");
        if (StringUtils.isEmpty(authorizeReq.getUserComments())) {
            throw new ApplicationException(ErrorCodeConstants.CEW_2066, "error.usercomments.are.null.or.empty");
        }
        try {
            List<String> customerStatusList = new ArrayList<>();
            customerStatusList.add(GenericConstants.PENDING_VERIFICATION);
            customerStatusList.add(GenericConstants.PENDING_AUTHORIZATION);
            customerStatusList.add(GenericConstants.PENDING_SEND);
            customerStatusList.add(GenericConstants.DRAFT);
            customerStatusList.add(GenericConstants.NEW);
            List<TransactionInfo> transactionList = returnMandateDao.getValidTransactionsToReturn(transactionIds,
                    customerStatusList, bankEntityId);
            if (!CollectionUtils.isEmpty(transactionList)) {
                transactionList.stream().forEach(tr -> tr.setUserComments(authorizeReq.getUserComments()));
            }
            List<TransactionSchema> transactionsResponseList = transactionMandateUtil.getUpdateTransactionMandate(
                    transactionList, userId, bankEntityId, GenericConstants.RETURN, isMultiTransaction);
            log.info("ReturnMandateRequestServiceImpl getTransactionsResponseListForReturnActionType :{} ",
                    transactionsResponseList);
            restoreEncryptedTransactionId(transactionIdMap, transactionsResponseList);
            checkAllTransactionIsPresent(encryptedTrnIds, transactionsResponseList);
            authorizeRes.setTransactions(transactionsResponseList);
        } catch (Exception ex) {
            log.error("Exception occurred on getUpdateTransactionMandateRequest method >> ", ex.getMessage());
            throw new ApplicationException(ErrorCodeConstants.NO_RECORDS_FOUND, "error.fetching.transaction.details");
        }
        return authorizeRes;
    }

    private void checkAllTransactionIsPresent(List<String> encryptedTransactionIds,
            List<TransactionSchema> transactionsResponseList) {
        if (null != encryptedTransactionIds && !encryptedTransactionIds.isEmpty()) {
            for (String encryptedTransactionId : encryptedTransactionIds) {
                Optional<TransactionSchema> matchingTransaction = transactionsResponseList.stream()
                        .filter(tr -> tr.getTransactionId().equals(encryptedTransactionId))
                        .findFirst();
                TransactionSchema transactionSchemaIsPresent = matchingTransaction.orElse(null);
                if (null == transactionSchemaIsPresent) {
                    TransactionSchema transactionSchemaObj = new TransactionSchema();
                    transactionSchemaObj.setTransactionId(transactionUtils.getEncrypted(encryptedTransactionId));
                    transactionSchemaObj.setStatusMessage("Invalid Transaction Id.");
                    transactionsResponseList.add(transactionSchemaObj);
                }
            }
        }
    }

    protected void getTransactionsResponseList(AuthorizeReq authorizeReq, AuthorizeResp authorizeRes,
            List<String> encryptedTrnIds, Map<String, String> transactionIdMap, List<String> transactionIds,
            boolean isMultiTransaction) {
        log.info("AuthorizationServiceImpl getTransactionsResponseList Started");
        if (StringUtils.isBlank(authorizeReq.getActionType())) {
            getTransactionsResponseListForSubmitActionType(authorizeRes, getBankEntity(authorizeReq),
                    getChangeTokenList(authorizeReq), transactionIds, encryptedTrnIds, getDecryptedUsedId(authorizeReq),
                    transactionIdMap, isMultiTransaction);
        } else if (GenericConstants.ACTION_TYPE_SINGLE_RETURN_TEXT.equals(authorizeReq.getActionType())) {
            getTransactionsResponseListForReturnActionType(authorizeReq, authorizeRes, getBankEntity(authorizeReq),
                    transactionIds, encryptedTrnIds, getDecryptedUsedId(authorizeReq), transactionIdMap,
                    isMultiTransaction);
        } else if (GenericConstants.ACTION_TYPE_SINGLE_REJECT_TEXT.equals(authorizeReq.getActionType())) {
            getTransactionsResponseListForRejectActionType(authorizeReq, authorizeRes, getBankEntity(authorizeReq),
                    getChangeTokenList(authorizeReq), transactionIds, encryptedTrnIds, getDecryptedUsedId(authorizeReq),
                    transactionIdMap);
        }
        updateDeleteRecordsForTransactions(authorizeReq, authorizeRes);
        log.info("AuthorizationServiceImpl getTransactionsResponseList Ended");
    }

    protected void updateDeleteRecordsForTransactions(AuthorizeReq authorizeReq, AuthorizeResp authorizeRes) {
        log.info("AuthorizationServiceImpl updateDeleteRecordsForTransactions Start");
        List<TransactionIdSchema> reqTranSchema = authorizeReq.getTransactionIds();
        List<TransactionSchema> tranSchemas = authorizeRes.getTransactions();
        for (TransactionSchema schema : tranSchemas) {
            if (SUCCESS_STATUS_MESSAGE.equals(schema.getStatusMessage())) {
                Optional<TransactionIdSchema> changeTokenList = reqTranSchema.stream()
                        .filter(trn -> trn.getTransactionId().equals(schema.getTransactionId()))
                        .findFirst();
                long transactionId = twsUtils.validateTransactionId(schema.getTransactionId());
                if (changeTokenList.isPresent()) {
                    twsUtils.updateDeleteRecords(transactionId, changeTokenList.get().getChangeToken(),
                            schema.getResourceId(), schema.getBankReferenceNumber());
                }
                if (RPP_MANDATE_TEXT.equals(schema.getResourceId())) {
                    twsUtils.updateAcceptAndTerms(transactionId);
                }
            }
        }
        log.info("AuthorizationServiceImpl updateDeleteRecordsForTransactions End");
    }

    public String[] getMultiSubmitChallengeCodes(List<TransactionInfo> transactionList) {
        log.info("AuthorizationServiceImpl getMultiSubmitChallengeCodes Started");
        String[] multiSubmitChallengeCodes = null;
        try {
            multiSubmitChallengeCodes = new String[]{
                    getMultisubmitChallengeCode.getMultiSubmitChallengeCodeOne(transactionList),
                    getMultisubmitChallengeCode.getMultiSubmitChallengeCodeTwo(transactionList)};
            log.info("Size of multiSubmitChallengeCodes{}-", multiSubmitChallengeCodes.length);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            log.error("Error in getMultiSubmitChallengeCodes Generation", ex);
        }
        log.info("AuthorizationServiceImpl getMultiSubmitChallengeCodes End");
        return multiSubmitChallengeCodes;
    }

    private String getBankEntity(AuthorizeReq authorizeReq) {
        return twsUtils.getDecryptedBankEntityId(authorizeReq.getAdditionalPropValue(GenericConstants.ENTITY_TEXT));
    }

    private Long getDecryptedUsedId(AuthorizeReq authorizeReq) {
        return twsUtils.getDecryptedUserId(authorizeReq.getUserId());
    }

    protected List<String> getChangeTokenList(AuthorizeReq authorizeReq) {
        return authorizeReq.getTransactionIds().stream().map(TransactionIdSchema::getChangeToken).toList();
    }

    private void checkLimitTransactions(AuthorizeReq authorizeReq) {
        if (CollectionUtils.isNotEmpty(authorizeReq.getTransactionIds())
                && authorizeReq.getTransactionIds().size() > GenericConstants.TRANSACTION_MAX_SIZE) {
            throw new ApplicationException(ErrorCodeConstants.CEW_2071, "error.transaction.limit.exceeded");
        }
    }

    private List<String> getEncryptedTrnIds(AuthorizeReq authorizeReq) {
        return authorizeReq.getTransactionIds().stream().map(TransactionIdSchema::getTransactionId).toList();
    }

    public String[] getChallengeCodes(List<TransactionInfo> transactionList) {
        log.info("AuthorizationServiceImpl getChallengeCodes Started");
        String[] challengeCodes = new String[]{getChallengeCodeByTnxAmount(transactionList.get(0)),
                getChallengeCode2(transactionList.get(0))};
        log.info("AuthorizationServiceImpl >> getChallengeCodes >> challengeCode1 :{} and challengeCode2");
        return challengeCodes;
    }

    private String getAppId(AuthorizeReq authorizeReq) {
        return isHardTokenType.test(authorizeReq) ? HARD_TOKEN_APP_ID : SOFT_TOKEN_APP_ID;
    }

    /* To remove duplicate records from response */
    public List<TransactionSchema> getTransactionsResponseListDistinctByBankRefId(
            List<TransactionSchema> transactionsResponseList) {
        List<TransactionSchema> uniqueTransactionsResponseList = null;
        if (Objects.nonNull(transactionsResponseList)) {
            uniqueTransactionsResponseList = transactionsResponseList.stream()
                    .filter(distinctKeyBy(TransactionSchema::getBankReferenceNumber))
                    .toList();
        }
        return uniqueTransactionsResponseList;
    }

    private Predicate<? super TransactionSchema> distinctKeyBy(Function<? super TransactionSchema, Object> key) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return transaction -> map.putIfAbsent(key.apply(transaction), Boolean.TRUE) == null;
    }

    private void setOriginalEncryptionTxnId(List<TransactionInfo> transactionList,
            Map<String, String> transactionIdMap) {
        transactionList.stream()
                .forEach(txn -> txn.setEncryptedTransactionId(transactionIdMap.get(txn.getTransactionId())));

    }
}
