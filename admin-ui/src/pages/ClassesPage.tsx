import { FormEvent, useEffect, useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { Table, TableColumn } from '../components/Table';
import { EmptyState } from '../components/EmptyState';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { SchoolClass } from '../types';
import {
  createClass,
  deleteClass,
  listClasses,
  updateClass,
} from '../api/classes';
import { useToast } from '../components/Toast';
import { extractErrorMessage } from '../api/client';

export function ClassesPage(): JSX.Element {
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [loading, setLoading] = useState(true);

  const [name, setName] = useState('');
  const [count, setCount] = useState('');
  const [adding, setAdding] = useState(false);

  const [editing, setEditing] = useState<SchoolClass | null>(null);
  const [editName, setEditName] = useState('');
  const [editCount, setEditCount] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  const [pendingDelete, setPendingDelete] = useState<SchoolClass | null>(null);
  const [deleting, setDeleting] = useState(false);

  const { showToast } = useToast();

  const load = (): void => {
    setLoading(true);
    listClasses()
      .then(setClasses)
      .catch((err: unknown) => showToast(extractErrorMessage(err, 'Could not load classes'), 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listClasses()
      .then((data) => {
        if (!cancelled) setClasses(data);
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
  }, [showToast]);

  const handleAdd = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (adding) return;
    setAdding(true);
    try {
      await createClass({ name: name.trim(), studentCount: Number.parseInt(count, 10) || 0 });
      showToast('Class added', 'success');
      setName('');
      setCount('');
      load();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not add class'), 'error');
    } finally {
      setAdding(false);
    }
  };

  const openEdit = (cls: SchoolClass): void => {
    setEditing(cls);
    setEditName(cls.name);
    setEditCount(String(cls.studentCount));
  };

  const handleEditSave = async (): Promise<void> => {
    if (!editing || savingEdit) return;
    setSavingEdit(true);
    try {
      await updateClass(editing.id, {
        name: editName.trim(),
        studentCount: Number.parseInt(editCount, 10) || 0,
      });
      showToast('Class updated', 'success');
      setEditing(null);
      load();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not update class'), 'error');
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDelete = async (): Promise<void> => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await deleteClass(pendingDelete.id);
      showToast('Class deleted', 'success');
      setPendingDelete(null);
      load();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not delete class'), 'error');
    } finally {
      setDeleting(false);
    }
  };

  const columns: TableColumn<SchoolClass>[] = [
    { key: 'name', header: 'Class', render: (row) => row.name },
    { key: 'count', header: 'Students', align: 'right', render: (row) => row.studentCount, width: '120px' },
    {
      key: 'actions',
      header: '',
      align: 'right',
      width: '160px',
      render: (row) => (
        <div className="btn-group-row" style={{ justifyContent: 'flex-end', gap: 8 }}>
          <Button variant="secondary" size="sm" onClick={() => openEdit(row)}>
            Edit
          </Button>
          <Button variant="danger" size="sm" onClick={() => setPendingDelete(row)}>
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Classes"
        subtitle="Define each class once with its student count. Elections then pick which classes take part."
      />

      <Card title="Add class">
        <form onSubmit={handleAdd} noValidate>
          <div className="inline-form">
            <Input
              label="Class name"
              name="className"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              placeholder="e.g. 4AHIT"
            />
            <Input
              label="Number of students"
              name="studentCount"
              type="number"
              min={0}
              value={count}
              onChange={(e) => setCount(e.target.value)}
              required
              placeholder="e.g. 25"
            />
            <Button type="submit" variant="primary" disabled={adding || !name.trim() || !count}>
              {adding ? <Spinner /> : 'Add'}
            </Button>
          </div>
        </form>
      </Card>

      <Card title={`All classes (${classes.length})`}>
        {loading ? (
          <Spinner centered size="lg" />
        ) : classes.length === 0 ? (
          <EmptyState
            title="No classes yet"
            description="Add your classes above. They become available to every election."
          />
        ) : (
          <Table columns={columns} data={classes} rowKey={(row) => row.id} />
        )}
      </Card>

      <Modal
        open={editing !== null}
        onClose={() => (savingEdit ? undefined : setEditing(null))}
        title="Edit class"
        footer={
          <>
            <Button variant="secondary" onClick={() => setEditing(null)} disabled={savingEdit}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleEditSave} disabled={savingEdit}>
              {savingEdit ? <Spinner /> : 'Save'}
            </Button>
          </>
        }
      >
        <Input
          label="Class name"
          name="editClassName"
          value={editName}
          onChange={(e) => setEditName(e.target.value)}
          required
        />
        <Input
          label="Number of students"
          name="editStudentCount"
          type="number"
          min={0}
          value={editCount}
          onChange={(e) => setEditCount(e.target.value)}
          required
        />
      </Modal>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete class"
        message={
          <p>
            Delete <strong>{pendingDelete?.name}</strong>? It will be removed from any election that
            uses it.
          </p>
        }
        confirmLabel="Delete"
        destructive
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </>
  );
}
