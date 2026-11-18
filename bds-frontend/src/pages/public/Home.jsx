import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../api/client';
import { ListingCard, ProviderCard, useRequestQuote } from '../../components/cards';
import { usePublicSettings } from '../../components/layouts';
import { Icon } from '../../components/ui';

export const HOW_STEPS = [
  ['edit_note', 'Share your requirement', 'Describe the business service you need, budget, location and deadline in a structured RFQ.'],
  ['send', 'Reach verified providers', 'Send the RFQ to providers you choose, platform recommendations or administrator-assisted matching.'],
  ['request_quote', 'Receive quotations', 'Each provider submits an independent technical and financial proposal before the deadline.'],
  ['compare', 'Compare & select', 'Evaluate side by side with weighted criteria - price is not automatically the winner.'],
  ['assignment', 'Work order & delivery', 'Issue the work order, track progress and resolve queries in one place.'],
  ['reviews', 'Confirm & rate', 'Confirm delivery and rate the provider to help other MSMEs choose well.']
];

export default function Home() {
  const navigate = useNavigate();
  const settings = usePublicSettings();
  const [categories, setCategories] = useState([]);
  const [providers, setProviders] = useState([]);
  const [services, setServices] = useState([]);
  const [stats, setStats] = useState(null);
  const [q, setQ] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [requestQuote, quoteModal] = useRequestQuote();

  useEffect(() => {
    api.get('/public/categories').then(setCategories).catch(() => {});
    api.get('/public/catalog/stats').then(setStats).catch(() => {});
    api.get('/public/services', { sort: 'popular', size: 6 }).then((p) => setServices(p.content)).catch(() => {});
    api.get('/public/providers', { size: 4 }).then((p) => setProviders(p.content)).catch(() => {});
  }, []);

  const search = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (categoryId) params.set('categoryId', categoryId);
    navigate(`/services?${params}`);
  };

  return (
    <>
      <section className="hero">
        <div className="container">
          <div className="eyebrow">Verified business development services for MSMEs</div>
          <h1>{settings['platform.tagline'] || 'Grow Your MSME with the Right Expertise'}</h1>
          <p className="lead">Discover approved consultants, agencies and institutions for project reports, credit facilitation, compliance, technology, marketing, quality and sustainability - and get competing quotations in days.</p>
          <form className="search-panel" onSubmit={search} role="search">
            <div className="row grow" style={{ gap: 0 }}>
              <Icon name="search" className="muted" />
              <input className="input" placeholder="Search services, expertise or providers..." value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search" />
            </div>
            <select className="select" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} aria-label="Category">
              <option value="">All categories</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button className="btn btn-accent btn-lg" type="submit">Search</button>
          </form>
          <div className="popular">
            <span>Popular:</span>
            {['DPR', 'GST', 'ISO', 'Loan Documentation', 'Website', 'Energy Audit'].map((t) => <Link key={t} to={`/services?q=${encodeURIComponent(t)}`}>{t}</Link>)}
          </div>
          <div className="row wrap gap-16 mt-24">
            <Link className="btn btn-primary btn-lg" to="/services"><Icon name="travel_explore" />Find Business Services</Link>
            <Link className="btn btn-outline btn-lg" style={{ background: 'transparent', color: '#fff', borderColor: 'rgba(255,255,255,.4)' }} to="/register/seller"><Icon name="verified" />Become a Verified Service Provider</Link>
          </div>
          {stats && (
            <div className="hero-stats">
              <div><div className="v">{stats.services}</div><div className="l">Approved services</div></div>
              <div><div className="v">{stats.providers}</div><div className="l">Verified providers</div></div>
              <div><div className="v">{stats.categories}</div><div className="l">Service categories</div></div>
            </div>
          )}
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head">
            <div><h2>Explore Categories</h2><p>Services organised the way MSMEs need them.</p></div>
            <Link to="/services" className="bold">All services <Icon name="arrow_forward" size="sm" /></Link>
          </div>
          <div className="grid grid-4">
            {categories.map((c) => (
              <div key={c.id} className="card cat-card" onClick={() => navigate(`/services?categoryId=${c.id}`)} role="link" tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && navigate(`/services?categoryId=${c.id}`)}>
                <div className="icon"><Icon name={c.icon} /></div>
                <h3>{c.name}</h3>
                <p className="small muted">{c.description}</p>
                <div className="count">{c.listingCount} service{c.listingCount === 1 ? '' : 's'}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section alt">
        <div className="container">
          <div className="section-head">
            <div><h2>Featured Verified Providers</h2><p>Approved by the BDS verification team.</p></div>
            <Link to="/providers" className="bold">All providers <Icon name="arrow_forward" size="sm" /></Link>
          </div>
          {providers.length === 0 ? <p className="muted">Verified providers will appear here once approved.</p> : (
            <div className="grid grid-4">{providers.map((p) => <ProviderCard key={p.id} provider={p} />)}</div>
          )}
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head">
            <div><h2>Popular Services</h2><p>Most requested this quarter.</p></div>
            <Link to="/services?sort=popular" className="bold">Browse all <Icon name="arrow_forward" size="sm" /></Link>
          </div>
          {services.length === 0 ? <p className="muted">Approved services will appear here.</p> : (
            <div className="grid grid-3">{services.map((s) => <ListingCard key={s.id} listing={s} onQuote={(l) => requestQuote({ listingId: l.id })} />)}</div>
          )}
        </div>
      </section>

      <section className="section alt">
        <div className="container">
          <div className="section-head"><div><h2>How It Works</h2><p>From requirement to engagement.</p></div></div>
          <div className="grid grid-3">
            {HOW_STEPS.map(([icon, title, text], i) => (
              <div className="card how-step" key={title}>
                <div className="row between"><span className="n">{i + 1}</span><Icon name={icon} className="muted" /></div>
                <h3>{title}</h3>
                <p className="muted small mt-8">{text}</p>
              </div>
            ))}
          </div>
          <div className="grid grid-4 mt-24">
            {[['verified_user', 'Verified providers only', 'Every provider passes document and credential verification.'],
              ['balance', 'Fair comparison', 'Structured technical and commercial evaluation with weighted scoring.'],
              ['lock', 'Secure & private', 'Sensitive identity data is encrypted; documents are never public.'],
              ['support_agent', 'Query tracker & support', 'Raise and resolve queries with providers; escalate to the platform anytime.']].map(([icon, t, d]) => (
              <div key={t} className="row top gap-16">
                <span className="cat-card" style={{ padding: 0 }}><span className="icon" style={{ width: 40, height: 40, borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--teal-100)', color: 'var(--teal-600)' }}><Icon name={icon} /></span></span>
                <div><h4>{t}</h4><p className="small muted mt-4">{d}</p></div>
              </div>
            ))}
          </div>
          <div className="alert info mt-24"><Icon name="info" /><div>BDS Marketplace does not collect or process payments. Purchasers and providers finalise payment and contractual arrangements directly.</div></div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="cta-band">
            <div>
              <h2>Are you a service provider?</h2>
              <p>Get verified, publish your services and receive business opportunities from MSMEs across India.</p>
            </div>
            <Link className="btn btn-accent btn-lg" to="/register/seller">Register as Provider</Link>
          </div>
        </div>
      </section>
      {quoteModal}
    </>
  );
}
