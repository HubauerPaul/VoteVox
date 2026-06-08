package at.htlwels.votevox.qrcode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentTokenRepository extends JpaRepository<StudentToken, UUID> {

    Optional<StudentToken> findByTokenTokenValueHash(String tokenValueHash);

    List<StudentToken> findAllByElectionId(UUID electionId);

    long countByElectionId(UUID electionId);

    boolean existsByStudentIdAndElectionId(UUID studentId, UUID electionId);
}
