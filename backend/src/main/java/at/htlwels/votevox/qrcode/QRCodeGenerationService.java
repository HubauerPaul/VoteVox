package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.common.error.BadRequestException;
import at.htlwels.votevox.common.error.NotFoundException;
import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.election.ElectionStatus;
import at.htlwels.votevox.qrcode.dto.GeneratedTokenResponse;
import at.htlwels.votevox.student.Student;
import at.htlwels.votevox.student.StudentRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Generates one-time voting tokens and the corresponding QR-code PNGs.
 *
 * <p>Plaintext = random UUID (v4). Hash = SHA-256 hex of the plaintext.
 * Only the hash is persisted; the plaintext is returned to the caller exactly
 * once and embedded in the printed QR code.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeGenerationService {

    private static final int QR_SIZE_PX = 300;

    @Value("${votevox.qr.base-url}")
    private String qrBaseUrl;

    private final StudentRepository studentRepository;
    private final TokenRepository tokenRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final ElectionManagementService electionService;
    private final AuditService auditService;

    /**
     * Generates a fresh token per enrolled student of the given election.
     * <ul>
     *   <li>If a student already has a token for this election the existing
     *       row is skipped (idempotent re-runs would otherwise expose new
     *       plaintexts for the same ballot).</li>
     *   <li>The election must NOT have started yet.</li>
     * </ul>
     *
     * @return list of generated plaintext tokens. Caller is responsible for
     *         delivering them as QR codes; the server forgets them after
     *         responding.
     */
    @Transactional
    public List<GeneratedTokenResponse> generateForElection(UUID electionId, List<UUID> studentIds) {
        Election election = electionService.findOrThrow(electionId);
        if (election.getStatus() != ElectionStatus.PLANNED) {
            throw new BadRequestException("Tokens can only be generated for PLANNED elections");
        }

        List<Student> students;
        if (studentIds == null || studentIds.isEmpty()) {
            // Generate for every student already attached to the election;
            // if none attached, generate for all known students.
            List<Student> attached = studentTokenRepository.findAllByElectionId(electionId).stream()
                    .map(StudentToken::getStudent)
                    .distinct()
                    .toList();
            students = attached.isEmpty() ? studentRepository.findAll() : attached;
        } else {
            students = studentRepository.findAllById(studentIds);
            if (students.size() != studentIds.size()) {
                throw new NotFoundException("One or more students not found");
            }
        }

        if (students.isEmpty()) {
            throw new BadRequestException("No students to generate tokens for");
        }

        List<GeneratedTokenResponse> generated = new ArrayList<>();
        for (Student student : students) {
            if (studentTokenRepository.existsByStudentIdAndElectionId(student.getId(), electionId)) {
                log.debug("Skipping student {} - already has token for election {}",
                        student.getStudentId(), electionId);
                continue;
            }
            String plaintext = UUID.randomUUID().toString();
            String hash = HashUtil.sha256Hex(plaintext);

            Token token = tokenRepository.save(Token.builder()
                    .tokenValueHash(hash)
                    .used(false)
                    .build());

            studentTokenRepository.save(StudentToken.builder()
                    .student(student)
                    .election(election)
                    .token(token)
                    .build());

            generated.add(new GeneratedTokenResponse(
                    student.getId(),
                    student.getName(),
                    student.getStudentId(),
                    plaintext
            ));
        }

        // Audit: log only the count, NEVER any plaintext or hash.
        auditService.logAdmin(currentAdminId(), "TOKENS_GENERATED",
                "Election: " + election.getTitle() + ", Tokens: " + generated.size());
        log.info("Generated {} tokens for election '{}'", generated.size(), election.getTitle());
        return generated;
    }

    /**
     * Builds a PNG QR-code image for the given plaintext token.
     */
    public byte[] buildQrPng(String plaintextToken) {
        String url = qrBaseUrl + "?token=" + plaintextToken;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX, hints);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
                return baos.toByteArray();
            }
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Failed to render QR code", ex);
        }
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
