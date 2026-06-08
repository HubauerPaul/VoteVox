interface SpinnerProps {
  label?: string;
}

export default function Spinner({ label = 'Loading...' }: SpinnerProps): JSX.Element {
  return (
    <div className="spinner-block" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
