package at.htlwels.votevox.common.error;

/**
 * Thrown when client input is semantically invalid.
 * Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
