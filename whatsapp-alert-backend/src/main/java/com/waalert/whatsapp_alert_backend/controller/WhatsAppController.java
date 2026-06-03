// package com.waalert.whatsapp_alert_backend.controller;

// import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.service.WhatsAppService;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/whatsapp")
// @RequiredArgsConstructor
// public class WhatsAppController {

//     private final WhatsAppService whatsAppService;

//     @PostMapping("/send")
//     public ResponseEntity<ApiResponse<List<Map<String, Object>>>> send(
//             @Valid @RequestBody WhatsAppRequest request) {
//         return ResponseEntity.ok(ApiResponse.success(
//                 "Messages processed", whatsAppService.sendMessages(request)));
//     }

//     @PostMapping("/generate-message")
//     public ResponseEntity<ApiResponse<String>> generateMessage(@RequestBody Map<String, Object> body) {
//         @SuppressWarnings("unchecked") List<String> columns = (List<String>) body.get("columns");
//         @SuppressWarnings("unchecked") List<List<Object>> rows = (List<List<Object>>) body.get("rows");
//         String header = body.getOrDefault("header", "Query Report").toString();
//         if (columns == null || rows == null)
//             return ResponseEntity.badRequest().body(ApiResponse.error("'columns' and 'rows' required"));
//         return ResponseEntity.ok(ApiResponse.success(
//                 whatsAppService.formatQueryResultAsMessage(columns, rows, header)));
//     }
// }
// package com.waalert.whatsapp_alert_backend.controller;

// import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
// import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.service.WhatsAppService;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/whatsapp")
// @RequiredArgsConstructor
// public class WhatsAppController {

//     private final WhatsAppService whatsAppService;

//     /** Send a single message to multiple fixed recipients */
//     @PostMapping("/send")
//     public ResponseEntity<ApiResponse<List<Map<String, Object>>>> send(
//             @Valid @RequestBody WhatsAppRequest request) {
//         return ResponseEntity.ok(ApiResponse.success(
//                 "Messages processed", whatsAppService.sendMessages(request)));
//     }

//     /**
//      * ✅ NEW: Send a personalised template message to each row's phone number.
//      *
//      * POST /api/whatsapp/send-template
//      * Body:
//      * {
//      *   "template": "Dear {{name}}, your salary for {{month}} is ₹{{amount}}",
//      *   "rows": [{ "name": "Kamali", "mob_no": "918610256725", "month": "january", "amount": 26444 }],
//      *   "phoneKey": "mob_no"   // optional — auto-detected if omitted
//      * }
//      */
//     @PostMapping("/send-template")
//     public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sendTemplate(
//             @RequestBody TemplateSendRequest request) {

//         if (request.getTemplate() == null || request.getTemplate().isBlank()) {
//             return ResponseEntity.badRequest().body(ApiResponse.error("Template cannot be empty."));
//         }
//         if (request.getRows() == null || request.getRows().isEmpty()) {
//             return ResponseEntity.badRequest().body(ApiResponse.error("Rows cannot be empty."));
//         }

//         List<Map<String, Object>> results = whatsAppService.sendTemplate(request);

//         long sent   = results.stream().filter(r -> "SENT".equals(r.get("status"))).count();
//         long failed = results.stream().filter(r -> "FAILED".equals(r.get("status"))).count();

//         return ResponseEntity.ok(ApiResponse.success(
//                 String.format("Sent: %d | Failed: %d | Skipped: %d",
//                         sent, failed, results.size() - sent - failed),
//                 results
//         ));
//     }

//     /** Format query results as a single WhatsApp message (existing endpoint) */
//     @PostMapping("/generate-message")
//     public ResponseEntity<ApiResponse<String>> generateMessage(@RequestBody Map<String, Object> body) {
//         @SuppressWarnings("unchecked") List<String> columns = (List<String>) body.get("columns");
//         @SuppressWarnings("unchecked") List<List<Object>> rows = (List<List<Object>>) body.get("rows");
//         String header = body.getOrDefault("header", "Query Report").toString();
//         if (columns == null || rows == null)
//             return ResponseEntity.badRequest().body(ApiResponse.error("'columns' and 'rows' required"));
//         return ResponseEntity.ok(ApiResponse.success(
//                 whatsAppService.formatQueryResultAsMessage(columns, rows, header)));
//     }
// }

package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
import com.waalert.whatsapp_alert_backend.service.WhatsAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    /**
     * ✅ NEW: Salary Broadcast Endpoint
     *
     * Sends the "salary_alert" Meta-approved template to EVERY recipient in the
     * supplied rows — no 24-hour interaction check, no prior-message requirement.
     * Works for cold leads as long as they are WhatsApp users.
     *
     * POST /api/whatsapp/broadcast-salary
     * Body:
     * {
     *   "rows": [
     *     { "name": "Kamali",  "mob_no": "919876543210", "month": "May 2026",
     *       "basic": "18000",  "hra": "4000", "total": "22000" },
     *     { "name": "Ravi",    "mob_no": "918610256725", "month": "May 2026",
     *       "basic": "22000",  "hra": "5000", "total": "27000" }
     *   ],
     *   "phoneKey": "mob_no"   // optional — auto-detected if omitted
     * }
     *
     * Response:
     * {
     *   "message": "Sent: 2 | Failed: 0 | Skipped: 0",
     *   "data": [
     *     { "recipient": "919876543210", "status": "SENT", "messageId": "wamid.xxx", "template": "salary_alert" },
     *     ...
     *   ]
     * }
     */
    @PostMapping("/broadcast-salary")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> broadcastSalary(
            @RequestBody TemplateSendRequest request) {

        if (request.getRows() == null || request.getRows().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("'rows' cannot be empty. Supply the salary query results."));
        }

        List<Map<String, Object>> results =
                whatsAppService.sendSalaryBroadcast(request.getRows(), request.getPhoneKey());

        long sent    = results.stream().filter(r -> "SENT".equals(r.get("status"))).count();
        long failed  = results.stream().filter(r -> "FAILED".equals(r.get("status"))).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.get("status"))).count();

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Sent: %d | Failed: %d | Skipped: %d", sent, failed, skipped),
                results
        ));
    }

    /**
     * Legacy Smart Sender — now delegates to sendSalaryBroadcast internally.
     * Kept so existing frontend calls to /send-template continue to work.
     *
     * POST /api/whatsapp/send-template
     */
    @PostMapping("/send-template")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sendTemplate(
            @RequestBody TemplateSendRequest request) {

        if (request.getRows() == null || request.getRows().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Rows cannot be empty."));
        }

        List<Map<String, Object>> results = whatsAppService.sendTemplate(request);

        long sent    = results.stream().filter(r -> "SENT".equals(r.get("status"))).count();
        long failed  = results.stream().filter(r -> "FAILED".equals(r.get("status"))).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.get("status"))).count();

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Sent: %d | Failed: %d | Skipped: %d", sent, failed, skipped),
                results
        ));
    }

    /** Legacy plain-text endpoint — redirects callers to the broadcast endpoint. */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> send(
            @Valid @RequestBody WhatsAppRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Messages processed", whatsAppService.sendMessages(request)));
    }

    /** Format query results as a single WhatsApp message (unchanged). */
    @PostMapping("/generate-message")
    public ResponseEntity<ApiResponse<String>> generateMessage(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked") List<String> columns = (List<String>) body.get("columns");
        @SuppressWarnings("unchecked") List<List<Object>> rows = (List<List<Object>>) body.get("rows");
        String header = body.getOrDefault("header", "Query Report").toString();
        if (columns == null || rows == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("'columns' and 'rows' required"));
        return ResponseEntity.ok(ApiResponse.success(
                whatsAppService.formatQueryResultAsMessage(columns, rows, header)));
    }
}
