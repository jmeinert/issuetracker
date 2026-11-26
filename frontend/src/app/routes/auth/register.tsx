import { paths } from '@/config/paths';
import { RegisterForm } from '@/features/auth';
import { Anchor, Text, Title } from '@mantine/core';
import { Link } from 'react-router';
import classes from './auth.module.css';

function RegisterRoute() {
  return (
    <>
      <Title className={classes.title}>Register</Title>
      <RegisterForm />
      <Text mt="lg">
        Already registered?{' '}
        <Anchor component={Link} to={paths.auth.login.getHref()}>
          Login
        </Anchor>
      </Text>
    </>
  );
}

export { RegisterRoute as Component };
