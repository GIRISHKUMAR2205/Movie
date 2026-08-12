import { BASE_URL } from "../api/axiosClient";

/**
 * Backend-driven Google OAuth2 (authorization code flow).
 *
 * This is a plain full-page navigation, NOT an axios/fetch call:
 * 1. Clicking sends the browser to `${BASE_URL}/auth/google` on the backend.
 * 2. The backend redirects the browser to Google's consent screen.
 * 3. Google redirects back to the backend's callback URL with a code.
 * 4. The backend exchanges the code, verifies the user, sets the
 *    httpOnly JWT cookies, and redirects the browser back to this app
 *    (e.g. to /dashboard on success, or /login?error=... on failure).
 *
 * The frontend never sees a Google token or client secret at any point —
 * it only ever sees the final redirect and the resulting cookie session.
 * No Google script, client ID, or popup handling is needed here.
 */
export default function GoogleAuthButton({ label = "Continue with Google" }) {
  const handleClick = () => {
    window.location.href = `${BASE_URL}/users/oauth2/authorization/google`;
  };

  return (
    <button
      type="button"
      className="google-auth-button"
      onClick={handleClick}
    >
      <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true" focusable="false">
        <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.9 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.6 6.1 29.6 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.7-.4-3.5z"/>
        <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.6 15.9 18.9 13 24 13c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.6 6.1 29.6 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/>
        <path fill="#4CAF50" d="M24 44c5.5 0 10.4-1.9 14.3-5.1l-6.6-5.6C29.6 35.1 27 36 24 36c-5.3 0-9.7-3.1-11.3-7.9l-6.6 5.1C9.6 39.6 16.3 44 24 44z"/>
        <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.2 5.7l6.6 5.6C41.5 36 44 30.5 44 24c0-1.3-.1-2.7-.4-3.5z"/>
      </svg>
      {label}
    </button>
  );
}
//Frontend Collects token and backend verifies the token