# bds-backend API smoke test — payloads

- Target: `http://localhost:8080`
- Run: `8102` at 2026-11-15 19:14
- Result: **73/73 passed**

## Onboarded accounts

| Account | Email | Password | userId | referenceNo |
|---|---|---|---|---|
| Seller | seller.self.7351@example.com | `Test@7351ab` | 31 | — |
| Buyer | buyer.self.7351@example.com | `Test@7351ab` | 32 | — |

## Summary

| # | Result | Status | Method | Path | Step |
|---|---|---|---|---|---|
| 1 | PASS | 200 | POST | `/api/v1/auth/login` | Login (ADMIN) |
| 2 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (ADMIN) |
| 3 | PASS | 200 | POST | `/api/v1/auth/login` | Login (SELLER) |
| 4 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (SELLER) |
| 5 | PASS | 200 | POST | `/api/v1/auth/login` | Login (BUYER) |
| 6 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (BUYER) |
| 7 | PASS | 200 | GET | `/api/v1/auth/me` | Seller: me |
| 8 | PASS | 200 | GET | `/api/v1/auth/me` | Buyer: me |
| 9 | PASS | 200 | GET | `/api/v1/seller/rfqs` | Admin: /api/v1/seller/rfqs |
| 10 | PASS | 200 | GET | `/api/v1/seller/rfqs/1` | Admin: /api/v1/seller/rfqs/1 |
| 11 | PASS | 200 | GET | `/api/v1/seller/quotations` | Admin: /api/v1/seller/quotations |
| 12 | PASS | 200 | GET | `/api/v1/seller/quotations/1` | Admin: /api/v1/seller/quotations/1 |
| 13 | PASS | 200 | GET | `/api/v1/seller/quotations/1/versions` | Admin: /api/v1/seller/quotations/1/versions |
| 14 | PASS | 200 | GET | `/api/v1/seller/quotations/1/pdf` | Admin: /api/v1/seller/quotations/1/pdf |
| 15 | PASS | 200 | GET | `/api/v1/seller/engagements` | Admin: /api/v1/seller/engagements |
| 16 | PASS | 200 | GET | `/api/v1/seller/engagements/1` | Admin: /api/v1/seller/engagements/1 |
| 17 | PASS | 200 | GET | `/api/v1/seller/work-orders` | Admin: /api/v1/seller/work-orders |
| 18 | PASS | 200 | GET | `/api/v1/seller/services` | Admin: /api/v1/seller/services |
| 19 | PASS | 200 | GET | `/api/v1/seller/reviews` | Admin: /api/v1/seller/reviews |
| 20 | PASS | 200 | GET | `/api/v1/buyer/rfqs` | Admin: /api/v1/buyer/rfqs |
| 21 | PASS | 200 | GET | `/api/v1/buyer/rfqs/1` | Admin: /api/v1/buyer/rfqs/1 |
| 22 | PASS | 200 | GET | `/api/v1/buyer/rfqs/1/quotations` | Admin: /api/v1/buyer/rfqs/1/quotations |
| 23 | PASS | 200 | GET | `/api/v1/buyer/rfqs/1/comparison` | Admin: /api/v1/buyer/rfqs/1/comparison |
| 24 | PASS | 200 | GET | `/api/v1/buyer/quotations` | Admin: /api/v1/buyer/quotations |
| 25 | PASS | 200 | GET | `/api/v1/buyer/quotations/1` | Admin: /api/v1/buyer/quotations/1 |
| 26 | PASS | 200 | GET | `/api/v1/buyer/quotations/1/versions` | Admin: /api/v1/buyer/quotations/1/versions |
| 27 | PASS | 200 | GET | `/api/v1/buyer/quotations/1/pdf` | Admin: /api/v1/buyer/quotations/1/pdf |
| 28 | PASS | 200 | GET | `/api/v1/buyer/engagements` | Admin: /api/v1/buyer/engagements |
| 29 | PASS | 200 | GET | `/api/v1/buyer/engagements/1` | Admin: /api/v1/buyer/engagements/1 |
| 30 | PASS | 200 | GET | `/api/v1/buyer/work-orders` | Admin: /api/v1/buyer/work-orders |
| 31 | PASS | 200 | GET | `/api/v1/buyer/feedback/questions` | Admin: /api/v1/buyer/feedback/questions |
| 32 | PASS | 200 | GET | `/api/v1/buyer/recommended-sellers?categoryId=1` | Admin: /api/v1/buyer/recommended-sellers?categoryId=1 |
| 33 | PASS | 400 | GET | `/api/v1/sellers/me/profile` | Admin without target: /api/v1/sellers/me/profile -> 400 |
| 34 | PASS | 400 | GET | `/api/v1/seller/dashboard` | Admin without target: /api/v1/seller/dashboard -> 400 |
| 35 | PASS | 400 | GET | `/api/v1/seller/services/stats` | Admin without target: /api/v1/seller/services/stats -> 400 |
| 36 | PASS | 400 | GET | `/api/v1/buyers/me/profile` | Admin without target: /api/v1/buyers/me/profile -> 400 |
| 37 | PASS | 400 | GET | `/api/v1/buyer/dashboard` | Admin without target: /api/v1/buyer/dashboard -> 400 |
| 38 | PASS | 200 | GET | `/api/v1/sellers/me/profile` | Admin as seller: /api/v1/sellers/me/profile [X-Act-As-User: 31] |
| 39 | PASS | 200 | GET | `/api/v1/sellers/me/verification` | Admin as seller: /api/v1/sellers/me/verification [X-Act-As-User: 31] |
| 40 | PASS | 200 | GET | `/api/v1/sellers/me/documents` | Admin as seller: /api/v1/sellers/me/documents [X-Act-As-User: 31] |
| 41 | PASS | 200 | GET | `/api/v1/seller/dashboard` | Admin as seller: /api/v1/seller/dashboard [X-Act-As-User: 31] |
| 42 | PASS | 200 | GET | `/api/v1/seller/services/stats` | Admin as seller: /api/v1/seller/services/stats [X-Act-As-User: 31] |
| 43 | PASS | 200 | GET | `/api/v1/seller/reviews/summary` | Admin as seller: /api/v1/seller/reviews/summary [X-Act-As-User: 31] |
| 44 | PASS | 200 | GET | `/api/v1/buyers/me/profile` | Admin as buyer: /api/v1/buyers/me/profile [X-Act-As-User: 32] |
| 45 | PASS | 200 | GET | `/api/v1/buyer/dashboard` | Admin as buyer: /api/v1/buyer/dashboard [X-Act-As-User: 32] |
| 46 | PASS | 404 | GET | `/api/v1/sellers/me/profile` | Admin as wrong role: buyer id on seller profile -> 404 [X-Act-As-User: 32] |
| 47 | PASS | 201 | POST | `/api/v1/buyer/rfqs` | Admin creates RFQ on behalf of buyer [X-Act-As-User: 32] |
| 48 | PASS | 400 | POST | `/api/v1/buyer/rfqs` | Admin creates RFQ without target -> 400 |
| 49 | PASS | 200 | GET | `/api/v1/buyer/rfqs/4` | Buyer sees the RFQ the admin raised for them |
| 50 | PASS | 200 | POST | `/api/v1/buyer/rfqs/4/extend-deadline` | Admin extends the buyer's RFQ deadline |
| 51 | PASS | 201 | POST | `/api/v1/seller/rfqs/4/quotations` | Admin submits quotation on behalf of seller [X-Act-As-User: 31] |
| 52 | PASS | 200 | POST | `/api/v1/buyer/quotations/4/shortlist` | Admin shortlists quotation |
| 53 | PASS | 200 | POST | `/api/v1/buyer/rfqs/4/cancel` | Admin cancels the RFQ |
| 54 | PASS | 403 | GET | `/api/v1/buyer/rfqs` | Seller cannot use buyer endpoints -> 403 |
| 55 | PASS | 403 | GET | `/api/v1/seller/quotations` | Buyer cannot use seller endpoints -> 403 |
| 56 | PASS | 403 | GET | `/api/v1/admin/users` | Seller cannot use admin endpoints -> 403 |
| 57 | PASS | 200 | GET | `/api/v1/sellers/me/profile` | Seller ignores act-as header (own profile) [X-Act-As-User: 32] |
| 58 | PASS | 200 | POST | `/api/v1/auth/otp/send` | Self-register second buyer |
| 59 | PASS | 201 | POST | `/api/v1/auth/register/buyer` | Register second buyer |
| 60 | PASS | 200 | POST | `/api/v1/admin/users/35/approve` | Admin approves second buyer |
| 61 | PASS | 200 | POST | `/api/v1/auth/login` | Login (BUYER) |
| 62 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (BUYER) |
| 63 | PASS | 404 | GET | `/api/v1/buyer/rfqs/1` | Other buyer cannot see first buyer's RFQ -> 404 |
| 64 | PASS | 200 | GET | `/api/v1/buyer/rfqs` | Other buyer's RFQ list excludes others |
| 65 | PASS | 201 | POST | `/api/v1/admin/users/admins` | Admin creates sub-admin with one privilege |
| 66 | PASS | 200 | POST | `/api/v1/auth/login` | Login (ADMIN) |
| 67 | PASS | 200 | POST | `/api/v1/auth/login/verify` | Verify login OTP (ADMIN) |
| 68 | PASS | 200 | GET | `/api/v1/admin/users` | Sub-admin: /api/v1/admin/users |
| 69 | PASS | 200 | GET | `/api/v1/admin/audit-logs` | Sub-admin: /api/v1/admin/audit-logs |
| 70 | PASS | 200 | GET | `/api/v1/admin/settings` | Sub-admin: /api/v1/admin/settings |
| 71 | PASS | 200 | GET | `/api/v1/admin/rfqs` | Sub-admin: /api/v1/admin/rfqs |
| 72 | PASS | 200 | GET | `/api/v1/admin/categories` | Sub-admin: /api/v1/admin/categories |
| 73 | PASS | 200 | GET | `/api/v1/seller/quotations` | Sub-admin: /api/v1/seller/quotations |

## Request / response payloads

### 1. Login (ADMIN)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "admin@bdsmarketplace.in",
  "password": "Admin@12345",
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
    "challengeToken": "sazGhL4egX59ATgY1pdt...",
    "maskedDestination": "a***n@bdsmarketplace.in",
    "expiresInSeconds": 300,
    "devOtp": "748084"
  },
  "timestamp": "2026-11-15T13:44:01.161016Z"
}
```

### 2. Verify login OTP (ADMIN)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "sazGhL4egX59ATgY1pdtvrYClJ0dpjJYP2oHjXbyg_4",
  "otp": "748084"
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
    "refreshToken": "CxC6_43zf4I0ZNzqaBQ0...",
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
      "lastLoginAt": "2026-11-15T19:14:01.5411333"
    }
  },
  "timestamp": "2026-11-15T13:44:01.554015200Z"
}
```

### 3. Login (SELLER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "seller.self.7351@example.com",
  "password": "Test@7351ab",
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
    "challengeToken": "E1GEmaNJlM9MlIctU5cs...",
    "maskedDestination": "s**************1@example.com",
    "expiresInSeconds": 300,
    "devOtp": "814627"
  },
  "timestamp": "2026-11-15T13:44:02.308876100Z"
}
```

### 4. Verify login OTP (SELLER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "E1GEmaNJlM9MlIctU5csSi-j81EQcwQSXm0t6-MferY",
  "otp": "814627"
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
    "refreshToken": "LBr4LsOUCjrr2fK3Q7Vz...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 31,
      "fullName": "Ravi Kumar Self",
      "email": "seller.self.7351@example.com",
      "mobile": "7848537465",
      "role": "SELLER",
      "status": "ACTIVE",
      "referenceNo": "BDS-SLR-000031",
      "organisationName": "Self Advisory Services Pvt Ltd",
      "permissions": [
        "ENGAGEMENT_MANAGE",
        "LISTING_PUBLISH",
        "RFQ_RESPOND"
      ],
      "mustChangePassword": false,
      "emailAlerts": true,
      "statusRemarks": "Documents verified, approved",
      "lastLoginAt": "2026-11-15T19:14:02.6588098"
    }
  },
  "timestamp": "2026-11-15T13:44:02.675727Z"
}
```

### 5. Login (BUYER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "buyer.self.7351@example.com",
  "password": "Test@7351ab",
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
    "challengeToken": "yNR_cuVHv0NmAP70HKJ-...",
    "maskedDestination": "b*************1@example.com",
    "expiresInSeconds": 300,
    "devOtp": "919438"
  },
  "timestamp": "2026-11-15T13:44:03.510743400Z"
}
```

### 6. Verify login OTP (BUYER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "yNR_cuVHv0NmAP70HKJ-FZpR2zVqJKqSFx4tXK6dKto",
  "otp": "919438"
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
    "refreshToken": "Azg2Bmki2Wwhfia1L-Fe...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 32,
      "fullName": "Anita Sharma Self",
      "email": "buyer.self.7351@example.com",
      "mobile": "7418233037",
      "role": "BUYER",
      "status": "ACTIVE",
      "referenceNo": "BDS-PUR-000032",
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
      "lastLoginAt": "2026-11-15T19:14:03.8507916"
    }
  },
  "timestamp": "2026-11-15T13:44:03.867374800Z"
}
```

### 7. Seller: me

`GET /api/v1/auth/me`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 31,
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.7351@example.com",
    "mobile": "7848537465",
    "role": "SELLER",
    "status": "ACTIVE",
    "referenceNo": "BDS-SLR-000031",
    "organisationName": "Self Advisory Services Pvt Ltd",
    "permissions": [
      "ENGAGEMENT_MANAGE",
      "LISTING_PUBLISH",
      "RFQ_RESPOND"
    ],
    "mustChangePassword": false,
    "emailAlerts": true,
    "statusRemarks": "Documents verified, approved",
    "lastLoginAt": "2026-11-15T19:14:02.65881"
  },
  "timestamp": "2026-11-15T13:44:03.894111200Z"
}
```

### 8. Buyer: me

`GET /api/v1/auth/me`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 32,
    "fullName": "Anita Sharma Self",
    "email": "buyer.self.7351@example.com",
    "mobile": "7418233037",
    "role": "BUYER",
    "status": "ACTIVE",
    "referenceNo": "BDS-PUR-000032",
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
    "lastLoginAt": "2026-11-15T19:14:03.850792"
  },
  "timestamp": "2026-11-15T13:44:03.925042500Z"
}
```

### 9. Admin: /api/v1/seller/rfqs

`GET /api/v1/seller/rfqs`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T19:13:56",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 0,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:56.442562",
        "publishedAt": "2026-11-15T19:13:56.473072"
      },
      {
        "id": 2,
        "rfqNumber": "BDS-RFQ-000002",
        "title": "Admin-assisted DPR requirement",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "buyerId": 26,
        "buyerName": "Anita Sharma Self",
        "deliveryMode": "ONLINE",
        "budgetMax": 50000,
        "submissionDeadline": "2026-11-24T19:13:31",
        "status": "RESPONSES_RECEIVED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 0,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:30.955397",
        "publishedAt": "2026-11-15T19:13:31.010267"
      },
      {
        "id": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 26,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T18:54:11",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 0,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T18:54:11.306587",
        "publishedAt": "2026-11-15T18:54:11.377372"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:03.963952400Z"
}
```

### 10. Admin: /api/v1/seller/rfqs/1

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
      "buyerId": 26,
      "buyerName": "Self Agro Foods",
      "projectState": "Uttar Pradesh",
      "projectDistrict": "Kanpur Nagar",
      "deliveryMode": "HYBRID",
      "budgetMin": 30000,
      "budgetMax": 60000,
      "submissionDeadline": "2026-11-22T18:54:11",
      "status": "PROVIDER_SELECTED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 0,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T18:54:11.306587",
      "publishedAt": "2026-11-15T18:54:11.377372"
    },
    "description": "Need a bank-ready detailed project report for expanding our pickle manufacturing unit capacity.",
    "scopeOfWork": "Market study, machinery costing, financial projections for 7 years",
    "deliverables": "DPR, CMA data",
    "industry": "Food Processing",
    "expectedDuration": "1 month",
    "requiredExperience": 3,
    "expectedStartDate": "2026-11-25",
    "statusRemarks": "Selected based on experience and price",
    "selectedQuotationId": 1,
    "attachments": [],
    "invitations": []
  },
  "timestamp": "2026-11-15T13:44:03.989513500Z"
}
```

### 11. Admin: /api/v1/seller/quotations

`GET /api/v1/seller/quotations`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 3,
        "quotationNumber": "BDS-QTN-000003",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "rfqTitle": "DPR for pickle unit expansion",
        "rfqStatus": "PROVIDER_SELECTED",
        "rfqDeadline": "2026-11-22T19:13:56",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 32,
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
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:13:56.735895",
        "decisionAt": "2026-11-15T19:13:57.008813",
        "decisionRemarks": "Selected based on experience and price",
        "shortlisted": true,
        "editable": false,
        "items": [
          {
            "id": 3,
            "description": "DPR preparation",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 40000,
            "amount": 40000
          },
          {
            "id": 4,
            "description": "CMA data",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 5000,
            "amount": 5000
          }
        ],
        "attachments": [],
        "createdAt": "2026-11-15T19:13:56.697809",
        "updatedAt": "2026-11-15T19:13:57.011335"
      },
      {
        "id": 2,
        "quotationNumber": "BDS-QTN-000002",
        "rfqId": 2,
        "rfqNumber": "BDS-RFQ-000002",
        "rfqTitle": "Admin-assisted DPR requirement",
        "rfqStatus": "RESPONSES_RECEIVED",
        "rfqDeadline": "2026-11-24T19:13:31",
        "sellerId": 25,
        "sellerName": "Ravi Kumar Self",
        "buyerId": 26,
        "buyerName": "Anita Sharma Self",
        "versionNo": 1,
        "status": "SHORTLISTED",
        "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
        "scopeAccepted": true,
        "deliverables": "DPR document",
        "timelineDays": 20,
        "professionalFee": 40000,
        "taxPercent": 18,
        "taxAmount": 7200,
        "otherExpenses": 0,
        "totalAmount": 47200,
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:13:31.47489",
        "shortlisted": true,
        "editable": false,
        "items": [],
        "attachments": [],
        "createdAt": "2026-11-15T19:13:31.451923",
        "updatedAt": "2026-11-15T19:13:31.576502"
      },
      {
        "id": 1,
        "quotationNumber": "BDS-QTN-000001",
        "rfqId": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "rfqTitle": "DPR for pickle unit expansion",
        "rfqStatus": "PROVIDER_SELECTED",
        "rfqDeadline": "2026-11-22T18:54:11",
        "sellerId": 25,
        "sellerName": "Self Advisory Services Pvt Ltd",
     
... (truncated)
```

### 12. Admin: /api/v1/seller/quotations/1

`GET /api/v1/seller/quotations/1`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "quotationNumber": "BDS-QTN-000001",
    "rfqId": 1,
    "rfqNumber": "BDS-RFQ-000001",
    "rfqTitle": "DPR for pickle unit expansion",
    "rfqStatus": "PROVIDER_SELECTED",
    "rfqDeadline": "2026-11-22T18:54:11",
    "sellerId": 25,
    "sellerName": "Self Advisory Services Pvt Ltd",
    "buyerId": 26,
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
    "validUntil": "2026-12-15",
    "submittedAt": "2026-11-15T18:54:12.007344",
    "decisionAt": "2026-11-15T18:54:12.608935",
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
    "createdAt": "2026-11-15T18:54:11.930971",
    "updatedAt": "2026-11-15T18:54:12.61195"
  },
  "timestamp": "2026-11-15T13:44:04.073435700Z"
}
```

### 13. Admin: /api/v1/seller/quotations/1/versions

`GET /api/v1/seller/quotations/1/versions`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "versionNo": 1,
      "totalAmount": 58100,
      "changeSummary": "Initial submission",
      "snapshot": "{\"id\":1,\"quotationNumber\":\"BDS-QTN-000001\",\"rfqId\":1,\"rfqNumber\":\"BDS-RFQ-000001\",\"rfqTitle\":\"DPR for pickle unit expansion\",\"rfqStatus\":\"PUBLISHED\",\"rfqDeadline\":\"2026-11-22T18:54:11\",\"sellerId\":25,\"sellerName\":\"Self Advisory Services Pvt Ltd\",\"buyerId\":26,\"buyerName\":\"Self Agro Foods\",\"versionNo\":1,\"status\":\"SUBMITTED\",\"technicalProposal\":\"We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.\",\"methodology\":\"Kick-off call, site visit, two review cycles\",\"scopeAccepted\":true,\"deliverables\":\"DPR document, CMA data, projected financials\",\"timelineDays\":21,\"teamComposition\":\"1 CA, 1 industry analyst\",\"relevantExperience\":\"40+ DPRs for food processing units\",\"professionalFee\":45000.00,\"taxPercent\":18,\"taxAmount\":8100.00,\"otherExpenses\":5000.00,\"totalAmount\":58100.00,\"paymentMilestones\":\"50% advance, 50% on delivery\",\"validUntil\":\"2026-12-15\",\"submittedAt\":\"2026-11-15T18:54:12.0073442\",\"shortlisted\":false,\"editable\":false,\"items\":[{\"id\":1,\"description\":\"DPR preparation\",\"quantity\":1,\"unit\":\"lot\",\"unitPrice\":40000,\"amount\":40000.00},{\"id\":2,\"description\":\"CMA data\",\"quantity\":1,\"unit\":\"lot\",\"unitPrice\":5000,\"amount\":5000.00}],\"attachments\":[],\"createdAt\":\"2026-11-15T18:54:11.9309707\",\"updatedAt\":\"2026-11-15T18:54:11.9309707\"}",
      "createdAt": "2026-11-15T18:54:12.007344"
    }
  ],
  "timestamp": "2026-11-15T13:44:04.102223100Z"
}
```

### 14. Admin: /api/v1/seller/quotations/1/pdf

`GET /api/v1/seller/quotations/1/pdf`  — `Authorization: Bearer <token>`

Response `200`:
```json
%PDF-1.5
%����
3 0 obj
<</Filter/FlateDecode/Length 1552>>stream
x��Z]W�8}ϯ���i�ג����ѳۥ���E�JPq�`;���B���,M{R�{5��\9�|Bۇq2�်���)���l�v�m�^��������K8c�/�)����Od��ˮɈoy�t�9^� ���~E7��u&b��op*2�����d��Y��M%��3t��x�s�6�Q.�`�K�c�Y�J!z��F6E�"��y\�9܉4�Xf�".�A!JK��!LYy�sHXɀe	�s>g9�O]��� �,Y���D�q~SX��xS)�xy-�����k�E�7r2\�t�L��NBΗ��A��S^�E��X���[�R,����	��9��D��0�Zpqz���9�X�]5��[+��L"�Ōg�$Gg�����?y�Ě�+�~2f,f(P�����X�i֜��Hΰ���ֳ'pt8��#Ye�ª��UQ�E���%�J8���\𬩉��"����0Ld�/����yQ�l
�L��I�'���ni���	鯛̈�T%�3�հ��>J�g��`��|��A�P�Z:���ҵDӂ_�nז�eMy�mB�d�Ŗ��`��2Gh�ʜ�����9˦\�W�rL����@�D�X��\�S�{;_��5+П��U7������!K��rQxS�����f���d<8��@��"�Bu�穁� 8*��c[^"�4�I�F~���!A�a�@8������0A�c�&�Z
�}F��ߐ<����F.�p�Y�\������?�v�;�]�r��42�~omJN�7Asz�M�?�v��M�?A�3�~o�6����\=���	���.1��A����G%�U>6���$ U�?�޸�'��������ǇfzǦ�g�%Ƽ(��\ǩv�I��GG'��ɱ�E�� <<����|�7����MT��EmU�,\~�@��|I��&��Ӭr�sߤ�2����,+6�P/u��4��ͨ�6�R?QwxJ��	&KQH<o��0Z��w��Jo4��ڮ�o�4�p�#�&!����ԋ��(����Y�Z��'[���M�!����Y��fA���:��Y)�V}Mc>�q��m�1��/J=|o��i�'�
�;����$�
��	B��-B��.���i�����t����t��*u7�յ�7+���8��E1����+���V���(���S�ފ)pŚ��N���f)%���8�R۪)�b�RZ��r���4��I���NA�*�Z�h��t��S哖�p&Z�b�&���1����iu���;Q*��-�h�~� ��'mf���r���Q-��te[�Ҿ��^Y��D�5�N�ݩ�b^��Z��۞R׳(� k�Y���Z>C��@h-`!��Ȭ4ϭkE��V`b��o؇52��G���ld�yk��FF�ˑ��?��՚�ݏj���o�d�R8�S�G�Ůe�֑zQ�H��Ϫ?ޓ��K]Y���+Q�v�(s��)|�o�'Q_'fלW���w��u�T�Ʋd�C�z���ms���V�?���
endstream
endobj
5 0 obj
<</Contents 3 0 R/Type/Page/Resources<</Font<</F1 1 0 R/F2 2 0 R>>>>/Parent 4 0 R/MediaBox[0 0 595 842]>>
endobj
1 0 obj
<</Subtype/Type1/Type/Font/BaseFont/Helvetica-Bold/Encoding/WinAnsiEncoding>>
endobj
2 0 obj
<</Subtype/Type1/Type/Font/BaseFont/Helvetica/Encoding/WinAnsiEncoding>>
endobj
4 0 obj
<</Kids[5 0 R]/Type/Pages/Count 1>>
endobj
6 0 obj
<</Type/Catalog/Pages 4 0 R>>
endobj
7 0 obj
<</CreationDate(D:20261115191404+05'30')/Producer(OpenPDF 2.0.2)>>
endobj
xref
0 8
0000000000 65535 f 
0000001756 00000 n 
0000001849 00000 n 
0000000015 00000 n 
0000001937 00000 n 
0000001635 00000 n 
0000001988 00000 n 
0000002033 00000 n 
trailer
<</Info 7 0 R/ID [<5f257952c13d843d225073efab2de305><5f257952c13d843d225073efab2de305>]/Root 6 0 R/Size 8>>
startxref
2115
%%EOF

```

### 15. Admin: /api/v1/seller/engagements

`GET /api/v1/seller/engagements`  — `Authorization: Bearer <token>`

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
        "engagementNumber": "BDS-ENG-000002",
        "title": "DPR for pickle unit expansion",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "quotationId": 3,
        "quotationNumber": "BDS-QTN-000003",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-25",
        "expectedEndDate": "2026-12-16",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-15T19:13:57.033191",
        "updatedAt": "2026-11-15T19:13:57.066972"
      },
      {
        "id": 1,
        "engagementNumber": "BDS-ENG-000001",
        "title": "DPR for pickle unit expansion",
        "rfqId": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "quotationId": 1,
        "quotationNumber": "BDS-QTN-000001",
        "sellerId": 25,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 26,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-25",
        "expectedEndDate": "2026-12-16",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-15T18:54:12.658519",
        "updatedAt": "2026-11-15T18:54:12.760069"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:04.162753900Z"
}
```

### 16. Admin: /api/v1/seller/engagements/1

`GET /api/v1/seller/engagements/1`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "summary": {
      "id": 1,
      "engagementNumber": "BDS-ENG-000001",
      "title": "DPR for pickle unit expansion",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "quotationId": 1,
      "quotationNumber": "BDS-QTN-000001",
      "sellerId": 25,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "buyerId": 26,
      "buyerName": "Self Agro Foods",
      "quotedValue": 58100,
      "contractValue": 53100,
      "startDate": "2026-11-25",
      "expectedEndDate": "2026-12-16",
      "status": "SELECTED",
      "progressPercent": 0,
      "feedbackSubmitted": false,
      "createdAt": "2026-11-15T18:54:12.658519",
      "updatedAt": "2026-11-15T18:54:12.760069"
    },
    "selectionRemarks": "Selected based on experience and price",
    "quotation": {
      "id": 1,
      "quotationNumber": "BDS-QTN-000001",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "rfqTitle": "DPR for pickle unit expansion",
      "rfqStatus": "PROVIDER_SELECTED",
      "rfqDeadline": "2026-11-22T18:54:11",
      "sellerId": 25,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "buyerId": 26,
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
      "validUntil": "2026-12-15",
      "submittedAt": "2026-11-15T18:54:12.007344",
      "decisionAt": "2026-11-15T18:54:12.608935",
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
      "createdAt": "2026-11-15T18:54:11.930971",
      "updatedAt": "2026-11-15T18:54:12.61195"
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
        "createdAt": "2026-11-15T18:54:12.70944"
      }
    ],
    "documents": [],
    "queries": [],
    "disclaimer": "Provider selection on BDS Marketplace is not by itself proof of a legally executed service contract. Payments are made outside the platform and are not processed, held or guaranteed by BDS Marketplace. Contract values shown are user-declared."
  },
  "timestamp": "2026-11-15T13:44:04.204054500Z"
}
```

### 17. Admin: /api/v1/seller/work-orders

`GET /api/v1/seller/work-orders`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:04.233161600Z"
}
```

### 18. Admin: /api/v1/seller/services

`GET /api/v1/seller/services`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 23,
        "referenceNo": "BDS-SVC-000023",
        "title": "Detailed Project Report for MSME term loans",
        "shortDescription": "Bank-ready DPRs with financial projections",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "categoryIcon": "business_center",
        "subcategoryId": 1,
        "subcategoryName": "Detailed Project Reports",
        "sellerId": 31,
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
        "publishedAt": "2026-11-15T19:13:55.040308",
        "updatedAt": "2026-11-15T19:13:55.050471"
      },
      {
        "id": 22,
        "referenceNo": "BDS-SVC-000022",
        "title": "Detailed Project Report for MSME term loans",
        "shortDescription": "Bank-ready DPRs with financial projections",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "categoryIcon": "business_center",
        "subcategoryId": 1,
        "subcategoryName": "Detailed Project Reports",
        "sellerId": 25,
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
        "publishedAt": "2026-11-15T18:54:09.7132",
        "updatedAt": "2026-11-15T18:54:09.721323"
      },
      {
        "id": 21,
        "referenceNo": "BDS-SVC-000021",
        "title": "Detailed Project Report for MSME term loans",
        "shortDescription": "Bank-ready DPRs with financial projections",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "categoryIcon": "business_center",
        "subcategoryId": 1,
        "subcategoryName": "Detailed Project Reports",
        "sellerId": 21,
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
        "updatedAt": "2026-11-15T18:53:42.183119"
      },
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
        "sellerName": "ABC 
... (truncated)
```

### 19. Admin: /api/v1/seller/reviews

`GET /api/v1/seller/reviews`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:04.294288900Z"
}
```

### 20. Admin: /api/v1/buyer/rfqs

`GET /api/v1/buyer/rfqs`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T19:13:56",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:56.442562",
        "publishedAt": "2026-11-15T19:13:56.473072"
      },
      {
        "id": 2,
        "rfqNumber": "BDS-RFQ-000002",
        "title": "Admin-assisted DPR requirement",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "buyerId": 26,
        "buyerName": "Anita Sharma Self",
        "deliveryMode": "ONLINE",
        "budgetMax": 50000,
        "submissionDeadline": "2026-11-24T19:13:31",
        "status": "RESPONSES_RECEIVED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:30.955397",
        "publishedAt": "2026-11-15T19:13:31.010267"
      },
      {
        "id": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 26,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T18:54:11",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T18:54:11.306587",
        "publishedAt": "2026-11-15T18:54:11.377372"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:04.352148Z"
}
```

### 21. Admin: /api/v1/buyer/rfqs/1

`GET /api/v1/buyer/rfqs/1`  — `Authorization: Bearer <token>`

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
      "buyerId": 26,
      "buyerName": "Self Agro Foods",
      "projectState": "Uttar Pradesh",
      "projectDistrict": "Kanpur Nagar",
      "deliveryMode": "HYBRID",
      "budgetMin": 30000,
      "budgetMax": 60000,
      "submissionDeadline": "2026-11-22T18:54:11",
      "status": "PROVIDER_SELECTED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 1,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T18:54:11.306587",
      "publishedAt": "2026-11-15T18:54:11.377372"
    },
    "description": "Need a bank-ready detailed project report for expanding our pickle manufacturing unit capacity.",
    "scopeOfWork": "Market study, machinery costing, financial projections for 7 years",
    "deliverables": "DPR, CMA data",
    "industry": "Food Processing",
    "expectedDuration": "1 month",
    "requiredExperience": 3,
    "expectedStartDate": "2026-11-25",
    "statusRemarks": "Selected based on experience and price",
    "selectedQuotationId": 1,
    "attachments": [],
    "invitations": [
      {
        "sellerId": 25,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "RESPONDED",
        "invitedBy": "BUYER",
        "invitedAt": "2026-11-15T18:54:11.377372",
        "viewedAt": "2026-11-15T18:54:11.797451",
        "respondedAt": "2026-11-15T18:54:12.189959"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:04.392146600Z"
}
```

### 22. Admin: /api/v1/buyer/rfqs/1/quotations

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
      "rfqStatus": "PROVIDER_SELECTED",
      "rfqDeadline": "2026-11-22T18:54:11",
      "sellerId": 25,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "sellerRatingCount": 0,
      "buyerId": 26,
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
      "validUntil": "2026-12-15",
      "submittedAt": "2026-11-15T18:54:12.007344",
      "decisionAt": "2026-11-15T18:54:12.608935",
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
      "createdAt": "2026-11-15T18:54:11.930971",
      "updatedAt": "2026-11-15T18:54:12.61195"
    }
  ],
  "timestamp": "2026-11-15T13:44:04.442751700Z"
}
```

### 23. Admin: /api/v1/buyer/rfqs/1/comparison

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
      "buyerId": 26,
      "buyerName": "Self Agro Foods",
      "projectState": "Uttar Pradesh",
      "projectDistrict": "Kanpur Nagar",
      "deliveryMode": "HYBRID",
      "budgetMin": 30000,
      "budgetMax": 60000,
      "submissionDeadline": "2026-11-22T18:54:11",
      "status": "PROVIDER_SELECTED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 1,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T18:54:11.306587",
      "publishedAt": "2026-11-15T18:54:11.377372"
    },
    "criteria": [],
    "rows": [
      {
        "quotationId": 1,
        "quotationNumber": "BDS-QTN-000001",
        "sellerId": 25,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "ACCEPTED",
        "shortlisted": true,
        "versionNo": 1,
        "professionalFee": 45000,
        "taxAmount": 8100,
        "otherExpenses": 5000,
        "totalAmount": 58100,
        "timelineDays": 21,
        "validUntil": "2026-12-15",
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
  "timestamp": "2026-11-15T13:44:04.490953600Z"
}
```

### 24. Admin: /api/v1/buyer/quotations

`GET /api/v1/buyer/quotations`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 3,
        "quotationNumber": "BDS-QTN-000003",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "rfqTitle": "DPR for pickle unit expansion",
        "rfqStatus": "PROVIDER_SELECTED",
        "rfqDeadline": "2026-11-22T19:13:56",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "sellerRatingCount": 0,
        "buyerId": 32,
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
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:13:56.735895",
        "decisionAt": "2026-11-15T19:13:57.008813",
        "decisionRemarks": "Selected based on experience and price",
        "shortlisted": true,
        "editable": false,
        "engagementId": 2,
        "items": [
          {
            "id": 3,
            "description": "DPR preparation",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 40000,
            "amount": 40000
          },
          {
            "id": 4,
            "description": "CMA data",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 5000,
            "amount": 5000
          }
        ],
        "attachments": [],
        "createdAt": "2026-11-15T19:13:56.697809",
        "updatedAt": "2026-11-15T19:13:57.011335"
      },
      {
        "id": 2,
        "quotationNumber": "BDS-QTN-000002",
        "rfqId": 2,
        "rfqNumber": "BDS-RFQ-000002",
        "rfqTitle": "Admin-assisted DPR requirement",
        "rfqStatus": "RESPONSES_RECEIVED",
        "rfqDeadline": "2026-11-24T19:13:31",
        "sellerId": 25,
        "sellerName": "Ravi Kumar Self",
        "sellerRatingCount": 0,
        "buyerId": 26,
        "buyerName": "Anita Sharma Self",
        "versionNo": 1,
        "status": "SHORTLISTED",
        "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
        "scopeAccepted": true,
        "deliverables": "DPR document",
        "timelineDays": 20,
        "professionalFee": 40000,
        "taxPercent": 18,
        "taxAmount": 7200,
        "otherExpenses": 0,
        "totalAmount": 47200,
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:13:31.47489",
        "shortlisted": true,
        "editable": false,
        "items": [],
        "attachments": [],
        "createdAt": "2026-11-15T19:13:31.451923",
        "updatedAt": "2026-11-15T19:13:31.576502"
      },
      {
        "id": 1,
        "quotationNumber": "BDS-QTN-000001",
        "rfqId": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "rfqTitle": "DPR for pickle unit expansion",
        "rfqStatus": "PROVIDER_SELECTED",
        "rfqDeadline": "2026-11-22T18:54
... (truncated)
```

### 25. Admin: /api/v1/buyer/quotations/1

`GET /api/v1/buyer/quotations/1`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "quotationNumber": "BDS-QTN-000001",
    "rfqId": 1,
    "rfqNumber": "BDS-RFQ-000001",
    "rfqTitle": "DPR for pickle unit expansion",
    "rfqStatus": "PROVIDER_SELECTED",
    "rfqDeadline": "2026-11-22T18:54:11",
    "sellerId": 25,
    "sellerName": "Self Advisory Services Pvt Ltd",
    "sellerRatingCount": 0,
    "buyerId": 26,
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
    "validUntil": "2026-12-15",
    "submittedAt": "2026-11-15T18:54:12.007344",
    "decisionAt": "2026-11-15T18:54:12.608935",
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
    "createdAt": "2026-11-15T18:54:11.930971",
    "updatedAt": "2026-11-15T18:54:12.61195"
  },
  "timestamp": "2026-11-15T13:44:04.613315600Z"
}
```

### 26. Admin: /api/v1/buyer/quotations/1/versions

`GET /api/v1/buyer/quotations/1/versions`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "versionNo": 1,
      "totalAmount": 58100,
      "changeSummary": "Initial submission",
      "snapshot": "{\"id\":1,\"quotationNumber\":\"BDS-QTN-000001\",\"rfqId\":1,\"rfqNumber\":\"BDS-RFQ-000001\",\"rfqTitle\":\"DPR for pickle unit expansion\",\"rfqStatus\":\"PUBLISHED\",\"rfqDeadline\":\"2026-11-22T18:54:11\",\"sellerId\":25,\"sellerName\":\"Self Advisory Services Pvt Ltd\",\"buyerId\":26,\"buyerName\":\"Self Agro Foods\",\"versionNo\":1,\"status\":\"SUBMITTED\",\"technicalProposal\":\"We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.\",\"methodology\":\"Kick-off call, site visit, two review cycles\",\"scopeAccepted\":true,\"deliverables\":\"DPR document, CMA data, projected financials\",\"timelineDays\":21,\"teamComposition\":\"1 CA, 1 industry analyst\",\"relevantExperience\":\"40+ DPRs for food processing units\",\"professionalFee\":45000.00,\"taxPercent\":18,\"taxAmount\":8100.00,\"otherExpenses\":5000.00,\"totalAmount\":58100.00,\"paymentMilestones\":\"50% advance, 50% on delivery\",\"validUntil\":\"2026-12-15\",\"submittedAt\":\"2026-11-15T18:54:12.0073442\",\"shortlisted\":false,\"editable\":false,\"items\":[{\"id\":1,\"description\":\"DPR preparation\",\"quantity\":1,\"unit\":\"lot\",\"unitPrice\":40000,\"amount\":40000.00},{\"id\":2,\"description\":\"CMA data\",\"quantity\":1,\"unit\":\"lot\",\"unitPrice\":5000,\"amount\":5000.00}],\"attachments\":[],\"createdAt\":\"2026-11-15T18:54:11.9309707\",\"updatedAt\":\"2026-11-15T18:54:11.9309707\"}",
      "createdAt": "2026-11-15T18:54:12.007344"
    }
  ],
  "timestamp": "2026-11-15T13:44:04.642661200Z"
}
```

### 27. Admin: /api/v1/buyer/quotations/1/pdf

`GET /api/v1/buyer/quotations/1/pdf`  — `Authorization: Bearer <token>`

Response `200`:
```json
%PDF-1.5
%����
3 0 obj
<</Filter/FlateDecode/Length 1552>>stream
x��Z]W�8}ϯ���i�ג����ѳۥ���E�JPq�`;���B���,M{R�{5��\9�|Bۇq2�်���)���l�v�m�^��������K8c�/�)����Od��ˮɈoy�t�9^� ���~E7��u&b��op*2�����d��Y��M%��3t��x�s�6�Q.�`�K�c�Y�J!z��F6E�"��y\�9܉4�Xf�".�A!JK��!LYy�sHXɀe	�s>g9�O]��� �,Y���D�q~SX��xS)�xy-�����k�E�7r2\�t�L��NBΗ��A��S^�E��X���[�R,����	��9��D��0�Zpqz���9�X�]5��[+��L"�Ōg�$Gg�����?y�Ě�+�~2f,f(P�����X�i֜��Hΰ���ֳ'pt8��#Ye�ª��UQ�E���%�J8���\𬩉��"����0Ld�/����yQ�l
�L��I�'���ni���	鯛̈�T%�3�հ��>J�g��`��|��A�P�Z:���ҵDӂ_�nז�eMy�mB�d�Ŗ��`��2Gh�ʜ�����9˦\�W�rL����@�D�X��\�S�{;_��5+П��U7������!K��rQxS�����f���d<8��@��"�Bu�穁� 8*��c[^"�4�I�F~���!A�a�@8������0A�c�&�Z
�}F��ߐ<����F.�p�Y�\������?�v�;�]�r��42�~omJN�7Asz�M�?�v��M�?A�3�~o�6����\=���	���.1��A����G%�U>6���$ U�?�޸�'��������ǇfzǦ�g�%Ƽ(��\ǩv�I��GG'��ɱ�E�� <<����|�7����MT��EmU�,\~�@��|I��&��Ӭr�sߤ�2����,+6�P/u��4��ͨ�6�R?QwxJ��	&KQH<o��0Z��w��Jo4��ڮ�o�4�p�#�&!����ԋ��(����Y�Z��'[���M�!����Y��fA���:��Y)�V}Mc>�q��m�1��/J=|o��i�'�
�;����$�
��	B��-B��.���i�����t����t��*u7�յ�7+���8��E1����+���V���(���S�ފ)pŚ��N���f)%���8�R۪)�b�RZ��r���4��I���NA�*�Z�h��t��S哖�p&Z�b�&���1����iu���;Q*��-�h�~� ��'mf���r���Q-��te[�Ҿ��^Y��D�5�N�ݩ�b^��Z��۞R׳(� k�Y���Z>C��@h-`!��Ȭ4ϭkE��V`b��o؇52��G���ld�yk��FF�ˑ��?��՚�ݏj���o�d�R8�S�G�Ůe�֑zQ�H��Ϫ?ޓ��K]Y���+Q�v�(s��)|�o�'Q_'fלW���w��u�T�Ʋd�C�z���ms���V�?���
endstream
endobj
5 0 obj
<</Contents 3 0 R/Type/Page/Resources<</Font<</F1 1 0 R/F2 2 0 R>>>>/Parent 4 0 R/MediaBox[0 0 595 842]>>
endobj
1 0 obj
<</Subtype/Type1/Type/Font/BaseFont/Helvetica-Bold/Encoding/WinAnsiEncoding>>
endobj
2 0 obj
<</Subtype/Type1/Type/Font/BaseFont/Helvetica/Encoding/WinAnsiEncoding>>
endobj
4 0 obj
<</Kids[5 0 R]/Type/Pages/Count 1>>
endobj
6 0 obj
<</Type/Catalog/Pages 4 0 R>>
endobj
7 0 obj
<</CreationDate(D:20261115191404+05'30')/Producer(OpenPDF 2.0.2)>>
endobj
xref
0 8
0000000000 65535 f 
0000001756 00000 n 
0000001849 00000 n 
0000000015 00000 n 
0000001937 00000 n 
0000001635 00000 n 
0000001988 00000 n 
0000002033 00000 n 
trailer
<</Info 7 0 R/ID [<79305d628df49aaa66d44a568bc8fdf1><79305d628df49aaa66d44a568bc8fdf1>]/Root 6 0 R/Size 8>>
startxref
2115
%%EOF

```

### 28. Admin: /api/v1/buyer/engagements

`GET /api/v1/buyer/engagements`  — `Authorization: Bearer <token>`

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
        "engagementNumber": "BDS-ENG-000002",
        "title": "DPR for pickle unit expansion",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "quotationId": 3,
        "quotationNumber": "BDS-QTN-000003",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-25",
        "expectedEndDate": "2026-12-16",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-15T19:13:57.033191",
        "updatedAt": "2026-11-15T19:13:57.066972"
      },
      {
        "id": 1,
        "engagementNumber": "BDS-ENG-000001",
        "title": "DPR for pickle unit expansion",
        "rfqId": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "quotationId": 1,
        "quotationNumber": "BDS-QTN-000001",
        "sellerId": 25,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 26,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-25",
        "expectedEndDate": "2026-12-16",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-15T18:54:12.658519",
        "updatedAt": "2026-11-15T18:54:12.760069"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:04.700945900Z"
}
```

### 29. Admin: /api/v1/buyer/engagements/1

`GET /api/v1/buyer/engagements/1`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "summary": {
      "id": 1,
      "engagementNumber": "BDS-ENG-000001",
      "title": "DPR for pickle unit expansion",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "quotationId": 1,
      "quotationNumber": "BDS-QTN-000001",
      "sellerId": 25,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "buyerId": 26,
      "buyerName": "Self Agro Foods",
      "quotedValue": 58100,
      "contractValue": 53100,
      "startDate": "2026-11-25",
      "expectedEndDate": "2026-12-16",
      "status": "SELECTED",
      "progressPercent": 0,
      "feedbackSubmitted": false,
      "createdAt": "2026-11-15T18:54:12.658519",
      "updatedAt": "2026-11-15T18:54:12.760069"
    },
    "selectionRemarks": "Selected based on experience and price",
    "quotation": {
      "id": 1,
      "quotationNumber": "BDS-QTN-000001",
      "rfqId": 1,
      "rfqNumber": "BDS-RFQ-000001",
      "rfqTitle": "DPR for pickle unit expansion",
      "rfqStatus": "PROVIDER_SELECTED",
      "rfqDeadline": "2026-11-22T18:54:11",
      "sellerId": 25,
      "sellerName": "Self Advisory Services Pvt Ltd",
      "buyerId": 26,
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
      "validUntil": "2026-12-15",
      "submittedAt": "2026-11-15T18:54:12.007344",
      "decisionAt": "2026-11-15T18:54:12.608935",
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
      "createdAt": "2026-11-15T18:54:11.930971",
      "updatedAt": "2026-11-15T18:54:12.61195"
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
        "createdAt": "2026-11-15T18:54:12.70944"
      }
    ],
    "documents": [],
    "queries": [],
    "disclaimer": "Provider selection on BDS Marketplace is not by itself proof of a legally executed service contract. Payments are made outside the platform and are not processed, held or guaranteed by BDS Marketplace. Contract values shown are user-declared."
  },
  "timestamp": "2026-11-15T13:44:04.746307700Z"
}
```

### 30. Admin: /api/v1/buyer/work-orders

`GET /api/v1/buyer/work-orders`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:04.778906300Z"
}
```

### 31. Admin: /api/v1/buyer/feedback/questions

`GET /api/v1/buyer/feedback/questions`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "questionText": "Were the deliverables provided as per the agreed scope?",
      "questionType": "YES_NO",
      "displayOrder": 1,
      "mandatory": true,
      "active": true
    },
    {
      "id": 2,
      "questionText": "Quality of deliverables",
      "questionType": "RATING",
      "displayOrder": 2,
      "mandatory": true,
      "active": true
    },
    {
      "id": 3,
      "questionText": "Timeliness of delivery against the agreed schedule",
      "questionType": "RATING",
      "displayOrder": 3,
      "mandatory": true,
      "active": true
    },
    {
      "id": 4,
      "questionText": "Communication and responsiveness of the provider",
      "questionType": "RATING",
      "displayOrder": 4,
      "mandatory": true,
      "active": true
    },
    {
      "id": 5,
      "questionText": "Value for money",
      "questionType": "RATING",
      "displayOrder": 5,
      "mandatory": false,
      "active": true
    },
    {
      "id": 6,
      "questionText": "Would you engage this provider again?",
      "questionType": "YES_NO",
      "displayOrder": 6,
      "mandatory": true,
      "active": true
    },
    {
      "id": 7,
      "questionText": "What could the provider have done better?",
      "questionType": "TEXT",
      "displayOrder": 7,
      "mandatory": false,
      "active": true
    }
  ],
  "timestamp": "2026-11-15T13:44:04.790444800Z"
}
```

### 32. Admin: /api/v1/buyer/recommended-sellers?categoryId=1

`GET /api/v1/buyer/recommended-sellers?categoryId=1`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "sellerId": 25,
      "name": "Self Advisory Services Pvt Ltd",
      "state": "Uttar Pradesh",
      "district": "Lucknow",
      "ratingCount": 0
    },
    {
      "sellerId": 31,
      "name": "Self Advisory Services Pvt Ltd",
      "state": "Uttar Pradesh",
      "district": "Lucknow",
      "ratingCount": 0
    },
    {
      "sellerId": 2,
      "name": "ABC Business Consulting Pvt Ltd",
      "state": "Uttar Pradesh",
      "district": "Lucknow",
      "ratingCount": 0
    }
  ],
  "timestamp": "2026-11-15T13:44:04.917020200Z"
}
```

### 33. Admin without target: /api/v1/sellers/me/profile -> 400

`GET /api/v1/sellers/me/profile`  — `Authorization: Bearer <token>`

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Administrators must specify the target user in the X-Act-As-User header",
  "timestamp": "2026-11-15T13:44:04.922540100Z",
  "path": "/api/v1/sellers/me/profile"
}
```

### 34. Admin without target: /api/v1/seller/dashboard -> 400

`GET /api/v1/seller/dashboard`  — `Authorization: Bearer <token>`

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Administrators must specify the target user in the X-Act-As-User header",
  "timestamp": "2026-11-15T13:44:04.927640200Z",
  "path": "/api/v1/seller/dashboard"
}
```

### 35. Admin without target: /api/v1/seller/services/stats -> 400

`GET /api/v1/seller/services/stats`  — `Authorization: Bearer <token>`

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Administrators must specify the target user in the X-Act-As-User header",
  "timestamp": "2026-11-15T13:44:04.947551900Z",
  "path": "/api/v1/seller/services/stats"
}
```

### 36. Admin without target: /api/v1/buyers/me/profile -> 400

`GET /api/v1/buyers/me/profile`  — `Authorization: Bearer <token>`

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Administrators must specify the target user in the X-Act-As-User header",
  "timestamp": "2026-11-15T13:44:04.977535800Z",
  "path": "/api/v1/buyers/me/profile"
}
```

### 37. Admin without target: /api/v1/buyer/dashboard -> 400

`GET /api/v1/buyer/dashboard`  — `Authorization: Bearer <token>`

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Administrators must specify the target user in the X-Act-As-User header",
  "timestamp": "2026-11-15T13:44:04.992451300Z",
  "path": "/api/v1/buyer/dashboard"
}
```

### 38. Admin as seller: /api/v1/sellers/me/profile [X-Act-As-User: 31]

`GET /api/v1/sellers/me/profile`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 31,
    "referenceNo": "BDS-SLR-000031",
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.7351@example.com",
    "mobile": "7848537465",
    "status": "ACTIVE",
    "statusRemarks": "Documents verified, approved",
    "organisationName": "Self Advisory Services Pvt Ltd",
    "constitution": "PRIVATE_LIMITED",
    "pan": "XNBCY7034E",
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
    "verifiedAt": "2026-11-15T19:13:53.150004",
    "reverificationDue": "2027-11-15",
    "profileCompletion": 65,
    "registeredAt": "2026-11-15T19:13:50.721712"
  },
  "timestamp": "2026-11-15T13:44:05.004159800Z"
}
```

### 39. Admin as seller: /api/v1/sellers/me/verification [X-Act-As-User: 31]

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
        "id": 20,
        "action": "APPROVE",
        "fromStatus": "PENDING_VERIFICATION",
        "toStatus": "ACTIVE",
        "remarks": "Documents verified, approved",
        "actorName": "Platform Administrator",
        "createdAt": "2026-11-15T19:13:53.150004"
      },
      {
        "id": 16,
        "action": "SUBMITTED",
        "toStatus": "PENDING_VERIFICATION",
        "remarks": "Application submitted for verification",
        "actorName": "Ravi Kumar Self",
        "createdAt": "2026-11-15T19:13:50.825197"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:05.019666600Z"
}
```

### 40. Admin as seller: /api/v1/sellers/me/documents [X-Act-As-User: 31]

`GET /api/v1/sellers/me/documents`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 6,
      "documentType": "REGISTRATION_CERTIFICATE",
      "originalName": "coi.pdf",
      "contentType": "application/pdf",
      "fileSize": 108,
      "verified": true,
      "remarks": "Document checked",
      "uploadedAt": "2026-11-15T19:13:50.819758"
    },
    {
      "id": 5,
      "documentType": "PAN",
      "originalName": "pan.pdf",
      "contentType": "application/pdf",
      "fileSize": 88,
      "verified": true,
      "remarks": "Document checked",
      "uploadedAt": "2026-11-15T19:13:50.78635"
    }
  ],
  "timestamp": "2026-11-15T13:44:05.028957600Z"
}
```

### 41. Admin as seller: /api/v1/seller/dashboard [X-Act-As-User: 31]

`GET /api/v1/seller/dashboard`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "pendingRfqs": 0,
    "totalInvitations": 1,
    "quotationsSubmitted": 1,
    "quotationsAccepted": 1,
    "activeEngagements": 1,
    "completedEngagements": 0,
    "workOrdersAwaited": 0,
    "openQueries": 0,
    "reviewCount": 0,
    "responseRate": 100.0,
    "selectionRate": 100.0,
    "monthlyEnquiries": [
      {
        "period": "Jun 26",
        "value": 0
      },
      {
        "period": "Jul 26",
        "value": 0
      },
      {
        "period": "Aug 26",
        "value": 0
      },
      {
        "period": "Sept 26",
        "value": 0
      },
      {
        "period": "Oct 26",
        "value": 0
      },
      {
        "period": "Nov 26",
        "value": 1
      }
    ],
    "recentRfqs": [
      {
        "id": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T19:13:56",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 0,
        "deadlinePassed": false,
        "myInvitationStatus": "RESPONDED",
        "myQuotationStatus": "ACCEPTED",
        "myQuotationId": 3,
        "createdAt": "2026-11-15T19:13:56.442562",
        "publishedAt": "2026-11-15T19:13:56.473072"
      }
    ],
    "recentQuotations": [
      {
        "id": 3,
        "quotationNumber": "BDS-QTN-000003",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "rfqTitle": "DPR for pickle unit expansion",
        "rfqStatus": "PROVIDER_SELECTED",
        "rfqDeadline": "2026-11-22T19:13:56",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 32,
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
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:13:56.735895",
        "decisionAt": "2026-11-15T19:13:57.008813",
        "decisionRemarks": "Selected based on experience and price",
        "shortlisted": true,
        "editable": false,
        "items": [
          {
            "id": 3,
            "description": "DPR preparation",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 40000,
            "amount": 40000
          },
          {
            "id": 4,
            "description": "CMA data",
            "quantity": 1,
            "unit": 
... (truncated)
```

### 42. Admin as seller: /api/v1/seller/services/stats [X-Act-As-User: 31]

`GET /api/v1/seller/services/stats`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "total": 1,
    "published": 1,
    "pending": 0,
    "views": 1,
    "enquiries": 0,
    "byStatus": {
      "DRAFT": 0,
      "SUBMITTED": 0,
      "UNDER_REVIEW": 0,
      "CORRECTION_REQUESTED": 0,
      "APPROVED": 1,
      "REJECTED": 0,
      "DEACTIVATED": 0,
      "FLAGGED": 0
    }
  },
  "timestamp": "2026-11-15T13:44:05.153146500Z"
}
```

### 43. Admin as seller: /api/v1/seller/reviews/summary [X-Act-As-User: 31]

`GET /api/v1/seller/reviews/summary`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "sellerId": 31,
    "average": 0.0,
    "count": 0,
    "distribution": {
      "5": 0,
      "4": 0,
      "3": 0,
      "2": 0,
      "1": 0
    }
  },
  "timestamp": "2026-11-15T13:44:05.184077900Z"
}
```

### 44. Admin as buyer: /api/v1/buyers/me/profile [X-Act-As-User: 32]

`GET /api/v1/buyers/me/profile`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 32,
    "referenceNo": "BDS-PUR-000032",
    "fullName": "Anita Sharma Self",
    "email": "buyer.self.7351@example.com",
    "mobile": "7418233037",
    "status": "ACTIVE",
    "statusRemarks": "Approved",
    "organisationName": "Self Agro Foods",
    "constitution": "PROPRIETORSHIP",
    "purchaserType": "MICRO",
    "contactPerson": "Anita Sharma Self",
    "designation": "Proprietor",
    "pan": "GARPK3435L",
    "industrySector": "Food Processing",
    "businessActivity": "Pickles and spices manufacturing",
    "addressLine": "45, Industrial Area Phase 2",
    "city": "Kanpur",
    "state": "Uttar Pradesh",
    "district": "Kanpur Nagar",
    "pincode": "208001",
    "registeredAt": "2026-11-15T19:13:51.965944"
  },
  "timestamp": "2026-11-15T13:44:05.285658400Z"
}
```

### 45. Admin as buyer: /api/v1/buyer/dashboard [X-Act-As-User: 32]

`GET /api/v1/buyer/dashboard`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "totalRfqs": 1,
    "openRfqs": 0,
    "quotationsReceived": 1,
    "comparisonPending": 0,
    "selectedProviders": 1,
    "activeEngagements": 1,
    "completedEngagements": 0,
    "workOrderRequests": 0,
    "deliveriesToConfirm": 0,
    "feedbackPending": 0,
    "openQueries": 0,
    "recentRfqs": [
      {
        "id": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T19:13:56",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:56.442562",
        "publishedAt": "2026-11-15T19:13:56.473072"
      }
    ],
    "recentEngagements": [
      {
        "id": 2,
        "engagementNumber": "BDS-ENG-000002",
        "title": "DPR for pickle unit expansion",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "quotationId": 3,
        "quotationNumber": "BDS-QTN-000003",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-25",
        "expectedEndDate": "2026-12-16",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-15T19:13:57.033191",
        "updatedAt": "2026-11-15T19:13:57.066972"
      },
      {
        "id": 1,
        "engagementNumber": "BDS-ENG-000001",
        "title": "DPR for pickle unit expansion",
        "rfqId": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "quotationId": 1,
        "quotationNumber": "BDS-QTN-000001",
        "sellerId": 25,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 26,
        "buyerName": "Self Agro Foods",
        "quotedValue": 58100,
        "contractValue": 53100,
        "startDate": "2026-11-25",
        "expectedEndDate": "2026-12-16",
        "status": "SELECTED",
        "progressPercent": 0,
        "feedbackSubmitted": false,
        "createdAt": "2026-11-15T18:54:12.658519",
        "updatedAt": "2026-11-15T18:54:12.760069"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:05.338712300Z"
}
```

### 46. Admin as wrong role: buyer id on seller profile -> 404 [X-Act-As-User: 32]

`GET /api/v1/sellers/me/profile`  — `Authorization: Bearer <token>`

Response `404`:
```json
{
  "success": false,
  "status": 404,
  "message": "Seller profile not found",
  "timestamp": "2026-11-15T13:44:05.351006500Z",
  "path": "/api/v1/sellers/me/profile"
}
```

### 47. Admin creates RFQ on behalf of buyer [X-Act-As-User: 32]

`POST /api/v1/buyer/rfqs`  — `Authorization: Bearer <token>`

Request:
```json
{
  "title": "Admin-assisted DPR requirement",
  "categoryId": 1,
  "description": "Raised by the administrator on behalf of the purchaser for a food processing DPR.",
  "deliveryMode": "ONLINE",
  "budgetMax": 50000,
  "submissionDeadline": "2026-11-20T19:14:05",
  "distributionMode": "SELECTED",
  "sellerIds": [
    31
  ],
  "publish": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "RFQ BDS-RFQ-000004 submitted",
  "data": {
    "summary": {
      "id": 4,
      "rfqNumber": "BDS-RFQ-000004",
      "title": "Admin-assisted DPR requirement",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "buyerId": 32,
      "buyerName": "Anita Sharma Self",
      "deliveryMode": "ONLINE",
      "budgetMax": 50000,
      "submissionDeadline": "2026-11-20T19:14:05",
      "status": "PUBLISHED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 0,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T19:14:05.3737802",
      "publishedAt": "2026-11-15T19:14:05.3911565"
    },
    "description": "Raised by the administrator on behalf of the purchaser for a food processing DPR.",
    "attachments": [],
    "invitations": [
      {
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "INVITED",
        "invitedBy": "BUYER",
        "invitedAt": "2026-11-15T19:14:05.3901654"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:05.429301700Z"
}
```

### 48. Admin creates RFQ without target -> 400

`POST /api/v1/buyer/rfqs`  — `Authorization: Bearer <token>`

Request:
```json
{
  "title": "Missing target",
  "categoryId": 1,
  "description": "This request names no purchaser so it must be refused.",
  "submissionDeadline": "2026-11-20T19:14:05",
  "distributionMode": "SELECTED",
  "publish": false
}
```

Response `400`:
```json
{
  "success": false,
  "status": 400,
  "message": "Administrators must specify the target user in the X-Act-As-User header",
  "timestamp": "2026-11-15T13:44:05.438962100Z",
  "path": "/api/v1/buyer/rfqs"
}
```

### 49. Buyer sees the RFQ the admin raised for them

`GET /api/v1/buyer/rfqs/4`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "summary": {
      "id": 4,
      "rfqNumber": "BDS-RFQ-000004",
      "title": "Admin-assisted DPR requirement",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "buyerId": 32,
      "buyerName": "Anita Sharma Self",
      "deliveryMode": "ONLINE",
      "budgetMax": 50000,
      "submissionDeadline": "2026-11-20T19:14:05",
      "status": "PUBLISHED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 0,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T19:14:05.37378",
      "publishedAt": "2026-11-15T19:14:05.391157"
    },
    "description": "Raised by the administrator on behalf of the purchaser for a food processing DPR.",
    "attachments": [],
    "invitations": [
      {
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "INVITED",
        "invitedBy": "BUYER",
        "invitedAt": "2026-11-15T19:14:05.390165"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:05.480464500Z"
}
```

### 50. Admin extends the buyer's RFQ deadline

`POST /api/v1/buyer/rfqs/4/extend-deadline`  — `Authorization: Bearer <token>`

Request:
```json
{
  "submissionDeadline": "2026-11-24T19:14:05",
  "remarks": "Extended by administrator"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Deadline extended",
  "data": {
    "summary": {
      "id": 4,
      "rfqNumber": "BDS-RFQ-000004",
      "title": "Admin-assisted DPR requirement",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "buyerId": 32,
      "buyerName": "Anita Sharma Self",
      "deliveryMode": "ONLINE",
      "budgetMax": 50000,
      "submissionDeadline": "2026-11-24T19:14:05",
      "status": "PUBLISHED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 0,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T19:14:05.37378",
      "publishedAt": "2026-11-15T19:14:05.391157"
    },
    "description": "Raised by the administrator on behalf of the purchaser for a food processing DPR.",
    "attachments": [],
    "invitations": [
      {
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "INVITED",
        "invitedBy": "BUYER",
        "invitedAt": "2026-11-15T19:14:05.390165"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:05.535614200Z"
}
```

### 51. Admin submits quotation on behalf of seller [X-Act-As-User: 31]

`POST /api/v1/seller/rfqs/4/quotations`  — `Authorization: Bearer <token>`

Request:
```json
{
  "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
  "deliverables": "DPR document",
  "timelineDays": 20,
  "professionalFee": 40000,
  "taxPercent": 18,
  "validUntil": "2026-12-15",
  "submit": true
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Quotation BDS-QTN-000004 submitted",
  "data": {
    "id": 4,
    "quotationNumber": "BDS-QTN-000004",
    "rfqId": 4,
    "rfqNumber": "BDS-RFQ-000004",
    "rfqTitle": "Admin-assisted DPR requirement",
    "rfqStatus": "RESPONSES_RECEIVED",
    "rfqDeadline": "2026-11-24T19:14:05",
    "sellerId": 31,
    "sellerName": "Ravi Kumar Self",
    "buyerId": 32,
    "buyerName": "Anita Sharma Self",
    "versionNo": 1,
    "status": "SUBMITTED",
    "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
    "scopeAccepted": true,
    "deliverables": "DPR document",
    "timelineDays": 20,
    "professionalFee": 40000.0,
    "taxPercent": 18,
    "taxAmount": 7200.0,
    "otherExpenses": 0,
    "totalAmount": 47200.0,
    "validUntil": "2026-12-15",
    "submittedAt": "2026-11-15T19:14:05.5812726",
    "shortlisted": false,
    "editable": true,
    "items": [],
    "attachments": [],
    "createdAt": "2026-11-15T19:14:05.5762672",
    "updatedAt": "2026-11-15T19:14:05.586957"
  },
  "timestamp": "2026-11-15T13:44:05.602922400Z"
}
```

### 52. Admin shortlists quotation

`POST /api/v1/buyer/quotations/4/shortlist`  — `Authorization: Bearer <token>`

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
    "id": 4,
    "quotationNumber": "BDS-QTN-000004",
    "rfqId": 4,
    "rfqNumber": "BDS-RFQ-000004",
    "rfqTitle": "Admin-assisted DPR requirement",
    "rfqStatus": "RESPONSES_RECEIVED",
    "rfqDeadline": "2026-11-24T19:14:05",
    "sellerId": 31,
    "sellerName": "Ravi Kumar Self",
    "sellerRatingCount": 0,
    "buyerId": 32,
    "buyerName": "Anita Sharma Self",
    "versionNo": 1,
    "status": "SHORTLISTED",
    "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
    "scopeAccepted": true,
    "deliverables": "DPR document",
    "timelineDays": 20,
    "professionalFee": 40000,
    "taxPercent": 18,
    "taxAmount": 7200,
    "otherExpenses": 0,
    "totalAmount": 47200,
    "validUntil": "2026-12-15",
    "submittedAt": "2026-11-15T19:14:05.581273",
    "shortlisted": true,
    "editable": false,
    "items": [],
    "attachments": [],
    "createdAt": "2026-11-15T19:14:05.576267",
    "updatedAt": "2026-11-15T19:14:05.6538556"
  },
  "timestamp": "2026-11-15T13:44:05.665043900Z"
}
```

### 53. Admin cancels the RFQ

`POST /api/v1/buyer/rfqs/4/cancel`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "Admin access test clean-up"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "RFQ cancelled",
  "data": {
    "summary": {
      "id": 4,
      "rfqNumber": "BDS-RFQ-000004",
      "title": "Admin-assisted DPR requirement",
      "categoryId": 1,
      "categoryName": "Business & Management Consultancy",
      "buyerId": 32,
      "buyerName": "Anita Sharma Self",
      "deliveryMode": "ONLINE",
      "budgetMax": 50000,
      "submissionDeadline": "2026-11-24T19:14:05",
      "status": "CANCELLED",
      "distributionMode": "SELECTED",
      "invitedCount": 1,
      "quotationCount": 1,
      "deadlinePassed": false,
      "createdAt": "2026-11-15T19:14:05.37378",
      "publishedAt": "2026-11-15T19:14:05.391157"
    },
    "description": "Raised by the administrator on behalf of the purchaser for a food processing DPR.",
    "statusRemarks": "Admin access test clean-up",
    "attachments": [],
    "invitations": [
      {
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "status": "RESPONDED",
        "invitedBy": "BUYER",
        "invitedAt": "2026-11-15T19:14:05.390165",
        "respondedAt": "2026-11-15T19:14:05.585421"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:05.728398600Z"
}
```

### 54. Seller cannot use buyer endpoints -> 403

`GET /api/v1/buyer/rfqs`  — `Authorization: Bearer <token>`

Response `403`:
```json
{
  "success": false,
  "status": 403,
  "message": "You do not have permission to perform this action",
  "timestamp": "2026-11-15T13:44:05.752034900Z",
  "path": "/api/v1/buyer/rfqs"
}
```

### 55. Buyer cannot use seller endpoints -> 403

`GET /api/v1/seller/quotations`  — `Authorization: Bearer <token>`

Response `403`:
```json
{
  "success": false,
  "status": 403,
  "message": "You do not have permission to perform this action",
  "timestamp": "2026-11-15T13:44:05.768368Z",
  "path": "/api/v1/seller/quotations"
}
```

### 56. Seller cannot use admin endpoints -> 403

`GET /api/v1/admin/users`  — `Authorization: Bearer <token>`

Response `403`:
```json
{
  "success": false,
  "status": 403,
  "message": "You do not have permission to perform this action",
  "timestamp": "2026-11-15T13:44:05.799575700Z",
  "path": "/api/v1/admin/users"
}
```

### 57. Seller ignores act-as header (own profile) [X-Act-As-User: 32]

`GET /api/v1/sellers/me/profile`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 31,
    "referenceNo": "BDS-SLR-000031",
    "fullName": "Ravi Kumar Self",
    "email": "seller.self.7351@example.com",
    "mobile": "7848537465",
    "status": "ACTIVE",
    "statusRemarks": "Documents verified, approved",
    "organisationName": "Self Advisory Services Pvt Ltd",
    "constitution": "PRIVATE_LIMITED",
    "pan": "XNBCY7034E",
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
    "verifiedAt": "2026-11-15T19:13:53.150004",
    "reverificationDue": "2027-11-15",
    "profileCompletion": 65,
    "registeredAt": "2026-11-15T19:13:50.721712"
  },
  "timestamp": "2026-11-15T13:44:05.808042900Z"
}
```

### 58. Self-register second buyer

`POST /api/v1/auth/otp/send`

Request:
```json
{
  "target": "buyer.other.8102@example.com",
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
    "maskedTarget": "b**************2@example.com",
    "expiresInSeconds": 300,
    "resendAfterSeconds": 30,
    "devOtp": "344822"
  },
  "timestamp": "2026-11-15T13:44:06.185994900Z"
}
```

### 59. Register second buyer

`POST /api/v1/auth/register/buyer`

Request:
```json
{
  "fullName": "Anita Sharma Other",
  "email": "buyer.other.8102@example.com",
  "mobile": "9133634906",
  "password": "Test@8102ab",
  "emailOtp": "344822",
  "organisationName": "Other Agro Foods",
  "constitution": "PROPRIETORSHIP",
  "purchaserType": "MICRO",
  "designation": "Proprietor",
  "pan": "IARPF2255Y",
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
    "userId": 35,
    "referenceNo": "BDS-PUR-000035",
    "status": "PENDING_VERIFICATION",
    "message": "Registration submitted. Your account BDS-PUR-000035 will be activated after verification."
  },
  "timestamp": "2026-11-15T13:44:06.940151700Z"
}
```

### 60. Admin approves second buyer

`POST /api/v1/admin/users/35/approve`  — `Authorization: Bearer <token>`

Request:
```json
{
  "remarks": "ok"
}
```

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Account approved",
  "data": {
    "id": 35,
    "referenceNo": "BDS-PUR-000035",
    "fullName": "Anita Sharma Other",
    "email": "buyer.other.8102@example.com",
    "mobile": "9133634906",
    "role": "BUYER",
    "status": "ACTIVE",
    "statusRemarks": "ok",
    "designation": "Proprietor",
    "permissions": [
      "FEEDBACK_SUBMIT",
      "QUOTATION_EVALUATE",
      "RFQ_CREATE"
    ],
    "emailVerified": true,
    "locked": false,
    "registeredAt": "2026-11-15T19:14:06.9159",
    "buyerProfile": {
      "userId": 35,
      "referenceNo": "BDS-PUR-000035",
      "fullName": "Anita Sharma Other",
      "email": "buyer.other.8102@example.com",
      "mobile": "9133634906",
      "status": "ACTIVE",
      "statusRemarks": "ok",
      "organisationName": "Other Agro Foods",
      "constitution": "PROPRIETORSHIP",
      "purchaserType": "MICRO",
      "contactPerson": "Anita Sharma Other",
      "designation": "Proprietor",
      "pan": "IARPF2255Y",
      "industrySector": "Food Processing",
      "businessActivity": "Pickles and spices manufacturing",
      "addressLine": "45, Industrial Area Phase 2",
      "city": "Kanpur",
      "state": "Uttar Pradesh",
      "district": "Kanpur Nagar",
      "pincode": "208001",
      "registeredAt": "2026-11-15T19:14:06.920876"
    },
    "documents": [],
    "checklist": [],
    "history": [
      {
        "id": 23,
        "action": "APPROVE",
        "fromStatus": "PENDING_VERIFICATION",
        "toStatus": "ACTIVE",
        "remarks": "ok",
        "actorName": "Platform Administrator",
        "createdAt": "2026-11-15T19:14:06.9510397"
      },
      {
        "id": 22,
        "action": "SUBMITTED",
        "toStatus": "PENDING_VERIFICATION",
        "remarks": "Purchaser registration submitted",
        "actorName": "Anita Sharma Other",
        "createdAt": "2026-11-15T19:14:06.92478"
      }
    ]
  },
  "timestamp": "2026-11-15T13:44:06.974767700Z"
}
```

### 61. Login (BUYER)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "buyer.other.8102@example.com",
  "password": "Test@8102ab",
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
    "challengeToken": "uSlijspjWq4TrYx_aOQV...",
    "maskedDestination": "b**************2@example.com",
    "expiresInSeconds": 300,
    "devOtp": "391692"
  },
  "timestamp": "2026-11-15T13:44:07.678568Z"
}
```

### 62. Verify login OTP (BUYER)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "uSlijspjWq4TrYx_aOQVFkVSqMAhXAlrjvurcGMocIE",
  "otp": "391692"
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
    "refreshToken": "qmUqmVK2E8oFxByzmGHO...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 35,
      "fullName": "Anita Sharma Other",
      "email": "buyer.other.8102@example.com",
      "mobile": "9133634906",
      "role": "BUYER",
      "status": "ACTIVE",
      "referenceNo": "BDS-PUR-000035",
      "organisationName": "Other Agro Foods",
      "designation": "Proprietor",
      "permissions": [
        "FEEDBACK_SUBMIT",
        "QUOTATION_EVALUATE",
        "RFQ_CREATE"
      ],
      "mustChangePassword": false,
      "emailAlerts": true,
      "statusRemarks": "ok",
      "lastLoginAt": "2026-11-15T19:14:08.0480582"
    }
  },
  "timestamp": "2026-11-15T13:44:08.065626200Z"
}
```

### 63. Other buyer cannot see first buyer's RFQ -> 404

`GET /api/v1/buyer/rfqs/1`  — `Authorization: Bearer <token>`

Response `404`:
```json
{
  "success": false,
  "status": 404,
  "message": "RFQ not found",
  "timestamp": "2026-11-15T13:44:08.092430700Z",
  "path": "/api/v1/buyer/rfqs/1"
}
```

### 64. Other buyer's RFQ list excludes others

`GET /api/v1/buyer/rfqs`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:08.122999400Z"
}
```

### 65. Admin creates sub-admin with one privilege

`POST /api/v1/admin/users/admins`  — `Authorization: Bearer <token>`

Request:
```json
{
  "fullName": "Reports Only Admin",
  "email": "subadmin.8102@example.com",
  "designation": "Analyst",
  "permissions": [
    "REPORT_VIEW"
  ]
}
```

Response `201`:
```json
{
  "success": true,
  "status": 201,
  "message": "Administrator created",
  "data": {
    "userId": 36,
    "referenceNo": "BDS-ADM-000036",
    "temporaryPassword": "U%TqF2RaLL8a"
  },
  "timestamp": "2026-11-15T13:44:08.506465600Z"
}
```

### 66. Login (ADMIN)

`POST /api/v1/auth/login`

Request:
```json
{
  "username": "subadmin.8102@example.com",
  "password": "U%TqF2RaLL8a",
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
    "challengeToken": "RuYPDJitceSCqTc_hxjK...",
    "maskedDestination": "s***********2@example.com",
    "expiresInSeconds": 300,
    "devOtp": "428712"
  },
  "timestamp": "2026-11-15T13:44:09.275824100Z"
}
```

### 67. Verify login OTP (ADMIN)

`POST /api/v1/auth/login/verify`

Request:
```json
{
  "challengeToken": "RuYPDJitceSCqTc_hxjKxlQgxC0wAN0vu1YIBkbdlyI",
  "otp": "428712"
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
    "refreshToken": "wrAfgNOjtcD9PBC829j1...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 36,
      "fullName": "Reports Only Admin",
      "email": "subadmin.8102@example.com",
      "role": "ADMIN",
      "status": "ACTIVE",
      "referenceNo": "BDS-ADM-000036",
      "organisationName": "BDS Marketplace",
      "designation": "Analyst",
      "permissions": [
        "REPORT_VIEW"
      ],
      "mustChangePassword": true,
      "emailAlerts": true,
      "lastLoginAt": "2026-11-15T19:14:09.6511237"
    }
  },
  "timestamp": "2026-11-15T13:44:09.667119800Z"
}
```

### 68. Sub-admin: /api/v1/admin/users

`GET /api/v1/admin/users`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 36,
        "referenceNo": "BDS-ADM-000036",
        "fullName": "Reports Only Admin",
        "email": "s***********2@example.com",
        "role": "ADMIN",
        "status": "ACTIVE",
        "typeLabel": "Analyst",
        "permissions": [
          "REPORT_VIEW"
        ],
        "registeredAt": "2026-11-15T19:14:08.490705",
        "lastLoginAt": "2026-11-15T19:14:09.651124"
      },
      {
        "id": 35,
        "referenceNo": "BDS-PUR-000035",
        "fullName": "Anita Sharma Other",
        "organisationName": "Other Agro Foods",
        "email": "b**************2@example.com",
        "mobile": "91******06",
        "role": "BUYER",
        "status": "ACTIVE",
        "state": "Uttar Pradesh",
        "district": "Kanpur Nagar",
        "typeLabel": "MICRO",
        "permissions": [
          "FEEDBACK_SUBMIT",
          "QUOTATION_EVALUATE",
          "RFQ_CREATE"
        ],
        "registeredAt": "2026-11-15T19:14:06.9159",
        "lastLoginAt": "2026-11-15T19:14:08.048058"
      },
      {
        "id": 34,
        "referenceNo": "BDS-PUR-000034",
        "fullName": "Anita Sharma Admin",
        "organisationName": "Admin Agro Foods",
        "email": "b**************1@example.com",
        "mobile": "75******00",
        "role": "BUYER",
        "status": "ACTIVE",
        "state": "Uttar Pradesh",
        "district": "Kanpur Nagar",
        "typeLabel": "MICRO",
        "permissions": [
          "FEEDBACK_SUBMIT",
          "QUOTATION_EVALUATE",
          "RFQ_CREATE"
        ],
        "registeredAt": "2026-11-15T19:13:52.785035",
        "lastLoginAt": "2026-11-15T19:13:59.41392"
      },
      {
        "id": 33,
        "referenceNo": "BDS-SLR-000033",
        "fullName": "Ravi Kumar Admin",
        "organisationName": "Admin Advisory Services Pvt Ltd",
        "email": "s***************1@example.com",
        "mobile": "93******29",
        "role": "SELLER",
        "status": "ACTIVE",
        "state": "Uttar Pradesh",
        "district": "Lucknow",
        "typeLabel": "CONSULTANCY_FIRM",
        "permissions": [
          "ENGAGEMENT_MANAGE",
          "LISTING_PUBLISH",
          "RFQ_RESPOND"
        ],
        "registeredAt": "2026-11-15T19:13:52.346444",
        "lastLoginAt": "2026-11-15T19:13:58.257128"
      },
      {
        "id": 32,
        "referenceNo": "BDS-PUR-000032",
        "fullName": "Anita Sharma Self",
        "organisationName": "Self Agro Foods",
        "email": "b*************1@example.com",
        "mobile": "74******37",
        "role": "BUYER",
        "status": "ACTIVE",
        "state": "Uttar Pradesh",
        "district": "Kanpur Nagar",
        "typeLabel": "MICRO",
        "permissions": [
          "FEEDBACK_SUBMIT",
          "QUOTATION_EVALUATE",
          "RFQ_CREATE"
        ],
        "registeredAt": "2026-11-15T19:13:51.96054",
        "lastLoginAt": "2026-11-15T19:14:03.850792"
      },
      {
        "id": 31,
        "referenceNo": "BDS-SLR-000031",
        "fullName": "Ravi Kumar Self",
        "organisationName": "Self Advisory Services Pvt Ltd",
        "email": "s**************1@example.com",
        "mobile": "78******65",
        "role": "SELLER",
        "status": "ACTIVE",
        "state": "Uttar Pradesh",
        "district": "Lucknow",
        "typeLabel": "CONSULTANCY_FIRM",
        "permissions": [
          "ENGAGEMENT_MANAGE",
          "
... (truncated)
```

### 69. Sub-admin: /api/v1/admin/audit-logs

`GET /api/v1/admin/audit-logs`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 105,
        "serviceName": "auth",
        "actorId": 36,
        "actorRole": "ADMIN",
        "actorName": "Reports Only Admin",
        "action": "LOGIN",
        "entityType": "USER",
        "entityId": "36",
        "createdAt": "2026-11-15T19:14:09.652634"
      },
      {
        "id": 104,
        "serviceName": "auth",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administrator",
        "action": "ADMIN_CREATED",
        "entityType": "USER",
        "entityId": "36",
        "details": "REPORT_VIEW",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "createdAt": "2026-11-15T19:14:08.500787"
      },
      {
        "id": 103,
        "serviceName": "auth",
        "actorId": 35,
        "actorRole": "BUYER",
        "actorName": "Anita Sharma Other",
        "action": "LOGIN",
        "entityType": "USER",
        "entityId": "35",
        "createdAt": "2026-11-15T19:14:08.049586"
      },
      {
        "id": 102,
        "serviceName": "auth",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administrator",
        "action": "USER_APPROVED",
        "entityType": "USER",
        "entityId": "35",
        "details": "ok",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "createdAt": "2026-11-15T19:14:06.955549"
      },
      {
        "id": 101,
        "serviceName": "auth",
        "actorId": 35,
        "actorRole": "BUYER",
        "actorName": "Anita Sharma Other",
        "action": "BUYER_REGISTERED",
        "entityType": "USER",
        "entityId": "35",
        "details": "BDS-PUR-000035",
        "createdAt": "2026-11-15T19:14:06.930803"
      },
      {
        "id": 100,
        "serviceName": "procurement",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administrator",
        "action": "RFQ_CANCELLED",
        "entityType": "RFQ",
        "entityId": "4",
        "details": "Admin access test clean-up",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "createdAt": "2026-11-15T19:14:05.680342"
      },
      {
        "id": 99,
        "serviceName": "procurement",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administrator",
        "action": "QUOTATION_SHORTLISTED",
        "entityType": "QUOTATION",
        "entityId": "4",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "createdAt": "2026-11-15T19:14:05.635244"
      },
      {
        "id": 98,
        "serviceName": "procurement",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administrator",
        "action": "QUOTATION_SUBMITTED",
        "entityType": "QUOTATION",
        "entityId": "4",
        "details": "BDS-RFQ-000004",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "createdAt": "2026-11-15T19:14:05.585421"
      },
      {
        "id": 97,
        "serviceName": "procurement",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administrator",
        "action": "QUOTATION_DRAFTED",
        "entityType": "QUOTATION",
        "entityId": "4",
        "details": "BDS-RFQ-000004",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "createdAt": "2026-11-15T19:14:05.581273"
      },
      {
        "id": 96,
        "serviceName": "procurement",
        "actorId": 1,
        "actorRole": "ADMIN",
        "actorName": "Platform Administr
... (truncated)
```

### 70. Sub-admin: /api/v1/admin/settings

`GET /api/v1/admin/settings`  — `Authorization: Bearer <token>`

Response `200`:
```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 6,
      "key": "announcement.text",
      "group": "CMS",
      "description": "Banner announcement shown on the public website",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:20.025403"
    },
    {
      "id": 13,
      "key": "disclaimer.payments",
      "value": "BDS Marketplace does not collect, hold or process payments. Contract and payment terms are agreed directly between purchaser and provider.",
      "group": "CMS",
      "description": "Payment disclaimer",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:20.094259"
    },
    {
      "id": 10,
      "key": "documents.allowed-types",
      "value": "pdf,jpg,jpeg,png,doc,docx,xls,xlsx",
      "group": "DOCUMENTS",
      "description": "Allowed file extensions",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:20.067452"
    },
    {
      "id": 9,
      "key": "documents.max-size-mb",
      "value": "5",
      "group": "DOCUMENTS",
      "description": "Maximum upload size in MB",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:20.057289"
    },
    {
      "id": 1,
      "key": "platform.name",
      "value": "BDS Marketplace",
      "group": "GENERAL",
      "description": "Platform display name",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:19.944207"
    },
    {
      "id": 2,
      "key": "platform.tagline",
      "value": "Grow Your MSME with the Right Expertise",
      "group": "GENERAL",
      "description": "Homepage headline",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:19.983176"
    },
    {
      "id": 12,
      "key": "notifications.sms-enabled",
      "value": "false",
      "group": "NOTIFICATIONS",
      "description": "Enable SMS channel when a provider is configured",
      "publicSetting": false,
      "updatedAt": "2026-11-15T17:03:20.08473"
    },
    {
      "id": 7,
      "key": "rfq.max-invites",
      "value": "25",
      "group": "RFQ",
      "description": "Maximum providers per RFQ",
      "publicSetting": false,
      "updatedAt": "2026-11-15T17:03:20.036569"
    },
    {
      "id": 8,
      "key": "rfq.reminder-hours",
      "value": "24",
      "group": "RFQ",
      "description": "Hours before deadline to send reminders",
      "publicSetting": false,
      "updatedAt": "2026-11-15T17:03:20.045565"
    },
    {
      "id": 3,
      "key": "support.email",
      "value": "support@bdsmarketplace.in",
      "group": "SUPPORT",
      "description": "Support email address",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:19.993907"
    },
    {
      "id": 5,
      "key": "support.hours",
      "value": "Mon-Sat, 9:30 am - 6:00 pm",
      "group": "SUPPORT",
      "description": "Support hours",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:20.015278"
    },
    {
      "id": 4,
      "key": "support.phone",
      "value": "1800-226-753",
      "group": "SUPPORT",
      "description": "Support helpline",
      "publicSetting": true,
      "updatedAt": "2026-11-15T17:03:20.004763"
    },
    {
      "id": 11,
      "key": "verification.renewal-months",
      "value": "12",
      "group": "VERIFICATION",
      "description": "Seller re-verification period in months",
      "publicSetting": false,
      "updatedAt": "2026-11-15T17:03:20.075463"
    }
  ],
  "timestamp": "2026-11-15T13:44:09.850943400Z"
}
```

### 71. Sub-admin: /api/v1/admin/rfqs

`GET /api/v1/admin/rfqs`  — `Authorization: Bearer <token>`

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
        "rfqNumber": "BDS-RFQ-000004",
        "title": "Admin-assisted DPR requirement",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "buyerId": 32,
        "buyerName": "Anita Sharma Self",
        "deliveryMode": "ONLINE",
        "budgetMax": 50000,
        "submissionDeadline": "2026-11-24T19:14:05",
        "status": "CANCELLED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:14:05.37378",
        "publishedAt": "2026-11-15T19:14:05.391157"
      },
      {
        "id": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 32,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T19:13:56",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:56.442562",
        "publishedAt": "2026-11-15T19:13:56.473072"
      },
      {
        "id": 2,
        "rfqNumber": "BDS-RFQ-000002",
        "title": "Admin-assisted DPR requirement",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "buyerId": 26,
        "buyerName": "Anita Sharma Self",
        "deliveryMode": "ONLINE",
        "budgetMax": 50000,
        "submissionDeadline": "2026-11-24T19:13:31",
        "status": "RESPONSES_RECEIVED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T19:13:30.955397",
        "publishedAt": "2026-11-15T19:13:31.010267"
      },
      {
        "id": 1,
        "rfqNumber": "BDS-RFQ-000001",
        "title": "DPR for pickle unit expansion",
        "categoryId": 1,
        "categoryName": "Business & Management Consultancy",
        "subcategoryName": "Detailed Project Reports",
        "buyerId": 26,
        "buyerName": "Self Agro Foods",
        "projectState": "Uttar Pradesh",
        "projectDistrict": "Kanpur Nagar",
        "deliveryMode": "HYBRID",
        "budgetMin": 30000,
        "budgetMax": 60000,
        "submissionDeadline": "2026-11-22T18:54:11",
        "status": "PROVIDER_SELECTED",
        "distributionMode": "SELECTED",
        "invitedCount": 1,
        "quotationCount": 1,
        "deadlinePassed": false,
        "createdAt": "2026-11-15T18:54:11.306587",
        "publishedAt": "2026-11-15T18:54:11.377372"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 4,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-11-15T13:44:09.905559300Z"
}
```

### 72. Sub-admin: /api/v1/admin/categories

`GET /api/v1/admin/categories`  — `Authorization: Bearer <token>`

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
      "listingCount": 3,
      "subcategories": [
        {
          "id": 1,
          "name": "Detailed Project Reports",
          "displayOrder": 1,
          "active": true,
          "listingCount": 3
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

### 73. Sub-admin: /api/v1/seller/quotations

`GET /api/v1/seller/quotations`  — `Authorization: Bearer <token>`

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
        "quotationNumber": "BDS-QTN-000004",
        "rfqId": 4,
        "rfqNumber": "BDS-RFQ-000004",
        "rfqTitle": "Admin-assisted DPR requirement",
        "rfqStatus": "CANCELLED",
        "rfqDeadline": "2026-11-24T19:14:05",
        "sellerId": 31,
        "sellerName": "Ravi Kumar Self",
        "buyerId": 32,
        "buyerName": "Anita Sharma Self",
        "versionNo": 1,
        "status": "REJECTED",
        "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
        "scopeAccepted": true,
        "deliverables": "DPR document",
        "timelineDays": 20,
        "professionalFee": 40000,
        "taxPercent": 18,
        "taxAmount": 7200,
        "otherExpenses": 0,
        "totalAmount": 47200,
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:14:05.581273",
        "decisionAt": "2026-11-15T19:14:05.677787",
        "decisionRemarks": "RFQ cancelled by purchaser: Admin access test clean-up",
        "shortlisted": true,
        "editable": false,
        "items": [],
        "attachments": [],
        "createdAt": "2026-11-15T19:14:05.576267",
        "updatedAt": "2026-11-15T19:14:05.681333"
      },
      {
        "id": 3,
        "quotationNumber": "BDS-QTN-000003",
        "rfqId": 3,
        "rfqNumber": "BDS-RFQ-000003",
        "rfqTitle": "DPR for pickle unit expansion",
        "rfqStatus": "PROVIDER_SELECTED",
        "rfqDeadline": "2026-11-22T19:13:56",
        "sellerId": 31,
        "sellerName": "Self Advisory Services Pvt Ltd",
        "buyerId": 32,
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
        "validUntil": "2026-12-15",
        "submittedAt": "2026-11-15T19:13:56.735895",
        "decisionAt": "2026-11-15T19:13:57.008813",
        "decisionRemarks": "Selected based on experience and price",
        "shortlisted": true,
        "editable": false,
        "items": [
          {
            "id": 3,
            "description": "DPR preparation",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 40000,
            "amount": 40000
          },
          {
            "id": 4,
            "description": "CMA data",
            "quantity": 1,
            "unit": "lot",
            "unitPrice": 5000,
            "amount": 5000
          }
        ],
        "attachments": [],
        "createdAt": "2026-11-15T19:13:56.697809",
        "updatedAt": "2026-11-15T19:13:57.011335"
      },
      {
        "id": 2,
        "quotationNumber": "BDS-QTN-000002",
        "rfqId": 2,
        "rfqNumber": "BDS-RFQ-000002",
        "rfqTitle": "Admin-assisted DPR requirement",
        "rfqStatus": "RESPONSES_RECEIVED",
   
... (truncated)
```
