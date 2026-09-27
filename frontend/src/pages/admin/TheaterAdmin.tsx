import { FormEvent, useEffect, useMemo, useState } from "react";

import { apiErrorMessage } from "../../api/error";
import {
  useAuditoriumsQuery,
  useCreateAuditoriumMutation,
  useConcertsQuery,
  useCreateScheduleMutation,
  useCreateSeatsMutation,
  useCreateSeatTypeMutation,
  useCreateTheaterMutation,
  useMoviesQuery,
  useMyTheatersQuery,
  useSeatTypesQuery,
  useTheaterScheduleQuery
} from "../../api/platformApi";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../../components/Feedback";
import { formatDateTime, formatMoney } from "../../utils/format";

const localDate = (offsetDays = 0) => {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
};

export default function TheaterAdmin() {
  const theaters = useMyTheatersQuery();
  const movies = useMoviesQuery();
  const concerts = useConcertsQuery();
  const [selectedTheater, setSelectedTheater] = useState<number | null>(null);
  const [selectedAuditorium, setSelectedAuditorium] = useState<number | null>(null);
  const auditoriums = useAuditoriumsQuery(selectedTheater ?? 0, { skip: selectedTheater === null });
  const seatTypes = useSeatTypesQuery({ theaterId: selectedTheater ?? 0, auditoriumId: selectedAuditorium ?? 0 }, { skip: selectedTheater === null || selectedAuditorium === null });
  const schedule = useTheaterScheduleQuery({ theaterId: selectedTheater ?? 0, from: localDate(), to: localDate(30) }, { skip: selectedTheater === null });
  const [createTheater, theaterState] = useCreateTheaterMutation();
  const [createAuditorium, auditoriumState] = useCreateAuditoriumMutation();
  const [createSeatType, seatTypeState] = useCreateSeatTypeMutation();
  const [createSeats, seatsState] = useCreateSeatsMutation();
  const [createSchedule, scheduleState] = useCreateScheduleMutation();
  const [message, setMessage] = useState<string | null>(null);
  const [theater, setTheater] = useState({ name: "", address: "", city: "", country: "India", timeZone: "Asia/Kolkata" });
  const [auditoriumName, setAuditoriumName] = useState("");
  const [seatType, setSeatType] = useState({ code: "REGULAR", displayName: "Regular", defaultPrice: 250 });
  const [seats, setSeats] = useState({ rows: "A, B, C, D", seatsPerRow: 12, seatTypeCode: "REGULAR" });
  const [show, setShow] = useState({
    contentId: "", contentTitle: "", fromDate: localDate(1), toDate: localDate(7), showTime: "18:30",
    durationMinutes: 120, intermissionStartMinute: 60 as number | null, intermissionDurationMinutes: 15,
    postShowBreakMinutes: 30
  });
  const [contentKind, setContentKind] = useState<"MOVIE" | "CONCERT">("MOVIE");
  const [schedulePrices, setSchedulePrices] = useState<Record<string, number>>({});

  useEffect(() => {
    if (selectedTheater === null && theaters.data?.length) setSelectedTheater(theaters.data[0].id);
  }, [selectedTheater, theaters.data]);
  useEffect(() => {
    setSelectedAuditorium(auditoriums.data?.[0]?.id ?? null);
  }, [auditoriums.data, selectedTheater]);
  useEffect(() => {
    if (seatTypes.data?.[0] && !seatTypes.data.some((type) => type.code === seats.seatTypeCode)) {
      setSeats((current) => ({ ...current, seatTypeCode: seatTypes.data![0].code }));
    }
    if (seatTypes.data) {
      setSchedulePrices((current) => Object.fromEntries(seatTypes.data!.map((type) => [type.code, current[type.code] ?? Number(type.defaultPrice)])));
    }
  }, [seatTypes.data, seats.seatTypeCode]);

  const selectedMovie = useMemo(() => movies.data?.find((movie) => String(movie.id) === show.contentId), [movies.data, show.contentId]);
  const selectedConcert = useMemo(() => concerts.data?.find((concert) => String(concert.id) === show.contentId), [concerts.data, show.contentId]);
  useEffect(() => {
    const selected = contentKind === "MOVIE" ? selectedMovie : selectedConcert;
    if (selected) setShow((current) => ({ ...current, contentTitle: selected.title, durationMinutes: selected.durationMinutes }));
  }, [contentKind, selectedConcert, selectedMovie]);

  const run = async (operation: () => Promise<unknown>, success: string) => {
    setMessage(null);
    try {
      await operation();
      setMessage(success);
    } catch (error) {
      setMessage(apiErrorMessage(error, "The operation could not be completed."));
    }
  };

  if (theaters.isLoading) return <LoadingBlock label="Loading your theaters…" />;
  if (theaters.error) return <ErrorBlock error={theaters.error} onRetry={theaters.refetch} />;
  return (
    <section className="admin-page">
      <div className="page-title"><span className="eyebrow">THEATER ADMIN</span><h1>Run the house</h1><p>Configure venues, auditoriums, seats, pricing, intermissions, and recurring schedules.</p></div>
      {message && <div className="notice">{message}</div>}
      <div className="theater-layout">
        <aside className="theater-sidebar">
          <div className="sidebar-heading"><h2>Your theaters</h2><span>{theaters.data?.length ?? 0}</span></div>
          {theaters.data?.map((item) => <button key={item.id} className={selectedTheater === item.id ? "active" : ""} onClick={() => setSelectedTheater(item.id)}><strong>{item.name}</strong><span>{item.city}</span></button>)}
          {!theaters.data?.length && <p>No theater registered yet.</p>}
        </aside>
        <div className="theater-workspace">
          <details className="panel disclosure" open={!theaters.data?.length}>
            <summary><span><span className="step-number">1</span><strong>Register a theater</strong></span><span>＋</span></summary>
            <form className="stack-form" onSubmit={(event: FormEvent) => { event.preventDefault(); void run(async () => { const created = await createTheater(theater).unwrap(); setTheater({ name: "", address: "", city: "", country: "India", timeZone: "Asia/Kolkata" }); setSelectedTheater(created.id); }, "Theater registered."); }}>
              <div className="two-fields"><label className="field"><span>Name</span><input className="input" required value={theater.name} onChange={(e) => setTheater({ ...theater, name: e.target.value })} /></label><label className="field"><span>City</span><input className="input" required value={theater.city} onChange={(e) => setTheater({ ...theater, city: e.target.value })} /></label></div>
              <label className="field"><span>Address</span><input className="input" required value={theater.address} onChange={(e) => setTheater({ ...theater, address: e.target.value })} /></label>
              <div className="two-fields"><label className="field"><span>Country</span><input className="input" required value={theater.country} onChange={(e) => setTheater({ ...theater, country: e.target.value })} /></label><label className="field"><span>IANA time zone</span><input className="input" required value={theater.timeZone} onChange={(e) => setTheater({ ...theater, timeZone: e.target.value })} /></label></div>
              <button className="button button-primary" disabled={theaterState.isLoading}>{theaterState.isLoading ? "Registering…" : "Register theater"}</button>
            </form>
          </details>

          {selectedTheater !== null && <>
            <details className="panel disclosure" open={!auditoriums.data?.length}>
              <summary><span><span className="step-number">2</span><strong>Auditoriums</strong></span><span>＋</span></summary>
              <form className="inline-form" onSubmit={(event) => { event.preventDefault(); void run(async () => { const created = await createAuditorium({ theaterId: selectedTheater, name: auditoriumName }).unwrap(); setAuditoriumName(""); setSelectedAuditorium(created.id); }, "Auditorium added."); }}><label className="field"><span>Auditorium name</span><input className="input" required value={auditoriumName} onChange={(e) => setAuditoriumName(e.target.value)} placeholder="Screen 1" /></label><button className="button button-secondary" disabled={auditoriumState.isLoading}>Add auditorium</button></form>
              <div className="choice-row">{auditoriums.data?.map((item) => <button key={item.id} className={selectedAuditorium === item.id ? "chip-button active" : "chip-button"} onClick={() => setSelectedAuditorium(item.id)}>{item.name}</button>)}</div>
            </details>

            {selectedAuditorium !== null && <>
              <details className="panel disclosure">
                <summary><span><span className="step-number">3</span><strong>Seat types and layout</strong></span><span>＋</span></summary>
                <h3>Add a seat category</h3>
                <form className="three-fields align-end" onSubmit={(event) => { event.preventDefault(); void run(() => createSeatType({ theaterId: selectedTheater, auditoriumId: selectedAuditorium, ...seatType }).unwrap(), "Seat type added."); }}><label className="field"><span>Code</span><input className="input" required value={seatType.code} onChange={(e) => setSeatType({ ...seatType, code: e.target.value.toUpperCase() })} /></label><label className="field"><span>Display name</span><input className="input" required value={seatType.displayName} onChange={(e) => setSeatType({ ...seatType, displayName: e.target.value })} /></label><label className="field"><span>Default price</span><input className="input" type="number" min={0.01} step={0.01} value={seatType.defaultPrice} onChange={(e) => setSeatType({ ...seatType, defaultPrice: Number(e.target.value) })} /></label><button className="button button-secondary" disabled={seatTypeState.isLoading}>Add type</button></form>
                <div className="choice-row">{seatTypes.data?.map((type) => <span className="info-chip" key={type.id}>{type.displayName} · {formatMoney(type.defaultPrice)}</span>)}</div>
                <h3>Generate seats</h3>
                <form className="three-fields align-end" onSubmit={(event) => { event.preventDefault(); void run(() => createSeats({ theaterId: selectedTheater, auditoriumId: selectedAuditorium, ...seats }).unwrap(), "Seats created."); }}><label className="field"><span>Rows (comma separated)</span><input className="input" required value={seats.rows} onChange={(e) => setSeats({ ...seats, rows: e.target.value })} /></label><label className="field"><span>Seats per row</span><input className="input" type="number" min={1} max={100} value={seats.seatsPerRow} onChange={(e) => setSeats({ ...seats, seatsPerRow: Number(e.target.value) })} /></label><label className="field"><span>Seat type</span><select className="input" value={seats.seatTypeCode} onChange={(e) => setSeats({ ...seats, seatTypeCode: e.target.value })}>{seatTypes.data?.map((type) => <option value={type.code} key={type.id}>{type.displayName}</option>)}</select></label><button className="button button-secondary" disabled={seatsState.isLoading || !seatTypes.data?.length}>Generate</button></form>
              </details>

              <details className="panel disclosure" open>
                <summary><span><span className="step-number">4</span><strong>Schedule shows</strong></span><span>＋</span></summary>
                <form className="stack-form" onSubmit={(event) => { event.preventDefault(); const prices = (seatTypes.data ?? []).map((type) => ({ seatTypeCode: type.code, price: schedulePrices[type.code] ?? Number(type.defaultPrice) })); void run(() => createSchedule({ theaterId: selectedTheater, auditoriumId: selectedAuditorium, body: { ...show, contentType: contentKind, seatPrices: prices } }).unwrap(), "Shows scheduled successfully."); }}>
                  <div className="three-fields"><label className="field"><span>Content type</span><select className="input" value={contentKind} onChange={(e) => { setContentKind(e.target.value as typeof contentKind); setShow({ ...show, contentId: "", contentTitle: "" }); }}><option value="MOVIE">Movie</option><option value="CONCERT">Concert</option></select></label><label className="field"><span>{contentKind === "MOVIE" ? "Movie" : "Concert"}</span><select className="input" required value={show.contentId} onChange={(e) => setShow({ ...show, contentId: e.target.value })}><option value="">Choose content</option>{contentKind === "MOVIE" ? movies.data?.map((movie) => <option value={movie.id} key={movie.id}>{movie.title}</option>) : concerts.data?.map((concert) => <option value={concert.id} key={concert.id}>{concert.title}</option>)}</select></label><label className="field"><span>Content title</span><input className="input" required value={show.contentTitle} onChange={(e) => setShow({ ...show, contentTitle: e.target.value })} /></label></div>
                  <div className="three-fields"><label className="field"><span>From date</span><input className="input" type="date" required value={show.fromDate} onChange={(e) => setShow({ ...show, fromDate: e.target.value })} /></label><label className="field"><span>To date</span><input className="input" type="date" required value={show.toDate} onChange={(e) => setShow({ ...show, toDate: e.target.value })} /></label><label className="field"><span>Show time</span><input className="input" type="time" required value={show.showTime} onChange={(e) => setShow({ ...show, showTime: e.target.value })} /></label></div>
                  <div className="three-fields"><label className="field"><span>Duration (minutes)</span><input className="input" type="number" min={1} max={600} value={show.durationMinutes} onChange={(e) => setShow({ ...show, durationMinutes: Number(e.target.value) })} /></label><label className="field"><span>Intermission starts at</span><input className="input" type="number" min={0} placeholder="No intermission" value={show.intermissionStartMinute ?? ""} onChange={(e) => setShow({ ...show, intermissionStartMinute: e.target.value ? Number(e.target.value) : null })} /></label><label className="field"><span>Intermission duration</span><input className="input" type="number" min={0} max={120} value={show.intermissionDurationMinutes} onChange={(e) => setShow({ ...show, intermissionDurationMinutes: Number(e.target.value) })} /></label></div>
                  <label className="field"><span>Post-show break (minutes)</span><input className="input" type="number" min={0} max={240} value={show.postShowBreakMinutes} onChange={(e) => setShow({ ...show, postShowBreakMinutes: Number(e.target.value) })} /></label>
                  <div><span className="field-caption">Show price by seat type</span><div className="three-fields">{seatTypes.data?.map((type) => <label className="field" key={type.id}><span>{type.displayName}</span><input className="input" type="number" min={0.01} step={0.01} required value={schedulePrices[type.code] ?? type.defaultPrice} onChange={(e) => setSchedulePrices({ ...schedulePrices, [type.code]: Number(e.target.value) })} /></label>)}</div></div>
                  <button className="button button-primary" disabled={scheduleState.isLoading || !seatTypes.data?.length}>{scheduleState.isLoading ? "Scheduling…" : "Create recurring schedule"}</button>
                </form>
              </details>
              <div className="panel"><div className="panel-heading"><div><span className="eyebrow">NEXT 30 DAYS</span><h2>Scheduled shows</h2></div></div>{schedule.isLoading ? <LoadingBlock /> : schedule.error ? <ErrorBlock error={schedule.error} /> : !schedule.data?.length ? <EmptyBlock title="No shows scheduled" message="Use the schedule form above to add the first one." /> : <div className="admin-list">{schedule.data.map((item) => <article className="admin-list-item" key={item.id}><div><strong>{item.contentTitle}</strong><span>{item.auditoriumName} · {formatDateTime(item.startsAt)} – {formatDateTime(item.endsAt)}</span></div><span>{item.seatPrices.map((price) => formatMoney(price.price)).join(" / ")}</span></article>)}</div>}</div>
            </>}
          </>}
        </div>
      </div>
    </section>
  );
}
