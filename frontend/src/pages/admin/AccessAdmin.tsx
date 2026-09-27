import { FormEvent, useState } from "react";

import { apiErrorMessage } from "../../api/error";
import { useDecideRoleRequestMutation, useRoleApprovalsQuery, useRoleAuditQuery } from "../../api/platformApi";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "../../components/Feedback";
import StatusBadge from "../../components/StatusBadge";
import { formatDateTime, readableRole } from "../../utils/format";

type Decision = "approve" | "reject" | "revoke" | "reactivate";

export default function AccessAdmin() {
  const requests = useRoleApprovalsQuery();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const audit = useRoleAuditQuery(selectedId ?? 0, { skip: selectedId === null });
  const [decide, decisionState] = useDecideRoleRequestMutation();
  const [decision, setDecision] = useState<{ id: number; action: Decision } | null>(null);
  const [reason, setReason] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!decision) return;
    setMessage(null);
    try {
      await decide({ ...decision, reason: reason.trim() || undefined }).unwrap();
      setMessage(`Request ${decision.action} action completed.`);
      setDecision(null);
      setReason("");
    } catch (error) {
      setMessage(apiErrorMessage(error, "The role decision could not be completed."));
    }
  };

  if (requests.isLoading) return <LoadingBlock label="Loading access requests…" />;
  if (requests.error) return <ErrorBlock error={requests.error} onRetry={requests.refetch} />;
  return (
    <section className="admin-page">
      <div className="page-title"><span className="eyebrow">SUPER ADMIN</span><h1>Access review</h1><p>Only verified users may request roles. Every decision is retained in the audit trail.</p></div>
      {message && <div className="notice">{message}</div>}
      {!requests.data?.length ? <EmptyBlock title="Review queue is clear" message="New role requests will appear here." /> : (
        <div className="access-grid">
          <div className="request-list">{requests.data.map((request) => <article className={selectedId === request.requestId ? "panel request-card active" : "panel request-card"} key={request.requestId} onClick={() => setSelectedId(request.requestId)}>
            <div className="panel-heading"><div><h2>{request.userName}</h2><p>{request.userEmail}</p></div><StatusBadge status={request.requestStatus} /></div>
            <div className="request-details"><span>Requests <strong>{readableRole(request.requestedRole)}</strong></span><span>{formatDateTime(request.requestedAt)}</span></div>
            <div className="form-actions">
              {request.requestStatus === "PENDING" && <><button className="button button-primary button-small" onClick={(event) => { event.stopPropagation(); setDecision({ id: request.requestId, action: "approve" }); }}>Approve</button><button className="button button-danger button-small" onClick={(event) => { event.stopPropagation(); setDecision({ id: request.requestId, action: "reject" }); }}>Reject</button></>}
              {request.roleStatus === "ACTIVE" && <button className="button button-danger button-small" onClick={(event) => { event.stopPropagation(); setDecision({ id: request.requestId, action: "revoke" }); }}>Revoke</button>}
              {request.roleStatus === "REVOKED" && <button className="button button-secondary button-small" onClick={(event) => { event.stopPropagation(); setDecision({ id: request.requestId, action: "reactivate" }); }}>Reactivate</button>}
              <button className="button button-ghost button-small" onClick={(event) => { event.stopPropagation(); setSelectedId(request.requestId); }}>Audit trail</button>
            </div>
          </article>)}</div>
          <aside className="panel audit-detail"><span className="eyebrow">DECISION HISTORY</span><h2>{selectedId ? `Request #${selectedId}` : "Select a request"}</h2>{selectedId && (audit.isLoading ? <LoadingBlock /> : audit.error ? <ErrorBlock error={audit.error} /> : <ol className="timeline">{audit.data?.map((item) => <li key={item.auditId}><i /><div><strong>{item.currentStatus.replace(/_/g, " ")}</strong><span>{formatDateTime(item.changedAt)} · {item.changedBy}</span>{item.reason && <p>{item.reason}</p>}</div></li>)}</ol>)}</aside>
        </div>
      )}
      {decision && <div className="modal-backdrop" onMouseDown={() => setDecision(null)}><form className="modal" onSubmit={submit} onMouseDown={(event) => event.stopPropagation()}><span className="eyebrow">ACCESS DECISION</span><h2>{decision.action.charAt(0).toUpperCase() + decision.action.slice(1)} request?</h2><label className="field"><span>Reason {decision.action === "approve" ? "(optional)" : ""}</span><textarea className="input" required={decision.action !== "approve"} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} /></label><div className="form-actions"><button type="button" className="button button-ghost" onClick={() => setDecision(null)}>Cancel</button><button className={decision.action === "approve" || decision.action === "reactivate" ? "button button-primary" : "button button-danger"} disabled={decisionState.isLoading}>{decisionState.isLoading ? "Saving…" : "Confirm"}</button></div></form></div>}
    </section>
  );
}
