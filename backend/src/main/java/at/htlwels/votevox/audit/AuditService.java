package at.htlwels.votevox.audit;

import at.htlwels.votevox.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Write/read facade for the audit log.
 * <p>
 * Writes always run with {@link Propagation#REQUIRES_NEW} so an audit record
 * is persisted independently of any business transaction that may roll back.
 * </p>
 * <p>
 * Privacy invariant: callers MUST NOT pass token plaintext, token hashes,
 * or any value that links a student to a vote into {@code details}.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_TEACHER = "TEACHER";
    public static final String USER_TYPE_STUDENT = "STUDENT";
    public static final String USER_TYPE_SYSTEM = "SYSTEM";

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String userType, UUID userId, String actionType, String details) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .userType(userType)
                .userId(userId)
                .actionType(actionType)
                .details(details)
                .build();
        auditLogRepository.save(entry);
        log.debug("Audit: [{}] {} by {} ({})", actionType, details, userType, userId);
    }

    public void logAdmin(UUID adminId, String actionType, String details) {
        log(USER_TYPE_ADMIN, adminId, actionType, details);
    }

    /**
     * Logs a student-initiated action. {@code studentId} MUST NOT be passed
     * for VOTE_CAST events to preserve anonymity; pass {@code null} instead.
     */
    public void logStudent(UUID studentId, String actionType, String details) {
        log(USER_TYPE_STUDENT, studentId, actionType, details);
    }

    public void logSystem(String actionType, String details) {
        log(USER_TYPE_SYSTEM, null, actionType, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> page(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(e -> new AuditLogResponse(
                        e.getId(),
                        e.getTimestamp(),
                        e.getUserType(),
                        e.getUserId(),
                        e.getActionType(),
                        e.getDetails()
                ));
    }
}
