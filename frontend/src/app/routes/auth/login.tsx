import { paths } from '@/config/paths';
import { LoginForm } from '@/features/auth';
import { Anchor, Text, Title } from '@mantine/core';
import { Link } from 'react-router';
import classes from './auth.module.css';

function LoginRoute() {
  return (
    <>
      <Title className={classes.title}>Login</Title>
      <LoginForm />
      <Text mt="lg">
        Not registered yet?{' '}
        <Anchor component={Link} to={paths.auth.register.getHref()}>
          Register
        </Anchor>
      </Text>
    </>
  );
}

export { LoginRoute as Component };
