import { useState } from 'react';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { Table, TableColumn } from '../../components/Table';
import { EmptyState } from '../../components/EmptyState';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ElectionStatus, GeneratedToken } from '../../types';
import { generateTokens, resetTokens } from '../../api/tokens';
import { useToast } from '../../components/Toast';
import { extractErrorMessage, triggerBlobDownload } from '../../api/client';
import { buildTokenPdf } from '../../utils/tokenPdf';

interface TokensTabProps {
  electionId: string;
  electionTitle: string;
  status: ElectionStatus;
  studentCount: number;
  onChanged: () => void;
}

export function TokensTab({
  electionId,
  electionTitle,
  status,
  studentCount,
  onChanged,
}: TokensTabProps): JSX.Element {
  const [generated, setGenerated] = useState<GeneratedToken[] | null>(null);
  const [generating, setGenerating] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false);
  // True once generation returns zero new tokens because every enrolled
  // student already has one — the plaintexts are gone, so the only way to get a
  // fresh printable PDF is to reset and re-issue.
  const [allAlreadyIssued, setAllAlreadyIssued] = useState(false);
  const { showToast } = useToast();

  const canGenerate = status === 'PLANNED' && studentCount > 0;

  const runGenerate = async (): Promise<GeneratedToken[]> => {
    const tokens = await generateTokens(electionId);
    if (tokens.length === 0) {
      setAllAlreadyIssued(true);
      showToast(
        'All enrolled students already have a token. Reset to issue a fresh set.',
        'warning',
      );
    } else {
      setGenerated(tokens);
      setAllAlreadyIssued(false);
      showToast(`${tokens.length} token(s) generated`, 'success');
    }
    onChanged();
    return tokens;
  };

  const handleGenerate = async (): Promise<void> => {
    setGenerating(true);
    try {
      await runGenerate();
      setConfirmOpen(false);
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not generate tokens'), 'error');
    } finally {
      setGenerating(false);
    }
  };

  const handleResetAndRegenerate = async (): Promise<void> => {
    setGenerating(true);
    try {
      await resetTokens(electionId);
      await runGenerate();
      setResetConfirmOpen(false);
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not reset tokens'), 'error');
    } finally {
      setGenerating(false);
    }
  };

  const handleDownload = async (): Promise<void> => {
    if (!generated) {
      return;
    }
    setDownloading(true);
    try {
      const blob = await buildTokenPdf(electionTitle, generated);
      triggerBlobDownload(blob, `votevox-tokens-${electionId}.pdf`);
      showToast('QR PDF downloaded', 'success');
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not build PDF'), 'error');
    } finally {
      setDownloading(false);
    }
  };

  const columns: TableColumn<GeneratedToken>[] = [
    { key: 'class', header: 'Class', render: (row) => row.className, width: '160px' },
    {
      key: 'token',
      header: 'Token (one-time)',
      render: (row) => <code>{row.tokenPlaintext}</code>,
    },
  ];

  // --- Freshly generated: show plaintexts once + client-side QR PDF ----------
  if (generated) {
    return (
      <>
        <div className="banner banner-warning" role="alert">
          <strong>Save or print these now.</strong> The plaintext tokens will not be shown again.
          Download the QR PDF below for distribution.
        </div>
        <Card
          title={`Generated tokens (${generated.length})`}
          actions={
            <Button variant="primary" onClick={handleDownload} disabled={downloading}>
              {downloading ? <Spinner /> : 'Generate PDF with QR Codes'}
            </Button>
          }
        >
          <Table columns={columns} data={generated} rowKey={(row) => row.tokenPlaintext} />
        </Card>
      </>
    );
  }

  // --- Nothing generated in this session -------------------------------------
  return (
    <>
      <Card title="Voting tokens">
        {studentCount === 0 ? (
          <EmptyState
            title="No classes selected for this election"
            description="Pick the participating classes in the Classes tab first, then return here to generate one-time voting tokens."
          />
        ) : allAlreadyIssued ? (
          <>
            <div className="banner banner-info">
              All voters already have a voting token. The plaintext codes are shown only
              once at generation time and cannot be recovered. If you lost the QR PDF, reset the
              tokens to issue a brand-new set (only possible while the election is{' '}
              <strong>Planned</strong>).
            </div>
            <Button
              variant="danger"
              onClick={() => setResetConfirmOpen(true)}
              disabled={status !== 'PLANNED'}
            >
              Reset &amp; re-generate codes
            </Button>
            {status !== 'PLANNED' && (
              <p className="text-muted mt-12">
                Tokens are locked once the election has started and cannot be reset.
              </p>
            )}
          </>
        ) : (
          <>
            <p>
              Generate {studentCount} anonymous one-time voting token(s) — one per student across
              the selected classes. Each token becomes a QR code that opens the ballot. Tokens are
              hashed in the database and cannot be recovered after generation — download the QR PDF
              immediately.
            </p>
            <Button variant="primary" onClick={() => setConfirmOpen(true)} disabled={!canGenerate}>
              Generate Tokens
            </Button>
            {status !== 'PLANNED' && (
              <p className="text-muted mt-12">
                Tokens can only be generated while the election is in Planned status.
              </p>
            )}
          </>
        )}
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="Generate voting tokens"
        message={
          <>
            <p>
              This will create {studentCount} anonymous one-time voting code(s) across the selected
              classes. The plaintext tokens will be displayed on the next screen{' '}
              <strong>once</strong> — make sure to download the QR PDF immediately.
            </p>
          </>
        }
        confirmLabel="Generate"
        loading={generating}
        onConfirm={handleGenerate}
        onCancel={() => setConfirmOpen(false)}
      />

      <ConfirmDialog
        open={resetConfirmOpen}
        title="Reset and re-generate tokens"
        message={
          <>
            <p>
              This permanently deletes the existing tokens for this election and issues a brand-new
              set. Any previously printed QR codes will stop working.
            </p>
            <p className="text-warning">
              <strong>Only do this if the original QR PDF was lost</strong> and the election has not
              started yet.
            </p>
          </>
        }
        confirmLabel="Reset & re-generate"
        destructive
        loading={generating}
        onConfirm={handleResetAndRegenerate}
        onCancel={() => setResetConfirmOpen(false)}
      />
    </>
  );
}
