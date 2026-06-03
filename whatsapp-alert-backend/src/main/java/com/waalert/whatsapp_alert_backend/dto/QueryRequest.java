package com.waalert.whatsapp_alert_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for executing a SQL query against the connected database.
 */
@Data
public class QueryRequest {

    @NotBlank(message = "SQL query cannot be empty")
    private String sql;

    @Min(value = 0, message = "Page must be >= 0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    private int pageSize = 50;
}
