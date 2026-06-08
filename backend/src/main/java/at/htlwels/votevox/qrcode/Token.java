package at.htlwels.votevox.qrcode;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent representation of a single-use voting token.
 * <p>
 * <strong>Only the SHA-256 hex digest of the plaintext is stored.</strong>
 * The plaintext token exists in exactly one place: inside the QR code printed
 * for the student. Once used, {@code isUsed} is flipped to {@code true}
 * inside the same transaction that inserts the Vote row.
 * </p>
 */
@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** SHA-256 hex digest of the plaintext (64 lowercase hex chars). */
    @Column(name = "token_value_hash", nullable = false, unique = true, length = 64)
    private String tokenValueHash;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "is_used", nullable = false)
    private boolean used;

    @PrePersist
    void onCreate() {
        if (generatedAt == null) generatedAt = Instant.now();
    }
}
