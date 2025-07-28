package app.store;

import app.config.Env;
import app.model.ApplyLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SqliteStore {
    private static final Logger log = LoggerFactory.getLogger(SqliteStore.class);
    
    private final Env env;
    private String dbPath;
    
    public SqliteStore(Env env) {
        this.env = env;
    }
    
    @PostConstruct
    public void init() {
        dbPath = env.getDbPath();
        createTables();
    }
    
    private void createTables() {
        try {
            File dbFile = new File(dbPath);
            dbFile.getParentFile().mkdirs();
            
            try (Connection conn = getConnection()) {
                // Create applied table
                conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS applied (
                        vacancy_id TEXT PRIMARY KEY,
                        applied_at TIMESTAMP NOT NULL
                    )
                """);
                
                // Create logs table
                conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        vacancy_id TEXT NOT NULL,
                        action TEXT NOT NULL,
                        message TEXT,
                        timestamp TIMESTAMP NOT NULL
                    )
                """);
                
                log.info("Database initialized at: {}", dbPath);
            }
        } catch (SQLException e) {
            log.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
    
    public boolean isApplied(String vacancyId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM applied WHERE vacancy_id = ?")) {
            stmt.setString(1, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check if vacancy {} is applied", vacancyId, e);
            return false;
        }
    }
    
    public void markApplied(String vacancyId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO applied (vacancy_id, applied_at) VALUES (?, ?)")) {
            stmt.setString(1, vacancyId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            log.info("Marked vacancy {} as applied", vacancyId);
        } catch (SQLException e) {
            log.error("Failed to mark vacancy {} as applied", vacancyId, e);
        }
    }
    
    public void addLog(ApplyLog logEntry) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO logs (vacancy_id, action, message, timestamp) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, logEntry.vacancyId());
            stmt.setString(2, logEntry.action());
            stmt.setString(3, logEntry.message());
            stmt.setTimestamp(4, Timestamp.valueOf(logEntry.timestamp()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to add log entry", e);
        }
    }
    
    public List<ApplyLog> getRecentLogs(int limit) {
        List<ApplyLog> logs = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT vacancy_id, action, message, timestamp FROM logs ORDER BY timestamp DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ApplyLog(
                        rs.getString("vacancy_id"),
                        rs.getString("action"),
                        rs.getString("message"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get recent logs", e);
        }
        return logs;
    }
    
    public int getAppliedCount() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM applied")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get applied count", e);
        }
        return 0;
    }
} 