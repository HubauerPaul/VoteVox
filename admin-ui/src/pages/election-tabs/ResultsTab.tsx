import { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { Table, TableColumn } from '../../components/Table';
import { EmptyState } from '../../components/EmptyState';
import {
  ElectionResults,
  ElectionStatus,
  ResultBreakdownEntry,
  ResultCandidate,
} from '../../types';
import {
  downloadResultsCsv,
  downloadResultsPdf,
  getResults,
} from '../../api/results';
import { useToast } from '../../components/Toast';
import { extractErrorMessage, triggerBlobDownload } from '../../api/client';
import { formatPercentage } from '../../utils/format';

interface ResultsTabProps {
  electionId: string;
  status: ElectionStatus;
}

export function ResultsTab({ electionId, status }: ResultsTabProps): JSX.Element {
  const [results, setResults] = useState<ElectionResults | null>(null);
  const [loading, setLoading] = useState(true);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [csvLoading, setCsvLoading] = useState(false);
  const { showToast } = useToast();

  const supported = status === 'RUNNING' || status === 'FINISHED';

  useEffect(() => {
    if (!supported) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    getResults(electionId)
      .then((data) => {
        if (!cancelled) setResults(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load results'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [electionId, supported, showToast]);

  const handlePdf = async (): Promise<void> => {
    setPdfLoading(true);
    try {
      const blob = await downloadResultsPdf(electionId);
      triggerBlobDownload(blob, `votevox-results-${electionId}.pdf`);
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not download PDF'), 'error');
    } finally {
      setPdfLoading(false);
    }
  };

  const handleCsv = async (): Promise<void> => {
    setCsvLoading(true);
    try {
      const blob = await downloadResultsCsv(electionId);
      triggerBlobDownload(blob, `votevox-results-${electionId}.csv`);
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not download CSV'), 'error');
    } finally {
      setCsvLoading(false);
    }
  };

  if (!supported) {
    return (
      <Card title="Results">
        <EmptyState
          title="Results not available yet"
          description="Results become available once the election is started."
        />
      </Card>
    );
  }

  if (loading) {
    return <Spinner centered size="lg" />;
  }

  if (!results) {
    return (
      <Card title="Results">
        <EmptyState title="No data" />
      </Card>
    );
  }

  const candidateCols: TableColumn<ResultCandidate>[] = [
    { key: 'name', header: 'Candidate', render: (row) => row.name },
    {
      key: 'class',
      header: 'Class',
      render: (row) => row.className ?? '—',
    },
    {
      key: 'department',
      header: 'Department',
      render: (row) => row.department ?? '—',
    },
    {
      key: 'votes',
      header: 'Votes',
      align: 'right',
      render: (row) => row.votes,
    },
    {
      key: 'percentage',
      header: 'Share',
      align: 'right',
      render: (row) => formatPercentage(row.percentage),
    },
  ];

  const breakdownCols: TableColumn<ResultBreakdownEntry>[] = [
    { key: 'label', header: 'Label', render: (row) => row.label },
    { key: 'count', header: 'Votes', align: 'right', render: (row) => row.count },
  ];

  return (
    <>
      <Card
        title={`Results — ${results.totalVotes} vote(s) cast`}
        actions={
          <>
            <Button variant="secondary" onClick={handlePdf} disabled={pdfLoading}>
              {pdfLoading ? <Spinner /> : 'Export PDF'}
            </Button>
            <Button variant="secondary" onClick={handleCsv} disabled={csvLoading}>
              {csvLoading ? <Spinner /> : 'Export CSV'}
            </Button>
          </>
        }
      >
        {results.candidates.length === 0 ? (
          <EmptyState title="No votes recorded yet" />
        ) : (
          <>
            <div className="results-bar-chart mb-16">
              <ResponsiveContainer width="100%" height={Math.max(220, results.candidates.length * 40)}>
                <BarChart
                  data={results.candidates}
                  layout="vertical"
                  margin={{ top: 8, right: 24, bottom: 8, left: 16 }}
                >
                  <CartesianGrid stroke="#e5e7eb" strokeDasharray="3 3" />
                  <XAxis type="number" allowDecimals={false} />
                  <YAxis type="category" dataKey="name" width={140} />
                  <Tooltip
                    formatter={(value: number) => [value, 'Votes']}
                    cursor={{ fill: 'rgba(30, 58, 95, 0.06)' }}
                  />
                  <Bar dataKey="votes" fill="#1e3a5f" />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <Table
              columns={candidateCols}
              data={results.candidates}
              rowKey={(row) => row.candidateId}
            />
          </>
        )}
      </Card>

      <div className="two-col">
        <Card title="By Class">
          {results.byClass.length === 0 ? (
            <EmptyState title="No data" />
          ) : (
            <Table
              columns={breakdownCols}
              data={results.byClass}
              rowKey={(row) => `class-${row.label}`}
            />
          )}
        </Card>
        <Card title="By Department">
          {results.byDepartment.length === 0 ? (
            <EmptyState title="No data" />
          ) : (
            <Table
              columns={breakdownCols}
              data={results.byDepartment}
              rowKey={(row) => `dept-${row.label}`}
            />
          )}
        </Card>
      </div>
    </>
  );
}
