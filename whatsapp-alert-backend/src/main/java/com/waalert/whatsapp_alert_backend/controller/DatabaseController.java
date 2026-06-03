package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.dto.QueryRequest;
import com.waalert.whatsapp_alert_backend.service.DatabaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeQuery(
            @Valid @RequestBody QueryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                databaseService.executeQuery(request.getSql(), request.getPage(), request.getPageSize())
        ));
    }

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<String>>> getTables() {
        return ResponseEntity.ok(ApiResponse.success(databaseService.getTables()));
    }

    @GetMapping("/tables/{tableName}/columns")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getColumns(
            @PathVariable String tableName) {
        return ResponseEntity.ok(ApiResponse.success(databaseService.getTableColumns(tableName)));
    }

    @GetMapping("/tables/{tableName}/rows")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRows(
            @PathVariable String tableName) {
        return ResponseEntity.ok(ApiResponse.success(
                databaseService.executeQuery("SELECT * FROM `" + tableName + "` LIMIT 100", 0, 100)
        ));
    }
}
