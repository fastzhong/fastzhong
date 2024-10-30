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

    private void setPartyIdsForContacts(List<PwsPartyContracts> contacts, 
            Map<String, Long> partyIdsByRef) {
        for (PwsPartyContracts contact : contacts) {
            String partyKey = contact.getBankReferenceId() + "_" + contact.getPartyType();
            Long partyId = partyIdsByRef.get(partyKey);
            if (partyId != null) {
                contact.setPartyId(partyId);
            } else {
                log.warn("No party ID found for contact: {}", partyKey);
            }
        }
    }
}
```
