import { useState } from "react";
import { useLocation } from "react-router-dom";

import { apiErrorMessage } from "../api/error";
import { useLogoutMutation, useMyRoleRequestProgressQuery, useRequestRoleMutation, useResendVerificationMutation } from "../api/authApi";
import { useAppSelector } from "../app/hooks";
import StatusBadge from "../components/StatusBadge";
import { formatDateTime, readableRole } from "../utils/format";

export default function Dashboard() {
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.user);
  const [logout] = useLogoutMutation();
  const [resendVerification, { isLoading: isResending }] = useResendVerificationMutation();
  const [requestRole, { isLoading: isRequestingRole }] = useRequestRoleMutation();
  const roleRequests = useMyRoleRequestProgressQuery();
  const [message, setMessage] = useState<string | null>((location.state as { accessDenied?: boolean } | null)?.accessDenied ? "Your account does not have access to that area." : null);
  const [requestedRole, setRequestedRole] = useState<"ROLE_ADMIN" | "ROLE_THEATER_ADMIN">("ROLE_THEATER_ADMIN");
  const [reason, setReason] = useState("");

  const resend = async () => {
    setMessage(null);
    try {
      await resendVerification().unwrap();
      setMessage("A fresh verification link is on its way.");
    } catch (error) {
      setMessage(apiErrorMessage(error, "Unable to send a verification email."));
    }
  };

  const submitRoleRequest = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage(null);
    try {
      await requestRole({ roleName: requestedRole, reason: reason.trim() || undefined }).unwrap();
      setReason("");
      setMessage("Request submitted. You can follow every decision below.");
    } catch (error) {
      setMessage(apiErrorMessage(error, "Unable to submit the role request."));
    }
  };

  return (
    <section className="account-page">
      <div className="account-hero">
        <div className="profile-avatar">{user?.name?.charAt(0).toUpperCase()}</div>
        <div>
          <span className="eyebrow">YOUR ACCOUNT</span>
          <h1>{user?.name}</h1>
          <p>{user?.email}</p>
          <div className="chip-list">{user?.roles.map((role) => <span className="chip" key={role}>{readableRole(role)}</span>)}</div>
        </div>
        <button className="button button-ghost" onClick={() => void logout()}>Sign out</button>
      </div>
      {message && <div className="notice" role="status">{message}</div>}
      <div className="account-grid">
        <div className="account-main">
          <article className="panel">
            <div className="panel-heading"><div><span className="eyebrow">SECURITY</span><h2>Email verification</h2></div><StatusBadge status={user?.emailVerified ? "VERIFIED" : "PENDING"} /></div>
            {user?.emailVerified ? <p>Your email is verified. Elevated-access requests are available to you.</p> : <><p>Verify your email before requesting theater or catalog administration access.</p><button className="button button-secondary" onClick={resend} disabled={isResending}>{isResending ? "Sending…" : "Resend verification email"}</button></>}
          </article>
          {user?.emailVerified && <article className="panel">
            <span className="eyebrow">ACCESS</span><h2>Request an elevated role</h2>
            <p>Requests are reviewed by a super administrator and remain visible in your audit trail.</p>
            <form className="stack-form" onSubmit={submitRoleRequest}>
              <label className="field"><span>Requested role</span><select className="input" value={requestedRole} onChange={(event) => setRequestedRole(event.target.value as typeof requestedRole)}><option value="ROLE_THEATER_ADMIN">Theater administrator</option><option value="ROLE_ADMIN">Catalog administrator</option></select></label>
              <label className="field"><span>Why do you need access?</span><textarea className="input" value={reason} maxLength={500} onChange={(event) => setReason(event.target.value)} placeholder="Tell the reviewer what you manage." /></label>
              <button className="button button-primary" disabled={isRequestingRole}>{isRequestingRole ? "Submitting…" : "Submit request"}</button>
            </form>
          </article>}
        </div>
        <aside className="panel audit-panel">
          <span className="eyebrow">ROLE AUDIT</span><h2>Request progress</h2>
          {roleRequests.isLoading && <p>Loading progress…</p>}
          {roleRequests.error && <p className="error-text">{apiErrorMessage(roleRequests.error, "Unable to load progress.")}</p>}
          {roleRequests.data?.length === 0 && <p>You have not requested an elevated role.</p>}
          {roleRequests.data?.map((request) => <article className="audit-request" key={request.requestId}>
            <div><strong>{readableRole(request.requestedRole)}</strong><StatusBadge status={request.requestStatus} /></div>
            <small>Requested {formatDateTime(request.requestedAt)}</small>
            <ol>{request.auditTrail.map((audit, index) => <li key={`${audit.changedAt}-${index}`}><i /><div><strong>{audit.currentStatus.replace(/_/g, " ")}</strong><span>{formatDateTime(audit.changedAt)}</span>{audit.reason && <p>{audit.reason}</p>}</div></li>)}</ol>
          </article>)}
        </aside>
      </div>
    </section>
  );
}
