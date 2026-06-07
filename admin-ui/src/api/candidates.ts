import { apiClient } from './client';
import { Candidate, NewCandidate } from '../types';

export async function listCandidates(electionId: string): Promise<Candidate[]> {
  const response = await apiClient.get<Candidate[]>(`/api/elections/${electionId}/candidates`);
  return response.data;
}

export async function createCandidate(
  electionId: string,
  payload: NewCandidate
): Promise<Candidate> {
  const response = await apiClient.post<Candidate>(
    `/api/elections/${electionId}/candidates`,
    payload
  );
  return response.data;
}

export async function deleteCandidate(
  electionId: string,
  electionCandidateId: string
): Promise<void> {
  await apiClient.delete(`/api/elections/${electionId}/candidates/${electionCandidateId}`);
}
