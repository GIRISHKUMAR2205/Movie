/**
 * axiosClient.js
 *
 * Central axios instance for the app.
 *
 * IMPORTANT — how httpOnly cookie auth works here:
 * - The JWT (access token) and refresh token are NEVER stored in JS
 *   (no localStorage/sessionStorage). The backend sets them as
 *   httpOnly, Secure, SameSite cookies on login/signup/refresh.
 * - Because the cookie is httpOnly, the browser attaches it to
 *   requests automatically — JS can't read it, and doesn't need to.
 * - `withCredentials: true` is what tells the browser to send/receive
 *   cookies on cross-origin requests (or same-site with different port
 *   in dev). Without this, the cookie will silently not be sent.
 *
 * Expected backend contract:
 * - POST /users/login          -> sets access + refresh cookies
 * - POST /users/signup         -> sets access + refresh cookies
 * - POST /users/google         -> sets access + refresh cookies (Google ID token in body)
 * - POST /users/refresh        -> reads refresh cookie, rotates access cookie
 * - POST /users/logout         -> clears cookies server-side
 * - GET  /users/me             -> returns current user from access cookie
 *
 * Cookie flags the backend should use (Express + cookie-parser example):
 *   res.cookie('accessToken', token, {
 *     httpOnly: true,
 *     secure: true,        // HTTPS only — required for SameSite=None
 *     sameSite: 'strict',  // or 'none' if frontend/backend are on different domains
 *     maxAge: 15 * 60 * 1000,
 *     path: '/',
 *   });
 */


import axios from "axios";

export const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8084";

const axiosClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // send/receive httpOnly cookies
  headers: {
    "Content-Type": "application/json",
  },
});

// --- Refresh-on-401 handling -------------------------------------------
//
// If an access token expires mid-session, the API returns 401. We try a
// silent refresh (which relies on the httpOnly refresh cookie) exactly
// once per failed request, then retry the original request. Concurrent
// 401s are queued so we don't fire multiple refresh calls at once.

let isRefreshing = false;
let pendingQueue = [];

const processQueue = (error) => {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve();
  });
  pendingQueue = [];
};

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    // Don't try to refresh on the auth endpoints themselves, and only
    // retry once per request (guarded by _retry).
    const isAuthRoute = originalRequest?.url?.includes("/auth/login") ||
      originalRequest?.url?.includes("/users/signup") ||
      originalRequest?.url?.includes("/users/refresh");

    if (status === 401 && !originalRequest._retry && !isAuthRoute) {
      if (isRefreshing) {
        // Queue this request until the in-flight refresh resolves
        return new Promise((resolve, reject) => {
          pendingQueue.push({ resolve, reject });
        })
          .then(() => axiosClient(originalRequest))
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await axiosClient.post("/users/refresh");
        processQueue(null);
        return axiosClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        // Refresh failed -> fully logged out. Let the caller (AuthContext)
        // handle redirect; we just propagate the error.
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
