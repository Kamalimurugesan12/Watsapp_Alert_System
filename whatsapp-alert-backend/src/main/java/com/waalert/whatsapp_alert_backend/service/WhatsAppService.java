// package com.waalert.whatsapp_alert_backend.service;

// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
// import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;

// import java.util.*;

// @Service
// @Slf4j
// public class WhatsAppService {

//     private final WhatsAppLogRepository logRepository;
//     private final WebClient webClient;

//     @Value("${whatsapp.access-token}")
//     private String accessToken;

//     @Value("${whatsapp.phone-number-id}")
//     private String phoneNumberId;

//     public WhatsAppService(WhatsAppLogRepository logRepository,
//                            @Qualifier("whatsAppWebClient") WebClient webClient) {
//         this.logRepository = logRepository;
//         this.webClient     = webClient;
//     }

//     public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         for (String raw : request.getRecipients()) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(request.getMessage())
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> response = callApi(buildPayload(phone, request.getMessage()));
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT", "messageId", mid));
//                 log.info("WhatsApp sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("WhatsApp failed for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     public String formatQueryResultAsMessage(List<String> columns, List<List<Object>> rows, String header) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 *").append(header).append("*\n");
//         sb.append("━".repeat(28)).append("\n");
//         sb.append("*").append(String.join(" | ", columns)).append("*\n").append("─".repeat(28)).append("\n");
//         int limit = Math.min(rows.size(), 20);
//         for (int i = 0; i < limit; i++) {
//             List<String> vals = new ArrayList<>();
//             for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
//             sb.append(String.join(" | ", vals)).append("\n");
//         }
//         if (rows.size() > 20) sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
//         sb.append("━".repeat(28)).append("\n_Sent by WA Alert System_");
//         return sb.toString();
//     }

//     private Map<String, Object> buildPayload(String to, String body) {
//         return Map.of(
//             "messaging_product", "whatsapp",
//             "recipient_type",    "individual",
//             "to",                to,
//             "type",              "text",
//             "text",              Map.of("body", body, "preview_url", false)
//         );
//     }

//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callApi(Map<String, Object> payload) {
//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     @SuppressWarnings("unchecked")
//     private String extractId(Map<String, Object> response) {
//         if (response == null) return "unknown";
//         List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
//         if (messages != null && !messages.isEmpty()) {
//             Object id = messages.get(0).get("id");
//             return id != null ? id.toString() : "unknown";
//         }
//         return "unknown";
//     }

//     private String sanitize(String phone) {
//         return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
//     }
// }
// package com.waalert.whatsapp_alert_backend.service;

// import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
// import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;

// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @Service
// @Slf4j
// public class WhatsAppService {

//     private final WhatsAppLogRepository logRepository;
//     private final WebClient webClient;

//     @Value("${whatsapp.access-token}")
//     private String accessToken;

//     @Value("${whatsapp.phone-number-id}")
//     private String phoneNumberId;

//     /** Column names treated as phone number fields (case-insensitive). */
//     private static final List<String> PHONE_KEYS = List.of(
//         "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
//         "contact", "phone_no", "cell", "cell_no", "contact_no"
//     );

//     public WhatsAppService(WhatsAppLogRepository logRepository,
//                            @Qualifier("whatsAppWebClient") WebClient webClient) {
//         this.logRepository = logRepository;
//         this.webClient     = webClient;
//     }

//     // ─────────────────────────────────────────────────────────
//     // Existing: send list of messages to fixed recipients
//     // ─────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         for (String raw : request.getRecipients()) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(request.getMessage())
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> response = callApi(buildPayload(phone, request.getMessage()));
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT", "messageId", mid));
//                 log.info("WhatsApp sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("WhatsApp failed for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────
//     // NEW: Template-based sending — one personalised message per row
//     // POST /api/whatsapp/send-template
//     // ─────────────────────────────────────────────────────────

//     /**
//      * For each row in the request:
//      *  1. Find the phone number column.
//      *  2. Replace {{placeholders}} in the template with row values.
//      *  3. Send a personalised WhatsApp message.
//      *
//      * @return list of per-recipient results with status
//      */
//     public List<Map<String, Object>> sendTemplate(TemplateSendRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();

//         if (request.getRows() == null || request.getRows().isEmpty()) {
//             return results;
//         }

//         for (Map<String, Object> row : request.getRows()) {
//             // 1 — Detect phone number
//             String phone = resolvePhone(row, request.getPhoneKey());
//             if (phone == null || phone.isBlank()) {
//                 results.add(Map.of("status", "SKIPPED", "reason", "No phone number found in row", "row", row));
//                 continue;
//             }
//             phone = sanitize(phone);

//             // 2 — Personalise template
//             String message = fillTemplate(request.getTemplate(), row);

//             // 3 — Send
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(message)
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> response = callApi(buildPayload(phone, message));
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT",
//                                    "messageId", mid, "message", message));
//                 log.info("Template sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("Template send failed for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────
//     // Template engine — replaces {{key}} with row[key]
//     // ─────────────────────────────────────────────────────────

//     private String fillTemplate(String template, Map<String, Object> row) {
//         if (template == null) return "";
//         Pattern p = Pattern.compile("\\{\\{(\\w+)}}");
//         Matcher m = p.matcher(template);
//         StringBuilder sb = new StringBuilder();
//         while (m.find()) {
//             String key = m.group(1);
//             // Case-insensitive lookup
//             String value = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(key))
//                     .map(e -> e.getValue() != null ? e.getValue().toString() : "")
//                     .findFirst()
//                     .orElse("{{" + key + "}}");        // leave placeholder if not found
//             m.appendReplacement(sb, Matcher.quoteReplacement(value));
//         }
//         m.appendTail(sb);
//         return sb.toString();
//     }

//     /**
//      * Resolve the phone number from a row map.
//      * Uses the explicit phoneKey if provided, otherwise auto-detects.
//      */
//     private String resolvePhone(Map<String, Object> row, String phoneKey) {
//         if (phoneKey != null && !phoneKey.isBlank()) {
//             Object v = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(phoneKey))
//                     .map(Map.Entry::getValue)
//                     .findFirst().orElse(null);
//             return v != null ? v.toString() : null;
//         }
//         // Auto-detect from known aliases
//         for (String alias : PHONE_KEYS) {
//             for (Map.Entry<String, Object> entry : row.entrySet()) {
//                 if (entry.getKey().equalsIgnoreCase(alias) && entry.getValue() != null) {
//                     return entry.getValue().toString();
//                 }
//             }
//         }
//         return null;
//     }

//     // ─────────────────────────────────────────────────────────
//     // Existing utility methods (unchanged)
//     // ─────────────────────────────────────────────────────────

//     public String formatQueryResultAsMessage(List<String> columns, List<List<Object>> rows, String header) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 *").append(header).append("*\n");
//         sb.append("━".repeat(28)).append("\n");
//         sb.append("*").append(String.join(" | ", columns)).append("*\n").append("─".repeat(28)).append("\n");
//         int limit = Math.min(rows.size(), 20);
//         for (int i = 0; i < limit; i++) {
//             List<String> vals = new ArrayList<>();
//             for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
//             sb.append(String.join(" | ", vals)).append("\n");
//         }
//         if (rows.size() > 20) sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
//         sb.append("━".repeat(28)).append("\n_Sent by WA Alert System_");
//         return sb.toString();
//     }

//     private Map<String, Object> buildPayload(String to, String body) {
//         return Map.of(
//             "messaging_product", "whatsapp",
//             "recipient_type",    "individual",
//             "to",                to,
//             "type",              "text",
//             "text",              Map.of("body", body, "preview_url", false)
//         );
//     }

//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callApi(Map<String, Object> payload) {
//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     @SuppressWarnings("unchecked")
//     private String extractId(Map<String, Object> response) {
//         if (response == null) return "unknown";
//         List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
//         if (messages != null && !messages.isEmpty()) {
//             Object id = messages.get(0).get("id");
//             return id != null ? id.toString() : "unknown";
//         }
//         return "unknown";
//     }

//     private String sanitize(String phone) {
//         return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
//     }
// }

// package com.waalert.whatsapp_alert_backend.service;

// import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
// import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.http.client.MultipartBodyBuilder;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;

// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// /**
//  * WhatsAppService
//  * ──────────────────────────────────────────────────────────────
//  * Handles all WhatsApp message sending via Meta Cloud API.
//  *
//  * Methods:
//  *  sendMessages()         - plain text to fixed recipients
//  *  sendTemplate()         - personalised per-row template messages
//  *  sendPdfAlert()         ← NEW: send PDF as document attachment + caption
//  *  formatQueryResultAsMessage() - format table as WhatsApp text
//  *  uploadMedia()          ← NEW: upload PDF to Meta media endpoint
//  */
// @Service
// @Slf4j
// public class WhatsAppService {

//     private final WhatsAppLogRepository logRepository;
//     private final WebClient             webClient;

//     @Value("${whatsapp.access-token}")
//     private String accessToken;

//     @Value("${whatsapp.phone-number-id}")
//     private String phoneNumberId;

//     /** Columns treated as phone number fields (case-insensitive). */
//     private static final List<String> PHONE_KEYS = List.of(
//         "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
//         "contact", "phone_no", "cell", "cell_no", "contact_no"
//     );

//     public WhatsAppService(WhatsAppLogRepository logRepository,
//                            @Qualifier("whatsAppWebClient") WebClient webClient) {
//         this.logRepository = logRepository;
//         this.webClient     = webClient;
//     }

//     // ─────────────────────────────────────────────────────────
//     // sendMessages() — plain text to fixed recipients (existing, unchanged)
//     // ─────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         for (String raw : request.getRecipients()) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(request.getMessage())
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> response = callTextApi(phone, request.getMessage());
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT", "messageId", mid));
//                 log.info("WhatsApp text sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("Text send failed for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────
//     // sendTemplate() — personalised per-row messages (existing, unchanged)
//     // ─────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendTemplate(TemplateSendRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         if (request.getRows() == null || request.getRows().isEmpty()) return results;

//         for (Map<String, Object> row : request.getRows()) {
//             String phone = resolvePhone(row, request.getPhoneKey());
//             if (phone == null || phone.isBlank()) {
//                 results.add(Map.of("status", "SKIPPED",
//                                    "reason", "No phone number found in row", "row", row));
//                 continue;
//             }
//             phone = sanitize(phone);
//             String message = fillTemplate(request.getTemplate(), row);

//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone).messageBody(message)
//                     .status(WhatsAppLog.Status.PENDING).build();
//             try {
//                 Map<String, Object> response = callTextApi(phone, message);
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT",
//                                    "messageId", mid, "message", message));
//                 log.info("Template sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────
//     // ← NEW: sendPdfAlert() — upload PDF and send as document
//     // ─────────────────────────────────────────────────────────

//     /**
//      * Send a PDF file as a WhatsApp document attachment to a list of recipients.
//      *
//      * Flow:
//      *   1. Upload the PDF to Meta's media endpoint → get mediaId
//      *   2. Send a document message with the mediaId + caption to each recipient
//      *
//      * @param recipients  list of phone numbers
//      * @param pdfBytes    the PDF file content
//      * @param fileName    filename shown to the recipient (e.g. "salary_report.pdf")
//      * @param caption     text shown above the document in WhatsApp
//      * @return per-recipient results
//      */
//     public List<Map<String, Object>> sendPdfAlert(List<String> recipients,
//                                                     byte[] pdfBytes,
//                                                     String fileName,
//                                                     String caption) {
//         List<Map<String, Object>> results = new ArrayList<>();

//         // Step 1: Upload the PDF once, reuse the mediaId for all recipients
//         String mediaId;
//         try {
//             mediaId = uploadMedia(pdfBytes, fileName);
//             log.info("PDF uploaded to Meta, mediaId={}", mediaId);
//         } catch (Exception e) {
//             log.error("Media upload failed: {}", e.getMessage());
//             // Fallback: send caption as text only
//             for (String phone : recipients) {
//                 results.add(Map.of(
//                     "recipient", sanitize(phone),
//                     "status",    "FAILED",
//                     "error",     "PDF upload failed: " + e.getMessage()
//                 ));
//             }
//             return results;
//         }

//         // Step 2: Send document message to each recipient
//         for (String raw : recipients) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody("[PDF] " + fileName + " — " + caption)
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> response = callDocumentApi(phone, mediaId, fileName, caption);
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of(
//                     "recipient", phone, "status", "SENT",
//                     "messageId", mid, "fileName", fileName
//                 ));
//                 log.info("PDF sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────
//     // formatQueryResultAsMessage() — unchanged
//     // ─────────────────────────────────────────────────────────

//     public String formatQueryResultAsMessage(List<String> columns,
//                                               List<List<Object>> rows,
//                                               String header) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 *").append(header).append("*\n");
//         sb.append("━".repeat(28)).append("\n");
//         sb.append("*").append(String.join(" | ", columns)).append("*\n")
//           .append("─".repeat(28)).append("\n");
//         int limit = Math.min(rows.size(), 20);
//         for (int i = 0; i < limit; i++) {
//             List<String> vals = new ArrayList<>();
//             for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
//             sb.append(String.join(" | ", vals)).append("\n");
//         }
//         if (rows.size() > 20) sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
//         sb.append("━".repeat(28)).append("\n_Sent by WA Alert System_");
//         return sb.toString();
//     }

//     // ─────────────────────────────────────────────────────────
//     // Meta API calls
//     // ─────────────────────────────────────────────────────────

//     /** Send a plain text message. */
//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callTextApi(String to, String body) {
//         Map<String, Object> payload = Map.of(
//             "messaging_product", "whatsapp",
//             "recipient_type",    "individual",
//             "to",                to,
//             "type",              "text",
//             "text",              Map.of("body", body, "preview_url", false)
//         );
//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     /**
//      * Upload a PDF to Meta's media endpoint.
//      * Returns the mediaId to use in document messages.
//      *
//      * API: POST /{phone-number-id}/media
//      * Content-Type: multipart/form-data
//      * Fields: file (binary), type (application/pdf), messaging_product (whatsapp)
//      */
//     @SuppressWarnings("unchecked")
//     private String uploadMedia(byte[] pdfBytes, String fileName) {
//         MultipartBodyBuilder builder = new MultipartBodyBuilder();
//         builder.part("file", new ByteArrayResource(pdfBytes) {
//             @Override public String getFilename() { return fileName; }
//         }).contentType(MediaType.APPLICATION_PDF);
//         builder.part("type",               "application/pdf");
//         builder.part("messaging_product",  "whatsapp");

//         Map<String, Object> response = webClient.post()
//                 .uri("/{phoneNumberId}/media", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.MULTIPART_FORM_DATA)
//                 .bodyValue(builder.build())
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();

//         if (response == null || !response.containsKey("id")) {
//             throw new RuntimeException("Media upload returned null or missing 'id'");
//         }
//         return response.get("id").toString();
//     }

//     /** Send a document (PDF) using an already-uploaded mediaId. */
//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callDocumentApi(String to, String mediaId,
//                                                   String fileName, String caption) {
//         Map<String, Object> documentObj = new LinkedHashMap<>();
//         documentObj.put("id",       mediaId);
//         documentObj.put("filename", fileName);
//         documentObj.put("caption",  caption != null ? caption : "");

//         Map<String, Object> payload = new LinkedHashMap<>();
//         payload.put("messaging_product", "whatsapp");
//         payload.put("recipient_type",    "individual");
//         payload.put("to",                to);
//         payload.put("type",              "document");
//         payload.put("document",          documentObj);

//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     // ─────────────────────────────────────────────────────────
//     // Template engine and helpers (unchanged)
//     // ─────────────────────────────────────────────────────────

//     private String fillTemplate(String template, Map<String, Object> row) {
//         if (template == null) return "";
//         Pattern p = Pattern.compile("\\{\\{(\\w+)}}");
//         Matcher m = p.matcher(template);
//         StringBuilder sb = new StringBuilder();
//         while (m.find()) {
//             String key = m.group(1);
//             String val = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(key))
//                     .map(e -> e.getValue() != null ? e.getValue().toString() : "")
//                     .findFirst()
//                     .orElse("{{" + key + "}}");
//             m.appendReplacement(sb, Matcher.quoteReplacement(val));
//         }
//         m.appendTail(sb);
//         return sb.toString();
//     }

//     private String resolvePhone(Map<String, Object> row, String phoneKey) {
//         if (phoneKey != null && !phoneKey.isBlank()) {
//             Object v = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(phoneKey))
//                     .map(Map.Entry::getValue).findFirst().orElse(null);
//             return v != null ? v.toString() : null;
//         }
//         for (String alias : PHONE_KEYS) {
//             for (Map.Entry<String, Object> e : row.entrySet()) {
//                 if (e.getKey().equalsIgnoreCase(alias) && e.getValue() != null) {
//                     return e.getValue().toString();
//                 }
//             }
//         }
//         return null;
//     }

//     @SuppressWarnings("unchecked")
//     private String extractId(Map<String, Object> response) {
//         if (response == null) return "unknown";
//         List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
//         if (messages != null && !messages.isEmpty()) {
//             Object id = messages.get(0).get("id");
//             return id != null ? id.toString() : "unknown";
//         }
//         return "unknown";
//     }

//     private String sanitize(String phone) {
//         return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
//     }
// }

// package com.waalert.whatsapp_alert_backend.service;

// import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
// import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.reactive.function.BodyInserters;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;

// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @Service
// @Slf4j
// public class WhatsAppService {

//     private final WhatsAppLogRepository logRepository;
//     private final WebClient webClient;

//     @Value("${whatsapp.access-token}")
//     private String accessToken;

//     @Value("${whatsapp.phone-number-id}")
//     private String phoneNumberId;

//     private static final List<String> PHONE_KEYS = List.of(
//         "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
//         "contact", "phone_no", "cell", "cell_no", "contact_no"
//     );

//     public WhatsAppService(WhatsAppLogRepository logRepository,
//                            @Qualifier("whatsAppWebClient") WebClient webClient) {
//         this.logRepository = logRepository;
//         this.webClient     = webClient;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Existing: send plain text messages
//     // ─────────────────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         for (String raw : request.getRecipients()) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone).messageBody(request.getMessage())
//                     .status(WhatsAppLog.Status.PENDING).build();
//             try {
//                 //Map<String, Object> response = callTextApi(buildTextPayload(phone, request.getMessage()));
//                 Map<String, Object> response = callTextApi(
//     buildTemplatePayload(
//         phone,
//         "Employee",
//         request.getMessage()
//     )
// );
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT", "messageId", mid));
//                 log.info("WhatsApp sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                  log.error("FULL META ERROR: {}", err);
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("WhatsApp failed for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Existing: template-based per-row messaging
//     // ─────────────────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendTemplate(TemplateSendRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         if (request.getRows() == null || request.getRows().isEmpty()) return results;

//         for (Map<String, Object> row : request.getRows()) {
//             String phone = resolvePhone(row, request.getPhoneKey());
//             if (phone == null || phone.isBlank()) {
//                 results.add(Map.of("status", "SKIPPED", "reason", "No phone number in row", "row", row));
//                 continue;
//             }
//             phone = sanitize(phone);
//             String message = fillTemplate(request.getTemplate(), row);

//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone).messageBody(message)
//                     .status(WhatsAppLog.Status.PENDING).build();
//             try {
//                // Map<String, Object> response = callTextApi(buildTextPayload(phone, message));
//                Map<String, Object> response = callTextApi(
//     buildTemplatePayload(
//         phone,
//         row.getOrDefault("name", "Employee").toString(),
//         message
//     )
// );
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT", "messageId", mid, "message", message));
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                  log.error("FULL META ERROR: {}", err);
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // ✅ NEW: Send a PDF document via WhatsApp Cloud API
//     //
//     //  Step 1 — Upload PDF → Meta media endpoint → get media_id
//     //  Step 2 — Send document message using media_id
//     //
//     // @param phone      recipient phone number (digits only, no +)
//     // @param caption    text caption shown below the document
//     // @param filename   filename shown in WhatsApp (e.g. report_20241021_120000.pdf)
//     // @param pdfBytes   raw PDF byte array
//     // @return           result map with status and messageId
//     // ─────────────────────────────────────────────────────────────────────────

//     public Map<String, Object> sendPdfDocument(String phone, String caption,
//                                                 String filename, byte[] pdfBytes) {
//         phone = sanitize(phone);

//         WhatsAppLog entry = WhatsAppLog.builder()
//                 .recipient(phone).messageBody(caption)
//                 .status(WhatsAppLog.Status.PENDING).build();

//         try {
//             // Step 1 — Upload the PDF to Meta's media endpoint
//             String mediaId = uploadMedia(pdfBytes, filename);
//             log.info("PDF uploaded to Meta, mediaId={}", mediaId);

//             // Step 2 — Send the document message
//             Map<String, Object> docPayload = Map.of(
//                 "messaging_product", "whatsapp",
//                 "recipient_type",    "individual",
//                 "to",                phone,
//                 "type",              "document",
//                 "document",          Map.of(
//                     "id",       mediaId,
//                     "caption",  caption,
//                     "filename", filename
//                 )
//             );

//             Map<String, Object> response = callTextApi(docPayload);
//             String mid = extractId(response);
//             entry.setStatus(WhatsAppLog.Status.SENT);
//             entry.setWaMessageId(mid);
//             logRepository.save(entry);

//             log.info("PDF document sent to {} — messageId={}", phone, mid);
//             return Map.of("recipient", phone, "status", "SENT", "messageId", mid);

//         } catch (WebClientResponseException e) {
//             String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//             entry.setStatus(WhatsAppLog.Status.FAILED);
//             entry.setErrorMessage(err);
//             logRepository.save(entry);
//             log.error("PDF send failed for {}: {}", phone, err);
//             return Map.of("recipient", phone, "status", "FAILED", "error", err);
//         } catch (Exception e) {
//             entry.setStatus(WhatsAppLog.Status.FAILED);
//             entry.setErrorMessage(e.getMessage());
//             logRepository.save(entry);
//             return Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage());
//         }
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Private helpers
//     // ─────────────────────────────────────────────────────────────────────────

//     /**
//      * Upload PDF bytes to Meta's media API.
//      * Returns the media_id string needed for sending a document message.
//      */
//     @SuppressWarnings("unchecked")
//     private String uploadMedia(byte[] pdfBytes, String filename) {
//         MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//         body.add("messaging_product", "whatsapp");
//         body.add("type", "application/pdf");
//         body.add("file", new ByteArrayResource(pdfBytes) {
//             @Override public String getFilename() { return filename; }
//         });

//         Map<String, Object> response = webClient.post()
//                 .uri("/{phoneNumberId}/media", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.MULTIPART_FORM_DATA)
//                 .body(BodyInserters.fromMultipartData(body))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();

//         if (response == null || !response.containsKey("id")) {
//             throw new RuntimeException("Media upload failed — no id in response: " + response);
//         }
//         return response.get("id").toString();
//     }

//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callTextApi(Map<String, Object> payload) {
//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     // private Map<String, Object> buildTextPayload(String to, String body) {
//     //     return Map.of(
//     //         "messaging_product", "whatsapp",
//     //         "recipient_type",    "individual",
//     //         "to",                to,
//     //         "type",              "text",
//     //         "text",              Map.of("body", body, "preview_url", false)
//     //     );
//     // }
//     private Map<String, Object> buildTemplatePayload(
//         String to,
//         String employeeName,
//         String messageContent
// ) {

//     return Map.of(
//         "messaging_product", "whatsapp",
//         "recipient_type", "individual",
//         "to", to,
//         "type", "template",

//         "template", Map.of(
//             "name", "general_alert",

//             "language", Map.of(
//                 "code", "en"
//             ),

//             "components", List.of(
//                 Map.of(
//                     "type", "body",

//                     "parameters", List.of(

//                         Map.of(
//                             "type", "text",
//                             "text", employeeName
//                         ),

//                         Map.of(
//                             "type", "text",
//                             "text", messageContent
//                         )
//                     )
//                 )
//             )
//         )
//     );
// }

//     @SuppressWarnings("unchecked")
//     private String extractId(Map<String, Object> response) {
//         if (response == null) return "unknown";
//         List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
//         if (messages != null && !messages.isEmpty()) {
//             Object id = messages.get(0).get("id");
//             return id != null ? id.toString() : "unknown";
//         }
//         return "unknown";
//     }

//     private String fillTemplate(String template, Map<String, Object> row) {
//         if (template == null) return "";
//         Pattern p = Pattern.compile("\\{\\{(\\w+)}}");
//         Matcher m = p.matcher(template);
//         StringBuilder sb = new StringBuilder();
//         while (m.find()) {
//             String key   = m.group(1);
//             String value = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(key))
//                     .map(e -> e.getValue() != null ? e.getValue().toString() : "")
//                     .findFirst().orElse("{{" + key + "}}");
//             m.appendReplacement(sb, Matcher.quoteReplacement(value));
//         }
//         m.appendTail(sb);
//         return sb.toString();
//     }

//     private String resolvePhone(Map<String, Object> row, String phoneKey) {
//         if (phoneKey != null && !phoneKey.isBlank()) {
//             Object v = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(phoneKey))
//                     .map(Map.Entry::getValue).findFirst().orElse(null);
//             return v != null ? v.toString() : null;
//         }
//         for (String alias : PHONE_KEYS) {
//             for (Map.Entry<String, Object> entry : row.entrySet()) {
//                 if (entry.getKey().equalsIgnoreCase(alias) && entry.getValue() != null)
//                     return entry.getValue().toString();
//             }
//         }
//         return null;
//     }

//     private String sanitize(String phone) {
//         return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
//     }

//     public String formatQueryResultAsMessage(List<String> columns, List<List<Object>> rows, String header) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 *").append(header).append("*\n");
//         sb.append("━".repeat(28)).append("\n");
//         sb.append("*").append(String.join(" | ", columns)).append("*\n").append("─".repeat(28)).append("\n");
//         int limit = Math.min(rows.size(), 20);
//         for (int i = 0; i < limit; i++) {
//             List<String> vals = new ArrayList<>();
//             for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
//             sb.append(String.join(" | ", vals)).append("\n");
//         }
//         if (rows.size() > 20) sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
//         sb.append("━".repeat(28)).append("\n_Sent by Smart Alert System_");
//         return sb.toString();
//     }
// }

// package com.waalert.whatsapp_alert_backend.service;

// import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
// import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.reactive.function.BodyInserters;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;

// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// /**
//  * WhatsAppService
//  * ─────────────────────────────────────────────────────────────────────
//  *
//  * KEY FIX (2026-05-26):
//  *   The previous code called buildTemplatePayload() for both sendMessages()
//  *   and sendTemplate(), which routes through Meta's template API and passes
//  *   the full personalised message text as a template parameter.
//  *
//  *   Meta rejects this with error #132018:
//  *     "Param text cannot have new-line/tab characters or more than 4 consecutive spaces"
//  *
//  *   FIX: sendTemplate() and sendMessages() now use the plain TEXT message API
//  *   (type: "text") instead of the template API. This allows multiline,
//  *   formatted, emoji-rich personalised messages to be sent without restriction.
//  *
//  *   The Meta template (general_alert) is still used internally for simple
//  *   one-liner alerts via sendSimpleAlert() if needed, but NOT for Smart Sender.
//  */
// @Service
// @Slf4j
// public class WhatsAppService {

//     private final WhatsAppLogRepository logRepository;
//     private final WebClient webClient;

//     @Value("${whatsapp.access-token}")
//     private String accessToken;

//     @Value("${whatsapp.phone-number-id}")
//     private String phoneNumberId;

//     private static final List<String> PHONE_KEYS = List.of(
//         "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
//         "contact", "phone_no", "cell", "cell_no", "contact_no"
//     );

//     public WhatsAppService(WhatsAppLogRepository logRepository,
//                            @Qualifier("whatsAppWebClient") WebClient webClient) {
//         this.logRepository = logRepository;
//         this.webClient     = webClient;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // sendMessages() — send plain text messages to a list of recipients
//     //
//     // FIXED: Now uses buildTextPayload() (type: "text") instead of
//     //        buildTemplatePayload(), so newlines and special chars are allowed.
//     // ─────────────────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         for (String raw : request.getRecipients()) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(request.getMessage())
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> response = callApi(buildTextPayload(phone, request.getMessage()));
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT", "messageId", mid));
//                 log.info("WhatsApp text sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("FULL META API ERROR for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//                 log.error("Send error for {}: {}", phone, e.getMessage());
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // sendTemplate() — personalised per-row messages from SQL query results
//     //
//     // FIXED: Now uses buildTextPayload() (type: "text") so that multiline
//     //        salary breakdowns, emoji, and formatted text are all allowed.
//     //        The {{placeholder}} substitution still happens — the message is
//     //        filled from row data before being sent as a free-form text message.
//     // ─────────────────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendTemplate(TemplateSendRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         if (request.getRows() == null || request.getRows().isEmpty()) return results;

//         for (Map<String, Object> row : request.getRows()) {
//             String phone = resolvePhone(row, request.getPhoneKey());
//             if (phone == null || phone.isBlank()) {
//                 results.add(Map.of("status", "SKIPPED", "reason", "No phone number in row", "row", row));
//                 continue;
//             }
//             phone = sanitize(phone);

//             // Fill {{placeholders}} from row values — produces the final message text
//             String message = fillTemplate(request.getTemplate(), row);

//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(message)
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 // ✅ FIX: use text API — allows newlines, emoji, formatting
//                 Map<String, Object> response = callApi(buildTextPayload(phone, message));
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of("recipient", phone, "status", "SENT",
//                                    "messageId", mid, "message", message));
//                 log.info("Template text sent to {} — id={}", phone, mid);
//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("FULL META API ERROR for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//                 log.error("Template send error for {}: {}", phone, e.getMessage());
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // sendPdfDocument() — upload PDF and send as a WhatsApp document attachment
//     // ─────────────────────────────────────────────────────────────────────────

//     public Map<String, Object> sendPdfDocument(String phone, String caption,
//                                                 String filename, byte[] pdfBytes) {
//         phone = sanitize(phone);

//         WhatsAppLog entry = WhatsAppLog.builder()
//                 .recipient(phone)
//                 .messageBody(caption)
//                 .status(WhatsAppLog.Status.PENDING)
//                 .build();

//         try {
//             String mediaId = uploadMedia(pdfBytes, filename);
//             log.info("PDF uploaded to Meta, mediaId={}", mediaId);

//             Map<String, Object> docPayload = Map.of(
//                 "messaging_product", "whatsapp",
//                 "recipient_type",    "individual",
//                 "to",                phone,
//                 "type",              "document",
//                 "document",          Map.of(
//                     "id",       mediaId,
//                     "caption",  caption,
//                     "filename", filename
//                 )
//             );

//             Map<String, Object> response = callApi(docPayload);
//             String mid = extractId(response);
//             entry.setStatus(WhatsAppLog.Status.SENT);
//             entry.setWaMessageId(mid);
//             logRepository.save(entry);

//             log.info("PDF document sent to {} — messageId={}", phone, mid);
//             return Map.of("recipient", phone, "status", "SENT", "messageId", mid);

//         } catch (WebClientResponseException e) {
//             String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//             entry.setStatus(WhatsAppLog.Status.FAILED);
//             entry.setErrorMessage(err);
//             logRepository.save(entry);
//             log.error("PDF send failed for {}: {}", phone, err);
//             return Map.of("recipient", phone, "status", "FAILED", "error", err);
//         } catch (Exception e) {
//             entry.setStatus(WhatsAppLog.Status.FAILED);
//             entry.setErrorMessage(e.getMessage());
//             logRepository.save(entry);
//             return Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage());
//         }
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Private: API call helpers
//     // ─────────────────────────────────────────────────────────────────────────

//     /**
//      * Build a plain text message payload.
//      * This is the correct type for personalised multiline messages.
//      * Meta does NOT restrict newlines or formatting in free-form text messages.
//      */
//     private Map<String, Object> buildTextPayload(String to, String body) {
//         return Map.of(
//             "messaging_product", "whatsapp",
//             "recipient_type",    "individual",
//             "to",                to,
//             "type",              "text",
//             "text",              Map.of("body", body, "preview_url", false)
//         );
//     }

//     /**
//      * Post a JSON payload to the WhatsApp Cloud API messages endpoint.
//      */
//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callApi(Map<String, Object> payload) {
//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     /**
//      * Upload PDF bytes to Meta's media API.
//      * Returns the media_id needed for sending a document message.
//      */
//     @SuppressWarnings("unchecked")
//     private String uploadMedia(byte[] pdfBytes, String filename) {
//         MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//         body.add("messaging_product", "whatsapp");
//         body.add("type", "application/pdf");
//         body.add("file", new ByteArrayResource(pdfBytes) {
//             @Override public String getFilename() { return filename; }
//         });

//         Map<String, Object> response = webClient.post()
//                 .uri("/{phoneNumberId}/media", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.MULTIPART_FORM_DATA)
//                 .body(BodyInserters.fromMultipartData(body))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();

//         if (response == null || !response.containsKey("id")) {
//             throw new RuntimeException("Media upload failed — no id in response: " + response);
//         }
//         return response.get("id").toString();
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Private: template engine and helpers
//     // ─────────────────────────────────────────────────────────────────────────

//     /**
//      * Replace {{key}} placeholders in a template string with values from a row map.
//      * Matching is case-insensitive so {{Name}} finds the "name" column too.
//      */
//     private String fillTemplate(String template, Map<String, Object> row) {
//         if (template == null) return "";
//         Pattern p = Pattern.compile("\\{\\{(\\w+)}}");
//         Matcher m = p.matcher(template);
//         StringBuilder sb = new StringBuilder();
//         while (m.find()) {
//             String key   = m.group(1);
//             String value = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(key))
//                     .map(e -> e.getValue() != null ? e.getValue().toString() : "")
//                     .findFirst()
//                     .orElse("{{" + key + "}}");  // leave placeholder if column not found
//             m.appendReplacement(sb, Matcher.quoteReplacement(value));
//         }
//         m.appendTail(sb);
//         return sb.toString();
//     }

//     /**
//      * Find the phone number in a row map.
//      * Uses explicit phoneKey if provided; otherwise auto-detects from PHONE_KEYS aliases.
//      */
//     private String resolvePhone(Map<String, Object> row, String phoneKey) {
//         if (phoneKey != null && !phoneKey.isBlank()) {
//             Object v = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(phoneKey))
//                     .map(Map.Entry::getValue)
//                     .findFirst()
//                     .orElse(null);
//             return v != null ? v.toString() : null;
//         }
//         for (String alias : PHONE_KEYS) {
//             for (Map.Entry<String, Object> entry : row.entrySet()) {
//                 if (entry.getKey().equalsIgnoreCase(alias) && entry.getValue() != null)
//                     return entry.getValue().toString();
//             }
//         }
//         return null;
//     }

//     @SuppressWarnings("unchecked")
//     private String extractId(Map<String, Object> response) {
//         if (response == null) return "unknown";
//         List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
//         if (messages != null && !messages.isEmpty()) {
//             Object id = messages.get(0).get("id");
//             return id != null ? id.toString() : "unknown";
//         }
//         return "unknown";
//     }

//     private String sanitize(String phone) {
//         return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
//     }

//     public String formatQueryResultAsMessage(List<String> columns, List<List<Object>> rows, String header) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 *").append(header).append("*\n");
//         sb.append("━".repeat(28)).append("\n");
//         sb.append("*").append(String.join(" | ", columns)).append("*\n")
//           .append("─".repeat(28)).append("\n");
//         int limit = Math.min(rows.size(), 20);
//         for (int i = 0; i < limit; i++) {
//             List<String> vals = new ArrayList<>();
//             for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
//             sb.append(String.join(" | ", vals)).append("\n");
//         }
//         if (rows.size() > 20) sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
//         sb.append("━".repeat(28)).append("\n_Sent by Smart Alert System_");
//         return sb.toString();
//     }
// }
// package com.waalert.whatsapp_alert_backend.service;

// import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
// import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
// import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
// import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.reactive.function.BodyInserters;
// import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.web.reactive.function.client.WebClientResponseException;

// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// /**
//  * WhatsAppService
//  * ─────────────────────────────────────────────────────────────────────
//  *
//  * Only the "salary_alert" Meta template is active.
//  * General/non-salary messages are NOT supported via template API yet.
//  *
//  * salary_alert (Utility) — 5 parameters:
//  *   Dear {{1}},
//  *
//  *   Your salary details for {{2}} are as follows:
//  *
//  *   Basic Salary : Rs.{{3}}
//  *   HRA          : Rs.{{4}}
//  *   Total Salary : Rs.{{5}}
//  *
//  *   Your salary has been credited to your account.
//  *   - HR Department
//  *
//  * sendMessages()  → returns SKIPPED (no general template available)
//  * sendTemplate()  → salary rows use salary_alert; non-salary rows SKIPPED
//  */
// @Service
// @Slf4j
// public class WhatsAppService {

//     private final WhatsAppLogRepository logRepository;
//     private final WebClient webClient;

//     @Value("${whatsapp.access-token}")
//     private String accessToken;

//     @Value("${whatsapp.phone-number-id}")
//     private String phoneNumberId;

//     private static final String SALARY_TEMPLATE_NAME = "salary_alert";
//     private static final String TEMPLATE_LANGUAGE    = "en";

//     private static final Set<String> SALARY_COLUMNS = Set.of(
//         "basic", "hra", "total", "gross", "net",
//         "basic_salary", "hra_amount", "total_salary"
//     );

//     private static final List<String> PHONE_KEYS = List.of(
//         "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
//         "contact", "phone_no", "cell", "cell_no", "contact_no"
//     );

//     public WhatsAppService(WhatsAppLogRepository logRepository,
//                            @Qualifier("whatsAppWebClient") WebClient webClient) {
//         this.logRepository = logRepository;
//         this.webClient     = webClient;
//     }

//     // ─────────────────────────────────────────────────────────────────────
//     // sendMessages()
//     // Called by: WhatsApp page → POST /api/whatsapp/send
//     //
//     // General messaging is not available until a general_alert template
//     // is created and approved in Meta. Returns SKIPPED for all recipients.
//     // ─────────────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         for (String raw : request.getRecipients()) {
//             String phone = sanitize(raw);
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(request.getMessage())
//                     .status(WhatsAppLog.Status.FAILED)
//                     .errorMessage("No general template available. Only salary_alert is active.")
//                     .build();
//             logRepository.save(entry);
//             results.add(Map.of(
//                 "recipient", phone,
//                 "status",    "SKIPPED",
//                 "reason",    "General messaging requires a general_alert Meta template which is not yet created. Only salary messages are supported."
//             ));
//             log.warn("sendMessages() skipped for {} — no general template", phone);
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────────────────
//     // sendTemplate()
//     // Called by: Smart Sender → POST /api/whatsapp/send-template
//     //
//     // Only processes rows that contain salary columns (basic, hra, total).
//     // Non-salary rows are SKIPPED with a clear reason.
//     // ─────────────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> sendTemplate(TemplateSendRequest request) {
//         List<Map<String, Object>> results = new ArrayList<>();
//         if (request.getRows() == null || request.getRows().isEmpty()) return results;

//         for (Map<String, Object> row : request.getRows()) {
//             // Resolve phone number
//             String phone = resolvePhone(row, request.getPhoneKey());
//             if (phone == null || phone.isBlank()) {
//                 results.add(Map.of(
//                     "status", "SKIPPED",
//                     "reason", "No phone number found in row",
//                     "row",    row
//                 ));
//                 continue;
//             }
//             phone = sanitize(phone);

//             // Only salary rows are supported
//             if (!isSalaryRow(row)) {
//                 results.add(Map.of(
//                     "recipient", phone,
//                     "status",    "SKIPPED",
//                     "reason",    "Only salary messages are supported. This row has no salary columns (basic, hra, total)."
//                 ));
//                 log.warn("Skipping non-salary row for {} — no general template available", phone);
//                 continue;
//             }

//             // Build salary_alert payload
//             WhatsAppLog entry = WhatsAppLog.builder()
//                     .recipient(phone)
//                     .messageBody(String.format(
//                         "salary_alert | name=%s | month=%s | basic=%s | hra=%s | total=%s",
//                         getRowValue(row, "name",  "?"),
//                         getRowValue(row, "month", "?"),
//                         getRowValue(row, "basic", "?"),
//                         getRowValue(row, "hra",   "?"),
//                         getRowValue(row, "total", "?")))
//                     .status(WhatsAppLog.Status.PENDING)
//                     .build();
//             try {
//                 Map<String, Object> payload  = buildSalaryTemplatePayload(phone, row);
//                 Map<String, Object> response = callApi(payload);
//                 String mid = extractId(response);
//                 entry.setStatus(WhatsAppLog.Status.SENT);
//                 entry.setWaMessageId(mid);
//                 results.add(Map.of(
//                     "recipient", phone,
//                     "status",    "SENT",
//                     "messageId", mid,
//                     "template",  SALARY_TEMPLATE_NAME
//                 ));
//                 log.info("[salary_alert] Sent to {} — id={}", phone, mid);

//             } catch (WebClientResponseException e) {
//                 String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(err);
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
//                 log.error("FULL META API ERROR for {}: {}", phone, err);
//             } catch (Exception e) {
//                 entry.setStatus(WhatsAppLog.Status.FAILED);
//                 entry.setErrorMessage(e.getMessage());
//                 results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
//                 log.error("Send error for {}: {}", phone, e.getMessage());
//             } finally {
//                 logRepository.save(entry);
//             }
//         }
//         return results;
//     }

//     // ─────────────────────────────────────────────────────────────────────
//     // sendPdfDocument()
//     // ─────────────────────────────────────────────────────────────────────

//     public Map<String, Object> sendPdfDocument(String phone, String caption,
//                                                 String filename, byte[] pdfBytes) {
//         phone = sanitize(phone);
//         WhatsAppLog entry = WhatsAppLog.builder()
//                 .recipient(phone)
//                 .messageBody(caption)
//                 .status(WhatsAppLog.Status.PENDING)
//                 .build();
//         try {
//             String mediaId = uploadMedia(pdfBytes, filename);
//             log.info("PDF uploaded to Meta, mediaId={}", mediaId);

//             Map<String, Object> docPayload = Map.of(
//                 "messaging_product", "whatsapp",
//                 "recipient_type",    "individual",
//                 "to",                phone,
//                 "type",              "document",
//                 "document",          Map.of(
//                     "id",       mediaId,
//                     "caption",  caption,
//                     "filename", filename
//                 )
//             );
//             Map<String, Object> response = callApi(docPayload);
//             String mid = extractId(response);
//             entry.setStatus(WhatsAppLog.Status.SENT);
//             entry.setWaMessageId(mid);
//             logRepository.save(entry);
//             log.info("PDF sent to {} — messageId={}", phone, mid);
//             return Map.of("recipient", phone, "status", "SENT", "messageId", mid);

//         } catch (WebClientResponseException e) {
//             String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
//             entry.setStatus(WhatsAppLog.Status.FAILED);
//             entry.setErrorMessage(err);
//             logRepository.save(entry);
//             log.error("PDF send failed for {}: {}", phone, err);
//             return Map.of("recipient", phone, "status", "FAILED", "error", err);
//         } catch (Exception e) {
//             entry.setStatus(WhatsAppLog.Status.FAILED);
//             entry.setErrorMessage(e.getMessage());
//             logRepository.save(entry);
//             return Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage());
//         }
//     }

//     // ─────────────────────────────────────────────────────────────────────
//     // Private: payload builders
//     // ─────────────────────────────────────────────────────────────────────

//     /**
//      * Builds salary_alert template payload with 5 clean single-line parameters.
//      *
//      * {{1}} = name   {{2}} = month   {{3}} = basic   {{4}} = hra   {{5}} = total
//      */
//     private Map<String, Object> buildSalaryTemplatePayload(String to, Map<String, Object> row) {
//         return templatePayload(to, SALARY_TEMPLATE_NAME, List.of(
//             textParam(getRowValue(row, "name",  "Employee")),
//             textParam(getRowValue(row, "month", "this month")),
//             textParam(getRowValue(row, "basic", "0")),
//             textParam(getRowValue(row, "hra",   "0")),
//             textParam(getRowValue(row, "total", "0"))
//         ));
//     }

//     private Map<String, Object> templatePayload(String to, String templateName,
//                                                   List<Map<String, Object>> parameters) {
//         return Map.of(
//             "messaging_product", "whatsapp",
//             "recipient_type",    "individual",
//             "to",                to,
//             "type",              "template",
//             "template",          Map.of(
//                 "name",       templateName,
//                 "language",   Map.of("code", TEMPLATE_LANGUAGE),
//                 "components", List.of(
//                     Map.of("type", "body", "parameters", parameters)
//                 )
//             )
//         );
//     }

//     private Map<String, Object> textParam(String value) {
//         return Map.of("type", "text", "text", value);
//     }

//     // ─────────────────────────────────────────────────────────────────────
//     // Private: helpers
//     // ─────────────────────────────────────────────────────────────────────

//     private boolean isSalaryRow(Map<String, Object> row) {
//         return row.keySet().stream()
//                 .anyMatch(k -> SALARY_COLUMNS.contains(k.toLowerCase()));
//     }

//     private String getRowValue(Map<String, Object> row, String key, String defaultValue) {
//         return row.entrySet().stream()
//                 .filter(e -> e.getKey().equalsIgnoreCase(key))
//                 .map(e -> e.getValue() != null ? e.getValue().toString().trim() : "")
//                 .filter(v -> !v.isEmpty())
//                 .findFirst()
//                 .orElse(defaultValue);
//     }

//     private String fillTemplate(String template, Map<String, Object> row) {
//         if (template == null) return "";
//         Pattern p = Pattern.compile("\\{\\{(\\w+)}}");
//         Matcher m = p.matcher(template);
//         StringBuilder sb = new StringBuilder();
//         while (m.find()) {
//             String key   = m.group(1);
//             String value = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(key))
//                     .map(e -> e.getValue() != null ? e.getValue().toString() : "")
//                     .findFirst()
//                     .orElse("{{" + key + "}}");
//             m.appendReplacement(sb, Matcher.quoteReplacement(value));
//         }
//         m.appendTail(sb);
//         return sb.toString();
//     }

//     private String resolvePhone(Map<String, Object> row, String phoneKey) {
//         if (phoneKey != null && !phoneKey.isBlank()) {
//             Object v = row.entrySet().stream()
//                     .filter(e -> e.getKey().equalsIgnoreCase(phoneKey))
//                     .map(Map.Entry::getValue)
//                     .findFirst()
//                     .orElse(null);
//             return v != null ? v.toString() : null;
//         }
//         for (String alias : PHONE_KEYS) {
//             for (Map.Entry<String, Object> e : row.entrySet()) {
//                 if (e.getKey().equalsIgnoreCase(alias) && e.getValue() != null)
//                     return e.getValue().toString();
//             }
//         }
//         return null;
//     }

//     @SuppressWarnings("unchecked")
//     private Map<String, Object> callApi(Map<String, Object> payload) {
//         return webClient.post()
//                 .uri("/{phoneNumberId}/messages", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .bodyValue(payload)
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//     }

//     @SuppressWarnings("unchecked")
//     private String uploadMedia(byte[] pdfBytes, String filename) {
//         MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//         body.add("messaging_product", "whatsapp");
//         body.add("type", "application/pdf");
//         body.add("file", new ByteArrayResource(pdfBytes) {
//             @Override public String getFilename() { return filename; }
//         });
//         Map<String, Object> response = webClient.post()
//                 .uri("/{phoneNumberId}/media", phoneNumberId)
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                 .contentType(MediaType.MULTIPART_FORM_DATA)
//                 .body(BodyInserters.fromMultipartData(body))
//                 .retrieve()
//                 .bodyToMono(Map.class)
//                 .block();
//         if (response == null || !response.containsKey("id"))
//             throw new RuntimeException("Media upload failed — no id in response: " + response);
//         return response.get("id").toString();
//     }

//     @SuppressWarnings("unchecked")
//     private String extractId(Map<String, Object> response) {
//         if (response == null) return "unknown";
//         List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
//         if (messages != null && !messages.isEmpty()) {
//             Object id = messages.get(0).get("id");
//             return id != null ? id.toString() : "unknown";
//         }
//         return "unknown";
//     }

//     private String sanitize(String phone) {
//         return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
//     }

//     public String formatQueryResultAsMessage(List<String> columns,
//                                               List<List<Object>> rows,
//                                               String header) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("📊 *").append(header).append("*\n");
//         sb.append("━".repeat(28)).append("\n");
//         sb.append("*").append(String.join(" | ", columns)).append("*\n")
//           .append("─".repeat(28)).append("\n");
//         int limit = Math.min(rows.size(), 20);
//         for (int i = 0; i < limit; i++) {
//             List<String> vals = new ArrayList<>();
//             for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
//             sb.append(String.join(" | ", vals)).append("\n");
//         }
//         if (rows.size() > 20)
//             sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
//         sb.append("━".repeat(28)).append("\n_Sent by Smart Alert System_");
//         return sb.toString();
//     }
// }

package com.waalert.whatsapp_alert_backend.service;

import com.waalert.whatsapp_alert_backend.dto.TemplateSendRequest;
import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
import com.waalert.whatsapp_alert_backend.repository.WhatsAppLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WhatsAppService
 * ─────────────────────────────────────────────────────────────────────
 *
 * BROADCAST CHANGES (2026-05-26):
 *
 *   ✅ REMOVED: isSalaryRow() gate that skipped non-salary rows.
 *   ✅ REMOVED: 24-hour customer-care window restriction
 *              (Meta enforces this; using approved templates bypasses it entirely).
 *   ✅ ADDED:   sendSalaryBroadcast() — iterates ALL rows from the DB query result
 *              and sends the "salary_alert" Meta-approved template to every recipient,
 *              regardless of whether they have previously messaged the business number.
 *
 * Meta Template in use — "salary_alert" (Utility, approved):
 *
 *   Dear {{1}},
 *
 *   Your salary details for {{2}} are as follows:
 *
 *   Basic Salary : Rs.{{3}}
 *   HRA          : Rs.{{4}}
 *   Total Salary : Rs.{{5}}
 *
 *   Your salary has been credited to your account.
 *   - HR Department
 *
 * Parameter mapping from row:
 *   {{1}} → name     {{2}} → month     {{3}} → basic
 *   {{4}} → hra      {{5}} → total
 *
 * WHY TEMPLATES BYPASS THE 24-HOUR WINDOW:
 *   Meta's messaging policy only restricts free-form "text" messages.
 *   Pre-approved HSM (Highly Structured Message) templates can be sent to
 *   any opted-in phone number at any time, including cold leads with zero
 *   prior interaction — as long as the WABA account has template access.
 *   The permanent_token + system-user admin account already has this access.
 */
@Service
@Slf4j
public class WhatsAppService {

    private final WhatsAppLogRepository logRepository;
    private final WebClient webClient;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    // ── Template identity ─────────────────────────────────────────────
    private static final String SALARY_TEMPLATE_NAME = "salary_alert";
    private static final String TEMPLATE_LANGUAGE    = "en";

    // ── Phone column auto-detection ───────────────────────────────────
    private static final List<String> PHONE_KEYS = List.of(
        "mob_no", "mobile", "phone", "whatsapp_number", "mobile_no",
        "contact", "phone_no", "cell", "cell_no", "contact_no"
    );

    public WhatsAppService(WhatsAppLogRepository logRepository,
                           @Qualifier("whatsAppWebClient") WebClient webClient) {
        this.logRepository = logRepository;
        this.webClient     = webClient;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  sendSalaryBroadcast()
    //
    //  ✅ NEW BROADCAST METHOD — use this for salary day.
    //
    //  Sends the "salary_alert" Meta-approved template to EVERY recipient
    //  returned by the SQL query. No interaction check. No 24-hour gate.
    //  No isSalaryRow() filtering. Every row gets a message.
    //
    //  Called by: POST /api/whatsapp/broadcast-salary
    //
    //  @param rows     all rows from the salary DB query (each is a column→value map)
    //  @param phoneKey optional explicit phone column name; auto-detects if null
    //  @return         per-recipient send results
    // ═════════════════════════════════════════════════════════════════════
    public List<Map<String, Object>> sendSalaryBroadcast(List<Map<String, Object>> rows,
                                                          String phoneKey) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            log.warn("[salary_broadcast] No rows supplied — nothing to send.");
            return results;
        }

        log.info("[salary_broadcast] Starting broadcast to {} recipients using template '{}'",
                 rows.size(), SALARY_TEMPLATE_NAME);

        for (Map<String, Object> row : rows) {

            // ── 1. Resolve phone number (no interaction filter) ──────────
            String phone = resolvePhone(row, phoneKey);
            if (phone == null || phone.isBlank()) {
                results.add(Map.of(
                    "status", "SKIPPED",
                    "reason", "No phone number column found in row",
                    "row",    row
                ));
                log.warn("[salary_broadcast] Skipping row — no phone number: {}", row);
                continue;
            }
            phone = sanitize(phone);  // strip spaces, dashes, leading +

            // ── 2. Build Meta-format template payload ────────────────────
            Map<String, Object> payload = buildSalaryTemplatePayload(phone, row);

            // ── 3. Log entry ─────────────────────────────────────────────
            WhatsAppLog entry = WhatsAppLog.builder()
                    .recipient(phone)
                    .messageBody(String.format(
                        "[salary_alert] name=%s | month=%s | basic=%s | hra=%s | total=%s",
                        getRowValue(row, "name",  "?"),
                        getRowValue(row, "month", "?"),
                        getRowValue(row, "basic", "?"),
                        getRowValue(row, "hra",   "?"),
                        getRowValue(row, "total", "?")))
                    .status(WhatsAppLog.Status.PENDING)
                    .build();

            // ── 4. Send via Meta Cloud API ───────────────────────────────
            try {
                Map<String, Object> response = callApi(payload);
                String mid = extractId(response);

                entry.setStatus(WhatsAppLog.Status.SENT);
                entry.setWaMessageId(mid);

                results.add(Map.of(
                    "recipient", phone,
                    "status",    "SENT",
                    "messageId", mid,
                    "template",  SALARY_TEMPLATE_NAME
                ));
                log.info("[salary_broadcast] ✅ Sent to {} — messageId={}", phone, mid);

            } catch (WebClientResponseException e) {
                String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
                entry.setStatus(WhatsAppLog.Status.FAILED);
                entry.setErrorMessage(err);
                results.add(Map.of("recipient", phone, "status", "FAILED", "error", err));
                log.error("[salary_broadcast] ❌ Meta API error for {}: {}", phone, err);

            } catch (Exception e) {
                entry.setStatus(WhatsAppLog.Status.FAILED);
                entry.setErrorMessage(e.getMessage());
                results.add(Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage()));
                log.error("[salary_broadcast] ❌ Unexpected error for {}: {}", phone, e.getMessage());

            } finally {
                logRepository.save(entry);
            }
        }

        long sent    = results.stream().filter(r -> "SENT".equals(r.get("status"))).count();
        long failed  = results.stream().filter(r -> "FAILED".equals(r.get("status"))).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.get("status"))).count();
        log.info("[salary_broadcast] Complete — sent={} failed={} skipped={}", sent, failed, skipped);

        return results;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  sendTemplate()
    //
    //  Called by Smart Sender → POST /api/whatsapp/send-template
    //  Now also routes through salary_alert for every row (no filtering).
    //  Kept for backward compatibility with existing frontend calls.
    // ═════════════════════════════════════════════════════════════════════
    public List<Map<String, Object>> sendTemplate(TemplateSendRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (request.getRows() == null || request.getRows().isEmpty()) return results;

        // Delegate to broadcast — all rows, no interaction gate
        return sendSalaryBroadcast(request.getRows(), request.getPhoneKey());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  sendMessages()
    //
    //  Called by POST /api/whatsapp/send (legacy plain-text endpoint).
    //  Plain-text messages are blocked by Meta's 24-hour window for cold
    //  recipients. This endpoint now returns SKIPPED with a clear message.
    //  Use sendSalaryBroadcast() / POST /api/whatsapp/broadcast-salary instead.
    // ═════════════════════════════════════════════════════════════════════
    public List<Map<String, Object>> sendMessages(WhatsAppRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (String raw : request.getRecipients()) {
            String phone = sanitize(raw);
            WhatsAppLog entry = WhatsAppLog.builder()
                    .recipient(phone)
                    .messageBody(request.getMessage())
                    .status(WhatsAppLog.Status.FAILED)
                    .errorMessage("Plain-text messages are blocked for cold recipients by Meta's 24-hour window. Use the salary broadcast endpoint instead.")
                    .build();
            logRepository.save(entry);
            results.add(Map.of(
                "recipient", phone,
                "status",    "SKIPPED",
                "reason",    "Use POST /api/whatsapp/broadcast-salary with the salary_alert template to reach all recipients regardless of interaction history."
            ));
        }
        return results;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  sendPdfDocument()  — unchanged
    // ═════════════════════════════════════════════════════════════════════
    public Map<String, Object> sendPdfDocument(String phone, String caption,
                                                String filename, byte[] pdfBytes) {
        phone = sanitize(phone);
        WhatsAppLog entry = WhatsAppLog.builder()
                .recipient(phone)
                .messageBody(caption)
                .status(WhatsAppLog.Status.PENDING)
                .build();
        try {
            String mediaId = uploadMedia(pdfBytes, filename);
            log.info("PDF uploaded to Meta, mediaId={}", mediaId);
            Map<String, Object> docPayload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type",    "individual",
                "to",                phone,
                "type",              "document",
                "document",          Map.of(
                    "id",       mediaId,
                    "caption",  caption,
                    "filename", filename
                )
            );
            Map<String, Object> response = callApi(docPayload);
            String mid = extractId(response);
            entry.setStatus(WhatsAppLog.Status.SENT);
            entry.setWaMessageId(mid);
            logRepository.save(entry);
            log.info("PDF sent to {} — messageId={}", phone, mid);
            return Map.of("recipient", phone, "status", "SENT", "messageId", mid);
        } catch (WebClientResponseException e) {
            String err = "Meta API " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
            entry.setStatus(WhatsAppLog.Status.FAILED);
            entry.setErrorMessage(err);
            logRepository.save(entry);
            return Map.of("recipient", phone, "status", "FAILED", "error", err);
        } catch (Exception e) {
            entry.setStatus(WhatsAppLog.Status.FAILED);
            entry.setErrorMessage(e.getMessage());
            logRepository.save(entry);
            return Map.of("recipient", phone, "status", "FAILED", "error", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PAYLOAD BUILDERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds the exact Meta Cloud API payload for the "salary_alert" template.
     *
     * Meta-required structure for template messages (cold leads / no prior interaction):
     * {
     *   "messaging_product": "whatsapp",
     *   "recipient_type": "individual",
     *   "to": "<phone_e164_no_plus>",
     *   "type": "template",
     *   "template": {
     *     "name": "salary_alert",
     *     "language": { "code": "en" },
     *     "components": [{
     *       "type": "body",
     *       "parameters": [
     *         { "type": "text", "text": "<name>" },      // {{1}}
     *         { "type": "text", "text": "<month>" },     // {{2}}
     *         { "type": "text", "text": "<basic>" },     // {{3}}
     *         { "type": "text", "text": "<hra>" },       // {{4}}
     *         { "type": "text", "text": "<total>" }      // {{5}}
     *       ]
     *     }]
     *   }
     * }
     *
     * IMPORTANT: Each parameter text must be a single-line, clean string.
     * No newlines (\n), no tabs (\t), no more than 4 consecutive spaces.
     * Meta error #132018 fires if this rule is violated.
     */
    private Map<String, Object> buildSalaryTemplatePayload(String to, Map<String, Object> row) {
        List<Map<String, Object>> parameters = List.of(
            textParam(getRowValue(row, "name",  "Employee")),     // {{1}}
            textParam(getRowValue(row, "month", "this month")),   // {{2}}
            textParam(getRowValue(row, "basic", "0")),            // {{3}}
            textParam(getRowValue(row, "hra",   "0")),            // {{4}}
            textParam(getRowValue(row, "total", "0"))             // {{5}}
        );

        return Map.of(
            "messaging_product", "whatsapp",
            "recipient_type",    "individual",
            "to",                to,
            "type",              "template",
            "template", Map.of(
                "name",       SALARY_TEMPLATE_NAME,
                "language",   Map.of("code", TEMPLATE_LANGUAGE),
                "components", List.of(
                    Map.of("type", "body", "parameters", parameters)
                )
            )
        );
    }

    /** Wraps a value in the Meta text-parameter object format. */
    private Map<String, Object> textParam(String value) {
        // Strip any newlines/tabs that would trigger Meta error #132018
        String clean = value.replaceAll("[\\n\\r\\t]", " ").replaceAll(" {5,}", "    ").trim();
        return Map.of("type", "text", "text", clean);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  META API CALL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * POST to Meta Cloud API messages endpoint.
     * Uses the permanent_token injected from application.properties.
     * No 24-hour window applies when sending approved template messages.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callApi(Map<String, Object> payload) {
        return webClient.post()
                .uri("/{phoneNumberId}/messages", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @SuppressWarnings("unchecked")
    private String uploadMedia(byte[] pdfBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("messaging_product", "whatsapp");
        body.add("type", "application/pdf");
        body.add("file", new ByteArrayResource(pdfBytes) {
            @Override public String getFilename() { return filename; }
        });
        Map<String, Object> response = webClient.post()
                .uri("/{phoneNumberId}/media", phoneNumberId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (response == null || !response.containsKey("id"))
            throw new RuntimeException("Media upload failed — no id in response: " + response);
        return response.get("id").toString();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private String resolvePhone(Map<String, Object> row, String phoneKey) {
        if (phoneKey != null && !phoneKey.isBlank()) {
            Object v = row.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(phoneKey))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
            return v != null ? v.toString() : null;
        }
        for (String alias : PHONE_KEYS) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey().equalsIgnoreCase(alias) && e.getValue() != null)
                    return e.getValue().toString();
            }
        }
        return null;
    }

    private String getRowValue(Map<String, Object> row, String key, String defaultValue) {
        return row.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(e -> e.getValue() != null ? e.getValue().toString().trim() : "")
                .filter(v -> !v.isEmpty())
                .findFirst()
                .orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    private String extractId(Map<String, Object> response) {
        if (response == null) return "unknown";
        List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
        if (messages != null && !messages.isEmpty()) {
            Object id = messages.get(0).get("id");
            return id != null ? id.toString() : "unknown";
        }
        return "unknown";
    }

    /** Strip spaces, dashes, parentheses, and leading '+' from a phone string. */
    private String sanitize(String phone) {
        return phone.replaceAll("[\\s\\-()]+", "").replaceAll("^\\+", "");
    }

    public String formatQueryResultAsMessage(List<String> columns,
                                              List<List<Object>> rows,
                                              String header) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *").append(header).append("*\n");
        sb.append("━".repeat(28)).append("\n");
        sb.append("*").append(String.join(" | ", columns)).append("*\n")
          .append("─".repeat(28)).append("\n");
        int limit = Math.min(rows.size(), 20);
        for (int i = 0; i < limit; i++) {
            List<String> vals = new ArrayList<>();
            for (Object v : rows.get(i)) vals.add(v != null ? v.toString() : "-");
            sb.append(String.join(" | ", vals)).append("\n");
        }
        if (rows.size() > 20)
            sb.append("_...and ").append(rows.size() - 20).append(" more rows_\n");
        sb.append("━".repeat(28)).append("\n_Sent by Smart Alert System_");
        return sb.toString();
    }
}
