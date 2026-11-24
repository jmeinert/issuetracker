import { Button } from '@mantine/core';
import { useLogout } from './auth';

export function LogoutButton() {
  const logout = useLogout();

  return (
    <Button onClick={() => logout.mutate()} loading={logout.isPending}>
      Logout
    </Button>
  );
}
