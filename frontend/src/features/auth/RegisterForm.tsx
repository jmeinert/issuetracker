import { getApiErrorMessage } from '@/lib/api-client';
import { type ErrorResponse } from '@/types/api';
import { Alert, Button, PasswordInput, Stack, TextInput } from '@mantine/core';
import { hasLength, isNotEmpty, useForm } from '@mantine/form';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useRegister } from './auth';

export function RegisterForm() {
  const register = useRegister();

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: {
      email: '',
      password: '',
      displayName: '',
    },
    validate: {
      email: (value) =>
        isNotEmpty('Email is required')(value) ??
        hasLength({ max: 255 }, 'Email must be 255 characters or less')(value),
      password: (value) =>
        isNotEmpty('Password is required')(value) ??
        (value.length < 15 || value.length > 128
          ? 'Password must be between 15 and 128 characters'
          : null),
      displayName: (value) =>
        isNotEmpty('Name is required')(value) ??
        hasLength({ max: 50 }, 'Name must be 50 characters or less')(value),
    },
  });

  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = form.onSubmit((values) => {
    setFormError(null);

    register.mutate(values, {
      onError: (error) => {
        if (
          isAxiosError<ErrorResponse>(error) &&
          error.response?.status === 400
        ) {
          const fieldErrors = error.response.data?.errors;

          if (fieldErrors && Object.keys(fieldErrors).length > 0) {
            form.setErrors(fieldErrors);
            return;
          }
        }

        setFormError(getApiErrorMessage(error));
      },
    });
  });

  return (
    <form noValidate onSubmit={handleSubmit}>
      <Stack>
        {formError && (
          <Alert color="red" role="alert">
            {formError}
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
          autoComplete="new-password"
          key={form.key('password')}
          {...form.getInputProps('password')}
          required
        />

        <TextInput
          label="Name"
          autoComplete="name"
          key={form.key('displayName')}
          {...form.getInputProps('displayName')}
          required
        />

        <Button type="submit" loading={register.isPending} mt="sm">
          Register
        </Button>
      </Stack>
    </form>
  );
}
