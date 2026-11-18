import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { PortalLayout, PublicLayout } from './components/layouts';
import { Empty } from './components/ui';
import * as A from './pages/admin/Admin';
import * as O from './pages/admin/AdminOps';
import * as B from './pages/buyer/Buyer';
import { ForgotPassword, Login, RegisterBuyer, RegisterChoice, RegisterSeller } from './pages/public/Auth';
import Home from './pages/public/Home';
import { About, HowItWorks } from './pages/public/Info';
import { ProviderDetail, ProviderSearch } from './pages/public/Providers';
import Services, { ServiceDetail } from './pages/public/Services';
import * as S from './pages/seller/Seller';
import { AccountSettings, Notifications, QueryDetail, QueryList } from './pages/shared/Common';

function NotFound() {
  return (
    <div className="container" style={{ padding: '48px 0' }}>
      <Empty icon="explore_off" title="Page not found"><Link to="/">Back to home</Link></Empty>
    </div>
  );
}

const shared = (
  <>
    <Route path="notifications" element={<Notifications />} />
    <Route path="queries" element={<QueryList />} />
    <Route path="queries/:id" element={<QueryDetail />} />
  </>
);

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/register" element={<RegisterChoice />} />
      <Route path="/register/buyer" element={<RegisterBuyer />} />
      <Route path="/register/seller" element={<RegisterSeller />} />

      <Route element={<PublicLayout />}>
        <Route index element={<Home />} />
        <Route path="services" element={<Services />} />
        <Route path="services/:id" element={<ServiceDetail />} />
        <Route path="providers" element={<ProviderSearch />} />
        <Route path="providers/:id" element={<ProviderDetail />} />
        <Route path="how-it-works" element={<HowItWorks />} />
        <Route path="about" element={<About />} />
      </Route>

      <Route path="/seller" element={<PortalLayout role="SELLER" />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<S.SellerDashboard />} />
        <Route path="profile" element={<S.SellerProfile />} />
        <Route path="services" element={<S.SellerServices />} />
        <Route path="services/new" element={<S.SellerListingEdit />} />
        <Route path="services/:id" element={<S.SellerListingDetail />} />
        <Route path="services/:id/edit" element={<S.SellerListingEdit />} />
        <Route path="rfqs" element={<S.SellerRfqs />} />
        <Route path="rfqs/:id" element={<S.SellerRfqDetail />} />
        <Route path="rfqs/:id/quote" element={<S.SellerQuote />} />
        <Route path="quotations" element={<S.SellerQuotations />} />
        <Route path="quotations/:quotationId/edit" element={<S.SellerQuote />} />
        <Route path="quotations/:id" element={<S.SellerQuotationDetail />} />
        <Route path="work-orders" element={<S.SellerWorkOrders />} />
        <Route path="engagements" element={<S.SellerEngagements />} />
        <Route path="engagements/:id" element={<S.SellerEngagementDetail />} />
        <Route path="reviews" element={<S.SellerReviews />} />
        <Route path="settings" element={<AccountSettings />} />
        {shared}
      </Route>

      <Route path="/buyer" element={<PortalLayout role="BUYER" />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<B.BuyerDashboard />} />
        <Route path="find-services" element={<B.FindServices />} />
        <Route path="find-services/:id" element={<B.FindServices />} />
        <Route path="find-providers" element={<B.FindProviders />} />
        <Route path="find-providers/:id" element={<B.FindProviders />} />
        <Route path="rfqs" element={<B.BuyerRfqs />} />
        <Route path="rfqs/new" element={<B.BuyerRfqEdit />} />
        <Route path="rfqs/:id" element={<B.BuyerRfqDetail />} />
        <Route path="rfqs/:id/edit" element={<B.BuyerRfqEdit />} />
        <Route path="rfqs/:id/comparison" element={<B.BuyerComparison />} />
        <Route path="quotations" element={<B.BuyerQuotations />} />
        <Route path="quotations/:id" element={<B.BuyerQuotationDetail />} />
        <Route path="work-orders" element={<B.BuyerWorkOrders />} />
        <Route path="engagements" element={<B.BuyerEngagements />} />
        <Route path="engagements/:id" element={<B.BuyerEngagementDetail />} />
        <Route path="profile" element={<B.BuyerProfile />} />
        <Route path="settings" element={<AccountSettings />} />
        {shared}
      </Route>

      <Route path="/admin" element={<PortalLayout role="ADMIN" />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<A.AdminDashboard />} />
        <Route path="approvals" element={<A.UserList role="SELLER" title="Seller approvals" approvals />} />
        <Route path="approvals/:id" element={<A.UserDetail />} />
        <Route path="sellers" element={<A.UserList role="SELLER" title="Sellers" />} />
        <Route path="sellers/new" element={<A.OnboardUser role="SELLER" />} />
        <Route path="sellers/:id" element={<A.UserDetail />} />
        <Route path="buyers" element={<A.UserList role="BUYER" title="Purchasers" />} />
        <Route path="buyers/new" element={<A.OnboardUser role="BUYER" />} />
        <Route path="buyers/:id" element={<A.UserDetail />} />
        <Route path="admins" element={<A.UserList role="ADMIN" title="Administrators" />} />
        <Route path="admins/:id" element={<A.UserDetail />} />
        <Route path="categories" element={<O.Categories />} />
        <Route path="listings" element={<O.AdminListings />} />
        <Route path="listings/new" element={<O.AdminListingNew />} />
        <Route path="listings/:id" element={<O.AdminListingDetail />} />
        <Route path="rfqs" element={<O.AdminRfqs />} />
        <Route path="rfqs/:id" element={<O.AdminRfqDetail />} />
        <Route path="rfqs/:id/comparison" element={<B.BuyerComparison admin />} />
        <Route path="quotations" element={<O.AdminQuotations />} />
        <Route path="quotations/:id" element={<O.AdminQuotationDetail />} />
        <Route path="engagements" element={<O.AdminEngagements />} />
        <Route path="engagements/:id" element={<O.AdminEngagementDetail />} />
        <Route path="grievances" element={<QueryList admin />} />
        <Route path="grievances/:id" element={<QueryDetail admin />} />
        <Route path="queries/:id" element={<QueryDetail admin />} />
        <Route path="feedback" element={<O.AdminFeedback />} />
        <Route path="reports" element={<O.Reports />} />
        <Route path="audit-logs" element={<O.AuditLogs />} />
        <Route path="settings" element={<O.SystemSettings />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="account" element={<AccountSettings />} />
      </Route>

      <Route element={<PublicLayout />}>
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
