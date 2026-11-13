import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { paths } from '@/config/paths';
import { AppRoot } from './routes/app/root';
import { AuthRoot } from './routes/auth/root';

const router = createBrowserRouter([
  {
    path: '/auth',
    Component: AuthRoot,
    children: [
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
    element: <AppRoot />, // @todo: add <ProtectedRoute> later
    children: [{ index: true, lazy: () => import('./routes/app/home') }],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
