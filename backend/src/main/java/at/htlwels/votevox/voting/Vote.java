package at.htlwels.votevox.voting;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The anonymous ballot. A Vote stores ONLY the election it belongs to and the
 * chosen {@code ElectionCandidate} - never a reference to the voter or the
 * token. Voter anonymity is enforced both in code AND at the database schema
 * level (no FK column to students or tokens exists).
 */
@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Column(name = "election_candidate_id", nullable = false)
    private UUID electionCandidateId;

    @Column(name = "cast_at", nullable = false, updatable = false)
    private Instant castAt;

    @PrePersist
    void onCreate() {
        if (castAt == null) castAt = Instant.now();
    }
}
