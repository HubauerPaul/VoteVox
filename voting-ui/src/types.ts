export interface BallotCandidate {
  id: string;
  name: string;
  className: string;
  department: string;
}

export type ElectionType = 'SCHOOL_REPRESENTATIVE' | 'CLASS_REPRESENTATIVE' | 'DEPARTMENT_REPRESENTATIVE' | string;

export interface ValidationResponse {
  electionId: string;
  electionTitle: string;
  electionType: ElectionType;
  candidates: BallotCandidate[];
}

export interface CastVoteRequest {
  token: string;
  candidateId: string;
}

export interface CastVoteResponse {
  success: boolean;
}

export interface ApiError {
  message: string;
  status: number;
}

export interface ConfirmPageState {
  token: string;
  candidate: BallotCandidate;
  election: ValidationResponse;
}
