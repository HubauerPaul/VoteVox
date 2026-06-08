package at.htlwels.votevox.schoolclass;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A school class - a named group with a fixed number of students. Global and
 * reusable across elections; an election picks which classes participate via
 * {@link ElectionClass}. Carries no personal data, so tokens minted per class
 * stay fully anonymous.
 */
@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClass {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "student_count", nullable = false)
    private int studentCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
