import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, errorMessage, fieldErrors } from '../../api/client';
import { protect } from '../../api/crypto';
import { homeFor, useAuth } from '../../auth/AuthContext';
import { Brand } from '../../components/layouts';
import { F, payload, useForm } from '../../components/kit';
import { Alert, Button, Checkbox, Field, Icon, Input, useToast } from '../../components/ui';
import { CONSTITUTION_LABELS, CONSTITUTIONS, DELIVERY_MODES, DOCUMENT_TYPES, INDUSTRIES, label, PROVIDER_TYPES, PURCHASER_LABELS, PURCHASER_TYPES, STATES } from '../../constants';
import { useCategories } from './Services';

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));
const ROLES = [['BUYER', 'apartment', 'Purchaser'], ['SELLER', 'storefront', 'Provider'], ['ADMIN', 'admin_panel_settings', 'Admin']];

function AuthShell({ title, subtitle, children, wide }) {
  return (
    <div className="auth-page">
      <aside className="auth-aside">
        <Brand />
        <div>
          <h2>Verified business development services for MSMEs</h2>
          <p className="mt-16">Discover approved providers, share requirements as structured RFQs, compare quotations side by side and manage engagements in one place.</p>
        </div>
        <div className="small" style={{ color: '#cfe3ee' }}>BDS Marketplace does not collect or process payments.</div>
      </aside>
      <div className="auth-form">
        <div className="auth-card" style={wide ? { maxWidth: 760 } : undefined}>
          <h1 style={{ fontSize: 26 }}>{title}</h1>
          {subtitle && <p className="muted mt-4">{subtitle}</p>}
          <div className="mt-24">{children}</div>
        </div>
      </div>
    </div>
  );
}

function DevOtp({ otp }) {
  if (!otp) return null;
  return <div className="demo-box mt-8">Development mode: OTP is <b className="mono">{otp}</b></div>;
}

export function Login() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { establish, isAuthenticated, user } = useAuth();
  const [portal, setPortal] = useState(params.get('role') || 'BUYER');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [challenge, setChallenge] = useState(null);
  const [otp, setOtp] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(params.get('expired') ? 'Your session expired. Please sign in again.' : '');
  const next = params.get('next');

  useEffect(() => { if (isAuthenticated && !challenge) navigate(next || homeFor(user.role), { replace: true }); }, [isAuthenticated]);

  const finish = (tokens) => {
    establish(tokens);
    navigate(next || homeFor(tokens.user.role), { replace: true });
  };
  const login = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const { data } = await api.post('/auth/login', { username: username.trim(), password: await protect(password), portal });
      if (data.otpRequired) setChallenge(data); else finish(data.tokens);
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };
  const verify = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const { data } = await api.post('/auth/login/verify', { challengeToken: challenge.challengeToken, otp });
      finish(data);
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };

  if (challenge) {
    return (
      <AuthShell title="Verify it's you" subtitle={`Enter the 6-digit code sent to ${challenge.maskedDestination}`}>
        <form className="col gap-16" onSubmit={verify}>
          {error && <Alert tone="bad">{error}</Alert>}
          <Field label="One-time password" required>
            <Input value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" autoFocus placeholder="6-digit code" />
          </Field>
          <DevOtp otp={challenge.devOtp} />
          <Button type="submit" size="lg" block loading={busy} disabled={otp.length !== 6}>Verify and sign in</Button>
          <Button type="button" variant="ghost" onClick={() => { setChallenge(null); setOtp(''); }}>Back</Button>
        </form>
      </AuthShell>
    );
  }
  return (
    <AuthShell title="Sign in" subtitle="Access your BDS Marketplace workspace">
      <form className="col gap-16" onSubmit={login}>
        <div className="role-picker" role="radiogroup" aria-label="Portal">
          {ROLES.map(([r, icon, text]) => (
            <button type="button" key={r} className={`role-option ${portal === r ? 'active' : ''}`} onClick={() => setPortal(r)} aria-pressed={portal === r}>
              <Icon name={icon} />{text}
            </button>
          ))}
        </div>
        {error && <Alert tone="bad">{error}</Alert>}
        <Field label="Email or mobile" required><Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus /></Field>
        <Field label="Password" required><Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" /></Field>
        <div className="row between"><span /><Link to="/forgot-password" className="small">Forgot password?</Link></div>
        <Button type="submit" size="lg" block loading={busy} disabled={!username || !password}>Sign in</Button>
        <p className="small muted center">New to BDS Marketplace? <Link to="/register">Create an account</Link></p>
      </form>
    </AuthShell>
  );
}

export function ForgotPassword() {
  const navigate = useNavigate();
  const toast = useToast();
  const [username, setUsername] = useState('');
  const [sent, setSent] = useState(null);
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const send = async (e) => {
    e.preventDefault(); setBusy(true); setError('');
    try { const { data } = await api.post('/auth/password/forgot', { username }); setSent(data); } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };
  const reset = async (e) => {
    e.preventDefault(); setBusy(true); setError('');
    try {
      await api.post('/auth/password/reset', { username, otp, newPassword: await protect(password) });
      toast.success('Password reset. Please sign in.');
      navigate('/login');
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };
  return (
    <AuthShell title="Reset password" subtitle={sent ? `Enter the code sent to ${sent.maskedTarget} and choose a new password` : 'We will send a one-time code to your registered email'}>
      {error && <Alert tone="bad">{error}</Alert>}
      {!sent ? (
        <form className="col gap-16 mt-16" onSubmit={send}>
          <Field label="Email or mobile" required><Input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus /></Field>
          <Button type="submit" block loading={busy} disabled={!username}>Send code</Button>
          <Link to="/login" className="small center">Back to sign in</Link>
        </form>
      ) : (
        <form className="col gap-16 mt-16" onSubmit={reset}>
          <Field label="One-time password" required><Input value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" /></Field>
          <DevOtp otp={sent.devOtp} />
          <Field label="New password" required hint="At least 8 characters with upper and lower case letters, a digit and a symbol">
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
          </Field>
          <Button type="submit" block loading={busy} disabled={otp.length !== 6 || !password}>Reset password</Button>
        </form>
      )}
    </AuthShell>
  );
}

export function RegisterChoice() {
  return (
    <AuthShell title="Create an account" subtitle="Choose how you want to use BDS Marketplace">
      <div className="col gap-16">
        <Link to="/register/buyer" className="card" style={{ padding: 20 }}>
          <div className="row gap-16"><Icon name="apartment" /><div><h3>Purchaser (MSME / organisation)</h3><p className="small muted">Find verified providers, publish RFQs and compare quotations.</p></div></div>
        </Link>
        <Link to="/register/seller" className="card" style={{ padding: 20 }}>
          <div className="row gap-16"><Icon name="storefront" /><div><h3>Service provider</h3><p className="small muted">Get verified, publish services and respond to RFQs.</p></div></div>
        </Link>
        <p className="small muted center">Already registered? <Link to="/login">Sign in</Link></p>
      </div>
    </AuthShell>
  );
}

/* Email + OTP + password block shared by both registrations. */
function AccountFields({ form }) {
  const [sent, setSent] = useState(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const sendOtp = async () => {
    setBusy(true);
    try { const { data } = await api.post('/auth/otp/send', { target: form.values.email, purpose: 'REGISTRATION' }); setSent(data); toast.success(`Code sent to ${data.maskedTarget}`); } catch (e) { toast.error(errorMessage(e)); } finally { setBusy(false); }
  };
  return (
    <div className="grid grid-2">
      <F form={form} name="fullName" label="Full name" required />
      <F form={form} name="mobile" label="Mobile" required hint="10-digit Indian mobile number" />
      <Field label="Email" required error={form.errors.email} className="span-2">
        <div className="input-group">
          <Input type="email" value={form.values.email} onChange={form.set('email')} />
          <Button type="button" variant="outline" loading={busy} disabled={!form.values.email} onClick={sendOtp}>{sent ? 'Resend code' : 'Send code'}</Button>
        </div>
      </Field>
      <F form={form} name="emailOtp" label="Email verification code" required />
      <F form={form} name="password" label="Password" type="password" required hint="8+ characters, upper & lower case, digit and symbol" />
      {sent?.devOtp && <div className="span-2"><DevOtp otp={sent.devOtp} /></div>}
    </div>
  );
}

function OrgFields({ form, buyer }) {
  return (
    <div className="grid grid-2">
      <F form={form} name="organisationName" label="Organisation name" required />
      <F form={form} name="constitution" label="Constitution" type="select" options={CONSTITUTIONS} labels={CONSTITUTION_LABELS} required={!buyer} />
      {buyer && <F form={form} name="purchaserType" label="Purchaser type" type="select" options={PURCHASER_TYPES} labels={PURCHASER_LABELS} required />}
      {buyer && <F form={form} name="industrySector" label="Industry sector" type="select" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} required />}
      {buyer && <F form={form} name="designation" label="Your designation" />}
      <F form={form} name="pan" label="PAN" required={!buyer} hint="e.g. ABCDE1234F" />
      <F form={form} name="gstin" label="GSTIN" />
      <F form={form} name="udyamNumber" label="Udyam registration number" />
      <F form={form} name="addressLine" label="Address" required className="span-2" />
      <F form={form} name="city" label="City" />
      <F form={form} name="state" label="State" type="select" options={STATES} labels={asLabels(STATES)} required />
      <F form={form} name="district" label="District" required />
      <F form={form} name="pincode" label="PIN code" />
      <F form={form} name="website" label="Website" />
      {buyer && <F form={form} name="businessActivity" label="Business activity" className="span-2" />}
    </div>
  );
}

function Steps({ steps, current }) {
  return (
    <div className="steps">
      {steps.map((s, i) => <div key={s} className={`step ${i < current ? 'done' : i === current ? 'active' : ''}`}><span className="num">{i < current ? <Icon name="check" size="sm" /> : i + 1}</span>{s}</div>)}
    </div>
  );
}

function Registered({ result }) {
  return (
    <AuthShell title="Registration submitted">
      <Alert tone="good" title={`Reference ${result.referenceNo}`}>{result.message}</Alert>
      <p className="mt-16 muted">You can sign in now. Some features are enabled after the BDS team verifies your account.</p>
      <Link to="/login" className="btn btn-primary btn-block mt-24">Go to sign in</Link>
    </AuthShell>
  );
}

const BUYER_INITIAL = { fullName: '', email: '', mobile: '', password: '', emailOtp: '', organisationName: '', constitution: '', purchaserType: '', designation: '', pan: '', gstin: '', udyamNumber: '', industrySector: '', businessActivity: '', addressLine: '', city: '', state: '', district: '', pincode: '', website: '', declarationAccepted: false };

export function RegisterBuyer() {
  const form = useForm(BUYER_INITIAL);
  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const submit = async () => {
    setBusy(true); setError(''); form.setErrors({});
    try {
      const body = payload({ ...form.values, password: await protect(form.values.password) });
      const { data } = await api.post('/auth/register/buyer', body);
      setResult(data);
    } catch (e) { setError(errorMessage(e)); form.setErrors(fieldErrors(e)); } finally { setBusy(false); }
  };
  if (result) return <Registered result={result} />;
  return (
    <AuthShell title="Register as purchaser" wide>
      <Steps steps={['Account', 'Organisation', 'Declaration']} current={step} />
      {error && <Alert tone="bad">{error}</Alert>}
      <div className="mt-16">
        {step === 0 && <AccountFields form={form} />}
        {step === 1 && <OrgFields form={form} buyer />}
        {step === 2 && (
          <div className="col gap-16">
            <Alert tone="info">BDS Marketplace facilitates discovery and procurement. Payments and contracts are agreed directly between purchasers and providers.</Alert>
            <Checkbox label="I confirm the information provided is true and I accept the terms of use and privacy policy." checked={form.values.declarationAccepted} onChange={form.set('declarationAccepted')} />
          </div>
        )}
      </div>
      <div className="row between mt-24">
        {step > 0 ? <Button variant="ghost" onClick={() => setStep(step - 1)}>Back</Button> : <Link to="/register" className="btn btn-ghost">Back</Link>}
        {step < 2 ? <Button onClick={() => setStep(step + 1)}>Continue</Button>
          : <Button loading={busy} disabled={!form.values.declarationAccepted} onClick={submit}>Submit registration</Button>}
      </div>
    </AuthShell>
  );
}

const SELLER_INITIAL = { fullName: '', email: '', mobile: '', password: '', emailOtp: '', organisationName: '', constitution: '', pan: '', gstin: '', udyamNumber: '', addressLine: '', city: '', state: '', district: '', pincode: '', website: '', providerType: '', categoryIds: [], expertise: [], industries: [], yearsExperience: '', teamSize: '', geographicCoverage: [], deliveryModes: [], introduction: '', declarationAccepted: false };

export function RegisterSeller() {
  const form = useForm(SELLER_INITIAL);
  const categories = useCategories();
  const [meta, setMeta] = useState(null);
  const [files, setFiles] = useState({});
  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  useEffect(() => { api.get('/auth/meta').then(setMeta).catch(() => {}); }, []);
  const mandatory = meta?.mandatoryDocuments?.[form.values.providerType] || [];
  const catLabels = Object.fromEntries(categories.map((c) => [String(c.id), c.name]));
  const submit = async () => {
    setBusy(true); setError(''); form.setErrors({});
    try {
      const v = form.values;
      const data = payload({ ...v, password: await protect(v.password), categoryIds: v.categoryIds.map(Number) }, ['yearsExperience', 'teamSize']);
      const entries = Object.entries(files).filter(([, f]) => f);
      const { data: res } = await api.upload('/auth/register/seller', { data, files: entries.map(([, f]) => f), fileTypes: entries.map(([t]) => t) });
      setResult(res);
    } catch (e) { setError(errorMessage(e)); form.setErrors(fieldErrors(e)); } finally { setBusy(false); }
  };
  if (result) return <Registered result={result} />;
  const missing = mandatory.filter((t) => !files[t]);
  return (
    <AuthShell title="Register as service provider" wide>
      <Steps steps={['Account', 'Organisation', 'Expertise', 'Documents']} current={step} />
      {error && <Alert tone="bad">{error}</Alert>}
      <div className="mt-16">
        {step === 0 && <AccountFields form={form} />}
        {step === 1 && <OrgFields form={form} />}
        {step === 2 && (
          <div className="grid grid-2">
            <F form={form} name="providerType" label="Provider type" type="select" options={PROVIDER_TYPES} required />
            <F form={form} name="yearsExperience" label="Years of experience" type="number" min="0" required />
            <F form={form} name="teamSize" label="Team size" type="number" min="1" />
            <div />
            <F form={form} name="categoryIds" label="Service categories" type="chips" options={categories.map((c) => String(c.id))} labels={catLabels} className="span-2" />
            <F form={form} name="deliveryModes" label="Delivery modes" type="chips" options={DELIVERY_MODES} className="span-2" />
            <F form={form} name="expertise" label="Areas of expertise" type="tags" className="span-2" />
            <F form={form} name="industries" label="Industries served" type="chips" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} className="span-2" />
            <F form={form} name="geographicCoverage" label="Geographic coverage (states)" type="tags" className="span-2" />
            <F form={form} name="introduction" label="Introduction" type="textarea" className="span-2" />
          </div>
        )}
        {step === 3 && (
          <div className="col gap-16">
            <p className="small muted">Upload PDF or image files (max 5 MB each). Mandatory for {label(form.values.providerType) || 'your provider type'}: {mandatory.map(label).join(', ') || 'choose a provider type first'}.</p>
            {(meta?.documentTypes || DOCUMENT_TYPES).map((t) => (
              <div key={t} className="row between" style={{ padding: '8px 12px', border: '1px solid var(--line-100)', borderRadius: 8 }}>
                <div className="small bold">{label(t)}{mandatory.includes(t) && <span className="req"> *</span>}</div>
                <div className="row gap-8">
                  {files[t] && <span className="tiny muted">{files[t].name}</span>}
                  <input type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={(e) => setFiles({ ...files, [t]: e.target.files?.[0] })} aria-label={label(t)} />
                </div>
              </div>
            ))}
            <Checkbox label="I confirm the information and documents are genuine and I accept the provider terms." checked={form.values.declarationAccepted} onChange={form.set('declarationAccepted')} />
          </div>
        )}
      </div>
      <div className="row between mt-24">
        {step > 0 ? <Button variant="ghost" onClick={() => setStep(step - 1)}>Back</Button> : <Link to="/register" className="btn btn-ghost">Back</Link>}
        {step < 3 ? <Button onClick={() => setStep(step + 1)}>Continue</Button>
          : <Button loading={busy} disabled={!form.values.declarationAccepted || missing.length > 0} onClick={submit}>Submit for verification</Button>}
      </div>
    </AuthShell>
  );
}
