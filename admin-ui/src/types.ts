export type ElectionType = 'SCHOOL_REP' | 'DEPARTMENT_REP' | 'SURVEY';

export type ElectionStatus = 'PLANNED' | 'RUNNING' | 'FINISHED';

export interface ElectionSummary {
  id: string;
  title: string;
  description: string;
  type: ElectionType;
  startTime: string;
  endTime: string;
  status: ElectionStatus;
  createdAt?: string;
  candidateCount: number;
  studentCount: number;
  voteCount: number;
}

export interface ElectionDetail extends ElectionSummary {
  candidates?: Candidate[];
  students?: Student[];
  tokensGeneratedAt?: string | null;
}

export interface Candidate {
  electionCandidateId: string;
  candidateId: string;
  name: string;
  className: string;
  department: string;
}

export interface Student {
  id: string;
  name: string;
  studentId: string;
  className: string;
  department: string;
}

export interface NewCandidate {
  name: string;
  className: string;
  department: string;
}

export interface NewStudent {
  name: string;
  studentId: string;
  className: string;
  department: string;
}

export interface NewElectionPayload {
  title: string;
  description: string;
  type: ElectionType;
  startTime: string;
  endTime: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: string;
  name: string;
}

export interface GeneratedToken {
  studentId: string;
  studentName: string;
  studentExternalId?: string;
  tokenPlaintext: string;
}

export interface ResultCandidate {
  candidateId: string;
  name: string;
  className?: string;
  department?: string;
  votes: number;
  percentage: number;
}

export interface ResultBreakdownEntry {
  label: string;
  count: number;
}

export interface ElectionResults {
  electionId?: string;
  electionTitle?: string;
  electionType?: ElectionType;
  status?: ElectionStatus;
  generatedAt?: string;
  candidates: ResultCandidate[];
  totalVotes: number;
  byClass: ResultBreakdownEntry[];
  byDepartment: ResultBreakdownEntry[];
}

export interface AuditEntry {
  id: string;
  timestamp: string;
  userType: string;
  userId: string | null;
  actionType: string;
  details: string;
}

export interface AuditPage {
  content: AuditEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface AuthUser {
  name: string;
  role: string;
}

export type ToastType = 'success' | 'error' | 'warning';

export interface ToastItem {
  id: number;
  message: string;
  type: ToastType;
}
