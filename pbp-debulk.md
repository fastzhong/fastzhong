```sql
SELECT bank_entity_id, transaction_id, account_number, account_currency,
       initiated_by, initiation_time, customer_transaction_status,
       authorization_status, bank_reference_id, resource_id,
       feature_id, company_id, company_group_id, change_token,
       transaction_currency, transaction_amount,
       customer_reference, value_date, is_recurring,
       destination_country, destination_bank_name, fx_flag,
       party_name, party_account_number, party_account_currency,
       bank_code, swift_code, beneficiary_reference_id,
       party_type, residency_status, party_id, proxy_id_type,
       id_issuing_country, address_line_1, address_line_2,
       address_line_3, phone_country, phone_country_code,
       phone_no, fx_type, booking_ref_id, earmark_id,
       total_child, highest_amount
FROM pws_transactions txn
WHERE EXISTS (
    SELECT 1 
    FROM (
        SELECT 'Smart-Same-Day_Bulk-File-Upload_21_PENDING_AUTHORIZATION' AS status FROM dual UNION ALL
        SELECT 'Smart-Same-Day_Single-Transaction_121_PENDING_REWORK' FROM dual UNION ALL
        SELECT 'Payroll-Promptpay-Next-Day_Bulk-Online-Transactions-Executive_121_PENDING_SEND' FROM dual UNION ALL
        SELECT 'Smart-Next-Day_Bulk-Online-Transactions_263_PENDING_SEND' FROM dual UNION ALL
        -- Add other statuses here up to 1000 or more as needed
        SELECT 'Inter-Account-Fund-Transfer_Single-Transaction_121_PENDING_SEND' FROM dual UNION ALL
        SELECT 'Promptpay-Next-Day_Bulk-File-Upload_81_PENDING_SEND' FROM dual
        -- Continue adding more statuses...
    ) AS status_list
    WHERE (txn.resource_id || '_' || txn.feature_id || '_' || txn.company_id || '_' || txn.customer_transaction_status) = status_list.status
)
AND txn.batch_id IS NULL 
AND NVL(txn.application_type,'X') != 'terminateRecurring';
```
