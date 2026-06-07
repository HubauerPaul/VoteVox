package at.htlwels.votevox.reporting;

import at.htlwels.votevox.reporting.dto.BreakdownEntry;
import at.htlwels.votevox.reporting.dto.CandidateResult;
import at.htlwels.votevox.reporting.dto.ResultsResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds a printable PDF report from a {@link ResultsResponse}.
 */
@Component
public class PdfReportGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_INSTANT;

    public byte[] render(ResultsResponse results) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float marginX = 56;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float y = pageHeight - 56;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Title
                drawText(cs, bold(), 20, marginX, y, "Election Results: " + safe(results.electionTitle()));
                y -= 28;
                drawText(cs, regular(), 11, marginX, y,
                        "Generated: " + TS.format(results.generatedAt())
                              + "   |   Type: " + results.electionType()
                              + "   |   Status: " + results.status());
                y -= 20;
                drawText(cs, regular(), 11, marginX, y,
                        "Total votes: " + results.totalVotes());
                y -= 24;

                // Candidates header
                drawText(cs, bold(), 13, marginX, y, "Candidates");
                y -= 18;
                drawText(cs, bold(), 10, marginX, y, "Name");
                drawText(cs, bold(), 10, marginX + 240, y, "Class");
                drawText(cs, bold(), 10, marginX + 310, y, "Department");
                drawText(cs, bold(), 10, marginX + 400, y, "Votes");
                drawText(cs, bold(), 10, marginX + 450, y, "Share %");
                y -= 4;
                cs.moveTo(marginX, y);
                cs.lineTo(pageWidth - marginX, y);
                cs.stroke();
                y -= 14;

                for (CandidateResult c : results.candidates()) {
                    if (y < 100) {
                        cs.close();
                        page = newPage(doc);
                        try (PDPageContentStream cs2 = new PDPageContentStream(doc, page)) {
                            y = continueOnPage(cs2, pageHeight);
                            renderRow(cs2, c, marginX, y);
                        }
                        y -= 14;
                        continue;
                    }
                    renderRow(cs, c, marginX, y);
                    y -= 14;
                }

                y -= 18;
                y = renderBreakdown(cs, doc, page, "Participation by class", results.byClass(),
                        marginX, pageWidth, pageHeight, y);
                y -= 12;
                y = renderBreakdown(cs, doc, page, "Participation by department", results.byDepartment(),
                        marginX, pageWidth, pageHeight, y);

                // Footer
                drawCentered(cs, italic(), 9, pageWidth, 32, "VoteVox - HTL Wels");
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private float renderBreakdown(PDPageContentStream cs, PDDocument doc, PDPage page, String title,
                                  List<BreakdownEntry> entries, float marginX, float pageWidth,
                                  float pageHeight, float y) throws IOException {
        if (entries.isEmpty()) return y;
        drawText(cs, bold(), 13, marginX, y, title);
        y -= 16;
        drawText(cs, bold(), 10, marginX, y, "Group");
        drawText(cs, bold(), 10, marginX + 200, y, "Eligible");
        drawText(cs, bold(), 10, marginX + 280, y, "Tokens issued");
        y -= 14;
        for (BreakdownEntry b : entries) {
            drawText(cs, regular(), 10, marginX, y, safe(b.label()));
            drawText(cs, regular(), 10, marginX + 200, y, String.valueOf(b.eligible()));
            drawText(cs, regular(), 10, marginX + 280, y, String.valueOf(b.tokensIssued()));
            y -= 12;
        }
        return y;
    }

    private void renderRow(PDPageContentStream cs, CandidateResult c, float marginX, float y) throws IOException {
        drawText(cs, regular(), 10, marginX, y, safe(c.name()));
        drawText(cs, regular(), 10, marginX + 240, y, safe(c.className()));
        drawText(cs, regular(), 10, marginX + 310, y, safe(c.department()));
        drawText(cs, regular(), 10, marginX + 400, y, String.valueOf(c.votes()));
        drawText(cs, regular(), 10, marginX + 450, y, String.format("%.2f", c.percentage()));
    }

    private float continueOnPage(PDPageContentStream cs, float pageHeight) throws IOException {
        drawText(cs, italic(), 9, 56, pageHeight - 40, "(continued)");
        return pageHeight - 60;
    }

    private PDPage newPage(PDDocument doc) {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        return page;
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawCentered(PDPageContentStream cs, PDType1Font font, float size, float pageWidth, float y,
                              String text) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * size;
        float x = (pageWidth - textWidth) / 2f;
        drawText(cs, font, size, x, y, text);
    }

    private PDType1Font bold() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    private PDType1Font regular() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private PDType1Font italic() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    }

    private String safe(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]+", " ");
    }
}
