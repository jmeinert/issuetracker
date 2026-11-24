import { LogoutButton } from '@/features/auth';
import { AppShell, Burger, Group, VisuallyHidden } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import type { ReactNode } from 'react';
import classes from './AppLayout.module.css';

export function AppLayout({ children }: { children: ReactNode }) {
  const [opened, { toggle }] = useDisclosure();

  return (
    <AppShell
      padding="md"
      header={{ height: 60 }}
      navbar={{
        width: 250,
        breakpoint: 'sm',
        collapsed: { mobile: !opened },
      }}
    >
      <AppShell.Header>
        <Group className={classes.burgerWrapper}>
          <Burger
            opened={opened}
            onClick={toggle}
            hiddenFrom="sm"
            size="sm"
            aria-expanded={opened}
          >
            <VisuallyHidden>Toggle navigation</VisuallyHidden>
          </Burger>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar
        className={classes.navbar}
        data-mobile-collapsed={!opened}
      >
        <AppShell.Section grow />
        <LogoutButton />
      </AppShell.Navbar>
      <AppShell.Main>{children}</AppShell.Main>
    </AppShell>
  );
}
