package at.htlwels.votevox.schoolclass;

import at.htlwels.votevox.schoolclass.dto.ClassRequest;
import at.htlwels.votevox.schoolclass.dto.ClassResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<List<ClassResponse>> listAll() {
        return ResponseEntity.ok(classService.listAll());
    }

    @PostMapping
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody ClassRequest request) {
        return ResponseEntity.status(201).body(classService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.ok(classService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        classService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
