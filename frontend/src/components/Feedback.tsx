import { apiErrorMessage } from "../api/error";

export function LoadingBlock({ label = "Loading…" }: { label?: string }) {
  return <div className="state-card"><span className="spinner" aria-hidden="true" />{label}</div>;
}

export function ErrorBlock({ error, fallback = "This section is temporarily unavailable.", onRetry }: {
  error: unknown;
  fallback?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="state-card state-error" role="alert">
      <div><strong>We couldn't load this.</strong><span>{apiErrorMessage(error, fallback)}</span></div>
      {onRetry && <button className="button button-ghost button-small" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function EmptyBlock({ title, message }: { title: string; message: string }) {
  return <div className="empty-state"><div className="empty-icon">◎</div><h3>{title}</h3><p>{message}</p></div>;
}
