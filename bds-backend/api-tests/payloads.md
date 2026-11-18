# bds-backend API smoke test — payloads

- Target: `http://localhost:8085`
- Run: `3336` at 2026-11-18 14:14
- Result: **54/54 passed**

## Onboarded accounts

| Account | Email | Password | userId | referenceNo |
|---|---|---|---|---|
| Seller (self-registered) | seller.self.3336@example.com | `<redacted>` | 4 | BDS-SLR-000004 |
| Buyer (self-registered) | buyer.self.3336@example.com | `<redacted>` | 5 | BDS-PUR-000005 |
| Seller (admin-onboarded) | seller.admin.3336@example.com | `<redacted>` | 6 | BDS-SLR-000006 |
| Buyer (admin-onboarded) | buyer.admin.3336@example.com | `<redacted>` | 7 | BDS-PUR-000007 |

## Summary

| # | Result | Status | Method | Path | Step |
|---|---|---|---|---|---|
| 1 | PASS | 200 | GET | `/actuator/health` | Health |
| 2 | PASS | 200 | GET | `/api/v1/auth/public-key` | Transport public key |
| 3 | PASS | 200 | GET | `/api/v1/auth/meta` | Onboarding reference data |
| 4 | PASS | 200 | GET | `/api/v1/public/categories` | Public categories |
| 5 | PASS | 200 | GET | `/api/v1/public/services?page=0&size=5` | Public services |
| 6 | PASS | 200 | GET | `/api/v1/public/catalog/stats` | Public catalog stats |
| 7 | PASS | 200 | GET | `/api/v1/public/providers?page=0&size=5` | Public providers |
| 8 | PASS | 200 | GET | `/api/v1/public/settings` | Public settings |
| 9 | PASS | 401 | GET | `/api/v1/auth/me` | Protected endpoint without token -> 401 |
| 10 | PASS | 401 | POST | `/api/v1/auth/login` | Login with wrong password -> 401 |
| 11 | PASS | 400 | POST | `/api/v1/auth/register/seller` | Seller registration with invalid PAN -> 400 |
| 12 | PASS | 200 | POST | `/api/v1/auth/login` | Login (ADMIN) |
| 13 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (ADMIN) |
| 14 | PASS | 200 | GET | `/api/v1/auth/me` | Admin: current user |
| 15 | PASS | 200 | GET | `/api/v1/admin/identity/stats` | Admin: identity stats |
| 16 | PASS | 200 | POST | `/api/v1/auth/otp/send` | Send registration OTP (seller) |
| 17 | PASS | 201 | POST | `/api/v1/auth/register/seller` | Self-register seller (multipart with documents) |
| 18 | PASS | 200 | POST | `/api/v1/auth/otp/send` | Send registration OTP (buyer) |
| 19 | PASS | 201 | POST | `/api/v1/auth/register/buyer` | Self-register buyer |
| 20 | PASS | 201 | POST | `/api/v1/admin/users/sellers` | Admin onboards seller |
| 21 | PASS | 201 | POST | `/api/v1/admin/users/buyers` | Admin onboards buyer |
| 22 | PASS | 200 | GET | `/api/v1/admin/users?status=PENDING_VERIFICATION&page=0&size=10` | Admin: list pending users |
| 23 | PASS | 200 | GET | `/api/v1/admin/users/4` | Admin: seller application detail |
| 24 | PASS | 200 | PUT | `/api/v1/admin/users/4/documents/2/verify` | Admin: verify document REGISTRATION_CERTIFICATE |
| 25 | PASS | 200 | PUT | `/api/v1/admin/users/4/documents/1/verify` | Admin: verify document PAN |
| 26 | PASS | 200 | POST | `/api/v1/admin/users/4/approve` | Admin: approve self-registered seller |
| 27 | PASS | 200 | POST | `/api/v1/admin/users/5/approve` | Admin: approve self-registered buyer |
| 28 | PASS | 200 | POST | `/api/v1/auth/login` | Login (SELLER) |
| 29 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (SELLER) |
| 30 | PASS | 200 | GET | `/api/v1/sellers/me/profile` | Seller: profile |
| 31 | PASS | 200 | GET | `/api/v1/sellers/me/verification` | Seller: verification status |
| 32 | PASS | 201 | POST | `/api/v1/seller/services` | Seller: create & submit service listing |
| 33 | PASS | 200 | GET | `/api/v1/seller/services?page=0&size=10` | Seller: my listings |
| 34 | PASS | 200 | POST | `/api/v1/admin/services/4/start-review` | Admin: start listing review |
| 35 | PASS | 200 | POST | `/api/v1/admin/services/4/approve` | Admin: approve listing |
| 36 | PASS | 200 | GET | `/api/v1/public/services/4` | Public: listing detail |
| 37 | PASS | 200 | POST | `/api/v1/auth/login` | Login (BUYER) |
| 38 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (BUYER) |
| 39 | PASS | 200 | GET | `/api/v1/buyers/me/profile` | Buyer: profile |
| 40 | PASS | 200 | GET | `/api/v1/buyer/dashboard` | Buyer: dashboard |
| 41 | PASS | 201 | POST | `/api/v1/buyer/rfqs` | Buyer: create & publish RFQ |
| 42 | PASS | 200 | GET | `/api/v1/seller/rfqs?page=0&size=10` | Seller: invited RFQs |
| 43 | PASS | 200 | GET | `/api/v1/seller/rfqs/1` | Seller: RFQ detail |
| 44 | PASS | 201 | POST | `/api/v1/seller/rfqs/1/quotations` | Seller: submit quotation |
| 45 | PASS | 200 | GET | `/api/v1/buyer/rfqs/1/quotations` | Buyer: quotations for RFQ |
| 46 | PASS | 200 | GET | `/api/v1/buyer/rfqs/1/comparison` | Buyer: comparison |
| 47 | PASS | 200 | POST | `/api/v1/buyer/quotations/1/shortlist` | Buyer: shortlist quotation |
| 48 | PASS | 200 | POST | `/api/v1/buyer/quotations/1/accept` | Buyer: accept quotation |
| 49 | PASS | 200 | GET | `/api/v1/buyer/engagements?page=0&size=10` | Buyer: engagements |
| 50 | PASS | 200 | POST | `/api/v1/auth/login` | Login (SELLER) |
| 51 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (SELLER) |
| 52 | PASS | 200 | POST | `/api/v1/auth/login` | Login (BUYER) |
| 53 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (BUYER) |
| 54 | PASS | 200 | GET | `/api/v1/admin/procurement/stats` | Admin: procurement stats |

## Request / response payloads

### 1. Health

`GET /actuator/health`

Response `200`:
```json
{
  "status": "UP"
}
```

### 2. Transport public key

`GET /api/v1/auth/public-key`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "keyId": "563cd5a3",
    "algorithm": "RSA-OAEP-256",
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu8Yt+rXHGgZpiDFR2RAtKOQQCm+t4bbYaW65UMtYzBRlES52wZO6Nwu+DnRAeJO45bTaMkaNAGo6o4sAcJvDls3D7RuLsPcmpt10o9uP341AdR4SZvt/z4xuPz7QXsaYrNL5RFP0G8vIbU9wtyNyd0QLwm57rPrVmeCNthhKn3j1AryJcnABZlcFLLOz0Qb7LUVSheX3FOrC5kWimdVJLqvrE97DWsp+OvF92EbJ0D5UMCs0vvtyevPZHe/Qs7a4tq2RVxj+dSnLviEzTPqd6ItG5lvggm/HD9AKHs92Ibdz8EQHxmIQahWhkqoFhxqUQwm0q04kBYE53hovTiRNcwIDAQAB"
  },
  "timestamp": "2026-11-18T08:44:08.774795500Z"
}
```

### 3. Onboarding reference data

`GET /api/v1/auth/meta`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "constitutions": [
      "PROPRIETORSHIP",
      "PARTNERSHIP",
      "LLP",
      "PRIVATE_LIMITED",
      "PUBLIC_LIMITED",
      "SOCIETY_TRUST_SECTION8",
      "INDIVIDUAL_PROFESSIONAL",
      "OTHER"
    ],
    "providerTypes": [
      "CONSULTANCY_FIRM",
      "INDIVIDUAL_CONSULTANT",
      "CHARTERED_ACCOUNTANT_FIRM",
      "LEGAL_FIRM",
      "TECHNOLOGY_COMPANY",
      "MARKETING_AGENCY",
      "TRAINING_INSTITUTION",
      "CERTIFICATION_BODY",
      "OTHER"
    ],
    "purchaserTypes": [
      "MICRO",
      "SMALL",
      "MEDIUM",
      "STARTUP",
      "INDUSTRY_ASSOCIATION",
      "LARGE_ENTERPRISE",
      "INSTITUTIONAL",
      "OTHER"
    ],
    "documentTypes": [
      "PAN",
      "REGISTRATION_CERTIFICATE",
      "GST_CERTIFICATE",
      "UDYAM_CERTIFICATE",
      "PROFESSIONAL_CREDENTIAL",
      "PORTFOLIO",
      "OTHER"
    ],
    "mandatoryDocuments": {
      "CONSULTANCY_FIRM": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "INDIVIDUAL_CONSULTANT": [
        "PAN",
        "PROFESSIONAL_CREDENTIAL"
      ],
      "CHARTERED_ACCOUNTANT_FIRM": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "LEGAL_FIRM": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "TECHNOLOGY_COMPANY": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "MARKETING_AGENCY": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "TRAINING_INSTITUTION": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "CERTIFICATION_BODY": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ],
      "OTHER": [
        "PAN",
        "REGISTRATION_CERTIFICATE"
      ]
    }
  },
  "timestamp": "2026-11-18T08:44:08.806971300Z"
}
```

### 4. Public categories

`GET /api/v1/public/categories`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "code": "CONSULTANCY",
      "name": "Business & Management Consultancy",
      "description": "Business plans, DPRs, project reports, feasibility studies, strategy and process advisory",
      "icon": "business_center",
      "displayOrder": 1,
      "active": true,
      "mandatoryFields": [],
      "listingCount": 1,
      "subcategories": [
        {
          "id": 1,
          "name": "Detailed Project Reports",
          "displayOrder": 1,
          "active": true,
          "listingCount": 1
        },
        {
          "id": 2,
          "name": "Business Plans",
          "displayOrder": 2,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 3,
          "name": "Feasibility Studies",
          "displayOrder": 3,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 4,
          "name": "Strategy Advisory",
          "displayOrder": 4,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 5,
          "name": "Process Advisory",
          "displayOrder": 5,
          "active": true,
          "listingCount": 0
        }
      ]
    },
    {
      "id": 2,
      "code": "FINANCE",
      "name": "Finance & Credit Facilitation",
      "description": "Loan documentation, financial modelling, subsidy guidance and credit facilitation",
      "icon": "account_balance",
      "displayOrder": 2,
      "active": true,
      "mandatoryFields": [],
      "listingCount": 2,
      "subcategories": [
        {
          "id": 6,
          "name": "Loan Documentation",
          "displayOrder": 1,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 7,
          "name": "Financial Modelling",
          "displayOrder": 2,
          "active": true,
          "listingCount": 1
        },
        {
          "id": 8,
          "name": "Subsidy Guidance",
          "displayOrder": 3,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 9,
          "name": "Credit Facilitation",
          "displayOrder": 4,
          "active": true,
          "listingCount": 1
        },
        {
          "id": 10,
          "name": "Working Capital Advisory",
          "displayOrder": 5,
          "active": true,
          "listingCount": 0
        }
      ]
    },
    {
      "id": 3,
      "code": "COMPLIANCE",
      "name": "Legal, Tax & Compliance",
      "description": "GST, accounting, audit, registrations, legal and regulatory support",
      "icon": "gavel",
      "displayOrder": 3,
      "active": true,
      "mandatoryFields": [],
      "listingCount": 0,
      "subcategories": [
        {
          "id": 11,
          "name": "GST Services",
          "displayOrder": 1,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 12,
          "name": "Accounting & Bookkeeping",
          "displayOrder": 2,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 13,
          "name": "Audit",
          "displayOrder": 3,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 14,
          "name": "Business Registrations",
          "displayOrder": 4,
          "active": true,
          "listingCount": 0
        },
        {
          "id": 15,
          "name": "Le
... (truncated)
```

### 5. Public services

`GET /api/v1/public/services?page=0&size=5`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 2,
        "referenceNo": "BDS-SVC-000002",
        "title": "Financial Modelling & Credit Appraisal Support",
        "shortDescription": "Integrated three-statement financial model with DSCR, IRR and break-even analysis aligned to lender formats.",
        "categoryId": 2,
        "categoryName": "Finance & Credit Facilitation",
        "categoryIcon": "account_balance",
        "subcategoryId": 7,
        "subcategoryName": "Financial Modelling",
        "sellerId": 2,
        "sellerName": "ABC Business Consulting Pvt Ltd",
        "sellerState": "Uttar Pradesh",
        "sellerVerified": true,
        "deliveryMode": "ONLINE",
        "state": "Uttar Pradesh",
        "experienceYears": 10,
        "estimatedDuration": "2 weeks",
        "indicativeFee": 30000,
        "pricingModel": "PER_ASSIGNMENT",
        "status": "APPROVED",
        "featured": true,
        "publishedAt": "2026-11-18T13:48:31.512754",
        "updatedAt": "2026-11-18T13:48:31.520014"
      },
      {
        "id": 1,
        "referenceNo": "BDS-SVC-000001",
        "title": "Detailed Project Report (DPR) for Term Loan",
        "shortDescription": "Bank-ready DPR covering technical, market and financial viability for MSME term loan applications.",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "categoryIcon": "business_center",
        "subcategoryId": 1,
        "subcategoryName": "Detailed Project Reports",
        "sellerId": 2,
        "sellerName": "ABC Business Consulting Pvt Ltd",
        "sellerState": "Uttar Pradesh",
        "sellerVerified": true,
        "deliveryMode": "HYBRID",
        "state": "Uttar Pradesh",
        "experienceYears": 12,
        "estimatedDuration": "3-4 weeks",
        "indicativeFee": 45000,
        "pricingModel": "FIXED_FEE",
        "status": "APPROVED",
        "featured": true,
        "publishedAt": "2026-11-18T13:48:31.478595",
        "updatedAt": "2026-11-18T13:48:31.519811"
      },
      {
        "id": 3,
        "referenceNo": "BDS-SVC-000003",
        "title": "CGTMSE & Collateral-free Credit Facilitation",
        "shortDescription": "End-to-end support for collateral-free credit under CGTMSE including eligibility review and documentation.",
        "categoryId": 2,
        "categoryName": "Finance & Credit Facilitation",
        "categoryIcon": "account_balance",
        "subcategoryId": 9,
        "subcategoryName": "Credit Facilitation",
        "sellerId": 2,
        "sellerName": "ABC Business Consulting Pvt Ltd",
        "sellerState": "Uttar Pradesh",
        "sellerVerified": true,
        "deliveryMode": "HYBRID",
        "state": "Uttar Pradesh",
        "experienceYears": 12,
        "estimatedDuration": "4-6 weeks",
        "pricingModel": "CUSTOM_QUOTATION",
        "status": "APPROVED",
        "featured": false,
        "publishedAt": "2026-11-18T13:48:31.515801",
        "updatedAt": "2026-11-18T13:48:31.52015"
      }
    ],
    "page": 0,
    "size": 5,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-18T08:44:09.805575200Z"
}
```

### 6. Public catalog stats

`GET /api/v1/public/catalog/stats`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "services": 3,
    "providers": 1,
    "categories": 9
  },
  "timestamp": "2026-11-18T08:44:10.192865900Z"
}
```

### 7. Public providers

`GET /api/v1/public/providers?page=0&size=5`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 2,
        "referenceNo": "BDS-SLR-000002",
        "organisationName": "ABC Business Consulting Pvt Ltd",
        "providerType": "CONSULTANCY_FIRM",
        "city": "Lucknow",
        "state": "Uttar Pradesh",
        "district": "Lucknow",
        "yearsExperience": 12,
        "teamSize": 24,
        "expertise": [
          "DPR",
          "Financial Modelling",
          "Project Consultancy",
          "Credit Facilitation"
        ],
        "categoryNames": [
          "Business & Management Consultancy",
          "Finance & Credit Facilitation"
        ],
        "deliveryModes": [
          "ONLINE",
          "ONSITE",
          "HYBRID"
        ],
        "ratingCount": 0,
        "verified": true,
        "featured": true
      }
    ],
    "page": 0,
    "size": 5,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-18T08:44:10.355796Z"
}
```

### 8. Public settings

`GET /api/v1/public/settings`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "platform.name": "BDS Marketplace",
    "platform.tagline": "Grow Your MSME with the Right Expertise",
    "support.email": "support@bdsmarketplace.in",
    "support.phone": "1800-226-753",
    "support.hours": "Mon-Sat, 9:30 am - 6:00 pm",
    "announcement.text": "",
    "documents.max-size-mb": "5",
    "documents.allowed-types": "pdf,jpg,jpeg,png,doc,docx,xls,xlsx",
    "disclaimer.payments": "BDS Marketplace does not collect, hold or process payments. Contract and payment terms are agreed directly between purchaser and provider."
  },
  "timestamp": "2026-11-18T08:44:10.451554800Z"
}
```

### 9. Protected endpoint without token -> 401

`GET /api/v1/auth/me`

Response `401`:
```json
{
  "success": false,
  "status": 401,
  "message": "Authentication required",
  "timestamp": "2026-11-18T08:44:10.476328600Z",
  "path": "/api/v1/auth/me"
}
```

### 10. Login with wrong password -> 401

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "admin@bdsmarketplace.in",
  "password": "<redacted>",
  "portal": "ADMIN"
}
```

Response `401`:
```json
{
  "success": false,
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2026-11-18T08:44:11.030747900Z",
  "path": "/api/v1/auth/login"
}
```

### 11. Seller registration with invalid PAN -> 400

`POST /api/v1/auth/register/seller`

Request:
```json
{
  "fullName": "Ravi Kumar Invalid",
  "email": "seller.invalid.3336@example.com",
  "mobile": "7488876824",
  "password": "<redacted>",
  "emailOtp": null,
  "organisationName": "Invalid Advisory Services Pvt Ltd",
  "constitution": "PRIVATE_LIMITED",
  "pan": "ABC123",
  "gstin": "",
  "udyamNumber": "",
  "addressLine": "12, MG Road, Hazratganj",
  "city": "Lucknow",
  "state": "Uttar Pradesh",
  "district": "Lucknow",
  "pincode": "226001",
  "providerType": "CONSULTANCY_FIRM",
  "categoryIds": [
    1
  ],
  "expertise": [
    "Detailed Project Reports",
    "Business Plans"
  ],
  "industries": [
    "Manufacturing",
    "Food Processing"
  ],
  "yearsExperience": 8,
  "geographicCoverage": [
    "Uttar Pradesh",
    "Bihar"
  ],
  "deliveryModes": [
    "ONLINE",
    "ONSITE"
  ],
  "website": "https://example.com",
  "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
  "declarationAccepted": true
}
```

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "pan",
      "message": "Enter a valid PAN"
    }
  ],
  "timestamp": "2026-11-18T08:44:11.193402400Z",
  "path": "/api/v1/auth/register/seller"
}
```

### 12. Login (ADMIN)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "admin@bdsmarketplace.in",
  "password": "<redacted>",
  "portal": "ADMIN"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent to registered email",
  "data": {
    "otpRequired": true,
    "challengeToken": "RTPZdk1lYgv_Qs6-bvvN...",
    "maskedDestination": "a***n@bdsmarketplace.in",
    "expiresInSeconds": 300,
    "devOtp": "331123"
  },
  "timestamp": "2026-11-18T08:44:12.212965700Z"
}
```

### 13. Verify login OTP (ADMIN)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "RTPZdk1lYgv_Qs6-bvvNNOl2r2I5bPYa6DkCrpwvFxc",
  "otp": "331123"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "WMDDdaYK3ZyCpzWgqpMe...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "fullName": "Platform Administrator",
      "email": "admin@bdsmarketplace.in",
      "role": "ADMIN",
      "status": "ACTIVE",
      "referenceNo": "BDS-ADM-000001",
      "organisationName": "BDS Marketplace",
      "designation": "Super Administrator",
      "permissions": [
        "ADMIN_DASHBOARD",
        "AUDIT_VIEW",
        "BUYER_MANAGE",
        "CATALOG_MANAGE",
        "FEEDBACK_MANAGE",
        "GRIEVANCE_MANAGE",
        "LISTING_MODERATE",
        "REPORT_VIEW",
        "RFQ_MONITOR",
        "SELLER_VERIFY",
        "SETTINGS_MANAGE",
        "USER_ADMIN"
      ],
      "mustChangePassword": false,
      "emailAlerts": true,
      "lastLoginAt": "2026-11-18T14:14:12.6278866"
    }
  },
  "timestamp": "2026-11-18T08:44:12.893495700Z"
}
```

### 14. Admin: current user

`GET /api/v1/auth/me`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "fullName": "Platform Administrator",
    "email": "admin@bdsmarketplace.in",
    "role": "ADMIN",
    "status": "ACTIVE",
    "referenceNo": "BDS-ADM-000001",
    "organisationName": "BDS Marketplace",
    "designation": "Super Administrator",
    "permissions": [
      "ADMIN_DASHBOARD",
      "AUDIT_VIEW",
      "BUYER_MANAGE",
      "CATALOG_MANAGE",
      "FEEDBACK_MANAGE",
      "GRIEVANCE_MANAGE",
      "LISTING_MODERATE",
      "REPORT_VIEW",
      "RFQ_MONITOR",
      "SELLER_VERIFY",
      "SETTINGS_MANAGE",
      "USER_ADMIN"
    ],
    "mustChangePassword": false,
    "emailAlerts": true,
    "lastLoginAt": "2026-11-18T14:14:12.627887"
  },
  "timestamp": "2026-11-18T08:44:13.006402200Z"
}
```

### 15. Admin: identity stats

`GET /api/v1/admin/identity/stats`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "totalSellers": 1,
    "totalBuyers": 1,
    "pendingSellers": 0,
    "pendingBuyers": 0,
    "sellersByStatus": {
      "PENDING_VERIFICATION": 0,
      "INFO_REQUESTED": 0,
      "ACTIVE": 1,
      "SUSPENDED": 0,
      "REJECTED": 0,
      "DEACTIVATED": 0
    },
    "buyersByStatus": {
      "PENDING_VERIFICATION": 0,
      "INFO_REQUESTED": 0,
      "ACTIVE": 1,
      "SUSPENDED": 0,
      "REJECTED": 0,
      "DEACTIVATED": 0
    },
    "registrationTrend": [
      {
        "period": "Dec 2025",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Jan 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Feb 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Mar 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Apr 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "May 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Jun 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Jul 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Aug 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Sept 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Oct 2026",
        "sellers": 0,
        "buyers": 0
      },
      {
        "period": "Nov 2026",
        "sellers": 1,
        "buyers": 1
      }
    ],
    "sellersByState": [
      {
        "label": "Uttar Pradesh",
        "count": 1
      }
    ],
    "sellersByProviderType": [
      {
        "label": "CONSULTANCY_FIRM",
        "count": 1
      }
    ],
    "buyersByType": [
      {
        "label": "SMALL",
        "count": 1
      }
    ],
    "buyersByIndustry": [
      {
        "label": "Manufacturing",
        "count": 1
      }
    ]
  },
  "timestamp": "2026-11-18T08:44:13.549395800Z"
}
```

### 16. Send registration OTP (seller)

`POST /api/v1/auth/otp/send`

Request:
```json
{
  "target": "seller.self.3336@example.com",
  "purpose": "REGISTRATION"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent",
  "data": {
    "maskedTarget": "s**************6@example.com",
    "expiresInSeconds": 300,
    "resendAfterSeconds": 30,
    "devOtp": "968562"
  },
  "timestamp": "2026-11-18T08:44:14.145875200Z"
}
```

### 17. Self-register seller (multipart with documents)

`POST /api/v1/auth/register/seller`

Request:
```json
{
  "data (json part)": {
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.3336@example.com",
    "mobile": "6109424217",
    "password": "<redacted>",
    "emailOtp": "968562",
    "organisationName": "Self Advisory Services Pvt Ltd",
    "constitution": "PRIVATE_LIMITED",
    "pan": "OJLCK6099V",
    "gstin": "",
    "udyamNumber": "",
    "addressLine": "12, MG Road, Hazratganj",
    "city": "Lucknow",
    "state": "Uttar Pradesh",
    "district": "Lucknow",
    "pincode": "226001",
    "providerType": "CONSULTANCY_FIRM",
    "categoryIds": [
      1
    ],
    "expertise": [
      "Detailed Project Reports",
      "Business Plans"
    ],
    "industries": [
      "Manufacturing",
      "Food Processing"
    ],
    "yearsExperience": 8,
    "geographicCoverage": [
      "Uttar Pradesh",
      "Bihar"
    ],
    "deliveryModes": [
      "ONLINE",
      "ONSITE"
    ],
    "website": "https://example.com",
    "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
    "declarationAccepted": true
  },
  "files": [
    "PAN -> pan.pdf",
    "REGISTRATION_CERTIFICATE -> coi.pdf"
  ]
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Registration submitted",
  "data": {
    "userId": 4,
    "referenceNo": "BDS-SLR-000004",
    "status": "PENDING_VERIFICATION",
    "message": "Registration submitted. Your application BDS-SLR-000004 is pending verification."
  },
  "timestamp": "2026-11-18T08:44:15.826750400Z"
}
```

### 18. Send registration OTP (buyer)

`POST /api/v1/auth/otp/send`

Request:
```json
{
  "target": "buyer.self.3336@example.com",
  "purpose": "REGISTRATION"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent",
  "data": {
    "maskedTarget": "b*************6@example.com",
    "expiresInSeconds": 300,
    "resendAfterSeconds": 30,
    "devOtp": "636497"
  },
  "timestamp": "2026-11-18T08:44:16.631776900Z"
}
```

### 19. Self-register buyer

`POST /api/v1/auth/register/buyer`

Request:
```json
{
  "fullName": "Anita Sharma Self",
  "email": "buyer.self.3336@example.com",
  "mobile": "7768506162",
  "password": "<redacted>",
  "emailOtp": "636497",
  "organisationName": "Self Agro Foods",
  "constitution": "PROPRIETORSHIP",
  "purchaserType": "MICRO",
  "designation": "Proprietor",
  "pan": "HHBPV4608N",
  "gstin": "",
  "udyamNumber": "",
  "industrySector": "Food Processing",
  "businessActivity": "Pickles and spices manufacturing",
  "addressLine": "45, Industrial Area Phase 2",
  "city": "Kanpur",
  "state": "Uttar Pradesh",
  "district": "Kanpur Nagar",
  "pincode": "208001",
  "website": "",
  "declarationAccepted": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Registration submitted",
  "data": {
    "userId": 5,
    "referenceNo": "BDS-PUR-000005",
    "status": "PENDING_VERIFICATION",
    "message": "Registration submitted. Your account BDS-PUR-000005 will be activated after verification."
  },
  "timestamp": "2026-11-18T08:44:17.863090900Z"
}
```

### 20. Admin onboards seller

`POST /api/v1/admin/users/sellers`  — `Authorization: Bearer <token>`

Request:
```json
{
  "fullName": "Ravi Kumar Admin",
  "email": "seller.admin.3336@example.com",
  "mobile": "7641758579",
  "organisationName": "Admin Advisory Services Pvt Ltd",
  "constitution": "PRIVATE_LIMITED",
  "pan": "KOMCX2779R",
  "gstin": "",
  "udyamNumber": "",
  "addressLine": "12, MG Road, Hazratganj",
  "city": "Lucknow",
  "state": "Uttar Pradesh",
  "district": "Lucknow",
  "pincode": "226001",
  "providerType": "CONSULTANCY_FIRM",
  "categoryIds": [
    1
  ],
  "expertise": [
    "Detailed Project Reports",
    "Business Plans"
  ],
  "industries": [
    "Manufacturing",
    "Food Processing"
  ],
  "yearsExperience": 8,
  "geographicCoverage": [
    "Uttar Pradesh",
    "Bihar"
  ],
  "deliveryModes": [
    "ONLINE",
    "ONSITE"
  ],
  "website": "https://example.com",
  "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
  "declarationAccepted": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Seller created",
  "data": {
    "userId": 6,
    "referenceNo": "BDS-SLR-000006",
    "temporaryPassword": "<redacted>"
  },
  "timestamp": "2026-11-18T08:44:19.326828300Z"
}
```

### 21. Admin onboards buyer

`POST /api/v1/admin/users/buyers`  — `Authorization: Bearer <token>`

Request:
```json
{
  "fullName": "Anita Sharma Admin",
  "email": "buyer.admin.3336@example.com",
  "mobile": "7507934177",
  "organisationName": "Admin Agro Foods",
  "constitution": "PROPRIETORSHIP",
  "purchaserType": "MICRO",
  "designation": "Proprietor",
  "pan": "UHSPV2503J",
  "gstin": "",
  "udyamNumber": "",
  "industrySector": "Food Processing",
  "businessActivity": "Pickles and spices manufacturing",
  "addressLine": "45, Industrial Area Phase 2",
  "city": "Kanpur",
  "state": "Uttar Pradesh",
  "district": "Kanpur Nagar",
  "pincode": "208001",
  "website": "",
  "declarationAccepted": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Purchaser created",
  "data": {
    "userId": 7,
    "referenceNo": "BDS-PUR-000007",
    "temporaryPassword": "<redacted>"
  },
  "timestamp": "2026-11-18T08:44:20.207468Z"
}
```

### 22. Admin: list pending users

`GET /api/v1/admin/users?status=PENDING_VERIFICATION&page=0&size=10`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 5,
        "referenceNo": "BDS-PUR-000005",
        "fullName": "Anita Sharma Self",
        "organisationName": "Self Agro Foods",
        "email": "b*************6@example.com",
        "mobile": "77******62",
        "role": "BUYER",
        "status": "PENDING_VERIFICATION",
        "state": "Uttar Pradesh",
        "district": "Kanpur Nagar",
        "typeLabel": "MICRO",
        "permissions": [],
        "registeredAt": "2026-11-18T14:14:17.350558"
      },
      {
        "id": 4,
        "referenceNo": "BDS-SLR-000004",
        "fullName": "Ravi Kumar Self",
        "organisationName": "Self Advisory Services Pvt Ltd",
        "email": "s**************6@example.com",
        "mobile": "61******17",
        "role": "SELLER",
        "status": "PENDING_VERIFICATION",
        "state": "Uttar Pradesh",
        "district": "Lucknow",
        "typeLabel": "CONSULTANCY_FIRM",
        "permissions": [],
        "registeredAt": "2026-11-18T14:14:14.952008"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-18T08:44:20.537329800Z"
}
```

### 23. Admin: seller application detail

`GET /api/v1/admin/users/4`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 4,
    "referenceNo": "BDS-SLR-000004",
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.3336@example.com",
    "mobile": "6109424217",
    "role": "SELLER",
    "status": "PENDING_VERIFICATION",
    "permissions": [],
    "emailVerified": true,
    "locked": false,
    "registeredAt": "2026-11-18T14:14:14.952008",
    "sellerProfile": {
      "userId": 4,
      "referenceNo": "BDS-SLR-000004",
      "fullName": "Ravi Kumar Self",
      "email": "seller.self.3336@example.com",
      "mobile": "6109424217",
      "status": "PENDING_VERIFICATION",
      "organisationName": "Self Advisory Services Pvt Ltd",
      "constitution": "PRIVATE_LIMITED",
      "pan": "OJLCK6099V",
      "addressLine": "12, MG Road, Hazratganj",
      "city": "Lucknow",
      "state": "Uttar Pradesh",
      "district": "Lucknow",
      "pincode": "226001",
      "providerType": "CONSULTANCY_FIRM",
      "categoryIds": [
        1
      ],
      "categoryNames": [],
      "expertise": [
        "Detailed Project Reports",
        "Business Plans"
      ],
      "industries": [
        "Manufacturing",
        "Food Processing"
      ],
      "yearsExperience": 8,
      "geographicCoverage": [
        "Uttar Pradesh",
        "Bihar"
      ],
      "deliveryModes": [
        "ONLINE",
        "ONSITE"
      ],
      "website": "https://example.com",
      "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
      "ratingCount": 0,
      "profileCompletion": 65,
      "registeredAt": "2026-11-18T14:14:15.064521"
    },
    "documents": [
      {
        "id": 2,
        "documentType": "REGISTRATION_CERTIFICATE",
        "originalName": "coi.pdf",
        "contentType": "application/pdf",
        "fileSize": 108,
        "verified": false,
        "uploadedAt": "2026-11-18T14:14:15.425221"
      },
      {
        "id": 1,
        "documentType": "PAN",
        "originalName": "pan.pdf",
        "contentType": "application/pdf",
        "fileSize": 88,
        "verified": false,
        "uploadedAt": "2026-11-18T14:14:15.252435"
      }
    ],
    "checklist": [
      {
        "documentType": "PAN",
        "mandatory": true,
        "uploaded": true,
        "verified": false
      },
      {
        "documentType": "REGISTRATION_CERTIFICATE",
        "mandatory": true,
        "uploaded": true,
        "verified": false
      },
      {
        "documentType": "GST_CERTIFICATE",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      },
      {
        "documentType": "UDYAM_CERTIFICATE",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      },
      {
        "documentType": "PROFESSIONAL_CREDENTIAL",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      }
    ],
    "history": [
      {
        "id": 1,
        "action": "SUBMITTED",
        "toStatus": "PENDING_VERIFICATION",
        "remarks": "Application submitted for verification",
        "actorName": "Ravi Kumar Self",
        "createdAt": "2026-11-18T14:14:15.542492"
      }
    ]
  },
  "timestamp": "2026-11-18T08:44:20.790134Z"
}
```

### 24. Admin: verify document REGISTRATION_CERTIFICATE

`PUT /api/v1/admin/users/4/documents/2/verify`  — `Authorization: Bearer <token>`

Request:
```json
{
  "verified": true,
  "remarks": "Document checked"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Document updated",
  "data": {
    "id": 2,
    "documentType": "REGISTRATION_CERTIFICATE",
    "originalName": "coi.pdf",
    "contentType": "application/pdf",
    "fileSize": 108,
    "verified": true,
    "remarks": "Document checked",
    "uploadedAt": "2026-11-18T14:14:15.425221"
  },
  "timestamp": "2026-11-18T08:44:21.157282Z"
}
```

### 25. Admin: verify document PAN

`PUT /api/v1/admin/users/4/documents/1/verify`  — `Authorization: Bearer <token>`

Request:
```json
{
  "verified": true,
  "remarks": "Document checked"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Document updated",
  "data": {
    "id": 1,
    "documentType": "PAN",
    "originalName": "pan.pdf",
    "contentType": "application/pdf",
    "fileSize": 88,
    "verified": true,
    "remarks": "Document checked",
    "uploadedAt": "2026-11-18T14:14:15.252435"
  },
  "timestamp": "2026-11-18T08:44:21.327294800Z"
}
```

### 26. Admin: approve self-registered seller

`POST /api/v1/admin/users/4/approve`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "Documents verified, approved"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Account approved",
  "data": {
    "id": 4,
    "referenceNo": "BDS-SLR-000004",
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.3336@example.com",
    "mobile": "6109424217",
    "role": "SELLER",
    "status": "ACTIVE",
    "statusRemarks": "Documents verified, approved",
    "permissions": [
      "ENGAGEMENT_MANAGE",
      "LISTING_PUBLISH",
      "RFQ_RESPOND"
    ],
    "emailVerified": true,
    "locked": false,
    "registeredAt": "2026-11-18T14:14:14.952008",
    "sellerProfile": {
      "userId": 4,
      "referenceNo": "BDS-SLR-000004",
      "fullName": "Ravi Kumar Self",
      "email": "seller.self.3336@example.com",
      "mobile": "6109424217",
      "status": "ACTIVE",
      "statusRemarks": "Documents verified, approved",
      "organisationName": "Self Advisory Services Pvt Ltd",
      "constitution": "PRIVATE_LIMITED",
      "pan": "OJLCK6099V",
      "addressLine": "12, MG Road, Hazratganj",
      "city": "Lucknow",
      "state": "Uttar Pradesh",
      "district": "Lucknow",
      "pincode": "226001",
      "providerType": "CONSULTANCY_FIRM",
      "categoryIds": [
        1
      ],
      "categoryNames": [],
      "expertise": [
        "Detailed Project Reports",
        "Business Plans"
      ],
      "industries": [
        "Manufacturing",
        "Food Processing"
      ],
      "yearsExperience": 8,
      "geographicCoverage": [
        "Uttar Pradesh",
        "Bihar"
      ],
      "deliveryModes": [
        "ONLINE",
        "ONSITE"
      ],
      "website": "https://example.com",
      "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
      "ratingCount": 0,
      "verifiedAt": "2026-11-18T14:14:21.6917306",
      "reverificationDue": "2027-11-18",
      "profileCompletion": 65,
      "registeredAt": "2026-11-18T14:14:15.064521"
    },
    "documents": [
      {
        "id": 2,
        "documentType": "REGISTRATION_CERTIFICATE",
        "originalName": "coi.pdf",
        "contentType": "application/pdf",
        "fileSize": 108,
        "verified": true,
        "remarks": "Document checked",
        "uploadedAt": "2026-11-18T14:14:15.425221"
      },
      {
        "id": 1,
        "documentType": "PAN",
        "originalName": "pan.pdf",
        "contentType": "application/pdf",
        "fileSize": 88,
        "verified": true,
        "remarks": "Document checked",
        "uploadedAt": "2026-11-18T14:14:15.252435"
      }
    ],
    "checklist": [
      {
        "documentType": "PAN",
        "mandatory": true,
        "uploaded": true,
        "verified": true
      },
      {
        "documentType": "REGISTRATION_CERTIFICATE",
        "mandatory": true,
        "uploaded": true,
        "verified": true
      },
      {
        "documentType": "GST_CERTIFICATE",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      },
      {
        "documentType": "UDYAM_CERTIFICATE",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      },
      {
        "documentType": "PROFESSIONAL_CREDENTIAL",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      }
    ],
    "history": [
      {
        "id": 5,
        "action": "APPROVE",
        "fromStatus": "PENDING_VERIFICATION",
        "toStatus": "ACTIVE",
        "remarks": "Documents verified, approved",
        "actorName": "Platform Administrator",
        "createdAt": "20
... (truncated)
```

### 27. Admin: approve self-registered buyer

`POST /api/v1/admin/users/5/approve`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "Approved"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Account approved",
  "data": {
    "id": 5,
    "referenceNo": "BDS-PUR-000005",
    "fullName": "Anita Sharma Self",
    "email": "buyer.self.3336@example.com",
    "mobile": "7768506162",
    "role": "BUYER",
    "status": "ACTIVE",
    "statusRemarks": "Approved",
    "designation": "Proprietor",
    "permissions": [
      "FEEDBACK_SUBMIT",
      "QUOTATION_EVALUATE",
      "RFQ_CREATE"
    ],
    "emailVerified": true,
    "locked": false,
    "registeredAt": "2026-11-18T14:14:17.350558",
    "buyerProfile": {
      "userId": 5,
      "referenceNo": "BDS-PUR-000005",
      "fullName": "Anita Sharma Self",
      "email": "buyer.self.3336@example.com",
      "mobile": "7768506162",
      "status": "ACTIVE",
      "statusRemarks": "Approved",
      "organisationName": "Self Agro Foods",
      "constitution": "PROPRIETORSHIP",
      "purchaserType": "MICRO",
      "contactPerson": "Anita Sharma Self",
      "designation": "Proprietor",
      "pan": "HHBPV4608N",
      "industrySector": "Food Processing",
      "businessActivity": "Pickles and spices manufacturing",
      "addressLine": "45, Industrial Area Phase 2",
      "city": "Kanpur",
      "state": "Uttar Pradesh",
      "district": "Kanpur Nagar",
      "pincode": "208001",
      "registeredAt": "2026-11-18T14:14:17.475354"
    },
    "documents": [],
    "checklist": [],
    "history": [
      {
        "id": 6,
        "action": "APPROVE",
        "fromStatus": "PENDING_VERIFICATION",
        "toStatus": "ACTIVE",
        "remarks": "Approved",
        "actorName": "Platform Administrator",
        "createdAt": "2026-11-18T14:14:22.5429576"
      },
      {
        "id": 2,
        "action": "SUBMITTED",
        "toStatus": "PENDING_VERIFICATION",
        "remarks": "Purchaser registration submitted",
        "actorName": "Anita Sharma Self",
        "createdAt": "2026-11-18T14:14:17.586183"
      }
    ]
  },
  "timestamp": "2026-11-18T08:44:22.914177600Z"
}
```

### 28. Login (SELLER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "seller.self.3336@example.com",
  "password": "<redacted>",
  "portal": "SELLER"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent to registered email",
  "data": {
    "otpRequired": true,
    "challengeToken": "AH3g7lJojnKyG5uv_xFJ...",
    "maskedDestination": "s**************6@example.com",
    "expiresInSeconds": 300,
    "devOtp": "560434"
  },
  "timestamp": "2026-11-18T08:44:23.799382400Z"
}
```

### 29. Verify login OTP (SELLER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "AH3g7lJojnKyG5uv_xFJ8kdjHBvt1Q5cnIrF_NUNxFo",
  "otp": "560434"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "UUmzPldvK9W7w5Ijoa1m...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 4,
      "fullName": "Ravi Kumar Self",
      "email": "seller.self.3336@example.com",
      "mobile": "6109424217",
      "role": "SELLER",
      "status": "ACTIVE",
      "referenceNo": "BDS-SLR-000004",
      "organisationName": "Self Advisory Services Pvt Ltd",
      "permissions": [
        "ENGAGEMENT_MANAGE",
        "LISTING_PUBLISH",
        "RFQ_RESPOND"
      ],
      "mustChangePassword": false,
      "emailAlerts": true,
      "statusRemarks": "Documents verified, approved",
      "lastLoginAt": "2026-11-18T14:14:24.1658197"
    }
  },
  "timestamp": "2026-11-18T08:44:24.479598900Z"
}
```

### 30. Seller: profile

`GET /api/v1/sellers/me/profile`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 4,
    "referenceNo": "BDS-SLR-000004",
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.3336@example.com",
    "mobile": "6109424217",
    "status": "ACTIVE",
    "statusRemarks": "Documents verified, approved",
    "organisationName": "Self Advisory Services Pvt Ltd",
    "constitution": "PRIVATE_LIMITED",
    "pan": "OJLCK6099V",
    "addressLine": "12, MG Road, Hazratganj",
    "city": "Lucknow",
    "state": "Uttar Pradesh",
    "district": "Lucknow",
    "pincode": "226001",
    "providerType": "CONSULTANCY_FIRM",
    "categoryIds": [
      1
    ],
    "categoryNames": [],
    "expertise": [
      "Detailed Project Reports",
      "Business Plans"
    ],
    "industries": [
      "Manufacturing",
      "Food Processing"
    ],
    "yearsExperience": 8,
    "geographicCoverage": [
      "Uttar Pradesh",
      "Bihar"
    ],
    "deliveryModes": [
      "ONLINE",
      "ONSITE"
    ],
    "website": "https://example.com",
    "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
    "ratingCount": 0,
    "verifiedAt": "2026-11-18T14:14:21.691731",
    "reverificationDue": "2027-11-18",
    "profileCompletion": 65,
    "registeredAt": "2026-11-18T14:14:15.064521"
  },
  "timestamp": "2026-11-18T08:44:24.590087500Z"
}
```

### 31. Seller: verification status

`GET /api/v1/sellers/me/verification`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "status": "ACTIVE",
    "statusRemarks": "Documents verified, approved",
    "checklist": [
      {
        "documentType": "PAN",
        "mandatory": true,
        "uploaded": true,
        "verified": true
      },
      {
        "documentType": "REGISTRATION_CERTIFICATE",
        "mandatory": true,
        "uploaded": true,
        "verified": true
      },
      {
        "documentType": "GST_CERTIFICATE",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      },
      {
        "documentType": "UDYAM_CERTIFICATE",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      },
      {
        "documentType": "PROFESSIONAL_CREDENTIAL",
        "mandatory": false,
        "uploaded": false,
        "verified": false
      }
    ],
    "history": [
      {
        "id": 5,
        "action": "APPROVE",
        "fromStatus": "PENDING_VERIFICATION",
        "toStatus": "ACTIVE",
        "remarks": "Documents verified, approved",
        "actorName": "Platform Administrator",
        "createdAt": "2026-11-18T14:14:21.6947"
      },
      {
        "id": 1,
        "action": "SUBMITTED",
        "toStatus": "PENDING_VERIFICATION",
        "remarks": "Application submitted for verification",
        "actorName": "Ravi Kumar Self",
        "createdAt": "2026-11-18T14:14:15.542492"
      }
    ]
  },
  "timestamp": "2026-11-18T08:44:24.774441900Z"
}
```

### 32. Seller: create & submit service listing

`POST /api/v1/seller/services`  — `Authorization: Bearer <token>`

Request:
```json
{
  "title": "Detailed Project Report for MSME term loans",
  "categoryId": 1,
  "subcategoryId": 1,
  "shortDescription": "Bank-ready DPRs with financial projections",
  "description": "End-to-end preparation of detailed project reports including market study, technical feasibility and five-year financial projections.",
  "scopeOfWork": "Site visit, data collection, market analysis, financial modelling",
  "deliverables": "DPR document, CMA data, projected financials",
  "methodology": "Discovery workshop followed by iterative drafts",
  "targetIndustries": [
    "Manufacturing",
    "Food Processing"
  ],
  "deliveryMode": "HYBRID",
  "serviceLocations": [
    "Lucknow",
    "Kanpur"
  ],
  "state": "Uttar Pradesh",
  "district": "Lucknow",
  "experienceYears": 8,
  "estimatedDuration": "3-4 weeks",
  "indicativeFee": 45000,
  "pricingModel": "FIXED_FEE",
  "tags": [
    "DPR",
    "term loan",
    "MSME"
  ],
  "submit": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Service listing saved",
  "data": {
    "summary": {
      "id": 4,
      "referenceNo": "BDS-SVC-000004",
      "title": "Detailed Project Report for MSME term loans",
      "shortDescription": "Bank-ready DPRs with financial projections",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "categoryIcon": "business_center",
      "subcategoryId": 1,
      "subcategoryName": "Detailed Project Reports",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "sellerState": "Uttar Pradesh",
      "sellerRatingCount": 0,
      "sellerVerified": true,
      "deliveryMode": "HYBRID",
      "state": "Uttar Pradesh",
      "experienceYears": 8,
      "estimatedDuration": "3-4 weeks",
      "indicativeFee": 45000,
      "pricingModel": "FIXED_FEE",
      "status": "SUBMITTED",
      "featured": false,
      "updatedAt": "2026-11-18T14:14:25.2399693"
    },
    "description": "End-to-end preparation of detailed project reports including market study, technical feasibility and five-year financial projections.",
    "scopeOfWork": "Site visit, data collection, market analysis, financial modelling",
    "deliverables": "DPR document, CMA data, projected financials",
    "methodology": "Discovery workshop followed by iterative drafts",
    "targetIndustries": [
      "Manufacturing",
      "Food Processing"
    ],
    "serviceLocations": [
      "Lucknow",
      "Kanpur"
    ],
    "district": "Lucknow",
    "tags": [
      "DPR",
      "term loan",
      "MSME"
    ],
    "attachments": [],
    "viewCount": 0,
    "enquiryCount": 0,
    "submittedAt": "2026-11-18T14:14:25.4567924",
    "createdAt": "2026-11-18T14:14:25.2399693",
    "publiclyVisible": false
  },
  "timestamp": "2026-11-18T08:44:25.723343100Z"
}
```

### 33. Seller: my listings

`GET /api/v1/seller/services?page=0&size=10`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 4,
        "referenceNo": "BDS-SVC-000004",
        "title": "Detailed Project Report for MSME term loans",
        "shortDescription": "Bank-ready DPRs with financial projections",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "categoryIcon": "business_center",
        "subcategoryId": 1,
        "subcategoryName": "Detailed Project Reports",
        "sellerId": 4,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "sellerState": "Uttar Pradesh",
        "sellerRatingCount": 0,
        "sellerVerified": true,
        "deliveryMode": "HYBRID",
        "state": "Uttar Pradesh",
        "experienceYears": 8,
        "estimatedDuration": "3-4 weeks",
        "indicativeFee": 45000,
        "pricingModel": "FIXED_FEE",
        "status": "SUBMITTED",
        "featured": false,
        "updatedAt": "2026-11-18T14:14:25.562819"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-18T08:44:25.952693Z"
}
```

### 34. Admin: start listing review

`POST /api/v1/admin/services/4/start-review`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "Picked up for review"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Review started",
  "data": {
    "summary": {
      "id": 4,
      "referenceNo": "BDS-SVC-000004",
      "title": "Detailed Project Report for MSME term loans",
      "shortDescription": "Bank-ready DPRs with financial projections",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "categoryIcon": "business_center",
      "subcategoryId": 1,
      "subcategoryName": "Detailed Project Reports",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "sellerState": "Uttar Pradesh",
      "sellerRatingCount": 0,
      "sellerVerified": true,
      "deliveryMode": "HYBRID",
      "state": "Uttar Pradesh",
      "experienceYears": 8,
      "estimatedDuration": "3-4 weeks",
      "indicativeFee": 45000,
      "pricingModel": "FIXED_FEE",
      "status": "UNDER_REVIEW",
      "featured": false,
      "updatedAt": "2026-11-18T14:14:25.562819"
    },
    "description": "End-to-end preparation of detailed project reports including market study, technical feasibility and five-year financial projections.",
    "scopeOfWork": "Site visit, data collection, market analysis, financial modelling",
    "deliverables": "DPR document, CMA data, projected financials",
    "methodology": "Discovery workshop followed by iterative drafts",
    "targetIndustries": [
      "Manufacturing",
      "Food Processing"
    ],
    "serviceLocations": [
      "Lucknow",
      "Kanpur"
    ],
    "district": "Lucknow",
    "tags": [
      "DPR",
      "term loan",
      "MSME"
    ],
    "attachments": [],
    "viewCount": 0,
    "enquiryCount": 0,
    "submittedAt": "2026-11-18T14:14:25.456792",
    "createdAt": "2026-11-18T14:14:25.239969",
    "publiclyVisible": false
  },
  "timestamp": "2026-11-18T08:44:26.457499900Z"
}
```

### 35. Admin: approve listing

`POST /api/v1/admin/services/4/approve`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "Listing meets catalogue guidelines"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Listing approved and published",
  "data": {
    "summary": {
      "id": 4,
      "referenceNo": "BDS-SVC-000004",
      "title": "Detailed Project Report for MSME term loans",
      "shortDescription": "Bank-ready DPRs with financial projections",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "categoryIcon": "business_center",
      "subcategoryId": 1,
      "subcategoryName": "Detailed Project Reports",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "sellerState": "Uttar Pradesh",
      "sellerRatingCount": 0,
      "sellerVerified": true,
      "deliveryMode": "HYBRID",
      "state": "Uttar Pradesh",
      "experienceYears": 8,
      "estimatedDuration": "3-4 weeks",
      "indicativeFee": 45000,
      "pricingModel": "FIXED_FEE",
      "status": "APPROVED",
      "featured": false,
      "publishedAt": "2026-11-18T14:14:26.690569",
      "updatedAt": "2026-11-18T14:14:26.349848"
    },
    "description": "End-to-end preparation of detailed project reports including market study, technical feasibility and five-year financial projections.",
    "scopeOfWork": "Site visit, data collection, market analysis, financial modelling",
    "deliverables": "DPR document, CMA data, projected financials",
    "methodology": "Discovery workshop followed by iterative drafts",
    "targetIndustries": [
      "Manufacturing",
      "Food Processing"
    ],
    "serviceLocations": [
      "Lucknow",
      "Kanpur"
    ],
    "district": "Lucknow",
    "tags": [
      "DPR",
      "term loan",
      "MSME"
    ],
    "attachments": [],
    "moderationRemarks": "Listing meets catalogue guidelines",
    "viewCount": 0,
    "enquiryCount": 0,
    "submittedAt": "2026-11-18T14:14:25.456792",
    "createdAt": "2026-11-18T14:14:25.239969",
    "publiclyVisible": true
  },
  "timestamp": "2026-11-18T08:44:26.959212700Z"
}
```

### 36. Public: listing detail

`GET /api/v1/public/services/4`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "summary": {
      "id": 4,
      "referenceNo": "BDS-SVC-000004",
      "title": "Detailed Project Report for MSME term loans",
      "shortDescription": "Bank-ready DPRs with financial projections",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "categoryIcon": "business_center",
      "subcategoryId": 1,
      "subcategoryName": "Detailed Project Reports",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "sellerState": "Uttar Pradesh",
      "sellerRatingCount": 0,
      "sellerVerified": true,
      "deliveryMode": "HYBRID",
      "state": "Uttar Pradesh",
      "experienceYears": 8,
      "estimatedDuration": "3-4 weeks",
      "indicativeFee": 45000,
      "pricingModel": "FIXED_FEE",
      "status": "APPROVED",
      "featured": false,
      "publishedAt": "2026-11-18T14:14:26.690569",
      "updatedAt": "2026-11-18T14:14:26.848216"
    },
    "description": "End-to-end preparation of detailed project reports including market study, technical feasibility and five-year financial projections.",
    "scopeOfWork": "Site visit, data collection, market analysis, financial modelling",
    "deliverables": "DPR document, CMA data, projected financials",
    "methodology": "Discovery workshop followed by iterative drafts",
    "targetIndustries": [
      "Manufacturing",
      "Food Processing"
    ],
    "serviceLocations": [
      "Lucknow",
      "Kanpur"
    ],
    "district": "Lucknow",
    "tags": [
      "DPR",
      "term loan",
      "MSME"
    ],
    "attachments": [],
    "moderationRemarks": "Listing meets catalogue guidelines",
    "viewCount": 0,
    "enquiryCount": 0,
    "submittedAt": "2026-11-18T14:14:25.456792",
    "createdAt": "2026-11-18T14:14:25.239969",
    "publiclyVisible": true
  },
  "timestamp": "2026-11-18T08:44:27.337383100Z"
}
```

### 37. Login (BUYER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "buyer.self.3336@example.com",
  "password": "<redacted>",
  "portal": "BUYER"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent to registered email",
  "data": {
    "otpRequired": true,
    "challengeToken": "TTYwxaOGIZgWgEp3QSkP...",
    "maskedDestination": "b*************6@example.com",
    "expiresInSeconds": 300,
    "devOtp": "940992"
  },
  "timestamp": "2026-11-18T08:44:28.219170200Z"
}
```

### 38. Verify login OTP (BUYER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "TTYwxaOGIZgWgEp3QSkPCrtWyyhMvUAXFS3O0F4hSEM",
  "otp": "940992"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "7eTtMncns_USRAz-RjXo...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 5,
      "fullName": "Anita Sharma Self",
      "email": "buyer.self.3336@example.com",
      "mobile": "7768506162",
      "role": "BUYER",
      "status": "ACTIVE",
      "referenceNo": "BDS-PUR-000005",
      "organisationName": "Self Agro Foods",
      "designation": "Proprietor",
      "permissions": [
        "FEEDBACK_SUBMIT",
        "QUOTATION_EVALUATE",
        "RFQ_CREATE"
      ],
      "mustChangePassword": false,
      "emailAlerts": true,
      "statusRemarks": "Approved",
      "lastLoginAt": "2026-11-18T14:14:28.5895287"
    }
  },
  "timestamp": "2026-11-18T08:44:28.896698100Z"
}
```

### 39. Buyer: profile

`GET /api/v1/buyers/me/profile`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 5,
    "referenceNo": "BDS-PUR-000005",
    "fullName": "Anita Sharma Self",
    "email": "buyer.self.3336@example.com",
    "mobile": "7768506162",
    "status": "ACTIVE",
    "statusRemarks": "Approved",
    "organisationName": "Self Agro Foods",
    "constitution": "PROPRIETORSHIP",
    "purchaserType": "MICRO",
    "contactPerson": "Anita Sharma Self",
    "designation": "Proprietor",
    "pan": "HHBPV4608N",
    "industrySector": "Food Processing",
    "businessActivity": "Pickles and spices manufacturing",
    "addressLine": "45, Industrial Area Phase 2",
    "city": "Kanpur",
    "state": "Uttar Pradesh",
    "district": "Kanpur Nagar",
    "pincode": "208001",
    "registeredAt": "2026-11-18T14:14:17.475354"
  },
  "timestamp": "2026-11-18T08:44:28.990248100Z"
}
```

### 40. Buyer: dashboard

`GET /api/v1/buyer/dashboard`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "totalRfqs": 0,
    "openRfqs": 0,
    "quotationsReceived": 0,
    "comparisonPending": 0,
    "selectedProviders": 0,
    "activeEngagements": 0,
    "completedEngagements": 0,
    "workOrderRequests": 0,
    "deliveriesToConfirm": 0,
    "feedbackPending": 0,
    "openQueries": 0,
    "recentRfqs": [],
    "recentEngagements": []
  },
  "timestamp": "2026-11-18T08:44:29.761549900Z"
}
```

### 41. Buyer: create & publish RFQ

`POST /api/v1/buyer/rfqs`  — `Authorization: Bearer <token>`

Request:
```json
{
  "title": "DPR for pickle unit expansion",
  "categoryId": 1,
  "subcategoryId": 1,
  "description": "Need a bank-ready detailed project report for expanding our pickle manufacturing unit capacity.",
  "scopeOfWork": "Market study, machinery costing, financial projections for 7 years",
  "deliverables": "DPR, CMA data",
  "industry": "Food Processing",
  "projectState": "Uttar Pradesh",
  "projectDistrict": "Kanpur Nagar",
  "deliveryMode": "HYBRID",
  "budgetMin": 30000,
  "budgetMax": 60000,
  "expectedDuration": "1 month",
  "requiredExperience": 3,
  "expectedStartDate": "2026-11-28",
  "submissionDeadline": "2026-11-25T14:14:29",
  "distributionMode": "SELECTED",
  "sellerIds": [
    4
  ],
  "publish": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "RFQ BDS-RFQ-000001 submitted",
  "data": {
    "summary": {
      "id": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "title": "DPR for pickle unit expansion",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "subcategoryName": "Detailed Project Reports",
      "buyerId": 5,
      "buyerName": "Self Agro Foods",
      "projectState": "Uttar Pradesh",
      "projectDistrict": "Kanpur Nagar",
      "deliveryMode": "HYBRID",
      "budgetMin": 30000,
      "budgetMax": 60000,
      "submissionDeadline": "2026-11-25T14:14:29",
      "status": "PUBLISHED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 0,
      "deadlinePassed": false,
      "createdAt": "2026-11-18T14:14:30.2069153",
      "publishedAt": "2026-11-18T14:14:30.5301246"
    },
    "description": "Need a bank-ready detailed project report for expanding our pickle manufacturing unit capacity.",
    "scopeOfWork": "Market study, machinery costing, financial projections for 7 years",
    "deliverables": "DPR, CMA data",
    "industry": "Food Processing",
    "expectedDuration": "1 month",
    "requiredExperience": 3,
    "expectedStartDate": "2026-11-28",
    "attachments": [],
    "invitations": [
      {
        "sellerId": 4,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "INVITED",
        "invitedBy": "BUYER",
        "invitedAt": "2026-11-18T14:14:30.5301246"
      }
    ]
  },
  "timestamp": "2026-11-18T08:44:31.084656Z"
}
```

### 42. Seller: invited RFQs

`GET /api/v1/seller/rfqs?page=0&size=10`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 5,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-25T14:14:29",
        "status": "PUBLISHED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 0,
        "deadlinePassed": false,
        "myInvitationStatus": "INVITED",
        "createdAt": "2026-11-18T14:14:30.206915",
        "publishedAt": "2026-11-18T14:14:30.530125"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-18T08:44:31.351145Z"
}
```

### 43. Seller: RFQ detail

`GET /api/v1/seller/rfqs/1`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "summary": {
      "id": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "title": "DPR for pickle unit expansion",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "subcategoryName": "Detailed Project Reports",
      "buyerId": 5,
      "buyerName": "Self Agro Foods",
      "projectState": "Uttar Pradesh",
      "projectDistrict": "Kanpur Nagar",
      "deliveryMode": "HYBRID",
      "budgetMin": 30000,
      "budgetMax": 60000,
      "submissionDeadline": "2026-11-25T14:14:29",
      "status": "PUBLISHED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 0,
      "deadlinePassed": false,
      "myInvitationStatus": "VIEWED",
      "createdAt": "2026-11-18T14:14:30.206915",
      "publishedAt": "2026-11-18T14:14:30.530125"
    },
    "description": "Need a bank-ready detailed project report for expanding our pickle manufacturing unit capacity.",
    "scopeOfWork": "Market study, machinery costing, financial projections for 7 years",
    "deliverables": "DPR, CMA data",
    "industry": "Food Processing",
    "expectedDuration": "1 month",
    "requiredExperience": 3,
    "expectedStartDate": "2026-11-28",
    "attachments": [],
    "invitations": []
  },
  "timestamp": "2026-11-18T08:44:31.738363800Z"
}
```

### 44. Seller: submit quotation

`POST /api/v1/seller/rfqs/1/quotations`  — `Authorization: Bearer <token>`

Request:
```json
{
  "technicalProposal": "We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.",
  "methodology": "Kick-off call, site visit, two review cycles",
  "scopeAccepted": true,
  "deliverables": "DPR document, CMA data, projected financials",
  "timelineDays": 21,
  "teamComposition": "1 CA, 1 industry analyst",
  "relevantExperience": "40+ DPRs for food processing units",
  "professionalFee": 45000,
  "taxPercent": 18,
  "otherExpenses": 5000,
  "paymentMilestones": "50% advance, 50% on delivery",
  "validUntil": "2026-12-18",
  "items": [
    {
      "description": "DPR preparation",
      "quantity": 1,
      "unit": "lot",
      "unitPrice": 40000
    },
    {
      "description": "CMA data",
      "quantity": 1,
      "unit": "lot",
      "unitPrice": 5000
    }
  ],
  "submit": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Quotation BDS-QTN-000001 submitted",
  "data": {
    "id": 1,
    "quotationNumber": "BDS-QTN-000001",
    "rfqId": 1,
    "rfqNumber": "BDS-RFQ-000001",
    "rfqTitle": "DPR for pickle unit expansion",
    "rfqStatus": "RESPONSES_RECEIVED",
    "rfqDeadline": "2026-11-25T14:14:29",
    "sellerId": 4,
    "sellerName": "Self Advisory Services Pvt Ltd",
    "buyerId": 5,
    "buyerName": "Self Agro Foods",
    "versionNo": 1,
    "status": "SUBMITTED",
    "technicalProposal": "We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.",
    "methodology": "Kick-off call, site visit, two review cycles",
    "scopeAccepted": true,
    "deliverables": "DPR document, CMA data, projected financials",
    "timelineDays": 21,
    "teamComposition": "1 CA, 1 industry analyst",
    "relevantExperience": "40+ DPRs for food processing units",
    "professionalFee": 45000.0,
    "taxPercent": 18,
    "taxAmount": 8100.0,
    "otherExpenses": 5000.0,
    "totalAmount": 58100.0,
    "paymentMilestones": "50% advance, 50% on delivery",
    "validUntil": "2026-12-18",
    "submittedAt": "2026-11-18T14:14:32.5846426",
    "shortlisted": false,
    "editable": true,
    "items": [
      {
        "id": 1,
        "description": "DPR preparation",
        "quantity": 1,
        "unit": "lot",
        "unitPrice": 40000,
        "amount": 40000.0
      },
      {
        "id": 2,
        "description": "CMA data",
        "quantity": 1,
        "unit": "lot",
        "unitPrice": 5000,
        "amount": 5000.0
      }
    ],
    "attachments": [],
    "createdAt": "2026-11-18T14:14:32.2543162",
    "updatedAt": "2026-11-18T14:14:32.7101202"
  },
  "timestamp": "2026-11-18T08:44:32.977348700Z"
}
```

### 45. Buyer: quotations for RFQ

`GET /api/v1/buyer/rfqs/1/quotations`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "quotationNumber": "BDS-QTN-000001",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "rfqTitle": "DPR for pickle unit expansion",
      "rfqStatus": "RESPONSES_RECEIVED",
      "rfqDeadline": "2026-11-25T14:14:29",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "sellerRatingCount": 0,
      "buyerId": 5,
      "buyerName": "Self Agro Foods",
      "versionNo": 1,
      "status": "SUBMITTED",
      "technicalProposal": "We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.",
      "methodology": "Kick-off call, site visit, two review cycles",
      "scopeAccepted": true,
      "deliverables": "DPR document, CMA data, projected financials",
      "timelineDays": 21,
      "teamComposition": "1 CA, 1 industry analyst",
      "relevantExperience": "40+ DPRs for food processing units",
      "professionalFee": 45000,
      "taxPercent": 18,
      "taxAmount": 8100,
      "otherExpenses": 5000,
      "totalAmount": 58100,
      "paymentMilestones": "50% advance, 50% on delivery",
      "validUntil": "2026-12-18",
      "submittedAt": "2026-11-18T14:14:32.584643",
      "shortlisted": false,
      "editable": false,
      "items": [
        {
          "id": 1,
          "description": "DPR preparation",
          "quantity": 1,
          "unit": "lot",
          "unitPrice": 40000,
          "amount": 40000
        },
        {
          "id": 2,
          "description": "CMA data",
          "quantity": 1,
          "unit": "lot",
          "unitPrice": 5000,
          "amount": 5000
        }
      ],
      "attachments": [],
      "createdAt": "2026-11-18T14:14:32.254316",
      "updatedAt": "2026-11-18T14:14:32.768327"
    }
  ],
  "timestamp": "2026-11-18T08:44:33.414169300Z"
}
```

### 46. Buyer: comparison

`GET /api/v1/buyer/rfqs/1/comparison`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "rfq": {
      "id": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "title": "DPR for pickle unit expansion",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "subcategoryName": "Detailed Project Reports",
      "buyerId": 5,
      "buyerName": "Self Agro Foods",
      "projectState": "Uttar Pradesh",
      "projectDistrict": "Kanpur Nagar",
      "deliveryMode": "HYBRID",
      "budgetMin": 30000,
      "budgetMax": 60000,
      "submissionDeadline": "2026-11-25T14:14:29",
      "status": "RESPONSES_RECEIVED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 1,
      "deadlinePassed": false,
      "createdAt": "2026-11-18T14:14:30.206915",
      "publishedAt": "2026-11-18T14:14:30.530125"
    },
    "criteria": [],
    "rows": [
      {
        "quotationId": 1,
        "quotationNumber": "BDS-QTN-000001",
        "sellerId": 4,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "SUBMITTED",
        "shortlisted": false,
        "versionNo": 1,
        "professionalFee": 45000,
        "taxAmount": 8100,
        "otherExpenses": 5000,
        "totalAmount": 58100,
        "timelineDays": 21,
        "validUntil": "2026-12-18",
        "scopeAccepted": true,
        "methodology": "Kick-off call, site visit, two review cycles",
        "deliverables": "DPR document, CMA data, projected financials",
        "relevantExperience": "40+ DPRs for food processing units",
        "scores": {},
        "lowestPrice": true,
        "shortestTimeline": true
      }
    ],
    "note": "Lowest price is not automatically the winning criterion. Evaluate technical and commercial factors together and record the reason for your selection."
  },
  "timestamp": "2026-11-18T08:44:33.949669400Z"
}
```

### 47. Buyer: shortlist quotation

`POST /api/v1/buyer/quotations/1/shortlist`  — `Authorization: Bearer <token>`

Request:
```json
{
  "shortlisted": true
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Shortlist updated",
  "data": {
    "id": 1,
    "quotationNumber": "BDS-QTN-000001",
    "rfqId": 1,
    "rfqNumber": "BDS-RFQ-000001",
    "rfqTitle": "DPR for pickle unit expansion",
    "rfqStatus": "RESPONSES_RECEIVED",
    "rfqDeadline": "2026-11-25T14:14:29",
    "sellerId": 4,
    "sellerName": "Self Advisory Services Pvt Ltd",
    "sellerRatingCount": 0,
    "buyerId": 5,
    "buyerName": "Self Agro Foods",
    "versionNo": 1,
    "status": "SHORTLISTED",
    "technicalProposal": "We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.",
    "methodology": "Kick-off call, site visit, two review cycles",
    "scopeAccepted": true,
    "deliverables": "DPR document, CMA data, projected financials",
    "timelineDays": 21,
    "teamComposition": "1 CA, 1 industry analyst",
    "relevantExperience": "40+ DPRs for food processing units",
    "professionalFee": 45000,
    "taxPercent": 18,
    "taxAmount": 8100,
    "otherExpenses": 5000,
    "totalAmount": 58100,
    "paymentMilestones": "50% advance, 50% on delivery",
    "validUntil": "2026-12-18",
    "submittedAt": "2026-11-18T14:14:32.584643",
    "shortlisted": true,
    "editable": false,
    "items": [
      {
        "id": 1,
        "description": "DPR preparation",
        "quantity": 1,
        "unit": "lot",
        "unitPrice": 40000,
        "amount": 40000
      },
      {
        "id": 2,
        "description": "CMA data",
        "quantity": 1,
        "unit": "lot",
        "unitPrice": 5000,
        "amount": 5000
      }
    ],
    "attachments": [],
    "createdAt": "2026-11-18T14:14:32.254316",
    "updatedAt": "2026-11-18T14:14:34.5284777"
  },
  "timestamp": "2026-11-18T08:44:34.733589800Z"
}
```

### 48. Buyer: accept quotation

`POST /api/v1/buyer/quotations/1/accept`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "Selected based on experience and price",
  "contractValue": 53100,
  "startDate": "2026-11-28",
  "expectedEndDate": "2026-12-19",
  "confirmed": true
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Provider selected. Engagement created.",
  "data": {
    "summary": {
      "id": 1,
      "engagementNumber": "BDS-ENG-000001",
      "title": "DPR for pickle unit expansion",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "quotationId": 1,
      "quotationNumber": "BDS-QTN-000001",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "buyerId": 5,
      "buyerName": "Self Agro Foods",
      "quotedValue": 58100,
      "contractValue": 53100,
      "startDate": "2026-11-28",
      "expectedEndDate": "2026-12-19",
      "status": "SELECTED",
      "progressPercent": 0,
      "feedbackSubmitted": false,
      "createdAt": "2026-11-18T14:14:35.4655577",
      "updatedAt": "2026-11-18T14:14:35.6941984"
    },
    "selectionRemarks": "Selected based on experience and price",
    "quotation": {
      "id": 1,
      "quotationNumber": "BDS-QTN-000001",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "rfqTitle": "DPR for pickle unit expansion",
      "rfqStatus": "PROVIDER_SELECTED",
      "rfqDeadline": "2026-11-25T14:14:29",
      "sellerId": 4,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "buyerId": 5,
      "buyerName": "Self Agro Foods",
      "versionNo": 1,
      "status": "ACCEPTED",
      "technicalProposal": "We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.",
      "methodology": "Kick-off call, site visit, two review cycles",
      "scopeAccepted": true,
      "deliverables": "DPR document, CMA data, projected financials",
      "timelineDays": 21,
      "teamComposition": "1 CA, 1 industry analyst",
      "relevantExperience": "40+ DPRs for food processing units",
      "professionalFee": 45000,
      "taxPercent": 18,
      "taxAmount": 8100,
      "otherExpenses": 5000,
      "totalAmount": 58100,
      "paymentMilestones": "50% advance, 50% on delivery",
      "validUntil": "2026-12-18",
      "submittedAt": "2026-11-18T14:14:32.584643",
      "decisionAt": "2026-11-18T14:14:35.2944813",
      "decisionRemarks": "Selected based on experience and price",
      "shortlisted": true,
      "editable": false,
      "engagementId": 1,
      "items": [
        {
          "id": 1,
          "description": "DPR preparation",
          "quantity": 1,
          "unit": "lot",
          "unitPrice": 40000,
          "amount": 40000
        },
        {
          "id": 2,
          "description": "CMA data",
          "quantity": 1,
          "unit": "lot",
          "unitPrice": 5000,
          "amount": 5000
        }
      ],
      "attachments": [],
      "createdAt": "2026-11-18T14:14:32.254316",
      "updatedAt": "2026-11-18T14:14:35.2964775"
    },
    "workOrders": [],
    "updates": [
      {
        "id": 1,
        "updateType": "STATUS_CHANGE",
        "message": "Provider selected. Selected based on experience and price",
        "toStatus": "SELECTED",
        "authorRole": "BUYER",
        "authorName": "Self Agro Foods",
        "createdAt": "2026-11-18T14:14:35.584969"
      }
    ],
    "documents": [],
    "queries": [],
    "disclaimer": "Provider selection on BDS Marketplace is not by itself proof of a legally executed service contract. Payments are made outside the platform and are not processed, held or guaranteed by BDS Marketplace. Contract values shown are user-declared."
  },
  "timestamp": "2026-11-18T08:44:36.247155300Z"
}
```

### 49. Buyer: engagements

`GET /api/v1/buyer/engagements?page=0&size=10`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "engagementNumber": "BDS-ENG-000001",
        "title": "DPR for pickle unit expansion",
        "rfqId": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "quotationId": 1,
        "quotationNumber": "BDS-QTN-000001",
        "sellerId": 4,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 5,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-28",
        "expectedEndDate": "2026-12-19",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-18T14:14:35.465558",
        "updatedAt": "2026-11-18T14:14:35.694198"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-18T08:44:36.499028900Z"
}
```

### 50. Login (SELLER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "seller.admin.3336@example.com",
  "password": "<redacted>",
  "portal": "SELLER"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent to registered email",
  "data": {
    "otpRequired": true,
    "challengeToken": "Ucm6zFo0o91jtIrB2TbZ...",
    "maskedDestination": "s***************6@example.com",
    "expiresInSeconds": 300,
    "devOtp": "084281"
  },
  "timestamp": "2026-11-18T08:44:37.370491800Z"
}
```

### 51. Verify login OTP (SELLER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "Ucm6zFo0o91jtIrB2TbZGMeFB6j8_0wPk0eIOIipcN8",
  "otp": "084281"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "hMJSwnVU-FkPBMwQxyj2...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 6,
      "fullName": "Ravi Kumar Admin",
      "email": "seller.admin.3336@example.com",
      "mobile": "7641758579",
      "role": "SELLER",
      "status": "ACTIVE",
      "referenceNo": "BDS-SLR-000006",
      "organisationName": "Admin Advisory Services Pvt Ltd",
      "permissions": [
        "ENGAGEMENT_MANAGE",
        "LISTING_PUBLISH",
        "RFQ_RESPOND"
      ],
      "mustChangePassword": true,
      "emailAlerts": true,
      "lastLoginAt": "2026-11-18T14:14:37.7363462"
    }
  },
  "timestamp": "2026-11-18T08:44:38.052694100Z"
}
```

### 52. Login (BUYER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "buyer.admin.3336@example.com",
  "password": "<redacted>",
  "portal": "BUYER"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "OTP sent to registered email",
  "data": {
    "otpRequired": true,
    "challengeToken": "ekdy5fbsPr7HnDochY2l...",
    "maskedDestination": "b**************6@example.com",
    "expiresInSeconds": 300,
    "devOtp": "415797"
  },
  "timestamp": "2026-11-18T08:44:38.929766Z"
}
```

### 53. Verify login OTP (BUYER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "ekdy5fbsPr7HnDochY2lJln5-eCLyPo8Fj6-VfyugjU",
  "otp": "415797"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "YTlfhW3LwJ2Jg31liR2j...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 7,
      "fullName": "Anita Sharma Admin",
      "email": "buyer.admin.3336@example.com",
      "mobile": "7507934177",
      "role": "BUYER",
      "status": "ACTIVE",
      "referenceNo": "BDS-PUR-000007",
      "organisationName": "Admin Agro Foods",
      "designation": "Proprietor",
      "permissions": [
        "FEEDBACK_SUBMIT",
        "QUOTATION_EVALUATE",
        "RFQ_CREATE"
      ],
      "mustChangePassword": true,
      "emailAlerts": true,
      "lastLoginAt": "2026-11-18T14:14:39.2965997"
    }
  },
  "timestamp": "2026-11-18T08:44:39.605197Z"
}
```

### 54. Admin: procurement stats

`GET /api/v1/admin/procurement/stats`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "totalRfqs": 1,
    "activeRfqs": 0,
    "totalQuotations": 1,
    "totalEngagements": 1,
    "activeEngagements": 1,
    "completedEngagements": 0,
    "selectionRate": 100.0,
    "averageQuotationsPerRfq": 1.0,
    "openGrievances": 0,
    "escalatedQueries": 0,
    "overdueQueries": 0,
    "rfqsByStatus": {
      "DRAFT": 0,
      "SUBMITTED": 0,
      "PUBLISHED": 0,
      "RESPONSES_RECEIVED": 0,
      "UNDER_EVALUATION": 0,
      "PROVIDER_SELECTED": 1,
      "CLOSED": 0,
      "CANCELLED": 0,
      "EXPIRED": 0,
      "NO_RESPONSE": 0
    },
    "quotationsByStatus": {
      "DRAFT": 0,
      "SUBMITTED": 0,
      "UNDER_REVIEW": 0,
      "CLARIFICATION_REQUESTED": 0,
      "REVISED": 0,
      "SHORTLISTED": 0,
      "ACCEPTED": 1,
      "REJECTED": 0,
      "WITHDRAWN": 0,
      "EXPIRED": 0
    },
    "engagementsByStatus": {
      "SELECTED": 1,
      "ENGAGEMENT_INITIATED": 0,
      "IN_PROGRESS": 0,
      "DELIVERED": 0,
      "COMPLETED": 0,
      "CANCELLED": 0,
      "DISPUTED": 0
    },
    "rfqsByCategory": [
      {
        "label": "Business & Management Consultancy",
        "count": 1
      }
    ],
    "rfqsByState": [
      {
        "label": "Uttar Pradesh",
        "count": 1
      }
    ],
    "monthlyTrend": [
      {
        "period": "Dec 25",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Jan 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Feb 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Mar 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Apr 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "May 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Jun 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Jul 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Aug 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Sept 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Oct 26",
        "rfqs": 0,
        "quotations": 0,
        "engagements": 0
      },
      {
        "period": "Nov 26",
        "rfqs": 1,
        "quotations": 1,
        "engagements": 1
      }
    ]
  },
  "timestamp": "2026-11-18T08:44:40.211976300Z"
}
```
