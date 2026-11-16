import { create, isAxiosError } from 'axios';

export const api = create({
  baseURL: '/api',
  timeout: 15_000,
  headers: { Accept: 'application/json' },
});

export function getApiErrorMessage(error: unknown): string {
  const fallback = 'Something went wrong. Please try again.';

  if (!isAxiosError(error)) {
    return fallback;
  }

  if (error.code === 'ECONNABORTED') {
    return 'The request timed out. Please try again.';
  }

  if (!error.response) {
    return 'Unable to reach server. Please try again.';
  }

  if (error.response.status >= 500) {
    return fallback;
  }

  const { data } = error.response;

  if (
    typeof data === 'object' &&
    data !== null &&
    'message' in data &&
    typeof data.message === 'string' &&
    data.message.trim() !== ''
  ) {
    return data.message;
  }

  return fallback;
}
