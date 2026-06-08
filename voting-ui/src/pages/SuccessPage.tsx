import Card from '../components/Card';

export default function SuccessPage(): JSX.Element {
  return (
    <div className="stack-lg">
      <Card>
        <div className="success-message">
          <div className="success-icon" aria-hidden="true">
            <svg viewBox="0 0 80 80" fill="none">
              <circle cx="40" cy="40" r="36" fill="#dcfce7" stroke="#16a34a" strokeWidth="3" />
              <path
                d="M24 41 L35 52 L57 30"
                stroke="#16a34a"
                strokeWidth="6"
                strokeLinecap="round"
                strokeLinejoin="round"
                fill="none"
              />
            </svg>
          </div>
          <h1>Your vote has been recorded.</h1>
          <p>Thank you for taking part in the election.</p>
          <p className="text-muted">You can now close this page.</p>
        </div>
      </Card>
    </div>
  );
}
