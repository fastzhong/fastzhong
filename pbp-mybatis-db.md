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
<!-- PwsSaveMapper.xml -->
<mapper namespace="com.example.dao.PwsSaveDao">

    <!-- Get Bank Reference Sequence Number -->
    <select id="getBankRefSequenceNum" resultType="int">
        SELECT SEQ_BANK_REF.NEXTVAL FROM DUAL
    </select>

    <select id="getBatchBankRefSequenceNum" resultType="int">
        SELECT SEQ_BANK_REF.NEXTVAL FROM DUAL CONNECT BY LEVEL &lt;= #{count}
    </select>

    <!-- Insert PwsTransactions -->
    <insert id="insertPwsTransactions" parameterType="com.example.model.PwsTransactions" useGeneratedKeys="true" keyProperty="transactionId">
        INSERT INTO PWS_TRANSACTIONS (
            ACCOUNT_CURRENCY, ACCOUNT_NUMBER, AUTHORIZATION_STATUS, CAPTURE_STATUS, COMPANY_GROUP_ID,
            COMPANY_ID, COMPANY_NAME, WIZARD, INITIATION_TIME, RELEASE_DATE, PROCESSING_STATUS, BANK_ENTITY_ID,
            BANK_REFERENCE_ID, RESOURCE_ID, CHANGE_TOKEN, INITIATED_BY, RELEASED_BY, MAXIMUM_AMOUNT, FEATURE_ID,
            APPLICATION_TYPE, CORRELATION_ID, CUSTOMER_TRANSACTION_STATUS, REJECT_REASON, TRANSACTION_CURRENCY,
            TRANSACTION_TOTAL_AMOUNT, TERMINATED_BY_DEBTOR_FLAG, HIGHEST_AMOUNT, ACCOUNT_PAB, TOTAL_CHILD, TOTAL_AMOUNT,
            ORIGINAL_TRANSACTION_ID, TRANSACTION_CATEGORY
        ) VALUES (
            #{accountCurrency}, #{accountNumber}, #{authorizationStatus}, #{captureStatus}, #{companyGroupId},
            #{companyId}, #{companyName}, #{wizard}, #{initiationTime}, #{releaseDate}, #{processingStatus}, #{bankEntityId},
            #{bankReferenceId}, #{resourceId}, #{changeToken}, #{initiatedBy}, #{releasedBy}, #{maximumAmount}, #{featureId},
            #{applicationType}, #{correlationId}, #{customerTransactionStatus}, #{rejectReason}, #{transactionCurrency},
            #{transactionTotalAmount}, #{terminatedByDebtorFlag}, #{highestAmount}, #{accountPAB}, #{totalChild}, #{totalAmount},
            #{originalTransactionId}, #{transactionCategory}
        )
    </insert>

    <!-- Insert PwsBulkTransactions -->
    <insert id="insertPwsBulkTransactions" parameterType="com.example.model.PwsBulkTransactions" useGeneratedKeys="true" keyProperty="bkTransactionId">
        INSERT INTO PWS_BULK_TRANSACTIONS (
            TRANSACTION_ID, FILE_UPLOAD_ID, RECIPIENTS_REFERENCE, RECIPIENTS_DESCRIPTION, FATE_FILE_NAME,
            FATE_FILE_PATH, COMBINE_DEBIT, STATUS, CHANGE_TOKEN, ERROR_DETAIL, FINAL_FATE_UPDATED_DATE, ACK_FILE_PATH,
            ACK_UPDATED_DATE, TRANSFER_DATE, USER_COMMENTS, DMP_BATCH_NUMBER, REJECT_CODE, BATCH_BOOKING,
            CHARGE_OPTIONS, PAYROLL_OPTIONS
        ) VALUES (
            #{transactionId}, #{fileUploadId}, #{recipientsReference}, #{recipientsDescription}, #{fateFileName},
            #{fateFilePath}, #{combineDebit}, #{status}, #{changeToken}, #{errorDetail}, #{finalFateUpdatedDate}, #{ackFilePath},
            #{ackUpdatedDate}, #{transferDate}, #{userComments}, #{dmpBatchNumber}, #{rejectCode}, #{batchBooking},
            #{chargeOptions}, #{payrollOptions}
        )
    </insert>

    <!-- Insert PwsBulkTransactionInstructions -->
    <insert id="insertPwsBulkTransactionInstructions" parameterType="com.example.model.PwsBulkTransactionInstructions" useGeneratedKeys="true" keyProperty="instructionId">
        INSERT INTO PWS_BULK_TRANSACTION_INSTRUCTIONS (
            /* Include fields here */
        ) VALUES (
            /* Include values here */
        )
    </insert>

    <!-- Insert PwsParties -->
    <insert id="insertPwsParties" parameterType="com.example.model.PwsParties" useGeneratedKeys="true" keyProperty="partyId">
        INSERT INTO PWS_PARTIES (
            BANK_ENTITY_ID, TRANSACTION_ID, BANK_REFERENCE_ID, CHILD_BANK_REFERENCE_ID, BANK_CODE, PARTY_ACCOUNT_TYPE,
            PARTY_ACCOUNT_NUMBER, PARTY_ACCOUNT_NAME, PARTY_ACCOUNT_CURRENCY, PARTY_AGENT_BIC, PARTY_NAME, PARTY_ROLE,
            RESIDENTIAL_STATUS, PROXY_ID, PROXY_ID_TYPE, ID_ISSUING_COUNTRY, PRODUCT_TYPE, PRIMARY_IDENTIFICATION_TYPE,
            PRIMARY_IDENTIFICATION_VALUE, SECONDARY_IDENTIFICATION_TYPE, SECONDARY_IDENTIFICATION_VALUE, REGISTRATION_ID,
            BENEFICIARY_REFERENCE_ID, SWIFT_CODE, PARTY_TYPE, RESIDENCY_STATUS, ACCOUNT_OWNERSHIP, RELATIONSHIP_TYPE,
            ULTIMATE_PAYEE_COUNTRY_CODE, ULTIMATE_PAYEE_NAME, PARTY_MODIFIED_DATE, BENEFICIARY_CHANGE_TOKEN, IS_NEW,
            IS_PREAPPROVED, BANK_ID
        ) VALUES (
            #{bankEntityId}, #{transactionId}, #{bankReferenceId}, #{childBankReferenceId}, #{bankCode}, #{partyAccountType},
            #{partyAccountNumber}, #{partyAccountName}, #{partyAccountCurrency}, #{partyAgentBIC}, #{partyName}, #{partyRole},
            #{residentialStatus}, #{proxyId}, #{proxyIdType}, #{idIssuingCountry}, #{productType}, #{primaryIdentificationType},
            #{primaryIdentificationValue}, #{secondaryIdentificationType}, #{secondaryIdentificationValue}, #{registrationId},
            #{beneficiaryReferenceId}, #{swiftCode}, #{partyType}, #{residencyStatus}, #{accountOwnership}, #{relationshipType},
            #{ultimatePayeeCountryCode}, #{ultimatePayeeName}, #{partyModifiedDate}, #{beneficiaryChangeToken}, #{isNew},
            #{isPreapproved}, #{bankId}
        )
    </insert>

    <!-- Insert PwsPartyContacts -->
    <insert id="insertPwsPartyContacts" parameterType="com.example.model.PwsPartyContacts" useGeneratedKeys="true" keyProperty="partyContactId">
        INSERT INTO PWS_PARTY_CONTACTS (
            PARTY_ID, BANK_ENTITY_ID, TRANSACTION_ID, BANK_REFERENCE_ID, CHILD_BANK_REFERENCE_ID, ADDRESS1, ADDRESS2,
            ADDRESS3, ADDRESS4, ADDRESS5, PHONE_COUNTRY, PHONE_NO, PHONE_COUNTRY_CODE, COUNTRY, PROVINCE, DISTRICT_NAME,
            SUB_DISTRICT_NAME, STREET_NAME, TOWN_NAME, POSTAL_CODE, BUILDING_NUMBER, BUILDING_NAME, FLOOR, UNIT_NUMBER,
            DEPARTMENT, IS_NEW, PARTY_CONTACT_TYPE
        ) VALUES (
            #{partyId}, #{bankEntityId}, #{transactionId}, #{bankReferenceId}, #{childBankReferenceId}, #{address1}, #{address2},
            #{address3}, #{address4}, #{address5}, #{phoneCountry}, #{phoneNo}, #{phoneCountryCode}, #{country}, #{province},
            #{districtName}, #{subDistrictName}, #{streetName}, #{townName}, #{postalCode}, #{buildingNumber}, #{buildingName},
            #{floor}, #{unitNumber}, #{department}, #{isNew}, #{partyContactType}
        )
    </insert>

    <!-- Insert PwsPartyBanks -->
    <insert id="insertPwsPartyBanks" parameterType="com.example.model.PwsPartyBanks" useGeneratedKeys="true" keyProperty="bankId">
        INSERT INTO PWS_PARTY_BANKS (
            PARTY_ID, TRANSACTION_ID, BANK_TYPE, IS_NEW, CLEARING_CODE_ID, CLEARING_CODE, CLEARING_CODE_DESC, BANK_CODE,
            BANK_SWIFT_CODE, BANK_NAME, BANK_ADDRESS1, BANK_ADDRESS2, BANK_ADDRESS3, BANK_TOWN, BANK_COUNTRY_CODE,
            BANK_COUNTRY_NAME, BRANCH_NAME_ADDRESS1, BRANCH_NAME_ADDRESS2, BRANCH_NAME_ADDRESS3, BANK_COUNTRY
        ) VALUES (
            #{partyId}, #{transactionId}, #{bankType}, #{isNew}, #{clearingCodeId}, #{clearingCode}, #{clearingCodeDesc}, #{bankCode},
            #{bankSwiftCode}, #{bankName}, #{bankAddress1}, #{bankAddress2}, #{bankAddress3}, #{bankTown}, #{bankCountryCode},
            #{bankCountryName}, #{branchNameAddress1}, #{branchNameAddress2}, #{branchNameAddress3}, #{bankCountry}
        )
    </insert>

    <!-- Insert PwsTransactionAdvices -->
    <insert id="insertPwsTransactionAdvices" parameterType="com.example.model.PwsTransactionAdvices" useGeneratedKeys="true" keyProperty="adviceId">
        INSERT INTO PWS_TRANSACTION_ADVICES (
            BANK_ENTITY_ID, TRANSACTION_ID, BANK_REFERENCE_ID, CHILD_BANK_REFERENCE_ID, PARTY_ID, ADVICE_ID, PARTY_NAME1,
            PARTY_NAME2, REFERENCE_NO, ADVISE_MESSAGE, DELIVERY_METHOD, DELIVERY_ADDRESS
        ) VALUES (
            #{bankEntityId}, #{transactionId}, #{bankReferenceId}, #{childBankReferenceId}, #{partyId}, #{adviceId},
            #{partyName1}, #{partyName2}, #{referenceNo}, #{adviseMessage}, #{deliveryMethod}, #{deliveryAddress}
        )
    </insert>

    <!-- Insert PwsTaxInstructions -->
    <insert id="insertPwsTaxInstructions" parameterType="com.example.model.PwsTaxInstructions" useGeneratedKeys="true" keyProperty="instructionId">
        INSERT INTO PWS_TAX_INSTRUCTIONS (
            BANK_REFERENCE_ID, CHILD_BANK_REFERENCE

```
