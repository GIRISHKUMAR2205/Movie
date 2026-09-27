import { NavLink, Outlet, useLocation } from "react-router-dom";

import { useLogoutMutation } from "../api/authApi";
import { useAppSelector } from "../app/hooks";

const primaryLinks = [
  ["/", "Discover"],
  ["/shows", "Shows"]
] as const;

export default function AppShell() {
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.user);
  const status = useAppSelector((state) => state.auth.status);
  const [logout, { isLoading }] = useLogoutMutation();
  const roles = user?.roles ?? [];
  const authenticated = status === "authenticated";
  const linkClass = ({ isActive }: { isActive: boolean }) => `nav-link${isActive ? " active" : ""}`;

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" className="brand" aria-label="ShowHub home">
          <span className="brand-mark">S</span><span>ShowHub</span>
        </NavLink>
        <nav className="main-nav" aria-label="Main navigation">
          {primaryLinks.map(([to, label]) => <NavLink key={to} to={to} end={to === "/"} className={linkClass}>{label}</NavLink>)}
          {authenticated && <NavLink to="/bookings" className={linkClass}>Bookings</NavLink>}
          {authenticated && <NavLink to="/payments" className={linkClass}>Payments</NavLink>}
          {roles.includes("ROLE_ADMIN") && <NavLink to="/admin/catalog" className={linkClass}>Catalog admin</NavLink>}
          {roles.includes("ROLE_THEATER_ADMIN") && <NavLink to="/admin/theaters" className={linkClass}>Theaters</NavLink>}
          {roles.includes("ROLE_SUPERADMIN") && <NavLink to="/admin/access" className={linkClass}>Access</NavLink>}
        </nav>
        <div className="nav-actions">
          {status === "checking" || status === "idle" ? <span className="nav-muted">Checking session…</span> : authenticated ? (
            <>
              <NavLink to="/account" className="avatar-link" title="Account">
                <span className="avatar">{user?.name?.charAt(0).toUpperCase() || "U"}</span>
                <span className="avatar-name">{user?.name}</span>
              </NavLink>
              <button className="button button-ghost button-small" disabled={isLoading} onClick={() => void logout()}>
                {isLoading ? "Signing out…" : "Sign out"}
              </button>
            </>
          ) : (
            <>
              <NavLink className="button button-ghost button-small" to="/login" state={{ from: location }}>Log in</NavLink>
              <NavLink className="button button-primary button-small" to="/signup">Join free</NavLink>
            </>
          )}
          <details className="mobile-menu">
            <summary aria-label="Open navigation">☰</summary>
            <nav>
              {primaryLinks.map(([to, label]) => <NavLink key={to} to={to}>{label}</NavLink>)}
              {authenticated && <><NavLink to="/bookings">Bookings</NavLink><NavLink to="/payments">Payments</NavLink><NavLink to="/account">Account</NavLink></>}
              {roles.includes("ROLE_ADMIN") && <NavLink to="/admin/catalog">Catalog admin</NavLink>}
              {roles.includes("ROLE_THEATER_ADMIN") && <NavLink to="/admin/theaters">Theater admin</NavLink>}
              {roles.includes("ROLE_SUPERADMIN") && <NavLink to="/admin/access">Access review</NavLink>}
              {authenticated && <button type="button" disabled={isLoading} onClick={() => void logout()}>{isLoading ? "Signing out…" : "Sign out"}</button>}
            </nav>
          </details>
        </div>
      </header>
      <main className="page"><Outlet /></main>
      <footer className="footer"><span>ShowHub</span><span>Movies, concerts, one memorable night.</span></footer>
    </div>
  );
}
