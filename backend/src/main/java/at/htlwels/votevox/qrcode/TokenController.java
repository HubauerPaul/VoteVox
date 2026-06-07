package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.common.error.BadRequestException;
import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.qrcode.dto.GeneratedTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/elections/{electionId}/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final QRCodeGenerationService qrCodeService;
    private final TokenPdfGenerator tokenPdfGenerator;
    private final ElectionManagementService electionService;
    private final AuditService auditService;

    /**
     * Generates one-time tokens for every enrolled student (or all known
     * students if the election has no roster yet). Returns the plaintexts so
     * the client can print/distribute them; the server does not retain them.
     */
    @PostMapping("/generate")
    public ResponseEntity<List<GeneratedTokenResponse>> generate(@PathVariable UUID electionId) {
        List<GeneratedTokenResponse> generated = qrCodeService.generateForElection(electionId, null);
        return ResponseEntity.ok(generated);
    }

    /**
     * Generates tokens AND immediately returns the PDF containing one page
     * per student. This is the convenience endpoint for the admin UI's
     * "Print ballots" button. The server holds the plaintexts only for the
     * duration of this single request.
     * <p>
     * If all enrolled students already have a token, no new ones are issued
     * and the call fails with 400 - reprinting old tokens is not possible
     * because their plaintexts are not stored.
     * </p>
     */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID electionId) throws IOException {
        Election election = electionService.findOrThrow(electionId);
        List<GeneratedTokenResponse> tokens = qrCodeService.generateForElection(electionId, null);
        if (tokens.isEmpty()) {
            throw new BadRequestException(
                    "No new tokens were generated. Either no students are enrolled, or "
                  + "all enrolled students already received tokens (plaintexts cannot be "
                  + "recovered - issue tokens to new students, or remove and re-enroll).");
        }

        byte[] pdf = tokenPdfGenerator.renderPdf(election.getTitle(), tokens);
        auditService.logAdmin(currentAdminId(), "TOKENS_PDF_EXPORTED",
                "Election: " + election.getTitle() + ", Pages: " + tokens.size());

        String filename = "votevox-tokens-" + slug(election.getTitle()) + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, 200);
    }

    private String slug(String s) {
        return s == null ? "election"
                : s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
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
