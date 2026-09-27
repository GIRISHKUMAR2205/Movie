import { FormEvent, useState } from "react";

import { apiErrorMessage } from "../../api/error";
import {
  useConcertsQuery,
  useCreateConcertMutation,
  useCreateMovieMutation,
  useMoviesQuery,
  useUpdateMovieMutation
} from "../../api/platformApi";
import type { Movie, MovieInput } from "../../api/types";
import { ErrorBlock, LoadingBlock } from "../../components/Feedback";

const toList = (value: string) => value.split(",").map((item) => item.trim()).filter(Boolean);
const emptyMovie: MovieInput = {
  title: "", synopsis: "", originalLanguage: "", durationMinutes: 120, certification: "U/A",
  releaseDate: null, director: "", presentationFormats: ["2D"], hasSubtitles: false,
  subtitleLanguages: [], genres: [], castMembers: [], posterUrl: "", trailerUrl: "", published: true
};

const movieInput = (movie: Movie): MovieInput => ({
  title: movie.title,
  synopsis: movie.synopsis,
  originalLanguage: movie.originalLanguage,
  durationMinutes: movie.durationMinutes,
  certification: movie.certification,
  releaseDate: movie.releaseDate,
  director: movie.director ?? "",
  presentationFormats: movie.presentationFormats,
  hasSubtitles: movie.hasSubtitles,
  subtitleLanguages: movie.subtitleLanguages,
  genres: movie.genres,
  castMembers: movie.castMembers,
  posterUrl: movie.posterUrl ?? "",
  trailerUrl: movie.trailerUrl ?? "",
  published: movie.published
});

export default function CatalogAdmin() {
  const movies = useMoviesQuery();
  const concerts = useConcertsQuery();
  const [createMovie, createState] = useCreateMovieMutation();
  const [updateMovie, updateState] = useUpdateMovieMutation();
  const [createConcert, concertState] = useCreateConcertMutation();
  const [tab, setTab] = useState<"movies" | "concerts">("movies");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [movie, setMovie] = useState<MovieInput>(emptyMovie);
  const [concert, setConcert] = useState({
    title: "", artist: "", description: "", genre: "", durationMinutes: 120,
    ageRestriction: "", posterUrl: "", published: true
  });
  const [message, setMessage] = useState<string | null>(null);

  const submitMovie = async (event: FormEvent) => {
    event.preventDefault();
    setMessage(null);
    try {
      if (editingId) await updateMovie({ id: editingId, body: movie }).unwrap();
      else await createMovie(movie).unwrap();
      setMovie(emptyMovie);
      setEditingId(null);
      setMessage(`Movie ${editingId ? "updated" : "created"} successfully.`);
    } catch (error) {
      setMessage(apiErrorMessage(error, "Unable to save movie."));
    }
  };

  const submitConcert = async (event: FormEvent) => {
    event.preventDefault();
    setMessage(null);
    try {
      await createConcert(concert).unwrap();
      setConcert({ title: "", artist: "", description: "", genre: "", durationMinutes: 120, ageRestriction: "", posterUrl: "", published: true });
      setMessage("Concert created successfully.");
    } catch (error) {
      setMessage(apiErrorMessage(error, "Unable to save concert. The catalog service may not be implemented yet."));
    }
  };

  return (
    <section className="admin-page">
      <div className="page-title"><span className="eyebrow">CATALOG ADMIN</span><h1>Program the marquee</h1><p>Publish movie details and concert listings seen by customers.</p></div>
      <div className="tabs"><button className={tab === "movies" ? "active" : ""} onClick={() => setTab("movies")}>Movies</button><button className={tab === "concerts" ? "active" : ""} onClick={() => setTab("concerts")}>Concerts</button></div>
      {message && <div className="notice">{message}</div>}
      {tab === "movies" ? (
        <div className="admin-grid">
          <form className="panel stack-form" onSubmit={submitMovie}>
            <div className="panel-heading"><h2>{editingId ? "Edit movie" : "Add movie"}</h2>{editingId && <button type="button" className="button button-ghost button-small" onClick={() => { setEditingId(null); setMovie(emptyMovie); }}>Cancel edit</button>}</div>
            <div className="two-fields"><label className="field"><span>Title</span><input className="input" required maxLength={250} value={movie.title} onChange={(e) => setMovie({ ...movie, title: e.target.value })} /></label><label className="field"><span>Original language</span><input className="input" required value={movie.originalLanguage} onChange={(e) => setMovie({ ...movie, originalLanguage: e.target.value })} /></label></div>
            <label className="field"><span>Synopsis</span><textarea className="input" required rows={4} value={movie.synopsis} onChange={(e) => setMovie({ ...movie, synopsis: e.target.value })} /></label>
            <div className="three-fields"><label className="field"><span>Duration (minutes)</span><input className="input" type="number" min={1} max={600} value={movie.durationMinutes} onChange={(e) => setMovie({ ...movie, durationMinutes: Number(e.target.value) })} /></label><label className="field"><span>Certification</span><input className="input" required value={movie.certification} onChange={(e) => setMovie({ ...movie, certification: e.target.value })} /></label><label className="field"><span>Release date</span><input className="input" type="date" value={movie.releaseDate ?? ""} onChange={(e) => setMovie({ ...movie, releaseDate: e.target.value || null })} /></label></div>
            <label className="field"><span>Director</span><input className="input" value={movie.director} onChange={(e) => setMovie({ ...movie, director: e.target.value })} /></label>
            <div className="two-fields"><label className="field"><span>Formats (comma separated)</span><input className="input" required value={movie.presentationFormats.join(", ")} onChange={(e) => setMovie({ ...movie, presentationFormats: toList(e.target.value) })} placeholder="2D, 3D, IMAX, 4DX" /></label><label className="field"><span>Genres</span><input className="input" value={movie.genres.join(", ")} onChange={(e) => setMovie({ ...movie, genres: toList(e.target.value) })} /></label></div>
            <label className="field"><span>Cast members</span><input className="input" value={movie.castMembers.join(", ")} onChange={(e) => setMovie({ ...movie, castMembers: toList(e.target.value) })} /></label>
            <label className="check-field"><input type="checkbox" checked={movie.hasSubtitles} onChange={(e) => setMovie({ ...movie, hasSubtitles: e.target.checked, subtitleLanguages: e.target.checked ? movie.subtitleLanguages : [] })} /> Has subtitles</label>
            {movie.hasSubtitles && <label className="field"><span>Subtitle languages</span><input className="input" value={movie.subtitleLanguages.join(", ")} onChange={(e) => setMovie({ ...movie, subtitleLanguages: toList(e.target.value) })} /></label>}
            <div className="two-fields"><label className="field"><span>Poster URL</span><input className="input" type="url" value={movie.posterUrl} onChange={(e) => setMovie({ ...movie, posterUrl: e.target.value })} /></label><label className="field"><span>Trailer URL</span><input className="input" type="url" value={movie.trailerUrl} onChange={(e) => setMovie({ ...movie, trailerUrl: e.target.value })} /></label></div>
            <label className="check-field"><input type="checkbox" checked={movie.published} onChange={(e) => setMovie({ ...movie, published: e.target.checked })} /> Published</label>
            <button className="button button-primary" disabled={createState.isLoading || updateState.isLoading}>{createState.isLoading || updateState.isLoading ? "Saving…" : editingId ? "Update movie" : "Create movie"}</button>
          </form>
          <div><h2 className="list-title">Published catalog</h2>{movies.isLoading ? <LoadingBlock /> : movies.error ? <ErrorBlock error={movies.error} /> : <div className="admin-list">{movies.data?.map((item) => <article className="admin-list-item" key={item.id}><div><strong>{item.title}</strong><span>{item.originalLanguage} · {item.durationMinutes} min · {item.presentationFormats.join(", ")}</span></div><button className="button button-ghost button-small" onClick={() => { setEditingId(item.id); setMovie(movieInput(item)); window.scrollTo({ top: 0, behavior: "smooth" }); }}>Edit</button></article>)}</div>}</div>
        </div>
      ) : (
        <div className="admin-grid">
          <form className="panel stack-form" onSubmit={submitConcert}>
            <h2>Add concert</h2>
            <div className="two-fields"><label className="field"><span>Concert title</span><input className="input" required value={concert.title} onChange={(e) => setConcert({ ...concert, title: e.target.value })} /></label><label className="field"><span>Artist</span><input className="input" required value={concert.artist} onChange={(e) => setConcert({ ...concert, artist: e.target.value })} /></label></div>
            <label className="field"><span>Description</span><textarea className="input" required rows={5} value={concert.description} onChange={(e) => setConcert({ ...concert, description: e.target.value })} /></label>
            <div className="two-fields"><label className="field"><span>Genre</span><input className="input" required value={concert.genre} onChange={(e) => setConcert({ ...concert, genre: e.target.value })} /></label><label className="field"><span>Duration</span><input className="input" type="number" min={1} value={concert.durationMinutes} onChange={(e) => setConcert({ ...concert, durationMinutes: Number(e.target.value) })} /></label></div>
            <label className="field"><span>Age restriction</span><input className="input" value={concert.ageRestriction} onChange={(e) => setConcert({ ...concert, ageRestriction: e.target.value })} /></label>
            <label className="field"><span>Poster URL</span><input className="input" type="url" value={concert.posterUrl} onChange={(e) => setConcert({ ...concert, posterUrl: e.target.value })} /></label>
            <label className="check-field"><input type="checkbox" checked={concert.published} onChange={(e) => setConcert({ ...concert, published: e.target.checked })} /> Published</label>
            <button className="button button-primary" disabled={concertState.isLoading}>{concertState.isLoading ? "Saving…" : "Create concert"}</button>
          </form>
          <div><h2 className="list-title">Concert catalog</h2>{concerts.isLoading ? <LoadingBlock /> : concerts.error ? <ErrorBlock error={concerts.error} fallback="Concert administration will work when the catalog service is implemented." /> : <div className="admin-list">{concerts.data?.map((item) => <article className="admin-list-item" key={item.id}><div><strong>{item.title}</strong><span>{item.artist} · {item.genre}</span></div></article>)}</div>}</div>
        </div>
      )}
    </section>
  );
}
