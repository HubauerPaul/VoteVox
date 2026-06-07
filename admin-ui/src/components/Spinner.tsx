interface SpinnerProps {
  size?: 'sm' | 'lg';
  centered?: boolean;
}

export function Spinner({ size = 'sm', centered = false }: SpinnerProps): JSX.Element {
  const classes = `spinner ${size === 'lg' ? 'spinner-lg' : ''}`;
  if (centered) {
    return (
      <div className="spinner-overlay">
        <span className={classes} />
      </div>
    );
  }
  return <span className={classes} />;
}
