package at.htlwels.votevox.schoolclass;

import at.htlwels.votevox.schoolclass.dto.ElectionClassOption;
import at.htlwels.votevox.schoolclass.dto.SetElectionClassesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/elections/{electionId}/classes")
@RequiredArgsConstructor
public class ElectionClassController {

    private final ClassService classService;

    /** All classes plus whether each is selected for this election. */
    @GetMapping
    public ResponseEntity<List<ElectionClassOption>> list(@PathVariable UUID electionId) {
        return ResponseEntity.ok(classService.listForElection(electionId));
    }

    /** Replaces the set of classes participating in this election (PLANNED only). */
    @PutMapping
    public ResponseEntity<List<ElectionClassOption>> set(@PathVariable UUID electionId,
                                                         @RequestBody SetElectionClassesRequest request) {
        return ResponseEntity.ok(classService.setForElection(electionId, request.classIds()));
    }
}
