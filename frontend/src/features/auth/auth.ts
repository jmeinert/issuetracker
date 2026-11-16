import { type CurrentUserResponse } from '@/types/api';
import { api } from '@/lib/api-client';
import { useQuery } from '@tanstack/react-query';
import { isAxiosError } from 'axios';

export const authKeys = {
  currentUser: () => ['auth', 'currentUser'] as const,
};

async function getCurrentUser() {
  try {
    const { data } = await api.get<CurrentUserResponse>('/auth/me');
    return data;
  } catch (error) {
    // User is not logged in
    if (isAxiosError(error) && error.response?.status === 401) {
      return null;
    }

    throw error;
  }
}

export function useCurrentUser() {
  const { isPending, error, data } = useQuery({
    queryKey: authKeys.currentUser(),
    queryFn: getCurrentUser,
  });

  return {
    isPending,
    error,
    user: data,
  };
}
