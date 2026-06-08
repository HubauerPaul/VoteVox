package at.htlwels.votevox.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error payload returned by {@link GlobalExceptionHandler}.
 *
 * @param timestamp ISO-8601 UTC instant of the error
 * @param status    HTTP status code
 * @param error     short error name (e.g. "Not Found")
 * @param message   human-readable message
 * @param path      request path that produced the error
 * @param fieldErrors per-field validation errors, may be {@code null}
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}
}
