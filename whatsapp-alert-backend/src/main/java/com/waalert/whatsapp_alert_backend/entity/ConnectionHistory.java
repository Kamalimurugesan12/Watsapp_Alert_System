package com.waalert.whatsapp_alert_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores every successful database connection for the history panel.
 * Saved in the waalert_system (system) database — NOT the user's target DB.
 */
@Entity
@Table(name = "connection_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "database_type", length = 20, nullable = false)
    private String databaseType;   // MYSQL | SQLSERVER

    @Column(nullable = false)
    private String host;

    @Column(nullable = false, length = 10)
    private String port;

    @Column(name = "database_name", nullable = false)
    private String databaseName;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @PrePersist
    void prePersist() {
        if (connectedAt == null) connectedAt = LocalDateTime.now();
    }
}