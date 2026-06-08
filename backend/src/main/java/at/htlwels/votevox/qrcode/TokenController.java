package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.qrcode.dto.GeneratedTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/elections/{electionId}/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final QRCodeGenerationService qrCodeService;

    /**
     * Mints one anonymous token per student of every selected class and returns
     * the plaintexts so the admin UI can build the QR PDF in the browser. The
     * server does not retain the plaintexts (only their SHA-256 hashes).
     */
    @PostMapping("/generate")
    public ResponseEntity<List<GeneratedTokenResponse>> generate(@PathVariable UUID electionId) {
        return ResponseEntity.ok(qrCodeService.generateForElection(electionId));
    }

    /**
     * Removes all tokens for the election so a fresh set can be issued. Only
     * allowed while the election is PLANNED. Used by the admin UI when the
     * one-time QR PDF was lost before the election started.
     */
    @DeleteMapping
    public ResponseEntity<Void> reset(@PathVariable UUID electionId) {
        qrCodeService.resetTokens(electionId);
        return ResponseEntity.noContent().build();
    }
}
