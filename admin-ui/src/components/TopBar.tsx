import { useAuth } from '../context/AuthContext';

export function TopBar(): JSX.Element {
  const { user } = useAuth();
  return (
    <header className="top-bar">
      <div className="top-bar-title">VoteVox Administration</div>
      <div className="top-bar-spacer" />
      <div className="top-bar-user">{user?.name ?? ''}</div>
    </header>
  );
}
