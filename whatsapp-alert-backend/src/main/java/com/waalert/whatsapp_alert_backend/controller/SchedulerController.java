package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.dto.SchedulerRequest;
import com.waalert.whatsapp_alert_backend.entity.Schedule;
import com.waalert.whatsapp_alert_backend.service.SchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerService schedulerService;

    @PostMapping
    public ResponseEntity<ApiResponse<Schedule>> create(@Valid @RequestBody SchedulerRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Schedule created", schedulerService.create(req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Schedule>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(schedulerService.getAll()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Schedule>> toggle(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(ApiResponse.success("Updated",
                schedulerService.toggle(id, body.getOrDefault("enabled", true))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        schedulerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
