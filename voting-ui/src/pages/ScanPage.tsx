import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import Card from '../components/Card';
import Button from '../components/Button';
import ErrorBanner from '../components/ErrorBanner';

const QR_READER_ID = 'qr-reader';

function extractToken(decodedText: string): string | null {
  const trimmed = decodedText.trim();
  if (!trimmed) {
    return null;
  }

  // Case 1: full URL containing token query param
  try {
    const maybeUrl = new URL(trimmed);
    const tokenParam = maybeUrl.searchParams.get('token');
    if (tokenParam) {
      return tokenParam;
    }
  } catch {
    // not a URL — fall through
  }

  // Case 2: raw query string starting with ?token=
  if (trimmed.includes('token=')) {
    const match = trimmed.match(/token=([^&\s]+)/);
    if (match && match[1]) {
      return decodeURIComponent(match[1]);
    }
  }

  // Case 3: treat the entire scan as the token
  return trimmed;
}

export default function ScanPage(): JSX.Element {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [scannerReady, setScannerReady] = useState(false);
  const [showManual, setShowManual] = useState(false);
  const [manualToken, setManualToken] = useState('');

  const scannerRef = useRef<Html5Qrcode | null>(null);
  const navigatedRef = useRef(false);

  // Auto-redirect if token is already in the URL
  useEffect(() => {
    const presetToken = searchParams.get('token');
    if (presetToken) {
      navigate(`/vote?token=${encodeURIComponent(presetToken)}`, { replace: true });
    }
  }, [searchParams, navigate]);

  useEffect(() => {
    let cancelled = false;

    async function startScanner(): Promise<void> {
      try {
        const element = document.getElementById(QR_READER_ID);
        if (!element) {
          return;
        }

        const scanner = new Html5Qrcode(QR_READER_ID, {
          formatsToSupport: [Html5QrcodeSupportedFormats.QR_CODE],
          verbose: false,
        });
        scannerRef.current = scanner;

        await scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 250 } },
          (decodedText) => {
            if (navigatedRef.current) {
              return;
            }
            const token = extractToken(decodedText);
            if (!token) {
              return;
            }
            navigatedRef.current = true;
            // stop scanner before navigation; do not await to avoid blocking
            void scanner.stop().catch(() => undefined);
            navigate(`/vote?token=${encodeURIComponent(token)}`);
          },
          () => {
            // ignore per-frame decode failures
          },
        );

        if (!cancelled) {
          setScannerReady(true);
        }
      } catch (err) {
        const message =
          err instanceof Error
            ? err.message
            : 'Unable to access the camera.';
        if (!cancelled) {
          setCameraError(message);
          setShowManual(true);
        }
      }
    }

    void startScanner();

    return () => {
      cancelled = true;
      const scanner = scannerRef.current;
      if (scanner) {
        scanner
          .stop()
          .then(() => scanner.clear())
          .catch(() => undefined);
      }
    };
  }, [navigate]);

  function handleManualSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    const token = manualToken.trim();
    if (!token) {
      return;
    }
    navigatedRef.current = true;
    navigate(`/vote?token=${encodeURIComponent(token)}`);
  }

  return (
    <div className="stack-lg">
      <div className="scan-intro">
        <h1>VoteVox</h1>
        <p>School Representative Election</p>
      </div>

      <Card>
        <div className="qr-frame">
          <div id={QR_READER_ID} />
        </div>
        <p className="qr-instruction">
          {scannerReady
            ? 'Position your QR code in the frame'
            : cameraError
              ? 'Camera unavailable.'
              : 'Starting camera...'}
        </p>

        {cameraError ? (
          <div style={{ marginTop: 12 }}>
            <ErrorBanner
              title="Camera not available"
              message={`${cameraError} You can still enter your voting code manually below.`}
            />
          </div>
        ) : null}

        <div className="scan-divider">or</div>

        {!showManual ? (
          <button
            type="button"
            className="manual-toggle"
            onClick={() => setShowManual(true)}
          >
            Enter code manually
          </button>
        ) : (
          <form className="manual-form" onSubmit={handleManualSubmit}>
            <label htmlFor="token-input">Voting code</label>
            <input
              id="token-input"
              type="text"
              autoComplete="off"
              autoCapitalize="off"
              autoCorrect="off"
              spellCheck={false}
              value={manualToken}
              onChange={(event) => setManualToken(event.target.value)}
              placeholder="Paste or type your code"
            />
            <Button type="submit" block disabled={!manualToken.trim()}>
              Continue
            </Button>
          </form>
        )}
      </Card>
    </div>
  );
}
