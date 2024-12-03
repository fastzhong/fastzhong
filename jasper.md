java
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.HashMap;
import net.sf.jasperreports.engine.*;

public class ReportGenerator {

    public void generateReport(String reportPath, Locale locale) {
        try {
            // Load the resource bundle
            ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", locale);

            // Prepare parameters
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, resourceBundle);
            parameters.put(JRParameter.REPORT_LOCALE, locale);

            // Fill the report
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                reportPath, parameters, new JREmptyDataSource()
            );

            // Export to PDF
            JasperExportManager.exportReportToPdfFile(jasperPrint, "output.pdf");

        } catch (JRException e) {
            e.printStackTrace();
        }
    }
}

java
import org.springframework.context.MessageSource;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

public class SpringResourceBundle extends ResourceBundle {

    private final MessageSource messageSource;
    private final Locale locale;

    public SpringResourceBundle(MessageSource messageSource, Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    @Override
    protected Object handleGetObject(String key) {
        return messageSource.getMessage(key, null, locale);
    }

    @Override
    public Enumeration<String> getKeys() {
        // JasperReports doesn't require keys enumeration for message resolution
        return null;
    }
}


import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class JasperReportService {

    @Autowired
    private MessageSource messageSource;

    public JasperPrint generateReport(String reportPath, Map<String, Object> reportParameters, Locale locale) throws JRException {
        // Custom ResourceBundle based on Spring's MessageSource
        ResourceBundle resourceBundle = new SpringResourceBundle(messageSource, locale);

        // Add resource bundle to report parameters
        reportParameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, resourceBundle);
        reportParameters.put(JRParameter.REPORT_LOCALE, locale);

        // Fill the report
        JasperReport jasperReport = JasperCompileManager.compileReport(reportPath);
        return JasperFillManager.fillReport(jasperReport, reportParameters, new JREmptyDataSource());
    }
}
'''

// Project structure
src/
  main/
    java/
      com/example/reportengine/
        config/
          JasperReportConfig.java
        controller/
          ReportController.java
        model/
          report/
            BaseReportData.java
            SingleTxnReportData.java
            BulkTxnReportData.java
            BeneficiaryReportData.java
            TaxReportData.java
          enums/
            ReportType.java
            ReportFormat.java
            Language.java
        service/
          ReportGenerationService.java
          ReportDataPreparationService.java
          LocalizationService.java
        repository/
          ReportTemplateRepository.java
    resources/
      reports/
        templates/
          single_txn_summary.jrxml
          single_txn_detail.jrxml
          bulk_txn_summary.jrxml
          bulk_txn_detail.jrxml
          beneficiary_report.jrxml
          tax_subreport.jrxml
      i18n/
        messages_en.properties
        messages_th.properties
        messages_zh.properties

// Key Classes Implementation
@Configuration
public class JasperReportConfig {
    @Bean
    public JasperReportsConfiguration jasperReportsConfiguration() {
        // Configuration for JasperReports
        SimpleJasperReportsConfiguration config = new SimpleJasperReportsConfiguration();
        config.setTemplateFolder("classpath:reports/templates/");
        return config;
    }
}

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @Autowired
    private ReportGenerationService reportGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(
            @RequestParam ReportType reportType,
            @RequestParam ReportFormat format,
            @RequestParam Language language,
            @RequestBody ReportParameters parameters) {
        
        if (format == ReportFormat.HTML) {
            String html = reportGenerationService.generateOnlineReport(
                reportType, language, parameters);
            return ResponseEntity.ok(html);
        } else {
            String pdfPath = reportGenerationService.generateOfflineReport(
                reportType, language, parameters);
            return ResponseEntity.ok(Map.of("pdfPath", pdfPath));
        }
    }
}

public enum ReportType {
    SINGLE_TXN_SUMMARY,
    SINGLE_TXN_DETAIL,
    BULK_TXN_SUMMARY,
    BULK_TXN_DETAIL,
    BENEFICIARY_MANAGEMENT
}

public enum ReportFormat {
    HTML,
    PDF
}

@Service
@Slf4j
public class ReportGenerationService {
    @Autowired
    private ReportDataPreparationService dataPreparationService;
    
    @Autowired
    private LocalizationService localizationService;
    
    @Autowired
    private JasperReportsConfiguration jasperConfig;

    public String generateOnlineReport(
            ReportType reportType,
            Language language,
            ReportParameters parameters) {
        
        // 1. Prepare data
        BaseReportData reportData = dataPreparationService
            .prepareReportData(reportType, parameters);
        
        // 2. Get template
        JasperReport mainReport = getCompiledTemplate(reportType);
        JasperReport taxSubReport = getCompiledTemplate("tax_subreport");
        
        // 3. Prepare parameters
        Map<String, Object> jasperParams = new HashMap<>();
        jasperParams.put("SUBREPORT_DIR", jasperConfig.getTemplateFolder());
        jasperParams.put("TAX_SUBREPORT", taxSubReport);
        jasperParams.put("SORT_FIELD", parameters.getSortField());
        jasperParams.put("LOCALE", language.getLocale());
        jasperParams.putAll(localizationService.getLocalizedLabels(language));
        
        // 4. Create datasource
        JRDataSource dataSource = new JRBeanCollectionDataSource(
            Collections.singletonList(reportData));
        
        // 5. Generate report
        JasperPrint jasperPrint = JasperFillManager.fillReport(
            mainReport, jasperParams, dataSource);
        
        // 6. Convert to HTML
        return HtmlExporter.export(jasperPrint);
    }

    public String generateOfflineReport(
            ReportType reportType,
            Language language,
            ReportParameters parameters) {
        
        // Similar to online report generation
        JasperPrint jasperPrint = // ... same as above
        
        // Export to PDF
        String pdfPath = generatePdfPath(reportType, parameters);
        JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
        return pdfPath;
    }
}

@Service
public class LocalizationService {
    @Autowired
    private MessageSource messageSource;

    public Map<String, String> getLocalizedLabels(Language language) {
        Map<String, String> labels = new HashMap<>();
        // Load labels from message source
        return labels;
    }

    public String getLocalizedValue(String key, Language language) {
        return messageSource.getMessage(key, null, language.getLocale());
    }
}

public abstract class BaseReportData {
    private String reportId;
    private Date generationDate;
    private TaxReportData taxData;
    // Common fields
}

public class SingleTxnReportData extends BaseReportData {
    private String transactionId;
    private BigDecimal amount;
    private String status;
    private Date transactionDate;
    // Additional fields
}

public class BulkTxnReportData extends BaseReportData {
    private List<SingleTxnReportData> transactions;
    private BigDecimal totalAmount;
    private int totalCount;
    // Additional fields
}
