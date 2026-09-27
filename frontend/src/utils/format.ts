export const formatMoney = (amount: number, currency = "INR") => {
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(2)}`;
  }
};

export const formatDateTime = (value?: string | null) => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
};

export const formatDate = (value?: string | null) => {
  if (!value) return "—";
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(date);
};

export const readableRole = (role: string) => role
  .replace(/^ROLE_/, "")
  .toLowerCase()
  .split("_")
  .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
  .join(" ");

export const newIdempotencyKey = () => {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) return crypto.randomUUID();
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};
