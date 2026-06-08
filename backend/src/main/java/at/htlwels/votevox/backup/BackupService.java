package at.htlwels.votevox.backup;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Produces a SQL dump of the current database content into a timestamped file
 * under the configured backup directory.
 * <p>
 * Implementation notes: a pure JDBC dump is used (rather than shelling out to
 * {@code pg_dump}) so the application has no external dependencies. The dump
 * is sufficient for restoring data into an empty schema created by Flyway.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    /**
     * Order matters: parents before children, so a sequential apply on an
     * empty schema satisfies foreign-key constraints.
     */
    private static final List<String> TABLES = List.of(
            "admin_users",
            "students",
            "candidates",
            "elections",
            "election_candidates",
            "tokens",
            "student_tokens",
            "votes",
            "audit_log"
    );

    @Value("${votevox.backup.directory}")
    private String backupDirectory;

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Path triggerBackup() {
        try {
            Path dir = Paths.get(backupDirectory).toAbsolutePath();
            Files.createDirectories(dir);
            String fileName = "votevox-backup-" + STAMP.format(Instant.now()) + ".sql";
            Path file = dir.resolve(fileName);

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("-- VoteVox backup generated at " + Instant.now() + "\n");
                writer.write("-- Apply against an empty schema (created by Flyway V1).\n");
                writer.write("BEGIN;\n\n");
                for (String table : TABLES) {
                    dumpTable(table, writer);
                }
                writer.write("COMMIT;\n");
            }

            auditService.logAdmin(currentAdminId(), "BACKUP_CREATED",
                    "File: " + file.getFileName());
            log.info("Backup written to {}", file);
            return file;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to create backup", ex);
        }
    }

    private void dumpTable(String table, BufferedWriter writer) throws IOException {
        writer.write("-- Table: " + table + "\n");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table);
        if (rows.isEmpty()) {
            writer.write("-- (empty)\n\n");
            return;
        }
        List<String> columns = List.copyOf(rows.get(0).keySet());
        String colList = String.join(", ", columns);
        for (Map<String, Object> row : rows) {
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(table).append(" (")
              .append(colList).append(") VALUES (");
            boolean first = true;
            for (String col : columns) {
                if (!first) sb.append(", ");
                sb.append(sqlLiteral(row.get(col)));
                first = false;
            }
            sb.append(");\n");
            writer.write(sb.toString());
        }
        writer.write("\n");
    }

    private String sqlLiteral(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Boolean b) return b ? "TRUE" : "FALSE";
        if (value instanceof Number n) return n.toString();
        if (value instanceof UUID u) return "'" + u + "'";
        if (value instanceof Timestamp ts) return "'" + ts.toInstant() + "'";
        if (value instanceof java.sql.Date d) return "'" + d + "'";
        if (value instanceof Instant i) return "'" + i + "'";
        if (value instanceof java.time.OffsetDateTime odt) return "'" + odt.toInstant() + "'";
        if (value instanceof java.time.LocalDateTime ldt) return "'" + ldt + "'";
        // Fallback: escape single quotes.
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private UUID currentAdminId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal p) {
            return p.userId();
        }
        return null;
    }
}
