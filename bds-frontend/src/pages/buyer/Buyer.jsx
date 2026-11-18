import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, fieldErrors } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { PageTitle } from '../../components/layouts';
import { DataTable, Docs, ExportButtons, F, Filters, PagedTable, payload, useForm, usePaged } from '../../components/kit';
import { ActionModal, Alert, Button, Card, Empty, Field, Icon, Input, Loading, Modal, StarInput, StatCard, Stars, Status, Textarea, useAction, useLoad } from '../../components/ui';
import { CONSTITUTION_LABELS, CONSTITUTIONS, date, inr, INDUSTRIES, label, PURCHASER_LABELS, PURCHASER_TYPES, STATES } from '../../constants';
import { ProviderDetail, ProviderSearch } from '../public/Providers';
import { ServiceDetail, ServiceSearch } from '../public/Services';
import { RaiseTicket } from '../shared/Common';
import { ENGAGEMENT_STATUSES, EngagementInfo, engagementColumns, Invitations, QUOTATION_STATUSES, QuotationInfo, quotationColumns, RFQ_STATUSES, RfqForm, RfqInfo, rfqColumns, Versions, WORK_ORDER_STATUSES, WorkOrderModal, workOrderColumns } from '../shared/Procurement';

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));

/* ---------- Dashboard ---------- */
export function BuyerDashboard() {
  const navigate = useNavigate();
  const { data: d, loading } = useLoad(() => api.get('/buyer/dashboard'), []);
  if (loading && !d) return <Loading />;
  if (!d) return <Empty title="Dashboard unavailable" />;
  return (
    <>
      <PageTitle title="Dashboard" actions={<Link to="/buyer/rfqs/new" className="btn btn-primary"><Icon name="add" size="sm" />New RFQ</Link>} />
      <div className="grid grid-4">
        <StatCard icon="description" value={d.openRfqs} label="Open RFQs" sub={`${d.totalRfqs} total`} onClick={() => navigate('/buyer/rfqs')} />
        <StatCard icon="request_quote" value={d.quotationsReceived} label="Quotations received" onClick={() => navigate('/buyer/quotations')} />
        <StatCard icon="compare" value={d.comparisonPending} label="Awaiting evaluation" tone="warn" />
        <StatCard icon="handshake" value={d.activeEngagements} label="Active engagements" sub={`${d.completedEngagements} completed`} onClick={() => navigate('/buyer/engagements')} />
        <StatCard icon="assignment" value={d.workOrderRequests} label="Work order requests" tone={d.workOrderRequests ? 'warn' : undefined} onClick={() => navigate('/buyer/work-orders')} />
        <StatCard icon="task_alt" value={d.deliveriesToConfirm} label="Deliveries to confirm" tone={d.deliveriesToConfirm ? 'warn' : undefined} />
        <StatCard icon="reviews" value={d.feedbackPending} label="Feedback pending" />
        <StatCard icon="forum" value={d.openQueries} label="Open queries" onClick={() => navigate('/buyer/queries')} />
      </div>
      <div className="grid grid-2 mt-16">
        <Card title="Recent RFQs" actions={<Link to="/buyer/rfqs" className="small">View all</Link>}>
          <DataTable rows={d.recentRfqs} columns={rfqColumns([{ key: 'quotationCount', title: 'Quotes' }])} onRow={(r) => navigate(`/buyer/rfqs/${r.id}`)} empty="No RFQs yet" />
        </Card>
        <Card title="Recent engagements" actions={<Link to="/buyer/engagements" className="small">View all</Link>}>
          <DataTable rows={d.recentEngagements} columns={engagementColumns([{ key: 'sellerName', title: 'Provider' }])} onRow={(e) => navigate(`/buyer/engagements/${e.id}`)} empty="No engagements yet" />
        </Card>
      </div>
    </>
  );
}

/* ---------- Discovery ---------- */
export function FindServices() {
  const { id } = useParams();
  return <>{!id && <PageTitle title="Find services" />}{id ? <ServiceDetail embedded providerBase="/buyer/find-providers" /> : <ServiceSearch embedded base="/buyer/find-services" />}</>;
}
export function FindProviders() {
  const { id } = useParams();
  return <>{!id && <PageTitle title="Find providers" />}{id ? <ProviderDetail embedded serviceBase="/buyer/find-services" /> : <ProviderSearch embedded base="/buyer/find-providers" />}</>;
}

/* ---------- RFQs ---------- */
export function BuyerRfqs() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/buyer/rfqs', filters);
  return (
    <>
      <PageTitle title="My RFQs" actions={<Link to="/buyer/rfqs/new" className="btn btn-primary"><Icon name="add" size="sm" />New RFQ</Link>} />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: RFQ_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(r) => navigate(`/buyer/rfqs/${r.id}`)} empty="No RFQs yet" emptyIcon="description" columns={rfqColumns([
        { key: 'invitedCount', title: 'Invited', render: (r) => r.invitedCount }, { key: 'quotationCount', title: 'Quotes', render: (r) => <b>{r.quotationCount}</b> }
      ])} /></Card>
    </>
  );
}

export function BuyerRfqEdit() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [run, busy] = useAction();
  const [initial, setInitial] = useState(id ? null : undefined);
  const [sellers, setSellers] = useState([]);
  const listingId = params.get('listingId');
  const sellerId = params.get('sellerId');
  useEffect(() => {
    if (id) {
      api.get(`/buyer/rfqs/${id}`).then((r) => {
        const s = r.summary;
        setSellers((r.invitations || []).map((i) => ({ id: i.sellerId, name: i.sellerName })));
        setInitial({
          title: s.title, categoryId: String(s.categoryId), description: r.description, scopeOfWork: r.scopeOfWork || '', deliverables: r.deliverables || '', industry: r.industry || '',
          projectState: s.projectState || '', projectDistrict: s.projectDistrict || '', deliveryMode: s.deliveryMode || '', budgetMin: s.budgetMin ?? '', budgetMax: s.budgetMax ?? '',
          expectedDuration: r.expectedDuration || '', requiredExperience: r.requiredExperience ?? '', expectedStartDate: r.expectedStartDate || '',
          submissionDeadline: (s.submissionDeadline || '').slice(0, 16), distributionMode: s.distributionMode, sellerIds: (r.invitations || []).map((i) => String(i.sellerId)), sourceListingId: r.sourceListingId || ''
        });
      });
    } else if (listingId) {
      api.get(`/public/services/${listingId}`).then((l) => {
        const s = l.summary;
        setSellers([{ id: s.sellerId, name: s.sellerName }]);
        setInitial({ title: s.title, categoryId: String(s.categoryId), subcategoryId: s.subcategoryId ? String(s.subcategoryId) : '', deliveryMode: s.deliveryMode, sourceListingId: s.id, distributionMode: 'SELECTED', sellerIds: [String(s.sellerId)] });
      }).catch(() => setInitial({}));
    } else if (sellerId) {
      api.get(`/public/providers/${sellerId}`).then((p) => {
        setSellers([{ id: p.summary.id, name: p.summary.organisationName }]);
        setInitial({ distributionMode: 'SELECTED', sellerIds: [String(p.summary.id)] });
      }).catch(() => setInitial({}));
    } else setInitial({});
  }, [id, listingId, sellerId]);
  if (initial === null || initial === undefined) return <Loading />;
  const save = async (body, form) => {
    try {
      const res = await run(() => (id ? api.put(`/buyer/rfqs/${id}`, body) : api.post('/buyer/rfqs', body)), body.publish ? 'RFQ published' : 'Draft saved');
      navigate(`/buyer/rfqs/${res.data.summary.id}`);
    } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  return (
    <>
      <PageTitle back title={id ? 'Edit RFQ' : 'New request for quotation'} subtitle="Describe your requirement; verified providers respond with technical and financial proposals." />
      <RfqForm key={JSON.stringify(initial)} initial={initial} sellers={sellers} onSubmit={save} busy={busy} />
    </>
  );
}

export function BuyerRfqDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: rfq, reload } = useLoad(() => api.get(`/buyer/rfqs/${id}`), [id]);
  const { data: quotes, reload: reloadQuotes } = useLoad(() => api.get(`/buyer/rfqs/${id}/quotations`), [id]);
  const [modal, setModal] = useState(null);
  const [run, busy] = useAction();
  if (!rfq) return <Loading />;
  const s = rfq.summary;
  const live = ['PUBLISHED', 'RESPONSES_RECEIVED', 'UNDER_EVALUATION'].includes(s.status);
  const finished = ['CLOSED', 'CANCELLED', 'EXPIRED', 'NO_RESPONSE', 'PROVIDER_SELECTED'].includes(s.status);
  const refresh = () => { reload(); reloadQuotes(); };
  return (
    <>
      <PageTitle back title={s.title} subtitle={<><span className="mono">{s.rfqNumber}</span> · <Status value={s.status} /></>} actions={<>
        {s.status === 'DRAFT' && <><Link to={`/buyer/rfqs/${id}/edit`} className="btn btn-outline"><Icon name="edit" size="sm" />Edit</Link>
          <Button icon="send" loading={busy} onClick={() => run(() => api.post(`/buyer/rfqs/${id}/publish`), 'RFQ published').then(refresh)}>Publish</Button></>}
        {s.quotationCount > 0 && <Link to={`/buyer/rfqs/${id}/comparison`} className="btn btn-primary"><Icon name="compare" size="sm" />Compare quotations</Link>}
        {live && <Button variant="outline" icon="person_add" onClick={() => setModal('invite')}>Invite providers</Button>}
        {live && <Button variant="outline" icon="event" onClick={() => setModal('extend')}>Extend deadline</Button>}
        {!finished && s.status !== 'DRAFT' && <Button variant="ghost" onClick={() => setModal('close')}>Close</Button>}
        {!finished && <Button variant="ghost" onClick={() => setModal('cancel')}>Cancel RFQ</Button>}
      </>} />
      <div className="layout-main-side mt-16">
        <div className="col gap-16">
          <RfqInfo rfq={rfq} docs={<Docs documents={rfq.attachments} uploadUrl={!finished ? `/buyer/rfqs/${id}/attachments` : null} category="RFQ" canDelete={!finished} onChange={reload} />} />
          <Card title={`Quotations received (${quotes?.length || 0})`}>
            <DataTable rows={quotes} onRow={(q) => navigate(`/buyer/quotations/${q.id}`)} empty="No quotations yet" columns={quotationColumns([{ key: 'sellerName', title: 'Provider', render: (q) => <><div className="small bold">{q.sellerName}</div>{q.sellerRating ? <Stars value={q.sellerRating} /> : null}</> }])} />
          </Card>
        </div>
        <Card title="Invited providers"><Invitations invitations={rfq.invitations} /></Card>
      </div>
      {modal === 'invite' && <InviteModal rfq={rfq} url={`/buyer/rfqs/${id}/invite`} recommendedUrl={`/buyer/recommended-sellers?categoryId=${s.categoryId}`} onDone={refresh} onClose={() => setModal(null)} />}
      {modal === 'extend' && <ExtendModal rfq={rfq} onDone={refresh} onClose={() => setModal(null)} />}
      {modal === 'close' && <ActionModal title="Close RFQ" remarks onConfirm={(remarks) => api.post(`/buyer/rfqs/${id}/close`, { remarks }).then(refresh)} onClose={() => setModal(null)} />}
      {modal === 'cancel' && <ActionModal title="Cancel RFQ" variant="danger" remarks remarksRequired remarksLabel="Reason" onConfirm={(remarks) => api.post(`/buyer/rfqs/${id}/cancel`, { remarks }).then(refresh)} onClose={() => setModal(null)} />}
    </>
  );
}

export function InviteModal({ rfq, url, recommendedUrl, onDone, onClose }) {
  const [list, setList] = useState(null);
  const [selected, setSelected] = useState([]);
  const [run, busy] = useAction();
  const invited = new Set(rfq.invitations.map((i) => i.sellerId));
  useEffect(() => {
    const [path, query] = recommendedUrl.split('?');
    api.get(path, Object.fromEntries(new URLSearchParams(query || ''))).then(setList).catch(() => setList([]));
  }, [recommendedUrl]);
  const toggle = (id) => setSelected(selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id]);
  return (
    <Modal title="Invite verified providers" onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button>
      <Button loading={busy} disabled={!selected.length} onClick={() => run(() => api.post(url, { sellerIds: selected }), 'Providers invited').then(() => { onDone(); onClose(); })}>Invite {selected.length || ''}</Button></>}>
      {!list ? <Loading /> : list.length === 0 ? <Empty title="No recommended providers for this category" /> : (
        <div className="col gap-8">
          {list.map((p) => (
            <label key={p.sellerId} className="row between" style={{ padding: '8px 12px', border: '1px solid var(--line-100)', borderRadius: 8, opacity: invited.has(p.sellerId) ? 0.5 : 1 }}>
              <span className="row gap-8"><input type="checkbox" disabled={invited.has(p.sellerId)} checked={selected.includes(p.sellerId)} onChange={() => toggle(p.sellerId)} />
                <span><div className="small bold">{p.name}</div><div className="tiny muted">{[p.district, p.state].filter(Boolean).join(', ')}</div></span></span>
              <span>{invited.has(p.sellerId) ? <span className="tiny">Already invited</span> : <Stars value={p.rating} count={p.ratingCount} />}</span>
            </label>
          ))}
        </div>
      )}
    </Modal>
  );
}

function ExtendModal({ rfq, onDone, onClose }) {
  const [deadline, setDeadline] = useState((rfq.summary.submissionDeadline || '').slice(0, 16));
  const [remarks, setRemarks] = useState('');
  const [run, busy] = useAction();
  return (
    <Modal title="Extend submission deadline" onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button>
      <Button loading={busy} onClick={() => run(() => api.post(`/buyer/rfqs/${rfq.summary.id}/extend-deadline`, { submissionDeadline: deadline.length === 16 ? `${deadline}:00` : deadline, remarks }), 'Deadline extended').then(() => { onDone(); onClose(); })}>Extend</Button></>}>
      <div className="col gap-16">
        <Field label="New deadline" required><Input type="datetime-local" value={deadline} onChange={(e) => setDeadline(e.target.value)} /></Field>
        <Field label="Remarks"><Input value={remarks} onChange={(e) => setRemarks(e.target.value)} /></Field>
      </div>
    </Modal>
  );
}

/* ---------- Comparison ---------- */
export function BuyerComparison({ admin }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const url = admin ? `/admin/rfqs/${id}/comparison` : `/buyer/rfqs/${id}/comparison`;
  const { data: c, reload } = useLoad(() => api.get(url), [id]);
  const [criteria, setCriteria] = useState([]);
  const [scores, setScores] = useState({});
  const [accepting, setAccepting] = useState(null);
  const [run, busy] = useAction();
  useEffect(() => {
    if (!c) return;
    setCriteria(c.criteria.length ? c.criteria.map(({ name, weight }) => ({ name, weight })) : [{ name: 'Technical approach', weight: 40 }, { name: 'Experience', weight: 30 }, { name: 'Price', weight: 30 }]);
    const s = {};
    c.rows.forEach((r) => Object.entries(r.scores || {}).forEach(([k, v]) => { s[`${r.quotationId}:${k}`] = v; }));
    setScores(s);
  }, [c]);
  if (!c) return <Loading />;
  const totalWeight = criteria.reduce((t, k) => t + Number(k.weight || 0), 0);
  const save = () => run(() => api.post(url, {
    criteria: criteria.map((k) => ({ name: k.name, weight: Number(k.weight) })),
    scores: Object.entries(scores).filter(([, v]) => v !== '' && v != null).map(([key, v]) => { const [quotationId, criterionIndex] = key.split(':'); return { quotationId: Number(quotationId), criterionIndex: Number(criterionIndex), score: Number(v) }; })
  }), 'Evaluation saved').then(reload);
  const rows = c.rows;
  const row = (title, render, highlight) => (
    <tr><td className="label-cell">{title}</td>{rows.map((r) => <td key={r.quotationId} className={highlight?.(r) ? 'highlight' : ''}>{render(r)}</td>)}</tr>
  );
  return (
    <>
      <PageTitle back title="Quotation comparison" subtitle={`${c.rfq.title} · ${c.rfq.rfqNumber}`} actions={!admin && <ExportButtons url={`/buyer/rfqs/${id}/comparison/export`} />} />
      {c.note && <Alert tone="info">{c.note}</Alert>}
      {rows.length === 0 ? <Card><Empty title="No quotations to compare yet" /></Card> : (
        <Card className="mt-16">
          <div className="table-wrap">
            <table className="table compare-table">
              <thead><tr><th>Criteria</th>{rows.map((r) => <th key={r.quotationId}>{r.sellerName}<div className="tiny muted mono">{r.quotationNumber} v{r.versionNo}</div></th>)}</tr></thead>
              <tbody>
                {row('Status', (r) => <div className="row gap-4"><Status value={r.status} />{r.shortlisted && <Icon name="bookmark" size="sm" />}</div>)}
                {row('Rating', (r) => <Stars value={r.sellerRating} />)}
                {row('Professional fee', (r) => inr(r.professionalFee))}
                {row('Tax', (r) => inr(r.taxAmount))}
                {row('Other expenses', (r) => inr(r.otherExpenses))}
                {row('Total', (r) => <b>{inr(r.totalAmount)}</b>, (r) => r.lowestPrice)}
                {row('Timeline', (r) => `${r.timelineDays} days`, (r) => r.shortestTimeline)}
                {row('Valid until', (r) => date(r.validUntil))}
                {row('Scope accepted', (r) => (r.scopeAccepted ? 'Yes' : 'With deviations'))}
                {row('Methodology', (r) => <div className="small pre" style={{ maxHeight: 120, overflow: 'auto' }}>{r.methodology || '-'}</div>)}
                {row('Deliverables', (r) => <div className="small pre" style={{ maxHeight: 120, overflow: 'auto' }}>{r.deliverables || '-'}</div>)}
                {row('Experience', (r) => <div className="small pre" style={{ maxHeight: 120, overflow: 'auto' }}>{r.relevantExperience || '-'}</div>)}
                {criteria.map((k, i) => row(`${k.name} (${k.weight}%)`, (r) => admin ? (r.scores?.[i] ?? '-') : (
                  <input className="input" type="number" min="0" max="10" step="0.5" style={{ width: 80 }} value={scores[`${r.quotationId}:${i}`] ?? ''} onChange={(e) => setScores({ ...scores, [`${r.quotationId}:${i}`]: e.target.value })} />
                )))}
                {row('Weighted score', (r) => <b>{r.weightedScore != null ? Number(r.weightedScore).toFixed(2) : '-'}</b>, (r) => r.rank === 1)}
                {row('Rank', (r) => r.rank ?? '-')}
                {!admin && row('Actions', (r) => (
                  <div className="col gap-4">
                    <Button size="sm" variant="ghost" onClick={() => navigate(`/buyer/quotations/${r.quotationId}`)}>View</Button>
                    {!['ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED'].includes(r.status) && <>
                      <Button size="sm" variant="outline" onClick={() => run(() => api.post(`/buyer/quotations/${r.quotationId}/shortlist`, { shortlisted: !r.shortlisted }), 'Shortlist updated').then(reload)}>{r.shortlisted ? 'Remove shortlist' : 'Shortlist'}</Button>
                      <Button size="sm" onClick={() => setAccepting(r)}>Accept</Button>
                    </>}
                  </div>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
      {!admin && rows.length > 0 && (
        <Card title="Evaluation criteria" className="mt-16" actions={<Button size="sm" variant="ghost" icon="add" onClick={() => setCriteria([...criteria, { name: '', weight: 0 }])}>Add criterion</Button>}>
          <div className="col gap-8">
            {criteria.map((k, i) => (
              <div key={i} className="row gap-8">
                <input className="input grow" placeholder="Criterion" value={k.name} onChange={(e) => setCriteria(criteria.map((x, j) => (j === i ? { ...x, name: e.target.value } : x)))} />
                <input className="input" type="number" style={{ width: 100 }} value={k.weight} onChange={(e) => setCriteria(criteria.map((x, j) => (j === i ? { ...x, weight: e.target.value } : x)))} />
                <span className="small muted">%</span>
                <Button size="sm" variant="ghost" icon="delete" aria-label="Remove" onClick={() => setCriteria(criteria.filter((_, j) => j !== i))} />
              </div>
            ))}
            <div className="row between"><span className={`small ${totalWeight === 100 ? 'muted' : 'bold'}`}>Total weight: {totalWeight}% {totalWeight !== 100 && '(should be 100%)'}</span>
              <Button icon="save" loading={busy} onClick={save}>Save evaluation</Button></div>
            <p className="tiny muted">Score each quotation from 0 to 10 per criterion. Price is not automatically the winner.</p>
          </div>
        </Card>
      )}
      {accepting && <AcceptModal quotation={{ id: accepting.quotationId, totalAmount: accepting.totalAmount, sellerName: accepting.sellerName }} onClose={() => setAccepting(null)} onDone={(e) => navigate(`/buyer/engagements/${e.summary.id}`)} />}
    </>
  );
}

function AcceptModal({ quotation, onClose, onDone }) {
  const form = useForm({ remarks: '', contractValue: quotation.totalAmount ?? '', startDate: '', expectedEndDate: '', confirmed: false });
  const [run, busy] = useAction();
  return (
    <Modal title={`Accept quotation from ${quotation.sellerName}`} onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button>
      <Button loading={busy} disabled={!form.values.remarks || !form.values.confirmed} onClick={() => run(() => api.post(`/buyer/quotations/${quotation.id}/accept`, payload(form.values, ['contractValue'])), 'Provider selected').then((res) => { onClose(); onDone(res.data); })}>Accept & select provider</Button></>}>
      <div className="col gap-16">
        <Alert tone="info">Accepting selects this provider and rejects other open quotations on the RFQ. Payment terms are agreed directly with the provider.</Alert>
        <F form={form} name="remarks" label="Selection remarks" type="textarea" required rows={3} />
        <div className="grid grid-3">
          <F form={form} name="contractValue" label="Contract value (₹)" type="number" />
          <F form={form} name="startDate" label="Start date" type="date" />
          <F form={form} name="expectedEndDate" label="Expected end" type="date" />
        </div>
        <F form={form} name="confirmed" label="I confirm the selection of this provider" type="checkbox" />
      </div>
    </Modal>
  );
}

/* ---------- Quotations ---------- */
export function BuyerQuotations() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/buyer/quotations', filters);
  return (
    <>
      <PageTitle title="Quotations received" />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: QUOTATION_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(q) => navigate(`/buyer/quotations/${q.id}`)} empty="No quotations received yet" columns={quotationColumns([{ key: 'sellerName', title: 'Provider', render: (q) => <span className="small">{q.sellerName}</span> }])} /></Card>
    </>
  );
}

export function BuyerQuotationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: q, reload } = useLoad(() => api.get(`/buyer/quotations/${id}`), [id]);
  const [modal, setModal] = useState(null);
  const [clar, setClar] = useState({ subject: '', message: '' });
  const [run, busy] = useAction();
  if (!q) return <Loading />;
  const open = !['ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED', 'DRAFT'].includes(q.status);
  return (
    <>
      <PageTitle back title={`${q.sellerName} — ${q.rfqTitle}`} subtitle={<><span className="mono">{q.quotationNumber}</span> · <Status value={q.status} /></>} actions={<>
        {open && <Button variant="outline" icon={q.shortlisted ? 'bookmark_remove' : 'bookmark_add'} loading={busy} onClick={() => run(() => api.post(`/buyer/quotations/${id}/shortlist`, { shortlisted: !q.shortlisted }), 'Shortlist updated').then(reload)}>{q.shortlisted ? 'Remove shortlist' : 'Shortlist'}</Button>}
        {open && <Button variant="outline" icon="help" onClick={() => setModal('clarify')}>Request clarification</Button>}
        {open && <Button variant="ghost" onClick={() => setModal('reject')}>Reject</Button>}
        {open && <Button icon="check" onClick={() => setModal('accept')}>Accept</Button>}
        <Button variant="ghost" icon="picture_as_pdf" onClick={() => api.download(`/buyer/quotations/${id}/pdf`, null, `${q.quotationNumber}.pdf`)}>PDF</Button>
        <Link to={`/buyer/rfqs/${q.rfqId}/comparison`} className="btn btn-ghost"><Icon name="compare" size="sm" />Compare</Link>
        {q.engagementId && <Link to={`/buyer/engagements/${q.engagementId}`} className="btn btn-primary">Engagement</Link>}
      </>} />
      <div className="col gap-16 mt-16">
        <QuotationInfo q={q} />
        <Versions url={`/buyer/quotations/${id}/versions`} />
      </div>
      {modal === 'clarify' && (
        <Modal title="Request clarification" onClose={() => setModal(null)} footer={<><Button variant="ghost" onClick={() => setModal(null)}>Cancel</Button>
          <Button loading={busy} disabled={!clar.subject || !clar.message} onClick={() => run(() => api.post(`/buyer/quotations/${id}/request-clarification`, clar), 'Clarification requested').then(() => { setModal(null); reload(); })}>Send</Button></>}>
          <div className="col gap-16">
            <Field label="Subject" required><Input value={clar.subject} onChange={(e) => setClar({ ...clar, subject: e.target.value })} /></Field>
            <Field label="Message" required><Textarea rows={4} value={clar.message} onChange={(e) => setClar({ ...clar, message: e.target.value })} /></Field>
          </div>
        </Modal>
      )}
      {modal === 'reject' && <ActionModal title="Reject quotation" variant="danger" remarks remarksRequired onConfirm={(remarks) => api.post(`/buyer/quotations/${id}/reject`, { remarks }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'accept' && <AcceptModal quotation={q} onClose={() => setModal(null)} onDone={(e) => navigate(`/buyer/engagements/${e.summary.id}`)} />}
    </>
  );
}

/* ---------- Work orders ---------- */
export function BuyerWorkOrders() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/buyer/work-orders', filters);
  const [issuing, setIssuing] = useState(null);
  const [declining, setDeclining] = useState(null);
  return (
    <>
      <PageTitle title="Work orders" subtitle="Requests from providers and work orders you have issued" />
      <Filters search={false} value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: WORK_ORDER_STATUSES }]} />
      <Card>
        <PagedTable paged={paged} empty="No work orders yet" emptyIcon="assignment" columns={workOrderColumns([
          { key: 'sellerName', title: 'Provider', render: (w) => <><div className="small">{w.sellerName}</div>{w.requestRemarks && <div className="tiny muted">{w.requestRemarks}</div>}</> },
          { key: 'actions', title: '', render: (w) => <div className="row gap-4">
            {w.status === 'REQUESTED' && <><Button size="sm" onClick={() => setIssuing(w)}>Issue</Button><Button size="sm" variant="ghost" onClick={() => setDeclining(w)}>Decline</Button></>}
            <Button size="sm" variant="ghost" onClick={() => navigate(`/buyer/engagements/${w.engagementId}`)}>Engagement</Button>
          </div> }
        ])} />
      </Card>
      {issuing && <WorkOrderModal onSubmit={(body) => api.post(`/buyer/work-orders/${issuing.id}/issue`, body).then(paged.reload)} onClose={() => setIssuing(null)} />}
      {declining && <ActionModal title="Decline work order request" remarks remarksRequired onConfirm={(remarks) => api.post(`/buyer/work-orders/${declining.id}/decline`, { remarks }).then(paged.reload)} onClose={() => setDeclining(null)} />}
    </>
  );
}

/* ---------- Engagements ---------- */
export function BuyerEngagements() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/buyer/engagements', filters);
  return (
    <>
      <PageTitle title="Engagements" />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: ENGAGEMENT_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(e) => navigate(`/buyer/engagements/${e.id}`)} empty="No engagements yet" emptyIcon="handshake" columns={engagementColumns([{ key: 'sellerName', title: 'Provider', render: (e) => <span className="small">{e.sellerName}</span> }])} /></Card>
    </>
  );
}

export function BuyerEngagementDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: e, reload } = useLoad(() => api.get(`/buyer/engagements/${id}`), [id]);
  const [modal, setModal] = useState(null);
  if (!e) return <Loading />;
  const s = e.summary;
  const requested = e.workOrders?.find((w) => w.status === 'REQUESTED');
  const issued = e.workOrders?.some((w) => ['ISSUED', 'ACKNOWLEDGED'].includes(w.status));
  const open = !['COMPLETED', 'CANCELLED'].includes(s.status);
  return (
    <>
      <PageTitle back title={s.title} subtitle={<><span className="mono">{s.engagementNumber}</span> · {s.sellerName} · <Status value={s.status} /></>} actions={<>
        {open && !issued && <Button icon="assignment" onClick={() => setModal(requested ? 'issueRequested' : 'issue')}>{requested ? 'Issue requested work order' : 'Issue work order'}</Button>}
        {requested && <Button variant="ghost" onClick={() => setModal('declineWo')}>Decline request</Button>}
        {s.status === 'DELIVERED' && !s.deliveryConfirmed && <Button icon="task_alt" onClick={() => setModal('confirm')}>Confirm delivery</Button>}
        {s.status === 'COMPLETED' && !s.feedbackSubmitted && <Button icon="star" onClick={() => setModal('feedback')}>Give feedback</Button>}
        {s.status === 'DISPUTED' && <Button variant="outline" onClick={() => setModal('resume')}>Resume</Button>}
        {open && s.status !== 'DISPUTED' && <Button variant="ghost" onClick={() => setModal('dispute')}>Raise dispute</Button>}
        {open && <Button variant="ghost" onClick={() => setModal('cancel')}>Cancel</Button>}
        <Button variant="ghost" icon="forum" onClick={() => setModal('query')}>Query</Button>
      </>} />
      <div className="mt-16">
        <EngagementInfo e={e} quotationLink={`/buyer/quotations/${s.quotationId}`}
          docs={<Docs documents={e.documents} uploadUrl={open ? `/buyer/engagements/${id}/documents` : null} category="ENGAGEMENT" canDelete={open} onChange={reload} />} />
      </div>
      {modal === 'issue' && <WorkOrderModal initial={{ agreedValue: s.contractValue || s.quotedValue || '', startDate: s.startDate || new Date().toISOString().slice(0, 10), endDate: s.expectedEndDate || '' }} onSubmit={(body) => api.post(`/buyer/engagements/${id}/work-order`, body).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'issueRequested' && <WorkOrderModal initial={{ agreedValue: s.contractValue || s.quotedValue || '', startDate: s.startDate || new Date().toISOString().slice(0, 10), endDate: s.expectedEndDate || '' }} onSubmit={(body) => api.post(`/buyer/work-orders/${requested.id}/issue`, body).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'declineWo' && <ActionModal title="Decline work order request" remarks remarksRequired onConfirm={(remarks) => api.post(`/buyer/work-orders/${requested.id}/decline`, { remarks }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'confirm' && <ConfirmDelivery id={id} onDone={reload} onClose={() => setModal(null)} />}
      {modal === 'feedback' && <FeedbackModal id={id} onDone={reload} onClose={() => setModal(null)} />}
      {modal === 'dispute' && <ActionModal title="Raise dispute" variant="danger" remarks remarksRequired remarksLabel="Reason" onConfirm={(reason) => api.post(`/buyer/engagements/${id}/dispute`, { reason }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'resume' && <ActionModal title="Resume engagement" remarks onConfirm={(remarks) => api.post(`/buyer/engagements/${id}/resume`, { remarks }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'cancel' && <ActionModal title="Cancel engagement" variant="danger" remarks remarksRequired remarksLabel="Reason" onConfirm={(reason) => api.post(`/buyer/engagements/${id}/cancel`, { reason }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'query' && <RaiseTicket contextType="ENGAGEMENT" contextId={Number(id)} counterpartyId={s.sellerId} onClose={() => setModal(null)} onDone={(t) => navigate(`/buyer/queries/${t.summary.id}`)} />}
    </>
  );
}

function ConfirmDelivery({ id, onDone, onClose }) {
  const [delivered, setDelivered] = useState(true);
  const [remarks, setRemarks] = useState('');
  const [run, busy] = useAction();
  return (
    <Modal title="Confirm delivery" onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button>
      <Button loading={busy} onClick={() => run(() => api.post(`/buyer/engagements/${id}/confirm-delivery`, { delivered, remarks }), delivered ? 'Delivery confirmed' : 'Response recorded').then(() => { onDone(); onClose(); })}>Submit</Button></>}>
      <div className="col gap-16">
        <div className="row gap-16">
          <label className="checkbox"><input type="radio" checked={delivered} onChange={() => setDelivered(true)} />Work delivered as agreed</label>
          <label className="checkbox"><input type="radio" checked={!delivered} onChange={() => setDelivered(false)} />Not delivered / needs rework</label>
        </div>
        <Field label="Remarks"><Textarea rows={3} value={remarks} onChange={(e) => setRemarks(e.target.value)} /></Field>
      </div>
    </Modal>
  );
}

function FeedbackModal({ id, onDone, onClose }) {
  const { data: questions } = useLoad(() => api.get('/buyer/feedback/questions'), []);
  const [rating, setRating] = useState(0);
  const [title, setTitle] = useState('');
  const [text, setText] = useState('');
  const [answers, setAnswers] = useState({});
  const [run, busy] = useAction();
  const setAnswer = (qid, v) => setAnswers({ ...answers, [qid]: v });
  const submit = () => run(() => api.post(`/buyer/engagements/${id}/feedback`, {
    rating, title, reviewText: text,
    answers: (questions || []).filter((q) => answers[q.id] !== undefined && answers[q.id] !== '').map((q) => ({
      questionId: q.id, ratingValue: q.questionType === 'RATING' ? answers[q.id] : undefined, booleanValue: q.questionType === 'YES_NO' ? answers[q.id] : undefined, textValue: q.questionType === 'TEXT' ? answers[q.id] : undefined
    }))
  }), 'Thank you for your feedback').then(() => { onDone(); onClose(); });
  return (
    <Modal title="Rate this engagement" wide onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button loading={busy} disabled={!rating} onClick={submit}>Submit feedback</Button></>}>
      <div className="col gap-16">
        <Field label="Overall rating" required><StarInput value={rating} onChange={setRating} size="lg" /></Field>
        <Field label="Title"><Input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={150} /></Field>
        <Field label="Review"><Textarea rows={4} value={text} onChange={(e) => setText(e.target.value)} /></Field>
        {(questions || []).map((q) => (
          <Field key={q.id} label={q.questionText} required={q.mandatory}>
            {q.questionType === 'RATING' ? <StarInput value={answers[q.id] || 0} onChange={(v) => setAnswer(q.id, v)} />
              : q.questionType === 'YES_NO' ? <div className="row gap-16">{[[true, 'Yes'], [false, 'No']].map(([v, t]) => <label key={t} className="checkbox"><input type="radio" checked={answers[q.id] === v} onChange={() => setAnswer(q.id, v)} />{t}</label>)}</div>
                : <Textarea rows={2} value={answers[q.id] || ''} onChange={(e) => setAnswer(q.id, e.target.value)} />}
          </Field>
        ))}
      </div>
    </Modal>
  );
}

/* ---------- Organisation profile ---------- */
export function BuyerProfile() {
  const { refreshUser } = useAuth();
  const { data: p, reload } = useLoad(() => api.get('/buyers/me/profile'), []);
  const form = useForm({});
  const [run, busy] = useAction();
  useEffect(() => { if (p) form.setValues(p); }, [p]);
  if (!p) return <Loading />;
  const save = async () => {
    form.setErrors({});
    try { await run(() => api.put('/buyers/me/profile', payload(form.values)), 'Profile saved'); reload(); refreshUser().catch(() => {}); } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  return (
    <>
      <PageTitle title="Organisation profile" subtitle={<><span className="mono">{p.referenceNo}</span> · <Status value={p.status} /></>} />
      <Card>
        <div className="grid grid-2">
          <F form={form} name="fullName" label="Contact person" required />
          <F form={form} name="designation" label="Designation" />
          <F form={form} name="organisationName" label="Organisation name" required />
          <F form={form} name="constitution" label="Constitution" type="select" options={CONSTITUTIONS} labels={CONSTITUTION_LABELS} />
          <F form={form} name="purchaserType" label="Purchaser type" type="select" options={PURCHASER_TYPES} labels={PURCHASER_LABELS} required />
          <F form={form} name="industrySector" label="Industry sector" type="select" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} required />
          <F form={form} name="pan" label="PAN" />
          <F form={form} name="gstin" label="GSTIN" />
          <F form={form} name="udyamNumber" label="Udyam number" />
          <F form={form} name="website" label="Website" />
          <F form={form} name="businessActivity" label="Business activity" className="span-2" />
          <F form={form} name="addressLine" label="Address" required className="span-2" />
          <F form={form} name="city" label="City" />
          <F form={form} name="state" label="State" type="select" options={STATES} labels={asLabels(STATES)} required />
          <F form={form} name="district" label="District" required />
          <F form={form} name="pincode" label="PIN code" />
        </div>
        <div className="row mt-16" style={{ justifyContent: 'flex-end' }}><Button icon="save" loading={busy} onClick={save}>Save profile</Button></div>
      </Card>
    </>
  );
}

