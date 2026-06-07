package at.htlwels.votevox.election;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElectionCandidateRepository extends JpaRepository<ElectionCandidate, UUID> {

    List<ElectionCandidate> findAllByElectionId(UUID electionId);

    Optional<ElectionCandidate> findByIdAndElectionId(UUID id, UUID electionId);

    long countByElectionId(UUID electionId);
}
