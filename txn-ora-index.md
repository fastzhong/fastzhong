# count 

```sql

```

# IF Reg

You can also use a regex search for a pair of opening and closing tags:
```txt
<#if\b[^>]*>[\s\S]*?</#if>
```

Search for <#if> without a corresponding </#if>:
```txt
<#if\b[^>]*>(?![\s\S]*?</#if>)
```

Search for <#/if> without a corresponding </#if>:
```txt
</#if>(?![\s\S]*?<)
```

# Mappe 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.uob.gwb.txn.pws.dao.TransactionWorkflowDAO">

    <select id="getApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
        SELECT
        COUNT(*) AS count FROM
        (select DISTINCT  txn.transaction_id
        FROM PWS_TRANSACTIONS txn
        INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
        LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
        AND ptfc.bank_reference_id = txn.bank_reference_id
        LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
        WHERE txn.customer_transaction_status NOT IN ('PENDING_SUBMIT', 'NEW','DELETED')
        AND (pty.party_role != 'DEBTOR' OR pty.party_role IS null)

        ]]>
		<include refid="fragmentGetApprovalStatusTxnCount"></include>
        <![CDATA[
        <#if filterParams.excludeApplicationType?has_content && filterParams.excludeApplicationType??>
             AND txn.application_type not in (<#list filterParams.excludeApplicationType as excludeApplicationTypeVal> <@p value=excludeApplicationTypeVal/> <#if excludeApplicationTypeVal_has_next>,</#if> </#list> )
        </#if>
          )
		]]>
    </select>
    <select id="getApprovalStatusTxnList" resultMap="ApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
        SELECT
            distinct txn.transaction_id,
            txn.account_number,
            pty.party_account_currency as account_currency,
            txn.initiation_time,
            txn.customer_transaction_status,
            txn.bank_reference_id,
            txn.resource_id,
            txn.feature_id,
            txn.company_id,
            txn.company_group_id,
            txn.CHANGE_TOKEN,
            txni.transaction_currency,
            txni.transaction_amount,
            txni.customer_reference,
            txni.value_date,
            txni.original_uetr,
            txni.is_recurring,
            txni.destination_country,
            txni.destination_bank_name,
            txni.fx_flag,
            pty.party_name,
            pty.party_account_number,
            pty.party_account_currency,
            pty.bank_code,
            pty.swift_code,
            pty.beneficiary_reference_id,
            pty.party_type,
            pty.residency_status,
            pty.party_id,
            pty.proxy_id_type,
            pty.id_issuing_country,
            ptyc.address_line_1,
            ptyc.address_line_2,
            ptyc.address_line_3,
            ptyc.phone_country,
            ptyc.phone_country_code,
            ptyc.phone_no,
            ptfc.fx_type,
            ptfb.booking_ref_id,
            ptfb.earmark_id,
            txni.original_uetr,
            txn.bank_entity_id
        FROM PWS_TRANSACTIONS txn
        INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
        LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
        AND ptfc.bank_reference_id = txn.bank_reference_id
        LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
        WHERE txn.customer_transaction_status NOT IN ('PENDING_SUBMIT', 'NEW','DELETED')
        AND (pty.party_role != 'DEBTOR' OR pty.party_role IS null)

        ]]>
        <include refid="fragmentGetApprovalStatusTxnCount"></include>
        <![CDATA[
        <#if filterParams.excludeApplicationType?has_content && filterParams.excludeApplicationType??>
             AND txn.application_type not in (<#list filterParams.excludeApplicationType as excludeApplicationTypeVal> <@p value=excludeApplicationTypeVal/> <#if excludeApplicationTypeVal_has_next>,</#if> </#list> )
        </#if>
        ORDER BY ${filterParams.sortFieldWithDirection}, txn.transaction_id
        OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>

    <resultMap id="ApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="company_id" property="companyId"/>
        <result column="transaction_currency" property="transactionCurrency"/>
        <result column="transaction_amount" property="transactionAmount"/>
        <result column="customer_reference" property="customerReference"/>
        <result column="value_date" property="valueDate"/>
        <result column="value_date" property="boReleaseDateTime"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="value_date" property="boReleaseDateTime"/>
        <result column="original_uetr" property="originalUetr"/>
        <result column="total_count" property="totalCount"/>
        <result column="total_count" property="count"/>
        <result column="bank_entity_id" property="bankEntityId"/>
    </resultMap>

    <select id="getMyTaskList" resultMap="MyTaskMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
                txn.change_token,
				txn.account_number,
				txn.account_currency,
				txn.initiation_time,
				txn.bank_reference_id,
				txn.customer_transaction_status,
                txn.authorization_status,
				txn.company_id,
				txn.resource_id,
				txn.feature_id,
				txn.initiated_by,
				txn.total_child,
				txn.highest_amount,
				txn.total_amount,
				txn.transaction_currency transactionCurrency,
				txnc.fees_currency,
				txnc.fees_amount,
				txni.customer_reference AS RECIPIENTS_REFERENCE,
				txni.value_date,
				txni.transaction_currency,
				txni.transaction_amount,
				txni.is_recurring,
				txni.destination_country,
				txni.destination_bank_name,
				txni.fx_flag,
				txni.purpose_codes,
				pty.party_name,
				pty.party_account_number,
				pty.party_account_currency,
				pty.bank_code,
				pty.swift_code,
				pty.beneficiary_reference_id,
				pty.party_type,
				pty.residency_status,
				pty.party_id,
				pty.proxy_id_type,
				pty.id_issuing_country,
				ptyc.address_line_1,
				ptyc.address_line_2,
				ptyc.address_line_3,
				ptyc.phone_country,
				ptyc.phone_country_code,
				ptyc.phone_no,
				txnfxb.booking_ref_id,
				txnfxb.earmark_id,
				txnfxc.fx_type,
				null as UPLOAD_FILE_NAME,
				txn.bank_entity_id
			FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id  AND (pty.party_role != 'DEBTOR' OR pty.party_role IS null)
				LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS txnfxc on txnfxc.transaction_id = txn.transaction_id
						   AND txnfxc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS txnfxb on txnfxb.booking_id = txnfxc.booking_id
				LEFT JOIN PWS_TRANSACTION_CHARGES txnc on txn.transaction_id = txnc.transaction_id
			WHERE 1=1
			AND txn.BATCH_ID is null
			    <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
                    AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		            in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                </#if>

				<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>

                <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			 AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
       			</#if>
                <#if filterParams.multiCustomerReferenceIds?has_content && filterParams.multiCustomerReferenceIds??>
		             AND UPPER(txni.customer_reference) in (<#list filterParams.multiCustomerReferenceIds as cusRef> UPPER(<@p value=cusRef/>)  <#if cusRef_has_next>,</#if> </#list>)

                </#if>
		        <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                      AND UPPER(txni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')

                </#if>
               <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                         AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                          AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
               </#if>
               <#if filterParams.statusList?has_content && filterParams.statusList??>
                          AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
	            <#if filterParams.transactionFromTotalAmount?has_content && filterParams.transactionToTotalAmount?has_content >
					and txn.total_amount between <@p name='filterParams.transactionFromTotalAmount'/> and <@p name='filterParams.transactionToTotalAmount'/>
	            </#if>
	            <#if filterParams.payerName?has_content && filterParams.payerName??>
                    AND UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%')
                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
                          AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
                          AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
                          AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )

                </#if>
               <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
                          AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(txni.value_date) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
                <#if filterParams.bulkFromDate?has_content && filterParams.bulkFromDate?? && filterParams.bulkToDate?has_content && filterParams.bulkToDate??>
                          AND trunc(btxn.transfer_date) between '${filterParams.bulkFromDate}' and '${filterParams.bulkToDate}'
                </#if>
                <#if filterParams.transactionId?has_content>
                          AND txn.transaction_id in (${filterParams.transactionId})
                </#if>
                <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		         		  AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                </#if>
                <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		            	  AND txn.TRANSACTION_ID not in (${filterParams.excludeTransactionId})
                </#if>
                <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                    AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                </#if>
                <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                    AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                </#if>
                <#if filterParams.excludeApplicationType?has_content && filterParams.excludeApplicationType??>
                     AND txn.application_type not in (<#list filterParams.excludeApplicationType as excludeApplicationTypeVal> <@p value=excludeApplicationTypeVal/> <#if excludeApplicationTypeVal_has_next>,</#if> </#list> )
                </#if>
			ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>

    <resultMap id="MyTaskMap"
               type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="transactionId"/>
        <id column="change_token" property="changeToken"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="initiated_by" property="userId"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resourceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="company_id" property="companyId"/>
        <result column="transaction_currency" property="transactionCurrency"/>
        <result column="transaction_amount" property="transactionAmount"/>
        <result column="customer_reference" property="customerReference"/>
        <result column="value_date" property="valueDate"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="purpose_codes" property="purposeCodes"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="transaction_currency" property="txnCurrency"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="total_child" property="numberOfChildTransactions"/>
        <result column="highest_amount" property="highestAmount"/>
        <result column="transaction_amount" property="totalAmount"/>
        <result column="recipients_reference" property="recipientsReference"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="fees_currency" property="feesCurrency"/>
        <result column="fees_amount" property="totalFees"/>
        <result column="transactionCurrency" property="txnCurrency"/>
        <result column="authorization_status" property="authorizationStatus"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
        <result column="rownum" property="rowNum"/>
        <result column="bank_entity_id" property="bankEntityId"/>
    </resultMap>

    <select id="getSingleTransactionDetails" resultMap="transDetailMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				txn.TRANSACTION_ID,
				txn.ACCOUNT_NUMBER,
				txn.COMPANY_ID,
				txn.CHANGE_TOKEN,
				txni.TRANSACTION_CURRENCY,
				txnfxcon.BOOKING_ID
			FROM PWS_TRANSACTIONS txn
                 LEFT JOIN PWS_TRANSACTION_INSTRUCTIONS txni ON txn.TRANSACTION_ID = txni.TRANSACTION_ID
                 LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS txnfxcon on txn.TRANSACTION_ID=txnfxcon.TRANSACTION_ID
 			WHERE txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
 			and txn.customer_transaction_status NOT IN ('DELETED','PENDING_SEND','PROCESSING','SUCCESSFUL','REJECTED')
        ]]>
    </select>
    <select id="getBulkTransactionDetails" resultMap="transDetailMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				txn.TRANSACTION_ID,
				txn.ACCOUNT_NUMBER,
				txn.COMPANY_ID,
				txn.CHANGE_TOKEN,
				txni.TRANSACTION_CURRENCY,
				txnfxcon.BOOKING_ID
			FROM PWS_TRANSACTIONS txn
                 LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS txni ON txn.TRANSACTION_ID = txni.TRANSACTION_ID
				 LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS txnfxcon on txn.TRANSACTION_ID=txnfxcon.TRANSACTION_ID
 			WHERE txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
 			and txn.customer_transaction_status NOT IN ('DELETED','PENDING_SEND','PROCESSING','SUCCESSFUL','REJECTED')
 				]]>
    </select>
    <resultMap id="transDetailMap"
               type="com.uob.gwb.txn.domain.BookFXTransaction">
        <id column="TRANSACTION_ID" property="transactionId" />
        <result column="ACCOUNT_NUMBER" property="accountNumber" />
        <result column="COMPANY_ID" property="companyId" />
        <result column="CHANGE_TOKEN" property="changeToken" />
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency" />
        <result column="BOOKING_ID" property="bookingId" />
    </resultMap>

    <insert id="insertEarmarkingId"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq"
            statementType="PREPARED" useGeneratedKeys="true"
            keyProperty="bookingId" keyColumn="BOOKING_ID">
        <![CDATA[insert into pws_transaction_fx_bookings
           (
	            EARMARK_ID,QUOTE_ID
	       )
	       values(
	             <@p name='fundsResReq.earmarkId'/>,
	             <@p name='fundsResReq.quoteId'/>
	       )]]>
    </insert>

    <select id="updateBookingIdInFxContracts"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_contracts con
					set con.booking_id = <@p name='fundsResReq.bookingId'/>
				  where con.transaction_id IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="updateFXBookingBasedOnTransaction"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_bookings book
					SET book.QUOTE_ID = <@p name='fundsResReq.quoteId'/>,
					    book.BOOKING_TYPE = <@p name='fundsResReq.bookingType'/>,
					    book.BOOKING_REF_ID = <@p name='fundsResReq.bookingRefId'/>,
					    book.RATE = <@p name='fundsResReq.rate'/>,
					    book.UNIT = <@p name='fundsResReq.unit'/>,
					    book.STATUS = <@p name='fundsResReq.status'/>,
					    book.TRANSACTION_CURRENCY = <@p name='fundsResReq.transactionCurrency'/>,
					    book.TRANSACTION_TOTAL_AMOUNT = <@p name='fundsResReq.transactionTotalAmount'/>,
					    book.EQUIVALENT_CURRENCY = <@p name='fundsResReq.equivalentCurrency'/>,
					    book.EQUIVALENT_TOTAL_AMOUNT = <@p name='fundsResReq.equivalentTotalAmount'/>,
					    book.BOOKING_DATE = <@p name='fundsResReq.bookingDate'/>,
					    book.BOOKED_BY = <@p name='fundsResReq.bookedBy'/>,
					    book.INSTRUCTION_TRACE_ID = <@p name='fundsResReq.instructionTraceId'/>,
					    book.SETTLEMENT_CURRENCY = <@p name='fundsResReq.settlementCurrency'/>,
					    book.SETTLEMENT_AMOUNT = <@p name='fundsResReq.settlementAmount'/>
				    where book.BOOKING_ID = <@p name='fundsResReq.bookingId'/>

			]]>
    </select>

    <select id="updateFxTypeInFXContracts"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_contracts con
					SET con.FX_TYPE = <@p name='fundsResReq.fxType'/>,
					con.SETTLEMENT_CURRENCY = <@p name='fundsResReq.settlementCurrency'/>,
					con.RATE = <@p name='fundsResReq.rate'/>,
					con.SETTLEMENT_AMOUNT = <@p name='fundsResReq.settlementAmount'/>
				    where con.transaction_id IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="updateEquivalentAmountInFXContracts"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_contracts con
					SET con.EQUIVALENT_AMOUNT = <@p name='fundsResReq.totalEquivalentAmount'/>
				     , con.SETTLEMENT_AMOUNT = <@p name='fundsResReq.settlementAmount'/>
					where con.TRANSACTION_ID = <@p name='fundsResReq.transactionId'/>
				    <#if fundsResReq.featureStatus??>
				    AND con.CHILD_BANK_REFERENCE_ID = <@p name='fundsResReq.childReferenceId'/>
				     </#if>
			]]>
    </select>

    <select id="updateEquivalentAmountInInstructions"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE PWS_TRANSACTION_INSTRUCTIONS ins
					SET ins.EQUIVALENT_AMOUNT = <@p name='fundsResReq.totalEquivalentAmount'/>
				    where ins.TRANSACTION_ID = <@p name='fundsResReq.transactionId'/>
			]]>
    </select>
    <select id="getTransactionContract" resultMap="transContractMap" statementType="PREPARED">
        <![CDATA[
			SELECT
				txn.TRANSACTION_ID,
				txn.BANK_REFERENCE_ID,
				txn.RESPONSE_MESSAGE,
				txn.TRANSACTION_AMOUNT,
				txn.EQUIVALENT_AMOUNT,
				txn.CURRENCY_PAIR,
				txnfxb.EQUIVALENT_TOTAL_AMOUNT,
                txnfxb.RATE
			FROM PWS_TRANSACTION_FX_CONTRACTS txn
			LEFT JOIN pws_transaction_fx_bookings txnfxb on txnfxb.booking_id = txn.booking_id
			WHERE txn.TRANSACTION_ID IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
 				]]>
    </select>
    <resultMap id="transContractMap"
               type="com.uob.gwb.txn.domain.FundReservationBookingIdReq">
        <id column="TRANSACTION_ID" property="transactionId" />
        <result column="BANK_REFERENCE_ID" property="bankReferenceId" />
        <result column="RESPONSE_MESSAGE" property="status" />
        <result column="TRANSACTION_AMOUNT" property="transactionTotalAmount" />
        <result column="EQUIVALENT_AMOUNT" property="equivalentTotalAmount" />
        <result column="CURRENCY_PAIR" property="currencyPair" />
        <result column="EQUIVALENT_TOTAL_AMOUNT" property="totalEquivalentAmount" />
        <result column="RATE" property="rate" />
    </resultMap>
    <select id="getBulkApprovalStatusTxnList" resultMap="bulkApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT DISTINCT
				btxni.CHILD_TRANSACTION_INSTRUCTIONS_ID,
				txn.CHANGE_TOKEN,
				btxni.CHILD_BANK_REFERENCE_ID,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.TRANSACTION_CURRENCY,
				btxni.TRANSACTION_AMOUNT bulkTransactionAmount,
				btxn.TRANSFER_DATE as VALUE_DATE,
				 <#if filterParams.isChildY ?has_content >
       		   NVL (btxni.initiation_time,txn.initiation_time) AS initiation_time,
       		   btxn.transfer_date,
       		   btxni.original_uetr,
   		         </#if>
                <#if filterParams.isChildN ?has_content >
      			  txn.initiation_time,
 		        </#if>
				btxni.CUSTOMER_REFERENCE bulkCustomerReference,
				btxni.CUSTOMER_TRANSACTION_STATUS bulkCustomerTransactionStatus,
                txn.company_id,
                pty.beneficiary_reference_id,
                btxni.DESTINATION_COUNTRY bulkDestinationCountry,
                btxni.DESTINATION_BANK_NAME bulkDestinationBankName,
                pty.bank_code,
                pty.swift_code,
                pty.party_name,
                pty.party_account_number,
                pty.party_account_currency,
                pty.party_id,
                pty.proxy_id,
				pty.proxy_id_type,
                txn.TOTAL_CHILD,
                txn.account_number,
                pty.party_modified_date,
                pty.beneficiary_change_token,
                pfu.UPLOAD_FILE_NAME,
                txn.HIGHEST_AMOUNT
			FROM PWS_TRANSACTIONS txn
				LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on txn.transaction_id = btxni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = btxni.transaction_id and btxni.CHILD_BANK_REFERENCE_ID = pty.CHILD_BANK_REFERENCE_ID
		        LEFT JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
				LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE 1=1
			    <#if filterParams.proxyIds?has_content && filterParams.proxyIds??>
                          AND pty.proxy_id in (<#list filterParams.proxyIds as proxyId> <@p value=proxyId/> <#if proxyId_has_next>,</#if> </#list> )
                </#if>
                <#if filterParams.proxyIdTypes?has_content && filterParams.proxyIdTypes??>
                          AND pty.proxy_id_type in (<#list filterParams.proxyIdTypes as proxyIdType> <@p value=proxyIdType/> <#if proxyIdType_has_next>,</#if> </#list> )
                </#if>
                    <#if filterParams.customerTransactionStatusList?has_content && filterParams.customerTransactionStatusList??>
		             AND txn.customer_transaction_status NOT IN
		             (<#list filterParams.customerTransactionStatusList as customerTransactionStatus> <@p value=customerTransactionStatus/> <#if customerTransactionStatus_has_next>,</#if> </#list> )
                   </#if>
                     <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                            AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                            AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                         </#if>
                     </#if>
                   <#if filterParams.bulkCustomerTransactionStatus?has_content && filterParams.bulkCustomerTransactionStatus??>
		              AND btxni.CUSTOMER_TRANSACTION_STATUS NOT IN ( ${filterParams.bulkCustomerTransactionStatus})
                  </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                  <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		              AND txn.company_group_id in ${filterParams.companyGroupId}
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		              AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>
                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in  (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                    </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					       and btxni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
					 </#if>
					 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					    and btxni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					    and btxni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
					  </#if>
                      <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                     </#if>
	                 <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		               AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
                     </#if>
                     <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                            AND UPPER(btxni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')
                      </#if>

                          <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                             AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )
                          </#if>
                           <#if filterParams.payerName ?? && filterParams.payerName?has_content>
                             AND pty.party_name LIKE ('%${filterParams.payerName}%')
                          </#if>
                          <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		                     AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )
                          </#if>

                          <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		                       AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )
                          </#if>
                          <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		                       AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                          </#if>

                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>

                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
                     <#if filterParams.minNumberOfChildTransactions?has_content && filterParams.minNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD >= ${filterParams.minNumberOfChildTransactions}
                     </#if>
                      <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                          AND btxni.customer_reference IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)
                      </#if>
                      <#if filterParams.excludeBatch == true>
                          AND txn.batch_id is null
                      </#if>
                      <#if filterParams.batchId?has_content>
                          AND txn.batch_id = <@p name='filterParams.batchId'/>
                      </#if>

                 ORDER BY ${filterParams.sortFieldWithDirection}
			     OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>

    <resultMap id="bulkApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="CHILD_TRANSACTION_INSTRUCTIONS_ID" property="transactionId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="CHILD_BANK_REFERENCE_ID" property="childBankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="bulkTransactionAmount" property="bulkTransactionAmount"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="transfer_date" property="boReleaseDateTime"/>
        <result column="original_uetr" property="originalUetr"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="bulkCustomerReference" property="bulkCustomerReference"/>
        <result column="bulkCustomerTransactionStatus" property="customerStatus"/>
        <result column="company_id" property="companyId"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="bulkDestinationCountry" property="bulkDestinationCountry"/>
        <result column="bulkDestinationBankName" property="bulkDestinationBankName"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id" property="proxyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="TOTAL_CHILD" property="totalChild"/>
        <result column="account_number" property="accountNumber"/>
        <result column="party_modified_date" property="partyModifiedDate"/>
        <result column="beneficiary_change_token" property="beneficiaryChangeToken"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
    </resultMap>

    <select id="getBulkApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count FROM
				(
				SELECT DISTINCT
				btxni.CHILD_TRANSACTION_INSTRUCTIONS_ID
			FROM PWS_TRANSACTIONS txn
				LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on txn.transaction_id = btxni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = btxni.transaction_id and btxni.CHILD_BANK_REFERENCE_ID = pty.CHILD_BANK_REFERENCE_ID
		        LEFT JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
				LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
                    <#if filterParams.customerTransactionStatusList?has_content && filterParams.customerTransactionStatusList??>
		               txn.customer_transaction_status NOT IN
		                  (<#list filterParams.customerTransactionStatusList as customerTransactionStatus> <@p value=customerTransactionStatus/> <#if customerTransactionStatus_has_next>,</#if> </#list> )
                    </#if>
                 <#if filterParams.bulkCustomerTransactionStatus?has_content && filterParams.bulkCustomerTransactionStatus??>
		              AND btxni.CUSTOMER_TRANSACTION_STATUS NOT IN ( ${filterParams.bulkCustomerTransactionStatus})
                  </#if>
                 <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                            AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                            AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                         </#if>
                     </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                   <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		              AND txn.company_group_id in ${filterParams.companyGroupId}
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		              AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                </#if>
                   <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>

                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in  (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                 </#if>
                 <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                 </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					       and btxni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
					 </#if>
					 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					    and btxni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					    and btxni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
					  </#if>
                    <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                      </#if>
	                 <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		                AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
                     </#if>
                     <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                           AND UPPER(btxni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')
                     </#if>
                      <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                          AND btxni.customer_reference IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)
                      </#if>
                     <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                            AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.payerName ?? && filterParams.payerName?has_content>
                             AND pty.party_name LIKE ('%${filterParams.payerName}%')
                          </#if>

                    <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		                    AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )
                    </#if>

                    <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		                    AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )
                    </#if>
                     <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		                    AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>

                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
                     <#if filterParams.minNumberOfChildTransactions?has_content && filterParams.minNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD >= ${filterParams.minNumberOfChildTransactions}
                     </#if>

                )

		]]>
    </select>
    <resultMap id="bulkApprovalStatusTxnCount"
               type="int">
        <result column="count" property="count" />
    </resultMap>
    <select id="getBulkParentApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count FROM
				(SELECT DISTINCT txn.transaction_id
			FROM PWS_TRANSACTIONS txn
			    LEFT JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
			    LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
			     <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                   txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		               txn.company_group_id in ${filterParams.companyGroupId} AND
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		               txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
                </#if>
	               <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		             txn.TRANSACTION_ID in (${filterParams.transactionId}) AND
                </#if>
               <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                             txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                             txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
                         </#if>
                     </#if>
                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		              txn.TRANSACTION_ID  not in (${filterParams.excludeTransactionId}) AND
                 </#if>
                 <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
                       (
					   <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
					   (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               IN
							(
                                <#list chunk as companyAccountGroup>
                                <@p value=companyAccountGroup/>
                                <#if companyAccountGroup_has_next>,</#if>
                            </#list>
                            )
                            <#if chunk_has_next> OR </#if>
                            </#list> ) AND
                 </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		              txn.COMPANY_NAME in ${filterParams.companyName} AND
                  </#if>
                  <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		               txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		                txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
                    </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
                         txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> and
					 </#if>
					  <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					     txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> and
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					     txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> and
					  </#if>
					  <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                           txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                     </#if>
	                 <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		                UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
                     </#if>
                     <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                             UPPER(btxn.RECIPIENTS_REFERENCE) LIKE UPPER ('%${filterParams.customerRefIds}%') AND
                      </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                           trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                 txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.minNumberOfChildTransactions?has_content && filterParams.minNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD >= ${filterParams.minNumberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.batchId?has_content>
                         txn.batch_id = <@p name='filterParams.batchId'/> AND
                     </#if>
                     txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )
                )

		]]>
    </select>

    <select id="getBulkBothApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
        SELECT
            COUNT(*) AS count
        FROM (
        <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
        SELECT DISTINCT
            txn.transaction_id
        FROM PWS_TRANSACTIONS txn
        INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
        LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
        AND ptfc.bank_reference_id = txn.bank_reference_id
        LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
        WHERE
        ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount1"></include>
        <![CDATA[
        AND (pty.party_role != 'DEBTOR' OR pty.party_role IS null)
        <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
       UNION ALL
        </#if>
        <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
        SELECT DISTINCT
            txn.transaction_id
        FROM PWS_TRANSACTIONS txn
        LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
        LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
        WHERE
        ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount2"></include>
        <![CDATA[
        </#if>
        )
		]]>
    </select>
    <select id="getBulkBothApprovalStatusTxnList" resultMap="bothParentApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
        SELECT DISTINCT
            transaction_id,
            account_number,
            account_currency,
            initiation_time,
            customer_transaction_status,
            bank_reference_id,
            resource_id,
            feature_id,
            company_id,
            company_group_id,
            CHANGE_TOKEN,
            transaction_currency,
            TOTAL_AMOUNT,
            RECIPIENTS_REFERENCE,
            VALUE_DATE,
            is_recurring,
            destination_country,
            destination_bank_name,
            fx_flag,
            party_name,
            party_account_number,
            party_account_currency,
            bank_code,
            swift_code,
            beneficiary_reference_id,
            party_type,
            residency_status,
            party_id,
            proxy_id_type,
            id_issuing_country,
            address_line_1,
            address_line_2,
            address_line_3,
            phone_country,
            phone_country_code,
            phone_no,
            fx_type,
            booking_ref_id,
            earmark_id,
            TOTAL_CHILD,
            HIGHEST_AMOUNT,
            upload_file_name
        FROM (
        <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
        SELECT
            txn.transaction_id,
            txn.account_number,
            txn.account_currency,
            txn.initiation_time,
            txn.customer_transaction_status,
            txn.bank_reference_id,
            txn.resource_id,
            txn.feature_id,
            txn.company_id,
            txn.company_group_id,
            txn.CHANGE_TOKEN,
            txni.transaction_currency,
            txni.transaction_amount AS TOTAL_AMOUNT,
            txni.customer_reference AS RECIPIENTS_REFERENCE,
            txni.value_date as VALUE_DATE,
            txni.is_recurring,
            txni.destination_country,
            txni.destination_bank_name,
            txni.fx_flag,
            pty.party_name,
            pty.party_account_number,
            pty.party_account_currency,
            pty.bank_code,
            pty.swift_code,
            pty.beneficiary_reference_id,
            pty.party_type,
            pty.residency_status,
            pty.party_id,
            pty.proxy_id_type,
            pty.id_issuing_country,
            ptyc.address_line_1,
            ptyc.address_line_2,
            ptyc.address_line_3,
            ptyc.phone_country,
            ptyc.phone_country_code,
            ptyc.phone_no,
            ptfc.fx_type,
            ptfb.booking_ref_id,
            ptfb.earmark_id,
            txn.TOTAL_CHILD,
            txn.HIGHEST_AMOUNT,
            null AS UPLOAD_FILE_NAME
        FROM PWS_TRANSACTIONS txn
        INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
        LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
        LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
            AND ptfc.bank_reference_id = txn.bank_reference_id
        LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
        WHERE
        ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount1"></include>
        <![CDATA[
        AND (pty.party_role != 'DEBTOR' OR pty.party_role IS null)
        <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
        UNION ALL
        </#if>
        <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
        SELECT
            txn.transaction_id,
            txn.account_number,
            txn.account_currency,
            txn.initiation_time,
            txn.customer_transaction_status,
            txn.bank_reference_id,
            txn.resource_id,
            txn.feature_id,
            txn.company_id,
            txn.company_group_id,
            txn.CHANGE_TOKEN,
            txn.TRANSACTION_CURRENCY,
            txn.TOTAL_AMOUNT,
            btxn.RECIPIENTS_REFERENCE,
            btxn.TRANSFER_DATE as VALUE_DATE,
            null as is_recurring,
            null as destination_country,
            null as destination_bank_name,
            null as fx_flag,
            null as party_name,
            null as party_account_number,
            null as party_account_currency,
            null as bank_code,
            null as swift_code,
            null as beneficiary_reference_id,
            null as party_type,
            null as residency_status,
            null as party_id,
            null as proxy_id_type,
            null as id_issuing_country,
            null as address_line_1,
            null as address_line_2,
            null as address_line_3,
            null as phone_country,
            null as phone_country_code,
            null as phone_no,
            null as fx_type,
            null as booking_ref_id,
            null as earmark_id,
            txn.TOTAL_CHILD,
            txn.HIGHEST_AMOUNT,
            pfu.upload_file_name AS UPLOAD_FILE_NAME
        FROM PWS_TRANSACTIONS txn
        LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
        LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
        WHERE
        ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount2"></include>
        <![CDATA[
        </#if>
        ) bothTrans
         ORDER BY ${filterParams.sortFieldWithDirection}, transaction_id
         OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>
    <resultMap id="bothParentApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="company_id" property="companyId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="TOTAL_AMOUNT" property="transactionAmount"/>
        <result column="TOTAL_AMOUNT" property="bulkTransactionAmount"/>
        <result column="RECIPIENTS_REFERENCE" property="customerReference"/>
        <result column="RECIPIENTS_REFERENCE" property="bulkCustomerReference"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="VALUE_DATE" property="valueDate"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="TOTAL_CHILD" property="totalChild"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
    </resultMap>

    <select id="getBulkParentApprovalStatusTxnList" resultMap="bulkParentApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT DISTINCT
				txn.transaction_id,
				txn.CHANGE_TOKEN,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT,
				btxn.TRANSFER_DATE as VALUE_DATE,
				<#if filterParams.isChildY ?has_content >
       		    btxni.initiation_time,
       		    btxn.transfer_date,
       		    btxni.original_uetr,
   		         </#if>
                <#if filterParams.isChildN ?has_content >
      			  txn.initiation_time,
 		        </#if>
				btxn.RECIPIENTS_REFERENCE,
				txn.customer_transaction_status,
                txn.company_id,
                txn.TOTAL_CHILD,
                txn.HIGHEST_AMOUNT,
                txn.account_number,
                pfu.UPLOAD_FILE_NAME,
                txn.bank_entity_id
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE
			   <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                  txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		          txn.company_group_id in ${filterParams.companyGroupId} AND
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		          txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
                </#if>

                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		             txn.TRANSACTION_ID in (${filterParams.transactionId}) AND
                </#if>
                <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                    <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                         txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
                    </#if>
                    <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                         txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
                    </#if>
                </#if>
                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		              txn.TRANSACTION_ID  not in (${filterParams.excludeTransactionId}) AND
                 </#if>
                 <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
                        (
                            <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
                            (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
                            IN (
                                <#list chunk as companyAccountGroup>
                                <@p value=companyAccountGroup/>
                                <#if companyAccountGroup_has_next>,</#if>
                            </#list>
                            )
                            <#if chunk_has_next> OR </#if>
                            </#list>
		               ) AND
                 </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		              txn.COMPANY_NAME in ${filterParams.companyName} AND
                  </#if>

                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		               txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		                txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
                    </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
                         txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> and
					 </#if>
					 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					     txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> and
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					     txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> and
					  </#if>
					  <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                  txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                     </#if>
	                 <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                           txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                     </#if>
	                 <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		                UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
                     </#if>
                     <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                             UPPER(btxn.RECIPIENTS_REFERENCE) LIKE UPPER ('%${filterParams.customerRefIds}%') AND
                      </#if>
                      <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                         btxn.RECIPIENTS_REFERENCE IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                           trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                 txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
                     </#if>
                      <#if filterParams.minNumberOfChildTransactions?has_content && filterParams.minNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD >= ${filterParams.minNumberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.excludeBatch == true>
                         txn.batch_id is null AND
                     </#if>
                     <#if filterParams.batchId?has_content>
                         txn.batch_id = <@p name='filterParams.batchId'/> AND
                     </#if>
                     txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )
                 ORDER BY ${filterParams.sortFieldWithDirection}, txn.transaction_id
			     OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>

    <resultMap id="bulkParentApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="TOTAL_AMOUNT" property="bulkTransactionAmount"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="RECIPIENTS_REFERENCE" property="bulkCustomerReference"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="company_id" property="companyId"/>
        <result column="TOTAL_CHILD" property="totalChild"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
        <result column="account_number" property="accountNumber"/>
        <result column="upload_file_name" property="uploadFileName"/>
        <result column="bank_entity_id" property="bankEntityId"/>
        <result column="transfer_date" property="boReleaseDateTime"/>
        <result column="original_uetr" property="originalUetr"/>
    </resultMap>

    <select id="getChargesDetail" resultMap="getBulkPWSChargesDetailsMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[select
				   TRANSACTION_ID
				   ,FEES_CURRENCY
				   ,FEES_AMOUNT
				from PWS_TRANSACTION_CHARGES
				where TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="getFxContracts" resultMap="getFxTransactions"
            parameterType="list" statementType="PREPARED">
        <![CDATA[SELECT
                     *
                     FROM (
                             SELECT
                              txn_rec.*,
                                 ROW_NUMBER()
                                 OVER(PARTITION BY transaction_id
                             ORDER BY
                                transaction_id
                             ) rw_num
                     FROM
                        (
                        SELECT
                               txn.transaction_id,
                               ptfc.fx_type,
                               ptfb.booking_ref_id,
                               ptfb.earmark_id,
                               CASE WHEN txni.fx_flag is null THEN btxni.fx_flag ELSE txni.fx_flag  END  AS fx_flag
                                 FROM
                                    pws_transactions  txn
                                    LEFT JOIN PWS_TRANSACTION_INSTRUCTIONS  txni on txn.transaction_id = txni.transaction_id
                                    LEFT JOIN pws_transaction_fx_contracts      ptfc ON ptfc.transaction_id = txn.transaction_id
                                    LEFT JOIN pws_transaction_fx_bookings       ptfb ON ptfb.booking_id = ptfc.booking_id
                                    LEFT JOIN pws_bulk_transaction_instructions btxni ON txn.transaction_id = btxni.transaction_id
                                    AND btxni.child_bank_reference_id = ptfc.child_bank_reference_id
                                     WHERE
                                        txn.transaction_id IN ( <#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
                         ) txn_rec
                    )
      WHERE
    rw_num = 1
			]]>
    </select>
    <resultMap id="getFxTransactions"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="fx_type" property="fxType"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
    </resultMap>

    <resultMap id="getBulkPWSChargesDetailsMap"
               type="com.uob.gwb.txn.domain.PwsTransactionCharges">
        <id column="TRANSACTION_ID" property="transactionId" />
        <result column="FEES_CURRENCY" property="feesCurrency" />
        <result column="FEES_AMOUNT" property="feesAmount" />
    </resultMap>
    <select id="getFXContractDetailsBasedOnTransaction" resultMap="getBulkPWSContractDetailsMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[SELECT
                    pt.transaction_id,
                    pt.feature_id,
                    pt.total_amount,
                    pt.bank_reference_id,
                    con.transaction_amount as childTransactionAmount,
                    con.child_bank_reference_id,
                    con.currency_pair,
                    book.equivalent_total_amount bookingequivalentamount,
                    book.rate                    bookingrate
                FROM
                    pws_transactions pt
                    INNER JOIN pws_transaction_fx_contracts con ON pt.transaction_id = con.transaction_id
                    LEFT JOIN pws_transaction_fx_bookings  book ON con.booking_id = book.booking_id
				<#if transactionIds?has_content>
		        where
                pt.transaction_id IN
                (<#list transactionIds as transactionId> <@p value=transactionId/><#if transactionId_has_next>,</#if></#list>)
                </#if>
			]]>
    </select>

    <resultMap id="getBulkPWSContractDetailsMap"
               type="com.uob.gwb.txn.domain.PwsFxContract">
        <result column="BANK_REFERENCE_ID" property="bankReferenceId" />
        <result column="TRANSACTION_ID" property="transactionId" />
        <result column="TRANSACTION_AMOUNT" property="transactionAmount" />
        <result column="CHILD_BANK_REFERENCE_ID" property="childBankRefId" />
        <result column="FEATURE_ID" property="featureId" />
        <result column="CURRENCY_PAIR" property="currencyPair" />
        <result column="bookingEquivalentAmount" property="bookingEquivalentAmount" />
        <result column="BookingRate" property="BookingRate" />
        <result column="childTransactionAmount" property="childTransactionAmount" />
        <result column="total_amount" property="totalAmount" />
    </resultMap>

    <select id="updateEquivalentAmountInBulkInstructions"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE PWS_BULK_TRANSACTION_INSTRUCTIONS ins
					SET ins.EQUIVALENT_AMOUNT = <@p name='fundsResReq.totalEquivalentAmount'/>
				    where ins.TRANSACTION_ID = <@p name='fundsResReq.transactionId'/>
				    AND ins.CHILD_BANK_REFERENCE_ID = <@p name='fundsResReq.childReferenceId'/>
			]]>
    </select>


    <select id="getBulkMyTaskList" resultMap="MyTaskMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
                txn.change_token,
				txn.account_number,
				txn.account_currency,
				txn.initiation_time,
				txn.bank_reference_id,
				txn.customer_transaction_status,
				txn.authorization_status,
				txn.company_id,
				txn.resource_id,
				txn.feature_id,
				txn.initiated_by,
				txn.total_child,
				txn.highest_amount,
				txn.total_amount as transaction_amount,
				txn.transaction_currency,
				btxn.recipients_reference,
				btxn.transfer_date as value_date,
				pfu.UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
				LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE 1=1
			AND txn.BATCH_ID is null
				<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		               AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
                 </#if>
                  <#if filterParams.multiCustomerReferenceIds?has_content && filterParams.multiCustomerReferenceIds??>
                          AND btxn.recipients_reference in (<#list filterParams.multiCustomerReferenceIds as cusRef> <@p value=cusRef/> <#if cusRef_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
		               AND UPPER(btxn.recipients_reference) LIKE UPPER('%${filterParams.customerRefIds}%')
                 </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
                          AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
               <#if filterParams.statusList?has_content && filterParams.statusList??>
                          AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
	           <#if filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount?has_content >
					and txn.total_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
	            <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
                          AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
                          AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
                          AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )

                </#if>

                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
                          AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )

                </#if>
               <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                          AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
               </#if>

                <#if filterParams.bulkFromDate?has_content && filterParams.bulkFromDate?? && filterParams.bulkToDate?has_content && filterParams.bulkToDate??>
                          AND trunc(btxn.transfer_date) between '${filterParams.bulkFromDate}' and '${filterParams.bulkToDate}'
                </#if>
                <#if filterParams.transactionId?has_content>
                          AND txn.transaction_id in (${filterParams.transactionId})
                </#if>
                <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		         		  AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                </#if>
                <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		            	  AND txn.TRANSACTION_ID  not in (${filterParams.excludeTransactionId})
                </#if>
			ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>

    <select id="getBulkBothMyTaskTxnList" resultMap="bothParentMyTaskTxnMap"
            statementType="PREPARED">
        <![CDATA[
        SELECT
                bank_entity_id,
                transaction_id,
				account_number,
				account_currency,
				initiated_by,
				initiation_time,
				customer_transaction_status,
				authorization_status,
				bank_reference_id,
				resource_id,
				feature_id,
				company_id,
				company_group_id,
				CHANGE_TOKEN,
				transaction_currency,
				transaction_amount,
				RECIPIENTS_REFERENCE,
                VALUE_DATE,
				is_recurring,
				destination_country,
				destination_bank_name,
				fx_flag,
                purpose_codes,
				party_name,
				party_account_number,
				party_account_currency,
				bank_code,
				swift_code,
				beneficiary_reference_id,
				party_type,
				residency_status,
				party_id,
				proxy_id_type,
				id_issuing_country,
				address_line_1,
				address_line_2,
				address_line_3,
				phone_country,
				phone_country_code,
				phone_no,
				fx_type,
                booking_ref_id,
                earmark_id,
				TOTAL_CHILD,
                HIGHEST_AMOUNT,
                UPLOAD_FILE_NAME
             FROM
                (
                <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
                    SELECT
                        txn.bank_entity_id,
				        txn.transaction_id,
				        txn.account_number,
				        txn.account_currency,
				        txn.initiated_by,
				        txn.initiation_time,
				        txn.customer_transaction_status,
				        txn.authorization_status,
				        txn.bank_reference_id,
				        txn.resource_id,
				        txn.feature_id,
				        txn.company_id,
				        txn.company_group_id,
				        txn.CHANGE_TOKEN,
				        txni.transaction_currency,
				        txni.transaction_amount,
				        txni.customer_reference AS RECIPIENTS_REFERENCE,
				        txni.value_date,
				        txni.is_recurring,
				        txni.destination_country,
				        txni.destination_bank_name,
				        txni.fx_flag,
                        txni.purpose_codes,
				        pty.party_name,
				        pty.party_account_number,
				        pty.party_account_currency,
				        pty.bank_code,
				        pty.swift_code,
				        pty.beneficiary_reference_id,
				        pty.party_type,
				        pty.residency_status,
				        pty.party_id,
				        pty.proxy_id_type,
				        pty.id_issuing_country,
				        ptyc.address_line_1,
				        ptyc.address_line_2,
				        ptyc.address_line_3,
				        ptyc.phone_country,
				        ptyc.phone_country_code,
				        ptyc.phone_no,
				        ptfc.fx_type,
                        ptfb.booking_ref_id,
                        ptfb.earmark_id,
				        txn.TOTAL_CHILD,
                        txn.HIGHEST_AMOUNT,
                        null AS UPLOAD_FILE_NAME
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			        WHERE 1=1
			        AND txn.BATCH_ID is null
                <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                 </#if>
				<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		               AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
                 </#if>
                <#if filterParams.multiCustomerReferenceIds?has_content && filterParams.multiCustomerReferenceIds??>
                          AND txni.customer_reference in (<#list filterParams.multiCustomerReferenceIds as cusRef> <@p value=cusRef/> <#if cusRef_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
		               AND UPPER(txni.customer_reference) LIKE UPPER('%${filterParams.customerRefIds}%')
                 </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		             AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
		             AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
                <#if filterParams.payerName?has_content && filterParams.payerName??>
		             AND UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%')
                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		             AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		             AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		             AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                 </#if>

                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(txni.value_date) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		            AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>
              <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                    AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
               </#if>
            </#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction?? && !(filterParams.payerNameList?has_content)>
			UNION ALL
			</#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && !(filterParams.payerNameList?has_content)>
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
				txn.account_number,
				txn.account_currency,
				txn.initiated_by,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.authorization_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT as transaction_amount,
				btxn.RECIPIENTS_REFERENCE,
				btxn.TRANSFER_DATE as VALUE_DATE,
				null as is_recurring,
				null as destination_country,
				null as destination_bank_name,
				null as fx_flag,
                null as purpose_codes,
				null as party_name,
				null as party_account_number,
				null as party_account_currency,
				null as bank_code,
				null as swift_code,
				null as beneficiary_reference_id,
				null as party_type,
				null as residency_status,
				null as party_id,
				null as proxy_id_type,
				null as id_issuing_country,
				null as address_line_1,
				null as address_line_2,
				null as address_line_3,
				null as phone_country,
				null as phone_country_code,
				null as phone_no,
				null as fx_type,
				null as booking_ref_id,
				null as earmark_id,
				txn.TOTAL_CHILD,
				txn.HIGHEST_AMOUNT,
				pfu.UPLOAD_FILE_NAME AS UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE 1=1
			AND txn.BATCH_ID is null
			 <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.bulkAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                 </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>
                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                   </#if>
                    <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                    AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                    </#if>
 	                <#if filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount?has_content >
					  and txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	                </#if>
	               <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		                 AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
                     </#if>
                     <#if filterParams.multiCustomerReferenceIds?has_content && filterParams.multiCustomerReferenceIds??>
                          AND btxn.recipients_reference in (<#list filterParams.multiCustomerReferenceIds as cusRef> <@p value=cusRef/> <#if cusRef_has_next>,</#if> </#list> )
                      </#if>
                     <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
		               AND UPPER(btxn.recipients_reference) LIKE UPPER('%${filterParams.customerRefIds}%')
                    </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
                     <#if filterParams.minNumberOfChildTransactions?has_content && filterParams.minNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD >= ${filterParams.minNumberOfChildTransactions}
                     </#if>
			</#if>
			) bothTrans
			 ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>
    <resultMap id="bothParentMyTaskTxnMap"
               type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="transactionId"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiated_by" property="userId"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resourceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="company_id" property="companyId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="TRANSACTION_CURRENCY" property="txnCurrency"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="transaction_amount" property="totalAmount"/>
        <result column="transaction_amount" property="transactionAmount"/>
        <result column="customer_reference" property="customerReference"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="VALUE_DATE" property="valueDate"/>
        <result column="RECIPIENTS_REFERENCE" property="recipientsReference"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="purpose_codes" property="purposeCodes"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="TOTAL_CHILD" property="numberOfChildTransactions"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
        <result column="authorization_status" property="authorizationStatus"/>
        <result column="rownum" property="rowNum"/>
        <result column="bank_entity_id" property="bankEntityId"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
    </resultMap>

    <select id="getMyTaskFxContracts" resultMap="FxMyTaskMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[
            select
                distinct
                txn.TRANSACTION_ID,
				ptfc.fx_type,
                ptfb.booking_ref_id,
                ptfb.earmark_id,
                CASE
                WHEN txni.fx_flag is null THEN btxni.fx_flag
                ELSE txni.fx_flag
                END
                AS fx_flag
            FROM PWS_TRANSACTIONS txn
            LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on btxni.transaction_id = txn.transaction_id
            LEFT JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txni.transaction_id = txn.transaction_id
            LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txn.transaction_id
            LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
            where txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
			]]>
    </select>

    <resultMap id="FxMyTaskMap"
               type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="transactionId"/>
        <result column="fx_type" property="fxType"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
    </resultMap>

    <select id="getTransactionsByIds" resultMap="transactionDetailsMap"
            parameterType="list" statementType="PREPARED">
        <include refid="fragmentGetPwsTransactionsById" />
    </select>
    <select id="getTransactionsByIdsAndTokens" resultMap="transactionDetailsMap"
            parameterType="list" statementType="PREPARED">
        <include refid="fragmentGetPwsTransactionsById" />
        <![CDATA[
            AND txn.CHANGE_TOKEN IN (<#list changeTokens as changeToken> <@p value=changeToken/> <#if changeToken_has_next>,</#if> </#list>)
        ]]>
    </select>
    <sql id ="fragmentGetPwsTransactionsById">
        <![CDATA[
            SELECT
                txn.TRANSACTION_ID,
                txn.RESOURCE_ID,
                txn.FEATURE_ID
            FROM PWS_TRANSACTIONS txn
            WHERE txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
         ]]>
    </sql>
    <resultMap id="transactionDetailsMap"
               type="com.uob.gwb.txn.domain.PwsTransactions">
        <id column="TRANSACTION_ID" property="transactionId"/>
        <result column="RESOURCE_ID" property="resourceId"/>
        <result column="FEATURE_ID" property="featureId"/>
    </resultMap>


    <select id="updateTransactionDetail"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE PWS_TRANSACTIONS trans
					SET trans.RATE = <@p name='fundsResReq.rate'/>
					where trans.transaction_id IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
			]]>
    </select>

    <update id="updateTransactionChangeToken"
            parameterType="list" statementType="PREPARED">
        <![CDATA[
            UPDATE PWS_TRANSACTIONS trans
            SET
                trans.change_token = <@p name='changeToken'/>
            WHERE trans.transaction_id IN (<#list transactionIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
        ]]>
    </update>

    <update id="updateTransactionCustomerStatus"
            parameterType="list" statementType="PREPARED">
        <![CDATA[
            UPDATE PWS_TRANSACTIONS trans
            SET
                trans.customer_transaction_status = 'PENDING_AUTHORIZATION',
                trans.authorization_status = 'PENDING_AUTHORIZATION'
            WHERE trans.transaction_id IN (<#list transactionIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
                AND trans.customer_transaction_status IN (<#list customerTransactionStatusList as customerTransactionStatus> <@p value=customerTransactionStatus/> <#if customerTransactionStatus_has_next>,</#if> </#list> )
        ]]>
    </update>

    <select id="getMyTaskTxnList" resultMap="bothParentMyTaskTxnMap"
            statementType="PREPARED">
        <![CDATA[
                SELECT
                bank_entity_id,
                transaction_id,
				account_number,
				account_currency,
				initiated_by,
				initiation_time,
				customer_transaction_status,
				authorization_status,
				bank_reference_id,
				resource_id,
				feature_id,
				company_id,
				company_group_id,
				CHANGE_TOKEN,
				transaction_currency,
				transaction_amount,
				customer_reference,
				RECIPIENTS_REFERENCE,
				VALUE_DATE,
				is_recurring,
				destination_country,
				destination_bank_name,
				fx_flag,
				party_name,
				party_account_number,
				party_account_currency,
				bank_code,
				swift_code,
				beneficiary_reference_id,
				party_type,
				residency_status,
				party_id,
				proxy_id_type,
				id_issuing_country,
				address_line_1,
				address_line_2,
				address_line_3,
				phone_country,
				phone_country_code,
				phone_no,
				fx_type,
                booking_ref_id,
                earmark_id,
				TOTAL_CHILD,
                HIGHEST_AMOUNT,
                UPLOAD_FILE_NAME
             FROM
                (
                    SELECT
                        txn.bank_entity_id,
				        txn.transaction_id,
				        txn.account_number,
				        txn.account_currency,
				        txn.initiated_by,
				        txn.initiation_time,
				        txn.customer_transaction_status,
				        txn.authorization_status,
				        txn.bank_reference_id,
				        txn.resource_id,
				        txn.feature_id,
				        txn.company_id,
				        txn.company_group_id,
				        txn.CHANGE_TOKEN,
				        txni.transaction_currency,
				        txni.transaction_amount,
				        txni.customer_reference,
				        null as RECIPIENTS_REFERENCE,
				        txni.value_date,
				        txni.is_recurring,
				        txni.destination_country,
				        txni.destination_bank_name,
				        txni.fx_flag,
				        pty.party_name,
				        pty.party_account_number,
				        pty.party_account_currency,
				        pty.bank_code,
				        pty.swift_code,
				        pty.beneficiary_reference_id,
				        pty.party_type,
				        pty.residency_status,
				        pty.party_id,
				        pty.proxy_id_type,
				        pty.id_issuing_country,
				        ptyc.address_line_1,
				        ptyc.address_line_2,
				        ptyc.address_line_3,
				        ptyc.phone_country,
				        ptyc.phone_country_code,
				        ptyc.phone_no,
				        ptfc.fx_type,
                        ptfb.booking_ref_id,
                        ptfb.earmark_id,
				        txn.TOTAL_CHILD,
                        txn.HIGHEST_AMOUNT,
                        null AS UPLOAD_FILE_NAME
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			        WHERE 1=1
			    <#if filterParams.companyIdToStatusList?has_content && filterParams.companyIdToStatusList??>
                    AND (
        <#list filterParams.companyIdToStatusList?chunk(999) as chunk>
         (txn.RESOURCE_ID || '_' || txn.FEATURE_ID || '_' || txn.COMPANY_ID || '_' || txn.customer_transaction_status)
		                IN (
                <#list chunk as companyIdToStatusList>
                    <@p value=companyIdToStatusList/>
                    <#if companyIdToStatusList_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )
</#if>
    <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
     AND (
        <#list filterParams.singleAccountBasedOnResourceFeatureList?chunk(999) as chunk>
            (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
            IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )
</#if>
	<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>

                <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			 AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
       			</#if>
                <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
		             AND UPPER(txni.customer_reference) in (<#list filterParams.customerReferencesList as cusRef> UPPER(<@p value=cusRef/>)  <#if cusRef_has_next>,</#if> </#list>)

                </#if>
		<#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                      AND UPPER(txni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')

                </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		             AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
		             AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.transactionFromAmount ??>
					and txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
	            </#if>
	            <#if filterParams.transactionToAmount ??>
					and txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
	            </#if>
		    <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.payerName?has_content && filterParams.payerName??>
		             AND UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%')

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		             AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		             AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		             AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                 </#if>

                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(txni.value_date) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		            AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>
		<#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                    AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
               </#if>
                <#if filterParams.bankEntityId ?? >
                          AND txn.BANK_ENTITY_ID = <@p name='filterParams.bankEntityId'/>
                 </#if>
                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
            <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			UNION ALL
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
				txn.account_number,
				txn.account_currency,
				txn.initiated_by,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.authorization_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT as transaction_amount,
				null as customer_reference,
				btxn.RECIPIENTS_REFERENCE,
				btxn.TRANSFER_DATE as VALUE_DATE,
				null as is_recurring,
				null as destination_country,
				null as destination_bank_name,
				null as fx_flag,
				null as party_name,
				null as party_account_number,
				null as party_account_currency,
				null as bank_code,
				null as swift_code,
				null as beneficiary_reference_id,
				null as party_type,
				null as residency_status,
				null as party_id,
				null as proxy_id_type,
				null as id_issuing_country,
				null as address_line_1,
				null as address_line_2,
				null as address_line_3,
				null as phone_country,
				null as phone_country_code,
				null as phone_no,
				null as fx_type,
				null as booking_ref_id,
				null as earmark_id,
				txn.TOTAL_CHILD,
				txn.HIGHEST_AMOUNT,
				pfu.UPLOAD_FILE_NAME AS UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE 1=1
			<#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			 AND (
        <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
          (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		              IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )
</#if>
 <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>
                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                   </#if>
		    <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                    AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                    </#if>
 	                <#if filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount?has_content >
					  and txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	                </#if>
	                 <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                 AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                        AND UPPER(btxn.RECIPIENTS_REFERENCE) IN (<#list filterParams.customerReferencesList as cusRef> UPPER(<@p value=cusRef/>)  <#if cusRef_has_next>,</#if> </#list>)
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
                     <#if filterParams.bankEntityId ?? >
                          AND txn.BANK_ENTITY_ID = <@p name='filterParams.bankEntityId'/>
                 </#if>
                 </#if>
			) bothTrans
			 ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>

    <select id="getV2BulkBothApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count
			FROM (
                <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
                    SELECT DISTINCT
				        txn.transaction_id,
				        txn.initiation_time
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
			        WHERE
            ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount1"></include>
        <![CDATA[
               <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>

			UNION ALL
			</#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
			SELECT DISTINCT
				txn.transaction_id,
				txn.initiation_time
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
            ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount2"></include>
        <![CDATA[
           </#if>
			) bothTrans
		]]>
    </select>

    <select id="getBookingDetailsFromContracts" resultMap="transBookingDetailFromContractMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				TRANSACTION_ID,
				 BOOKING_ID
			FROM PWS_TRANSACTION_FX_CONTRACTS
 			WHERE TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
 					]]>
    </select>
    <resultMap id="transBookingDetailFromContractMap"
               type="com.uob.gwb.txn.domain.BookFXTransaction">
        <id column="TRANSACTION_ID" property="transactionId" />
        <result column="BOOKING_ID" property="bookingId" />
    </resultMap>


    <sql id ="fragmentGetBulkBothApprovalStatusTxnCount1">
        <![CDATA[
         <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
             txn.company_group_id in ${filterParams.companyGroupId} AND
        </#if>
        <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
             txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
        </#if>
       <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
			 (
        <#list filterParams.accountNumberList?chunk(999) as chunk>
          ( txn.account_number)
		              IN (
                <#list chunk as accountNumber>
                    <@p value=accountNumber/>
                    <#if accountNumber_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )AND
</#if>
        <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
             txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		     UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
        </#if>
        <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
             txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
		     UPPER(txni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%') AND
        </#if>
        <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
      (
        <#list filterParams.singleAccountBasedOnResourceFeatureList?chunk(999) as chunk>
            (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
            IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>

            <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
             txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.statusList?has_content && filterParams.statusList??>
             txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
             txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> AND
        </#if>
        <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
             txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/> AND
        </#if>
        <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
             txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/> AND
        </#if>
        <#if filterParams.payerName?has_content && filterParams.payerName??>
             UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%') AND
        </#if>
        <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
             pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
             pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
             pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
             trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
        </#if>
        <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
             trunc(txni.value_date) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
        </#if>
        <#if filterParams.singleFeatureId?has_content && filterParams.singleFeatureId??>
             txn.FEATURE_ID in '${filterParams.singleFeatureId}' AND
        </#if>
        <#if filterParams.singleResourceId?has_content && filterParams.singleResourceId??>
             txn.RESOURCE_ID in '${filterParams.singleResourceId}' AND
        </#if>

        <#if filterParams.excludeBatch == true>
             txn.batch_id is null AND
        </#if>
        <#if filterParams.batchId?has_content>
              txn.batch_id = <@p name='filterParams.batchId'/> AND
        </#if>

        </#if>
            txn.customer_transaction_status NOT IN ('PENDING_SUBMIT', 'NEW','DELETED')
          ]]>
    </sql>

    <sql id ="fragmentGetBulkBothApprovalStatusTxnCount2">
        <![CDATA[
        <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
             txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
             txn.company_group_id in ${filterParams.companyGroupId} AND
        </#if>
        <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
             txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
        </#if>
       <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			  (
        <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
          (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		              IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>
        <#if filterParams.transactionId?has_content && filterParams.transactionId??>
             txn.TRANSACTION_ID in ${filterParams.transactionId} AND
        </#if>

        <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
             txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
        </#if>
        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
             txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
        </#if>
        </#if>
        <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
             txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId} AND
        </#if>
        <#if filterParams.companyName?has_content && filterParams.companyName??>
             txn.COMPANY_NAME in ${filterParams.companyName} AND
        </#if>
        <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
             txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
             trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
        </#if>
        <#if filterParams.statusList?has_content && filterParams.statusList??>
             txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
             txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
             txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> AND
        </#if>
        <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
             txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> AND
        </#if>
        <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
             txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> AND
        </#if>
        <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
             txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
             UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
        </#if>
        <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
            UPPER(btxn.RECIPIENTS_REFERENCE) IN (<#list filterParams.customerReferencesList as cusRef> UPPER(<@p value=cusRef/>)  <#if cusRef_has_next>,</#if> </#list>) AND
        </#if>
        <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
             UPPER(btxn.RECIPIENTS_REFERENCE) LIKE UPPER ('%${filterParams.customerRefIds}%') AND
        </#if>
        <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
             trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
        </#if>
        <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
             txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
        </#if>
        <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
             txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
        </#if>
        <#if filterParams.minNumberOfChildTransactions?has_content && filterParams.minNumberOfChildTransactions??>
             txn.TOTAL_CHILD >= ${filterParams.minNumberOfChildTransactions} AND
        </#if>
         txn.customer_transaction_status NOT IN ('PENDING_SUBMIT','NEW','DELETED')
        ]]>
    </sql>

    <sql id="fragmentGetApprovalStatusTxnCount">
        <![CDATA[
        <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
            AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
            AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
        </#if>
        <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
            AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
        </#if>
        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
            AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
        </#if>
        </#if>
        <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
            AND UPPER(txni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')
        </#if>
        <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
            AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
            in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
            AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
            AND txn.company_group_id in ${filterParams.companyGroupId}
        </#if>
        <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
            AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
            AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.statusList?has_content && filterParams.statusList??>
            AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
            and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
        </#if>
        <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
            and txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
        </#if>
        <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
            and txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
        </#if>
        <#if filterParams.payerName?has_content && filterParams.payerName??>
            AND UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%')
        </#if>
        <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
            AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
            AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
            AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
            AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
        </#if>
        <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
            AND trunc(txni.value_date) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
        </#if>
        <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
            AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
        </#if>
        <#if filterParams.excludeBatch == true>
              AND txn.batch_id is null
        </#if>
        <#if filterParams.batchId?has_content>
               AND txn.batch_id = <@p name='filterParams.batchId'/>
        </#if>

        ]]>
    </sql>


    <select id="getPwsResourceConfigurationsConfigCode"
            resultMap="getPwsResourceConfigurationsMap" statementType="PREPARED">
        <![CDATA[SELECT
				prc.config_code,
		        prc.config_value
		      FROM pws_resource_configurations prc
		      where prc.resource_id = <@p name='resourceId'/>
		      AND prc.config_code =<@p name='configCode'/>
		      and (trunc(sysdate) >= EFFECTIVE_DATE and trunc(sysdate)  <= END_DATE)
			]]>
    </select>

    <resultMap id="getPwsResourceConfigurationsMap"
               type="com.uob.gwb.txn.model.ConfigDetail">
        <result column="config_code" 		property="code" />
        <result column="config_value" 		property="value" />
    </resultMap>

    <select id="getPartyEntities"
            resultMap="GetPartyEntitiesMap" statementType="PREPARED">
        <![CDATA[
		SELECT
			    pp.bank_reference_id,
			    pp.transaction_id,
			    pp.beneficiary_reference_id,
			    pp.bank_code,
			    pp.bank_id,
			    pp.swift_code,
			    pp.party_account_number,
			    pp.party_name,
			    pp.party_type,
			    pp.residency_status,
			    pp.party_id,
			    pp.proxy_id_type,
			    pp.id_issuing_country,
			    pp.CHILD_BANK_REFERENCE_ID,
			    ppc.address_line_1,
			    ppc.address_line_2,
			    ppc.address_line_3,
			    ppc.town_name,
			    ppc.street_name,
			    ppc.postal_code,
			    ppc.country_code,
			    ppc.phone_country,
			    ppc.phone_country_code,
			    ppc.phone_no,
			    ppc.province,
			    ppc.building_Number,
                ppc.address_category
		FROM    pws_parties pp
                LEFT JOIN pws_party_contacts ppc
                ON pp.party_id  = ppc.party_id
                WHERE
                ppc.PARTY_CONTACT_TYPE ='PAYEE' AND
                pp.bank_reference_id= <@p name='bankReferenceId' />

]]>
    </select>

    <resultMap id="GetPartyEntitiesMap" type="com.uob.gwb.txn.domain.PwsPartyEntities">
        <result column="BANK_REFERENCE_ID"           property="bankReferenceId" />
        <result column="TRANSACTION_ID"              property="transactionId" />
        <result column="BENEFICIARY_REFERENCE_ID"    property="beneficiaryReferenceId" />
        <result column="BANK_CODE"                   property="bankCode" />
        <result column="BANK_ID"                   property="bankId" />
        <result column="SWIFT_CODE"                  property="swiftCode" />
        <result column="PARTY_ACCOUNT_NUMBER"        property="partyAccountNumber" />
        <result column="PARTY_NAME"                  property="partyName" />
        <result column="PARTY_TYPE"                  property="partyType" />
        <result column="RESIDENCY_STATUS"            property="residencyStatus" />
        <result column="PARTY_ID"                    property="partyId" />
        <result column="PROXY_ID_TYPE"               property="proxyIdType" />
        <result column="ID_ISSUING_COUNTRY"          property="idIssuingCountry" />
        <result column="CHILD_BANK_REFERENCE_ID"     property="childBankReferenceId" />
        <!-- PartyContacts-->
        <result column="ADDRESS_LINE_1"       property="addressLine1" />
        <result column="ADDRESS_LINE_2"       property="addressLine2" />
        <result column="ADDRESS_LINE_3"       property="addressLine3" />
        <result column="PHONE_COUNTRY"        property="phoneCountry" />
        <result column="PHONE_COUNTRY_CODE"   property="phoneCountryCode" />
        <result column="PHONE_NO"             property="phoneNo" />
        <result column="ADDRESS_CATEGORY"     property="addressCategory" />
        <result column="town_name"            property="address.townName" />
        <result column="street_name"          property="address.streetName" />
        <result column="postal_code"          property="address.postalCode" />
        <result column="country_code"         property="address.country" />
        <result column="province"             property="address.province" />
        <result column="building_Number"      property="address.buildingNumber" />

        </resultMap>

</mapper>
```

# Mapper V2

```xml

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.uob.gwb.txn.pws.dao.TransactionWorkflowV2DAO">

    <select id="getApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count FROM
		 (select DISTINCT  txn.transaction_id
			FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			WHERE txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )
        ]]>
        <include refid="fragmentGetApprovalStatusTxnCount"></include>
        <![CDATA[
          )

		]]>
    </select>
    <select id="getApprovalStatusTxnList" resultMap="ApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
        Select DISTINCT total_count, transaction_id,account_number,account_currency,initiation_time,customer_transaction_status,
 bank_reference_id,resource_id,feature_id,company_id,company_group_id,CHANGE_TOKEN,transaction_currency,transaction_amount,
 customer_reference,value_date,original_uetr,is_recurring,destination_country,destination_bank_name,fx_flag,
 party_name, party_account_number,party_account_currency,bank_code,swift_code,beneficiary_reference_id,
 party_type,residency_status,party_id,proxy_id_type,id_issuing_country,address_line_1,address_line_2,
 address_line_3,phone_country,phone_country_code,phone_no,fx_type,booking_ref_id,earmark_id,correlation_id
 from
			((SELECT count(*) over (order by 1) total_count,
				txn.transaction_id,
				txn.account_number,
				pty.party_account_currency as account_currency,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txni.transaction_currency,
				txni.transaction_amount,
				txni.customer_reference,
				txni.value_date,
				txni.original_uetr,
				txni.is_recurring,
				txni.destination_country,
				txni.destination_bank_name,
				txni.fx_flag,
				pty.party_name,
				pty.party_account_number,
				pty.party_account_currency,
				pty.bank_code,
				pty.swift_code,
				pty.beneficiary_reference_id,
				pty.party_type,
				pty.residency_status,
				pty.party_id,
				pty.proxy_id_type,
				pty.id_issuing_country,
				ptyc.address_line_1,
				ptyc.address_line_2,
				ptyc.address_line_3,
				ptyc.phone_country,
				ptyc.phone_country_code,
				ptyc.phone_no,
				ptfc.fx_type,
                ptfb.booking_ref_id,
                ptfb.earmark_id,
                null as correlation_Id
			FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
                           AND ptyc.PARTY_CONTACT_TYPE='PAYEE'
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			WHERE txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )
                AND nvl(txn.application_type,'X') != 'terminateRecurring'
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID = ${filterParams.transactionId}
                </#if>
				<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>

                <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			 AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
       			</#if>
                <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                     <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                        AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                     </#if>
                      <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                        AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                      </#if>
                </#if>
                <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                 </#if>
                <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
                 </#if>
                <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
                    AND txn.company_group_id in (${filterParams.companyGroupId})
                </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		              AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                </#if>
                <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                      AND UPPER(txni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')

                </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
                          AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
                          AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
	             <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					    and txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
			     </#if>
				 <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					    and txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
				 </#if>
                <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.payerName?has_content && filterParams.payerName??>
		             AND UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%')

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
                          AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
                          AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
                          AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )

                </#if>

                <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                    AND trunc(txn.initiation_time) between TO_DATE('${filterParams.applicationFromDate}', 'DD-Mon-YYYY') and TO_DATE('${filterParams.applicationToDate}', 'DD-Mon-YYYY')
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>

                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
                          AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>

                <#if filterParams.excludeBatch == true>
                         AND txn.batch_id is null
                </#if>
                <#if filterParams.batchId?has_content>
                         AND txn.batch_id = <@p name='filterParams.batchId'/>
                </#if>)
                UNION ALL
                (SELECT
                null as total_count,
                txn.transaction_id,
		        txn.account_number,
		        null as account_currency,
		        txn.initiation_time,
                rtxn.status AS customer_transaction_status ,
		        txn.bank_reference_id,
		        txn.resource_id,
		        txn.feature_id,
		        txn.company_id,
		        txn.company_group_id,
                rtxn.change_token AS CHANGE_TOKEN,
		        txni.transaction_currency,
		        txni.transaction_amount,
		        txni.customer_reference,
                rtxn.processing_date AS value_date,
                rtxn.original_uetr as original_uetr,
	            txni.is_recurring, txni.destination_country,
                txni.destination_bank_name, txni.fx_flag, pty.party_name,pty.party_account_number, pty.party_account_currency,
                pty.bank_code, pty.swift_code, pty.beneficiary_reference_id,
                pty.party_type, pty.residency_status, pty.party_id,
                pty.proxy_id_type, pty.id_issuing_country,
				ptyc.address_line_1, ptyc.address_line_2, ptyc.address_line_3, ptyc.
                phone_country, ptyc.phone_country_code, ptyc.phone_no, ptfc.fx_type, ptfb.booking_ref_id, ptfb.
                earmark_id, rtxn.correlation_id  AS correlation_Id
				from
            pws_transaction_instructions pti
            JOIN
            pws_recurring_transactions rtxn
            ON
            pti.transaction_id = rtxn.transaction_id
            Join
            pws_transactions txn on
            txn.transaction_id = rtxn.transaction_id
			INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
			LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
			LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
                           AND ptyc.PARTY_CONTACT_TYPE='PAYEE'
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
            WHERE
            pti.is_recurring = 'Y'
            AND txn.customer_transaction_status IN ('PROCESSING', 'SUCCESSFUL', 'REJECTED', 'TERMINATED')))
			ORDER BY ${filterParams.sortFieldWithDirection}
			OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>

    <resultMap id="ApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="company_id" property="companyId"/>
        <result column="transaction_currency" property="transactionCurrency"/>
        <result column="transaction_amount" property="transactionAmount"/>
        <result column="customer_reference" property="customerReference"/>
        <result column="value_date" property="valueDate"/>
        <result column="value_date" property="boReleaseDateTime"/>
        <result column="original_uetr" property="originalUetr"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="total_count" property="totalCount"/>
        <result column="total_count" property="count"/>
        <result column="correlation_Id" property="correlationId"/>
    </resultMap>

    <select id="getMyTaskList" resultMap="MyTaskMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
                txn.change_token,
				txn.account_number,
				txn.account_currency,
				txn.initiation_time,
				txn.bank_reference_id,
				txn.customer_transaction_status,
                txn.authorization_status,
				txn.company_id,
				txn.resource_id,
				txn.feature_id,
				txn.initiated_by,
				txn.total_child,
				txn.highest_amount,
				txn.total_amount,
				txn.transaction_currency transactionCurrency,
				txnc.fees_currency,
				txnc.fees_amount,
				txni.customer_reference,
				txni.value_date,
				txni.transaction_currency,
				txni.transaction_amount,
				txni.is_recurring,
				txni.destination_country,
				txni.destination_bank_name,
				txni.fx_flag,
				pty.party_name,
				pty.party_account_number,
				pty.party_account_currency,
				pty.bank_code,
				pty.swift_code,
				pty.beneficiary_reference_id,
				pty.party_type,
				pty.residency_status,
				pty.party_id,
				pty.proxy_id_type,
				pty.id_issuing_country,
				ptyc.address_line_1,
				ptyc.address_line_2,
				ptyc.address_line_3,
				ptyc.phone_country,
				ptyc.phone_country_code,
				ptyc.phone_no,
				txnfxb.booking_ref_id,
				txnfxb.earmark_id,
				txnfxc.fx_type,
				null as UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS txnfxc on txnfxc.transaction_id = txn.transaction_id
						   AND txnfxc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS txnfxb on txnfxb.booking_id = txnfxc.booking_id
				LEFT JOIN PWS_TRANSACTION_CHARGES txnc on txn.transaction_id = txnc.transaction_id
			WHERE 1=1
			 AND nvl(txn.application_type,'X') != 'terminateRecurring'
			  <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID = ${filterParams.transactionId}
                </#if>
			    <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
                    AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		            in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                </#if>

				<#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
                          AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                          AND txni.customer_reference in (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)

                </#if>
               <#if filterParams.recipientsReferenceList?has_content && filterParams.recipientsReferenceList??>
                          AND btxn.recipients_reference in (<#list filterParams.recipientsReferenceList as resRef> <@p value=resRef/>  <#if resRef_has_next>,</#if> </#list>)

                </#if>
               <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                         AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
                          AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND txn.initiation_time between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
	            <#if filterParams.transactionFromTotalAmount?has_content && filterParams.transactionToTotalAmount?has_content >
					and txn.total_amount between <@p name='filterParams.transactionFromTotalAmount'/> and <@p name='filterParams.transactionToTotalAmount'/>
	            </#if>
	            <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
                          AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
                          AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
                          AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )

                </#if>
               <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
                          AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
                <#if filterParams.bulkFromDate?has_content && filterParams.bulkFromDate?? && filterParams.bulkToDate?has_content && filterParams.bulkToDate??>
                          AND trunc(btxn.transfer_date) between '${filterParams.bulkFromDate}' and '${filterParams.bulkToDate}'
                </#if>
                <#if filterParams.transactionId?has_content>
                          AND txn.transaction_id in ${filterParams.transactionId}
                </#if>
                <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		         		  AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                </#if>
                <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		            	  AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                </#if>
			ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>

    <resultMap id="MyTaskMap"
               type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="transactionId"/>
        <id column="change_token" property="changeToken"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="initiated_by" property="userId"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resourceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="company_id" property="companyId"/>
        <result column="transaction_currency" property="transactionCurrency"/>
        <result column="transaction_amount" property="transactionAmount"/>
        <result column="customer_reference" property="customerReference"/>
        <result column="value_date" property="valueDate"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="transaction_currency" property="txnCurrency"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="total_child" property="numberOfChildTransactions"/>
        <result column="highest_amount" property="highestAmount"/>
        <result column="transaction_amount" property="totalAmount"/>
        <result column="recipients_reference" property="recipientsReference"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="fees_currency" property="feesCurrency"/>
        <result column="fees_amount" property="totalFees"/>
        <result column="transactionCurrency" property="txnCurrency"/>
        <result column="authorization_status" property="authorizationStatus"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
        <result column="rownum" property="rowNum"/>
        <result column="bank_entity_id" property="bankEntityId"/>
    </resultMap>

    <select id="getSingleTransactionDetails" resultMap="transDetailMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				txn.TRANSACTION_ID,
				txn.ACCOUNT_NUMBER,
				txn.COMPANY_ID,
				txni.TRANSACTION_CURRENCY,
				txnfxcon.BOOKING_ID
			FROM PWS_TRANSACTIONS txn
             LEFT JOIN PWS_TRANSACTION_INSTRUCTIONS txni ON txn.TRANSACTION_ID = txni.TRANSACTION_ID
			 LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS txnfxcon on txn.TRANSACTION_ID=txnfxcon.TRANSACTION_ID
 			WHERE txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
 				]]>
    </select>
    <select id="getBulkTransactionDetails" resultMap="transDetailMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				txn.TRANSACTION_ID,
				txn.ACCOUNT_NUMBER,
				txn.COMPANY_ID,
				txni.TRANSACTION_CURRENCY,
				txnfxcon.BOOKING_ID
			FROM PWS_TRANSACTIONS txn
                 LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS txni ON txn.TRANSACTION_ID = txni.TRANSACTION_ID
				 LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS txnfxcon on txn.TRANSACTION_ID=txnfxcon.TRANSACTION_ID
 			WHERE txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
 				]]>
    </select>
    <resultMap id="transDetailMap"
               type="com.uob.gwb.txn.domain.BookFXTransaction">
        <id column="TRANSACTION_ID" property="transactionId"/>
        <result column="ACCOUNT_NUMBER" property="accountNumber"/>
        <result column="COMPANY_ID" property="companyId"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="BOOKING_ID" property="bookingId"/>
    </resultMap>

    <insert id="insertEarmarkingId"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq"
            statementType="PREPARED" useGeneratedKeys="true"
            keyProperty="bookingId" keyColumn="BOOKING_ID">
        <![CDATA[insert into pws_transaction_fx_bookings
           (
	            EARMARK_ID,QUOTE_ID
	       )
	       values(
	             <@p name='fundsResReq.earmarkId'/>,
	             <@p name='fundsResReq.quoteId'/>
	       )]]>
    </insert>

    <select id="updateBookingIdInFxContracts"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_contracts con
					set con.booking_id = <@p name='fundsResReq.bookingId'/>
				  where con.transaction_id IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="updateFXBookingBasedOnTransaction"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_bookings book
					SET book.QUOTE_ID = <@p name='fundsResReq.quoteId'/>,
					    book.BOOKING_TYPE = <@p name='fundsResReq.bookingType'/>,
					    book.BOOKING_REF_ID = <@p name='fundsResReq.bookingRefId'/>,
					    book.RATE = <@p name='fundsResReq.rate'/>,
					    book.UNIT = <@p name='fundsResReq.unit'/>,
					    book.STATUS = <@p name='fundsResReq.status'/>,
					    book.TRANSACTION_CURRENCY = <@p name='fundsResReq.transactionCurrency'/>,
					    book.TRANSACTION_TOTAL_AMOUNT = <@p name='fundsResReq.transactionTotalAmount'/>,
					    book.EQUIVALENT_CURRENCY = <@p name='fundsResReq.equivalentCurrency'/>,
					    book.EQUIVALENT_TOTAL_AMOUNT = <@p name='fundsResReq.equivalentTotalAmount'/>,
					    book.BOOKING_DATE = <@p name='fundsResReq.bookingDate'/>,
					    book.BOOKED_BY = <@p name='fundsResReq.bookedBy'/>,
					    book.INSTRUCTION_TRACE_ID = <@p name='fundsResReq.instructionTraceId'/>,
					    book.SETTLEMENT_CURRENCY = <@p name='fundsResReq.settlementCurrency'/>,
					    book.SETTLEMENT_AMOUNT = <@p name='fundsResReq.settlementAmount'/>
				    where book.BOOKING_ID = <@p name='fundsResReq.bookingId'/>

			]]>
    </select>

    <select id="updateFxTypeInFXContracts"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_contracts con
					SET con.FX_TYPE = <@p name='fundsResReq.fxType'/>,
					con.SETTLEMENT_CURRENCY = <@p name='fundsResReq.settlementCurrency'/>,
					con.RATE = <@p name='fundsResReq.rate'/>,
					con.SETTLEMENT_AMOUNT = <@p name='fundsResReq.settlementAmount'/>
				    where con.transaction_id IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="updateEquivalentAmountInFXContracts"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE pws_transaction_fx_contracts con
					SET con.EQUIVALENT_AMOUNT = <@p name='fundsResReq.totalEquivalentAmount'/>
				     , con.SETTLEMENT_AMOUNT = <@p name='fundsResReq.settlementAmount'/>
					where con.TRANSACTION_ID = <@p name='fundsResReq.transactionId'/>
				    <#if fundsResReq.featureStatus??>
				    AND con.CHILD_BANK_REFERENCE_ID = <@p name='fundsResReq.childReferenceId'/>
				     </#if>
			]]>
    </select>

    <select id="updateEquivalentAmountInInstructions"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE PWS_TRANSACTION_INSTRUCTIONS ins
					SET ins.EQUIVALENT_AMOUNT = <@p name='fundsResReq.totalEquivalentAmount'/>
				    where ins.TRANSACTION_ID = <@p name='fundsResReq.transactionId'/>
			]]>
    </select>
    <select id="getTransactionContract" resultMap="transContractMap" statementType="PREPARED">
        <![CDATA[
			SELECT
				txn.TRANSACTION_ID,
				txn.BANK_REFERENCE_ID,
				txn.RESPONSE_MESSAGE,
				txn.TRANSACTION_AMOUNT,
				txn.EQUIVALENT_AMOUNT,
				txn.CURRENCY_PAIR,
				txnfxb.EQUIVALENT_TOTAL_AMOUNT,
                txnfxb.RATE
			FROM PWS_TRANSACTION_FX_CONTRACTS txn
			LEFT JOIN pws_transaction_fx_bookings txnfxb on txnfxb.booking_id = txn.booking_id
			WHERE txn.TRANSACTION_ID IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
 				]]>
    </select>
    <resultMap id="transContractMap"
               type="com.uob.gwb.txn.domain.FundReservationBookingIdReq">
        <id column="TRANSACTION_ID" property="transactionId"/>
        <result column="BANK_REFERENCE_ID" property="bankReferenceId"/>
        <result column="RESPONSE_MESSAGE" property="status"/>
        <result column="TRANSACTION_AMOUNT" property="transactionTotalAmount"/>
        <result column="EQUIVALENT_AMOUNT" property="equivalentTotalAmount"/>
        <result column="CURRENCY_PAIR" property="currencyPair"/>
        <result column="EQUIVALENT_TOTAL_AMOUNT" property="totalEquivalentAmount"/>
        <result column="RATE" property="rate"/>
    </resultMap>
    <select id="getBulkApprovalStatusTxnList" resultMap="bulkApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT DISTINCT
				btxni.CHILD_TRANSACTION_INSTRUCTIONS_ID,
				txn.CHANGE_TOKEN,
				btxni.CHILD_BANK_REFERENCE_ID,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.TRANSACTION_CURRENCY,
				btxni.TRANSACTION_AMOUNT bulkTransactionAmount,
				btxn.TRANSFER_DATE as VALUE_DATE,
				txn.initiation_time,
				btxni.CUSTOMER_REFERENCE bulkCustomerReference,
				btxni.CUSTOMER_TRANSACTION_STATUS bulkCustomerTransactionStatus,
                txn.company_id,
                pty.beneficiary_reference_id,
                btxni.DESTINATION_COUNTRY bulkDestinationCountry,
                btxni.DESTINATION_BANK_NAME bulkDestinationBankName,
                pty.bank_code,
                pty.swift_code,
                pty.party_name,
                pty.party_account_number,
                pty.party_account_currency,
                pty.party_id,
                txn.TOTAL_CHILD,
                txn.account_number,
                pty.party_modified_date,
                pty.beneficiary_change_token,
                pfu.UPLOAD_FILE_NAME,
                txn.HIGHEST_AMOUNT
			FROM PWS_TRANSACTIONS txn
				LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on txn.transaction_id = btxni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = btxni.transaction_id and btxni.CHILD_BANK_REFERENCE_ID = pty.CHILD_BANK_REFERENCE_ID
		        LEFT JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
				LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
                    <#if filterParams.customerTransactionStatusList?has_content && filterParams.customerTransactionStatusList??>
		             txn.customer_transaction_status NOT IN
		             (<#list filterParams.customerTransactionStatusList as customerTransactionStatus> <@p value=customerTransactionStatus/> <#if customerTransactionStatus_has_next>,</#if> </#list> )
                   </#if>
                     <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                            AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                            AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                         </#if>
                     </#if>
                   <#if filterParams.bulkCustomerTransactionStatus?has_content && filterParams.bulkCustomerTransactionStatus??>
		              AND btxni.CUSTOMER_TRANSACTION_STATUS NOT IN ( ${filterParams.bulkCustomerTransactionStatus})
                  </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                 <#if filterParams.accountBasedOnResourceFeatureList?has_content && filterParams.accountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.accountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                    </#if>
                  <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		              AND txn.company_group_id in ${filterParams.companyGroupId}
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		              AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>
                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in  (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                    </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					       and btxni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
					 </#if>
					 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					    and btxni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					    and btxni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
					  </#if>

	                 <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                 AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                          AND btxni.customer_reference IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)
                      </#if>

                          <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                             AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )
                          </#if>
                          <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		                     AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )
                          </#if>

                          <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		                       AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )
                          </#if>
                          <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		                       AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                          </#if>

                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>

                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
                     <#if filterParams.excludeBatch == true>
                       AND txn.batch_id is null
                     </#if>
                     <#if filterParams.batchId?has_content>
                       AND txn.batch_id = <@p name='filterParams.batchId'/>
                     </#if>

                 ORDER BY ${filterParams.sortFieldWithDirection}
			     OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>

    <resultMap id="bulkApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="CHILD_TRANSACTION_INSTRUCTIONS_ID" property="transactionId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="CHILD_BANK_REFERENCE_ID" property="childBankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="bulkTransactionAmount" property="bulkTransactionAmount"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="bulkCustomerReference" property="bulkCustomerReference"/>
        <result column="bulkCustomerTransactionStatus" property="customerStatus"/>
        <result column="company_id" property="companyId"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="bulkDestinationCountry" property="bulkDestinationCountry"/>
        <result column="bulkDestinationBankName" property="bulkDestinationBankName"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="party_id" property="partyId"/>
        <result column="TOTAL_CHILD" property="totalChild"/>
        <result column="account_number" property="accountNumber"/>
        <result column="party_modified_date" property="partyModifiedDate"/>
        <result column="beneficiary_change_token" property="beneficiaryChangeToken"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
    </resultMap>

    <select id="getBulkApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count FROM
				(
				SELECT DISTINCT
				btxni.CHILD_TRANSACTION_INSTRUCTIONS_ID
			FROM PWS_TRANSACTIONS txn
				LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on txn.transaction_id = btxni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = btxni.transaction_id and btxni.CHILD_BANK_REFERENCE_ID = pty.CHILD_BANK_REFERENCE_ID
		        LEFT JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
				LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
                    <#if filterParams.customerTransactionStatusList?has_content && filterParams.customerTransactionStatusList??>
		               txn.customer_transaction_status NOT IN
		                  (<#list filterParams.customerTransactionStatusList as customerTransactionStatus> <@p value=customerTransactionStatus/> <#if customerTransactionStatus_has_next>,</#if> </#list> )
                    </#if>
                 <#if filterParams.bulkCustomerTransactionStatus?has_content && filterParams.bulkCustomerTransactionStatus??>
		              AND btxni.CUSTOMER_TRANSACTION_STATUS NOT IN ( ${filterParams.bulkCustomerTransactionStatus})
                  </#if>
                 <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                            AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                            AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                         </#if>
                     </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                  <#if filterParams.accountBasedOnResourceFeatureList?has_content && filterParams.accountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.accountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                    </#if>
                   <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		              AND txn.company_group_id in ${filterParams.companyGroupId}
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		              AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                </#if>
                   <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>

                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in  (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                 </#if>
                 <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                 </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					       and btxni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
					 </#if>
					 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					    and btxni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					    and btxni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
					  </#if>

	                 <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                 AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                          AND btxni.customer_reference IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)
                     </#if>

                     <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                            AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )
                     </#if>
                    <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		                    AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )
                    </#if>

                    <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		                    AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )
                    </#if>
                     <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		                    AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>

                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>

                )

		]]>
    </select>
    <resultMap id="bulkApprovalStatusTxnCount"
               type="int">
        <result column="count" property="count"/>
    </resultMap>
    <select id="getBulkParentApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count FROM
				(SELECT DISTINCT txn.transaction_id
			FROM PWS_TRANSACTIONS txn
			    LEFT JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
			    LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
			   <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		               txn.company_group_id in ${filterParams.companyGroupId} AND
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		               txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
                </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		             txn.TRANSACTION_ID in (${filterParams.transactionId}) AND
                </#if>
               <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                             txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                             txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
                         </#if>
                     </#if>
                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		              txn.TRANSACTION_ID  not in (${filterParams.excludeTransactionId}) AND
                 </#if>
				       <#if filterParams.accountBasedOnResourceFeatureList?has_content && filterParams.accountBasedOnResourceFeatureList??>
                      (
					   <#list filterParams.accountBasedOnResourceFeatureList?chunk(999) as chunk>
					   (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in
					   (
					   <#list chunk as companyAccountGroup>
					   <@p value=companyAccountGroup/>
					   <#if companyAccountGroup_has_next>,</#if>
					   </#list>
					   )
					    <#if chunk_has_next> OR </#if>
						 </#list>
					   ) AND
                    </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		              txn.COMPANY_NAME in ${filterParams.companyName} AND
                  </#if>
                  <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		               txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		                txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
                    </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
                         txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> and
					 </#if>
					  <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					     txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> and
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					     txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> and
					  </#if>
	                 <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                  txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                     </#if>
                       <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                         btxn.RECIPIENTS_REFERENCE IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                           trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                 txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.batchId?has_content>
                         txn.batch_id = <@p name='filterParams.batchId'/> AND
                     </#if>
                    txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )
                )

		]]>
    </select>

    <select id="getBulkBothApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count
			FROM (
                <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
                    SELECT DISTINCT
				        txn.transaction_id,
				        txn.account_number,
				        txn.account_currency,
				        txn.initiation_time,
				        txn.customer_transaction_status,
				        txn.bank_reference_id,
				        txn.resource_id,
				        txn.feature_id,
				        txn.company_id,
				        txn.company_group_id,
				        txn.CHANGE_TOKEN,
				        txni.transaction_currency,
				        txni.transaction_amount AS TOTAL_AMOUNT,
				        txni.customer_reference AS RECIPIENTS_REFERENCE,
				        txni.value_date as VALUE_DATE,
				        txni.is_recurring,
				        txni.destination_country,
				        txni.destination_bank_name,
				        txni.fx_flag,
				        pty.party_name,
				        pty.party_account_number,
				        pty.party_account_currency,
				        pty.bank_code,
				        pty.swift_code,
				        pty.beneficiary_reference_id,
				        pty.party_type,
				        pty.residency_status,
				        pty.party_id,
				        pty.proxy_id_type,
				        pty.id_issuing_country,
				        ptyc.address_line_1,
				        ptyc.address_line_2,
				        ptyc.address_line_3,
				        ptyc.phone_country,
				        ptyc.phone_country_code,
				        ptyc.phone_no,
				        ptfc.fx_type,
                        ptfb.booking_ref_id,
                        ptfb.earmark_id,
				        txn.TOTAL_CHILD,
                        txn.HIGHEST_AMOUNT,
                        null AS UPLOAD_FILE_NAME
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			        WHERE
        ]]>

        <include refid="fragmentGetBulkBothApprovalStatusTxnCount1"></include>
        <![CDATA[
        <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
			UNION ALL
			</#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
			SELECT DISTINCT
				txn.transaction_id,
				txn.account_number,
				txn.account_currency,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT,
				btxn.RECIPIENTS_REFERENCE,
				btxn.TRANSFER_DATE as VALUE_DATE,
				null as is_recurring,
				null as destination_country,
				null as destination_bank_name,
				null as fx_flag,
				null as party_name,
				null as party_account_number,
				null as party_account_currency,
				null as bank_code,
				null as swift_code,
				null as beneficiary_reference_id,
				null as party_type,
				null as residency_status,
				null as party_id,
				null as proxy_id_type,
				null as id_issuing_country,
				null as address_line_1,
				null as address_line_2,
				null as address_line_3,
				null as phone_country,
				null as phone_country_code,
				null as phone_no,
				null as fx_type,
				null as booking_ref_id,
				null as earmark_id,
				txn.TOTAL_CHILD,
				txn.HIGHEST_AMOUNT,
				pfu.upload_file_name AS UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
        ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount2"></include>

        <![CDATA[
           </#if>
			) bothTrans
                 ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>
    <select id="getBulkBothApprovalStatusTxnList" resultMap="bothParentApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
        SELECT DISTINCT
               transaction_id,
				account_number,
				account_currency,
				initiation_time,
				customer_transaction_status,
				bank_reference_id,
				resource_id,
				feature_id,
				company_id,
				company_group_id,
				CHANGE_TOKEN,
				transaction_currency,
				TOTAL_AMOUNT,
				RECIPIENTS_REFERENCE,
				VALUE_DATE,
				original_uetr,
				is_recurring,
				destination_country,
				destination_bank_name,
				fx_flag,
				party_name,
				party_account_number,
				party_account_currency,
				bank_code,
				swift_code,
				beneficiary_reference_id,
				party_type,
				residency_status,
				party_id,
				proxy_id_type,
				id_issuing_country,
				address_line_1,
				address_line_2,
				address_line_3,
				phone_country,
				phone_country_code,
				phone_no,
				fx_type,
                booking_ref_id,
                earmark_id,
				TOTAL_CHILD,
                HIGHEST_AMOUNT,
                upload_file_name,
                correlation_Id,
                batch_id
             FROM
                (
                <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
                    SELECT
				        txn.transaction_id,
				        txn.account_number,
				        txn.account_currency,
				        txn.initiation_time,
				        txn.customer_transaction_status,
				        txn.bank_reference_id,
				        txn.resource_id,
				        txn.feature_id,
				        txn.company_id,
				        txn.company_group_id,
				        txn.CHANGE_TOKEN,
				        txni.transaction_currency,
				        txni.transaction_amount AS TOTAL_AMOUNT,
				        txni.customer_reference AS RECIPIENTS_REFERENCE,
				        txni.value_date as VALUE_DATE,
				        txni.original_uetr,
				        txni.is_recurring,
				        txni.destination_country,
				        txni.destination_bank_name,
				        txni.fx_flag,
				        pty.party_name,
				        pty.party_account_number,
				        pty.party_account_currency,
				        pty.bank_code,
				        pty.swift_code,
				        pty.beneficiary_reference_id,
				        pty.party_type,
				        pty.residency_status,
				        pty.party_id,
				        pty.proxy_id_type,
				        pty.id_issuing_country,
				        ptyc.address_line_1,
				        ptyc.address_line_2,
				        ptyc.address_line_3,
				        ptyc.phone_country,
				        ptyc.phone_country_code,
				        ptyc.phone_no,
				        ptfc.fx_type,
                        ptfb.booking_ref_id,
                        ptfb.earmark_id,
				        txn.TOTAL_CHILD,
                        txn.HIGHEST_AMOUNT,
                        null AS UPLOAD_FILE_NAME,
                        null as correlation_Id,
                        txn.batch_id
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
                AND ptyc.PARTY_CONTACT_TYPE = <@p value=partyContactType/>
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			        WHERE
                <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		               txn.company_group_id in ${filterParams.companyGroupId} AND
                 </#if>
                <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                       txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
			  (
        <#list filterParams.accountNumberList?chunk(999) as chunk>
          ( txn.account_number)
		              IN (
                <#list chunk as accountNumber>
                    <@p value=accountNumber/>
                    <#if accountNumber_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
  </#if>
 				<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
		            txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                </#if>
                <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			  UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
       			</#if>
                <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
		              txni.customer_reference in (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
                </#if>
               <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
		              UPPER(txni.customer_reference) LIKE UPPER('%${filterParams.customerRefIds}%') AND
                </#if>
                 <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
      (
        <#list filterParams.singleAccountBasedOnResourceFeatureList?chunk(999) as chunk>
            (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
            IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		              txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
		              txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND

                </#if>
                <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					 txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> AND
	            </#if>
	             <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					     txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/> AND
			     </#if>
				 <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					     txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/> AND
				 </#if>
                <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
		              pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> ) AND

                </#if>
                <#if filterParams.payerName?has_content && filterParams.payerName??>
		              UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%') AND
                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		              pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> ) AND

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		              pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> ) AND

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		              pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> ) AND
                 </#if>

                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                           trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                           txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
                </#if>
                <#if filterParams.excludeBatch == true>
                         txn.batch_id is null AND
                </#if>
                 <#if filterParams.batchId?has_content>
                        txn.batch_id = <@p name='filterParams.batchId'/> AND
                 </#if>
                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		             txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
                </#if>

			txn.customer_transaction_status NOT IN (
                        'PENDING_SUBMIT',
                        'NEW',
                        'DELETED'
                        )
                AND nvl(txn.application_type,'X') != 'terminateRecurring'
             </#if>
             <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
			UNION ALL
			</#if>
		<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
			SELECT
				txn.transaction_id,
				txn.account_number,
				txn.account_currency,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT,
				btxn.RECIPIENTS_REFERENCE,
				btxn.TRANSFER_DATE as VALUE_DATE,
				btxni.original_uetr as original_uetr,
				null as is_recurring,
				null as destination_country,
				null as destination_bank_name,
				null as fx_flag,
				null as party_name,
				null as party_account_number,
				null as party_account_currency,
				null as bank_code,
				null as swift_code,
				null as beneficiary_reference_id,
				null as party_type,
				null as residency_status,
				null as party_id,
				null as proxy_id_type,
				null as id_issuing_country,
				null as address_line_1,
				null as address_line_2,
				null as address_line_3,
				null as phone_country,
				null as phone_country_code,
				null as phone_no,
				null as fx_type,
				null as booking_ref_id,
				null as earmark_id,
				txn.TOTAL_CHILD,
				txn.HIGHEST_AMOUNT,
				pfu.upload_file_name AS UPLOAD_FILE_NAME,
				null as correlation_Id,
				txn.batch_id
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on txn.transaction_id = btxni.transaction_id
			LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
			WHERE
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		               txn.company_group_id in ${filterParams.companyGroupId} AND
                 </#if>
              <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                       txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		               txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
                </#if>
                 <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			  (
        <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
          (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		              IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		             txn.TRANSACTION_ID in ${filterParams.transactionId} AND
                </#if>
                <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                             txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                             txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
                         </#if>
                     </#if>
                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		              txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId} AND
                 </#if>

                 <#if filterParams.companyName?has_content && filterParams.companyName??>
		              txn.COMPANY_NAME in ${filterParams.companyName} AND
                 </#if>
                 <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		               txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
                 </#if>
                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
                 </#if>
                 <#if filterParams.statusList?has_content && filterParams.statusList??>
		                txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
                 </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
                 </#if>
                 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
                         txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> AND
				 </#if>
				  <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					     txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> AND
			     </#if>
				 <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					     txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> AND
				 </#if>
	             <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
		                  txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                 </#if>
                  <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			  UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
       			</#if>
                 <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                         btxn.RECIPIENTS_REFERENCE IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
                 </#if>
                 <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                         btxn.RECIPIENTS_REFERENCE LIKE UPPER('%${filterParams.customerRefIds}%') AND
                 </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                           trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
                 </#if>
                 <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                 txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
                 </#if>
                 <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
                 </#if>
                 <#if filterParams.excludeBatch == true>
                        txn.batch_id is null AND
                </#if>
                <#if filterParams.batchId?has_content>
                        txn.batch_id = <@p name='filterParams.batchId'/> AND
                </#if>
txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )
            AND nvl(txn.application_type,'X') != 'terminateRecurring'
        </#if>
        Union All
        SELECT
         txn.transaction_id,
		 txn.account_number,
		 txn.account_currency,
		 txn.initiation_time,
        rtxn.status AS customer_transaction_status,
		txn.bank_reference_id,
		txn.resource_id,
		txn.feature_id,
		txn.company_id,
		txn.company_group_id,
        rtxn.change_token AS CHANGE_TOKEN,
		txni.transaction_currency,
		txni.transaction_amount AS TOTAL_AMOUNT,
		txni.customer_reference AS RECIPIENTS_REFERENCE,
        rtxn.processing_date AS value_date,
        rtxn.original_uetr as original_uetr,
	txni.is_recurring,  txni.destination_country,
 txni.destination_bank_name, txni.fx_flag,  pty.party_name,  pty.party_account_number, pty.party_account_currency,
 pty.bank_code,  pty.swift_code,  pty.beneficiary_reference_id,
 pty.party_type,  pty.residency_status,  pty.party_id,
 pty.proxy_id_type,  pty.id_issuing_country, ptyc.address_line_1, ptyc.address_line_2, ptyc.address_line_3, ptyc.
phone_country, ptyc.phone_country_code, ptyc.phone_no, ptfc.fx_type, ptfb.booking_ref_id, ptfb.
earmark_id, txn.TOTAL_CHILD, txn.HIGHEST_AMOUNT, null as UPLOAD_FILE_NAME, rtxn.correlation_id  AS correlation_Id, txn.batch_id
   from
    pws_transaction_instructions txni
JOIN
    pws_recurring_transactions rtxn
ON
    txni.transaction_id = rtxn.transaction_id
    Join
            pws_transactions txn on
            txn.transaction_id = rtxn.transaction_id
			LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
                AND ptyc.PARTY_CONTACT_TYPE = <@p value=partyContactType/>
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
WHERE
    txni.is_recurring = 'Y'
    AND txn.customer_transaction_status IN ('PROCESSING', 'SUCCESSFUL', 'REJECTED', 'TERMINATED')
			) bothTrans
			 ORDER BY ${filterParams.sortFieldWithDirection}
			     OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>
    <resultMap id="bothParentApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="company_id" property="companyId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="TOTAL_AMOUNT" property="transactionAmount"/>
        <result column="TOTAL_AMOUNT" property="bulkTransactionAmount"/>
        <result column="RECIPIENTS_REFERENCE" property="customerReference"/>
        <result column="RECIPIENTS_REFERENCE" property="bulkCustomerReference"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="VALUE_DATE" property="valueDate"/>
        <result column="VALUE_DATE" property="boReleaseDateTime"/>
        <result column="original_uetr" property="originalUetr"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="TOTAL_CHILD" property="totalChild"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
        <result column="correlation_Id" property="correlationId"/>
    </resultMap>

    <select id="getBulkParentApprovalStatusTxnList" resultMap="bulkParentApprovalStatusTxnMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT DISTINCT
				txn.transaction_id,
				txn.CHANGE_TOKEN,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT,
				btxn.TRANSFER_DATE as VALUE_DATE,
				txn.initiation_time,
				btxn.RECIPIENTS_REFERENCE,
				txn.customer_transaction_status,
                txn.company_id,
                txn.TOTAL_CHILD,
                txn.HIGHEST_AMOUNT,
                txn.account_number,
                pfu.UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE
			   <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                   txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
                </#if>
                <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
                       txn.company_group_id in ${filterParams.companyGroupId} AND
                </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		               txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
                </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		             txn.TRANSACTION_ID in (${filterParams.transactionId}) AND
                </#if>
                <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                        <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                             txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
                        </#if>
                        <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                             txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
                         </#if>
                </#if>
                <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		              txn.TRANSACTION_ID  not in (${filterParams.excludeTransactionId}) AND
                </#if>
                 <#if filterParams.accountBasedOnResourceFeatureList?has_content && filterParams.accountBasedOnResourceFeatureList??>
                        (
                            <#list filterParams.accountBasedOnResourceFeatureList?chunk(999) as chunk>
                            txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
                            IN (
                                <#list chunk as companyAccountGroup>
                                <@p value=companyAccountGroup/>
                                <#if companyAccountGroup_has_next>,</#if>
                            </#list>
                            )
                            <#if chunk_has_next> OR </#if>
                            </#list>
		               ) AND
                </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		              txn.COMPANY_NAME in ${filterParams.companyName} AND
                  </#if>

                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		               txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		                txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                 txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
                    </#if>
                     <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
                         txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> and
					 </#if>
					 <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					     txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> and
					  </#if>
					  <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					     txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> and
					  </#if>
	                 <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                  txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
                     </#if>
                     <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                         btxn.RECIPIENTS_REFERENCE IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                           trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                 txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                 txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
                     </#if>
                     <#if filterParams.excludeBatch == true>
                        txn.batch_id is null AND
                     </#if>
                     <#if filterParams.batchId?has_content>
                        txn.batch_id = <@p name='filterParams.batchId'/> AND
                     </#if>
                     txn.customer_transaction_status NOT IN (
                    'PENDING_SUBMIT',
                    'NEW',
                    'DELETED'
                )

                 ORDER BY ${filterParams.sortFieldWithDirection}
			     OFFSET ${filterParams.offset} ROWS FETCH NEXT ${filterParams.limit} ROWS ONLY
		]]>
    </select>

    <resultMap id="bulkParentApprovalStatusTxnMap"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resouceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="TOTAL_AMOUNT" property="bulkTransactionAmount"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="RECIPIENTS_REFERENCE" property="bulkCustomerReference"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="company_id" property="companyId"/>
        <result column="TOTAL_CHILD" property="totalChild"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
        <result column="account_number" property="accountNumber"/>
        <result column="upload_file_name" property="uploadFileName"/>
    </resultMap>

    <select id="getChargesDetail" resultMap="getBulkPWSChargesDetailsMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[select
				   TRANSACTION_ID
				   ,FEES_CURRENCY
				   ,FEES_AMOUNT
				from PWS_TRANSACTION_CHARGES
				where TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="getFxContracts" resultMap="getFxTransactions"
            parameterType="list" statementType="PREPARED">
        <![CDATA[SELECT
                     *
                     FROM (
                             SELECT
                              txn_rec.*,
                                 ROW_NUMBER()
                                 OVER(PARTITION BY transaction_id
                             ORDER BY
                                transaction_id
                             ) rw_num
                     FROM
                        (
                        SELECT
                               txn.transaction_id,
                               ptfc.fx_type,
                               ptfb.booking_ref_id,
                               ptfb.earmark_id,
                               CASE WHEN txni.fx_flag is null THEN btxni.fx_flag ELSE txni.fx_flag  END  AS fx_flag
                                 FROM
                                    pws_transactions  txn
                                    LEFT JOIN PWS_TRANSACTION_INSTRUCTIONS  txni on txn.transaction_id = txni.transaction_id
                                    LEFT JOIN pws_bulk_transaction_instructions btxni ON txn.transaction_id = btxni.transaction_id
                                    LEFT JOIN pws_transaction_fx_contracts      ptfc ON ptfc.transaction_id = txn.transaction_id
                                    LEFT JOIN pws_transaction_fx_bookings       ptfb ON ptfb.booking_id = ptfc.booking_id
                                     WHERE
                                        txn.transaction_id IN ( <#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
                         ) txn_rec
                    )
      WHERE
    rw_num = 1
			]]>
    </select>
    <resultMap id="getFxTransactions"
               type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="transactionId"/>
        <result column="fx_type" property="fxType"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
    </resultMap>

    <resultMap id="getBulkPWSChargesDetailsMap"
               type="com.uob.gwb.txn.domain.PwsTransactionCharges">
        <id column="TRANSACTION_ID" property="transactionId"/>
        <result column="FEES_CURRENCY" property="feesCurrency"/>
        <result column="FEES_AMOUNT" property="feesAmount"/>
    </resultMap>
    <select id="getFXContractDetailsBasedOnTransaction" resultMap="getBulkPWSContractDetailsMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[select
				   con.BANK_REFERENCE_ID
				   ,con.TRANSACTION_ID
				   ,con.TRANSACTION_AMOUNT
				   ,con.CHILD_BANK_REFERENCE_ID
				   ,con.CURRENCY_PAIR
				   ,book.EQUIVALENT_TOTAL_AMOUNT bookingEquivalentAmount
				   ,book.RATE BookingRate
				from PWS_TRANSACTION_FX_CONTRACTS con
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS book
				ON con.BOOKING_ID = book.BOOKING_ID
				where con.TRANSACTION_ID = <@p name='transactionId'/>
			]]>
    </select>

    <resultMap id="getBulkPWSContractDetailsMap"
               type="com.uob.gwb.txn.domain.PwsFxContract">
        <result column="BANK_REFERENCE_ID" property="bankReferenceId"/>
        <result column="TRANSACTION_ID" property="transactionId"/>
        <result column="TRANSACTION_AMOUNT" property="transactionAmount"/>
        <result column="CHILD_BANK_REFERENCE_ID" property="childBankRefId"/>
        <result column="CURRENCY_PAIR" property="currencyPair"/>
        <result column="bookingEquivalentAmount" property="bookingEquivalentAmount"/>
        <result column="BookingRate" property="BookingRate"/>
    </resultMap>

    <select id="updateEquivalentAmountInBulkInstructions"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE PWS_BULK_TRANSACTION_INSTRUCTIONS ins
					SET ins.EQUIVALENT_AMOUNT = <@p name='fundsResReq.totalEquivalentAmount'/>
				    where ins.TRANSACTION_ID = <@p name='fundsResReq.transactionId'/>
				    AND ins.CHILD_BANK_REFERENCE_ID = <@p name='fundsResReq.childReferenceId'/>
			]]>
    </select>


    <select id="getBulkMyTaskList" resultMap="MyTaskMap"
            statementType="PREPARED">
        <![CDATA[
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
                txn.change_token,
				txn.account_number,
				txn.account_currency,
				txn.initiation_time,
				txn.bank_reference_id,
				txn.customer_transaction_status,
				txn.authorization_status,
				txn.company_id,
				txn.resource_id,
				txn.feature_id,
				txn.initiated_by,
				txn.total_child,
				txn.highest_amount,
				txn.total_amount as transaction_amount,
				txn.transaction_currency,
				btxn.recipients_reference,
				btxn.transfer_date as VALUE_DATE,
				pfu.UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_BULK_TRANSACTIONS btxn on txn.transaction_id = btxn.transaction_id
				LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE 1=1
			    AND txn.BATCH_ID is null
			    AND nvl(txn.application_type,'X') != 'terminateRecurring'
			    <#if filterParams.accountBasedOnResourceFeatureList?has_content && filterParams.accountBasedOnResourceFeatureList??>
                     AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		             in (<#list filterParams.accountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                </#if>
				<#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
                          AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.recipientsReferenceList?has_content && filterParams.recipientsReferenceList??>
                          AND btxn.recipients_reference in (<#list filterParams.recipientsReferenceList as resRef> <@p value=resRef/>  <#if resRef_has_next>,</#if> </#list>)

                </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
                          AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
               <#if filterParams.statusList?has_content && filterParams.statusList??>
                          AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND txn.initiation_time between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
	            <#if filterParams.transactionFromAmount?has_content >
					and txn.total_amount >= <@p name='filterParams.transactionFromAmount'/>
	            </#if>
	            <#if filterParams.transactionToAmount?has_content >
					and txn.total_amount <= <@p name='filterParams.transactionToAmount'/>
	            </#if>
	            <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
                          AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
                          AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
                          AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )

                </#if>

                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
                          AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.bulkFromDate?has_content && filterParams.bulkFromDate?? && filterParams.bulkToDate?has_content && filterParams.bulkToDate??>
                          AND trunc(btxn.transfer_date) between '${filterParams.bulkFromDate}' and '${filterParams.bulkToDate}'
                </#if>
                <#if filterParams.transactionId?has_content>
                          AND txn.transaction_id in (${filterParams.transactionId})
                </#if>
                <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		         		  AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                </#if>
                <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		            	  AND txn.TRANSACTION_ID  not in (${filterParams.excludeTransactionId})
                </#if>
			ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>

    <select id="getBulkBothMyTaskTxnList" resultMap="bothParentMyTaskTxnMap"
            statementType="PREPARED">
        <![CDATA[
        SELECT
                bank_entity_id,
                transaction_id,
				account_number,
				account_currency,
				initiated_by,
				initiation_time,
				customer_transaction_status,
				authorization_status,
				bank_reference_id,
				resource_id,
				feature_id,
				company_id,
				company_group_id,
				CHANGE_TOKEN,
				transaction_currency,
				transaction_amount,
				RECIPIENTS_REFERENCE,
				VALUE_DATE,
				is_recurring,
				destination_country,
				destination_bank_name,
				fx_flag,
				party_name,
				party_account_number,
				party_account_currency,
				bank_code,
				swift_code,
				beneficiary_reference_id,
				party_type,
				residency_status,
				party_id,
				proxy_id_type,
				id_issuing_country,
				address_line_1,
				address_line_2,
				address_line_3,
				phone_country,
				phone_country_code,
				phone_no,
				fx_type,
                booking_ref_id,
                earmark_id,
				TOTAL_CHILD,
                HIGHEST_AMOUNT,
                UPLOAD_FILE_NAME
             FROM
                (
                <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
                    SELECT
                        txn.bank_entity_id,
				        txn.transaction_id,
				        txn.account_number,
				        txn.account_currency,
				        txn.initiated_by,
				        txn.initiation_time,
				        txn.customer_transaction_status,
				        txn.authorization_status,
				        txn.bank_reference_id,
				        txn.resource_id,
				        txn.feature_id,
				        txn.company_id,
				        txn.company_group_id,
				        txn.CHANGE_TOKEN,
				        txni.transaction_currency,
				        txni.transaction_amount,
				        txni.customer_reference AS RECIPIENTS_REFERENCE,
				        txni.value_date as VALUE_DATE,
				        txni.is_recurring,
				        txni.destination_country,
				        txni.destination_bank_name,
				        txni.fx_flag,
				        pty.party_name,
				        pty.party_account_number,
				        pty.party_account_currency,
				        pty.bank_code,
				        pty.swift_code,
				        pty.beneficiary_reference_id,
				        pty.party_type,
				        pty.residency_status,
				        pty.party_id,
				        pty.proxy_id_type,
				        pty.id_issuing_country,
				        ptyc.address_line_1,
				        ptyc.address_line_2,
				        ptyc.address_line_3,
				        ptyc.phone_country,
				        ptyc.phone_country_code,
				        ptyc.phone_no,
				        ptfc.fx_type,
                        ptfb.booking_ref_id,
                        ptfb.earmark_id,
				        txn.TOTAL_CHILD,
                        txn.HIGHEST_AMOUNT,
                        null AS UPLOAD_FILE_NAME
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			        WHERE 1=1
			         AND txn.BATCH_ID is null
			         AND nvl(txn.application_type,'X') != 'terminateRecurring'
                <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                 </#if>
				<#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		           AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>
                <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
		             AND txni.customer_reference in (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)

                </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		             AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
		             AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
                <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
		             AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		             AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		             AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		             AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                 </#if>

                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		            AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>
            </#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
			UNION ALL
			</#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
				txn.account_number,
				txn.account_currency,
				txn.initiated_by,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.authorization_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT as transaction_amount,
				btxn.RECIPIENTS_REFERENCE,
				btxn.TRANSFER_DATE as VALUE_DATE,
				null as is_recurring,
				null as destination_country,
				null as destination_bank_name,
				null as fx_flag,
				null as party_name,
				null as party_account_number,
				null as party_account_currency,
				null as bank_code,
				null as swift_code,
				null as beneficiary_reference_id,
				null as party_type,
				null as residency_status,
				null as party_id,
				null as proxy_id_type,
				null as id_issuing_country,
				null as address_line_1,
				null as address_line_2,
				null as address_line_3,
				null as phone_country,
				null as phone_country_code,
				null as phone_no,
				null as fx_type,
				null as booking_ref_id,
				null as earmark_id,
				txn.TOTAL_CHILD,
				txn.HIGHEST_AMOUNT,
				pfu.UPLOAD_FILE_NAME AS UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE 1=1
			AND txn.BATCH_ID is null
			 <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.bulkAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                 </#if>
                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>
                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                   </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                    </#if>
 	                <#if filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount?has_content >
					  and txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	                </#if>
	                 <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
		                 AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                        AND btxn.RECIPIENTS_REFERENCE IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
			</#if>
			) bothTrans
			 ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>
    <resultMap id="bothParentMyTaskTxnMap"
               type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="transactionId"/>
        <result column="account_number" property="accountNumber"/>
        <result column="account_currency" property="accountCurrency"/>
        <result column="initiated_by" property="userId"/>
        <result column="initiation_time" property="initiationTime"/>
        <result column="customer_transaction_status" property="customerStatus"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="resource_id" property="resourceId"/>
        <result column="feature_id" property="featureId"/>
        <result column="company_id" property="companyId"/>
        <result column="CHANGE_TOKEN" property="changeToken"/>
        <result column="TRANSACTION_CURRENCY" property="txnCurrency"/>
        <result column="TRANSACTION_CURRENCY" property="transactionCurrency"/>
        <result column="transaction_amount" property="totalAmount"/>
        <result column="transaction_amount" property="transactionAmount"/>
        <result column="customer_reference" property="customerReference"/>
        <result column="recipients_reference" property="recipientsReference"/>
        <result column="VALUE_DATE" property="transferDate"/>
        <result column="VALUE_DATE" property="valueDate"/>
        <result column="is_recurring" property="isRecurringPayment"/>
        <result column="destination_country" property="destinationCountry"/>
        <result column="destination_bank_name" property="destinationBankName"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="party_name" property="partyName"/>
        <result column="party_account_number" property="partyAccountNumber"/>
        <result column="party_account_currency" property="partyAccountCurrency"/>
        <result column="bank_code" property="bankCode"/>
        <result column="swift_code" property="swiftCode"/>
        <result column="beneficiary_reference_id" property="beneficiaryReferenceId"/>
        <result column="party_type" property="partyType"/>
        <result column="residency_status" property="residencyStatus"/>
        <result column="party_id" property="partyId"/>
        <result column="proxy_id_type" property="proxyIdType"/>
        <result column="id_issuing_country" property="idIssuingCountry"/>
        <result column="address_line_1" property="partyAddress1"/>
        <result column="address_line_2" property="partyAddress2"/>
        <result column="address_line_3" property="partyAddress3"/>
        <result column="phone_country" property="partyPhoneCountryName"/>
        <result column="phone_country_code" property="partyPhoneCountryCode"/>
        <result column="phone_no" property="partyPhoneNumber"/>
        <result column="fx_type" property="fxType"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
        <result column="TOTAL_CHILD" property="numberOfChildTransactions"/>
        <result column="HIGHEST_AMOUNT" property="highestAmount"/>
        <result column="authorization_status" property="authorizationStatus"/>
        <result column="rownum" property="rowNum"/>
        <result column="bank_entity_id" property="bankEntityId"/>
        <result column="UPLOAD_FILE_NAME" property="uploadFileName"/>
    </resultMap>

    <select id="getMyTaskFxContracts" resultMap="FxMyTaskMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[
            select
                distinct
                txn.TRANSACTION_ID,
				ptfc.fx_type,
                ptfb.booking_ref_id,
                ptfb.earmark_id,
                CASE
                WHEN txni.fx_flag is null THEN btxni.fx_flag
                ELSE txni.fx_flag
                END
                AS fx_flag
            FROM PWS_TRANSACTIONS txn
            LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on btxni.transaction_id = txn.transaction_id
            LEFT JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txni.transaction_id = txn.transaction_id
            LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txn.transaction_id
            LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
            where txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
			]]>
    </select>

    <resultMap id="FxMyTaskMap"
               type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="transactionId"/>
        <result column="fx_type" property="fxType"/>
        <result column="fx_flag" property="fxFlag"/>
        <result column="booking_ref_id" property="bookingRefId"/>
        <result column="earmark_id" property="earmarkId"/>
    </resultMap>
    <select id="getTransactionDetailsBasedOnTransactionId" resultMap="transactionByTransactionIdMap"
            parameterType="list" statementType="PREPARED">
        <![CDATA[
            select
                txn.TRANSACTION_ID,
				txn.RESOURCE_ID,
                txn.FEATURE_ID
            FROM PWS_TRANSACTIONS txn
            where txn.TRANSACTION_ID IN (<#list transactionIds as transactionId> <@p value=transactionId/> <#if transactionId_has_next>,</#if> </#list>)
			]]>
    </select>

    <resultMap id="transactionByTransactionIdMap"
               type="com.uob.gwb.txn.domain.PwsTransactions">
        <id column="TRANSACTION_ID" property="transactionId"/>
        <result column="RESOURCE_ID" property="resourceId"/>
        <result column="FEATURE_ID" property="featureId"/>
    </resultMap>

    <select id="updateTransactionDetail"
            parameterType="com.uob.gwb.txn.domain.FundReservationBookingIdReq" statementType="PREPARED">
        <![CDATA[UPDATE PWS_TRANSACTIONS trans
					SET trans.RATE = <@p name='fundsResReq.rate'/>
					where trans.transaction_id IN (<#list fundsResReq.listOfTransIds as transIds> <@p value=transIds/> <#if transIds_has_next>,</#if> </#list>)
			]]>
    </select>

    <select id="getMyTaskTxnList" resultMap="bothParentMyTaskTxnMap"
            statementType="PREPARED">
        <![CDATA[
                SELECT
                bank_entity_id,
                transaction_id,
				account_number,
				account_currency,
				initiated_by,
				initiation_time,
				customer_transaction_status,
				authorization_status,
				bank_reference_id,
				resource_id,
				feature_id,
				company_id,
				company_group_id,
				CHANGE_TOKEN,
				transaction_currency,
				transaction_amount,
				customer_reference,
				RECIPIENTS_REFERENCE,
				VALUE_DATE,
				is_recurring,
				destination_country,
				destination_bank_name,
				fx_flag,
				party_name,
				party_account_number,
				party_account_currency,
				bank_code,
				swift_code,
				beneficiary_reference_id,
				party_type,
				residency_status,
				party_id,
				proxy_id_type,
				id_issuing_country,
				address_line_1,
				address_line_2,
				address_line_3,
				phone_country,
				phone_country_code,
				phone_no,
				fx_type,
                booking_ref_id,
                earmark_id,
				TOTAL_CHILD,
                HIGHEST_AMOUNT,
                UPLOAD_FILE_NAME
             FROM
                (
                    SELECT
                        txn.bank_entity_id,
				        txn.transaction_id,
				        txn.account_number,
				        txn.account_currency,
				        txn.initiated_by,
				        txn.initiation_time,
				        txn.customer_transaction_status,
				        txn.authorization_status,
				        txn.bank_reference_id,
				        txn.resource_id,
				        txn.feature_id,
				        txn.company_id,
				        txn.company_group_id,
				        txn.CHANGE_TOKEN,
				        txni.transaction_currency,
				        txni.transaction_amount,
				        txni.customer_reference,
				        null as RECIPIENTS_REFERENCE,
				        txni.value_date,
				        txni.is_recurring,
				        txni.destination_country,
				        txni.destination_bank_name,
				        txni.fx_flag,
				        pty.party_name,
				        pty.party_account_number,
				        pty.party_account_currency,
				        pty.bank_code,
				        pty.swift_code,
				        pty.beneficiary_reference_id,
				        pty.party_type,
				        pty.residency_status,
				        pty.party_id,
				        pty.proxy_id_type,
				        pty.id_issuing_country,
				        ptyc.address_line_1,
				        ptyc.address_line_2,
				        ptyc.address_line_3,
				        ptyc.phone_country,
				        ptyc.phone_country_code,
				        ptyc.phone_no,
				        ptfc.fx_type,
                        ptfb.booking_ref_id,
                        ptfb.earmark_id,
				        txn.TOTAL_CHILD,
                        txn.HIGHEST_AMOUNT,
                        null AS UPLOAD_FILE_NAME
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
                LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id
				LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id
						   AND ptfc.bank_reference_id = txn.bank_reference_id
				LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id
			        WHERE 1=1
			        AND  txn.BATCH_ID is null
			        AND nvl(txn.application_type,'X') != 'terminateRecurring'
	  <#if filterParams.companyIdToStatusList?has_content && filterParams.companyIdToStatusList??>
                    AND (
        <#list filterParams.companyIdToStatusList?chunk(999) as chunk>
         (txn.RESOURCE_ID || '_' || txn.FEATURE_ID || '_' || txn.COMPANY_ID || '_' || txn.customer_transaction_status)
		                IN (
                <#list chunk as companyIdToStatusList>
                    <@p value=companyIdToStatusList/>
                    <#if companyIdToStatusList_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )
</#if>
    <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
     AND (
        <#list filterParams.singleAccountBasedOnResourceFeatureList?chunk(999) as chunk>
            (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
            IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )
</#if>
				<#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
                          AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>

                <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			 AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
       			</#if>
                <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
		             AND UPPER(txni.customer_reference) in (<#list filterParams.customerReferencesList as cusRef> UPPER(<@p value=cusRef/>)  <#if cusRef_has_next>,</#if> </#list>)

                </#if>
		<#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                      AND UPPER(txni.customer_reference) LIKE UPPER ('%${filterParams.customerRefIds}%')

                </#if>
                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		             AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.statusList?has_content && filterParams.statusList??>
		             AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.transactionFromAmount ??>
					and txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
	            </#if>
	            <#if filterParams.transactionToAmount ??>
					and txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
	            </#if>
		    <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.payerName?has_content && filterParams.payerName??>
		             AND UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%')

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
		             AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
		             AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
		             AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )
                 </#if>

                 <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
                <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		            AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                </#if>
		<#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                    AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
               </#if>
                <#if filterParams.bankEntityId ?? >
                          AND txn.BANK_ENTITY_ID = <@p name='filterParams.bankEntityId'/>
                 </#if>
                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
            <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			UNION ALL
			SELECT
                txn.bank_entity_id,
				txn.transaction_id,
				txn.account_number,
				txn.account_currency,
				txn.initiated_by,
				txn.initiation_time,
				txn.customer_transaction_status,
				txn.authorization_status,
				txn.bank_reference_id,
				txn.resource_id,
				txn.feature_id,
				txn.company_id,
				txn.company_group_id,
				txn.CHANGE_TOKEN,
				txn.TRANSACTION_CURRENCY,
				txn.TOTAL_AMOUNT as transaction_amount,
				null as customer_reference,
				btxn.RECIPIENTS_REFERENCE,
				btxn.TRANSFER_DATE as VALUE_DATE,
				null as is_recurring,
				null as destination_country,
				null as destination_bank_name,
				null as fx_flag,
				null as party_name,
				null as party_account_number,
				null as party_account_currency,
				null as bank_code,
				null as swift_code,
				null as beneficiary_reference_id,
				null as party_type,
				null as residency_status,
				null as party_id,
				null as proxy_id_type,
				null as id_issuing_country,
				null as address_line_1,
				null as address_line_2,
				null as address_line_3,
				null as phone_country,
				null as phone_country_code,
				null as phone_no,
				null as fx_type,
				null as booking_ref_id,
				null as earmark_id,
				txn.TOTAL_CHILD,
				txn.HIGHEST_AMOUNT,
				pfu.UPLOAD_FILE_NAME AS UPLOAD_FILE_NAME
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN PWS_FILE_UPLOAD pfu on btxn.file_upload_id = pfu.file_upload_id
			WHERE 1=1
			AND  txn.BATCH_ID is null
			AND nvl(txn.application_type,'X') != 'terminateRecurring'
			  <#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			 AND (
        <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
          (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		              IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    )
</#if>

                <#if filterParams.transactionId?has_content && filterParams.transactionId??>
		            AND txn.TRANSACTION_ID in ${filterParams.transactionId}
                </#if>

                 <#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
		             AND txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId}
                 </#if>
                  <#if filterParams.companyName?has_content && filterParams.companyName??>
		             AND txn.COMPANY_NAME in ${filterParams.companyName}
                  </#if>
                   <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
		              AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )
                   </#if>
		    <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
                    AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                        AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                    </#if>
                    <#if filterParams.statusList?has_content && filterParams.statusList??>
		               AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )
                    </#if>
                    <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
		                AND txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )
                    </#if>
 	                <#if filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount?has_content >
					  and txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	                </#if>
	                 <#if filterParams.multiBankReferenceIds?has_content && filterParams.multiBankReferenceIds??>
		                 AND txn.bank_reference_id in (<#list filterParams.multiBankReferenceIds as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                     </#if>
                     <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                        AND UPPER(btxn.RECIPIENTS_REFERENCE) IN (<#list filterParams.customerReferencesList as cusRef> UPPER(<@p value=cusRef/>)  <#if cusRef_has_next>,</#if> </#list>)
                     </#if>
                     <#if filterParams.customerRefIds?has_content && filterParams.customerRefIds??>
                      AND UPPER(btxn.RECIPIENTS_REFERENCE) LIKE UPPER ('%${filterParams.customerRefIds}%')
                     </#if>
                     <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                     </#if>
                     <#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
		                AND txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions}
                     </#if>
                     <#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
		                AND txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions}
                     </#if>
                     <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	       			 AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
       			     </#if>
                     <#if filterParams.bankEntityId ?? >
                          AND txn.BANK_ENTITY_ID = <@p name='filterParams.bankEntityId'/>
                 </#if>
                 </#if>
			) bothTrans
			 ORDER BY ${filterParams.sortFieldWithDirection}
		]]>
    </select>
    <select id="getV2BulkBothApprovalStatusTxnCount" resultMap="bulkApprovalStatusTxnCount"
            statementType="PREPARED">
        <![CDATA[
			SELECT
				COUNT(*) AS count
			FROM (
                <#if filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
                    SELECT DISTINCT
				        txn.transaction_id
			    FROM PWS_TRANSACTIONS txn
				INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id
				LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id
			        WHERE
            ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount1"></include>
        <![CDATA[
        AND nvl(txn.application_type,'X') != 'terminateRecurring'
        <#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction?? && filterParams.isSingleTransaction?has_content && filterParams.isSingleTransaction??>
			UNION ALL
			</#if>
			<#if filterParams.isBulkTransaction?has_content && filterParams.isBulkTransaction??>
			SELECT DISTINCT
				txn.transaction_id
			FROM PWS_TRANSACTIONS txn
			LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id
			LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID
            AND nvl(txn.application_type,'X') != 'terminateRecurring'
			WHERE
            ]]>
        <include refid="fragmentGetBulkBothApprovalStatusTxnCount2"></include>
        <![CDATA[
           </#if>
			) bothTrans
		]]>
    </select>


    <sql id="fragmentGetBulkBothApprovalStatusTxnCount1">
        <![CDATA[
        <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
         txn.company_group_id in ${filterParams.companyGroupId} AND
        </#if>
        <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
         txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
			  (
        <#list filterParams.accountNumberList?chunk(999) as chunk>
          ( txn.account_number)
		              IN (
                <#list chunk as accountNumber>
                    <@p value=accountNumber/>
                    <#if accountNumber_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>
        <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
         txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		    AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
        </#if>
        <#if filterParams.batchId?has_content>
             txn.batch_id = <@p name='filterParams.batchId'/> AND
         </#if>
<#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
         txni.customer_reference in (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
        </#if>

<#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
      (
        <#list filterParams.singleAccountBasedOnResourceFeatureList?chunk(999) as chunk>
            (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
            IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>

<#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
         txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND

        </#if>
<#if filterParams.statusList?has_content && filterParams.statusList??>
         txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND

        </#if>
<#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
         txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> and
        </#if>
<#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
         txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/> and
        </#if>
<#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
         txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/> and
        </#if>
<#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
         pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> ) AND

        </#if>
<#if filterParams.payerName?has_content && filterParams.payerName??>
		 UPPER(pty.party_name) LIKE UPPER ('%${filterParams.payerName}%') AND
        </#if>
<#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
         pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> ) AND

        </#if>
<#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
         pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> ) AND

        </#if>
<#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
         pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> ) AND
        </#if>
<#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
        </#if>
<#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
         txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
        </#if>
<#if filterParams.singleFeatureId?has_content && filterParams.singleFeatureId??>
         txn.FEATURE_ID in '${filterParams.singleFeatureId}' AND

        </#if>
<#if filterParams.singleResourceId?has_content && filterParams.singleResourceId??>
         txn.RESOURCE_ID in '${filterParams.singleResourceId}' AND
        </#if>

<#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	     UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
       	</#if>
<#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
         txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
        </#if>
        </#if>
         txn.customer_transaction_status NOT IN ('PENDING_SUBMIT','NEW','DELETED')

]]>
    </sql>

    <sql id="fragmentGetBulkBothApprovalStatusTxnCount2">
        <![CDATA[
        <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
         txn.company_group_id in ${filterParams.companyGroupId} AND
        </#if>
<#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
         txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> ) AND
        </#if>
<#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
         txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.transactionId?has_content && filterParams.transactionId??>
         txn.TRANSACTION_ID in ${filterParams.transactionId} AND
    </#if>
    <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
    <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
     txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>) AND
        </#if>
<#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
         txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>) AND
        </#if>
        </#if>
<#if filterParams.excludeTransactionId?has_content && filterParams.excludeTransactionId??>
         txn.TRANSACTION_ID  not in ${filterParams.excludeTransactionId} AND
        </#if>
<#if filterParams.bulkAccountBasedOnResourceFeatureList?has_content && filterParams.bulkAccountBasedOnResourceFeatureList??>
			  (
        <#list filterParams.bulkAccountBasedOnResourceFeatureList?chunk(999) as chunk>
          (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		              IN (
                <#list chunk as companyAccountGroup>
                    <@p value=companyAccountGroup/>
                    <#if companyAccountGroup_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
    ) AND
</#if>
<#if filterParams.companyName?has_content && filterParams.companyName??>
         txn.COMPANY_NAME in ${filterParams.companyName} AND
        </#if>

<#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
         txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> ) AND
        </#if>
<#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
         trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}' AND
        </#if>
<#if filterParams.statusList?has_content && filterParams.statusList??>
         txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> ) AND
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	     AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
        </#if>
        <#if filterParams.batchId?has_content>
          txn.batch_id = <@p name='filterParams.batchId'/> AND
        </#if>
<#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
         txn.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> ) AND
        </#if>
<#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
         txn.TOTAL_AMOUNT between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/> AND
        </#if>
<#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
         txn.TOTAL_AMOUNT >= <@p name='filterParams.transactionFromAmount'/> AND
        </#if>
<#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
         txn.TOTAL_AMOUNT <=  <@p name='filterParams.transactionToAmount'/> AND
        </#if>
<#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
         txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> ) AND
        </#if>
<#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
         btxn.RECIPIENTS_REFERENCE IN (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>) AND
        </#if>
<#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
         trunc(btxn.TRANSFER_DATE) between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}' AND
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
		    AND UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%')
        </#if>
<#if filterParams.numberOfChildTransactions?has_content && filterParams.numberOfChildTransactions??>
         txn.TOTAL_CHILD = ${filterParams.numberOfChildTransactions} AND
        </#if>
<#if filterParams.maxNumberOfChildTransactions?has_content && filterParams.maxNumberOfChildTransactions??>
         txn.TOTAL_CHILD <= ${filterParams.maxNumberOfChildTransactions} AND
        </#if>
        <#if filterParams.bankRefIds?has_content && filterParams.bankRefIds??>
	     UPPER(txn.bank_reference_id) LIKE UPPER('%${filterParams.bankRefIds}%') AND
       	</#if>
       	 txn.customer_transaction_status NOT IN ('PENDING_SUBMIT', 'NEW','DELETED' )
        ]]>
    </sql>

    <sql id="fragmentGetApprovalStatusTxnCount">
        <![CDATA[
        <#if filterParams.bankReferencesList?has_content && filterParams.bankReferencesList??>
                          AND txn.bank_reference_id in (<#list filterParams.bankReferencesList as bankReferencesVal> <@p value=bankReferencesVal/> <#if bankReferencesVal_has_next>,</#if> </#list> )
                </#if>
                 <#if filterParams.isChannelAdmin?has_content && filterParams.isChannelAdmin??>
                     <#if filterParams.resourceIdList?has_content && filterParams.resourceIdList??>
                        AND txn.RESOURCE_ID in (<#list filterParams.resourceIdList as resourceIds> <@p value=resourceIds/> <#if resourceIds_has_next>,</#if> </#list>)
                     </#if>
                      <#if filterParams.featureIdList?has_content && filterParams.featureIdList??>
                        AND txn.FEATURE_ID in (<#list filterParams.featureIdList as featureIds> <@p value=featureIds/> <#if featureIds_has_next>,</#if> </#list>)
                      </#if>
                </#if>
                <#if filterParams.customerReferencesList?has_content && filterParams.customerReferencesList??>
                          AND txni.customer_reference in (<#list filterParams.customerReferencesList as cusRef> <@p value=cusRef/>  <#if cusRef_has_next>,</#if> </#list>)

                </#if>
                 <#if filterParams.singleAccountBasedOnResourceFeatureList?has_content && filterParams.singleAccountBasedOnResourceFeatureList??>
                       AND (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID)
		               in (<#list filterParams.singleAccountBasedOnResourceFeatureList as companyAccountGroup> <@p value=companyAccountGroup/> <#if companyAccountGroup_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.companyIdList?has_content && filterParams.companyIdList??>
                      AND txn.COMPANY_ID in (<#list filterParams.companyIdList as companyIds> <@p value=companyIds/> <#if companyIds_has_next>,</#if> </#list> )
                 </#if>
                 <#if filterParams.companyGroupId?has_content && filterParams.companyGroupId??>
		              AND txn.company_group_id in ${filterParams.companyGroupId}
                 </#if>
                <#if filterParams.accountNumberList?has_content && filterParams.accountNumberList??>
		              AND txn.account_number in (<#list filterParams.accountNumberList as accountNumbers> <@p value=accountNumbers/> <#if accountNumbers_has_next>,</#if> </#list> )
                </#if>

                <#if filterParams.transactionCurrencyList?has_content && filterParams.transactionCurrencyList?? >
                          AND txni.transaction_currency in (<#list filterParams.transactionCurrencyList as transactionCurrencies> <@p value=transactionCurrencies/> <#if transactionCurrencies_has_next>,</#if> </#list> )

                </#if>
               <#if filterParams.statusList?has_content && filterParams.statusList??>
                          AND txn.customer_transaction_status in (<#list filterParams.statusList as statusVal> <@p value=statusVal/> <#if statusVal_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content && filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content >
					and txni.transaction_amount between <@p name='filterParams.transactionFromAmount'/> and <@p name='filterParams.transactionToAmount'/>
	            </#if>
	              <#if filterParams.transactionFromAmount ?? && filterParams.transactionFromAmount?has_content>
					    and txni.transaction_amount >= <@p name='filterParams.transactionFromAmount'/>
			     </#if>
				 <#if filterParams.transactionToAmount ?? && filterParams.transactionToAmount?has_content>
					    and txni.transaction_amount <=  <@p name='filterParams.transactionToAmount'/>
				 </#if>
                 <#if filterParams.payerNameList?has_content && filterParams.payerNameList??>
                          AND pty.party_name in (<#list filterParams.payerNameList as payerNames> <@p value=payerNames/> <#if payerNames_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerAccountNumberList?has_content && filterParams.payerAccountNumberList??>
                          AND pty.party_account_number in (<#list filterParams.payerAccountNumberList as payerAccountNumbers> <@p value=payerAccountNumbers/> <#if payerAccountNumbers_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerBankCodeList?has_content && filterParams.payerBankCodeList??>
                          AND pty.bank_code in (<#list filterParams.payerBankCodeList as payerBankCodes> <@p value=payerBankCodes/> <#if payerBankCodes_has_next>,</#if> </#list> )

                </#if>
                 <#if filterParams.payerSwiftCodeList?has_content && filterParams.payerSwiftCodeList??>
                          AND pty.swift_code in (<#list filterParams.payerSwiftCodeList as payerSwiftCodes> <@p value=payerSwiftCodes/> <#if payerSwiftCodes_has_next>,</#if> </#list> )

                </#if>
                <#if filterParams.applicationFromDate?has_content && filterParams.applicationFromDate?? && filterParams.applicationToDate?has_content && filterParams.applicationToDate??>
                          AND trunc(txn.initiation_time) between '${filterParams.applicationFromDate}' and '${filterParams.applicationToDate}'
                </#if>
                 <#if filterParams.valueFromDate?has_content && filterParams.valueFromDate?? && filterParams.valueToDate?has_content && filterParams.valueToDate??>
                          AND txni.value_date between '${filterParams.valueFromDate}' and '${filterParams.valueToDate}'
                </#if>
               <#if filterParams.accountCurrencyList?has_content && filterParams.accountCurrencyList??>
                          AND txn.account_currency in (<#list filterParams.accountCurrencyList as accountCurrencys> <@p value=accountCurrencys/> <#if accountCurrencys_has_next>,</#if> </#list> )

                </#if>

        ]]>
    </sql>

    <select id="getTerminateRecurringTransactions"
            statementType="PREPARED" parameterType="list"
            resultMap="TerminateRecurringTrxsMap">
        <![CDATA[
		 		SELECT
				transaction_id,
				bank_reference_id,
				change_token,
				customer_transaction_status,
				initiation_time,
				processing_status_updated_date
			FROM
				pws_transactions
			WHERE

				<#if bankReferenceNumberList?has_content && bankReferenceNumberList??>
        <#list bankReferenceNumberList?chunk(999) as chunk>
          bank_reference_id
		              IN (
                <#list chunk as bankReferenceNumber>
                    <@p value=bankReferenceNumber/>
                    <#if bankReferenceNumber_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
</#if>
				AND bank_entity_id = <@p name='bankEntityId'/>
				AND application_type = 'terminateRecurring'
		]]>
    </select>

    <resultMap id="TerminateRecurringTrxsMap" type="com.uob.gwb.txn.domain.ApprovalStatusTxn">
        <id column="transaction_id" property="terminateRecurringTnxId"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="change_token" property="terminateChangeToken"/>
        <result column="customer_transaction_status" property="terminateTransactionStatus"/>
    </resultMap>

    <select id="getTerminateRecurringTransactionsForMyTask"
            statementType="PREPARED" parameterType="list"
            resultMap="TerminateRecurringTrxsMapMyTask">
        <![CDATA[
		 		SELECT
				transaction_id,
				bank_reference_id,
				change_token,
				customer_transaction_status,
				initiation_time,
				processing_status_updated_date
			FROM
				pws_transactions
			WHERE

				<#if bankReferenceNumberList?has_content && bankReferenceNumberList??>
        <#list bankReferenceNumberList?chunk(999) as chunk>
          bank_reference_id
		              IN (
                <#list chunk as bankReferenceNumber>
                    <@p value=bankReferenceNumber/>
                    <#if bankReferenceNumber_has_next>,</#if>
                </#list>
            )
            <#if chunk_has_next> OR </#if>
        </#list>
</#if>

				AND bank_entity_id = <@p name='bankEntityId'/>
				AND application_type = 'terminateRecurring'
		]]>
    </select>

    <resultMap id="TerminateRecurringTrxsMapMyTask" type="com.uob.gwb.txn.domain.MyTask">
        <id column="transaction_id" property="terminateRecurringTnxId"/>
        <result column="bank_reference_id" property="bankReferenceId"/>
        <result column="change_token" property="terminateChangeToken"/>
        <result column="customer_transaction_status" property="terminateTransactionStatus"/>
    </resultMap>


</mapper>
```
