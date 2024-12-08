
# StepAware

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StepAwareService Test Suite")
class StepAwareServiceTest {

    @Mock
    private JobExecution jobExecution;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private ExecutionContext jobContext;
    @Mock
    private ExecutionContext stepContext;
    @Mock
    private Logger log;

    private TestStepAwareService service;

    // Test implementation of abstract class
    private static class TestStepAwareService extends StepAwareService {
        @Override
        protected Logger getLogger() {
            return log;
        }
    }

    @BeforeEach
    void setUp() {
        service = new TestStepAwareService();
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
    }

    @Nested
    @DisplayName("Step Lifecycle Tests")
    class StepLifecycleTests {
        
        @Test
        @DisplayName("Should set step execution in beforeStep")
        void shouldSetStepExecutionInBeforeStep() {
            service.beforeStep(stepExecution);
            
            assertEquals(stepExecution, service.getStepExecution());
        }

        @Test
        @DisplayName("Should not throw exception in afterStep")
        void shouldNotThrowExceptionInAfterStep() {
            service.beforeStep(stepExecution);
            
            assertDoesNotThrow(() -> service.afterStep(stepExecution));
        }
    }

    @Nested
    @DisplayName("Context Access Tests")
    class ContextAccessTests {

        @BeforeEach
        void setupContext() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("Should return job execution")
        void shouldReturnJobExecution() {
            assertEquals(jobExecution, service.getJobExecution());
        }

        @Test
        @DisplayName("Should return job context")
        void shouldReturnJobContext() {
            assertEquals(jobContext, service.getJobContext());
        }

        @Test
        @DisplayName("Should return step context")
        void shouldReturnStepContext() {
            assertEquals(stepContext, service.getStepContext());
        }
    }

    @Nested
    @DisplayName("Route Configuration Tests")
    class RouteConfigTests {

        @BeforeEach
        void setupContext() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("Should get route configuration")
        void shouldGetRouteConfig() {
            BulkRoutesConfig.BulkRoute route = mock(BulkRoutesConfig.BulkRoute.class);
            when(jobContext.get(ContextKey.routeConfig, BulkRoutesConfig.BulkRoute.class))
                .thenReturn(route);
            when(route.getRouteName()).thenReturn("testRoute");

            assertEquals(route, service.getRouteConfig());
            assertEquals("testRoute", service.getRouteName());
        }
    }

    @Nested
    @DisplayName("Account Resource Tests")
    class AccountResourceTests {

        @BeforeEach
        void setupContext() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("Should handle account resource operations")
        void shouldHandleAccountResourceOperations() {
            Map<String, AccountResource> resources = new HashMap<>();
            when(jobContext.get(ContextKey.accountResources, Map.class))
                .thenReturn(resources);

            AccountResource resource = mock(AccountResource.class);
            String accountNum = "123456";

            service.setAccountResource(accountNum, resource);
            assertEquals(resource, service.getAccountResource(accountNum));
        }

        @Test
        @DisplayName("Should create new map when getting non-existent account resources")
        void shouldCreateNewMapWhenGettingNonExistentAccountResources() {
            when(jobContext.get(ContextKey.accountResources, Map.class))
                .thenReturn(null);

            AccountResource resource = mock(AccountResource.class);
            String accountNum = "123456";

            service.setAccountResource(accountNum, resource);
            verify(jobContext, times(2)).get(ContextKey.accountResources, Map.class);
        }

        @Test
        @DisplayName("Should throw NullPointerException for non-existent account")
        void shouldThrowNullPointerExceptionForNonExistentAccount() {
            Map<String, AccountResource> resources = new HashMap<>();
            when(jobContext.get(ContextKey.accountResources, Map.class))
                .thenReturn(resources);

            assertThrows(NullPointerException.class, 
                () -> service.getAccountResource("nonexistent"));
        }
    }

    @Nested
    @DisplayName("Debug Logging Tests")
    class DebugLoggingTests {

        @BeforeEach
        void setupContext() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("Should log debug information when debug enabled")
        void shouldLogDebugInformationWhenDebugEnabled() {
            when(log.isDebugEnabled()).thenReturn(true);
            
            // Setup mock returns for all getters
            BulkRoutesConfig.BulkRoute route = mock(BulkRoutesConfig.BulkRoute.class);
            when(route.getRouteName()).thenReturn("testRoute");
            when(jobContext.get(ContextKey.routeConfig, BulkRoutesConfig.BulkRoute.class))
                .thenReturn(route);
            
            Country country = mock(Country.class);
            country.countryCode = "US";
            when(jobContext.get(ContextKey.country, Country.class)).thenReturn(country);
            
            Channel channel = Channel.WEB;
            when(jobContext.get(ContextKey.channel, Channel.class)).thenReturn(channel);

            service.debugContext();

            verify(log, atLeastOnce()).debug(anyString(), any());
        }

        @Test
        @DisplayName("Should not log debug information when debug disabled")
        void shouldNotLogDebugInformationWhenDebugDisabled() {
            when(log.isDebugEnabled()).thenReturn(false);

            service.debugContext();

            verify(log, never()).debug(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Processing Status Tests")
    class ProcessingStatusTests {

        @BeforeEach
        void setupContext() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("Should update processing status")
        void shouldUpdateProcessingStatus() {
            Pain001InboundProcessingResult result = mock(Pain001InboundProcessingResult.class);
            when(jobContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(result);

            Pain001InboundProcessingStatus status = Pain001InboundProcessingStatus.SUCCESS;
            String message = "Processing completed";

            service.updateProcessingStatus(status, message);

            verify(result).setProcessingStatus(status);
            verify(result).setMessage(message);
        }
    }

    @Nested
    @DisplayName("Context Update Tests")
    class ContextUpdateTests {

        @BeforeEach
        void setupContext() {
            service.beforeStep(stepExecution);
        }

        @Test
        @DisplayName("Should update source format in step context")
        void shouldUpdateSourceFormatInStepContext() {
            String sourceFormat = "XML";
            service.setSourceFormat(sourceFormat);
            verify(stepContext).putString(ContextKey.sourceFormat, sourceFormat);
        }

        @Test
        @DisplayName("Should update company settings in job context")
        void shouldUpdateCompanySettingsInJobContext() {
            CompanySettings settings = mock(CompanySettings.class);
            service.setCompanySettings(settings);
            verify(jobContext).put(ContextKey.companySettings, settings);
        }

        @Test
        @DisplayName("Should update user ID in job context")
        void shouldUpdateUserIdInJobContext() {
            long userId = 123L;
            service.setUserId(userId);
            verify(jobContext).putLong(ContextKey.userId, userId);
        }
    }
}
```

# Pain001ProcessService

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Pain001ProcessServiceImpl Test Suite")
class Pain001ProcessServiceImplTest {

    @Mock
    private AppConfig appConfig;
    @Mock
    private PaymentMappingService paymentMappingService;
    @Mock
    private PaymentIntegrationservice paymentIntegrationservice;
    @Mock
    private PaymentQueryService paymentQueryService;
    @Mock
    private PaymentSaveService paymentSaveService;
    @Mock
    private AesQueryDao aesQueryDao;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext jobContext;
    @Mock
    private ExecutionContext stepContext;

    @InjectMocks
    private Pain001ProcessServiceImpl service;

    @BeforeEach
    void setUp() {
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        service.beforeStep(stepExecution);
    }

    @Nested
    @DisplayName("Process Pain001 Group Header Tests")
    class ProcessPain001GroupHeaderTests {
        
        private Pain001 pain001;
        private GroupHeaderDTO groupHeaderDTO;
        private BusinessDocument businessDocument;
        private CustomerCreditTransferInitiation creditTransferInitiation;

        @BeforeEach
        void setUp() {
            pain001 = mock(Pain001.class);
            groupHeaderDTO = mock(GroupHeaderDTO.class);
            businessDocument = mock(BusinessDocument.class);
            creditTransferInitiation = mock(CustomerCreditTransferInitiation.class);
            
            when(pain001.getBusinessDocument()).thenReturn(businessDocument);
            when(businessDocument.getCustomerCreditTransferInitiation()).thenReturn(creditTransferInitiation);
            when(creditTransferInitiation.getGroupHeader()).thenReturn(groupHeaderDTO);
            
            when(jobContext.get(eq(ContextKey.result), any())).thenReturn(new Pain001InboundProcessingResult());
        }

        @Test
        @DisplayName("Should reject when DMP file status is REJECTED")
        void shouldRejectWhenDmpFileStatusIsRejected() {
            when(groupHeaderDTO.getFilestatus()).thenReturn(DmpFileStatus.REJECTED.getValue());
            
            SourceProcessStatus status = service.processPain001GroupHeader(pain001);
            
            assertEquals(SourceReject, status.getStatus());
            assertEquals("File rejected by DMP", status.getMessage());
        }

        @Test
        @DisplayName("Should return error when file upload not found")
        void shouldReturnErrorWhenFileUploadNotFound() {
            when(groupHeaderDTO.getFilestatus()).thenReturn(DmpFileStatus.ACCEPTED.getValue());
            when(groupHeaderDTO.getFilereference()).thenReturn("REF123");
            when(paymentQueryService.getFileUpload(anyString())).thenReturn(null);
            
            SourceProcessStatus status = service.processPain001GroupHeader(pain001);
            
            assertEquals(SourceError, status.getStatus());
            assertTrue(status.getMessage().contains("Failed to find file upload"));
        }

        @Test
        @DisplayName("Should return error when user is not entitled")
        void shouldReturnErrorWhenUserIsNotEntitled() {
            when(groupHeaderDTO.getFilestatus()).thenReturn(DmpFileStatus.ACCEPTED.getValue());
            when(groupHeaderDTO.getFilereference()).thenReturn("REF123");
            
            PwsFileUpload fileUpload = mock(PwsFileUpload.class);
            when(paymentQueryService.getFileUpload(anyString())).thenReturn(fileUpload);
            
            UserResourceFeaturesActionsData resourceData = mock(UserResourceFeaturesActionsData.class);
            when(paymentIntegrationservice.getResourcesAndFeatures(anyLong())).thenReturn(resourceData);
            
            SourceProcessStatus status = service.processPain001GroupHeader(pain001);
            
            assertEquals(SourceError, status.getStatus());
            assertTrue(status.getMessage().contains("NO resource and feature entitlement"));
        }

        @Test
        @DisplayName("Should process successfully with valid inputs")
        void shouldProcessSuccessfullyWithValidInputs() {
            when(groupHeaderDTO.getFilestatus()).thenReturn(DmpFileStatus.ACCEPTED.getValue());
            when(groupHeaderDTO.getFilereference()).thenReturn("REF123");
            when(groupHeaderDTO.getFileformat()).thenReturn("XML");
            
            PwsFileUpload fileUpload = mock(PwsFileUpload.class);
            when(paymentQueryService.getFileUpload(anyString())).thenReturn(fileUpload);
            
            // Setup user entitlement
            UserResourceFeaturesActionsData resourceData = mockValidUserEntitlement(fileUpload);
            when(paymentIntegrationservice.getResourcesAndFeatures(anyLong())).thenReturn(resourceData);
            
            SourceProcessStatus status = service.processPain001GroupHeader(pain001);
            
            assertEquals(SourceOK, status.getStatus());
            assertEquals("Pain001 group header processed OK", status.getMessage());
            verify(stepContext).putString(eq(ContextKey.sourceFormat), eq("XML"));
        }
    }

    @Nested
    @DisplayName("Process Pre Pain001 BO Mapping Tests")
    class ProcessPrePain001BoMappingTests {

        private Pain001 pain001;
        private BusinessDocument businessDocument;
        private CustomerCreditTransferInitiation creditTransferInitiation;
        private PaymentInformationDTO paymentInfo;
        private DebtorDTO debtor;

        @BeforeEach
        void setUp() {
            pain001 = mock(Pain001.class);
            businessDocument = mock(BusinessDocument.class);
            creditTransferInitiation = mock(CustomerCreditTransferInitiation.class);
            paymentInfo = mock(PaymentInformationDTO.class);
            debtor = mock(DebtorDTO.class);
            
            when(pain001.getBusinessDocument()).thenReturn(businessDocument);
            when(businessDocument.getCustomerCreditTransferInitiation()).thenReturn(creditTransferInitiation);
            when(creditTransferInitiation.getPaymentInformation()).thenReturn(Collections.singletonList(paymentInfo));
            when(paymentInfo.getDebtor()).thenReturn(debtor);
        }

        @Test
        @DisplayName("Should process company id from file upload")
        void shouldProcessCompanyIdFromFileUpload() {
            PwsFileUpload fileUpload = mock(PwsFileUpload.class);
            when(fileUpload.getCompanyId()).thenReturn(123L);
            when(jobContext.get(eq(ContextKey.fileUpload), any())).thenReturn(fileUpload);
            when(appConfig.getUploadSourceFormatNoCompany()).thenReturn(Collections.emptyList());
            
            SourceProcessStatus status = service.processPrePain001BoMapping(pain001);
            
            assertEquals(SourceOK, status.getStatus());
            verify(jobContext).putLong(eq(ContextKey.companyId), eq(123L));
        }

        @Test
        @DisplayName("Should process company id from debtor name")
        void shouldProcessCompanyIdFromDebtorName() {
            when(appConfig.getUploadSourceFormatNoCompany()).thenReturn(Collections.singletonList("XML"));
            when(jobContext.getString(ContextKey.sourceFormat)).thenReturn("XML");
            when(debtor.getName()).thenReturn("Test Company");
            when(aesQueryDao.getCompanyIdFromName("Test Company")).thenReturn(456L);
            
            SourceProcessStatus status = service.processPrePain001BoMapping(pain001);
            
            assertEquals(SourceOK, status.getStatus());
            verify(jobContext).putLong(eq(ContextKey.companyId), eq(456L));
        }
    }

    @Nested
    @DisplayName("Process Pain001 BO Mapping Tests")
    class ProcessPain001BoMappingTests {
        
        private Pain001 pain001;
        private GroupHeaderDTO groupHeaderDTO;
        private BusinessDocument businessDocument;
        private CustomerCreditTransferInitiation creditTransferInitiation;
        private PaymentInformationDTO paymentInfoDTO;

        @BeforeEach
        void setUp() {
            pain001 = mock(Pain001.class);
            groupHeaderDTO = mock(GroupHeaderDTO.class);
            businessDocument = mock(BusinessDocument.class);
            creditTransferInitiation = mock(CustomerCreditTransferInitiation.class);
            paymentInfoDTO = mock(PaymentInformationDTO.class);
            
            when(pain001.getBusinessDocument()).thenReturn(businessDocument);
            when(businessDocument.getCustomerCreditTransferInitiation()).thenReturn(creditTransferInitiation);
            when(creditTransferInitiation.getGroupHeader()).thenReturn(groupHeaderDTO);
            when(creditTransferInitiation.getPaymentInformation())
                .thenReturn(Collections.singletonList(paymentInfoDTO));
            
            when(jobContext.get(eq(ContextKey.result), any()))
                .thenReturn(new Pain001InboundProcessingResult());
        }

        @Test
        @DisplayName("Should process payment information successfully")
        void shouldProcessPaymentInformationSuccessfully() {
            PaymentInformation paymentInfo = mock(PaymentInformation.class);
            when(paymentMappingService.pain001PaymentToBo(any())).thenReturn(paymentInfo);
            when(paymentMappingService.postMappingPain001ToBo(any(), any()))
                .thenReturn(Collections.singletonList(paymentInfo));
            when(groupHeaderDTO.getControlSum()).thenReturn("1000.00");
            when(groupHeaderDTO.getNumberOfTransactions()).thenReturn("1");
            
            List<PaymentInformation> result = service.processPain001BoMapping(pain001);
            
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(paymentMappingService).pain001PaymentToBo(paymentInfoDTO);
            verify(paymentMappingService).postMappingPain001ToBo(pain001, Collections.singletonList(paymentInfo));
        }

        @Test
        @DisplayName("Should handle rejected payments")
        void shouldHandleRejectedPayments() {
            PaymentInformation paymentInfo = mock(PaymentInformation.class);
            when(paymentInfo.isDmpRejected()).thenReturn(true);
            when(paymentMappingService.pain001PaymentToBo(any())).thenReturn(paymentInfo);
            when(paymentMappingService.postMappingPain001ToBo(any(), any()))
                .thenReturn(Collections.singletonList(paymentInfo));
            
            List<PaymentInformation> result = service.processPain001BoMapping(pain001);
            
            assertTrue(result.isEmpty());
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }
    }

    private UserResourceFeaturesActionsData mockValidUserEntitlement(PwsFileUpload fileUpload) {
        UserResourceFeaturesActionsData resourceData = mock(UserResourceFeaturesActionsData.class);
        UserResourceAndFeatureAccess access = mock(UserResourceAndFeatureAccess.class);
        Resource resource = mock(Resource.class);
        Feature feature = mock(Feature.class);
        
        when(resourceData.getUserResourceAndFeatureAccess()).thenReturn(access);
        when(access.getResources()).thenReturn(Collections.singletonList(resource));
        when(resource.getResourceId()).thenReturn("RESOURCE_ID");
        when(resource.getFeatures()).thenReturn(Collections.singletonList(feature));
        when(feature.getFeatureId()).thenReturn("FEATURE_ID");
        when(resource.getActions()).thenReturn(Collections.singleton(ACTION_CREATE));
        
        when(fileUpload.getResourceId()).thenReturn("RESOURCE_ID");
        when(fileUpload.getFeatureId()).thenReturn("FEATURE_ID");
        
        return resourceData;
    }
}
```

# Pain001ServiceImpl

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Pain001ServiceImpl Test Suite")
class Pain001ServiceImplTest {

    @Mock
    private AppConfig appConfig;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PaymentUtils paymentUtils;
    @Mock
    private Pain001ProcessService pain001ProcessService;
    @Mock
    private PaymentEnrichmentService paymentEnrichmentService;
    @Mock
    private PaymentValidationService paymentValidationService;
    @Mock
    private PaymentDebulkService paymentDebulkService;
    @Mock
    private PaymentSaveService paymentSaveService;
    @Mock
    private PaymentUpdateService paymentUpdateService;
    @Mock
    private PaymentDeleteService paymentDeleteService;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext jobContext;
    @Mock
    private ExecutionContext stepContext;

    @InjectMocks
    private Pain001ServiceImpl service;

    @BeforeEach
    void setUp() {
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(jobContext);
        when(stepExecution.getExecutionContext()).thenReturn(stepContext);
        when(jobContext.get(eq(ContextKey.result), any())).thenReturn(new Pain001InboundProcessingResult());
        service.beforeStep(stepExecution);
    }

    @Nested
    @DisplayName("Process Pain001 Tests")
    class ProcessPain001Tests {

        @Test
        @DisplayName("Should handle JSON parsing error")
        void shouldHandleJsonParsingError() throws Exception {
            String json = "invalid json";
            when(objectMapper.readValue(json, Pain001.class)).thenThrow(new JsonProcessingException("Parse error") {});

            List<PaymentInformation> result = service.processPain001(json);

            assertNull(result);
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }

        @Test
        @DisplayName("Should handle group header processing error")
        void shouldHandleGroupHeaderProcessingError() throws Exception {
            String json = "valid json";
            Pain001 pain001 = mock(Pain001.class);
            when(objectMapper.readValue(json, Pain001.class)).thenReturn(pain001);
            when(pain001ProcessService.processPain001GroupHeader(pain001))
                .thenReturn(new SourceProcessStatus(SourceError, "Header error"));

            List<PaymentInformation> result = service.processPain001(json);

            assertNull(result);
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }

        @Test
        @DisplayName("Should handle empty payment list")
        void shouldHandleEmptyPaymentList() throws Exception {
            String json = "valid json";
            Pain001 pain001 = mock(Pain001.class);
            when(objectMapper.readValue(json, Pain001.class)).thenReturn(pain001);
            when(pain001ProcessService.processPain001GroupHeader(pain001))
                .thenReturn(new SourceProcessStatus(SourceOK, "OK"));
            when(pain001ProcessService.processPrePain001BoMapping(pain001))
                .thenReturn(new SourceProcessStatus(SourceOK, "OK"));
            when(pain001ProcessService.processPain001BoMapping(pain001))
                .thenReturn(Collections.emptyList());

            List<PaymentInformation> result = service.processPain001(json);

            assertNull(result);
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }

        @Test
        @DisplayName("Should process successfully")
        void shouldProcessSuccessfully() throws Exception {
            String json = "valid json";
            Pain001 pain001 = mock(Pain001.class);
            PaymentInformation payment = mock(PaymentInformation.class);
            when(objectMapper.readValue(json, Pain001.class)).thenReturn(pain001);
            when(pain001ProcessService.processPain001GroupHeader(pain001))
                .thenReturn(new SourceProcessStatus(SourceOK, "OK"));
            when(pain001ProcessService.processPrePain001BoMapping(pain001))
                .thenReturn(new SourceProcessStatus(SourceOK, "OK"));
            when(pain001ProcessService.processPain001BoMapping(pain001))
                .thenReturn(Collections.singletonList(payment));
            when(pain001ProcessService.processPostPain001BoMapping(pain001))
                .thenReturn(new SourceProcessStatus(SourceOK, "OK"));
            when(payment.isDmpRejected()).thenReturn(false);

            List<PaymentInformation> result = service.processPain001(json);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(paymentSaveService).createTransitMessage(any(PwsTransitMessage.class));
        }
    }

    @Nested
    @DisplayName("Validation Process Tests")
    class ValidationProcessTests {

        private List<PaymentInformation> payments;
        private PaymentInformation payment;

        @BeforeEach
        void setUp() {
            payment = mock(PaymentInformation.class);
            payments = Collections.singletonList(payment);
        }

        @Test
        @DisplayName("Should handle entitlement validation failure")
        void shouldHandleEntitlementValidationFailure() {
            when(paymentValidationService.doEntitlementValidation(payments)).thenReturn(payments);
            when(payment.isEntitled()).thenReturn(false);

            List<PaymentInformation> result = service.entitlementValidation(payments);

            assertTrue(result.isEmpty());
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }

        @Test
        @DisplayName("Should handle pre-transaction validation failure")
        void shouldHandlePreTransactionValidationFailure() {
            when(paymentEnrichmentService.enrichPreTransactionValidation(payments)).thenReturn(payments);
            when(paymentValidationService.doPreTransactionValidation(payments)).thenReturn(payments);
            when(payment.hasNoValidationError()).thenReturn(false);

            List<PaymentInformation> result = service.preTransactionValidation(payments);

            assertTrue(result.isEmpty());
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }

        @Test
        @DisplayName("Should handle transaction validation with invalid transactions")
        void shouldHandleTransactionValidationWithInvalidTransactions() {
            CreditTransferTransaction validTxn = mock(CreditTransferTransaction.class);
            CreditTransferTransaction invalidTxn = mock(CreditTransferTransaction.class);
            List<CreditTransferTransaction> transactions = Arrays.asList(validTxn, invalidTxn);

            when(payment.getCreditTransferTransactionList()).thenReturn(transactions);
            when(validTxn.hasNoValidationError()).thenReturn(true);
            when(invalidTxn.hasNoValidationError()).thenReturn(false);
            when(paymentValidationService.doTransactionValidation(payments)).thenReturn(payments);

            List<PaymentInformation> result = service.transactionValidation(payments);

            verify(payment).setCreditTransferTransactionList(argThat(list -> list.size() == 1 && list.contains(validTxn)));
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }
    }

    @Nested
    @DisplayName("Save Process Tests")
    class SaveProcessTests {

        @Test
        @DisplayName("Should handle save exceptions")
        void shouldHandleSaveExceptions() {
            PaymentInformation payment = mock(PaymentInformation.class);
            when(payment.isValid()).thenReturn(false);
            doThrow(new RuntimeException("Save error")).when(paymentSaveService).savePaymentInformation(payment);

            service.save(Collections.singletonList(payment));

            verify(paymentDeleteService).deletePaymentInformation(payment);
            verify(paymentUtils).createPwsSaveRecord(any(), any());
            verify(paymentUtils).updatePaymentSavedError(any(), any());
        }

        @Test
        @DisplayName("Should update file upload status on success")
        void shouldUpdateFileUploadStatusOnSuccess() {
            Pain001InboundProcessingResult result = getResult();
            result.setPaymentDebulkTotal(1);
            result.setPaymentCreatedTotal(1);
            result.setTransactionReceivedTotal(1);
            result.setTransactionCreatedTotal(1);

            PaymentInformation payment = mock(PaymentInformation.class);
            when(payment.isValid()).thenReturn(false);
            PwsFileUpload fileUpload = mock(PwsFileUpload.class);
            when(jobContext.get(ContextKey.fileUpload, PwsFileUpload.class)).thenReturn(fileUpload);

            service.save(Collections.singletonList(payment));

            verify(paymentUpdateService).updateFileUploadStatus(argThat(fu -> 
                fu.getStatus().equals(FileUploadStatus.SUCCESS.value)));
        }
    }

    @Nested
    @DisplayName("Transit Message Tests")
    class TransitMessageTests {

        @Test
        @DisplayName("Should create transit message")
        void shouldCreateTransitMessage() {
            String fileRef = "REF123";
            String status = "STATUS";

            PwsTransitMessage result = service.createTransitMessage(fileRef, status);

            assertNotNull(result);
            assertEquals(fileRef, result.getBankReferenceId());
            assertEquals(status, result.getStatus());
            assertEquals(SERVICE_INBOUND, result.getServiceType());
            assertEquals(SYSTEM_DMP, result.getEndSystem());
            verify(paymentSaveService).createTransitMessage(result);
        }

        @Test
        @DisplayName("Should update transit message status")
        void shouldUpdateTransitMessageStatus() {
            Pain001InboundProcessingResult result = getResult();
            result.setProcessingStatus(Pain001InboundProcessingStatus.SavePassed);
            PwsTransitMessage transitMessage = mock(PwsTransitMessage.class);
            when(jobContext.get(ContextKey.transitMessage, PwsTransitMessage.class)).thenReturn(transitMessage);

            service.updateTransitMessage(result);

            verify(transitMessage).setStatus(Pain001InboundProcessingStatus.SavePassed.name());
            verify(paymentUpdateService).updateTransitMessageStatus(transitMessage);
        }
    }

    private Pain001InboundProcessingResult getResult() {
        return (Pain001InboundProcessingResult) jobContext.get(ContextKey.result, Pain001InboundProcessingResult.class);
    }
}
```

# debulk

```java
@ExtendWith(MockitoExtension.class)
class PaymentDebulkServiceImplTHTest {

    @InjectMocks
    private PaymentDebulkServiceImplTH paymentDebulkServiceImplTH;

    @Mock
    private AppConfig appConfig;

    @Mock
    private PwsTransactions pwsTransactions;

    @Mock
    private PaymentInformation paymentInformation;

    @Mock
    private CreditTransferTransaction creditTransferTransaction;

    @Mock
    private PwsBulkTransaction pwsBulkTransaction;

    @Mock
    private BigDecimal transactionAmount;

    @BeforeEach
    void setUp() {
        // Setup mock data for tests
        when(paymentInformation.isDmpRejected()).thenReturn(false);
        when(paymentInformation.getPwsTransactions()).thenReturn(pwsTransactions);
        when(paymentInformation.getCreditTransferTransactionList()).thenReturn(List.of(creditTransferTransaction));
    }

    @Test
    void testDebulk_SmartPayment() {
        // Setup mock for SMART payment
        when(pwsTransactions.getResourceId()).thenReturn(Resource.SMART.id);
        when(creditTransferTransaction.getPwsBulkTransactionInstructions()).thenReturn(pwsBulkTransaction);
        when(pwsBulkTransaction.getTransactionAmount()).thenReturn(new BigDecimal("1000"));

        // Setup appConfig mock
        DebulkConfig debulkConfig = mock(DebulkConfig.class);
        DebulkSmart debulkSmart = mock(DebulkSmart.class);
        when(appConfig.getDebulk()).thenReturn(debulkConfig);
        when(debulkConfig.getDebulkSmart()).thenReturn(debulkSmart);
        when(debulkSmart.getBankCode()).thenReturn("bankCode");
        when(debulkSmart.getBahtnetThreshold()).thenReturn(new BigDecimal("500"));
        when(debulkSmart.getSourceFormat()).thenReturn(List.of("sourceFormat"));

        List<PaymentInformation> result = paymentDebulkServiceImplTH.debulk(List.of(paymentInformation));

        assertThat(result).isNotEmpty();
        verify(paymentInformation).getPwsTransactions();
        verify(creditTransferTransaction).getPwsBulkTransactionInstructions();
    }

    @Test
    void testDebulk_SmartPayment_InvalidResourceId() {
        // Setup for invalid resourceId (non-SMART payment)
        when(pwsTransactions.getResourceId()).thenReturn("OTHER");
        
        List<PaymentInformation> result = paymentDebulkServiceImplTH.debulk(List.of(paymentInformation));

        assertThat(result).isNotEmpty();
        verify(paymentInformation).getPwsTransactions();
        verify(creditTransferTransaction, never()).getPwsBulkTransactionInstructions();
    }

    @Test
    void testDebulk_SmartPayment_WithGroupedTransactions() {
        // Setup for SMART payment
        when(pwsTransactions.getResourceId()).thenReturn(Resource.SMART.id);
        when(creditTransferTransaction.getPwsBulkTransactionInstructions()).thenReturn(pwsBulkTransaction);
        when(pwsBulkTransaction.getTransactionAmount()).thenReturn(new BigDecimal("1000"));

        // Setup appConfig mock
        DebulkConfig debulkConfig = mock(DebulkConfig.class);
        DebulkSmart debulkSmart = mock(DebulkSmart.class);
        when(appConfig.getDebulk()).thenReturn(debulkConfig);
        when(debulkConfig.getDebulkSmart()).thenReturn(debulkSmart);
        when(debulkSmart.getBankCode()).thenReturn("bankCode");
        when(debulkSmart.getBahtnetThreshold()).thenReturn(new BigDecimal("500"));
        when(debulkSmart.getSourceFormat()).thenReturn(List.of("sourceFormat"));

        // Test if grouped transactions are handled correctly
        List<CreditTransferTransaction> groupedTxns = new ArrayList<>();
        groupedTxns.add(creditTransferTransaction);

        // Create a mock for createNewPaymentInfo method
        PaymentInformation newPaymentInfo = mock(PaymentInformation.class);
        when(paymentDebulkServiceImplTH.createNewPaymentInfo(any(), any(), any())).thenReturn(newPaymentInfo);

        List<PaymentInformation> result = paymentDebulkServiceImplTH.debulk(List.of(paymentInformation));

        assertThat(result).contains(newPaymentInfo);
    }

    @Test
    void testDetermineResourceIdForSmart() {
        // Setup appConfig mock
        DebulkConfig debulkConfig = mock(DebulkConfig.class);
        DebulkSmart debulkSmart = mock(DebulkSmart.class);
        when(appConfig.getDebulk()).thenReturn(debulkConfig);
        when(debulkConfig.getDebulkSmart()).thenReturn(debulkSmart);
        when(debulkSmart.getBankCode()).thenReturn("bankCode");
        when(debulkSmart.getBahtnetThreshold()).thenReturn(new BigDecimal("500"));
        when(debulkSmart.getSourceFormat()).thenReturn(List.of("sourceFormat"));

        // Test when amount is greater than threshold
        String result = paymentDebulkServiceImplTH.determineResourceIdForSmart("bankCode", new BigDecimal("1000"));
        assertThat(result).isEqualTo(Resource.BAHTNET.id);

        // Test when source format contains current format
        when(paymentDebulkServiceImplTH.getSourceFormat()).thenReturn("sourceFormat");
        result = paymentDebulkServiceImplTH.determineResourceIdForSmart("bankCode", new BigDecimal("100"));
        assertThat(result).isEqualTo(Resource.SMART_NEXT_DAY.id);

        // Test default case
        result = paymentDebulkServiceImplTH.determineResourceIdForSmart("otherBank", new BigDecimal("100"));
        assertThat(result).isEqualTo(Resource.SMART_SAME_DAY.id);
    }

    @Test
    void testCreateNewPaymentInfo() {
        // Create mock data for the method
        List<CreditTransferTransaction> txns = new ArrayList<>();
        PaymentInformation newPaymentInfo = paymentDebulkServiceImplTH.createNewPaymentInfo(paymentInformation, "resourceId", txns);

        assertThat(newPaymentInfo).isNotNull();
        assertThat(newPaymentInfo.getCreditTransferTransactionList()).isEqualTo(txns);
    }

}
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
