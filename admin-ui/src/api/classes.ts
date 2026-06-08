import { apiClient } from './client';
import { ElectionClassOption, NewClass, SchoolClass } from '../types';

// ---- Global classes --------------------------------------------------------

export async function listClasses(): Promise<SchoolClass[]> {
  const response = await apiClient.get<SchoolClass[]>('/api/classes');
  return response.data;
}

export async function createClass(payload: NewClass): Promise<SchoolClass> {
  const response = await apiClient.post<SchoolClass>('/api/classes', payload);
  return response.data;
}

export async function updateClass(id: string, payload: NewClass): Promise<SchoolClass> {
  const response = await apiClient.put<SchoolClass>(`/api/classes/${id}`, payload);
  return response.data;
}

export async function deleteClass(id: string): Promise<void> {
  await apiClient.delete(`/api/classes/${id}`);
}

// ---- Per-election class selection ------------------------------------------

export async function getElectionClasses(electionId: string): Promise<ElectionClassOption[]> {
  const response = await apiClient.get<ElectionClassOption[]>(
    `/api/elections/${electionId}/classes`
  );
  return response.data;
}

export async function setElectionClasses(
  electionId: string,
  classIds: string[]
): Promise<ElectionClassOption[]> {
  const response = await apiClient.put<ElectionClassOption[]>(
    `/api/elections/${electionId}/classes`,
    { classIds }
  );
  return response.data;
}
