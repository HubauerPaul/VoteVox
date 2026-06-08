import { apiClient } from './client';
import { LoginRequest, LoginResponse } from '../types';

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/api/auth/login', payload);
  return response.data;
}
