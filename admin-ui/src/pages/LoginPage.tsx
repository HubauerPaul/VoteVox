import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Input } from '../components/Input';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { extractErrorMessage } from '../api/client';

interface LocationState {
  from?: string;
}

export function LoginPage(): JSX.Element {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      const state = location.state as LocationState | null;
      const target = state?.from && state.from !== '/login' ? state.from : '/';
      navigate(target, { replace: true });
    }
  }, [isAuthenticated, navigate, location.state]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      await login(email, password);
      const state = location.state as LocationState | null;
      const target = state?.from && state.from !== '/login' ? state.from : '/';
      navigate(target, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, 'Invalid email or password'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="auth-brand">
          <h1>VoteVox</h1>
          <p>Administration</p>
        </div>

        {error && (
          <div className="banner banner-error" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <Input
            label="Email"
            type="email"
            name="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Input
            label="Password"
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Button type="submit" variant="primary" disabled={loading} style={{ width: '100%' }}>
            {loading ? (
              <>
                <Spinner /> <span style={{ marginLeft: 8 }}>Signing in...</span>
              </>
            ) : (
              'Sign in'
            )}
          </Button>
        </form>

        <div className="auth-hint">
          Default credentials: <code>admin@votevox.at</code> / <code>Admin1234!</code> (change after
          first login)
        </div>
      </div>
    </div>
  );
}
