import { Link } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useBookingsQuery, useCancelBookingMutation } from "../api/platformApi";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../components/Feedback";
import StatusBadge from "../components/StatusBadge";
import { formatDateTime, formatMoney } from "../utils/format";

export default function Bookings() {
  const bookings = useBookingsQuery();
  const [cancel, cancelState] = useCancelBookingMutation();
  if (bookings.isLoading) return <LoadingBlock label="Loading your bookings…" />;
  if (bookings.error) return <ErrorBlock error={bookings.error} fallback="The booking service has not been started yet." onRetry={bookings.refetch} />;
  return <section className="content-section page-section"><div className="page-title"><span className="eyebrow">MY TICKETS</span><h1>Your bookings</h1><p>Everything you have booked, from seat hold to show night.</p></div>{!bookings.data?.length ? <EmptyBlock title="No bookings yet" message="Choose a show and your tickets will appear here." /> : <div className="booking-grid">{bookings.data.map((booking) => <article className="panel booking-card" key={booking.id}><div className="booking-card-top"><div><span className="eyebrow">{formatDateTime(booking.startsAt)}</span><h2>{booking.contentTitle}</h2><p>{booking.venueName}</p></div><StatusBadge status={booking.status} /></div><div className="ticket-seats">{booking.seats.map((seat) => <span className="chip" key={seat.seatId}>{seat.rowLabel}{seat.seatNumber}</span>)}</div><div className="booking-card-bottom"><strong>{formatMoney(booking.totalAmount, booking.currency)}</strong><div>{["PENDING_PAYMENT", "PAYMENT_PROCESSING"].includes(booking.status) && <Link className="button button-primary button-small" to={`/checkout/${booking.id}`}>Pay now</Link>}{booking.status === "PENDING_PAYMENT" && <button className="button button-ghost button-small" disabled={cancelState.isLoading} onClick={async () => { try { await cancel(booking.id).unwrap(); } catch (error) { window.alert(apiErrorMessage(error, "Unable to cancel booking.")); } }}>Cancel</button>}</div></div></article>)}</div>}</section>;
}
