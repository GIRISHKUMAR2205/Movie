import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { useShowsQuery } from "../api/platformApi";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../components/Feedback";
import { formatDateTime, formatMoney } from "../utils/format";

export default function Shows() {
  const [params] = useSearchParams();
  const [kind, setKind] = useState<"ALL" | "MOVIE" | "CONCERT">("ALL");
  const [city, setCity] = useState("");
  const [date, setDate] = useState("");
  const [search, setSearch] = useState(params.get("q") ?? "");
  const shows = useShowsQuery({ contentType: kind === "ALL" ? undefined : kind, city: city || undefined, date: date || undefined });
  const data = useMemo(() => (shows.data ?? []).filter((show) => !search.trim() || `${show.contentTitle} ${show.venueName}`.toLowerCase().includes(search.trim().toLowerCase())), [search, shows.data]);
  return <section className="content-section page-section"><div className="page-title"><span className="eyebrow">WHAT'S ON</span><h1>Find your next show</h1><p>Browse every available movie and concert performance.</p></div><div className="filter-bar"><input className="input" placeholder="Title or venue" value={search} onChange={(e) => setSearch(e.target.value)} /><select className="input" value={kind} onChange={(e) => setKind(e.target.value as typeof kind)}><option value="ALL">All events</option><option value="MOVIE">Movies</option><option value="CONCERT">Concerts</option></select><input className="input" placeholder="City" value={city} onChange={(e) => setCity(e.target.value)} /><input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} /></div>{shows.isLoading ? <LoadingBlock label="Loading shows…" /> : shows.error ? <ErrorBlock error={shows.error} fallback="The show catalog service has not been started yet." onRetry={shows.refetch} /> : data.length === 0 ? <EmptyBlock title="No matching shows" message="Try a different date, city, or event type." /> : <div className="results-list">{data.map((show) => <Link className="result-card" to={`/shows/${show.id}`} key={show.id}><div className="date-block"><strong>{new Date(show.startsAt).toLocaleDateString(undefined, { day: "2-digit" })}</strong><span>{new Date(show.startsAt).toLocaleDateString(undefined, { month: "short" })}</span></div><div className="result-main"><span className="eyebrow">{show.contentType}</span><h3>{show.contentTitle}</h3><p>{show.venueName}{show.auditoriumName ? ` · ${show.auditoriumName}` : ""}, {show.city}</p></div><div className="result-side"><strong>{formatDateTime(show.startsAt)}</strong><span>{formatMoney(show.minPrice, show.currency)} onwards</span>{typeof show.availableSeats === "number" && <small>{show.availableSeats} seats left</small>}</div><span className="row-arrow">Select →</span></Link>)}</div>}</section>;
}
