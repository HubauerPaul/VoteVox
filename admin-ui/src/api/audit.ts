import { apiClient } from './client';
import { AuditPage } from '../types';

export async function fetchAuditLog(page = 0, size = 50): Promise<AuditPage> {
  const response = await apiClient.get<AuditPage>('/api/audit', {
    params: { page, size },
  });
  return response.data;
}
