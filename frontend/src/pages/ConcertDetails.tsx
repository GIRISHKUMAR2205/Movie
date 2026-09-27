import { Link, useParams } from "react-router-dom";

import { useConcertQuery, useShowsQuery } from "../api/platformApi";
import { ErrorBlock, LoadingBlock } from "../components/Feedback";
import { formatDateTime, formatMoney } from "../utils/format";

export default function ConcertDetails() {
  const id = useParams().concertId ?? "";
  const concert = useConcertQuery(id, { skip: !id });
  const shows = useShowsQuery({ contentId: id, contentType: "CONCERT" }, { skip: !id });
  if (concert.isLoading) return <LoadingBlock label="Loading concert…" />;
  if (concert.error || !concert.data) return <ErrorBlock error={concert.error} fallback="Concert not found." onRetry={concert.refetch} />;
  const value = concert.data;
  return <><section className="detail-hero concert-detail"><div className="detail-poster">{value.posterUrl ? <img src={value.posterUrl} alt="" /> : <div className="poster-fallback"><span>LIVE</span></div>}</div><div className="detail-copy"><span className="eyebrow">{value.genre} · LIVE</span><h1>{value.title}</h1><h2>{value.artist}</h2><p>{value.description}</p><div className="detail-meta"><span>{value.durationMinutes} min</span>{value.ageRestriction && <span>{value.ageRestriction}</span>}</div></div></section><section className="content-section"><div className="section-heading"><div><span className="eyebrow">TOUR DATES</span><h2>Choose a performance</h2></div></div>{shows.isLoading ? <LoadingBlock /> : shows.error ? <ErrorBlock error={shows.error} fallback="Concert dates are currently unavailable." /> : <div className="show-strip">{shows.data?.map((show) => <Link to={`/shows/${show.id}`} className="show-row" key={show.id}><div><strong>{show.venueName}</strong><span>{show.city}</span></div><div><strong>{formatDateTime(show.startsAt)}</strong><span>from {formatMoney(show.minPrice, show.currency)}</span></div><span className="row-arrow">Tickets →</span></Link>)}</div>}</section></>;
}
