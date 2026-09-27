import {
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError
} from "@reduxjs/toolkit/query/react";
import { Mutex } from "async-mutex";

import type { RootState } from "../app/store";
import { API_BASE_URL } from "../config/environment";
import { clearAuth, setSession } from "../features/auth/authSlice";
import type { AuthSession, GatewayResponse } from "./types";

export { API_BASE_URL } from "../config/environment";

const unauthenticatedEndpoints = new Set([
  "login",
  "signup",
  "refresh",
  "verifyEmail"
]);

const rawBaseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  credentials: "include",
  prepareHeaders: (headers, { getState, endpoint }) => {
    const accessToken = (getState() as RootState).auth.accessToken;
    if (accessToken && !unauthenticatedEndpoints.has(endpoint)) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
    return headers;
  }
});

const refreshBaseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  credentials: "include"
});

const refreshMutex = new Mutex();
const USER_API = "/api/v1/users";
const reauthExcludedPaths = new Set([
  `${USER_API}/login`,
  `${USER_API}/signup`,
  `${USER_API}/refresh`,
  `${USER_API}/email-verifications/verify`
]);

const requestPath = (args: string | FetchArgs) => (typeof args === "string" ? args : args.url);

const isAuthSession = (value: unknown): value is GatewayResponse<AuthSession> => {
  if (!value || typeof value !== "object") return false;
  const response = value as GatewayResponse<AuthSession>;
  return response.success && response.data !== null && typeof response.data.accessToken === "string";
};

export const baseQueryWithReauth: BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> = async (
  args,
  api,
  extraOptions
) => {
  await refreshMutex.waitForUnlock();
  let result = await rawBaseQuery(args, api, extraOptions);

  if (result.error?.status !== 401 || reauthExcludedPaths.has(requestPath(args))) {
    return result;
  }

  if (!refreshMutex.isLocked()) {
    const release = await refreshMutex.acquire();
    try {
      const refreshResult = await refreshBaseQuery(
        {
          url: `${USER_API}/refresh`,
          method: "POST",
          headers: { "X-Requested-With": "XMLHttpRequest" }
        },
        api,
        extraOptions
      );
      if (isAuthSession(refreshResult.data) && refreshResult.data.data) {
        api.dispatch(setSession(refreshResult.data.data));
        result = await rawBaseQuery(args, api, extraOptions);
      } else {
        api.dispatch(clearAuth());
      }
    } finally {
      release();
    }
  } else {
    await refreshMutex.waitForUnlock();
    if ((api.getState() as RootState).auth.accessToken) {
      result = await rawBaseQuery(args, api, extraOptions);
    }
  }

  return result;
};
