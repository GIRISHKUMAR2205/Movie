import useAuth from "../hooks/useAuth";

export default function Dashboard() {
  const { user, logout, oidcLogout } = useAuth();

  return (
    <div style={{ padding: 40, fontFamily: "sans-serif" }}>
      <h1>Welcome{user?.name ? `, ${user.name}` : ""}</h1>
      <p>Email: {user?.email}</p>
      <button onClick={logout}>Log out</button>
      <button onClick={oidcLogout}>Log out with OIDC</button>
    </div>
  );
}
