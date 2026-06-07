package at.htlwels.votevox.election;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Join entity linking a {@link Candidate} to an {@link Election}.
 * The vote's foreign key points here (not directly at the Candidate) so that
 * a candidate may appear in multiple elections with distinct vote counts.
 */
@Entity
@Table(
    name = "election_candidates",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_election_candidate",
        columnNames = {"election_id", "candidate_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionCandidate {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
}
