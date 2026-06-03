package com.waalert.whatsapp_alert_backend.service;

import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
import com.waalert.whatsapp_alert_backend.dto.SchedulerRequest;
import com.waalert.whatsapp_alert_backend.dto.WhatsAppRequest;
import com.waalert.whatsapp_alert_backend.entity.Schedule;
import com.waalert.whatsapp_alert_backend.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SchedulerService — Spring Boot equivalent of the Python script's:
 *
 *   def run_job(job):
 *       rows = fetch_data(job[2])          # run the query
 *       body = build_html_table(rows)
 *       if job[4] == "Email":
 *           send_email(...)               # send output
 *       elif job[4] == "WhatsApp":
 *           send_whatsapp(...)
 *
 *   def schedule_jobs():
 *       for job in jobs WHERE active=1:
 *           schedule.every(job[3]).minutes.do(run_job, job)
 *
 * Uses Spring's @Scheduled poller (every 60 s) instead of Python's
 * schedule library + daemon thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ScheduleRepository       scheduleRepository;
    private final DynamicDataSourceManager dataSourceManager;
    private final WhatsAppService          whatsAppService;
    private final DatabaseService          databaseService;
    private final EmailService             emailService;

    // ─────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────

    public Schedule create(SchedulerRequest req) {
        String recipients = (req.getRecipients() != null)
                ? String.join(",", req.getRecipients()) : "";

        Schedule s = Schedule.builder()
                .name(req.getName())
                .scheduleType(Schedule.ScheduleType.valueOf(req.getScheduleType().toUpperCase()))
                .cronExpression(defaultCron(req))
                .reportType(req.getReportType())
                .sqlQuery(req.getSqlQuery())
                .recipients(recipients)
                .messageTemplate(req.getMessageTemplate())
                .outputType(req.getOutputType())
                .emailRecipient(req.getEmailRecipient())
                .isEnabled(req.isEnabled())
                .build();

        return scheduleRepository.save(s);
    }

    public List<Schedule> getAll()            { return scheduleRepository.findAll(); }
    public void delete(Long id)               { scheduleRepository.deleteById(id); }

    public Schedule toggle(Long id, boolean enabled) {
        Schedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + id));
        s.setIsEnabled(enabled);
        return scheduleRepository.save(s);
    }

    // ─────────────────────────────────────────────────────────
    // Scheduler runner — equivalent to Python's while True: schedule.run_pending()
    // ─────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    public void runDueSchedules() {
        // Don't run if no DB is connected (can't fetch data)
        if (!dataSourceManager.isConnected()) return;

        List<Schedule> active = scheduleRepository.findByIsEnabledTrue();
        LocalDateTime  now    = LocalDateTime.now();

        for (Schedule s : active) {
            try {
                if (isDue(s, now)) {
                    log.info("Running scheduled job: '{}'", s.getName());
                    runJob(s);
                    s.setLastRunAt(now);
                    scheduleRepository.save(s);
                }
            } catch (Exception e) {
                log.error("Job '{}' failed: {}", s.getName(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // run_job() equivalent
    // ─────────────────────────────────────────────────────────

    /**
     * Executes a single scheduled job:
     * 1. Runs the SQL query against the connected DB
     * 2. Sends output via WhatsApp and/or Email depending on outputType
     */
    private void runJob(Schedule job) {
        if (job.getSqlQuery() == null || job.getSqlQuery().isBlank()) {
            log.warn("Job '{}' has no SQL query — skipping", job.getName());
            return;
        }

        // fetch_data(query) equivalent
        Map<String, Object> queryResult = databaseService.executeQuery(job.getSqlQuery(), 0, 500);
        @SuppressWarnings("unchecked")
        List<String>       columns = (List<String>)       queryResult.get("columns");
        @SuppressWarnings("unchecked")
        List<List<Object>> rows    = (List<List<Object>>) queryResult.get("rows");

        String outputType = job.getOutputType() != null ? job.getOutputType().toUpperCase() : "WHATSAPP";

        // ── WhatsApp output ───────────────────────────────────
        if (outputType.equals("WHATSAPP") || outputType.equals("BOTH")) {
            if (job.getRecipients() != null && !job.getRecipients().isBlank()) {
                String message = buildWhatsAppMessage(job, columns, rows);
                WhatsAppRequest waReq = new WhatsAppRequest();
                waReq.setRecipients(Arrays.asList(job.getRecipients().split(",")));
                waReq.setMessage(message);
                whatsAppService.sendMessages(waReq);
                log.info("WhatsApp sent for job '{}'", job.getName());
            }
        }

        // ── Email output ──────────────────────────────────────
        if (outputType.equals("EMAIL") || outputType.equals("BOTH")) {
            if (job.getEmailRecipient() != null && !job.getEmailRecipient().isBlank()) {
                String htmlBody = emailService.buildHtmlTable(job.getName(), columns, rows);
                emailService.sendEmail("Scheduled Report: " + job.getName(), htmlBody, job.getEmailRecipient());
                log.info("Email sent for job '{}'", job.getName());
            }
        }
    }

    private String buildWhatsAppMessage(Schedule job, List<String> columns, List<List<Object>> rows) {
        if (job.getMessageTemplate() != null && !job.getMessageTemplate().isBlank()) {
            return job.getMessageTemplate()
                    .replace("{date}", LocalDateTime.now().toLocalDate().toString())
                    .replace("{name}", job.getName())
                    .replace("{count}", String.valueOf(rows.size()));
        }
        return whatsAppService.formatQueryResultAsMessage(columns, rows, job.getName());
    }

    private boolean isDue(Schedule s, LocalDateTime now) {
        if (s.getLastRunAt() == null) return true;
        return switch (s.getScheduleType()) {
            case DAILY  -> s.getLastRunAt().plusDays(1).isBefore(now);
            case WEEKLY -> s.getLastRunAt().plusWeeks(1).isBefore(now);
            case CUSTOM -> s.getNextRunAt() != null && s.getNextRunAt().isBefore(now);
        };
    }

    private String defaultCron(SchedulerRequest req) {
        if (req.getCronExpression() != null && !req.getCronExpression().isBlank())
            return req.getCronExpression();
        return switch (req.getScheduleType().toUpperCase()) {
            case "WEEKLY" -> "0 0 8 ? * MON";
            default       -> "0 0 8 * * ?";   // 8 AM daily
        };
    }
}
