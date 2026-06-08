import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { Table, TableColumn } from '../components/Table';
import { StatusBadge } from '../components/StatusBadge';
import { ElectionSummary } from '../types';
import { listElections } from '../api/elections';
import { useToast } from '../components/Toast';
import { extractErrorMessage } from '../api/client';
import { formatDateTime, electionTypeLabel } from '../utils/format';

export function DashboardPage(): JSX.Element {
  const [elections, setElections] = useState<ElectionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const { showToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listElections()
      .then((data) => {
        if (!cancelled) setElections(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load elections'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [showToast]);

  const totalElections = elections.length;
  const runningCount = elections.filter((e) => e.status === 'RUNNING').length;
  const totalCandidates = elections.reduce((sum, e) => sum + (e.candidateCount ?? 0), 0);
  const totalVotes = elections.reduce((sum, e) => sum + (e.voteCount ?? 0), 0);

  const recent = [...elections]
    .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())
    .slice(0, 5);

  const columns: TableColumn<ElectionSummary>[] = [
    {
      key: 'title',
      header: 'Title',
      render: (row) => <Link to={`/elections/${row.id}`}>{row.title}</Link>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: 'type',
      header: 'Type',
      render: (row) => electionTypeLabel(row.type),
    },
    {
      key: 'votes',
      header: 'Votes',
      align: 'right',
      render: (row) => row.voteCount,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (row) => (
        <Link to={`/elections/${row.id}`} className="btn-link">
          View
        </Link>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Dashboard" subtitle={`Today is ${formatDateTime(new Date().toISOString(), 'date')}`} />

      {loading ? (
        <Spinner centered size="lg" />
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-card-label">Total Elections</div>
              <div className="stat-card-value">{totalElections}</div>
            </div>
            <div className="stat-card">
              <div className="stat-card-label">Currently Running</div>
              <div className="stat-card-value">{runningCount}</div>
            </div>
            <div className="stat-card">
              <div className="stat-card-label">Total Candidates</div>
              <div className="stat-card-value">{totalCandidates}</div>
            </div>
            <div className="stat-card">
              <div className="stat-card-label">Total Votes Cast</div>
              <div className="stat-card-value">{totalVotes}</div>
            </div>
          </div>

          <div className="two-col">
            <Card
              title="Recent Elections"
              actions={
                <Link to="/elections" className="btn-link">
                  View all
                </Link>
              }
            >
              <Table
                columns={columns}
                data={recent}
                rowKey={(row) => row.id}
                emptyMessage="No elections yet"
              />
            </Card>
            <Card title="Quick Actions">
              <div className="flex" style={{ flexDirection: 'column', gap: 8 }}>
                <Button variant="primary" onClick={() => navigate('/elections/new')}>
                  New Election
                </Button>
                <Button variant="secondary" onClick={() => navigate('/audit')}>
                  View Audit Log
                </Button>
              </div>
            </Card>
          </div>
        </>
      )}
    </>
  );
}
