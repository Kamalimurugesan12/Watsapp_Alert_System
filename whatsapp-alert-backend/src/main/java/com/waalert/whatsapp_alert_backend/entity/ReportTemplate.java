package com.waalert.whatsapp_alert_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores user-saved SQL query templates for the Reports page.
 * Persisted in the waalert_system (system) database.
 */
@Entity
@Table(name = "report_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique display name shown in the dropdown. */
    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    /** The saved SQL query. */
    @Column(name = "sql_query", columnDefinition = "TEXT", nullable = false)
    private String sqlQuery;

    /** MYSQL or SQLSERVER — auto-filled from active connection at save time. */
    @Column(name = "database_type", length = 20)
    @Builder.Default
    private String databaseType = "MYSQL";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}