```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilsTest {

    @Test
    void testStringToDate_validDateString() throws ParseException {
        String dateStr = "2024-12-08";
        Date date = CommonUtils.stringToDate(dateStr);
        assertNotNull(date);
        assertEquals(CommonUtils.ISO_DATE_FORMAT.parse(dateStr).getTime(), date.getTime());
    }

    @Test
    void testStringToDate_nullString() throws ParseException {
        String dateStr = null;
        assertNull(CommonUtils.stringToDate(dateStr));
    }

    @Test
    void testStringToDate_invalidDateString() {
        String dateStr = "invalid-date";
        Executable executable = () -> CommonUtils.stringToDate(dateStr);
        assertThrows(ParseException.class, executable);
    }

    @Test
    void testStringToTimestamp_validDateTimeString() throws ParseException {
        String dateTimeStr = "2024-12-08 15:30:45.123";
        Timestamp timestamp = CommonUtils.stringToTimestamp(dateTimeStr);
        assertNotNull(timestamp);
        assertEquals(CommonUtils.ISO_DATETIME_FORMAT.parse(dateTimeStr).getTime(), timestamp.getTime());
    }

    @Test
    void testStringToTimestamp_nullString() throws ParseException {
        String dateTimeStr = null;
        assertNull(CommonUtils.stringToTimestamp(dateTimeStr));
    }

    @Test
    void testStringToTimestamp_invalidDateTimeString() {
        String dateTimeStr = "invalid-datetime";
        Executable executable = () -> CommonUtils.stringToTimestamp(dateTimeStr);
        assertThrows(ParseException.class, executable);
    }

    @Test
    void testGetShortErrorMessage_withRootCause() {
        Exception rootCause = new IllegalArgumentException("Root cause message");
        Exception wrapper = new RuntimeException("Wrapper exception", rootCause);
        String errorMessage = CommonUtils.getShortErrorMessage(wrapper);
        assertEquals("Root cause message", errorMessage);
    }

    @Test
    void testGetShortErrorMessage_withoutRootCause() {
        Exception exception = new IllegalStateException("No root cause");
        String errorMessage = CommonUtils.getShortErrorMessage(exception);
        assertEquals("No root cause", errorMessage);
    }
}

```

```java
@ExtendWith(MockitoExtension.class)
class PaymentUtilsTest {

    @Mock
    private StepAwareService stepAwareService;

    @InjectMocks
    private PaymentUtils paymentUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreatePwsSaveRecord_WithValidInputs() {
        long id = 123L;
        String dmpTxnRef = "dmpRef123";

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, dmpTxnRef);

        assertAll(
                () -> assertEquals(id, record.getTxnId(), "ID mismatch"),
                () -> assertEquals(dmpTxnRef, record.getDmpTxnRef(), "Transaction reference mismatch")
        );
    }

    @Test
    void testCreatePwsSaveRecord_WithNullDmpTxnRef() {
        long id = 123L;

        PwsSaveRecord record = paymentUtils.createPwsSaveRecord(id, null);

        assertAll(
                () -> assertEquals(id, record.getTxnId(), "ID mismatch"),
                () -> assertEquals("", record.getDmpTxnRef(), "Default transaction reference not set")
        );
    }

    @Test
    void testUpdatePaymentSaved_AddsRecord() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord(123L, "ref123");

        paymentUtils.updatePaymentSaved(result, record);

        assertTrue(result.getPaymentSaved().contains(record), "Record not added to saved payments");
    }

    @Test
    void testUpdatePaymentSavedError_AddsRecordWhenNotPresent() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord(123L, "ref123");

        paymentUtils.updatePaymentSavedError(result, record);

        assertTrue(result.getPaymentSavedError().contains(record), "Record not added to saved payment errors");
    }

    @Test
    void testUpdatePaymentSavedError_DoesNotDuplicateRecord() {
        Pain001InboundProcessingResult result = new Pain001InboundProcessingResult();
        PwsSaveRecord record = new PwsSaveRecord(123L, "ref123");
        result.getPaymentSavedError().add(record);

        paymentUtils.updatePaymentSavedError(result, record);

        assertEquals(1, result.getPaymentSavedError().size(), "Duplicate record added to saved payment errors");
    }

    @Test
    void testCreateRecordForRejectedFile_WithValidFileUpload() {
        PwsFileUpload fileUpload = new PwsFileUpload(123L, "fileRef123", "user123");
        Mockito.when(stepAwareService.getFileUpload()).thenReturn(fileUpload);

        String errMsg = "File rejected due to error";
        PwsRejectedRecord rejectedRecord = PaymentUtils.createRecordForRejectedFile(stepAwareService, errMsg);

        assertAll(
                () -> assertEquals("Bulk File Rejected", rejectedRecord.getEntityType(), "Incorrect entity type"),
                () -> assertEquals(123L, rejectedRecord.getEntityId(), "File ID mismatch"),
                () -> assertEquals("user123", rejectedRecord.getCreatedBy(), "Created by mismatch"),
                () -> assertEquals(errMsg, rejectedRecord.getErrorDetail(), "Error message mismatch")
        );
    }

    @Test
    void testCreateRecordForRejectedFile_WithoutFileUpload() {
        Mockito.when(stepAwareService.getFileUpload()).thenReturn(null);

        String errMsg = "File rejected due to error";
        PwsRejectedRecord rejectedRecord = PaymentUtils.createRecordForRejectedFile(stepAwareService, errMsg);

        assertAll(
                () -> assertEquals(stepAwareService.getBankEntityId(), rejectedRecord.getEntityType(), "Entity type mismatch"),
                () -> assertEquals(errMsg, rejectedRecord.getErrorDetail(), "Error message mismatch")
        );
    }

    @Test
    void testCreateRecordForRejectedPayment() {
        PwsFileUpload fileUpload = new PwsFileUpload(123L, "fileRef123", "user123");
        Mockito.when(stepAwareService.getFileUpload()).thenReturn(fileUpload);

        PaymentInformation paymentInfo = new PaymentInformation();
        paymentInfo.setPwsBulkTransactions(new PwsBulkTransactions("batch123"));

        String errMsg = "Payment rejected due to insufficient funds";
        PwsRejectedRecord rejectedRecord = PaymentUtils.createRecordForRejectedPayment(stepAwareService, paymentInfo, errMsg);

        assertAll(
                () -> assertEquals("Payment Rejected", rejectedRecord.getEntityType(), "Incorrect entity type"),
                () -> assertEquals("batch123", paymentInfo.getPwsBulkTransactions().getDmpBatchNumber(), "Batch number mismatch"),
                () -> assertTrue(rejectedRecord.getErrorDetail().contains(errMsg), "Error message mismatch")
        );
    }

    @Test
    void testCreateRecordForRejectedTransaction() {
        PwsFileUpload fileUpload = new PwsFileUpload(123L, "fileRef123", "user123");
        Mockito.when(stepAwareService.getFileUpload()).thenReturn(fileUpload);

        PaymentInformation paymentInfo = new PaymentInformation();
        paymentInfo.setPwsBulkTransactions(new PwsBulkTransactions("batch123"));

        CreditTransferTransaction txn = new CreditTransferTransaction(456L);

        String errMsg = "Transaction rejected due to limit exceeded";
        PwsRejectedRecord rejectedRecord = PaymentUtils.createRecordForRejectedTransaction(stepAwareService, paymentInfo, txn, errMsg);

        assertAll(
                () -> assertEquals("Transaction Rejected", rejectedRecord.getEntityType(), "Incorrect entity type"),
                () -> assertEquals(456L, rejectedRecord.getLineNo(), "Transaction line number mismatch"),
                () -> assertTrue(rejectedRecord.getErrorDetail().contains(errMsg), "Error message mismatch")
        );
    }
}

```
