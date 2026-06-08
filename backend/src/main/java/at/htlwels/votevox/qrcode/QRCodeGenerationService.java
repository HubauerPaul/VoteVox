package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.common.error.BadRequestException;
import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.election.ElectionStatus;
import at.htlwels.votevox.qrcode.dto.GeneratedTokenResponse;
import at.htlwels.votevox.schoolclass.ElectionClass;
import at.htlwels.votevox.schoolclass.ElectionClassRepository;
import at.htlwels.votevox.schoolclass.SchoolClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Mints one-time voting tokens for the classes participating in an election.
 *
 * <p>Plaintext = random UUID (v4). Hash = SHA-256 hex of the plaintext. Only
 * the hash is persisted; the plaintext is returned to the caller exactly once
 * and embedded in the printed QR code. Tokens carry no personal data - they are
 * grouped only by class, for distribution and participation counts.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeGenerationService {

    private final ElectionClassRepository electionClassRepository;
    private final TokenRepository tokenRepository;
    private final ElectionTokenRepository electionTokenRepository;
    private final ElectionManagementService electionService;
    private final AuditService auditService;

    /**
     * Generates {@code studentCount} anonymous tokens for every class selected
     * for the election.
     * <ul>
     *   <li>The election must still be PLANNED.</li>
     *   <li>If tokens already exist for the election, nothing is minted and an
     *       empty list is returned (the caller offers a reset instead - the old
     *       plaintexts cannot be recovered).</li>
     * </ul>
     *
     * @return the freshly minted plaintext tokens, grouped by class. The server
     *         forgets the plaintexts after responding.
     */
    @Transactional
    public List<GeneratedTokenResponse> generateForElection(UUID electionId) {
        Election election = electionService.findOrThrow(electionId);
        if (election.getStatus() != ElectionStatus.PLANNED) {
            throw new BadRequestException("Tokens can only be generated for PLANNED elections");
        }
        if (electionTokenRepository.existsByElectionId(electionId)) {
            // Idempotent: tokens already issued. Caller must reset to re-issue.
            return List.of();
        }

        List<ElectionClass> classes = electionClassRepository.findAllByElectionId(electionId).stream()
                .sorted(Comparator.comparing(ec -> ec.getSchoolClass().getName()))
                .toList();
        if (classes.isEmpty()) {
            throw new BadRequestException("No classes selected for this election");
        }

        List<GeneratedTokenResponse> generated = new ArrayList<>();
        for (ElectionClass ec : classes) {
            SchoolClass cls = ec.getSchoolClass();
            for (int i = 0; i < cls.getStudentCount(); i++) {
                String plaintext = UUID.randomUUID().toString();
                String hash = HashUtil.sha256Hex(plaintext);

                Token token = tokenRepository.save(Token.builder()
                        .tokenValueHash(hash)
                        .used(false)
                        .build());

                electionTokenRepository.save(ElectionToken.builder()
                        .token(token)
                        .election(election)
                        .schoolClass(cls)
                        .build());

                generated.add(new GeneratedTokenResponse(cls.getName(), plaintext));
            }
        }

        // Audit: log only the count, NEVER any plaintext or hash.
        auditService.logAdmin(currentAdminId(), "TOKENS_GENERATED",
                "Election: " + election.getTitle() + ", Tokens: " + generated.size());
        log.info("Generated {} tokens for election '{}'", generated.size(), election.getTitle());
        return generated;
    }

    /**
     * Deletes every token (and its issuance ledger row) for the given election
     * so a fresh set can be generated. Only permitted while the election is
     * still PLANNED - once voting has started, tokens are immutable.
     *
     * @return the number of tokens that were removed
     */
    @Transactional
    public int resetTokens(UUID electionId) {
        Election election = electionService.findOrThrow(electionId);
        if (election.getStatus() != ElectionStatus.PLANNED) {
            throw new BadRequestException("Tokens can only be reset for PLANNED elections");
        }

        List<ElectionToken> issued = electionTokenRepository.findAllByElectionId(electionId);
        List<UUID> tokenIds = issued.stream()
                .map(et -> et.getToken().getId())
                .toList();

        electionTokenRepository.deleteAll(issued);
        electionTokenRepository.flush();
        tokenRepository.deleteAllById(tokenIds);

        auditService.logAdmin(currentAdminId(), "TOKENS_RESET",
                "Election: " + election.getTitle() + ", Removed: " + tokenIds.size());
        log.info("Reset {} tokens for election '{}'", tokenIds.size(), election.getTitle());
        return tokenIds.size();
    }

    private UUID currentAdminId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal p) {
            return p.userId();
        }
        return null;
    }
}
