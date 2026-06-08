import { FormEvent, useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { Table, TableColumn } from '../../components/Table';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { Candidate, ElectionStatus } from '../../types';
import {
  createCandidate,
  deleteCandidate,
  listCandidates,
} from '../../api/candidates';
import { useToast } from '../../components/Toast';
import { extractErrorMessage } from '../../api/client';

interface CandidatesTabProps {
  electionId: string;
  status: ElectionStatus;
  onChanged: () => void;
}

export function CandidatesTab({
  electionId,
  status,
  onChanged,
}: CandidatesTabProps): JSX.Element {
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [className, setClassName] = useState('');
  const [department, setDepartment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<Candidate | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const { showToast } = useToast();

  const canEdit = status === 'PLANNED';

  const load = (): void => {
    setLoading(true);
    listCandidates(electionId)
      .then(setCandidates)
      .catch((err: unknown) => showToast(extractErrorMessage(err, 'Could not load candidates'), 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listCandidates(electionId)
      .then((data) => {
        if (!cancelled) setCandidates(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load candidates'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [electionId, showToast]);

  const handleAdd = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    try {
      await createCandidate(electionId, {
        name: name.trim(),
        className: className.trim(),
        department: department.trim(),
      });
      showToast('Candidate added', 'success');
      setName('');
      setClassName('');
      setDepartment('');
      load();
      onChanged();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not add candidate'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (): Promise<void> => {
    if (!pendingDelete) return;
    setDeleteLoading(true);
    try {
      await deleteCandidate(electionId, pendingDelete.electionCandidateId);
      showToast('Candidate removed', 'success');
      setPendingDelete(null);
      load();
      onChanged();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not remove candidate'), 'error');
    } finally {
      setDeleteLoading(false);
    }
  };

  const columns: TableColumn<Candidate>[] = [
    { key: 'candidateId', header: 'ID', render: (row) => row.candidateId, width: '120px' },
    { key: 'name', header: 'Name', render: (row) => row.name },
    { key: 'className', header: 'Class', render: (row) => row.className },
    { key: 'department', header: 'Department', render: (row) => row.department },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (row) => (
        <Button
          variant="danger"
          size="sm"
          onClick={() => setPendingDelete(row)}
          disabled={!canEdit}
        >
          Remove
        </Button>
      ),
    },
  ];

  return (
    <>
      {canEdit && (
        <Card title="Add candidate">
          <form onSubmit={handleAdd} noValidate>
            <div className="inline-form">
              <Input
                label="Name"
                name="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <Input
                label="Class"
                name="className"
                value={className}
                onChange={(e) => setClassName(e.target.value)}
                required
                placeholder="e.g. 4AHITS"
              />
              <Input
                label="Department"
                name="department"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                required
                placeholder="e.g. Informatik"
              />
              <Button type="submit" variant="primary" disabled={submitting}>
                {submitting ? <Spinner /> : 'Add'}
              </Button>
            </div>
          </form>
        </Card>
      )}

      <Card title={`Candidates (${candidates.length})`}>
        {!canEdit && (
          <div className="banner banner-info">
            Candidates can only be modified while the election is in <strong>Planned</strong>{' '}
            status.
          </div>
        )}
        {loading ? (
          <Spinner centered size="lg" />
        ) : candidates.length === 0 ? (
          <EmptyState
            title="No candidates yet"
            description={
              canEdit
                ? 'Use the form above to add candidates.'
                : 'There are no candidates registered for this election.'
            }
          />
        ) : (
          <Table columns={columns} data={candidates} rowKey={(row) => row.electionCandidateId} />
        )}
      </Card>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Remove candidate"
        message={
          <p>
            Remove <strong>{pendingDelete?.name}</strong> from this election?
          </p>
        }
        confirmLabel="Remove"
        destructive
        loading={deleteLoading}
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </>
  );
}
