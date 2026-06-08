package at.htlwels.votevox.schoolclass;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.common.error.BadRequestException;
import at.htlwels.votevox.common.error.NotFoundException;
import at.htlwels.votevox.election.Election;
import at.htlwels.votevox.election.ElectionManagementService;
import at.htlwels.votevox.election.ElectionStatus;
import at.htlwels.votevox.schoolclass.dto.ClassRequest;
import at.htlwels.votevox.schoolclass.dto.ClassResponse;
import at.htlwels.votevox.schoolclass.dto.ElectionClassOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the global list of classes and which classes participate in a given
 * election. Classes carry only a name and a student count - no personal data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final ElectionClassRepository electionClassRepository;
    private final ElectionManagementService electionService;
    private final AuditService auditService;

    // ---------------------------------------------------------------- Global CRUD

    @Transactional(readOnly = true)
    public List<ClassResponse> listAll() {
        return classRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClassResponse create(ClassRequest request) {
        String name = request.name().trim();
        if (classRepository.existsByName(name)) {
            throw new BadRequestException("A class named '" + name + "' already exists");
        }
        SchoolClass saved = classRepository.save(SchoolClass.builder()
                .name(name)
                .studentCount(request.studentCount())
                .build());
        auditService.logAdmin(currentAdminId(), "CLASS_CREATED",
                "Class: " + saved.getName() + " (" + saved.getStudentCount() + ")");
        return toResponse(saved);
    }

    @Transactional
    public ClassResponse update(UUID id, ClassRequest request) {
        SchoolClass cls = findOrThrow(id);
        String name = request.name().trim();
        classRepository.findByName(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("A class named '" + name + "' already exists");
                });
        cls.setName(name);
        cls.setStudentCount(request.studentCount());
        classRepository.save(cls);
        auditService.logAdmin(currentAdminId(), "CLASS_UPDATED", "Class: " + cls.getName());
        return toResponse(cls);
    }

    @Transactional
    public void delete(UUID id) {
        SchoolClass cls = findOrThrow(id);
        // FKs handle the rest: election_classes ON DELETE CASCADE,
        // election_tokens.class_id ON DELETE SET NULL.
        classRepository.delete(cls);
        auditService.logAdmin(currentAdminId(), "CLASS_DELETED", "Class: " + cls.getName());
    }

    // ---------------------------------------------------------------- Per election

    @Transactional(readOnly = true)
    public List<ElectionClassOption> listForElection(UUID electionId) {
        electionService.findOrThrow(electionId);
        Set<UUID> selected = electionClassRepository.findAllByElectionId(electionId).stream()
                .map(ec -> ec.getSchoolClass().getId())
                .collect(HashSet::new, Set::add, Set::addAll);
        return classRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new ElectionClassOption(
                        c.getId(), c.getName(), c.getStudentCount(), selected.contains(c.getId())))
                .toList();
    }

    @Transactional
    public List<ElectionClassOption> setForElection(UUID electionId, List<UUID> classIds) {
        Election election = electionService.findOrThrow(electionId);
        if (election.getStatus() != ElectionStatus.PLANNED) {
            throw new BadRequestException("Classes can only be changed while the election is PLANNED");
        }
        List<UUID> ids = classIds == null ? List.of() : classIds.stream().distinct().toList();

        // Validate every requested class exists before mutating anything.
        for (UUID classId : ids) {
            if (!classRepository.existsById(classId)) {
                throw new NotFoundException("Class not found: " + classId);
            }
        }

        electionClassRepository.deleteByElectionId(electionId);
        electionClassRepository.flush();
        for (UUID classId : ids) {
            SchoolClass cls = classRepository.getReferenceById(classId);
            electionClassRepository.save(ElectionClass.builder()
                    .election(election)
                    .schoolClass(cls)
                    .build());
        }
        auditService.logAdmin(currentAdminId(), "ELECTION_CLASSES_SET",
                "Election: " + election.getTitle() + ", Classes: " + ids.size());
        log.info("Set {} classes for election '{}'", ids.size(), election.getTitle());
        return listForElection(electionId);
    }

    // ---------------------------------------------------------------- Internals

    private SchoolClass findOrThrow(UUID id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class not found: " + id));
    }

    private ClassResponse toResponse(SchoolClass c) {
        return new ClassResponse(c.getId(), c.getName(), c.getStudentCount(), c.getCreatedAt());
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
