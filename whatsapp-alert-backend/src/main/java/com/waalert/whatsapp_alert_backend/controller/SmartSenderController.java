package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.service.DatabaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * SmartSenderController
 *
 * POST /api/query/execute-and-detect
 *   Runs a SELECT query and automatically detects the phone-number column.
 *   Returns the full query result plus the detected phoneColumn name.
 */
@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class SmartSenderController {

    private final DatabaseService databaseService;

    /** Column names that are considered phone/WhatsApp recipient columns (case-insensitive). */
    private static final List<String> PHONE_COLUMN_ALIASES = List.of(
        "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
        "contact", "phone_no", "cell", "cell_no", "contact_no"
    );

    @PostMapping("/execute-and-detect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeAndDetect(
            @RequestBody Map<String, Object> body) {

        String sql      = Objects.toString(body.get("sql"), "").trim();
        int page        = body.containsKey("page")     ? Integer.parseInt(body.get("page").toString())     : 0;
        int pageSize    = body.containsKey("pageSize") ? Integer.parseInt(body.get("pageSize").toString()) : 200;

        if (sql.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("SQL query is required."));
        }

        long startMs = System.currentTimeMillis();

        // Execute query (security validation is inside DatabaseService)
        Map<String, Object> queryResult = databaseService.executeQuery(sql, page, pageSize);

        long execMs = System.currentTimeMillis() - startMs;

        // Detect phone column
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) queryResult.get("columns");
        String phoneColumn   = detectPhoneColumn(columns);

        // Build enriched response
        Map<String, Object> result = new LinkedHashMap<>(queryResult);
        result.put("phoneColumn",  phoneColumn);           // null if none detected
        result.put("hasRecipients", phoneColumn != null);
        result.put("executionMs",  execMs);

        // Extract recipient list for UI convenience
        if (phoneColumn != null) {
            @SuppressWarnings("unchecked")
            List<List<Object>> rows = (List<List<Object>>) queryResult.get("rows");
            int colIdx = columns.indexOf(phoneColumn);
            List<String> recipients = new ArrayList<>();
            for (List<Object> row : rows) {
                if (colIdx < row.size() && row.get(colIdx) != null) {
                    recipients.add(row.get(colIdx).toString());
                }
            }
            result.put("recipients", recipients);
        } else {
            result.put("recipients", List.of());
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Query executed. Phone column: " + (phoneColumn != null ? phoneColumn : "not detected"),
                result
        ));
    }

    /**
     * Returns the first column name that matches a known phone alias, or null if none found.
     */
    private String detectPhoneColumn(List<String> columns) {
        if (columns == null) return null;
        for (String col : columns) {
            String lower = col.toLowerCase();
            for (String alias : PHONE_COLUMN_ALIASES) {
                if (lower.equals(alias) || lower.contains(alias)) {
                    return col;
                }
            }
        }
        return null;
    }
}