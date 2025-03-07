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

```java
public class ReportGenerator {

    public static final String TEMPLATE_PATH = "TEMPLATE_PATH";
    public static final String TITLE = "TITLE";
    public static final String COMPILED_TEMPLATES = "compiledJaspertemplates";
    public static final String HEADER_TEMPLATE = "headerTemplate";
    public static final String FOOTER_TEMPLATE = "footerTemplate";
    public static final String HEADER_JASPER = "header.jasper";
    public static final String FOOTER_JASPER = "footer.jasper";
    public static final String SINGAPORE = "SG";
    public static final String EN = "en";

    // Constants for report types
    public static final String REPORT_TYPE_TRANSACTION = "transaction";
    public static final String REPORT_TYPE_CREDITOR_LIST = "creditorList";
    public static final String REPORT_TYPE_CREDIT_ADVICE = "creditAdvice";
    public static final String REPORT_TYPE_DEBIT_ADVICE = "debitAdvice";
    
    // Constants for export formats
    public static final String FORMAT_PDF = "pdf";
    public static final String FORMAT_CSV = "csv";
    public static final String FORMAT_EXCEL = "excel";
    
    // Template paths mapping
    private static final Map<String, String> TEMPLATE_MAP = Map.of(
        "FT", "/jasper/reportTemplates/single-transaction.jrxml", 
        "BC", "/jasper/reportTemplates/bulk-child-transaction.jrxml", 
        "BK", "/jasper/reportTemplates/bulk-transaction.jrxml",
        "CL", "/jasper/reportTemplates/creditor-list.jrxml",
        "CA", "/jasper/reportTemplates/credit-advice.jrxml",
        "DA", "/jasper/reportTemplates/debit-advice.jrxml"
    );
    
    @Autowired
    private ReportConfig reportConfig;

    /**
     * Generate a report with the specified format
     * 
     * @param reportType Type of report to generate (from constants)
     * @param items Data to include in the report
     * @param outputFormat Format to output the report (PDF, CSV, etc.)
     * @return Byte array containing the generated report
     * @throws IOException If there's an error reading the template
     * @throws JRException If there's an error generating the report
     */
    public byte[] generateReport(String reportType, Object items, String outputFormat) throws IOException, JRException {
        log.info("ReportGenerator.generateReport :: START with type {}, format {}", reportType, outputFormat);
        
        // Default to PDF if no format specified
        if (outputFormat == null || outputFormat.isEmpty()) {
            outputFormat = FORMAT_PDF;
        }
        
        ReportInfo reportInfo = getReportInfo(reportType);
        Map<String, Object> subReportObjects = new HashMap<>();
        subReportObjects.put("items", items);

        // Get configuration for report type
        List<ReportTypePojoConfig> pojoConfigs = reportConfig.getReportTypePojoConfigs();
        ReportTypePojoConfig pojoConfig = pojoConfigs.stream()
                .filter(a -> reportInfo != null && reportInfo.getReportType() != null
                        && reportInfo.getReportType().equalsIgnoreCase(a.getReportType()))
                .findFirst()
                .orElseThrow(() -> new ApplicationException("Failed to load pojo configs for report type: " + reportType));

        String reportName = pojoConfig.getReportName();
        String parentTemplate = pojoConfig.getParentTemplate();
        List<String> enabledTabs = pojoConfig.getEnablePojoClasses();
        
        // Set up locale and parameters
        Locale locale = new Locale(EN, SINGAPORE);
        Map<String, Object> jrparameters = new HashMap<>();
        ResourceBundle discoverProperties = ResourceBundle.getBundle("export.reportsDiscover");
        setDefaultJrParameters(jrparameters, locale, reportName);

        // Load all required subreport templates
        for (String tab : enabledTabs) {
            Object jaspertemplate = discoverProperties.getObject(new StringBuilder(tab).append(".jasper").toString());
            // For address-related tabs, handle structured/unstructured address templates
            if (tab.equalsIgnoreCase("customerOnBehalfOf") || tab.equalsIgnoreCase("payeeThirdPartyDetails")) {
                setAddressesForPayeeAndCustomer(tab, jrparameters);
            }
            StringBuilder templateName = new StringBuilder(tab).append("Template");
            JasperReport rpt = loadPreCompiledReport(jaspertemplate.toString());
            jrparameters.put(templateName.toString(), rpt);
        }

        // Create data source from the provided items
        JRMapCollectionDataSource mapDataSource = new JRMapCollectionDataSource(List.of(subReportObjects));
        
        // Fill the report with data
        JasperPrint jasperPrint = JasperFillManager.fillReport(loadMainTemplate(parentTemplate), jrparameters, mapDataSource);
        
        // Export to the requested format
        return exportReport(jasperPrint, outputFormat);
    }
    
    /**
     * Overloaded method for backward compatibility - defaults to PDF format
     */
    public byte[] generateReport(String reportType, Object items) throws IOException, JRException {
        return generateReport(reportType, items, FORMAT_PDF);
    }
    
    /**
     * Export the JasperPrint to the specified format
     */
    private byte[] exportReport(JasperPrint jasperPrint, String outputFormat) throws JRException {
        switch (outputFormat.toLowerCase()) {
            case FORMAT_PDF:
                return JasperExportManager.exportReportToPdf(jasperPrint);
                
            case FORMAT_CSV:
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JRCsvExporter exporter = new JRCsvExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleWriterExporterOutput(baos));
                
                SimpleCsvExporterConfiguration configuration = new SimpleCsvExporterConfiguration();
                configuration.setFieldDelimiter(",");
                exporter.setConfiguration(configuration);
                exporter.exportReport();
                return baos.toByteArray();
                
            case FORMAT_EXCEL:
                ByteArrayOutputStream xlsOut = new ByteArrayOutputStream();
                JRXlsExporter xlsExporter = new JRXlsExporter();
                xlsExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(xlsOut));
                
                SimpleXlsReportConfiguration xlsConfig = new SimpleXlsReportConfiguration();
                xlsConfig.setOnePagePerSheet(false);
                xlsConfig.setRemoveEmptySpaceBetweenRows(true);
                xlsConfig.setDetectCellType(true);
                xlsConfig.setWhitePageBackground(false);
                xlsExporter.setConfiguration(xlsConfig);
                xlsExporter.exportReport();
                return xlsOut.toByteArray();
                
            default:
                log.warn("Unsupported export format: {}. Defaulting to PDF.", outputFormat);
                return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }

    private ReportInfo getReportInfo(String reportType) {
        List<ReportTypeProperty> reportTypeProperties = reportConfig.getReportTypeProperties()
                .stream()
                .filter(e -> e.getReportType().equalsIgnoreCase(reportType))
                .toList();
                
        if (reportTypeProperties.isEmpty()) {
            throw new ApplicationException("No report type properties found for: " + reportType);
        }
        
        ReportTypeProperty reportTypeProperty = reportTypeProperties.get(0);
        String resourceId = reportTypeProperty.getProductType();
        String featureId = reportTypeProperty.getReportCategory();
        return ReportInfo.builder().reportType(reportType).resourceId(resourceId).featureId(featureId).build();
    }

    private JasperReport loadPreCompiledReport(String reportName) throws JRException, IOException {
        ClassPathResource resource = new ClassPathResource(COMPILED_TEMPLATES + "/" + reportName);
        try (InputStream inputStream = resource.getInputStream()) {
            return (JasperReport) JRLoader.loadObject(inputStream);
        }
    }

    private JasperReport loadMainTemplate(String parentTemplate) throws JRException {
        try {
            ClassPathResource resource = new ClassPathResource("/jasper/reportTemplates/" + parentTemplate);
            try (InputStream inputStream = resource.getInputStream()) {
                return JasperCompileManager.compileReport(inputStream);
            }
        } catch (IOException e) {
            log.error("ReportGenerator.loadMainTemplate :: Template path is not correct", e);
            throw new ApplicationException("Failed to load report template: " + parentTemplate, e);
        }
    }

    private String getTemplatePath(String reportType) {
        for (Map.Entry<String, String> entry : TEMPLATE_MAP.entrySet()) {
            if (reportType.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new ApplicationException("No template found for report type: " + reportType);
    }

    private void setAddressesForPayeeAndCustomer(String tabs, Map<String, Object> jrparameters)
            throws JRException, IOException {
        if (tabs.equalsIgnoreCase("customerOnBehalfOf")) {
            JasperReport structuredAddressRpt = loadPreCompiledReport("customer-address-details-structured.jasper");
            JasperReport unStructuredAddressRpt = loadPreCompiledReport("customer-address-details-unStructured.jasper");
            jrparameters.put("customerAddressDetailsStructuredTemplate", structuredAddressRpt);
            jrparameters.put("customerAddressDetailsUnstructuredTemplate", unStructuredAddressRpt);
        }
        if (tabs.equalsIgnoreCase("payeeThirdPartyDetails")) {
            JasperReport structuredAddressRpt = loadPreCompiledReport("payee-address-details-structured.jasper");
            JasperReport unStructuredAddressRpt = loadPreCompiledReport("payee-address-details-unStructured.jasper");
            jrparameters.put("payeeAddressDetailsStructuredTemplate", structuredAddressRpt);
            jrparameters.put("payeeAddressDetailsUnstructuredTemplate", unStructuredAddressRpt);
        }
    }

    private void setDefaultJrParameters(Map<String, Object> jrparameters, Locale locale, String localisedReportName)
            throws JRException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ResourceBundle resourceBundle = ResourceBundle.getBundle("export.reportsLocalization", locale);
        jrparameters.put(TEMPLATE_PATH, new ClassPathResource("/jasper").getPath() + "/");
        jrparameters.put(JRParameter.REPORT_LOCALE, locale);
        jrparameters.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone("Asia/Singapore"));
        jrparameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, resourceBundle);
        jrparameters.put(TITLE, localisedReportName);
        jrparameters.put(HEADER_TEMPLATE, loadPreCompiledReport(HEADER_JASPER));
        jrparameters.put(FOOTER_TEMPLATE, loadPreCompiledReport(FOOTER_JASPER));
    }
}
```
