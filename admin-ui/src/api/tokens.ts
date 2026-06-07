import { apiClient } from './client';
import { GeneratedToken } from '../types';

export async function generateTokens(electionId: string): Promise<GeneratedToken[]> {
  const response = await apiClient.post<GeneratedToken[]>(
    `/api/elections/${electionId}/tokens/generate`
  );
  return response.data;
}

export async function downloadTokenPdf(electionId: string): Promise<Blob> {
  const response = await apiClient.get(`/api/elections/${electionId}/tokens/pdf`, {
    responseType: 'blob',
  });
  return response.data as Blob;
}
