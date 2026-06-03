package com.waalert.whatsapp_alert_backend.repository;

import com.waalert.whatsapp_alert_backend.entity.WhatsAppLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for WhatsApp message delivery logs.
 */
@Repository
public interface WhatsAppLogRepository extends JpaRepository<WhatsAppLog, Long> {

    /** Returns all log entries ordered newest-first, with pagination. */
    Page<WhatsAppLog> findAllByOrderBySentAtDesc(Pageable pageable);
}
