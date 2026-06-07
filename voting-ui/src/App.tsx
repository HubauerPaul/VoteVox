import { Outlet } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';

export default function App(): JSX.Element {
  return (
    <div className="app-shell">
      <Header />
      <main className="app-main" role="main">
        <div className="app-container">
          <Outlet />
        </div>
      </main>
      <Footer />
    </div>
  );
}
