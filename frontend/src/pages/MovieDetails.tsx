import { Link, useParams } from "react-router-dom";

import { useMovieQuery, useShowsQuery } from "../api/platformApi";
import { ErrorBlock, LoadingBlock } from "../components/Feedback";
import { formatDate, formatDateTime, formatMoney } from "../utils/format";

export default function MovieDetails() {
  const id = Number(useParams().movieId);
  const movie = useMovieQuery(id, { skip: !Number.isFinite(id) });
  const shows = useShowsQuery({ contentId: String(id), contentType: "MOVIE" }, { skip: !Number.isFinite(id) });
  if (movie.isLoading) return <LoadingBlock label="Loading movie…" />;
  if (movie.error || !movie.data) return <ErrorBlock error={movie.error} fallback="Movie not found." onRetry={movie.refetch} />;
  const value = movie.data;

  return <>
    <section className="detail-hero">
      <div className="detail-poster">{value.posterUrl ? <img src={value.posterUrl} alt={`${value.title} poster`} /> : <div className="poster-fallback"><span>{value.title.charAt(0)}</span></div>}</div>
      <div className="detail-copy"><span className="eyebrow">{value.genres.join(" · ")}</span><h1>{value.title}</h1><div className="detail-meta"><span>{value.certification}</span><span>{value.durationMinutes} min</span><span>{value.originalLanguage}</span><span>{formatDate(value.releaseDate)}</span></div><p>{value.synopsis}</p><div className="chip-list">{value.presentationFormats.map((format) => <span className="chip" key={format}>{format}</span>)}{value.hasSubtitles && <span className="chip">Subtitles: {value.subtitleLanguages.join(", ")}</span>}</div>{value.director && <p className="credit"><strong>Director</strong> {value.director}</p>}{value.castMembers.length > 0 && <p className="credit"><strong>Cast</strong> {value.castMembers.join(", ")}</p>}{value.trailerUrl && <a className="button button-secondary" href={value.trailerUrl} target="_blank" rel="noreferrer">Watch trailer ↗</a>}</div>
    </section>
    <section className="content-section"><div className="section-heading"><div><span className="eyebrow">BOOK TICKETS</span><h2>Choose a showtime</h2></div></div>
      {shows.isLoading ? <LoadingBlock label="Finding showtimes…" /> : shows.error ? <ErrorBlock error={shows.error} fallback="The show catalog is not available yet." onRetry={shows.refetch} /> : shows.data?.length ? <div className="show-strip">{shows.data.map((show) => <Link to={`/shows/${show.id}`} className="show-row" key={show.id}><div><strong>{show.venueName}</strong><span>{show.auditoriumName} · {show.presentationFormat}</span></div><div><strong>{formatDateTime(show.startsAt)}</strong><span>from {formatMoney(show.minPrice, show.currency)}</span></div><span className="row-arrow">Choose seats →</span></Link>)}</div> : <div className="empty-inline">No showtimes have been published for this movie.</div>}
    </section>
  </>;
}
