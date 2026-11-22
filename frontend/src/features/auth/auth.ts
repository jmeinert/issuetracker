import { api } from '@/lib/api-client';
import { type CurrentUserResponse, type LoginBody } from '@/types/api';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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

async function login(values: LoginBody) {
  await api.post('/auth/login', new URLSearchParams(values));
}

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: login,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: authKeys.currentUser(),
      }),
  });
}
