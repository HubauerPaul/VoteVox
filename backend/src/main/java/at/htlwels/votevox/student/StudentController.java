package at.htlwels.votevox.student;

import at.htlwels.votevox.student.dto.ImportStudentsRequest;
import at.htlwels.votevox.student.dto.StudentRequest;
import at.htlwels.votevox.student.dto.StudentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/api/students")
    public ResponseEntity<List<StudentResponse>> listAll() {
        return ResponseEntity.ok(studentService.listAll());
    }

    @PostMapping("/api/students")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(201).body(studentService.create(request));
    }

    @GetMapping("/api/students/{id}")
    public ResponseEntity<StudentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.get(id));
    }

    @PutMapping("/api/students/{id}")
    public ResponseEntity<StudentResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.update(id, request));
    }

    @DeleteMapping("/api/students/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/elections/{electionId}/students")
    public ResponseEntity<List<StudentResponse>> listForElection(@PathVariable UUID electionId) {
        return ResponseEntity.ok(studentService.listEligibleForElection(electionId));
    }

    @PostMapping("/api/elections/{electionId}/students")
    public ResponseEntity<List<StudentResponse>> addToElection(@PathVariable UUID electionId,
                                                               @Valid @RequestBody ImportStudentsRequest request) {
        return ResponseEntity.status(201).body(studentService.bulkAddToElection(electionId, request));
    }
}
