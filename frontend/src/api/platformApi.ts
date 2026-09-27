import { createApi } from "@reduxjs/toolkit/query/react";

import { baseQueryWithReauth } from "./baseQuery";
import type {
  Auditorium,
  Booking,
  Concert,
  ConcertInput,
  CreateBookingInput,
  CreatePaymentInput,
  GatewayResponse,
  Movie,
  MovieInput,
  Payment,
  Refund,
  RoleAudit,
  RoleRequestSummary,
  ScheduledShow,
  SeatType,
  ShowSeatMap,
  ShowSummary,
  Theater,
  Venue
} from "./types";

const unwrap = <T>(response: GatewayResponse<T>): T => {
  if (!response.success || response.data === null) {
    throw new Error(response.message || "The request could not be completed.");
  }
  return response.data;
};

const csv = (values: string) => values.split(",").map((value) => value.trim()).filter(Boolean);

export interface ShowFilters {
  contentId?: string;
  contentType?: "MOVIE" | "CONCERT";
  city?: string;
  date?: string;
}

export interface TheaterInput {
  name: string;
  address: string;
  city: string;
  country: string;
  timeZone: string;
}

export interface ScheduleInput {
  theaterId: number;
  auditoriumId: number;
  body: {
    contentType: "MOVIE" | "CONCERT";
    contentId: string;
    contentTitle: string;
    fromDate: string;
    toDate: string;
    showTime: string;
    durationMinutes: number;
    intermissionStartMinute: number | null;
    intermissionDurationMinutes: number;
    postShowBreakMinutes: number;
    seatPrices: Array<{ seatTypeCode: string; price: number }>;
  };
}

export const platformApi = createApi({
  reducerPath: "platformApi",
  baseQuery: baseQueryWithReauth,
  tagTypes: ["Movies", "Concerts", "Shows", "Bookings", "Payments", "Theaters", "Auditoriums", "SeatTypes", "RoleApprovals"],
  endpoints: (build) => ({
    movies: build.query<Movie[], void>({
      query: () => "/api/v1/movies",
      providesTags: ["Movies"]
    }),
    movie: build.query<Movie, number>({
      query: (id) => `/api/v1/movies/${id}`
    }),
    createMovie: build.mutation<Movie, MovieInput>({
      query: (body) => ({ url: "/api/v1/movies", method: "POST", body }),
      invalidatesTags: ["Movies"]
    }),
    updateMovie: build.mutation<Movie, { id: number; body: MovieInput }>({
      query: ({ id, body }) => ({ url: `/api/v1/movies/${id}`, method: "PUT", body }),
      invalidatesTags: ["Movies"]
    }),
    concerts: build.query<Concert[], void>({
      query: () => "/api/v1/concerts",
      providesTags: ["Concerts"]
    }),
    concert: build.query<Concert, string>({
      query: (id) => `/api/v1/concerts/${id}`
    }),
    createConcert: build.mutation<Concert, ConcertInput>({
      query: (body) => ({ url: "/api/v1/concerts", method: "POST", body }),
      invalidatesTags: ["Concerts"]
    }),
    venues: build.query<Venue[], void>({
      query: () => "/api/v1/venues"
    }),
    shows: build.query<ShowSummary[], ShowFilters | void>({
      query: (params) => ({ url: "/api/v1/shows", params: params || undefined }),
      providesTags: ["Shows"]
    }),
    showSeats: build.query<ShowSeatMap, string>({
      query: (showId) => `/api/v1/shows/${showId}/seats`,
      providesTags: (_result, _error, id) => [{ type: "Shows", id }]
    }),
    bookings: build.query<Booking[], void>({
      query: () => "/api/v1/bookings",
      providesTags: ["Bookings"]
    }),
    booking: build.query<Booking, string>({
      query: (id) => `/api/v1/bookings/${id}`,
      providesTags: (_result, _error, id) => [{ type: "Bookings", id }]
    }),
    createBooking: build.mutation<Booking, CreateBookingInput>({
      query: ({ idempotencyKey, ...body }) => ({
        url: "/api/v1/bookings", method: "POST", body,
        headers: { "Idempotency-Key": idempotencyKey }
      }),
      invalidatesTags: ["Bookings", "Shows"]
    }),
    cancelBooking: build.mutation<Booking, string>({
      query: (id) => ({ url: `/api/v1/bookings/${id}/cancel`, method: "POST" }),
      invalidatesTags: ["Bookings", "Shows"]
    }),
    payments: build.query<Payment[], void>({
      query: () => "/api/v1/payments",
      providesTags: ["Payments"]
    }),
    payment: build.query<Payment, string>({
      query: (id) => `/api/v1/payments/${id}`,
      providesTags: (_result, _error, id) => [{ type: "Payments", id }]
    }),
    createPayment: build.mutation<Payment, CreatePaymentInput>({
      query: ({ idempotencyKey, ...body }) => ({
        url: "/api/v1/payments",
        method: "POST",
        body,
        headers: { "Idempotency-Key": idempotencyKey }
      }),
      invalidatesTags: ["Payments", "Bookings"]
    }),
    cancelPayment: build.mutation<Payment, string>({
      query: (id) => ({ url: `/api/v1/payments/${id}/cancel`, method: "POST" }),
      invalidatesTags: ["Payments"]
    }),
    refundPayment: build.mutation<Refund, { id: string; amount?: number; reason: string }>({
      query: ({ id, ...body }) => ({ url: `/api/v1/payments/${id}/refunds`, method: "POST", body }),
      invalidatesTags: ["Payments"]
    }),
    myTheaters: build.query<Theater[], void>({
      query: () => "/api/v1/theaters/mine",
      providesTags: ["Theaters"]
    }),
    createTheater: build.mutation<Theater, TheaterInput>({
      query: (body) => ({ url: "/api/v1/theaters", method: "POST", body }),
      invalidatesTags: ["Theaters"]
    }),
    auditoriums: build.query<Auditorium[], number>({
      query: (theaterId) => `/api/v1/theaters/${theaterId}/auditoriums`,
      providesTags: ["Auditoriums"]
    }),
    createAuditorium: build.mutation<Auditorium, { theaterId: number; name: string }>({
      query: ({ theaterId, name }) => ({ url: `/api/v1/theaters/${theaterId}/auditoriums`, method: "POST", body: { name } }),
      invalidatesTags: ["Auditoriums"]
    }),
    seatTypes: build.query<SeatType[], { theaterId: number; auditoriumId: number }>({
      query: ({ theaterId, auditoriumId }) => `/api/v1/theaters/${theaterId}/auditoriums/${auditoriumId}/seat-types`,
      providesTags: ["SeatTypes"]
    }),
    createSeatType: build.mutation<SeatType, { theaterId: number; auditoriumId: number; code: string; displayName: string; defaultPrice: number }>({
      query: ({ theaterId, auditoriumId, ...body }) => ({
        url: `/api/v1/theaters/${theaterId}/auditoriums/${auditoriumId}/seat-types`, method: "POST", body
      }),
      invalidatesTags: ["SeatTypes"]
    }),
    createSeats: build.mutation<void, { theaterId: number; auditoriumId: number; rows: string; seatsPerRow: number; seatTypeCode: string }>({
      query: ({ theaterId, auditoriumId, rows, seatsPerRow, seatTypeCode }) => ({
        url: `/api/v1/theaters/${theaterId}/auditoriums/${auditoriumId}/seats`,
        method: "POST",
        body: {
          seats: csv(rows).flatMap((rowLabel) => Array.from({ length: seatsPerRow }, (_, index) => ({
            rowLabel,
            seatNumber: index + 1,
            seatTypeCode
          })))
        }
      })
    }),
    createSchedule: build.mutation<ScheduledShow[], ScheduleInput>({
      query: ({ theaterId, auditoriumId, body }) => ({
        url: `/api/v1/theaters/${theaterId}/auditoriums/${auditoriumId}/show-schedules`, method: "POST", body
      }),
      invalidatesTags: ["Shows"]
    }),
    theaterSchedule: build.query<ScheduledShow[], { theaterId: number; from: string; to: string }>({
      query: ({ theaterId, from, to }) => ({ url: `/api/v1/theaters/${theaterId}/schedule`, params: { from, to } }),
      providesTags: ["Shows"]
    }),
    roleApprovals: build.query<RoleRequestSummary[], void>({
      query: () => "/api/v1/users/super/role-requests",
      transformResponse: (response: GatewayResponse<RoleRequestSummary[]>) => unwrap(response),
      providesTags: ["RoleApprovals"]
    }),
    roleAudit: build.query<RoleAudit[], number>({
      query: (id) => `/api/v1/users/super/role-requests/${id}/audit`,
      transformResponse: (response: GatewayResponse<RoleAudit[]>) => unwrap(response)
    }),
    decideRoleRequest: build.mutation<void, { id: number; action: "approve" | "reject" | "revoke" | "reactivate"; reason?: string }>({
      query: ({ id, action, reason }) => ({
        url: `/api/v1/users/super/role-requests/${id}/${action}`, method: "POST", body: { reason: reason || null }
      }),
      transformResponse: () => undefined,
      invalidatesTags: ["RoleApprovals"]
    })
  })
});

export const {
  useMoviesQuery, useMovieQuery, useCreateMovieMutation, useUpdateMovieMutation,
  useConcertsQuery, useConcertQuery, useCreateConcertMutation, useVenuesQuery,
  useShowsQuery, useShowSeatsQuery,
  useBookingsQuery, useBookingQuery, useCreateBookingMutation, useCancelBookingMutation,
  usePaymentsQuery, usePaymentQuery, useCreatePaymentMutation, useCancelPaymentMutation, useRefundPaymentMutation,
  useMyTheatersQuery, useCreateTheaterMutation, useAuditoriumsQuery, useCreateAuditoriumMutation,
  useSeatTypesQuery, useCreateSeatTypeMutation, useCreateSeatsMutation, useCreateScheduleMutation, useTheaterScheduleQuery,
  useRoleApprovalsQuery, useRoleAuditQuery, useDecideRoleRequestMutation
} = platformApi;
