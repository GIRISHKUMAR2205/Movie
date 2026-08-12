import axiosClient from "./axiosClient";

// All of these rely on the browser sending/receiving httpOnly cookies —
// we never read or write tokens from JS.

export const loginRequest = ({ email, password }) =>
  axiosClient.post("/users/login", { email, password }).then((res) => res.data);

export const signupRequest = ({ name, email, password }) =>
  axiosClient.post("/users/signup", { name, email, password }).then((res) => res.data);

export const logoutRequest = () =>
  axiosClient.post("/users/signout").then((res) => res.data);

export const fetchCurrentUser = () =>
  axiosClient.get("/users/me").then((res) => res.data);

export const oidcLogoutRequest = () =>
  axiosClient.get("/users/signout/oidc").then((res) => res.data);
