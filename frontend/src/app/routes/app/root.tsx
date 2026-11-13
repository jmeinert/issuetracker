import { AppLayout } from '@/components/layouts/AppLayout';
import { Outlet } from 'react-router';

export function AppRoot() {
  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  );
}
