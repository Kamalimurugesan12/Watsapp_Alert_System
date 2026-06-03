package com.waalert.whatsapp_alert_backend.config;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic DataSources for user-connected databases.
 * Each session (JWT subject) gets its own DataSource.
 */
@Component
public class DataSourceConfig {

    // Per-user datasource cache
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * Test connectivity before storing the datasource.
     */
    public boolean testConnection(String host, String port, String dbName,
                                   String username, String password) {
        String url = buildUrl(host, port, dbName);
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Register a datasource for the given session key (usually username or JWT subject).
     */
    public void registerDataSource(String sessionKey, String host, String port,
                                    String dbName, String username, String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(buildUrl(host, port, dbName));
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSourceMap.put(sessionKey, ds);
    }

    /**
     * Retrieve an active DataSource for a session.
     */
    public DataSource getDataSource(String sessionKey) {
        DataSource ds = dataSourceMap.get(sessionKey);
        if (ds == null) {
            throw new IllegalStateException("No active database connection for session: " + sessionKey);
        }
        return ds;
    }

    public boolean hasDataSource(String sessionKey) {
        return dataSourceMap.containsKey(sessionKey);
    }

    public void removeDataSource(String sessionKey) {
        dataSourceMap.remove(sessionKey);
    }

    private String buildUrl(String host, String port, String dbName) {
        return String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            host, port, dbName
        );
    }
}