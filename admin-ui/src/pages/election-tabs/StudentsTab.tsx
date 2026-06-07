import { FormEvent, useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { Input } from '../../components/Input';
import { TextArea } from '../../components/TextArea';
import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { Modal } from '../../components/Modal';
import { Table, TableColumn } from '../../components/Table';
import { EmptyState } from '../../components/EmptyState';
import { ElectionStatus, NewStudent, Student } from '../../types';
import { importStudents, listStudents } from '../../api/students';
import { useToast } from '../../components/Toast';
import { extractErrorMessage } from '../../api/client';

interface StudentsTabProps {
  electionId: string;
  status: ElectionStatus;
  onChanged: () => void;
}

function parseCsv(text: string): { rows: NewStudent[]; errors: string[] } {
  const rows: NewStudent[] = [];
  const errors: string[] = [];
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
  lines.forEach((line, index) => {
    const parts = line.split(',').map((part) => part.trim());
    if (parts.length < 4) {
      errors.push(`Line ${index + 1}: expected 4 columns, got ${parts.length}`);
      return;
    }
    const [name, studentId, className, department] = parts;
    if (!name || !studentId || !className || !department) {
      errors.push(`Line ${index + 1}: missing field`);
      return;
    }
    rows.push({ name, studentId, className, department });
  });
  return { rows, errors };
}

export function StudentsTab({
  electionId,
  status,
  onChanged,
}: StudentsTabProps): JSX.Element {
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [csvText, setCsvText] = useState('');
  const [csvErrors, setCsvErrors] = useState<string[]>([]);
  const [importing, setImporting] = useState(false);

  const [name, setName] = useState('');
  const [studentId, setStudentId] = useState('');
  const [className, setClassName] = useState('');
  const [department, setDepartment] = useState('');
  const [addLoading, setAddLoading] = useState(false);

  const { showToast } = useToast();
  const canEdit = status === 'PLANNED';

  const load = (): void => {
    setLoading(true);
    listStudents(electionId)
      .then(setStudents)
      .catch((err: unknown) => showToast(extractErrorMessage(err, 'Could not load students'), 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listStudents(electionId)
      .then((data) => {
        if (!cancelled) setStudents(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) showToast(extractErrorMessage(err, 'Could not load students'), 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [electionId, showToast]);

  const handleImport = async (): Promise<void> => {
    if (importing) return;
    const { rows, errors } = parseCsv(csvText);
    if (errors.length > 0) {
      setCsvErrors(errors);
      return;
    }
    if (rows.length === 0) {
      setCsvErrors(['No valid rows found.']);
      return;
    }
    setCsvErrors([]);
    setImporting(true);
    try {
      await importStudents(electionId, rows);
      showToast(`${rows.length} student(s) imported`, 'success');
      setCsvText('');
      setModalOpen(false);
      load();
      onChanged();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Import failed'), 'error');
    } finally {
      setImporting(false);
    }
  };

  const handleAddSingle = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (addLoading) return;
    setAddLoading(true);
    try {
      await importStudents(electionId, [
        {
          name: name.trim(),
          studentId: studentId.trim(),
          className: className.trim(),
          department: department.trim(),
        },
      ]);
      showToast('Student added', 'success');
      setName('');
      setStudentId('');
      setClassName('');
      setDepartment('');
      load();
      onChanged();
    } catch (err) {
      showToast(extractErrorMessage(err, 'Could not add student'), 'error');
    } finally {
      setAddLoading(false);
    }
  };

  const columns: TableColumn<Student>[] = [
    { key: 'studentId', header: 'Student ID', render: (row) => row.studentId, width: '140px' },
    { key: 'name', header: 'Name', render: (row) => row.name },
    { key: 'className', header: 'Class', render: (row) => row.className },
    { key: 'department', header: 'Department', render: (row) => row.department },
  ];

  return (
    <>
      {canEdit && (
        <Card
          title="Add students"
          actions={
            <Button variant="primary" onClick={() => setModalOpen(true)}>
              Import (CSV)
            </Button>
          }
        >
          <form onSubmit={handleAddSingle} noValidate>
            <div className="inline-form">
              <Input
                label="Name"
                name="studentName"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <Input
                label="Student ID"
                name="studentId"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                required
              />
              <Input
                label="Class"
                name="studentClass"
                value={className}
                onChange={(e) => setClassName(e.target.value)}
                required
              />
              <Input
                label="Department"
                name="studentDepartment"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                required
              />
              <Button type="submit" variant="secondary" disabled={addLoading}>
                {addLoading ? <Spinner /> : 'Add'}
              </Button>
            </div>
          </form>
        </Card>
      )}

      <Card title={`Eligible students (${students.length})`}>
        {!canEdit && (
          <div className="banner banner-info">
            Students can only be added while the election is in <strong>Planned</strong> status.
          </div>
        )}
        {loading ? (
          <Spinner centered size="lg" />
        ) : students.length === 0 ? (
          <EmptyState
            title="No students added"
            description={
              canEdit
                ? 'Add students individually or via CSV import.'
                : 'There are no eligible students registered for this election.'
            }
          />
        ) : (
          <Table columns={columns} data={students} rowKey={(row) => row.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => (importing ? undefined : setModalOpen(false))}
        title="Import students (CSV)"
        size="lg"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => setModalOpen(false)}
              disabled={importing}
            >
              Cancel
            </Button>
            <Button variant="primary" onClick={handleImport} disabled={importing}>
              {importing ? <Spinner /> : 'Import'}
            </Button>
          </>
        }
      >
        <p className="text-muted">
          Paste one student per line with columns:{' '}
          <code>name,studentId,className,department</code>
        </p>
        <TextArea
          name="csv"
          value={csvText}
          onChange={(e) => setCsvText(e.target.value)}
          rows={10}
          placeholder={`Max Mustermann,2025001,4AHITS,Informatik\nAnna Beispiel,2025002,4BHITS,Informatik`}
        />
        {csvErrors.length > 0 && (
          <div className="banner banner-error" role="alert">
            <strong>Errors:</strong>
            <ul style={{ margin: '4px 0 0 18px', padding: 0 }}>
              {csvErrors.map((err, i) => (
                <li key={i}>{err}</li>
              ))}
            </ul>
          </div>
        )}
      </Modal>
    </>
  );
}
