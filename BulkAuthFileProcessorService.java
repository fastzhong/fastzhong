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
package com.uob.gwb.pis.service.impl;

import static com.uob.gwb.pis.common.CctiMappingConstant.*;
import static com.uob.gwb.pis.common.PISConstants.*;
import static com.uob.gwb.pis.common.PISConstants.COUNTRY;
import static com.uob.gwb.pis.common.PISConstants.DATE;
import static com.uob.gwb.pis.common.PISConstants.FILE_NAME;
import static com.uob.gwb.pis.common.PISConstants.FIRST_NAME;
import static com.uob.gwb.pis.common.PISConstants.INDICATIVE;
import static com.uob.gwb.pis.common.PISConstants.LAST_NAME;
import static com.uob.gwb.pis.common.PISConstants.ONE;
import static com.uob.gwb.pis.common.PISConstants.PREBOOK;
import static com.uob.gwb.pis.common.PISConstants.ZERO;
import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uob.gwb.pis.common.CctiMappingConstant;
import com.uob.gwb.pis.config.BatchInsertionTemplate;
import com.uob.gwb.pis.config.CCTIConfig;
import com.uob.gwb.pis.config.LazyBatchSqlSessionTemplate;
import com.uob.gwb.pis.dao.BulkPwsTransactionDao;
import com.uob.gwb.pis.data.mapper.PwsTransactionsMapper;
import com.uob.gwb.pis.domain.*;
import com.uob.gwb.pis.domain.PwsResourceConfigurations;
import com.uob.gwb.pis.domain.PwsTransactions;
import com.uob.gwb.pis.domain.PwsTransitMessage;
import com.uob.gwb.pis.integration.BulkFileUploadEventCreatorService;
import com.uob.gwb.pis.integration.NotificationCreatorService;
import com.uob.gwb.pis.integration.cctimapping.common.*;
import com.uob.gwb.pis.integration.cctimapping.common.Amount;
import com.uob.gwb.pis.integration.cctimapping.common.InstructedAmount;
import com.uob.gwb.pis.integration.cctimapping.common.Other;
import com.uob.gwb.pis.integration.cctimapping.creditor.CreditTransferTransactionInformation;
import com.uob.gwb.pis.integration.cctimapping.forex.ContractUtilisationAmount;
import com.uob.gwb.pis.integration.cctimapping.forex.ExchangeRateInformation;
import com.uob.gwb.pis.integration.cctimapping.forex.ForeignExchange;
import com.uob.gwb.pis.integration.cctimapping.payment.PaymentInformation;
import com.uob.gwb.pis.service.BulkFileProcessor;
import com.uob.gwb.pis.utils.CommonUtil;
import com.uob.gwb.pis.utils.LazyBatchSessionTemplateUtils;
import com.uob.gwb.pre.processing.domain.*;
import com.uob.gwb.pre.processing.domain.Record;
import com.uob.gwb.transaction.common.domain.*;
import com.uob.gwb.transaction.common.domain.Company;
import com.uob.gwb.transaction.common.model.AccountResource;
import com.uob.gwb.transaction.common.model.AmountAllocationRespond;
import com.uob.gwb.transaction.common.model.BankDetailsResp;
import com.uob.gwb.transaction.common.model.BankListLookUpResp;
import com.uob.gwb.transaction.common.model.BankListSchema;
import com.uob.gwb.transaction.common.model.CodeDetails;
import com.uob.gwb.transaction.common.model.CompanyAccountforUser;
import com.uob.gwb.transaction.common.model.CompanyAndAccountsForResourceFeaturesResp;
import com.uob.gwb.transaction.common.model.CompanyAndAccountsForUser;
import com.uob.gwb.transaction.common.model.ComputeEquivalentAmountResp;
import com.uob.gwb.transaction.common.model.Deal;
import com.uob.gwb.transaction.common.model.FxRateDetails;
import com.uob.gwb.transaction.common.model.GetFXRateResp;
import com.uob.gwb.transaction.common.service.CamelService;
import com.uob.gwb.transaction.common.util.CamelUtils;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.utils.model.AxwayResponseData;
import com.uob.gwb.utils.service.GetAxwayTokenService;
import com.uob.ufw.core.exception.ApplicationException;
import com.uob.ufw.core.util.JsonUtil;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.tooling.model.Strings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service("BulkAuthFileProcessorService")
@Slf4j
@AllArgsConstructor
public class BulkAuthFileProcessorService implements BulkFileProcessor {

    LazyBatchSessionTemplateUtils lazyBatchSessionTemplateUtils;

    protected PwsTransactionsMapper pwsTransactionsMapper;

    private TransactionUtils transactionUtils;

    private CommonUtil commonUtil;

    private GetAxwayTokenService getAxwayTokenService;

    private NotificationCreatorService notificationCreatorService;
    private BulkFileUploadEventCreatorService bulkFileUploadEventCreatorService;

    private CCTIConfig cctiConfig;

    private final CamelService camelService;

    @Override
    public String processFile(Exchange exchange) {
        long startFileProcessTime = System.currentTimeMillis();
        String authFileName = (String) exchange.getIn().getHeaders().get(CAMEL_FILE_NAME);
        PwsFileUpload fileUpload = null;
        LazyBatchSqlSessionTemplate lazyBatch = null;
        String fileReferenceId = null;
        String authFileJsonContent = null;
        Company companies = null;
        List<TransactionIdSchema> transactionIdList = new ArrayList<>();

        try {
            authFileJsonContent = exchange.getIn().getBody(String.class);

            lazyBatch = lazyBatchSessionTemplateUtils.createLazyBatchSqlSessionTemplate();
            BulkAuthFileData bulkAuthFileData = commonUtil.convertToBulkAuthFileData(authFileJsonContent);

            GroupHeader groupHeader = bulkAuthFileData.getBusinessDocument().getCustomerCreditTransferInitiation()
                    .getGroupHeader();
            int rejectedTnxCount;
            int noOfTnxCount = 0;
            long userId = 0;
            long transactionId;

            fileReferenceId = groupHeader.getFilereference();
            fileUpload = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .getBulkFileUploadData(fileReferenceId);
            fileUpload.setRejectFileOnError(false);
            Map<String, Object> headers = getHeaders();
            companies = getCompanyInfo(fileUpload, headers);

            String fileStatusCode = groupHeader.getFilestatus();
            if (ZERO_TWO.equals(fileStatusCode) && CollectionUtils.isNotEmpty(groupHeader.getErrordescription())) {
                persistRejectedRecords(fileUpload, groupHeader.getErrordescription(), lazyBatch);
                log.info(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        groupHeader.getErrordescription());
                persistTransitMessageDmpFile(authFileJsonContent, fileReferenceId, INBOUND_SERVICE_TYPE, lazyBatch);
                // END - PERSIST 'PWS_TRANSIT_MESSAGE'
            } else {
                Map<String, String> refIdMetaData = enrichWithHardCodedValues(CROSS_BORDER_UOBSEND);

                userId = Long.parseLong(fileUpload.getCreatedBy());
                List<PaymentInformation> paymentInformationList = bulkAuthFileData.getBusinessDocument()
                        .getCustomerCreditTransferInitiation().getPaymentInformation();

                List<Integer> idsFromSequence = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                        .reserveListBankRefIdFromSequence(paymentInformationList.size());
                log.info("idsFromSequence size::{}", idsFromSequence.size());
                Iterator<Integer> sequenceIterator = idsFromSequence.iterator();
                noOfTnxCount = paymentInformationList.size();

                for (PaymentInformation paymentInformation : paymentInformationList) {
                    if (fileUpload.isRejectFileOnError()) {
                        persistTransitMessageDmpFile(authFileJsonContent, fileReferenceId, INBOUND_SERVICE_TYPE,
                                lazyBatch);
                        persistRejectedRecords(fileUpload,
                                List.of(CUSTOMER_HAS_OPTED_TO_REJECT_WHOLE_FILE_THEN_REJECT_WHOLE_FILE), lazyBatch);
                        log.info(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION,
                                fileUpload.getFileUploadId(),
                                CUSTOMER_HAS_OPTED_TO_REJECT_WHOLE_FILE_THEN_REJECT_WHOLE_FILE);

                        fileStatusCode = ZERO_TWO;
                        transactionIdList = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                                .getTransactionIds(fileUpload.getFileUploadId());
                        deletePwsTransactions(lazyBatch, transactionIdList);
                        break;
                    } else if (ZERO_TWO.equals(paymentInformation.getBulkstatus())
                            && CollectionUtils.isNotEmpty(paymentInformation.getErrordescription())) {
                        fileStatusCode = ZERO_TWO;
                        insertRejectedRecords(fileUpload, lazyBatch, fileReferenceId, authFileJsonContent,
                                paymentInformation);
                    } else {
                        List<String> validationFieldErrorList = new ArrayList<>();

                        List<CreditTransferTransactionInformation> creditTransferTransactionInformation = paymentInformation
                                .getCreditTransferTransactionInformation();

                        String encryptedUserId = transactionUtils.getEncrypted(fileUpload.getCreatedBy());

                        // STEP1 Entitlement check
                        validateUserResourceFeatureActions(fileUpload, paymentInformation, validationFieldErrorList,
                                headers, encryptedUserId);

                        String accountNumber = paymentInformation.getDebtorAccount().getIdentification().getOther()
                                .getIdentification();
                        String transactionCurrency = creditTransferTransactionInformation.stream()
                                .map(CreditTransferTransactionInformation::getAmount).map(Amount::getInstructedAmount)
                                .map(InstructedAmount::getCurrency).findFirst().orElse(null);
                        AccountResource accountResource = validateCompanyAccounts(fileUpload, paymentInformation,
                                validationFieldErrorList, headers, encryptedUserId, accountNumber);
                        // STEP2: Calling v3 entitlement API

                        validateRequestedExecutionDate(paymentInformation, validationFieldErrorList);

                        if (CollectionUtils.isNotEmpty(validationFieldErrorList)) {
                            paymentInformation.setErrordescription(validationFieldErrorList);
                        }

                        if (ZERO_TWO.equals(paymentInformation.getBulkstatus())
                                && CollectionUtils.isNotEmpty(paymentInformation.getErrordescription())) {
                            fileStatusCode = ZERO_TWO;
                            insertRejectedRecords(fileUpload, lazyBatch, fileReferenceId, authFileJsonContent,
                                    paymentInformation);
                        } else {
                            String accountCurrency = accountResource.getAccountCurrency();
                            String bankRefNumber = commonUtil.generateBankRefId(LocalDateTime.now(),
                                    sequenceIterator.next(), refIdMetaData);

                            // START - PERSIST 'PWS_TRANSIT_MESSAGE'
                            String correlationId = getBulkMsgIdr(bankRefNumber);
                            String messageRefNo = randomUUID().toString();
                            persistTransitMessageForDmpFile(authFileJsonContent, correlationId, bankRefNumber,
                                    messageRefNo, lazyBatch);

                            PwsTransactions pwsTransactions = pwsTransactionsMapper.getPwsTransactions(bankRefNumber,
                                    fileUpload);
                            pwsTransactions.setInitiatedBy(userId);
                            pwsTransactions.setAccountNumber(accountNumber);
                            pwsTransactions.setCompanyId(fileUpload.getCompanyId());
                            pwsTransactions.setTotalChild(creditTransferTransactionInformation.size());
                            pwsTransactions.setTransactionCurrency(transactionCurrency);
                            pwsTransactions.setAccountCurrency(accountCurrency);

                            setCompanyNameAndGroupId(pwsTransactions, companies);

                            setTotalAmount(creditTransferTransactionInformation, pwsTransactions);

                            // insert in 'PWS_TRANSACTIONS'
                            lazyBatch.insertAndFlush(BATCH_INSERT_PWS_TRANSACTION, pwsTransactions);
                            transactionId = pwsTransactions.getTransactionId();
                            log.info("TransactionId ::{}", transactionId);

                            PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
                            pwsBulkTransactions.setTransactionId(transactionId);
                            pwsBulkTransactions
                                    .setRecipientsReference(paymentInformation.getPaymentInformationIdentification());
                            pwsBulkTransactions.setChangeToken(pwsTransactions.getChangeToken());
                            String bulkStatus = getBulkStatus(paymentInformation.getBulkstatus());
                            pwsBulkTransactions.setStatus(bulkStatus);
                            pwsBulkTransactions.setFileUploadId(fileUpload.getFileUploadId());
                            pwsBulkTransactions.setDmpBatchNumber(paymentInformation.getDMPBatchRef());
                            setbatchBooking(paymentInformation, pwsBulkTransactions);

                            if (StringUtils.isNotEmpty(paymentInformation.getRequestedExecutionDate().getDate())) {
                                LocalDate transferDate = LocalDate.parse(
                                        paymentInformation.getRequestedExecutionDate().getDate().substring(0, 10));
                                pwsBulkTransactions.setTransferDate(transferDate);
                            }

                            // insert in 'PWS_BULK_TRANSACTIONS'
                            lazyBatch.insertAndFlush(BATCH_INSERT_PWS_BULK_TRANSACTION, pwsBulkTransactions);

                            rejectedTnxCount = insertPwsBulkTransactionInstructions(paymentInformation, fileUpload,
                                    lazyBatch, headers, pwsTransactions, accountResource);
                            fileUpload.setBankReferenceId(bankRefNumber);
                            setFileStatus(fileUpload, rejectedTnxCount);

                            List<TransactionIdSchema> transactionIds = new ArrayList<>();
                            TransactionIdSchema transactionIdObj = new TransactionIdSchema();
                            transactionIdObj.setChangeToken(String.valueOf(pwsTransactions.getChangeToken()));
                            transactionIdObj.setTransactionId(String.valueOf(transactionId));
                            transactionIds.add(transactionIdObj);
                            updateWithComputedValues(lazyBatch, transactionIds, bulkStatus);
                            lazyBatch.flushStatements();
                        }
                    }
                }
            }
            transactionIdList = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .getTransactionIds(fileUpload.getFileUploadId());
            String fileProcessingStatus = getFileProcessingStatus(fileStatusCode, fileUpload);

            String status = getPwsFileUplaodStatus(noOfTnxCount, transactionIdList, fileProcessingStatus);
            fileUpload.setStatus(status);
            fileUpload.setUpdatedBy(String.valueOf(userId));
            lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class).updateFileUplaodStatus(fileUpload);
            log.info("updateFileUploadStatus fileReference:{} with Status:{}", fileReferenceId, fileUpload.getStatus());

            if (!isRejectOrUploadFailed.test(fileUpload.getStatus())) {-

                updateDerivedValueDate(lazyBatch, transactionIdList);

                boolean isFileReject = getDuplicateCheckValidation(userId, fileUpload, lazyBatch, headers);

                updateFileUploadDetails(lazyBatch, fileUpload, isFileReject, transactionIdList, userId);
            }

            insertFileUploadFailedAdits(fileUpload, companies, lazyBatch);

            sendNotificationForBulkUpload(fileReferenceId, fileUpload, companies);
        } catch (Exception ex) {
            log.error("Exception occurred: with error while processing _Auth file: {} ", authFileName);
            log.error(ex.getMessage(), ex);

            persistRejectedRecords(fileUpload,
                    List.of("Exception occurred while processing _Auth file." + ex.getMessage()), lazyBatch);
            log.info(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    "Exception occurred while processing _Auth file.");
            persistTransitMessageDmpFile(authFileJsonContent, fileReferenceId, INBOUND_SERVICE_TYPE, lazyBatch);

            fileUpload.setFileReferenceId(fileReferenceId);
            fileUpload.setStatus(UPLOAD_FAILED);
            fileUpload.setUpdatedBy(fileUpload.getCreatedBy());
            lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class).updateFileUplaodStatus(fileUpload);
            log.info("updateFileUploadStatus fileReference:{} with Status:{}", fileReferenceId, UPLOAD_FAILED);

            deletePwsTransactions(lazyBatch, transactionIdList);

            insertFileUploadFailedAdits(fileUpload, companies, lazyBatch);

            sendNotificationForBulkUpload(fileReferenceId, fileUpload, companies);
        }
        long endFileProcessTime = System.currentTimeMillis();
        log.info("Auth file: {} total execution time: {} ms", authFileName,
                (endFileProcessTime - startFileProcessTime));
        log.info("DMP Auth file: {} processed successfully", authFileName);
        return SUCCESS;
    }

    private void setbatchBooking(PaymentInformation paymentInformation, PwsBulkTransactions pwsBulkTransactions) {
        log.info("isBatchBooking: {}", paymentInformation.isBatchBooking());
        if (paymentInformation.isBatchBooking()) {
            pwsBulkTransactions.setBatchBooking(BATCH_BOOKING_Y);
        } else {
            pwsBulkTransactions.setBatchBooking(BATCH_BOOKING_N);
        }

    }

    private void setFileStatus(PwsFileUpload fileUpload, int rejectedTnxCount) {
        if (rejectedTnxCount != 0) {
            fileUpload.setStatus(PARTIAL);
        }
    }

    private void insertFileUploadFailedAdits(PwsFileUpload fileUpload, Company companies,
            LazyBatchSqlSessionTemplate lazyBatch) {
        if (UPLOAD_FAILED.equals(fileUpload.getStatus())) {
            PwsTransactions pwsTransactions = new PwsTransactions();
            pwsTransactions.setBankReferenceId(fileUpload.getFileReferenceId());
            pwsTransactions.setResourceId(fileUpload.getResourceId());
            pwsTransactions.setInitiatedBy(Long.parseLong(fileUpload.getCreatedBy()));
            if (Objects.nonNull(companies) && Objects.nonNull(companies.getCompanyGroupId())) {
                String comapnyGroupId = transactionUtils.getDecrypted(companies.getCompanyGroupId());
                pwsTransactions.setCompanyGroupId(Long.parseLong(comapnyGroupId));
                String comapnyId = transactionUtils.getDecrypted(companies.getCompanyId());
                pwsTransactions.setCompanyId(Long.parseLong(comapnyId));
            }
            insertPwsTransactionAuditsDetails(lazyBatch, pwsTransactions);
        }
    }

    private void updateFileUploadDetails(LazyBatchSqlSessionTemplate lazyBatch, PwsFileUpload fileUpload,
            boolean isFileReject, List<TransactionIdSchema> transactionIdList, long userId) {
        if (isFileReject) {
            fileUpload.setStatus(UPLOAD_FAILED);
            fileUpload.setBankReferenceId(null);
            fileUpload.setTransactionId(null);
            fileUpload.setUpdatedBy(String.valueOf(userId));
            lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class).updateFileUplaodStatus(fileUpload);
        } else {
            // update the Computed Values(noOfTrnx, totalAmount, highestAmount) when File
            // status is PARTIAL
            updateWithComputedValues(lazyBatch, transactionIdList, fileUpload.getStatus());
            lazyBatch.flushStatements();
            createBulkEvent(userId, transactionIdList, isFileReject); // publish bulk event to MQ
        }

    }

    private void validateRequestedExecutionDate(PaymentInformation paymentInformation,
            List<String> validationFieldErrorList) {
        if (StringUtils.isNotEmpty(paymentInformation.getRequestedExecutionDate().getDate())) {
            LocalDate transferDate = LocalDate
                    .parse(paymentInformation.getRequestedExecutionDate().getDate().substring(0, 10));
            LocalDate systemDate = LocalDateTime.now().toLocalDate();
            if (systemDate.isAfter(transferDate)) {
                paymentInformation.setBulkstatus(ZERO_TWO);
                validationFieldErrorList.add("RequestedExecutionDate is less than the system date");
            }
        }
    }

    private void mangeTransactionDetails(LazyBatchSqlSessionTemplate lazyBatch, String transactionId) {
        PwsRejectedRecord rejectedRecord = new PwsRejectedRecord();
        rejectedRecord.setBankReferenceId(EMPTY);
        rejectedRecord.setTransactionId(ZERO);
        rejectedRecord.setRejectedRecordId(Long.parseLong(transactionId));
        lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class).updateRejectedRecord(rejectedRecord);
        deleteTransactions(lazyBatch, Long.parseLong(transactionId));
    }

    private void updateDerivedValueDate(LazyBatchSqlSessionTemplate lazyBatch,
            List<TransactionIdSchema> transactionIdList) {
        for (TransactionIdSchema transactionIdObj : transactionIdList) {
            PwsTransactions pwsTransactions = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .getTransactionDetailsForAudits(transactionIdObj.getTransactionId());

            ProcessApiResponseList processApiResponse = commonUtil.getprocessExecutionOutline(pwsTransactions);
            updateFessCurrencyAndAmount(lazyBatch, processApiResponse);
            updateValudedate(lazyBatch, transactionIdObj, pwsTransactions, processApiResponse);

        }
    }

    private void updateValudedate(LazyBatchSqlSessionTemplate lazyBatch, TransactionIdSchema transactionIdObj,
            PwsTransactions pwsTransactions, ProcessApiResponseList processApiResponse) {
        if (commonUtil.getValueDateRequired()) {
            String valueDate = commonUtil.getValueDate(pwsTransactions);
            if (Objects.nonNull(valueDate)) {
                PwsTransactions pwsTransaction = new PwsTransactions();
                pwsTransaction.setTrxnId(transactionIdObj.getTransactionId());
                pwsTransaction.setTransferDate(valueDate);
                lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class).updateValueDate(pwsTransactions);
            }
        } else {
            updateTransferDate(lazyBatch, transactionIdObj, pwsTransactions, processApiResponse);
        }
    }

    private void updateTransferDate(LazyBatchSqlSessionTemplate lazyBatch, TransactionIdSchema transactionIdObj,
            PwsTransactions pwsTransactions, ProcessApiResponseList processApiResponse) {
        if (Objects.nonNull(processApiResponse)
                && CollectionUtils.isNotEmpty(processApiResponse.getTransactionInformationEvaluations())) {
            List<ProcessingApiResponse> processingApiResponses = processApiResponse
                    .getTransactionInformationEvaluations();
            updateValueDt(lazyBatch, transactionIdObj, pwsTransactions, processingApiResponses);
        }
    }

    private void updateValueDt(LazyBatchSqlSessionTemplate lazyBatch, TransactionIdSchema transactionIdObj,
            PwsTransactions pwsTransactions, List<ProcessingApiResponse> processingApiResponses) {
        if (CollectionUtils.isNotEmpty(processingApiResponses)) {
            for (ProcessingApiResponse processingApiResponse : processingApiResponses) {
                AccountingOutline accountingOutline = processingApiResponse.getAccountingOutline();
                updatePwsValueDate(lazyBatch, transactionIdObj, pwsTransactions, accountingOutline);
            }
        }
    }

    private void updatePwsValueDate(LazyBatchSqlSessionTemplate lazyBatch, TransactionIdSchema transactionIdObj,
            PwsTransactions pwsTransactions, AccountingOutline accountingOutline) {
        if (Objects.nonNull(accountingOutline)) {
            String valueDt = accountingOutline.getOriginatingEntryValueDate().getDate();
            if (Objects.nonNull(valueDt)) {
                PwsTransactions pwsTransaction = new PwsTransactions();
                pwsTransaction.setTrxnId(transactionIdObj.getTransactionId());
                pwsTransaction.setTransferDate(valueDt);
                lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class).updateValueDate(pwsTransactions);
            }
        }
    }

    private void updateFessCurrencyAndAmount(LazyBatchSqlSessionTemplate lazyBatch,
            ProcessApiResponseList processApiResponse) {
        if (Objects.nonNull(processApiResponse)) {
            List<ProcessingApiResponse> processingApiResponses = processApiResponse
                    .getTransactionInformationEvaluations();
            if (CollectionUtils.isNotEmpty(processingApiResponses)) {
                updateTransactionCharges(lazyBatch, processingApiResponses);
            }
        }
    }

    private void updateTransactionCharges(LazyBatchSqlSessionTemplate lazyBatch,
            List<ProcessingApiResponse> processingApiResponses) {
        for (ProcessingApiResponse processingApiResponse : processingApiResponses) {
            ChargesOutline chargesOutline = processingApiResponse.getChargesOutline();
            if (Objects.nonNull(chargesOutline) && Objects.nonNull(processingApiResponse.getOriginalExtension())) {
                String childBankreference = processingApiResponse.getOriginalExtension().getTxidValue();
                List<Record> records = chargesOutline.getRecords();
                if (CollectionUtils.isNotEmpty(records) && !Strings.isNullOrEmpty(childBankreference)) {
                    for (Record myRecord : records) {
                        String feesAmount = myRecord.getAmount().getAmount();
                        String feesCurrency = myRecord.getAmount().getCurrency();
                        PwsTransactionCharges transactionCharges = new PwsTransactionCharges();
                        transactionCharges.setChildBankReferenceId(childBankreference);
                        transactionCharges.setFeesAmount(new BigDecimal(feesAmount));
                        transactionCharges.setFeesCurrency(feesCurrency);
                        lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                                .updatePwsTransactionCharges(transactionCharges);
                    }
                }
            }
        }
    }

    private void insertRejectedRecords(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            String fileReferenceId, String authFileJsonContent, PaymentInformation paymentInformation) {
        persistRejectedRecords(fileUpload, paymentInformation.getErrordescription(), lazyBatch);
        log.info(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                paymentInformation.getErrordescription());
        persistTransitMessageDmpFile(authFileJsonContent, fileReferenceId, INBOUND_SERVICE_TYPE, lazyBatch);
        // END - PERSIST 'PWS_TRANSIT_MESSAGE'
    }

    private void insertPwsTransactionAuditsDetails(LazyBatchSqlSessionTemplate lazyBatch,
            PwsTransactions pwsTransactions) {
        PwsTransactionAudits transactionAudits = new PwsTransactionAudits();
        transactionAudits.setBankEntityId(pwsTransactions.getBankEntityId());
        transactionAudits.setBankReferenceId(pwsTransactions.getBankReferenceId());
        transactionAudits.setChangeToken(pwsTransactions.getChangeToken());
        transactionAudits.setCompanyGroupId(pwsTransactions.getCompanyGroupId());
        transactionAudits.setCompanyId(pwsTransactions.getCompanyId());
        transactionAudits.setCurrentState(CREATED);
        transactionAudits.setUserId(pwsTransactions.getInitiatedBy());
        transactionAudits.setResourceId(pwsTransactions.getResourceId());
        transactionAudits.setTransactionId(pwsTransactions.getTransactionId());

        lazyBatch.insertAndFlush(BATCH_INSERT_PWS_AUDIT_TRAIL, transactionAudits);
    }

    private void deletePwsTransactions(LazyBatchSqlSessionTemplate lazyBatch,
            List<TransactionIdSchema> transactionIdList) {
        if (CollectionUtils.isNotEmpty(transactionIdList)) {
            for (TransactionIdSchema transactionIds : transactionIdList) {
                log.info("Rollback the failed transactions Id:{} ", transactionIds.getTransactionId());
                deleteTransactions(lazyBatch, Long.parseLong(transactionIds.getTransactionId()));
            }
        }
    }

    private boolean getDuplicateCheckValidation(long userId, PwsFileUpload pwsFileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, Map<String, Object> headers) {
        boolean isFileReject = false;

        List<TransactionIdSchema> transactionIdList = lazyBatch.getSessionTemplate()
                .getMapper(BulkPwsTransactionDao.class).getTransactionIds(pwsFileUpload.getFileUploadId());

        if (!REJECTED.equals(pwsFileUpload.getStatus()) && CollectionUtils.isNotEmpty(transactionIdList)) {
            DuplicateCheckReq duplicateCheckReq = getDuplicateCheckRequest(userId, transactionIdList, pwsFileUpload,
                    lazyBatch);
            log.info("DuplicateCheckReq: {}", JsonUtil.toJsonString(duplicateCheckReq));
            DuplicateCheckResp duplicateCheckResp = null;
            try {
                Exchange exchange = camelService.callEndpoint(DUPLICATE_CHECK_ROUTE_ENDPOINT,
                        JsonUtil.toJsonString(duplicateCheckReq), headers);
                CamelUtils.throwCamelExchangeExceptionIfAny(exchange);
                String responseBody = exchange.getIn().getBody(String.class);
                log.info("DuplicateCheckResp: {}", responseBody);
                duplicateCheckResp = commonUtil.convertToDuplicateCheckResp(responseBody);
            } catch (Exception e) {
                log.error("DuplicateCheckValidation getting Exception", e);
            }

            if (Objects.nonNull(duplicateCheckResp)
                    && CollectionUtils.isNotEmpty(duplicateCheckResp.getInstructions())) {
                List<InstructionSchema> instructionSchemas = duplicateCheckResp.getInstructions();
                log.info("isFileReject: {}", duplicateCheckResp.getIsFileReject());
                if (Boolean.TRUE.equals(duplicateCheckResp.getIsFileReject())) {
                    isFileReject = true;
                    updatePwsTransactionStatus(lazyBatch, instructionSchemas, pwsFileUpload);
                } else {
                    isFileReject = updateTransactionLevelDuplicate(lazyBatch, instructionSchemas, pwsFileUpload);
                }
            }
        }
        return isFileReject;
    }

    private boolean updateTransactionLevelDuplicate(LazyBatchSqlSessionTemplate lazyBatch,
            List<InstructionSchema> instructionSchemas, PwsFileUpload pwsFileUpload) {
        boolean isFileReject = false;
        for (InstructionSchema instruction : instructionSchemas) {
            List<TransactionSchema> transactionSchemas = instruction.getTransactions();
            for (TransactionSchema transactionSchema : transactionSchemas) {
                if (Boolean.TRUE.equals(transactionSchema.getIsDuplicate())) {
                    String childBankReferenceId = transactionSchema.getChildBankReferenceId();
                    PwsTransactions transactions = new PwsTransactions();
                    transactions.setChildBankReferenceId(childBankReferenceId);
                    transactions.setStatus(DELETED);
                    transactions.setRejectReason(DUPLICATE_TRANSACTION);
                    transactions.setRejectCode(CEW_8803);
                    lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                            .updatePwsBulkTransactionInstructionsStatus(transactions);
                    log.info("updatePwsBulkTransactionInstructionsStatus Status: {}", DELETED);

                    pwsFileUpload.setBankReferenceId(instruction.getBankReferenceId());
                    pwsFileUpload.setRejectCode(CEW_8803);
                    persistRejectedRecords(pwsFileUpload, List.of(DUPLICATE_TRANSACTION), lazyBatch);
                }
            }
            List<String> childStatusList = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .getChildStatus(instruction.getBankReferenceId());
            log.info("ChildStatusList: {}", childStatusList);
            String finalStatusForParent = getFinalStatusForParent(childStatusList);
            log.info("FinalStatusForParent: {}", finalStatusForParent);
            isFileReject = updateFinalStatus(lazyBatch, pwsFileUpload, instruction, finalStatusForParent);
        }
        return isFileReject;
    }

    private boolean updateFinalStatus(LazyBatchSqlSessionTemplate lazyBatch, PwsFileUpload pwsFileUpload,
            InstructionSchema instruction, String finalStatusForParent) {
        boolean isFileReject = false;
        if (DELETED.equals(finalStatusForParent)) {
            isFileReject = true;
            updateTransaction(lazyBatch, instruction);
        } else if (PARTIAL.equals(finalStatusForParent)) {
            if (isZeroOne.test(pwsFileUpload.getStringValue())) {
                isFileReject = true;
                updateTransaction(lazyBatch, instruction);
            } else {
                pwsFileUpload.setStatus(PARTIAL);
                pwsFileUpload.setUpdatedBy(pwsFileUpload.getUpdatedBy());
                lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                        .updateFileUplaodStatus(pwsFileUpload);
            }
        }
        return isFileReject;
    }

    private String getFinalStatusForParent(List<String> childStatusList) {
        String finalStatusForParent = null;

        if (CollectionUtils.isEmpty(childStatusList)) {
            return finalStatusForParent;
        }

        if (isAllChildDeleted(childStatusList)) {
            finalStatusForParent = DELETED;
        } else if (isAtleastOneChildDeleted(childStatusList)) {
            finalStatusForParent = PARTIAL;
        }
        return finalStatusForParent;
    }

    private static boolean isAllChildDeleted(List<String> childStatusList) {
        return childStatusList.stream().allMatch(DELETED::equals);
    }

    private static boolean isAtleastOneChildDeleted(List<String> childStatusList) {
        return childStatusList.stream().anyMatch(PENDING_STATUS::equals)
                && childStatusList.stream().anyMatch(DELETED::contains);
    }

    private void updatePwsTransactionStatus(LazyBatchSqlSessionTemplate lazyBatch,
            List<InstructionSchema> instructionSchemas, PwsFileUpload pwsFileUpload) {
        for (InstructionSchema instruction : instructionSchemas) {
            updateTransaction(lazyBatch, instruction);
            String bankReferenceId = instruction.getBankReferenceId();
            pwsFileUpload.setBankReferenceId(bankReferenceId);
            pwsFileUpload.setRejectCode(CEW_8803);
            persistRejectedRecords(pwsFileUpload, List.of(DUPLICATE_TRANSACTION), lazyBatch);
        }
    }

    private void updateTransaction(LazyBatchSqlSessionTemplate lazyBatch, InstructionSchema instruction) {
        String bankReferenceId = instruction.getBankReferenceId();
        PwsTransactions transactions = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .getPwsTransactionId(bankReferenceId);
        transactions.setCustomerTransactionStatus(DELETED);
        transactions.setRejectReason(DUPLICATE_TRANSACTION);
        lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .updatePwsTransactionCustomerStatus(transactions);
        log.info("updatePwsTransactions Status: {}", DELETED);

        transactions.setStatus(DELETED);
        transactions.setRejectReason(DUPLICATE_TRANSACTION);
        transactions.setRejectCode(CEW_8803);
        lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .updatePwsBulkTransactionStatus(transactions);
        log.info("updatePwsBulkTransactions Status: {}", DELETED);
    }

    private DuplicateCheckReq getDuplicateCheckRequest(long userId, List<TransactionIdSchema> transactionIdList,
            PwsFileUpload pwsFileUpload, LazyBatchSqlSessionTemplate lazyBatch) {
        DuplicateCheckReq duplicateCheckReq = new DuplicateCheckReq();
        duplicateCheckReq.setUserId(transactionUtils.getEncrypted(String.valueOf(userId)));
        duplicateCheckReq.setFeatureId(List.of(pwsFileUpload.getFeatureId()));
        duplicateCheckReq.setResourceIds(List.of(pwsFileUpload.getResourceId()));
        duplicateCheckReq.setFileReferenceId(pwsFileUpload.getFileReferenceId());
        List<Instruction> instructionDetails = new ArrayList<>();
        for (TransactionIdSchema schema : transactionIdList) {
            List<PwsTransactions> pwsTransactions = lazyBatch.getSessionTemplate()
                    .getMapper(BulkPwsTransactionDao.class).getTransactionDetails(schema.getTransactionId());
            if (CollectionUtils.isNotEmpty(pwsTransactions)) {
                Timestamp timeStamp = pwsTransactions.get(0).getInitiationTime();
                LocalDate localDate = timeStamp.toLocalDateTime().toLocalDate();
                duplicateCheckReq.setCreationDateTime(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                duplicateCheckReq.setNumberOfTransactions(String.valueOf(pwsTransactions.get(0).getTotalChild()));
                Instruction instruction = new Instruction();
                instruction.setBankReferenceId(pwsTransactions.get(0).getBankReferenceId());
                instruction.setDebtorAccountNumber(pwsTransactions.get(0).getAccountNumber());
                List<Transaction> transactionDetails = new ArrayList<>();
                for (PwsTransactions transactions : pwsTransactions) {
                    Transaction transaction = new Transaction();
                    transaction.setBankCode(transactions.getBankCode());
                    transaction.setChildBankReferenceId(transactions.getChildBankReferenceId());
                    transaction.setCreditorAccountNumber(transactions.getPartyAccountNumber());
                    transaction.setCustomerReference(transactions.getCustomerReference());
                    transaction.setSwiftCode(transactions.getSwiftCode());
                    transaction.setTransactionAmount(transactions.getTransactionAmount());
                    transaction.setTransactionCurrency(transactions.getTransactionCurrency());
                    Timestamp valueDate = Timestamp.valueOf(transactions.getValueDate());
                    LocalDate localValueDate = valueDate.toLocalDateTime().toLocalDate();
                    transaction.setValueDate(localValueDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    transactionDetails.add(transaction);
                }
                instruction.setTransactionDetails(transactionDetails);
                instructionDetails.add(instruction);
            }
        }
        duplicateCheckReq.setInstructionDetails(instructionDetails);
        return duplicateCheckReq;
    }

    private void sendNotificationForBulkUpload(String fileReferenceId, PwsFileUpload fileUpload, Company companies) {
        sendNotificationForFileUpload(fileUpload, fileReferenceId, companies);
    }

    private void createBulkEvent(long userId, List<TransactionIdSchema> transactionIdList, boolean isFileReject) {
        // Publish Bulk-File-Upload-TWS-Event to MQ -->
        // "UOB.CEW.SIT4.MYPIS2TWSINTERNALQ"
        if (!isFileReject && CollectionUtils.isNotEmpty(transactionIdList)) {
            BulkFileUploadTransactionInfo bulkFileUploadTransactionInfo = new BulkFileUploadTransactionInfo();
            bulkFileUploadTransactionInfo.setBankEntityId(ENTITY_ID);
            bulkFileUploadTransactionInfo.setUserId(String.valueOf(userId));
            bulkFileUploadTransactionInfo.setTransactionIds(transactionIdList);
            sendBulkFileUploadTwsEvent(bulkFileUploadTransactionInfo);
            log.info("Transactions::{} for users:: {} pushed to MQ(UOB.CEW.SIT4.MYPIS2TWSINTERNALQ) Successfully. ",
                    transactionIdList, userId);
        }
    }

    /**
     *
     * @param bulkFileUploadTransactionInfo
     *            bulkFileUploadTransactionInfo
     */
    protected void sendBulkFileUploadTwsEvent(BulkFileUploadTransactionInfo bulkFileUploadTransactionInfo) {
        try {
            bulkFileUploadEventCreatorService.publishBulkFileUploadTwsEvent(bulkFileUploadTransactionInfo);
            String json = commonUtil.convertBulkFileUploadTransactionInfoToJson(bulkFileUploadTransactionInfo);
            log.info("BulkFileUploadTwsEvent: {}", json);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred in sendBulkFileUploadTwsEvent: {} , BulkFileUploadTwsEvent:{}", e,
                    bulkFileUploadTransactionInfo);
        }
    }

    private void sendNotificationForFileUpload(PwsFileUpload fileUpload, String fileReferenceId, Company companies) {
        NotificationMessage notificationMessage = getNotificationMessageRequest(fileUpload, fileReferenceId, companies);
        log.info("sendNotificationForFileUpload notificationMessage: {}", notificationMessage);
        notificationCreatorService.publishMessage(notificationMessage);
    }

    private NotificationMessage getNotificationMessageRequest(PwsFileUpload fileUpload, String fileReferenceId,
            Company companies) {
        NotificationMessage notificationMessage = new NotificationMessage();
        notificationMessage.setNotifyEvent(FILE_UPLOAD_NOTIFY_EVENT);
        notificationMessage.setUserId(fileUpload.getCreatedBy());
        notificationMessage.setTransactionId(String.valueOf(fileUpload.getFileUploadId()));
        notificationMessage.setFileReferenceNumber(fileReferenceId);
        notificationMessage.setFileName(fileUpload.getFileName());
        notificationMessage.setStatus(fileUpload.getStatus());
        notificationMessage.setResourceId(fileUpload.getResourceId());
        notificationMessage.setFeatureId(fileUpload.getFeatureId());
        notificationMessage.setBankEntityId(ENTITY_ID);
        notificationMessage.setCompanyId(String.valueOf(fileUpload.getCompanyId()));

        if (UPLOAD_FAILED.equals(fileUpload.getStatus())) {
            notificationMessage.setReferenceId(fileReferenceId);
        } else {
            notificationMessage.setReferenceId(fileUpload.getBankReferenceId());
        }

        if (Objects.nonNull(companies) && Objects.nonNull(companies.getCompanyGroupId())) {
            String comapnyGroupId = transactionUtils.getDecrypted(companies.getCompanyGroupId());
            notificationMessage.setGroupId(comapnyGroupId);
        }
        notificationMessage.setGroupId(GROUP_ID);
        List<Event> eventList = new ArrayList<>();
        Event event = new Event();
        event.setNotifyEvent(FILE_UPLOAD_NOTIFY_EVENT);
        event.setNotificationType(CctiMappingConstant.APPROVAL_STATUS);
        eventList.add(event);
        notificationMessage.setEvents(eventList);
        Map<String, String> data = new HashMap<>();
        data.put(CctiMappingConstant.FILE_REFERENCE_NUMBER, fileReferenceId);
        data.put(FILE_NAME, fileUpload.getFileName());
        data.put(CctiMappingConstant.STATUS, fileUpload.getStatus());
        data.put(DATE, String.valueOf(LocalDateTime.now()));
        data.put(FIRST_NAME, fileUpload.getFirstName());
        data.put(LAST_NAME, fileUpload.getLastName());
        notificationMessage.setData(data);
        return notificationMessage;
    }

    private void updateWithComputedValues(LazyBatchSqlSessionTemplate lazyBatch,
            List<TransactionIdSchema> transactionIdList, String status) {
        for (TransactionIdSchema transactionIds : transactionIdList) {
            PwsTransactions pwsTransactions = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .getDataFromBulkTransactionInstructions(Long.parseLong(transactionIds.getTransactionId()));
            pwsTransactions.setTransactionId(Long.parseLong(transactionIds.getTransactionId()));
            lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .updateWithComputedValues(pwsTransactions);
            log.info("updateWithComputedValues with Status:{} and transactionId:{}", status,
                    transactionIds.getTransactionId());
        }
    }

    private void persistTransitMessageForDmpFile(String json, String correlationId, String bankRefNumber,
            String messageRefNo, BatchInsertionTemplate batchSqlSession) {
        PwsTransitMessage pwsTransitMessage = new PwsTransitMessage();
        pwsTransitMessage.setCorrelationId(correlationId);
        pwsTransitMessage.setBankReferenceId(bankRefNumber);
        if (StringUtils.isNoneEmpty(messageRefNo)) {
            pwsTransitMessage.setMessageRefNo(messageRefNo);
        } else {
            pwsTransitMessage.setMessageRefNo(BLANK);
        }
        pwsTransitMessage.setRetryCount(0);
        pwsTransitMessage.setMessageContent(json.getBytes());
        pwsTransitMessage.setServiceType(INBOUND_SERVICE_TYPE);
        pwsTransitMessage.setStatus(COMPLETED);
        pwsTransitMessage.setEndSystem(DMP);
        batchSqlSession.insertToQueue(BATCH_PERSIST_TRANSIT_MESSAGE, pwsTransitMessage);
    }

    private AccountResource validateCompanyAccounts(PwsFileUpload fileUpload, PaymentInformation paymentInformation,
            List<String> validationFieldErrorList, Map<String, Object> headers, String encryptedUserId,
            String accountNumber) {
        CompanyAndAccountsForResourceFeaturesResp v3EntitlementServiceResponse = commonUtil.v3EntitlementEndPointCall(
                encryptedUserId, fileUpload.getResourceId(), fileUpload.getFeatureId(), headers);
        log.info("EntitlementServiceResponse: {} ", v3EntitlementServiceResponse);
        AccountResource accountResource = null;
        if (ObjectUtils.isEmpty(v3EntitlementServiceResponse)) {
            paymentInformation.setBulkstatus(ZERO_TWO);
            validationFieldErrorList.add("Resource and Features is not matched with company accounts");
        } else {
            List<CompanyAccountforUser> companyAccountforUsers = v3EntitlementServiceResponse
                    .getCompaniesAccountResourceFeature().stream().map(CompanyAndAccountsForUser::getCompanies)
                    .findAny().orElse(null);
            if (CollectionUtils.isNotEmpty(companyAccountforUsers)) {
                List<AccountResource> accountResources = companyAccountforUsers.stream()
                        .filter(company -> validateComapnyId(company, fileUpload))
                        .map(CompanyAccountforUser::getAccounts).findAny().orElse(null);
                if (CollectionUtils.isNotEmpty(accountResources)) {
                    accountResource = accountResources.stream()
                            .filter(resource -> resource.getAccountNumber().equals(accountNumber)).findAny()
                            .orElse(null);
                }
            }

            if (Objects.isNull(accountResource)) {
                paymentInformation.setBulkstatus(ZERO_TWO);
                validationFieldErrorList.add("Given Account Number is not match with company accounts");
            }
        }
        return accountResource;
    }

    private boolean validateComapnyId(CompanyAccountforUser company, PwsFileUpload fileUpload) {
        String comapnyId = transactionUtils.getDecrypted(company.getCompanyId());
        return fileUpload.getCompanyId().equals(Long.parseLong(comapnyId));
    }

    private Map<String, Object> getHeaders() {
        AxwayResponseData axwayResponseData = getAxwayTokenService.getAxwayJwtToken();
        Map<String, Object> headers = new HashMap<>(HEADER_MAP);
        headers.put(AUTHORIZATION_NAME, axwayResponseData.getTokenType() + " " + axwayResponseData.getAccessToken());

        headers.put(REQ_DATE_TIME, OffsetDateTime.now().toInstant().atZone(ZoneId.of(UTC)).toOffsetDateTime());
        return headers;
    }

    private void validateUserResourceFeatureActions(PwsFileUpload fileUpload, PaymentInformation paymentInformation,
            List<String> validationFieldErrorList, Map<String, Object> headers, String encryptedUserId) {
        UserResourceFeaturesActionsData resourceFeaturesActionsData = commonUtil
                .getResourcesAndFeaturesByUserId(encryptedUserId, headers);
        log.info("Resource Features ActionsData: {} ", resourceFeaturesActionsData);
        if (ObjectUtils.isEmpty(resourceFeaturesActionsData)) {
            paymentInformation.setBulkstatus(ZERO_TWO);
            validationFieldErrorList.add("Given user there is no Resource Features Actions found");
        } else {
            Optional<Resource> resourceFeaturesActions = resourceFeaturesActionsData.getUserResourceAndFeatureAccess()
                    .getResources().stream()
                    .filter(resourceId -> fileUpload.getResourceId().equals(resourceId.getResourceId()))
                    .filter(resourceId -> resourceId.getFeatures().stream()
                            .anyMatch(feature -> fileUpload.getFeatureId().equals(feature.getFeatureId())))
                    .filter(resourceId -> resourceId.getActions().contains(CREATE)).findAny();

            if (resourceFeaturesActions.isEmpty()) {
                paymentInformation.setBulkstatus(ZERO_TWO);
                validationFieldErrorList.add("Given user Resource Features Actions not matched");
            }
        }
    }

    private int insertPwsBulkTransactionInstructions(PaymentInformation paymentInformation, PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, Map<String, Object> headers, PwsTransactions pwsTransactionsObj,
            AccountResource accountResource) {

        int rejectedTnxCount = 0;
        Long transactionId = pwsTransactionsObj.getTransactionId();
        fileUpload.setBankReferenceId(pwsTransactionsObj.getBankReferenceId());
        fileUpload.setTransactionId(String.valueOf(pwsTransactionsObj.getTransactionId()));
        List<CreditTransferTransactionInformation> creditTransferTransactionInformationList = paymentInformation
                .getCreditTransferTransactionInformation();

        List<String> validationFieldErrorList = new ArrayList<>();

        List<PwsResourceConfigurations> resourceConfigurations = lazyBatch.getSessionTemplate()
                .getMapper(BulkPwsTransactionDao.class)
                .findPwsResourceConfigsByResourceId(fileUpload.getResourceId(), ENTITY_ID);

        // maxChildTransactionCount
        Optional<PwsResourceConfigurations> transactionLimitConfiguration = resourceConfigurations.stream()
                .filter(configCode -> configCode.getConfigCode().equals(MAX_CHILD_TRANSACTION_COUNT)).findAny();
        String transactionLimitConfigVal = getTransactionLimit(transactionLimitConfiguration);

        int transactionCout = creditTransferTransactionInformationList.size();
        // Transaction Limit check
        if (Integer.parseInt(transactionLimitConfigVal) < transactionCout) {
            validationFieldErrorList.add(
                    "TransactionLimit is greater than " + transactionLimitConfigVal + "Actual size " + transactionCout);
            fileUpload.setBankReferenceId(null);
            fileUpload.setTransactionId(null);
            persistRejectedRecords(fileUpload, validationFieldErrorList, lazyBatch);
            log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    validationFieldErrorList);
            lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .deletePwsBulkTransactions(String.valueOf(transactionId));
            lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .deletePWSTransactions(String.valueOf(transactionId));
            return creditTransferTransactionInformationList.size();
        }

        String rejectFileOnError = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .getRejectFileOnError(String.valueOf(fileUpload.getCompanyId()));

        CountriesResp countriesResp = commonUtil.getCountriesByResourceIds(List.of(fileUpload.getResourceId()),
                headers);
        log.info("countriesResp ::{}", countriesResp);

        List<PwsBulkTransactionInstructions> pwsBulkTransactionInstructionsList = new ArrayList<>();
        for (CreditTransferTransactionInformation creditTransferTransaction : creditTransferTransactionInformationList) {
            if (isZeroTwo.test(creditTransferTransaction.getTransactionstatus())) {
                if (isZeroOne.test(rejectFileOnError)) {
                    log.info("reject_file_on_error is:{}", rejectFileOnError);
                    fileUpload.setRejectFileOnError(true);
                    fileUpload.setBankReferenceId(null);
                    fileUpload.setTransactionId(null);
                    persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                    deleteTransactions(lazyBatch, transactionId);
                    log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION,
                            fileUpload.getFileUploadId(), creditTransferTransaction.getErrordescription());
                    return creditTransferTransactionInformationList.size();
                }
                rejectedTnxCount++;

                setBankreferenceId(fileUpload, rejectedTnxCount, transactionCout, lazyBatch);

                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
            } else {
                fileUpload.setStringValue(rejectFileOnError);
                validationFieldErrorList = new ArrayList<>();
                if (validateTransactionDetails(paymentInformation, fileUpload, lazyBatch, headers,
                        validationFieldErrorList, countriesResp, creditTransferTransaction)) {
                    mangeTransactionDetails(lazyBatch, fileUpload.getTransactionId());
                    return creditTransferTransactionInformationList.size();
                }

                setErrordescription(validationFieldErrorList, creditTransferTransaction);
                pwsTransactionsObj.setTotalChild(transactionCout);
                pwsTransactionsObj.setRejectedTnxCount(rejectedTnxCount);
                rejectedTnxCount = setTransactionDetails(paymentInformation, fileUpload, lazyBatch, headers,
                        pwsTransactionsObj, pwsBulkTransactionInstructionsList, creditTransferTransaction);
            }
        }

        callComputeEquivalent(lazyBatch, pwsTransactionsObj, accountResource, pwsBulkTransactionInstructionsList);
        lazyBatch.flushStatements();
        return rejectedTnxCount;
    }

    private void deleteTransactions(LazyBatchSqlSessionTemplate lazyBatch, Long transactionId) {
        List<String> childBankReferenceids = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .getChildBankReferenceIds(String.valueOf(transactionId));
        if (CollectionUtils.isNotEmpty(childBankReferenceids)) {
            commonUtil.deleteBulkChildDetails(childBankReferenceids);
        }
        lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .deletePwsBulkTransactions(String.valueOf(transactionId));
        lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                .deletePWSTransactions(String.valueOf(transactionId));
    }

    private int setTransactionDetails(PaymentInformation paymentInformation, PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, Map<String, Object> headers, PwsTransactions pwsTransactionsObj,
            List<PwsBulkTransactionInstructions> pwsBulkTransactionInstructionsList,
            CreditTransferTransactionInformation creditTransferTransaction) {
        int rejectedTnxCount = pwsTransactionsObj.getRejectedTnxCount();
        int transactionCout = (int) pwsTransactionsObj.getTotalChild();

        List<Integer> reserveListBankRefIdFromSequence = lazyBatch.getSessionTemplate()
                .getMapper(BulkPwsTransactionDao.class).reserveListBankRefIdFromSequence(transactionCout);
        log.info("insertPwsBulkTransactionInstructions::reserveListBankRefIdFromSequence size ::{}",
                reserveListBankRefIdFromSequence.size());

        Iterator<Integer> iterator = reserveListBankRefIdFromSequence.iterator();
        Map<String, String> refIdMetaDataChild = enrichWithHardCodedValues(CROSS_BORDER_UOBSEND_CHILD);

        if (ZERO_TWO.equals(creditTransferTransaction.getTransactionstatus())) {
            rejectedTnxCount++;
            setBankreferenceId(fileUpload, rejectedTnxCount, transactionCout, lazyBatch);
            persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
            log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    creditTransferTransaction.getErrordescription());
        } else {

            List<ForeignExchange> foreignExchanges = creditTransferTransaction.getSupplementaryData().get(0)
                    .getEnvelope().getTransactionInformationSupplement().getExecutionLeg().getForex()
                    .getForeignExchange();

            String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
            String contractId = getContractId(foreignExchanges, currencyCode);

            String transactionReferenceId = commonUtil.generateBankRefId(LocalDateTime.now(), iterator.next(),
                    refIdMetaDataChild);
            PwsBulkTransactionInstructions bulkTransactionInstructions = pwsTransactionsMapper
                    .getPwsBulkTransactionInstructions(creditTransferTransaction, transactionReferenceId,
                            pwsTransactionsObj.getBankReferenceId(), pwsTransactionsObj.getTransactionId());
            setValueDate(paymentInformation, bulkTransactionInstructions);
            GebCompanyMvrData gebCompanyMvrData = lazyBatch.getSessionTemplate().getMapper(BulkPwsTransactionDao.class)
                    .getGebCompanyMvrData(String.valueOf(fileUpload.getCompanyId()));
            bulkTransactionInstructions.setDuplicationFlag(getDuplicateFlag(gebCompanyMvrData));
            bulkTransactionInstructions.setTransactionAmount(
                    new BigDecimal(creditTransferTransaction.getAmount().getInstructedAmount().getAmount()));
            bulkTransactionInstructions.setDMPTransRef(creditTransferTransaction.getDMPTransRef());
            setPaymentCodeId(fileUpload, headers, creditTransferTransaction, currencyCode, bulkTransactionInstructions);
            setPaymentDetails(creditTransferTransaction, bulkTransactionInstructions);
            bulkTransactionInstructions.setEquivalentCurrency(pwsTransactionsObj.getAccountCurrency());

            setFxFlag(pwsTransactionsObj, currencyCode, contractId, bulkTransactionInstructions);
            setTransferSpeed(fileUpload, headers, currencyCode, bulkTransactionInstructions);
            lazyBatch.insertToQueue(BATCH_INSERT_PWS_BULK_TRANSACTION_INSTRUCTIONS, bulkTransactionInstructions);

            PwsTransactions pwsTransactions = new PwsTransactions();
            pwsTransactions.setTransactionCurrency(currencyCode);
            pwsTransactions.setChildBankReferenceId(transactionReferenceId);
            pwsTransactions.setBankReferenceId(pwsTransactionsObj.getBankReferenceId());
            pwsTransactions.setTransactionId(pwsTransactionsObj.getTransactionId());

            Long partyId = insertPwsParty(pwsTransactions, lazyBatch, creditTransferTransaction, fileUpload, headers);
            insertPwsTransactionAdvices(lazyBatch, creditTransferTransaction, partyId);
            insertPwsPartyContacts(lazyBatch, creditTransferTransaction, partyId);
            insertPwsTransactionCharges(pwsTransactionsObj.getBankReferenceId(), transactionReferenceId,
                    pwsTransactionsObj.getTransactionId(), lazyBatch, paymentInformation);
            insertPwsTransactionFxContracts(lazyBatch, creditTransferTransaction, contractId, pwsTransactionsObj,
                    bulkTransactionInstructions, currencyCode);
            pwsBulkTransactionInstructionsList.add(bulkTransactionInstructions);
        }
        return rejectedTnxCount;
    }

    private String getDuplicateFlag(GebCompanyMvrData gebCompanyMvrData) {
        if (isDuplicateFlagForScenario3(gebCompanyMvrData)) {
            return FXFLAG_Y;
        } else if (isDuplicateFlagForScenario4(gebCompanyMvrData)) {
            return FXFLAG_Y;
        } else if (isDuplicateFlagForScenario2(gebCompanyMvrData)) {
            return FXFLAG_Y;
        } else if (isDuplicateFlagForScenario1(gebCompanyMvrData)) {
            return FXFLAG_N;
        }
        return null;
    }

    private boolean isDuplicateFlagForScenario4(GebCompanyMvrData gebCompanyMvrData) {
        return (isN.test(gebCompanyMvrData.getCheckDuplicateFile())
                || isN.test(gebCompanyMvrData.getCheckDuplicateCustRef()))
                && (isN.test(gebCompanyMvrData.getCheckDuplicateRecordWithinFile())
                        || isN.test(gebCompanyMvrData.getCheckDuplicateCustRefWithinFile()));
    }

    private boolean isDuplicateFlagForScenario3(GebCompanyMvrData gebCompanyMvrData) {
        return (isN.test(gebCompanyMvrData.getCheckDuplicateFile())
                || isN.test(gebCompanyMvrData.getCheckDuplicateCustRef()))
                && (isY.test(gebCompanyMvrData.getCheckDuplicateRecordWithinFile())
                        || isY.test(gebCompanyMvrData.getCheckDuplicateCustRefWithinFile()));
    }

    private boolean isDuplicateFlagForScenario2(GebCompanyMvrData gebCompanyMvrData) {
        return (isY.test(gebCompanyMvrData.getCheckDuplicateFile())
                || isY.test(gebCompanyMvrData.getCheckDuplicateCustRef()))
                && (isN.test(gebCompanyMvrData.getCheckDuplicateRecordWithinFile())
                        || isN.test(gebCompanyMvrData.getCheckDuplicateCustRefWithinFile()));
    }

    private boolean isDuplicateFlagForScenario1(GebCompanyMvrData gebCompanyMvrData) {
        return (isY.test(gebCompanyMvrData.getCheckDuplicateFile())
                || isY.test(gebCompanyMvrData.getCheckDuplicateCustRef()))
                && (isY.test(gebCompanyMvrData.getCheckDuplicateRecordWithinFile())
                        || isY.test(gebCompanyMvrData.getCheckDuplicateCustRefWithinFile()));
    }

    private String getContractId(List<ForeignExchange> foreignExchanges, String currencyCode) {
        String contractId = null;
        if (isCurrencyCodeValid(currencyCode, foreignExchanges)) {
            ExchangeRateInformation exchangeRateInformation = foreignExchanges.stream()
                    .map(ForeignExchange::getExchangeRateInformation).filter(Objects::nonNull).findFirst().orElse(null);
            if (Objects.nonNull(exchangeRateInformation)) {
                contractId = exchangeRateInformation.getContractIdentification();
            }
        }
        return contractId;
    }

    private boolean validateTransactionDetails(PaymentInformation paymentInformation, PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, Map<String, Object> headers, List<String> validationFieldErrorList,
            CountriesResp countriesResp, CreditTransferTransactionInformation creditTransferTransaction) {
        String rejectFileOnError = fileUpload.getStringValue();

        return validateIBANandBatchbooking(paymentInformation, fileUpload, lazyBatch, validationFieldErrorList,
                rejectFileOnError, countriesResp, creditTransferTransaction)
                || validateBankCodeAndPhoneNumber(fileUpload, lazyBatch, headers, validationFieldErrorList,
                        rejectFileOnError, countriesResp, creditTransferTransaction)
                || validateCurrencyCode(fileUpload, lazyBatch, validationFieldErrorList, creditTransferTransaction,
                        rejectFileOnError);
    }

    private boolean validateCurrencyCode(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, CreditTransferTransactionInformation creditTransferTransaction,
            String rejectFileOnError) {
        List<ForeignExchange> foreignExchanges = creditTransferTransaction.getSupplementaryData().get(0).getEnvelope()
                .getTransactionInformationSupplement().getExecutionLeg().getForex().getForeignExchange();
        String contractId = null;
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        if (isCurrencyCodeValid(currencyCode, foreignExchanges)) {
            ExchangeRateInformation exchangeRateInformation = foreignExchanges.stream()
                    .map(ForeignExchange::getExchangeRateInformation).filter(Objects::nonNull).findFirst().orElse(null);
            if (Objects.nonNull(exchangeRateInformation)) {
                contractId = exchangeRateInformation.getContractIdentification();
            }
            if (!transactionCurrencyValidation(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                    creditTransferTransaction, currencyCode, contractId)) {
                mangeTransactionDetails(lazyBatch, fileUpload.getTransactionId());
                return true;
            }
        }
        return false;
    }

    private boolean validateBankCodeAndPhoneNumber(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            Map<String, Object> headers, List<String> validationFieldErrorList, String fileStatusCode,
            CountriesResp countriesResp, CreditTransferTransactionInformation creditTransferTransaction) {
        List<String> bankCodeList = new ArrayList<>();
        List<String> bankSwiftCodeList = new ArrayList<>();
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        setBankCodeAndSwiftCode(creditTransferTransaction, currencyCode, bankCodeList, bankSwiftCodeList);
        BankListLookUpResp bankListLookUpResp = commonUtil.getBankList(bankCodeList, bankSwiftCodeList, headers,
                fileUpload.getResourceId());
        return isValidPhoneNumberAndCreditorName(fileUpload, lazyBatch, validationFieldErrorList, fileStatusCode,
                countriesResp, creditTransferTransaction)
                || isValidPurposeCodeAndBankCode(fileUpload, lazyBatch, headers, validationFieldErrorList,
                        fileStatusCode, creditTransferTransaction, bankListLookUpResp);
    }

    private boolean validateIBANandBatchbooking(PaymentInformation paymentInformation, PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, List<String> validationFieldErrorList, String rejectFileOnError,
            CountriesResp countriesResp, CreditTransferTransactionInformation creditTransferTransaction) {
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        return isValidIBANAndCountryCode(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                countriesResp, creditTransferTransaction)
                || isValidBatchBookingAndPaymentcode(paymentInformation, fileUpload, lazyBatch,
                        validationFieldErrorList, rejectFileOnError, creditTransferTransaction, currencyCode)
                || isValidTransactionAmountAndTransactionCurrency(fileUpload, lazyBatch, validationFieldErrorList,
                        rejectFileOnError, creditTransferTransaction, currencyCode);
    }

    private boolean isValidTransactionAmountAndTransactionCurrency(PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode) {
        Map<String, Object> headers = getHeaders();
        CurrenciesResp currenciesResp = commonUtil.getCurrenciesByResourceIds(List.of(fileUpload.getResourceId()),
                headers);
        return !transactionCurrencyValidation(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                creditTransferTransaction, currencyCode, currenciesResp)
                || !transactionAmountValidation(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                        creditTransferTransaction, currencyCode, currenciesResp);
    }

    private boolean transactionCurrencyValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode,
            CurrenciesResp currenciesResp) {
        log.info("transactionCurrencyValidation ResourceId: {}, currencyCode: {}", fileUpload.getResourceId(),
                currencyCode);
        List<CurrencyLists> currencyLists = currenciesResp.getCurrencyList().stream()
                .filter(currency -> currencyCode.equalsIgnoreCase(currency.getCurrencyISOCode()))
                .filter(currency -> currency.getMinorUnit().compareTo(BigDecimal.ZERO) == 0).toList();
        if (!currencyLists.isEmpty()) {
            BigDecimal transactionAmount = new BigDecimal(
                    creditTransferTransaction.getAmount().getInstructedAmount().getAmount());
            int decimalScale = transactionAmount.scale();
            log.info("Currency Minor unit check CurrencyCode: {}, transactionAmount: {}", currencyCode,
                    transactionAmount);
            if (decimalScale > CctiMappingConstant.ZERO) {
                if (ZERO_TWO.equals(rejectFileOnError)) {
                    log.error(FAILED_TO_VALIDATE_THE_CURRENCY_MINOR_UNIT_IS_0_REJECT_FILE_ON_ERROR_IS,
                            rejectFileOnError);
                    creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                    validationFieldErrorList.add(CURRENCY_MINOR_UNIT_IS_0_FOR_CURRENCY_CODE.concat(currencyCode));
                } else if (ZERO_ONE.equals(rejectFileOnError)) {
                    fileUpload.setRejectFileOnError(true);
                    log.error(FAILED_TO_VALIDATE_THE_CURRENCY_MINOR_UNIT_IS_0_REJECT_FILE_ON_ERROR_IS,
                            rejectFileOnError);
                    validationFieldErrorList.add(CURRENCY_MINOR_UNIT_IS_0_FOR_CURRENCY_CODE.concat(currencyCode));
                    creditTransferTransaction.setErrordescription(validationFieldErrorList);
                    persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                    log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION,
                            fileUpload.getFileUploadId(), creditTransferTransaction.getErrordescription());
                    return false;
                }
            }
        }
        return true;
    }

    private boolean transactionAmountValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode,
            CurrenciesResp currenciesResp) {

        BigDecimal transactionAmount = new BigDecimal(
                creditTransferTransaction.getAmount().getInstructedAmount().getAmount());
        log.info("transactionAmountValidation ResourceId: {}, currencyCode: {}, transactionAmount: {}",
                fileUpload.getResourceId(), transactionAmount, currencyCode);

        Optional<CurrencyLists> currencyLists = currenciesResp.getCurrencyList().stream()
                .filter(currency -> currencyCode.equalsIgnoreCase(currency.getCurrencyISOCode()))
                .filter(currencyList -> currencyList.getResourceIds().stream()
                        .anyMatch(resourceIds -> transactionAmount.compareTo(resourceIds.getThresholdValue()) > 0))
                .findAny();

        if (currencyLists.isPresent()) {
            BigDecimal thresholdAmount = currencyLists.get().getResourceIds().stream()
                    .map(ResourceCurrencies::getThresholdValue).findAny().orElse(null);
            if (ZERO_TWO.equals(rejectFileOnError)) {
                log.error(TRANSACTION_AMOUNT_IS_GREATER_THAN_THRESHOLD_AMOUNT_REJECT_FILE_ON_ERROR, thresholdAmount,
                        rejectFileOnError);
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                validationFieldErrorList.add(TRANSACTION_AMOUNT_CANNOT_BE_GREATER_THAN_THRESHOLD_AMOUNT
                        .concat(String.valueOf(thresholdAmount)));
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                log.error(TRANSACTION_AMOUNT_IS_GREATER_THAN_THRESHOLD_AMOUNT_REJECT_FILE_ON_ERROR, thresholdAmount,
                        rejectFileOnError);
                fileUpload.setRejectFileOnError(true);
                validationFieldErrorList.add(TRANSACTION_AMOUNT_CANNOT_BE_GREATER_THAN_THRESHOLD_AMOUNT
                        .concat(String.valueOf(thresholdAmount)));
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                return false;
            }
        }
        return true;
    }

    private boolean isValidIBANAndCountryCode(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError, CountriesResp countriesResp,
            CreditTransferTransactionInformation creditTransferTransaction) {
        String residencyStatus = creditTransferTransaction.getSupplementaryData().get(0).getEnvelope()
                .getTransactionInformationSupplement().getCreditorInformation().getResidentStatus();
        return !ibanValidation(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError, countriesResp,
                creditTransferTransaction)
                || !countryCodeValidation(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                        creditTransferTransaction, countriesResp, residencyStatus);
    }

    private boolean isValidPurposeCodeAndBankCode(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            Map<String, Object> headers, List<String> validationFieldErrorList, String fileStatusCode,
            CreditTransferTransactionInformation creditTransferTransaction, BankListLookUpResp bankListLookUpResp) {
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        return !purposeCodeValidation(fileUpload, lazyBatch, validationFieldErrorList, fileStatusCode,
                creditTransferTransaction, currencyCode, headers)
                || !bankCodeAndSwiftCodeValidation(fileUpload, lazyBatch, validationFieldErrorList, fileStatusCode,
                        bankListLookUpResp, creditTransferTransaction);
    }

    private boolean isValidPhoneNumberAndCreditorName(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String fileStatusCode, CountriesResp countriesResp,
            CreditTransferTransactionInformation creditTransferTransaction) {
        String creditorName = creditTransferTransaction.getCreditor().getName();
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        return !creditorNameValidation(fileUpload, lazyBatch, validationFieldErrorList, fileStatusCode,
                creditTransferTransaction, creditorName)
                || !phoneNumberValidation(fileUpload, lazyBatch, validationFieldErrorList, fileStatusCode,
                        countriesResp, creditTransferTransaction, currencyCode)
                || !addressLineValidation(fileUpload, lazyBatch, validationFieldErrorList, fileStatusCode,
                        creditTransferTransaction, currencyCode);
    }

    private boolean addressLineValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode) {
        List<String> addressLines = creditTransferTransaction.getCreditor().getPostalAddress().getAddressLine();
        log.info("addressLineValidation currencyCode: {}, AddressLine: {}", currencyCode, addressLines);

        if (cctiConfig.getAddressCurrencyList().contains(currencyCode) && !isValidAddressLine(addressLines)) {
            if (ZERO_TWO.equals(rejectFileOnError)) {
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                validationFieldErrorList.add(INVALID_ADDRESS_LINE_FOR_THIS_TRANSACTION_CURRENCY.concat(currencyCode));
                log.error(FAILED_TO_VALIDATE_THE_ADDRESS_LINE_CURRENCY_CODE_REJECT_FILE_ON_ERROR, currencyCode,
                        rejectFileOnError);
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                validationFieldErrorList.add(INVALID_ADDRESS_LINE_FOR_THIS_TRANSACTION_CURRENCY.concat(currencyCode));
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                fileUpload.setRejectFileOnError(true);
                log.error(FAILED_TO_VALIDATE_THE_ADDRESS_LINE_CURRENCY_CODE_REJECT_FILE_ON_ERROR, currencyCode,
                        rejectFileOnError);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                return false;
            }
        }
        return true;
    }

    private boolean isValidAddressLine(List<String> addressLines) {
        boolean isValid;
        if (CollectionUtils.isNotEmpty(addressLines)) {
            String addressLine = String.join("", addressLines);
            isValid = isValidAddressLine(addressLine.split(SEPARATOR));
        } else {
            isValid = false;
        }
        return isValid;
    }

    private boolean isValidAddressLine(String[] addressLines) {
        if (ObjectUtils.isNotEmpty(addressLines) && addressLines.length == FOUR) {
            return !ObjectUtils.isEmpty(addressLines[0]) && !ObjectUtils.isEmpty(addressLines[1])
                    && !ObjectUtils.isEmpty(addressLines[2]) && !ObjectUtils.isEmpty(addressLines[3]);
        } else {
            return false;
        }
    }

    private boolean isValidBatchBookingAndPaymentcode(PaymentInformation paymentInformation, PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode) {
        return !batchBookingValidation(paymentInformation, fileUpload, lazyBatch, validationFieldErrorList,
                rejectFileOnError, creditTransferTransaction)
                || !paymentCodeValidation(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                        creditTransferTransaction, currencyCode);
    }

    private boolean isCurrencyCodeValid(String currencyCode, List<ForeignExchange> foreignExchanges) {
        return TRANSACTION_CURRENCY_CODE.contains(currencyCode) && CollectionUtils.isNotEmpty(foreignExchanges);
    }

    private void setBankCodeAndSwiftCode(CreditTransferTransactionInformation creditTransferTransaction,
            String currencyCode, List<String> bankCodeList, List<String> bankSwiftCodeList) {
        if (TRANSACTION_CURRENCY_CODE5.contains(currencyCode)) {
            String bankCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                    .getClearingSystemMemberIdentification().getMemberIdentification();
            bankCodeList.add(bankCode);
        } else if (TRANSACTION_CURRENCY_CODE4.contains(currencyCode)) {
            String bankSwiftCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                    .getBICFI();
            bankSwiftCodeList.add(bankSwiftCode);
        }
    }

    private String getTransactionLimit(Optional<PwsResourceConfigurations> transactionLimitConfiguration) {
        String transactionLimitConfigVal = EMPTY;
        if (transactionLimitConfiguration.isPresent()) {
            transactionLimitConfigVal = transactionLimitConfiguration.get().getConfigValue();
        }
        return transactionLimitConfigVal;
    }

    private void setErrordescription(List<String> validationFieldErrorList,
            CreditTransferTransactionInformation creditTransferTransaction) {
        if (CollectionUtils.isNotEmpty(validationFieldErrorList)) {
            creditTransferTransaction.setErrordescription(validationFieldErrorList);
        }
    }

    private void setValueDate(PaymentInformation paymentInformation,
            PwsBulkTransactionInstructions bulkTransactionInstructions) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime localDateTime = LocalDateTime.parse(paymentInformation.getRequestedExecutionDate().getDate(),
                    formatter);
            bulkTransactionInstructions.setValueDate(localDateTime);
            bulkTransactionInstructions.setOriginalValueDate(localDateTime);
        } catch (Exception e) {
            log.error("date format wrong");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate
                    .parse(paymentInformation.getRequestedExecutionDate().getDate().substring(0, 10), formatter);
            LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalDateTime.now().toLocalTime());
            bulkTransactionInstructions.setValueDate(localDateTime);
            bulkTransactionInstructions.setOriginalValueDate(localDateTime);
        }
    }

    private void setFxFlag(PwsTransactions pwsTransactionsObj, String currencyCode, String contractId,
            PwsBulkTransactionInstructions bulkTransactionInstructions) {
        if (currencyCode.equals(pwsTransactionsObj.getAccountCurrency())) {
            bulkTransactionInstructions.setFxFlag(FXFLAG_N);
        } else if (ObjectUtils.isNotEmpty(contractId)) {
            bulkTransactionInstructions.setFxFlag(FXFLAG_Y);
        } else {
            bulkTransactionInstructions.setFxFlag(FXFLAG_N);
        }
    }

    private void setPaymentDetails(CreditTransferTransactionInformation creditTransferTransaction,
            PwsBulkTransactionInstructions bulkTransactionInstructions) {
        List<InstructionForCreditorAgent> creditorAgents = creditTransferTransaction.getInstructionForCreditorAgent();
        if (ObjectUtils.isNotEmpty(creditorAgents)) {
            String paymentDetails = creditorAgents.stream()
                    .filter(creditorAgent -> ObjectUtils.isNotEmpty(creditorAgent)
                            && ObjectUtils.isNotEmpty(creditorAgent.getInstructionInformation()))
                    .map(InstructionForCreditorAgent::getInstructionInformation).findFirst().orElse(null);
            bulkTransactionInstructions.setPaymentDetails(paymentDetails);
        }
    }

    private void callComputeEquivalent(LazyBatchSqlSessionTemplate lazyBatch, PwsTransactions pwsTransactionsObj,
            AccountResource accountResource, List<PwsBulkTransactionInstructions> pwsBulkTransactionInstructionsList) {
        try {
            log.info("callComputeEquivalent method started");
            Map<String, Object> headers = getHeaders();
            headers.put("isAxway", "true");
            GetFXRateResp fxRateResp = commonUtil.getFXDetails(pwsTransactionsObj.getResourceId(),
                    pwsTransactionsObj.getFeatureId(), String.valueOf(pwsTransactionsObj.getInitiatedBy()),
                    pwsTransactionsObj, pwsTransactionsObj.getAccountCurrency(), accountResource.getAccountId(),
                    headers);
            if (ObjectUtils.isNotEmpty(fxRateResp) && ObjectUtils.isNotEmpty(fxRateResp.getFxRateDetails())) {
                log.info("fxRateResp.getFxRateDetails() size = {}", fxRateResp.getFxRateDetails().size());
                FxRateDetails fxRate = fxRateResp.getFxRateDetails().stream().findFirst().orElse(new FxRateDetails());
                String encryptedTransactionId = transactionUtils
                        .getEncrypted(String.valueOf(pwsTransactionsObj.getTransactionId()));
                ComputeEquivalentAmountResp computeEquivalentAmountResp = commonUtil.computeEquivalentAmount(
                        encryptedTransactionId, pwsTransactionsObj, fxRate, pwsBulkTransactionInstructionsList,
                        headers);
                List<PwsTransactionFxContracts> fxContractList = new ArrayList<>();
                pwsBulkTransactionInstructionsList.forEach(bulkTransactionInstructions -> {
                    if (FXFLAG_N.equalsIgnoreCase(bulkTransactionInstructions.getFxFlag())) {
                        Deal dealVal = getDealBasedOnChild(computeEquivalentAmountResp,
                                bulkTransactionInstructions.getChildBankReferenceId(),
                                pwsTransactionsObj.getTransactionId());
                        fxContractList.add(pwsTransactionsMapper.pwsFXContractDetails(fxRate,
                                computeEquivalentAmountResp, bulkTransactionInstructions, pwsTransactionsObj, dealVal,
                                getValueFromDate(fxRate.getValueDate())));
                        bulkTransactionInstructions.setEquivalentCurrency(fxRate.getEquivalentCurrency());
                        bulkTransactionInstructions.setEquivalentAmount(dealVal.getEquivalentAmount());
                    }
                });
                log.info("fxContractList size = {}", fxContractList.size());
                fxContractList.forEach(
                        contract -> lazyBatch.insertAndFlush(BATCH_UPDATE_TRANSACTION_CONTRACT_SQL_QUERY, contract));
                pwsBulkTransactionInstructionsList.forEach(instruction -> lazyBatch
                        .insertAndFlush(BATCH_UPDATE_BULK_TRANSACTION_INSTRUCTION_SQL_QUERY, instruction));
            }
        } catch (Exception e) {
            log.error("Error while getting equivalent amount {}", e);
        }

    }

    private void setBankreferenceId(PwsFileUpload fileUpload, int rejectedTnxCount, int transactionCout,
            LazyBatchSqlSessionTemplate lazyBatch) {
        if (rejectedTnxCount == transactionCout) {
            mangeTransactionDetails(lazyBatch, fileUpload.getTransactionId());
            fileUpload.setBankReferenceId(null);
            fileUpload.setTransactionId(null);
        }
    }

    private void setPaymentCodeId(PwsFileUpload fileUpload, Map<String, Object> headers,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode,
            PwsBulkTransactionInstructions bulkTransactionInstructions) {
        if (TRANSACTION_CURRENCY_CODE3.contains(currencyCode)) {
            String proprietary = creditTransferTransaction.getPurpose().getProprietary();
            PwsTransactionsInfo pwsTransactions = new PwsTransactionsInfo();
            pwsTransactions.setTransactionCurrency(currencyCode);
            pwsTransactions.setPaymentCodeId(proprietary);
            pwsTransactions.setDestinationCountry(bulkTransactionInstructions.getDestinationCountry());
            CodeDetails paymentCodeDetails = commonUtil.getPaymentCodes(pwsTransactions, fileUpload.getResourceId(),
                    headers);
            if (Objects.nonNull(paymentCodeDetails)) {
                bulkTransactionInstructions.setPaymentCodeId(paymentCodeDetails.getCodeId());
            }
        }
    }

    private void setTransferSpeed(PwsFileUpload fileUpload, Map<String, Object> headers, String currencyCode,
            PwsBulkTransactionInstructions bulkTransactionInstructions) {
        if (CROSS_BORDER_UOBSEND.equals(fileUpload.getResourceId())) {
            String transferSpeed = commonUtil.getTransferSpeedFromCommonlookup(currencyCode, fileUpload.getResourceId(),
                    headers);
            if (!Strings.isNullOrEmpty(transferSpeed)) {
                bulkTransactionInstructions.setTransferSpeed(Long.parseLong(transferSpeed));
            }
        }
    }

    private boolean batchBookingValidation(PaymentInformation paymentInformation, PwsFileUpload fileUpload,
            LazyBatchSqlSessionTemplate lazyBatch, List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction) {
        String contractIdentification = null;
        log.info("batchBookingValidation ResourceId: {}, isBatchBooking: {}", fileUpload.getResourceId(),
                paymentInformation.isBatchBooking());
        if (PAYROLL_CROSS_BORDER_UOBSEND.equals(fileUpload.getResourceId()) && paymentInformation.isBatchBooking()) {
            List<ForeignExchange> foreignExchanges = creditTransferTransaction.getSupplementaryData().get(0)
                    .getEnvelope().getTransactionInformationSupplement().getExecutionLeg().getForex()
                    .getForeignExchange();
            ExchangeRateInformation exchangeRateInformation = foreignExchanges.stream()
                    .map(ForeignExchange::getExchangeRateInformation).filter(Objects::nonNull).findFirst().orElse(null);
            if (Objects.nonNull(exchangeRateInformation)) {
                contractIdentification = exchangeRateInformation.getContractIdentification();
            }
            if (!Strings.isNullOrEmpty(contractIdentification)) {
                if (ZERO_TWO.equals(rejectFileOnError)) {
                    creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                    validationFieldErrorList.add(CONTRACT_IDENTIFICATION_IS_NOT_NULL_FOR_THIS_RESOURCE_ID
                            .concat(fileUpload.getResourceId()));
                    log.error(FAILED_TO_VALIDATE_THE_BATCH_BOOKING_RESOURCE_ID_REJECT_FILE_ON_ERROR,
                            fileUpload.getResourceId(), rejectFileOnError);
                } else if (ZERO_ONE.equals(rejectFileOnError)) {
                    fileUpload.setRejectFileOnError(true);
                    log.error(FAILED_TO_VALIDATE_THE_BATCH_BOOKING_RESOURCE_ID_REJECT_FILE_ON_ERROR,
                            fileUpload.getResourceId(), rejectFileOnError);
                    validationFieldErrorList.add(CONTRACT_IDENTIFICATION_IS_NOT_NULL_FOR_THIS_RESOURCE_ID
                            .concat(fileUpload.getResourceId()));
                    creditTransferTransaction.setErrordescription(validationFieldErrorList);
                    persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                    log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION,
                            fileUpload.getFileUploadId(), creditTransferTransaction.getErrordescription());
                    return false;
                }
            }
        }
        return true;
    }

    private boolean bankCodeAndSwiftCodeValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError, BankListLookUpResp bankListLookUpResp,
            CreditTransferTransactionInformation creditTransferTransaction) {
        log.info("bankCodeAndSwiftCodeValidation bankListLookUpResp: {}", bankListLookUpResp);
        String countryCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                .getPostalAddress().getCountry();
        BankListSchema bankListSchema = null;
        List<BankListSchema> bankListSchemas = null;
        if (ObjectUtils.isNotEmpty(bankListLookUpResp)) {
            bankListSchemas = bankListLookUpResp.getBankDetails().stream()
                    .filter(bankDetails -> bankDetails.getResourceId().equals(fileUpload.getResourceId()))
                    .map(BankDetailsResp::getBankList).findAny().orElse(null);
        }

        if (CollectionUtils.isNotEmpty(bankListSchemas)) {
            bankListSchema = bankListSchemas.stream().filter(bankList -> bankList.getCountryCode().equals(countryCode))
                    .findAny().orElse(null);
        }

        String bankCodeAndSwiftCode = getBankcodeAndSwiftcode(creditTransferTransaction);

        if (ObjectUtils.isEmpty(bankListSchema)) {
            if (ZERO_TWO.equals(rejectFileOnError)) {
                log.error(FAILED_TO_VALIDATE_THE_BANK_CODE_AND_SWIFT_CODE_BANK_CODE_AND_SWIFT_CODE_REJECT_FILE_ON_ERROR,
                        bankCodeAndSwiftCode, rejectFileOnError);
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                validationFieldErrorList.add(BANK_CODE_AND_SWIFT_CODE_VALIDATION_FAILED.concat(bankCodeAndSwiftCode));
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                fileUpload.setRejectFileOnError(true);
                validationFieldErrorList.add(BANK_CODE_AND_SWIFT_CODE_VALIDATION_FAILED.concat(bankCodeAndSwiftCode));
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                log.error(FAILED_TO_VALIDATE_THE_BANK_CODE_AND_SWIFT_CODE_BANK_CODE_AND_SWIFT_CODE_REJECT_FILE_ON_ERROR,
                        bankCodeAndSwiftCode, rejectFileOnError);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                return false;
            }
        }
        return true;
    }

    private String getBankcodeAndSwiftcode(CreditTransferTransactionInformation creditTransferTransaction) {
        String bankCodeAndSwiftCode = null;
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        if (TRANSACTION_CURRENCY_CODE5.contains(currencyCode)) {
            bankCodeAndSwiftCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                    .getClearingSystemMemberIdentification().getMemberIdentification();
        } else if (TRANSACTION_CURRENCY_CODE4.contains(currencyCode)) {
            bankCodeAndSwiftCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                    .getBICFI();
        }
        return bankCodeAndSwiftCode;
    }

    private boolean ibanValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError, CountriesResp countriesResp,
            CreditTransferTransactionInformation creditTransferTransaction) {

        String destinationCountry = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                .getPostalAddress().getCountry();
        String ibanAccount = creditTransferTransaction.getCreditorAccount().getIdentification().getOther()
                .getIdentification();
        IBanValidationReqSchema reqSchema = new IBanValidationReqSchema();
        reqSchema.setAccountNo(ibanAccount);
        reqSchema.setDestinationCountry(destinationCountry);
        reqSchema.setResourceId(fileUpload.getResourceId());
        boolean isIbanValidation = commonUtil.getIbanValidation(reqSchema, countriesResp);
        log.info("isIbanValidation {}", isIbanValidation);
        if (!isIbanValidation) {
            return ibanAccountCheck(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                    creditTransferTransaction);
        }

        return true;
    }

    private boolean ibanAccountCheck(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction) throws ApplicationException {
        if (ZERO_TWO.equals(rejectFileOnError)) {
            creditTransferTransaction.setTransactionstatus(ZERO_TWO);
            validationFieldErrorList.add(IBAN_VALIDATION_FAILED);
            log.error(FAILED_TO_VALIDATE_THE_IBAN_REJECT_FILE_ON_ERROR, rejectFileOnError);
        } else if (ZERO_ONE.equals(rejectFileOnError)) {
            validationFieldErrorList.add(IBAN_VALIDATION_FAILED);
            log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    creditTransferTransaction.getErrordescription());
            log.error(FAILED_TO_VALIDATE_THE_IBAN_REJECT_FILE_ON_ERROR, rejectFileOnError);
            fileUpload.setRejectFileOnError(true);
            creditTransferTransaction.setErrordescription(validationFieldErrorList);
            persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
            log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    creditTransferTransaction.getErrordescription());
            return false;
        }
        return true;
    }

    private boolean transactionCurrencyValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode, String contractId) {
        log.info("transactionCurrencyValidation currencyCode: {}, contractId: {}", currencyCode, contractId);

        if (isNull.test(contractId)) {
            if (isZeroTwo.test(rejectFileOnError)) {
                log.error(FAILED_TO_VALIDATE_THE_TRANSACTION_CURRENCY_CURRENCY_CODE_REJECT_FILE_ON_ERROR,
                        TRANSACTION_CURRENCY_CODE, rejectFileOnError);
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                validationFieldErrorList.add(CONTRACT_ID_IS_NOT_NULL_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY
                        .concat(String.valueOf(TRANSACTION_CURRENCY_CODE)));
            } else if (isZeroOne.test(rejectFileOnError)) {
                fileUpload.setRejectFileOnError(true);
                validationFieldErrorList.add(CONTRACT_ID_IS_NOT_NULL_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY
                        .concat(String.valueOf(TRANSACTION_CURRENCY_CODE)));
                log.error(FAILED_TO_VALIDATE_THE_TRANSACTION_CURRENCY_CURRENCY_CODE_REJECT_FILE_ON_ERROR,
                        TRANSACTION_CURRENCY_CODE, rejectFileOnError);
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                return false;
            }
        }
        return true;
    }

    private boolean purposeCodeValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode,
            Map<String, Object> headers) {
        String purposeCode = creditTransferTransaction.getPurpose().getProprietary();
        log.info("purposeCodeValidation currencyCode: {}, purposeCode: {}", currencyCode, purposeCode);
        if (TRANSACTION_CURRENCY_CODE3.contains(currencyCode)) {
            if (Objects.isNull(purposeCode)) {
                return validatePurposeCode(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                        creditTransferTransaction, currencyCode);
            } else {
                String destinationCountry = creditTransferTransaction.getCreditorAgent()
                        .getFinancialInstitutionIdentification().getPostalAddress().getCountry();
                PwsTransactionsInfo pwsTransactions = new PwsTransactionsInfo();
                pwsTransactions.setTransactionCurrency(currencyCode);
                pwsTransactions.setPaymentCodeId(purposeCode);
                pwsTransactions.setDestinationCountry(destinationCountry);
                CodeDetails paymentCodeDetails = commonUtil.getPaymentCodes(pwsTransactions, fileUpload.getResourceId(),
                        headers);
                if (Objects.isNull(paymentCodeDetails)) {
                    return validatePurposeCode(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                            creditTransferTransaction, currencyCode);
                }
            }
        }
        return true;
    }

    private boolean validatePurposeCode(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode) {
        if (ZERO_TWO.equals(rejectFileOnError)) {
            log.error(FAILED_TO_VALIDATE_THE_PURPOSE_CODE_REJECT_FILE_ON_ERROR, rejectFileOnError);
            creditTransferTransaction.setTransactionstatus(ZERO_TWO);
            validationFieldErrorList
                    .add(PURPOSE_CODE_IS_INVALID_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY.concat(currencyCode));
        } else if (ZERO_ONE.equals(rejectFileOnError)) {
            log.error(FAILED_TO_VALIDATE_THE_PURPOSE_CODE_REJECT_FILE_ON_ERROR, rejectFileOnError);
            fileUpload.setRejectFileOnError(true);
            validationFieldErrorList
                    .add(PURPOSE_CODE_IS_INVALID_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY.concat(currencyCode));
            creditTransferTransaction.setErrordescription(validationFieldErrorList);
            persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
            log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    creditTransferTransaction.getErrordescription());
            return false;
        }
        return true;
    }

    private boolean phoneNumberValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError, CountriesResp countriesResp,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode) {
        String phoneNumber = creditTransferTransaction.getCreditor().getContactDetails().getPhoneNumber();
        String countryCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                .getPostalAddress().getCountry();
        log.info("phoneNumberValidation phoneNumber: {}, countriesResp: {}", phoneNumber, countriesResp);
        if (TRANSACTION_CURRENCY_CODE2.contains(currencyCode) && ObjectUtils.isNotEmpty(countriesResp)) {
            if (Objects.nonNull(phoneNumber)) {
                String[] phoneNum = phoneNumber.split(HYPHEN);
                Optional<CountryLists> countryList = countriesResp.getCountryList().stream()
                        .filter(country -> country.getCountryAlpha2Code().equals(countryCode))
                        .filter(country -> country.getPhoneCode().equals(phoneNum[0])).findAny();
                if (countryList.isEmpty()) {
                    if (ZERO_TWO.equals(rejectFileOnError)) {
                        creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                        validationFieldErrorList.add(PHONE_CODE_IS_NOT_MATHED_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY
                                .concat(currencyCode).concat(PHONE_CODE).concat(phoneNum[0]));
                        log.error(FAILED_TO_VALIDATE_THE_PHONE_CODE_REJECT_FILE_ON_ERROR, phoneNum[0],
                                creditTransferTransaction.getErrordescription());
                    } else if (ZERO_ONE.equals(rejectFileOnError)) {
                        fileUpload.setRejectFileOnError(true);
                        validationFieldErrorList.add(PHONE_CODE_IS_NOT_MATHED_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY
                                .concat(currencyCode).concat(PHONE_CODE).concat(phoneNum[0]));
                        creditTransferTransaction.setErrordescription(validationFieldErrorList);
                        persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                        log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION,
                                fileUpload.getFileUploadId(), creditTransferTransaction.getErrordescription());
                        log.error(FAILED_TO_VALIDATE_THE_PHONE_CODE_REJECT_FILE_ON_ERROR, phoneNum[0],
                                rejectFileOnError);
                        return false;
                    }
                }
            } else {
                return isPhoneNumberNUll(fileUpload, lazyBatch, validationFieldErrorList, rejectFileOnError,
                        creditTransferTransaction);
            }
        }
        return true;
    }

    private boolean isPhoneNumberNUll(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction) {
        if (ZERO_ONE.equals(rejectFileOnError)) {
            log.error(FAILED_TO_VALIDATE_THE_PHONE_NUMBER_REJECT_FILE_ON_ERROR, rejectFileOnError);
            fileUpload.setRejectFileOnError(true);
            validationFieldErrorList.add(PHONE_NUMBER_IS_NOT_NULL_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY
                    .concat(String.valueOf(TRANSACTION_CURRENCY_CODE2)));
            creditTransferTransaction.setErrordescription(validationFieldErrorList);
            persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
            log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                    creditTransferTransaction.getErrordescription());
            return false;
        } else {
            creditTransferTransaction.setTransactionstatus(ZERO_TWO);
            validationFieldErrorList.add(PHONE_NUMBER_IS_NOT_NULL_FOR_THE_FOLLOWING_TRANSACTION_CURRENCY
                    .concat(String.valueOf(TRANSACTION_CURRENCY_CODE2)));
            log.error(FAILED_TO_VALIDATE_THE_PHONE_NUMBER_REJECT_FILE_ON_ERROR, rejectFileOnError);
        }
        return true;
    }

    private boolean creditorNameValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String creditorName) {
        log.info("creditorNameValidation creditorName: {}", creditorName);
        if (Objects.isNull(creditorName)) {
            if (ZERO_TWO.equals(rejectFileOnError)) {
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                log.error(FAILED_TO_VALIDATE_THE_CREDITOR_NAME_REJECT_FILE_ON_ERROR, creditorName, rejectFileOnError);
                validationFieldErrorList.add(CREDITOR_NAME_IS_NULL);
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                validationFieldErrorList.add(CREDITOR_NAME_IS_NULL);
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                fileUpload.setRejectFileOnError(true);
                log.error(FAILED_TO_VALIDATE_THE_CREDITOR_NAME_REJECT_FILE_ON_ERROR, creditorName, rejectFileOnError);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                return false;
            }
        }
        return true;
    }

    private boolean paymentCodeValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, String currencyCode) {
        String instructionInformation = creditTransferTransaction.getInstructionForCreditorAgent().get(0)
                .getInstructionInformation();
        log.info("paymentCodeValidation currencyCode: {}, instructionInformation: {}", currencyCode,
                instructionInformation);
        if (COUNTRY_CURRENCY_CNH.equals(currencyCode) && Objects.isNull(instructionInformation)) {
            if (ZERO_TWO.equals(rejectFileOnError)) {
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                validationFieldErrorList
                        .add(INSTRUCTION_INFORMATION_IS_NOT_NULL_FOR_THIS_TRANSACTION_CURRENCY.concat(currencyCode));
                log.error(FAILED_TO_VALIDATE_THE_PAYMENT_CODE_CURRENCY_CODE_REJECT_FILE_ON_ERROR, currencyCode,
                        rejectFileOnError);
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                log.error(FAILED_TO_VALIDATE_THE_PAYMENT_CODE_CURRENCY_CODE_REJECT_FILE_ON_ERROR, currencyCode,
                        rejectFileOnError, creditTransferTransaction.getErrordescription());
                validationFieldErrorList
                        .add(INSTRUCTION_INFORMATION_IS_NOT_NULL_FOR_THIS_TRANSACTION_CURRENCY.concat(currencyCode));
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                fileUpload.setRejectFileOnError(true);
                return false;
            }
        }
        return true;
    }

    private boolean countryCodeValidation(PwsFileUpload fileUpload, LazyBatchSqlSessionTemplate lazyBatch,
            List<String> validationFieldErrorList, String rejectFileOnError,
            CreditTransferTransactionInformation creditTransferTransaction, CountriesResp countriesResp,
            String residencyStatus) {
        String countryCode = creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                .getPostalAddress().getCountry();
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();
        log.info("countryCodeValidation currencyCode: {}, residencyStatus: {}", currencyCode, residencyStatus);
        Optional<CountryLists> countryList = countriesResp.getCountryList().stream()
                .filter(country -> country.getCountryAlpha2Code().equalsIgnoreCase(countryCode)).findAny();
        if (countryList.isEmpty()) {
            if (ZERO_TWO.equals(rejectFileOnError)) {
                log.error(FAILED_TO_VALIDATE_THE_COUNTRY_CODE_REJECT_FILE_ON_ERROR_IS, rejectFileOnError);
                validationFieldErrorList.add(COUNTRY_CODE_VALIDATION_FAILED + countryCode);
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                log.error(FAILED_TO_VALIDATE_THE_COUNTRY_CODE_REJECT_FILE_ON_ERROR_IS, rejectFileOnError);
                validationFieldErrorList.add(COUNTRY_CODE_VALIDATION_FAILED + countryCode);
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                fileUpload.setRejectFileOnError(true);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                return false;
            }
        } else if (COUNTRY_CODE_IE.equals(countryCode) && COUNTRY_CURRENCY_IDR.equals(currencyCode)
                && ObjectUtils.allNull(residencyStatus)) {
            if (ZERO_TWO.equals(rejectFileOnError)) {
                log.error("rejectFileOnError is 02 for Country Code :{}", countryCode);
                creditTransferTransaction.setTransactionstatus(ZERO_TWO);
                validationFieldErrorList.add(RESIDENCY_STATUS_IS_NULL_FOR_COUNTRY_CODE + countryCode);
            } else if (ZERO_ONE.equals(rejectFileOnError)) {
                log.error("rejectFileOnError is 01 for Country Code :{}", countryCode);
                fileUpload.setRejectFileOnError(true);
                validationFieldErrorList.add(RESIDENCY_STATUS_IS_NULL_FOR_COUNTRY_CODE + countryCode);
                creditTransferTransaction.setErrordescription(validationFieldErrorList);
                persistRejectedRecords(fileUpload, creditTransferTransaction.getErrordescription(), lazyBatch);
                log.error(INSERT_REJECTED_RECORD_FILE_UPLOAD_ID_WITH_ERROR_DESCRIPTION, fileUpload.getFileUploadId(),
                        creditTransferTransaction.getErrordescription());
                return false;
            }
        }
        return true;
    }

    private Long insertPwsParty(PwsTransactions pwsTransactions, LazyBatchSqlSessionTemplate lazyBatch,
            CreditTransferTransactionInformation creditTransferTransaction, PwsFileUpload fileUpload,
            Map<String, Object> headers) {

        PartyInfo partyInfo = pwsTransactionsMapper.getPwsPartyInfo(creditTransferTransaction,
                pwsTransactions.getChildBankReferenceId(), pwsTransactions.getTransactionId(),
                pwsTransactions.getBankReferenceId());
        partyInfo.setPartyModifiedDate(new Timestamp(System.currentTimeMillis()));

        setBankCodeAndBickCode(pwsTransactions, creditTransferTransaction, partyInfo);

        if (COUNTRY_CURRENCY_IDR.equals(pwsTransactions.getTransactionCurrency())) {
            PwsTransactionsInfo pwsTransactionsInfo = new PwsTransactionsInfo();
            pwsTransactionsInfo.setPartyType(creditTransferTransaction.getSupplementaryData().get(0).getEnvelope()
                    .getTransactionInformationSupplement().getCreditorInformation().getSubType());
            pwsTransactionsInfo.setResidencyStatus(creditTransferTransaction.getSupplementaryData().get(0).getEnvelope()
                    .getTransactionInformationSupplement().getCreditorInformation().getResidentStatus());
            GenericLookUpInfo lookUpInfo = commonUtil.getCommonlookupData(pwsTransactionsInfo,
                    fileUpload.getResourceId(), headers);
            log.info("GenericLookUpInfo: {}", lookUpInfo);

            partyInfo.setPartyType(Long.parseLong(lookUpInfo.getSubType()));
            partyInfo.setResidencyStatus(Long.parseLong(lookUpInfo.getResidentStatus()));
        }

        lazyBatch.insertAndFlush(BATCH_INSERT_PWS_PARTY, partyInfo); // Persist in 'PWS_PARTY'
        log.info("BatchInsertPwsParty partyInfo: {}", partyInfo);

        return partyInfo.getPartyId();
    }

    private void setBankCodeAndBickCode(PwsTransactions pwsTransactions,
            CreditTransferTransactionInformation creditTransferTransaction, PartyInfo partyInfo) {
        if (TRANSACTION_CURRENCY_CODE5.contains(pwsTransactions.getTransactionCurrency())) {
            partyInfo.setBankCode(creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification()
                    .getClearingSystemMemberIdentification().getMemberIdentification());
        } else if (TRANSACTION_CURRENCY_CODE4.contains(pwsTransactions.getTransactionCurrency())) {
            partyInfo.setSwiftCode(
                    creditTransferTransaction.getCreditorAgent().getFinancialInstitutionIdentification().getBICFI());
        }
    }

    private void insertPwsTransactionAdvices(LazyBatchSqlSessionTemplate lazyBatch,
            CreditTransferTransactionInformation creditTransferTransaction, Long partyId) {
        PwsTransactionAdvices pwsTransactionAdvices = new PwsTransactionAdvices();
        pwsTransactionAdvices.setPartyId(partyId);
        List<Other> other = creditTransferTransaction.getCreditor().getIdentification().getOrganisationIdentification()
                .getOther();
        setAdviceIdAndPartyName(pwsTransactionAdvices, other);
        pwsTransactionAdvices.setDeliveryMethod(creditTransferTransaction.getRelatedRemittanceInformation().get(0)
                .getRemittanceLocationDetails().get(0).getMethod());
        pwsTransactionAdvices.setDeliveryAddress(creditTransferTransaction.getRelatedRemittanceInformation().get(0)
                .getRemittanceLocationDetails().get(0).getElectronicAddress());
        List<String> unstructured = creditTransferTransaction.getRemittanceInformation().getUnstructured();
        if (!unstructured.isEmpty()) {
            pwsTransactionAdvices.setAdviseMessage(unstructured.get(0));
        }
        pwsTransactionAdvices.setReferenceNo(
                creditTransferTransaction.getRelatedRemittanceInformation().get(0).getRemittanceIdentification());
        lazyBatch.insertToQueue(BATCH_INSERT_PWS_TRANSACTION_ADVICES, pwsTransactionAdvices);
        log.info("insertPwsTransactionAdvices PwsTransactionAdvices: {}", pwsTransactionAdvices);
    }

    private void setAdviceIdAndPartyName(PwsTransactionAdvices pwsTransactionAdvices, List<Other> other) {
        if (CollectionUtils.isNotEmpty(other)) {
            int length = other.size();
            switch (length) {
                case ONE -> pwsTransactionAdvices.setPartyName1(other.get(0).getIdentification());
                case TWO -> {
                    pwsTransactionAdvices.setPartyName1(other.get(0).getIdentification());
                    pwsTransactionAdvices.setPartyName2(other.get(1).getIdentification());
                }
                case THREE -> {
                    pwsTransactionAdvices.setPartyName1(other.get(0).getIdentification());
                    pwsTransactionAdvices.setPartyName2(other.get(1).getIdentification());
                    pwsTransactionAdvices.setAdviceId(other.get(2).getIdentification());
                }
                default -> log.info("For Identification No case matched:{}", other.size());
            }
        }
    }

    private void insertPwsPartyContacts(LazyBatchSqlSessionTemplate lazyBatch,
            CreditTransferTransactionInformation creditTransferTransaction, Long partyId) {
        PwsPartyContacts pwsPartyContacts = new PwsPartyContacts();
        pwsPartyContacts.setPartyId(partyId);
        List<String> addreList = creditTransferTransaction.getCreditor().getPostalAddress().getAddressLine();
        String currencyCode = creditTransferTransaction.getAmount().getInstructedAmount().getCurrency();

        if (cctiConfig.getAddressCurrencyList().contains(currencyCode)) {
            setStreetNameAndPostalCode(pwsPartyContacts, addreList);
        } else {
            setAddressLine(pwsPartyContacts, addreList);
        }

        String phoneNumber = creditTransferTransaction.getCreditor().getContactDetails().getPhoneNumber();
        if (!Strings.isNullOrEmpty(phoneNumber)) {
            int firstHyphenIndex = phoneNumber.indexOf(HYPHEN);
            String countryCode = phoneNumber.substring(0, firstHyphenIndex);
            String phNumber = phoneNumber.substring(firstHyphenIndex + 1);
            pwsPartyContacts.setPhoneCountryCode(countryCode);
            pwsPartyContacts.setPhoneNo(phNumber);
        }

        lazyBatch.insertToQueue(BATCH_INSERT_PWS_PARTY_CONTACTS, pwsPartyContacts);
        log.info("insertPwsPartyContacts PwsPartyContacts: {}", pwsPartyContacts);
    }

    private void setAddressLine(PwsPartyContacts pwsPartyContacts, List<String> addreList) {
        if (CollectionUtils.isNotEmpty(addreList)) {
            int length = addreList.size();
            switch (length) {
                case ONE -> pwsPartyContacts.setAddressLine1(addreList.get(0));
                case TWO -> {
                    pwsPartyContacts.setAddressLine1(addreList.get(0));
                    pwsPartyContacts.setAddressLine2(addreList.get(1));
                }
                case THREE -> {
                    pwsPartyContacts.setAddressLine1(addreList.get(0));
                    pwsPartyContacts.setAddressLine2(addreList.get(1));
                    pwsPartyContacts.setAddressLine3(addreList.get(2));
                }
                default -> log.info("For AddressLine No case matched:{}", addreList.size());
            }
        }
    }

    private void setStreetNameAndPostalCode(PwsPartyContacts pwsPartyContacts, List<String> addreList) {
        if (CollectionUtils.isNotEmpty(addreList)) {
            String addressLine = String.join("", addreList);
            String[] addressLines = addressLine.split(SEPARATOR);
            if (addressLines.length == FOUR) {
                pwsPartyContacts.setStreetName(
                        addressLines[0].length() > 49 ? addressLines[0].substring(0, 48) : addressLines[0]);
                pwsPartyContacts.setPostalCode(
                        addressLines[1].length() > 16 ? addressLines[1].substring(0, 15) : addressLines[1]);
                pwsPartyContacts.setTownName(
                        addressLines[2].length() > 35 ? addressLines[2].substring(0, 34) : addressLines[2]);
                pwsPartyContacts.setCountryCode(
                        addressLines[3].length() > 2 ? addressLines[3].substring(0, 1) : addressLines[3]);
            }
        }
    }

    private void insertPwsTransactionCharges(String bankReferenceId, String transactionReferenceId, long transactionId,
            LazyBatchSqlSessionTemplate lazyBatch, PaymentInformation paymentInformation) {
        PwsTransactionCharges pwsTransactionCharges = new PwsTransactionCharges();
        pwsTransactionCharges.setBankReferenceId(bankReferenceId);
        pwsTransactionCharges.setChildBankReferenceId(transactionReferenceId);
        OtherVO otherVO = paymentInformation.getChargesAccount().getIdentification().getOther();
        if (Objects.nonNull(otherVO) && !Strings.isNullOrEmpty(otherVO.getIdentification())) {
            pwsTransactionCharges.setChargeAccountNumber(Long.parseLong(otherVO.getIdentification()));
        }
        pwsTransactionCharges.setFeesAmount(new BigDecimal(0));
        pwsTransactionCharges.setTransactionId(transactionId);
        lazyBatch.insertToQueue(BATCH_INSERT_PWS_TRANSACTION_CHARGES, pwsTransactionCharges);
        log.info("insertPwsTransactionCharges pwsTransactionChanrges: {}", pwsTransactionCharges);
    }

    private void insertPwsTransactionFxContracts(LazyBatchSqlSessionTemplate lazyBatch,
            CreditTransferTransactionInformation creditTransferTransaction, String contractId,
            PwsTransactions pwsTransactionsObj, PwsBulkTransactionInstructions bulkTransactionInstructions,
            String currencyCode) {
        PwsTransactionFxContracts pwsTransactionFxContracts = new PwsTransactionFxContracts();
        pwsTransactionFxContracts.setBankReferenceId(pwsTransactionsObj.getBankReferenceId());
        pwsTransactionFxContracts.setChildBankReferenceId(bulkTransactionInstructions.getChildBankReferenceId());
        pwsTransactionFxContracts.setTransactionId(String.valueOf(pwsTransactionsObj.getTransactionId()));
        if (currencyCode.equals(pwsTransactionsObj.getAccountCurrency()) || Objects.isNull(contractId)) {
            insertIndicativeContract(lazyBatch, pwsTransactionFxContracts);
        } else {
            insertPreebookContract(creditTransferTransaction, currencyCode, lazyBatch, pwsTransactionsObj,
                    bulkTransactionInstructions);
        }
    }

    private void insertIndicativeContract(LazyBatchSqlSessionTemplate lazyBatch,
            PwsTransactionFxContracts pwsTransactionFxContracts) {
        pwsTransactionFxContracts.setFxType(INDICATIVE);
        lazyBatch.insertAndFlush(BATCH_INSERT_PWS_TRANSACTION_FX_CONTRACTS, pwsTransactionFxContracts);
        log.info(INSERT_PWS_TRANSACTION_FX_CONTRACTS, pwsTransactionFxContracts);
    }

    private void insertPreebookContract(CreditTransferTransactionInformation creditTransferTransaction,
            String currencyCode, LazyBatchSqlSessionTemplate lazyBatch, PwsTransactions pwsTransactionsObj,
            PwsBulkTransactionInstructions bulkTransactionInstructions) {
        List<ForeignExchange> foreignExchanges = creditTransferTransaction.getSupplementaryData().get(0).getEnvelope()
                .getTransactionInformationSupplement().getExecutionLeg().getForex().getForeignExchange();
        if (isCurrencyCodeValid(currencyCode, foreignExchanges)) {
            for (ForeignExchange foreignExchange : foreignExchanges) {
                String contractId = null;
                ContractUtilisationAmount contractUtilisationAmount = null;
                if (Objects.nonNull(foreignExchange.getExchangeRateInformation())) {
                    contractId = foreignExchange.getExchangeRateInformation().getContractIdentification();
                    contractUtilisationAmount = foreignExchange.getExchangeRateInformation()
                            .getContractUtilisationAmount();
                }

                if (Objects.nonNull(contractId) && ObjectUtils.isNotEmpty(contractUtilisationAmount)) {
                    PwsTransactionFxContracts pwsTransactionFxContracts = new PwsTransactionFxContracts();
                    pwsTransactionFxContracts.setBankReferenceId(pwsTransactionsObj.getBankReferenceId());
                    pwsTransactionFxContracts
                            .setChildBankReferenceId(bulkTransactionInstructions.getChildBankReferenceId());
                    pwsTransactionFxContracts.setTransactionId(String.valueOf(pwsTransactionsObj.getTransactionId()));
                    pwsTransactionFxContracts.setFxType(PREBOOK);
                    pwsTransactionFxContracts.setContractNumber(contractId);
                    setFxTransactionCurrency(pwsTransactionFxContracts, contractUtilisationAmount);
                    setFxTransactionAmount(pwsTransactionFxContracts, contractUtilisationAmount);
                    lazyBatch.insertToQueue(BATCH_INSERT_PWS_TRANSACTION_FX_CONTRACTS, pwsTransactionFxContracts);
                    log.info(INSERT_PWS_TRANSACTION_FX_CONTRACTS, pwsTransactionFxContracts);
                }
            }
        }
    }

    private void setFxTransactionCurrency(PwsTransactionFxContracts pwsTransactionFxContracts,
            ContractUtilisationAmount contractUtilisationAmount) {
        if (ObjectUtils.isNotEmpty(contractUtilisationAmount.getCurrency())) {
            pwsTransactionFxContracts.setTransactionCurrency(contractUtilisationAmount.getCurrency());
        }
    }

    private void setFxTransactionAmount(PwsTransactionFxContracts pwsTransactionFxContracts,
            ContractUtilisationAmount contractUtilisationAmount) {
        if (ObjectUtils.isNotEmpty(contractUtilisationAmount.getAmount())) {
            pwsTransactionFxContracts.setTransactionAmount(new BigDecimal(contractUtilisationAmount.getAmount()));
        }
    }

    public void persistRejectedRecords(PwsFileUpload fileUpload, List<String> errorList,
            BatchInsertionTemplate batchSqlSession) {
        for (String error : errorList) {
            PwsRejectedRecord pwsRejectedRecord = new PwsRejectedRecord();
            pwsRejectedRecord.setEntityId(fileUpload.getFileUploadId());
            pwsRejectedRecord.setEntityType(ENTITY_ID);
            pwsRejectedRecord.setCreatedBy(fileUpload.getCreatedBy());
            pwsRejectedRecord.setTransactionId(fileUpload.getTransactionId());
            pwsRejectedRecord.setBankReferenceId(fileUpload.getBankReferenceId());
            pwsRejectedRecord.setRejectCode(fileUpload.getRejectCode());
            if (error.contains(SPLITER)) {
                try {
                    setPwsRejectedRecordForError(pwsRejectedRecord, error);
                } catch (NumberFormatException e) {
                    log.error("Exception getting while setting ErrorDescription", e);
                    pwsRejectedRecord.setErrorDetail(error);
                }
            } else {
                pwsRejectedRecord.setErrorDetail(error);
            }

            batchSqlSession.insertAndFlush(BATCH_INSERT_REJECTED_RECORD, pwsRejectedRecord);
            log.info("insertRejectedRecord fileUploadId:{} with ErrorDescription:{} ", fileUpload.getFileUploadId(),
                    error);
        }
    }

    private void setPwsRejectedRecordForError(PwsRejectedRecord pwsRejectedRecord, String error) {
        String[] errorSplitter = error.split(SPLITER);
        if (errorSplitter.length == THREE) {
            pwsRejectedRecord.setLineNo(Long.parseLong(errorSplitter[0]));
            pwsRejectedRecord.setColumNo(Long.parseLong(errorSplitter[1]));
            pwsRejectedRecord.setErrorDetail(errorSplitter[2]);
        } else {
            pwsRejectedRecord.setErrorDetail(error);
        }
    }

    public void persistTransitMessageDmpFile(String json, String fileReference, String serviceType,
            BatchInsertionTemplate batchSqlSession) {
        PwsTransitMessage pwsTransitMessage = new PwsTransitMessage();
        pwsTransitMessage.setCorrelationId(BLANK);
        pwsTransitMessage.setBankReferenceId(fileReference);
        pwsTransitMessage.setMessageRefNo(BLANK);
        pwsTransitMessage.setRetryCount(0);
        pwsTransitMessage.setMessageContent(json.getBytes());
        pwsTransitMessage.setServiceType(serviceType);
        pwsTransitMessage.setStatus("ERROR");
        pwsTransitMessage.setEndSystem(DMP);
        batchSqlSession.insertAndFlush(BATCH_PERSIST_TRANSIT_MESSAGE, pwsTransitMessage);
    }

    private Map<String, String> enrichWithHardCodedValues(String resource) {
        Map<String, String> refIdMetaData = new HashMap<>();
        refIdMetaData.put(COUNTRY, ENTITY_ID);
        refIdMetaData.put(CHANNEL, CHANNEL_VAL);
        if (CROSS_BORDER_UOBSEND.equals(resource)) {
            refIdMetaData.put(TRANSACTION, FEATURE_ID_BT);
        } else if (CROSS_BORDER_UOBSEND_CHILD.equals(resource)) {
            refIdMetaData.put(TRANSACTION, FEATURE_ID_ST);
        }
        return refIdMetaData;
    }

    private void setCompanyNameAndGroupId(PwsTransactions pwsTransactions, Company companies) {

        if (Objects.nonNull(companies)) {
            if (Objects.nonNull(companies.getCompanyGroupId())) {
                String comapnyGroupId = transactionUtils.getDecrypted(companies.getCompanyGroupId());
                pwsTransactions.setCompanyGroupId(Long.parseLong(comapnyGroupId));
            }
            pwsTransactions.setCompanyName(companies.getCompanyName());
        }
    }

    private Company getCompanyInfo(PwsFileUpload fileUpload, Map<String, Object> headers) {
        GetResourceAndFileTypeResp userResourceAndFileTypeData = commonUtil
                .getResourcesAndFileType(fileUpload.getCreatedBy(), FILE_RESOURCE_TYPE1, headers);
        log.info("userResourceAndFileTypeData: {}", userResourceAndFileTypeData);
        if (ObjectUtils.isNotEmpty(userResourceAndFileTypeData)
                && ObjectUtils.isNotEmpty(userResourceAndFileTypeData.getFileUploadTypes())) {
            return userResourceAndFileTypeData.getFileUploadTypes().stream()
                    .filter(companyList -> isValidCompanyId(fileUpload, companyList)).findAny()
                    .map(FileUploadTypesSchema::getCompanies).orElse(Collections.emptyList()).stream().findFirst()
                    .orElse(null);
        }
        return null;
    }

    private boolean isValidCompanyId(PwsFileUpload fileUpload, FileUploadTypesSchema companies) {
        return companies.getCompanies().stream().anyMatch(companyid -> fileUpload.getCompanyId()
                .equals(Long.parseLong(transactionUtils.getDecrypted(companyid.getCompanyId()))));
    }

    private void setTotalAmount(List<CreditTransferTransactionInformation> creditTransferTransactionInformationList,
            PwsTransactions pwsTransactions) {
        double totalAmount = 0;
        List<BigDecimal> childMaxAmountList = new ArrayList<>();
        for (CreditTransferTransactionInformation creditTransferTransactionInformation : creditTransferTransactionInformationList) {
            if (StringUtils
                    .isNoneEmpty(creditTransferTransactionInformation.getAmount().getInstructedAmount().getAmount())) {
                totalAmount += Double.parseDouble(
                        creditTransferTransactionInformation.getAmount().getInstructedAmount().getAmount());
                childMaxAmountList.add(BigDecimal.valueOf(Double.parseDouble(
                        creditTransferTransactionInformation.getAmount().getInstructedAmount().getAmount())));
            }
        }
        if (!childMaxAmountList.isEmpty()) {
            pwsTransactions.setHighestAmount(Collections.max(childMaxAmountList));
        } else {
            pwsTransactions.setHighestAmount(BigDecimal.valueOf(0));
        }
        pwsTransactions.setTotalAmount(BigDecimal.valueOf(totalAmount));
    }

    public String getBulkMsgIdr(String bankRefNumber) {
        return LocalDate.now().format(DATE_FORMATTER_YYYY_MM_DD) + bankRefNumber.substring(8, 16);
    }

    private String getBulkStatus(String bulkStatus) {
        if (ZERO_ONE.equals(bulkStatus)) {
            return ACCEPTED;
        } else if (ZERO_THREE.equals(bulkStatus)) {
            return PARTIAL;
        }
        return null;
    }

    private String getFileProcessingStatus(String fileStatusCode, PwsFileUpload fileUpload) {
        if (PARTIAL.equals(fileUpload.getStatus())) {
            return PARTIAL;
        } else {
            return switch (fileStatusCode) {
                case ZERO_ONE -> SUCCESS;
                case ZERO_TWO -> UPLOAD_FAILED;
                case ZERO_THREE -> PARTIAL;
                default -> null;
            };
        }

    }

    public Deal getDealBasedOnChild(ComputeEquivalentAmountResp computeEquivalentAmountResp,
            String childBankReferenceId, long transactionId) {
        var amountAllocation = computeEquivalentAmountResp.getAmountAllocationRespond().stream()
                .filter(amount -> String.valueOf(transactionId)
                        .equalsIgnoreCase(transactionUtils.getDecrypted(amount.getTransactionId())))
                .findFirst().orElse(new AmountAllocationRespond());
        return amountAllocation.getDeal().stream()
                .filter(dealVal -> childBankReferenceId.equalsIgnoreCase(dealVal.getBankReference())).findFirst()
                .orElse(new Deal());
    }

    public Timestamp getValueFromDate(String valueDate) {
        Timestamp dateVal = null;
        if (StringUtils.isNotEmpty(valueDate)) {
            LocalDate date = LocalDate.parse(valueDate);
            return getTimeStampDate(date);
        }
        return dateVal;
    }

    public Timestamp getTimeStampDate(LocalDate localDate) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yy HH:mm:ss.SSSSSSSS");

        if (null != localDate) {
            LocalDateTime localDateWithTime = localDate.atTime(LocalTime.MIDNIGHT);
            return Timestamp
                    .valueOf(LocalDateTime.from(dateTimeFormatter.parse(localDateWithTime.format(dateTimeFormatter))));
        }
        return null;
    }

    private String getPwsFileUplaodStatus(int noOfTnxCount, List<TransactionIdSchema> transactionIdList,
            String fileProcessingStatus) {
        log.info("noOfTnxCount {} , AcceptedTrxnCount {}", noOfTnxCount, transactionIdList.size());
        if (CollectionUtils.isEmpty(transactionIdList)) {
            return UPLOAD_FAILED;
        } else if (noOfTnxCount > transactionIdList.size()) {
            return PARTIAL;
        }
        return fileProcessingStatus;
    }
}
