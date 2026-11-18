import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../api/client';
import { label, STATUS_ICON, STATUS_TONE } from '../constants';

export function Icon({ name, size, fill, className = '' }) {
  return <span className={`ms ${size || ''} ${fill ? 'fill' : ''} ${className}`} aria-hidden="true">{name}</span>;
}

export function Button({ variant = 'primary', size, icon, loading, children, className = '', block, ...props }) {
  return (
    <button className={`btn btn-${variant} ${size ? `btn-${size}` : ''} ${block ? 'btn-block' : ''} ${className}`} disabled={loading || props.disabled} {...props}>
      {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : icon && <Icon name={icon} size="sm" />}
      {children}
    </button>
  );
}

export function Card({ title, actions, children, footer, className = '', bodyClass = '', flat }) {
  return (
    <div className={`card ${flat ? 'flat' : ''} ${className}`}>
      {(title || actions) && (
        <div className="card-header">
          {typeof title === 'string' ? <h3>{title}</h3> : title}
          {actions && <div className="row gap-8">{actions}</div>}
        </div>
      )}
      <div className={`card-body ${bodyClass}`}>{children}</div>
      {footer && <div className="card-footer">{footer}</div>}
    </div>
  );
}

export function Status({ value, text }) {
  const tone = STATUS_TONE[value] ?? '';
  return (
    <span className={`badge ${tone}`}>
      <Icon name={STATUS_ICON[tone]} size="sm" />
      {text || label(value)}
    </span>
  );
}

export function Badge({ tone = '', icon, children }) {
  return <span className={`badge ${tone}`}>{icon && <Icon name={icon} size="sm" />}{children}</span>;
}

export function Verified() {
  return <span className="verified"><Icon name="verified" size="sm" fill />BDS Verified</span>;
}

export function Alert({ tone = 'info', icon, title, children, action }) {
  const icons = { info: 'info', warn: 'warning', bad: 'error', good: 'check_circle' };
  return (
    <div className={`alert ${tone}`} role={tone === 'bad' ? 'alert' : 'status'}>
      <Icon name={icon || icons[tone]} />
      <div className="grow">
        {title && <div className="bold">{title}</div>}
        <div>{children}</div>
      </div>
      {action}
    </div>
  );
}

export function Field({ label: text, required, hint, error, children, className = '' }) {
  return (
    <div className={`field ${className}`}>
      {text && <label>{text}{required && <span className="req">*</span>}</label>}
      {children}
      {error ? <span className="err">{error}</span> : hint && <span className="hint">{hint}</span>}
    </div>
  );
}

export function Input({ error, className = '', ...props }) {
  return <input className={`input ${error ? 'invalid' : ''} ${className}`} {...props} />;
}

export function Textarea({ error, className = '', ...props }) {
  return <textarea className={`textarea ${error ? 'invalid' : ''} ${className}`} {...props} />;
}

export function Select({ options = [], placeholder, error, className = '', labels, ...props }) {
  return (
    <select className={`select ${error ? 'invalid' : ''} ${className}`} {...props}>
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {options.map((o) => {
        const value = typeof o === 'object' ? o.value : o;
        const text = typeof o === 'object' ? o.label : labels?.[o] || label(o);
        return <option key={value} value={value}>{text}</option>;
      })}
    </select>
  );
}

export function Checkbox({ label: text, ...props }) {
  return <label className="checkbox"><input type="checkbox" {...props} />{text}</label>;
}

export function ChipSelect({ options, value = [], onChange, labels }) {
  const toggle = (o) => onChange(value.includes(o) ? value.filter((v) => v !== o) : [...value, o]);
  return (
    <div className="chips">
      {options.map((o) => (
        <button type="button" key={o} className={`chip selectable ${value.includes(o) ? 'selected' : ''}`} onClick={() => toggle(o)}>
          {value.includes(o) && <Icon name="check" size="sm" />}{labels?.[o] || label(o)}
        </button>
      ))}
    </div>
  );
}

export function TagInput({ value = [], onChange, placeholder = 'Type and press Enter' }) {
  const [text, setText] = useState('');
  const add = () => {
    const v = text.trim().replace(/,$/, '');
    if (v && !value.includes(v)) onChange([...value, v]);
    setText('');
  };
  return (
    <div className="col gap-8">
      <Input value={text} placeholder={placeholder} onChange={(e) => setText(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); add(); } }} onBlur={add} />
      {value.length > 0 && (
        <div className="chips">
          {value.map((v) => (
            <span className="chip" key={v}>{v}<button type="button" aria-label={`Remove ${v}`} onClick={() => onChange(value.filter((x) => x !== v))}><Icon name="close" size="sm" /></button></span>
          ))}
        </div>
      )}
    </div>
  );
}

export function Loading() {
  return <div className="loading"><div className="spinner" /></div>;
}

export function Empty({ icon = 'inbox', title = 'Nothing here yet', children }) {
  return (
    <div className="empty">
      <Icon name={icon} />
      <div className="bold" style={{ color: 'var(--ink-700)' }}>{title}</div>
      {children && <div className="mt-4">{children}</div>}
    </div>
  );
}

export function Pagination({ page, onPage }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="pagination">
      <span className="muted small">Page {page.page + 1} of {page.totalPages} · {page.totalElements} records</span>
      <div className="row gap-8">
        <Button variant="outline" size="sm" icon="chevron_left" disabled={page.first} onClick={() => onPage(page.page - 1)}>Previous</Button>
        <Button variant="outline" size="sm" disabled={page.last} onClick={() => onPage(page.page + 1)}>Next<Icon name="chevron_right" size="sm" /></Button>
      </div>
    </div>
  );
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((t) => (
        <button key={t.value} role="tab" aria-selected={value === t.value} className={`tab ${value === t.value ? 'active' : ''}`} onClick={() => onChange(t.value)}>
          {t.label}{t.count !== undefined && <span className="count">{t.count}</span>}
        </button>
      ))}
    </div>
  );
}

export function Stars({ value = 0, count }) {
  const rounded = Math.round(value || 0);
  return (
    <span className="row gap-4" title={`${value || 0} out of 5`}>
      <span className="stars">{[1, 2, 3, 4, 5].map((i) => <Icon key={i} name="star" fill className={i <= rounded ? '' : 'off'} />)}</span>
      {value ? <span className="small bold">{Number(value).toFixed(1)}</span> : <span className="small muted">No ratings</span>}
      {count !== undefined && count > 0 && <span className="small muted">({count})</span>}
    </span>
  );
}

export function StarInput({ value, onChange, size }) {
  const [hover, setHover] = useState(0);
  const labels = ['', 'Poor', 'Fair', 'Good', 'Very good', 'Excellent'];
  return (
    <span className="row gap-8">
      <span className={`stars input`} onMouseLeave={() => setHover(0)} role="radiogroup">
        {[1, 2, 3, 4, 5].map((i) => (
          <span key={i} role="radio" aria-checked={value === i} aria-label={`${i} star`} tabIndex={0}
            onMouseEnter={() => setHover(i)} onClick={() => onChange(i)} onKeyDown={(e) => e.key === 'Enter' && onChange(i)}>
            <Icon name="star" fill className={i <= (hover || value) ? '' : 'off'} size={size} />
          </span>
        ))}
      </span>
      <span className="small muted">{labels[hover || value] || 'Select rating'}</span>
    </span>
  );
}

export function StatCard({ icon, value, label: text, tone, onClick, sub }) {
  const tones = { warn: ['#fdf1e3', '#b4610e'], bad: ['#fbe9e6', '#c2412d'], good: ['#e5f5ec', '#17704a'], dark: ['#0b2a42', '#fff'] };
  const [bg, fg] = tones[tone] || [];
  return (
    <div className={`card stat ${onClick ? 'link' : ''}`} onClick={onClick} role={onClick ? 'button' : undefined}>
      <div className="icon" style={bg ? { background: bg, color: fg } : undefined}><Icon name={icon} /></div>
      <div>
        <div className="value">{value ?? '-'}</div>
        <div className="label">{text}</div>
        {sub && <div className="tiny muted mt-4">{sub}</div>}
      </div>
    </div>
  );
}

export function Progress({ value }) {
  return <div className="progress" aria-valuenow={value} role="progressbar"><div style={{ width: `${Math.min(100, value || 0)}%` }} /></div>;
}

export function Modal({ title, onClose, children, footer, wide }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

export function ActionModal({ title, description, confirmText = 'Confirm', variant = 'primary', remarks, remarksRequired, remarksLabel = 'Remarks', onConfirm, onClose, children }) {
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const submit = async () => {
    if (remarksRequired && !text.trim()) {
      toast.error(`${remarksLabel} is required`);
      return;
    }
    setBusy(true);
    try {
      await onConfirm(text.trim());
      onClose();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal title={title} onClose={onClose} footer={<><Button variant="ghost" onClick={onClose}>Cancel</Button><Button variant={variant} loading={busy} onClick={submit}>{confirmText}</Button></>}>
      <div className="col gap-16">
        {description && <p className="muted">{description}</p>}
        {children}
        {remarks && (
          <Field label={remarksLabel} required={remarksRequired}>
            <Textarea value={text} onChange={(e) => setText(e.target.value)} maxLength={1000} autoFocus />
          </Field>
        )}
      </div>
    </Modal>
  );
}

export function FilePicker({ onFile, accept = '.pdf,.jpg,.jpeg,.png,.doc,.docx,.xls,.xlsx', label: text = 'Choose file', busy, variant = 'outline', size = 'sm' }) {
  const ref = useRef(null);
  return (
    <>
      <input ref={ref} type="file" hidden accept={accept} onChange={(e) => { const f = e.target.files?.[0]; if (f) onFile(f); e.target.value = ''; }} />
      <Button variant={variant} size={size} icon="upload_file" loading={busy} onClick={() => ref.current?.click()}>{text}</Button>
    </>
  );
}

export function DocumentList({ documents = [], onDownload, onDelete }) {
  if (!documents.length) return <p className="muted small">No documents uploaded.</p>;
  return (
    <div className="col gap-8">
      {documents.map((d) => (
        <div key={d.id} className="row between" style={{ padding: '8px 10px', border: '1px solid var(--line-100)', borderRadius: 8 }}>
          <div className="row gap-8 grow">
            <Icon name={d.contentType?.includes('pdf') ? 'picture_as_pdf' : 'description'} />
            <div className="grow">
              <div className="small bold" style={{ wordBreak: 'break-all' }}>{d.originalName}</div>
              <div className="tiny muted">{label(d.category || d.documentType)}{d.fileSize ? ` · ${Math.ceil(d.fileSize / 1024)} KB` : ''}{d.uploaderName ? ` · ${d.uploaderName}` : ''}</div>
            </div>
            {d.verified && <Badge tone="good" icon="verified">Verified</Badge>}
          </div>
          <div className="row gap-4">
            {onDownload && <Button variant="ghost" size="sm" icon="download" onClick={() => onDownload(d)} aria-label="Download" />}
            {onDelete && <Button variant="ghost" size="sm" icon="delete" onClick={() => onDelete(d)} aria-label="Delete" />}
          </div>
        </div>
      ))}
    </div>
  );
}

export function KeyValue({ items }) {
  return (
    <dl className="dl">
      {items.filter(Boolean).map(([k, v]) => (
        <div key={k} style={{ display: 'contents' }}><dt>{k}</dt><dd>{v === null || v === undefined || v === '' ? '-' : v}</dd></div>
      ))}
    </dl>
  );
}

export function Flow({ steps, current, failed }) {
  const index = steps.findIndex((s) => s.value === current);
  return (
    <div className="flow">
      {steps.map((s, i) => (
        <div key={s.value} className="row gap-4" style={{ gap: 0 }}>
          <div className={`flow-step ${i < index || (i === index && s.done) ? 'done' : i === index ? 'current' : ''}`}>
            <span className="dot">{i < index ? <Icon name="check" size="sm" /> : i + 1}</span>{s.label}
          </div>
          {i < steps.length - 1 && <span className="flow-sep" />}
        </div>
      ))}
      {failed && <Status value={failed} />}
    </div>
  );
}

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const push = useCallback((message, tone) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, message, tone }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), tone === 'bad' ? 6000 : 3500);
  }, []);
  const api = useRef({ success: (m) => push(m, 'good'), error: (m) => push(m, 'bad'), info: (m) => push(m, '') }).current;
  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.tone}`}>
            <Icon name={t.tone === 'bad' ? 'error' : t.tone === 'good' ? 'check_circle' : 'info'} />
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext);
}

export function useLoad(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const toast = useToast();
  const reload = useCallback(async () => {
    setState((s) => ({ ...s, loading: true }));
    try {
      const data = await loader();
      setState({ data, loading: false, error: null });
      return data;
    } catch (e) {
      setState({ data: null, loading: false, error: e });
      if (e?.response?.status !== 404) toast.error(errorMessage(e));
      return null;
    }
  }, deps);
  useEffect(() => { reload(); }, [reload]);
  return { ...state, reload, setData: (data) => setState((s) => ({ ...s, data })) };
}

export function useAction() {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const run = useCallback(async (fn, success) => {
    setBusy(true);
    try {
      const result = await fn();
      if (success) toast.success(typeof success === 'function' ? success(result) : success);
      return result;
    } catch (e) {
      toast.error(errorMessage(e));
      throw e;
    } finally {
      setBusy(false);
    }
  }, [toast]);
  return [run, busy];
}
