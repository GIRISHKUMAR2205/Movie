import { FormEvent, useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useLoginMutation } from "../api/authApi";
import { useAppSelector } from "../app/hooks";
import GoogleAuthButton from "../components/GoogleAuthButton";
import "./auth.css";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { status } = useAppSelector((state) => state.auth);
  const [login, { isLoading }] = useLoginMutation();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState<string | null>(null);
  const redirectTo = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? "/dashboard";

  useEffect(() => {
    if (status === "authenticated") navigate(redirectTo, { replace: true });
  }, [navigate, redirectTo, status]);

  useEffect(() => {
    if (new URLSearchParams(location.search).get("error") === "google_failed") {
      setError("Google sign-in could not be completed. Please try again or use your password.");
    }
  }, [location.search]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!form.username.trim() || !form.password) {
      setError("Username/email and password are required.");
      return;
    }
    try {
      await login({ ...form, username: form.username.trim() }).unwrap();
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "Unable to sign in."));
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-card">
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Log in to continue to your account.</p>
        {error && <div className="auth-error-banner">{error}</div>}
        <form onSubmit={submit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="username">Username or email</label>
            <input id="username" name="username" type="text" autoComplete="username" className="auth-input"
              value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} />
          </div>
          <div className="auth-field">
            <label className="auth-label" htmlFor="password">Password</label>
            <input id="password" name="password" type="password" autoComplete="current-password" className="auth-input"
              value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
          </div>
          <button className="auth-submit" disabled={isLoading}>{isLoading ? "Logging in…" : "Log in"}</button>
        </form>
        <div className="auth-divider">OR</div>
        <GoogleAuthButton />
        <p className="auth-footer">Don't have an account? <Link className="auth-link" to="/signup">Sign up</Link></p>
      </section>
    </main>
  );
}
