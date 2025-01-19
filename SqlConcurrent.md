```java
@Slf4j
@Service("ApprovalStatusService")
@RequiredArgsConstructor
public class ApprovalStatusServiceImpl implements ApprovalStatusService {
    
    private final TransactionWorkflowDAO transactionWorkflowDAO;
    // ... other dependencies remain the same ...

    // Create a dedicated thread pool for concurrent execution
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

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

    protected List<ApprovalStatusTxn> getBulkApprovalStatus(FilterParams filterParams) {
        if (!shouldProcessBulkApproval(filterParams)) {
            return new ArrayList<>();
        }

        return YES.equalsIgnoreCase(filterParams.getIsChild())
            ? processChildTransactions(filterParams)
            : processParentTransactions(filterParams);
    }

    private boolean shouldProcessBulkApproval(FilterParams filterParams) {
        return YES.equalsIgnoreCase(filterParams.getIsChannelAdmin())
            || CollectionUtils.isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList());
    }

    private List<ApprovalStatusTxn> processChildTransactions(FilterParams filterParams) {
        filterParams.setIsChildY(YES);
        
        try {
            // Execute main transaction list query
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkApprovalStatusTxnList(filterParams), 
                           executorService);

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(7, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            // Execute count query concurrently
            CompletableFuture<Integer> countFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkApprovalStatusTxnCount(filterParams),
                           executorService);

            // Wait for count result and update list
            int count = countFuture.get(7, TimeUnit.SECONDS);
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
            // Execute main transaction list query
            CompletableFuture<List<ApprovalStatusTxn>> txnListFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(filterParams), 
                           executorService);

            List<ApprovalStatusTxn> approvalStatusList = txnListFuture.get(7, TimeUnit.SECONDS);
            
            if (ObjectUtils.isEmpty(approvalStatusList)) {
                return approvalStatusList;
            }

            List<String> transIds = approvalStatusList.stream()
                .map(ApprovalStatusTxn::getTransactionId)
                .toList();

            // Execute all queries concurrently
            CompletableFuture<Integer> countFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(filterParams),
                           executorService);

            CompletableFuture<List<PwsTransactionCharges>> chargesFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getChargesDetail(transIds),
                           executorService);

            CompletableFuture<List<ApprovalStatusTxn>> fxContractsFuture = CompletableFuture
                .supplyAsync(() -> transactionWorkflowDAO.getFxContracts(transIds),
                           executorService);

            // Wait for all futures and process results
            int count = countFuture.get(7, TimeUnit.SECONDS);
            List<PwsTransactionCharges> chargesDetail = chargesFuture.get(7, TimeUnit.SECONDS);
            List<ApprovalStatusTxn> fxContracts = fxContractsFuture.get(7, TimeUnit.SECONDS);

            // Process the results
            updateParentTransactions(approvalStatusList, count, chargesDetail, fxContracts);

            return approvalStatusList;

        } catch (Exception e) {
            log.error("Error processing parent transactions", e);
            throw new TransactionProcessingException("Failed to process parent transactions", e);
        }
    }

    private void updateParentTransactions(
            List<ApprovalStatusTxn> approvalStatusList,
            int count,
            List<PwsTransactionCharges> chargesDetail,
            List<ApprovalStatusTxn> fxContracts) {
        
        // Create FX contracts map
        Map<String, ApprovalStatusTxn> fxContractMap = fxContracts.stream()
            .collect(Collectors.toMap(
                ApprovalStatusTxn::getTransactionId,
                Function.identity(),
                (existing, replacement) -> existing
            ));

        // Process charges
        Map<Long, List<PwsTransactionCharges>> chargeFeeAmountMap = chargesDetail.stream()
            .collect(Collectors.groupingBy(PwsTransactionCharges::getTransactionId));

        // Update approval status list
        chargeFeeAmountMap.forEach((transactionId, charges) -> {
            String transId = transactionId.toString();
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
                .forEach(approv -> updateApprovalStatus(approv, transCurrency, totalAmount, fx));
        });

        // Set count for first transaction
        approvalStatusList.stream()
            .findFirst()
            .ifPresent(txn -> txn.setCount(BigDecimal.valueOf(count)));
    }

    private void updateApprovalStatus(
            ApprovalStatusTxn approval,
            String currency,
            BigDecimal amount,
            ApprovalStatusTxn fx) {
        approval.setFeesCurrency(currency);
        approval.setTotalFeeAmount(amount);
        if (fx != null) {
            approval.setFxType(fx.getFxType());
            approval.setFxFlag(fx.getFxFlag());
            approval.setBookingRefId(fx.getBookingRefId());
            approval.setEarmarkId(fx.getEarmarkId());
        }
    }
}

@Slf4j
class TransactionProcessingException extends RuntimeException {
    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
@ExtendWith(MockitoExtension.class)
class ApprovalStatusServiceImplTest {

    @Mock
    private TransactionWorkflowDAO transactionWorkflowDAO;

    @InjectMocks
    private ApprovalStatusServiceImpl service;

    private FilterParams filterParams;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        filterParams = new FilterParams();
        executorService = mock(ExecutorService.class);
        ReflectionTestUtils.setField(service, "executorService", executorService);
    }

    @Nested
    @DisplayName("getBulkApprovalStatus Tests")
    class GetBulkApprovalStatusTests {

        @Test
        @DisplayName("Should return empty list when conditions not met")
        void shouldReturnEmptyListWhenConditionsNotMet() {
            // Given
            filterParams.setIsChannelAdmin("N");
            filterParams.setBulkAccountBasedOnResourceFeatureList(null);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(transactionWorkflowDAO);
        }

        @Test
        @DisplayName("Should process child transactions successfully")
        void shouldProcessChildTransactionsSuccessfully() {
            // Given
            setupChildTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnCount(any()))
                .thenReturn(10);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result)
                .hasSize(2)
                .first()
                .satisfies(txn -> {
                    assertThat(txn.getCount()).isEqualTo(BigDecimal.valueOf(10));
                    assertThat(txn.getTransactionId()).isEqualTo("1");
                });
                
            verify(transactionWorkflowDAO).getBulkApprovalStatusTxnList(any());
            verify(transactionWorkflowDAO).getBulkApprovalStatusTxnCount(any());
            verify(transactionWorkflowDAO, never()).getFxContracts(any());
        }

        @Test
        @DisplayName("Should handle empty result for child transactions")
        void shouldHandleEmptyResultForChildTransactions() {
            // Given
            setupChildTransactionScenario();
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenReturn(Collections.emptyList());

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).isEmpty();
            verify(transactionWorkflowDAO).getBulkApprovalStatusTxnList(any());
            verify(transactionWorkflowDAO, never()).getBulkApprovalStatusTxnCount(any());
        }

        @Test
        @DisplayName("Should process parent transactions successfully")
        void shouldProcessParentTransactionsSuccessfully() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            List<ApprovalStatusTxn> fxContracts = createSampleFxContracts();
            List<PwsTransactionCharges> charges = createSampleCharges();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getFxContracts(any()))
                .thenReturn(fxContracts);
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(charges);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                .satisfies(txn -> {
                    assertThat(txn.getCount()).isEqualTo(BigDecimal.valueOf(10));
                    assertThat(txn.getFxType()).isEqualTo("TYPE1");
                    assertThat(txn.getTotalFeeAmount()).isEqualTo(BigDecimal.TEN);
                    assertThat(txn.getFeesCurrency()).isEqualTo("USD");
                });

            verifyParentTransactionCalls();
        }

        @Test
        @DisplayName("Should handle empty result for parent transactions")
        void shouldHandleEmptyResultForParentTransactions() {
            // Given
            setupParentTransactionScenario();
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(Collections.emptyList());

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).isEmpty();
            verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnList(any());
            verify(transactionWorkflowDAO, never()).getBulkParentApprovalStatusTxnCount(any());
            verify(transactionWorkflowDAO, never()).getFxContracts(any());
            verify(transactionWorkflowDAO, never()).getChargesDetail(any());
        }

        @Test
        @DisplayName("Should handle null FX contracts in parent transactions")
        void shouldHandleNullFxContractsInParentTransactions() {
            // Given
            setupParentTransactionScenario();
            List<ApprovalStatusTxn> expectedList = createSampleTransactionList();
            List<PwsTransactionCharges> charges = createSampleCharges();
            
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenReturn(expectedList);
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnCount(any()))
                .thenReturn(10);
            when(transactionWorkflowDAO.getFxContracts(any()))
                .thenReturn(null);
            when(transactionWorkflowDAO.getChargesDetail(any()))
                .thenReturn(charges);

            // When
            List<ApprovalStatusTxn> result = service.getBulkApprovalStatus(filterParams);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                .satisfies(txn -> {
                    assertThat(txn.getFxType()).isNull();
                    assertThat(txn.getTotalFeeAmount()).isEqualTo(BigDecimal.TEN);
                });
        }

        @Test
        @DisplayName("Should handle exception in child transactions")
        void shouldHandleExceptionInChildTransactions() {
            // Given
            setupChildTransactionScenario();
            when(transactionWorkflowDAO.getBulkApprovalStatusTxnList(any()))
                .thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed to process child transactions");
        }

        @Test
        @DisplayName("Should handle exception in parent transactions")
        void shouldHandleExceptionInParentTransactions() {
            // Given
            setupParentTransactionScenario();
            when(transactionWorkflowDAO.getBulkParentApprovalStatusTxnList(any()))
                .thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatThrownBy(() -> service.getBulkApprovalStatus(filterParams))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Failed to process parent transactions");
        }
    }

    @Test
    @DisplayName("Should clean up resources properly")
    void shouldCleanUpResourcesProperly() {
        // When
        service.cleanup();

        // Then
        verify(executorService).shutdown();
    }

    // Helper methods
    private void setupChildTransactionScenario() {
        filterParams.setIsChannelAdmin("Y");
        filterParams.setIsChild("Y");
        filterParams.setBulkAccountBasedOnResourceFeatureList(List.of("ACCOUNT1"));
    }

    private void setupParentTransactionScenario() {
        filterParams.setIsChannelAdmin("Y");
        filterParams.setIsChild("N");
        filterParams.setBulkAccountBasedOnResourceFeatureList(List.of("ACCOUNT1"));
    }

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
        fx1.setBookingRefId("BOOKING1");
        fx1.setEarmarkId("EARMARK1");
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

    private void verifyParentTransactionCalls() {
        verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnList(any());
        verify(transactionWorkflowDAO).getBulkParentApprovalStatusTxnCount(any());
        verify(transactionWorkflowDAO).getFxContracts(any());
        verify(transactionWorkflowDAO).getChargesDetail(any());
    }
}
```

```java
import static com.uob.gwb.txn.common.GenericConstants.*;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uob.gwb.transaction.common.domain.*;
import com.uob.gwb.transaction.common.domain.v1.CompanyAccounts;
import com.uob.gwb.transaction.common.domain.v1.CompanyAndAccountsForResource;
import com.uob.gwb.transaction.common.domain.v1.CompanyAndAccountsForUserResourceFeaturesResp;
import com.uob.gwb.transaction.common.domain.v1.ResourceFeatureAndCompanies;
import com.uob.gwb.transaction.common.model.BeneDetails;
import com.uob.gwb.transaction.common.service.CamelService;
import com.uob.gwb.transaction.common.service.EntitlementService;
import com.uob.gwb.transaction.common.util.HttpUtils;
import com.uob.gwb.transaction.common.util.TransactionUtils;
import com.uob.gwb.txn.common.GenericConstants;
import com.uob.gwb.txn.config.TransactionWorkFlowConfig;
import com.uob.gwb.txn.data.mapper.ApprovalStatusListMapper;
import com.uob.gwb.txn.data.mapper.ResourceCompanyAndAccountsforResourcesMapper;
import com.uob.gwb.txn.data.mapper.TransactionEntityDomainMapper;
import com.uob.gwb.txn.domain.ApprovalStatusTxn;
import com.uob.gwb.txn.domain.FilterParams;
import com.uob.gwb.txn.model.*;
import com.uob.gwb.txn.model.GenericLookUp;
import com.uob.gwb.txn.model.Resource;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowDAO;
import com.uob.gwb.txn.pws.dao.TransactionWorkflowV2DAO;
import com.uob.gwb.txn.utils.TransactionWorkflowUtils;
import com.uob.gwb.txn.utils.TxnUtils;
import com.uob.ufw.core.exception.ApplicationException;
import com.uob.ufw.core.util.JsonUtil;
import java.math.BigDecimal;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ApprovalStatusV2Service")
public class ApprovalStatusV2ServiceImpl extends ApprovalStatusServiceImpl implements ApprovalStatusV2Service {

    private EntitlementService entitlementService;
    private TransactionWorkflowV2DAO transactionWorkflowV2DAO;
    private ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper;
    private ApprovalStatusListMapper approvalStatusListMapper;
    private TransactionUtils transactionUtils;
    private ObjectMapper objectMapper;
    private TransactionWorkflowUtils transactionWorkflowUtils;
    private TransactionEntityDomainMapper transactionEntityDomainMapper;

    public ApprovalStatusV2ServiceImpl(EntitlementService entitlementService,
            TransactionWorkflowDAO transactionWorkflowDAO, TransactionWorkflowV2DAO transactionWorkflowV2DAO,
            ResourceCompanyAndAccountsforResourcesMapper resourceCompanyAndAccountsforResourcesMapper,
            ApprovalStatusListMapper approvalStatusListMapper, TransactionWorkFlowConfig transactionWorkFlowConfig,
            TransactionUtils transactionUtils, HttpUtils httpUtils, CamelService camelService,
            ObjectMapper objectMapper, TransactionWorkflowUtils transactionWorkflowUtils,
            TransactionWorkflowEnquiryCommonService commonService,
            TransactionEntityDomainMapper transactionEntityDomainMapper,
            EntitlementsResourceService entitlementsResourceService, TxnUtils txnUtils) {
        super(entitlementService, transactionWorkflowDAO, transactionWorkflowV2DAO,
                resourceCompanyAndAccountsforResourcesMapper, approvalStatusListMapper, transactionWorkFlowConfig,
                transactionUtils, httpUtils, camelService, objectMapper, transactionWorkflowUtils, commonService,
                transactionEntityDomainMapper, entitlementsResourceService, txnUtils);
        this.entitlementService = entitlementService;
        this.transactionWorkflowV2DAO = transactionWorkflowV2DAO;
        this.resourceCompanyAndAccountsforResourcesMapper = resourceCompanyAndAccountsforResourcesMapper;
        this.approvalStatusListMapper = approvalStatusListMapper;
        this.transactionUtils = transactionUtils;
        this.objectMapper = objectMapper;
        this.transactionWorkflowUtils = transactionWorkflowUtils;
        this.transactionEntityDomainMapper = transactionEntityDomainMapper;
    }

    private ApprovalStatusLookUpResp setApprovalStatusTxnList(String transactionType, FilterParams filterParams,
            TransactionsLookUpReq request, CompanyAndAccountsForUserResourceFeaturesResp response, String isChild) {
        ApprovalStatusLookUpResp approvalStatusLookUpResp = null;
        List<ApprovalStatusTxn> approvalStatusTxnList = new ArrayList<>();
        if (SINGLE.equalsIgnoreCase(transactionType)) {
            approvalStatusTxnList.addAll(transactionWorkflowV2DAO.getApprovalStatusTxnList(filterParams));
        } else if (BULK.equalsIgnoreCase(transactionType)) {
            approvalStatusTxnList.addAll(super.getBulkApprovalStatus(filterParams));
        } else if (BOTH.equalsIgnoreCase(transactionType)) {

            filterParams.setLimit(String.valueOf(request.getAdditionalProperties().get(LIMIT)));
            String orderBy = filterParams.getSortFieldWithDirection()
                    .substring(filterParams.getSortFieldWithDirection().indexOf(DOT_SEPARATOR) + 1);
            String orderByWithDirection = filterParams.getSortFieldWithDirection();

            StringBuilder sortBuilder = new StringBuilder();

            if (orderByWithDirection.contains(LOWER)) {
                sortBuilder.append(LOWER).append(OPEN_BRACE);
            }

            sortBuilder.append(BOTH_TRANS).append(DOT_SEPARATOR).append(orderBy.trim());
            filterParams.setSortFieldWithDirection(sortBuilder.toString());

            if (isNotEmpty(filterParams.getSingleAccountBasedOnResourceFeatureList())) {
                filterParams.setSingleFeatureId(SINGLE_TRANSACTION);
                filterParams.setIsSingleTransaction(YES);
            }

            if (isNotEmpty(filterParams.getBulkAccountBasedOnResourceFeatureList())) {
                filterParams.setIsBulkTransaction(YES);
            }

            approvalStatusTxnList
                    .addAll(transactionWorkflowV2DAO.getBulkBothApprovalStatusTxnList(filterParams, PAYEE_TAX));
            if (isNotEmpty(approvalStatusTxnList)) {
                List<String> transIds = approvalStatusTxnList.stream().map(app -> app.getTransactionId()).toList();
                int count = transactionWorkflowV2DAO.getV2BulkBothApprovalStatusTxnCount(filterParams);
                Map<String, ApprovalStatusTxn> fxContractMap = new HashMap<>();
                List<ApprovalStatusTxn> fxContracts = transactionWorkflowV2DAO.getFxContracts(transIds);
                fxContracts.forEach(fx -> fxContractMap.put(fx.getTransactionId(), fx));
                approvalStatusTxnList.stream().forEach(approve -> {
                    ApprovalStatusTxn fx = fxContractMap.get(approve.getTransactionId());
                    approve.setFxType(fx.getFxType());
                    approve.setFxFlag(fx.getFxFlag());
                    approve.setEarmarkId(fx.getEarmarkId());
                    approve.setBookingRefId(fx.getBookingRefId());
                    approve.setCount(BigDecimal.valueOf(count));
                });
            }
        }
        log.debug("ApprovalStatusV2ServiceImpl::approvalStatusTxnList:: {} ", approvalStatusTxnList);

        if (isNotEmpty(approvalStatusTxnList)) {
            approvalStatusLookUpResp = setApprovalStatusLookUpResp(approvalStatusTxnList, response, transactionType,
                    isChild, request);
        }

        return approvalStatusLookUpResp;
    }

}
```
