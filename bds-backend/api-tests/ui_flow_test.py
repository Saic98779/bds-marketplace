"""Exercises every frontend page's API calls through the Vite dev proxy, using the
same payload shapes the React forms send.

    python -I ui_flow_test.py [frontend_url]

Requires BDS_ADMIN_PASSWORD in the environment.
"""
import json
import os
import random
import string
import sys
import uuid
from datetime import date, datetime, timedelta
from urllib import error, request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:5173").rstrip("/") + "/api/v1"
ADMIN_PASSWORD = os.environ["BDS_ADMIN_PASSWORD"]
RUN = "".join(random.choices(string.digits, k=4))
PASSWORD = f"Ui@{RUN}pass"
PDF = b"%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n"
results = []


def call(name, method, path, body=None, token=None, multipart=None, expect=(200, 201), raw=False):
    headers = {"Accept": "application/json"}
    data = None
    if multipart is not None:
        boundary = uuid.uuid4().hex
        parts = []
        for field, value in multipart:
            if isinstance(value, tuple):  # (filename, bytes, content_type)
                fn, content, ct = value
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"; filename="{fn}"\r\nContent-Type: {ct}\r\n\r\n'.encode() + content + b"\r\n")
            else:
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"\r\n\r\n{value}\r\n'.encode())
        parts.append(f"--{boundary}--\r\n".encode())
        data = b"".join(parts)
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        with request.urlopen(req, timeout=60) as resp:
            status, content = resp.status, resp.read()
    except error.HTTPError as exc:
        status, content = exc.code, exc.read()
    ok = status in expect
    parsed = None
    if not raw:
        try:
            parsed = json.loads(content.decode("utf-8", "replace")) if content else None
        except ValueError:
            parsed = None
    msg = "" if ok else f"  <- {(parsed or {}).get('message') if isinstance(parsed, dict) else content[:200]}"
    if not ok and isinstance(parsed, dict) and parsed.get("errors"):
        msg += " " + json.dumps(parsed["errors"])[:300]
    print(f"{'PASS' if ok else 'FAIL'}  {status}  {method:6} {path[:90]}  - {name}{msg}")
    results.append(ok)
    if raw:
        return len(content) if ok else None
    return parsed.get("data") if isinstance(parsed, dict) else parsed


def json_part(obj):
    return ("blob", json.dumps(obj).encode(), "application/json")


def login(username, password, portal):
    data = call(f"Login ({portal})", "POST", "/auth/login", {"username": username, "password": password, "portal": portal})
    if not data:
        return None
    if data.get("otpRequired"):
        data = call(f"Verify OTP ({portal})", "POST", "/auth/login/verify", {"challengeToken": data["challengeToken"], "otp": data["devOtp"]})
        return data and data["accessToken"]
    return data["tokens"]["accessToken"]


def page(name, path, token=None):
    return call(f"Page: {name}", "GET", path, token=token)


def main():
    print(f"Target: {BASE}   run id: {RUN}\n")
    # ---- Public site
    cats = page("Home categories", "/public/categories")
    page("Home stats", "/public/catalog/stats")
    page("Settings", "/public/settings")
    services = page("Services search", "/public/services?size=12&sort=popular")
    page("Services filtered", f"/public/services?categoryId={cats[0]['id']}&deliveryMode=ONLINE&size=12")
    if services and services["content"]:
        sid = services["content"][0]["id"]
        detail = page("Service detail", f"/public/services/{sid}")
        page("Service reviews", f"/public/reviews/sellers/{detail['summary']['sellerId']}?size=3")
    providers = page("Providers search", "/public/providers?size=12")
    if providers and providers["content"]:
        pid = providers["content"][0]["id"]
        page("Provider profile", f"/public/providers/{pid}")
        page("Provider rating summary", f"/public/reviews/sellers/{pid}/summary")
        page("Provider services", f"/public/services?sellerId={pid}&size=12")
    meta = page("Seller registration meta", "/auth/meta")

    # ---- Registration (RegisterBuyer / RegisterSeller forms)
    b_email, s_email = f"ui.buyer.{RUN}@example.com", f"ui.seller.{RUN}@example.com"
    otp = call("Send registration OTP (buyer)", "POST", "/auth/otp/send", {"target": b_email, "purpose": "REGISTRATION"})
    buyer_reg = call("Register buyer", "POST", "/auth/register/buyer", {
        "fullName": "UI Buyer", "email": b_email, "mobile": f"9{random.randint(100000000, 999999999)}", "password": PASSWORD, "emailOtp": otp["devOtp"],
        "organisationName": f"UI Buyer Enterprises {RUN}", "constitution": "PRIVATE_LIMITED", "purchaserType": "SMALL", "designation": "Director",
        "industrySector": "Manufacturing", "addressLine": "12 Industrial Estate", "city": "Lucknow", "state": "Uttar Pradesh", "district": "Lucknow", "pincode": "226001",
        "declarationAccepted": True})
    otp = call("Send registration OTP (seller)", "POST", "/auth/otp/send", {"target": s_email, "purpose": "REGISTRATION"})
    mandatory = meta["mandatoryDocuments"]["CONSULTANCY_FIRM"]
    seller_data = {"fullName": "UI Seller", "email": s_email, "mobile": f"8{random.randint(100000000, 999999999)}", "password": PASSWORD, "emailOtp": otp["devOtp"],
                   "organisationName": f"UI Consulting LLP {RUN}", "constitution": "LLP", "pan": "ABCPU" + RUN + "Z", "addressLine": "4 MG Road", "city": "Pune",
                   "state": "Maharashtra", "district": "Pune", "pincode": "411001", "providerType": "CONSULTANCY_FIRM", "categoryIds": [cats[0]["id"]],
                   "expertise": ["DPR", "Feasibility"], "industries": ["Manufacturing"], "yearsExperience": 8, "teamSize": 6, "geographicCoverage": ["Maharashtra"],
                   "deliveryModes": ["ONLINE", "HYBRID"], "introduction": "Registered through the UI flow test.", "declarationAccepted": True}
    parts = [("data", json_part(seller_data))]
    for t in mandatory:
        parts += [("files", (f"{t.lower()}.pdf", PDF, "application/pdf")), ("fileTypes", t)]
    seller_reg = call("Register seller (multipart with documents)", "POST", "/auth/register/seller", multipart=parts)

    # ---- Admin portal
    admin = login("admin@bdsmarketplace.in", ADMIN_PASSWORD, "ADMIN")
    for n, p in [("Dashboard identity", "/admin/identity/stats"), ("Dashboard procurement", "/admin/procurement/stats"), ("Dashboard catalog", "/admin/catalog/stats"),
                 ("System health", "/admin/system/health"), ("Approvals", "/admin/users?role=SELLER&status=PENDING_VERIFICATION&page=0&size=10"),
                 ("Sellers", "/admin/users?role=SELLER&page=0&size=10"), ("Purchasers", "/admin/users?role=BUYER&page=0&size=10"), ("Administrators", "/admin/users?role=ADMIN&page=0&size=10"),
                 ("Permissions", "/admin/users/permissions"), ("Categories", "/admin/categories"), ("Listings", "/admin/services?page=0&size=10"),
                 ("RFQs", "/admin/rfqs?page=0&size=10"), ("Quotations", "/admin/quotations?page=0&size=10"), ("Engagements", "/admin/engagements?page=0&size=10"),
                 ("Grievances", "/admin/queries?page=0&size=10"), ("Reviews", "/admin/reviews?page=0&size=10"), ("Feedback questions", "/admin/feedback-questions"),
                 ("Audit logs", f"/admin/audit-logs?from={date.today() - timedelta(days=7)}&to={date.today()}&page=0&size=20"),
                 ("Settings", "/admin/settings"), ("Notification templates", "/admin/notification-templates"), ("Notifications", "/notifications?page=0&size=15")]:
        page(n, p, admin)
    for url in ["/admin/identity/reports/sellers", "/admin/identity/reports/buyers", "/admin/catalog/reports/services", "/admin/procurement/reports/rfqs",
                "/admin/procurement/reports/quotations", "/admin/procurement/reports/engagements", "/admin/procurement/reports/grievances", "/admin/audit-logs/export"]:
        call(f"Report export {url.split('/')[-1]}", "GET", f"{url}?format=XLSX", token=admin, raw=True)

    perms = call("Permission list", "GET", "/admin/users/permissions", token=admin)
    seller_perms = [p["code"] for p in perms if p["role"] == "SELLER"]
    buyer_perms = [p["code"] for p in perms if p["role"] == "BUYER"]
    sid = seller_reg["userId"]
    u = page("Seller application detail", f"/admin/users/{sid}", admin)
    for d in u["documents"]:
        call(f"Verify document {d['documentType']}", "PUT", f"/admin/users/{sid}/documents/{d['id']}/verify", {"verified": True}, admin)
        call("Download seller document", "GET", f"/admin/users/{sid}/documents/{d['id']}/download", token=admin, raw=True)
    call("Approve seller with privileges", "POST", f"/admin/users/{sid}/approve", {"remarks": "Verified via UI", "permissions": seller_perms}, admin)
    call("Approve buyer", "POST", f"/admin/users/{buyer_reg['userId']}/approve", {"remarks": "OK", "permissions": buyer_perms}, admin)
    call("Feature seller", "PUT", f"/admin/users/{sid}/featured", {"featured": True}, admin)
    created = call("Create administrator", "POST", "/admin/users/admins", {"fullName": "UI Moderator", "email": f"ui.admin.{RUN}@example.com", "designation": "Moderator", "permissions": ["ADMIN_DASHBOARD", "LISTING_MODERATE"]}, admin)
    if created:
        call("Update admin privileges", "PUT", f"/admin/users/{created['userId']}/permissions", {"permissions": ["ADMIN_DASHBOARD", "LISTING_MODERATE", "REPORT_VIEW"]}, admin)
        call("Reset admin password", "POST", f"/admin/users/{created['userId']}/reset-password", token=admin)
    ob = call("Onboard buyer on behalf", "POST", "/admin/users/buyers", {"fullName": "OB Buyer", "email": f"ob.buyer.{RUN}@example.com", "mobile": f"7{random.randint(100000000, 999999999)}",
              "organisationName": f"OB Traders {RUN}", "purchaserType": "MICRO", "industrySector": "Retail & Trade", "addressLine": "1 Market Rd", "state": "Delhi", "district": "New Delhi", "declarationAccepted": True}, admin)
    if ob:
        call("Suspend onboarded buyer", "POST", f"/admin/users/{ob['userId']}/suspend", {"remarks": "UI test"}, admin)
        call("Activate onboarded buyer", "POST", f"/admin/users/{ob['userId']}/activate", {"remarks": "UI test"}, admin)

    cat = call("Create category", "POST", "/admin/categories", {"code": f"UI_TEST_{RUN}", "name": f"UI Test Category {RUN}", "description": "Created by UI flow test", "icon": "science", "displayOrder": 99, "active": True, "mandatoryFields": []}, admin)
    if cat:
        cat = call("Add subcategory", "POST", f"/admin/categories/{cat['id']}/subcategories", {"name": "UI Sub", "displayOrder": 1, "active": True}, admin)
        call("Edit subcategory", "PUT", f"/admin/categories/{cat['id']}/subcategories/{cat['subcategories'][0]['id']}", {"name": "UI Sub (edited)", "displayOrder": 1, "active": True}, admin)
        call("Edit category", "PUT", f"/admin/categories/{cat['id']}", {"code": cat["code"], "name": cat["name"] + " (edited)", "icon": "science", "displayOrder": 99, "active": True}, admin)
        call("Deactivate category", "PATCH", f"/admin/categories/{cat['id']}/status", {"active": False}, admin)
    q = call("Create feedback question", "POST", "/admin/feedback-questions", {"questionText": f"Would you hire again? ({RUN})", "questionType": "YES_NO", "displayOrder": 50, "mandatory": False, "active": True}, admin)
    settings = page("Settings (edit)", "/admin/settings", admin)
    tag = next((s for s in settings if s["key"] == "platform.tagline"), None)
    if tag:
        call("Save setting (unchanged value)", "PUT", f"/admin/settings/{tag['key']}", {"value": tag["value"]}, admin)
    tpl = page("Templates (edit)", "/admin/notification-templates", admin)[0]
    call("Save notification template", "PUT", f"/admin/notification-templates/{tpl['id']}", {k: tpl[k] for k in ("title", "message", "category", "severity", "emailEnabled", "smsEnabled", "active", "description") if tpl.get(k) is not None}, admin)

    # ---- Seller portal
    seller = login(s_email, PASSWORD, "SELLER")
    for n, p in [("Seller dashboard", "/seller/dashboard"), ("Seller documents", "/sellers/me/documents"), ("Seller verification", "/sellers/me/verification"),
                 ("Seller listing stats", "/seller/services/stats"), ("Seller reviews summary", "/seller/reviews/summary"), ("Seller reviews", "/seller/reviews?page=0&size=10")]:
        page(n, p, seller)
    prof = page("Seller profile", "/sellers/me/profile", seller)
    prof.update({"website": "https://example.com", "certifications": "ISO 9001", "yearEstablished": 2015})
    call("Save seller profile (form sends whole profile)", "PUT", "/sellers/me/profile", prof, seller)
    doc = call("Upload extra seller document", "POST", "/sellers/me/documents?documentType=GST_CERTIFICATE", multipart=[("file", ("gst.pdf", PDF, "application/pdf"))], token=seller)
    if doc:
        call("Delete unverified document", "DELETE", f"/sellers/me/documents/{doc['id']}", token=seller)
    listing_body = {"title": f"UI DPR Service {RUN}", "categoryId": cats[0]["id"], "subcategoryId": cats[0]["subcategories"][0]["id"], "shortDescription": "Bank-ready DPRs.",
                    "description": "Detailed project reports prepared by the UI flow test.", "scopeOfWork": "Market, technical and financial analysis.", "deliverables": "DPR document",
                    "methodology": "Site visit and analysis", "targetIndustries": ["Manufacturing"], "deliveryMode": "HYBRID", "serviceLocations": ["Pune"], "state": "Maharashtra",
                    "district": "Pune", "experienceYears": 8, "estimatedDuration": "3 weeks", "indicativeFee": 45000, "pricingModel": "FIXED_FEE", "tags": ["DPR"], "submit": False}
    lst = call("Create listing (draft)", "POST", "/seller/services", listing_body, seller)
    lid = lst["summary"]["id"]
    att = call("Attach listing file", "POST", f"/seller/services/{lid}/attachments?label=Brochure", multipart=[("file", ("brochure.pdf", PDF, "application/pdf"))], token=seller)
    call("Download listing attachment", "GET", f"/services/{lid}/attachments/{att['id']}/download", token=seller, raw=True)
    call("Edit listing", "PUT", f"/seller/services/{lid}", {**listing_body, "indicativeFee": 50000}, seller)
    call("Submit listing", "POST", f"/seller/services/{lid}/submit", token=seller)
    page("Listing history", f"/seller/services/{lid}/history", seller)
    page("My listings", "/seller/services?page=0&size=10", seller)
    call("Admin: start review", "POST", f"/admin/services/{lid}/start-review", token=admin)
    call("Admin: approve listing", "POST", f"/admin/services/{lid}/approve", {"remarks": "Looks good"}, admin)
    call("Admin: feature listing", "PUT", f"/admin/services/{lid}/featured", {"featured": True}, admin)
    page("Admin listing detail", f"/admin/services/{lid}", admin)
    page("Admin listing history", f"/admin/services/{lid}/history", admin)
    page("Public listing detail", f"/public/services/{lid}")

    # ---- Buyer portal
    buyer = login(b_email, PASSWORD, "BUYER")
    for n, p in [("Buyer dashboard", "/buyer/dashboard"), ("Feedback questions", "/buyer/feedback/questions"), ("Buyer work orders", "/buyer/work-orders?page=0&size=10")]:
        page(n, p, buyer)
    bprof = page("Buyer profile", "/buyers/me/profile", buyer)
    bprof["website"] = "https://buyer.example.com"
    call("Save buyer profile", "PUT", "/buyers/me/profile", bprof, buyer)
    page("Recommended providers", f"/buyer/recommended-sellers?categoryId={cats[0]['id']}", buyer)
    deadline = (datetime.now() + timedelta(days=10)).replace(second=0, microsecond=0).isoformat(timespec="minutes") + ":00"
    rfq_body = {"title": f"UI DPR for expansion {RUN}", "categoryId": cats[0]["id"], "subcategoryId": cats[0]["subcategories"][0]["id"], "description": "Need a DPR for a term loan.",
                "scopeOfWork": "Full DPR", "deliverables": "DPR + financial model", "industry": "Manufacturing", "projectState": "Uttar Pradesh", "projectDistrict": "Lucknow",
                "deliveryMode": "HYBRID", "budgetMin": 30000, "budgetMax": 80000, "expectedDuration": "4 weeks", "requiredExperience": 5,
                "expectedStartDate": (date.today() + timedelta(days=15)).isoformat(), "submissionDeadline": deadline, "distributionMode": "SELECTED", "sellerIds": [sid],
                "sourceListingId": lid, "publish": False}
    rfq = call("Create RFQ (draft from listing)", "POST", "/buyer/rfqs", rfq_body, buyer)
    rid = rfq["summary"]["id"]
    call("Edit draft RFQ", "PUT", f"/buyer/rfqs/{rid}", {**rfq_body, "budgetMax": 90000}, buyer)
    call("Attach RFQ document", "POST", f"/buyer/rfqs/{rid}/attachments?category=RFQ", multipart=[("file", ("requirement.pdf", PDF, "application/pdf"))], token=buyer)
    call("Publish RFQ", "POST", f"/buyer/rfqs/{rid}/publish", token=buyer)
    call("Extend deadline", "POST", f"/buyer/rfqs/{rid}/extend-deadline", {"submissionDeadline": (datetime.now() + timedelta(days=12)).replace(microsecond=0).isoformat(timespec="minutes") + ":00", "remarks": "More time"}, buyer)
    page("My RFQs", "/buyer/rfqs?page=0&size=10", buyer)
    page("Admin RFQ detail", f"/admin/rfqs/{rid}", admin)
    page("Admin recommended for RFQ", f"/admin/rfqs/{rid}/recommended-sellers", admin)

    # ---- Seller quotes
    page("Seller RFQ inbox", "/seller/rfqs?page=0&size=10", seller)
    page("Seller RFQ detail", f"/seller/rfqs/{rid}", seller)
    quote_body = {"technicalProposal": "Comprehensive DPR approach.", "methodology": "Visit, analyse, draft, review", "scopeAccepted": True, "deliverables": "DPR, model",
                  "timelineDays": 25, "teamComposition": "2 analysts", "relevantExperience": "40 DPRs", "professionalFee": 50000, "taxPercent": 18, "otherExpenses": 2000,
                  "paymentMilestones": "50/50", "validUntil": (date.today() + timedelta(days=45)).isoformat(),
                  "items": [{"description": "DPR preparation", "quantity": 1, "unit": "lot", "unitPrice": 50000}], "submit": False}
    qt = call("Create quotation (draft)", "POST", f"/seller/rfqs/{rid}/quotations", quote_body, seller)
    qid = qt["id"]
    call("Attach quotation document", "POST", f"/seller/quotations/{qid}/attachments?category=TECHNICAL", multipart=[("file", ("proposal.pdf", PDF, "application/pdf"))], token=seller)
    call("Submit quotation", "POST", f"/seller/quotations/{qid}/submit", token=seller)
    call("Quotation PDF (seller)", "GET", f"/seller/quotations/{qid}/pdf", token=seller, raw=True)

    # ---- Buyer evaluates
    page("RFQ quotations", f"/buyer/rfqs/{rid}/quotations", buyer)
    page("Buyer quotation detail", f"/buyer/quotations/{qid}", buyer)
    call("Shortlist", "POST", f"/buyer/quotations/{qid}/shortlist", {"shortlisted": True}, buyer)
    call("Request clarification", "POST", f"/buyer/quotations/{qid}/request-clarification", {"subject": "Timeline", "message": "Can you do 20 days?"}, buyer)
    call("Seller revises quotation", "PUT", f"/seller/quotations/{qid}", {**quote_body, "timelineDays": 20, "changeSummary": "Timeline reduced", "submit": True}, seller)
    page("Seller versions", f"/seller/quotations/{qid}/versions", seller)
    page("Buyer versions", f"/buyer/quotations/{qid}/versions", buyer)
    call("Quotation PDF (buyer)", "GET", f"/buyer/quotations/{qid}/pdf", token=buyer, raw=True)
    page("Comparison", f"/buyer/rfqs/{rid}/comparison", buyer)
    call("Save evaluation", "POST", f"/buyer/rfqs/{rid}/comparison", {"criteria": [{"name": "Technical approach", "weight": 40}, {"name": "Experience", "weight": 30}, {"name": "Price", "weight": 30}],
         "scores": [{"quotationId": qid, "criterionIndex": i, "score": s} for i, s in enumerate([8, 9, 7])]}, buyer)
    for f in ("XLSX", "PDF", "CSV"):
        call(f"Comparison export {f}", "GET", f"/buyer/rfqs/{rid}/comparison/export?format={f}", token=buyer, raw=True)
    page("Admin comparison (read-only)", f"/admin/rfqs/{rid}/comparison", admin)
    eng = call("Accept quotation", "POST", f"/buyer/quotations/{qid}/accept", {"remarks": "Best technical score", "contractValue": 59000,
               "startDate": (date.today() + timedelta(days=3)).isoformat(), "expectedEndDate": (date.today() + timedelta(days=30)).isoformat(), "confirmed": True}, buyer)
    eid = eng["summary"]["id"]

    # ---- Engagement lifecycle
    wo = call("Seller requests work order", "POST", f"/seller/engagements/{eid}/work-order-request", {"remarks": "Please issue the PO"}, seller)
    call("Buyer issues requested work order", "POST", f"/buyer/work-orders/{wo['id']}/issue", {"poReference": f"PO-{RUN}", "workOrderDate": date.today().isoformat(), "agreedValue": 59000,
         "startDate": (date.today() + timedelta(days=3)).isoformat(), "endDate": (date.today() + timedelta(days=30)).isoformat(), "scopeSummary": "DPR", "terms": "Net 30"}, buyer)
    page("Seller work orders", "/seller/work-orders?page=0&size=10", seller)
    call("Seller acknowledges work order", "POST", f"/seller/work-orders/{wo['id']}/acknowledge", token=seller)
    call("Seller starts work", "POST", f"/seller/engagements/{eid}/start", token=seller)
    call("Seller posts progress", "POST", f"/seller/engagements/{eid}/progress", {"message": "Site visit done", "progressPercent": 40}, seller)
    call("Seller uploads deliverable", "POST", f"/seller/engagements/{eid}/documents?category=DELIVERABLE", multipart=[("file", ("dpr.pdf", PDF, "application/pdf"))], token=seller)
    call("Buyer uploads engagement document", "POST", f"/buyer/engagements/{eid}/documents?category=ENGAGEMENT", multipart=[("file", ("inputs.pdf", PDF, "application/pdf"))], token=buyer)
    detail = page("Buyer engagement detail", f"/buyer/engagements/{eid}", buyer)
    for d in detail["documents"][:1]:
        call("Download engagement document", "GET", f"/documents/{d['id']}/download", token=buyer, raw=True)
    call("Seller marks delivered", "POST", f"/seller/engagements/{eid}/deliver", {"message": "DPR delivered"}, seller)
    call("Buyer confirms delivery", "POST", f"/buyer/engagements/{eid}/confirm-delivery", {"delivered": True, "remarks": "Received"}, buyer)
    questions = page("Feedback questions", "/buyer/feedback/questions", buyer)
    answers = []
    for qq in questions:
        answers.append({"questionId": qq["id"], "ratingValue": 5} if qq["questionType"] == "RATING" else {"questionId": qq["id"], "booleanValue": True} if qq["questionType"] == "YES_NO" else {"questionId": qq["id"], "textValue": "Good work"})
    review = call("Submit feedback", "POST", f"/buyer/engagements/{eid}/feedback", {"rating": 5, "title": "Excellent DPR", "reviewText": "On time and thorough.", "answers": answers}, buyer)
    page("Seller engagement detail", f"/seller/engagements/{eid}", seller)
    page("Admin engagement detail", f"/admin/engagements/{eid}", admin)
    if review:
        call("Admin hides review", "PUT", f"/admin/reviews/{review['id']}/visibility", {"visible": False, "remarks": "UI test"}, admin)
        call("Admin publishes review", "PUT", f"/admin/reviews/{review['id']}/visibility", {"visible": True}, admin)

    # ---- Queries & grievances
    t = call("Buyer raises query", "POST", "/queries", {"contextType": "ENGAGEMENT", "contextId": eid, "counterpartyId": sid, "subject": "Invoice copy", "description": "Please share the invoice.", "priority": "MEDIUM"}, buyer)
    tid = t["summary"]["id"]
    call("Seller replies", "POST", f"/queries/{tid}/messages", {"body": "Shared via email."}, seller)
    call("Attach file to query", "POST", f"/queries/{tid}/attachments", multipart=[("file", ("invoice.pdf", PDF, "application/pdf"))], token=seller)
    call("Buyer escalates", "POST", f"/queries/{tid}/escalate", {"remarks": "Not received"}, buyer)
    page("Admin grievances list", "/admin/queries?page=0&size=10", admin)
    page("Admin query detail", f"/admin/queries/{tid}", admin)
    call("Admin responds", "POST", f"/admin/queries/{tid}/messages", {"body": "Seller, please resend."}, admin)
    call("Admin resolves", "POST", f"/admin/queries/{tid}/resolve", {"remarks": "Resent"}, admin)
    call("Admin closes", "POST", f"/admin/queries/{tid}/close", {"remarks": "Done"}, admin)
    page("Buyer query tracker", "/queries?page=0&size=10", buyer)
    page("Buyer query detail", f"/queries/{tid}", buyer)
    call("Buyer raises grievance", "POST", "/grievances", {"contextType": "PLATFORM", "subject": "UI test grievance", "description": "Testing grievance flow.", "priority": "LOW"}, buyer)

    # ---- Account pages
    for tok, who in ((buyer, "buyer"), (seller, "seller"), (admin, "admin")):
        page(f"Notifications ({who})", "/notifications?page=0&size=15", tok)
        page(f"Unread count ({who})", "/notifications/unread-count", tok)
        call(f"Mark all read ({who})", "POST", "/notifications/read-all", token=tok)
        call(f"Save preferences ({who})", "PUT", "/auth/me/preferences", {"emailAlerts": True}, tok)
    call("Change buyer password", "POST", "/auth/password/change", {"currentPassword": PASSWORD, "newPassword": PASSWORD + "2"}, buyer)
    fp = call("Forgot password (seller)", "POST", "/auth/password/forgot", {"username": s_email})
    if fp:
        call("Reset password (seller)", "POST", "/auth/password/reset", {"username": s_email, "otp": fp["devOtp"], "newPassword": PASSWORD + "3"})

    # ---- Cleanup test data that would show publicly
    call("Admin deactivates test listing", "POST", f"/admin/services/{lid}/deactivate", {"remarks": "UI flow test data"}, admin)
    call("Unfeature test seller", "PUT", f"/admin/users/{sid}/featured", {"featured": False}, admin)
    if q:
        call("Deactivate test feedback question", "PUT", f"/admin/feedback-questions/{q['id']}", {"questionText": q["questionText"], "questionType": "YES_NO", "displayOrder": 50, "mandatory": False, "active": False}, admin)

    passed = sum(results)
    print(f"\n{passed}/{len(results)} passed. Accounts: {b_email} / {s_email} (passwords changed during the test)")
    sys.exit(0 if passed == len(results) else 1)


if __name__ == "__main__":
    main()
