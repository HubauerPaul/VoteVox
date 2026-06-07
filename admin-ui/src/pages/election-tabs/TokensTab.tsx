import { useState } from 'react';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { Table, TableColumn } from '../../components/Table';
import { EmptyState } from '../../components/EmptyState';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ElectionStatus, GeneratedToken } from '../../types';
import { downloadTokenPdf, generateTokens } from '../../api/tokens';
import { useToast } from '../../components/Toast';
import { extractErrorMessage, triggerBlobDownload } from '../../api/client';
import { formatDateTime } from '../../utils/format';

interface TokensTabProps {
  electionId: string;
  tokensGeneratedAt: string | null;
  status: ElectionStatus;
  studentCount: number;
  onChanged: () => void;
}

export function TokensTab({
  electionId,
  tokensGeneratedAt,
  status,
  studentCount,
  onChanged,
}: TokensTabProps): JSX.Element {
  const [generated, setGenerated] = useState<GeneratedToken[] | null>(null);
  const [generating, setGenerating] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const { showToast } = useToast();

  const alreadyGenerated = tokensGeneratedAt !== null;
  const canGenerate = status === 'PLANNED' && studentCount > 0;

  const handleGenerate = async (): Promise<void> => {
    setGenerating(true);
    try {
      const tokens = await generateTokens(electionId);
      setGenerated(tokens);
      setConfirmOpen(false);
      showToast(`${tokens.length} token(s) generated`, 'success');
      onChanged();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not generate tokens'), 'error');
    } finally {
      setGenerating(false);
    }
  };

  const handleDownload = async (): Promise<void> => {
    setDownloading(true);
    try {
      const blob = await downloadTokenPdf(electionId);
      triggerBlobDownload(blob, `votevox-tokens-${electionId}.pdf`);
      showToast('PDF downloaded', 'success');
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not download PDF'), 'error');
    } finally {
      setDownloading(false);
    }
  };

  const columns: TableColumn<GeneratedToken>[] = [
    { key: 'name', header: 'Student', render: (row) => row.studentName },
    { key: 'studentId', header: 'Student ID', render: (row) => row.studentId, width: '140px' },
    {
      key: 'token',
      header: 'Token (one-time)',
      render: (row) => <code>{row.tokenPlaintext}</code>,
    },
  ];

  if (generated) {
    return (
      <>
        <div className="banner banner-warning" role="alert">
          <strong>Save or print these now.</strong> The plaintext tokens will not be shown again.
          Use the QR PDF for distribution.
        </div>
        <Card
          title={`Generated tokens (${generated.length})`}
          actions={
            <Button variant="primary" onClick={handleDownload} disabled={downloading}>
              {downloading ? <Spinner /> : 'Download QR PDF'}
            </Button>
          }
        >
          <Table columns={columns} data={generated} rowKey={(row) => row.studentId} />
        </Card>
      </>
    );
  }

  if (alreadyGenerated) {
    return (
      <Card title="Voting tokens">
        <div className="banner banner-info">
          Tokens were generated on <strong>{formatDateTime(tokensGeneratedAt)}</strong>. The
          plaintext codes are only shown once at generation time. You can re-download the QR PDF
          at any time.
        </div>
        <Button variant="primary" onClick={handleDownload} disabled={downloading}>
          {downloading ? <Spinner /> : 'Download QR PDF'}
        </Button>
      </Card>
    );
  }

  return (
    <>
      <Card title="Voting tokens">
        {studentCount === 0 ? (
          <EmptyState
            title="No students to generate tokens for"
            description="Add eligible students first, then return here to generate one-time voting tokens."
          />
        ) : (
          <>
            <p>
              Generate one-time voting tokens for the {studentCount} registered student(s). Each
              student will receive a QR code that can be scanned to cast their vote. Tokens are
              hashed in the database and cannot be recovered after generation.
            </p>
            <Button
              variant="primary"
              onClick={() => setConfirmOpen(true)}
              disabled={!canGenerate}
            >
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
              This will create one-time voting codes for all {studentCount} registered
              student(s). The plaintext tokens will be displayed on the next screen{' '}
              <strong>once</strong> — make sure to print or save the QR PDF immediately.
            </p>
            <p className="text-warning">
              <strong>Warning:</strong> Existing tokens (if any) will be replaced.
            </p>
          </>
        }
        confirmLabel="Generate"
        loading={generating}
        onConfirm={handleGenerate}
        onCancel={() => setConfirmOpen(false)}
      />
    </>
  );
}
