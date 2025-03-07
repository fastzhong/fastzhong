# Handler

```java
public class SingleOnlineReportHandler implements ReportHandler<OnlineReportReq> {
    private final TransactionUtils transactionUtils;
    private final DomesticPaymentWorkflowRoutesService domesticPaymentWorkflowRoutesService;
    private final TransactionWorkflowEnquiryRoutesService enquiryRoutesService;
    private final SingleTransactionReportServiceImpl singleTransactionService;
    private final ReportGenerator reportGenerator;
    private final ReportConfig reportConfig;
    private final QueryReportDataUtil queryReportDataUtil;

    @Autowired
    public SingleOnlineReportHandler(
            TransactionUtils transactionUtils, 
            DomesticPaymentWorkflowRoutesService domesticPaymentWorkflowRoutesService,
            TransactionWorkflowEnquiryRoutesService enquiryRoutesService,
            SingleTransactionReportServiceImpl singleTransactionService,
            ReportGenerator reportGenerator,
            ReportConfig reportConfig,
            QueryReportDataUtil queryReportDataUtil) {
        this.transactionUtils = transactionUtils;
        this.domesticPaymentWorkflowRoutesService = domesticPaymentWorkflowRoutesService;
        this.enquiryRoutesService = enquiryRoutesService;
        this.singleTransactionService = singleTransactionService;
        this.reportGenerator = reportGenerator;
        this.reportConfig = reportConfig;
        this.queryReportDataUtil = queryReportDataUtil;
    }

    @Override
    public MultipartData handler(OnlineReportReq onlineReportReq) {
        log.info("SingleOnlineReportHandler.handler() :: START");
        log.info("ReportConfig {}", reportConfig);

        // Extract report details
        ReportReq reportReq = onlineReportReq.getReportDetails()
                .getReportReqs()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApplicationException("No report request found"));
                
        String transactionId = reportReq.getTransactionDetails().getTransactionId();
        log.info("SingleOnlineReportHandler.handler() :: transactionId = {}", transactionId);
        
        String userId = onlineReportReq.getUserInfo().getUserId();
        log.info("SingleOnlineReportHandler.handler() :: userId = {}", userId);
        
        // Get output format (default to PDF if not specified)
        String outputFormat = onlineReportReq.getReportDetails().getOutputFormat();
        if (outputFormat == null || outputFormat.isEmpty()) {
            outputFormat = ReportGenerator.FORMAT_PDF;
        }
        
        // Get transaction details based on whether it's a parent or child transaction
        TransactionDetailViewResp detailViewResp = fetchTransactionDetails(onlineReportReq, transactionId, userId);
        log.info("SingleOnlineReportHandler.handler() :: detailViewResp {}", detailViewResp);
        
        // Get approval status details if required
        ApprovalStatusLookUpResp approvalStatusDetails = fetchApprovalStatusIfRequired(onlineReportReq, detailViewResp, userId);
        
        // Process child details if needed
        processChildDetailsIfPresent(onlineReportReq, approvalStatusDetails);
        
        // Get bank reference ID
        String bankReferenceId = reportReq.getTransactionDetails().getBankReferenceId();

        // Generate the payment report data
        SingleTransactionPojo stPojo = singleTransactionService.generatePaymentReport(
                detailViewResp,
                IS_WITH_AUDIT_TRAILS.test(onlineReportReq), 
                userId, 
                transactionId, 
                bankReferenceId, 
                null,
                approvalStatusDetails);

        // Prepare the data for the report generator
        List<SingleTransactionPojo> singleTransactionPojos = new ArrayList<>();
        singleTransactionPojos.add(stPojo);
        log.info("SingleOnlineReportHandler.handler() :: SingleTransactionPojo {}", stPojo);

        // Set up report information
        ReportInfo reportInfo = setReportDetailsForReportGenerator(onlineReportReq);
        
        try {
            // Get the report type from the request
            String reportType = reportReq.getReportType();
            
            // Generate the report using the enhanced report generator with the specified format
            byte[] data = reportGenerator.generateReport(reportType, singleTransactionPojos, outputFormat);
            
            // Create the response
            MultipartData multipartResponse = new MultipartData();
            multipartResponse.setContent(data);
            
            // Set an appropriate file extension based on the output format
            String fileExtension = getFileExtension(outputFormat);
            
            // Create a filename
            String localisedReportName = String
                    .valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .concat("_")
                    .concat(reportInfo.getReportType())
                    .concat("_")
                    .concat(LocalDateTime.now().toString())
                    .concat(fileExtension);
                    
            multipartResponse.setFileName(localisedReportName);
            return multipartResponse;
            
        } catch (JRException | IOException e) {
            log.error("Failed to generate report, exception details: ", e);
            throw new ApplicationException("Failed to generate report", e);
        }
    }
    
    /**
     * Fetch the transaction details based on whether it's a parent or child transaction
     */
    private TransactionDetailViewResp fetchTransactionDetails(OnlineReportReq onlineReportReq, String transactionId, String userId) {
        if (!IS_WITH_SINGLE_CHILD.test(onlineReportReq)) {
            log.info("SingleOnlineReportHandler.fetchTransactionDetails() :: isChild = {}", BOOLEAN_N);
            log.info("SingleOnlineReportHandler.fetchTransactionDetails() :: transactionUtils.camelHeaders() = {}",
                    transactionUtils.camelHeaders());
                    
            return domesticPaymentWorkflowRoutesService.getTransactionByTransactionId(
                    transactionId, 
                    userId,
                    BOOLEAN_N, 
                    transactionUtils.camelHeaders());
        } else {
            log.info("SingleOnlineReportHandler.fetchTransactionDetails() :: isChild = {}", BOOLEAN_Y);
            
            String childTransactionId = onlineReportReq.getReportDetails()
                    .getReportReqs()
                    .stream()
                    .findFirst()
                    .get()
                    .getTransactionDetails()
                    .getChildDetails()
                    .get(ZERO)
                    .getChildTransactionId();

            log.info("SingleOnlineReportHandler.fetchTransactionDetails() :: transactionUtils.camelHeaders() = {}",
                    transactionUtils.camelHeaders());
                    
            return domesticPaymentWorkflowRoutesService.getTransactionByTransactionId(
                    childTransactionId,
                    userId, 
                    BOOLEAN_Y, 
                    transactionUtils.camelHeaders());
        }
    }
    
    /**
     * Fetch approval status details if required
     */
    private ApprovalStatusLookUpResp fetchApprovalStatusIfRequired(
            OnlineReportReq onlineReportReq, 
            TransactionDetailViewResp detailViewResp, 
            String userId) {
            
        if (APPROVAL_STATUS_REQUIRED.test(onlineReportReq)) {
            ApprovalStatusLookUpResp approvalStatusDetails = queryReportDataUtil.getApprovalStatusTransaction(
                    detailViewResp, 
                    userId,
                    enquiryRoutesService, 
                    transactionUtils);
                    
            log.info("SingleOnlineReportHandler.fetchApprovalStatusIfRequired() :: approvalStatusDetails {}", 
                    approvalStatusDetails);
                    
            return approvalStatusDetails;
        }
        
        return null;
    }
    
    /**
     * Process child details if present and approval status is available
     */
    private void processChildDetailsIfPresent(OnlineReportReq onlineReportReq, ApprovalStatusLookUpResp approvalStatusDetails) {
        if (IS_WITH_CHILD_DETAILS.test(onlineReportReq) && Objects.nonNull(approvalStatusDetails)) {
            List<ApprovalStatusDetail> transactionDetails = approvalStatusDetails.getTransactions();
            log.info("List of children attached to the transaction {}", transactionDetails);
            // Additional processing can be added here if needed
        }
    }
    
    /**
     * Set up report details for the report generator
     */
    private ReportInfo setReportDetailsForReportGenerator(OnlineReportReq onlineReportReq) {
        if (Objects.nonNull(onlineReportReq)) {
            String reportType = onlineReportReq.getReportDetails()
                    .getReportReqs()
                    .stream()
                    .findFirst()
                    .get()
                    .getReportType();

            String language = onlineReportReq.getReportDetails().getCommonEnrichment().getLanguage();

            return ReportInfo.builder()
                    .reportType(reportType)
                    .language(language)
                    .build();
        }
        return null;
    }
    
    /**
     * Get the appropriate file extension based on output format
     */
    private String getFileExtension(String outputFormat) {
        if (outputFormat == null) {
            return ".pdf";
        }
        
        switch (outputFormat.toLowerCase()) {
            case ReportGenerator.FORMAT_CSV:
                return ".csv";
            case ReportGenerator.FORMAT_EXCEL:
                return ".xlsx";
            case ReportGenerator.FORMAT_PDF:
            default:
                return ".pdf";
        }
    }
}
```

# Generator
```java
@Component
public class CustomTransactionReportProvider implements ReportGenerator.ReportParameterProvider {

    @Autowired
    private ReportGenerator reportGenerator;
    
    @PostConstruct
    public void init() {
        // Register this custom provider for transaction reports
        reportGenerator.registerParameterProvider(ReportGenerator.REPORT_TYPE_TRANSACTION, this);
    }
    
    @Override
    public void addParameters(String reportType, String tab, Map<String, Object> jrparameters) throws JRException, IOException {
        // Add standard address parameters (like the default provider does)
        if (tab.equalsIgnoreCase("customerOnBehalfOf")) {
            JasperReport structuredAddressRpt = loadPreCompiledReport("customer-address-details-structured.jasper");
            JasperReport unStructuredAddressRpt = loadPreCompiledReport("customer-address-details-unStructured.jasper");
            jrparameters.put("customerAddressDetailsStructuredTemplate", structuredAddressRpt);
            jrparameters.put("customerAddressDetailsUnstructuredTemplate", unStructuredAddressRpt);
        }
        if (tab.equalsIgnoreCase("payeeThirdPartyDetails")) {
            JasperReport structuredAddressRpt = loadPreCompiledReport("payee-address-details-structured.jasper");
            JasperReport unStructuredAddressRpt = loadPreCompiledReport("payee-address-details-unStructured.jasper");
            jrparameters.put("payeeAddressDetailsStructuredTemplate", structuredAddressRpt);
            jrparameters.put("payeeAddressDetailsUnstructuredTemplate", unStructuredAddressRpt);
        }
        
        // Add company-specific branding for transaction reports
        if (tab.equalsIgnoreCase("mainTab")) {
            JasperReport companyLogoRpt = loadPreCompiledReport("company-logo.jasper");
            jrparameters.put("companyLogoTemplate", companyLogoRpt);
            
            // Add watermark for draft transactions
            if (jrparameters.containsKey("IS_DRAFT") && Boolean.TRUE.equals(jrparameters.get("IS_DRAFT"))) {
                JasperReport watermarkRpt = loadPreCompiledReport("draft-watermark.jasper");
                jrparameters.put("watermarkTemplate", watermarkRpt);
            }
        }
    }
    
    private JasperReport loadPreCompiledReport(String reportName) throws JRException, IOException {
        ClassPathResource resource = new ClassPathResource("compiledJaspertemplates/" + reportName);
        try (InputStream inputStream = resource.getInputStream()) {
            return (JasperReport) JRLoader.loadObject(inputStream);
        }
    }
}
```

```java
@Component
public class CustomTransactionReportProvider implements ReportGenerator.ReportParameterProvider {

    @Autowired
    private ReportGenerator reportGenerator;
    
    @PostConstruct
    public void init() {
        // Register this custom provider for transaction reports
        reportGenerator.registerParameterProvider(ReportGenerator.REPORT_TYPE_TRANSACTION, this);
    }
    
    @Override
    public void addParameters(String reportType, String tab, Map<String, Object> jrparameters) throws JRException, IOException {
        // Add standard address parameters (like the default provider does)
        if (tab.equalsIgnoreCase("customerOnBehalfOf")) {
            JasperReport structuredAddressRpt = loadPreCompiledReport("customer-address-details-structured.jasper");
            JasperReport unStructuredAddressRpt = loadPreCompiledReport("customer-address-details-unStructured.jasper");
            jrparameters.put("customerAddressDetailsStructuredTemplate", structuredAddressRpt);
            jrparameters.put("customerAddressDetailsUnstructuredTemplate", unStructuredAddressRpt);
        }
        if (tab.equalsIgnoreCase("payeeThirdPartyDetails")) {
            JasperReport structuredAddressRpt = loadPreCompiledReport("payee-address-details-structured.jasper");
            JasperReport unStructuredAddressRpt = loadPreCompiledReport("payee-address-details-unStructured.jasper");
            jrparameters.put("payeeAddressDetailsStructuredTemplate", structuredAddressRpt);
            jrparameters.put("payeeAddressDetailsUnstructuredTemplate", unStructuredAddressRpt);
        }
        
        // Add company-specific branding for transaction reports
        if (tab.equalsIgnoreCase("mainTab")) {
            JasperReport companyLogoRpt = loadPreCompiledReport("company-logo.jasper");
            jrparameters.put("companyLogoTemplate", companyLogoRpt);
            
            // Add watermark for draft transactions
            if (jrparameters.containsKey("IS_DRAFT") && Boolean.TRUE.equals(jrparameters.get("IS_DRAFT"))) {
                JasperReport watermarkRpt = loadPreCompiledReport("draft-watermark.jasper");
                jrparameters.put("watermarkTemplate", watermarkRpt);
            }
        }
    }
    
    private JasperReport loadPreCompiledReport(String reportName) throws JRException, IOException {
        ClassPathResource resource = new ClassPathResource("compiledJaspertemplates/" + reportName);
        try (InputStream inputStream = resource.getInputStream()) {
            return (JasperReport) JRLoader.loadObject(inputStream);
        }
    }
}
```
