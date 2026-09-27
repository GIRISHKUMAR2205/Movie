import { createApi } from "@reduxjs/toolkit/query/react";

import { clearAuth, setSession } from "../features/auth/authSlice";
import { baseQueryWithReauth } from "./baseQuery";
import type {
  AuthSession,
  GatewayResponse,
  LoginRequest,
  RoleRequestCreated,
  RoleRequestInput,
  RoleRequestProgress,
  SignupRequest,
  UserProfile
} from "./types";

const unwrap = <T>(response: GatewayResponse<T>): T => {
  if (!response.success || response.data === null) {
    throw new Error(response.message || "The request could not be completed.");
  }
  return response.data;
};

const USER_API = "/api/v1/users";

export const authApi = createApi({
  reducerPath: "authApi",
  baseQuery: baseQueryWithReauth,
  tagTypes: ["RoleRequests"],
  endpoints: (build) => ({
    login: build.mutation<AuthSession, LoginRequest>({
      query: (body) => ({ url: `${USER_API}/login`, method: "POST", body }),
      transformResponse: (response: GatewayResponse<AuthSession>) => unwrap(response),
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(setSession(data));
        } catch {
          // The caller controls the visible failure state.
        }
      }
    }),
    signup: build.mutation<AuthSession, SignupRequest>({
      query: (body) => ({ url: `${USER_API}/signup`, method: "POST", body }),
      transformResponse: (response: GatewayResponse<AuthSession>) => unwrap(response),
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(setSession(data));
        } catch {
          // The caller controls the visible failure state.
        }
      }
    }),
    refresh: build.mutation<AuthSession, void>({
      query: () => ({
        url: `${USER_API}/refresh`,
        method: "POST",
        headers: { "X-Requested-With": "XMLHttpRequest" }
      }),
      transformResponse: (response: GatewayResponse<AuthSession>) => unwrap(response),
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(setSession(data));
        } catch {
          dispatch(clearAuth());
        }
      }
    }),
    currentUser: build.query<UserProfile, void>({
      query: () => `${USER_API}/me`,
      transformResponse: (response: GatewayResponse<UserProfile>) => unwrap(response)
    }),
    myRoleRequestProgress: build.query<RoleRequestProgress[], void>({
      query: () => `${USER_API}/role-requests`,
      transformResponse: (response: GatewayResponse<RoleRequestProgress[]>) => unwrap(response),
      providesTags: ["RoleRequests"]
    }),
    requestRole: build.mutation<RoleRequestCreated, RoleRequestInput>({
      query: (body) => ({ url: `${USER_API}/requestAdminPrivilege`, method: "POST", body }),
      transformResponse: (response: GatewayResponse<RoleRequestCreated>) => unwrap(response),
      invalidatesTags: ["RoleRequests"]
    }),
    logout: build.mutation<void, void>({
      query: () => ({ url: `${USER_API}/signout`, method: "POST" }),
      transformResponse: () => undefined,
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          await queryFulfilled;
        } catch {
          // The local session must still be cleared after a failed sign-out call.
        } finally {
          dispatch(clearAuth());
        }
      }
    }),
    resendVerification: build.mutation<void, void>({
      query: () => ({ url: `${USER_API}/email-verifications/resend`, method: "POST" }),
      transformResponse: () => undefined
    }),
    verifyEmail: build.query<void, string>({
      query: (token) => ({ url: `${USER_API}/email-verifications/verify`, params: { token } }),
      transformResponse: () => undefined
    })
  })
});

export const {
  useLoginMutation,
  useSignupMutation,
  useLogoutMutation,
  useMyRoleRequestProgressQuery,
  useRequestRoleMutation,
  useResendVerificationMutation,
  useVerifyEmailQuery
} = authApi;
