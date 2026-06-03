package com.waalert.whatsapp_alert_backend.repository;

import com.waalert.whatsapp_alert_backend.entity.ConnectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectionHistoryRepository extends JpaRepository<ConnectionHistory, Long> {

    /** Returns the 10 most recent successful connections, newest first. */
    List<ConnectionHistory> findTop10ByOrderByConnectedAtDesc();
}