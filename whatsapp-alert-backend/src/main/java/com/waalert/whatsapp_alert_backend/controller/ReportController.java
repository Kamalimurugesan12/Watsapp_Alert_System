// package com.waalert.whatsapp_alert_backend.controller;

// import com.waalert.whatsapp_alert_backend.service.ReportService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.io.IOException;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/reports")
// @RequiredArgsConstructor
// public class ReportController {

//     private final ReportService reportService;

//     @PostMapping("/excel")
//     public ResponseEntity<byte[]> excel(@RequestBody Map<String, String> body) throws IOException {
//         String sql  = body.getOrDefault("sql",        "SELECT 1");
//         String name = body.getOrDefault("reportName", "Report");
//         byte[] data = reportService.generateExcel(name, sql);
//         return ResponseEntity.ok()
//                 .header(HttpHeaders.CONTENT_DISPOSITION,
//                         "attachment; filename=\"" + name.replaceAll("[^a-zA-Z0-9_]", "_") + ".xlsx\"")
//                 .contentType(MediaType.parseMediaType(
//                         "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
//                 .body(data);
//     }

//     @PostMapping("/csv")
//     public ResponseEntity<byte[]> csv(@RequestBody Map<String, String> body) {
//         String sql  = body.getOrDefault("sql",        "SELECT 1");
//         String name = body.getOrDefault("reportName", "Report");
//         byte[] data = reportService.generateCsv(sql);
//         return ResponseEntity.ok()
//                 .header(HttpHeaders.CONTENT_DISPOSITION,
//                         "attachment; filename=\"" + name.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv\"")
//                 .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
//                 .body(data);
//     }
// }
package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.dto.ReportTemplateDto;
import com.waalert.whatsapp_alert_backend.entity.ReportTemplate;
import com.waalert.whatsapp_alert_backend.repository.ReportTemplateRepository;
import com.waalert.whatsapp_alert_backend.service.PdfReportService;
import com.waalert.whatsapp_alert_backend.service.ReportService;
import com.waalert.whatsapp_alert_backend.service.WhatsAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ReportController — full replacement.
 *
 * Existing endpoints (unchanged):
 *   POST /api/reports/excel
 *   POST /api/reports/csv
 *
 * New endpoints:
 *   GET    /api/report-templates           — list all saved templates
 *   POST   /api/report-templates           — save a new template
 *   DELETE /api/report-templates/{id}      — soft-delete a template
 *   POST   /api/reports/download-pdf       — generate + download PDF
 *   POST   /api/reports/send-whatsapp-pdf  — generate PDF + send via WhatsApp
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService            reportService;
    private final PdfReportService         pdfReportService;
    private final ReportTemplateRepository templateRepository;
    private final WhatsAppService          whatsAppService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ─────────────────────────────────────────────────────────
    // EXISTING ENDPOINTS (unchanged behaviour)
    // ─────────────────────────────────────────────────────────

    @PostMapping("/api/reports/excel")
    public ResponseEntity<byte[]> excel(@RequestBody Map<String, String> body) throws IOException {
        String sql  = body.getOrDefault("sql",        "SELECT 1");
        String name = body.getOrDefault("reportName", "Report");
        byte[] data = reportService.generateExcel(name, sql);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safe(name) + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/api/reports/csv")
    public ResponseEntity<byte[]> csv(@RequestBody Map<String, String> body) {
        String sql  = body.getOrDefault("sql",        "SELECT 1");
        String name = body.getOrDefault("reportName", "Report");
        byte[] data = reportService.generateCsv(sql);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safe(name) + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    // ─────────────────────────────────────────────────────────
    // ✅ TEMPLATE CRUD
    // ─────────────────────────────────────────────────────────

    /**
     * GET /api/report-templates
     * Returns all active templates for the dropdown.
     */
    @GetMapping("/api/report-templates")
    public ResponseEntity<ApiResponse<List<ReportTemplate>>> listTemplates() {
        return ResponseEntity.ok(ApiResponse.success(
                templateRepository.findByIsActiveTrueOrderByCreatedAtDesc()));
    }

    /**
     * POST /api/report-templates
     * Save a new SQL query template.
     * Returns 409 if a template with the same name already exists.
     */
    @PostMapping("/api/report-templates")
    public ResponseEntity<ApiResponse<ReportTemplate>> saveTemplate(
            @Valid @RequestBody ReportTemplateDto dto) {

        if (templateRepository.existsByNameIgnoreCase(dto.getName())) {
            return ResponseEntity.status(409).body(ApiResponse.error(
                    "A template named '" + dto.getName() + "' already exists. " +
                    "Use a different name or delete the existing one first."));
        }

        ReportTemplate saved = templateRepository.save(ReportTemplate.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .sqlQuery(dto.getSqlQuery().trim())
                .databaseType(dto.getDatabaseType() != null ? dto.getDatabaseType() : "MYSQL")
                .build());

        log.info("Report template saved: '{}'", saved.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Template '" + saved.getName() + "' saved successfully.", saved));
    }

    /**
     * DELETE /api/report-templates/{id}
     * Soft-delete (sets is_active = false).
     */
    @DeleteMapping("/api/report-templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        return templateRepository.findById(id).map(t -> {
            t.setIsActive(false);
            templateRepository.save(t);
            log.info("Template '{}' deactivated", t.getName());
            return ResponseEntity.ok(ApiResponse.<Void>success(
                    "Template '" + t.getName() + "' deleted.", null));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────
    // ✅ PDF DOWNLOAD
    // ─────────────────────────────────────────────────────────

    /**
     * POST /api/reports/download-pdf
     * Body: { "sql": "...", "reportName": "..." }
     * Returns: application/pdf binary stream.
     */
    @PostMapping("/api/reports/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestBody Map<String, String> body) {
        String sql  = body.getOrDefault("sql",        "SELECT 1");
        String name = body.getOrDefault("reportName", "Report");
        String filename = "report_" + LocalDateTime.now().format(TS) + ".pdf";

        byte[] pdf = pdfReportService.generateFromQuery(name, sql);
        log.info("PDF download: '{}', {} bytes", filename, pdf.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─────────────────────────────────────────────────────────
    // ✅ SEND PDF VIA WHATSAPP
    // ─────────────────────────────────────────────────────────

    /**
     * POST /api/reports/send-whatsapp-pdf
     * Body:
     * {
     *   "phoneNumber": "+919876543210",
     *   "message":     "Please find the report attached.",
     *   "query":       "SELECT * FROM employees",
     *   "reportName":  "Employee Report"
     * }
     */
    @PostMapping("/api/reports/send-whatsapp-pdf")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendWhatsAppPdf(
            @RequestBody Map<String, String> body) {

        String phone      = body.getOrDefault("phoneNumber", "");
        String message    = body.getOrDefault("message",     "Report attached.");
        String sql        = body.getOrDefault("query",       "SELECT 1");
        String reportName = body.getOrDefault("reportName",  "Report");

        if (phone.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("phoneNumber is required."));
        }

        String filename = "report_" + LocalDateTime.now().format(TS) + ".pdf";

        // 1 — Generate PDF
        byte[] pdf = pdfReportService.generateFromQuery(reportName, sql);

        // 2 — Send via WhatsApp
        Map<String, Object> result = whatsAppService.sendPdfDocument(
                phone, message, filename, pdf);

        boolean sent = "SENT".equals(result.get("status"));
        return sent
                ? ResponseEntity.ok(ApiResponse.success("PDF sent via WhatsApp.", result))
                : ResponseEntity.badRequest().body(ApiResponse.error(
                        "Failed to send PDF: " + result.get("error")));
    }

    // ─────────────────────────────────────────────────────────

    private String safe(String s) {
        return s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}