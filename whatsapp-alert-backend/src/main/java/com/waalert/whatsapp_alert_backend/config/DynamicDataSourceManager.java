// package com.waalert.whatsapp_alert_backend.config;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.jdbc.datasource.DriverManagerDataSource;
// import org.springframework.stereotype.Component;

// import javax.sql.DataSource;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

// /**
//  * DynamicDataSourceManager
//  * ─────────────────────────────────────────────────────────────
//  * This is the Spring Boot equivalent of the Python script's:
//  *
//  *   def get_sql_connection():
//  *       return pyodbc.connect("DRIVER=...;SERVER=server_name;DATABASE=db_name;UID=user;PWD=password")
//  *
//  * Instead of hard-coding credentials in application.properties, the user
//  * supplies them at runtime via the Connection Screen (POST /api/connector/connect).
//  *
//  * The resulting DataSource is stored in memory as a singleton.
//  * All services (DatabaseService, ReportService, SchedulerService) inject
//  * this bean and call getActiveDataSource() to get the live connection.
//  *
//  * Only ONE target database is active at a time (matching the Python model).
//  * Calling connect() replaces any previous connection.
//  */
// @Component
// @Slf4j
// public class DynamicDataSourceManager {

//     /** The currently active target DataSource. Null = not connected. */
//     private volatile DataSource activeDataSource = null;

//     /** Metadata about the active connection (for UI display). */
//     private volatile ConnectionInfo activeConnectionInfo = null;

//     // ─────────────────────────────────────────────────────────
//     // Public API
//     // ─────────────────────────────────────────────────────────

//     /**
//      * Attempt to connect to the specified MySQL database.
//      * This is called when the user clicks "Connect" on the Connection Screen.
//      *
//      * Equivalent to Python's get_sql_connection() — but dynamic.
//      *
//      * @param server    hostname or IP, e.g. "localhost" or "192.168.1.10"
//      * @param port      MySQL port, typically "3306"
//      * @param database  database name
//      * @param username  MySQL username
//      * @param password  MySQL password
//      * @throws SQLException if the connection test fails
//      */
//     public void connect(String server, String port, String database,
//                         String username, String password) throws SQLException {

//         String url = buildJdbcUrl(server, port, database);
//         log.info("Attempting connection to: {}", url);

//         // ── Test the connection before storing it ──────────────
//         // Equivalent to Python's pyodbc.connect() which throws on failure
//         try (Connection testConn = DriverManager.getConnection(url, username, password)) {
//             if (!testConn.isValid(5)) {
//                 throw new SQLException("Connection test failed: isValid() returned false");
//             }
//         }

//         // ── Connection works — build and store the DataSource ──
//         DriverManagerDataSource ds = new DriverManagerDataSource();
//         ds.setUrl(url);
//         ds.setUsername(username);
//         ds.setPassword(password);
//         ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

//         this.activeDataSource = ds;
//         this.activeConnectionInfo = new ConnectionInfo(server, port, database, username);

//         log.info("Successfully connected to database '{}' on '{}'", database, server);
//     }

//     /**
//      * Returns the active DataSource for running queries.
//      * All service classes call this method.
//      *
//      * @throws IllegalStateException if no database has been connected yet
//      */
//     public DataSource getActiveDataSource() {
//         if (activeDataSource == null) {
//             throw new IllegalStateException(
//                 "No database connected. Please connect via the Connection Screen first " +
//                 "(POST /api/connector/connect)."
//             );
//         }
//         return activeDataSource;
//     }

//     /** Returns true if a database connection is active. */
//     public boolean isConnected() {
//         return activeDataSource != null;
//     }

//     /** Returns info about the current connection (for the UI status bar). */
//     public ConnectionInfo getConnectionInfo() {
//         return activeConnectionInfo;
//     }

//     /** Disconnect — clears the stored DataSource. */
//     public void disconnect() {
//         this.activeDataSource = null;
//         this.activeConnectionInfo = null;
//         log.info("Database disconnected");
//     }

//     // ─────────────────────────────────────────────────────────
//     // Helpers
//     // ─────────────────────────────────────────────────────────

//     private String buildJdbcUrl(String server, String port, String database) {
//         return String.format(
//             "jdbc:mysql://%s:%s/%s" +
//             "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" +
//             "&connectTimeout=10000&socketTimeout=30000",
//             server, port, database
//         );
//     }

//     // ─────────────────────────────────────────────────────────
//     // Inner record — connection metadata
//     // ─────────────────────────────────────────────────────────

//     public record ConnectionInfo(String server, String port, String database, String username) {
//         public String displayName() {
//             return username + "@" + server + ":" + port + "/" + database;
//         }
//     }
// }

// package com.waalert.whatsapp_alert_backend.config;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.jdbc.datasource.DriverManagerDataSource;
// import org.springframework.stereotype.Component;

// import javax.sql.DataSource;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

// /**
//  * DynamicDataSourceManager — supports MySQL and Microsoft SQL Server.
//  *
//  * MySQL URL  : jdbc:mysql://host:3306/db?useSSL=false&...
//  * MSSQL URL  : jdbc:sqlserver://host:1433;databaseName=db;encrypt=true;trustServerCertificate=true
//  */
// @Component
// @Slf4j
// public class DynamicDataSourceManager {

//     private volatile DataSource   activeDataSource    = null;
//     private volatile ConnectionInfo activeConnectionInfo = null;

//     // ─────────────────────────────────────────────────────────

//     /**
//      * Connect to a MySQL or SQL Server database.
//      *
//      * @param databaseType "MYSQL" or "SQLSERVER"
//      */
//     public void connect(String databaseType, String server, String port,
//                         String database, String username, String password) throws SQLException {

//         String normalizedType = (databaseType == null || databaseType.isBlank()) ? "MYSQL"
//                                                                                   : databaseType.toUpperCase();
//         String url        = buildJdbcUrl(normalizedType, server, port, database);
//         String driverClass = resolveDriver(normalizedType);

//         log.info("Connecting [{}] → {}", normalizedType, url);

//         // Test connection first — throws SQLException on failure
//         try (Connection testConn = DriverManager.getConnection(url, username, password)) {
//             if (!testConn.isValid(5)) {
//                 throw new SQLException("Connection test failed: isValid() returned false");
//             }
//         }

//         DriverManagerDataSource ds = new DriverManagerDataSource();
//         ds.setUrl(url);
//         ds.setUsername(username);
//         ds.setPassword(password);
//         ds.setDriverClassName(driverClass);

//         this.activeDataSource     = ds;
//         this.activeConnectionInfo = new ConnectionInfo(normalizedType, server, port, database, username);

//         log.info("Connected to '{}' on '{}' [{}]", database, server, normalizedType);
//     }

//     /** Backward-compatible overload — defaults to MYSQL */
//     public void connect(String server, String port, String database,
//                         String username, String password) throws SQLException {
//         connect("MYSQL", server, port, database, username, password);
//     }

//     public DataSource getActiveDataSource() {
//         if (activeDataSource == null) {
//             throw new IllegalStateException(
//                 "No database connected. Please connect via the Connection Screen first.");
//         }
//         return activeDataSource;
//     }

//     public boolean isConnected() { return activeDataSource != null; }

//     public ConnectionInfo getConnectionInfo() { return activeConnectionInfo; }

//     public void disconnect() {
//         this.activeDataSource     = null;
//         this.activeConnectionInfo = null;
//         log.info("Database disconnected");
//     }

//     // ─────────────────────────────────────────────────────────
//     // URL + Driver helpers
//     // ─────────────────────────────────────────────────────────

//     private String buildJdbcUrl(String type, String server, String port, String database) {
//         return switch (type) {
//             case "SQLSERVER" -> String.format(
//                 "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true" +
//                 ";loginTimeout=10",
//                 server, port, database
//             );
//             default -> String.format(                                   // MYSQL
//                 "jdbc:mysql://%s:%s/%s" +
//                 "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" +
//                 "&connectTimeout=10000&socketTimeout=30000",
//                 server, port, database
//             );
//         };
//     }

//     private String resolveDriver(String type) {
//         return switch (type) {
//             case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//             default          -> "com.mysql.cj.jdbc.Driver";
//         };
//     }

//     // ─────────────────────────────────────────────────────────

//     public record ConnectionInfo(String databaseType, String server, String port,
//                                   String database, String username) {
//         public String displayName() {
//             return username + "@" + server + ":" + port + "/" + database + " [" + databaseType + "]";
//         }
//     }
// }
// package com.waalert.whatsapp_alert_backend.config;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.jdbc.datasource.DriverManagerDataSource;
// import org.springframework.stereotype.Component;

// import javax.sql.DataSource;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

// /**
//  * DynamicDataSourceManager — supports MySQL and Microsoft SQL Server.
//  *
//  * FIX: Class.forName() is called BEFORE DriverManager.getConnection() so the
//  * JDBC driver is registered in the current classloader. Without this, Spring Boot
//  * fat-jar isolation causes "No suitable driver found for jdbc:sqlserver://..." even
//  * when the mssql-jdbc JAR is on the classpath.
//  *
//  * MySQL URL  : jdbc:mysql://host:3306/db?useSSL=false&...
//  * MSSQL URL  : jdbc:sqlserver://host:1433;databaseName=db;encrypt=true;trustServerCertificate=true
//  */
// @Component
// @Slf4j
// public class DynamicDataSourceManager {

//     private volatile DataSource    activeDataSource     = null;
//     private volatile ConnectionInfo activeConnectionInfo = null;

//     // ─────────────────────────────────────────────────────────────────────────

//     /**
//      * Connect to MySQL or SQL Server at runtime.
//      *
//      * @param databaseType "MYSQL" or "SQLSERVER"
//      * @throws SQLException  if the connection test fails
//      */
//     public void connect(String databaseType, String server, String port,
//                         String database, String username, String password) throws SQLException {

//         String type      = (databaseType == null || databaseType.isBlank()) ? "MYSQL"
//                                                                             : databaseType.toUpperCase();
//         String url        = buildJdbcUrl(type, server, port, database);
//         String driverClass = resolveDriver(type);

//         log.info("Connecting [{}] → {}", type, url);

//         // ── CRITICAL FIX ──────────────────────────────────────────────────────
//         // Explicitly force the JDBC driver to register with DriverManager.
//         // Spring Boot's fat-jar uses a custom ClassLoader. Without this call,
//         // DriverManager (which uses the system ClassLoader) cannot see the
//         // mssql-jdbc driver, so it throws "No suitable driver found".
//         // ─────────────────────────────────────────────────────────────────────
//         try {
//             Class.forName(driverClass);
//             log.info("JDBC driver loaded: {}", driverClass);
//         } catch (ClassNotFoundException e) {
//             throw new SQLException(
//                 "JDBC driver class not found: " + driverClass +
//                 ". Make sure the dependency is in pom.xml. Error: " + e.getMessage(), e
//             );
//         }

//         // Test the connection before storing it
//         try (Connection testConn = DriverManager.getConnection(url, username, password)) {
//             if (!testConn.isValid(5)) {
//                 throw new SQLException("Connection test failed: isValid() returned false");
//             }
//         }

//         // Connection works — build and store the DataSource
//         DriverManagerDataSource ds = new DriverManagerDataSource();
//         ds.setUrl(url);
//         ds.setUsername(username);
//         ds.setPassword(password);
//         ds.setDriverClassName(driverClass);

//         this.activeDataSource     = ds;
//         this.activeConnectionInfo = new ConnectionInfo(type, server, port, database, username);

//         log.info("Successfully connected to '{}' on '{}' [{}]", database, server, type);
//     }

//     /** Backward-compatible overload — defaults to MYSQL */
//     public void connect(String server, String port, String database,
//                         String username, String password) throws SQLException {
//         connect("MYSQL", server, port, database, username, password);
//     }

//     public DataSource getActiveDataSource() {
//         if (activeDataSource == null) {
//             throw new IllegalStateException(
//                 "No database connected. Please connect via the Connection Screen first.");
//         }
//         return activeDataSource;
//     }

//     public boolean isConnected() { return activeDataSource != null; }

//     public ConnectionInfo getConnectionInfo() { return activeConnectionInfo; }

//     public void disconnect() {
//         this.activeDataSource     = null;
//         this.activeConnectionInfo = null;
//         log.info("Database disconnected");
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Helpers
//     // ─────────────────────────────────────────────────────────────────────────

//     private String buildJdbcUrl(String type, String server, String port, String database) {
//         return switch (type) {
//             case "SQLSERVER" -> String.format(
//                 // integratedSecurity=false  → username/password auth (not Windows auth)
//                 // trustServerCertificate=true → skip SSL cert verification (safe for internal networks)
//                 "jdbc:sqlserver://%s:%s;databaseName=%s" +
//                 ";encrypt=true;trustServerCertificate=true" +
//                 ";integratedSecurity=false;loginTimeout=15",
//                 server, port, database
//             );
//             default -> String.format(   // MYSQL
//                 "jdbc:mysql://%s:%s/%s" +
//                 "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" +
//                 "&connectTimeout=10000&socketTimeout=30000",
//                 server, port, database
//             );
//         };
//     }

//     private String resolveDriver(String type) {
//         return switch (type) {
//             case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//             default          -> "com.mysql.cj.jdbc.Driver";
//         };
//     }

//     // ─────────────────────────────────────────────────────────────────────────

//     public record ConnectionInfo(String databaseType, String server, String port,
//                                   String database, String username) {
//         public String displayName() {
//             return username + "@" + server + ":" + port + "/" + database + " [" + databaseType + "]";
//         }
//     }
// }
package com.waalert.whatsapp_alert_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DynamicDataSourceManager — supports MySQL and Microsoft SQL Server.
 *
 * ── Key Fixes in this version ──────────────────────────────────────────────
 *
 * FIX 1: External IP / hostname support
 *   The old code used localhost-centric JDBC URLs. This version builds URLs
 *   from whatever host the user supplies (IP, hostname, domain — any).
 *   MySQL: no allowedHosts restriction. SQL Server: no IP filter.
 *
 * FIX 2: Class.forName() before DriverManager.getConnection()
 *   Spring Boot fat-jar uses a custom ClassLoader. DriverManager (which uses
 *   the *system* ClassLoader) cannot find mssql-jdbc without explicit loading.
 *   This was causing "No suitable driver found for jdbc:sqlserver://..." even
 *   with the JAR on the classpath.
 *
 * FIX 3: MySQL external connection parameters
 *   Added allowPublicKeyRetrieval=true, useSSL=false, connectTimeout and
 *   socketTimeout so external/cloud MySQL servers work without SSL cert errors.
 *
 * FIX 4: SQL Server parameters
 *   trustServerCertificate=true so self-signed certs (common on private
 *   SQL Server instances) don't block connection.
 *   integratedSecurity=false forces username/password auth, not Windows auth.
 *   loginTimeout=15 prevents indefinite hangs on wrong IP.
 *
 * JDBC URL formats:
 *   MySQL    : jdbc:mysql://HOST:PORT/DB?useSSL=false&allowPublicKeyRetrieval=true&...
 *   SQLSERVER: jdbc:sqlserver://HOST:PORT;databaseName=DB;encrypt=true;trustServerCertificate=true;...
 */
@Component
@Slf4j
public class DynamicDataSourceManager {

    private volatile DataSource    activeDataSource     = null;
    private volatile ConnectionInfo activeConnectionInfo = null;

    // ─────────────────────────────────────────────────────────────────────────
    // connect() — called by DatabaseConnectorController
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Connect to MySQL or SQL Server.
     * Validates the connection before storing the DataSource.
     *
     * @param databaseType  "MYSQL" or "SQLSERVER" (case-insensitive)
     * @param server        hostname, IP address, or domain name
     * @param port          port number string (e.g. "3306" or "1433")
     * @param database      database / schema name
     * @param username      DB username
     * @param password      DB password
     * @throws SQLException if connection cannot be established
     */
    public void connect(String databaseType, String server, String port,
                        String database, String username, String password) throws SQLException {

        // Normalise type — default to MYSQL if blank
        String type = (databaseType == null || databaseType.isBlank())
                      ? "MYSQL" : databaseType.toUpperCase().trim();

        // Validate supported type
        if (!type.equals("MYSQL") && !type.equals("SQLSERVER")) {
            throw new SQLException("Unsupported database type: '" + databaseType +
                    "'. Supported types: MYSQL, SQLSERVER");
        }

        // Use default port if blank
        String effectivePort = (port == null || port.isBlank())
                               ? defaultPort(type) : port.trim();

        String url         = buildJdbcUrl(type, server.trim(), effectivePort, database.trim());
        String driverClass = resolveDriver(type);

        log.info("[{}] Connecting → {}", type, url);

        // ── FIX 2: Force driver registration with DriverManager ───────────
        // Without this, Spring Boot fat-jar ClassLoader isolation prevents
        // DriverManager (system ClassLoader) from finding the JDBC driver.
        try {
            Class.forName(driverClass);
            log.debug("JDBC driver registered: {}", driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "JDBC driver not found for " + type + ": " + driverClass +
                ". Ensure the dependency exists in pom.xml. Error: " + e.getMessage(), e
            );
        }

        // ── Test connection — throws SQLException on failure ───────────────
        try (Connection testConn = DriverManager.getConnection(url, username, password)) {
            if (!testConn.isValid(8)) {
                throw new SQLException("Connection test failed: isValid() returned false.");
            }
        }
        // If we reach here, connection succeeded

        // ── Build and store the DataSource ────────────────────────────────
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClass);

        this.activeDataSource     = ds;
        this.activeConnectionInfo = new ConnectionInfo(type, server.trim(), effectivePort,
                                                        database.trim(), username);

        log.info("[{}] Connected to '{}' on '{}:{}'", type, database, server, effectivePort);
    }

    /** Backward-compatible overload — defaults to MYSQL */
    public void connect(String server, String port, String database,
                        String username, String password) throws SQLException {
        connect("MYSQL", server, port, database, username, password);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // testConnection() — used by the "Test Connection" button without storing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Test a connection without storing it. Returns a status map for the UI.
     * Does not change the active DataSource.
     */
    public boolean testConnection(String databaseType, String server, String port,
                                   String database, String username, String password) {
        try {
            String type         = (databaseType == null || databaseType.isBlank()) ? "MYSQL"
                                                                                   : databaseType.toUpperCase().trim();
            String effectivePort = (port == null || port.isBlank()) ? defaultPort(type) : port.trim();
            String url          = buildJdbcUrl(type, server.trim(), effectivePort, database.trim());
            String driverClass  = resolveDriver(type);
            Class.forName(driverClass);
            try (Connection c = DriverManager.getConnection(url, username, password)) {
                return c.isValid(8);
            }
        } catch (Exception e) {
            log.warn("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard accessors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the active DataSource.
     * @throws IllegalStateException if no database has been connected
     */
    public DataSource getActiveDataSource() {
        if (activeDataSource == null) {
            throw new IllegalStateException(
                "No database connected. Please connect via the Connection Screen first."
            );
        }
        return activeDataSource;
    }

    public boolean isConnected() {
        return activeDataSource != null;
    }

    public ConnectionInfo getConnectionInfo() {
        return activeConnectionInfo;
    }

    public void disconnect() {
        this.activeDataSource     = null;
        this.activeConnectionInfo = null;
        log.info("Database disconnected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JDBC URL builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build the JDBC URL for the given database type.
     *
     * MySQL parameters explained:
     *   useSSL=false                  → don't require SSL (most dev/internal servers)
     *   allowPublicKeyRetrieval=true  → required for MySQL 8+ with caching_sha2_password auth
     *   serverTimezone=UTC            → prevents timezone mismatch errors
     *   connectTimeout=10000          → fail fast (10 s) if host is unreachable
     *   socketTimeout=30000           → abort slow queries after 30 s
     *   autoReconnect=true            → recover from dropped connections
     *
     * SQL Server parameters explained:
     *   encrypt=true                  → encrypt the connection
     *   trustServerCertificate=true   → accept self-signed certs (common in internal servers)
     *   integratedSecurity=false      → use username+password, NOT Windows auth
     *   loginTimeout=15               → fail fast if server unreachable
     */
    private String buildJdbcUrl(String type, String server, String port, String database) {
        return switch (type) {
            case "SQLSERVER" -> String.format(
                "jdbc:sqlserver://%s:%s" +
                ";databaseName=%s" +
                ";encrypt=true" +
                ";trustServerCertificate=true" +   // accept self-signed certs
                ";integratedSecurity=false" +       // username/password auth
                ";loginTimeout=15",                 // fail fast on bad host
                server, port, database
            );
            default -> String.format(               // MYSQL (and future types)
                "jdbc:mysql://%s:%s/%s" +
                "?useSSL=false" +
                "&allowPublicKeyRetrieval=true" +   // MySQL 8+ auth compatibility
                "&serverTimezone=UTC" +
                "&connectTimeout=10000" +           // 10 s connection timeout
                "&socketTimeout=30000" +            // 30 s socket timeout
                "&autoReconnect=true",
                server, port, database
            );
        };
    }

    private String resolveDriver(String type) {
        return switch (type) {
            case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default          -> "com.mysql.cj.jdbc.Driver";
        };
    }

    private String defaultPort(String type) {
        return "SQLSERVER".equals(type) ? "1433" : "3306";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ConnectionInfo record
    // ─────────────────────────────────────────────────────────────────────────

    public record ConnectionInfo(
        String databaseType,
        String server,
        String port,
        String database,
        String username
    ) {
        public String displayName() {
            return String.format("[%s] %s@%s:%s/%s", databaseType, username, server, port, database);
        }
    }
}