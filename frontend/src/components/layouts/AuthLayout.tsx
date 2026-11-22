import { Container } from '@mantine/core';
import type { ReactNode } from 'react';
import { Logo } from '../ui/Logo';

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main>
      <Container size="xs" mt="4rem">
        <Logo size="xl" />
        {children}
      </Container>
    </main>
  );
}
