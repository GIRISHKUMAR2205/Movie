const tone = (status: string) => {
  const normalized = status.toUpperCase();
  if (["SUCCEEDED", "CONFIRMED", "APPROVED", "ACTIVE", "AVAILABLE"].includes(normalized)) return "success";
  if (["FAILED", "REJECTED", "CANCELLED", "EXPIRED", "REVOKED", "BLOCKED"].includes(normalized)) return "danger";
  if (["PENDING", "PENDING_PAYMENT", "HELD", "PARTIALLY_REFUNDED"].includes(normalized)) return "warning";
  return "neutral";
};

export default function StatusBadge({ status }: { status: string }) {
  return <span className={`status-badge status-${tone(status)}`}>{status.replace(/_/g, " ")}</span>;
}
