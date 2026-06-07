import axios, { AxiosError, AxiosInstance } from 'axios';
import type {
  ApiError,
  CastVoteResponse,
  ValidationResponse,
} from '../types';

const client: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

interface BackendErrorShape {
  message?: string;
  status?: number;
  error?: string;
}

function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError<BackendErrorShape>;
    const status = axiosErr.response?.status ?? 0;
    const data = axiosErr.response?.data;

    let message: string;
    if (data && typeof data === 'object' && typeof data.message === 'string') {
      message = data.message;
    } else if (data && typeof data === 'object' && typeof data.error === 'string') {
      message = data.error;
    } else if (axiosErr.message) {
      message = axiosErr.message;
    } else {
      message = 'An unexpected error occurred.';
    }

    return { message, status };
  }

  if (err instanceof Error) {
    return { message: err.message, status: 0 };
  }

  return { message: 'An unexpected error occurred.', status: 0 };
}

export async function validateToken(token: string): Promise<ValidationResponse> {
  try {
    const response = await client.post<ValidationResponse>('/api/vote/validate', {
      token,
    });
    return response.data;
  } catch (err) {
    throw toApiError(err);
  }
}

export async function castVote(
  token: string,
  candidateId: string,
): Promise<CastVoteResponse> {
  try {
    const response = await client.post<CastVoteResponse>('/api/vote/cast', {
      token,
      candidateId,
    });
    return response.data;
  } catch (err) {
    throw toApiError(err);
  }
}

export default client;
