package at.htlwels.votevox.voting;

import at.htlwels.votevox.voting.dto.CastVoteRequest;
import at.htlwels.votevox.voting.dto.CastVoteResponse;
import at.htlwels.votevox.voting.dto.ValidateTokenRequest;
import at.htlwels.votevox.voting.dto.ValidateTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public voting endpoints - the only routes that may be called without
 * authentication. Security is enforced by token possession.
 */
@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor
public class VotingController {

    private final VotingService votingService;

    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(votingService.validate(request));
    }

    @PostMapping("/cast")
    public ResponseEntity<CastVoteResponse> cast(@Valid @RequestBody CastVoteRequest request) {
        return ResponseEntity.ok(votingService.cast(request));
    }
}
