package com.waalert.whatsapp_alert_backend.repository;

import com.waalert.whatsapp_alert_backend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for scheduled jobs.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /** Returns all schedules that are currently enabled (used by the scheduler runner). */
    List<Schedule> findByIsEnabledTrue();

    /** Returns schedules owned by a specific user. */
   // List<Schedule> findByCreatedByUsername(String username);
}
