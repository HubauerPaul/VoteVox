import { apiClient } from './client';
import { GeneratedToken } from '../types';

export async function generateTokens(electionId: string): Promise<GeneratedToken[]> {
  const response = await apiClient.post<GeneratedToken[]>(
    `/api/elections/${electionId}/tokens/generate`
  );
  return response.data;
}

/**
 * Removes all tokens for the election (PLANNED only) so a fresh set can be
 * issued. Used when the one-time QR PDF was lost before the election started.
 */
export async function resetTokens(electionId: string): Promise<void> {
  await apiClient.delete(`/api/elections/${electionId}/tokens`);
}
