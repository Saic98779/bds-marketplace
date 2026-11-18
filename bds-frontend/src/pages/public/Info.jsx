import { Link } from 'react-router-dom';
import { usePublicSettings } from '../../components/layouts';
import { Alert, Card, Icon } from '../../components/ui';
import { HOW_STEPS } from './Home';

export function HowItWorks() {
  return (
    <div className="container" style={{ paddingBottom: 56 }}>
      <div className="page-head">
        <div className="breadcrumbs"><Link to="/">Home</Link> / How it works</div>
        <h1>How BDS Marketplace works</h1>
        <p className="muted mt-4">A transparent path from requirement to completed engagement.</p>
      </div>
      <div className="grid grid-3">
        {HOW_STEPS.map(([icon, title, text], i) => (
          <div className="card how-step" key={title}>
            <div className="row between"><span className="n">{i + 1}</span><Icon name={icon} className="muted" /></div>
            <h3>{title}</h3>
            <p className="muted small mt-8">{text}</p>
          </div>
        ))}
      </div>
      <div className="grid grid-2 mt-24">
        <Card title="For purchasers">
          <ol className="small" style={{ paddingLeft: 18, lineHeight: 1.9 }}>
            <li>Register your organisation and verify your email.</li>
            <li>Browse approved services or verified providers.</li>
            <li>Publish an RFQ to selected, recommended or admin-matched providers.</li>
            <li>Compare quotations with weighted criteria and accept the best fit.</li>
            <li>Issue a work order, track progress, confirm delivery and rate the provider.</li>
          </ol>
          <Link to="/register/buyer" className="btn btn-primary mt-16">Register as purchaser</Link>
        </Card>
        <Card title="For service providers">
          <ol className="small" style={{ paddingLeft: 18, lineHeight: 1.9 }}>
            <li>Register and upload verification documents.</li>
            <li>Get verified by the BDS team.</li>
            <li>Publish service listings (moderated before going live).</li>
            <li>Respond to RFQs with technical and financial quotations.</li>
            <li>Deliver the engagement and build your rating.</li>
          </ol>
          <Link to="/register/seller" className="btn btn-accent mt-16">Register as provider</Link>
        </Card>
      </div>
    </div>
  );
}

const SECTIONS = [
  ['faq', 'Frequently asked questions', 'content.faq', 'Registration is free for purchasers and providers. Providers are verified against their documents before they can publish services or quote. Payments are agreed and made directly between the parties.'],
  ['grievance', 'Grievance redressal', 'content.grievance', 'Signed-in users can raise a grievance from their Query Tracker. Grievances are handled by the platform administration within the published service levels.'],
  ['contact', 'Contact support', 'content.contact', 'Email support@bdsmarketplace.in for help with registration, verification or the platform.'],
  ['terms', 'Terms of use', 'content.terms', 'By using BDS Marketplace you agree to provide accurate information and to use the platform for legitimate business purposes only.'],
  ['privacy', 'Privacy policy', 'content.privacy', 'Personal and identity data is encrypted at rest. Verification documents are never made public.'],
  ['disclaimer', 'Disclaimer', 'disclaimer.payments', 'BDS Marketplace does not collect, hold or process payments and does not provide escrow.']
];

export function About() {
  const settings = usePublicSettings();
  return (
    <div className="container" style={{ paddingBottom: 56 }}>
      <div className="page-head">
        <div className="breadcrumbs"><Link to="/">Home</Link> / About</div>
        <h1>About {settings['platform.name'] || 'BDS Marketplace'}</h1>
        <p className="muted mt-4">{settings['platform.tagline'] || 'A verified digital marketplace for MSME business development services.'}</p>
      </div>
      <div className="col gap-16">
        {SECTIONS.map(([id, title, key, fallback]) => (
          <Card key={id} title={title}>
            <div id={id} className="pre small">{settings[key] || fallback}</div>
            {id === 'contact' && (
              <div className="small mt-8">
                <div><Icon name="mail" size="sm" /> {settings['support.email'] || 'support@bdsmarketplace.in'}</div>
                {settings['support.phone'] && <div><Icon name="call" size="sm" /> {settings['support.phone']}</div>}
                {settings['support.hours'] && <div><Icon name="schedule" size="sm" /> {settings['support.hours']}</div>}
              </div>
            )}
          </Card>
        ))}
        <Alert tone="info">Provider verification does not certify every listed service or guarantee outcomes.</Alert>
      </div>
    </div>
  );
}
