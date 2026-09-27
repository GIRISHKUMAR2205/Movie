import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useSignupMutation } from "../api/authApi";
import { useAppSelector } from "../app/hooks";
import GoogleAuthButton from "../components/GoogleAuthButton";
import "./auth.css";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Signup() {
  const navigate = useNavigate();
  const { status } = useAppSelector((state) => state.auth);
  const [signup, { isLoading }] = useSignupMutation();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirmPassword: "" });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "authenticated") navigate("/dashboard", { replace: true });
  }, [navigate, status]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!form.name.trim() || !EMAIL_RE.test(form.email) || form.password.length < 12 || form.password !== form.confirmPassword) {
      setError("Provide a name, valid email, matching passwords, and a password of at least 12 characters.");
      return;
    }
    try {
      await signup({ ...form, name: form.name.trim() }).unwrap();
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "Unable to create the account."));
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-card">
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">We will email a verification link after sign-up.</p>
        {error && <div className="auth-error-banner">{error}</div>}
        <form onSubmit={submit} noValidate>
          {([['name', 'Full name', 'text'], ['email', 'Email', 'email'], ['password', 'Password', 'password'], ['confirmPassword', 'Confirm password', 'password']] as const).map(([name, label, type]) => (
            <div className="auth-field" key={name}>
              <label className="auth-label" htmlFor={name}>{label}</label>
              <input id={name} name={name} type={type} className="auth-input"
                autoComplete={name === "password" ? "new-password" : undefined}
                value={form[name]} onChange={(event) => setForm({ ...form, [name]: event.target.value })} />
            </div>
          ))}
          <button className="auth-submit" disabled={isLoading}>{isLoading ? "Creating account…" : "Create account"}</button>
        </form>
        <div className="auth-divider">OR</div>
        <GoogleAuthButton label="Continue with Google" flow="user" />
        <p className="auth-footer">Already have an account? <Link className="auth-link" to="/login">Log in</Link></p>
      </section>
    </main>
  );
}
