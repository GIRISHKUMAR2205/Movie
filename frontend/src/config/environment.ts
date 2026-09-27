const apiBaseUrl = import.meta.env.VITE_GATEWAY_URL?.trim();

if (!apiBaseUrl) {
  throw new Error("VITE_GATEWAY_URL must be set in the frontend environment.");
}

export const API_BASE_URL = apiBaseUrl.replace(/\/$/, "");
