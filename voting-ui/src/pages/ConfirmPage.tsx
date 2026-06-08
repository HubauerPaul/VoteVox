import { useEffect, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { castVote } from '../api/client';
import Card from '../components/Card';
import Button from '../components/Button';
import Spinner from '../components/Spinner';
import ErrorBanner from '../components/ErrorBanner';
import type { ApiError, ConfirmPageState } from '../types';

function isConfirmState(value: unknown): value is ConfirmPageState {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const v = value as Partial<ConfirmPageState>;
  return (
    typeof v.token === 'string' &&
    typeof v.candidate === 'object' &&
    v.candidate !== null &&
    typeof v.election === 'object' &&
    v.election !== null
  );
}

function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'message' in value &&
    'status' in value &&
    typeof (value as { message: unknown }).message === 'string' &&
    typeof (value as { status: unknown }).status === 'number'
  );
}

export default function ConfirmPage(): JSX.Element {
  const navigate = useNavigate();
  const location = useLocation();
  const state = isConfirmState(location.state) ? location.state : null;

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const token = state?.token ?? '';
  const candidateId = state?.candidate.id ?? '';

  // Reset error if the selection changes
  useEffect(() => {
    setSubmitError(null);
  }, [token, candidateId]);

  if (!state) {
    return <Navigate to="/" replace />;
  }

  const { candidate, election } = state;

  async function handleSubmit(): Promise<void> {
    if (!state) {
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    try {
      await castVote(state.token, state.candidate.id);
      navigate('/success', { replace: true });
    } catch (err) {
      const apiError: ApiError = isApiError(err)
        ? err
        : { message: 'Failed to submit your vote. Please try again.', status: 0 };

      // Hard errors (token consumed / invalid / election closed) should send to ErrorPage
      if ([404, 409, 410, 401, 403].includes(apiError.status)) {
        const params = new URLSearchParams({
          message: apiError.message,
          status: String(apiError.status),
        });
        navigate(`/error?${params.toString()}`, { replace: true });
        return;
      }

      setSubmitError(apiError.message);
      setSubmitting(false);
    }
  }

  function handleGoBack(): void {
    navigate(`/vote?token=${encodeURIComponent(token)}`, {
      state: { preselectedCandidateId: candidate.id },
    });
  }

  if (submitting) {
    return (
      <Card>
        <Spinner label="Submitting your vote..." />
      </Card>
    );
  }

  return (
    <div className="stack-lg">
      <div>
        <h1>Confirm your vote</h1>
        <p className="text-muted">{election.electionTitle}</p>
      </div>

      <Card>
        <div className="confirm-selection">
          <div className="label">You selected</div>
          <div className="name">{candidate.name}</div>
          <div className="meta">
            {candidate.className} &mdash; {candidate.department}
          </div>
        </div>

        <div className="warning-box">
          Once submitted, your vote cannot be changed.
        </div>

        {submitError ? (
          <div style={{ marginBottom: 16 }}>
            <ErrorBanner message={submitError} />
          </div>
        ) : null}

        <div className="btn-group btn-group-row">
          <Button variant="secondary" onClick={handleGoBack}>
            Go back
          </Button>
          <Button onClick={() => void handleSubmit()}>Submit vote</Button>
        </div>
      </Card>
    </div>
  );
}
