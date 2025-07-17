
  ## Payment Status Update Flow & Table Update Logic

  Overview

  The updatePaymentStatus method processes pain002 messages and updates different database tables based on transaction type (single vs bulk) and status. Here's the detailed flow:

  Main Flow Decision Tree

```java
  @Transactional
  @Override
  public Pain002v12UniversalApplicationMessage1 updatePaymentStatus(
          Pain002v12UniversalApplicationMessage1 pain002v12UniversalApplicationMessage1) {

      log.info("PaymentStatusServiceImpl updatePaymentStatus START");

      // STEP 1: Extract message components and metadata
      String uuid = pain002v12UniversalApplicationMessage1.getAdditionalPropValue(UUID);
      String messageContent = JsonUtil.toJsonString(pain002v12UniversalApplicationMessage1);

      // Extract group-level information (bulk transaction parent info)
      OriginalGroupHeader17 groupInformationStatus = getGroupInformation(pain002v12UniversalApplicationMessage1);

      // Extract payment instruction-level information (bulk instruction details)
      OriginalPaymentInstruction40 paymentInformationStatus = getPaymentTransactionInfo(pain002v12UniversalApplicationMessage1);

      // Extract report indicator (REPT = report only, not actual status update)
      String reportIndicator = getIndicatorDetails(pain002v12UniversalApplicationMessage1, reportIndicator);

      // STEP 2: Decision tree based on message structure and status

      // SCENARIO 1: Group-level bulk status update (REJECTED or PARTIAL at group level)
      // Updates: PWS_TRANSACTIONS, PWS_BULK_TRANSACTIONS, PWS_BULK_TRANSACTION_INSTRUCTIONS
      if (isGroupStatusHasRJCTSAndPARTtatus(groupInformationStatus)) {
          updateBulkGroupLevelStatus(groupInformationStatus, messageContent, reportIndicator,
                  paymentInformationStatus, uuid);
          log.info("updateBulkGroupLevelStatus GroupStatus: {} ", groupInformationStatus.getGroupStatus());
      }
      // SCENARIO 2: Payment instruction-level bulk status update
      // Updates: PWS_TRANSACTIONS, PWS_BULK_TRANSACTION_INSTRUCTIONS, PWS_TRANSACTION_FX_CONTRACTS
      else if (Objects.nonNull(paymentInformationStatus)
              && isNotNullofPaymentInformationStatus(groupInformationStatus, paymentInformationStatus)) {
          updateBulkLevelStatus(paymentInformationStatus, messageContent,
                  groupInformationStatus.getOriginalMessageIdentification(), reportIndicator, uuid);
          log.info("updateBulkLevelStatus BulkStatus: {} ", paymentInformationStatus.getPaymentInformationStatus());
      }
      // SCENARIO 3: Report-only update (REPT indicator with successful status)
      // Updates: PWS_TRANSACTIONS (report indicator only), PWS_TRANSIT_MESSAGE
      else if (Objects.nonNull(groupInformationStatus) && isIndicatorREPTAndStatusSuccess(reportIndicator,
              groupInformationStatus.getOriginalMessageIdentification())) {
          String bankReferenceId = groupInformationStatus.getOriginalMessageIdentification();
          updateReportIndicator(bankReferenceId, reportIndicator);
          persistBulkTransitMessage(bankReferenceId, bankReferenceId, messageContent, INBOUND_SERVICE_TYPE);
      }
      // SCENARIO 4: Individual transaction-level status updates (single or bulk children)
      // Updates: PWS_TRANSACTIONS, PWS_TRANSACTION_INSTRUCTIONS, PWS_BULK_TRANSACTION_INSTRUCTIONS
      else {
          updateSingleAndBulkChildTransactionStatus(pain002v12UniversalApplicationMessage1, uuid, messageContent,
                  reportIndicator);
      }

      // STEP 3: Generate output files for successful transactions
      createFateJsonAndDoneFile(pain002v12UniversalApplicationMessage1);

      log.info("PaymentStatusServiceImpl updatePaymentStatus END");
      return null;
  }
```

  Database Tables Updated by Scenario

  Tables Used:

  1. PWS_TRANSACTIONS - Main transaction table (parent for bulk, single for individual)
  2. PWS_TRANSACTION_INSTRUCTIONS - Single transaction instruction details
  3. PWS_BULK_TRANSACTION_INSTRUCTIONS - Bulk transaction child instruction details
  4. PWS_BULK_TRANSACTIONS - Bulk transaction metadata
  5. PWS_TRANSACTION_FX_CONTRACTS - Foreign exchange contract details
  6. PWS_TRANSIT_MESSAGE - Message persistence for downstream systems

  Update Logic by Status:

```java
  /**
   * Core transaction status update logic
   * Updates different tables based on transaction type (single vs bulk) and status
   * 
   * @param pwsTransactionsReq Request object with new status
   * @param pwsTransactions Existing transaction from database
   * @param trackerDataList FX tracking data for successful transactions
   */
  private void updateTransactionStatus(PwsTransactions pwsTransactionsReq, PwsTransactions pwsTransactions,
          TrackerData1 trackerDataList) {

      String transactionStatus = pwsTransactionsReq.getCustomerStatus();
      String instructionIdentification = pwsTransactionsReq.getBankReferenceId();
      String reasonCodes = pwsTransactionsReq.getRejectCode();
      String uuid = pwsTransactionsReq.getUuidValue();
      String messageContent = pwsTransactionsReq.getMessageContent();
      String rejectReasons = pwsTransactionsReq.getRejectReason();

      switch (transactionStatus) {
          // STATUS: ACCP (Accepted Customer Profile) - Initial acceptance
          case ACCP_STATUS:
              // SINGLE TRANSACTION: Only persist message for downstream processing
              if (isFeatureIdSingleTransactionValue.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistTransitMessage(instructionIdentification, uuid, messageContent, INBOUND_SERVICE_TYPE);
              }
              // BULK TRANSACTION: Persist bulk message for downstream processing
              else if (isFeatureIdBulkOnlineOrBulkUpload.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistBulkTransitMessage(pwsTransactions.getBankReferenceId(),
                          pwsTransactions.getBankReferenceId(), messageContent, INBOUND_SERVICE_TYPE);
              }
              break;

          // STATUS: RJCT (Rejected) - Transaction rejected
          case RJCT_STATUS:
              if (StringUtils.isEmpty(reasonCodes)) {
                  throw new ApplicationException(INVALID_REQUEST_DATA);
              }

              // SINGLE TRANSACTION REJECTION
              if (isFeatureIdSingleTransactionValue.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_TRANSACTIONS (set all statuses to REJECTED)
                  pwsTransactions.setCustomerTransactionStatus(REJECTED_STATUS);
                  pwsTransactions.setProcessingStatus(REJECTED_STATUS);
                  pwsTransactions.setCaptureStatus(REJECTED_STATUS);
                  getTransactionStatusDao.updatePWSTransactions(pwsTransactions);

                  // Table Updated: PWS_TRANSACTION_INSTRUCTIONS (add rejection details)
                  PwsTransactionInstructions pwsTransactionInstructions = new PwsTransactionInstructions();
                  pwsTransactionInstructions.setTransactionId(pwsTransactions.getTransactionId());
                  pwsTransactionInstructions.setCustomerTransactionStatus(REJECTED_STATUS);
                  pwsTransactionInstructions.setProcessingStatus(REJECTED_STATUS);
                  setRejectCodeAndReason(reasonCodes, rejectReasons, pwsTransactionInstructions);
                  pwsTransactionInstructions.setBankReferenceId(pwsTransactions.getBankReferenceId());
                  getTransactionStatusDao.updatePWSTrnInstructions(pwsTransactionInstructions);

                  // Send notification to customer
                  NotificationMessage rejectNotificationMessage = getNotificationMessageRequest(pwsTransactions, getHeaders());
                  service.publishMessage(rejectNotificationMessage);

                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistTransitMessage(instructionIdentification, uuid, messageContent, INBOUND_SERVICE_TYPE);
              }
              // BULK TRANSACTION CHILD REJECTION
              else if (isFeatureIdBulkOnlineOrBulkUpload.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_BULK_TRANSACTION_INSTRUCTIONS (update child status)
                  PwsBulkTransactionInstructions pwsBulkTransactionInstructions = new PwsBulkTransactionInstructions();
                  pwsBulkTransactionInstructions.setTransactionId(pwsTransactions.getTransactionId());
                  setRejectCodeAndReasonForBulk(reasonCodes, rejectReasons, pwsBulkTransactionInstructions);
                  pwsBulkTransactionInstructions.setCustomerTransactionStatus(REJECTED_STATUS);
                  pwsBulkTransactionInstructions.setProcessingStatus(REJECTED_STATUS);
                  pwsBulkTransactionInstructions.setChildBankReferenceId(instructionIdentification);
                  getTransactionStatusDao.updatePWSBulkTrnInstructions(pwsBulkTransactionInstructions);

                  // Update parent transaction status based on all children statuses
                  // Table Updated: PWS_TRANSACTIONS (parent status calculation)
                  updateFinalBulkParentStatus(pwsTransactions);

                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistBulkTransitMessage(pwsTransactions.getBankReferenceId(),
                          pwsTransactions.getBankReferenceId(), messageContent, INBOUND_SERVICE_TYPE);
              }
              break;

          // STATUS: ACSP (Accepted Settlement in Process) - Payment successful
          case ACSP_STATUS:
              // SINGLE TRANSACTION SUCCESS
              if (isFeatureIdSingleTransactionValue.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_TRANSACTIONS (set all statuses to SUCCESSFUL)
                  pwsTransactions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
                  pwsTransactions.setProcessingStatus(SUCCESSFUL_STATUS);
                  pwsTransactions.setCaptureStatus(SUCCESSFUL_STATUS);
                  getTransactionStatusDao.updatePWSTransactions(pwsTransactions);

                  // Table Updated: PWS_TRANSACTION_FX_CONTRACTS (FX contract details)
                  updatePwsTrxFxContracts(pwsTransactions, trackerDataList);

                  // Send success notification
                  NotificationMessage successNotificationMessage = getNotificationMessageRequest(pwsTransactions, getHeaders());
                  service.publishMessage(successNotificationMessage);

                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistTransitMessage(instructionIdentification, uuid, messageContent, INBOUND_SERVICE_TYPE);
              }
              // BULK TRANSACTION CHILD SUCCESS
              else if (isFeatureIdBulkOnlineOrBulkUpload.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_BULK_TRANSACTION_INSTRUCTIONS (update child status)
                  PwsBulkTransactionInstructions pwsBulkTransactionInstructions = new PwsBulkTransactionInstructions();
                  pwsBulkTransactionInstructions.setTransactionId(pwsTransactions.getTransactionId());
                  pwsBulkTransactionInstructions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
                  pwsBulkTransactionInstructions.setProcessingStatus(SUCCESSFUL_STATUS);
                  pwsBulkTransactionInstructions.setChildBankReferenceId(instructionIdentification);
                  getTransactionStatusDao.updatePWSBulkTrnInstructions(pwsBulkTransactionInstructions);

                  // Table Updated: PWS_TRANSACTION_FX_CONTRACTS (FX contract for bulk child)
                  updatePwsTrxFxContractsForBulk(instructionIdentification, trackerDataList);

                  // Update parent transaction status based on all children statuses
                  // Table Updated: PWS_TRANSACTIONS (parent status calculation)
                  updateFinalBulkParentStatus(pwsTransactions);

                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistBulkTransitMessage(pwsTransactions.getBankReferenceId(),
                          pwsTransactions.getBankReferenceId(), messageContent, INBOUND_SERVICE_TYPE);
              }
              break;

          // STATUS: ACCC (Accepted Customer Credit) - Payment completed
          case ACCC_STATUS:
              // SINGLE TRANSACTION COMPLETION (only for single transactions)
              if (isFeatureIdSingleTransactionValue.test(pwsTransactions.getFeatureId())) {
                  // Table Updated: PWS_TRANSACTIONS (successful but capture status = SUBMITTED)
                  pwsTransactions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
                  pwsTransactions.setProcessingStatus(SUCCESSFUL_STATUS);
                  pwsTransactions.setCaptureStatus(SUBMITTED_STATUS); // Different from ACSP
                  getTransactionStatusDao.updatePWSTransactions(pwsTransactions);

                  // Table Updated: PWS_TRANSACTION_INSTRUCTIONS (completion details)
                  PwsTransactionInstructions pwsTransactionInstructions = new PwsTransactionInstructions();
                  pwsTransactionInstructions.setTransactionId(pwsTransactions.getTransactionId());
                  pwsTransactionInstructions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
                  pwsTransactionInstructions.setProcessingStatus(SUCCESSFUL_STATUS);
                  pwsTransactionInstructions.setBankReferenceId(pwsTransactions.getBankReferenceId());
                  getTransactionStatusDao.updatePWSTrnInstructions(pwsTransactionInstructions);

                  // Send completion notification
                  NotificationMessage successNotificationMessage = getNotificationMessageRequest(pwsTransactions, getHeaders());
                  service.publishMessage(successNotificationMessage);

                  // Table Updated: PWS_TRANSIT_MESSAGE
                  persistTransitMessage(instructionIdentification, uuid, messageContent, INBOUND_SERVICE_TYPE);
              }
              // Note: ACCC is not processed for bulk transactions
              break;

          default:
              throw new ApplicationException(INVALID_TRANSACTION_STATUS);
      }
  }

  /**
   * Updates parent bulk transaction status based on all children statuses
   * Logic: 
   * - All children successful → Parent = SUCCESSFUL
   * - All children rejected → Parent = REJECTED  
   * - Mixed results → Parent = PARTIAL_REJECTED
   * 
   * Tables Updated: PWS_TRANSACTIONS (parent record)
   */
  private void updateFinalBulkParentStatus(PwsTransactions pwsTransactions) {
      // Get all child transaction statuses
      List<String> childStatusList = getTransactionStatusDao.getChildStatus(pwsTransactions.getTransactionId());

      // Calculate final parent status based on children
      String finalStatusForParent = getFinalStatusForParent(childStatusList);

      if (Objects.nonNull(finalStatusForParent)) {
          log.info("updateFinalBulkParentStatus, Final Status of parent {}", finalStatusForParent);

          // Update parent transaction based on children outcomes
          if (finalStatusForParent.equals(REJECTED_STATUS)) {
              // All children failed
              pwsTransactions.setCustomerTransactionStatus(REJECTED_STATUS);
              pwsTransactions.setProcessingStatus(REJECTED_STATUS);
              pwsTransactions.setCaptureStatus(REJECTED_STATUS);
              getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
          } else if (finalStatusForParent.equals(SUCCESSFUL_STATUS)) {
              // All children succeeded
              pwsTransactions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
              pwsTransactions.setProcessingStatus(SUCCESSFUL_STATUS);
              pwsTransactions.setCaptureStatus(SUBMITTED_STATUS);
              getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
          } else if (finalStatusForParent.equals(PARTIAL_REJECTED_STATUS)) {
              // Mixed results: some succeeded, some failed
              pwsTransactions.setCustomerTransactionStatus(PARTIAL_REJECTED_STATUS);
              pwsTransactions.setProcessingStatus(PARTIAL_REJECTED_STATUS);
              pwsTransactions.setCaptureStatus(SUBMITTED_STATUS);
              getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
          }

          // Send bulk completion notification
          NotificationMessage notificationMessage = getNotificationMessageRequestForBulk(pwsTransactions);
          service.publishMessage(notificationMessage);
      } else {
          // No valid status combination found, update only report indicator
          PwsTransactions pwsTransactionsForIndicatior = new PwsTransactions();
          pwsTransactionsForIndicatior.setReportIndicator(pwsTransactions.getReportIndicator());
          pwsTransactionsForIndicatior.setTransactionId(pwsTransactions.getTransactionId());
          getTransactionStatusDao.updatePWSTransactions(pwsTransactionsForIndicatior);
      }
  }
```

  Summary of Table Updates by Status

  | Status | Transaction Type | Tables Updated                                                                                                  | Purpose                                |
  |--------|------------------|-----------------------------------------------------------------------------------------------------------------|----------------------------------------|
  | ACCP   | Single           | PWS_TRANSIT_MESSAGE                                                                                             | Message persistence                    |
  | ACCP   | Bulk             | PWS_TRANSIT_MESSAGE                                                                                             | Message persistence                    |
  | RJCT   | Single           | PWS_TRANSACTIONS, PWS_TRANSACTION_INSTRUCTIONS, PWS_TRANSIT_MESSAGE                                             | Status + rejection details             |
  | RJCT   | Bulk Child       | PWS_BULK_TRANSACTION_INSTRUCTIONS, PWS_TRANSACTIONS (parent), PWS_TRANSIT_MESSAGE                               | Child rejection + parent recalculation |
  | ACSP   | Single           | PWS_TRANSACTIONS, PWS_TRANSACTION_FX_CONTRACTS, PWS_TRANSIT_MESSAGE                                             | Success + FX details                   |
  | ACSP   | Bulk Child       | PWS_BULK_TRANSACTION_INSTRUCTIONS, PWS_TRANSACTION_FX_CONTRACTS, PWS_TRANSACTIONS (parent), PWS_TRANSIT_MESSAGE | Child success + parent recalculation   |
  | ACCC   | Single           | PWS_TRANSACTIONS, PWS_TRANSACTION_INSTRUCTIONS, PWS_TRANSIT_MESSAGE                                             | Completion details                     |
  | REPT   | Any              | PWS_TRANSACTIONS (report indicator only), PWS_TRANSIT_MESSAGE                                                   | Report-only update                     |
    
## Deadlock RCA 

During the performance testing (high concurrency), deadlock detect:

Scenario:

  - Bulk payment with 1000 children + 1 parent pain002 message
  - 1001 concurrent threads processing pain002 messages
  - Each child thread calls updateFinalBulkParentStatus() which updates the same parent record

  Critical Deadlock Points:

  1. Parent Table Lock Contention (PaymentStatusServiceImpl.java:654-664)
```java
  // DEADLOCK SCENARIO 1: Multiple children updating same parent simultaneously
  private void updateFinalBulkParentStatus(PwsTransactions pwsTransactions) {
      // Multiple threads execute this concurrently for same parent
      List<String> childStatusList = getTransactionStatusDao.getChildStatus(pwsTransactions.getTransactionId());

      if (finalStatusForParent.equals(REJECTED_STATUS)) {
          // DEADLOCK: Multiple threads try to update same parent row
          getTransactionStatusDao.updatePWSTransactions(pwsTransactions); // Line 654
      } else if (finalStatusForParent.equals(SUCCESSFUL_STATUS)) {
          // DEADLOCK: Multiple threads try to update same parent row  
          getTransactionStatusDao.updatePWSTransactions(pwsTransactions); // Line 659
      }
  }
```

  2. Transaction Boundary Issues
```java
  @Transactional  // Method-level transaction holds locks too long
  public Pain002v12UniversalApplicationMessage1 updatePaymentStatus(...) {
      // Long-running transaction with multiple DB operations
      // Each child update triggers parent update within same transaction
      updateFinalBulkParentStatus(pwsTransactions); // Line 532, 561
  }
```
  3. Lock Ordering Problem

  The code updates tables in inconsistent order:
  - Child Thread A: PWS_BULK_TRANSACTION_INSTRUCTIONS → PWS_TRANSACTIONS (parent)
  - Child Thread B: PWS_TRANSACTIONS (parent) → PWS_BULK_TRANSACTION_INSTRUCTIONS

  Database Lock Analysis:

  Oracle 19c Row-Level Locking:
  - PWS_TRANSACTIONS parent row gets locked by first child
  - Subsequent children wait for parent row lock
  - If parent transaction also tries to update children → DEADLOCK

  Lock Wait Chain:
  Thread 1: Holds lock on PWS_TRANSACTIONS(parent) → Waits for PWS_BULK_TRANSACTION_INSTRUCTIONS(child_1)
  Thread 2: Holds lock on PWS_BULK_TRANSACTION_INSTRUCTIONS(child_1) → Waits for PWS_TRANSACTIONS(parent)

  Solutions to Prevent Deadlocks

  Solution 1: Implement Optimistic Locking with Version Control
  Solution 2: Implement Queue-Based Parent Status Update
  Solution 3: Database-Level Parent Status Calculation
  Solution 4: Separate Transaction Boundaries
```java
  // Split the transaction to reduce lock time
  public Pain002v12UniversalApplicationMessage1 updatePaymentStatus(
          Pain002v12UniversalApplicationMessage1 pain002v12UniversalApplicationMessage1) {

      // Process child updates in separate transaction
      updateChildTransactionStatus(pain002v12UniversalApplicationMessage1);

      // Process parent updates in separate transaction with delay
      scheduleParentStatusUpdate(pain002v12UniversalApplicationMessage1);

      return null;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  private void updateChildTransactionStatus(Pain002v12UniversalApplicationMessage1 message) {
      // Child-specific updates only
      // Shorter transaction duration
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  private void scheduleParentStatusUpdate(Pain002v12UniversalApplicationMessage1 message) {
      // Parent updates with proper locking
      // Can be delayed/queued
  }
```
  Recommended Implementation Strategy

  Phase 1: Immediate Fix (Low Risk)

  1. Add optimistic locking with version control
  2. Implement retry logic for parent updates
  3. Add database connection pool monitoring

  Phase 2: Architecture Improvement (Medium Risk)

  1. Implement queue-based parent status updates
  2. Separate transaction boundaries
  3. Add async processing for non-critical operations

  Phase 3: Database Optimization (High Risk)

  1. Implement stored procedure for parent status calculation
  2. Add database-level concurrency controls
  3. Optimize table indexes for bulk operations

  Performance Monitoring

  // Add metrics to track deadlock occurrences
  @Component
  public class DeadlockMonitor {
      private final MeterRegistry meterRegistry;
      private final Counter deadlockCounter;

      public DeadlockMonitor(MeterRegistry meterRegistry) {
          this.meterRegistry = meterRegistry;
          this.deadlockCounter = Counter.builder("payment.status.deadlocks")
              .description("Number of deadlocks in payment status updates")
              .register(meterRegistry);
      }

      public void recordDeadlock(String scenario) {
          deadlockCounter.increment(Tags.of("scenario", scenario));
      }
  }



  ## The Fix: Application-Level Synchronization

Deadlock Problem Analysis

  Current Issue:
  // CURRENT PROBLEMATIC CODE (lines 645-678)
  private void updateFinalBulkParentStatus(PwsTransactions pwsTransactions) {
      // PROBLEM 1: Multiple threads access this method concurrently for the same parent transaction
      List<String> childStatusList = getTransactionStatusDao.getChildStatus(pwsTransactions.getTransactionId());

      // PROBLEM 2: Multiple threads try to update the same parent row simultaneously
      // Thread 1: Updates PWS_TRANSACTIONS row (transaction_id = 12345)
      // Thread 2: Updates PWS_TRANSACTIONS row (transaction_id = 12345) <- DEADLOCK!
      getTransactionStatusDao.updatePWSTransactions(pwsTransactions); // Lines 654, 659, 664, 673
  }

  Why Deadlock Occurs:
  1. Bulk payment with 1000 children → 1000 concurrent threads
  2. Each child completion → calls updateFinalBulkParentStatus()
  3. All threads target same parent record → Oracle row-level lock contention
  4. Result: ORA-00060 deadlock

  Here's the complete solution with detailed comments:

  Step 1: Add New Imports
```java
  // Add these imports to handle concurrency
  import java.util.concurrent.ConcurrentHashMap;
  import java.util.concurrent.locks.ReentrantLock;
  import org.springframework.dao.DeadlockLoserDataAccessException;
  import org.springframework.dao.CannotAcquireLockException;
```

  Step 2: Add Class-Level Fields
```java
  // Add these fields to PaymentStatusServiceImpl class
  // WHY: These provide thread-safe locks per transaction ID
  private static final ConcurrentHashMap<Long, ReentrantLock> parentUpdateLocks = new ConcurrentHashMap<>();
  private static final int MAX_RETRY_ATTEMPTS = 3;
  private static final int RETRY_DELAY_MS = 100;
```

  Step 3: Replace updateFinalBulkParentStatus Method
```java
  /**
   * FIXED VERSION: Thread-safe parent status update with deadlock prevention
   * 
   * HOW IT WORKS:
   * 1. Creates one lock per transaction ID (not global lock)
   * 2. Only one thread can update parent at a time per transaction
   * 3. Other threads wait their turn (no database deadlock)
   * 4. Includes retry logic for any remaining database issues
   * 5. Cleans up locks to prevent memory leaks
   */
  private void updateFinalBulkParentStatus(PwsTransactions pwsTransactions) {
      Long transactionId = pwsTransactions.getTransactionId();

      // WHY: Get or create a lock specific to this transaction ID
      // This ensures only one thread updates parent for transaction 12345,
      // while transaction 67890 can be updated by another thread simultaneously
      ReentrantLock lock = parentUpdateLocks.computeIfAbsent(transactionId, k -> new ReentrantLock());

      // WHY: Acquire lock before updating parent
      // This prevents multiple threads from updating same parent record
      lock.lock();
      try {
          // WHY: Call the actual update logic with retry mechanism
          updateFinalBulkParentStatusWithRetry(pwsTransactions);
      } finally {
          // WHY: Always release the lock, even if exception occurs
          lock.unlock();

          // WHY: Clean up locks to prevent memory leaks
          // Only remove if no other threads are waiting for this lock
          if (!lock.hasQueuedThreads()) {
              parentUpdateLocks.remove(transactionId);
          }
      }
  }
```

  Step 4: Add Retry Logic Method
```java
  /**
   * Helper method with retry logic for database deadlock handling
   * 
   * WHY THIS IS NEEDED:
   * - Even with application locks, database deadlocks can still occur
   * - Oracle might have internal locking conflicts
   * - Retry with exponential backoff gives database time to resolve conflicts
   */
  private void updateFinalBulkParentStatusWithRetry(PwsTransactions pwsTransactions) {
      int attempt = 0;

      while (attempt < MAX_RETRY_ATTEMPTS) {
          try {
              // WHY: Refresh child status on each attempt
              // Another thread might have updated children between attempts
              List<String> childStatusList = getTransactionStatusDao.getChildStatus(pwsTransactions.getTransactionId());
              String finalStatusForParent = getFinalStatusForParent(childStatusList);

              if (Objects.nonNull(finalStatusForParent)) {
                  log.info("updateFinalBulkParentStatus, Final Status of parent {} (attempt {})",
                          finalStatusForParent, attempt + 1);

                  // WHY: Separate method for cleaner code and better error handling
                  updateParentTransactionStatus(pwsTransactions, finalStatusForParent);

                  // WHY: Send notification after successful update
                  NotificationMessage notificationMessage = getNotificationMessageRequestForBulk(pwsTransactions);
                  service.publishMessage(notificationMessage);

                  log.info("Successfully updated parent transaction {} with status {}",
                          pwsTransactions.getTransactionId(), finalStatusForParent);
                  return; // SUCCESS: Exit retry loop

              } else {
                  // WHY: Handle edge case where no valid status combination found
                  handleInvalidStatusCombination(pwsTransactions);
                  return; // Exit retry loop
              }

          } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
              // WHY: Catch specific database deadlock exceptions
              attempt++;

              if (attempt >= MAX_RETRY_ATTEMPTS) {
                  log.error("Failed to update parent transaction {} after {} attempts due to database deadlock",
                           pwsTransactions.getTransactionId(), MAX_RETRY_ATTEMPTS, e);
                  throw new ApplicationException("Database deadlock: Unable to update parent transaction status after "
                                               + MAX_RETRY_ATTEMPTS + " attempts");
              }

              // WHY: Exponential backoff with jitter prevents thundering herd
              // attempt 1: ~100ms, attempt 2: ~200ms, attempt 3: ~400ms
              long delay = RETRY_DELAY_MS * (1L << attempt) + (long)(Math.random() * 50);
              log.warn("Database deadlock on attempt {} for transaction {}, retrying in {}ms",
                      attempt, pwsTransactions.getTransactionId(), delay);

              try {
                  Thread.sleep(delay);
              } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                  throw new ApplicationException("Thread interrupted during deadlock retry");
              }

          } catch (Exception e) {
              log.error("Unexpected error updating parent transaction {}",
                       pwsTransactions.getTransactionId(), e);
              throw new ApplicationException("Error updating parent transaction status", e);
          }
      }
  }
```

  Step 5: Add Helper Methods
```java
  /**
   * Helper method to update parent transaction status
   * WHY: Separates business logic from retry/locking logic
   */
  private void updateParentTransactionStatus(PwsTransactions pwsTransactions, String finalStatusForParent) {
      if (finalStatusForParent.equals(REJECTED_STATUS)) {
          pwsTransactions.setCustomerTransactionStatus(REJECTED_STATUS);
          pwsTransactions.setProcessingStatus(REJECTED_STATUS);
          pwsTransactions.setCaptureStatus(REJECTED_STATUS);
      } else if (finalStatusForParent.equals(SUCCESSFUL_STATUS)) {
          pwsTransactions.setCustomerTransactionStatus(SUCCESSFUL_STATUS);
          pwsTransactions.setProcessingStatus(SUCCESSFUL_STATUS);
          pwsTransactions.setCaptureStatus(SUBMITTED_STATUS);
      } else if (finalStatusForParent.equals(PARTIAL_REJECTED_STATUS)) {
          pwsTransactions.setCustomerTransactionStatus(PARTIAL_REJECTED_STATUS);
          pwsTransactions.setProcessingStatus(PARTIAL_REJECTED_STATUS);
          pwsTransactions.setCaptureStatus(SUBMITTED_STATUS);
      }

      // WHY: Single database update call (same as original)
      getTransactionStatusDao.updatePWSTransactions(pwsTransactions);
  }

  /**
   * Helper method to handle invalid status combinations
   * WHY: Separates error handling logic
   */
  private void handleInvalidStatusCombination(PwsTransactions pwsTransactions) {
      try {
          PwsTransactions pwsTransactionsForIndicator = new PwsTransactions();
          pwsTransactionsForIndicator.setReportIndicator(pwsTransactions.getReportIndicator());
          pwsTransactionsForIndicator.setTransactionId(pwsTransactions.getTransactionId());
          getTransactionStatusDao.updatePWSTransactions(pwsTransactionsForIndicator);

          log.error("updateFinalBulkParentStatus, no suitable combinations match for parent {} Status null",
                  pwsTransactions.getTransactionId());
      } catch (Exception e) {
          log.error("Failed to update report indicator for transaction {}",
                   pwsTransactions.getTransactionId(), e);
      }
  }
```


  ## How This Fixes the Deadlock

  Before (Problematic)

  Thread 1 (Child 1): updateFinalBulkParentStatus() → Lock PWS_TRANSACTIONS row
  Thread 2 (Child 2): updateFinalBulkParentStatus() → Wait for PWS_TRANSACTIONS row
  Thread 3 (Child 3): updateFinalBulkParentStatus() → Wait for PWS_TRANSACTIONS row
  ...
  Thread 1000 (Child 1000): updateFinalBulkParentStatus() → DEADLOCK!

  After (Fixed)

  Thread 1 (Child 1): Acquire app lock → Update database → Release app lock
  Thread 2 (Child 2): Wait for app lock → Update database → Release app lock
  Thread 3 (Child 3): Wait for app lock → Update database → Release app lock
  ...
  Thread 1000 (Child 1000): Wait for app lock → Update database → Success!

  Benefits of This Solution

  1. No Database Schema Changes - Uses existing MyBatis setup
  2. Thread-Safe - Application-level locking prevents concurrent updates
  3. Scalable - Lock per transaction ID (not global lock)
  4. Resilient - Retry logic handles any remaining database issues
  5. Memory Efficient - Automatic lock cleanup prevents memory leaks
  6. Production Ready - Proper exception handling and logging

  Performance Impact

  - Minimal - Only parent updates are serialized (child updates remain parallel)
  - Bulk with 1000 children - Still processes 1000 children concurrently
  - Parent update - Only 1 thread updates parent instead of 1000 threads fighting




    
