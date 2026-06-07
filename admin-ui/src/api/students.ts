import { apiClient } from './client';
import { NewStudent, Student } from '../types';

export async function listStudents(electionId: string): Promise<Student[]> {
  const response = await apiClient.get<Student[]>(`/api/elections/${electionId}/students`);
  return response.data;
}

export async function importStudents(
  electionId: string,
  students: NewStudent[]
): Promise<void> {
  await apiClient.post(`/api/elections/${electionId}/students`, { students });
}
