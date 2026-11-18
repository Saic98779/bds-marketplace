# BDS Marketplace — end-to-end test guide

Frontend: <http://localhost:5173> (start with `npm run dev` in `bds-frontend`), API: <https://bds-api.metaversedu.in> (same server database).

While `BDS_EXPOSE_OTP=true` on the server, every OTP is shown on screen as **"Development mode: OTP is …"**, so no email is needed.

## 1. The business flow

```
 PROVIDER (Seller)                      ADMIN                          PURCHASER (Buyer)
 ─────────────────                      ─────                          ─────────────────
 1 Register + documents ───────────▶ 2 Verify documents, approve
   PENDING_VERIFICATION                  user → ACTIVE                  1 Register → approve → ACTIVE
 3 Create service listing ─────────▶ 4 Review & approve listing
   DRAFT → SUBMITTED                     UNDER_REVIEW → APPROVED ────▶  5 Finds service on public site
                                                                        6 Creates RFQ → PUBLISHED
 7 RFQ in inbox (INVITED → VIEWED)  ◀──────────────────────────────────── (invites selected providers)
 8 Submits quotation ─────────────────────────────────────────────────▶ RFQ → RESPONSES_RECEIVED
   SUBMITTED                                                            9 Opens quote → UNDER_REVIEW
                                                                          Shortlist → SHORTLISTED
   CLARIFICATION_REQUESTED ◀──────────────────────────────────────────── Request clarification
10 Revises → REVISED ─────────────────────────────────────────────────▶ 11 Compare & score (UNDER_EVALUATION)
                                                                        12 Accept → quote ACCEPTED,
                                                                           RFQ PROVIDER_SELECTED,
                                                                           other quotes REJECTED,
                                                                           engagement SELECTED
13 Request work order (REQUESTED) ────────────────────────────────────▶ 14 Issue work order (ISSUED)
                                                                           engagement ENGAGEMENT_INITIATED
15 Acknowledge (ACKNOWLEDGED)
16 Start work → IN_PROGRESS, post progress %, upload deliverables
17 Mark delivered → DELIVERED ────────────────────────────────────────▶ 18 Confirm delivery → COMPLETED
                                                                           RFQ → CLOSED
                                                                        19 Rating & feedback
20 Sees review                          Hide / publish review
   Query tracker: either party raises a query → reply → escalate ──▶ Admin responds → resolve → close
```

Payments are agreed outside the platform; there is no payment step.

## 2. Accounts

| Role | Email | Password | Portal button |
|---|---|---|---|
| Admin | admin@bdsmarketplace.in | `BDS_ADMIN_PASSWORD` in `/etc/bds/bds-backend.env` on the server | Admin |
| Seller (existing) | any seller from the latest `api-tests/payloads.md` (local, not in git) | listed there | Provider |
| Buyer (existing) | any buyer from the same file | listed there | Purchaser |

For a full run, create a new seller and buyer in Phase 1 so you test registration too. Use two browser windows (one normal, one private) so you can stay signed in as two roles at once.

## 3. Quick reference

Go through the phases in order; each one creates what the next one needs.

| Phase | Who | What you do | What you should see |
|---|---|---|---|
| 1. Register | New seller, new buyer | Register → Send code → fill the steps → upload PDFs (seller) | Reference number shown; seller dashboard has a "pending verification" banner |
| 2. Verify | Admin | Seller Approvals → open → Verify each document → Approve; approve the buyer under Purchasers | Status ACTIVE; banner goes away after **Refresh status** |
| 3. Listing | Seller, then Admin | My Services → New → Save draft → attach file → Submit; admin: Start review → Approve | DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED; visible on the public Services page |
| 4. RFQ | Buyer | Find Services → open it → Request Quotation → Save draft → attach → Publish; try Invite providers and Extend deadline | PUBLISHED; provider shown as INVITED |
| 5. Quote | Seller | RFQ Inbox → Submit quotation → add a line item → Save draft → Submit → download PDF | Quote SUBMITTED; RFQ RESPONSES_RECEIVED |
| 6. Evaluate | Buyer, then Seller | Open the quote → Shortlist → Request clarification; seller revises; buyer: Compare → score → Save → Export → Accept | Quote shows v1 and v2; weighted rank; quote ACCEPTED; engagement created; other quotes REJECTED |
| 7. Deliver | Both | Seller: Request work order; buyer: Issue; seller: Acknowledge → Start → Post progress → upload → Mark delivered; buyer: Confirm delivery | SELECTED → ENGAGEMENT_INITIATED → IN_PROGRESS → DELIVERED → COMPLETED; RFQ CLOSED |
| 8. Feedback | Buyer, then Admin | Give feedback; admin: Hide, then Publish the review | Rating shows on the seller's reviews and the public provider page |
| 9. Queries | Both, then Admin | Buyer: Query on the engagement; seller replies; buyer Escalates; admin replies → Resolve → Close; buyer also raises a Grievance | OPEN → RESPONDED → ESCALATED → RESOLVED → CLOSED |
| 10. Tools | All | Notifications bell, change password, forgot password; admin: Reports, Categories, new administrator, onboard a user, Audit Logs, System Settings | Each action appears in the audit log; reports download |

**Things that should fail:**

- **Wrong password:** shows an error, and 5 failures lock the account for 15 minutes. Admin can **Unlock** it.
- **Past deadline:** publishing an RFQ with a deadline in the past gives an error.
- **Seller declines an RFQ:** the buyer sees DECLINED with the reason.
- **Delivery not accepted:** if the buyer chooses "Not delivered", the engagement goes back to IN_PROGRESS.
- **Correction requested:** when admin requests a correction on a listing, the seller can edit it and resubmit.

## 4. Step-by-step test

Each step: **where** → **action** → **expected result**.

### Phase 1 — Registration (no login)

| # | Where | Action | Expected |
|---|---|---|---|
| 1.1 | Home | Browse categories, Services, Find Providers, open a service and a provider, How It Works, About | Pages load with server data; About shows support email/phone |
| 1.2 | Register → Service provider | Step 1: name, email, mobile, password → **Send code** → enter OTP. Step 2: organisation (PAN like `ABCDE1234F`). Step 3: provider type *Consultancy Firm*, experience, categories. Step 4: upload PDF for each `*` document, tick declaration → **Submit for verification** | "Registration submitted" with reference `BDS-SLR-…` |
| 1.3 | Register → Purchaser | Same account step, organisation details, declaration → **Submit registration** | Reference `BDS-PUR-…` |
| 1.4 | Sign in (Provider) as new seller | Sign in, enter OTP | Lands on seller dashboard with yellow **"pending verification"** banner |
| 1.5 | Negative | Sign in with a wrong password | Red error; after 5 failures the account locks for 15 minutes (admin can **Unlock**) |

### Phase 2 — Admin verification

| # | Where | Action | Expected |
|---|---|---|---|
| 2.1 | Admin → Seller Approvals | Open the new seller | Profile, documents, checklist, history |
| 2.2 | Documents | Download one; click **Verify** on each | Checklist shows Verified |
| 2.3 | Privileges panel | Keep defaults → **Approve** | Status **ACTIVE**; seller gets a notification |
| 2.4 | Admin → Purchasers | Open new buyer → **Approve** | ACTIVE |
| 2.5 | Seller window | Click **Refresh status** on the banner | Banner disappears |
| 2.6 | Optional | On another user: **Request info**, **Suspend** → **Activate**, **Reset password** | Status changes; reset shows a temporary password |

### Phase 3 — Service listing

| # | Where | Action | Expected |
|---|---|---|---|
| 3.1 | Seller → Business Profile | Fill expertise, introduction → **Save profile** | Completion % increases |
| 3.2 | Seller → My Services → New service | Fill form → **Save draft** | Status DRAFT |
| 3.3 | Service detail | Attach a file with a label; **Edit** something; **Submit** | SUBMITTED |
| 3.4 | Admin → Service Listings | Open it → **Start review** → **Approve** | UNDER_REVIEW → APPROVED; history shows both |
| 3.5 | Optional | **Feature** it | Appears in homepage "Popular/Featured" |
| 3.6 | Public Services page (logged out) | Search for the title | Listing is visible |
| 3.7 | Negative | Admin **Request correction** on another listing | Seller sees CORRECTION_REQUESTED + remarks, can edit and resubmit |

### Phase 4 — RFQ

| # | Where | Action | Expected |
|---|---|---|---|
| 4.1 | Buyer → Find Services | Open the new service → **Request Quotation** | RFQ form pre-filled with category and provider |
| 4.2 | RFQ form | Fill description, budget, deadline (future) → **Save draft** | DRAFT |
| 4.3 | RFQ detail | Attach a document → **Edit** → **Publish** | PUBLISHED; invited provider listed as INVITED |
| 4.4 | RFQ detail | **Invite providers** (pick one), **Extend deadline** | Invitations list grows; new deadline shown |
| 4.5 | Negative | Publish an RFQ with a past deadline | Error "Submission deadline must be in the future" |
| 4.6 | Optional | New RFQ with distribution **Administrator-assisted matching** | Status SUBMITTED; Admin → RFQs → **Match providers** invites sellers → PUBLISHED |

### Phase 5 — Quotation

| # | Where | Action | Expected |
|---|---|---|---|
| 5.1 | Seller → RFQ Inbox | Open the RFQ | Invitation becomes VIEWED |
| 5.2 | **Submit quotation** | Proposal, deliverables, timeline, a line item, fee, tax 18% → **Save draft** | Quote DRAFT; estimated total calculated |
| 5.3 | Quotation detail | Upload attachment → **Submit**; download **PDF** | SUBMITTED; RFQ shows RESPONSES_RECEIVED |
| 5.4 | Negative | Another seller: **Decline** an RFQ with a reason | Buyer sees invitation DECLINED with reason |

### Phase 6 — Evaluation & selection

| # | Where | Action | Expected |
|---|---|---|---|
| 6.1 | Buyer → RFQ → Quotations received | Open the quote | Quote → UNDER_REVIEW |
| 6.2 | Quote detail | **Shortlist** | SHORTLISTED (seller notified) |
| 6.3 | | **Request clarification** (subject + message) | CLARIFICATION_REQUESTED; seller sees warning |
| 6.4 | Seller → quotation → **Revise** | Change timeline, enter change summary → **Submit revision** | REVISED; Version history shows v1, v2 |
| 6.5 | Buyer → **Compare quotations** | Enter 0–10 scores per criterion (weights total 100) → **Save evaluation** | Weighted score and rank; lowest price / shortest timeline highlighted; RFQ UNDER_EVALUATION |
| 6.6 | Comparison | Export XLSX / PDF / CSV | Files download |
| 6.7 | **Accept** | Remarks, contract value, dates, tick confirm → **Accept & select provider** | Opens the engagement (SELECTED); quote ACCEPTED; RFQ PROVIDER_SELECTED; other quotes REJECTED |

### Phase 7 — Work order & delivery

| # | Where | Action | Expected |
|---|---|---|---|
| 7.1 | Seller → Engagement | **Request work order** | Work order REQUESTED |
| 7.2 | Buyer → Work Orders (or engagement) | **Issue** with PO reference, dates | ISSUED; engagement ENGAGEMENT_INITIATED |
| 7.3 | Seller → Work Orders | **Acknowledge** | ACKNOWLEDGED |
| 7.4 | Seller → Engagement | **Start work** → **Post progress** (40%) → upload deliverable | IN_PROGRESS; progress bar 40%; update in timeline |
| 7.5 | | **Mark delivered** with note | DELIVERED |
| 7.6 | Buyer → Engagement | **Confirm delivery** → "Work delivered as agreed" | COMPLETED; RFQ CLOSED |
| 7.7 | Negative | Instead choose "Not delivered / needs rework" | Engagement back to IN_PROGRESS |
| 7.8 | Optional | **Raise dispute** → **Resume**; or **Cancel** | DISPUTED → IN_PROGRESS; or CANCELLED |

### Phase 8 — Feedback

| # | Where | Action | Expected |
|---|---|---|---|
| 8.1 | Buyer → Engagement | **Give feedback**: stars, title, review, answer questions | Feedback card on engagement |
| 8.2 | Seller → Ratings & Reviews | — | Review and average rating shown |
| 8.3 | Public provider page | — | Rating and review visible |
| 8.4 | Admin → Feedback & Reviews | **Hide** (remarks) → **Publish** | Disappears from / reappears on public page |

### Phase 9 — Queries & grievances

| # | Where | Action | Expected |
|---|---|---|---|
| 9.1 | Buyer → Engagement → **Query** | Subject, description | Ticket OPEN in Query Tracker |
| 9.2 | Seller → Query Tracker | Open, reply, attach a file | RESPONDED |
| 9.3 | Buyer | **Escalate to platform** with reason | ESCALATED |
| 9.4 | Admin → Grievances | Open, reply, **Resolve**, **Close** | RESOLVED → CLOSED |
| 9.5 | Buyer → Query Tracker → **Raise grievance** | Platform grievance | Appears in Admin → Grievances |
| 9.6 | Optional | On a resolved query: **Reopen** | REOPENED |

### Phase 10 — Account & admin tools

| # | Where | Action | Expected |
|---|---|---|---|
| 10.1 | Any role → bell icon | Open, click a notification, **Mark all read** | Navigates to the record; count resets |
| 10.2 | Settings | Change password; toggle email alerts | Success toast; new password works on next sign in |
| 10.3 | Sign in → Forgot password | Enter email → OTP → new password | Can sign in with the new password |
| 10.4 | Admin → Dashboard / Reports & MIS | — | Counters and charts; each report downloads XLSX/CSV/PDF |
| 10.5 | Admin → Categories | New category, add/edit subcategory, Deactivate | Shown/hidden on public Services filters |
| 10.6 | Admin → Administrators | **New administrator** with some privileges | Temporary password shown; that admin sees only permitted menus |
| 10.7 | Admin → Sellers / Purchasers → **Onboard** | Create on behalf | Temporary password; user must change it at first login |
| 10.8 | Admin → Service Listings → **Add on behalf of seller** | Choose seller, fill form | Listing created for that seller |
| 10.9 | Admin → Audit Logs | Filter by service and dates; export | Every action above is logged |
| 10.10 | Admin → System Settings | Edit a setting (e.g. `platform.tagline`), a template; check System health | Tagline changes on the home page |

## 5. Automated check

Replays all page calls (182 requests) through the frontend proxy and creates a fresh set of test data:

```bash
BDS_ADMIN_PASSWORD='<admin password>' python -I bds-backend/api-tests/ui_flow_test.py http://localhost:5173
```

Expected: `182/182 passed`.

## 6. Reporting a problem

Note the page URL, the button, and the red error text (or a screenshot). Everything created here is stored in the production database on 51.79.165.43 — use obviously fake names ("Test Co").
