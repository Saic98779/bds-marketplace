export const STATES = [
  'Andaman and Nicobar Islands', 'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chandigarh', 'Chhattisgarh',
  'Dadra and Nagar Haveli and Daman and Diu', 'Delhi', 'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jammu and Kashmir',
  'Jharkhand', 'Karnataka', 'Kerala', 'Ladakh', 'Lakshadweep', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya',
  'Mizoram', 'Nagaland', 'Odisha', 'Puducherry', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura',
  'Uttar Pradesh', 'Uttarakhand', 'West Bengal'
];

export const INDUSTRIES = [
  'Manufacturing', 'Food Processing', 'Textiles & Apparel', 'Engineering', 'Chemicals', 'Pharmaceuticals', 'Automotive Components',
  'Electronics', 'Agriculture & Allied', 'Handicrafts', 'Leather', 'Plastics & Packaging', 'IT & ITeS', 'Logistics', 'Retail & Trade',
  'Healthcare', 'Education', 'Hospitality & Tourism', 'Construction', 'Renewable Energy', 'Services', 'Other'
];

export const label = (value) => {
  if (value === null || value === undefined || value === '') return '-';
  return String(value).replace(/_/g, ' ').toLowerCase().replace(/(^|\s)\S/g, (c) => c.toUpperCase());
};

export const CONSTITUTIONS = ['PROPRIETORSHIP', 'PARTNERSHIP', 'LLP', 'PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'SOCIETY_TRUST_SECTION8', 'INDIVIDUAL_PROFESSIONAL', 'OTHER'];
export const CONSTITUTION_LABELS = {
  PROPRIETORSHIP: 'Proprietorship', PARTNERSHIP: 'Partnership', LLP: 'LLP', PRIVATE_LIMITED: 'Private Limited Company',
  PUBLIC_LIMITED: 'Public Limited Company', SOCIETY_TRUST_SECTION8: 'Society / Trust / Section 8 Company',
  INDIVIDUAL_PROFESSIONAL: 'Individual Professional', OTHER: 'Other eligible organisation'
};
export const PROVIDER_TYPES = ['CONSULTANCY_FIRM', 'INDIVIDUAL_CONSULTANT', 'CHARTERED_ACCOUNTANT_FIRM', 'LEGAL_FIRM', 'TECHNOLOGY_COMPANY', 'MARKETING_AGENCY', 'TRAINING_INSTITUTION', 'CERTIFICATION_BODY', 'OTHER'];
export const PURCHASER_TYPES = ['MICRO', 'SMALL', 'MEDIUM', 'STARTUP', 'INDUSTRY_ASSOCIATION', 'LARGE_ENTERPRISE', 'INSTITUTIONAL', 'OTHER'];
export const PURCHASER_LABELS = {
  MICRO: 'Micro Enterprise', SMALL: 'Small Enterprise', MEDIUM: 'Medium Enterprise', STARTUP: 'Startup / Entrepreneur',
  INDUSTRY_ASSOCIATION: 'Industry Association', LARGE_ENTERPRISE: 'Large Enterprise', INSTITUTIONAL: 'Institutional Purchaser', OTHER: 'Other eligible organisation'
};
export const DOCUMENT_TYPES = ['PAN', 'REGISTRATION_CERTIFICATE', 'GST_CERTIFICATE', 'UDYAM_CERTIFICATE', 'PROFESSIONAL_CREDENTIAL', 'PORTFOLIO', 'OTHER'];
export const DELIVERY_MODES = ['ONLINE', 'ONSITE', 'HYBRID'];
export const PRICING_MODELS = ['FIXED_FEE', 'PER_DAY', 'PER_MONTH', 'PER_ASSIGNMENT', 'HOURLY', 'CUSTOM_QUOTATION'];
export const PRICING_LABELS = {
  FIXED_FEE: 'Fixed fee', PER_DAY: 'Per day', PER_MONTH: 'Per month', PER_ASSIGNMENT: 'Per assignment', HOURLY: 'Hourly', CUSTOM_QUOTATION: 'Custom quotation'
};

export const STATUS_TONE = {
  ACTIVE: 'good', APPROVED: 'good', PUBLISHED: 'info', COMPLETED: 'good', ACCEPTED: 'good', ISSUED: 'good', ACKNOWLEDGED: 'good',
  RESOLVED: 'good', CLOSED: '', PROVIDER_SELECTED: 'good', SHORTLISTED: 'info', RESPONDED: 'info', SENT: 'good',
  PENDING_VERIFICATION: 'warn', INFO_REQUESTED: 'warn', SUBMITTED: 'info', UNDER_REVIEW: 'warn', UNDER_EVALUATION: 'warn',
  RESPONSES_RECEIVED: 'info', CLARIFICATION_REQUESTED: 'warn', CORRECTION_REQUESTED: 'warn', REVISED: 'info', REQUESTED: 'warn',
  OPEN: 'warn', REOPENED: 'warn', DELIVERED: 'info', IN_PROGRESS: 'info', ENGAGEMENT_INITIATED: 'info', SELECTED: 'info',
  INVITED: 'info', VIEWED: '', DRAFT: '', ESCALATED: 'bad', DISPUTED: 'bad',
  SUSPENDED: 'bad', REJECTED: 'bad', DEACTIVATED: 'bad', CANCELLED: 'bad', FLAGGED: 'bad', DECLINED: 'bad', WITHDRAWN: 'bad',
  EXPIRED: 'bad', NO_RESPONSE: 'bad', FAILED: 'bad', HIGH: 'bad', MEDIUM: 'warn', LOW: ''
};

export const STATUS_ICON = { good: 'check_circle', warn: 'schedule', bad: 'error', info: 'info', '': 'radio_button_unchecked' };

export const ADMIN_PERMISSIONS = ['ADMIN_DASHBOARD', 'SELLER_VERIFY', 'BUYER_MANAGE', 'USER_ADMIN', 'CATALOG_MANAGE', 'LISTING_MODERATE',
  'RFQ_MONITOR', 'GRIEVANCE_MANAGE', 'FEEDBACK_MANAGE', 'REPORT_VIEW', 'AUDIT_VIEW', 'SETTINGS_MANAGE'];

export const inr = (value) => {
  if (value === null || value === undefined || value === '') return '-';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(value));
};

export const date = (value) => {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const dateTime = (value) => {
  if (!value) return '-';
  return new Date(value).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

export const ago = (value) => {
  if (!value) return '';
  const diff = (Date.now() - new Date(value).getTime()) / 1000;
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} h ago`;
  if (diff < 604800) return `${Math.floor(diff / 86400)} d ago`;
  return date(value);
};

export const initials = (name) => (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]).join('').toUpperCase();

export const toLocalInput = (d) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

export const today = () => new Date().toISOString().slice(0, 10);
export const plusDays = (n) => {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
};
