package at.htlwels.votevox.reporting;

import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionCandidate;
import at.htlwels.votevox.election.ElectionCandidateRepository;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.qrcode.StudentToken;
import at.htlwels.votevox.qrcode.StudentTokenRepository;
import at.htlwels.votevox.reporting.dto.BreakdownEntry;
import at.htlwels.votevox.reporting.dto.CandidateResult;
import at.htlwels.votevox.reporting.dto.ResultsResponse;
import at.htlwels.votevox.student.Student;
import at.htlwels.votevox.voting.VoteRepository;
import at.htlwels.votevox.voting.VoteRepository.VoteTally;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates anonymous votes into result statistics. Because the Vote table
 * has no link to students, the byClass/byDepartment breakdowns are
 * participation breakdowns (how many tokens were issued per group) - never
 * vote breakdowns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final ElectionManagementService electionService;
    private final ElectionCandidateRepository electionCandidateRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final VoteRepository voteRepository;

    @Transactional(readOnly = true)
    public ResultsResponse getResults(UUID electionId) {
        Election election = electionService.findOrThrow(electionId);

        List<ElectionCandidate> ecList = electionCandidateRepository.findAllByElectionId(electionId);
        Map<UUID, Long> tallyByEc = voteRepository.tallyByElection(electionId).stream()
                .collect(Collectors.toMap(VoteTally::getElectionCandidateId, VoteTally::getVotes));

        long totalVotes = tallyByEc.values().stream().mapToLong(Long::longValue).sum();

        List<CandidateResult> candidateResults = ecList.stream()
                .map(ec -> {
                    long votes = tallyByEc.getOrDefault(ec.getId(), 0L);
                    double pct = totalVotes == 0 ? 0.0 : (votes * 100.0) / totalVotes;
                    return new CandidateResult(
                            ec.getCandidate().getId(),
                            ec.getId(),
                            ec.getCandidate().getName(),
                            ec.getCandidate().getClassName(),
                            ec.getCandidate().getDepartment(),
                            votes,
                            round2(pct)
                    );
                })
                .sorted(Comparator.comparingLong(CandidateResult::votes).reversed()
                        .thenComparing(CandidateResult::name))
                .toList();

        List<BreakdownEntry> byClass = computeParticipationBreakdown(electionId, true);
        List<BreakdownEntry> byDepartment = computeParticipationBreakdown(electionId, false);

        return new ResultsResponse(
                election.getId(),
                election.getTitle(),
                election.getType(),
                election.getStatus(),
                Instant.now(),
                candidateResults,
                totalVotes,
                byClass,
                byDepartment
        );
    }

    /**
     * Builds a participation breakdown. The "eligible" count and the
     * "tokensIssued" count match by construction (a student is enrolled in an
     * election by receiving a token), so both columns are equal here. The
     * structure keeps both for future-proofing if eligibility lists ever
     * become independent of token issuance.
     */
    private List<BreakdownEntry> computeParticipationBreakdown(UUID electionId, boolean byClass) {
        List<StudentToken> tokens = studentTokenRepository.findAllByElectionId(electionId);
        Map<String, Long> counts = tokens.stream()
                .map(StudentToken::getStudent)
                .collect(Collectors.groupingBy(
                        byClass ? Student::getClassName : Student::getDepartment,
                        TreeMap::new,
                        Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> new BreakdownEntry(e.getKey(), e.getValue(), e.getValue()))
                .toList();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
