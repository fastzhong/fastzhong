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
