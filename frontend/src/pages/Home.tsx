import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { ErrorBlock, LoadingBlock } from "../components/Feedback";
import { useConcertsQuery, useMoviesQuery, useShowsQuery } from "../api/platformApi";
import { formatDateTime, formatMoney } from "../utils/format";

export default function Home() {
  const [query, setQuery] = useState("");
  const movies = useMoviesQuery();
  const concerts = useConcertsQuery();
  const shows = useShowsQuery();
  const needle = query.trim().toLowerCase();
  const filteredMovies = useMemo(() => (movies.data ?? []).filter((movie) =>
    !needle || [movie.title, movie.originalLanguage, ...movie.genres].some((value) => value?.toLowerCase().includes(needle))), [movies.data, needle]);
  const filteredConcerts = useMemo(() => (concerts.data ?? []).filter((concert) =>
    !needle || [concert.title, concert.artist, concert.genre].some((value) => value?.toLowerCase().includes(needle))), [concerts.data, needle]);

  return (
    <>
      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow">YOUR NEXT STORY STARTS HERE</span>
          <h1>Find a screen.<br /><em>Feel the moment.</em></h1>
          <p>Discover movies and live concerts, choose your perfect seats, and keep every ticket in one place.</p>
          <div className="hero-search">
            <span aria-hidden="true">⌕</span>
            <input aria-label="Search movies and concerts" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by title, artist, genre…" />
            <Link className="button button-primary" to={`/shows${needle ? `?q=${encodeURIComponent(query)}` : ""}`}>Find shows</Link>
          </div>
          <div className="hero-facts"><span><strong>{movies.data?.length ?? 0}</strong> movies</span><span><strong>{concerts.data?.length ?? 0}</strong> concerts</span><span><strong>Secure</strong> checkout</span></div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-ticket"><span>ADMIT ONE</span><strong>SHOW<br />HUB</strong><small>MAKE IT A NIGHT</small></div>
          <div className="hero-orbit orbit-one" /><div className="hero-orbit orbit-two" />
        </div>
      </section>

      <section className="content-section">
        <div className="section-heading"><div><span className="eyebrow">NOW PLAYING</span><h2>Stories worth leaving home for</h2></div><Link to="/shows">View all shows →</Link></div>
        {movies.isLoading ? <LoadingBlock label="Loading movies…" /> : movies.error ? <ErrorBlock error={movies.error} onRetry={movies.refetch} /> : (
          <div className="poster-grid">
            {filteredMovies.map((movie) => (
              <Link className="poster-card" to={`/movies/${movie.id}`} key={movie.id}>
                <div className="poster-image">
                  {movie.posterUrl ? <img src={movie.posterUrl} alt="" /> : <div className="poster-fallback"><span>{movie.title.charAt(0)}</span></div>}
                  <span className="poster-format">{movie.presentationFormats[0] ?? "2D"}</span>
                </div>
                <h3>{movie.title}</h3>
                <p>{movie.genres.slice(0, 2).join(" · ") || movie.originalLanguage} · {movie.durationMinutes} min</p>
              </Link>
            ))}
            {!filteredMovies.length && <div className="empty-inline">No movies match “{query}”.</div>}
          </div>
        )}
      </section>

      <section className="content-section surface-section">
        <div className="section-heading"><div><span className="eyebrow">LIVE & LOUD</span><h2>Concerts that stay with you</h2></div></div>
        {concerts.isLoading ? <LoadingBlock label="Loading concerts…" /> : concerts.error ? (
          <ErrorBlock error={concerts.error} fallback="The concert catalog service has not been started yet." onRetry={concerts.refetch} />
        ) : (
          <div className="event-grid">
            {filteredConcerts.map((concert) => (
              <Link className="event-card" to={`/concerts/${concert.id}`} key={concert.id}>
                <div className="event-image">{concert.posterUrl ? <img src={concert.posterUrl} alt="" /> : <span>LIVE</span>}</div>
                <div><span className="eyebrow">{concert.genre}</span><h3>{concert.title}</h3><p>{concert.artist}</p></div>
              </Link>
            ))}
            {!filteredConcerts.length && !needle && <div className="empty-inline">Concert listings will appear here when the catalog service publishes them.</div>}
          </div>
        )}
      </section>

      {(shows.data?.length ?? 0) > 0 && <section className="content-section">
        <div className="section-heading"><div><span className="eyebrow">UP NEXT</span><h2>Shows near you</h2></div></div>
        <div className="show-strip">{shows.data?.slice(0, 4).map((show) => <Link to={`/shows/${show.id}`} className="show-row" key={show.id}><div><strong>{show.contentTitle}</strong><span>{show.venueName} · {show.city}</span></div><div><strong>{formatDateTime(show.startsAt)}</strong><span>from {formatMoney(show.minPrice, show.currency)}</span></div><span className="row-arrow">→</span></Link>)}</div>
      </section>}
    </>
  );
}
