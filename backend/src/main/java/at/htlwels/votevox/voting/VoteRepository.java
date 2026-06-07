package at.htlwels.votevox.voting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    long countByElectionId(UUID electionId);

    long countByElectionCandidateId(UUID electionCandidateId);

    @Query("""
            SELECT v.electionCandidateId AS electionCandidateId, COUNT(v) AS votes
            FROM Vote v
            WHERE v.electionId = :electionId
            GROUP BY v.electionCandidateId
            """)
    List<VoteTally> tallyByElection(UUID electionId);

    /** Projection for grouped vote counts. */
    interface VoteTally {
        UUID getElectionCandidateId();
        long getVotes();
    }
}
