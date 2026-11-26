import { getApiErrorMessage } from '@/lib/api-client';
import { Alert, Button, PasswordInput, Stack, TextInput } from '@mantine/core';
import { hasLength, isNotEmpty, useForm } from '@mantine/form';
import { useLogin } from './auth';

export function LoginForm() {
  const login = useLogin();

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: {
      email: '',
      password: '',
    },
    validate: {
      email: (value) =>
        isNotEmpty('Email is required')(value) ??
        hasLength({ max: 255 }, 'Email must be 255 characters or less')(value),
      password: isNotEmpty('Password is required'),
    },
  });

  return (
    <form noValidate onSubmit={form.onSubmit((values) => login.mutate(values))}>
      <Stack>
        {login.isError && (
          <Alert color="red" role="alert">
            {getApiErrorMessage(login.error)}
          </Alert>
        )}

        <TextInput
          type="email"
          label="Email"
          autoComplete="username"
          key={form.key('email')}
          {...form.getInputProps('email')}
          required
        />

        <PasswordInput
          label="Password"
          autoComplete="current-password"
          key={form.key('password')}
          {...form.getInputProps('password')}
          required
        />

        <Button type="submit" loading={login.isPending} mt="sm">
          Login
        </Button>
      </Stack>
    </form>
  );
}
