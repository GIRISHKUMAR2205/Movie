import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import useAuth from "../hooks/useAuth";
import GoogleAuthButton from "../components/GoogleAuthButton";
import "./auth.css";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LEN = 8;

// Maps the backend's ?error=<code> redirect query param to user-facing
// copy, for when the Google OAuth2 exchange fails server-side.
const GOOGLE_ERROR_MESSAGES = {
  google_failed: "Google sign-up failed. Please try again.",
  google_cancelled: "Google sign-up was cancelled.",
  email_exists: "An account with that email already exists using a different sign-in method.",
};

export default function Signup() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [form, setForm] = useState({ name: "", email: "", password: "", confirmPassword: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const errorCode = searchParams.get("error");
    if (errorCode) {
      setFormError(GOOGLE_ERROR_MESSAGES[errorCode] || "Sign-up failed. Please try again.");
      searchParams.delete("error");
      setSearchParams(searchParams, { replace: true });
    }
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => ({ ...prev, [name]: null }));
  };

  const validate = () => {
    const errors = {};
    if (!form.name.trim()) errors.name = "Name is required.";
    if (!form.email) errors.email = "Email is required.";
    else if (!EMAIL_RE.test(form.email)) errors.email = "Enter a valid email address.";
    if (!form.password) errors.password = "Password is required.";
    else if (form.password.length < MIN_PASSWORD_LEN)
      errors.password = `Password must be at least ${MIN_PASSWORD_LEN} characters.`;
    if (form.confirmPassword !== form.password)
      errors.confirmPassword = "Passwords do not match.";
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);
    if (!validate()) return;

    setSubmitting(true);
    const result = await signup({
      name: form.name.trim(),
      email: form.email,
      password: form.password,
    });
    setSubmitting(false);

    if (result.ok) navigate("/dashboard", { replace: true });
    else setFormError(result.error);
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">Start your session in a few seconds.</p>

        {formError && <div className="auth-error-banner">{formError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="name">Full name</label>
            <input
              id="name"
              name="name"
              type="text"
              autoComplete="name"
              className="auth-input"
              value={form.name}
              onChange={handleChange}
              aria-invalid={Boolean(fieldErrors.name)}
            />
            {fieldErrors.name && <p className="auth-field-error">{fieldErrors.name}</p>}
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              className="auth-input"
              value={form.email}
              onChange={handleChange}
              aria-invalid={Boolean(fieldErrors.email)}
            />
            {fieldErrors.email && <p className="auth-field-error">{fieldErrors.email}</p>}
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="new-password"
              className="auth-input"
              value={form.password}
              onChange={handleChange}
              aria-invalid={Boolean(fieldErrors.password)}
            />
            {fieldErrors.password && <p className="auth-field-error">{fieldErrors.password}</p>}
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="confirmPassword">Confirm password</label>
            <input
              id="confirmPassword"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              className="auth-input"
              value={form.confirmPassword}
              onChange={handleChange}
              aria-invalid={Boolean(fieldErrors.confirmPassword)}
            />
            {fieldErrors.confirmPassword && (
              <p className="auth-field-error">{fieldErrors.confirmPassword}</p>
            )}
          </div>

          <button className="auth-submit" type="submit" disabled={submitting}>
            {submitting ? "Creating account…" : "Create account"}
          </button>
        </form>

        <div className="auth-divider">OR</div>
        <GoogleAuthButton />

        <p className="auth-footer">
          Already have an account? <Link className="auth-link" to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}