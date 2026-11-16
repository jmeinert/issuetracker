import { Center } from '@mantine/core';
import type { ReactNode } from 'react';
import classes from './FullPageCenter.module.css';

export function FullPageCenter({ children }: { children: ReactNode }) {
  return <Center className={classes.fullPageCenter}>{children}</Center>;
}
