// package com.waalert.whatsapp_alert_backend.controller;

// import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
// import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
// import com.waalert.whatsapp_alert_backend.dto.ConnectRequest;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.sql.SQLException;
// import java.util.Map;

// /**
//  * DatabaseConnectorController
//  * ─────────────────────────────────────────────────────────────
//  * Replaces the old AuthController / login system entirely.
//  *
//  * This is the Spring Boot equivalent of the Python script's:
//  *
//  *   def get_sql_connection():
//  *       return pyodbc.connect("DRIVER=...;SERVER=...;DATABASE=...;UID=...;PWD=...")
//  *
//  * The frontend Connection Screen (replaces Login page) calls
//  * POST /api/connector/connect with the DB credentials.
//  * On success, the backend stores the live DataSource and all
//  * subsequent API calls (query, reports, scheduler) use it.
//  *
//  * No JWT. No session tokens. No user table.
//  */
// @RestController
// @RequestMapping("/api/connector")
// @RequiredArgsConstructor
// @Slf4j
// public class DatabaseConnectorController {

//     private final DynamicDataSourceManager dataSourceManager;

//     /**
//      * Connect to the user-specified database.
//      * Called by the Connection Screen on startup.
//      *
//      * POST /api/connector/connect
//      * Body: { "server":"localhost", "port":"3306", "database":"mydb",
//      *         "username":"root", "password":"secret" }
//      */
//     @PostMapping("/connect")
//     public ResponseEntity<ApiResponse<Map<String, Object>>> connect(
//             @Valid @RequestBody ConnectRequest request) {

//         try {
//             dataSourceManager.connect(
//                     request.getServer(),
//                     request.getPort(),
//                     request.getDatabase(),
//                     request.getUsername(),
//                     request.getPassword()
//             );

//             DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();
//             return ResponseEntity.ok(ApiResponse.success(
//                     "Connected to '" + request.getDatabase() + "' successfully.",
//                     Map.of(
//                         "connected",  true,
//                         "server",     request.getServer(),
//                         "database",   request.getDatabase(),
//                         "username",   request.getUsername(),
//                         "displayName", info.displayName()
//                     )
//             ));

//         } catch (SQLException e) {
//             log.warn("Connection failed for {}@{}/{}: {}", request.getUsername(),
//                     request.getServer(), request.getDatabase(), e.getMessage());
//             return ResponseEntity.badRequest().body(ApiResponse.error(
//                     "Cannot connect to " + request.getServer() + "/" + request.getDatabase() +
//                     ". Check your credentials and ensure the MySQL server is reachable. " +
//                     "Error: " + e.getMessage()
//             ));
//         }
//     }

//     /**
//      * Check connection status.
//      * GET /api/connector/status
//      */
//     @GetMapping("/status")
//     public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
//         boolean connected = dataSourceManager.isConnected();
//         DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();

//         Map<String, Object> data = connected && info != null
//                 ? Map.of("connected", true,  "displayName", info.displayName(),
//                          "database",  info.database(), "server", info.server())
//                 : Map.of("connected", false, "displayName", "Not connected");

//         return ResponseEntity.ok(ApiResponse.success(data));
//     }

//     /**
//      * Disconnect — clears the stored DataSource.
//      * POST /api/connector/disconnect
//      */
//     @PostMapping("/disconnect")
//     public ResponseEntity<ApiResponse<Void>> disconnect() {
//         dataSourceManager.disconnect();
//         return ResponseEntity.ok(ApiResponse.success("Disconnected", null));
//     }

//     /**
//      * Health check — always returns 200.
//      * GET /api/connector/health
//      */
//     @GetMapping("/health")
//     public ResponseEntity<String> health() {
//         return ResponseEntity.ok("WhatsApp Alert System is running");
//     }
// }
// package com.waalert.whatsapp_alert_backend.controller;

// import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
// import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
// import com.waalert.whatsapp_alert_backend.dto.ConnectRequest;
// import com.waalert.whatsapp_alert_backend.entity.ConnectionHistory;
// import com.waalert.whatsapp_alert_backend.repository.ConnectionHistoryRepository;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.sql.SQLException;
// import java.util.List;
// import java.util.Map;

// /**
//  * DatabaseConnectorController
//  *
//  * POST /api/connector/connect     — connect with new credentials (saves history)
//  * POST /api/connector/reconnect   — reconnect from a saved history entry
//  * GET  /api/connector/history     — fetch the last 10 successful connections
//  * GET  /api/connector/status      — current connection status
//  * POST /api/connector/disconnect  — clear active connection
//  * GET  /api/connector/health      — always 200
//  */
// @RestController
// @RequestMapping("/api/connector")
// @RequiredArgsConstructor
// @Slf4j
// public class DatabaseConnectorController {

//     private final DynamicDataSourceManager    dataSourceManager;
//     private final ConnectionHistoryRepository historyRepository;

//     // ─────────────────────────────────────────────────────────
//     // Connect  — supports MYSQL and SQLSERVER
//     // ─────────────────────────────────────────────────────────

//     @PostMapping("/connect")
//     public ResponseEntity<ApiResponse<Map<String, Object>>> connect(
//             @Valid @RequestBody ConnectRequest req) {

//         try {
//             dataSourceManager.connect(
//                     req.getDatabaseType(),
//                     req.getServer(),
//                     req.getPort(),
//                     req.getDatabase(),
//                     req.getUsername(),
//                     req.getPassword()
//             );

//             // Persist to connection history (system DB)
//             historyRepository.save(ConnectionHistory.builder()
//                     .databaseType(req.getDatabaseType() == null ? "MYSQL" : req.getDatabaseType().toUpperCase())
//                     .host(req.getServer())
//                     .port(req.getPort())
//                     .databaseName(req.getDatabase())
//                     .username(req.getUsername())
//                     .build());

//             DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();
//             return ResponseEntity.ok(ApiResponse.success(
//                     "Connected to '" + req.getDatabase() + "' successfully.",
//                     Map.of(
//                         "connected",    true,
//                         "databaseType", info.databaseType(),
//                         "server",       req.getServer(),
//                         "database",     req.getDatabase(),
//                         "username",     req.getUsername(),
//                         "displayName",  info.displayName()
//                     )
//             ));

//         } catch (SQLException e) {
//             log.warn("Connection failed for {}@{}/{}: {}", req.getUsername(),
//                     req.getServer(), req.getDatabase(), e.getMessage());
//             return ResponseEntity.badRequest().body(ApiResponse.error(
//                     "Cannot connect to " + req.getServer() + "/" + req.getDatabase() +
//                     ". Check your credentials and ensure the database server is reachable. " +
//                     "Error: " + e.getMessage()
//             ));
//         }
//     }

//     // ─────────────────────────────────────────────────────────
//     // Reconnect — one-click reconnect from history
//     // POST /api/connector/reconnect
//     // Body: { id, password }  (password re-supplied for security)
//     // ─────────────────────────────────────────────────────────

//     // @PostMapping("/reconnect")
//     // public ResponseEntity<ApiResponse<Map<String, Object>>> reconnect(
//     //         @RequestBody Map<String, Object> body) {

//     //     Long id = Long.parseLong(body.get("id").toString());
//     //     String password = body.get("password").toString();

//     //     return historyRepository.findById(id)
//     //             .map(h -> {
//     //                 try {
//     //                     dataSourceManager.connect(
//     //                             h.getDatabaseType(), h.getHost(), h.getPort(),
//     //                             h.getDatabaseName(), h.getUsername(), password
//     //                     );

//     //                     // Update timestamp in history
//     //                     historyRepository.save(ConnectionHistory.builder()
//     //                             .databaseType(h.getDatabaseType())
//     //                             .host(h.getHost())
//     //                             .port(h.getPort())
//     //                             .databaseName(h.getDatabaseName())
//     //                             .username(h.getUsername())
//     //                             .build());

//     //                     DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();
//     //                     return ResponseEntity.ok(ApiResponse.success(
//     //                             "Reconnected to '" + h.getDatabaseName() + "'.",
//     //                             Map.of(
//     //                                 "connected",    true,
//     //                                 "databaseType", info.databaseType(),
//     //                                 "server",       h.getHost(),
//     //                                 "database",     h.getDatabaseName(),
//     //                                 "displayName",  info.displayName()
//     //                             )
//     //                     ));
//     //                 } catch (SQLException e) {
//     //                     return ResponseEntity.badRequest().body(
//     //                             ApiResponse.<Map<String, Object>>error(
//     //                                     "Reconnect failed: " + e.getMessage()));
//     //                 }
//     //             })
//     //             .orElse(ResponseEntity.badRequest().body(
//     //                     ApiResponse.error("History entry not found.")));
//     // }

//     // ─────────────────────────────────────────────────────────
//     // History — GET /api/connector/history
//     // ─────────────────────────────────────────────────────────
// @PostMapping("/reconnect")
// public ResponseEntity<ApiResponse<Map<String, Object>>> reconnect(
//         @RequestBody Map<String, Object> body) {

//     Long id = Long.parseLong(body.get("id").toString());
//     String password = body.get("password").toString();

//     return historyRepository.findById(id)
//             .map(h -> {
//                 try {
//                     dataSourceManager.connect(
//                             h.getDatabaseType(),
//                             h.getHost(),
//                             h.getPort(),
//                             h.getDatabaseName(),
//                             h.getUsername(),
//                             password
//                     );

//                     // Update timestamp in history
//                     historyRepository.save(ConnectionHistory.builder()
//                             .databaseType(h.getDatabaseType())
//                             .host(h.getHost())
//                             .port(h.getPort())
//                             .databaseName(h.getDatabaseName())
//                             .username(h.getUsername())
//                             .build());

//                     DynamicDataSourceManager.ConnectionInfo info =
//                             dataSourceManager.getConnectionInfo();

//                     Map<String, Object> responseData = Map.of(
//                             "connected", true,
//                             "databaseType", info.databaseType(),
//                             "server", h.getHost(),
//                             "database", h.getDatabaseName(),
//                             "displayName", info.displayName()
//                     );

//                     return ResponseEntity.ok(
//                             ApiResponse.<Map<String, Object>>success(
//                                     "Reconnected to '" + h.getDatabaseName() + "'.",
//                                     responseData
//                             )
//                     );

//                 } catch (SQLException e) {

//                     return ResponseEntity.badRequest().body(
//                             ApiResponse.<Map<String, Object>>error(
//                                     "Reconnect failed: " + e.getMessage()
//                             )
//                     );
//                 }
//             })
//             .orElse(
//                     ResponseEntity.badRequest().body(
//                             ApiResponse.<Map<String, Object>>error(
//                                     "History entry not found."
//                             )
//                     )
//             );
// }
//     @GetMapping("/history")
//     public ResponseEntity<ApiResponse<List<ConnectionHistory>>> history() {
//         return ResponseEntity.ok(ApiResponse.success(
//                 historyRepository.findTop10ByOrderByConnectedAtDesc()));
//     }

//     // ─────────────────────────────────────────────────────────
//     // Status / Disconnect / Health
//     // ─────────────────────────────────────────────────────────

//     @GetMapping("/status")
//     public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
//         boolean connected = dataSourceManager.isConnected();
//         DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();

//         Map<String, Object> data = connected && info != null
//                 ? Map.of("connected", true,  "displayName", info.displayName(),
//                          "database",  info.database(), "server", info.server(),
//                          "databaseType", info.databaseType())
//                 : Map.of("connected", false, "displayName", "Not connected");

//         return ResponseEntity.ok(ApiResponse.success(data));
//     }

//     @PostMapping("/disconnect")
//     public ResponseEntity<ApiResponse<Void>> disconnect() {
//         dataSourceManager.disconnect();
//         return ResponseEntity.ok(ApiResponse.success("Disconnected", null));
//     }

//     @GetMapping("/health")
//     public ResponseEntity<String> health() {
//         return ResponseEntity.ok("WhatsApp Alert System is running");
//     }
// }
package com.waalert.whatsapp_alert_backend.controller;

import com.waalert.whatsapp_alert_backend.config.DynamicDataSourceManager;
import com.waalert.whatsapp_alert_backend.dto.ApiResponse;
import com.waalert.whatsapp_alert_backend.dto.ConnectRequest;
import com.waalert.whatsapp_alert_backend.entity.ConnectionHistory;
import com.waalert.whatsapp_alert_backend.repository.ConnectionHistoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DatabaseConnectorController — all connection management endpoints.
 *
 * POST /api/connector/connect          connect with new credentials
 * POST /api/connector/test-connection  ← NEW: test without saving
 * POST /api/connector/reconnect        reconnect from saved history
 * GET  /api/connector/history          last 10 successful connections
 * GET  /api/connector/status           current connection status
 * POST /api/connector/disconnect       clear active connection
 * GET  /api/connector/health           always 200
 */
@RestController
@RequestMapping("/api/connector")
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectorController {

    private final DynamicDataSourceManager    dataSourceManager;
    private final ConnectionHistoryRepository historyRepository;

    // ─────────────────────────────────────────────────────────
    // POST /api/connector/connect
    // ─────────────────────────────────────────────────────────

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connect(
            @Valid @RequestBody ConnectRequest req) {

        try {
            dataSourceManager.connect(
                    req.getDatabaseType(),
                    req.getServer(),
                    req.getPort(),
                    req.getDatabase(),
                    req.getUsername(),
                    req.getPassword()
            );

            // Save to connection history (system DB)
            historyRepository.save(ConnectionHistory.builder()
                    .databaseType(req.getDatabaseType() == null ? "MYSQL"
                                                                : req.getDatabaseType().toUpperCase())
                    .host(req.getServer())
                    .port(req.getPort())
                    .databaseName(req.getDatabase())
                    .username(req.getUsername())
                    .build());

            DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();
            return ResponseEntity.ok(ApiResponse.success(
                    "Connected to '" + req.getDatabase() + "' successfully.",
                    Map.of(
                        "connected",    true,
                        "databaseType", info.databaseType(),
                        "server",       req.getServer(),
                        "database",     req.getDatabase(),
                        "username",     req.getUsername(),
                        "displayName",  info.displayName()
                    )
            ));

        } catch (SQLException e) {
            log.warn("Connection failed [{}] {}@{}/{}: {}",
                    req.getDatabaseType(), req.getUsername(),
                    req.getServer(), req.getDatabase(), e.getMessage());

            return ResponseEntity.badRequest().body(ApiResponse.error(buildErrorMessage(req, e)));
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/connector/test-connection  ← NEW
    // Test without connecting or saving history
    // ─────────────────────────────────────────────────────────

    @PostMapping("/test-connection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection(
            @RequestBody ConnectRequest req) {

        boolean ok = dataSourceManager.testConnection(
                req.getDatabaseType(),
                req.getServer(),
                req.getPort(),
                req.getDatabase(),
                req.getUsername(),
                req.getPassword()
        );

        if (ok) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Connection test successful.",
                    Map.of("reachable", true,
                           "databaseType", req.getDatabaseType() == null ? "MYSQL" : req.getDatabaseType(),
                           "server", req.getServer(),
                           "database", req.getDatabase())
            ));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Connection test failed. Check your host, port, credentials, " +
                    "and ensure the database server is reachable from this machine."
            ));
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/connector/reconnect
    // One-click reconnect from saved history entry
    // Body: { "id": 5, "password": "secret" }
    // ─────────────────────────────────────────────────────────

    @PostMapping("/reconnect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reconnect(
            @RequestBody Map<String, Object> body) {

        Long   id       = Long.parseLong(body.get("id").toString());
        String password = body.get("password").toString();

        return historyRepository.findById(id)
                .map(h -> {
                    try {
                        dataSourceManager.connect(
                                h.getDatabaseType(),
                                h.getHost(),
                                h.getPort(),
                                h.getDatabaseName(),
                                h.getUsername(),
                                password
                        );

                        // Update timestamp in history
                        historyRepository.save(ConnectionHistory.builder()
                                .databaseType(h.getDatabaseType())
                                .host(h.getHost())
                                .port(h.getPort())
                                .databaseName(h.getDatabaseName())
                                .username(h.getUsername())
                                .build());

                        DynamicDataSourceManager.ConnectionInfo info =
                                dataSourceManager.getConnectionInfo();

                        return ResponseEntity.ok(
                                ApiResponse.<Map<String, Object>>success(
                                        "Reconnected to '" + h.getDatabaseName() + "'.",
                                        Map.of(
                                            "connected",    true,
                                            "databaseType", info.databaseType(),
                                            "server",       h.getHost(),
                                            "database",     h.getDatabaseName(),
                                            "displayName",  info.displayName()
                                        )
                                )
                        );
                    } catch (SQLException e) {
                        return ResponseEntity.badRequest().body(
                                ApiResponse.<Map<String, Object>>error(
                                        "Reconnect failed: " + e.getMessage()
                                )
                        );
                    }
                })
                .orElse(ResponseEntity.badRequest().body(
                        ApiResponse.<Map<String, Object>>error("History entry not found.")
                ));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/connector/history
    // ─────────────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ConnectionHistory>>> history() {
        return ResponseEntity.ok(ApiResponse.success(
                historyRepository.findTop10ByOrderByConnectedAtDesc()));
    }

    // DELETE /api/connector/history/{id}
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(@PathVariable Long id) {
        historyRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ─────────────────────────────────────────────────────────
    // Status / Disconnect / Health
    // ─────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        boolean connected = dataSourceManager.isConnected();
        DynamicDataSourceManager.ConnectionInfo info = dataSourceManager.getConnectionInfo();

        Map<String, Object> data = connected && info != null
                ? Map.of("connected", true, "displayName", info.displayName(),
                         "database",  info.database(), "server", info.server(),
                         "databaseType", info.databaseType())
                : Map.of("connected", false, "displayName", "Not connected");

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        dataSourceManager.disconnect();
        return ResponseEntity.ok(ApiResponse.success("Disconnected", null));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("WhatsApp Alert System is running");
    }

    // ─────────────────────────────────────────────────────────
    // Error message builder — explains WHY connection failed
    // ─────────────────────────────────────────────────────────

    private String buildErrorMessage(ConnectRequest req, SQLException e) {
        String base = "Cannot connect to " + req.getServer() + "/" + req.getDatabase() + ".";
        String hint;
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (msg.contains("communications link failure") || msg.contains("connection refused")) {
            hint = " The server is unreachable. Check: " +
                   "(1) MySQL bind-address in my.cnf — change to 0.0.0.0 for external access, " +
                   "(2) Firewall allows port " + req.getPort() + ", " +
                   "(3) Host '" + req.getServer() + "' is correct.";
        } else if (msg.contains("access denied")) {
            hint = " Access denied. Check username and password.";
        } else if (msg.contains("unknown database") || msg.contains("cannot open database")) {
            hint = " Database '" + req.getDatabase() + "' does not exist on the server.";
        } else if (msg.contains("no suitable driver")) {
            hint = " JDBC driver not found. Ensure mssql-jdbc is in pom.xml for SQL Server.";
        } else if (msg.contains("ssl") || msg.contains("certificate")) {
            hint = " SSL/certificate error. For internal servers, this should not occur with " +
                   "trustServerCertificate=true (SQL Server) or useSSL=false (MySQL).";
        } else {
            hint = " Error: " + e.getMessage();
        }

        return base + hint;
    }
}