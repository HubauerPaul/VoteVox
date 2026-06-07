package at.htlwels.votevox.student;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A student enrolled at the school. The {@code studentId} is the school's
 * external identifier (e.g. matriculation number).
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "student_id", nullable = false, unique = true, length = 64)
    private String studentId;

    @Column(name = "class_name", nullable = false, length = 32)
    private String className;

    @Column(name = "department", nullable = false, length = 64)
    private String department;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
