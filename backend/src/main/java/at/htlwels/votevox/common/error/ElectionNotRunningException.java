package at.htlwels.votevox.common.error;

/**
 * Thrown when a vote is attempted on an election that is not in the RUNNING
 * status or outside its time window. Mapped to HTTP 409 Conflict.
 */
public class ElectionNotRunningException extends RuntimeException {
    public ElectionNotRunningException(String message) {
        super(message);
    }
}
