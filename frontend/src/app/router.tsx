import { createBrowserRouter, replace } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { paths } from '@/config/paths';
import { AppRoot } from './routes/app/root';
import { AuthRoot } from './routes/auth/root';
import { LoadingFallback } from '@/components/fallbacks';

const router = createBrowserRouter([
  {
    path: '/auth',
    Component: AuthRoot,
    HydrateFallback: LoadingFallback,
    children: [
      {
        index: true,
        loader: () => replace(paths.auth.login.getHref()),
        Component: () => null,
      },
      {
        path: paths.auth.register.path,
        lazy: () => import('./routes/auth/register'),
      },
      {
        path: paths.auth.login.path,
        lazy: () => import('./routes/auth/login'),
      },
    ],
  },
  {
    path: '/',
    Component: AppRoot,
    HydrateFallback: LoadingFallback,
    children: [{ index: true, lazy: () => import('./routes/app/home') }],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
