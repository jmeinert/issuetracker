import { ErrorFallback, LoadingFallback } from '@/components/fallbacks';
import { AppLayout } from '@/components/layouts/AppLayout';
import { paths } from '@/config/paths';
import { useCurrentUser } from '@/features/auth';
import { Navigate, Outlet } from 'react-router';

export function AppRoot() {
  const { isPending, error, user } = useCurrentUser();

  if (isPending) {
    return <LoadingFallback />;
  }

  if (error && user === undefined) {
    return <ErrorFallback error={error} />;
  }

  if (!user) {
    return <Navigate to={paths.auth.login.getHref()} replace />;
  }

  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  );
}
