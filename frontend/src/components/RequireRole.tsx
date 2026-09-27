import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAppSelector } from "../app/hooks";

export default function RequireRole({ anyOf }: { anyOf: string[] }) {
  const location = useLocation();
  const { status, user } = useAppSelector((state) => state.auth);
  if (status === "idle" || status === "checking") return <div className="state-card">Checking access…</div>;
  if (status !== "authenticated") return <Navigate to="/login" replace state={{ from: location }} />;
  if (!anyOf.some((role) => user?.roles.includes(role))) return <Navigate to="/account" replace state={{ accessDenied: true }} />;
  return <Outlet />;
}
