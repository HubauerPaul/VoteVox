import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Sidebar(): JSX.Element {
  const { user, logout } = useAuth();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="brand-mark">V</span>
        VoteVox
      </div>
      <nav className="sidebar-nav">
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        <NavLink to="/elections">Elections</NavLink>
        <NavLink to="/classes">Classes</NavLink>
        <NavLink to="/audit">Audit Log</NavLink>
      </nav>
      <div className="sidebar-footer">
        <div className="sidebar-user">{user?.name ?? 'Administrator'}</div>
        <div className="sidebar-user-role">{user?.role ?? 'ADMIN'}</div>
        <button type="button" className="sidebar-signout" onClick={logout}>
          Sign out
        </button>
      </div>
    </aside>
  );
}
