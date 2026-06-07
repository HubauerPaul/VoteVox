package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.student.Student;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Ledger entry linking a {@link Student} to a {@link Token} for a specific
 * {@link Election}. Used only during ballot delivery; the resulting Vote
 * holds no reference back to this row.
 */
@Entity
@Table(
    name = "student_tokens",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_election",
        columnNames = {"student_id", "election_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentToken {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_id", nullable = false, unique = true)
    private Token token;
}
