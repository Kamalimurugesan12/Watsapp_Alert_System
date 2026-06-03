package com.waalert.whatsapp_alert_backend.repository;

import com.waalert.whatsapp_alert_backend.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    /** All active templates, newest first — used to populate the dropdown. */
    List<ReportTemplate> findByIsActiveTrueOrderByCreatedAtDesc();

    /** Check name uniqueness before saving. */
    boolean existsByNameIgnoreCase(String name);

    Optional<ReportTemplate> findByNameIgnoreCase(String name);
}