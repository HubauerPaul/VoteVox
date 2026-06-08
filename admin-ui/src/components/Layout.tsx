import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';

export function Layout(): JSX.Element {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="main-area">
        <TopBar />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
