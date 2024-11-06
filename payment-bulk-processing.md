## pain001 service

```java
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
package com.uob.gwb.pbp.service.impl;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.pbp.bo.BankRefMetaData;
import com.uob.gwb.pbp.bo.CreditTransferTransaction;
import com.uob.gwb.pbp.bo.PaymentInformation;
import com.uob.gwb.pbp.bo.status.DmpFileStatus;
import com.uob.gwb.pbp.config.AppConfig;
import com.uob.gwb.pbp.config.BulkRoutesConfig;
import com.uob.gwb.pbp.flow.BulkProcessingException;
import com.uob.gwb.pbp.flow.Pain001InboundProcessingResult;
import com.uob.gwb.pbp.flow.Pain001InboundProcessingStatus;
import com.uob.gwb.pbp.flow.PwsSaveRecord;
import com.uob.gwb.pbp.iso.pain001.GroupHeaderDTO;
import com.uob.gwb.pbp.iso.pain001.Pain001;
import com.uob.gwb.pbp.po.PwsTransactions;
import com.uob.gwb.pbp.service.Pain001Service;
import com.uob.gwb.pbp.service.PaymentDebulkService;
import com.uob.gwb.pbp.service.PaymentEnrichmentService;
import com.uob.gwb.pbp.service.PaymentMappingService;
import com.uob.gwb.pbp.service.PaymentSaveService;
import com.uob.gwb.pbp.service.PaymentValidationService;
import com.uob.gwb.pbp.util.Constants;
import com.uob.gwb.pbp.util.PaymentUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class Pain001ServiceImpl extends StepAwareService implements Pain001Service {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final PaymentUtils paymentUtils;
    private final PaymentMappingService paymentMappingService;
    private final PaymentEnrichmentService paymentEnrichmentService;
    private final PaymentValidationService paymentValidationService;
    private final PaymentDebulkService paymentDebulkService;
    private final PaymentSaveService paymentSaveService;

    @Override
    public List<PaymentInformation> validateJson(String json) {
        Pain001 pain001;
        try {
            pain001 = objectMapper.readValue(json, Pain001.class);
        } catch (JsonProcessingException e) {
            throw new BulkProcessingException("Error on parsing pain001 json", e);
        }
        GroupHeaderDTO groupHeaderDTO = pain001.getBusinessDocument()
                .getCustomerCreditTransferInitiation()
                .getGroupHeader();
        Pain001InboundProcessingResult result = getResult();
        result.setSourceReference(groupHeaderDTO.getFilereference());

        // stop at fileStatus = 02
        DmpFileStatus fileStatus = DmpFileStatus.fromValue(
                pain001.getBusinessDocument().getCustomerCreditTransferInitiation().getGroupHeader().getFilestatus());
        log.info("auth file status: {}", fileStatus);
        result.setDmpFileStatus(fileStatus);
        if (DmpFileStatus.REJECTED.equals(fileStatus)) {
            result.setProcessingStatus(Pain001InboundProcessingStatus.DmpRejected);
            // ToDo: create transit message 
            // ToDo: update pws_file_uploads
            return null; // stop processing
        }
        // ToDo: create transit message 
        // ToDo: update pws_file_uploads 
        
        // convert to BO
        Optional.ofNullable(groupHeaderDTO.getControlSum())
                .ifPresent((v -> result.setPaymentReceivedAmount(Double.valueOf(v))));
        Optional.ofNullable(groupHeaderDTO.getNumberOfTransactions())
                .ifPresent(v -> result.setTransactionReceivedTotal(Integer.valueOf(v)));
        List<PaymentInformation> paymentInfos = pain001.getBusinessDocument()
                .getCustomerCreditTransferInitiation()
                .getPaymentInformation()
                .stream()
                .map(paymentMappingService::pain001PaymentToBo)
                .collect(Collectors.toList());
        paymentInfos = paymentMappingService.postMappingPain001ToBo(pain001, paymentInfos);
        Optional.ofNullable(paymentInfos).ifPresent(v -> result.setPaymentReceivedTotal(v.size()));
        
        // data preparation
        // ToDo: pws_resource_configurations ?
        // ToDo: REJECT_FILE_ON_ERROR: SELECT REJECT_FILE_ON_ERROR FROM GEB_COMPANY_ATTRIBUTE_MVR WHERE COMPANY_ID = <@p name='companyId'/>
        // bank ref metadata 
        BulkRoutesConfig.BulkRoute routeConfig = getRouteConfig();
        BankRefMetaData bankRefMetaData = new BankRefMetaData(appConfig.getCountry().name(),
                routeConfig.getChannel().prefix, routeConfig.getRequestType().prefix,
                LocalDateTime.now().format(Constants.BANK_REF_YY_MM));
        setBankRefMetaData(bankRefMetaData);

        // ToDo: https://confluencep.sg.uobnet.com/display/GBTCEW/BFU+Mapping+Overview
        // row 8?
        // row 9?
        // row 10?
        
        result.setProcessingStatus(Pain001InboundProcessingStatus.Processing);
        updateProcessingResult(getRouteConfig(), result);
        
        return paymentInfos;
    }

    @Override
    public List<PaymentInformation> debulk(List<PaymentInformation> paymentInfos) {
        paymentDebulkService.beforeStep(getStepExecution());
        List<PaymentInformation> debulked = paymentDebulkService.debulk(paymentInfos);
        Pain001InboundProcessingResult result = getResult();
        result.setPaymentDebulkTotal(debulked.size());
        result.setProcessingStatus(Pain001InboundProcessingStatus.DebulkPassed);
        return debulked;
    }

    @Override
    public List<PaymentInformation> validate(List<PaymentInformation> paymentInfos) {
        // ToDo: entitlment check
        paymentEnrichmentService.beforeStep(getStepExecution());
        List<PaymentInformation> preEntitled = paymentEnrichmentService.enrichPreEntitlement(paymentInfos);
        paymentValidationService.beforeStep(getStepExecution());
        List<PaymentInformation> entitled = paymentValidationService.entitlementCheck(preEntitled);
        updateProcessingResult(getRouteConfig(), getResult());
        
        // validations 
        List<PaymentInformation> preValidated = paymentEnrichmentService.enrichPreValidation(paymentInfos);
        List<PaymentInformation> validated = paymentValidationService.validate(preValidated);
        updateAfterValidation(validated);
        updateProcessingResult(getRouteConfig(), getResult());
        return validated;
    }

    @Override
    public List<PaymentInformation> enrich(List<PaymentInformation> paymentInfos) {
        // ToDo: PWS_TAX_INSTRUCTIONS.TAX_AMOUNT (SUM of all tax records for the txn)
        paymentEnrichmentService.beforeStep(getStepExecution());
        List<PaymentInformation> enriched = paymentEnrichmentService.enrichPostValidation(paymentInfos);
        updateProcessingResult(getRouteConfig(), getResult());
        return enriched;
    }

    @Override
    public void save(List<PaymentInformation> paymentInfos) {
        paymentSaveService.beforeStep(getStepExecution());
        boolean noError = true;
        for (PaymentInformation paymentInfo : paymentInfos) {
            if (!paymentInfo.isValid()) {
                try {
                    paymentSaveService.savePaymentInformation(paymentInfo);
                } catch (Exception e) {
                    noError = false;
                    log.error("Failed saving payment", e);
                    Pain001InboundProcessingResult result = getResult();
                    PwsSaveRecord record = paymentUtils.createPwsSaveRecord(
                            paymentInfo.getPwsTransactions().getTransactionId(),
                            paymentInfo.getPwsBulkTransactions().getDmpBatchNumber());
                    paymentUtils.updatePaymentSavedError(result, record);
                    // ToDo: clean up dirty payment data
                }
            } else {
                log.warn("Skipping invalid payment: {}", paymentInfo.getPwsBulkTransactions().getDmpBatchNumber());
            }
        }

        Pain001InboundProcessingResult result = getResult();
        if (noError) {
            result.setProcessingStatus(Pain001InboundProcessingStatus.SavePassed);
        } else {
            result.setProcessingStatus(Pain001InboundProcessingStatus.SaveWithException);
        }
        updateProcessingResult(getRouteConfig(), result);
    }
    

    private void updateAfterValidation(List<PaymentInformation> paymentInfos) {
        int paymentValidTotal = 0;
        BigDecimal paymentValidAmount = BigDecimal.ZERO;
        int paymentInvalidTotal = 0;
        BigDecimal paymentInvalidAmount = BigDecimal.ZERO;

        for (PaymentInformation paymentInfo : paymentInfos) {
            boolean validPayment = paymentInfo.isValid();

            int childTxnValidTotal = 0;
            BigDecimal childTxnValidAmount = BigDecimal.ZERO;
            BigDecimal childTxnMaxAmount = BigDecimal.ZERO;
            BigDecimal childTxnInvalidAmount = BigDecimal.ZERO;

            for (CreditTransferTransaction txn : paymentInfo.getCreditTransferTransactionList()) {
                BigDecimal txnAmount = txn.getPwsBulkTransactionInstructions().getTransactionAmount();
                
                if (validPayment && txn.isValid()) {
                    // for valid payment & valid txn
                    childTxnValidTotal += 1;
                    childTxnValidAmount = childTxnValidAmount.add(txnAmount);
                    if (txnAmount.compareTo(childTxnMaxAmount) < 0) {
                        childTxnMaxAmount = txnAmount;
                    }
                }

                // for valid payment & invalid txn
                // for invalid payment
                childTxnInvalidAmount = childTxnInvalidAmount.add(txnAmount);
            }

            if (validPayment && childTxnValidTotal > 0) {
                PwsTransactions pwsTransactions = paymentInfo.getPwsTransactions();
                pwsTransactions.setTotalChild(childTxnValidTotal);
                pwsTransactions.setTotalAmount(childTxnValidAmount);
                pwsTransactions.setMaximumAmount(childTxnMaxAmount);
                paymentValidTotal += 1;
                paymentValidAmount.add(childTxnValidAmount);
                continue;
            }

            paymentInvalidTotal += 1;
            paymentInvalidAmount.add(childTxnInvalidAmount);
        }

        // update result
        Pain001InboundProcessingResult result = getResult();
        result.setPaymentValidTotal(paymentValidTotal);
        result.setPaymentValidAmount(paymentValidAmount);
        result.setPaymentInvalidTotal(paymentInvalidTotal);
        result.setPaymentInvalidAmount(paymentInvalidAmount);
    }

    private void handleRejectFileOnError(BulkRoutesConfig.BulkRoute routeConfig, Pain001InboundProcessingResult result) {
        // ToDo 
    }

    private void updateProcessingResult(BulkRoutesConfig.BulkRoute routeConfig, Pain001InboundProcessingResult result) {
        // ToDo
        // update pws_transit_messages
    }

    
}

```
