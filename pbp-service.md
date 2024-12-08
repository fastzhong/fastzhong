
# StepAware

```java
package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StepAwareServiceTest {

    private StepAwareService service;
    @Mock private StepExecution stepExecution;
    @Mock private JobExecution jobExecution;
    @Mock private ExecutionContext executionContext;
    @Mock private ObjectMapper objectMapper;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new StepAwareService();
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(stepExecution.getExecutionContext()).thenReturn(new ExecutionContext());
        service.beforeStep(stepExecution);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Step Execution Tests")
    class StepExecutionTests {
        
        @Test
        @DisplayName("Should store and retrieve step execution")
        void beforeStep_WithStepExecution_ShouldStoreExecution() {
            // Assert
            assertSame(stepExecution, service.getStepExecution());
        }

        @Test
        @DisplayName("Should get job execution")
        void getJobExecution_ShouldReturnJobExecution() {
            assertEquals(jobExecution, service.getJobExecution());
        }

        @Test
        @DisplayName("Should get job context")
        void getJobContext_ShouldReturnExecutionContext() {
            assertEquals(executionContext, service.getJobContext());
        }
    }

    @Nested
    @DisplayName("Context Property Tests")
    class ContextPropertyTests {
        
        @Test
        @DisplayName("Should get and set source format")
        void sourceFormat_GetAndSet_ShouldWork() {
            // Arrange
            String format = "JSON";
            
            // Act
            service.setSourceFormat(format);
            
            // Assert
            verify(executionContext).putString(ContextKey.sourceFormat, format);
        }

        @Test
        @DisplayName("Should get and set user ID")
        void userId_GetAndSet_ShouldWork() {
            // Arrange
            long userId = 123L;
            
            // Act
            service.setUserId(userId);
            
            // Assert
            verify(executionContext).putLong(ContextKey.userId, userId);
        }

        @Test
        @DisplayName("Should get and set company ID")
        void companyId_GetAndSet_ShouldWork() {
            // Arrange
            long companyId = 456L;
            
            // Act
            service.setCompanyId(companyId);
            
            // Assert
            verify(executionContext).putLong(ContextKey.companyId, companyId);
        }
    }

    @Nested
    @DisplayName("Bank Settings Tests")
    class BankSettingsTests {
        
        @Test
        @DisplayName("Should get and set bank settings")
        void bankSettings_GetAndSet_ShouldWork() {
            // Arrange
            String resourceId = "RES001";
            String featureId = "FEAT001";
            BankSettings.SettingsName name = BankSettings.SettingsName.GENERAL;
            Object settings = new HashMap<>();
            BankSettings bankSettings = new BankSettings();
            
            when(executionContext.get(ContextKey.bankSettings, BankSettings.class))
                .thenReturn(bankSettings);

            // Act
            service.setBankSettings(resourceId, featureId, name, settings);
            
            // Assert
            verify(executionContext).get(ContextKey.bankSettings, BankSettings.class);
        }

        @Test
        @DisplayName("Should create new bank settings if null")
        void bankSettings_WithNullSettings_ShouldCreateNew() {
            // Arrange
            String resourceId = "RES001";
            BankSettings.SettingsName name = BankSettings.SettingsName.GENERAL;
            Object settings = new HashMap<>();
            
            when(executionContext.get(ContextKey.bankSettings, BankSettings.class))
                .thenReturn(null);

            // Act
            service.setBankSettings(resourceId, name, settings);
            
            // Assert
            verify(executionContext).get(ContextKey.bankSettings, BankSettings.class);
        }
    }

    @Nested
    @DisplayName("Account Resource Tests")
    class AccountResourceTests {
        
        @Test
        @DisplayName("Should get and set account resource")
        void accountResource_GetAndSet_ShouldWork() {
            // Arrange
            String accountNum = "ACC001";
            AccountResource resource = new AccountResource();
            Map<String, AccountResource> resourceMap = new HashMap<>();
            
            when(executionContext.get(ContextKey.accountResources, Map.class))
                .thenReturn(resourceMap);

            // Act
            service.setAccountResource(accountNum, resource);
            
            // Assert
            verify(executionContext).get(ContextKey.accountResources, Map.class);
            assertTrue(resourceMap.containsKey(accountNum));
        }

        @Test
        @DisplayName("Should create new map if null")
        void accountResource_WithNullMap_ShouldCreateNew() {
            // Arrange
            String accountNum = "ACC001";
            AccountResource resource = new AccountResource();
            
            when(executionContext.get(ContextKey.accountResources, Map.class))
                .thenReturn(null);

            // Act
            service.setAccountResource(accountNum, resource);
            
            // Assert
            verify(executionContext).get(ContextKey.accountResources, Map.class);
        }
    }

    @Nested
    @DisplayName("Derived Value Date Tests")
    class DerivedValueDateTests {
        
        @Test
        @DisplayName("Should get and set derived value date")
        void derivedValueDate_GetAndSet_ShouldWork() {
            // Arrange
            String accountNum = "ACC001";
            String currency = "USD";
            LocalDate date = LocalDate.now();
            Map<String, Map<String, LocalDate>> dateMap = new HashMap<>();
            
            when(executionContext.get(ContextKey.derivedValueDate, Map.class))
                .thenReturn(dateMap);

            // Act
            service.setDerivedValueDate(accountNum, currency, date);
            LocalDate retrievedDate = service.getDerivedValueDate(accountNum, currency);
            
            // Assert
            verify(executionContext, times(2))
                .get(ContextKey.derivedValueDate, Map.class);
            assertEquals(date, retrievedDate);
        }

        @Test
        @DisplayName("Should handle missing account")
        void derivedValueDate_WithMissingAccount_ShouldHandleGracefully() {
            // Arrange
            String accountNum = "ACC001";
            String currency = "USD";
            Map<String, Map<String, LocalDate>> dateMap = new HashMap<>();
            
            when(executionContext.get(ContextKey.derivedValueDate, Map.class))
                .thenReturn(dateMap);

            // Act
            LocalDate result = service.getDerivedValueDate(accountNum, currency);
            
            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Processing Result Tests")
    class ProcessingResultTests {
        
        @Test
        @DisplayName("Should update processing status")
        void updateProcessingStatus_WithValidData_ShouldUpdate() {
            // Arrange
            Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
            when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(result);
            
            Pain001InboundProcessingStatus status = Pain001InboundProcessingStatus.COMPLETED;
            String message = "Processing completed";

            // Act
            service.updateProcessingStatus(status, message, objectMapper);
            
            // Assert
            assertEquals(status, result.getProcessingStatus());
            assertEquals(message, result.getMessage());
        }
    }

    @Nested
    @DisplayName("File Upload Tests")
    class FileUploadTests {
        
        @Test
        @DisplayName("Should get and set file upload")
        void fileUpload_GetAndSet_ShouldWork() {
            // Arrange
            PwsFileUpload fileUpload = new PwsFileUpload();
            
            // Act
            service.setFileUpload(fileUpload);
            
            // Assert
            verify(executionContext).put(ContextKey.fileUpload, fileUpload);
        }

        @Test
        @DisplayName("Should return new file upload if null")
        void fileUpload_WhenNull_ShouldReturnNew() {
            // Arrange
            when(executionContext.get(ContextKey.fileUpload, PwsFileUpload.class))
                .thenReturn(null);

            // Act
            PwsFileUpload result = service.getFileUpload();
            
            // Assert
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Debug Tests")
    class DebugTests {
        
        @Test
        @DisplayName("Should debug context without exception")
        void debugContext_ShouldNotThrowException() {
            // Arrange
            AppConfig.BulkRoute route = new AppConfig.BulkRoute();
            when(executionContext.get(eq(ContextKey.routeConfig), any()))
                .thenReturn(route);

            // Act & Assert
            assertDoesNotThrow(() -> service.debugContext(objectMapper));
        }

        @Test
        @DisplayName("Should debug payment information without exception")
        void debugPaymentInformation_ShouldNotThrowException() {
            // Arrange
            PaymentInformation paymentInfo = new PaymentInformation();

            // Act & Assert
            assertDoesNotThrow(() -> 
                service.debugPaymentInformation(objectMapper, paymentInfo));
        }
    }
}
```

# Pain001Processing

```java
```


# Pain001InboundService

```java
```

# debulk

```java
```

# enrich

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEnrichmentServiceImpl Test Suite")
class PaymentEnrichmentServiceImplTest {

    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext jobContext;
    @Mock
    private PwsFileUpload fileUpload;

    @InjectMocks
    private PaymentEnrichmentServiceImpl service;

    @BeforeEach
    void setUp() {
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        service.beforeStep(stepExecution);

        when(jobContext.get(eq(ContextKey.fileUpload), any())).thenReturn(fileUpload);
        when(fileUpload.getFileUploadId()).thenReturn(123L);
    }

    @Nested
    @DisplayName("Pre/Post Transaction Validation Tests")
    class PrePostTransactionValidationTests {

        @Test
        @DisplayName("Should pass through payments in pre-transaction validation")
        void shouldPassThroughPaymentsInPreTransactionValidation() {
            List<PaymentInformation> payments = Collections.singletonList(new PaymentInformation());
            
            List<PaymentInformation> result = service.enrichPreTransactionValidation(payments);
            
            assertSame(payments, result);
        }

        @Test
        @DisplayName("Should pass through payments in post-transaction validation")
        void shouldPassThroughPaymentsInPostTransactionValidation() {
            List<PaymentInformation> payments = Collections.singletonList(new PaymentInformation());
            
            List<PaymentInformation> result = service.enrichPostTransactionValidation(payments);
            
            assertSame(payments, result);
        }
    }

    @Nested
    @DisplayName("Post Validation Enrichment Tests")
    class PostValidationEnrichmentTests {

        @Test
        @DisplayName("Should skip invalid payments during enrichment")
        void shouldSkipInvalidPaymentsDuringEnrichment() {
            // Arrange
            PaymentInformation invalidPayment = createPaymentInfo(false);
            List<PaymentInformation> payments = Collections.singletonList(invalidPayment);
            
            // Act
            List<PaymentInformation> result = service.enrichPostValidation(payments);
            
            // Assert
            assertTrue(result.isEmpty());
            verify(jobContext).put(eq(ContextKey.result), any(Pain001InboundProcessingResult.class));
        }

        @Test
        @DisplayName("Should enrich valid payments with all required fields")
        void shouldEnrichValidPaymentsWithAllRequiredFields() {
            // Arrange
            PaymentInformation validPayment = createPaymentInfo(true);
            setupValidPaymentMocks(validPayment);
            List<PaymentInformation> payments = Collections.singletonList(validPayment);

            // Act
            List<PaymentInformation> result = service.enrichPostValidation(payments);

            // Assert
            assertFalse(result.isEmpty());
            PwsTransactions enrichedTxn = result.get(0).getPwsTransactions();
            assertEquals(CaptureStatus.SUBMITTED.name(), enrichedTxn.getCaptureStatus());
            assertEquals(CustomerStatus.PENDING_VERIFICATION.name(), enrichedTxn.getCustomerTransactionStatus());
            
            PwsBulkTransactions enrichedBulkTxn = result.get(0).getPwsBulkTransactions();
            assertEquals(CustomerStatus.PENDING_VERIFICATION.name(), enrichedBulkTxn.getStatus());
            assertEquals(123L, enrichedBulkTxn.getFileUploadId());
        }

        @Test
        @DisplayName("Should set account and transaction currency correctly")
        void shouldSetAccountAndTransactionCurrencyCorrectly() {
            // Arrange
            PaymentInformation validPayment = createPaymentInfo(true);
            setupValidPaymentMocks(validPayment);
            
            AccountResource accountResource = mock(AccountResource.class);
            when(accountResource.getAccountCurrency()).thenReturn("USD");
            when(jobContext.get(eq(ContextKey.accountResources), any())).thenReturn(
                Collections.singletonMap("ACC123", accountResource));
            
            validPayment.getPwsTransactions().setAccountNumber("ACC123");
            
            // Act
            List<PaymentInformation> result = service.enrichPostValidation(Collections.singletonList(validPayment));

            // Assert
            assertFalse(result.isEmpty());
            PwsTransactions enrichedTxn = result.get(0).getPwsTransactions();
            assertEquals("USD", enrichedTxn.getAccountCurrency());
            assertEquals("EUR", enrichedTxn.getTransactionCurrency());
        }
    }

    @Nested
    @DisplayName("Bulk Payment Computation Tests")
    class BulkPaymentComputationTests {

        @Test
        @DisplayName("Should compute totals correctly for valid transactions")
        void shouldComputeTotalsCorrectlyForValidTransactions() {
            // Arrange
            PaymentInformation payment = createPaymentInfo(true);
            List<CreditTransferTransaction> transactions = Arrays.asList(
                createTransaction(true, new BigDecimal("100.00")),
                createTransaction(true, new BigDecimal("200.00")),
                createTransaction(true, new BigDecimal("150.00"))
            );
            payment.setCreditTransferTransactionList(transactions);

            // Act
            List<PaymentInformation> result = service.bulkPaymentComputation(Collections.singletonList(payment));

            // Assert
            assertFalse(result.isEmpty());
            PwsTransactions computed = result.get(0).getPwsTransactions();
            assertEquals(3, computed.getTotalChild());
            assertEquals(new BigDecimal("450.00"), computed.getTotalAmount());
            assertEquals(new BigDecimal("200.00"), computed.getHighestAmount());
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid transactions")
        void shouldHandleMixedValidAndInvalidTransactions() {
            // Arrange
            PaymentInformation payment = createPaymentInfo(true);
            List<CreditTransferTransaction> transactions = Arrays.asList(
                createTransaction(true, new BigDecimal("100.00")),
                createTransaction(false, new BigDecimal("200.00")),
                createTransaction(true, new BigDecimal("150.00"))
            );
            payment.setCreditTransferTransactionList(transactions);

            // Act
            List<PaymentInformation> result = service.bulkPaymentComputation(Collections.singletonList(payment));

            // Assert
            assertFalse(result.isEmpty());
            PwsTransactions computed = result.get(0).getPwsTransactions();
            assertEquals(2, computed.getTotalChild());
            assertEquals(new BigDecimal("250.00"), computed.getTotalAmount());
            assertEquals(new BigDecimal("150.00"), computed.getHighestAmount());
        }

        @Test
        @DisplayName("Should mark payment as invalid when no valid transactions")
        void shouldMarkPaymentAsInvalidWhenNoValidTransactions() {
            // Arrange
            PaymentInformation payment = createPaymentInfo(true);
            List<CreditTransferTransaction> transactions = Arrays.asList(
                createTransaction(false, BigDecimal.ZERO),
                createTransaction(false, BigDecimal.ZERO)
            );
            payment.setCreditTransferTransactionList(transactions);

            // Act
            List<PaymentInformation> result = service.bulkPaymentComputation(Collections.singletonList(payment));

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    private PaymentInformation createPaymentInfo(boolean isValid) {
        PaymentInformation payment = new PaymentInformation();
        payment.setPwsTransactions(new PwsTransactions());
        payment.setPwsBulkTransactions(new PwsBulkTransactions());
        if (!isValid) {
            payment.addValidationError("TEST", "Test error");
        }
        return payment;
    }

    private CreditTransferTransaction createTransaction(boolean isValid, BigDecimal amount) {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionAmount(amount);
        instructions.setTransactionCurrency("EUR");
        transaction.setPwsBulkTransactionInstructions(instructions);
        if (!isValid) {
            transaction.addValidationError("TEST", "Test error");
        }
        return transaction;
    }

    private void setupValidPaymentMocks(PaymentInformation payment) {
        when(jobContext.getLong(ContextKey.userId)).thenReturn(456L);
        when(jobContext.getLong(ContextKey.companyId)).thenReturn(789L);
        
        CreditTransferTransaction transaction = createTransaction(true, new BigDecimal("100.00"));
        payment.setCreditTransferTransactionList(Collections.singletonList(transaction));
    }
}
```

# integrate

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentIntegrationserviceImpl Test Suite")
class PaymentIntegrationserviceImplTest {

    @Mock
    private TransactionUtils transactionUtils;
    @Mock
    private GetAxwayTokenService getAxwayTokenService;
    @Mock
    private EntitlementService entitlementService;
    @Mock
    private RefDataService refDataService;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext jobContext;

    @InjectMocks
    private PaymentIntegrationserviceImpl service;

    @BeforeEach
    void setUp() {
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        service.beforeStep(stepExecution);
        
        // Set the URL values using reflection since they're normally set by @Value
        ReflectionTestUtils.setField(service, "ueqsResourceFeature", "http://test.com/resources");
        ReflectionTestUtils.setField(service, "ueqsCompanyAccount", "http://test.com/accounts");
    }

    @Nested
    @DisplayName("CEW Headers Tests")
    class CewHeadersTests {

        @Test
        @DisplayName("Should create CEW headers with correct values")
        void shouldCreateCewHeadersWithCorrectValues() {
            // Arrange
            AxwayResponseData axwayResponse = new AxwayResponseData();
            axwayResponse.setTokenType("Bearer");
            axwayResponse.setAccessToken("test-token");
            when(getAxwayTokenService.getAxwayJwtToken()).thenReturn(axwayResponse);

            // Act
            Map<String, Object> headers = service.getCewHeaders();

            // Assert
            assertNotNull(headers);
            assertEquals("Bearer test-token", headers.get("Authorization"));
            assertTrue(headers.containsKey("Req-Date-Time"));
            
            // Verify the date is in UTC
            OffsetDateTime dateTime = (OffsetDateTime) headers.get("Req-Date-Time");
            assertEquals(ZoneOffset.UTC, dateTime.getOffset());
            
            // Verify other required CEW headers are present
            assertTrue(headers.containsAll(HEADER_CEW.keySet()));
        }

        @Test
        @DisplayName("Should handle null Axway response")
        void shouldHandleNullAxwayResponse() {
            // Arrange
            when(getAxwayTokenService.getAxwayJwtToken()).thenReturn(null);

            // Act & Assert
            assertThrows(NullPointerException.class, () -> service.getCewHeaders());
        }
    }

    @Nested
    @DisplayName("Resources and Features Tests")
    class ResourcesAndFeaturesTests {

        @Test
        @DisplayName("Should get resources and features for valid user")
        void shouldGetResourcesAndFeaturesForValidUser() {
            // Arrange
            long userId = 123L;
            String encryptedUserId = "encrypted-123";
            UserResourceFeaturesActionsData expectedData = new UserResourceFeaturesActionsData();
            
            when(transactionUtils.getEncrypted(String.valueOf(userId))).thenReturn(encryptedUserId);
            when(entitlementService.getResourcesAndFeaturesByUserId(eq(encryptedUserId), any()))
                .thenReturn(expectedData);

            // Act
            UserResourceFeaturesActionsData result = service.getResourcesAndFeatures(userId);

            // Assert
            assertNotNull(result);
            assertEquals(expectedData, result);
            verify(entitlementService).getResourcesAndFeaturesByUserId(eq(encryptedUserId), any());
        }

        @Test
        @DisplayName("Should handle encryption failure")
        void shouldHandleEncryptionFailure() {
            // Arrange
            long userId = 123L;
            when(transactionUtils.getEncrypted(String.valueOf(userId)))
                .thenThrow(new RuntimeException("Encryption failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> service.getResourcesAndFeatures(userId));
        }
    }

    @Nested
    @DisplayName("Company and Accounts Tests")
    class CompanyAndAccountsTests {

        @Test
        @DisplayName("Should get company and accounts for valid inputs")
        void shouldGetCompanyAndAccountsForValidInputs() {
            // Arrange
            long userId = 123L;
            String resourceId = "RES_001";
            String featureId = "FEAT_001";
            String encryptedUserId = "encrypted-123";
            
            when(jobContext.getLong(ContextKey.userId)).thenReturn(userId);
            when(transactionUtils.getEncrypted(String.valueOf(userId))).thenReturn(encryptedUserId);
            
            CompanyAndAccountsForResourceFeaturesResp expectedResponse = new CompanyAndAccountsForResourceFeaturesResp();
            when(entitlementService.getV3CompanyAndAccountsForUserResourceFeatures(any(), any()))
                .thenReturn(expectedResponse);

            // Act
            CompanyAndAccountsForResourceFeaturesResp result = 
                service.getCompanyAndAccounts(userId, resourceId, featureId);

            // Assert
            assertNotNull(result);
            assertEquals(expectedResponse, result);
            
            // Verify request structure
            ArgumentCaptor<CompanyAndAccountsForResourceFeaturesReq> requestCaptor = 
                ArgumentCaptor.forClass(CompanyAndAccountsForResourceFeaturesReq.class);
            verify(entitlementService).getV3CompanyAndAccountsForUserResourceFeatures(
                requestCaptor.capture(), any());
            
            CompanyAndAccountsForResourceFeaturesReq capturedRequest = requestCaptor.getValue();
            assertEquals(encryptedUserId, capturedRequest.getUserId());
            assertEquals(1, capturedRequest.getResources().size());
            assertEquals(resourceId, capturedRequest.getResources().get(0).getResourceId());
            assertEquals(Collections.singletonList(featureId), 
                capturedRequest.getResources().get(0).getFeatureIds());
        }

        @Test
        @DisplayName("Should handle empty response")
        void shouldHandleEmptyResponse() {
            // Arrange
            long userId = 123L;
            String resourceId = "RES_001";
            String featureId = "FEAT_001";
            
            when(jobContext.getLong(ContextKey.userId)).thenReturn(userId);
            when(transactionUtils.getEncrypted(anyString())).thenReturn("encrypted-123");
            when(entitlementService.getV3CompanyAndAccountsForUserResourceFeatures(any(), any()))
                .thenReturn(null);

            // Act
            CompanyAndAccountsForResourceFeaturesResp result = 
                service.getCompanyAndAccounts(userId, resourceId, featureId);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle service exception")
        void shouldHandleServiceException() {
            // Arrange
            long userId = 123L;
            String resourceId = "RES_001";
            String featureId = "FEAT_001";
            
            when(jobContext.getLong(ContextKey.userId)).thenReturn(userId);
            when(transactionUtils.getEncrypted(anyString())).thenReturn("encrypted-123");
            when(entitlementService.getV3CompanyAndAccountsForUserResourceFeatures(any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> service.getCompanyAndAccounts(userId, resourceId, featureId));
        }
    }
}
```


# query

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryServiceImpl Test Suite")
class PaymentQueryServiceImplTest {

    @Mock
    private PwsQueryDao pwsQueryDao;

    @InjectMocks
    private PaymentQueryServiceImpl service;

    @Nested
    @DisplayName("File Upload Query Tests")
    class FileUploadQueryTests {

        @Test
        @DisplayName("Should return file upload when found")
        void shouldReturnFileUploadWhenFound() {
            // Arrange
            String fileRef = "REF123";
            PwsFileUpload expectedFileUpload = new PwsFileUpload();
            when(pwsQueryDao.getPwsFileUpload(fileRef)).thenReturn(expectedFileUpload);

            // Act
            PwsFileUpload result = service.getFileUpload(fileRef);

            // Assert
            assertNotNull(result);
            assertEquals(expectedFileUpload, result);
            verify(pwsQueryDao).getPwsFileUpload(fileRef);
        }

        @Test
        @DisplayName("Should return null when file upload not found")
        void shouldReturnNullWhenFileUploadNotFound() {
            // Arrange
            String fileRef = "NONEXISTENT";
            when(pwsQueryDao.getPwsFileUpload(fileRef)).thenReturn(null);

            // Act
            PwsFileUpload result = service.getFileUpload(fileRef);

            // Assert
            assertNull(result);
            verify(pwsQueryDao).getPwsFileUpload(fileRef);
        }

        @Test
        @DisplayName("Should handle null file reference")
        void shouldHandleNullFileReference() {
            // Arrange
            when(pwsQueryDao.getPwsFileUpload(null)).thenReturn(null);

            // Act
            PwsFileUpload result = service.getFileUpload(null);

            // Assert
            assertNull(result);
            verify(pwsQueryDao).getPwsFileUpload(null);
        }

        @Test
        @DisplayName("Should handle DAO exception")
        void shouldHandleDaoException() {
            // Arrange
            String fileRef = "ERROR_REF";
            when(pwsQueryDao.getPwsFileUpload(fileRef)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> service.getFileUpload(fileRef));
            verify(pwsQueryDao).getPwsFileUpload(fileRef);
        }
    }
}
```

# delete

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentDeleteServiceImpl Test Suite")
class PaymentDeleteServiceImplTest {

    @Mock
    private PwsDeleteDao pwsDeleteDao;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext jobContext;
    @Mock
    private ExecutionContext stepContext;

    @InjectMocks
    private PaymentDeleteServiceImpl service;

    @BeforeEach
    void setUp() {
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        service.beforeStep(stepExecution);
    }

    @Nested
    @DisplayName("Delete Payment Information Tests")
    class DeletePaymentInformationTests {

        @Test
        @DisplayName("Should delete all related records when transaction ID exists")
        void shouldDeleteAllRelatedRecordsWhenTransactionIdExists() {
            // Arrange
            long transactionId = 123L;
            PaymentInformation paymentInfo = createPaymentInfoWithTransactionId(transactionId);

            // Act
            service.deletePaymentInformation(paymentInfo);

            // Assert
            // Verify all delete operations were called in correct order
            InOrder inOrder = inOrder(pwsDeleteDao);
            inOrder.verify(pwsDeleteDao).deletePwsTaxInstructions(transactionId);
            inOrder.verify(pwsDeleteDao).deletePwsTransactionAdvices(transactionId);
            inOrder.verify(pwsDeleteDao).deletePwsPartyContacts(transactionId);
            inOrder.verify(pwsDeleteDao).deletePwsParties(transactionId);
            inOrder.verify(pwsDeleteDao).deletePwsBulkTransactionInstructions(transactionId);
            inOrder.verify(pwsDeleteDao).deletePwsBulkTransactions(transactionId);
            inOrder.verify(pwsDeleteDao).deletePwsTransactions(transactionId);

            // Verify IDs were reset
            assertEquals(0L, paymentInfo.getPwsTransactions().getTransactionId());
            assertEquals(0L, paymentInfo.getPwsBulkTransactions().getTransactionId());
            assertTrue(paymentInfo.getCreditTransferTransactionList().stream()
                    .allMatch(tx -> tx.getPwsBulkTransactionInstructions().getTransactionId() == 0L));
        }

        @Test
        @DisplayName("Should skip deletion when transaction ID is zero")
        void shouldSkipDeletionWhenTransactionIdIsZero() {
            // Arrange
            PaymentInformation paymentInfo = createPaymentInfoWithTransactionId(0L);

            // Act
            service.deletePaymentInformation(paymentInfo);

            // Assert
            verifyNoInteractions(pwsDeleteDao);
            assertEquals(0L, paymentInfo.getPwsTransactions().getTransactionId());
            assertEquals(0L, paymentInfo.getPwsBulkTransactions().getTransactionId());
        }

        @Test
        @DisplayName("Should handle multiple credit transfer transactions")
        void shouldHandleMultipleCreditTransferTransactions() {
            // Arrange
            long transactionId = 123L;
            PaymentInformation paymentInfo = createPaymentInfoWithMultipleTransactions(transactionId, 3);

            // Act
            service.deletePaymentInformation(paymentInfo);

            // Assert
            verify(pwsDeleteDao, times(1)).deletePwsTransactions(transactionId);
            assertEquals(3, paymentInfo.getCreditTransferTransactionList().size());
            assertTrue(paymentInfo.getCreditTransferTransactionList().stream()
                    .allMatch(tx -> tx.getPwsBulkTransactionInstructions().getTransactionId() == 0L));
        }

        @Test
        @DisplayName("Should handle database errors while maintaining transaction integrity")
        void shouldHandleDatabaseErrorsWhileMaintainingTransactionIntegrity() {
            // Arrange
            long transactionId = 123L;
            PaymentInformation paymentInfo = createPaymentInfoWithTransactionId(transactionId);
            doThrow(new RuntimeException("Database error"))
                    .when(pwsDeleteDao)
                    .deletePwsPartyContacts(transactionId);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> service.deletePaymentInformation(paymentInfo));
            
            // Verify operations before error were called
            verify(pwsDeleteDao).deletePwsTaxInstructions(transactionId);
            verify(pwsDeleteDao).deletePwsTransactionAdvices(transactionId);
            verify(pwsDeleteDao).deletePwsPartyContacts(transactionId);
            
            // Verify operations after error were not called
            verify(pwsDeleteDao, never()).deletePwsParties(transactionId);
            verify(pwsDeleteDao, never()).deletePwsBulkTransactionInstructions(transactionId);
            verify(pwsDeleteDao, never()).deletePwsBulkTransactions(transactionId);
            verify(pwsDeleteDao, never()).deletePwsTransactions(transactionId);
        }
    }

    private PaymentInformation createPaymentInfoWithTransactionId(long transactionId) {
        PwsTransactions pwsTransactions = new PwsTransactions();
        pwsTransactions.setTransactionId(transactionId);

        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setTransactionId(transactionId);

        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionId(transactionId);

        CreditTransferTransaction creditTransferTransaction = new CreditTransferTransaction();
        creditTransferTransaction.setPwsBulkTransactionInstructions(instructions);

        PaymentInformation paymentInfo = new PaymentInformation();
        paymentInfo.setPwsTransactions(pwsTransactions);
        paymentInfo.setPwsBulkTransactions(pwsBulkTransactions);
        paymentInfo.setCreditTransferTransactionList(Collections.singletonList(creditTransferTransaction));

        return paymentInfo;
    }

    private PaymentInformation createPaymentInfoWithMultipleTransactions(long transactionId, int count) {
        PaymentInformation paymentInfo = createPaymentInfoWithTransactionId(transactionId);
        List<CreditTransferTransaction> transactions = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
            instructions.setTransactionId(transactionId);
            
            CreditTransferTransaction transaction = new CreditTransferTransaction();
            transaction.setPwsBulkTransactionInstructions(instructions);
            transactions.add(transaction);
        }
        
        paymentInfo.setCreditTransferTransactionList(transactions);
        return paymentInfo;
    }
}
```

# update

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentUpdateServiceImpl Test Suite")
class PaymentUpdateServiceImplTest {

    @Mock
    private PwsUpdateDao pwsUpdateDao;

    @InjectMocks
    private PaymentUpdateServiceImpl service;

    @Nested
    @DisplayName("File Upload Status Update Tests")
    class FileUploadStatusUpdateTests {

        @Test
        @DisplayName("Should update file upload status successfully")
        void shouldUpdateFileUploadStatusSuccessfully() {
            // Arrange
            PwsFileUpload fileUpload = new PwsFileUpload();
            fileUpload.setFileUploadId(123L);
            fileUpload.setStatus("COMPLETED");

            // Act
            assertDoesNotThrow(() -> service.updateFileUploadStatus(fileUpload));

            // Assert
            verify(pwsUpdateDao).updateFileUploadStatus(fileUpload);
        }

        @Test
        @DisplayName("Should handle null file upload")
        void shouldHandleNullFileUpload() {
            // Arrange
            doThrow(new IllegalArgumentException("File upload cannot be null"))
                .when(pwsUpdateDao).updateFileUploadStatus(null);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, 
                () -> service.updateFileUploadStatus(null));
            verify(pwsUpdateDao).updateFileUploadStatus(null);
        }

        @Test
        @DisplayName("Should handle DAO exception during file upload update")
        void shouldHandleDaoExceptionDuringFileUploadUpdate() {
            // Arrange
            PwsFileUpload fileUpload = new PwsFileUpload();
            doThrow(new RuntimeException("Database error"))
                .when(pwsUpdateDao).updateFileUploadStatus(fileUpload);

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> service.updateFileUploadStatus(fileUpload));
            verify(pwsUpdateDao).updateFileUploadStatus(fileUpload);
        }
    }

    @Nested
    @DisplayName("Transit Message Status Update Tests")
    class TransitMessageStatusUpdateTests {

        @Test
        @DisplayName("Should update transit message status successfully")
        void shouldUpdateTransitMessageStatusSuccessfully() {
            // Arrange
            PwsTransitMessage transitMessage = new PwsTransitMessage();
            transitMessage.setTransitMessageId(456L);
            transitMessage.setStatus("PROCESSED");

            // Act
            assertDoesNotThrow(() -> service.updateTransitMessageStatus(transitMessage));

            // Assert
            verify(pwsUpdateDao).updateTransitMessageStatus(transitMessage);
        }

        @Test
        @DisplayName("Should handle null transit message")
        void shouldHandleNullTransitMessage() {
            // Arrange
            doThrow(new IllegalArgumentException("Transit message cannot be null"))
                .when(pwsUpdateDao).updateTransitMessageStatus(null);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, 
                () -> service.updateTransitMessageStatus(null));
            verify(pwsUpdateDao).updateTransitMessageStatus(null);
        }

        @Test
        @DisplayName("Should handle DAO exception during transit message update")
        void shouldHandleDaoExceptionDuringTransitMessageUpdate() {
            // Arrange
            PwsTransitMessage transitMessage = new PwsTransitMessage();
            doThrow(new RuntimeException("Database error"))
                .when(pwsUpdateDao).updateTransitMessageStatus(transitMessage);

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> service.updateTransitMessageStatus(transitMessage));
            verify(pwsUpdateDao).updateTransitMessageStatus(transitMessage);
        }
    }
}
```
