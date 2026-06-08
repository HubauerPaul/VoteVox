import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { EmptyState } from '../../components/EmptyState';
import { ElectionClassOption, ElectionStatus } from '../../types';
import { getElectionClasses, setElectionClasses } from '../../api/classes';
import { useToast } from '../../components/Toast';
import { extractErrorMessage } from '../../api/client';

interface ClassesTabProps {
  electionId: string;
  status: ElectionStatus;
  onChanged: () => void;
}

export function ClassesTab({ electionId, status, onChanged }: ClassesTabProps): JSX.Element {
  const [options, setOptions] = useState<ElectionClassOption[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const { showToast } = useToast();

  const canEdit = status === 'PLANNED';

  const applyOptions = (data: ElectionClassOption[]): void => {
    setOptions(data);
    setSelected(new Set(data.filter((o) => o.selected).map((o) => o.classId)));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getElectionClasses(electionId)
      .then((data) => {
        if (!cancelled) applyOptions(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load classes'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [electionId, showToast]);

  const totalStudents = useMemo(
    () => options.filter((o) => selected.has(o.classId)).reduce((sum, o) => sum + o.studentCount, 0),
    [options, selected]
  );

  const dirty = useMemo(() => {
    const original = new Set(options.filter((o) => o.selected).map((o) => o.classId));
    if (original.size !== selected.size) return true;
    for (const id of selected) if (!original.has(id)) return true;
    return false;
  }, [options, selected]);

  const toggle = (classId: string): void => {
    if (!canEdit) return;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(classId)) next.delete(classId);
      else next.add(classId);
      return next;
    });
  };

  const allSelected = options.length > 0 && selected.size === options.length;

  const toggleAll = (): void => {
    if (!canEdit) return;
    setSelected(allSelected ? new Set() : new Set(options.map((o) => o.classId)));
  };

  const handleSave = async (): Promise<void> => {
    if (saving) return;
    setSaving(true);
    try {
      const data = await setElectionClasses(electionId, Array.from(selected));
      applyOptions(data);
      showToast('Participating classes updated', 'success');
      onChanged();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not save selection'), 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <Spinner centered size="lg" />;
  }

  return (
    <Card
      title="Participating classes"
      actions={
        canEdit ? (
          <Button variant="primary" onClick={handleSave} disabled={!dirty || saving}>
            {saving ? <Spinner /> : 'Save selection'}
          </Button>
        ) : undefined
      }
    >
      {!canEdit && (
        <div className="banner banner-info">
          Classes can only be changed while the election is in <strong>Planned</strong> status.
        </div>
      )}

      {options.length === 0 ? (
        <EmptyState
          title="No classes defined yet"
          description="Create your classes once on the Classes page, then select them here."
          action={
            <Link to="/classes">
              <Button variant="primary">Go to Classes</Button>
            </Link>
          }
        />
      ) : (
        <>
          <p className="text-muted">
            Tick the classes that take part in this election. Token generation mints one anonymous
            QR code per student of every selected class.
          </p>
          {canEdit && (
            <label
              className="class-check-row"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '8px 4px',
                borderBottom: '2px solid #ddd',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              <input type="checkbox" checked={allSelected} onChange={toggleAll} />
              <span style={{ flex: 1 }}>{allSelected ? 'Deselect all' : 'Select all'}</span>
            </label>
          )}
          <div className="class-checklist">
            {options.map((o) => (
              <label
                key={o.classId}
                className="class-check-row"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '8px 4px',
                  borderBottom: '1px solid #eee',
                  cursor: canEdit ? 'pointer' : 'default',
                }}
              >
                <input
                  type="checkbox"
                  checked={selected.has(o.classId)}
                  onChange={() => toggle(o.classId)}
                  disabled={!canEdit}
                />
                <span style={{ flex: 1 }}>{o.name}</span>
                <span className="text-muted">{o.studentCount} students</span>
              </label>
            ))}
          </div>
          <p className="mt-12">
            <strong>{selected.size}</strong> class(es) selected ·{' '}
            <strong>{totalStudents}</strong> eligible voter(s)
          </p>
        </>
      )}
    </Card>
  );
}
