package com.waalert.whatsapp_alert_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for saving/updating a report query template.
 */
@Data
public class ReportTemplateDto {

    @NotBlank(message = "Template name is required")
    @Size(max = 200, message = "Name must be under 200 characters")
    private String name;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;

    @NotBlank(message = "SQL query is required")
    private String sqlQuery;

    /** MYSQL or SQLSERVER — filled by frontend from active connection type. */
    private String databaseType = "MYSQL";
}