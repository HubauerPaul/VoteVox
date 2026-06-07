package at.htlwels.votevox.voting;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.common.error.BadRequestException;
import at.htlwels.votevox.common.error.ElectionNotRunningException;
import at.htlwels.votevox.common.error.NotFoundException;
import at.htlwels.votevox.common.error.TokenUsedException;
import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionCandidate;
import at.htlwels.votevox.election.ElectionCandidateRepository;
import at.htlwels.votevox.qrcode.HashUtil;
import at.htlwels.votevox.qrcode.StudentToken;
import at.htlwels.votevox.qrcode.StudentTokenRepository;
import at.htlwels.votevox.qrcode.Token;
import at.htlwels.votevox.qrcode.TokenRepository;
import at.htlwels.votevox.voting.dto.BallotCandidate;
import at.htlwels.votevox.voting.dto.CastVoteRequest;
import at.htlwels.votevox.voting.dto.CastVoteResponse;
import at.htlwels.votevox.voting.dto.ValidateTokenRequest;
import at.htlwels.votevox.voting.dto.ValidateTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements the two public voting endpoints.
 * <p>
 * <strong>Vote casting</strong> runs inside one SERIALIZABLE transaction that
 * also takes a pessimistic write lock on the token row. Together these guarantee
 * that even under concurrent attacks the same token can be redeemed exactly
 * once. The Vote row is inserted without any reference to the voter or the
 * token, preserving anonymity even against database snapshots.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VotingService {

    private final TokenRepository tokenRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final ElectionCandidateRepository electionCandidateRepository;
    private final VoteRepository voteRepository;
    private final AuditService auditService;

    // ---------------------------------------------------------------- Validate

    @Transactional(readOnly = true)
    public ValidateTokenResponse validate(ValidateTokenRequest request) {
        String hash = HashUtil.sha256Hex(request.token().trim());

        StudentToken st = studentTokenRepository.findByTokenTokenValueHash(hash)
                .orElseThrow(() -> new NotFoundException("Invalid token"));

        Token token = st.getToken();
        if (token.isUsed()) {
            throw new TokenUsedException("This token has already been used");
        }

        Election election = st.getElection();
        if (!election.isActiveNow()) {
            throw new ElectionNotRunningException(
                    "Election is not currently running (status: " + election.getStatus() + ")");
        }

        List<BallotCandidate> candidates = electionCandidateRepository
                .findAllByElectionId(election.getId()).stream()
                .map(ec -> new BallotCandidate(
                        ec.getId(),
                        ec.getCandidate().getName(),
                        ec.getCandidate().getClassName(),
                        ec.getCandidate().getDepartment()))
                .toList();

        // Audit: do NOT include token info. Student id is acceptable here
        // because this event records ballot delivery, not the actual vote.
        auditService.logStudent(st.getStudent().getId(), "TOKEN_VALIDATED",
                "Election: " + election.getTitle());

        return new ValidateTokenResponse(
                election.getId(),
                election.getTitle(),
                election.getType(),
                candidates
        );
    }

    // ---------------------------------------------------------------- Cast

    /**
     * Atomically validates the token, marks it used, and persists an
     * anonymous Vote. SERIALIZABLE isolation plus a pessimistic write lock on
     * the Token row protect against double-submission and lost updates.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CastVoteResponse cast(CastVoteRequest request) {
        String hash = HashUtil.sha256Hex(request.token().trim());

        // Pessimistic lock on the token row.
        Token lockedToken = tokenRepository.findByTokenValueHashForUpdate(hash)
                .orElseThrow(() -> new NotFoundException("Invalid token"));

        if (lockedToken.isUsed()) {
            throw new TokenUsedException("This token has already been used");
        }

        StudentToken st = studentTokenRepository.findByTokenTokenValueHash(hash)
                .orElseThrow(() -> new NotFoundException("Invalid token"));

        Election election = st.getElection();
        if (!election.isActiveNow()) {
            throw new ElectionNotRunningException(
                    "Election is not currently running (status: " + election.getStatus() + ")");
        }

        ElectionCandidate selection = electionCandidateRepository
                .findByIdAndElectionId(request.candidateId(), election.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Selected candidate does not belong to this election"));

        // Mark token spent.
        lockedToken.setUsed(true);
        tokenRepository.save(lockedToken);

        // Insert anonymous ballot. NO student reference, NO token reference.
        Vote vote = Vote.builder()
                .electionId(election.getId())
                .electionCandidateId(selection.getId())
                .build();
        voteRepository.save(vote);

        // Audit: only the election title. No student id, no token, no candidate.
        auditService.log(AuditService.USER_TYPE_STUDENT, null, "VOTE_CAST",
                "Election: " + election.getTitle());

        log.info("Vote cast in election '{}' (anonymous)", election.getTitle());
        return new CastVoteResponse(true);
    }
}
