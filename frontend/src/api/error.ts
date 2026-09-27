import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";

import type { ApiFailure, GatewayResponse } from "./types";

export const apiErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === "object" && "data" in error) {
    const data = (error as FetchBaseQueryError).data as (GatewayResponse<ApiFailure> & ApiFailure) | undefined;
    return data?.message ?? Object.values(data?.errors ?? {})[0] ?? fallback;
  }
  if (error instanceof Error) return error.message;
  return fallback;
};
