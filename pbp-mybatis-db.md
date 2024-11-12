# Oracle DB 

For the other optimizations (NOLOGGING, disabling indexes, changing system parameters), I would recommend:

-   Discuss with your database administrator and other application owners before implementing.
-   If possible, consider setting up a separate schema or database instance for your bulk operations.
-   If you must use these in a shared environment, implement them in a controlled maintenance window where other applications are not actively using the database.

-   session wise tuning

Disable logging during bulk insert:
Before the bulk insert: ALTER TABLE your_table NOLOGGING;
After the bulk insert: ALTER TABLE your_table LOGGING;

Increase SORT_AREA_SIZE (if your insert involves sorting): ALTER SYSTEM SET db_file_multiblock_read_count = 128 SCOPE=BOTH;

Consider ALTER SESSION SET OPTIMIZER_MODE=FIRST_ROWS_N for prioritizing the first set of rows returned, beneficial for large volume inserts.

-   system wise tuning

Disable indexes before insert and rebuild after:
ALTER INDEX your_index UNUSABLE;
-- Perform bulk insert
ALTER INDEX your_index REBUILD;

Consider ALTER SESSION SET OPTIMIZER_MODE=FIRST_ROWS_N for prioritizing the first set of rows returned, beneficial for large volume inserts.

Partitioning large tables can significantly enhance performance by reducing the amount of data scanned during inserts.

Ensure that the Oracle redo log files are sufficiently large and the database is configured to handle large transactions efficiently. If you're not in archivelog mode, large bulk operations will be faster. Disable logging during bulk insert:
Before the bulk insert: ALTER TABLE your_table NOLOGGING;
After the bulk insert: ALTER TABLE your_table LOGGING;

Use Oracle’s Resource Manager to allocate appropriate resources to the bulk insert process without affecting other applications.

Ensure that the temporary tablespaces have sufficient space and are optimized for large sort operations.

## plsql insert 

```sql
CREATE OR REPLACE TYPE your_table_row_type AS OBJECT (
    field1 VARCHAR2(100),
    field2 NUMBER,
    field3 DATE
    -- Other fields as necessary
);
/

CREATE OR REPLACE TYPE your_table_type AS TABLE OF your_table_row_type;
/

CREATE OR REPLACE PROCEDURE bulk_insert_your_data (
    p_data IN your_table_type -- This is a PL/SQL table type of your data objects
) AS
BEGIN
    -- Perform the bulk insert using the FORALL statement
    FORALL i IN p_data.FIRST .. p_data.LAST
        INSERT INTO your_table_name (
            column1, column2, column3 -- List all relevant columns
        ) VALUES (
            p_data(i).field1, p_data(i).field2, p_data(i).field3 -- Map to your data object fields
        );

    -- Optionally, handle exceptions, logging, or other business logic here
    COMMIT; -- Commit after bulk insert
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK; -- Rollback if any error occurs
        RAISE;    -- Rethrow the exception to the calling environment
END;
/
```

1. Performance
   PL/SQL Bulk Insert: Oracle's PL/SQL is highly optimized for bulk operations. The FORALL statement and the ability to work directly within the database engine allows for efficient handling of large datasets. It minimizes context switching between the application and the database, which can significantly reduce overhead and improve performance.
   MyBatis Batch Insert: While MyBatis supports batch processing, it operates on the application side, which may involve more overhead due to network round trips and context switching between the application and the database. Although MyBatis can be tuned for batch operations, it generally won't match the efficiency of a well-designed PL/SQL bulk insert, especially with very large datasets.
2. Scalability
   PL/SQL Bulk Insert: PL/SQL can handle very large volumes of data natively within the Oracle database. The Oracle database is designed to manage large transactions and can handle millions of records efficiently when using bulk operations.
   MyBatis Batch Insert: MyBatis can be configured to handle large volumes, but as the data size grows, it may struggle with memory consumption and network latency. This approach also increases the load on the application server.
3. Resource Management
   PL/SQL Bulk Insert: By offloading the bulk of the processing to the database, you reduce the memory and CPU load on your application server. Oracle's database engine is optimized to manage its own resources efficiently during bulk operations.
   MyBatis Batch Insert: Batch operations in MyBatis require careful management of resources like memory and database connections. If not configured properly, you may run into issues with memory consumption and connection pool exhaustion.
4. Error Handling
   PL/SQL Bulk Insert: Error handling in PL/SQL is more fine-grained and can be managed directly within the stored procedure. You can handle specific exceptions and manage transactions more effectively, ensuring that large operations can be rolled back or partially committed as needed.
   MyBatis Batch Insert: MyBatis does provide mechanisms for transaction management, but handling errors across large batches of data can be more complex, especially if you need to retry failed batches or handle partial failures.
5. Network Overhead
   PL/SQL Bulk Insert: Since the data is processed within the database, there is minimal network overhead. The bulk of the data transfer occurs once when sending the array of data to the database.
   MyBatis Batch Insert: Each batch of records requires a network round trip between the application and the database. This can become a bottleneck with very large datasets.

Conclusion

For very large datasets, particularly when dealing with more than 100K records, the Oracle PL/SQL bulk insert approach is generally more efficient, scalable, and easier to manage in terms of performance and resource utilization. It leverages Oracle's internal optimizations for handling large volumes of data, which is ideal for high-performance data insertion tasks.

# HikariCP properties:

maximum-pool-size: 20 is a good starting point for high-load scenarios. Adjust based on your server's capabilities.
minimum-idle: 10 keeps a good number of connections ready.
connection-timeout: 30000 ms (30 seconds) is usually sufficient.
idle-timeout: 600000 ms (10 minutes) before an idle connection is removed.
max-lifetime: 1800000 ms (30 minutes) maximum lifetime of a connection in the pool.

```text
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
# the maximum amount of time that a connection can sit idle in the pool before being eligible for eviction.
spring.datasource.hikari.idle-timeout=600000
# Connections will be retired after 30 minutes (1,800,000 ms), regardless of their activity. This is to avoid using stale connections that could have been closed by the database.
spring.datasource.hikari.max-lifetime=1800000

spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048

spring.datasource.hikari.data-source-properties.defaultExecuteBatch=100
spring.datasource.hikari.data-source-properties.defaultBatchValue=100
```

# Mybatis & JDBC 

-   We're using ExecutorType.BATCH to enable JDBC batching.
-   We flush and clear the session every BATCH_SIZE (1000 in this example) inserts to manage memory usage.
-   The @Transactional annotation ensures that the entire operation is wrapped in a database transaction.
-   Error handling is implemented to catch and log any issues during the insert process.
-    Use the APPEND hint in your INSERT statements:


# Pws Save

```java
@Service
@Slf4j
@Transactional
public class Pain001ServiceImpl implements Pain001Service {

    @Autowired 
    private PaymentDao paymentDao;
    
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    
    @Value("${jdbc.batch.size:1000}")
    private int jdbcBatchSize;

    private final RetryTemplate retryTemplate;

    @PostConstruct
    private void init() {
        this.retryTemplate = createRetryTemplate();
    }

    private void executeBatchInsertsInOrder(BatchCollections collections) {
        Map<String, Long> partyIdsByRef = new HashMap<>();
        
        try {
            // 1. Process bulk transaction instructions
            if (!collections.getInstructions().isEmpty()) {
                executeBatchInsert("insertBulkTransactionInstruction", 
                    collections.getInstructions(), 
                    null);
            }

            // 2. Process parties and store their IDs
            if (!collections.getParties().isEmpty()) {
                List<PwsParties> insertedParties = executeBatchInsertWithIds("insertParty", 
                    collections.getParties());
                
                // Store party IDs for contacts
                for (PwsParties party : insertedParties) {
                    partyIdsByRef.put(party.getBankReferenceId() + "_" + party.getPartyType(), 
                        party.getPartyId());
                }
            }

            // 3. Process contacts with party IDs
            for (Map.Entry<String, List<PwsPartyContracts>> entry : 
                    collections.getContactsByPartyType().entrySet()) {
                List<PwsPartyContracts> contacts = entry.getValue();
                setPartyIdsForContacts(contacts, partyIdsByRef);
                executeBatchInsert("insertPartyContact", contacts, null);
            }

            // 4. Process tax instructions
            if (!collections.getTaxInstructions().isEmpty()) {
                executeBatchInsert("insertTaxInstruction", 
                    collections.getTaxInstructions(), 
                    null);
            }

            // 5. Process transaction advices
            if (!collections.getAdvices().isEmpty()) {
                executeBatchInsert("insertTransactionAdvice", 
                    collections.getAdvices(), 
                    null);
            }

            // 6. Process transaction charges
            if (!collections.getCharges().isEmpty()) {
                executeBatchInsert("insertTransactionCharge", 
                    collections.getCharges(), 
                    null);
            }

        } catch (Exception e) {
            log.error("Failed to execute batch inserts", e);
            throw new PaymentProcessingException("Failed to execute batch inserts", e);
        }
    }

    /**
     * Generic batch insert method
     */
    private <T> void executeBatchInsert(String insertMethod, List<T> records, 
            Consumer<List<T>> postBatchProcessor) {
        retryTemplate.execute(context -> {
            SqlSession session = sqlSessionTemplate.getSqlSessionFactory()
                .openSession(ExecutorType.BATCH);
            
            try {
                PaymentDao mapper = session.getMapper(PaymentDao.class);
                int count = 0;
                List<T> currentBatch = new ArrayList<>();

                for (T record : records) {
                    // Use reflection to call the appropriate insert method
                    Method method = mapper.getClass().getMethod(insertMethod, record.getClass());
                    method.invoke(mapper, record);
                    currentBatch.add(record);
                    count++;
                    
                    if (count % jdbcBatchSize == 0) {
                        session.flushStatements();
                        if (postBatchProcessor != null) {
                            postBatchProcessor.accept(currentBatch);
                        }
                        currentBatch = new ArrayList<>();
                    }
                }
                
                // Process remaining records
                if (!currentBatch.isEmpty()) {
                    session.flushStatements();
                    if (postBatchProcessor != null) {
                        postBatchProcessor.accept(currentBatch);
                    }
                }
                
                session.commit();
                return null;
            } catch (Exception e) {
                session.rollback();
                throw new PaymentProcessingException("Batch insert failed", e);
            } finally {
                session.close();
            }
        });
    }

    /**
     * Generic batch insert method with ID return
     */
    private <T> List<T> executeBatchInsertWithIds(String insertMethod, List<T> records) {
        return retryTemplate.execute(context -> {
            SqlSession session = sqlSessionTemplate.getSqlSessionFactory()
                .openSession(ExecutorType.BATCH);
            List<T> processedRecords = new ArrayList<>();
            
            try {
                PaymentDao mapper = session.getMapper(PaymentDao.class);
                int count = 0;
                List<T> currentBatch = new ArrayList<>();

                for (T record : records) {
                    Method method = mapper.getClass().getMethod(insertMethod, record.getClass());
                    method.invoke(mapper, record);
                    currentBatch.add(record);
                    count++;
                    
                    if (count % jdbcBatchSize == 0) {
                        session.flushStatements();
                        processedRecords.addAll(currentBatch);
                        currentBatch = new ArrayList<>();
                    }
                }
                
                if (!currentBatch.isEmpty()) {
                    session.flushStatements();
                    processedRecords.addAll(currentBatch);
                }
                
                session.commit();
                return processedRecords;
            } catch (Exception e) {
                session.rollback();
                throw new PaymentProcessingException("Batch insert with IDs failed", e);
            } finally {
                session.close();
            }
        });
    }
}
```

# DAO 
```java
@Repository("pwsSaveDao")
public interface PwsSaveDao {

    // bank ref
    int getBankRefSequenceNum();
    int[] getBatchBankRefSequenceNum(int count);

    // bulk payment
    long insertPwsTransactions(@Param("pwsTransactions") PwsTransactions pwsTransactions);
    long insertPwsBulkTransactions(@Param("pwsBulkTransactions") PwsBulkTransactions pwsBulkTransactions);

    // child txn
    long insertPwsBulkTransactionInstructions(@Param("pwsBulkTransactionInstructions" pwsBulkTransactionInstructions);
    long insertPwsParties(@Param("pwsParties" pwsParties);
    void insertPwsPartyContacts(@Param("pwsPartyContacts" pwsPartyContacts);
    void insertPwsPartyBanks(@Param("pwsPartyBanks" pwsPartyBanks);
    void insertPwsTransactionAdvices(@Param("pwsTransactionAdvices" pwsTransactionAdvices);
    void insertPwsTaxInstructions(@Param("pwsTaxInstructions" pwsTaxInstructions);


    // batch insert child txns
    List<Long> batchInsertTxnInstructions(List<PwsBulkTransactionInstructions> txnInstructions);
    List<Long> batchInsertPwsParties(List<PwsParties> parties);
    void batchInsertPwsPartyContacts(List<PwsPartyContacts> partyContacts);
    void batchInsertPwsPartyBanks(List<PwsPartyBanks> partyBanks);
    void batchInsertPwsTransactionAdvices(List<PwsTransactionAdvices> advices);
    void batchInsertPwsTaxInstructions(List<PwsTaxInstructions> taxInstructions);

}
```

# Mapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.uob.gwb.pbp.dao.pws.PwsSaveDao">

    <!-- Insert PwsTransactions -->
    <insert id="insertPwsTransactions" parameterType="com.uob.gwb.pbp.po.PwsTransactions"
            statementType="PREPARED" useGeneratedKeys="true" keyProperty="transactionId" keyColumn="transaction_id">
        <![CDATA[
        INSERT INTO pws_transactions (
            bank_entity_id,
            resource_id,
            feature_id,
            correlation_id,
            bank_reference_id,
            application_type,
            account_currency,
            account_number,
            company_group_id,
            company_id,
            company_name,
            transaction_currency,
            total_amount,
            total_child,
            highest_amount,
            authorization_status,
            capture_status,
            customer_transaction_status,
            processing_status,
            reject_reason,
            initiated_by,
            initiation_time,
            released_by,
            release_date,
            change_token
        ) VALUES (
            #{bankEntityId},
            #{resourceId},
            #{featureId},
            #{correlationId},
            #{bankReferenceId},
            #{applicationType},
            #{accountCurrency},
            #{accountNumber},
            #{companyGroupId},
            #{companyId},
            #{companyName},
            #{transactionCurrency},
            #{totalAmount},
            #{totalChild},
            #{highestAmount},
            #{authorizationStatus},
            #{captureStatus},
            #{customerTransactionStatus},
            #{processingStatus},
            #{rejectReason},
            #{initiatedBy},
            #{initiationTime},
            #{releasedBy},
            #{releaseDate},
            #{changeToken}
        )
        ]]>
    </insert>

    <!-- Insert PwsBulkTransactions -->
    <insert id="insertPwsBulkTransactions" parameterType="com.uob.gwb.pbp.po.PwsBulkTransactions"
            statementType="PREPARED" useGeneratedKeys="true" keyProperty="bkTransactionId" keyColumn="bk_transaction_id">
        <![CDATA[
        INSERT INTO pws_bulk_transactions (
            transaction_id,
            file_upload_id,
            recipients_reference,
            recipients_description,
            fate_file_name,
            fate_file_path,
            combine_debit,
            status,
            change_token,
            error_detail,
            final_fate_updated_date,
            ack_file_path,
            ack_updated_date,
            transfer_date,
            user_comments,
            dmp_batch_number,
            reject_code,
            batch_booking,
            charge_options,
            payroll_options
        ) VALUES (
            #{transactionId},
            #{fileUploadId},
            #{recipientsReference},
            #{recipientsDescription},
            #{fateFileName},
            #{fateFilePath},
            #{combineDebit},
            #{status},
            #{changeToken},
            #{errorDetail},
            #{finalFateUpdatedDate},
            #{ackFilePath},
            #{ackUpdatedDate},
            #{transferDate},
            #{userComments},
            #{dmpBatchNumber},
            #{rejectCode},
            #{batchBooking},
            #{chargeOptions},
            #{payrollOptions}
        )
        ]]>
    </insert>

    <!-- Insert PwsBulkTransactionInstructions -->
    <insert id="insertPwsBulkTransactionInstructions" parameterType="com.uob.gwb.pbp.po.PwsBulkTransactionInstructions"
            statementType="PREPARED" useGeneratedKeys="true" keyProperty="childTransactionInstructionsId" keyColumn="child_transaction_instructions_id">
        <![CDATA[
        INSERT INTO pws_bulk_transaction_instructions (
            child_bank_reference_id,
            transaction_id,
            bank_reference_id,
            transaction_currency,
            transaction_amount,
            equivalent_currency,
            equivalent_amount,
            destination_country,
            destination_bank_name,
            fx_flag,
            charge_options,
            original_value_date,
            value_date,
            settlement_date,
            payment_code_id,
            payment_details,
            rail_code,
            is_recurring,
            is_pre_approved,
            customer_reference,
            customer_transaction_status,
            processing_status,
            reject_reason,
            remarks_for_approval,
            user_comments,
            duplication_flag,
            reject_code,
            transfer_speed,
            dmp_trans_ref,
            child_template_id,
            initiation_time
        ) VALUES (
            #{childBankReferenceId},
            #{transactionId},
            #{bankReferenceId},
            #{transactionCurrency},
            #{transactionAmount},
            #{equivalentCurrency},
            #{equivalentAmount},
            #{destinationCountry},
            #{destinationBankName},
            #{fxFlag},
            #{chargeOptions},
            #{originalValueDate},
            #{valueDate},
            #{settlementDate},
            #{paymentCodeId},
            #{paymentDetails},
            #{railCode},
            #{isRecurring},
            #{isPreApproved},
            #{customerReference},
            #{customerTransactionStatus},
            #{processingStatus},
            #{rejectReason},
            #{remarksForApproval},
            #{userComments},
            #{duplicationFlag},
            #{rejectCode},
            #{transferSpeed},
            #{dmpTransRef},
            #{childTemplateId},
            #{initiationTime}
        )
        ]]>
    </insert>

    <!-- Insert PwsParties -->
    <insert id="insertPwsParties" parameterType="com.uob.gwb.pbp.po.PwsParties"
            statementType="PREPARED" useGeneratedKeys="true" keyProperty="partyId" keyColumn="party_id">
        <![CDATA[
        INSERT INTO pws_parties (
            bank_entity_id,
            transaction_id,
            bank_reference_id,
            child_bank_reference_id,
            bank_code,
            bank_id,
            party_account_type,
            party_account_number,
            party_account_name,
            party_nick_name,
            party_masked_nick_name,
            party_account_currency,
            party_agent_bic,
            party_name,
            party_role,
            residential_status,
            proxy_id,
            proxy_id_type,
            id_issuing_country,
            product_type,
            primary_identification_type,
            primary_identification_value,
            secondary_identification_type,
            secondary_identification_value,
            registration_id,
            beneficiary_reference_id,
            swift_code,
            party_type,
            residency_status,
            account_ownership,
            relationship_type,
            ultimate_payee_country_code,
            ultimate_payee_name,
            ultimate_payer_name,
            is_preapproved,
            party_modified_date,
            beneficiary_change_token
        ) VALUES (
            #{bankEntityId},
            #{transactionId},
            #{bankReferenceId},
            #{childBankReferenceId},
            #{bankCode},
            #{bankId},
            #{partyAccountType},
            #{partyAccountNumber},
            #{partyAccountName},
            #{partyNickName},
            #{partyMaskedNickName},
            #{partyAccountCurrency},
            #{partyAgentBIC},
            #{partyName},
            #{partyRole},
            #{residentialStatus},
            #{proxyId},
            #{proxyIdType},
            #{idIssuingCountry},
            #{productType},
            #{primaryIdentificationType},
            #{primaryIdentificationValue},
            #{secondaryIdentificationType},
            #{secondaryIdentificationValue},
            #{registrationId},
            #{beneficiaryReferenceId},
            #{swiftCode},
            #{partyType},
            #{residencyStatus},
            #{accountOwnership},
            #{relationshipType},
            #{ultimatePayeeCountryCode},
            #{ultimatePayeeName},
            #{ultimatePayerName},
            #{isPreapproved},
            #{partyModifiedDate},
            #{beneficiaryChangeToken}
        )
        ]]>
    </insert>

    <!-- Insert PwsPartyContacts -->
    <insert id="insertPwsPartyContacts" parameterType="com.uob.gwb.pbp.po.PwsPartyContacts" statementType="PREPARED">
        <![CDATA[
        INSERT INTO pws_party_contacts (
            party_id,
            transaction_id,
            bank_entity_id,
            bank_reference_id,
            child_bank_reference_id,
            address1,
            address2,
            address3,
            address4,
            address5,
            phone_country,
            phone_no,
            phone_country_code,
            address_category,
            country,
            province,
            district_name,
            sub_district_name,
            street_name,
            town_name,
            postal_code,
            building_number,
            building_name,
            floor,
            unit_number,
            department,
            sub_department,
            party_contact_type
        ) VALUES (
            #{partyId},
            #{transactionId},
            #{bankEntityId},
            #{bankReferenceId},
            #{childBankReferenceId},
            #{address1},
            #{address2},
            #{address3},
            #{address4},
            #{address5},
            #{phoneCountry},
            #{phoneNo},
            #{phoneCountryCode},
            #{addressCategory},
            #{country},
            #{province},
            #{districtName},
            #{subDistrictName},
            #{streetName},
            #{townName},
            #{postalCode},
            #{buildingNumber},
            #{buildingName},
            #{floor},
            #{unitNumber},
            #{department},
            #{subDepartment},
            #{partyContactType}
        )
        ]]>
    </insert>

    <!-- Insert PwsTransactionAdvices -->
    <insert id="insertPwsTransactionAdvices" parameterType="com.uob.gwb.pbp.po.PwsTransactionAdvices" statementType="PREPARED">
        <![CDATA[
        INSERT INTO pws_transaction_advices (
            party_id,
            bank_entity_id,
            transaction_id,
            bank_reference_id,
            child_bank_reference_id,
            advice_id,
            party_name1,
            party_name2,
            party_fax_number,
            party_contact_number,
            reference_no,
            advise_message,
            delivery_method,
            delivery_address
        ) VALUES (
            #{partyId},
            #{bankEntityId},
            #{transactionId},
            #{bankReferenceId},
            #{childBankReferenceId},
            #{adviceId},
            #{partyName1},
            #{partyName2},
            #{partyFaxNumber},
            #{partyContactNumber},
            #{referenceNo},
            #{adviseMessage},
            #{deliveryMethod},
            #{deliveryAddress}
        )
        ]]>
    </insert>

    <!-- Insert PwsTaxInstructions -->
    <insert id="insertPwsTaxInstructions" parameterType="com.uob.gwb.pbp.po.PwsTaxInstructions" statementType="PREPARED">
        <![CDATA[
        INSERT INTO pws_tax_instructions (
            transaction_id,
            bank_reference_id,
            child_bank_reference_id,
            tax_payer_name,
            tax_payer_id,
            tax_payee_name,
            tax_payee_id,
            tax_name,
            tax_type,
            tax_payment_condition,
            tax_document_number,
            tax_sequence_number,
            taxable_amount,
            tax_amount,
            vat_amount,
            type_of_income,
            tax_rate_in_percentage,
            tax_description,
            tax_period_year,
            tax_period_month,
            tax_period_day,
            tax_subscription,
            tax_return_type,
            payer_vat_branch_code,
            payee_vat_branch_code,
            vat_branch_code
        ) VALUES (
            #{transactionId},
            #{bankReferenceId},
            #{childBankReferenceId},
            #{taxPayerName},
            #{taxPayerId},
            #{taxPayeeName},
            #{taxPayeeId},
            #{taxName},
            #{taxType},
            #{taxPaymentCondition},
            #{taxDocumentNumber},
            #{taxSequenceNumber},
            #{taxableAmount},
            #{taxAmount},
            #{vatAmount},
            #{typeOfIncome},
            #{taxRateInPercentage},
            #{taxDescription},
            #{taxPeriodYear},
            #{taxPeriodMonth},
            #{taxPeriodDay},
            #{taxSubscription},
            #{taxReturnType},
            #{payerVatBranchCode},
            #{payeeVatBranchCode},
            #{vatBranchCode}
        )
        ]]>
    </insert>

</mapper>
```

# mybatis config 
```java
@Configuration
public class MyBatisConfig {

    @Bean
    @Primary
    public SqlSessionFactory defaultSqlSessionFactory(@Qualifier("defaultDataSource") DataSource dataSource)
            throws Exception {
        return createSqlSessionFactory(dataSource, "classpath:mappers/default/**/*.xml");
    }

    @Bean
    public SqlSessionFactory paymentSaveSqlSessionFactory(@Qualifier("paymentSaveDataSource") DataSource dataSource)
            throws Exception {
        return createSqlSessionFactory(dataSource, "classpath:mappers/paymentSave/**/*.xml");
    }

    @Bean
    public SqlSessionFactory paymentLoadingSqlSessionFactory(
            @Qualifier("paymentLoadingDataSource") DataSource dataSource) throws Exception {
        return createSqlSessionFactory(dataSource, "classpath:mappers/paymentLoading/**/*.xml");
    }

    private SqlSessionFactory createSqlSessionFactory(DataSource dataSource, String mapperLocation) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocation));
        return sessionFactory.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate defaultSqlSessionTemplate(
            @Qualifier("defaultSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public SqlSessionTemplate paymentSaveSqlSessionTemplate(
            @Qualifier("paymentSaveSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public SqlSessionTemplate paymentLoadingSqlSessionTemplate(
            @Qualifier("paymentLoadingSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}

```
