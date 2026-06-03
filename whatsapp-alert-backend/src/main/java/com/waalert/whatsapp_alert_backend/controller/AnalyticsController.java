package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.service.DatabaseService;
import com.waalert.whatsapp_alert_backend.service.PdfReportService;
import com.waalert.whatsapp_alert_backend.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AnalyticsController
 *
 * POST /api/analytics/generate       — execute SQL + return data for chart rendering
 * POST /api/analytics/download-pdf   — generate analytics PDF
 * POST /api/analytics/send-whatsapp  — generate analytics PDF + send via WhatsApp
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final DatabaseService  databaseService;
    private final PdfReportService pdfReportService;
    private final WhatsAppService  whatsAppService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ─────────────────────────────────────────────────────────
    // POST /api/analytics/generate
    //
    // Executes SQL and enriches the response with:
    //   - column type hints  (TEXT | NUMERIC | DATE)
    //   - suggested chart type  (bar | line | pie | doughnut | scatter)
    //   - x-axis and y-axis column suggestions
    // ─────────────────────────────────────────────────────────

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(
            @RequestBody Map<String, Object> body) {

        String sql = Objects.toString(body.get("sql"), "").trim();
        if (sql.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("SQL query is required."));
        }

        long startMs = System.currentTimeMillis();
        Map<String, Object> queryResult = databaseService.executeQuery(sql, 0, 2000);
        long execMs = System.currentTimeMillis() - startMs;

        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) queryResult.get("columns");

        @SuppressWarnings("unchecked")
        List<List<Object>> rows = (List<List<Object>>) queryResult.get("rows");

        // Determine column types from actual data
        List<String> columnTypes = inferColumnTypes(columns, rows);

        // Smart chart suggestion
        Map<String, String> chartSuggestion = suggestChart(columns, columnTypes, rows);

        Map<String, Object> result = new LinkedHashMap<>(queryResult);
        result.put("columnTypes",     columnTypes);
        result.put("chartSuggestion", chartSuggestion);
        result.put("executionMs",     execMs);

        return ResponseEntity.ok(ApiResponse.success(
                "Analytics data ready. Suggested chart: " + chartSuggestion.get("type"),
                result));
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/analytics/download-pdf
    // ─────────────────────────────────────────────────────────

    @PostMapping("/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestBody Map<String, Object> body) {
        String sql   = Objects.toString(body.get("sql"),   "SELECT 1");
        String title = Objects.toString(body.get("title"), "Analytics Report");

        byte[] pdf      = pdfReportService.generateFromQuery(title, sql);
        String filename = "analytics_" + LocalDateTime.now().format(TS) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/analytics/send-whatsapp
    // Body: { "phoneNumber", "message", "sql", "title" }
    // ─────────────────────────────────────────────────────────

    @PostMapping("/send-whatsapp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendWhatsApp(
            @RequestBody Map<String, String> body) {

        String phone   = body.getOrDefault("phoneNumber", "");
        String message = body.getOrDefault("message", "Analytics report attached.");
        String sql     = body.getOrDefault("sql", "SELECT 1");
        String title   = body.getOrDefault("title", "Analytics Report");

        if (phone.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("phoneNumber is required."));
        }

        String filename = "analytics_" + LocalDateTime.now().format(TS) + ".pdf";
        byte[] pdf = pdfReportService.generateFromQuery(title, sql);

        Map<String, Object> result = whatsAppService.sendPdfDocument(
                phone, message, filename, pdf);

        boolean sent = "SENT".equals(result.get("status"));
        return sent
                ? ResponseEntity.ok(ApiResponse.success("Analytics PDF sent.", result))
                : ResponseEntity.badRequest().body(
                        ApiResponse.error("Send failed: " + result.get("error")));
    }

    // ─────────────────────────────────────────────────────────
    // Chart type auto-detection
    // ─────────────────────────────────────────────────────────

    /**
     * Infers each column's type from the first non-null value.
     * Returns "NUMERIC", "DATE", or "TEXT" for each column.
     */
    private List<String> inferColumnTypes(List<String> columns, List<List<Object>> rows) {
        List<String> types = new ArrayList<>();
        for (int c = 0; c < columns.size(); c++) {
            String type = "TEXT";
            for (List<Object> row : rows) {
                if (c < row.size() && row.get(c) != null) {
                    Object val = row.get(c);
                    if (val instanceof Number) { type = "NUMERIC"; break; }
                    String s = val.toString().trim();
                    if (s.matches("-?\\d+(\\.\\d+)?")) { type = "NUMERIC"; break; }
                    if (s.matches("\\d{4}-\\d{2}-\\d{2}.*") ||
                        s.matches("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*"))
                    { type = "DATE"; break; }
                }
            }
            types.add(type);
        }
        return types;
    }

    /**
     * Suggests the best chart type given column types and data size.
     * Returns a map with: type, xAxis, yAxis
     */
    private Map<String, String> suggestChart(List<String> columns,
                                              List<String> types,
                                              List<List<Object>> rows) {
        if (columns.size() < 2) {
            return Map.of("type", "bar", "xAxis", columns.isEmpty() ? "" : columns.get(0), "yAxis", "");
        }

        String col0Type = types.get(0);
        String col1Type = types.size() > 1 ? types.get(1) : "TEXT";

        int rowCount = rows.size();

        // DATE in col0 → line chart (trends over time)
        if ("DATE".equals(col0Type) && "NUMERIC".equals(col1Type)) {
            return Map.of("type", "line", "xAxis", columns.get(0), "yAxis", columns.get(1));
        }

        // Column name hints → line
        String col0Lower = columns.get(0).toLowerCase();
        if ((col0Lower.contains("month") || col0Lower.contains("date") || col0Lower.contains("year"))
                && "NUMERIC".equals(col1Type)) {
            return Map.of("type", "line", "xAxis", columns.get(0), "yAxis", columns.get(1));
        }

        // TEXT in col0, NUMERIC in col1
        if ("TEXT".equals(col0Type) && "NUMERIC".equals(col1Type)) {
            // Few distinct categories → pie/doughnut
            if (rowCount > 0 && rowCount <= 8) {
                return Map.of("type", "pie", "xAxis", columns.get(0), "yAxis", columns.get(1));
            }
            // Many categories → bar
            return Map.of("type", "bar", "xAxis", columns.get(0), "yAxis", columns.get(1));
        }

        // Both numeric → scatter
        if ("NUMERIC".equals(col0Type) && "NUMERIC".equals(col1Type)) {
            return Map.of("type", "scatter", "xAxis", columns.get(0), "yAxis", columns.get(1));
        }

        // Fallback
        return Map.of("type", "bar",
                "xAxis", columns.get(0),
                "yAxis", columns.size() > 1 ? columns.get(1) : "");
    }
}