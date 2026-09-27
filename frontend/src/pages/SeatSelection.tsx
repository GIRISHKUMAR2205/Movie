import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useCreateBookingMutation, useShowSeatsQuery, useShowsQuery } from "../api/platformApi";
import { ErrorBlock, LoadingBlock } from "../components/Feedback";
import { formatDateTime, formatMoney, newIdempotencyKey } from "../utils/format";

const MAX_SEATS_PER_BOOKING = 10;

export default function SeatSelection() {
  const showId = useParams().showId ?? "";
  const navigate = useNavigate();
  const seatMap = useShowSeatsQuery(showId, {
    skip: !showId,
    pollingInterval: 15_000,
    refetchOnFocus: true
  });
  const shows = useShowsQuery();
  const [createBooking, bookingState] = useCreateBookingMutation();
  const [selected, setSelected] = useState<Array<string | number>>([]);
  const [error, setError] = useState<string | null>(null);
  const bookingKey = useRef(newIdempotencyKey());
  const show = shows.data?.find((entry) => String(entry.id) === showId);
  const seats = seatMap.data?.seats ?? [];
  const rows = useMemo(() => Object.entries(seats.reduce<Record<string, typeof seats>>((grouped, seat) => {
    (grouped[seat.rowLabel] ??= []).push(seat);
    return grouped;
  }, {})).map(([row, values]) => [row, values.sort((a, b) => a.seatNumber - b.seatNumber)] as const), [seats]);
  const total = seats.filter((seat) => selected.includes(seat.id)).reduce((sum, seat) => sum + Number(seat.price), 0);
  const currency = seats[0]?.currency ?? show?.currency ?? "INR";

  useEffect(() => {
    const availableIds = new Set(seats.filter((seat) => seat.status === "AVAILABLE").map((seat) => seat.id));
    setSelected((current) => {
      const available = current.filter((seatId) => availableIds.has(seatId));
      if (available.length !== current.length) setError("One or more selected seats are no longer available.");
      return available;
    });
  }, [seats]);

  const toggle = (id: string | number) => {
    if (!selected.includes(id) && selected.length >= MAX_SEATS_PER_BOOKING) {
      setError(`You can reserve up to ${MAX_SEATS_PER_BOOKING} seats in one booking.`);
      return;
    }
    bookingKey.current = newIdempotencyKey();
    setError(null);
    setSelected((current) => current.includes(id) ? current.filter((seatId) => seatId !== id) : [...current, id]);
  };
  const reserve = async () => {
    if (!selected.length) return;
    setError(null);
    try {
      const booking = await createBooking({ showId, seatIds: selected.map(Number), idempotencyKey: bookingKey.current }).unwrap();
      navigate(`/checkout/${booking.id}`);
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "Unable to reserve these seats. They may have just been taken."));
      void seatMap.refetch();
    }
  };

  if (seatMap.isLoading) return <LoadingBlock label="Loading live seat availability…" />;
  if (seatMap.error) return <ErrorBlock error={seatMap.error} fallback="Seat inventory is not available. Start the booking service and try again." onRetry={seatMap.refetch} />;
  return (
    <section className="seat-page">
      <div className="seat-heading">
        <div>
          <span className="eyebrow">SELECT SEATS</span>
          <h1>{show?.contentTitle ?? "Choose your seats"}</h1>
          <p>{show ? `${show.venueName} · ${formatDateTime(show.startsAt)}` : `Show ${showId}`}</p>
        </div>
        <div className="seat-legend">
          <span><i className="legend-seat available" />Available</span>
          <span><i className="legend-seat selected" />Selected</span>
          <span><i className="legend-seat occupied" />Unavailable</span>
        </div>
      </div>
      <div className="seat-layout">
        <div className="seat-map">
          <div className="screen"><span>SCREEN / STAGE</span></div>
          {rows.map(([row, rowSeats]) => (
            <div className="seat-row" key={row}>
              <strong>{row}</strong>
              <div>
                {rowSeats.map((seat) => {
                  const isSelected = selected.includes(seat.id);
                  const unavailable = seat.status !== "AVAILABLE";
                  const selectionLimitReached = !isSelected && selected.length >= MAX_SEATS_PER_BOOKING;
                  return (
                    <button
                      aria-pressed={isSelected}
                      className={`seat ${isSelected ? "is-selected" : ""} ${unavailable ? "is-occupied" : ""}`}
                      disabled={unavailable || selectionLimitReached}
                      key={seat.id}
                      onClick={() => toggle(seat.id)}
                      title={`${seat.seatType} · ${formatMoney(seat.price, seat.currency)}`}
                      type="button"
                    >
                      {seat.seatNumber}
                    </button>
                  );
                })}
              </div>
              <strong>{row}</strong>
            </div>
          ))}
          {!rows.length && <div className="empty-inline">No seats have been configured for this show.</div>}
        </div>
        <aside className="selection-card">
          <span className="eyebrow">YOUR SELECTION</span>
          <h2>{selected.length} {selected.length === 1 ? "seat" : "seats"}</h2>
          <p className="selection-limit">Up to {MAX_SEATS_PER_BOOKING} seats per booking. Availability refreshes automatically.</p>
          <div className="selected-list">
            {seats.filter((seat) => selected.includes(seat.id)).map((seat) => (
              <div key={seat.id}>
                <span>{seat.rowLabel}{seat.seatNumber} · {seat.seatType}</span>
                <strong>{formatMoney(seat.price, seat.currency)}</strong>
              </div>
            ))}
          </div>
          <div className="total-row"><span>Total</span><strong>{formatMoney(total, currency)}</strong></div>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="button button-primary button-wide" disabled={!selected.length || bookingState.isLoading} onClick={reserve}>
            {bookingState.isLoading ? "Reserving…" : "Continue to checkout"}
          </button>
          <small>Seats are held for a limited time after you continue.</small>
        </aside>
      </div>
    </section>
  );
}
