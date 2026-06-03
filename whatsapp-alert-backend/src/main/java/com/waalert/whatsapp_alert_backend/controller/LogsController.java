package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.entity.ReportLog;
import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
import com.waalert.whatsapp_alert_backend.repository.ReportLogRepository;
import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogsController {

    private final WhatsAppLogRepository waLogRepo;
    private final ReportLogRepository   reportLogRepo;

    @GetMapping("/whatsapp")
    public ResponseEntity<ApiResponse<Page<WhatsAppLog>>> waLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                waLogRepo.findAllByOrderBySentAtDesc(PageRequest.of(page, size))));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<ReportLog>>> reportLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reportLogRepo.findAllByOrderByGeneratedAtDesc(PageRequest.of(page, size))));
    }
}
