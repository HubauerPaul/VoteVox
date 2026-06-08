import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { validateToken } from '../api/client';
import Card from '../components/Card';
import Button from '../components/Button';
import Spinner from '../components/Spinner';
import type { ApiError, BallotCandidate, ValidationResponse } from '../types';

interface VotePageLocationState {
  preselectedCandidateId?: string;
}

function formatElectionType(type: string): string {
  return type
    .toLowerCase()
    .split('_')
    .map((part) => (part.length === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)))
    .join(' ');
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

export default function VotePage(): JSX.Element {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [election, setElection] = useState<ValidationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      navigate('/error?message=Missing%20voting%20code.%20Please%20scan%20again.', {
        replace: true,
      });
      return;
    }

    let cancelled = false;

    async function load(): Promise<void> {
      setLoading(true);
      try {
        const data = await validateToken(token);
        if (cancelled) {
          return;
        }
        setElection(data);

        const state = location.state as VotePageLocationState | null;
        if (state?.preselectedCandidateId) {
          const stillExists = data.candidates.some(
            (c) => c.id === state.preselectedCandidateId,
          );
          if (stillExists) {
            setSelectedId(state.preselectedCandidateId);
          }
        }
      } catch (err) {
        if (cancelled) {
          return;
        }
        const apiError: ApiError = isApiError(err)
          ? err
          : { message: 'Unable to validate your code.', status: 0 };
        const params = new URLSearchParams({
          message: apiError.message,
          status: String(apiError.status),
        });
        navigate(`/error?${params.toString()}`, { replace: true });
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [token, navigate, location.state]);

  function handleContinue(): void {
    if (!election || !selectedId) {
      return;
    }
    const candidate = election.candidates.find((c) => c.id === selectedId);
    if (!candidate) {
      return;
    }
    navigate('/confirm', {
      state: {
        token,
        candidate,
        election,
      },
    });
  }

  if (loading || !election) {
    return (
      <Card>
        <Spinner label="Validating your voting code..." />
      </Card>
    );
  }

  return (
    <div className="stack-lg">
      <div className="election-meta">
        <span className="type-tag">{formatElectionType(election.electionType)}</span>
        <h1>{election.electionTitle}</h1>
        <p className="text-muted">Select one candidate, then continue.</p>
      </div>

      <div
        className="candidate-list"
        role="radiogroup"
        aria-label="Candidates"
      >
        {election.candidates.map((candidate: BallotCandidate) => {
          const selected = candidate.id === selectedId;
          return (
            <button
              key={candidate.id}
              type="button"
              role="radio"
              aria-checked={selected}
              className={`candidate-card${selected ? ' selected' : ''}`}
              onClick={() => setSelectedId(candidate.id)}
            >
              <span className="candidate-radio" aria-hidden="true" />
              <span className="candidate-info">
                <span className="candidate-name">{candidate.name}</span>
                <span className="candidate-details">
                  {candidate.className} &mdash; {candidate.department}
                </span>
              </span>
            </button>
          );
        })}
      </div>

      <Button block disabled={!selectedId} onClick={handleContinue}>
        Continue
      </Button>
    </div>
  );
}
