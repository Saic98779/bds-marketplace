import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { inr, initials, label, PRICING_LABELS } from '../constants';
import { Button, Icon, Modal, Stars, Verified } from './ui';

export function ListingCard({ listing, base = '/services', onQuote }) {
  return (
    <div className="card listing-card">
      <div className="card-body">
        <div className="row between">
          <span className="cat">{listing.categoryName}</span>
          {listing.featured && <span className="badge dark"><Icon name="star" size="sm" fill />Featured</span>}
        </div>
        <h3><Link to={`${base}/${listing.id}`}>{listing.title}</Link></h3>
        <p className="desc">{listing.shortDescription}</p>
        <div className="row gap-8 wrap">
          <span className="small bold">{listing.sellerName}</span>
          {listing.sellerVerified && <Verified />}
        </div>
        <Stars value={listing.sellerRating} count={listing.sellerRatingCount} />
        <div className="meta">
          <span><Icon name="devices" size="sm" />{label(listing.deliveryMode)}</span>
          {listing.state && <span><Icon name="location_on" size="sm" />{listing.state}</span>}
          {listing.experienceYears != null && <span><Icon name="work_history" size="sm" />{listing.experienceYears} yrs</span>}
          {listing.estimatedDuration && <span><Icon name="schedule" size="sm" />{listing.estimatedDuration}</span>}
        </div>
      </div>
      <div className="card-footer">
        <div>
          <div className="fee">{listing.indicativeFee ? inr(listing.indicativeFee) : 'On request'}</div>
          <div className="tiny muted">{PRICING_LABELS[listing.pricingModel]}</div>
        </div>
        <div className="row gap-8">
          <Link className="btn btn-outline btn-sm" to={`${base}/${listing.id}`}>Details</Link>
          {onQuote && <Button size="sm" onClick={() => onQuote(listing)}>Request Quotation</Button>}
        </div>
      </div>
    </div>
  );
}

export function ProviderCard({ provider, base = '/providers', onQuote }) {
  return (
    <div className="card listing-card">
      <div className="card-body">
        <div className="row top gap-16">
          <span className="avatar">{initials(provider.organisationName)}</span>
          <div className="grow">
            <h3><Link to={`${base}/${provider.id}`}>{provider.organisationName}</Link></h3>
            <div className="row gap-8 wrap mt-4">
              <Verified />
              <span className="tiny muted">{label(provider.providerType)}</span>
            </div>
          </div>
        </div>
        <Stars value={provider.averageRating} count={provider.ratingCount} />
        {provider.expertise?.length > 0 && (
          <div className="chips">{provider.expertise.slice(0, 4).map((e) => <span key={e} className="chip">{e}</span>)}</div>
        )}
        <div className="meta">
          {provider.yearsExperience != null && <span><Icon name="work_history" size="sm" />{provider.yearsExperience} yrs experience</span>}
          <span><Icon name="location_on" size="sm" />{[provider.city || provider.district, provider.state].filter(Boolean).join(', ')}</span>
          {provider.deliveryModes?.length > 0 && <span><Icon name="devices" size="sm" />{provider.deliveryModes.map(label).join(' / ')}</span>}
        </div>
      </div>
      <div className="card-footer">
        <Link className="btn btn-outline btn-sm" to={`${base}/${provider.id}`}>View Profile</Link>
        {onQuote && <Button size="sm" onClick={() => onQuote(provider)}>Request Quotation</Button>}
      </div>
    </div>
  );
}

export function useRequestQuote() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState(false);
  const request = ({ listingId, sellerId }) => {
    const query = listingId ? `listingId=${listingId}` : `sellerId=${sellerId}`;
    if (user?.role === 'BUYER') {
      navigate(`/buyer/rfqs/new?${query}`);
    } else if (user) {
      setPrompt('role');
    } else {
      setPrompt(query);
    }
  };
  const modal = prompt && (
    <Modal title={prompt === 'role' ? 'Purchaser account required' : 'Sign in to request a quotation'} onClose={() => setPrompt(false)}
      footer={prompt === 'role' ? <Button onClick={() => setPrompt(false)}>OK</Button> : (
        <>
          <Link className="btn btn-outline" to="/register/buyer">Register as Purchaser</Link>
          <Link className="btn btn-primary" to={`/login?role=BUYER&next=${encodeURIComponent(`/buyer/rfqs/new?${prompt}`)}`}>Sign in</Link>
        </>
      )}>
      {prompt === 'role'
        ? <p>Quotations can be requested from a purchaser account. Please sign in as a purchaser.</p>
        : <p>Browse freely without an account. To share your requirement and receive quotations from verified providers, sign in or register as a purchaser. Registration is free.</p>}
    </Modal>
  );
  return [request, modal];
}
