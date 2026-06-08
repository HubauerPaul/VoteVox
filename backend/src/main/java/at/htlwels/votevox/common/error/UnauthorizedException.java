package at.htlwels.votevox.common.error;

/**
 * Thrown when authentication credentials are missing, invalid or expired.
 * Mapped to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
