import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../../api/client';
import { ListingCard, ProviderCard, useRequestQuote } from '../../components/cards';
import { Alert, Button, Card, Empty, Field, Icon, KeyValue, Loading, Pagination, Select, Stars, Verified } from '../../components/ui';
import { date, DELIVERY_MODES, label, PROVIDER_TYPES, STATES } from '../../constants';
import { useCategories } from './Services';

const asLabels = (list) => Object.fromEntries(list.map((s) => [s, s]));

export function ProviderSearch({ base = '/providers', embedded }) {
  const [params, setParams] = useSearchParams();
  const categories = useCategories();
  const [page, setPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState(params.get('q') || '');
  const [requestQuote, quoteModal] = useRequestQuote();
  const filters = Object.fromEntries(params.entries());
  useEffect(() => {
    setLoading(true);
    api.get('/public/providers', { ...filters, size: 12 }).then(setPage).catch(() => setPage(null)).finally(() => setLoading(false));
  }, [params.toString()]);
  const update = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value); else next.delete(key);
    if (key !== 'page') next.delete('page');
    setParams(next);
  };
  return (
    <div className={embedded ? '' : 'container'} style={embedded ? undefined : { paddingBottom: 56 }}>
      {!embedded && (
        <div className="page-head">
          <div className="breadcrumbs"><Link to="/">Home</Link> / Providers</div>
          <h1>Verified Service Providers</h1>
          <p className="muted mt-4">Every provider is verified by the BDS team before appearing here.</p>
        </div>
      )}
      <form className="input-group mb-16" onSubmit={(e) => { e.preventDefault(); update('q', q); }} role="search">
        <input className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search by name, expertise or location" aria-label="Search providers" />
        <button className="btn btn-primary" type="submit"><Icon name="search" />Search</button>
      </form>
      <div className="layout-2">
        <aside className="card filters" aria-label="Filters">
          <div className="row between"><h3>Filters</h3><button className="btn btn-ghost btn-sm" onClick={() => setParams(new URLSearchParams())}>Clear</button></div>
          <Field label="Category"><Select value={filters.categoryId || ''} onChange={(e) => update('categoryId', e.target.value)} placeholder="All categories" options={categories.map((c) => ({ value: String(c.id), label: c.name }))} /></Field>
          <Field label="Provider type"><Select value={filters.providerType || ''} onChange={(e) => update('providerType', e.target.value)} placeholder="Any" options={PROVIDER_TYPES} /></Field>
          <Field label="State"><Select value={filters.state || ''} onChange={(e) => update('state', e.target.value)} placeholder="Any" options={STATES} labels={asLabels(STATES)} /></Field>
          <Field label="Delivery mode"><Select value={filters.deliveryMode || ''} onChange={(e) => update('deliveryMode', e.target.value)} placeholder="Any" options={DELIVERY_MODES} /></Field>
          <Field label="Minimum experience"><Select value={filters.minExperience || ''} onChange={(e) => update('minExperience', e.target.value)} placeholder="Any" options={[3, 5, 10, 15].map((v) => ({ value: String(v), label: `${v}+ years` }))} /></Field>
          <Field label="Sort by"><Select value={filters.sort || ''} onChange={(e) => update('sort', e.target.value)} options={[{ value: '', label: 'Relevance' }, { value: 'rating', label: 'Rating' }, { value: 'experience', label: 'Experience' }, { value: 'name', label: 'Name' }]} /></Field>
        </aside>
        <div>
          <div className="muted mb-16">{page ? `${page.totalElements} provider${page.totalElements === 1 ? '' : 's'} found` : ''}</div>
          {loading ? <Loading /> : !page || page.content.length === 0 ? (
            <div className="card"><Empty icon="search_off" title="No providers match these filters" /></div>
          ) : (
            <>
              <div className="grid grid-auto">{page.content.map((p) => <ProviderCard key={p.id} provider={p} base={base} onQuote={(x) => requestQuote({ sellerId: x.id })} />)}</div>
              <div className="card mt-16"><Pagination page={page} onPage={(p) => update('page', String(p))} /></div>
            </>
          )}
        </div>
      </div>
      {quoteModal}
    </div>
  );
}

export function ProviderDetail({ serviceBase = '/services', embedded }) {
  const { id } = useParams();
  const [profile, setProfile] = useState(null);
  const [missing, setMissing] = useState(false);
  const [summary, setSummary] = useState(null);
  const [reviews, setReviews] = useState(null);
  const [services, setServices] = useState([]);
  const [requestQuote, quoteModal] = useRequestQuote();
  useEffect(() => {
    setProfile(null);
    api.get(`/public/providers/${id}`).then(setProfile).catch(() => setMissing(true));
    api.get(`/public/reviews/sellers/${id}/summary`).then(setSummary).catch(() => {});
    api.get(`/public/reviews/sellers/${id}`, { size: 5 }).then(setReviews).catch(() => {});
    api.get('/public/services', { sellerId: id, size: 12 }).then((p) => setServices(p.content)).catch(() => {});
  }, [id]);
  if (missing) return <div className="container section"><div className="card"><Empty icon="person_off" title="Provider not available" /></div></div>;
  if (!profile) return <Loading />;
  const s = profile.summary;
  return (
    <div className={embedded ? '' : 'container'} style={{ paddingBottom: 56 }}>
      {!embedded && <div className="page-head"><div className="breadcrumbs"><Link to="/">Home</Link> / <Link to="/providers">Providers</Link> / {s.organisationName}</div></div>}
      <div className="layout-main-side">
        <div className="col gap-16">
          <Card>
            <div className="row top gap-16">
              <span className="avatar" style={{ width: 64, height: 64, fontSize: 22 }}>{s.organisationName.slice(0, 2).toUpperCase()}</span>
              <div className="grow">
                <h1 style={{ fontSize: 24 }}>{s.organisationName}</h1>
                <div className="row wrap gap-16 mt-8"><Verified /><span className="small muted">{label(s.providerType)}</span><Stars value={s.averageRating} count={s.ratingCount} /></div>
              </div>
            </div>
            {profile.introduction && <p className="pre mt-16">{profile.introduction}</p>}
            <div className="mt-16">
              <KeyValue items={[
                ['Location', [s.city || s.district, s.state].filter(Boolean).join(', ')],
                ['Experience', s.yearsExperience != null ? `${s.yearsExperience} years` : null],
                ['Team size', s.teamSize],
                ['Established', profile.yearEstablished],
                ['Constitution', label(profile.constitution)],
                ['Delivery modes', s.deliveryModes?.map(label).join(', ')],
                ['Website', profile.website && <a href={profile.website} target="_blank" rel="noreferrer">{profile.website}</a>],
                ['Verified on', date(profile.verifiedAt)]
              ]} />
            </div>
            {s.expertise?.length > 0 && <div className="mt-16"><h3>Expertise</h3><div className="chips mt-8">{s.expertise.map((e) => <span key={e} className="chip">{e}</span>)}</div></div>}
            {profile.industries?.length > 0 && <div className="mt-16"><h3>Industries served</h3><div className="chips mt-8">{profile.industries.map((e) => <span key={e} className="chip">{e}</span>)}</div></div>}
            {profile.certifications && <div className="mt-16"><h3>Certifications</h3><p className="pre small mt-4">{profile.certifications}</p></div>}
            {profile.teamProfile && <div className="mt-16"><h3>Team</h3><p className="pre small mt-4">{profile.teamProfile}</p></div>}
            {profile.previousAssignments && <div className="mt-16"><h3>Previous assignments</h3><p className="pre small mt-4">{profile.previousAssignments}</p></div>}
          </Card>
          <Card title={`Services (${services.length})`}>
            {services.length === 0 ? <p className="muted small">No published services.</p> : <div className="grid grid-2">{services.map((l) => <ListingCard key={l.id} listing={l} base={serviceBase} />)}</div>}
          </Card>
          <Card title="Ratings & feedback">
            {summary && summary.count > 0 && (
              <div className="row gap-16 mb-16"><Stars value={summary.average} count={summary.count} /></div>
            )}
            {!reviews || reviews.content.length === 0 ? <p className="muted small">No published feedback yet.</p> : (
              <div className="col">
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
          </Card>
        </div>
        <div className="col gap-16" style={{ position: 'sticky', top: 80 }}>
          <Card>
            <h3>Work with this provider</h3>
            <p className="small muted mt-8">Send a structured RFQ directly to {s.organisationName}.</p>
            <Button className="mt-16" block icon="request_quote" onClick={() => requestQuote({ sellerId: s.id })}>Request Quotation</Button>
          </Card>
          <Alert tone="info">Verification confirms provider credentials; it does not guarantee outcomes.</Alert>
          <div className="tiny muted mono">Ref: {s.referenceNo}</div>
        </div>
      </div>
      {quoteModal}
    </div>
  );
}
