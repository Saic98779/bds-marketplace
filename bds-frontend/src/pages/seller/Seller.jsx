import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, fieldErrors } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { TrendChart } from '../../components/charts';
import { PageTitle } from '../../components/layouts';
import { DataTable, Docs, F, Filters, History, PagedTable, payload, useForm, usePaged } from '../../components/kit';
import { ActionModal, Alert, Button, Card, DocumentList, Empty, FilePicker, Icon, Input, KeyValue, Loading, Modal, Progress, Select, StatCard, Stars, Status, Tabs, useAction, useLoad } from '../../components/ui';
import { CONSTITUTION_LABELS, CONSTITUTIONS, date, dateTime, DELIVERY_MODES, DOCUMENT_TYPES, inr, INDUSTRIES, label, PRICING_LABELS, PRICING_MODELS, PROVIDER_TYPES, STATES } from '../../constants';
import { useCategories } from '../public/Services';
import { RaiseTicket } from '../shared/Common';
import { ENGAGEMENT_STATUSES, EngagementInfo, engagementColumns, QUOTATION_STATUSES, QuotationForm, QuotationInfo, quotationColumns, RFQ_STATUSES, RfqInfo, rfqColumns, Versions, WORK_ORDER_STATUSES, workOrderColumns } from '../shared/Procurement';

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));
const LISTING_STATUSES = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CORRECTION_REQUESTED', 'APPROVED', 'REJECTED', 'DEACTIVATED', 'FLAGGED'];

/* ---------- Dashboard ---------- */
export function SellerDashboard() {
  const navigate = useNavigate();
  const { data: d, loading } = useLoad(() => api.get('/seller/dashboard'), []);
  if (loading && !d) return <Loading />;
  if (!d) return <Empty title="Dashboard unavailable" />;
  return (
    <>
      <PageTitle title="Dashboard" actions={<Link to="/seller/services/new" className="btn btn-primary"><Icon name="add" size="sm" />New service</Link>} />
      <div className="grid grid-4">
        <StatCard icon="inbox" value={d.pendingRfqs} label="RFQs awaiting response" tone="warn" onClick={() => navigate('/seller/rfqs')} />
        <StatCard icon="request_quote" value={d.quotationsSubmitted} label="Quotations submitted" sub={`${d.quotationsAccepted} accepted`} onClick={() => navigate('/seller/quotations')} />
        <StatCard icon="handshake" value={d.activeEngagements} label="Active engagements" sub={`${d.completedEngagements} completed`} onClick={() => navigate('/seller/engagements')} />
        <StatCard icon="star" value={d.averageRating ? Number(d.averageRating).toFixed(1) : '-'} label="Average rating" sub={`${d.reviewCount} reviews`} onClick={() => navigate('/seller/reviews')} />
        <StatCard icon="mail" value={d.totalInvitations} label="Total invitations" />
        <StatCard icon="percent" value={`${Math.round(d.responseRate || 0)}%`} label="Response rate" />
        <StatCard icon="emoji_events" value={`${Math.round(d.selectionRate || 0)}%`} label="Selection rate" tone="good" />
        <StatCard icon="assignment" value={d.workOrdersAwaited} label="Work orders awaited" onClick={() => navigate('/seller/work-orders')} />
      </div>
      <div className="grid grid-2 mt-16">
        <TrendChart title="Monthly enquiries" data={d.monthlyEnquiries} series={[{ key: 'value', name: 'Enquiries' }]} />
        <Card title="Recent RFQs" actions={<Link to="/seller/rfqs" className="small">View all</Link>}>
          <DataTable rows={d.recentRfqs} columns={rfqColumns()} onRow={(r) => navigate(`/seller/rfqs/${r.id}`)} empty="No RFQs yet" />
        </Card>
        <Card title="Recent quotations" actions={<Link to="/seller/quotations" className="small">View all</Link>}>
          <DataTable rows={d.recentQuotations} columns={quotationColumns()} onRow={(q) => navigate(`/seller/quotations/${q.id}`)} empty="No quotations yet" />
        </Card>
        <Card title="Recent engagements" actions={<Link to="/seller/engagements" className="small">View all</Link>}>
          <DataTable rows={d.recentEngagements} columns={engagementColumns([{ key: 'buyerName', title: 'Purchaser' }])} onRow={(e) => navigate(`/seller/engagements/${e.id}`)} empty="No engagements yet" />
        </Card>
      </div>
    </>
  );
}

/* ---------- Profile, documents, verification ---------- */
export function SellerProfile() {
  const [tab, setTab] = useState('profile');
  return (
    <>
      <PageTitle title="Business profile" subtitle="Your public provider profile, verification documents and status" />
      <Tabs value={tab} onChange={setTab} tabs={[{ value: 'profile', label: 'Profile' }, { value: 'documents', label: 'Documents' }, { value: 'verification', label: 'Verification' }]} />
      <div className="mt-16">
        {tab === 'profile' && <ProfileForm />}
        {tab === 'documents' && <SellerDocuments />}
        {tab === 'verification' && <Verification />}
      </div>
    </>
  );
}

function ProfileForm() {
  const categories = useCategories();
  const { refreshUser } = useAuth();
  const { data: p, loading, reload } = useLoad(() => api.get('/sellers/me/profile'), []);
  const form = useForm({});
  const [run, busy] = useAction();
  useEffect(() => { if (p) form.setValues({ ...p, categoryIds: (p.categoryIds || []).map(String) }); }, [p]);
  if (loading && !p) return <Loading />;
  if (!p) return null;
  const save = async () => {
    form.setErrors({});
    try {
      await run(() => api.put('/sellers/me/profile', payload({ ...form.values, categoryIds: (form.values.categoryIds || []).map(Number) }, ['yearsExperience', 'teamSize', 'yearEstablished'])), 'Profile saved');
      reload(); refreshUser().catch(() => {});
    } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  const catLabels = Object.fromEntries(categories.map((c) => [String(c.id), c.name]));
  return (
    <div className="col gap-16">
      <Card>
        <div className="row between">
          <div><div className="bold">Profile completion</div><div style={{ width: 260 }}><Progress value={p.profileCompletion} /></div><span className="tiny muted">{p.profileCompletion}%</span></div>
          <div className="right"><Status value={p.status} /><div className="tiny muted mt-4">Verified {date(p.verifiedAt)} · re-verification due {date(p.reverificationDue)}</div></div>
        </div>
        <Alert tone="info" className="mt-16">Changing PAN, constitution or organisation name sends your profile back for verification.</Alert>
      </Card>
      <Card title="Organisation">
        <div className="grid grid-2">
          <F form={form} name="fullName" label="Contact person" required />
          <F form={form} name="organisationName" label="Organisation name" required />
          <F form={form} name="constitution" label="Constitution" type="select" options={CONSTITUTIONS} labels={CONSTITUTION_LABELS} required />
          <F form={form} name="providerType" label="Provider type" type="select" options={PROVIDER_TYPES} required />
          <F form={form} name="pan" label="PAN" required />
          <F form={form} name="gstin" label="GSTIN" />
          <F form={form} name="udyamNumber" label="Udyam number" />
          <F form={form} name="website" label="Website" />
          <F form={form} name="addressLine" label="Address" required className="span-2" />
          <F form={form} name="city" label="City" />
          <F form={form} name="state" label="State" type="select" options={STATES} labels={asLabels(STATES)} required />
          <F form={form} name="district" label="District" required />
          <F form={form} name="pincode" label="PIN code" />
        </div>
      </Card>
      <Card title="Expertise">
        <div className="grid grid-2">
          <F form={form} name="yearsExperience" label="Years of experience" type="number" required />
          <F form={form} name="teamSize" label="Team size" type="number" />
          <F form={form} name="yearEstablished" label="Year established" type="number" />
          <div />
          <F form={form} name="categoryIds" label="Categories" type="chips" options={categories.map((c) => String(c.id))} labels={catLabels} className="span-2" />
          <F form={form} name="deliveryModes" label="Delivery modes" type="chips" options={DELIVERY_MODES} className="span-2" />
          <F form={form} name="expertise" label="Expertise" type="tags" className="span-2" />
          <F form={form} name="industries" label="Industries" type="chips" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} className="span-2" />
          <F form={form} name="geographicCoverage" label="Geographic coverage" type="tags" className="span-2" />
          <F form={form} name="introduction" label="Introduction" type="textarea" rows={4} className="span-2" />
          <F form={form} name="certifications" label="Certifications" type="textarea" rows={2} className="span-2" />
          <F form={form} name="teamProfile" label="Team profile" type="textarea" rows={2} className="span-2" />
          <F form={form} name="previousAssignments" label="Previous assignments" type="textarea" rows={3} className="span-2" />
          <F form={form} name="clientReferences" label="Client references" type="textarea" rows={2} className="span-2" />
        </div>
      </Card>
      <div className="row" style={{ justifyContent: 'flex-end' }}><Button icon="save" loading={busy} onClick={save}>Save profile</Button></div>
    </div>
  );
}

function SellerDocuments() {
  const { data: docs, reload } = useLoad(() => api.get('/sellers/me/documents'), []);
  const [type, setType] = useState('PAN');
  const [run, busy] = useAction();
  return (
    <Card title="Verification documents" actions={<div className="row gap-8">
      <Select value={type} onChange={(e) => setType(e.target.value)} options={DOCUMENT_TYPES} style={{ width: 220 }} />
      <FilePicker label="Upload" busy={busy} onFile={(file) => run(() => api.upload(`/sellers/me/documents?documentType=${type}`, { file }), 'Document uploaded').then(reload)} />
    </div>}>
      <DocumentList documents={docs || []}
        onDownload={(d) => api.download(`/sellers/me/documents/${d.id}/download`, null, d.originalName)}
        onDelete={(d) => run(() => api.del(`/sellers/me/documents/${d.id}`), 'Document deleted').then(reload)} />
      <p className="tiny muted mt-8">Verified documents cannot be deleted.</p>
    </Card>
  );
}

function Verification() {
  const { reissue } = useAuth();
  const { data: v, reload } = useLoad(() => api.get('/sellers/me/verification'), []);
  const [resubmit, setResubmit] = useState(false);
  if (!v) return <Loading />;
  return (
    <div className="grid grid-2">
      <Card title="Status" actions={v.status === 'INFO_REQUESTED' && <Button onClick={() => setResubmit(true)}>Resubmit for verification</Button>}>
        <Status value={v.status} />
        {v.statusRemarks && <Alert tone="warn" className="mt-16">{v.statusRemarks}</Alert>}
        <h4 className="mt-16">Document checklist</h4>
        <DataTable rows={v.checklist.map((c) => ({ ...c, id: c.documentType }))} columns={[
          { key: 'documentType', title: 'Document', render: (c) => <>{label(c.documentType)}{c.mandatory && <span className="req"> *</span>}</> },
          { key: 'uploaded', title: 'Uploaded', render: (c) => <Icon name={c.uploaded ? 'check_circle' : 'cancel'} className={c.uploaded ? '' : 'muted'} /> },
          { key: 'verified', title: 'Verified', render: (c) => <Icon name={c.verified ? 'verified' : 'schedule'} className={c.verified ? '' : 'muted'} /> }
        ]} />
      </Card>
      <Card title="History"><History items={v.history} /></Card>
      {resubmit && <ActionModal title="Resubmit application" remarks remarksLabel="What did you update?" onConfirm={(remarks) => api.post('/sellers/me/verification/resubmit', { remarks }).then(() => { reload(); reissue(); })} onClose={() => setResubmit(false)} />}
    </div>
  );
}

/* ---------- Services ---------- */
export function SellerServices() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/seller/services', filters);
  const { data: stats } = useLoad(() => api.get('/seller/services/stats'), []);
  return (
    <>
      <PageTitle title="My services" actions={<Link to="/seller/services/new" className="btn btn-primary"><Icon name="add" size="sm" />New service</Link>} />
      {stats && (
        <div className="grid grid-4 mb-16">
          <StatCard icon="storefront" value={stats.total} label="Total listings" />
          <StatCard icon="public" value={stats.published} label="Published" tone="good" />
          <StatCard icon="pending" value={stats.pending} label="Pending moderation" tone="warn" />
          <StatCard icon="visibility" value={stats.views} label="Views" sub={`${stats.enquiries} enquiries`} />
        </div>
      )}
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: LISTING_STATUSES }]} />
      <Card>
        <PagedTable paged={paged} onRow={(l) => navigate(`/seller/services/${l.id}`)} empty="No services yet" emptyIcon="storefront" columns={listingColumns} />
      </Card>
    </>
  );
}

export const listingColumns = [
  { key: 'title', title: 'Service', render: (l) => <><div className="bold small">{l.title}</div><div className="tiny muted mono">{l.referenceNo}</div></> },
  { key: 'categoryName', title: 'Category', render: (l) => <span className="small">{l.categoryName}{l.subcategoryName ? ` · ${l.subcategoryName}` : ''}</span> },
  { key: 'indicativeFee', title: 'Fee', render: (l) => <span className="small">{l.indicativeFee ? inr(l.indicativeFee) : '-'} · {PRICING_LABELS[l.pricingModel]}</span> },
  { key: 'updatedAt', title: 'Updated', render: (l) => <span className="small muted">{date(l.updatedAt)}</span> },
  { key: 'status', title: 'Status', render: (l) => <div className="row gap-4"><Status value={l.status} />{l.featured && <Icon name="star" size="sm" fill />}</div> }
];

export function ListingForm({ initial, onSubmit, busy, submitLabel = 'Submit for approval', allowDraft = true }) {
  const categories = useCategories();
  const form = useForm({ title: '', categoryId: '', subcategoryId: '', shortDescription: '', description: '', scopeOfWork: '', deliverables: '', methodology: '', targetIndustries: [], deliveryMode: 'ONLINE', serviceLocations: [], state: '', district: '', experienceYears: '', estimatedDuration: '', indicativeFee: '', pricingModel: 'FIXED_FEE', tags: [], ...initial });
  const category = categories.find((c) => String(c.id) === String(form.values.categoryId));
  const submit = (submitNow) => onSubmit({ ...payload(form.values, ['categoryId', 'subcategoryId', 'experienceYears', 'indicativeFee']), submit: submitNow }, form);
  return (
    <div className="col gap-16">
      <Card title="Service">
        <div className="grid grid-2">
          <F form={form} name="title" label="Title" required className="span-2" />
          <F form={form} name="categoryId" label="Category" type="select" required options={categories.map((c) => ({ value: String(c.id), label: c.name }))} />
          <F form={form} name="subcategoryId" label="Subcategory" type="select" options={(category?.subcategories || []).map((s) => ({ value: String(s.id), label: s.name }))} />
          <F form={form} name="shortDescription" label="Short description" maxLength={300} className="span-2" />
          <F form={form} name="description" label="Description" type="textarea" rows={4} required className="span-2" />
          <F form={form} name="scopeOfWork" label="Scope of services" type="textarea" rows={3} className="span-2" />
          <F form={form} name="deliverables" label="Deliverables" type="textarea" rows={3} className="span-2" />
          <F form={form} name="methodology" label="Methodology" type="textarea" rows={3} className="span-2" />
        </div>
      </Card>
      <Card title="Delivery & pricing">
        <div className="grid grid-2">
          <F form={form} name="deliveryMode" label="Delivery mode" type="select" options={DELIVERY_MODES} required />
          <F form={form} name="estimatedDuration" label="Estimated duration" placeholder="e.g. 3 weeks" />
          <F form={form} name="pricingModel" label="Pricing model" type="select" options={PRICING_MODELS} labels={PRICING_LABELS} required />
          <F form={form} name="indicativeFee" label="Indicative fee (₹)" type="number" min="0" />
          <F form={form} name="experienceYears" label="Experience (years)" type="number" min="0" />
          <F form={form} name="state" label="State" type="select" options={STATES} labels={asLabels(STATES)} />
          <F form={form} name="district" label="District" />
          <div />
          <F form={form} name="serviceLocations" label="Service locations" type="tags" className="span-2" />
          <F form={form} name="targetIndustries" label="Target industries" type="chips" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} className="span-2" />
          <F form={form} name="tags" label="Search tags" type="tags" className="span-2" />
        </div>
      </Card>
      <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
        {allowDraft && <Button variant="outline" loading={busy} onClick={() => submit(false)}>Save draft</Button>}
        <Button icon="send" loading={busy} onClick={() => submit(true)}>{submitLabel}</Button>
      </div>
    </div>
  );
}

const toListingForm = (l) => ({
  ...l.summary, categoryId: String(l.summary.categoryId), subcategoryId: l.summary.subcategoryId ? String(l.summary.subcategoryId) : '',
  description: l.description, scopeOfWork: l.scopeOfWork, deliverables: l.deliverables, methodology: l.methodology, targetIndustries: l.targetIndustries,
  serviceLocations: l.serviceLocations, district: l.district, tags: l.tags
});

export function SellerListingEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [run, busy] = useAction();
  const { data: l, loading } = useLoad(() => (id ? api.get(`/seller/services/${id}`) : Promise.resolve(null)), [id]);
  if (id && loading) return <Loading />;
  const save = async (body, form) => {
    try {
      const res = await run(() => (id ? api.put(`/seller/services/${id}`, body) : api.post('/seller/services', body)), body.submit ? 'Submitted for approval' : 'Saved as draft');
      navigate(`/seller/services/${res.data.summary.id}`);
    } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  return (
    <>
      <PageTitle back title={id ? 'Edit service' : 'New service'} subtitle={id && l?.summary.status === 'APPROVED' ? 'Editing a published service sends it back for moderation.' : 'Services are published after administrator approval.'} />
      <ListingForm initial={l ? toListingForm(l) : undefined} onSubmit={save} busy={busy} />
    </>
  );
}

export function SellerListingDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: l, reload } = useLoad(() => api.get(`/seller/services/${id}`), [id]);
  const { data: history, reload: reloadHistory } = useLoad(() => api.get(`/seller/services/${id}/history`), [id]);
  const [modal, setModal] = useState(null);
  const [labelText, setLabelText] = useState('');
  const [run, busy] = useAction();
  if (!l) return <Loading />;
  const s = l.summary;
  const refresh = () => { reload(); reloadHistory(); };
  return (
    <>
      <PageTitle back title={s.title} subtitle={<><span className="mono">{s.referenceNo}</span> · <Status value={s.status} /></>} actions={<>
        {['DRAFT', 'CORRECTION_REQUESTED', 'REJECTED'].includes(s.status) && <Button icon="send" loading={busy} onClick={() => run(() => api.post(`/seller/services/${id}/submit`), 'Submitted for approval').then(refresh)}>Submit</Button>}
        {s.status !== 'DEACTIVATED' && <Link to={`/seller/services/${id}/edit`} className="btn btn-outline"><Icon name="edit" size="sm" />Edit</Link>}
        {['APPROVED', 'FLAGGED'].includes(s.status) && <Button variant="outline" onClick={() => setModal('deactivate')}>Deactivate</Button>}
        {s.status === 'DRAFT' && <Button variant="danger" icon="delete" onClick={() => setModal('delete')}>Delete</Button>}
        {l.publiclyVisible && <Link to={`/services/${id}`} className="btn btn-ghost" target="_blank"><Icon name="open_in_new" size="sm" />Public page</Link>}
      </>} />
      {l.moderationRemarks && <Alert tone="warn" title="Moderator remarks">{l.moderationRemarks}</Alert>}
      <div className="layout-main-side mt-16">
        <div className="col gap-16">
          <Card>
            <KeyValue items={[['Category', `${s.categoryName}${s.subcategoryName ? ` · ${s.subcategoryName}` : ''}`], ['Delivery', label(s.deliveryMode)], ['Pricing', `${s.indicativeFee ? inr(s.indicativeFee) : '-'} · ${PRICING_LABELS[s.pricingModel]}`],
              ['Duration', s.estimatedDuration], ['Experience', s.experienceYears != null ? `${s.experienceYears} years` : null], ['Location', [l.district, s.state].filter(Boolean).join(', ')],
              ['Views / enquiries', `${l.viewCount} / ${l.enquiryCount}`], ['Submitted', dateTime(l.submittedAt)], ['Published', dateTime(s.publishedAt)]]} />
            {[['Short description', s.shortDescription], ['Description', l.description], ['Scope', l.scopeOfWork], ['Deliverables', l.deliverables], ['Methodology', l.methodology]].map(([t, v]) => v && <div key={t} className="mt-16"><h4>{t}</h4><p className="pre small mt-4">{v}</p></div>)}
          </Card>
          <Card title="Attachments" actions={<div className="row gap-8">
            <Input placeholder="Label (optional)" value={labelText} onChange={(e) => setLabelText(e.target.value)} style={{ width: 180 }} />
            <FilePicker label="Attach" busy={busy} onFile={(file) => run(() => api.upload(`/seller/services/${id}/attachments${labelText ? `?label=${encodeURIComponent(labelText)}` : ''}`, { file }), 'Attached').then(() => { setLabelText(''); reload(); })} />
          </div>}>
            <DocumentList documents={l.attachments.map((a) => ({ ...a, category: a.label }))}
              onDownload={(a) => api.download(`/services/${id}/attachments/${a.id}/download`, null, a.originalName)}
              onDelete={(a) => run(() => api.del(`/seller/services/${id}/attachments/${a.id}`), 'Attachment removed').then(reload)} />
          </Card>
        </div>
        <Card title="Moderation history"><History items={history || []} /></Card>
      </div>
      {modal === 'deactivate' && <ActionModal title="Deactivate service" remarks onConfirm={(remarks) => api.post(`/seller/services/${id}/deactivate`, { remarks }).then(refresh)} onClose={() => setModal(null)} />}
      {modal === 'delete' && <ActionModal title="Delete draft" variant="danger" confirmText="Delete" description="This draft will be permanently deleted." onConfirm={() => api.del(`/seller/services/${id}`).then(() => navigate('/seller/services'))} onClose={() => setModal(null)} />}
    </>
  );
}

/* ---------- RFQ inbox ---------- */
export function SellerRfqs() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/seller/rfqs', filters);
  return (
    <>
      <PageTitle title="RFQ inbox" subtitle="Requests for quotation shared with you" />
      <Filters value={filters} onChange={setFilters} fields={[
        { name: 'invitationStatus', placeholder: 'Any invitation', options: ['INVITED', 'VIEWED', 'RESPONDED', 'DECLINED'] },
        { name: 'status', placeholder: 'Any RFQ status', options: RFQ_STATUSES }
      ]} />
      <Card>
        <PagedTable paged={paged} onRow={(r) => navigate(`/seller/rfqs/${r.id}`)} empty="No RFQs in your inbox" columns={rfqColumns([
          { key: 'buyerName', title: 'Purchaser', render: (r) => <span className="small">{r.buyerName}</span> },
          { key: 'my', title: 'My response', render: (r) => <div className="col gap-4"><Status value={r.myInvitationStatus} />{r.myQuotationStatus && <span className="tiny">Quote: {label(r.myQuotationStatus)}</span>}</div> }
        ])} />
      </Card>
    </>
  );
}

export function SellerRfqDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: rfq, reload } = useLoad(() => api.get(`/seller/rfqs/${id}`), [id]);
  const [modal, setModal] = useState(null);
  if (!rfq) return <Loading />;
  const s = rfq.summary;
  const open = !s.deadlinePassed && ['PUBLISHED', 'RESPONSES_RECEIVED'].includes(s.status);
  return (
    <>
      <PageTitle back title={s.title} subtitle={`${s.buyerName} · deadline ${dateTime(s.submissionDeadline)}`} actions={<>
        {s.myQuotationId ? <Link to={`/seller/quotations/${s.myQuotationId}`} className="btn btn-primary">View my quotation</Link>
          : open && s.myInvitationStatus !== 'DECLINED' && <Link to={`/seller/rfqs/${id}/quote`} className="btn btn-primary"><Icon name="request_quote" size="sm" />Submit quotation</Link>}
        {!s.myQuotationId && open && s.myInvitationStatus !== 'DECLINED' && <Button variant="outline" onClick={() => setModal('decline')}>Decline</Button>}
        <Button variant="ghost" icon="forum" onClick={() => setModal('query')}>Ask a question</Button>
      </>} />
      {s.myInvitationStatus === 'DECLINED' && <Alert tone="warn">You declined this RFQ.</Alert>}
      <div className="mt-16"><RfqInfo rfq={rfq} /></div>
      {modal === 'decline' && <ActionModal title="Decline to quote" remarks remarksRequired remarksLabel="Reason" onConfirm={(reason) => api.post(`/seller/rfqs/${id}/decline`, { reason }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'query' && <RaiseTicket contextType="RFQ" contextId={Number(id)} counterpartyId={s.buyerId} onClose={() => setModal(null)} onDone={(t) => navigate(`/seller/queries/${t.summary.id}`)} />}
    </>
  );
}

export function SellerQuote() {
  const { id, quotationId } = useParams();
  const navigate = useNavigate();
  const [run, busy] = useAction();
  const { data: q, loading } = useLoad(() => (quotationId ? api.get(`/seller/quotations/${quotationId}`) : Promise.resolve(null)), [quotationId]);
  const { data: rfq } = useLoad(() => api.get(`/seller/rfqs/${id || q?.rfqId}`), [id, q?.rfqId]);
  if (quotationId && loading) return <Loading />;
  const revision = q && q.status !== 'DRAFT';
  const save = async (body, form) => {
    try {
      const res = await run(() => (q ? api.put(`/seller/quotations/${q.id}`, body) : api.post(`/seller/rfqs/${id}/quotations`, body)), body.submit ? 'Quotation submitted' : 'Draft saved');
      navigate(`/seller/quotations/${res.data.id}`);
    } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  return (
    <>
      <PageTitle back title={q ? (revision ? 'Revise quotation' : 'Edit quotation') : 'Submit quotation'} subtitle={rfq ? `${rfq.summary.title} · deadline ${dateTime(rfq.summary.submissionDeadline)}` : ''} />
      {rfq && <details className="card mb-16" style={{ padding: 16 }}><summary className="bold">RFQ requirement</summary><div className="mt-16"><RfqInfo rfq={rfq} /></div></details>}
      <QuotationForm initial={q || undefined} onSubmit={save} busy={busy} revision={revision} />
    </>
  );
}

/* ---------- Quotations ---------- */
export function SellerQuotations() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/seller/quotations', filters);
  return (
    <>
      <PageTitle title="Quotations" />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: QUOTATION_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(q) => navigate(`/seller/quotations/${q.id}`)} empty="No quotations yet" columns={quotationColumns([{ key: 'buyerName', title: 'Purchaser', render: (q) => <span className="small">{q.buyerName}</span> }])} /></Card>
    </>
  );
}

export function SellerQuotationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: q, reload } = useLoad(() => api.get(`/seller/quotations/${id}`), [id]);
  const [modal, setModal] = useState(null);
  const [run, busy] = useAction();
  if (!q) return <Loading />;
  const active = !['ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED'].includes(q.status);
  return (
    <>
      <PageTitle back title={q.rfqTitle} subtitle={<><span className="mono">{q.quotationNumber}</span> · <Status value={q.status} /></>} actions={<>
        {q.status === 'DRAFT' && <Button icon="send" loading={busy} onClick={() => run(() => api.post(`/seller/quotations/${id}/submit`), 'Quotation submitted').then(reload)}>Submit</Button>}
        {q.editable && <Link to={`/seller/quotations/${id}/edit`} className="btn btn-outline"><Icon name="edit" size="sm" />{q.status === 'DRAFT' ? 'Edit' : 'Revise'}</Link>}
        {active && q.status !== 'DRAFT' && <Button variant="outline" onClick={() => setModal('withdraw')}>Withdraw</Button>}
        <Button variant="ghost" icon="picture_as_pdf" onClick={() => api.download(`/seller/quotations/${id}/pdf`, null, `${q.quotationNumber}.pdf`)}>PDF</Button>
        <Button variant="ghost" icon="forum" onClick={() => setModal('query')}>Query</Button>
        {q.engagementId && <Link to={`/seller/engagements/${q.engagementId}`} className="btn btn-primary">Engagement</Link>}
      </>} />
      {q.status === 'CLARIFICATION_REQUESTED' && <Alert tone="warn" title="Clarification requested">Check your Query Tracker and revise the quotation if needed.</Alert>}
      <div className="col gap-16 mt-16">
        <QuotationInfo q={q} docs={<Docs documents={q.attachments} uploadUrl={active ? `/seller/quotations/${id}/attachments` : null} category="TECHNICAL" canDelete={active} onChange={reload} />} />
        <Versions url={`/seller/quotations/${id}/versions`} />
      </div>
      {modal === 'withdraw' && <ActionModal title="Withdraw quotation" variant="danger" remarks onConfirm={(remarks) => api.post(`/seller/quotations/${id}/withdraw`, { remarks }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'query' && <RaiseTicket contextType="QUOTATION" contextId={Number(id)} counterpartyId={q.buyerId} onClose={() => setModal(null)} onDone={(t) => navigate(`/seller/queries/${t.summary.id}`)} />}
    </>
  );
}

/* ---------- Work orders ---------- */
export function SellerWorkOrders() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/seller/work-orders', filters);
  const [run] = useAction();
  return (
    <>
      <PageTitle title="Work orders" />
      <Filters search={false} value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: WORK_ORDER_STATUSES }]} />
      <Card>
        <PagedTable paged={paged} empty="No work orders yet" emptyIcon="assignment" columns={workOrderColumns([
          { key: 'buyerName', title: 'Purchaser', render: (w) => <span className="small">{w.buyerName}</span> },
          { key: 'actions', title: '', render: (w) => <div className="row gap-4">
            {w.status === 'ISSUED' && <Button size="sm" onClick={() => run(() => api.post(`/seller/work-orders/${w.id}/acknowledge`), 'Work order acknowledged').then(paged.reload)}>Acknowledge</Button>}
            <Button size="sm" variant="ghost" onClick={() => navigate(`/seller/engagements/${w.engagementId}`)}>Engagement</Button>
          </div> }
        ])} />
      </Card>
    </>
  );
}

/* ---------- Engagements ---------- */
export function SellerEngagements() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/seller/engagements', filters);
  return (
    <>
      <PageTitle title="Engagements" />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: ENGAGEMENT_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(e) => navigate(`/seller/engagements/${e.id}`)} empty="No engagements yet" emptyIcon="handshake" columns={engagementColumns([{ key: 'buyerName', title: 'Purchaser', render: (e) => <span className="small">{e.buyerName}</span> }])} /></Card>
    </>
  );
}

export function SellerEngagementDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: e, reload } = useLoad(() => api.get(`/seller/engagements/${id}`), [id]);
  const [modal, setModal] = useState(null);
  const [progress, setProgress] = useState({ message: '', progressPercent: '' });
  const [run, busy] = useAction();
  if (!e) return <Loading />;
  const s = e.summary;
  const hasIssued = e.workOrders?.some((w) => ['ISSUED', 'ACKNOWLEDGED'].includes(w.status));
  const pendingRequest = e.workOrders?.some((w) => w.status === 'REQUESTED');
  const open = !['COMPLETED', 'CANCELLED'].includes(s.status);
  const post = (path, body, msg) => run(() => api.post(`/seller/engagements/${id}/${path}`, body), msg).then(reload);
  return (
    <>
      <PageTitle back title={s.title} subtitle={<><span className="mono">{s.engagementNumber}</span> · {s.buyerName} · <Status value={s.status} /></>} actions={<>
        {s.status === 'SELECTED' && !hasIssued && !pendingRequest && <Button variant="outline" onClick={() => setModal('request')}>Request work order</Button>}
        {hasIssued && ['SELECTED', 'ENGAGEMENT_INITIATED'].includes(s.status) && <Button icon="play_arrow" loading={busy} onClick={() => post('start', undefined, 'Work started')}>Start work</Button>}
        {['IN_PROGRESS', 'ENGAGEMENT_INITIATED'].includes(s.status) && <Button icon="update" variant="outline" onClick={() => setModal('progress')}>Post progress</Button>}
        {s.status === 'IN_PROGRESS' && <Button icon="task_alt" onClick={() => setModal('deliver')}>Mark delivered</Button>}
        {s.status === 'DISPUTED' && <Button variant="outline" onClick={() => setModal('resume')}>Resume</Button>}
        {open && s.status !== 'DISPUTED' && <Button variant="ghost" onClick={() => setModal('dispute')}>Raise dispute</Button>}
        <Button variant="ghost" icon="forum" onClick={() => setModal('query')}>Query</Button>
      </>} />
      <div className="mt-16">
        <EngagementInfo e={e} quotationLink={`/seller/quotations/${s.quotationId}`}
          docs={<Docs documents={e.documents} uploadUrl={open ? `/seller/engagements/${id}/documents` : null} category="DELIVERABLE" canDelete={open} onChange={reload} />} />
      </div>
      {modal === 'request' && <ActionModal title="Request work order" remarks onConfirm={(remarks) => api.post(`/seller/engagements/${id}/work-order-request`, { remarks }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'progress' && (
        <Modal title="Post progress update" onClose={() => setModal(null)} footer={<><Button variant="ghost" onClick={() => setModal(null)}>Cancel</Button>
          <Button loading={busy} disabled={!progress.message} onClick={() => post('progress', { message: progress.message, progressPercent: progress.progressPercent ? Number(progress.progressPercent) : undefined }, 'Progress posted').then(() => { setModal(null); setProgress({ message: '', progressPercent: '' }); })}>Post</Button></>}>
          <div className="col gap-16">
            <textarea className="textarea" rows={4} placeholder="What has been done?" value={progress.message} onChange={(ev) => setProgress({ ...progress, message: ev.target.value })} />
            <Input type="number" min="0" max="100" placeholder="Progress %" value={progress.progressPercent} onChange={(ev) => setProgress({ ...progress, progressPercent: ev.target.value })} />
          </div>
        </Modal>
      )}
      {modal === 'deliver' && <ActionModal title="Mark deliverables as submitted" remarks remarksRequired remarksLabel="Delivery note" onConfirm={(message) => api.post(`/seller/engagements/${id}/deliver`, { message }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'dispute' && <ActionModal title="Raise dispute" variant="danger" remarks remarksRequired remarksLabel="Reason" onConfirm={(reason) => api.post(`/seller/engagements/${id}/dispute`, { reason }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'resume' && <ActionModal title="Resume engagement" remarks onConfirm={(remarks) => api.post(`/seller/engagements/${id}/resume`, { remarks }).then(reload)} onClose={() => setModal(null)} />}
      {modal === 'query' && <RaiseTicket contextType="ENGAGEMENT" contextId={Number(id)} counterpartyId={s.buyerId} onClose={() => setModal(null)} onDone={(t) => navigate(`/seller/queries/${t.summary.id}`)} />}
    </>
  );
}

/* ---------- Reviews ---------- */
export function SellerReviews() {
  const { data: summary } = useLoad(() => api.get('/seller/reviews/summary'), []);
  const paged = usePaged('/seller/reviews', {});
  return (
    <>
      <PageTitle title="Ratings & reviews" />
      {summary && (
        <Card className="mb-16">
          <div className="row gap-24 wrap">
            <div><div style={{ fontSize: 36, fontWeight: 700 }}>{summary.average ? Number(summary.average).toFixed(1) : '-'}</div><Stars value={summary.average} count={summary.count} /></div>
            <div className="grow" style={{ maxWidth: 360 }}>
              {[5, 4, 3, 2, 1].map((n) => {
                const c = summary.distribution?.[n] || 0;
                return <div key={n} className="bar-row"><span>{n} star</span><div className="track"><div style={{ width: `${summary.count ? (c / summary.count) * 100 : 0}%` }} /></div><span>{c}</span></div>;
              })}
            </div>
          </div>
        </Card>
      )}
      <Card>
        {paged.loading && !paged.data ? <Loading /> : !paged.data?.content.length ? <Empty icon="reviews" title="No reviews yet" /> : paged.data.content.map((r) => (
          <div key={r.id} className="card flat mb-8" style={{ padding: 14 }}>
            <div className="row between"><Stars value={r.rating} /><span className="tiny muted">{date(r.createdAt)}{!r.visible && ' · hidden'}</span></div>
            {r.title && <div className="bold mt-8">{r.title}</div>}
            {r.reviewText && <p className="small mt-4">{r.reviewText}</p>}
            <div className="tiny muted mt-8">{r.buyerName} · {r.engagementTitle} ({r.engagementNumber})</div>
          </div>
        ))}
      </Card>
    </>
  );
}
