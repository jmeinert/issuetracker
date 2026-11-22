import { Text } from '@mantine/core';
import classes from './Logo.module.css';

type LogoProps = {
  size?: 'md' | 'xl';
};

export function Logo({ size = 'md' }: LogoProps) {
  return (
    <Text className={classes.logo} data-size={size}>
      <span>Issue</span>
      Tracker
    </Text>
  );
}
