import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { dateTime, label } from '../constants';
import { Button, Checkbox, ChipSelect, DocumentList, Empty, Field, FilePicker, Input, Loading, Pagination, Select, TagInput, Textarea, useAction, useToast } from './ui';

/* Paged list bound to an endpoint; filters are passed as query params. */
export function usePaged(url, filters = {}, size = 10) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const key = JSON.stringify(filters);
  const toast = useToast();
  const reload = useCallback(() => {
    setLoading(true);
    return api.get(url, { ...filters, page, size })
      .then(setData)
      .catch((e) => { setData(null); if (e?.response?.status !== 404) toast.error(e?.response?.data?.message || 'Could not load data'); })
      .finally(() => setLoading(false));
  }, [url, key, page, size]);
  useEffect(() => { reload(); }, [reload]);
  useEffect(() => { setPage(0); }, [key]);
  return { data, loading, reload, setPage };
}

/* Simple table: columns = [{ key, title, render?, className? }] */
export function DataTable({ columns, rows, onRow, empty = 'No records found', emptyIcon }) {
  if (!rows?.length) return <Empty icon={emptyIcon} title={empty} />;
  return (
    <div className="table-wrap">
      <table className="table">
        <thead><tr>{columns.map((c) => <th key={c.key || c.title} className={c.className}>{c.title}</th>)}</tr></thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.id ?? i} className={onRow ? 'clickable' : ''} onClick={onRow ? () => onRow(r) : undefined}>
              {columns.map((c) => <td key={c.key || c.title} className={c.className}>{c.render ? c.render(r) : r[c.key] ?? '-'}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function PagedTable({ paged, columns, onRow, empty, emptyIcon }) {
  if (paged.loading && !paged.data) return <Loading />;
  return (
    <>
      <DataTable columns={columns} rows={paged.data?.content} onRow={onRow} empty={empty} emptyIcon={emptyIcon} />
      <Pagination page={paged.data} onPage={paged.setPage} />
    </>
  );
}

/* Filter bar: [{ name, placeholder, options?, labels? }] */
export function Filters({ value, onChange, fields, search = true }) {
  const [q, setQ] = useState(value.q || '');
  const set = (k, v) => onChange({ ...value, [k]: v || undefined });
  return (
    <div className="row wrap gap-8 mb-16">
      {search && (
        <form className="input-group" style={{ flex: '1 1 260px' }} onSubmit={(e) => { e.preventDefault(); set('q', q); }}>
          <input className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search..." aria-label="Search" />
          <button className="btn btn-outline" type="submit">Search</button>
        </form>
      )}
      {fields.map((f) => (
        <div key={f.name} style={{ width: f.width || 200 }}>
          <Select value={value[f.name] || ''} onChange={(e) => set(f.name, e.target.value)} placeholder={f.placeholder} options={f.options} labels={f.labels} aria-label={f.placeholder} />
        </div>
      ))}
    </div>
  );
}

/* Form state helper. */
export function useForm(initial) {
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState({});
  const set = (name) => (e) => {
    const v = e && e.target ? (e.target.type === 'checkbox' ? e.target.checked : e.target.value) : e;
    setValues((s) => ({ ...s, [name]: v }));
  };
  return { values, setValues, set, errors, setErrors };
}

/* Declarative field: type = text|email|number|date|datetime-local|password|textarea|select|tags|chips|checkbox */
export function F({ form, name, label: text, type = 'text', required, options, labels, hint, className, ...rest }) {
  const value = form.values[name];
  const error = form.errors[name];
  let control;
  if (type === 'textarea') control = <Textarea value={value ?? ''} onChange={form.set(name)} error={error} {...rest} />;
  else if (type === 'select') control = <Select value={value ?? ''} onChange={form.set(name)} options={options} labels={labels} placeholder={rest.placeholder ?? 'Select'} error={error} />;
  else if (type === 'tags') control = <TagInput value={value || []} onChange={form.set(name)} />;
  else if (type === 'chips') control = <ChipSelect options={options} labels={labels} value={value || []} onChange={form.set(name)} />;
  else if (type === 'checkbox') return <Checkbox label={text} checked={!!value} onChange={form.set(name)} />;
  else control = <Input type={type} value={value ?? ''} onChange={form.set(name)} error={error} {...rest} />;
  return <Field label={text} required={required} hint={hint} error={error} className={className}>{control}</Field>;
}

/* Strip empty strings and convert numeric fields. */
export function payload(values, numeric = []) {
  const out = {};
  Object.entries(values).forEach(([k, v]) => {
    if (v === '' || v === undefined) return;
    out[k] = numeric.includes(k) && v !== null ? Number(v) : v;
  });
  return out;
}

export function ExportButtons({ url, params }) {
  const [run, busy] = useAction();
  return (
    <div className="row gap-4">
      {['XLSX', 'PDF', 'CSV'].map((f) => (
        <Button key={f} variant="outline" size="sm" icon="download" loading={busy}
          onClick={() => run(() => api.download(url, { ...params, format: f }, `report.${f.toLowerCase()}`))}>{f}</Button>
      ))}
    </div>
  );
}

/* Documents with upload/download/delete. */
export function Docs({ documents, uploadUrl, category, onChange, canDelete, downloadUrl = (d) => `/documents/${d.id}/download`, deleteUrl = (d) => `/documents/${d.id}` }) {
  const [run, busy] = useAction();
  return (
    <div className="col gap-8">
      <DocumentList documents={documents}
        onDownload={(d) => api.download(downloadUrl(d), null, d.originalName)}
        onDelete={canDelete ? (d) => run(() => api.del(deleteUrl(d)), 'Document removed').then(onChange) : undefined} />
      {uploadUrl && (
        <div>
          <FilePicker label="Upload document" busy={busy}
            onFile={(file) => run(() => api.upload(`${uploadUrl}${category ? `${uploadUrl.includes('?') ? '&' : '?'}category=${encodeURIComponent(category)}` : ''}`, { file }), 'Document uploaded').then(onChange)} />
        </div>
      )}
    </div>
  );
}

export function History({ items = [] }) {
  if (!items.length) return <p className="small muted">No history yet.</p>;
  return (
    <div className="timeline">
      {items.map((h) => (
        <div key={h.id} className="timeline-item">
          <span className="timeline-dot" />
          <div>
            <div className="small bold">{label(h.action || h.updateType)}{h.toStatus ? ` → ${label(h.toStatus)}` : ''}{h.progressPercent != null ? ` · ${h.progressPercent}%` : ''}</div>
            {(h.remarks || h.message) && <div className="small">{h.remarks || h.message}</div>}
            <div className="tiny muted">{h.actorName || h.authorName}{(h.actorRole || h.authorRole) ? ` (${label(h.actorRole || h.authorRole)})` : ''} · {dateTime(h.createdAt)}</div>
          </div>
        </div>
      ))}
    </div>
  );
}

/* Remarks/confirm action wrapper used with ActionModal state: [modal, open] */
export function useModal() {
  const [modal, setModal] = useState(null);
  return [modal, setModal, () => setModal(null)];
}
