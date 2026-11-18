import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../../api/client';
import { ListingCard, useRequestQuote } from '../../components/cards';
import { Alert, Button, Card, DocumentList, Empty, Field, Icon, Loading, Pagination, Select, Stars, Verified } from '../../components/ui';
import { DELIVERY_MODES, inr, INDUSTRIES, label, PRICING_LABELS, PRICING_MODELS, STATES, date } from '../../constants';
import { useAuth } from '../../auth/AuthContext';

export function useCategories() {
  const [categories, setCategories] = useState([]);
  useEffect(() => { api.get('/public/categories').then(setCategories).catch(() => {}); }, []);
  return categories;
}

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));

export function ServiceSearch({ base = '/services', embedded }) {
  const [params, setParams] = useSearchParams();
  const categories = useCategories();
  const [page, setPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState(params.get('q') || '');
  const [requestQuote, quoteModal] = useRequestQuote();
  const filters = Object.fromEntries(params.entries());
  const selectedCategory = categories.find((c) => String(c.id) === filters.categoryId);

  useEffect(() => {
    setLoading(true);
    api.get('/public/services', { ...filters, size: 12 }).then(setPage).catch(() => setPage(null)).finally(() => setLoading(false));
  }, [params.toString()]);

  const update = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value); else next.delete(key);
    if (key === 'categoryId') next.delete('subcategoryId');
    if (key !== 'page') next.delete('page');
    setParams(next);
  };

  return (
    <div className={embedded ? '' : 'container'} style={embedded ? undefined : { paddingBottom: 56 }}>
      {!embedded && (
        <div className="page-head">
          <div className="breadcrumbs"><Link to="/">Home</Link> / Services</div>
          <h1>Business Development Services</h1>
          <p className="muted mt-4">Approved services from BDS verified providers only.</p>
        </div>
      )}
      <form className="input-group mb-16" onSubmit={(e) => { e.preventDefault(); update('q', q); }} role="search">
        <input className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search by service, expertise, provider or keyword" aria-label="Search services" />
        <button className="btn btn-primary" type="submit"><Icon name="search" />Search</button>
      </form>
      <div className="layout-2">
        <aside className="card filters" aria-label="Filters">
          <div className="row between"><h3>Filters</h3><button className="btn btn-ghost btn-sm" onClick={() => setParams(new URLSearchParams())}>Clear</button></div>
          <Field label="Category">
            <Select value={filters.categoryId || ''} onChange={(e) => update('categoryId', e.target.value)} placeholder="All categories"
              options={categories.map((c) => ({ value: String(c.id), label: c.name }))} />
          </Field>
          {selectedCategory && (
            <Field label="Subcategory">
              <Select value={filters.subcategoryId || ''} onChange={(e) => update('subcategoryId', e.target.value)} placeholder="All subcategories"
                options={selectedCategory.subcategories.map((s) => ({ value: String(s.id), label: s.name }))} />
            </Field>
          )}
          <Field label="State"><Select value={filters.state || ''} onChange={(e) => update('state', e.target.value)} placeholder="Any location" options={STATES} labels={asLabels(STATES)} /></Field>
          <Field label="Delivery mode"><Select value={filters.deliveryMode || ''} onChange={(e) => update('deliveryMode', e.target.value)} placeholder="Any" options={DELIVERY_MODES} /></Field>
          <Field label="Pricing model"><Select value={filters.pricingModel || ''} onChange={(e) => update('pricingModel', e.target.value)} placeholder="Any" options={PRICING_MODELS} labels={PRICING_LABELS} /></Field>
          <Field label="Industry expertise"><Select value={filters.industry || ''} onChange={(e) => update('industry', e.target.value)} placeholder="Any" options={INDUSTRIES} labels={asLabels(INDUSTRIES)} /></Field>
          <Field label="Minimum experience">
            <Select value={filters.minExperience || ''} onChange={(e) => update('minExperience', e.target.value)} placeholder="Any"
              options={[3, 5, 10, 15].map((v) => ({ value: String(v), label: `${v}+ years` }))} />
          </Field>
          <Field label="Indicative budget up to">
            <Select value={filters.maxFee || ''} onChange={(e) => update('maxFee', e.target.value)} placeholder="Any"
              options={[25000, 50000, 100000, 250000, 500000].map((v) => ({ value: String(v), label: inr(v) }))} />
          </Field>
          <div className="tiny muted"><Icon name="verified" size="sm" /> BDS Verified providers only</div>
        </aside>
        <div>
          <div className="row between mb-16">
            <span className="muted">{page ? `${page.totalElements} service${page.totalElements === 1 ? '' : 's'} found` : ''}</span>
            <div className="row gap-8">
              <span className="small muted">Sort by</span>
              <Select value={filters.sort || ''} onChange={(e) => update('sort', e.target.value)} style={{ width: 200 }}
                options={[{ value: '', label: 'Relevance' }, { value: 'experience', label: 'Experience' }, { value: 'location', label: 'Location' },
                  { value: 'recent', label: 'Recently added' }, { value: 'feeAsc', label: 'Fee: low to high' }, { value: 'feeDesc', label: 'Fee: high to low' }]} />
            </div>
          </div>
          {loading ? <Loading /> : !page || page.content.length === 0 ? (
            <div className="card"><Empty icon="search_off" title="No services match these filters">Try removing a filter or searching a broader keyword.</Empty></div>
          ) : (
            <>
              <div className="grid grid-auto">{page.content.map((l) => <ListingCard key={l.id} listing={l} base={base} onQuote={(x) => requestQuote({ listingId: x.id })} />)}</div>
              <div className="card mt-16"><Pagination page={page} onPage={(p) => update('page', String(p))} /></div>
            </>
          )}
        </div>
      </div>
      {quoteModal}
    </div>
  );
}

export function ServiceDetail({ providerBase = '/providers', embedded }) {
  const { id } = useParams();
  const { user } = useAuth();
  const [listing, setListing] = useState(null);
  const [reviews, setReviews] = useState(null);
  const [missing, setMissing] = useState(false);
  const [requestQuote, quoteModal] = useRequestQuote();
  useEffect(() => {
    setListing(null);
    api.get(`/public/services/${id}`).then((l) => {
      setListing(l);
      api.get(`/public/reviews/sellers/${l.summary.sellerId}`, { size: 3 }).then(setReviews).catch(() => {});
    }).catch(() => setMissing(true));
  }, [id]);
  if (missing) return <div className="container section"><div className="card"><Empty icon="search_off" title="Service not available">This service is not published or the provider is not active.</Empty></div></div>;
  if (!listing) return <Loading />;
  const s = listing.summary;
  const section = (title, text) => text && <div className="mt-24"><h3>{title}</h3><p className="pre mt-8" style={{ color: 'var(--ink-700)' }}>{text}</p></div>;
  return (
    <div className={embedded ? '' : 'container'} style={{ paddingBottom: 56 }}>
      {!embedded && <div className="page-head"><div className="breadcrumbs"><Link to="/">Home</Link> / <Link to="/services">Services</Link> / {s.categoryName}</div></div>}
      <div className="layout-main-side">
        <div className="card">
          <div className="card-body">
            <span className="small bold" style={{ color: 'var(--teal-600)', textTransform: 'uppercase' }}>{s.categoryName}{s.subcategoryName ? ` · ${s.subcategoryName}` : ''}</span>
            <h1 className="mt-8">{s.title}</h1>
            <div className="row wrap gap-16 mt-16">
              <Link to={`${providerBase}/${s.sellerId}`} className="bold">{s.sellerName}</Link>
              <Verified />
              <Stars value={s.sellerRating} count={s.sellerRatingCount} />
            </div>
            <div className="grid grid-4 mt-24">
              {[['schedule', 'Timeline', s.estimatedDuration || '-'], ['devices', 'Delivery mode', label(s.deliveryMode)],
                ['work_history', 'Experience', s.experienceYears != null ? `${s.experienceYears} years` : '-'],
                ['currency_rupee', 'Indicative fee', s.indicativeFee ? `${inr(s.indicativeFee)} · ${PRICING_LABELS[s.pricingModel]}` : PRICING_LABELS[s.pricingModel]]].map(([icon, k, v]) => (
                <div key={k} className="card flat" style={{ padding: 12 }}>
                  <div className="row gap-8 muted small"><Icon name={icon} size="sm" />{k}</div>
                  <div className="bold mt-4">{v}</div>
                </div>
              ))}
            </div>
            {section('Description', listing.description)}
            {section('Scope of services', listing.scopeOfWork)}
            {section('Deliverables', listing.deliverables)}
            {section('Methodology', listing.methodology)}
            {listing.targetIndustries.length > 0 && <div className="mt-24"><h3>Target industries</h3><div className="chips mt-8">{listing.targetIndustries.map((t) => <span className="chip" key={t}>{t}</span>)}</div></div>}
            {listing.serviceLocations.length > 0 && <div className="mt-24"><h3>Service locations</h3><div className="chips mt-8">{listing.serviceLocations.map((t) => <span className="chip" key={t}><Icon name="location_on" size="sm" />{t}</span>)}</div></div>}
            {listing.attachments.length > 0 && (
              <div className="mt-24">
                <h3>Supporting documents</h3>
                <div className="mt-8">
                  {user ? <DocumentList documents={listing.attachments} onDownload={(d) => api.download(`/services/${s.id}/attachments/${d.id}/download`)} />
                    : <p className="small muted">{listing.attachments.length} document(s) available after sign in.</p>}
                </div>
              </div>
            )}
            <div className="mt-24">
              <h3>Feedback & ratings</h3>
              {!reviews || reviews.content.length === 0 ? <p className="muted small mt-8">No eligible feedback yet. Ratings are accepted only from purchasers with completed engagements.</p> : (
                <div className="col mt-8">
                  {reviews.content.map((r) => (
                    <div key={r.id} className="card flat" style={{ padding: 14 }}>
                      <div className="row between"><Stars value={r.rating} /><span className="tiny muted">{date(r.createdAt)}</span></div>
                      {r.title && <div className="bold mt-8">{r.title}</div>}
                      {r.reviewText && <p className="small mt-4">{r.reviewText}</p>}
                      <div className="tiny muted mt-8">{r.buyerName} · {r.engagementTitle}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="col gap-16" style={{ position: 'sticky', top: 80 }}>
          <Card>
            <h3>Interested in this service?</h3>
            <p className="small muted mt-8">Share your requirement with this provider and receive a structured technical and financial quotation.</p>
            <Button className="mt-16" block size="lg" icon="request_quote" onClick={() => requestQuote({ listingId: s.id })}>Request Quotation</Button>
            {!user && <p className="tiny muted mt-8 center">Login required to submit an RFQ.</p>}
          </Card>
          <Card title="About the provider">
            <div className="col gap-8">
              <div className="bold">{s.sellerName}</div>
              <Verified />
              <Stars value={s.sellerRating} count={s.sellerRatingCount} />
              {s.sellerState && <div className="small muted"><Icon name="location_on" size="sm" />{s.sellerState}</div>}
              <Link to={`${providerBase}/${s.sellerId}`} className="btn btn-outline btn-sm mt-8">View provider profile</Link>
            </div>
          </Card>
          <Alert tone="info">Verification confirms provider credentials; it does not certify every service or guarantee outcomes.</Alert>
          <div className="tiny muted mono">Ref: {s.referenceNo}</div>
        </div>
      </div>
      {quoteModal}
    </div>
  );
}

export default function ServicesPage() {
  return <ServiceSearch />;
}
