import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAppSelector } from "../app/hooks";
import "../pages/auth.css";

export default function ProtectedRoute() {
  const { status } = useAppSelector((state) => state.auth);
  const location = useLocation();

  if (status === "idle" || status === "checking") {
    return <div className="auth-loading">Checking session…</div>;
  }
  if (status !== "authenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
