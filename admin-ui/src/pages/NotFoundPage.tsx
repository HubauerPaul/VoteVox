import { Link } from 'react-router-dom';
import { Card } from '../components/Card';

export function NotFoundPage(): JSX.Element {
  return (
    <div className="auth-layout">
      <Card>
        <h2>Page not found</h2>
        <p className="text-muted">The page you are looking for does not exist.</p>
        <Link to="/" className="btn-link">
          Return to dashboard
        </Link>
      </Card>
    </div>
  );
}
