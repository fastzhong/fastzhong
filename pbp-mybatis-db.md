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
