-- -- -- ============================================================
-- -- -- WhatsApp Alert System - Database Schema
-- -- -- ============================================================

-- -- CREATE DATABASE IF NOT EXISTS waalert_system;
-- -- USE waalert_system;

-- -- -- Users table (system login, not the target DB)
-- -- CREATE TABLE IF NOT EXISTS users (
-- --     id BIGINT AUTO_INCREMENT PRIMARY KEY,
-- --     username VARCHAR(100) NOT NULL UNIQUE,
-- --     password VARCHAR(255) NOT NULL,
-- --     email VARCHAR(150),
-- --     role VARCHAR(50) DEFAULT 'ROLE_USER',
-- --     enabled BOOLEAN DEFAULT TRUE,
-- --     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
-- --     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- -- );

-- -- -- Schedules table
-- -- CREATE TABLE IF NOT EXISTS schedules (
-- --     id BIGINT AUTO_INCREMENT PRIMARY KEY,
-- --     name VARCHAR(200) NOT NULL,
-- --     schedule_type ENUM('DAILY','WEEKLY','CUSTOM') NOT NULL,
-- --     cron_expression VARCHAR(100),
-- --     report_type VARCHAR(100),
-- --     sql_query TEXT,
-- --     recipients TEXT,          -- comma-separated phone numbers
-- --     message_template TEXT,
-- --     is_enabled BOOLEAN DEFAULT TRUE,
-- --     last_run_at TIMESTAMP,
-- --     next_run_at TIMESTAMP,
-- --     created_by BIGINT,
-- --     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
-- --     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
-- --     FOREIGN KEY (created_by) REFERENCES users(id)
-- -- );

-- -- -- WhatsApp message logs
-- -- CREATE TABLE IF NOT EXISTS whatsapp_logs (
-- --     id BIGINT AUTO_INCREMENT PRIMARY KEY,
-- --     recipient VARCHAR(20) NOT NULL,
-- --     message_type ENUM('TEXT','DOCUMENT','TEMPLATE') DEFAULT 'TEXT',
-- --     message_body TEXT,
-- --     status ENUM('PENDING','SENT','FAILED','DELIVERED') DEFAULT 'PENDING',
-- --     wa_message_id VARCHAR(255),
-- --     error_message TEXT,
-- --     schedule_id BIGINT,
-- --     sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
-- --     FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
-- -- );

-- -- -- Report generation logs
-- -- CREATE TABLE IF NOT EXISTS report_logs (
-- --     id BIGINT AUTO_INCREMENT PRIMARY KEY,
-- --     report_name VARCHAR(200),
-- --     report_type VARCHAR(100),
-- --     format ENUM('PDF','EXCEL','CSV') DEFAULT 'EXCEL',
-- --     sql_query TEXT,
-- --     file_path VARCHAR(500),
-- --     generated_by BIGINT,
-- --     sent_via_whatsapp BOOLEAN DEFAULT FALSE,
-- --     recipients TEXT,
-- --     status ENUM('GENERATED','SENT','FAILED') DEFAULT 'GENERATED',
-- --     generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
-- --     FOREIGN KEY (generated_by) REFERENCES users(id)
-- -- );

-- -- -- Report templates
-- -- CREATE TABLE IF NOT EXISTS report_templates (
-- --     id BIGINT AUTO_INCREMENT PRIMARY KEY,
-- --     name VARCHAR(200) NOT NULL,
-- --     description TEXT,
-- --     sql_query TEXT NOT NULL,
-- --     columns_config TEXT,       -- JSON: column display names
-- --     is_active BOOLEAN DEFAULT TRUE,
-- --     created_by BIGINT,
-- --     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
-- --     FOREIGN KEY (created_by) REFERENCES users(id)
-- -- );

-- -- -- ---- Sample Data ----

-- -- -- Default admin user (password: Admin@123)
-- -- INSERT IGNORE INTO users (username, password, email, role) VALUES (
-- --     'admin',
-- --     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
-- --     'admin@waalert.com',
-- --     'ROLE_ADMIN'
-- -- );

-- -- -- Sample report templates
-- -- INSERT IGNORE INTO report_templates (name, description, sql_query, created_by) VALUES
-- -- ('Basic Salary Report', 'Monthly salary summary for all employees',
-- --  'SELECT emp_id, emp_name, department, basic_salary, hra, gross_salary FROM employee_salary ORDER BY department, emp_name',
-- --  1),
-- -- ('HRA Report', 'House Rent Allowance report by department',
-- --  'SELECT department, COUNT(*) AS total_employees, SUM(hra) AS total_hra FROM employee_salary GROUP BY department',
-- --  1),
-- -- ('Employee Summary', 'Complete employee details and compensation',
-- --  'SELECT e.emp_id, e.emp_name, e.designation, e.join_date, s.basic_salary, s.gross_salary FROM employees e JOIN employee_salary s ON e.emp_id = s.emp_id',
-- --  1);
-- -- ============================================================
-- -- WhatsApp Alert System - Database Schema
-- -- System DB: waalert_system (NOT the user's target database)
-- -- ============================================================

-- CREATE DATABASE IF NOT EXISTS waalert_system;
-- USE waalert_system;

-- -- Users table (system login, not the target DB)
-- CREATE TABLE IF NOT EXISTS users (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     username VARCHAR(100) NOT NULL UNIQUE,
--     password VARCHAR(255) NOT NULL,
--     email VARCHAR(150),
--     role VARCHAR(50) DEFAULT 'ROLE_USER',
--     enabled BOOLEAN DEFAULT TRUE,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- );

-- -- ✅ NEW: Connection history — tracks every successful DB connection
-- CREATE TABLE IF NOT EXISTS connection_history (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     database_type VARCHAR(20) NOT NULL DEFAULT 'MYSQL',
--     host VARCHAR(255) NOT NULL,
--     port VARCHAR(10) NOT NULL,
--     database_name VARCHAR(255) NOT NULL,
--     username VARCHAR(100) NOT NULL,
--     connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- -- Schedules table
-- CREATE TABLE IF NOT EXISTS schedules (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     name VARCHAR(200) NOT NULL,
--     schedule_type ENUM('DAILY','WEEKLY','CUSTOM') NOT NULL,
--     cron_expression VARCHAR(100),
--     report_type VARCHAR(100),
--     sql_query TEXT,
--     recipients TEXT,
--     message_template TEXT,
--     is_enabled BOOLEAN DEFAULT TRUE,
--     last_run_at TIMESTAMP,
--     next_run_at TIMESTAMP,
--     created_by BIGINT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     FOREIGN KEY (created_by) REFERENCES users(id)
-- );

-- -- WhatsApp message logs
-- CREATE TABLE IF NOT EXISTS whatsapp_logs (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     recipient VARCHAR(20) NOT NULL,
--     message_type ENUM('TEXT','DOCUMENT','TEMPLATE') DEFAULT 'TEXT',
--     message_body TEXT,
--     status ENUM('PENDING','SENT','FAILED','DELIVERED') DEFAULT 'PENDING',
--     wa_message_id VARCHAR(255),
--     error_message TEXT,
--     schedule_id BIGINT,
--     sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
-- );

-- -- Report generation logs
-- CREATE TABLE IF NOT EXISTS report_logs (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     report_name VARCHAR(200),
--     report_type VARCHAR(100),
--     format ENUM('PDF','EXCEL','CSV') DEFAULT 'EXCEL',
--     sql_query TEXT,
--     file_path VARCHAR(500),
--     generated_by BIGINT,
--     sent_via_whatsapp BOOLEAN DEFAULT FALSE,
--     recipients TEXT,
--     status ENUM('GENERATED','SENT','FAILED') DEFAULT 'GENERATED',
--     generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (generated_by) REFERENCES users(id)
-- );

-- -- Report templates
-- CREATE TABLE IF NOT EXISTS report_templates (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     name VARCHAR(200) NOT NULL,
--     description TEXT,
--     sql_query TEXT NOT NULL,
--     columns_config TEXT,
--     is_active BOOLEAN DEFAULT TRUE,
--     created_by BIGINT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (created_by) REFERENCES users(id)
-- );

-- -- Sample data
-- INSERT IGNORE INTO users (username, password, email, role) VALUES (
--     'admin',
--     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
--     'admin@waalert.com',
--     'ROLE_ADMIN'
-- );
-- ============================================================
-- WhatsApp Alert System - Database Schema
-- System DB: waalert_system (NOT the user's target database)
-- ============================================================

CREATE DATABASE IF NOT EXISTS waalert_system;
USE waalert_system;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(150),
    role VARCHAR(50) DEFAULT 'ROLE_USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Connection history
CREATE TABLE IF NOT EXISTS connection_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    database_type VARCHAR(20) NOT NULL DEFAULT 'MYSQL',
    host VARCHAR(255) NOT NULL,
    port VARCHAR(10) NOT NULL,
    database_name VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ NEW: Saved SQL query templates for the Reports page
CREATE TABLE IF NOT EXISTS report_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(500),
    sql_query TEXT NOT NULL,
    database_type VARCHAR(20) DEFAULT 'MYSQL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Schedules table
CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    schedule_type ENUM('DAILY','WEEKLY','CUSTOM') NOT NULL,
    cron_expression VARCHAR(100),
    report_type VARCHAR(100),
    sql_query TEXT,
    recipients TEXT,
    message_template TEXT,
    is_enabled BOOLEAN DEFAULT TRUE,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- WhatsApp message logs
CREATE TABLE IF NOT EXISTS whatsapp_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(20) NOT NULL,
    message_type ENUM('TEXT','DOCUMENT','TEMPLATE') DEFAULT 'TEXT',
    message_body TEXT,
    status ENUM('PENDING','SENT','FAILED','DELIVERED') DEFAULT 'PENDING',
    wa_message_id VARCHAR(255),
    error_message TEXT,
    schedule_id BIGINT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Report generation logs
CREATE TABLE IF NOT EXISTS report_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(200),
    report_type VARCHAR(100),
    format ENUM('PDF','EXCEL','CSV') DEFAULT 'EXCEL',
    sql_query TEXT,
    file_path VARCHAR(500),
    sent_via_whatsapp BOOLEAN DEFAULT FALSE,
    recipients TEXT,
    status ENUM('GENERATED','SENT','FAILED') DEFAULT 'GENERATED',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed data
INSERT IGNORE INTO users (username, password, email, role) VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@waalert.com',
    'ROLE_ADMIN'
);

-- ✅ Seed: Built-in report templates (static ones shown in dropdown)
INSERT IGNORE INTO report_templates (name, description, sql_query, database_type) VALUES
('Employee Report',
 'All employees with designation and department',
 'SELECT s_no, name, designation, department, mob_no, email FROM employee_details ORDER BY department, name',
 'MYSQL'),
('Salary Report',
 'Full salary breakdown per employee (basic, HRA, total)',
 'SELECT e.name, e.mob_no, MAX(CASE WHEN s.component=''basic'' THEN s.amount END) AS basic, MAX(CASE WHEN s.component=''HRA'' THEN s.amount END) AS hra, MAX(CASE WHEN s.component=''total'' THEN s.amount END) AS total, s.month FROM employee_details e JOIN salary_details s ON e.s_no = s.det_s_no GROUP BY e.s_no, e.name, e.mob_no, s.month',
 'MYSQL'),
('Attendance Report',
 'Employee attendance summary',
 'SELECT name, department, designation FROM employee_details ORDER BY department',
 'MYSQL');