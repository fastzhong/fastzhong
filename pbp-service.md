
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


# Pain001InboundService

```java
package com.example.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentProcessingServiceTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private Pain001ProcessService pain001ProcessService;
    @Mock private PaymentDebulkService paymentDebulkService;
    @Mock private PaymentEnrichmentService paymentEnrichmentService;
    @Mock private PaymentValidationService paymentValidationService;
    @Mock private PaymentSaveService paymentSaveService;
    @Mock private PaymentDeleteService paymentDeleteService;
    @Mock private PaymentUpdateService paymentUpdateService;
    @Mock private PaymentUtils paymentUtils;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    @Mock private JsonParser jsonParser;
    
    private PaymentProcessingService service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new PaymentProcessingService(
            objectMapper, pain001ProcessService, paymentDebulkService,
            paymentEnrichmentService, paymentValidationService,
            paymentSaveService, paymentDeleteService, paymentUpdateService,
            paymentUtils
        );
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Pain001 Processing Tests")
    class Pain001ProcessingTests {
        
        @Test
        @DisplayName("Should process valid Pain001 file")
        void processPain001_WithValidFile_ShouldProcess() throws Exception {
            // Arrange
            File jsonFile = mock(File.class);
            GroupHeaderDTO groupHeader = new GroupHeaderDTO();
            List<PaymentInformation> paymentInfos = Arrays.asList(
                new PaymentInformation(), new PaymentInformation()
            );
            
            when(objectMapper.getFactory().createParser(jsonFile)).thenReturn(jsonParser);
            when(pain001ProcessService.processPain001GroupHeader(any()))
                .thenReturn(new SourceProcessStatus(SourceProcessStatus.Result.Success, ""));
            when(pain001ProcessService.processPain001BoMappingValidation(paymentInfos))
                .thenReturn(new SourceProcessStatus(SourceProcessStatus.Result.Success, ""));
            
            // Act
            List<PaymentInformation> result = service.processPain001(jsonFile);

            // Assert
            assertNotNull(result);
            verify(pain001ProcessService).processPain001GroupHeader(any());
            verify(pain001ProcessService).processPain001BoMappingValidation(paymentInfos);
        }

        @Test
        @DisplayName("Should handle parse error")
        void processPain001_WithParseError_ShouldHandleError() throws Exception {
            // Arrange
            File jsonFile = mock(File.class);
            when(objectMapper.getFactory().createParser(jsonFile))
                .thenThrow(new IOException("Parse error"));

            // Act
            List<PaymentInformation> result = service.processPain001(jsonFile);

            // Assert
            assertNull(result);
            verify(paymentSaveService).createRejectedRecord(any(PwsRejectedRecord.class));
        }
    }

    @Nested
    @DisplayName("Payment Debulking Tests")
    class PaymentDebulkingTests {
        
        @Test
        @DisplayName("Should debulk valid payments")
        void debulk_WithValidPayments_ShouldDebulk() {
            // Arrange
            List<PaymentInformation> payments = Arrays.asList(
                new PaymentInformation(), new PaymentInformation()
            );
            List<PaymentInformation> debulkedPayments = Arrays.asList(
                new PaymentInformation(), new PaymentInformation(), new PaymentInformation()
            );
            
            when(paymentDebulkService.debulk(payments)).thenReturn(debulkedPayments);
            when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(new Pain001InboundProcessingResult());

            // Act
            List<PaymentInformation> result = service.debulk(payments);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            verify(paymentDebulkService).debulk(payments);
        }
    }

    @Nested
    @DisplayName("Payment Validation Tests")
    class PaymentValidationTests {
        
        @Test
        @DisplayName("Should validate payments through all stages")
        void validate_WithValidPayments_ShouldValidate() {
            // Arrange
            List<PaymentInformation> payments = Arrays.asList(
                createValidPayment(), createValidPayment()
            );
            
            when(paymentValidationService.doEntitlementValidation(any()))
                .thenReturn(payments);
            when(paymentValidationService.doPreTransactionValidation(any()))
                .thenReturn(payments);
            when(paymentValidationService.doTransactionValidation(any()))
                .thenReturn(payments);
            when(paymentValidationService.doPostTransactionValidation(any()))
                .thenReturn(payments);
            when(paymentValidationService.doDuplicationValidation(any()))
                .thenReturn(payments);

            // Act
            List<PaymentInformation> result = service.validate(payments);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(paymentValidationService).doEntitlementValidation(any());
            verify(paymentValidationService).doPreTransactionValidation(any());
            verify(paymentValidationService).doTransactionValidation(any());
            verify(paymentValidationService).doPostTransactionValidation(any());
            verify(paymentValidationService).doDuplicationValidation(any());
        }

        @Test
        @DisplayName("Should handle validation failures")
        void validate_WithInvalidPayments_ShouldReject() {
            // Arrange
            List<PaymentInformation> payments = Arrays.asList(
                createInvalidPayment(), createInvalidPayment()
            );
            
            when(paymentValidationService.doEntitlementValidation(any()))
                .thenReturn(payments);
            when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(new Pain001InboundProcessingResult());

            // Act
            List<PaymentInformation> result = service.validate(payments);

            // Assert
            assertNull(result);
            verify(paymentSaveService, times(2)).createRejectedRecord(any());
        }
    }

    @Nested
    @DisplayName("Payment Save Tests")
    class PaymentSaveTests {
        
        @Test
        @DisplayName("Should save valid payments")
        void save_WithValidPayments_ShouldSave() {
            // Arrange
            PaymentInformation validPayment = createValidPayment();
            validPayment.setValid(true);
            List<PaymentInformation> payments = Arrays.asList(validPayment);
            
            when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(new Pain001InboundProcessingResult());

            // Act
            List<PaymentInformation> result = service.save(payments);

            // Assert
            assertNotNull(result);
            verify(paymentSaveService).savePaymentInformation(validPayment);
            verify(paymentUpdateService).updateFileUploadStatus(any());
        }

        @Test
        @DisplayName("Should handle save errors")
        void save_WithSaveError_ShouldHandleError() {
            // Arrange
            PaymentInformation payment = createValidPayment();
            payment.setValid(true);
            List<PaymentInformation> payments = Arrays.asList(payment);
            
            doThrow(new RuntimeException("Save error"))
                .when(paymentSaveService).savePaymentInformation(any());
            when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
                .thenReturn(new Pain001InboundProcessingResult());

            // Act
            List<PaymentInformation> result = service.save(payments);

            // Assert
            assertNotNull(result);
            verify(paymentDeleteService).deleteSavedPaymentInformation(payment);
            verify(paymentUtils).updatePaymentSavedError(any(), any());
        }
    }

    private PaymentInformation createValidPayment() {
        PaymentInformation payment = new PaymentInformation();
        payment.setEntitled(true);
        payment.setValid(true);
        payment.setPwsTransactions(new PwsTransactions());
        payment.setPwsBulkTransactions(new PwsBulkTransactions());
        return payment;
    }

    private PaymentInformation createInvalidPayment() {
        PaymentInformation payment = new PaymentInformation();
        payment.setEntitled(false);
        payment.setValid(false);
        payment.setPwsTransactions(new PwsTransactions());
        payment.setPwsBulkTransactions(new PwsBulkTransactions());
        return payment;
    }
}
```

# Pain001Processing

```java
package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Pain001ProcessServiceImplTest {

    @Mock private AppConfig appConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private PaymentMappingService paymentMappingService;
    @Mock private PaymentIntegrationservice paymentIntegrationservice;
    @Mock private PaymentQueryService paymentQueryService;
    @Mock private PaymentSaveService paymentSaveService;
    @Mock private AesQueryDao aesQueryDao;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    
    private Pain001ProcessServiceImpl service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new Pain001ProcessServiceImpl(
            appConfig,
            objectMapper,
            paymentMappingService,
            paymentIntegrationservice,
            paymentQueryService,
            paymentSaveService,
            aesQueryDao
        );
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
            .thenReturn(new Pain001InboundProcessingResult());
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Group Header Processing Tests")
    class GroupHeaderProcessingTests {
        
        @Test
        @DisplayName("Should process valid group header")
        void processPain001GroupHeader_WithValidHeader_ShouldProcess() {
            // Arrange
            GroupHeaderDTO header = new GroupHeaderDTO();
            header.setFilestatus(DmpFileStatus.APPROVED.value);
            header.setFilereference("REF123");
            header.setFileformat("JSON");
            header.setControlSum("1000.00");
            header.setNumberOfTransactions("5");
            
            PwsFileUpload fileUpload = new PwsFileUpload();
            fileUpload.setCreatedBy("123");
            fileUpload.setCompanyId(456L);
            fileUpload.setResourceId("RES001");
            fileUpload.setFeatureId("FEAT001");
            
            when(paymentQueryService.getFileUpload("REF123")).thenReturn(fileUpload);
            when(paymentIntegrationservice.getResourcesAndFeatures(anyLong()))
                .thenReturn(createValidResourceFeaturesData());

            // Act
            SourceProcessStatus result = service.processPain001GroupHeader(header);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, result.getResult());
            verify(paymentQueryService).getFileUpload("REF123");
            verify(paymentIntegrationservice).getResourcesAndFeatures(anyLong());
        }

        @Test
        @DisplayName("Should reject file with rejected status")
        void processPain001GroupHeader_WithRejectedStatus_ShouldReject() {
            // Arrange
            GroupHeaderDTO header = new GroupHeaderDTO();
            header.setFilestatus(DmpFileStatus.REJECTED.value);

            // Act
            SourceProcessStatus result = service.processPain001GroupHeader(header);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceReject, result.getResult());
            assertTrue(result.getMessage().contains("File rejected by DMP"));
        }

        @Test
        @DisplayName("Should handle missing file upload")
        void processPain001GroupHeader_WithMissingFileUpload_ShouldError() {
            // Arrange
            GroupHeaderDTO header = new GroupHeaderDTO();
            header.setFilestatus(DmpFileStatus.APPROVED.value);
            header.setFilereference("REF123");
            
            when(paymentQueryService.getFileUpload("REF123")).thenReturn(null);

            // Act
            SourceProcessStatus result = service.processPain001GroupHeader(header);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceError, result.getResult());
            assertTrue(result.getMessage().contains("Failed to find file upload"));
        }
    }

    @Nested
    @DisplayName("Pre-Mapping Processing Tests")
    class PreMappingProcessingTests {
        
        @Test
        @DisplayName("Should process valid pre-mapping")
        void processPrePain001BoMapping_WithValidData_ShouldProcess() {
            // Arrange
            GroupHeaderDTO header = new GroupHeaderDTO();
            String debtorName = "TestDebtor";
            
            when(aesQueryDao.getCompanyIdFromName(debtorName)).thenReturn(123L);
            when(aesQueryDao.getBatchBookingIndicator(anyLong()))
                .thenReturn(BatchBookingIndicator.ITEMIZED.name());
            when(paymentQueryService.getResourceConfig(any()))
                .thenReturn("100");

            // Act
            SourceProcessStatus result = service.processPrePain001BoMapping(header, debtorName);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, result.getResult());
            verify(aesQueryDao).getBatchBookingIndicator(anyLong());
            verify(paymentQueryService).getResourceConfig(any());
        }

        @Test
        @DisplayName("Should handle missing company settings")
        void processPrePain001BoMapping_WithMissingSettings_ShouldUseDefaults() {
            // Arrange
            GroupHeaderDTO header = new GroupHeaderDTO();
            String debtorName = "TestDebtor";
            
            when(aesQueryDao.getBatchBookingIndicator(anyLong())).thenReturn(null);

            // Act
            SourceProcessStatus result = service.processPrePain001BoMapping(header, debtorName);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, result.getResult());
            verify(aesQueryDao).getBatchBookingIndicator(anyLong());
        }
    }

    @Nested
    @DisplayName("Mapping Validation Tests")
    class MappingValidationTests {
        
        @Test
        @DisplayName("Should validate correct resource and feature IDs")
        void processPain001BoMappingValidation_WithValidIds_ShouldValidate() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfos(true);
            mockFileUploadWithIds("RES001", "FEAT001");

            // Act
            SourceProcessStatus result = service.processPain001BoMappingValidation(payments);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, result.getResult());
        }

        @Test
        @DisplayName("Should reject invalid resource and feature IDs")
        void processPain001BoMappingValidation_WithInvalidIds_ShouldReject() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfos(false);
            mockFileUploadWithIds("RES001", "FEAT001");

            // Act
            SourceProcessStatus result = service.processPain001BoMappingValidation(payments);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceError, result.getResult());
            assertTrue(result.getMessage().contains("Invalid resourceId & featureId"));
        }
    }

    @Nested
    @DisplayName("Post-Mapping Processing Tests")
    class PostMappingProcessingTests {
        
        @Test
        @DisplayName("Should process post-mapping successfully")
        void processPostPain001BoMapping_ShouldSetupContext() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfos(true);
            AppConfig.BulkRoute route = new AppConfig.BulkRoute();
            when(executionContext.get(eq(ContextKey.routeConfig), any()))
                .thenReturn(route);

            // Act
            SourceProcessStatus result = service.processPostPain001BoMapping(payments);

            // Assert
            assertEquals(SourceProcessStatus.Result.SourceOK, result.getResult());
            verify(executionContext).put(eq(ContextKey.accountResources), any(HashMap.class));
        }
    }

    private UserResourceFeaturesActionsData createValidResourceFeaturesData() {
        UserResourceFeaturesActionsData data = new UserResourceFeaturesActionsData();
        UserResourceAndFeatureAccess access = new UserResourceAndFeatureAccess();
        List<Resource> resources = new ArrayList<>();
        Resource resource = new Resource();
        resource.setResourceId("RES001");
        
        Feature feature = new Feature();
        feature.setFeatureId("FEAT001");
        resource.setFeatures(Arrays.asList(feature));
        
        resource.setActions(Arrays.asList("CREATE"));
        resources.add(resource);
        access.setResources(resources);
        data.setUserResourceAndFeatureAccess(access);
        return data;
    }

    private List<PaymentInformation> createPaymentInfos(boolean validIds) {
        List<PaymentInformation> payments = new ArrayList<>();
        PaymentInformation payment = new PaymentInformation();
        
        PwsTransactions pwsTransactions = new PwsTransactions();
        if (validIds) {
            pwsTransactions.setResourceId("RES001");
            pwsTransactions.setFeatureId("FEAT001");
        } else {
            pwsTransactions.setResourceId("INVALID");
            pwsTransactions.setFeatureId("INVALID");
        }
        payment.setPwsTransactions(pwsTransactions);
        
        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        pwsBulkTransactions.setDmpBatchNumber("BATCH001");
        payment.setPwsBulkTransactions(pwsBulkTransactions);
        
        payments.add(payment);
        return payments;
    }

    private void mockFileUploadWithIds(String resourceId, String featureId) {
        PwsFileUpload fileUpload = new PwsFileUpload();
        fileUpload.setResourceId(resourceId);
        fileUpload.setFeatureId(featureId);
        when(executionContext.get(ContextKey.fileUpload, PwsFileUpload.class))
            .thenReturn(fileUpload);
    }
}
```

# mapping

```java
package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentMappingServiceTest {

    @Mock private Pain001ToBoMapper pain001ToBoMapper;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    
    private PaymentMappingService service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new PaymentMappingService(pain001ToBoMapper);
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.get(ContextKey.bankEntity, BankEntity.class))
            .thenReturn(new BankEntity("BANK001"));
        when(executionContext.getLong(ContextKey.companyId)).thenReturn(123L);
        when(executionContext.getLong(ContextKey.userId)).thenReturn(456L);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Payment Information Mapping Tests")
    class PaymentInformationMappingTests {
        
        @Test
        @DisplayName("Should map valid payment information DTO to business object")
        void pain001PaymentToBo_WithValidDTO_ShouldMap() throws ParseException {
            // Arrange
            PaymentInformationDTO paymentDTO = createValidPaymentDTO();
            PaymentInformation expectedPaymentInfo = new PaymentInformation();
            PwsTransactions expectedTransactions = new PwsTransactions();
            PwsBulkTransactions expectedBulkTransactions = new PwsBulkTransactions();
            
            when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO))
                .thenReturn(expectedPaymentInfo);
            when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO))
                .thenReturn(expectedTransactions);
            when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any()))
                .thenReturn(expectedBulkTransactions);

            // Act
            PaymentInformation result = service.pain001PaymentToBo(paymentDTO);

            // Assert
            assertNotNull(result);
            assertEquals(expectedPaymentInfo, result);
            assertEquals("BANK001", result.getPwsTransactions().getBankEntityId());
            assertEquals(123L, result.getPwsTransactions().getCompanyId());
            assertEquals(456L, result.getPwsTransactions().getInitiatedBy());
            assertNotNull(result.getPwsTransactions().getInitiationTime());
            
            verify(pain001ToBoMapper).mapToPaymentInformation(paymentDTO);
            verify(pain001ToBoMapper).mapToPwsTransactions(paymentDTO);
            verify(pain001ToBoMapper).mapToPwsBulkTransactions(eq(paymentDTO), any());
        }

        @Test
        @DisplayName("Should throw exception for payment without transactions")
        void pain001PaymentToBo_WithNoTransactions_ShouldThrowException() {
            // Arrange
            PaymentInformationDTO paymentDTO = createPaymentDTOWithoutTransactions();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, 
                () -> service.pain001PaymentToBo(paymentDTO));
        }
    }

    @Nested
    @DisplayName("Transaction Mapping Tests")
    class TransactionMappingTests {
        
        @Test
        @DisplayName("Should map credit transfer transactions correctly")
        void pain001PaymentToBo_WithMultipleTransactions_ShouldMapAll() throws ParseException {
            // Arrange
            PaymentInformationDTO paymentDTO = createPaymentDTOWithMultipleTransactions();
            PaymentInformation expectedPaymentInfo = new PaymentInformation();
            PwsTransactions expectedTransactions = new PwsTransactions();
            PwsBulkTransactions expectedBulkTransactions = new PwsBulkTransactions();
            
            when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO))
                .thenReturn(expectedPaymentInfo);
            when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO))
                .thenReturn(expectedTransactions);
            when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any()))
                .thenReturn(expectedBulkTransactions);
            when(pain001ToBoMapper.mapToPwsBulkTransactionInstructions(any(), any()))
                .thenReturn(new PwsBulkTransactionInstructions());
            when(pain001ToBoMapper.mapToCreditor(any()))
                .thenReturn(new Creditor());
            when(pain001ToBoMapper.mapToPwsTransactionAdvices(any()))
                .thenReturn(new PwsTransactionAdvices());
            when(pain001ToBoMapper.mapToTaxInformation(any()))
                .thenReturn(new TaxInformation());

            // Act
            PaymentInformation result = service.pain001PaymentToBo(paymentDTO);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getCreditTransferTransactionList().size());
            verify(pain001ToBoMapper, times(2)).mapToCreditor(any());
            verify(pain001ToBoMapper, times(2)).mapToPwsTransactionAdvices(any());
            verify(pain001ToBoMapper, times(2)).mapToTaxInformation(any());
        }

        @Test
        @DisplayName("Should handle payment instructions correctly")
        void pain001PaymentToBo_WithPaymentInstructions_ShouldMapInstructions() throws ParseException {
            // Arrange
            PaymentInformationDTO paymentDTO = createPaymentDTOWithInstructions();
            when(pain001ToBoMapper.mapToPaymentInformation(paymentDTO))
                .thenReturn(new PaymentInformation());
            when(pain001ToBoMapper.mapToPwsTransactions(paymentDTO))
                .thenReturn(new PwsTransactions());
            when(pain001ToBoMapper.mapToPwsBulkTransactions(eq(paymentDTO), any()))
                .thenReturn(new PwsBulkTransactions());
            when(pain001ToBoMapper.mapToPwsBulkTransactionInstructions(any(), any()))
                .thenReturn(new PwsBulkTransactionInstructions());

            // Act
            PaymentInformation result = service.pain001PaymentToBo(paymentDTO);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getCreditTransferTransactionList().get(0)
                .getPwsBulkTransactionInstructions().getPaymentDetails());
        }
    }

    private PaymentInformationDTO createValidPaymentDTO() {
        PaymentInformationDTO dto = new PaymentInformationDTO();
        RequestedExecutionDateDTO execDate = new RequestedExecutionDateDTO();
        execDate.setDate("2024-01-01");
        dto.setRequestedExecutionDate(execDate);
        
        List<CreditTransferTransactionInformationDTO> transactions = new ArrayList<>();
        CreditTransferTransactionInformationDTO transaction = createTransactionDTO("1", "APPROVED", "USD");
        transactions.add(transaction);
        dto.setCreditTransferTransactionInformation(transactions);
        
        return dto;
    }

    private PaymentInformationDTO createPaymentDTOWithoutTransactions() {
        PaymentInformationDTO dto = new PaymentInformationDTO();
        RequestedExecutionDateDTO execDate = new RequestedExecutionDateDTO();
        execDate.setDate("2024-01-01");
        dto.setRequestedExecutionDate(execDate);
        return dto;
    }

    private PaymentInformationDTO createPaymentDTOWithMultipleTransactions() {
        PaymentInformationDTO dto = createValidPaymentDTO();
        List<CreditTransferTransactionInformationDTO> transactions = new ArrayList<>();
        transactions.add(createTransactionDTO("1", "APPROVED", "USD"));
        transactions.add(createTransactionDTO("2", "APPROVED", "USD"));
        dto.setCreditTransferTransactionInformation(transactions);
        return dto;
    }

    private PaymentInformationDTO createPaymentDTOWithInstructions() {
        PaymentInformationDTO dto = createValidPaymentDTO();
        CreditTransferTransactionInformationDTO transaction = dto.getCreditTransferTransactionInformation().get(0);
        
        List<InstructionForCreditorAgentDTO> instructions = new ArrayList<>();
        InstructionForCreditorAgentDTO instruction = new InstructionForCreditorAgentDTO();
        instruction.setInstructionInformation("Payment details test");
        instructions.add(instruction);
        transaction.setInstructionForCreditorAgent(instructions);
        
        return dto;
    }

    private CreditTransferTransactionInformationDTO createTransactionDTO(
            String lineNumber, String status, String currency) {
        CreditTransferTransactionInformationDTO dto = new CreditTransferTransactionInformationDTO();
        dto.setLinenumber(lineNumber);
        dto.setTransactionstatus(status);
        
        AmountDTO amountDTO = new AmountDTO();
        InstructedAmountDTO instructedAmount = new InstructedAmountDTO();
        instructedAmount.setCurrency(currency);
        amountDTO.setInstructedAmount(instructedAmount);
        dto.setAmount(amountDTO);
        
        return dto;
    }
}
```

# debulk

```java
package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentDebulkServiceImplTHTest {

    @Mock private AppConfig appConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private PaymentSaveService paymentSaveService;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    
    private PaymentDebulkServiceImplTH service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new PaymentDebulkServiceImplTH(appConfig, objectMapper, paymentSaveService);
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
            .thenReturn(new Pain001InboundProcessingResult());
        
        AppConfig.DebulkConfig debulkConfig = new AppConfig.DebulkConfig();
        AppConfig.SmartDebulkConfig smartConfig = new AppConfig.SmartDebulkConfig();
        smartConfig.setBankCode("024");
        smartConfig.setBahtnetThreshold(new BigDecimal("2000000"));
        smartConfig.setSourceFormat(Arrays.asList("FORMAT1", "FORMAT2"));
        debulkConfig.setDebulkSmart(smartConfig);
        when(appConfig.getDebulk()).thenReturn(debulkConfig);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Smart Payment Splitting Tests")
    class SmartPaymentSplittingTests {
        
        @Test
        @DisplayName("Should split SMART payments by resource type")
        void splitByResourceType_WithSmartPayments_ShouldSplitCorrectly() {
            // Arrange
            PaymentInformation payment = createSmartPayment(Arrays.asList(
                createTransaction(new BigDecimal("1000000")),
                createTransaction(new BigDecimal("2500000")),
                createTransaction(new BigDecimal("500000"))
            ));
            
            when(executionContext.get(ContextKey.sourceFormat, String.class))
                .thenReturn("FORMAT1");

            // Act
            List<PaymentInformation> result = service.splitByResourceType(payment);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            verifyResourceTypes(result, 
                Resource.SMART_SAME_DAY.id,
                Resource.BAHTNET.id,
                Resource.SMART_NEXT_DAY.id);
        }

        @Test
        @DisplayName("Should not split non-SMART payments")
        void splitByResourceType_WithNonSmartPayments_ShouldNotSplit() {
            // Arrange
            PaymentInformation payment = createNonSmartPayment(Arrays.asList(
                createTransaction(new BigDecimal("1000000")),
                createTransaction(new BigDecimal("2000000"))
            ));

            // Act
            List<PaymentInformation> result = service.splitByResourceType(payment);

            // Assert
            assertEquals(1, result.size());
            assertEquals(payment.getPwsTransactions().getResourceId(), 
                result.get(0).getPwsTransactions().getResourceId());
        }
    }

    @Nested
    @DisplayName("Itemized Splitting Tests")
    class ItemizedSplittingTests {
        
        @Test
        @DisplayName("Should split by maximum transaction count for itemized")
        void splitByItemized_WithinMaxCount_ShouldSplitCorrectly() {
            // Arrange
            PaymentInformation payment = createPayment(createTransactionList(5));
            int maxCount = 2;

            // Act
            List<PaymentInformation> result = service.splitByItemized(payment, maxCount);

            // Assert
            assertEquals(3, result.size());
            assertEquals(2, result.get(0).getCreditTransferTransactionList().size());
            assertEquals(2, result.get(1).getCreditTransferTransactionList().size());
            assertEquals(1, result.get(2).getCreditTransferTransactionList().size());
        }
    }

    @Nested
    @DisplayName("Lumpsum Splitting Tests")
    class LumpsumSplittingTests {
        
        @Test
        @DisplayName("Should split by maximum amount for lumpsum")
        void splitByLumpsum_WithinMaxAmount_ShouldSplitCorrectly() {
            // Arrange
            PaymentInformation payment = createPayment(Arrays.asList(
                createTransaction(new BigDecimal("500000")),
                createTransaction(new BigDecimal("600000")),
                createTransaction(new BigDecimal("400000"))
            ));
            List<PwsRejectedRecord> rejects = new ArrayList<>();
            int maxAmount = 1000000;

            // Act
            List<PaymentInformation> result = service.splitByLumpsum(payment, maxAmount, rejects);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(rejects.isEmpty());
        }

        @Test
        @DisplayName("Should handle transactions exceeding maximum amount")
        void splitByLumpsum_ExceedingMaxAmount_ShouldReject() {
            // Arrange
            PaymentInformation payment = createPayment(Arrays.asList(
                createTransaction(new BigDecimal("1500000")),
                createTransaction(new BigDecimal("500000"))
            ));
            List<PwsRejectedRecord> rejects = new ArrayList<>();
            int maxAmount = 1000000;
            
            CompanySettings settings = new CompanySettings();
            settings.setRejectOnErrorConfig(RejectOnErrorConfig.NO);
            when(executionContext.get(ContextKey.companySettings, CompanySettings.class))
                .thenReturn(settings);

            // Act
            List<PaymentInformation> result = service.splitByLumpsum(payment, maxAmount, rejects);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1, rejects.size());
        }
    }

    @Nested
    @DisplayName("Full Debulking Process Tests")
    class FullDebulkingTests {
        
        @Test
        @DisplayName("Should process complete debulking workflow")
        void debulk_WithValidPayments_ShouldProcessComplete() {
            // Arrange
            List<PaymentInformation> payments = Arrays.asList(
                createSmartPayment(createTransactionList(3)),
                createNonSmartPayment(createTransactionList(2))
            );
            
            CompanySettings settings = new CompanySettings();
            settings.setBatchBookingIndicator(BatchBookingIndicator.ITEMIZED);
            settings.setMaxCountOfBatchBooking(2);
            when(executionContext.get(ContextKey.companySettings, CompanySettings.class))
                .thenReturn(settings);

            // Act
            List<PaymentInformation> result = service.debulk(payments);

            // Assert
            assertNotNull(result);
            assertTrue(result.size() > 0);
            verify(paymentSaveService, never()).createRejectedRecords(any());
        }

        @Test
        @DisplayName("Should handle invalid booking indicator")
        void debulk_WithInvalidBookingIndicator_ShouldReturnNull() {
            // Arrange
            List<PaymentInformation> payments = Arrays.asList(
                createSmartPayment(createTransactionList(2))
            );
            
            CompanySettings settings = new CompanySettings();
            settings.setBatchBookingIndicator(null);
            when(executionContext.get(ContextKey.companySettings, CompanySettings.class))
                .thenReturn(settings);

            // Act
            List<PaymentInformation> result = service.debulk(payments);

            // Assert
            assertNull(result);
            verify(executionContext).get(eq(ContextKey.result), any());
        }
    }

    private PaymentInformation createSmartPayment(List<CreditTransferTransaction> transactions) {
        PaymentInformation payment = createPayment(transactions);
        payment.getPwsTransactions().setResourceId(Resource.SMART.id);
        return payment;
    }

    private PaymentInformation createNonSmartPayment(List<CreditTransferTransaction> transactions) {
        PaymentInformation payment = createPayment(transactions);
        payment.getPwsTransactions().setResourceId("OTHER");
        return payment;
    }

    private PaymentInformation createPayment(List<CreditTransferTransaction> transactions) {
        PaymentInformation payment = new PaymentInformation();
        PwsTransactions pwsTransactions = new PwsTransactions();
        PwsBulkTransactions pwsBulkTransactions = new PwsBulkTransactions();
        
        payment.setPwsTransactions(pwsTransactions);
        payment.setPwsBulkTransactions(pwsBulkTransactions);
        payment.setCreditTransferTransactionList(transactions);
        
        return payment;
    }

    private List<CreditTransferTransaction> createTransactionList(int count) {
        List<CreditTransferTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(createTransaction(new BigDecimal("100000")));
        }
        return transactions;
    }

    private CreditTransferTransaction createTransaction(BigDecimal amount) {
        CreditTransferTransaction transaction = new CreditTransferTransaction();
        PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
        instructions.setTransactionAmount(amount);
        transaction.setPwsBulkTransactionInstructions(instructions);
        return transaction;
    }

    private void verifyResourceTypes(List<PaymentInformation> payments, String... expectedTypes) {
        Set<String> actualTypes = new HashSet<>();
        for (PaymentInformation payment : payments) {
            actualTypes.add(payment.getPwsTransactions().getResourceId());
        }
        Set<String> expectedTypeSet = new HashSet<>(Arrays.asList(expectedTypes));
        assertTrue(actualTypes.containsAll(expectedTypeSet));
    }
}
```

# validation

```java
package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentValidationServiceImplTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private TransactionUtils transactionUtils;
    @Mock private PaymentIntegrationservice paymentIntegrationservice;
    @Mock private PaymentValidationHelper paymentValidationHelper;
    @Mock private DecisionMatrixService<TransactionValidationRecord> txnValidationService;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    
    private PaymentValidationServiceImpl service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new PaymentValidationServiceImpl(
            objectMapper,
            transactionUtils,
            paymentIntegrationservice,
            paymentValidationHelper,
            txnValidationService
        );
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
            .thenReturn(new Pain001InboundProcessingResult());
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Entitlement Validation Tests")
    class EntitlementValidationTests {
        
        @Test
        @DisplayName("Should validate entitlements successfully")
        void doEntitlementValidation_WithValidEntitlements_ShouldValidate() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfoList();
            CompanyAndAccountsForResourceFeaturesResp resp = createValidEntitlementResponse();
            
            when(paymentIntegrationservice.getCompanyAndAccounts(anyLong(), anyString(), anyString()))
                .thenReturn(resp);
            when(transactionUtils.getDecrypted(anyString())).thenReturn("123");
            
            // Act
            List<PaymentInformation> result = service.doEntitlementValidation(payments);

            // Assert
            assertNotNull(result);
            assertEquals(Pain001InboundProcessingStatus.EntitlementValidationPassed, 
                getResult().getProcessingStatus());
            verify(paymentIntegrationservice).getCompanyAndAccounts(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle missing entitlements")
        void doEntitlementValidation_WithMissingEntitlements_ShouldReturnNull() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfoList();
            when(paymentIntegrationservice.getCompanyAndAccounts(anyLong(), anyString(), anyString()))
                .thenReturn(null);

            // Act
            List<PaymentInformation> result = service.doEntitlementValidation(payments);

            // Assert
            assertNull(result);
            assertEquals(Pain001InboundProcessingStatus.EntitlementValidationStop, 
                getResult().getProcessingStatus());
        }
    }

    @Nested
    @DisplayName("Pre-Transaction Validation Tests")
    class PreTransactionValidationTests {
        
        @Test
        @DisplayName("Should validate pre-transaction conditions")
        void doPreTransactionValidation_WithValidConditions_ShouldValidate() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfoList();
            setupValidPreTransactionConditions();

            // Act
            List<PaymentInformation> result = service.doPreTransactionValidation(payments);

            // Assert
            assertNotNull(result);
            assertEquals(Pain001InboundProcessingStatus.PreTransactionValidationPassed, 
                getResult().getProcessingStatus());
            verify(paymentValidationHelper).validateValueDate(any(), any());
        }

        @Test
        @DisplayName("Should handle validation errors with reject on error")
        void doPreTransactionValidation_WithErrors_ShouldReject() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfoList();
            setupRejectOnErrorConditions();

            // Act
            List<PaymentInformation> result = service.doPreTransactionValidation(payments);

            // Assert
            assertNull(result);
            assertEquals(Pain001InboundProcessingStatus.RejectOnError, 
                getResult().getProcessingStatus());
        }
    }

    @Nested
    @DisplayName("Transaction Validation Tests")
    class TransactionValidationTests {
        
        @Test
        @DisplayName("Should validate transactions successfully")
        void doTransactionValidation_WithValidTransactions_ShouldValidate() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfoList();
            when(txnValidationService.applyRulesShouldStop(any(), any(), anyBoolean()))
                .thenReturn(false);

            // Act
            List<PaymentInformation> result = service.doTransactionValidation(payments);

            // Assert
            assertNotNull(result);
            assertEquals(Pain001InboundProcessingStatus.TransactionValidationPassed, 
                getResult().getProcessingStatus());
            verify(txnValidationService).applyRulesShouldStop(any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Should handle validation exceptions")
        void doTransactionValidation_WithException_ShouldHandleError() {
            // Arrange
            List<PaymentInformation> payments = createPaymentInfoList();
            when(txnValidationService.applyRulesShouldStop(any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("Validation error"));

            // Act
            List<PaymentInformation> result = service.doTransactionValidation(payments);

            // Assert
            assertNotNull(result);
            assertEquals(Pain001InboundProcessingStatus.TransactionValidationWithException, 
                getResult().getProcessingStatus());
        }
    }

    @Nested
    @DisplayName("Payment Update Tests")
    class PaymentUpdateTests {
        
        @Test
        @DisplayName("Should update payment totals correctly")
        void updatePaymentAfterValidation_ShouldUpdateTotals() {
            // Arrange
            List<PaymentInformation> payments = Arrays.asList(
                createValidPayment(new BigDecimal("1000")),
                createInvalidPayment(new BigDecimal("500"))
            );

            // Act
            service.updatePaymentAfterValidation(payments);

            // Assert
            Pain001InboundProcessingResult result = getResult();
            assertEquals(1, result.getPaymentValidTotal());
            assertEquals(new BigDecimal("1000"), result.getPaymentValidAmount());
            assertEquals(1, result.getPaymentInvalidTotal());
            assertEquals(new BigDecimal("500"), result.getPaymentInvalidAmount());
        }
    }

    private List<PaymentInformation> createPaymentInfoList() {
        return Arrays.asList(createValidPayment(new BigDecimal("1000")));
    }

    private PaymentInformation createValidPayment(BigDecimal amount) {
        PaymentInformation payment = new PaymentInformation();
        payment.setValid(true);
        
        PwsTransactions transactions = new PwsTransactions();
        transactions.setAccountNumber("ACC123");
        transactions.setTotalAmount(amount);
        payment.setPwsTransactions(transactions);
        
        PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
        bulkTransactions.setDmpBatchNumber("BATCH001");
        payment.setPwsBulkTransactions(bulkTransactions);
        
        return payment;
    }

    private PaymentInformation createInvalidPayment(BigDecimal amount) {
        PaymentInformation payment = createValidPayment(amount);
        payment.setValid(false);
        return payment;
    }

    private CompanyAndAccountsForResourceFeaturesResp createValidEntitlementResponse() {
        CompanyAndAccountsForResourceFeaturesResp resp = new CompanyAndAccountsForResourceFeaturesResp();
        List<CompanyAndAccountsForUser> companyGroups = new ArrayList<>();
        
        CompanyAndAccountsForUser group = new CompanyAndAccountsForUser();
        group.setCompanyGroupId("123");
        
        CompanyAccountforUser company = new CompanyAccountforUser();
        company.setCompanyId("123");
        company.setCompanyName("Test Company");
        
        AccountResource account = new AccountResource();
        account.setAccountNumber("ACC123");
        account.setAccountStatus("ACTIVE");
        company.setAccounts(Arrays.asList(account));
        
        group.setCompanies(Arrays.asList(company));
        companyGroups.add(group);
        
        resp.setCompaniesAccountResourceFeature(companyGroups);
        return resp;
    }

    private void setupValidPreTransactionConditions() {
        CompanySettings settings = new CompanySettings();
        settings.setRejectOnErrorConfig(RejectOnErrorConfig.NO);
        when(executionContext.get(ContextKey.companySettings, CompanySettings.class))
            .thenReturn(settings);
    }

    private void setupRejectOnErrorConditions() {
        CompanySettings settings = new CompanySettings();
        settings.setRejectOnErrorConfig(RejectOnErrorConfig.YES);
        when(executionContext.get(ContextKey.companySettings, CompanySettings.class))
            .thenReturn(settings);
        
        PaymentValidationResult validationResult = new PaymentValidationResult(
            PaymentValidationResult.Result.ERROR, "Validation error"
        );
        when(paymentValidationHelper.validateValueDate(any(), any()))
            .thenReturn(validationResult);
    }

    private Pain001InboundProcessingResult getResult() {
        return executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class);
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
 package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentIntegrationserviceImplTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private TransactionUtils transactionUtils;
    @Mock private GetAxwayTokenService getAxwayTokenService;
    @Mock private EntitlementService entitlementService;
    @Mock private ValueDateDerivationService valueDateDerivationService;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    
    private PaymentIntegrationserviceImpl service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new PaymentIntegrationserviceImpl(
            objectMapper,
            transactionUtils,
            getAxwayTokenService,
            entitlementService,
            valueDateDerivationService
        );
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        
        // Set URLs via reflection
        ReflectionTestUtils.setField(service, "ueqsResourceFeature", "http://test.com/resources");
        ReflectionTestUtils.setField(service, "ueqsCompanyAccount", "http://test.com/accounts");
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("CEW Headers Tests")
    class CewHeadersTests {
        
        @Test
        @DisplayName("Should create valid CEW headers")
        void getCewHeaders_ShouldCreateValidHeaders() {
            // Arrange
            AxwayResponseData axwayResponse = new AxwayResponseData();
            axwayResponse.setTokenType("Bearer");
            axwayResponse.setAccessToken("test-token");
            
            when(getAxwayTokenService.getAxwayJwtToken()).thenReturn(axwayResponse);
            when(executionContext.get(ContextKey.bankEntity, BankEntity.class))
                .thenReturn(new BankEntity("BANK001"));
            when(transactionUtils.getEncrypted("BANK001"))
                .thenReturn("encrypted-bank-id");

            // Act
            Map<String, Object> headers = service.getCewHeaders();

            // Assert
            assertNotNull(headers);
            assertEquals("Bearer test-token", headers.get("authorization"));
            assertEquals("encrypted-bank-id", headers.get("uob-entity-id"));
            assertNotNull(headers.get("req-date-time"));
            
            verify(getAxwayTokenService).getAxwayJwtToken();
            verify(transactionUtils).getEncrypted("BANK001");
        }
    }

    @Nested
    @DisplayName("Resources and Features Tests")
    class ResourcesAndFeaturesTests {
        
        @Test
        @DisplayName("Should get resources and features for user")
        void getResourcesAndFeatures_ShouldReturnData() {
            // Arrange
            long userId = 123L;
            UserResourceFeaturesActionsData expectedData = new UserResourceFeaturesActionsData();
            Map<String, Object> headers = new HashMap<>();
            
            when(transactionUtils.getEncrypted(String.valueOf(userId)))
                .thenReturn("encrypted-user-id");
            when(entitlementService.getResourcesAndFeaturesByUserId(
                eq("encrypted-user-id"), any()
            )).thenReturn(expectedData);

            // Act
            UserResourceFeaturesActionsData result = service.getResourcesAndFeatures(userId);

            // Assert
            assertNotNull(result);
            verify(transactionUtils).getEncrypted(String.valueOf(userId));
            verify(entitlementService).getResourcesAndFeaturesByUserId(
                eq("encrypted-user-id"), any()
            );
        }
    }

    @Nested
    @DisplayName("Company and Accounts Tests")
    class CompanyAndAccountsTests {
        
        @Test
        @DisplayName("Should get company and accounts for resource features")
        void getCompanyAndAccounts_ShouldReturnData() {
            // Arrange
            long userId = 123L;
            String resourceId = "RES001";
            String featureId = "FEAT001";
            CompanyAndAccountsForResourceFeaturesResp expectedResp = 
                new CompanyAndAccountsForResourceFeaturesResp();
            
            when(transactionUtils.getEncrypted(String.valueOf(userId)))
                .thenReturn("encrypted-user-id");
            when(entitlementService.getV3CompanyAndAccountsForUserResourceFeatures(
                any(), any()
            )).thenReturn(expectedResp);
            when(executionContext.getLong(ContextKey.userId))
                .thenReturn(userId);

            // Act
            CompanyAndAccountsForResourceFeaturesResp result = 
                service.getCompanyAndAccounts(userId, resourceId, featureId);

            // Assert
            assertNotNull(result);
            verify(entitlementService).getV3CompanyAndAccountsForUserResourceFeatures(
                argThat(req -> {
                    assertEquals("encrypted-user-id", req.getUserId());
                    assertEquals(resourceId, req.getResources().get(0).getResourceId());
                    assertEquals(featureId, req.getResources().get(0).getFeatureIds().get(0));
                    return true;
                }),
                any()
            );
        }
    }

    @Nested
    @DisplayName("Value Date Derivation Tests")
    class ValueDateDerivationTests {
        
        @Test
        @DisplayName("Should derive value date correctly")
        void getDerivedValueDates_ShouldReturnDerivedDate() {
            // Arrange
            Date valueDate = new Date();
            String acctCurrency = "USD";
            String transferCurrency = "THB";
            LocalDate expectedDate = LocalDate.now();
            
            when(executionContext.get(eq(ContextKey.bankEntity), any()))
                .thenReturn(new BankEntity("BANK001"));
            when(executionContext.get(eq(ContextKey.routeConfig), any()))
                .thenReturn(createRouteConfig());
            when(valueDateDerivationService.deriveValueDate(any()))
                .thenReturn(expectedDate.toString());

            // Act
            LocalDate result = service.getDerivedValueDates(
                valueDate, acctCurrency, transferCurrency
            );

            // Assert
            assertEquals(expectedDate, result);
            verify(valueDateDerivationService).deriveValueDate(
                argThat(req -> {
                    assertEquals("BANK001", req.getBankEntityId());
                    assertEquals(acctCurrency, req.getDebitAccountCurrency());
                    assertEquals(transferCurrency, req.getTransactionCurrency());
                    return true;
                })
            );
        }
    }

    private AppConfig.BulkRoute createRouteConfig() {
        AppConfig.BulkRoute route = new AppConfig.BulkRoute();
        route.setFeatureId("FEAT001");
        route.setResourceId("RES001");
        return route;
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

# save

```java
package com.example.service;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mybatis.spring.SqlSessionTemplate;
import org.apache.ibatis.session.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentSaveServiceImplTest {

    @Mock private AppConfig appConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private PaymentUtils paymentUtils;
    @Mock private PaymentDeleteService paymentDeleteService;
    @Mock private RetryTemplate retryTemplate;
    @Mock private SqlSessionTemplate sqlSessionTemplate;
    @Mock private PwsSaveDao pwsSaveDao;
    @Mock private StepExecution stepExecution;
    @Mock private ExecutionContext executionContext;
    @Mock private SqlSessionFactory sqlSessionFactory;
    @Mock private SqlSession sqlSession;
    
    private PaymentSaveServiceImpl service;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new PaymentSaveServiceImpl(
            appConfig, objectMapper, paymentUtils, paymentDeleteService,
            retryTemplate, sqlSessionTemplate, pwsSaveDao
        );
        service.beforeStep(stepExecution);
        
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.get(ContextKey.result, Pain001InboundProcessingResult.class))
            .thenReturn(new Pain001InboundProcessingResult());
        when(sqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        when(sqlSessionFactory.openSession(any(ExecutorType.class))).thenReturn(sqlSession);
        when(sqlSession.getMapper(PwsSaveDao.class)).thenReturn(pwsSaveDao);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Transit Message Tests")
    class TransitMessageTests {
        
        @Test
        @DisplayName("Should create transit message")
        void createTransitMessage_WithValidMessage_ShouldCreate() {
            // Arrange
            PwsTransitMessage message = new PwsTransitMessage();
            message.setBankReferenceId("REF001");

            // Act
            service.createTransitMessage(message);

            // Assert
            verify(pwsSaveDao).insertPwsTransitMessage(message);
        }
    }

    @Nested
    @DisplayName("Rejected Record Tests")
    class RejectedRecordTests {
        
        @Test
        @DisplayName("Should create single rejected record")
        void createRejectedRecord_WithValidRecord_ShouldCreate() {
            // Arrange
            PwsRejectedRecord record = new PwsRejectedRecord();
            record.setEntityId("ENT001");

            // Act
            service.createRejectedRecord(record);

            // Assert
            verify(pwsSaveDao).insertPwsRejectedRecord(record);
        }

        @Test
        @DisplayName("Should create multiple rejected records in batches")
        void createRejectedRecords_WithMultipleRecords_ShouldCreateInBatches() {
            // Arrange
            List<PwsRejectedRecord> records = createRejectedRecords(5);
            when(appConfig.getBatchInsertSize()).thenReturn(2);
            when(retryTemplate.execute(any(), any())).thenAnswer(invocation -> {
                invocation.getArgument(0, RetryCallback.class).doWithRetry(null);
                return null;
            });

            // Act
            service.createRejectedRecords(records);

            // Assert
            verify(sqlSession, times(3)).commit();
            verify(sqlSession, times(3)).close();
        }
    }

    @Nested
    @DisplayName("Payment Information Save Tests")
    class PaymentInformationSaveTests {
        
        @Test
        @DisplayName("Should save valid payment information")
        void savePaymentInformation_WithValidPayment_ShouldSave() {
            // Arrange
            PaymentInformation payment = createValidPayment();
            when(appConfig.getBatchInsertSize()).thenReturn(2);
            when(retryTemplate.execute(any(), any())).thenAnswer(invocation -> {
                invocation.getArgument(0, RetryCallback.class).doWithRetry(null);
                return null;
            });

            // Act
            service.savePaymentInformation(payment);

            // Assert
            verify(pwsSaveDao).insertPwsTransactions(any());
            verify(pwsSaveDao).insertPwsBulkTransactions(any());
            verify(sqlSession, atLeastOnce()).commit();
        }

        @Test
        @DisplayName("Should handle save failure")
        void savePaymentInformation_WithSaveFailure_ShouldHandleError() {
            // Arrange
            PaymentInformation payment = createValidPayment();
            when(appConfig.getBatchInsertSize()).thenReturn(2);
            when(retryTemplate.execute(any(), any())).thenThrow(new RuntimeException("Save failed"));

            // Act
            service.savePaymentInformation(payment);

            // Assert
            verify(paymentDeleteService).deleteSavedPaymentInformation(payment);
            verify(paymentUtils).updatePaymentSavedError(any(), any());
        }
    }

    @Nested
    @DisplayName("Batch Processing Tests")
    class BatchProcessingTests {
        
        @Test
        @DisplayName("Should save bulk payment")
        void saveBulkPayment_WithValidPayment_ShouldSave() {
            // Arrange
            PaymentInformation payment = createValidPayment();
            when(pwsSaveDao.getBankRefSequenceNum()).thenReturn(1);
            when(executionContext.get(ContextKey.bankRefMetaData, BankRefMetaData.class))
                .thenReturn(new BankRefMetaData("TH", "PWS", "PAY", "2401"));

            // Act
            long txnId = service.saveBulkPayment(payment);

            // Assert
            assertTrue(txnId > 0);
            verify(pwsSaveDao).insertPwsTransactions(any());
            verify(pwsSaveDao).insertPwsBulkTransactions(any());
        }

        @Test
        @DisplayName("Should process transaction batch")
        void saveCreditTransferBatch_WithValidBatch_ShouldProcess() throws Exception {
            // Arrange
            PaymentInformation payment = createValidPayment();
            List<CreditTransferTransaction> batch = createTransactionBatch(2);
            when(pwsSaveDao.getBatchBankRefSequenceNum(anyInt()))
                .thenReturn(Arrays.asList(1, 2));
            when(executionContext.get(ContextKey.bankRefMetaData, BankRefMetaData.class))
                .thenReturn(new BankRefMetaData("TH", "PWS", "PAY", "2401"));

            // Act
            service.saveCreditTransferBatch(payment, batch, 1);

            // Assert
            verify(sqlSession, atLeastOnce()).commit();
            verify(sqlSession).close();
        }
    }

    private List<PwsRejectedRecord> createRejectedRecords(int count) {
        List<PwsRejectedRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PwsRejectedRecord record = new PwsRejectedRecord();
            record.setEntityId("ENT" + i);
            record.setEntityType("TYPE" + i);
            records.add(record);
        }
        return records;
    }

    private PaymentInformation createValidPayment() {
        PaymentInformation payment = new PaymentInformation();
        
        PwsTransactions transactions = new PwsTransactions();
        transactions.setTransactionId(1L);
        transactions.setBankReferenceId("REF001");
        payment.setPwsTransactions(transactions);
        
        PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
        bulkTransactions.setDmpBatchNumber("BATCH001");
        payment.setPwsBulkTransactions(bulkTransactions);
        
        List<CreditTransferTransaction> creditTransfers = createTransactionBatch(2);
        payment.setCreditTransferTransactionList(creditTransfers);
        
        return payment;
    }

    private List<CreditTransferTransaction> createTransactionBatch(int count) {
        List<CreditTransferTransaction> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CreditTransferTransaction txn = new CreditTransferTransaction();
            txn.setValid(true);
            
            PwsBulkTransactionInstructions instructions = new PwsBulkTransactionInstructions();
            instructions.setTransactionAmount(BigDecimal.valueOf(1000));
            txn.setPwsBulkTransactionInstructions(instructions);
            
            Creditor creditor = new Creditor();
            PwsParties parties = new PwsParties();
            creditor.setPwsParties(parties);
            txn.setCreditor(creditor);
            
            PwsTransactionAdvices advice = new PwsTransactionAdvices();
            txn.setAdvice(advice);
            
            TaxInformation taxInfo = new TaxInformation();
            txn.setTaxInformation(taxInfo);
            
            batch.add(txn);
        }
        return batch;
    }
}

@Nested
    @DisplayName("Batch Collection Tests")
    class BatchCollectionTests {
        
        @Test
        @DisplayName("Should collect all transaction batch records")
        void collectTransactionBatchRecords_WithAllFields_ShouldCollect() {
            // Arrange
            CreditTransferTransaction txn = createFullTransaction();
            TransactionBatchCollections collections = new TransactionBatchCollections();
            
            // Act
            service.collectTransactionBatchRecords(txn, 1L, "PARENT-REF", "CHILD-REF", 
                System.currentTimeMillis(), collections);

            // Assert
            assertNotNull(collections.getTxnInstructions());
            assertEquals(1, collections.getTxnInstructions().size());
            assertEquals(1, collections.getCreditors().size());
            assertTrue(collections.getCreditorContacts().size() > 0);
            assertEquals(1, collections.getAdvices().size());
            assertTrue(collections.getTaxInstructions().size() > 0);
            assertEquals(1, collections.getPayerTaxContacts().size());
            assertEquals(1, collections.getPayeeTaxContacts().size());
        }

        @Test
        @DisplayName("Should collect transaction records with null optionals")
        void collectTransactionBatchRecords_WithNullFields_ShouldCollect() {
            // Arrange
            CreditTransferTransaction txn = createMinimalTransaction();
            TransactionBatchCollections collections = new TransactionBatchCollections();
            
            // Act
            service.collectTransactionBatchRecords(txn, 1L, "PARENT-REF", "CHILD-REF", 
                System.currentTimeMillis(), collections);

            // Assert
            assertNotNull(collections.getTxnInstructions());
            assertEquals(1, collections.getTxnInstructions().size());
            assertEquals(1, collections.getCreditors().size());
            assertTrue(collections.getCreditorContacts().isEmpty());
            assertTrue(collections.getAdvices().contains(null));
            assertTrue(collections.getTaxInstructions().isEmpty());
            assertTrue(collections.getPayerTaxContacts().contains(null));
            assertTrue(collections.getPayeeTaxContacts().contains(null));
        }
    }

    @Nested
    @DisplayName("Batch Execution Tests")
    class BatchExecutionTests {
        
        @Test
        @DisplayName("Should execute batch inserts in correct order")
        void executeTransactionBatchInsertsInOrder_ShouldExecuteAll() {
            // Arrange
            TransactionBatchCollections collections = createFullBatchCollections();
            
            // Act
            service.executeTransactionBatchInsertsInOrder(collections, 1);

            // Assert
            verify(sqlSession, times(7)).commit(); // Verify all 7 batch types are committed
            verify(pwsSaveDao, atLeastOnce()).insertPwsBulkTransactionInstructions(any());
            verify(pwsSaveDao, atLeastOnce()).insertPwsParties(any());
            verify(pwsSaveDao, atLeastOnce()).insertPwsPartyContacts(any());
            verify(pwsSaveDao, atLeastOnce()).insertPwsTransactionAdvices(any());
            verify(pwsSaveDao, atLeastOnce()).insertPwsTaxInstructions(any());
        }

        @Test
        @DisplayName("Should handle batch insert failure")
        void executeBatchInsert_WithFailure_ShouldRollback() {
            // Arrange
            List<PwsParties> records = Collections.singletonList(new PwsParties());
            doThrow(new RuntimeException("Insert failed"))
                .when(pwsSaveDao).insertPwsParties(any());

            // Act & Assert
            assertThrows(BulkProcessingException.class, 
                () -> service.executeBatchInsert("insertPwsParties", records, null));
            verify(sqlSession).rollback();
            verify(sqlSession).close();
        }
    }

    @Nested
    @DisplayName("Payment Save Edge Cases")
    class PaymentSaveEdgeCasesTests {
        
        @Test
        @DisplayName("Should handle empty valid transactions")
        void savePaymentInformation_WithNoValidTransactions_ShouldReturn() {
            // Arrange
            PaymentInformation payment = createValidPayment();
            payment.getCreditTransferTransactionList()
                .forEach(txn -> txn.setValid(false));

            // Act
            service.savePaymentInformation(payment);

            // Assert
            verify(pwsSaveDao, never()).insertPwsTransactions(any());
            verify(pwsSaveDao, never()).insertPwsBulkTransactions(any());
        }

        @Test
        @DisplayName("Should handle retry exhaustion")
        void savePaymentInformation_WithRetryExhaustion_ShouldHandleError() {
            // Arrange
            PaymentInformation payment = createValidPayment();
            when(retryTemplate.execute(any(), any()))
                .thenThrow(new RuntimeException("Retry exhausted"));

            // Act
            service.savePaymentInformation(payment);

            // Assert
            verify(paymentDeleteService).deleteSavedPaymentInformation(payment);
            verify(paymentUtils).updatePaymentSavedError(any(), any());
        }
    }

    // Helper methods for creating test data
    private CreditTransferTransaction createFullTransaction() {
        CreditTransferTransaction txn = new CreditTransferTransaction();
        
        // Set required fields
        txn.setPwsBulkTransactionInstructions(new PwsBulkTransactionInstructions());
        
        // Create creditor with contacts
        Creditor creditor = new Creditor();
        creditor.setPwsParties(new PwsParties());
        List<PwsPartyContacts> contacts = Arrays.asList(
            new PwsPartyContacts(), new PwsPartyContacts()
        );
        creditor.setPwsPartyContactList(contacts);
        txn.setCreditor(creditor);
        
        // Set advice
        txn.setAdvice(new PwsTransactionAdvices());
        
        // Create tax information
        TaxInformation taxInfo = new TaxInformation();
        taxInfo.setInstructionList(Arrays.asList(
            new PwsTaxInstructions(), new PwsTaxInstructions()
        ));
        taxInfo.setPayerTaxContact(new PwsPartyContacts());
        taxInfo.setPayeeTaxContact(new PwsPartyContacts());
        txn.setTaxInformation(taxInfo);
        
        return txn;
    }

    private CreditTransferTransaction createMinimalTransaction() {
        CreditTransferTransaction txn = new CreditTransferTransaction();
        txn.setPwsBulkTransactionInstructions(new PwsBulkTransactionInstructions());
        
        Creditor creditor = new Creditor();
        creditor.setPwsParties(new PwsParties());
        txn.setCreditor(creditor);
        
        txn.setTaxInformation(new TaxInformation());
        
        return txn;
    }

    private TransactionBatchCollections createFullBatchCollections() {
        TransactionBatchCollections collections = new TransactionBatchCollections();
        
        // Add instructions
        collections.getTxnInstructions().add(new PwsBulkTransactionInstructions());
        
        // Add creditors
        collections.getCreditors().add(new PwsParties());
        
        // Add creditor contacts
        List<PwsPartyContacts> contacts = new ArrayList<>();
        contacts.add(new PwsPartyContacts());
        collections.getCreditorContacts().add(contacts);
        
        // Add advice
        collections.getAdvices().add(new PwsTransactionAdvices());
        
        // Add tax instructions
        List<PwsTaxInstructions> taxInstructions = new ArrayList<>();
        taxInstructions.add(new PwsTaxInstructions());
        collections.getTaxInstructions().add(taxInstructions);
        
        // Add tax contacts
        collections.getPayerTaxContacts().add(new PwsPartyContacts());
        collections.getPayeeTaxContacts().add(new PwsPartyContacts());
        
        return collections;
    }
```
