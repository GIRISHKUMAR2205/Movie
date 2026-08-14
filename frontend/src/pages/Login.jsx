import { useEffect, useState } from "react";
import { Link, replace, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import useAuth from "../hooks/useAuth";
import GoogleAuthButton from "../components/GoogleAuthButton";
import "./auth.css";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Maps the backend's ?error=<code> redirect query param to user-facing
// copy. The backend redirects here (e.g. `${CLIENT_URL}/login?error=google_failed`)
// when the Google OAuth2 exchange fails server-side.
const GOOGLE_ERROR_MESSAGES = {
  google_failed: "Google sign-in failed. Please try again.",
  google_cancelled: "Google sign-in was cancelled.",
  email_exists: "An account with that email already exists using a different sign-in method.",
};

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const redirectTo = location.state?.from?.pathname || "/dashboard";

  const [form, setForm] = useState({ email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Surface an error the backend sent back via redirect after a failed
  // Google OAuth2 attempt, then strip it from the URL.
  useEffect(() => {
    if(user != null){
      navigate("/dashboard", replace);
      return ;
    }
    const errorCode = searchParams.get("error");
    if (errorCode) {
      setFormError(GOOGLE_ERROR_MESSAGES[errorCode] || "Sign-in failed. Please try again.");
      searchParams.delete("error");
      setSearchParams(searchParams, { replace: true });
    }
  }, [user, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => ({ ...prev, [name]: null }));
  };

  const validate = () => {
    const errors = {};
    // if (!form.email) errors.email = "Email is required.";
    // else if (!EMAIL_RE.test(form.email)) errors.email = "Enter a valid email address.";
    // if (!form.password) errors.password = "Password is required.";
    // setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);
    if (!validate()) return;

    setSubmitting(true);
    const result = await login(form);
    setSubmitting(false);

    if (result.ok) navigate(redirectTo, { replace: true });
    else setFormError(result.error);
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Log in to continue to your account.</p>

        {formError && <div className="auth-error-banner">{formError}</div>} 

        <form onSubmit={handleSubmit} noValidate>
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
              aria-describedby={fieldErrors.email ? "email-error" : undefined}
            />
            {fieldErrors.email && (
              <p id="email-error" className="auth-field-error">{fieldErrors.email}</p>
            )}
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              className="auth-input"
              value={form.password}
              onChange={handleChange}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? "password-error" : undefined}
            />
            {fieldErrors.password && (
              <p id="password-error" className="auth-field-error">{fieldErrors.password}</p>
            )}
          </div>

          <button className="auth-submit" type="submit" disabled={submitting}>
            {submitting ? "Logging in…" : "Log in"}
          </button>
        </form>

        <div className="auth-divider">OR</div>
        <GoogleAuthButton />

        <p className="auth-footer">
          Don't have an account? <Link className="auth-link" to="/signup">Sign up</Link>
        </p>
      </div>
    </div>
  );
}