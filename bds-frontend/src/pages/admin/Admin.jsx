import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, fieldErrors } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { BarList, TrendChart } from '../../components/charts';
import { PageTitle } from '../../components/layouts';
import { DataTable, F, Filters, History, PagedTable, payload, useForm, usePaged } from '../../components/kit';
import { ActionModal, Alert, Badge, Button, Card, Checkbox, DocumentList, Icon, KeyValue, Loading, Modal, StatCard, Stars, Status, Tabs, useAction, useLoad } from '../../components/ui';
import { CONSTITUTION_LABELS, CONSTITUTIONS, date, dateTime, DELIVERY_MODES, INDUSTRIES, label, PROVIDER_TYPES, PURCHASER_LABELS, PURCHASER_TYPES, STATES } from '../../constants';
import { useCategories } from '../public/Services';

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));
const USER_STATUSES = ['PENDING_VERIFICATION', 'INFO_REQUESTED', 'ACTIVE', 'SUSPENDED', 'REJECTED', 'DEACTIVATED'];
const toItems = (map) => Object.entries(map || {}).map(([k, v]) => ({ label: label(k), count: v }));

/* ---------- Dashboard ---------- */
export function AdminDashboard() {
  const navigate = useNavigate();
  const { can } = useAuth();
  const { data: id } = useLoad(() => api.get('/admin/identity/stats'), []);
  const { data: pr } = useLoad(() => (can('RFQ_MONITOR') || can('REPORT_VIEW') ? api.get('/admin/procurement/stats') : Promise.resolve(null)), []);
  const { data: cat } = useLoad(() => (can('CATALOG_MANAGE') || can('LISTING_MODERATE') || can('REPORT_VIEW') ? api.get('/admin/catalog/stats') : Promise.resolve(null)), []);
  const { data: health } = useLoad(() => api.get('/admin/system/health').catch(() => null), []);
  if (!id) return <Loading />;
  return (
    <>
      <PageTitle title="Administrator dashboard" />
      <div className="grid grid-4">
        <StatCard icon="how_to_reg" value={id.pendingSellers} label="Sellers pending verification" tone="warn" onClick={() => navigate('/admin/approvals')} />
        <StatCard icon="store" value={id.totalSellers} label="Sellers" onClick={() => navigate('/admin/sellers')} />
        <StatCard icon="corporate_fare" value={id.totalBuyers} label="Purchasers" sub={`${id.pendingBuyers} pending`} onClick={() => navigate('/admin/buyers')} />
        {cat && <StatCard icon="fact_check" value={cat.pendingModeration} label="Listings awaiting moderation" tone="warn" onClick={() => navigate('/admin/listings?status=SUBMITTED')} />}
        {cat && <StatCard icon="storefront" value={cat.publicListings} label="Published services" sub={`${cat.totalListings} total`} />}
        {pr && <StatCard icon="description" value={pr.activeRfqs} label="Active RFQs" sub={`${pr.totalRfqs} total`} onClick={() => navigate('/admin/rfqs')} />}
        {pr && <StatCard icon="handshake" value={pr.activeEngagements} label="Active engagements" sub={`${pr.completedEngagements} completed`} onClick={() => navigate('/admin/engagements')} />}
        {pr && <StatCard icon="support_agent" value={pr.openGrievances} label="Open grievances" tone={pr.openGrievances ? 'bad' : undefined} sub={`${pr.escalatedQueries} escalated · ${pr.overdueQueries} overdue`} onClick={() => navigate('/admin/grievances')} />}
      </div>
      <div className="grid grid-2 mt-16">
        <TrendChart title="Registrations" data={id.registrationTrend} series={[{ key: 'sellers', name: 'Sellers' }, { key: 'buyers', name: 'Purchasers' }]} />
        {pr && <TrendChart title="Procurement activity" data={pr.monthlyTrend} series={[{ key: 'rfqs', name: 'RFQs' }, { key: 'quotations', name: 'Quotations' }, { key: 'engagements', name: 'Engagements' }]} />}
        <Card title="Sellers by status"><BarList items={toItems(id.sellersByStatus)} /></Card>
        {cat && <Card title="Listings by category"><BarList items={cat.byCategory} /></Card>}
        {pr && <Card title="RFQs by status"><BarList items={toItems(pr.rfqsByStatus)} /></Card>}
        {pr && <Card title="Key ratios"><KeyValue items={[['Selection rate', `${Math.round(pr.selectionRate || 0)}%`], ['Avg. quotations per RFQ', Number(pr.averageQuotationsPerRfq || 0).toFixed(1)], ['Total quotations', pr.totalQuotations], ['Total engagements', pr.totalEngagements]]} /></Card>}
        {health && (
          <Card title="System health" actions={<Status value={health.overall === 'UP' ? 'ACTIVE' : 'FAILED'} text={health.overall} />}>
            <KeyValue items={[...health.services.map((s) => [s.service, `${s.status} (${s.instances} instance${s.instances === 1 ? '' : 's'})`]),
              ['Notifications sent', health.notificationsSent], ['Failed', health.notificationsFailed], ['Skipped', health.notificationsSkipped], ['Checked', dateTime(health.checkedAt)]]} />
          </Card>
        )}
      </div>
    </>
  );
}

/* ---------- Users ---------- */
const userColumns = [
  { key: 'name', title: 'Name', render: (u) => <><div className="bold small">{u.organisationName || u.fullName}</div><div className="tiny muted">{u.organisationName ? u.fullName : ''} <span className="mono">{u.referenceNo}</span></div></> },
  { key: 'email', title: 'Contact', render: (u) => <><div className="small">{u.email}</div><div className="tiny muted">{u.mobile}</div></> },
  { key: 'typeLabel', title: 'Type', render: (u) => <span className="small">{u.typeLabel || label(u.role)}</span> },
  { key: 'state', title: 'Location', render: (u) => <span className="small">{[u.district, u.state].filter(Boolean).join(', ') || '-'}</span> },
  { key: 'registeredAt', title: 'Registered', render: (u) => <span className="small muted">{date(u.registeredAt)}</span> },
  { key: 'status', title: 'Status', render: (u) => <Status value={u.status} /> }
];

export function UserList({ role, title, approvals }) {
  const navigate = useNavigate();
  const [filters, setFilters] = useState(approvals ? { status: 'PENDING_VERIFICATION' } : {});
  const [creating, setCreating] = useState(false);
  const paged = usePaged('/admin/users', { role, ...filters });
  const base = role === 'ADMIN' ? '/admin/admins' : role === 'SELLER' ? (approvals ? '/admin/approvals' : '/admin/sellers') : '/admin/buyers';
  const report = role === 'SELLER' ? '/admin/identity/reports/sellers' : role === 'BUYER' ? '/admin/identity/reports/buyers' : null;
  return (
    <>
      <PageTitle title={title} actions={<>
        {role === 'SELLER' && !approvals && <Link to="/admin/sellers/new" className="btn btn-primary"><Icon name="add" size="sm" />Onboard seller</Link>}
        {role === 'BUYER' && <Link to="/admin/buyers/new" className="btn btn-primary"><Icon name="add" size="sm" />Onboard purchaser</Link>}
        {role === 'ADMIN' && <Button icon="add" onClick={() => setCreating(true)}>New administrator</Button>}
        {report && <ReportButtons url={report} params={{ status: filters.status, state: filters.state }} />}
      </>} />
      {approvals ? (
        <Tabs value={filters.status} onChange={(status) => setFilters({ ...filters, status })} tabs={[{ value: 'PENDING_VERIFICATION', label: 'Pending verification' }, { value: 'INFO_REQUESTED', label: 'Information requested' }]} />
      ) : null}
      <div className={approvals ? 'mt-16' : ''}>
        <Filters value={filters} onChange={setFilters} fields={[
          ...(approvals ? [] : [{ name: 'status', placeholder: 'Any status', options: USER_STATUSES }]),
          ...(role === 'ADMIN' ? [] : [{ name: 'state', placeholder: 'Any state', options: STATES, labels: asLabels(STATES) }])
        ]} />
      </div>
      <Card><PagedTable paged={paged} onRow={(u) => navigate(`${base}/${u.id}`)} empty="No users found" emptyIcon="group" columns={userColumns} /></Card>
      {creating && <CreateAdmin onClose={() => setCreating(false)} onDone={paged.reload} />}
    </>
  );
}

function ReportButtons({ url, params }) {
  const [run, busy] = useAction();
  return (
    <div className="row gap-4">
      {['XLSX', 'CSV', 'PDF'].map((f) => <Button key={f} size="sm" variant="ghost" icon="download" loading={busy} onClick={() => run(() => api.download(url, { ...params, format: f }, `report.${f.toLowerCase()}`))}>{f}</Button>)}
    </div>
  );
}

function usePermissions() {
  const [perms, setPerms] = useState([]);
  useEffect(() => { api.get('/admin/users/permissions').then(setPerms).catch(() => {}); }, []);
  return perms;
}

function PermissionPicker({ role, value, onChange }) {
  const perms = usePermissions().filter((p) => p.role === role);
  const toggle = (code) => onChange(value.includes(code) ? value.filter((c) => c !== code) : [...value, code]);
  return (
    <div className="col gap-8">
      {perms.map((p) => <Checkbox key={p.code} checked={value.includes(p.code)} onChange={() => toggle(p.code)} label={<span><b className="small">{label(p.code)}</b> <span className="tiny muted">— {p.description}</span></span>} />)}
    </div>
  );
}

function TempPassword({ result, onClose }) {
  return (
    <Modal title="Account created" onClose={onClose} footer={<Button onClick={onClose}>Done</Button>}>
      <Alert tone="good" title={`Reference ${result.referenceNo}`}>Share the temporary password securely. The user must change it at first sign in.</Alert>
      <div className="demo-box mt-16">Temporary password: <b className="mono">{result.temporaryPassword}</b></div>
    </Modal>
  );
}

function CreateAdmin({ onClose, onDone }) {
  const form = useForm({ fullName: '', email: '', mobile: '', designation: '', permissions: ['ADMIN_DASHBOARD'] });
  const [run, busy] = useAction();
  const [result, setResult] = useState(null);
  if (result) return <TempPassword result={result} onClose={() => { onDone(); onClose(); }} />;
  return (
    <Modal title="New administrator" wide onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button>
      <Button loading={busy} onClick={() => run(() => api.post('/admin/users/admins', payload(form.values)), 'Administrator created').then((r) => setResult(r.data)).catch((e) => form.setErrors(fieldErrors(e)))}>Create</Button></>}>
      <div className="grid grid-2">
        <F form={form} name="fullName" label="Full name" required />
        <F form={form} name="email" label="Email" type="email" required />
        <F form={form} name="mobile" label="Mobile" />
        <F form={form} name="designation" label="Designation" />
        <div className="span-2"><h4 className="mb-8">Privileges</h4><PermissionPicker role="ADMIN" value={form.values.permissions} onChange={form.set('permissions')} /></div>
      </div>
    </Modal>
  );
}

export function UserDetail() {
  const { id } = useParams();
  const { user: me } = useAuth();
  const { data: u, reload } = useLoad(() => api.get(`/admin/users/${id}`), [id]);
  const [modal, setModal] = useState(null);
  const [perms, setPerms] = useState([]);
  const [temp, setTemp] = useState(null);
  const [run, busy] = useAction();
  useEffect(() => { if (u) setPerms(u.permissions || []); }, [u]);
  if (!u) return <Loading />;
  const p = u.sellerProfile || u.buyerProfile;
  const pending = ['PENDING_VERIFICATION', 'INFO_REQUESTED'].includes(u.status);
  const post = (path, body, msg) => api.post(`/admin/users/${id}/${path}`, body).then(reload).then(() => msg);
  return (
    <>
      <PageTitle back title={p?.organisationName || u.fullName} subtitle={<><span className="mono">{u.referenceNo}</span> · {label(u.role)} · <Status value={u.status} />{u.locked && <Badge tone="bad" icon="lock">Locked</Badge>}</>} actions={<>
        {pending && <Button icon="check" onClick={() => setModal('approve')}>Approve</Button>}
        {pending && <Button variant="outline" onClick={() => setModal('request-info')}>Request info</Button>}
        {pending && <Button variant="ghost" onClick={() => setModal('reject')}>Reject</Button>}
        {u.status === 'ACTIVE' && u.id !== me.id && <Button variant="outline" onClick={() => setModal('suspend')}>Suspend</Button>}
        {['SUSPENDED', 'DEACTIVATED', 'REJECTED'].includes(u.status) && <Button variant="outline" onClick={() => setModal('activate')}>Activate</Button>}
        {u.locked && <Button variant="outline" icon="lock_open" loading={busy} onClick={() => run(() => api.post(`/admin/users/${id}/unlock`), 'Account unlocked').then(reload)}>Unlock</Button>}
        {u.id !== me.id && <Button variant="ghost" icon="key" onClick={() => setModal('reset')}>Reset password</Button>}
        {u.role === 'SELLER' && u.status === 'ACTIVE' && <Button variant="ghost" icon="star" loading={busy} onClick={() => run(() => api.put(`/admin/users/${id}/featured`, { featured: true }), 'Marked as featured')}>Feature</Button>}
        {u.role === 'SELLER' && u.status === 'ACTIVE' && <Button variant="ghost" loading={busy} onClick={() => run(() => api.put(`/admin/users/${id}/featured`, { featured: false }), 'Removed from featured')}>Unfeature</Button>}
      </>} />
      {u.statusRemarks && <Alert tone="warn">{u.statusRemarks}</Alert>}
      <div className="layout-main-side mt-16">
        <div className="col gap-16">
          <Card title="Account">
            <KeyValue items={[['Name', u.fullName], ['Email', `${u.email}${u.emailVerified ? ' (verified)' : ''}`], ['Mobile', u.mobile], ['Designation', u.designation], ['Registered', dateTime(u.registeredAt)], ['Last login', dateTime(u.lastLoginAt)]]} />
          </Card>
          {u.sellerProfile && <SellerProfileView p={u.sellerProfile} />}
          {u.buyerProfile && <BuyerProfileView p={u.buyerProfile} />}
          {u.role === 'SELLER' && (
            <Card title="Verification documents">
              <DocumentList documents={u.documents} onDownload={(d) => api.download(`/admin/users/${id}/documents/${d.id}/download`, null, d.originalName)} />
              <div className="col gap-8 mt-16">
                {u.documents.map((d) => (
                  <div key={d.id} className="row between small">
                    <span>{label(d.documentType)} — {d.originalName}{d.remarks ? <span className="tiny muted"> ({d.remarks})</span> : null}</span>
                    <span className="row gap-4">
                      <Button size="sm" variant="outline" icon="verified" disabled={d.verified} onClick={() => run(() => api.put(`/admin/users/${id}/documents/${d.id}/verify`, { verified: true }), 'Document verified').then(reload)}>Verify</Button>
                      <Button size="sm" variant="ghost" onClick={() => setModal({ doc: d })}>Mark not verified</Button>
                    </span>
                  </div>
                ))}
              </div>
              {u.checklist?.length > 0 && (
                <div className="mt-16"><h4>Checklist</h4>
                  <DataTable rows={u.checklist.map((c) => ({ ...c, id: c.documentType }))} columns={[
                    { key: 'documentType', title: 'Document', render: (c) => <>{label(c.documentType)}{c.mandatory && <span className="req"> *</span>}</> },
                    { key: 'uploaded', title: 'Uploaded', render: (c) => (c.uploaded ? 'Yes' : 'No') }, { key: 'verified', title: 'Verified', render: (c) => (c.verified ? 'Yes' : 'No') }
                  ]} />
                </div>
              )}
            </Card>
          )}
        </div>
        <div className="col gap-16">
          <Card title="Privileges" actions={<Button size="sm" loading={busy} onClick={() => run(() => api.put(`/admin/users/${id}/permissions`, { permissions: perms }), 'Privileges updated').then(reload)}>Save</Button>}>
              <PermissionPicker role={u.role} value={perms} onChange={setPerms} />
          </Card>
          <Card title="Verification history"><History items={u.history} /></Card>
        </div>
      </div>
      {modal === 'approve' && (
        <ActionModal title="Approve application" remarks onConfirm={(remarks) => post('approve', { remarks, permissions: perms })} onClose={() => setModal(null)}>
          <p className="small">The account becomes active with the privileges selected in the Privileges panel ({perms.length} selected).</p>
        </ActionModal>
      )}
      {['reject', 'request-info', 'suspend'].includes(modal) && <ActionModal title={label(modal)} variant={modal === 'request-info' ? 'primary' : 'danger'} remarks remarksRequired onConfirm={(remarks) => post(modal, { remarks })} onClose={() => setModal(null)} />}
      {modal === 'activate' && <ActionModal title="Activate account" remarks onConfirm={(remarks) => post('activate', { remarks })} onClose={() => setModal(null)} />}
      {modal === 'reset' && <ActionModal title="Issue temporary password" description="The user's current password stops working immediately." onConfirm={() => api.post(`/admin/users/${id}/reset-password`).then((r) => setTemp(r.data))} onClose={() => setModal(null)} />}
      {modal?.doc && <ActionModal title="Mark document not verified" remarks remarksRequired onConfirm={(remarks) => api.put(`/admin/users/${id}/documents/${modal.doc.id}/verify`, { verified: false, remarks }).then(reload)} onClose={() => setModal(null)} />}
      {temp && <TempPassword result={temp} onClose={() => setTemp(null)} />}
    </>
  );
}

function SellerProfileView({ p }) {
  return (
    <Card title="Business profile">
      <KeyValue items={[['Organisation', p.organisationName], ['Constitution', label(p.constitution)], ['Provider type', label(p.providerType)], ['PAN', p.pan], ['GSTIN', p.gstin], ['Udyam', p.udyamNumber],
        ['Address', [p.addressLine, p.city, p.district, p.state, p.pincode].filter(Boolean).join(', ')], ['Experience', `${p.yearsExperience ?? '-'} years`], ['Team size', p.teamSize],
        ['Categories', p.categoryNames?.join(', ')], ['Expertise', p.expertise?.join(', ')], ['Industries', p.industries?.join(', ')], ['Coverage', p.geographicCoverage?.join(', ')],
        ['Delivery', p.deliveryModes?.map(label).join(', ')], ['Website', p.website], ['Rating', <Stars value={p.averageRating} count={p.ratingCount} />],
        ['Profile completion', `${p.profileCompletion}%`], ['Verified', date(p.verifiedAt)], ['Re-verification due', date(p.reverificationDue)]]} />
      {p.introduction && <p className="pre small mt-16">{p.introduction}</p>}
    </Card>
  );
}

function BuyerProfileView({ p }) {
  return (
    <Card title="Organisation profile">
      <KeyValue items={[['Organisation', p.organisationName], ['Constitution', label(p.constitution)], ['Purchaser type', PURCHASER_LABELS[p.purchaserType]], ['Industry', p.industrySector],
        ['Business activity', p.businessActivity], ['PAN', p.pan], ['GSTIN', p.gstin], ['Udyam', p.udyamNumber], ['Address', [p.addressLine, p.city, p.district, p.state, p.pincode].filter(Boolean).join(', ')], ['Website', p.website]]} />
    </Card>
  );
}

/* ---------- Onboarding on behalf ---------- */
export function OnboardUser({ role }) {
  const navigate = useNavigate();
  const categories = useCategories();
  const seller = role === 'SELLER';
  const form = useForm({ fullName: '', email: '', mobile: '', organisationName: '', constitution: '', pan: '', gstin: '', udyamNumber: '', addressLine: '', city: '', state: '', district: '', pincode: '', website: '',
    ...(seller ? { providerType: '', categoryIds: [], expertise: [], industries: [], yearsExperience: '', teamSize: '', geographicCoverage: [], deliveryModes: [], introduction: '' } : { purchaserType: '', designation: '', industrySector: '', businessActivity: '' }), declarationAccepted: true });
  const [run, busy] = useAction();
  const [result, setResult] = useState(null);
  const submit = async () => {
    form.setErrors({});
    const body = payload({ ...form.values, ...(seller ? { categoryIds: form.values.categoryIds.map(Number) } : {}) }, ['yearsExperience', 'teamSize']);
    try { const r = await run(() => api.post(seller ? '/admin/users/sellers' : '/admin/users/buyers', body), 'Account created'); setResult(r.data); } catch (e) { form.setErrors(fieldErrors(e)); }
  };
  const catLabels = Object.fromEntries(categories.map((c) => [String(c.id), c.name]));
  return (
    <>
      <PageTitle back title={seller ? 'Onboard a seller' : 'Onboard a purchaser'} subtitle="Create the account on the user's behalf; a temporary password is generated." />
      <div className="col gap-16">
        <Card title="Contact">
          <div className="grid grid-2">
            <F form={form} name="fullName" label="Full name" required />
            <F form={form} name="email" label="Email" type="email" required />
            <F form={form} name="mobile" label="Mobile" required />
            {!seller && <F form={form} name="designation" label="Designation" />}
          </div>
        </Card>
        <Card title="Organisation">
          <div className="grid grid-2">
            <F form={form} name="organisationName" label="Organisation name" required />
            <F form={form} name="constitution" label="Constitution" type="select" options={CONSTITUTIONS} labels={CONSTITUTION_LABELS} required={seller} />
            {seller ? <F form={form} name="providerType" label="Provider type" type="select" options={PROVIDER_TYPES} required />
              : <F form={form} name="purchaserType" label="Purchaser type" type="select" options={PURCHASER_TYPES} labels={PURCHASER_LABELS} required />}
            {!seller && <F form={form} name="industrySector" label="Industry sector" type="select" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} required />}
            <F form={form} name="pan" label="PAN" required={seller} />
            <F form={form} name="gstin" label="GSTIN" />
            <F form={form} name="udyamNumber" label="Udyam number" />
            <F form={form} name="website" label="Website" />
            <F form={form} name="addressLine" label="Address" required className="span-2" />
            <F form={form} name="city" label="City" />
            <F form={form} name="state" label="State" type="select" options={STATES} labels={asLabels(STATES)} required />
            <F form={form} name="district" label="District" required />
            <F form={form} name="pincode" label="PIN code" />
            {!seller && <F form={form} name="businessActivity" label="Business activity" className="span-2" />}
          </div>
        </Card>
        {seller && (
          <Card title="Expertise">
            <div className="grid grid-2">
              <F form={form} name="yearsExperience" label="Years of experience" type="number" required />
              <F form={form} name="teamSize" label="Team size" type="number" />
              <F form={form} name="categoryIds" label="Categories" type="chips" options={categories.map((c) => String(c.id))} labels={catLabels} className="span-2" />
              <F form={form} name="deliveryModes" label="Delivery modes" type="chips" options={DELIVERY_MODES} className="span-2" />
              <F form={form} name="expertise" label="Expertise" type="tags" className="span-2" />
              <F form={form} name="industries" label="Industries" type="chips" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} className="span-2" />
              <F form={form} name="introduction" label="Introduction" type="textarea" className="span-2" />
            </div>
          </Card>
        )}
        <div className="row" style={{ justifyContent: 'flex-end' }}><Button icon="person_add" loading={busy} onClick={submit}>Create account</Button></div>
      </div>
      {result && <TempPassword result={result} onClose={() => navigate(seller ? `/admin/sellers/${result.userId}` : `/admin/buyers/${result.userId}`)} />}
    </>
  );
}

