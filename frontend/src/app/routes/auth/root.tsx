import { AuthLayout } from '@/components/layouts/AuthLayout';
import { Outlet } from 'react-router';

export function AuthRoot() {
  return (
    <AuthLayout>
      <Outlet />
    </AuthLayout>
  );
}
