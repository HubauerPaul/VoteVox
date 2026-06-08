package at.htlwels.votevox.reporting;

import at.htlwels.votevox.reporting.dto.BreakdownEntry;
import at.htlwels.votevox.reporting.dto.CandidateResult;
import at.htlwels.votevox.reporting.dto.ResultsResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Writes a results CSV containing the per-candidate tally plus two breakdown
 * sections (by class, by department). Uses comma as the delimiter and quotes
 * every field for spreadsheet portability.
 */
@Component
public class CsvReportGenerator {

    public byte[] render(ResultsResponse results) {
        StringBuilder sb = new StringBuilder(4096);

        // Header line
        sb.append("# VoteVox Results\n");
        sb.append("# Election: ").append(escape(results.electionTitle())).append("\n");
        sb.append("# Type: ").append(results.electionType()).append("\n");
        sb.append("# Status: ").append(results.status()).append("\n");
        sb.append("# Generated: ").append(results.generatedAt()).append("\n");
        sb.append("# Total votes: ").append(results.totalVotes()).append("\n");
        sb.append("\n");

        // Candidate results
        sb.append("candidate_name,class,department,votes,percentage\n");
        for (CandidateResult c : results.candidates()) {
            sb.append(q(c.name())).append(',')
              .append(q(c.className())).append(',')
              .append(q(c.department())).append(',')
              .append(c.votes()).append(',')
              .append(String.format("%.2f", c.percentage()))
              .append('\n');
        }

        // By class
        sb.append('\n');
        sb.append("# Participation by class\n");
        sb.append("class,eligible,tokens_issued\n");
        for (BreakdownEntry b : results.byClass()) {
            sb.append(q(b.label())).append(',')
              .append(b.eligible()).append(',')
              .append(b.tokensIssued()).append('\n');
        }

        // By department
        sb.append('\n');
        sb.append("# Participation by department\n");
        sb.append("department,eligible,tokens_issued\n");
        for (BreakdownEntry b : results.byDepartment()) {
            sb.append(q(b.label())).append(',')
              .append(b.eligible()).append(',')
              .append(b.tokensIssued()).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String q(String s) {
        return "\"" + escape(s) + "\"";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }
}
