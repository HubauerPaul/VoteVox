package at.htlwels.votevox.qrcode;

import at.htlwels.votevox.qrcode.dto.GeneratedTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Renders the printable QR token sheet - one A4 page per student.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenPdfGenerator {

    private final QRCodeGenerationService qrCodeService;

    public byte[] renderPdf(String electionTitle, List<GeneratedTokenResponse> tokens) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (GeneratedTokenResponse t : tokens) {
                addPage(doc, electionTitle, t);
            }
            // If no tokens, still produce a one-page placeholder.
            if (tokens.isEmpty()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18,
                            72, page.getMediaBox().getHeight() - 72, "VoteVox - " + electionTitle);
                    drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12,
                            72, page.getMediaBox().getHeight() - 110, "No tokens generated.");
                }
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private void addPage(PDDocument doc, String electionTitle, GeneratedTokenResponse t) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        byte[] qrPng = qrCodeService.buildQrPng(t.tokenPlaintext());
        PDImageXObject qrImage = PDImageXObject.createFromByteArray(doc, qrPng, "qr-" + t.studentId());

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Header
            drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20,
                    72, pageHeight - 72, "VoteVox  -  " + safe(electionTitle));

            // Divider
            cs.setLineWidth(0.5f);
            cs.moveTo(72, pageHeight - 86);
            cs.lineTo(pageWidth - 72, pageHeight - 86);
            cs.stroke();

            // Student info
            float y = pageHeight - 120;
            drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14, 72, y,
                    "Student: " + safe(t.studentName()));
            drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12, 72, y - 20,
                    "Student-ID: " + safe(t.studentExternalId()));

            // QR code centered horizontally - 200x200 points (~70mm) for readability
            float qrSize = 200f;
            float qrX = (pageWidth - qrSize) / 2f;
            float qrY = (pageHeight - qrSize) / 2f - 30;
            cs.drawImage(qrImage, qrX, qrY, qrSize, qrSize);

            // Instructions below QR
            String instructions1 = "Scan with the school's voting app to cast your vote.";
            drawCentered(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11,
                    pageWidth, qrY - 24, instructions1);

            // Footer
            String footer = "One-time use only. Do not share. Bring this to vote.";
            drawCentered(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 10,
                    pageWidth, 56, footer);
            drawCentered(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8,
                    pageWidth, 40, "VoteVox - HTL Wels");
        }
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

    private String safe(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]+", " ");
    }
}
