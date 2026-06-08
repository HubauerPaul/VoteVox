package at.htlwels.votevox.schoolclass;

import at.htlwels.votevox.election.Election;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Join row marking that a {@link SchoolClass} participates in an
 * {@link Election}. The set of selected classes determines how many anonymous
 * tokens get minted for the election.
 */
@Entity
@Table(
    name = "election_classes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_election_class",
        columnNames = {"election_id", "class_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionClass {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
