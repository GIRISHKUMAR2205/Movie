import { useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useBookingQuery, useCreatePaymentMutation } from "../api/platformApi";
import type { Payment, PaymentMethod } from "../api/types";
import { ErrorBlock, LoadingBlock } from "../components/Feedback";
import StatusBadge from "../components/StatusBadge";
import { formatDateTime, formatMoney, newIdempotencyKey } from "../utils/format";

const methods: Array<{ value: PaymentMethod; label: string; detail: string }> = [
  { value: "UPI", label: "UPI", detail: "Any UPI app" },
  { value: "CARD", label: "Card", detail: "Credit or debit card" },
  { value: "NET_BANKING", label: "Net banking", detail: "Your bank account" },
  { value: "WALLET", label: "Wallet", detail: "Supported wallets" }
];

export default function Checkout() {
  const bookingId = useParams().bookingId ?? "";
  const booking = useBookingQuery(bookingId, { skip: !bookingId });
  const [method, setMethod] = useState<PaymentMethod>("UPI");
  const [createPayment, state] = useCreatePaymentMutation();
  const [payment, setPayment] = useState<Payment | null>(null);
  const [error, setError] = useState<string | null>(null);
  const idempotencyKey = useRef(newIdempotencyKey());
  if (booking.isLoading) return <LoadingBlock label="Preparing checkout…" />;
  if (booking.error || !booking.data) return <ErrorBlock error={booking.error} fallback="Booking not found or its seat hold has expired." />;
  const value = booking.data;
  const pay = async () => {
    setError(null);
    try {
      const created = await createPayment({ bookingId: value.id, paymentMethod: method, idempotencyKey: idempotencyKey.current }).unwrap();
      setPayment(created);
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "Payment could not be started."));
    }
  };
  return <section className="checkout-page"><div className="page-title"><span className="eyebrow">SECURE CHECKOUT</span><h1>Almost there</h1><p>Review your booking and choose how you want to pay.</p></div><div className="checkout-grid"><div className="checkout-main"><article className="panel ticket-summary"><div><span className="eyebrow">{formatDateTime(value.startsAt)}</span><h2>{value.contentTitle}</h2><p>{value.venueName}</p></div><StatusBadge status={value.status} /><div className="ticket-seats">{value.seats.map((seat) => <span className="chip" key={seat.seatId}>{seat.rowLabel}{seat.seatNumber} · {seat.seatType}</span>)}</div></article><article className="panel"><h2>Payment method</h2><div className="payment-methods">{methods.map((item) => <label className={`payment-method ${method === item.value ? "active" : ""}`} key={item.value}><input type="radio" name="method" value={item.value} checked={method === item.value} onChange={() => setMethod(item.value)} /><span className="method-icon">{item.value === "CARD" ? "▣" : item.value === "UPI" ? "U" : item.value === "WALLET" ? "◫" : "⌂"}</span><span><strong>{item.label}</strong><small>{item.detail}</small></span></label>)}</div></article></div><aside className="panel order-card"><h2>Order summary</h2><div className="order-lines"><div><span>Tickets ({value.seats.length})</span><strong>{formatMoney(value.totalAmount, value.currency)}</strong></div><div><span>Taxes and fees</span><span>Included</span></div></div><div className="total-row"><span>Payable now</span><strong>{formatMoney(value.totalAmount, value.currency)}</strong></div>{value.expiresAt && <p className="hold-note">Seat hold expires {formatDateTime(value.expiresAt)}</p>}{error && <p className="form-error">{error}</p>}{payment ? <div className="payment-created"><StatusBadge status={payment.status} /><p>Your payment was created safely. Complete it with the provider.</p>{payment.checkoutUrl ? <a href={payment.checkoutUrl} className="button button-primary button-wide" target="_blank" rel="noreferrer">Open secure payment</a> : <Link className="button button-primary button-wide" to="/payments">View payment status</Link>}</div> : <button className="button button-primary button-wide" disabled={state.isLoading || !["PENDING_PAYMENT", "PAYMENT_PROCESSING"].includes(value.status)} onClick={pay}>{state.isLoading ? "Starting payment…" : `Pay ${formatMoney(value.totalAmount, value.currency)}`}</button>}<small>ShowHub never stores card numbers or UPI credentials.</small></aside></div></section>;
}
