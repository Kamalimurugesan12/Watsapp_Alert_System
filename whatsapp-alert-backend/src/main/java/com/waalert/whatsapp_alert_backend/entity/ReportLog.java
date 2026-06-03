package com.waalert.whatsapp_alert_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_name", length = 200)
    private String reportName;

    @Column(name = "report_type", length = 100)
    private String reportType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Format format = Format.EXCEL;

    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.GENERATED;

    @Column(name = "generated_at")
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    public enum Format { PDF, EXCEL, CSV }
    public enum Status { GENERATED, SENT, FAILED }
}
