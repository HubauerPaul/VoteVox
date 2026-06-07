package at.htlwels.votevox.election;

import at.htlwels.votevox.election.dto.CandidateRequest;
import at.htlwels.votevox.election.dto.CandidateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/elections/{electionId}/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final ElectionManagementService electionService;

    @GetMapping
    public ResponseEntity<List<CandidateResponse>> list(@PathVariable UUID electionId) {
        return ResponseEntity.ok(electionService.listCandidates(electionId));
    }

    @PostMapping
    public ResponseEntity<CandidateResponse> add(@PathVariable UUID electionId,
                                                 @Valid @RequestBody CandidateRequest request) {
        return ResponseEntity.status(201).body(electionService.addCandidate(electionId, request));
    }

    @DeleteMapping("/{electionCandidateId}")
    public ResponseEntity<Void> remove(@PathVariable UUID electionId,
                                       @PathVariable UUID electionCandidateId) {
        electionService.removeCandidate(electionId, electionCandidateId);
        return ResponseEntity.noContent().build();
    }
}
