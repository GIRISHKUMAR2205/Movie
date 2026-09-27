import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { authApi } from "../api/authApi";
import { useAppDispatch } from "../app/hooks";
import { clearAuth, setAccessToken, setProfile } from "../features/auth/authSlice";

export default function OAuthCallback() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    const accessToken = new URLSearchParams(window.location.hash.slice(1)).get("access_token");
    window.history.replaceState(null, "", window.location.pathname);
    if (!accessToken) {
      dispatch(clearAuth());
      navigate("/login?error=google_failed", { replace: true });
      return;
    }

    void (async () => {
      dispatch(setAccessToken(accessToken));
      try {
        const user = await dispatch(authApi.endpoints.currentUser.initiate()).unwrap();
        dispatch(setProfile(user));
        navigate("/dashboard", { replace: true });
      } catch {
        dispatch(clearAuth());
        navigate("/login?error=google_failed", { replace: true });
      }
    })();
  }, [dispatch, navigate]);

  return <div className="auth-loading">Completing Google sign-in…</div>;
}
