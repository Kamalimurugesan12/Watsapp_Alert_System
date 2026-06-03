package com.waalert.whatsapp_alert_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Schedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "report_type", length = 100)
    private String reportType;

    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    /** Comma-separated phone numbers for WhatsApp output */
    @Column(columnDefinition = "TEXT")
    private String recipients;

    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate;

    /** WHATSAPP, EMAIL, BOTH */
    @Column(name = "output_type", length = 20)
    @Builder.Default
    private String outputType = "WHATSAPP";

    @Column(name = "email_recipient", length = 200)
    private String emailRecipient;

    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ScheduleType { DAILY, WEEKLY, CUSTOM }
}
