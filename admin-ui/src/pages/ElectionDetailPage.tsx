import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { StatusBadge } from '../components/StatusBadge';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ElectionDetail } from '../types';
import {
  deleteElection,
  getElection,
  startElection,
  stopElection,
} from '../api/elections';
import { useToast } from '../components/Toast';
import { extractErrorMessage } from '../api/client';
import { electionTypeLabel, formatDateTime } from '../utils/format';
import { CandidatesTab } from './election-tabs/CandidatesTab';
import { StudentsTab } from './election-tabs/StudentsTab';
import { TokensTab } from './election-tabs/TokensTab';
import { ResultsTab } from './election-tabs/ResultsTab';

type TabKey = 'overview' | 'candidates' | 'students' | 'tokens' | 'results';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'overview', label: 'Overview' },
  { key: 'candidates', label: 'Candidates' },
  { key: 'students', label: 'Students' },
  { key: 'tokens', label: 'Tokens' },
  { key: 'results', label: 'Results' },
];

type ActionType = 'delete' | 'start' | 'stop';

export function ElectionDetailPage(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { showToast } = useToast();

  const electionId = id ?? '';
  const validId = electionId.length > 0;

  const tabParam = searchParams.get('tab');
  const currentTab: TabKey =
    tabParam && TABS.some((t) => t.key === tabParam) ? (tabParam as TabKey) : 'overview';

  const [election, setElection] = useState<ElectionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [pendingAction, setPendingAction] = useState<ActionType | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const loadElection = useCallback(async (): Promise<void> => {
    if (!validId) return;
    setLoading(true);
    try {
      const data = await getElection(electionId);
      setElection(data);
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not load election'), 'error');
    } finally {
      setLoading(false);
    }
  }, [electionId, validId, showToast]);

  useEffect(() => {
    let cancelled = false;
    if (!validId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getElection(electionId)
      .then((data) => {
        if (!cancelled) setElection(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load election'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [electionId, validId, showToast]);

  const setTab = (tab: TabKey): void => {
    const params = new URLSearchParams(searchParams);
    if (tab === 'overview') {
      params.delete('tab');
    } else {
      params.set('tab', tab);
    }
    setSearchParams(params, { replace: true });
  };

  const handleConfirm = async (): Promise<void> => {
    if (!election || !pendingAction) return;
    setActionLoading(true);
    try {
      if (pendingAction === 'delete') {
        await deleteElection(election.id);
        showToast('Election deleted', 'success');
        navigate('/elections');
        return;
      }
      if (pendingAction === 'start') {
        const updated = await startElection(election.id);
        setElection((prev) => (prev ? { ...prev, ...updated } : updated));
        showToast('Election started', 'success');
      } else if (pendingAction === 'stop') {
        const updated = await stopElection(election.id);
        setElection((prev) => (prev ? { ...prev, ...updated } : updated));
        showToast('Election stopped', 'success');
      }
      setPendingAction(null);
    } catch (err) {
      showToast(extractErrorMessage(err, 'Action failed'), 'error');
    } finally {
      setActionLoading(false);
    }
  };

  if (!validId) {
    return (
      <Card>
        <p>Invalid election id.</p>
        <Button variant="secondary" onClick={() => navigate('/elections')}>
          Back to Elections
        </Button>
      </Card>
    );
  }

  if (loading) {
    return <Spinner centered size="lg" />;
  }

  if (!election) {
    return (
      <Card>
        <p>Election not found.</p>
        <Button variant="secondary" onClick={() => navigate('/elections')}>
          Back to Elections
        </Button>
      </Card>
    );
  }

  return (
    <>
      <PageHeader
        title={
          <span>
            {election.title}{' '}
            <span style={{ marginLeft: 8 }}>
              <StatusBadge status={election.status} />
            </span>
          </span>
        }
        subtitle={electionTypeLabel(election.type)}
        actions={
          <>
            {election.status === 'PLANNED' && (
              <Button variant="secondary" onClick={() => setPendingAction('start')}>
                Start Election
              </Button>
            )}
            {election.status === 'RUNNING' && (
              <Button variant="secondary" onClick={() => setPendingAction('stop')}>
                Stop Election
              </Button>
            )}
            <Button
              variant="danger"
              onClick={() => setPendingAction('delete')}
              disabled={election.status === 'RUNNING'}
            >
              Delete
            </Button>
          </>
        }
      />

      <div className="tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`tab-btn ${currentTab === tab.key ? 'active' : ''}`}
            onClick={() => setTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {currentTab === 'overview' && (
        <Card title="Overview">
          <dl className="kvp-list">
            <dt>Status</dt>
            <dd>
              <StatusBadge status={election.status} />
            </dd>
            <dt>Type</dt>
            <dd>{electionTypeLabel(election.type)}</dd>
            <dt>Start time</dt>
            <dd>{formatDateTime(election.startTime)}</dd>
            <dt>End time</dt>
            <dd>{formatDateTime(election.endTime)}</dd>
            <dt>Description</dt>
            <dd>{election.description || <span className="text-muted">No description</span>}</dd>
            <dt>Candidates</dt>
            <dd>{election.candidateCount}</dd>
            <dt>Eligible students</dt>
            <dd>{election.studentCount}</dd>
            <dt>Votes cast</dt>
            <dd>{election.voteCount}</dd>
          </dl>
        </Card>
      )}

      {currentTab === 'candidates' && (
        <CandidatesTab
          electionId={election.id}
          status={election.status}
          onChanged={loadElection}
        />
      )}

      {currentTab === 'students' && (
        <StudentsTab electionId={election.id} status={election.status} onChanged={loadElection} />
      )}

      {currentTab === 'tokens' && (
        <TokensTab
          electionId={election.id}
          tokensGeneratedAt={election.tokensGeneratedAt ?? null}
          status={election.status}
          studentCount={election.studentCount}
          onChanged={loadElection}
        />
      )}

      {currentTab === 'results' && (
        <ResultsTab electionId={election.id} status={election.status} />
      )}

      <ConfirmDialog
        open={pendingAction !== null}
        title={
          pendingAction === 'delete'
            ? 'Delete election'
            : pendingAction === 'start'
              ? 'Start election'
              : 'Stop election'
        }
        message={
          pendingAction === 'delete' ? (
            <p>
              Permanently delete <strong>{election.title}</strong>? All candidates, students,
              tokens and votes will be removed. This cannot be undone.
            </p>
          ) : pendingAction === 'start' ? (
            <p>
              Start the election now? Voters will be able to cast their ballots immediately.
            </p>
          ) : (
            <p>Stop the election? No further votes can be recorded.</p>
          )
        }
        confirmLabel={
          pendingAction === 'delete'
            ? 'Delete'
            : pendingAction === 'start'
              ? 'Start'
              : 'Stop'
        }
        destructive={pendingAction === 'delete'}
        loading={actionLoading}
        onConfirm={handleConfirm}
        onCancel={() => setPendingAction(null)}
      />
    </>
  );
}
