import { createContext, useCallback, useEffect, useMemo, useReducer } from "react";
import {
  fetchCurrentUser,
  loginRequest,
  logoutRequest,
  signupRequest,
  googleAuthRequest
} from "../api/authApi";

const AuthContext = createContext(null);

const initialState = {
  user: null,
  status: "idle", // 'idle' | 'loading' | 'authenticated' | 'unauthenticated'
  error: null,
};

function authReducer(state, action) {
  switch (action.type) {
    case "AUTH_START":
      return { ...state, status: "loading", error: null };
    case "AUTH_SUCCESS":
      return { ...state, status: "authenticated", user: action.payload, error: null };
    case "AUTH_FAILURE":
      return { ...state, status: "unauthenticated", user: null, error: action.payload };
    case "LOGOUT":
      return { ...state, status: "unauthenticated", user: null, error: null };
    default:
      return state;
  }
}

export function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  // On first load, ask the backend "who am I?" — the httpOnly cookie
  // (if present and valid) authenticates this request automatically.
  // This is what lets a session survive a page refresh without ever
  // touching localStorage.
  useEffect(() => {
    let cancelled = false;

    (async () => {
      dispatch({ type: "AUTH_START" });
      try {
        const user = await fetchCurrentUser();
        if (!cancelled) dispatch({ type: "AUTH_SUCCESS", payload: user });
      } catch {
        if (!cancelled) dispatch({ type: "AUTH_FAILURE", payload: null });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (credentials) => {
    dispatch({ type: "AUTH_START" });
    try {
      const user = await loginRequest(credentials);
      dispatch({ type: "AUTH_SUCCESS", payload: user });
      return { ok: true };
    } catch (err) {
      const message = err.response?.data?.message || "Invalid email or password.";
      dispatch({ type: "AUTH_FAILURE", payload: message });
      return { ok: false, error: message };
    }
  }, []);

  const signup = useCallback(async (details) => {
    dispatch({ type: "AUTH_START" });
    try {
      const user = await signupRequest(details);
      dispatch({ type: "AUTH_SUCCESS", payload: user });
      return { ok: true };
    } catch (err) {
      const message = err.response?.data?.message || "Could not create account.";
      dispatch({ type: "AUTH_FAILURE", payload: message });
      return { ok: false, error: message };
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest(); // clears httpOnly cookies server-side
    } finally {
      dispatch({ type: "LOGOUT" });
    }
  }, []);

  

  const value = useMemo(
    () => ({
      user: state.user,
      status: state.status,
      error: state.error,
      isAuthenticated: state.status === "authenticated",
      isLoading: state.status === "loading" || state.status === "idle",
      login,
      signup,
      logout
    }),
    [state, login, signup, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthContext;
