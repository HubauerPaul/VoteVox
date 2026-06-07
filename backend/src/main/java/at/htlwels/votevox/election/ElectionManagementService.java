package at.htlwels.votevox.election;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.common.error.BadRequestException;
import at.htlwels.votevox.common.error.NotFoundException;
import at.htlwels.votevox.election.dto.*;
import at.htlwels.votevox.qrcode.StudentTokenRepository;
import at.htlwels.votevox.voting.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle of elections and their candidate roster.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectionManagementService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionCandidateRepository electionCandidateRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final VoteRepository voteRepository;
    private final AuditService auditService;

    // ---------------------------------------------------------------- Queries

    @Transactional(readOnly = true)
    public List<ElectionResponse> listAll() {
        return electionRepository.findAllByOrderByStartTimeDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ElectionResponse get(UUID id) {
        Election election = findOrThrow(id);
        return toResponse(election);
    }

    // ---------------------------------------------------------------- CRUD

    @Transactional
    public ElectionResponse create(CreateElectionRequest request) {
        validateWindow(request.startTime(), request.endTime());
        Election election = Election.builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ElectionStatus.PLANNED)
                .build();
        Election saved = electionRepository.save(election);
        auditService.logAdmin(currentAdminId(), "ELECTION_CREATED",
                "Election: " + saved.getTitle() + " (" + saved.getType() + ")");
        log.info("Created election '{}' (id={})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ElectionResponse update(UUID id, UpdateElectionRequest request) {
        Election election = findOrThrow(id);
        if (election.getStatus() == ElectionStatus.FINISHED) {
            throw new IllegalStateException("Finished elections cannot be modified");
        }
        if (election.getStatus() == ElectionStatus.RUNNING
                && (!election.getStartTime().equals(request.startTime())
                    || !election.getEndTime().equals(request.endTime())
                    || election.getType() != request.type())) {
            throw new IllegalStateException("Running elections cannot have their type or window modified");
        }
        validateWindow(request.startTime(), request.endTime());

        election.setTitle(request.title());
        election.setDescription(request.description());
        election.setType(request.type());
        election.setStartTime(request.startTime());
        election.setEndTime(request.endTime());
        Election saved = electionRepository.save(election);
        auditService.logAdmin(currentAdminId(), "ELECTION_UPDATED",
                "Election: " + saved.getTitle());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Election election = findOrThrow(id);
        if (election.getStatus() == ElectionStatus.RUNNING) {
            throw new IllegalStateException("Cannot delete a running election");
        }
        electionRepository.delete(election);
        auditService.logAdmin(currentAdminId(), "ELECTION_DELETED",
                "Election: " + election.getTitle());
        log.info("Deleted election '{}' (id={})", election.getTitle(), election.getId());
    }

    // ---------------------------------------------------------------- Status

    @Transactional
    public ElectionResponse start(UUID id) {
        Election election = findOrThrow(id);
        if (election.getStatus() != ElectionStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED elections can be started (current: "
                    + election.getStatus() + ")");
        }
        if (electionCandidateRepository.countByElectionId(id) == 0) {
            throw new BadRequestException("Cannot start an election without candidates");
        }
        election.setStatus(ElectionStatus.RUNNING);
        Election saved = electionRepository.save(election);
        auditService.logAdmin(currentAdminId(), "ELECTION_STARTED",
                "Election: " + saved.getTitle());
        log.info("Started election '{}'", saved.getTitle());
        return toResponse(saved);
    }

    @Transactional
    public ElectionResponse stop(UUID id) {
        Election election = findOrThrow(id);
        if (election.getStatus() != ElectionStatus.RUNNING) {
            throw new IllegalStateException("Only RUNNING elections can be stopped (current: "
                    + election.getStatus() + ")");
        }
        election.setStatus(ElectionStatus.FINISHED);
        Election saved = electionRepository.save(election);
        auditService.logAdmin(currentAdminId(), "ELECTION_STOPPED",
                "Election: " + saved.getTitle());
        log.info("Stopped election '{}'", saved.getTitle());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------- Candidates

    @Transactional(readOnly = true)
    public List<CandidateResponse> listCandidates(UUID electionId) {
        ensureExists(electionId);
        return electionCandidateRepository.findAllByElectionId(electionId).stream()
                .map(ec -> new CandidateResponse(
                        ec.getId(),
                        ec.getCandidate().getId(),
                        ec.getCandidate().getName(),
                        ec.getCandidate().getClassName(),
                        ec.getCandidate().getDepartment()))
                .toList();
    }

    @Transactional
    public CandidateResponse addCandidate(UUID electionId, CandidateRequest request) {
        Election election = findOrThrow(electionId);
        if (election.getStatus() == ElectionStatus.FINISHED) {
            throw new IllegalStateException("Cannot modify candidates of a finished election");
        }
        Candidate candidate = candidateRepository.save(Candidate.builder()
                .name(request.name())
                .className(request.className())
                .department(request.department())
                .build());
        ElectionCandidate ec = electionCandidateRepository.save(ElectionCandidate.builder()
                .election(election)
                .candidate(candidate)
                .build());
        auditService.logAdmin(currentAdminId(), "CANDIDATE_ADDED",
                "Election: " + election.getTitle() + ", Candidate: " + candidate.getName());
        return new CandidateResponse(ec.getId(), candidate.getId(), candidate.getName(),
                candidate.getClassName(), candidate.getDepartment());
    }

    @Transactional
    public void removeCandidate(UUID electionId, UUID electionCandidateId) {
        Election election = findOrThrow(electionId);
        if (election.getStatus() != ElectionStatus.PLANNED) {
            throw new IllegalStateException("Cannot remove candidates after election has started");
        }
        ElectionCandidate ec = electionCandidateRepository
                .findByIdAndElectionId(electionCandidateId, electionId)
                .orElseThrow(() -> new NotFoundException("Election candidate not found"));
        electionCandidateRepository.delete(ec);
        auditService.logAdmin(currentAdminId(), "CANDIDATE_REMOVED",
                "Election: " + election.getTitle() + ", Candidate: " + ec.getCandidate().getName());
    }

    // ---------------------------------------------------------------- Internals

    public Election findOrThrow(UUID id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Election not found: " + id));
    }

    private void ensureExists(UUID id) {
        if (!electionRepository.existsById(id)) {
            throw new NotFoundException("Election not found: " + id);
        }
    }

    private void validateWindow(java.time.Instant start, java.time.Instant end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private ElectionResponse toResponse(Election e) {
        return new ElectionResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getType(),
                e.getStartTime(),
                e.getEndTime(),
                e.getStatus(),
                e.getCreatedAt(),
                electionCandidateRepository.countByElectionId(e.getId()),
                studentTokenRepository.countByElectionId(e.getId()),
                voteRepository.countByElectionId(e.getId())
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
