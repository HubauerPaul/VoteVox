package at.htlwels.votevox.backup;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> trigger() {
        Path file = backupService.triggerBackup();
        return ResponseEntity.ok(Map.of(
                "path", file.toString(),
                "fileName", file.getFileName().toString(),
                "createdAt", Instant.now().toString()
        ));
    }
}
