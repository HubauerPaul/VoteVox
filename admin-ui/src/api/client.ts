import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

export const TOKEN_KEY = 'votevox_token';
export const ROLE_KEY = 'votevox_role';
export const NAME_KEY = 'votevox_name';

export const apiClient = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

type UnauthorizedHandler = () => void;

let onUnauthorized: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler;
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // 401 = no/expired session. 403 = the backend rejected the token (e.g. a
    // stale token signed with an old secret). Either way the admin must re-login,
    // so clear the session and bounce to the login page instead of leaving the
    // UI stuck showing errors. The login request itself is exempt.
    const status = error.response?.status;
    const isLoginCall = error.config?.url?.includes('/api/auth/login');
    if ((status === 401 || status === 403) && !isLoginCall) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ROLE_KEY);
      localStorage.removeItem(NAME_KEY);
      if (onUnauthorized) {
        onUnauthorized();
      }
    }
    return Promise.reject(error);
  }
);

export function extractErrorMessage(err: unknown, fallback = 'An unexpected error occurred'): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string; error?: string } | undefined;
    if (data?.message) return data.message;
    if (data?.error) return data.error;
    if (err.message) return err.message;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}

export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
