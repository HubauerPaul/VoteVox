import { jsPDF } from 'jspdf';
import QRCode from 'qrcode';
import { GeneratedToken } from '../types';

/**
 * Base URL the QR codes point at. Must match the voting UI's /vote route so a
 * scan opens the ballot directly. Override per environment via VITE_QR_BASE_URL.
 */
const QR_BASE_URL =
  (import.meta.env.VITE_QR_BASE_URL as string | undefined) ?? 'http://localhost:5173/vote';

/** The voting URL embedded in a token's QR code. */
export function votingUrlFor(plaintext: string): string {
  return `${QR_BASE_URL}?token=${encodeURIComponent(plaintext)}`;
}

// --- A4 grid layout (mm) ----------------------------------------------------
const PAGE_W = 210;
const PAGE_H = 297;
const MARGIN = 10;
const COLS = 3;
const ROWS = 4;
const PER_PAGE = COLS * ROWS; // 12
const GRID_W = PAGE_W - 2 * MARGIN;
const GRID_H = PAGE_H - 2 * MARGIN;
const CELL_W = GRID_W / COLS;
const CELL_H = GRID_H / ROWS;
const QR_SIZE = 38;

/**
 * Draws the dashed cut grid for one page: light dashed lines along every cell
 * boundary (and the outer border) so the printed sheet can be cut into 12
 * separate voting cards.
 */
function drawCutGrid(doc: jsPDF): void {
  doc.setDrawColor(160);
  doc.setLineWidth(0.1);
  doc.setLineDashPattern([1.5, 1.5], 0);
  for (let c = 0; c <= COLS; c += 1) {
    const x = MARGIN + c * CELL_W;
    doc.line(x, MARGIN, x, PAGE_H - MARGIN);
  }
  for (let r = 0; r <= ROWS; r += 1) {
    const y = MARGIN + r * CELL_H;
    doc.line(MARGIN, y, PAGE_W - MARGIN, y);
  }
  // Reset to defaults for subsequent drawing/text.
  doc.setLineDashPattern([], 0);
  doc.setDrawColor(0);
}

/**
 * Builds the printable QR token sheet entirely in the browser: 12 codes per A4
 * page in a 3x4 grid with dashed cut lines. Each card shows its class name and
 * a one-time QR code, grouped by class.
 *
 * This never round-trips the plaintext back to the server: tokens are hashed
 * server-side and can never be recovered, so the PDF can only be produced here,
 * at generation time, from the in-memory set.
 */
/** Groups tokens by class, preserving the order in which classes first appear. */
function groupByClass(tokens: GeneratedToken[]): { className: string; tokens: GeneratedToken[] }[] {
  const groups: { className: string; tokens: GeneratedToken[] }[] = [];
  const indexByName = new Map<string, number>();
  for (const t of tokens) {
    let idx = indexByName.get(t.className);
    if (idx === undefined) {
      idx = groups.length;
      indexByName.set(t.className, idx);
      groups.push({ className: t.className, tokens: [] });
    }
    groups[idx].tokens.push(t);
  }
  return groups;
}

function drawCard(doc: jsPDF, t: GeneratedToken, posOnPage: number, qrDataUrl: string): void {
  const col = posOnPage % COLS;
  const row = Math.floor(posOnPage / COLS);
  const x0 = MARGIN + col * CELL_W;
  const y0 = MARGIN + row * CELL_H;
  const cx = x0 + CELL_W / 2;

  // Class label
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.text(t.className, cx, y0 + 8, { align: 'center' });

  // QR code (centered)
  const qrX = x0 + (CELL_W - QR_SIZE) / 2;
  const qrY = y0 + 11;
  doc.addImage(qrDataUrl, 'PNG', qrX, qrY, QR_SIZE, QR_SIZE);

  // Caption
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8);
  doc.text('One-time voting code', cx, qrY + QR_SIZE + 6, { align: 'center' });
  doc.setFontSize(6.5);
  doc.setTextColor(120);
  doc.text('Scan to vote · do not share', cx, qrY + QR_SIZE + 10, { align: 'center' });
  doc.setTextColor(0);
}

export async function buildTokenPdf(
  electionTitle: string,
  tokens: GeneratedToken[],
): Promise<Blob> {
  const doc = new jsPDF({ unit: 'mm', format: 'a4' });
  let firstPage = true;

  // Each class starts on its own fresh page; within a class, 12 codes per page.
  for (const group of groupByClass(tokens)) {
    for (let j = 0; j < group.tokens.length; j += 1) {
      const posOnPage = j % PER_PAGE;
      if (posOnPage === 0) {
        if (!firstPage) doc.addPage();
        firstPage = false;
        drawCutGrid(doc);
      }
      const qrDataUrl = await QRCode.toDataURL(votingUrlFor(group.tokens[j].tokenPlaintext), {
        errorCorrectionLevel: 'M',
        margin: 1,
        width: 400,
      });
      drawCard(doc, group.tokens[j], posOnPage, qrDataUrl);
    }
  }

  // Footer note on the first page (election title) - small, inside top margin.
  if (!firstPage) {
    doc.setPage(1);
    doc.setFont('helvetica', 'italic');
    doc.setFontSize(7);
    doc.setTextColor(120);
    doc.text(`VoteVox · ${electionTitle}`, MARGIN, MARGIN - 3);
    doc.setTextColor(0);
  }

  return doc.output('blob');
}
