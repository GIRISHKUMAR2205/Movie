import { Link, useSearchParams } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useVerifyEmailQuery } from "../api/authApi";
import "./auth.css";

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const { isLoading, isSuccess, error } = useVerifyEmailQuery(token ?? "", { skip: !token });

  const content = !token
    ? "The verification link is incomplete."
    : isLoading
      ? "Verifying your email address…"
      : isSuccess
        ? "Your email address is verified. You can now request role upgrades."
        : apiErrorMessage(error, "This verification link is invalid or has expired.");

  return (
    <main className="auth-page">
      <section className="auth-card">
        <h1 className="auth-title">Verify email</h1>
        <p className="auth-subtitle">{content}</p>
        {isSuccess ? <Link className="auth-link" to="/login">Continue to login</Link> : <Link className="auth-link" to="/signup">Back to sign up</Link>}
      </section>
    </main>
  );
}
