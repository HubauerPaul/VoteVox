package at.htlwels.votevox.qrcode.dto;

import java.util.UUID;

/**
 * Single response carrying token plaintext.
 * <p>
 * <strong>Only</strong> returned by the token-generation endpoint, never again.
 * The admin UI is expected to immediately print the QR PDF; the plaintext
 * is not stored anywhere else on the server.
 * </p>
 */
public record GeneratedTokenResponse(
        UUID studentId,
        String studentName,
        String studentExternalId,
        String tokenPlaintext
) {}
