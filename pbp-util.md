# commong

```java
package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommonUtilsTest {

    private ObjectMapper objectMapper;
    private Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    }

    @Nested
    @DisplayName("Date Formatting Tests")
    class DateFormattingTests {

        @Test
        @DisplayName("Should format date with zone correctly")
        void isoZoneFormatDateTime_WithValidDate_ShouldFormatCorrectly() {
            // Arrange
            Date date = new Date();
            
            // Act
            String formatted = CommonUtils.isoZoneFormatDateTime(date);
            
            // Assert
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null date")
        void isoZoneFormatDateTime_WithNullDate_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class, 
                () -> CommonUtils.isoZoneFormatDateTime((Date) null));
        }
    }

    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {

        @Test
        @DisplayName("Should parse valid date string")
        void stringToDate_WithValidDateString_ShouldParseCorrectly() throws ParseException {
            // Arrange
            String dateStr = "2024-01-01";
            
            // Act
            Date result = CommonUtils.stringToDate(dateStr);
            
            // Assert
            assertNotNull(result);
            assertEquals(dateStr, CommonUtils.ISO_DATE_FORMAT.get().format(result));
        }

        @Test
        @DisplayName("Should return null for null or empty string")
        void stringToDate_WithNullOrEmptyString_ShouldReturnNull() throws ParseException {
            assertNull(CommonUtils.stringToDate(null));
            assertNull(CommonUtils.stringToDate(""));
        }

        @Test
        @DisplayName("Should throw ParseException for invalid date format")
        void stringToDate_WithInvalidFormat_ShouldThrowParseException() {
            assertThrows(ParseException.class, () -> CommonUtils.stringToDate("invalid-date"));
        }
    }

    @Nested
    @DisplayName("Timestamp Conversion Tests")
    class TimestampConversionTests {

        @Test
        @DisplayName("Should convert valid datetime string to timestamp")
        void stringToTimestamp_WithValidString_ShouldConvertCorrectly() throws ParseException {
            // Arrange
            String datetimeStr = "2024-01-01 12:00:00.000";
            
            // Act
            Timestamp result = CommonUtils.stringToTimestamp(datetimeStr);
            
            // Assert
            assertNotNull(result);
            assertEquals(datetimeStr, CommonUtils.ISO_DATETIME_FORMAT.get().format(result));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return null for null or empty input")
        void stringToTimestamp_WithNullOrEmpty_ShouldReturnNull(String input) throws ParseException {
            assertNull(CommonUtils.stringToTimestamp(input));
        }
    }

    @Nested
    @DisplayName("Date Type Conversion Tests")
    class DateTypeConversionTests {

        @Test
        @DisplayName("Should convert between util.Date and LocalDate")
        void dateConversion_UtilDateAndLocalDate_ShouldConvertCorrectly() {
            // Arrange
            Date utilDate = new Date();
            
            // Act
            LocalDate localDate = CommonUtils.utilAsLocalDate(utilDate);
            Date convertedBack = CommonUtils.localDateAsUtil(localDate);
            
            // Assert
            assertNotNull(localDate);
            assertNotNull(convertedBack);
            assertEquals(localDate, convertedBack.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate());
        }

        @Test
        @DisplayName("Should convert between util.Date and LocalDateTime")
        void dateConversion_UtilDateAndLocalDateTime_ShouldConvertCorrectly() {
            // Arrange
            Date utilDate = new Date();
            
            // Act
            LocalDateTime localDateTime = CommonUtils.utilAsLocalDateTime(utilDate);
            Date convertedBack = CommonUtils.localDateTimeAsUtil(localDateTime);
            
            // Assert
            assertNotNull(localDateTime);
            assertNotNull(convertedBack);
            assertEquals(localDateTime.withNano(0), 
                convertedBack.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .withNano(0));
        }

        @Test
        @DisplayName("Should convert between util.Date and sql.Date")
        void dateConversion_UtilDateAndSqlDate_ShouldConvertCorrectly() {
            // Arrange
            Date utilDate = new Date();
            
            // Act
            java.sql.Date sqlDate = CommonUtils.utilAsSqlDate(utilDate);
            Date convertedBack = CommonUtils.sqlDateAsUtil(sqlDate);
            
            // Assert
            assertNotNull(sqlDate);
            assertNotNull(convertedBack);
            assertEquals(utilDate.getTime(), convertedBack.getTime());
        }

        @Test
        @DisplayName("Should convert between util.Date and sql.Timestamp")
        void dateConversion_UtilDateAndSqlTimestamp_ShouldConvertCorrectly() {
            // Arrange
            Date utilDate = new Date();
            
            // Act
            Timestamp sqlTimestamp = CommonUtils.utilAsSqlTimestamp(utilDate);
            Date convertedBack = CommonUtils.sqlTimestampDateAsUtil(sqlTimestamp);
            
            // Assert
            assertNotNull(sqlTimestamp);
            assertNotNull(convertedBack);
            assertEquals(utilDate.getTime(), convertedBack.getTime());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should get root cause message from nested exception")
        void getShortErrorMessage_WithNestedException_ShouldGetRootCause() {
            // Arrange
            Exception innermost = new IllegalArgumentException("Root cause");
            Exception middle = new RuntimeException("Middle", innermost);
            Exception outer = new Exception("Outer", middle);
            
            // Act
            String message = CommonUtils.getShortErrorMessage(outer);
            
            // Assert
            assertTrue(message.contains("Root cause"));
        }
    }

    @Nested
    @DisplayName("Object Printing Tests")
    class ObjectPrintingTests {

        @Test
        @DisplayName("Should pretty print JSON object")
        void prettyPrint_WithValidObject_ShouldFormatJson() {
            // Arrange
            TestObject obj = new TestObject("test", 123);
            
            // Act
            String result = CommonUtils.prettyPrint(objectMapper, obj);
            
            // Assert
            assertNotNull(result);
            assertTrue(result.contains("test"));
            assertTrue(result.contains("123"));
        }

        @Test
        @DisplayName("Should handle null object")
        void prettyPrint_WithNullObject_ShouldReturnEmptyString() {
            assertEquals("", CommonUtils.prettyPrint(objectMapper, null));
        }
    }

    @Nested
    @DisplayName("File Operation Tests")
    class FileOperationTests {

        @Test
        @DisplayName("Should move file successfully")
        void moveFile_WithValidPaths_ShouldMoveFile() throws Exception {
            // Arrange
            Path source = Files.createTempFile(tempDir, "source", ".tmp");
            Path target = tempDir.resolve("target.tmp");
            
            // Act
            boolean result = CommonUtils.moveFile(source, target);
            
            // Assert
            assertTrue(result);
            assertTrue(Files.exists(target));
            assertFalse(Files.exists(source));
            
            // Cleanup
            Files.deleteIfExists(target);
        }

        @Test
        @DisplayName("Should handle file move failure")
        void moveFile_WithInvalidSource_ShouldReturnFalse() {
            // Arrange
            Path source = tempDir.resolve("nonexistent.tmp");
            Path target = tempDir.resolve("target.tmp");
            
            // Act
            boolean result = CommonUtils.moveFile(source, target);
            
            // Assert
            assertFalse(result);
        }
    }

    // Helper class for testing JSON serialization
    private static class TestObject {
        private String name;
        private int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public int getValue() { return value; }
    }
}
```

```java
package com.example.utils;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentUtilsTest {

    @Mock
    private StepAwareService stepAwareService;
    
    private PaymentUtils paymentUtils;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        paymentUtils = new PaymentUtils();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Rejected File Record Tests")
    class RejectedFileRecordTests {
        
        @Test
        @DisplayName("Should create rejected record with file upload")
        void createRecordForRejectedFile_WithFileUpload_ShouldCreateRecord() {
            // Arrange
            PwsFileUpload fileUpload = new PwsFileUpload();
            fileUpload.setFileUploadId("FILE001");
            fileUpload.setCreatedBy("USER001");
            fileUpload.setFileReferenceId("REF001");
            
            when(stepAwareService.getFileUpload()).thenReturn(fileUpload);
            String errorMessage = "File processing error";

            // Act
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedFile(stepAwareService, errorMessage);

            // Assert
            assertNotNull(result);
            assertEquals("Bulk File Rejected", result.getEntityType());
            assertEquals("FILE001", result.getEntityId());
            assertEquals("USER001", result.getCreatedBy());
            assertEquals("REF001", result.getBankReferenceId());
            assertEquals(ErrorCode.PBP_2001, result.getRejectCode());
            assertEquals(errorMessage, result.getErrorDetail());
            assertEquals(Date.valueOf(LocalDate.now()), result.getCreatedDate());
            
            verify(stepAwareService).getFileUpload();
        }

        @Test
        @DisplayName("Should create rejected record without file upload")
        void createRecordForRejectedFile_WithoutFileUpload_ShouldCreateRecord() {
            // Arrange
            when(stepAwareService.getFileUpload()).thenReturn(null);
            when(stepAwareService.getBankEntityId()).thenReturn("BANK001");
            String errorMessage = "Processing error";

            // Act
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedFile(stepAwareService, errorMessage);

            // Assert
            assertNotNull(result);
            assertEquals("BANK001", result.getEntityType());
            assertEquals(ErrorCode.PBP_2001, result.getRejectCode());
            assertEquals(errorMessage, result.getErrorDetail());
            assertEquals(Date.valueOf(LocalDate.now()), result.getCreatedDate());
            
            verify(stepAwareService).getFileUpload();
            verify(stepAwareService).getBankEntityId();
        }
    }

    @Nested
    @DisplayName("Rejected Payment Record Tests")
    class RejectedPaymentRecordTests {
        
        @Test
        @DisplayName("Should create rejected record for payment")
        void createRecordForRejectedPayment_WithValidData_ShouldCreateRecord() {
            // Arrange
            PwsFileUpload fileUpload = new PwsFileUpload();
            fileUpload.setFileUploadId("FILE001");
            fileUpload.setCreatedBy("USER001");
            fileUpload.setFileReferenceId("REF001");
            
            PaymentInformation paymentInfo = new PaymentInformation();
            PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
            bulkTransactions.setDmpBatchNumber("BATCH001");
            paymentInfo.setPwsBulkTransactions(bulkTransactions);
            
            when(stepAwareService.getFileUpload()).thenReturn(fileUpload);
            String errorMessage = "Payment processing error";

            // Act
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedPayment(
                stepAwareService, paymentInfo, errorMessage);

            // Assert
            assertNotNull(result);
            assertEquals("Payment Rejected", result.getEntityType());
            assertEquals("FILE001", result.getEntityId());
            assertEquals("USER001", result.getCreatedBy());
            assertEquals("REF001", result.getBankReferenceId());
            assertEquals(ErrorCode.PBP_2001, result.getRejectCode());
            assertTrue(result.getErrorDetail().contains("BATCH001"));
            assertTrue(result.getErrorDetail().contains(errorMessage));
            assertEquals(Date.valueOf(LocalDate.now()), result.getCreatedDate());
            
            verify(stepAwareService).getFileUpload();
        }
    }

    @Nested
    @DisplayName("Rejected Transaction Record Tests")
    class RejectedTransactionRecordTests {
        
        @Test
        @DisplayName("Should create rejected record for transaction")
        void createRecordForRejectedTransaction_WithValidData_ShouldCreateRecord() {
            // Arrange
            PwsFileUpload fileUpload = new PwsFileUpload();
            fileUpload.setFileUploadId("FILE001");
            fileUpload.setCreatedBy("USER001");
            fileUpload.setFileReferenceId("REF001");
            
            PaymentInformation paymentInfo = new PaymentInformation();
            PwsBulkTransactions bulkTransactions = new PwsBulkTransactions();
            bulkTransactions.setDmpBatchNumber("BATCH001");
            paymentInfo.setPwsBulkTransactions(bulkTransactions);
            
            CreditTransferTransaction transaction = new CreditTransferTransaction();
            transaction.setDmpLineNo("123");
            
            when(stepAwareService.getFileUpload()).thenReturn(fileUpload);
            String errorMessage = "Transaction processing error";

            // Act
            PwsRejectedRecord result = PaymentUtils.createRecordForRejectedTransaction(
                stepAwareService, paymentInfo, transaction, errorMessage);

            // Assert
            assertNotNull(result);
            assertEquals("Transaction Rejected", result.getEntityType());
            assertEquals("FILE001", result.getEntityId());
            assertEquals(123L, result.getLineNo());
            assertEquals("USER001", result.getCreatedBy());
            assertEquals("REF001", result.getBankReferenceId());
            assertEquals(ErrorCode.PBP_2001, result.getRejectCode());
            assertTrue(result.getErrorDetail().contains("BATCH001"));
            assertTrue(result.getErrorDetail().contains(errorMessage));
            assertEquals(Date.valueOf(LocalDate.now()), result.getCreatedDate());
            
            verify(stepAwareService).getFileUpload();
        }
    }

    @Nested
    @DisplayName("PWS Save Record Tests")
    class PwsSaveRecordTests {
        
        @Test
        @DisplayName("Should create PWS save record with valid data")
        void createPwsSaveRecord_WithValidData_ShouldCreateRecord() {
            // Arrange
            long id = 123L;
            String dmpTxnRef = "TXN001";

            // Act
            PwsSaveRecord result = paymentUtils.createPwsSaveRecord(id, dmpTxnRef);

            // Assert
            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals(dmpTxnRef, result.getDmpTxnRef());
        }

        @Test
        @DisplayName("Should create PWS save record with null reference")
        void createPwsSaveRecord_WithNullReference_ShouldCreateRecord() {
            // Arrange
            long id = 123L;

            // Act
            PwsSaveRecord result = paymentUtils.createPwsSaveRecord(id, null);

            // Assert
            assertNotNull(result);
            assertEquals(id, result.getId());
            assertEquals("", result.getDmpTxnRef());
        }
    }

    @Nested
    @DisplayName("Payment Result Update Tests")
    class PaymentResultUpdateTests {
        
        @Test
        @DisplayName("Should update payment saved successfully")
        void updatePaymentSaved_WithValidRecord_ShouldUpdate() {
            // Arrange
            Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
            PwsSaveRecord record = new PwsSaveRecord(123L, "TXN001");

            // Act
            paymentUtils.updatePaymentSaved(result, record);

            // Assert
            assertTrue(result.getPaymentSaved().contains(record));
            assertEquals(1, result.getPaymentSaved().size());
        }

        @Test
        @DisplayName("Should update payment saved error only once")
        void updatePaymentSavedError_WithDuplicateRecord_ShouldUpdateOnce() {
            // Arrange
            Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
            PwsSaveRecord record = new PwsSaveRecord(123L, "TXN001");

            // Act
            paymentUtils.updatePaymentSavedError(result, record);
            paymentUtils.updatePaymentSavedError(result, record);

            // Assert
            assertTrue(result.getPaymentSavedError().contains(record));
            assertEquals(1, result.getPaymentSavedError().size());
        }
    }
}
```
