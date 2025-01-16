```sq1
SELECT DISTINCT transaction_id, account_number, account_currency, initiation_time, customer_transaction_status, bank_reference_id, resource_id, feature_id, company_id, company_group_id, CHANGE_TOKEN, transaction_currency, TOTAL_AMOUNT, RECIPIENTS_REFERENCE, VALUE_DATE, original_uetr, is_recurring, destination_country, destination_bank_name, fx_flag, party_name, party_account_number, party_account_currency, bank_code, swift_code, beneficiary_reference_id, party_type, residency_status, party_id, proxy_id_type, id_issuing_country, address_line_1, address_line_2, address_line_3, phone_country, phone_country_code, phone_no, fx_type, booking_ref_id, earmark_id, TOTAL_CHILD, HIGHEST_AMOUNT, upload_file_name, correlation_Id, batch_id 
FROM ( 
	
	SELECT txn.transaction_id, txn.account_number, txn.account_currency, txn.initiation_time, txn.customer_transaction_status, txn.bank_reference_id, txn.resource_id, txn.feature_id, txn.company_id, txn.company_group_id, txn.CHANGE_TOKEN, txni.transaction_currency, txni.transaction_amount AS TOTAL_AMOUNT, txni.customer_reference AS RECIPIENTS_REFERENCE, txni.value_date as VALUE_DATE, txni.original_uetr, txni.is_recurring, txni.destination_country, txni.destination_bank_name, txni.fx_flag, pty.party_name, pty.party_account_number, pty.party_account_currency, pty.bank_code, pty.swift_code, pty.beneficiary_reference_id, pty.party_type, pty.residency_status, pty.party_id, pty.proxy_id_type, pty.id_issuing_country, ptyc.address_line_1, ptyc.address_line_2, ptyc.address_line_3, ptyc.phone_country, ptyc.phone_country_code, ptyc.phone_no, ptfc.fx_type, ptfb.booking_ref_id, ptfb.earmark_id, txn.TOTAL_CHILD, txn.HIGHEST_AMOUNT, null AS UPLOAD_FILE_NAME, null as correlation_Id, txn.batch_id 
	FROM PWS_TRANSACTIONS txn 
	INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id 
	LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id 
	LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id AND ptyc.PARTY_CONTACT_TYPE = ? 
	LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id AND ptfc.bank_reference_id = txn.bank_reference_id 
	LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id 
	WHERE 
	txn.company_group_id in 421 
	AND txn.COMPANY_ID in ( ? , ? , ? , ? , ? ) 
	AND ( ( txn.account_number) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) ) 
	
	AND ( (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) ) 
	
	AND trunc(txn.initiation_time) between '18-Nov-2024' and '16-Jan-2025' 
	AND txn.customer_transaction_status NOT IN ( 'PENDING_SUBMIT', 'NEW', 'DELETED' ) 
	AND nvl(txn.application_type,'X') != 'terminateRecurring' 
	
	UNION ALL 
	
	SELECT txn.transaction_id, txn.account_number, txn.account_currency, txn.initiation_time, txn.customer_transaction_status, txn.bank_reference_id, txn.resource_id, txn.feature_id, txn.company_id, txn.company_group_id, txn.CHANGE_TOKEN, txn.TRANSACTION_CURRENCY, txn.TOTAL_AMOUNT, btxn.RECIPIENTS_REFERENCE, btxn.TRANSFER_DATE as VALUE_DATE, btxni.original_uetr as original_uetr, null as is_recurring, null as destination_country, null as destination_bank_name, null as fx_flag, null as party_name, null as party_account_number, null as party_account_currency, null as bank_code, null as swift_code, null as beneficiary_reference_id, null as party_type, null as residency_status, null as party_id, null as proxy_id_type, null as id_issuing_country, null as address_line_1, null as address_line_2, null as address_line_3, null as phone_country, null as phone_country_code, null as phone_no, null as fx_type, null as booking_ref_id, null as earmark_id, txn.TOTAL_CHILD, txn.HIGHEST_AMOUNT, pfu.upload_file_name AS UPLOAD_FILE_NAME, null as correlation_Id, txn.batch_id 
	FROM PWS_TRANSACTIONS txn 
	LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id 
	LEFT JOIN PWS_BULK_TRANSACTION_INSTRUCTIONS btxni on txn.transaction_id = btxni.transaction_id 
	LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID 
	WHERE 
	txn.company_group_id in 421 
	AND txn.COMPANY_ID in ( ? , ? , ? , ? , ? ) 
	AND txn.account_number in ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) 
	
	AND ( (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) OR (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) ) 
	
	AND trunc(txn.initiation_time) between '18-Nov-2024' and '16-Jan-2025' 
	AND txn.customer_transaction_status NOT IN ( 'PENDING_SUBMIT', 'NEW', 'DELETED' ) 
	AND nvl(txn.application_type,'X') != 'terminateRecurring' 
	
	Union All 
	
	SELECT txn.transaction_id, txn.account_number, txn.account_currency, txn.initiation_time, rtxn.status AS customer_transaction_status, txn.bank_reference_id, txn.resource_id, txn.feature_id, txn.company_id, txn.company_group_id, rtxn.change_token AS CHANGE_TOKEN, txni.transaction_currency, txni.transaction_amount AS TOTAL_AMOUNT, txni.customer_reference AS RECIPIENTS_REFERENCE, rtxn.processing_date AS value_date, rtxn.original_uetr as original_uetr, txni.is_recurring, txni.destination_country, txni.destination_bank_name, txni.fx_flag, pty.party_name, pty.party_account_number, pty.party_account_currency, pty.bank_code, pty.swift_code, pty.beneficiary_reference_id, pty.party_type, pty.residency_status, pty.party_id, pty.proxy_id_type, pty.id_issuing_country, ptyc.address_line_1, ptyc.address_line_2, ptyc.address_line_3, ptyc. phone_country, ptyc.phone_country_code, ptyc.phone_no, ptfc.fx_type, ptfb.booking_ref_id, ptfb. earmark_id, txn.TOTAL_CHILD, txn.HIGHEST_AMOUNT, null as UPLOAD_FILE_NAME, rtxn.correlation_id AS correlation_Id, txn.batch_id 
	from pws_transaction_instructions txni 
	JOIN pws_recurring_transactions rtxn ON txni.transaction_id = rtxn.transaction_id 
	Join pws_transactions txn on txn.transaction_id = rtxn.transaction_id 
	LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id 
	LEFT JOIN PWS_PARTY_CONTACTS ptyc on pty.party_id = ptyc.party_id AND ptyc.PARTY_CONTACT_TYPE = ? 
	LEFT JOIN PWS_TRANSACTION_FX_CONTRACTS ptfc on ptfc.transaction_id = txni.transaction_id AND ptfc.bank_reference_id = txn.bank_reference_id 
	LEFT JOIN PWS_TRANSACTION_FX_BOOKINGS ptfb on ptfb.booking_id = ptfc.booking_id 
	WHERE txni.is_recurring = 'Y' AND txn.customer_transaction_status IN ('PROCESSING', 'SUCCESSFUL', 'REJECTED', 'TERMINATED') ) bothTrans 
	
	ORDER BY bothTrans.initiation_time DESC OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
```

```sql
SELECT COUNT(*) AS count FROM ( 
	
	SELECT DISTINCT txn.transaction_id FROM PWS_TRANSACTIONS txn 
	INNER JOIN PWS_TRANSACTION_INSTRUCTIONS txni on txn.transaction_id = txni.transaction_id 
	LEFT JOIN PWS_PARTIES pty on pty.transaction_id = txni.transaction_id 
	WHERE 
	txn.COMPANY_ID in ( ? , ? , ? , ? , ? ) 
	AND txn.company_group_id in 421 
	AND ( ( txn.account_number) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) ) 
	
	AND txn.COMPANY_ID in ( ? , ? , ? , ? , ? ) 
	AND txn.company_group_id in 421 
	AND txn.account_number in ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) 
	
	AND ( (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) ) 
	
	AND trunc(txn.initiation_time) between '18-Nov-2024' and '16-Jan-2025' 
	
	AND txn.FEATURE_ID in 'Single-Transaction' AND txn.customer_transaction_status NOT IN ('PENDING_SUBMIT','NEW','DELETED') AND nvl(txn.application_type,'X') != 'terminateRecurring' 
	
	UNION ALL 
	
	SELECT DISTINCT txn.transaction_id FROM PWS_TRANSACTIONS txn 
	LEFT JOIN PWS_BULK_TRANSACTIONS btxn on btxn.transaction_id = txn.transaction_id 
	LEFT JOIN pws_file_upload pfu on pfu.FILE_UPLOAD_ID = btxn.FILE_UPLOAD_ID AND nvl(txn.application_type,'X') != 'terminateRecurring' 
	WHERE 
	txn.company_group_id in 421 
	AND txn.COMPANY_ID in ( ? , ? , ? , ? , ? ) 
	AND txn.account_number in ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) 
	
	AND ( (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) OR (txn.company_group_id || '_' || txn.COMPANY_ID || '_' || txn.account_number || '_' || txn.RESOURCE_ID || '_' || txn.FEATURE_ID) IN ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? ) ) 
	
	AND trunc(txn.initiation_time) between '18-Nov-2024' and '16-Jan-2025' 
	AND txn.customer_transaction_status NOT IN ('PENDING_SUBMIT', 'NEW','DELETED' ) ) bothTrans
```

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionWorkflowService {
    private final TransactionWorkflowDAO transactionWorkflowDAO;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    // Constants
    private static final String YES = "Y";
    private static final String DOT_SEPARATOR = ".";
    private static final String BOTH_TRANS = "bothTrans";
    private static final String LOWER = "lower";
    private static final String OPEN_BRACE = "(";
    private static final String SINGLE_TRANSACTION = "Single-Transaction";
    private static final int QUERY_TIMEOUT_SECONDS = 7;
    private static final String SINGLE = "SINGLE";
    private static final String BULK = "BULK";
    private static final String BOTH = "BOTH";
    private static final String LIMIT = "limit";
    private static final String PAYEE_TAX = "PAYEE_TAX";

	

    @Value("${transaction.concurrent.enabled:true}")
    private boolean concurrentExecutionEnabled;

    /**
     * Processes both approval status transactions
     */
    protected List<ApprovalStatusTxn> getBothApprovalStatus(FilterParams filterParams) {
        if (!shouldProcessBothApprovalStatus(filterParams)) {
            return new ArrayList<>();
        }

        processSortField(filterParams);
        setTransactionTypes(filterParams);

        try {
            // Execute main transaction list query
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = executeAsync(
                () -> transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(filterParams)
            );

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            List<String> transIds = extractTransactionIds(approvalStatusList);

            // Execute count and FX contracts queries concurrently
            CompletableFuture<Integer> countFuture = executeAsync(
                () -> transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(filterParams)
            );

            CompletableFuture<Map<String, ApprovalStatusTxn>> fxContractsFuture = executeAsync(
                () -> fetchFxContracts(transIds)
            );

            // Wait for both futures
            int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Map<String, ApprovalStatusTxn> fxContractMap = fxContractsFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            updateApprovalStatusList(approvalStatusList, count, fxContractMap);

            return approvalStatusList;

        } catch (Exception e) {
            log.error("Error processing both approval status transactions", e);
            throw new TransactionProcessingException("Failed to process both approval status transactions", e);
        }
    }

    /**
     * Processes bulk approval status transactions
     */
    protected List<ApprovalStatusTxn> getBulkApprovalStatus(FilterParams filterParams) {
        if (!shouldProcessBulkApproval(filterParams)) {
            return new ArrayList<>();
        }

        return YES.equalsIgnoreCase(filterParams.getIsChild()) 
            ? processChildTransactions(filterParams)
            : processParentTransactions(filterParams);
    }

    // Private helper methods for getBothApprovalStatus
    private boolean shouldProcessBothApprovalStatus(FilterParams filterParams) {
        return YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
            || ObjectUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())
            || ObjectUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList());
    }

    private void processSortField(FilterParams filterParams) {
        String orderByWithDirection = filterParams.getSortFieldWithDirection();
        String orderBy = orderByWithDirection.substring(orderByWithDirection.indexOf(DOT_SEPARATOR) + 1);
        
        StringBuilder sortBuilder = new StringBuilder();
        if (orderByWithDirection.contains(LOWER)) {
            sortBuilder.append(LOWER).append(OPEN_BRACE);
        }
        sortBuilder.append(BOTH_TRANS).append(DOT_SEPARATOR).append(orderBy.trim());
        
        filterParams.setSortFieldWithDirection(sortBuilder.toString());
    }

    private void setTransactionTypes(FilterParams filterParams) {
        if (ObjectUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
            filterParams.setIsSingleTransaction(YES);
            filterParams.setSingleFeatureId(SINGLE_TRANSACTION);
        }
        if (ObjectUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
            filterParams.setIsBulkTransaction(YES);
        }
    }

    // Private helper methods for getBulkApprovalStatus
    private boolean shouldProcessBulkApproval(FilterParams filterParams) {
        return YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
            || CollectionUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList());
    }

    private List<ApprovalStatusTxn> processChildTransactions(FilterParams filterParams) {
        filterParams.setIsChildY(YES);
        
        try {
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = executeAsync(
                () -> transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams)
            );

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            int count = executeAsync(
                () -> transactionWorkflowDAO.getBulkApprovalStatusTxnCount(filterParams)
            ).get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Total number of transaction Count for Bulk Transaction {}", count);
            approvalStatusList.stream()
                .findFirst()
                .ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));

            return approvalStatusList;

        } catch (Exception e) {
            log.error("Error processing child transactions", e);
            throw new TransactionProcessingException("Failed to process child transactions", e);
        }
    }

    private List<ApprovalStatusTxn> processParentTransactions(FilterParams filterParams) {
        filterParams.setIsChildN(YES);
        
        try {
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = executeAsync(
                () -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams)
            );

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            List<String> transIds = extractTransactionIds(approvalStatusList);

            // Execute all queries concurrently
            CompletableFuture<Integer> countFuture = executeAsync(
                () -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams)
            );

            CompletableFuture<Map<String, List<PwsTransactionCharges>>> chargesFuture = executeAsync(
                () -> fetchChargesDetail(transIds)
            );

            CompletableFuture<Map<String, ApprovalStatusTxn>> fxContractsFuture = executeAsync(
                () -> fetchFxContracts(transIds)
            );

            // Wait for all futures
            int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Map<String, List<PwsTransactionCharges>> chargesMap = chargesFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Map<String, ApprovalStatusTxn> fxContractMap = fxContractsFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            updateParentTransactions(approvalStatusList, count, chargesMap, fxContractMap);

            return approvalStatusList;

        } catch (Exception e) {
            log.error("Error processing parent transactions", e);
            throw new TransactionProcessingException("Failed to process parent transactions", e);
        }
    }

    // Shared utility methods
    private <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        return concurrentExecutionEnabled
            ? CompletableFuture.supplyAsync(supplier, executorService)
            : CompletableFuture.completedFuture(supplier.get());
    }

    private List<String> extractTransactionIds(List<ApprovalStatusTxn> transactions) {
        return transactions.stream()
            .map(ApprovalStatusTxn::getTransactionId)
            .toList();
    }

    private Map<String, List<PwsTransactionCharges>> fetchChargesDetail(List<String> transIds) {
        return transactionWorkflowDAO.getChargesDetail(transIds).stream()
            .collect(Collectors.groupingBy(
                charge -> charge.getTransactionId().toString(),
                Collectors.mapping(
                    charge -> charge,
                    Collectors.toList()
                )
            ));
    }

    private Map<String, ApprovalStatusTxn> fetchFxContracts(List<String> transIds) {
        return transactionWorkflowDAO.getFxContracts(transIds)
            .stream()
            .collect(Collectors.toMap(
                ApprovalStatusTxn::getTransactionId,
                Function.identity(),
                (existing, replacement) -> existing
            ));
    }

    private void updateApprovalStatusList(
            List<ApprovalStatusTxn> approvalStatusList,
            int count,
            Map<String, ApprovalStatusTxn> fxContractMap) {
        
        BigDecimal countValue = BigDecimal.valueOf(count);
        approvalStatusList.forEach(approve -> {
            approve.setCount(countValue);
            ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
            if (fx != null) {
                approve.setFxType(fx.getFxType());
                approve.setFxFlag(fx.getFxFlag());
                approve.setBookingRefId(fx.getBookingRefId());
                approve.setEarmarkId(fx.getEarmarkId());
            }
        });
    }

    private void updateParentTransactions(
            List<ApprovalStatusTxn> approvalStatusList,
            int count,
            Map<String, List<PwsTransactionCharges>> chargesMap,
            Map<String, ApprovalStatusTxn> fxContractMap) {

        chargesMap.forEach((transId, charges) -> {
            BigDecimal totalAmount = charges.stream()
                .map(PwsTransactionCharges::getFeesAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            String transCurrency = charges.stream()
                .findFirst()
                .map(PwsTransactionCharges::getFeesCurrency)
                .orElse(null);

            ApprovalStatusTxn fx = fxContractMap.get(transId);

            approvalStatusList.stream()
                .filter(approve -> approve.getTransactionId().equalsIgnoreCase(transId))
                .forEach(approv -> {
                    approv.setFeesCurrency(transCurrency);
                    approv.setTotalFeeAmount(totalAmount);
                    if (fx != null) {
                        approv.setFxType(fx.getFxType());
                        approv.setFxFlag(fx.getFxFlag());
                        approv.setBookingRefId(fx.getBookingRefId());
                        approv.setEarmarkId(fx.getEarmarkId());
                    }
                });
        });

        approvalStatusList.stream()
            .findFirst()
            .ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));
    }

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

@Slf4j
public class TransactionProcessingException extends RuntimeException {
    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
     * Process approval status transactions based on transaction type
     */
    protected ApprovalStatusLookUpResp setApprovalStatusTxnList(
            String transactionType,
            FilterParams filterParams,
            TransactionsLookUpReq request,
            CompanyAndAccountsForUserResourceFeaturesResp response,
            String isChild) {
        
        try {
            List<ApprovalStatusTxn> approvalStatusTxnList = processTransactionsByType(
                transactionType, filterParams, request);

            if (ObjectUtils.isEmpty(approvalStatusTxnList)) {
                return null;
            }

            return setApprovalStatusLookUpResp(
                approvalStatusTxnList, 
                response, 
                transactionType, 
                isChild, 
                request
            );

        } catch (Exception e) {
            log.error("Error processing approval status transactions for type: {}", transactionType, e);
            throw new TransactionProcessingException(
                String.format("Failed to process approval status transactions for type: %s", transactionType), 
                e
            );
        }
    }

    private List<ApprovalStatusTxn> processTransactionsByType(
            String transactionType,
            FilterParams filterParams,
            TransactionsLookUpReq request) throws Exception {
        
        return switch(transactionType.toUpperCase()) {
            case SINGLE -> processSingleTransactions(filterParams);
            case BULK -> super.getBulkApprovalStatus(filterParams);
            case BOTH -> processBothTransactions(filterParams, request);
            default -> new ArrayList<>();
        };
    }

    private List<ApprovalStatusTxn> processSingleTransactions(FilterParams filterParams) {
        return transactionWorkflowV2DAO.getApprovalStatusTxnList(filterParams);
    }

    private List<ApprovalStatusTxn> processBothTransactions(
            FilterParams filterParams,
            TransactionsLookUpReq request) throws Exception {
        
        prepareBothTransactionsParams(filterParams, request);

        // Execute main transaction list query
        CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = executeAsync(
            () -> transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(filterParams, PAYEE_TAX)
        );

        List<ApprovalStatusTxn> approvalStatusTxnList = txnListFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        if (ObjectUtils.isEmpty(approvalStatusTxnList)) {
            return approvalStatusTxnList;
        }

        List<String> transIds = extractTransactionIds(approvalStatusTxnList);

        // Execute count and FX contracts queries concurrently
        CompletableFuture<Integer> countFuture = executeAsync(
            () -> transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(filterParams)
        );

        CompletableFuture<Map<String, ApprovalStatusTxn>> fxContractsFuture = executeAsync(
            () -> fetchFxContracts(transIds)
        );

        // Wait for both futures
        int count = countFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Map<String, ApprovalStatusTxn> fxContractMap = fxContractsFuture.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Update approval status list
        updateBothTransactions(approvalStatusTxnList, count, fxContractMap);

        return approvalStatusTxnList;
    }

    private void prepareBothTransactionsParams(FilterParams filterParams, TransactionsLookUpReq request) {
        // Set limit
        filterParams.setLimit(String.valueOf(request.getAdditionalProperties().get(LIMIT)));

        // Process sort field
        String orderByWithDirection = filterParams.getSortFieldWithDirection();
        String orderBy = orderByWithDirection.substring(orderByWithDirection.indexOf(DOT_SEPARATOR) + 1);

        StringBuilder sortBuilder = new StringBuilder();
        if (orderByWithDirection.contains(LOWER)) {
            sortBuilder.append(LOWER).append(OPEN_BRACE);
        }
        sortBuilder.append(BOTH_TRANS).append(DOT_SEPARATOR).append(orderBy.trim());
        filterParams.setSortFieldWithDirection(sortBuilder.toString());

        // Set transaction types
        if (ObjectUtils.isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
            filterParams.setSingleFeatureId(SINGLE_TRANSACTION);
            filterParams.setIsSingleTransaction(YES);
        }

        if (ObjectUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
            filterParams.setIsBulkTransaction(YES);
        }
    }

    private void updateBothTransactions(
            List<ApprovalStatusTxn> approvalStatusList,
            int count,
            Map<String, ApprovalStatusTxn> fxContractMap) {

        BigDecimal countValue = BigDecimal.valueOf(count);
        approvalStatusList.forEach(approve -> {
            ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
            if (fx != null) {
                approve.setFxType(fx.getFxType());
                approve.setFxFlag(fx.getFxFlag());
                approve.setEarmarkId(fx.getEarmarkId());
                approve.setBookingRefId(fx.getBookingRefId());
            }
            approve.setCount(countValue);
        });
    }
```

```java
@ExtendWith(MockitoExtension.class)
class TransactionWorkflowServiceTest {

    @Mock
    private TransactionWorkflowDAO transactionWorkflowDAO;
    
    @Mock
    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;

    @InjectMocks
    private TransactionWorkflowService service;

    private FilterParams filterParams;
    private ExecutorService executorService;
    private TransactionsLookUpReq request;
    private CompanyAndAccountsForUserResourceFeaturesResp response;

    @BeforeEach
    void setUp() {
        filterParams = new FilterParams();
        filterParams.setSortFieldWithDirection("bothTrans.orderBy");
        executorService = mock(ExecutorService.class);
        ReflectionTestUtils.setField(service, "executorService", executorService);
        ReflectionTestUtils.setField(service, "concurrentExecutionEnabled", true);

        request = new TransactionsLookUpReq();
        Map<String, Object> props = new HashMap<>();
        props.put("limit", "10");
        request.setAdditionalProperties(props);
        response = new CompanyAndAccountsForUserResourceFeaturesResp();
    }

    @Test
    @DisplayName("getBothApprovalStatus: Should return empty list when conditions not met")
    void getBothApprovalStatus_ShouldReturnEmptyListWhenConditionsNotMet() {
        filterParams.setIsChannelAdmin("N");

        List<ApprovalStatusTxn> result = service.getBothApprovalStatus(filterParams);

        assertThat(result).isEmpty();
        verifyNoInteractions(transactionWorkflowDAO);
    }

    @Test
    @DisplayName("getBothApprovalStatus: Should process transactions successfully")
    void getBothApprovalStatus_ShouldProcessTransactionsSuccessfully() {
        filterParams.setIsChannelAdmin("Y");
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        List<ApprovalStatusTxn> fxContracts = createSampleFxContracts();
        
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(any()))
            .thenReturn(txnList);
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(any()))
            .thenReturn(10);
        when(transactionWorkflowDAO.getFxContracts(any()))
            .thenReturn(fxContracts);

        List<ApprovalStatusTxn> result = service.getBothApprovalStatus(filterParams);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCount()).isEqualTo(BigDecimal.valueOf(10));
        verify(transactionWorkflowDAO).getBulkBothApprovalStatusTxnList(any());
        verify(transactionWorkflowDAO).getBulkBothApprovalStatusTxnCount(any());
        verify(transactionWorkflowDAO).getFxContracts(any());
    }

    @Test
    @DisplayName("getBothApprovalStatus: Should handle empty transaction list")
    void getBothApprovalStatus_ShouldHandleEmptyTransactionList() {
        filterParams.setIsChannelAdmin("Y");
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(any()))
            .thenReturn(Collections.emptyList());

        List<ApprovalStatusTxn> result = service.getBothApprovalStatus(filterParams);

        assertThat(result).isEmpty();
        verify(transactionWorkflowDAO, never()).getBulkBothApprovalStatusTxnCount(any());
        verify(transactionWorkflowDAO, never()).getFxContracts(any());
    }

    @Test
    @DisplayName("getBulkApprovalStatus: Should process child transactions successfully")
    void getBulkApprovalStatus_ShouldProcessChildTransactionsSuccessfully() {
        filterParams.setIsChannelAdmin("Y");
        filterParams.setIsChild("Y");
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
            .thenReturn(txnList);
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(any()))
            .thenReturn(10);

        List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCount()).isEqualTo(BigDecimal.valueOf(10));
        verify(transactionWorkflowDAO).getBulkApprovalStatusTxnList(any());
        verify(transactionWorkflowDAO).getBulkApprovalStatusTxnCount(any());
    }

    @Test
    @DisplayName("getBulkApprovalStatus: Should process parent transactions successfully")
    void getBulkApprovalStatus_ShouldProcessParentTransactionsSuccessfully() {
        filterParams.setIsChannelAdmin("Y");
        filterParams.setIsChild("N");
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        List<ApprovalStatusTxn> fxContracts = createSampleFxContracts();
        List<PwsTransactionCharges> charges = createSampleCharges();
        
        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
            .thenReturn(txnList);
        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
            .thenReturn(10);
        when(transactionWorkflowDAO.getFxContracts(any()))
            .thenReturn(fxContracts);
        when(transactionWorkflowDAO.getChargesDetail(any()))
            .thenReturn(charges);

        List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

        assertThat(result).hasSize(2);
        verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnList(any());
        verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnCount(any());
        verify(transactionWorkflowDAO).getFxContracts(any());
        verify(transactionWorkflowDAO).getChargesDetail(any());
    }

    @Test
    @DisplayName("setApprovalStatusTxnList: Should process SINGLE transactions")
    void setApprovalStatusTxnList_ShouldProcessSingleTransactions() {
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        when(transactionWorkflowV2DAO.getApprovalStatusTxnList(any()))
            .thenReturn(txnList);

        ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
            "SINGLE", filterParams, request, response, "N"
        );

        assertThat(result).isNotNull();
        verify(transactionWorkflowV2DAO).getApprovalStatusTxnList(any());
    }

    @Test
    @DisplayName("setApprovalStatusTxnList: Should process BOTH transactions")
    void setApprovalStatusTxnList_ShouldProcessBothTransactions() {
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        List<ApprovalStatusTxn> fxContracts = createSampleFxContracts();
        
        when(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(any(), eq("PAYEE_TAX")))
            .thenReturn(txnList);
        when(transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(any()))
            .thenReturn(10);
        when(transactionWorkflowV2DAO.getFxContracts(any()))
            .thenReturn(fxContracts);

        ApprovalStatusLookUpResp result = service.setApprovalStatusTxnList(
            "BOTH", filterParams, request, response, "N"
        );

        assertThat(result).isNotNull();
        verify(transactionWorkflowV2DAO).getBulkBothApprovalStatusTxnList(any(), eq("PAYEE_TAX"));
        verify(transactionWorkflowV2DAO).getV2BulkBothApprovalStatusTxnCount(any());
        verify(transactionWorkflowV2DAO).getFxContracts(any());
    }

    @Test
    @DisplayName("Should handle exception in getBothApprovalStatus")
    void shouldHandleExceptionInGetBothApprovalStatus() {
        filterParams.setIsChannelAdmin("Y");
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(any()))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> service.getBothApprovalStatus(filterParams))
            .isInstanceOf(TransactionProcessingException.class)
            .hasMessageContaining("Failed to process both approval status transactions");
    }

    @Test
    @DisplayName("Should handle exception in getBulkApprovalStatus for child transactions")
    void shouldHandleExceptionInGetBulkApprovalStatusForChild() {
        filterParams.setIsChannelAdmin("Y");
        filterParams.setIsChild("Y");
        when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
            .isInstanceOf(TransactionProcessingException.class)
            .hasMessageContaining("Failed to process child transactions");
    }

    @Test
    @DisplayName("Should handle exception in concurrent execution")
    void shouldHandleExceptionInConcurrentExecution() {
        filterParams.setIsChannelAdmin("Y");
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(any()))
            .thenReturn(txnList);
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(any()))
            .thenThrow(new RuntimeException("Timeout error"));

        assertThatThrownBy(() -> service.getBothApprovalStatus(filterParams))
            .isInstanceOf(TransactionProcessingException.class)
            .hasMessageContaining("Failed to process both approval status transactions");
    }

    @Test
    @DisplayName("Should handle null FX contracts")
    void shouldHandleNullFxContracts() {
        filterParams.setIsChannelAdmin("Y");
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnList(any()))
            .thenReturn(txnList);
        when(transactionWorkflowDAO.getBulkBothApprovalStatusTxnCount(any()))
            .thenReturn(10);
        when(transactionWorkflowDAO.getFxContracts(any()))
            .thenReturn(null);

        List<ApprovalStatusTxn> result = service.getBothApprovalStatus(filterParams);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFxType()).isNull();
    }

    @Test
    @DisplayName("Should handle empty charges list")
    void shouldHandleEmptyChargesList() {
        filterParams.setIsChannelAdmin("Y");
        filterParams.setIsChild("N");
        List<ApprovalStatusTxn> txnList = createSampleTransactionList();
        
        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
            .thenReturn(txnList);
        when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
            .thenReturn(10);
        when(transactionWorkflowDAO.getFxContracts(any()))
            .thenReturn(Collections.emptyList());
        when(transactionWorkflowDAO.getChargesDetail(any()))
            .thenReturn(Collections.emptyList());

        List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTotalFeeAmount()).isNull();
    }

    @Test
    @DisplayName("Should clean up resources properly")
    void shouldCleanUpResourcesProperly() {
        service.cleanup();
        verify(executorService).shutdown();
    }

    // Helper methods to create test data
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
        list.add(fx1);
        return list;
    }

    private List<PwsTransactionCharges> createSampleCharges() {
        List<PwsTransactionCharges> list = new ArrayList<>();
        PwsTransactionCharges charge1 = new PwsTransactionCharges();
        charge1.setTransactionId(1L);
        charge1.setFeesAmount(BigDecimal.TEN);
        charge1.setFeesCurrency("USD");
        list.add(charge1);
        return list;
    }
}
```

```java
Map<String, String> payrollAccounts = new HashMap<>();
List<String> payrollConfigValues = new ArrayList<>();

// Assuming payrollAccounts and payrollConfigValues are already populated and of the same size

Map<String, String> updatedPayrollAccounts = IntStream.range(0, payrollAccounts.size())
    .boxed()
    .collect(Collectors.toMap(
        i -> payrollConfigValues.get(i),
        i -> payrollAccounts.get(payrollAccounts.keySet().toArray(new String[0])[i]),
        (v1, v2) -> v1,
        LinkedHashMap::new
    ));

payrollAccounts = updatedPayrollAccounts;
```

