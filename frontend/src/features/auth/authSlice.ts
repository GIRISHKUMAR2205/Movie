import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

import type { AuthSession, UserProfile } from "../../api/types";

export type AuthStatus = "idle" | "checking" | "authenticated" | "unauthenticated";

interface AuthState {
  accessToken: string | null;
  user: UserProfile | null;
  status: AuthStatus;
}

const initialState: AuthState = {
  accessToken: null,
  user: null,
  status: "idle"
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    beginBootstrap: (state) => {
      state.status = "checking";
    },
    setSession: (state, action: PayloadAction<AuthSession>) => {
      const { accessToken, ...user } = action.payload;
      state.accessToken = accessToken;
      state.user = user;
      state.status = "authenticated";
    },
    setAccessToken: (state, action: PayloadAction<string>) => {
      state.accessToken = action.payload;
      state.status = "checking";
    },
    setProfile: (state, action: PayloadAction<UserProfile>) => {
      state.user = action.payload;
      state.status = state.accessToken ? "authenticated" : state.status;
    },
    clearAuth: (state) => {
      state.accessToken = null;
      state.user = null;
      state.status = "unauthenticated";
    }
  }
});

export const { beginBootstrap, setSession, setAccessToken, setProfile, clearAuth } = authSlice.actions;
export default authSlice.reducer;
