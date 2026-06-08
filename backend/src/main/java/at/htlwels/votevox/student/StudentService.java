package at.htlwels.votevox.student;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.common.error.NotFoundException;
import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.qrcode.StudentToken;
import at.htlwels.votevox.qrcode.StudentTokenRepository;
import at.htlwels.votevox.student.dto.ImportStudentsRequest;
import at.htlwels.votevox.student.dto.StudentRequest;
import at.htlwels.votevox.student.dto.StudentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CRUD for students plus the bulk enrollment endpoint that attaches students
 * as eligible voters to a given election. Eligibility is tracked indirectly:
 * a student becomes eligible by being given a token (StudentToken).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final ElectionManagementService electionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<StudentResponse> listAll() {
        return studentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        Student student = upsert(request);
        return toResponse(student);
    }

    @Transactional
    public StudentResponse update(UUID id, StudentRequest request) {
        Student student = findOrThrow(id);
        student.setName(request.name());
        student.setStudentId(request.studentId());
        student.setClassName(request.className());
        student.setDepartment(request.department());
        studentRepository.save(student);
        auditService.logAdmin(currentAdminId(), "STUDENT_UPDATED",
                "Student: " + student.getStudentId());
        return toResponse(student);
    }

    @Transactional
    public void delete(UUID id) {
        Student student = findOrThrow(id);
        studentRepository.delete(student);
        auditService.logAdmin(currentAdminId(), "STUDENT_DELETED",
                "Student: " + student.getStudentId());
    }

    /**
     * Lists students that have been enrolled in the given election (i.e. have
     * a StudentToken row for it). Used by the admin UI to display the roster.
     */
    @Transactional(readOnly = true)
    public List<StudentResponse> listEligibleForElection(UUID electionId) {
        electionService.findOrThrow(electionId);
        return studentTokenRepository.findAllByElectionId(electionId).stream()
                .map(StudentToken::getStudent)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Bulk-add students to an election. Existing students (matched by
     * {@code studentId}) are reused; new ones are created. Students already
     * enrolled in this election are skipped silently.
     * <p>
     * Note: this method only creates {@link Student} rows and does NOT issue
     * tokens. Token generation is a separate step (see QRCodeGenerationService).
     * </p>
     */
    @Transactional
    public List<StudentResponse> bulkAddToElection(UUID electionId, ImportStudentsRequest request) {
        Election election = electionService.findOrThrow(electionId);
        List<StudentResponse> result = new ArrayList<>();
        for (StudentRequest s : request.students()) {
            Student student = upsert(s);
            result.add(toResponse(student));
        }
        auditService.logAdmin(currentAdminId(), "STUDENTS_IMPORTED",
                "Election: " + election.getTitle() + ", Count: " + request.students().size());
        log.info("Imported {} students for election '{}'", result.size(), election.getTitle());
        return result;
    }

    // ---------------------------------------------------------------- Internals

    private Student upsert(StudentRequest request) {
        Student student = studentRepository.findByStudentId(request.studentId())
                .map(existing -> {
                    existing.setName(request.name());
                    existing.setClassName(request.className());
                    existing.setDepartment(request.department());
                    return existing;
                })
                .orElseGet(() -> Student.builder()
                        .name(request.name())
                        .studentId(request.studentId())
                        .className(request.className())
                        .department(request.department())
                        .build());
        return studentRepository.save(student);
    }

    private Student findOrThrow(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student not found: " + id));
    }

    private StudentResponse toResponse(Student s) {
        return new StudentResponse(
                s.getId(), s.getName(), s.getStudentId(),
                s.getClassName(), s.getDepartment(), s.getCreatedAt()
        );
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
