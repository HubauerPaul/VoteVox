import { apiClient } from './client';
import { ElectionResults } from '../types';

export async function getResults(electionId: string): Promise<ElectionResults> {
  const response = await apiClient.get<ElectionResults>(`/api/elections/${electionId}/results`);
  return response.data;
}

export async function downloadResultsPdf(electionId: string): Promise<Blob> {
  const response = await apiClient.get(`/api/elections/${electionId}/results/pdf`, {
    responseType: 'blob',
  });
  return response.data as Blob;
}

export async function downloadResultsCsv(electionId: string): Promise<Blob> {
  const response = await apiClient.get(`/api/elections/${electionId}/results/csv`, {
    responseType: 'blob',
  });
  return response.data as Blob;
}
