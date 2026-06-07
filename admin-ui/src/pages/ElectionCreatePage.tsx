import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { Select } from '../components/Select';
import { TextArea } from '../components/TextArea';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { ElectionType } from '../types';
import { createElection } from '../api/elections';
import { useToast } from '../components/Toast';
import { extractErrorMessage } from '../api/client';
import { fromDatetimeLocalValue } from '../utils/format';

export function ElectionCreatePage(): JSX.Element {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<ElectionType>('SCHOOL_REP');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (submitting) return;
    setError(null);

    const startIso = fromDatetimeLocalValue(startTime);
    const endIso = fromDatetimeLocalValue(endTime);

    if (!startIso || !endIso) {
      setError('Please provide valid start and end times.');
      return;
    }
    if (new Date(endIso).getTime() <= new Date(startIso).getTime()) {
      setError('End time must be after start time.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await createElection({
        title: title.trim(),
        description: description.trim(),
        type,
        startTime: startIso,
        endTime: endIso,
      });
      showToast('Election created', 'success');
      navigate(`/elections/${created.id}`);
    } catch (err) {
      const msg = extractErrorMessage(err, 'Could not create election');
      setError(msg);
      showToast(msg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="New Election"
        subtitle="Define the basics. You can add candidates and students after creating."
      />

      <Card>
        {error && (
          <div className="banner banner-error" role="alert">
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit} noValidate>
          <Input
            label="Title"
            name="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            maxLength={120}
          />
          <TextArea
            label="Description"
            name="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
          />
          <Select
            label="Type"
            name="type"
            value={type}
            onChange={(e) => setType(e.target.value as ElectionType)}
            required
          >
            <option value="SCHOOL_REP">School Representative</option>
            <option value="DEPARTMENT_REP">Department Representative</option>
            <option value="SURVEY">Survey</option>
          </Select>
          <div className="form-row">
            <Input
              label="Start Time"
              type="datetime-local"
              name="startTime"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
            />
            <Input
              label="End Time"
              type="datetime-local"
              name="endTime"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              required
            />
          </div>
          <div className="form-actions">
            <Button type="submit" variant="primary" disabled={submitting}>
              {submitting ? (
                <>
                  <Spinner /> <span style={{ marginLeft: 8 }}>Creating...</span>
                </>
              ) : (
                'Create Election'
              )}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/elections')}
              disabled={submitting}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
