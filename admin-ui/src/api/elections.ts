import { apiClient } from './client';
import { ElectionDetail, ElectionSummary, NewElectionPayload } from '../types';

export async function listElections(): Promise<ElectionSummary[]> {
  const response = await apiClient.get<ElectionSummary[]>('/api/elections');
  return response.data;
}

export async function getElection(id: string): Promise<ElectionDetail> {
  const response = await apiClient.get<ElectionDetail>(`/api/elections/${id}`);
  return response.data;
}

export async function createElection(payload: NewElectionPayload): Promise<ElectionSummary> {
  const response = await apiClient.post<ElectionSummary>('/api/elections', payload);
  return response.data;
}

export async function updateElection(
  id: string,
  payload: NewElectionPayload
): Promise<ElectionDetail> {
  const response = await apiClient.put<ElectionDetail>(`/api/elections/${id}`, payload);
  return response.data;
}

export async function deleteElection(id: string): Promise<void> {
  await apiClient.delete(`/api/elections/${id}`);
}

export async function startElection(id: string): Promise<ElectionDetail> {
  const response = await apiClient.post<ElectionDetail>(`/api/elections/${id}/start`);
  return response.data;
}

export async function stopElection(id: string): Promise<ElectionDetail> {
  const response = await apiClient.post<ElectionDetail>(`/api/elections/${id}/stop`);
  return response.data;
}
