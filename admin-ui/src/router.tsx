import { Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ElectionsPage } from './pages/ElectionsPage';
import { ElectionCreatePage } from './pages/ElectionCreatePage';
import { ElectionDetailPage } from './pages/ElectionDetailPage';
import { ClassesPage } from './pages/ClassesPage';
import { AuditPage } from './pages/AuditPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';

export function AppRouter(): JSX.Element {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/elections" element={<ElectionsPage />} />
        <Route path="/elections/new" element={<ElectionCreatePage />} />
        <Route path="/elections/:id" element={<ElectionDetailPage />} />
        <Route path="/classes" element={<ClassesPage />} />
        <Route path="/audit" element={<AuditPage />} />
      </Route>
      <Route path="/404" element={<NotFoundPage />} />
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  );
}
