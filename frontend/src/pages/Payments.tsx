import { type FormEvent, useState } from "react";

import { apiErrorMessage } from "../api/error";
import { useAppSelector } from "../app/hooks";
import { useCancelPaymentMutation, usePaymentsQuery, useRefundPaymentMutation } from "../api/platformApi";
import type { Payment } from "../api/types";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../components/Feedback";
import StatusBadge from "../components/StatusBadge";
import { formatDateTime, formatMoney } from "../utils/format";

export default function Payments() {
  const payments = usePaymentsQuery();
  const roles = useAppSelector((state) => state.auth.user?.roles ?? []);
  const [cancel, cancelState] = useCancelPaymentMutation();
  const [refund, refundState] = useRefundPaymentMutation();
  const [refundFor, setRefundFor] = useState<Payment | null>(null);
  const [refundAmount, setRefundAmount] = useState("");
  const [reason, setReason] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionNotice, setActionNotice] = useState<string | null>(null);

  const closeRefund = () => {
    setRefundFor(null);
    setRefundAmount("");
    setReason("");
  };

  const cancelPayment = async (payment: Payment) => {
    setActionError(null);
    setActionNotice(null);
    try {
      await cancel(payment.id).unwrap();
      setActionNotice(`Payment ${payment.id.slice(0, 8)} was cancelled.`);
    } catch (error) {
      setActionError(apiErrorMessage(error, "Unable to cancel this payment."));
    }
  };

  const submitRefund = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!refundFor) return;
    setActionError(null);
    const requestedAmount = refundAmount.trim() ? Number(refundAmount) : undefined;
    const refundableAmount = Number(refundFor.amount) - Number(refundFor.refundedAmount ?? 0);
    if (requestedAmount !== undefined && (!Number.isFinite(requestedAmount) || requestedAmount <= 0 || requestedAmount > refundableAmount)) {
      setActionError(`Enter an amount between 0 and ${formatMoney(refundableAmount, refundFor.currency)}.`);
      return;
    }
    try {
      await refund({ id: refundFor.id, amount: requestedAmount, reason: reason.trim() }).unwrap();
      setActionNotice(`Refund requested for payment ${refundFor.id.slice(0, 8)}.`);
      closeRefund();
    } catch (error) {
      setActionError(apiErrorMessage(error, "Refund failed."));
    }
  };

  if (payments.isLoading) return <LoadingBlock label="Loading payments…" />;
  if (payments.error) return <ErrorBlock error={payments.error} onRetry={payments.refetch} />;

  return (
    <section className="content-section page-section">
      <div className="page-title">
        <span className="eyebrow">TRANSACTIONS</span>
        <h1>Payments</h1>
        <p>Track payment, cancellation, and refund status.</p>
      </div>
      {actionError && <p className="form-error" role="alert">{actionError}</p>}
      {actionNotice && <p className="form-notice" role="status">{actionNotice}</p>}
      {!payments.data?.length ? (
        <EmptyBlock title="No payments" message="Payments you start at checkout will appear here." />
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr><th>Created</th><th>Booking</th><th>Method</th><th>Amount</th><th>Status</th><th aria-label="Actions" /></tr>
            </thead>
            <tbody>
              {payments.data.map((payment) => (
                <tr key={payment.id}>
                  <td>{formatDateTime(payment.createdAt)}</td>
                  <td><code>{payment.bookingId.slice(0, 8)}</code></td>
                  <td>{payment.paymentMethod.replace("_", " ")}</td>
                  <td>
                    <strong>{formatMoney(payment.amount, payment.currency)}</strong>
                    {payment.refundedAmount > 0 && <small className="table-note">{formatMoney(payment.refundedAmount, payment.currency)} refunded</small>}
                  </td>
                  <td>
                    <StatusBadge status={payment.status} />
                    {payment.failureMessage && <small className="table-note error-text">{payment.failureMessage}</small>}
                  </td>
                  <td className="table-actions">
                    {payment.status === "PENDING" && (
                      <button className="button button-ghost button-small" disabled={cancelState.isLoading} onClick={() => void cancelPayment(payment)}>
                        {cancelState.isLoading ? "Cancelling…" : "Cancel"}
                      </button>
                    )}
                    {roles.includes("ROLE_ADMIN") && ["SUCCEEDED", "PARTIALLY_REFUNDED"].includes(payment.status) && (
                      <button className="button button-ghost button-small" onClick={() => { setActionError(null); setActionNotice(null); setRefundFor(payment); }}>
                        Refund
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {refundFor && (
        <div className="modal-backdrop" onMouseDown={closeRefund}>
          <form className="modal" onMouseDown={(event) => event.stopPropagation()} onSubmit={submitRefund}>
            <span className="eyebrow">ADMIN ACTION</span>
            <h2>Refund payment</h2>
            <p>Leave the amount empty to refund the remaining {formatMoney(Number(refundFor.amount) - Number(refundFor.refundedAmount ?? 0), refundFor.currency)}.</p>
            {actionError && <p className="form-error" role="alert">{actionError}</p>}
            <label className="field">
              <span>Amount (optional)</span>
              <input className="input" inputMode="decimal" min="0.01" step="0.01" value={refundAmount} onChange={(event) => setRefundAmount(event.target.value)} />
            </label>
            <label className="field">
              <span>Reason</span>
              <textarea className="input" required maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} />
            </label>
            <div className="form-actions">
              <button type="button" className="button button-ghost" onClick={closeRefund}>Cancel</button>
              <button className="button button-danger" disabled={refundState.isLoading}>{refundState.isLoading ? "Refunding…" : "Confirm refund"}</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
