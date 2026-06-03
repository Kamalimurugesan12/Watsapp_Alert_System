package com.waalert.whatsapp_alert_backend.repository;

import com.waalert.whatsapp_alert_backend.entity.ReportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for generated report audit logs.
 */
@Repository
public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {

    /** Returns all log entries ordered newest-first, with pagination. */
    Page<ReportLog> findAllByOrderByGeneratedAtDesc(Pageable pageable);
}
