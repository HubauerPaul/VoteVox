package at.htlwels.votevox.reporting;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.JwtAuthenticationFilter.AuthenticatedPrincipal;
import at.htlwels.votevox.reporting.dto.ResultsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/elections/{electionId}/results")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
    private final PdfReportGenerator pdfReportGenerator;
    private final CsvReportGenerator csvReportGenerator;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ResultsResponse> json(@PathVariable UUID electionId) {
        ResultsResponse results = reportingService.getResults(electionId);
        auditService.logAdmin(currentAdminId(), "RESULTS_VIEWED",
                "Election: " + results.electionTitle());
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID electionId) throws IOException {
        ResultsResponse results = reportingService.getResults(electionId);
        byte[] pdf = pdfReportGenerator.render(results);
        auditService.logAdmin(currentAdminId(), "RESULTS_PDF_EXPORTED",
                "Election: " + results.electionTitle());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "votevox-results-" + slug(results.electionTitle()) + ".pdf");
        return new ResponseEntity<>(pdf, headers, 200);
    }

    @GetMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<byte[]> csv(@PathVariable UUID electionId) {
        ResultsResponse results = reportingService.getResults(electionId);
        byte[] csv = csvReportGenerator.render(results);
        auditService.logAdmin(currentAdminId(), "RESULTS_CSV_EXPORTED",
                "Election: " + results.electionTitle());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment",
                "votevox-results-" + slug(results.electionTitle()) + ".csv");
        return new ResponseEntity<>(csv, headers, 200);
    }

    private String slug(String s) {
        return s == null ? "election"
                : s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
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
