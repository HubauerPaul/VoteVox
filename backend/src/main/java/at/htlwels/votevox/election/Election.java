package at.htlwels.votevox.election;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An election or survey. Status transitions PLANNED → RUNNING → FINISHED.
 */
@Entity
@Table(name = "elections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Election {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ElectionType type;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ElectionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ElectionStatus.PLANNED;
    }

    /**
     * @return true iff the election is in RUNNING status and the current
     * instant lies within {@code [startTime, endTime]}.
     */
    public boolean isActiveNow() {
        Instant now = Instant.now();
        return status == ElectionStatus.RUNNING
                && !now.isBefore(startTime)
                && !now.isAfter(endTime);
    }
}
