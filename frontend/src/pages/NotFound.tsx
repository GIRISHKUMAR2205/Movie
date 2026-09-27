import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <section className="not-found-page">
      <span className="eyebrow">404</span>
      <h1>That page is not on the bill.</h1>
      <p>It may have moved, expired, or never existed.</p>
      <div className="form-actions">
        <Link className="button button-primary" to="/shows">Browse shows</Link>
        <Link className="button button-ghost" to="/">Go home</Link>
      </div>
    </section>
  );
}
