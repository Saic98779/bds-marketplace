import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../../api/client';
import { protect } from '../../api/crypto';
import { portalPrefix, resolveLink, useAuth } from '../../auth/AuthContext';
import { PageTitle } from '../../components/layouts';
import { Docs, F, Filters, PagedTable, payload, useForm, usePaged } from '../../components/kit';
import { ActionModal, Alert, Button, Card, Checkbox, Empty, FilePicker, Icon, KeyValue, Loading, Modal, Status, Textarea, useAction, useLoad } from '../../components/ui';
import { ago, dateTime, label } from '../../constants';

/* ---------- Notifications ---------- */
export function Notifications() {
  const { role } = useAuth();
  const navigate = useNavigate();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const paged = usePaged('/notifications', { unreadOnly: unreadOnly || undefined }, 15);
  const [run] = useAction();
  const open = async (n) => {
    if (!n.read) await api.post(`/notifications/${n.id}/read`).catch(() => {});
    const link = resolveLink(n.linkUrl, role);
    if (link) navigate(link); else paged.reload();
  };
  return (
    <>
      <PageTitle title="Notifications" actions={<>
        <Checkbox label="Unread only" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
        <Button variant="outline" icon="done_all" onClick={() => run(() => api.post('/notifications/read-all'), 'All marked as read').then(paged.reload)}>Mark all read</Button>
      </>} />
      <Card>
        {paged.loading && !paged.data ? <Loading /> : !paged.data?.content.length ? <Empty icon="notifications_off" title="No notifications" /> : (
          <div className="col gap-8">
            {paged.data.content.map((n) => (
              <div key={n.id} className="row top gap-16" style={{ padding: 12, borderRadius: 8, cursor: 'pointer', background: n.read ? undefined : 'var(--bg-50)' }} onClick={() => open(n)}>
                <Icon name={n.read ? 'notifications' : 'notifications_active'} className={n.read ? 'muted' : ''} />
                <div className="grow">
                  <div className="row between"><span className="bold small">{n.title}</span><span className="tiny muted">{ago(n.createdAt)}</span></div>
                  <div className="small">{n.message}</div>
                  <div className="tiny muted mt-4">{label(n.category)}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  );
}

/* ---------- Account settings ---------- */
export function AccountSettings() {
  const { user, refreshUser, reissue } = useAuth();
  const [params] = useSearchParams();
  const form = useForm({ currentPassword: '', newPassword: '', confirm: '' });
  const [run, busy] = useAction();
  const change = async (e) => {
    e.preventDefault();
    if (form.values.newPassword !== form.values.confirm) { form.setErrors({ confirm: 'Passwords do not match' }); return; }
    await run(async () => api.post('/auth/password/change', { currentPassword: await protect(form.values.currentPassword), newPassword: await protect(form.values.newPassword) }), 'Password changed');
    form.setValues({ currentPassword: '', newPassword: '', confirm: '' });
    await reissue().catch(() => {});
    await refreshUser().catch(() => {});
  };
  return (
    <>
      <PageTitle title="Account settings" />
      {(params.get('force') || user.mustChangePassword) && <Alert tone="warn" title="Change your temporary password">For security, choose a new password before continuing.</Alert>}
      <div className="grid grid-2 mt-16">
        <Card title="Profile">
          <KeyValue items={[['Name', user.fullName], ['Email', user.email], ['Mobile', user.mobile], ['Role', label(user.role)], ['Reference', <span className="mono">{user.referenceNo}</span>], ['Status', <Status value={user.status} />], ['Last login', dateTime(user.lastLoginAt)]]} />
        </Card>
        <Card title="Change password">
          <form className="col gap-16" onSubmit={change}>
            <F form={form} name="currentPassword" label="Current password" type="password" required autoComplete="current-password" />
            <F form={form} name="newPassword" label="New password" type="password" required autoComplete="new-password" hint="8+ characters, upper & lower case, digit and symbol" />
            <F form={form} name="confirm" label="Confirm new password" type="password" required autoComplete="new-password" />
            <Button type="submit" loading={busy}>Update password</Button>
          </form>
        </Card>
        <Card title="Notification preferences">
          <Checkbox label="Email me alerts for RFQs, quotations, engagements and queries" checked={!!user.emailAlerts}
            onChange={(e) => run(() => api.put('/auth/me/preferences', { emailAlerts: e.target.checked }), 'Preferences saved').then(refreshUser)} />
        </Card>
      </div>
    </>
  );
}

/* ---------- Queries & grievances ---------- */
const TICKET_STATUSES = ['OPEN', 'RESPONDED', 'RESOLVED', 'REOPENED', 'ESCALATED', 'CLOSED'];
const CONTEXTS = ['RFQ', 'QUOTATION', 'WORK_ORDER', 'ENGAGEMENT', 'PLATFORM'];

export function QueryList({ admin }) {
  const { role } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState(admin ? {} : {});
  const [raise, setRaise] = useState(null);
  const paged = usePaged(admin ? '/admin/queries' : '/queries', filters);
  const base = admin ? '/admin/grievances' : `${portalPrefix(role)}/queries`;
  return (
    <>
      <PageTitle title={admin ? 'Grievances & escalated queries' : 'Query tracker'} subtitle={admin ? 'Queries escalated by users and grievances raised with the platform' : 'Questions and issues with your counterparties, and grievances raised with the platform'}
        actions={!admin && <><Button variant="outline" icon="support_agent" onClick={() => setRaise('GRIEVANCE')}>Raise grievance</Button><Button icon="add" onClick={() => setRaise('QUERY')}>New query</Button></>} />
      <Filters value={filters} onChange={setFilters} fields={[
        { name: 'status', placeholder: 'Any status', options: TICKET_STATUSES },
        { name: 'ticketType', placeholder: 'Queries & grievances', options: ['QUERY', 'GRIEVANCE'] },
        ...(admin ? [] : [{ name: 'contextType', placeholder: 'Any context', options: CONTEXTS }])
      ]} />
      <Card>
        <PagedTable paged={paged} onRow={(t) => navigate(`${base}/${t.id}`)} emptyIcon="forum" empty="No queries yet" columns={[
          { key: 'ticketNumber', title: 'Ticket', render: (t) => <span className="mono small">{t.ticketNumber}</span> },
          { key: 'subject', title: 'Subject', render: (t) => <><div className="bold small">{t.subject}</div><div className="tiny muted">{label(t.ticketType)} · {label(t.contextType)} {t.contextReference || ''}</div></> },
          { key: 'parties', title: 'Parties', render: (t) => <div className="small">{[t.buyerName, t.sellerName].filter(Boolean).join(' ↔ ') || t.raisedByName}</div> },
          { key: 'priority', title: 'Priority', render: (t) => <Status value={t.priority} /> },
          { key: 'status', title: 'Status', render: (t) => <div className="col gap-4"><Status value={t.status} />{t.overdue && <span className="tiny" style={{ color: 'var(--danger)' }}>Overdue</span>}{t.awaitingMyResponse && <span className="tiny bold">Awaiting you</span>}</div> },
          { key: 'lastActivityAt', title: 'Last activity', render: (t) => <span className="small muted">{ago(t.lastActivityAt)}</span> }
        ]} />
      </Card>
      {raise && <RaiseTicket type={raise} onClose={() => setRaise(null)} onDone={(t) => navigate(`${base}/${t.summary.id}`)} />}
    </>
  );
}

export function RaiseTicket({ type = 'QUERY', contextType, contextId, counterpartyId, onClose, onDone }) {
  const form = useForm({ contextType: contextType || (type === 'GRIEVANCE' ? 'PLATFORM' : 'RFQ'), contextId: contextId || '', counterpartyId: counterpartyId || '', subject: '', description: '', priority: 'MEDIUM' });
  const [run, busy] = useAction();
  const fixed = !!contextType;
  const submit = async () => {
    const body = payload(form.values, ['contextId', 'counterpartyId']);
    const res = await run(() => api.post(type === 'GRIEVANCE' ? '/grievances' : '/queries', body), type === 'GRIEVANCE' ? 'Grievance raised' : 'Query raised');
    onDone?.(res.data);
    onClose();
  };
  return (
    <Modal title={type === 'GRIEVANCE' ? 'Raise a grievance' : 'Raise a query'} onClose={onClose}
      footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit} disabled={!form.values.subject || !form.values.description}>Submit</Button></>}>
      <div className="col gap-16">
        {!fixed && (
          <div className="grid grid-2">
            <F form={form} name="contextType" label="Related to" type="select" options={type === 'GRIEVANCE' ? CONTEXTS : CONTEXTS.filter((c) => c !== 'PLATFORM')} />
            {form.values.contextType !== 'PLATFORM' && <F form={form} name="contextId" label={`${label(form.values.contextType)} ID`} type="number" hint="Numeric ID shown in the URL of the record" />}
          </div>
        )}
        <F form={form} name="subject" label="Subject" required maxLength={200} />
        <F form={form} name="description" label="Description" type="textarea" required rows={5} />
        <F form={form} name="priority" label="Priority" type="select" options={['LOW', 'MEDIUM', 'HIGH']} />
      </div>
    </Modal>
  );
}

export function QueryDetail({ admin }) {
  const { id } = useParams();
  const { user } = useAuth();
  const { data: t, reload, loading } = useLoad(() => api.get(admin ? `/admin/queries/${id}` : `/queries/${id}`), [id]);
  const [body, setBody] = useState('');
  const [action, setAction] = useState(null);
  const [run, busy] = useAction();
  if (loading && !t) return <Loading />;
  if (!t) return <Empty title="Query not found" />;
  const s = t.summary;
  const base = admin ? '/admin/queries' : '/queries';
  const reply = () => run(() => api.post(`${base}/${id}/messages`, { body }), 'Reply sent').then(() => { setBody(''); reload(); });
  const actions = admin
    ? [['resolve', 'Resolve', true], ['close', 'Close', true]]
    : [['resolve', 'Mark resolved', t.canResolve], ['reopen', 'Reopen', t.canReopen], ['escalate', 'Escalate to platform', t.canEscalate], ['close', 'Close', t.canClose]];
  const canReply = admin ? s.status !== 'CLOSED' : t.canReply;
  return (
    <>
      <PageTitle back title={s.subject} subtitle={<span className="mono">{s.ticketNumber}</span>} actions={actions.filter(([, , ok]) => ok).map(([k, text]) => <Button key={k} variant={k === 'escalate' ? 'danger' : 'outline'} onClick={() => setAction(k)}>{text}</Button>)} />
      <div className="layout-main-side">
        <div className="col gap-16">
          <Card title="Conversation">
            <div className="chat">
              <div className="msg"><div className="meta">{s.raisedByName} · {dateTime(s.createdAt)}</div><div className="pre small">{t.description}</div></div>
              {t.messages.map((m) => (
                <div key={m.id} className={`msg ${m.systemGenerated ? 'system' : m.senderId === user.id ? 'mine' : ''}`}>
                  {!m.systemGenerated && <div className="meta">{m.senderName} ({label(m.senderRole)}) · {dateTime(m.createdAt)}</div>}
                  <div className="pre small">{m.body}</div>
                </div>
              ))}
            </div>
            {canReply && (
              <div className="col gap-8 mt-16">
                <Textarea value={body} onChange={(e) => setBody(e.target.value)} placeholder="Write a reply..." rows={3} />
                <div className="row between">
                  {!admin ? <FilePicker label="Attach file" busy={busy} onFile={(file) => run(() => api.upload(`/queries/${id}/attachments`, { file }), 'File attached').then(reload)} /> : <span />}
                  <Button icon="send" loading={busy} disabled={!body.trim()} onClick={reply}>Send</Button>
                </div>
              </div>
            )}
          </Card>
          {t.attachments.length > 0 && <Card title="Attachments"><Docs documents={t.attachments} canDelete={!admin} onChange={reload} /></Card>}
        </div>
        <Card title="Details">
          <KeyValue items={[
            ['Type', label(s.ticketType)], ['Status', <Status value={s.status} />], ['Priority', <Status value={s.priority} />],
            ['Context', `${label(s.contextType)} ${s.contextReference || ''}`], ['Purchaser', s.buyerName], ['Provider', s.sellerName],
            ['Raised by', `${s.raisedByName} (${label(s.raisedByRole)})`], ['Due', dateTime(s.dueAt)], ['Escalated', s.escalated ? 'Yes' : 'No'],
            ['Escalation reason', t.escalationReason], ['Resolution', t.resolutionNote], ['Resolved', dateTime(t.resolvedAt)], ['Closed', dateTime(t.closedAt)]
          ]} />
        </Card>
      </div>
      {action && <ActionModal title={label(action)} remarks remarksLabel={action === 'escalate' ? 'Reason for escalation' : 'Remarks'} remarksRequired={action === 'escalate'}
        onConfirm={(remarks) => api.post(`${base}/${id}/${action}`, { remarks }).then(reload)} onClose={() => setAction(null)} />}
    </>
  );
}

/* ---------- Reusable bits ---------- */
export function RemarksAction({ title, url, required, label: lbl = 'Remarks', field = 'remarks', variant, onDone, onClose, description }) {
  return <ActionModal title={title} description={description} remarks remarksRequired={required} remarksLabel={lbl} variant={variant}
    onConfirm={(text) => api.post(url, { [field]: text }).then(onDone)} onClose={onClose} />;
}

export function Back({ to, children }) {
  return <Link to={to} className="small"><Icon name="arrow_back" size="sm" />{children}</Link>;
}

