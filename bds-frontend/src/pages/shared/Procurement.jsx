import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { DataTable, Docs, F, History, payload, useForm } from '../../components/kit';
import { Button, Card, Flow, Icon, KeyValue, Modal, Progress, Stars, Status, useAction } from '../../components/ui';
import { date, dateTime, DELIVERY_MODES, inr, INDUSTRIES, label, plusDays, STATES } from '../../constants';
import { useCategories } from '../public/Services';

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));

export const RFQ_STATUSES = ['DRAFT', 'PUBLISHED', 'RESPONSES_RECEIVED', 'UNDER_EVALUATION', 'PROVIDER_SELECTED', 'CLOSED', 'CANCELLED', 'EXPIRED', 'NO_RESPONSE'];
export const QUOTATION_STATUSES = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CLARIFICATION_REQUESTED', 'REVISED', 'SHORTLISTED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED'];
export const ENGAGEMENT_STATUSES = ['SELECTED', 'ENGAGEMENT_INITIATED', 'IN_PROGRESS', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'DISPUTED'];
export const WORK_ORDER_STATUSES = ['REQUESTED', 'ISSUED', 'ACKNOWLEDGED', 'DECLINED'];

const budget = (r) => (r.budgetMin || r.budgetMax ? `${inr(r.budgetMin)} – ${inr(r.budgetMax)}` : 'Not specified');

export const rfqColumns = (extra = []) => [
  { key: 'rfqNumber', title: 'RFQ', render: (r) => <><div className="bold small">{r.title}</div><div className="tiny muted mono">{r.rfqNumber}</div></> },
  { key: 'categoryName', title: 'Category', render: (r) => <span className="small">{r.categoryName}</span> },
  ...extra,
  { key: 'submissionDeadline', title: 'Deadline', render: (r) => <span className={`small ${r.deadlinePassed ? 'muted' : ''}`}>{dateTime(r.submissionDeadline)}</span> },
  { key: 'status', title: 'Status', render: (r) => <Status value={r.status} /> }
];

export const quotationColumns = (extra = []) => [
  { key: 'quotationNumber', title: 'Quotation', render: (q) => <><div className="bold small">{q.rfqTitle}</div><div className="tiny muted mono">{q.quotationNumber} · v{q.versionNo}</div></> },
  ...extra,
  { key: 'totalAmount', title: 'Total', className: 'right', render: (q) => <span className="bold">{inr(q.totalAmount)}</span> },
  { key: 'timelineDays', title: 'Timeline', render: (q) => `${q.timelineDays} days` },
  { key: 'status', title: 'Status', render: (q) => <div className="row gap-4"><Status value={q.status} />{q.shortlisted && <Icon name="bookmark" size="sm" />}</div> }
];

export const engagementColumns = (extra = []) => [
  { key: 'engagementNumber', title: 'Engagement', render: (e) => <><div className="bold small">{e.title}</div><div className="tiny muted mono">{e.engagementNumber}</div></> },
  ...extra,
  { key: 'contractValue', title: 'Value', className: 'right', render: (e) => inr(e.contractValue || e.quotedValue) },
  { key: 'progressPercent', title: 'Progress', render: (e) => <div style={{ width: 100 }}><Progress value={e.progressPercent} /><span className="tiny muted">{e.progressPercent || 0}%</span></div> },
  { key: 'status', title: 'Status', render: (e) => <Status value={e.status} /> }
];

export const workOrderColumns = (extra = []) => [
  { key: 'workOrderNumber', title: 'Work order', render: (w) => <><div className="bold small">{w.engagementTitle}</div><div className="tiny muted mono">{w.workOrderNumber || 'Requested'} · {w.engagementNumber}</div></> },
  ...extra,
  { key: 'poReference', title: 'PO ref', render: (w) => w.poReference || '-' },
  { key: 'agreedValue', title: 'Value', className: 'right', render: (w) => inr(w.agreedValue) },
  { key: 'status', title: 'Status', render: (w) => <Status value={w.status} /> }
];

export function RfqInfo({ rfq, docs }) {
  const s = rfq.summary;
  const section = (title, text) => text && <div className="mt-16"><h4>{title}</h4><p className="pre small mt-4">{text}</p></div>;
  return (
    <Card>
      <Flow steps={[{ value: 'DRAFT', label: 'Draft' }, { value: 'PUBLISHED', label: 'Published' }, { value: 'RESPONSES_RECEIVED', label: 'Responses' }, { value: 'UNDER_EVALUATION', label: 'Evaluation' }, { value: 'PROVIDER_SELECTED', label: 'Selected' }]}
        current={s.status} failed={['CANCELLED', 'EXPIRED', 'NO_RESPONSE', 'CLOSED'].includes(s.status) ? s.status : null} />
      <div className="mt-16">
        <KeyValue items={[
          ['Reference', <span className="mono">{s.rfqNumber}</span>], ['Category', `${s.categoryName}${s.subcategoryName ? ` · ${s.subcategoryName}` : ''}`],
          ['Purchaser', s.buyerName], ['Location', [s.projectDistrict, s.projectState].filter(Boolean).join(', ')], ['Delivery mode', label(s.deliveryMode)],
          ['Budget', budget(s)], ['Expected duration', rfq.expectedDuration], ['Required experience', rfq.requiredExperience != null ? `${rfq.requiredExperience}+ years` : null],
          ['Expected start', date(rfq.expectedStartDate)], ['Submission deadline', dateTime(s.submissionDeadline)], ['Distribution', label(s.distributionMode)],
          ['Industry', rfq.industry], ['Invited / quotations', `${s.invitedCount} / ${s.quotationCount}`],
          rfq.sourceListingTitle && ['Source listing', rfq.sourceListingTitle], rfq.statusRemarks && ['Remarks', rfq.statusRemarks]
        ]} />
      </div>
      {section('Description', rfq.description)}
      {section('Scope of work', rfq.scopeOfWork)}
      {section('Deliverables', rfq.deliverables)}
      <div className="mt-16"><h4>Attachments</h4><div className="mt-8">{docs || <Docs documents={rfq.attachments} />}</div></div>
    </Card>
  );
}

export function Invitations({ invitations = [] }) {
  return (
    <DataTable rows={invitations.map((i) => ({ ...i, id: i.sellerId }))} empty="No providers invited yet" columns={[
      { key: 'sellerName', title: 'Provider', render: (i) => <><div className="small bold">{i.sellerName}</div>{i.sellerRating ? <Stars value={i.sellerRating} /> : null}</> },
      { key: 'status', title: 'Status', render: (i) => <><Status value={i.status} />{i.declineReason && <div className="tiny muted">{i.declineReason}</div>}</> },
      { key: 'invitedAt', title: 'Invited', render: (i) => <span className="small">{dateTime(i.invitedAt)}{i.invitedBy ? ` · ${label(i.invitedBy)}` : ''}</span> }
    ]} />
  );
}

export function QuotationInfo({ q, docs }) {
  return (
    <Card>
      <KeyValue items={[
        ['Quotation', <span className="mono">{q.quotationNumber} · version {q.versionNo}</span>], ['RFQ', `${q.rfqTitle} (${q.rfqNumber})`],
        ['Provider', <>{q.sellerName} {q.sellerRating ? <Stars value={q.sellerRating} count={q.sellerRatingCount} /> : null}</>], ['Purchaser', q.buyerName],
        ['Status', <Status value={q.status} />], ['Scope accepted', q.scopeAccepted ? 'Yes' : 'With deviations'], ['Timeline', `${q.timelineDays} days`],
        ['Valid until', date(q.validUntil)], ['Submitted', dateTime(q.submittedAt)], q.decisionRemarks && ['Decision remarks', q.decisionRemarks]
      ]} />
      {[['Technical proposal', q.technicalProposal], ['Methodology', q.methodology], ['Scope deviations', q.scopeDeviations], ['Deliverables', q.deliverables],
        ['Team composition', q.teamComposition], ['Relevant experience', q.relevantExperience], ['Payment milestones', q.paymentMilestones]].map(([t, v]) => v && (
        <div key={t} className="mt-16"><h4>{t}</h4><p className="pre small mt-4">{v}</p></div>
      ))}
      <div className="mt-16">
        <h4>Financial proposal</h4>
        {q.items?.length > 0 && (
          <DataTable rows={q.items} columns={[
            { key: 'description', title: 'Item' }, { key: 'quantity', title: 'Qty', render: (i) => `${i.quantity} ${i.unit || ''}` },
            { key: 'unitPrice', title: 'Unit price', className: 'right', render: (i) => inr(i.unitPrice) }, { key: 'amount', title: 'Amount', className: 'right', render: (i) => inr(i.amount) }
          ]} />
        )}
        <div className="mt-8" style={{ maxWidth: 360, marginLeft: 'auto' }}>
          <KeyValue items={[['Professional fee', inr(q.professionalFee)], [`Tax (${q.taxPercent || 0}%)`, inr(q.taxAmount)], ['Other expenses', inr(q.otherExpenses)], ['Total', <b>{inr(q.totalAmount)}</b>]]} />
        </div>
      </div>
      <div className="mt-16"><h4>Attachments</h4><div className="mt-8">{docs || <Docs documents={q.attachments} />}</div></div>
    </Card>
  );
}

export function Versions({ url }) {
  const [versions, setVersions] = useState(null);
  useEffect(() => { api.get(url).then(setVersions).catch(() => setVersions([])); }, [url]);
  if (!versions) return null;
  return (
    <Card title="Version history">
      <DataTable rows={versions} empty="No revisions" columns={[
        { key: 'versionNo', title: 'Version', render: (v) => `v${v.versionNo}` }, { key: 'totalAmount', title: 'Total', render: (v) => inr(v.totalAmount) },
        { key: 'changeSummary', title: 'Changes', render: (v) => <span className="small">{v.changeSummary || '-'}</span> }, { key: 'createdAt', title: 'Date', render: (v) => <span className="small">{dateTime(v.createdAt)}</span> }
      ]} />
    </Card>
  );
}

export function EngagementInfo({ e, docs, quotationLink }) {
  const s = e.summary;
  return (
    <div className="col gap-16">
      <Card>
        <Flow steps={[{ value: 'SELECTED', label: 'Selected' }, { value: 'ENGAGEMENT_INITIATED', label: 'Work order' }, { value: 'IN_PROGRESS', label: 'In progress' }, { value: 'DELIVERED', label: 'Delivered' }, { value: 'COMPLETED', label: 'Completed', done: true }]}
          current={s.status} failed={['CANCELLED', 'DISPUTED'].includes(s.status) ? s.status : null} />
        <div className="mt-16">
          <KeyValue items={[
            ['Reference', <span className="mono">{s.engagementNumber}</span>], ['RFQ', s.rfqNumber], ['Quotation', quotationLink ? <Link to={quotationLink}>{s.quotationNumber}</Link> : s.quotationNumber],
            ['Purchaser', s.buyerName], ['Provider', s.sellerName], ['Quoted value', inr(s.quotedValue)], ['Contract value', inr(s.contractValue)],
            ['Start', date(s.startDate)], ['Expected end', date(s.expectedEndDate)], ['Work order', label(s.workOrderStatus)],
            ['Progress', <div style={{ width: 160 }}><Progress value={s.progressPercent} /><span className="tiny">{s.progressPercent || 0}%</span></div>],
            ['Selection remarks', e.selectionRemarks], e.deliveredAt && ['Delivered', `${dateTime(e.deliveredAt)} — ${e.deliveryNote || ''}`],
            e.deliveryRemarks && ['Delivery remarks', e.deliveryRemarks], e.completedAt && ['Completed', dateTime(e.completedAt)], e.cancelReason && ['Cancellation', e.cancelReason]
          ]} />
        </div>
        {e.disclaimer && <p className="tiny muted mt-16">{e.disclaimer}</p>}
      </Card>
      {e.workOrders?.length > 0 && <Card title="Work orders">{e.workOrders.map((w) => <WorkOrderInfo key={w.id} w={w} />)}</Card>}
      <Card title="Progress updates"><History items={e.updates} /></Card>
      <Card title="Documents & deliverables">{docs || <Docs documents={e.documents} />}</Card>
      {e.review && (
        <Card title="Feedback">
          <Stars value={e.review.rating} />
          {e.review.title && <div className="bold mt-8">{e.review.title}</div>}
          {e.review.reviewText && <p className="small mt-4">{e.review.reviewText}</p>}
          {e.review.answers?.map((a) => <div key={a.questionId} className="small mt-4"><span className="muted">{a.questionText}:</span> {a.ratingValue ?? (a.booleanValue != null ? (a.booleanValue ? 'Yes' : 'No') : a.textValue)}</div>)}
        </Card>
      )}
    </div>
  );
}

export function WorkOrderInfo({ w, actions }) {
  return (
    <div className="card flat mb-8" style={{ padding: 14 }}>
      <div className="row between"><span className="bold small mono">{w.workOrderNumber || 'Work order request'}</span><Status value={w.status} /></div>
      <div className="mt-8">
        <KeyValue items={[
          w.requestRemarks && ['Request', `${w.requestRemarks} (${dateTime(w.requestedAt)})`], ['PO reference', w.poReference], ['Order date', date(w.workOrderDate)],
          ['Agreed value', inr(w.agreedValue)], ['Period', `${date(w.startDate)} – ${date(w.endDate)}`], ['Scope', w.scopeSummary], ['Terms', w.terms],
          ['Purchaser remarks', w.buyerRemarks], ['Issued', dateTime(w.issuedAt)], ['Acknowledged', dateTime(w.acknowledgedAt)], w.declinedReason && ['Declined', w.declinedReason]
        ]} />
      </div>
      {w.documents?.length > 0 && <div className="mt-8"><Docs documents={w.documents} /></div>}
      {actions && <div className="row gap-8 mt-8">{actions}</div>}
    </div>
  );
}

/* ---------- Forms ---------- */
export function RfqForm({ initial, onSubmit, busy, sellers = [] }) {
  const categories = useCategories();
  const form = useForm({
    title: '', categoryId: '', subcategoryId: '', description: '', scopeOfWork: '', deliverables: '', industry: '', projectState: '', projectDistrict: '',
    deliveryMode: '', budgetMin: '', budgetMax: '', expectedDuration: '', requiredExperience: '', expectedStartDate: '', submissionDeadline: `${plusDays(14)}T18:00`,
    distributionMode: sellers.length ? 'SELECTED' : 'RECOMMENDED', sellerIds: sellers.map((s) => String(s.id)), sourceListingId: '', ...initial
  });
  const [recommended, setRecommended] = useState([]);
  const v = form.values;
  const category = categories.find((c) => String(c.id) === String(v.categoryId));
  useEffect(() => {
    if (!v.categoryId) return;
    api.get('/buyer/recommended-sellers', { categoryId: v.categoryId, subcategoryId: v.subcategoryId || undefined }).then(setRecommended).catch(() => setRecommended([]));
  }, [v.categoryId, v.subcategoryId]);
  const known = [...sellers.map((s) => ({ sellerId: s.id, name: s.name })), ...recommended.filter((r) => !sellers.some((s) => s.id === r.sellerId))];
  const submit = (publish) => onSubmit({
    ...payload({ ...v, sellerIds: v.sellerIds.map(Number), submissionDeadline: v.submissionDeadline.length === 16 ? `${v.submissionDeadline}:00` : v.submissionDeadline },
      ['categoryId', 'subcategoryId', 'budgetMin', 'budgetMax', 'requiredExperience', 'sourceListingId']), publish
  }, form);
  return (
    <div className="col gap-16">
      <Card title="Requirement">
        <div className="grid grid-2">
          <F form={form} name="title" label="Title" required className="span-2" />
          <F form={form} name="categoryId" label="Category" type="select" required options={categories.map((c) => ({ value: String(c.id), label: c.name }))} />
          <F form={form} name="subcategoryId" label="Subcategory" type="select" options={(category?.subcategories || []).map((s) => ({ value: String(s.id), label: s.name }))} />
          <F form={form} name="description" label="Description" type="textarea" required rows={4} className="span-2" />
          <F form={form} name="scopeOfWork" label="Scope of work" type="textarea" rows={3} className="span-2" />
          <F form={form} name="deliverables" label="Expected deliverables" type="textarea" rows={3} className="span-2" />
        </div>
      </Card>
      <Card title="Project details">
        <div className="grid grid-2">
          <F form={form} name="industry" label="Industry" type="select" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} />
          <F form={form} name="deliveryMode" label="Delivery mode" type="select" options={DELIVERY_MODES} />
          <F form={form} name="projectState" label="Project state" type="select" options={STATES} labels={asLabels(STATES)} />
          <F form={form} name="projectDistrict" label="Project district" />
          <F form={form} name="budgetMin" label="Budget from (₹)" type="number" min="0" />
          <F form={form} name="budgetMax" label="Budget up to (₹)" type="number" min="0" />
          <F form={form} name="expectedDuration" label="Expected duration" placeholder="e.g. 6 weeks" />
          <F form={form} name="requiredExperience" label="Required experience (years)" type="number" min="0" />
          <F form={form} name="expectedStartDate" label="Expected start" type="date" />
          <F form={form} name="submissionDeadline" label="Quotation deadline" type="datetime-local" required />
        </div>
      </Card>
      <Card title="Send to providers">
        <div className="col gap-16">
          <F form={form} name="distributionMode" label="Distribution" type="select" options={['SELECTED', 'RECOMMENDED', 'ADMIN_ASSISTED']}
            labels={{ SELECTED: 'Providers I select', RECOMMENDED: 'Platform-recommended providers', ADMIN_ASSISTED: 'Administrator-assisted matching' }} />
          {v.distributionMode === 'SELECTED' && (
            known.length === 0 ? <p className="small muted">Choose a category to see recommended verified providers.</p> : (
              <F form={form} name="sellerIds" label="Providers" type="chips" options={known.map((k) => String(k.sellerId))} labels={Object.fromEntries(known.map((k) => [String(k.sellerId), k.name]))} />
            )
          )}
          {v.distributionMode === 'RECOMMENDED' && <p className="small muted">{recommended.length} recommended provider(s) for this category will be invited when published.</p>}
        </div>
      </Card>
      <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
        <Button variant="outline" loading={busy} onClick={() => submit(false)}>Save draft</Button>
        <Button icon="send" loading={busy} onClick={() => submit(true)}>Publish RFQ</Button>
      </div>
    </div>
  );
}

export function QuotationForm({ initial, onSubmit, busy, revision }) {
  const form = useForm({
    technicalProposal: '', methodology: '', scopeAccepted: true, scopeDeviations: '', deliverables: '', timelineDays: '', teamComposition: '', relevantExperience: '',
    professionalFee: '', taxPercent: '18', otherExpenses: '', paymentMilestones: '', validUntil: plusDays(45), changeSummary: '', ...initial
  });
  const [items, setItems] = useState(initial?.items?.map(({ description, quantity, unit, unitPrice }) => ({ description, quantity, unit, unitPrice })) || []);
  const v = form.values;
  const itemsTotal = items.reduce((t, i) => t + Number(i.quantity || 0) * Number(i.unitPrice || 0), 0);
  const fee = Number(v.professionalFee || 0) || itemsTotal;
  const total = fee + (fee * Number(v.taxPercent || 0)) / 100 + Number(v.otherExpenses || 0);
  const submit = (submitNow) => onSubmit({
    ...payload(v, ['timelineDays', 'professionalFee', 'taxPercent', 'otherExpenses']),
    items: items.filter((i) => i.description).map((i) => ({ ...i, quantity: Number(i.quantity), unitPrice: Number(i.unitPrice) })), submit: submitNow
  }, form);
  const setItem = (idx, k, val) => setItems(items.map((it, i) => (i === idx ? { ...it, [k]: val } : it)));
  return (
    <div className="col gap-16">
      <Card title="Technical proposal">
        <div className="grid grid-2">
          <F form={form} name="technicalProposal" label="Technical proposal" type="textarea" required rows={5} className="span-2" />
          <F form={form} name="methodology" label="Methodology" type="textarea" rows={3} className="span-2" />
          <F form={form} name="deliverables" label="Deliverables" type="textarea" required rows={3} className="span-2" />
          <F form={form} name="scopeAccepted" label="I accept the scope as specified in the RFQ" type="checkbox" />
          <div />
          {!v.scopeAccepted && <F form={form} name="scopeDeviations" label="Scope deviations" type="textarea" rows={2} className="span-2" />}
          <F form={form} name="timelineDays" label="Timeline (days)" type="number" min="1" required />
          <F form={form} name="validUntil" label="Valid until" type="date" required />
          <F form={form} name="teamComposition" label="Team composition" type="textarea" rows={2} />
          <F form={form} name="relevantExperience" label="Relevant experience" type="textarea" rows={2} />
        </div>
      </Card>
      <Card title="Financial proposal" actions={<Button variant="ghost" size="sm" icon="add" onClick={() => setItems([...items, { description: '', quantity: 1, unit: '', unitPrice: '' }])}>Add line item</Button>}>
        {items.length > 0 && (
          <div className="col gap-8 mb-16">
            {items.map((it, i) => (
              <div key={i} className="row gap-8">
                <input className="input grow" placeholder="Description" value={it.description} onChange={(e) => setItem(i, 'description', e.target.value)} />
                <input className="input" style={{ width: 80 }} type="number" placeholder="Qty" value={it.quantity} onChange={(e) => setItem(i, 'quantity', e.target.value)} />
                <input className="input" style={{ width: 90 }} placeholder="Unit" value={it.unit} onChange={(e) => setItem(i, 'unit', e.target.value)} />
                <input className="input" style={{ width: 130 }} type="number" placeholder="Unit price" value={it.unitPrice} onChange={(e) => setItem(i, 'unitPrice', e.target.value)} />
                <Button variant="ghost" size="sm" icon="delete" aria-label="Remove" onClick={() => setItems(items.filter((_, x) => x !== i))} />
              </div>
            ))}
            <div className="small muted right">Line items total: {inr(itemsTotal)}</div>
          </div>
        )}
        <div className="grid grid-3">
          <F form={form} name="professionalFee" label="Professional fee (₹)" type="number" min="0" hint={items.length ? 'Leave blank to use line items total' : undefined} />
          <F form={form} name="taxPercent" label="Tax %" type="number" min="0" max="28" />
          <F form={form} name="otherExpenses" label="Other expenses (₹)" type="number" min="0" />
        </div>
        <F form={form} name="paymentMilestones" label="Payment milestones" type="textarea" rows={2} />
        <div className="right bold mt-8">Estimated total: {inr(total)}</div>
      </Card>
      {revision && <Card><F form={form} name="changeSummary" label="Summary of changes in this revision" type="textarea" rows={2} /></Card>}
      <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
        {!revision && <Button variant="outline" loading={busy} onClick={() => submit(false)}>Save draft</Button>}
        <Button icon="send" loading={busy} onClick={() => submit(true)}>{revision ? 'Submit revision' : 'Submit quotation'}</Button>
      </div>
    </div>
  );
}

export function WorkOrderModal({ title = 'Issue work order', initial, onSubmit, onClose }) {
  const form = useForm({ poReference: '', workOrderDate: new Date().toISOString().slice(0, 10), agreedValue: '', startDate: new Date().toISOString().slice(0, 10), endDate: '', scopeSummary: '', terms: '', remarks: '', ...initial });
  const [run, busy] = useAction();
  return (
    <Modal title={title} onClose={onClose} wide footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button>
      <Button loading={busy} disabled={!form.values.poReference || !form.values.startDate} onClick={() => run(() => onSubmit(payload(form.values, ['agreedValue'])), 'Work order issued').then(onClose)}>Issue</Button></>}>
      <div className="grid grid-2">
        <F form={form} name="poReference" label="PO / work order reference" required />
        <F form={form} name="workOrderDate" label="Order date" type="date" />
        <F form={form} name="agreedValue" label="Agreed value (₹)" type="number" min="0" />
        <div />
        <F form={form} name="startDate" label="Start date" type="date" required />
        <F form={form} name="endDate" label="End date" type="date" />
        <F form={form} name="scopeSummary" label="Scope summary" type="textarea" rows={2} className="span-2" />
        <F form={form} name="terms" label="Terms" type="textarea" rows={2} className="span-2" />
        <F form={form} name="remarks" label="Remarks" className="span-2" />
      </div>
    </Modal>
  );
}
