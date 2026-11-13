import { create } from 'axios';

export const api = create({
  baseURL: '/api',
  timeout: 15_000,
  headers: { Accept: 'application/json' },
});
