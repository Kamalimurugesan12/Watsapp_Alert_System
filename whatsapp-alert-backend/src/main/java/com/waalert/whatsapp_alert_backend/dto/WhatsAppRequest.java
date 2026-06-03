package com.waalert.whatsapp_alert_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Request body for sending a WhatsApp message to one or more recipients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppRequest {

    @NotEmpty(message = "At least one recipient is required")
    private List<String> recipients;

    @NotBlank(message = "Message body cannot be empty")
    private String message;

    /** TEXT (default) or DOCUMENT */
    private String messageType = "TEXT";

    /** Used when messageType = DOCUMENT */
    private String documentUrl;

    /** Display filename for document messages */
    private String documentName;
}
