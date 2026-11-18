import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, fieldErrors } from '../../api/client';
import { BarList, TrendChart } from '../../components/charts';
import { PageTitle } from '../../components/layouts';
import { DataTable, ExportButtons, F, Filters, History, PagedTable, payload, useForm, usePaged } from '../../components/kit';
import { ActionModal, Alert, Badge, Button, Card, DocumentList, FilePicker, Icon, Input, KeyValue, Loading, Modal, Select, StatCard, Stars, Status, Tabs, Textarea, useAction, useLoad } from '../../components/ui';
import { dateTime, inr, label, PRICING_LABELS } from '../../constants';
import { useCategories } from '../public/Services';
import { InviteModal } from '../buyer/Buyer';
import { ListingForm, listingColumns } from '../seller/Seller';
import { ENGAGEMENT_STATUSES, EngagementInfo, engagementColumns, Invitations, QUOTATION_STATUSES, QuotationInfo, quotationColumns, RFQ_STATUSES, RfqInfo, rfqColumns } from '../shared/Procurement';

const LISTING_STATUSES = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CORRECTION_REQUESTED', 'APPROVED', 'REJECTED', 'DEACTIVATED', 'FLAGGED'];
const toItems = (map) => Object.entries(map || {}).map(([k, v]) => ({ label: label(k), count: v }));

/* ---------- Categories ---------- */
export function Categories() {
  const { data: cats, reload } = useLoad(() => api.get('/admin/categories'), []);
  const [editing, setEditing] = useState(null);
  const [sub, setSub] = useState(null);
  const [run] = useAction();
  if (!cats) return <Loading />;
  return (
    <>
      <PageTitle title="Service categories" actions={<Button icon="add" onClick={() => setEditing({})}>New category</Button>} />
      <div className="col gap-16">
        {cats.map((c) => (
          <Card key={c.id} title={<div className="row gap-8"><Icon name={c.icon || 'category'} /><h3>{c.name}</h3><span className="tiny muted mono">{c.code}</span>{!c.active && <Badge tone="bad">Inactive</Badge>}<span className="tiny muted">{c.listingCount} listings</span></div>}
            actions={<>
              <Button size="sm" variant="ghost" icon="edit" onClick={() => setEditing(c)}>Edit</Button>
              <Button size="sm" variant="ghost" icon="add" onClick={() => setSub({ category: c })}>Subcategory</Button>
              <Button size="sm" variant="outline" onClick={() => run(() => api.patch(`/admin/categories/${c.id}/status`, { active: !c.active }), c.active ? 'Category deactivated' : 'Category activated').then(reload)}>{c.active ? 'Deactivate' : 'Activate'}</Button>
            </>}>
            {c.description && <p className="small muted mb-8">{c.description}</p>}
            {c.subcategories.length === 0 ? <p className="small muted">No subcategories.</p> : (
              <div className="chips">
                {c.subcategories.map((s) => (
                  <button key={s.id} className="chip selectable" onClick={() => setSub({ category: c, sub: s })} style={s.active ? undefined : { opacity: 0.5 }}>{s.name} · {s.listingCount}<Icon name="edit" size="sm" /></button>
                ))}
              </div>
            )}
          </Card>
        ))}
      </div>
      {editing && <CategoryModal category={editing} onClose={() => setEditing(null)} onDone={reload} />}
      {sub && <SubcategoryModal {...sub} onClose={() => setSub(null)} onDone={reload} />}
    </>
  );
}

function CategoryModal({ category, onClose, onDone }) {
  const form = useForm({ code: '', name: '', description: '', icon: 'category', displayOrder: '', active: true, mandatoryFields: [], eligibilityRules: '', ...category });
  const [run, busy] = useAction();
  const save = () => run(() => (category.id ? api.put(`/admin/categories/${category.id}`, payload(form.values, ['displayOrder'])) : api.post('/admin/categories', payload(form.values, ['displayOrder']))), 'Category saved')
    .then(() => { onDone(); onClose(); }).catch((e) => form.setErrors(fieldErrors(e)));
  return (
    <Modal title={category.id ? 'Edit category' : 'New category'} onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
      <div className="grid grid-2">
        <F form={form} name="code" label="Code" required hint="UPPER_CASE, digits, underscore" />
        <F form={form} name="name" label="Name" required />
        <F form={form} name="icon" label="Icon" hint="Material Symbols name" />
        <F form={form} name="displayOrder" label="Display order" type="number" />
        <F form={form} name="description" label="Description" type="textarea" className="span-2" />
        <F form={form} name="mandatoryFields" label="Mandatory listing fields" type="tags" className="span-2" />
        <F form={form} name="eligibilityRules" label="Eligibility rules" type="textarea" className="span-2" />
        <F form={form} name="active" label="Active" type="checkbox" />
      </div>
    </Modal>
  );
}

function SubcategoryModal({ category, sub, onClose, onDone }) {
  const form = useForm({ name: '', description: '', displayOrder: '', active: true, ...sub });
  const [run, busy] = useAction();
  const save = () => run(() => (sub ? api.put(`/admin/categories/${category.id}/subcategories/${sub.id}`, payload(form.values, ['displayOrder'])) : api.post(`/admin/categories/${category.id}/subcategories`, payload(form.values, ['displayOrder']))), 'Subcategory saved')
    .then(() => { onDone(); onClose(); });
  return (
    <Modal title={`${sub ? 'Edit' : 'Add'} subcategory — ${category.name}`} onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
      <div className="col gap-16">
        <F form={form} name="name" label="Name" required />
        <F form={form} name="description" label="Description" type="textarea" />
        <F form={form} name="displayOrder" label="Display order" type="number" />
        <F form={form} name="active" label="Active" type="checkbox" />
      </div>
    </Modal>
  );
}

/* ---------- Listings moderation ---------- */
export function AdminListings() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const categories = useCategories();
  const [filters, setFilters] = useState(params.get('status') ? { status: params.get('status') } : {});
  const paged = usePaged('/admin/services', filters);
  return (
    <>
      <PageTitle title="Service listings" actions={<>
        <ExportButtons url="/admin/catalog/reports/services" params={{ status: filters.status, categoryId: filters.categoryId }} />
        <Link to="/admin/listings/new" className="btn btn-primary"><Icon name="add" size="sm" />Add on behalf of seller</Link>
      </>} />
      <Filters value={filters} onChange={setFilters} fields={[
        { name: 'status', placeholder: 'Any status', options: LISTING_STATUSES },
        { name: 'categoryId', placeholder: 'Any category', options: categories.map((c) => ({ value: String(c.id), label: c.name })), width: 240 }
      ]} />
      <Card><PagedTable paged={paged} onRow={(l) => navigate(`/admin/listings/${l.id}`)} empty="No listings" columns={[listingColumns[0], { key: 'sellerName', title: 'Seller', render: (l) => <span className="small">{l.sellerName}</span> }, ...listingColumns.slice(1)]} /></Card>
    </>
  );
}

export function AdminListingNew() {
  const navigate = useNavigate();
  const [sellers, setSellers] = useState([]);
  const [sellerId, setSellerId] = useState('');
  const [run, busy] = useAction();
  useEffect(() => { api.get('/admin/users', { role: 'SELLER', status: 'ACTIVE', size: 200 }).then((p) => setSellers(p.content)).catch(() => {}); }, []);
  const save = async (listing, form) => {
    try {
      const r = await run(() => api.post('/admin/services', { sellerId: Number(sellerId), listing }), 'Listing added');
      navigate(`/admin/listings/${r.data.summary.id}`);
    } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  return (
    <>
      <PageTitle back title="Add listing on behalf of a seller" />
      <Card className="mb-16"><Select value={sellerId} onChange={(e) => setSellerId(e.target.value)} placeholder="Choose an approved seller" options={sellers.map((s) => ({ value: String(s.id), label: `${s.organisationName || s.fullName} (${s.referenceNo})` }))} /></Card>
      {sellerId ? <ListingForm onSubmit={save} busy={busy} submitLabel="Add listing" allowDraft={false} /> : <Alert tone="info">Choose a seller to continue.</Alert>}
    </>
  );
}

export function AdminListingDetail() {
  const { id } = useParams();
  const { data: l, reload } = useLoad(() => api.get(`/admin/services/${id}`), [id]);
  const { data: history, reload: reloadHistory } = useLoad(() => api.get(`/admin/services/${id}/history`), [id]);
  const [modal, setModal] = useState(null);
  const [run, busy] = useAction();
  if (!l) return <Loading />;
  const s = l.summary;
  const refresh = () => { reload(); reloadHistory(); };
  const act = (path, required) => setModal({ path, required });
  return (
    <>
      <PageTitle back title={s.title} subtitle={<><span className="mono">{s.referenceNo}</span> · {s.sellerName} · <Status value={s.status} /></>} actions={<>
        {s.status === 'SUBMITTED' && <Button loading={busy} onClick={() => run(() => api.post(`/admin/services/${id}/start-review`), 'Review started').then(refresh)}>Start review</Button>}
        {['SUBMITTED', 'UNDER_REVIEW', 'FLAGGED'].includes(s.status) && <Button icon="check" onClick={() => act('approve')}>Approve</Button>}
        {['SUBMITTED', 'UNDER_REVIEW'].includes(s.status) && <Button variant="outline" onClick={() => act('request-correction', true)}>Request correction</Button>}
        {['SUBMITTED', 'UNDER_REVIEW'].includes(s.status) && <Button variant="ghost" onClick={() => act('reject', true)}>Reject</Button>}
        {s.status === 'APPROVED' && <Button variant="outline" onClick={() => act('flag', true)}>Flag</Button>}
        {['APPROVED', 'FLAGGED'].includes(s.status) && <Button variant="ghost" onClick={() => act('deactivate', true)}>Deactivate</Button>}
        {s.status === 'APPROVED' && <Button variant="ghost" icon="star" loading={busy} onClick={() => run(() => api.put(`/admin/services/${id}/featured`, { featured: !s.featured }), 'Featured flag updated').then(reload)}>{s.featured ? 'Unfeature' : 'Feature'}</Button>}
      </>} />
      {l.moderationRemarks && <Alert tone="warn">{l.moderationRemarks}</Alert>}
      <div className="layout-main-side mt-16">
        <div className="col gap-16">
          <Card>
            <KeyValue items={[['Seller', s.sellerName], ['Category', `${s.categoryName}${s.subcategoryName ? ` · ${s.subcategoryName}` : ''}`], ['Delivery', label(s.deliveryMode)], ['Pricing', `${s.indicativeFee ? inr(s.indicativeFee) : '-'} · ${PRICING_LABELS[s.pricingModel]}`],
              ['Duration', s.estimatedDuration], ['Experience', s.experienceYears != null ? `${s.experienceYears} years` : null], ['Locations', l.serviceLocations?.join(', ')], ['Industries', l.targetIndustries?.join(', ')], ['Tags', l.tags?.join(', ')],
              ['Views / enquiries', `${l.viewCount} / ${l.enquiryCount}`], ['Submitted', dateTime(l.submittedAt)], ['Published', dateTime(s.publishedAt)], ['Public', l.publiclyVisible ? 'Yes' : 'No']]} />
            {[['Short description', s.shortDescription], ['Description', l.description], ['Scope', l.scopeOfWork], ['Deliverables', l.deliverables], ['Methodology', l.methodology]].map(([t, v]) => v && <div key={t} className="mt-16"><h4>{t}</h4><p className="pre small mt-4">{v}</p></div>)}
          </Card>
          <Card title="Attachments" actions={<FilePicker label="Attach" busy={busy} onFile={(file) => run(() => api.upload(`/admin/services/${id}/attachments`, { file }), 'Attached').then(reload)} />}>
            <DocumentList documents={l.attachments.map((a) => ({ ...a, category: a.label }))} onDownload={(a) => api.download(`/services/${id}/attachments/${a.id}/download`, null, a.originalName)} />
          </Card>
        </div>
        <Card title="Moderation history"><History items={history || []} /></Card>
      </div>
      {modal && <ActionModal title={label(modal.path)} remarks remarksRequired={modal.required} variant={['reject', 'flag', 'deactivate'].includes(modal.path) ? 'danger' : 'primary'}
        onConfirm={(remarks) => api.post(`/admin/services/${id}/${modal.path}`, { remarks }).then(refresh)} onClose={() => setModal(null)} />}
    </>
  );
}

/* ---------- Procurement monitoring ---------- */
export function AdminRfqs() {
  const navigate = useNavigate();
  const categories = useCategories();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/admin/rfqs', filters);
  return (
    <>
      <PageTitle title="RFQs" actions={<ExportButtons url="/admin/procurement/reports/rfqs" params={{ status: filters.status, categoryId: filters.categoryId }} />} />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: RFQ_STATUSES }, { name: 'categoryId', placeholder: 'Any category', options: categories.map((c) => ({ value: String(c.id), label: c.name })), width: 240 }]} />
      <Card><PagedTable paged={paged} onRow={(r) => navigate(`/admin/rfqs/${r.id}`)} empty="No RFQs" columns={rfqColumns([
        { key: 'buyerName', title: 'Purchaser', render: (r) => <span className="small">{r.buyerName}</span> },
        { key: 'distributionMode', title: 'Distribution', render: (r) => <span className="small">{label(r.distributionMode)}</span> },
        { key: 'quotationCount', title: 'Quotes', render: (r) => `${r.quotationCount}/${r.invitedCount}` }
      ])} /></Card>
    </>
  );
}

export function AdminRfqDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: rfq, reload } = useLoad(() => api.get(`/admin/rfqs/${id}`), [id]);
  const [inviting, setInviting] = useState(false);
  if (!rfq) return <Loading />;
  const s = rfq.summary;
  return (
    <>
      <PageTitle back title={s.title} subtitle={<><span className="mono">{s.rfqNumber}</span> · {s.buyerName} · <Status value={s.status} /></>} actions={<>
        {['PUBLISHED', 'RESPONSES_RECEIVED', 'UNDER_EVALUATION'].includes(s.status) && <Button icon="person_add" onClick={() => setInviting(true)}>Match providers</Button>}
        {s.quotationCount > 0 && <Link to={`/admin/rfqs/${id}/comparison`} className="btn btn-outline"><Icon name="compare" size="sm" />Comparison</Link>}
        <Button variant="ghost" onClick={() => navigate(`/admin/quotations?rfqId=${id}`)}>Quotations</Button>
      </>} />
      {s.distributionMode === 'ADMIN_ASSISTED' && <Alert tone="warn">The purchaser asked for administrator-assisted provider matching.</Alert>}
      <div className="layout-main-side mt-16">
        <RfqInfo rfq={rfq} />
        <Card title="Invited providers"><Invitations invitations={rfq.invitations} /></Card>
      </div>
      {inviting && <InviteModal rfq={rfq} url={`/admin/rfqs/${id}/invite`} recommendedUrl={`/admin/rfqs/${id}/recommended-sellers`} onDone={reload} onClose={() => setInviting(false)} />}
    </>
  );
}

export function AdminQuotations() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [filters, setFilters] = useState(params.get('rfqId') ? { rfqId: params.get('rfqId') } : {});
  const paged = usePaged('/admin/quotations', filters);
  return (
    <>
      <PageTitle title="Quotations" actions={<ExportButtons url="/admin/procurement/reports/quotations" params={{ status: filters.status }} />} />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: QUOTATION_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(q) => navigate(`/admin/quotations/${q.id}`)} empty="No quotations" columns={quotationColumns([
        { key: 'sellerName', title: 'Provider', render: (q) => <span className="small">{q.sellerName}</span> }, { key: 'buyerName', title: 'Purchaser', render: (q) => <span className="small">{q.buyerName}</span> }
      ])} /></Card>
    </>
  );
}

export function AdminQuotationDetail() {
  const { id } = useParams();
  const { data: q } = useLoad(() => api.get(`/admin/quotations/${id}`), [id]);
  if (!q) return <Loading />;
  return <><PageTitle back title={q.rfqTitle} subtitle={<><span className="mono">{q.quotationNumber}</span> · <Status value={q.status} /></>} /><div className="mt-16"><QuotationInfo q={q} /></div></>;
}

export function AdminEngagements() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({});
  const paged = usePaged('/admin/engagements', filters);
  return (
    <>
      <PageTitle title="Engagements" actions={<ExportButtons url="/admin/procurement/reports/engagements" params={{ status: filters.status }} />} />
      <Filters value={filters} onChange={setFilters} fields={[{ name: 'status', placeholder: 'Any status', options: ENGAGEMENT_STATUSES }]} />
      <Card><PagedTable paged={paged} onRow={(e) => navigate(`/admin/engagements/${e.id}`)} empty="No engagements" columns={engagementColumns([
        { key: 'parties', title: 'Parties', render: (e) => <span className="small">{e.buyerName} ↔ {e.sellerName}</span> }
      ])} /></Card>
    </>
  );
}

export function AdminEngagementDetail() {
  const { id } = useParams();
  const { data: e } = useLoad(() => api.get(`/admin/engagements/${id}`), [id]);
  if (!e) return <Loading />;
  return <><PageTitle back title={e.summary.title} subtitle={<><span className="mono">{e.summary.engagementNumber}</span> · <Status value={e.summary.status} /></>} /><div className="mt-16"><EngagementInfo e={e} quotationLink={`/admin/quotations/${e.summary.quotationId}`} /></div></>;
}

/* ---------- Feedback ---------- */
export function AdminFeedback() {
  const [tab, setTab] = useState('reviews');
  return (
    <>
      <PageTitle title="Feedback & reviews" />
      <Tabs value={tab} onChange={setTab} tabs={[{ value: 'reviews', label: 'Reviews' }, { value: 'questions', label: 'Feedback questions' }]} />
      <div className="mt-16">{tab === 'reviews' ? <Reviews /> : <Questions />}</div>
    </>
  );
}

function Reviews() {
  const paged = usePaged('/admin/reviews', {});
  const [hiding, setHiding] = useState(null);
  const [run] = useAction();
  return (
    <Card>
      <PagedTable paged={paged} empty="No reviews yet" emptyIcon="reviews" columns={[
        { key: 'rating', title: 'Rating', render: (r) => <Stars value={r.rating} /> },
        { key: 'review', title: 'Review', render: (r) => <><div className="small bold">{r.title || '-'}</div><div className="small">{r.reviewText}</div></> },
        { key: 'parties', title: 'Parties', render: (r) => <span className="small">{r.buyerName} → {r.sellerName}<div className="tiny muted mono">{r.engagementNumber}</div></span> },
        { key: 'createdAt', title: 'Date', render: (r) => <span className="small">{dateTime(r.createdAt)}</span> },
        { key: 'visible', title: 'Visibility', render: (r) => (r.visible
          ? <Button size="sm" variant="outline" onClick={() => setHiding(r)}>Hide</Button>
          : <Button size="sm" onClick={() => run(() => api.put(`/admin/reviews/${r.id}/visibility`, { visible: true }), 'Review published').then(paged.reload)}>Publish</Button>) }
      ]} />
      {hiding && <ActionModal title="Hide review" remarks remarksRequired onConfirm={(remarks) => api.put(`/admin/reviews/${hiding.id}/visibility`, { visible: false, remarks }).then(paged.reload)} onClose={() => setHiding(null)} />}
    </Card>
  );
}

function Questions() {
  const { data: qs, reload } = useLoad(() => api.get('/admin/feedback-questions'), []);
  const [editing, setEditing] = useState(null);
  return (
    <Card title="Questions asked after an engagement is completed" actions={<Button size="sm" icon="add" onClick={() => setEditing({})}>New question</Button>}>
      <DataTable rows={qs} onRow={setEditing} empty="No questions" columns={[
        { key: 'displayOrder', title: '#' }, { key: 'questionText', title: 'Question' }, { key: 'questionType', title: 'Type', render: (q) => label(q.questionType) },
        { key: 'mandatory', title: 'Mandatory', render: (q) => (q.mandatory ? 'Yes' : 'No') }, { key: 'active', title: 'Active', render: (q) => (q.active ? 'Yes' : 'No') }
      ]} />
      {editing && <QuestionModal question={editing} onClose={() => setEditing(null)} onDone={reload} />}
    </Card>
  );
}

function QuestionModal({ question, onClose, onDone }) {
  const form = useForm({ questionText: '', questionType: 'RATING', displayOrder: '', mandatory: false, active: true, ...question });
  const [run, busy] = useAction();
  const save = () => run(() => (question.id ? api.put(`/admin/feedback-questions/${question.id}`, payload(form.values, ['displayOrder'])) : api.post('/admin/feedback-questions', payload(form.values, ['displayOrder']))), 'Question saved').then(() => { onDone(); onClose(); });
  return (
    <Modal title={question.id ? 'Edit question' : 'New question'} onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
      <div className="col gap-16">
        <F form={form} name="questionText" label="Question" required />
        <F form={form} name="questionType" label="Type" type="select" options={['RATING', 'YES_NO', 'TEXT']} />
        <F form={form} name="displayOrder" label="Display order" type="number" />
        <F form={form} name="mandatory" label="Mandatory" type="checkbox" />
        <F form={form} name="active" label="Active" type="checkbox" />
      </div>
    </Modal>
  );
}

/* ---------- Reports ---------- */
export function Reports() {
  const { data: id } = useLoad(() => api.get('/admin/identity/stats'), []);
  const { data: pr } = useLoad(() => api.get('/admin/procurement/stats'), []);
  const { data: cat } = useLoad(() => api.get('/admin/catalog/stats'), []);
  const reports = [
    ['Seller registrations', '/admin/identity/reports/sellers'], ['Purchaser registrations', '/admin/identity/reports/buyers'], ['Service catalogue', '/admin/catalog/reports/services'],
    ['RFQs', '/admin/procurement/reports/rfqs'], ['Quotations', '/admin/procurement/reports/quotations'], ['Engagements', '/admin/procurement/reports/engagements'], ['Grievances', '/admin/procurement/reports/grievances']
  ];
  return (
    <>
      <PageTitle title="Reports & MIS" />
      <Card title="Download reports">
        <div className="col gap-8">{reports.map(([t, url]) => <div key={url} className="row between"><span className="bold small">{t}</span><ExportButtons url={url} /></div>)}</div>
      </Card>
      <div className="grid grid-2 mt-16">
        {id && <TrendChart title="Registration trend" data={id.registrationTrend} series={[{ key: 'sellers', name: 'Sellers' }, { key: 'buyers', name: 'Purchasers' }]} />}
        {pr && <TrendChart title="Procurement trend" data={pr.monthlyTrend} series={[{ key: 'rfqs', name: 'RFQs' }, { key: 'quotations', name: 'Quotations' }, { key: 'engagements', name: 'Engagements' }]} />}
        {id && <Card title="Sellers by state"><BarList items={id.sellersByState} /></Card>}
        {id && <Card title="Sellers by provider type"><BarList items={id.sellersByProviderType.map((i) => ({ ...i, label: label(i.label) }))} /></Card>}
        {id && <Card title="Purchasers by type"><BarList items={id.buyersByType.map((i) => ({ ...i, label: label(i.label) }))} /></Card>}
        {id && <Card title="Purchasers by industry"><BarList items={id.buyersByIndustry} /></Card>}
        {cat && <Card title="Listings by status"><BarList items={toItems(cat.byStatus)} /></Card>}
        {pr && <Card title="RFQs by category"><BarList items={pr.rfqsByCategory} /></Card>}
        {pr && <Card title="RFQs by state"><BarList items={pr.rfqsByState} /></Card>}
        {pr && <Card title="Quotations by status"><BarList items={toItems(pr.quotationsByStatus)} /></Card>}
        {pr && <Card title="Engagements by status"><BarList items={toItems(pr.engagementsByStatus)} /></Card>}
      </div>
    </>
  );
}

/* ---------- Audit logs ---------- */
export function AuditLogs() {
  const [filters, setFilters] = useState({});
  const [range, setRange] = useState({ from: '', to: '' });
  const query = { ...filters, from: range.from || undefined, to: range.to || undefined };
  const paged = usePaged('/admin/audit-logs', query, 20);
  return (
    <>
      <PageTitle title="Audit logs" actions={<ExportButtons url="/admin/audit-logs/export" params={{ service: filters.service, action: filters.action, from: query.from, to: query.to }} />} />
      <div className="row wrap gap-8">
        <Filters value={filters} onChange={setFilters} fields={[{ name: 'service', placeholder: 'Any service', options: ['auth', 'catalog', 'procurement', 'notification'], labels: { auth: 'Identity', catalog: 'Catalogue', procurement: 'Procurement', notification: 'Notifications' } }]} />
        <Input type="date" value={range.from} onChange={(e) => setRange({ ...range, from: e.target.value })} style={{ width: 160 }} aria-label="From" />
        <Input type="date" value={range.to} onChange={(e) => setRange({ ...range, to: e.target.value })} style={{ width: 160 }} aria-label="To" />
      </div>
      <Card>
        <PagedTable paged={paged} empty="No audit entries" emptyIcon="history" columns={[
          { key: 'createdAt', title: 'Time', render: (a) => <span className="small nowrap">{dateTime(a.createdAt)}</span> },
          { key: 'actor', title: 'Actor', render: (a) => <span className="small">{a.actorName || '-'}{a.actorRole ? ` (${label(a.actorRole)})` : ''}</span> },
          { key: 'action', title: 'Action', render: (a) => <><div className="small bold">{a.action}</div><div className="tiny muted">{a.serviceName}</div></> },
          { key: 'entity', title: 'Entity', render: (a) => <span className="small mono">{a.entityType} {a.entityId}</span> },
          { key: 'details', title: 'Details', render: (a) => <span className="small">{a.details}</span> },
          { key: 'ipAddress', title: 'IP', render: (a) => <span className="tiny mono">{a.ipAddress}</span> }
        ]} />
      </Card>
    </>
  );
}

/* ---------- System settings ---------- */
export function SystemSettings() {
  const [tab, setTab] = useState('settings');
  return (
    <>
      <PageTitle title="System settings" />
      <Tabs value={tab} onChange={setTab} tabs={[{ value: 'settings', label: 'Platform settings' }, { value: 'templates', label: 'Notification templates' }, { value: 'health', label: 'System health' }]} />
      <div className="mt-16">{tab === 'settings' ? <Settings /> : tab === 'templates' ? <Templates /> : <Health />}</div>
    </>
  );
}

function Settings() {
  const { data: settings, reload } = useLoad(() => api.get('/admin/settings'), []);
  const [values, setValues] = useState({});
  const [run] = useAction();
  useEffect(() => { if (settings) setValues(Object.fromEntries(settings.map((s) => [s.key, s.value ?? '']))); }, [settings]);
  if (!settings) return <Loading />;
  const groups = [...new Set(settings.map((s) => s.group))];
  return (
    <div className="col gap-16">
      {groups.map((g) => (
        <Card key={g} title={label(g)}>
          <div className="col gap-16">
            {settings.filter((s) => s.group === g).map((s) => (
              <div key={s.key} className="row top gap-16">
                <div style={{ width: 260 }}><div className="small bold mono">{s.key}</div><div className="tiny muted">{s.description}{s.publicSetting ? ' · public' : ''}</div></div>
                <div className="grow">{(values[s.key] || '').length > 80 ? <Textarea rows={3} value={values[s.key]} onChange={(e) => setValues({ ...values, [s.key]: e.target.value })} />
                  : <Input value={values[s.key] ?? ''} onChange={(e) => setValues({ ...values, [s.key]: e.target.value })} />}</div>
                <Button size="sm" variant="outline" disabled={(values[s.key] ?? '') === (s.value ?? '')} onClick={() => run(() => api.put(`/admin/settings/${encodeURIComponent(s.key)}`, { value: values[s.key] }), 'Setting saved').then(reload)}>Save</Button>
              </div>
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}

function Templates() {
  const { data: templates, reload } = useLoad(() => api.get('/admin/notification-templates'), []);
  const [editing, setEditing] = useState(null);
  return (
    <Card>
      <DataTable rows={templates} onRow={setEditing} empty="No templates" columns={[
        { key: 'code', title: 'Code', render: (t) => <span className="small mono">{t.code}</span> }, { key: 'title', title: 'Title', render: (t) => <span className="small">{t.title}</span> },
        { key: 'category', title: 'Category', render: (t) => <span className="small">{label(t.category)}</span> },
        { key: 'channels', title: 'Channels', render: (t) => <span className="small">In-app{t.emailEnabled ? ' · Email' : ''}{t.smsEnabled ? ' · SMS' : ''}</span> },
        { key: 'active', title: 'Active', render: (t) => (t.active ? 'Yes' : 'No') }
      ]} />
      {editing && <TemplateModal template={editing} onClose={() => setEditing(null)} onDone={reload} />}
    </Card>
  );
}

function TemplateModal({ template, onClose, onDone }) {
  const form = useForm(template);
  const [run, busy] = useAction();
  const save = () => run(() => api.put(`/admin/notification-templates/${template.id}`, payload({ title: form.values.title, message: form.values.message, category: form.values.category, severity: form.values.severity, emailEnabled: form.values.emailEnabled, smsEnabled: form.values.smsEnabled, active: form.values.active, description: form.values.description })), 'Template saved').then(() => { onDone(); onClose(); });
  return (
    <Modal title={template.code} wide onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
      <div className="col gap-16">
        <F form={form} name="title" label="Title" required />
        <F form={form} name="message" label="Message" type="textarea" rows={4} required hint={template.placeholders?.length ? `Placeholders: ${template.placeholders.map((p) => `{${p}}`).join(' ')}` : undefined} />
        <div className="grid grid-2">
          <F form={form} name="category" label="Category" />
          <F form={form} name="severity" label="Severity" type="select" options={['INFO', 'SUCCESS', 'WARNING', 'ACTION']} />
        </div>
        <F form={form} name="description" label="Description" />
        <div className="row gap-16"><F form={form} name="emailEnabled" label="Email" type="checkbox" /><F form={form} name="smsEnabled" label="SMS" type="checkbox" /><F form={form} name="active" label="Active" type="checkbox" /></div>
      </div>
    </Modal>
  );
}

function Health() {
  const { data: h, reload } = useLoad(() => api.get('/admin/system/health'), []);
  if (!h) return <Loading />;
  return (
    <Card title="Backend health" actions={<Button size="sm" variant="ghost" icon="refresh" onClick={reload}>Refresh</Button>}>
      <div className="grid grid-4 mb-16">
        <StatCard icon="monitor_heart" value={h.overall} label="Overall" tone={h.overall === 'UP' ? 'good' : 'bad'} />
        <StatCard icon="send" value={h.notificationsSent} label="Notifications sent" />
        <StatCard icon="error" value={h.notificationsFailed} label="Failed" tone={h.notificationsFailed ? 'bad' : undefined} />
        <StatCard icon="block" value={h.notificationsSkipped} label="Skipped" />
      </div>
      <DataTable rows={h.services.map((s) => ({ ...s, id: s.service }))} columns={[{ key: 'service', title: 'Service' }, { key: 'status', title: 'Status' }, { key: 'instances', title: 'Instances' }, { key: 'urls', title: 'URLs', render: (s) => <span className="tiny mono">{s.urls?.join(', ')}</span> }]} />
      <p className="tiny muted mt-8">Checked {dateTime(h.checkedAt)}</p>
    </Card>
  );
}

