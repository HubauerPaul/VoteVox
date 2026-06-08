package at.htlwels.votevox.election;

import at.htlwels.votevox.election.dto.CreateElectionRequest;
import at.htlwels.votevox.election.dto.ElectionResponse;
import at.htlwels.votevox.election.dto.UpdateElectionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/elections")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionManagementService electionService;

    @GetMapping
    public ResponseEntity<List<ElectionResponse>> list() {
        return ResponseEntity.ok(electionService.listAll());
    }

    @PostMapping
    public ResponseEntity<ElectionResponse> create(@Valid @RequestBody CreateElectionRequest request) {
        ElectionResponse created = electionService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ElectionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(electionService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ElectionResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateElectionRequest request) {
        return ResponseEntity.ok(electionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        electionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ElectionResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(electionService.start(id));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<ElectionResponse> stop(@PathVariable UUID id) {
        return ResponseEntity.ok(electionService.stop(id));
    }
}
