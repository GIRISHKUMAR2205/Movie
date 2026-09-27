export interface GatewayResponse<T> {
  timestamp: string;
  status: number;
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  errors: Record<string, string> | null;
}

export interface UserProfile {
  name: string;
  email: string;
  roles: string[];
  emailVerified: boolean;
}

export interface AuthSession extends UserProfile {
  accessToken: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface ApiFailure {
  message?: string;
  errors?: Record<string, string> | null;
}

export interface RoleAuditStatus {
  previousStatus: string | null;
  currentStatus: string;
  reason: string | null;
  changedAt: string;
}

export interface RoleRequestProgress {
  requestId: number;
  requestedRole: string;
  requestStatus: string;
  roleStatus: string | null;
  requestedAt: string;
  auditTrail: RoleAuditStatus[];
}

export interface RoleRequestInput {
  roleName: "ROLE_ADMIN" | "ROLE_THEATER_ADMIN";
  reason?: string;
}

export interface RoleRequestCreated {
  requestId: number;
}

export interface Movie {
  id: number;
  title: string;
  synopsis: string;
  originalLanguage: string;
  durationMinutes: number;
  certification: string;
  releaseDate: string | null;
  director: string | null;
  presentationFormats: string[];
  hasSubtitles: boolean;
  subtitleLanguages: string[];
  genres: string[];
  castMembers: string[];
  posterUrl: string | null;
  trailerUrl: string | null;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MovieInput {
  title: string;
  synopsis: string;
  originalLanguage: string;
  durationMinutes: number;
  certification: string;
  releaseDate: string | null;
  director: string;
  presentationFormats: string[];
  hasSubtitles: boolean;
  subtitleLanguages: string[];
  genres: string[];
  castMembers: string[];
  posterUrl: string;
  trailerUrl: string;
  published: boolean;
}

export interface Concert {
  id: string;
  title: string;
  artist: string;
  description: string;
  genre: string;
  durationMinutes: number;
  ageRestriction: string | null;
  posterUrl: string | null;
  published: boolean;
}

export interface ConcertInput {
  title: string;
  artist: string;
  description: string;
  genre: string;
  durationMinutes: number;
  ageRestriction: string;
  posterUrl: string;
  published: boolean;
}

export interface Venue {
  id: string;
  name: string;
  address: string;
  city: string;
  country: string;
  timeZone: string;
}

export interface ShowSummary {
  id: string;
  contentId: string;
  contentType: "MOVIE" | "CONCERT";
  contentTitle: string;
  posterUrl?: string | null;
  venueId: string;
  venueName: string;
  auditoriumName?: string | null;
  city: string;
  startsAt: string;
  endsAt: string;
  presentationFormat?: string | null;
  minPrice: number;
  currency: string;
  availableSeats?: number;
}

export type SeatStatus = "AVAILABLE" | "HELD" | "BOOKED" | "BLOCKED";

export interface ShowSeat {
  id: string | number;
  rowLabel: string;
  seatNumber: number;
  seatType: string;
  price: number;
  currency: string;
  status: SeatStatus;
}

export interface ShowSeatMap {
  showId: string;
  seats: ShowSeat[];
  holdDurationSeconds?: number;
}

export interface BookingSeat {
  seatId: string | number;
  rowLabel: string;
  seatNumber: number;
  seatType: string;
  price: number;
}

export type BookingStatus = "PENDING_PAYMENT" | "PAYMENT_PROCESSING" | "CONFIRMED" | "CANCELLED" | "EXPIRED" | "PAYMENT_FAILED" | "REFUNDED";

export interface Booking {
  id: string;
  showId: string;
  contentTitle: string;
  venueName: string;
  startsAt: string;
  seats: BookingSeat[];
  totalAmount: number;
  currency: string;
  status: BookingStatus;
  expiresAt: string | null;
  createdAt: string;
}

export interface CreateBookingInput {
  showId: string;
  seatIds: Array<string | number>;
  idempotencyKey: string;
}

export type PaymentMethod = "CARD" | "UPI" | "NET_BANKING" | "WALLET";
export type PaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED" | "CANCELLED" | "PARTIALLY_REFUNDED" | "REFUNDED";

export interface Payment {
  id: string;
  bookingId: string;
  amount: number;
  refundedAmount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  providerReference: string;
  checkoutUrl: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentInput {
  bookingId: string;
  paymentMethod: PaymentMethod;
  idempotencyKey: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  amount: number;
  status: string;
  providerReference: string;
  reason: string;
  createdAt: string;
}

export interface Theater {
  id: number;
  name: string;
  address: string;
  city: string;
  country: string;
  timeZone: string;
}

export interface Auditorium {
  id: number;
  name: string;
}

export interface SeatType {
  id: number;
  code: string;
  displayName: string;
  defaultPrice: number;
}

export interface ScheduledShow {
  id: number;
  auditoriumId: number;
  auditoriumName: string;
  contentType: "MOVIE" | "CONCERT";
  contentId: string;
  contentTitle: string;
  startsAt: string;
  endsAt: string;
  intermissionStartsAt: string | null;
  intermissionEndsAt: string | null;
  availableAt: string;
  seatPrices: Array<{ seatTypeCode: string; seatTypeName: string; price: number }>;
}

export interface RoleRequestSummary {
  requestId: number;
  userName: string;
  userEmail: string;
  requestedRole: string;
  requestStatus: string;
  roleStatus: string | null;
  requestedAt: string;
}

export interface RoleAudit {
  auditId: number;
  previousStatus: string | null;
  currentStatus: string;
  reason: string | null;
  changedBy: string;
  changedAt: string;
}
