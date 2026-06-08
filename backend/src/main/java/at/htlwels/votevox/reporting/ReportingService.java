package at.htlwels.votevox.reporting;

import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionCandidate;
import at.htlwels.votevox.election.ElectionCandidateRepository;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.qrcode.ElectionToken;
import at.htlwels.votevox.qrcode.ElectionTokenRepository;
import at.htlwels.votevox.reporting.dto.BreakdownEntry;
import at.htlwels.votevox.reporting.dto.CandidateResult;
import at.htlwels.votevox.reporting.dto.ResultsResponse;
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
    private final ElectionTokenRepository electionTokenRepository;
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

        List<BreakdownEntry> byClass = computeParticipationByClass(electionId);
        // Department is no longer tracked on the voter side (classes carry no
        // department), so this breakdown is intentionally empty.
        List<BreakdownEntry> byDepartment = List.of();

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
     * Participation breakdown by class: how many anonymous tokens were issued
     * per class. "eligible" and "tokensIssued" are equal by construction (a
     * token is the unit of eligibility here).
     */
    private List<BreakdownEntry> computeParticipationByClass(UUID electionId) {
        List<ElectionToken> tokens = electionTokenRepository.findAllByElectionId(electionId);
        Map<String, Long> counts = tokens.stream()
                .map(ElectionToken::getSchoolClass)
                .map(sc -> sc == null ? "(unassigned)" : sc.getName())
                .collect(Collectors.groupingBy(name -> name, TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> new BreakdownEntry(e.getKey(), e.getValue(), e.getValue()))
                .toList();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
