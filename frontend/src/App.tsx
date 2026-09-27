import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import AppShell from "./components/AppShell";
import AppErrorBoundary from "./components/AppErrorBoundary";
import ProtectedRoute from "./components/ProtectedRoute";
import RequireRole from "./components/RequireRole";
import ScrollToTop from "./components/ScrollToTop";
import AuthBootstrap from "./features/auth/AuthBootstrap";
import Bookings from "./pages/Bookings";
import Checkout from "./pages/Checkout";
import ConcertDetails from "./pages/ConcertDetails";
import Dashboard from "./pages/Dashboard";
import Home from "./pages/Home";
import Login from "./pages/Login";
import MovieDetails from "./pages/MovieDetails";
import NotFound from "./pages/NotFound";
import OAuthCallback from "./pages/OAuthCallback";
import Payments from "./pages/Payments";
import SeatSelection from "./pages/SeatSelection";
import Shows from "./pages/Shows";
import Signup from "./pages/Signup";
import VerifyEmail from "./pages/VerifyEmail";
import AccessAdmin from "./pages/admin/AccessAdmin";
import CatalogAdmin from "./pages/admin/CatalogAdmin";
import TheaterAdmin from "./pages/admin/TheaterAdmin";

export default function App() {
  return (
    <BrowserRouter>
      <AppErrorBoundary>
        <ScrollToTop />
        <AuthBootstrap>
          <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/auth/callback" element={<OAuthCallback />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route element={<AppShell />}>
            <Route index element={<Home />} />
            <Route path="/shows" element={<Shows />} />
            <Route path="/movies/:movieId" element={<MovieDetails />} />
            <Route path="/concerts/:concertId" element={<ConcertDetails />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/shows/:showId" element={<SeatSelection />} />
              <Route path="/checkout/:bookingId" element={<Checkout />} />
              <Route path="/bookings" element={<Bookings />} />
              <Route path="/payments" element={<Payments />} />
              <Route path="/account" element={<Dashboard />} />
              <Route path="/dashboard" element={<Navigate to="/account" replace />} />
            </Route>
            <Route element={<RequireRole anyOf={["ROLE_ADMIN"]} />}>
              <Route path="/admin/catalog" element={<CatalogAdmin />} />
            </Route>
            <Route element={<RequireRole anyOf={["ROLE_THEATER_ADMIN"]} />}>
              <Route path="/admin/theaters" element={<TheaterAdmin />} />
            </Route>
            <Route element={<RequireRole anyOf={["ROLE_SUPERADMIN"]} />}>
              <Route path="/admin/access" element={<AccessAdmin />} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Route>
          </Routes>
        </AuthBootstrap>
      </AppErrorBoundary>
    </BrowserRouter>
  );
}
