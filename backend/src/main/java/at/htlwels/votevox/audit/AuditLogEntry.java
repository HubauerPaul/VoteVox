package at.htlwels.votevox.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit log entry. Used for both administrative actions and
 * anonymous voter actions (TOKEN_VALIDATED, VOTE_CAST).
 * <p>
 * <strong>Security:</strong> {@code details} must NEVER contain token
 * plaintext, token hashes, or any data that could correlate a voter to
 * their ballot.
 * </p>
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntry {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /** "ADMIN", "TEACHER", "STUDENT", "SYSTEM". */
    @Column(name = "user_type", nullable = false, length = 32)
    private String userType;

    /** UUID of the acting user; nullable for SYSTEM actions. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void onCreate() {
        if (timestamp == null) timestamp = Instant.now();
    }
}
