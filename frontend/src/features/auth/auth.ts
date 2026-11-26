import { paths } from '@/config/paths';
import { api, getApiErrorMessage } from '@/lib/api-client';
import {
  type CurrentSessionResponse,
  type LoginBody,
  type RegisterRequest,
} from '@/types/api';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

export const authKeys = {
  currentUser: () => ['auth', 'currentUser'] as const,
};

async function getCurrentUser() {
  const { data } = await api.get<CurrentSessionResponse>('/auth/me');
  return data.user;
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

async function logout() {
  await api.post('/auth/logout');
}

export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      const queryKey = authKeys.currentUser();
      await queryClient.cancelQueries({
        queryKey,
        exact: true,
      });
      queryClient.setQueryData(queryKey, null);
    },
    onError: (error) => {
      notifications.show({
        color: 'red',
        title: 'Logout failed',
        message: getApiErrorMessage(error),
      });
    },
  });
}

async function register(values: RegisterRequest) {
  await api.post('/auth/register', values);
}

export function useRegister() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: register,
    onSuccess: () => navigate(paths.auth.login.getHref(), { replace: true }),
  });
}
