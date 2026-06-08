package at.htlwels.votevox.qrcode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElectionTokenRepository extends JpaRepository<ElectionToken, UUID> {

    Optional<ElectionToken> findByTokenTokenValueHash(String tokenValueHash);

    List<ElectionToken> findAllByElectionId(UUID electionId);

    long countByElectionId(UUID electionId);

    boolean existsByElectionId(UUID electionId);
}
