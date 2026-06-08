import { createBrowserRouter, Navigate } from 'react-router-dom';
import App from './App';
import ScanPage from './pages/ScanPage';
import VotePage from './pages/VotePage';
import ConfirmPage from './pages/ConfirmPage';
import SuccessPage from './pages/SuccessPage';
import ErrorPage from './pages/ErrorPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <ScanPage /> },
      { path: 'vote', element: <VotePage /> },
      { path: 'confirm', element: <ConfirmPage /> },
      { path: 'success', element: <SuccessPage /> },
      { path: 'error', element: <ErrorPage /> },
      { path: '*', element: <Navigate to="/error?message=Page%20not%20found" replace /> },
    ],
  },
]);
