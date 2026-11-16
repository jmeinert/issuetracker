import { ErrorFallback, LoadingFallback } from '@/components/fallbacks';
import { AuthLayout } from '@/components/layouts/AuthLayout';
import { paths } from '@/config/paths';
import { useCurrentUser } from '@/features/auth';
import { Navigate, Outlet } from 'react-router';

export function AuthRoot() {
  const { isPending, error, user } = useCurrentUser();

  if (isPending) {
    return <LoadingFallback />;
  }

  if (error && user === undefined) {
    return <ErrorFallback error={error} />;
  }

  if (user) {
    return <Navigate to={paths.app.home.getHref()} replace />;
  }

  return (
    <AuthLayout>
      <Outlet />
    </AuthLayout>
  );
}
