package at.htlwels.votevox.common.error;

/**
 * Thrown when a token has already been used (one-time-use violation).
 * Mapped to HTTP 410 Gone.
 */
public class TokenUsedException extends RuntimeException {
    public TokenUsedException(String message) {
        super(message);
    }
}
