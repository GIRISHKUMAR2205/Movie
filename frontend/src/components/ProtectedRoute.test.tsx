import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import authReducer, { clearAuth, setSession } from "../features/auth/authSlice";
import ProtectedRoute from "./ProtectedRoute";

const renderRoute = (authenticated: boolean) => {
  const store = configureStore({ reducer: { auth: authReducer } });
  if (authenticated) {
    store.dispatch(setSession({
      name: "Ada",
      email: "ada@example.com",
      roles: ["ROLE_USER"],
      emailVerified: true,
      accessToken: "token"
    }));
  } else {
    store.dispatch(clearAuth());
  }
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={["/dashboard"]}>
        <Routes>
          <Route element={<ProtectedRoute />}><Route path="/dashboard" element={<div>Dashboard</div>} /></Route>
          <Route path="/login" element={<div>Login</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>
  );
};

describe("ProtectedRoute", () => {
  it("redirects an unauthenticated visitor to login", () => {
    renderRoute(false);
    expect(screen.getByText("Login")).toBeInTheDocument();
  });

  it("renders protected content for an authenticated visitor", () => {
    renderRoute(true);
    expect(screen.getByText("Dashboard")).toBeInTheDocument();
  });
});
