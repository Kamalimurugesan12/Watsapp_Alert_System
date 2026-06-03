package com.waalert.whatsapp_alert_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class SchedulerRequest {
    @NotBlank(message = "Name required")
    private String name;

    @NotBlank(message = "Schedule type required")
    private String scheduleType;   // DAILY, WEEKLY, CUSTOM

    private String cronExpression;
    private String reportType;
    private String sqlQuery;
    private List<String> recipients;
    private String messageTemplate;
    private String outputType = "WHATSAPP";   // WHATSAPP, EMAIL, BOTH
    private String emailRecipient;
    private boolean enabled = true;
}
