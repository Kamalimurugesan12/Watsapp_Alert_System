package com.waalert.whatsapp_alert_backend.service;

import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Runs SQL queries against the dynamically connected user database.
 * Gets the live DataSource from DynamicDataSourceManager — no sessionKey needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final DynamicDataSourceManager dataSourceManager;

    private static final List<String> BLOCKED_KEYWORDS = List.of(
        "DROP", "TRUNCATE", "DELETE", "UPDATE", "INSERT",
        "ALTER", "CREATE", "RENAME", "REPLACE", "GRANT", "REVOKE", "SHUTDOWN"
    );

    // ─────────────────────────────────────────────────────────

    public Map<String, Object> executeQuery(String sql, int page, int pageSize) {
        validateQuery(sql);
        DataSource ds = dataSourceManager.getActiveDataSource(); // throws if not connected

        List<String>       columns   = new ArrayList<>();
        List<List<Object>> rows      = new ArrayList<>();
        int                totalRows = 0;

        try (Connection conn = ds.getConnection();
             Statement  stmt = conn.createStatement(
                     ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            stmt.setMaxRows(10_000);
            ResultSet         rs   = stmt.executeQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            int skip      = page * pageSize;
            int collected = 0;

            while (rs.next()) {
                totalRows++;
                if (collected < skip) { collected++; continue; }
                if (rows.size() < pageSize) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object val = rs.getObject(i);
                        if      (val instanceof java.sql.Date d) row.add(d.toString());
                        else if (val instanceof Timestamp ts)    row.add(ts.toString());
                        else                                     row.add(val);
                    }
                    rows.add(row);
                }
                collected++;
            }

        } catch (SQLException e) {
            log.error("SQL execution error: {}", e.getMessage());
            throw new RuntimeException("Query failed: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns",    columns);
        result.put("rows",       rows);
        result.put("totalRows",  totalRows);
        result.put("page",       page);
        result.put("pageSize",   pageSize);
        result.put("totalPages", pageSize > 0 ? (int) Math.ceil((double) totalRows / pageSize) : 1);
        return result;
    }

    public List<String> getTables() {
        DataSource ds = dataSourceManager.getActiveDataSource();
        List<String> tables = new ArrayList<>();
        try (Connection conn = ds.getConnection()) {
            ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
            while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tables: " + e.getMessage());
        }
        return tables;
    }

    public List<Map<String, Object>> getTableColumns(String tableName) {
        DataSource ds = dataSourceManager.getActiveDataSource();
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = ds.getConnection()) {
            ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, "%");
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name",     rs.getString("COLUMN_NAME"));
                col.put("type",     rs.getString("TYPE_NAME"));
                col.put("size",     rs.getInt("COLUMN_SIZE"));
                col.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                columns.add(col);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch columns: " + e.getMessage());
        }
        return columns;
    }

    // ─────────────────────────────────────────────────────────

    private void validateQuery(String sql) {
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("Query cannot be empty.");
        String upper = sql.trim().toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("SHOW")
                && !upper.startsWith("DESCRIBE") && !upper.startsWith("DESC")
                && !upper.startsWith("EXPLAIN")) {
            throw new SecurityException("Only SELECT, SHOW, DESCRIBE queries are permitted.");
        }
        for (String kw : BLOCKED_KEYWORDS) {
            if (upper.matches(".*\\b" + kw + "\\b.*"))
                throw new SecurityException("Blocked keyword '" + kw + "' detected.");
        }
    }
}
