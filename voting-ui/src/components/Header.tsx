import { Link } from 'react-router-dom';

export default function Header(): JSX.Element {
  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link to="/" className="site-header-brand" aria-label="VoteVox home">
          <svg viewBox="0 0 64 64" fill="none" aria-hidden="true">
            <rect x="8" y="22" width="48" height="34" rx="2" fill="#ffffff" stroke="#ffffff" strokeWidth="2" />
            <rect x="22" y="14" width="20" height="12" rx="1" fill="#1e3a5f" stroke="#ffffff" strokeWidth="2" />
            <path
              d="M20 36 L28 44 L44 28"
              stroke="#1e3a5f"
              strokeWidth="4"
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          </svg>
          <span>VoteVox</span>
        </Link>
        <span className="site-header-tag">HTL Wels</span>
      </div>
    </header>
  );
}
