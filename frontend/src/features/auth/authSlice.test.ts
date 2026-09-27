import { describe, expect, it } from "vitest";

import reducer, { clearAuth, setSession } from "./authSlice";

describe("auth state", () => {
  it("keeps the access token only in Redux state and clears it on logout", () => {
    let state = reducer(undefined, setSession({
      name: "Ada",
      email: "ada@example.com",
      roles: ["ROLE_USER"],
      emailVerified: true,
      accessToken: "short-lived-access-token"
    }));

    expect(state.accessToken).toBe("short-lived-access-token");
    expect(state.user?.email).toBe("ada@example.com");

    state = reducer(state, clearAuth());
    expect(state.accessToken).toBeNull();
    expect(state.user).toBeNull();
  });
});
