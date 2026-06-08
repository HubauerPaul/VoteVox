package at.htlwels.votevox.qrcode.dto;

/**
 * Single generated token: the class it belongs to and the one-time plaintext.
 * <p>
 * <strong>Only</strong> returned by the token-generation endpoint, never again.
 * The admin UI builds the QR PDF from these in the browser; the plaintext is
 * not stored anywhere on the server (only its SHA-256 hash). There is no
 * student reference - tokens are anonymous by construction.
 * </p>
 */
public record GeneratedTokenResponse(
        String className,
        String tokenPlaintext
) {}
