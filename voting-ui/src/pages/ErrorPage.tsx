import { useNavigate, useSearchParams } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';

function messageForStatus(status: number, fallback: string): { title: string; body: string } {
  switch (status) {
    case 404:
      return {
        title: 'Invalid QR code',
        body: 'This QR code is not valid. Please make sure you scanned the correct code.',
      };
    case 410:
      return {
        title: 'Code already used',
        body: 'This QR code has already been used. Each voter receives a single one-time code.',
      };
    case 409:
      return {
        title: 'Voting closed',
        body: 'Voting is not currently open. Please try again during the election period.',
      };
    case 401:
    case 403:
      return {
        title: 'Not authorised',
        body: 'You are not authorised to use this voting code.',
      };
    default:
      return {
        title: 'Something went wrong',
        body: fallback || 'An unexpected error occurred. Please try again.',
      };
  }
}

export default function ErrorPage(): JSX.Element {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const statusParam = searchParams.get('status');
  const messageParam = searchParams.get('message') ?? '';
  const status = statusParam ? Number.parseInt(statusParam, 10) : 0;
  const { title, body } = messageForStatus(Number.isFinite(status) ? status : 0, messageParam);

  return (
    <div className="stack-lg">
      <Card>
        <div className="error-message">
          <div className="error-icon" aria-hidden="true">
            <svg viewBox="0 0 64 64" fill="none">
              <circle cx="32" cy="32" r="28" fill="#fee2e2" stroke="#dc2626" strokeWidth="3" />
              <path
                d="M32 18 V36"
                stroke="#dc2626"
                strokeWidth="4"
                strokeLinecap="round"
              />
              <circle cx="32" cy="44" r="3" fill="#dc2626" />
            </svg>
          </div>
          <h1>{title}</h1>
          <p>{body}</p>
        </div>

        <div className="btn-group">
          <Button block onClick={() => navigate('/', { replace: true })}>
            Scan another code
          </Button>
        </div>
      </Card>
    </div>
  );
}
