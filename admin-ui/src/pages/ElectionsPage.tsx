import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { Button } from '../components/Button';
import { Select } from '../components/Select';
import { Spinner } from '../components/Spinner';
import { Table, TableColumn } from '../components/Table';
import { StatusBadge } from '../components/StatusBadge';
import { EmptyState } from '../components/EmptyState';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ElectionStatus, ElectionSummary } from '../types';
import {
  deleteElection,
  listElections,
  startElection,
  stopElection,
} from '../api/elections';
import { useToast } from '../components/Toast';
import { extractErrorMessage } from '../api/client';
import { electionTypeLabel, formatDateTime } from '../utils/format';

type StatusFilter = 'ALL' | ElectionStatus;

interface PendingAction {
  type: 'delete' | 'start' | 'stop';
  election: ElectionSummary;
}

export function ElectionsPage(): JSX.Element {
  const [elections, setElections] = useState<ElectionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [pending, setPending] = useState<PendingAction | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const { showToast } = useToast();
  const navigate = useNavigate();

  const loadElections = (): void => {
    setLoading(true);
    listElections()
      .then(setElections)
      .catch((err: unknown) => showToast(extractErrorMessage(err, 'Could not load elections'), 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
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

  const filtered = useMemo(() => {
    if (statusFilter === 'ALL') return elections;
    return elections.filter((e) => e.status === statusFilter);
  }, [elections, statusFilter]);

  const handleConfirm = async (): Promise<void> => {
    if (!pending) return;
    setActionLoading(true);
    try {
      if (pending.type === 'delete') {
        await deleteElection(pending.election.id);
        showToast('Election deleted', 'success');
      } else if (pending.type === 'start') {
        await startElection(pending.election.id);
        showToast('Election started', 'success');
      } else if (pending.type === 'stop') {
        await stopElection(pending.election.id);
        showToast('Election stopped', 'success');
      }
      setPending(null);
      loadElections();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Action failed'), 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const columns: TableColumn<ElectionSummary>[] = [
    {
      key: 'title',
      header: 'Title',
      render: (row) => <Link to={`/elections/${row.id}`}>{row.title}</Link>,
    },
    {
      key: 'type',
      header: 'Type',
      render: (row) => electionTypeLabel(row.type),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: 'start',
      header: 'Start',
      render: (row) => formatDateTime(row.startTime),
    },
    {
      key: 'end',
      header: 'End',
      render: (row) => formatDateTime(row.endTime),
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
        <div className="data-table-actions">
          <Button variant="link" onClick={() => navigate(`/elections/${row.id}`)}>
            View
          </Button>
          {row.status === 'PLANNED' && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPending({ type: 'start', election: row })}
            >
              Start
            </Button>
          )}
          {row.status === 'RUNNING' && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPending({ type: 'stop', election: row })}
            >
              Stop
            </Button>
          )}
          <Button
            variant="danger"
            size="sm"
            onClick={() => setPending({ type: 'delete', election: row })}
            disabled={row.status === 'RUNNING'}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Elections"
        actions={
          <Button variant="primary" onClick={() => navigate('/elections/new')}>
            New Election
          </Button>
        }
      />

      <div className="card">
        <div className="flex-between mb-16">
          <div style={{ maxWidth: 240, width: '100%' }}>
            <Select
              label="Filter by status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            >
              <option value="ALL">All statuses</option>
              <option value="PLANNED">Planned</option>
              <option value="RUNNING">Running</option>
              <option value="FINISHED">Finished</option>
            </Select>
          </div>
          <div className="text-muted">{filtered.length} election(s)</div>
        </div>

        {loading ? (
          <Spinner centered size="lg" />
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No elections"
            description="Create your first election to get started."
            action={
              <Button variant="primary" onClick={() => navigate('/elections/new')}>
                New Election
              </Button>
            }
          />
        ) : (
          <Table columns={columns} data={filtered} rowKey={(row) => row.id} />
        )}
      </div>

      <ConfirmDialog
        open={pending !== null}
        title={
          pending?.type === 'delete'
            ? 'Delete election'
            : pending?.type === 'start'
              ? 'Start election'
              : 'Stop election'
        }
        message={
          pending?.type === 'delete' ? (
            <p>
              Are you sure you want to delete <strong>{pending.election.title}</strong>? This
              action cannot be undone.
            </p>
          ) : pending?.type === 'start' ? (
            <p>
              Start <strong>{pending.election.title}</strong>? Voters will be able to cast their
              ballots immediately.
            </p>
          ) : (
            <p>
              Stop <strong>{pending?.election.title}</strong>? No further votes can be recorded
              after stopping.
            </p>
          )
        }
        confirmLabel={
          pending?.type === 'delete' ? 'Delete' : pending?.type === 'start' ? 'Start' : 'Stop'
        }
        destructive={pending?.type === 'delete'}
        loading={actionLoading}
        onConfirm={handleConfirm}
        onCancel={() => setPending(null)}
      />
    </>
  );
}
