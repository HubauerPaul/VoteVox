package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.schoolclass.SchoolClass;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Issuance ledger entry linking an anonymous {@link Token} to an
 * {@link Election} and (for sheet grouping / participation counts) a
 * {@link SchoolClass}. Replaces the old student-bound StudentToken: there is
 * deliberately NO student reference, so a token can never be traced to a
 * person. The resulting Vote holds no reference back to this row either.
 */
@Entity
@Table(name = "election_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionToken {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_id", nullable = false, unique = true)
    private Token token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;
}
