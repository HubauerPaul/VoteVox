interface ErrorBannerProps {
  title?: string;
  message: string;
}

export default function ErrorBanner({
  title = 'Something went wrong',
  message,
}: ErrorBannerProps): JSX.Element {
  return (
    <div className="error-banner" role="alert">
      <div className="error-banner-title">{title}</div>
      <div>{message}</div>
    </div>
  );
}
