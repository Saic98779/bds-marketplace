import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { homeFor, resolveLink, useAuth } from '../auth/AuthContext';
import { ago, initials, label } from '../constants';
import { Alert, Button, Icon } from './ui';

export function Brand({ to = '/' }) {
  return (
    <Link to={to} className="brand">
      <span className="brand-mark">BDS</span>
      <span>BDS Marketplace<small>MSME Services</small></span>
    </Link>
  );
}

let settingsCache = null;
export function usePublicSettings() {
  const [settings, setSettings] = useState(settingsCache || {});
  useEffect(() => {
    if (settingsCache) return;
    api.get('/public/settings').then((s) => { settingsCache = s; setSettings(s); }).catch(() => {});
  }, []);
  return settings;
}

export function PublicLayout() {
  const { isAuthenticated, user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const settings = usePublicSettings();
  const location = useLocation();
  useEffect(() => { setOpen(false); window.scrollTo(0, 0); }, [location.pathname]);
  return (
    <>
      {settings['announcement.text'] && (
        <div style={{ background: 'var(--accent)', color: '#fff', textAlign: 'center', padding: '6px 12px', fontSize: 13 }}>{settings['announcement.text']}</div>
      )}
      <header className="public-header">
        <div className="container bar">
          <Brand />
          <nav className={`public-nav ${open ? 'open' : ''}`} aria-label="Main">
            <NavLink to="/" end>Home</NavLink>
            <NavLink to="/services">Services</NavLink>
            <NavLink to="/providers">Find Providers</NavLink>
            <NavLink to="/how-it-works">How It Works</NavLink>
            <NavLink to="/about">About</NavLink>
          </nav>
          <div className="row gap-8">
            {isAuthenticated ? (
              <>
                <Link className="btn btn-primary btn-sm" to={homeFor(user.role)}><Icon name="dashboard" size="sm" />My Workspace</Link>
                <Button variant="outline" size="sm" onClick={logout}>Sign out</Button>
              </>
            ) : (
              <>
                <Link className="btn btn-outline btn-sm" to="/login">Login</Link>
                <Link className="btn btn-accent btn-sm" to="/register">Register</Link>
              </>
            )}
            <button className="btn btn-ghost btn-icon menu-toggle" style={{ color: '#fff' }} onClick={() => setOpen(!open)} aria-label="Menu"><Icon name="menu" /></button>
          </div>
        </div>
      </header>
      <main><Outlet /></main>
      <footer className="public-footer">
        <div className="container">
          <div className="grid grid-4">
            <div>
              <Brand />
              <p className="small mt-16">A verified digital marketplace connecting MSMEs with approved business development service providers.</p>
              <p className="small mt-8"><Icon name="mail" size="sm" /> {settings['support.email'] || 'support@bdsmarketplace.in'}</p>
              <p className="small"><Icon name="call" size="sm" /> {settings['support.phone'] || '1800-226-753'}</p>
              <p className="small"><Icon name="schedule" size="sm" /> {settings['support.hours'] || 'Mon-Sat, 9:30 am - 6:00 pm'}</p>
            </div>
            <div>
              <h4>Marketplace</h4>
              <Link to="/services">Browse Services</Link>
              <Link to="/providers">Find Providers</Link>
              <Link to="/register/seller">Become a Provider</Link>
              <Link to="/register/buyer">Register as Purchaser</Link>
            </div>
            <div>
              <h4>Help</h4>
              <Link to="/how-it-works">How It Works</Link>
              <Link to="/about#faq">FAQs</Link>
              <Link to="/about#grievance">Grievance Redressal</Link>
              <Link to="/about#contact">Contact Support</Link>
            </div>
            <div>
              <h4>Legal</h4>
              <Link to="/about#terms">Terms of Use</Link>
              <Link to="/about#privacy">Privacy Policy</Link>
              <Link to="/about#disclaimer">Disclaimer</Link>
            </div>
          </div>
          <div className="disclaimer">
            {settings['disclaimer.payments'] || 'BDS Marketplace does not collect, hold or process payments and does not provide escrow.'} Provider verification does not certify every listed service or guarantee outcomes.
            <div className="mt-8">© {new Date().getFullYear()} BDS Marketplace. An initiative for MSME business development.</div>
          </div>
        </div>
      </footer>
    </>
  );
}

const NAV = {
  SELLER: [
    { to: '/seller/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { to: '/seller/profile', icon: 'business', label: 'Business Profile' },
    { to: '/seller/services', icon: 'storefront', label: 'My Services' },
    { group: 'Business' },
    { to: '/seller/rfqs', icon: 'inbox', label: 'RFQ Inbox' },
    { to: '/seller/quotations', icon: 'request_quote', label: 'Quotations' },
    { to: '/seller/work-orders', icon: 'assignment', label: 'Work Orders' },
    { to: '/seller/engagements', icon: 'handshake', label: 'Engagements' },
    { to: '/seller/queries', icon: 'forum', label: 'Query Tracker' },
    { to: '/seller/reviews', icon: 'reviews', label: 'Ratings & Reviews' },
    { group: 'Account' },
    { to: '/seller/notifications', icon: 'notifications', label: 'Notifications' },
    { to: '/seller/settings', icon: 'settings', label: 'Settings' }
  ],
  BUYER: [
    { to: '/buyer/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { to: '/buyer/find-services', icon: 'search', label: 'Find Services' },
    { to: '/buyer/find-providers', icon: 'groups', label: 'Find Providers' },
    { group: 'Procurement' },
    { to: '/buyer/rfqs', icon: 'description', label: 'My RFQs' },
    { to: '/buyer/quotations', icon: 'request_quote', label: 'Quotations' },
    { to: '/buyer/work-orders', icon: 'assignment', label: 'Work Orders' },
    { to: '/buyer/engagements', icon: 'handshake', label: 'Engagements' },
    { to: '/buyer/queries', icon: 'forum', label: 'Query Tracker' },
    { group: 'Account' },
    { to: '/buyer/profile', icon: 'apartment', label: 'Organisation Profile' },
    { to: '/buyer/notifications', icon: 'notifications', label: 'Notifications' },
    { to: '/buyer/settings', icon: 'settings', label: 'Settings' }
  ],
  ADMIN: [
    { to: '/admin/dashboard', icon: 'dashboard', label: 'Dashboard', perm: 'ADMIN_DASHBOARD' },
    { group: 'Onboarding' },
    { to: '/admin/approvals', icon: 'how_to_reg', label: 'Seller Approvals', perm: 'SELLER_VERIFY' },
    { to: '/admin/sellers', icon: 'store', label: 'Sellers', perm: 'SELLER_VERIFY' },
    { to: '/admin/buyers', icon: 'corporate_fare', label: 'Purchasers', perm: 'BUYER_MANAGE' },
    { to: '/admin/admins', icon: 'admin_panel_settings', label: 'Administrators', perm: 'USER_ADMIN' },
    { group: 'Catalogue' },
    { to: '/admin/categories', icon: 'category', label: 'Categories', perm: 'CATALOG_MANAGE' },
    { to: '/admin/listings', icon: 'fact_check', label: 'Service Listings', perm: 'LISTING_MODERATE' },
    { group: 'Procurement' },
    { to: '/admin/rfqs', icon: 'description', label: 'RFQs', perm: 'RFQ_MONITOR' },
    { to: '/admin/quotations', icon: 'request_quote', label: 'Quotations', perm: 'RFQ_MONITOR' },
    { to: '/admin/engagements', icon: 'handshake', label: 'Engagements', perm: 'RFQ_MONITOR' },
    { to: '/admin/grievances', icon: 'support_agent', label: 'Grievances', perm: 'GRIEVANCE_MANAGE' },
    { to: '/admin/feedback', icon: 'reviews', label: 'Feedback & Reviews', perm: 'FEEDBACK_MANAGE' },
    { group: 'Governance' },
    { to: '/admin/reports', icon: 'bar_chart', label: 'Reports & MIS', perm: 'REPORT_VIEW' },
    { to: '/admin/audit-logs', icon: 'history', label: 'Audit Logs', perm: 'AUDIT_VIEW' },
    { to: '/admin/settings', icon: 'tune', label: 'System Settings', perm: 'SETTINGS_MANAGE' },
    { to: '/admin/notifications', icon: 'notifications', label: 'Notifications' },
    { to: '/admin/account', icon: 'manage_accounts', label: 'My Account' }
  ]
};

function NotificationBell() {
  const { role } = useAuth();
  const navigate = useNavigate();
  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const ref = useRef(null);

  const refresh = () => api.get('/notifications/unread-count').then((d) => setCount(d.unread)).catch(() => {});
  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 30000);
    return () => clearInterval(t);
  }, []);
  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);
  const toggle = async () => {
    if (!open) {
      const page = await api.get('/notifications', { size: 8 }).catch(() => ({ content: [] }));
      setItems(page.content);
    }
    setOpen(!open);
  };
  const openItem = async (n) => {
    setOpen(false);
    if (!n.read) {
      await api.post(`/notifications/${n.id}/read`).catch(() => {});
      refresh();
    }
    const link = resolveLink(n.linkUrl, role);
    if (link) navigate(link);
  };
  const prefix = role === 'ADMIN' ? '/admin' : role === 'SELLER' ? '/seller' : '/buyer';
  return (
    <div className="bell" ref={ref} style={{ position: 'relative' }}>
      <button className="btn btn-ghost btn-icon" onClick={toggle} aria-label={`Notifications, ${count} unread`}>
        <Icon name="notifications" />
        {count > 0 && <span className="dot">{count > 99 ? '99+' : count}</span>}
      </button>
      {open && (
        <div className="dropdown">
          <div className="row between" style={{ padding: '12px 16px', borderBottom: '1px solid var(--line-100)' }}>
            <h4>Notifications</h4>
            <button className="btn btn-ghost btn-sm" onClick={async () => { await api.post('/notifications/read-all'); setItems(items.map((i) => ({ ...i, read: true }))); refresh(); }}>Mark all read</button>
          </div>
          {items.length === 0 && <div className="empty small">You are all caught up.</div>}
          <div style={{ maxHeight: 380, overflowY: 'auto' }}>
            {items.map((n) => (
              <div key={n.id} className={`item ${n.read ? '' : 'unread'}`} onClick={() => openItem(n)}>
                <Icon name={n.read ? 'notifications' : 'notifications_active'} className={n.read ? 'muted' : ''} />
                <div className="grow">
                  <div className="small bold">{n.title}</div>
                  <div className="tiny muted" style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{n.message}</div>
                  <div className="tiny muted mt-4">{ago(n.createdAt)}</div>
                </div>
              </div>
            ))}
          </div>
          <div style={{ padding: 10, textAlign: 'center' }}><Link to={`${prefix}/notifications`} onClick={() => setOpen(false)} className="small bold">View all</Link></div>
        </div>
      )}
    </div>
  );
}

function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);
  const settingsPath = user.role === 'ADMIN' ? '/admin/account' : user.role === 'SELLER' ? '/seller/settings' : '/buyer/settings';
  return (
    <div className="user-chip" ref={ref} onClick={() => setOpen(!open)}>
      <span className="avatar">{initials(user.fullName)}</span>
      <div className="small" style={{ lineHeight: 1.2 }}>
        <div className="bold">{user.fullName}</div>
        <div className="tiny muted">{user.organisationName || label(user.role)}</div>
      </div>
      <Icon name="expand_more" size="sm" />
      {open && (
        <div className="dropdown" style={{ width: 240, top: 48 }}>
          <div className="item" style={{ cursor: 'default' }}>
            <div>
              <div className="small bold">{user.email}</div>
              <div className="tiny muted mono">{user.referenceNo}</div>
            </div>
          </div>
          <div className="item" onClick={() => navigate(settingsPath)}><Icon name="settings" /> Account settings</div>
          <div className="item" onClick={() => navigate('/')}><Icon name="public" /> Public website</div>
          <div className="item" onClick={async () => { await logout(); navigate('/login'); }}><Icon name="logout" /> Sign out</div>
        </div>
      )}
    </div>
  );
}

export function PortalLayout({ role, title }) {
  const { user, isAuthenticated, can } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  useEffect(() => setOpen(false), [location.pathname]);
  if (!isAuthenticated) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />;
  if (user.role !== role) return <Navigate to={homeFor(user.role)} replace />;
  if (user.mustChangePassword && !location.pathname.endsWith('/settings') && !location.pathname.endsWith('/account')) {
    return <Navigate to={role === 'ADMIN' ? '/admin/account?force=1' : `/${role.toLowerCase()}/settings?force=1`} replace />;
  }
  const items = NAV[role].filter((i) => !i.perm || can(i.perm));
  const current = items.find((i) => i.to && location.pathname.startsWith(i.to));
  const roleLabel = { SELLER: 'Seller Workspace', BUYER: 'Purchaser Workspace', ADMIN: 'Administrator Portal' }[role];
  return (
    <div className="portal">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <Brand to={homeFor(role)} />
        <div className="role">{roleLabel}</div>
        <nav className="side-nav" aria-label="Portal">
          {items.map((i, idx) => i.group
            ? <div key={`g${idx}`} className="group">{i.group}</div>
            : <NavLink key={i.to} to={i.to}><Icon name={i.icon} />{i.label}</NavLink>)}
        </nav>
        <div className="foot">Payments are agreed and made outside the platform.</div>
      </aside>
      <div className="portal-main">
        <header className="topbar">
          <button className="btn btn-ghost btn-icon menu-toggle" onClick={() => setOpen(!open)} aria-label="Menu"><Icon name="menu" /></button>
          <div className="title">{current?.label || title || roleLabel}</div>
          <NotificationBell />
          <UserMenu />
        </header>
        {user.status !== 'ACTIVE' && <StatusBanner user={user} />}
        <div className="portal-content"><Outlet /></div>
      </div>
    </div>
  );
}

function StatusBanner({ user }) {
  const { reissue } = useAuth();
  const messages = {
    PENDING_VERIFICATION: ['warn', 'Your account is pending verification', 'You can complete your profile and documents. Publishing, RFQs and quotations are enabled after administrator approval.'],
    INFO_REQUESTED: ['warn', 'Additional information requested', user.statusRemarks || 'Update your profile or documents and resubmit for verification.'],
    SUSPENDED: ['bad', 'Your account is suspended', `${user.statusRemarks || 'Contact support for details.'} New service publication and quotation submission are restricted.`]
  };
  const [tone, title, text] = messages[user.status] || ['info', label(user.status), ''];
  return (
    <div className="status-banner">
      <Alert tone={tone} title={title} action={<Button variant="ghost" size="sm" icon="refresh" onClick={reissue}>Refresh status</Button>}>{text}</Alert>
    </div>
  );
}

export function PageTitle({ title, subtitle, actions, back }) {
  const navigate = useNavigate();
  return (
    <div className="page-title">
      <div className="row top gap-8">
        {back && <button className="btn btn-ghost btn-icon" onClick={() => navigate(-1)} aria-label="Back"><Icon name="arrow_back" /></button>}
        <div>
          <h1 style={{ fontSize: 24 }}>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="row wrap gap-8">{actions}</div>}
    </div>
  );
}
