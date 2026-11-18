"""End-to-end smoke test for bds-backend.

Runs public, auth, onboarding, catalog and procurement flows against a live
server and writes every request/response pair to payloads.md.

    python -I smoke_test.py [base_url]
"""
import json
import os
import random
import string
import sys
import uuid
from datetime import date, datetime, timedelta
from pathlib import Path
from urllib import error, request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
OUT = Path(__file__).with_name("payloads.md")
RUN = "".join(random.choices(string.digits, k=4))
PASSWORD = "Test@" + RUN + "ab"

log = []
results = []


def short(value, limit=3500):
    text = json.dumps(value, indent=2, ensure_ascii=False) if not isinstance(value, str) else value
    return text if len(text) <= limit else text[:limit] + "\n... (truncated)"


SECRET_KEYS = ("password", "newPassword", "currentPassword", "temporaryPassword")
# Reports for anything but a local backend must not contain real passwords (they may be committed)
def is_local():
    return BASE.startswith(("http://localhost", "http://127.0.0.1"))


def redact(obj):
    if isinstance(obj, dict):
        return {k: (v[:20] + "...") if k in ("accessToken", "refreshToken", "challengeToken") and isinstance(v, str)
                else "<redacted>" if k in SECRET_KEYS and not is_local() and isinstance(v, str) else redact(v)
                for k, v in obj.items()}
    if isinstance(obj, list):
        return [redact(v) for v in obj]
    return obj


def call(name, method, path, body=None, token=None, files=None, expect=(200, 201)):
    url = BASE + path
    headers = {"Accept": "application/json"}
    data = None
    shown_body = body
    if files is not None:
        boundary = uuid.uuid4().hex
        parts = []
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\nContent-Type: application/json\r\n\r\n'.encode()
                     + json.dumps(body).encode() + b"\r\n")
        for doc_type, filename, content in files:
            parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="files"; filename="{filename}"\r\n'
                         f'Content-Type: application/pdf\r\n\r\n'.encode() + content + b"\r\n")
            parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="fileTypes"\r\n\r\n{doc_type}\r\n'.encode())
        parts.append(f"--{boundary}--\r\n".encode())
        data = b"".join(parts)
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
        shown_body = {"data (json part)": body, "files": [f"{t} -> {n}" for t, n, _ in files]}
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = request.Request(url, data=data, method=method, headers=headers)
    try:
        with request.urlopen(req, timeout=60) as resp:
            status, raw = resp.status, resp.read().decode("utf-8", "replace")
    except error.HTTPError as exc:
        status, raw = exc.code, exc.read().decode("utf-8", "replace")
    try:
        parsed = json.loads(raw) if raw else None
    except ValueError:
        parsed = raw
    ok = status in expect
    results.append((name, method, path, status, ok))
    log.append((name, method, path, shown_body, status, parsed, ok, token is not None))
    print(f"{'PASS' if ok else 'FAIL'}  {status}  {method:6} {path}  - {name}")
    return parsed.get("data") if isinstance(parsed, dict) else parsed


def login(username, password, portal):
    first = call(f"Login ({portal})", "POST", "/api/v1/auth/login",
                 {"username": username, "password": password, "portal": portal})
    if not first:
        return None
    if first.get("otpRequired"):
        tokens = call(f"Verify login OTP ({portal})", "POST", "/api/v1/auth/login/verify",
                      {"challengeToken": first["challengeToken"], "otp": first["devOtp"]})
    else:
        tokens = first.get("tokens")
    return tokens["accessToken"] if tokens else None


def id_of(obj):
    """Detail responses nest the id under 'summary'; flat responses carry it directly."""
    if not obj:
        return None
    return obj.get("id") or (obj.get("summary") or {}).get("id")


def pan(entity="C"):
    letters = "".join(random.choices(string.ascii_uppercase, k=3))
    return f"{letters}{entity}{random.choice(string.ascii_uppercase)}{random.randint(1000, 9999)}{random.choice(string.ascii_uppercase)}"


def mobile():
    return random.choice("6789") + "".join(random.choices(string.digits, k=9))


def pdf(label):
    return b"%PDF-1.4\n% " + label.encode() + b"\n1 0 obj << /Type /Catalog >> endobj\ntrailer << /Root 1 0 R >>\n%%EOF\n"


def seller_payload(tag, otp=None):
    return {
        "fullName": f"Ravi Kumar {tag}",
        "email": f"seller.{tag.lower()}.{RUN}@example.com",
        "mobile": mobile(),
        "password": PASSWORD,
        "emailOtp": otp,
        "organisationName": f"{tag} Advisory Services Pvt Ltd",
        "constitution": "PRIVATE_LIMITED",
        "pan": pan("C"),
        "gstin": "",
        "udyamNumber": "",
        "addressLine": "12, MG Road, Hazratganj",
        "city": "Lucknow",
        "state": "Uttar Pradesh",
        "district": "Lucknow",
        "pincode": "226001",
        "providerType": "CONSULTANCY_FIRM",
        "categoryIds": [1],
        "expertise": ["Detailed Project Reports", "Business Plans"],
        "industries": ["Manufacturing", "Food Processing"],
        "yearsExperience": 8,
        "geographicCoverage": ["Uttar Pradesh", "Bihar"],
        "deliveryModes": ["ONLINE", "ONSITE"],
        "website": "https://example.com",
        "introduction": "We prepare bankable DPRs and business plans for MSMEs.",
        "declarationAccepted": True,
    }


def buyer_payload(tag, otp=None):
    return {
        "fullName": f"Anita Sharma {tag}",
        "email": f"buyer.{tag.lower()}.{RUN}@example.com",
        "mobile": mobile(),
        "password": PASSWORD,
        "emailOtp": otp,
        "organisationName": f"{tag} Agro Foods",
        "constitution": "PROPRIETORSHIP",
        "purchaserType": "MICRO",
        "designation": "Proprietor",
        "pan": pan("P"),
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
        "declarationAccepted": True,
    }


def main():
    print(f"Target: {BASE}   run id: {RUN}\n")

    # ---------- public ----------
    call("Health", "GET", "/actuator/health")
    call("Transport public key", "GET", "/api/v1/auth/public-key")
    meta = call("Onboarding reference data", "GET", "/api/v1/auth/meta")
    categories = call("Public categories", "GET", "/api/v1/public/categories") or []
    call("Public services", "GET", "/api/v1/public/services?page=0&size=5")
    call("Public catalog stats", "GET", "/api/v1/public/catalog/stats")
    call("Public providers", "GET", "/api/v1/public/providers?page=0&size=5")
    call("Public settings", "GET", "/api/v1/public/settings")
    category_id = categories[0]["id"] if categories else 1
    subcategory_id = (categories[0].get("subcategories") or [{}])[0].get("id") if categories else None

    # ---------- negative checks ----------
    call("Protected endpoint without token -> 401", "GET", "/api/v1/auth/me", expect=(401, 403))
    call("Login with wrong password -> 401", "POST", "/api/v1/auth/login",
         {"username": "admin@bdsmarketplace.in", "password": "Wrong@123", "portal": "ADMIN"}, expect=(400, 401))
    bad = seller_payload("Invalid")
    bad["pan"] = "ABC123"
    call("Seller registration with invalid PAN -> 400", "POST", "/api/v1/auth/register/seller", bad, expect=(400,))

    # ---------- admin ----------
    admin = login("admin@bdsmarketplace.in", os.environ.get("BDS_ADMIN_PASSWORD", "Admin@12345"), "ADMIN")
    if not admin:
        print("Admin login failed; stopping.")
        return
    call("Admin: current user", "GET", "/api/v1/auth/me", token=admin)
    call("Admin: identity stats", "GET", "/api/v1/admin/identity/stats", token=admin)

    # ---------- onboarding: self-registered seller (with documents) ----------
    s1 = seller_payload("Self")
    otp = call("Send registration OTP (seller)", "POST", "/api/v1/auth/otp/send",
               {"target": s1["email"], "purpose": "REGISTRATION"})
    s1["emailOtp"] = otp and otp.get("devOtp")
    reg_s1 = call("Self-register seller (multipart with documents)", "POST", "/api/v1/auth/register/seller", s1,
                  files=[("PAN", "pan.pdf", pdf("PAN card")),
                         ("REGISTRATION_CERTIFICATE", "coi.pdf", pdf("Certificate of incorporation"))])

    # ---------- onboarding: self-registered buyer ----------
    b1 = buyer_payload("Self")
    otp = call("Send registration OTP (buyer)", "POST", "/api/v1/auth/otp/send",
               {"target": b1["email"], "purpose": "REGISTRATION"})
    b1["emailOtp"] = otp and otp.get("devOtp")
    reg_b1 = call("Self-register buyer", "POST", "/api/v1/auth/register/buyer", b1)

    # ---------- onboarding: admin-created seller & buyer ----------
    s2 = seller_payload("Admin")
    s2.pop("password"); s2.pop("emailOtp")
    created_s2 = call("Admin onboards seller", "POST", "/api/v1/admin/users/sellers", s2, token=admin)
    b2 = buyer_payload("Admin")
    b2.pop("password"); b2.pop("emailOtp")
    created_b2 = call("Admin onboards buyer", "POST", "/api/v1/admin/users/buyers", b2, token=admin)

    # ---------- admin review of self registrations ----------
    call("Admin: list pending users", "GET", "/api/v1/admin/users?status=PENDING_VERIFICATION&page=0&size=10", token=admin)
    if reg_s1:
        detail = call("Admin: seller application detail", "GET", f"/api/v1/admin/users/{reg_s1['userId']}", token=admin)
        for doc in (detail or {}).get("documents", []) or []:
            call(f"Admin: verify document {doc.get('documentType')}", "PUT",
                 f"/api/v1/admin/users/{reg_s1['userId']}/documents/{doc['id']}/verify",
                 {"verified": True, "remarks": "Document checked"}, token=admin)
        call("Admin: approve self-registered seller", "POST", f"/api/v1/admin/users/{reg_s1['userId']}/approve",
             {"remarks": "Documents verified, approved"}, token=admin)
    if reg_b1 and reg_b1.get("status") != "ACTIVE":
        call("Admin: approve self-registered buyer", "POST", f"/api/v1/admin/users/{reg_b1['userId']}/approve",
             {"remarks": "Approved"}, token=admin)

    # ---------- seller journey ----------
    seller = login(s1["email"], PASSWORD, "SELLER") if reg_s1 else None
    listing = None
    if seller:
        call("Seller: profile", "GET", "/api/v1/sellers/me/profile", token=seller)
        call("Seller: verification status", "GET", "/api/v1/sellers/me/verification", token=seller)
        listing = call("Seller: create & submit service listing", "POST", "/api/v1/seller/services", {
            "title": "Detailed Project Report for MSME term loans",
            "categoryId": category_id,
            "subcategoryId": subcategory_id,
            "shortDescription": "Bank-ready DPRs with financial projections",
            "description": "End-to-end preparation of detailed project reports including market study, technical feasibility and five-year financial projections.",
            "scopeOfWork": "Site visit, data collection, market analysis, financial modelling",
            "deliverables": "DPR document, CMA data, projected financials",
            "methodology": "Discovery workshop followed by iterative drafts",
            "targetIndustries": ["Manufacturing", "Food Processing"],
            "deliveryMode": "HYBRID",
            "serviceLocations": ["Lucknow", "Kanpur"],
            "state": "Uttar Pradesh",
            "district": "Lucknow",
            "experienceYears": 8,
            "estimatedDuration": "3-4 weeks",
            "indicativeFee": 45000,
            "pricingModel": "FIXED_FEE",
            "tags": ["DPR", "term loan", "MSME"],
            "submit": True,
        }, token=seller)
        call("Seller: my listings", "GET", "/api/v1/seller/services?page=0&size=10", token=seller)
    listing_id = id_of(listing)
    if listing_id:
        call("Admin: start listing review", "POST", f"/api/v1/admin/services/{listing_id}/start-review",
             {"remarks": "Picked up for review"}, token=admin)
        call("Admin: approve listing", "POST", f"/api/v1/admin/services/{listing_id}/approve",
             {"remarks": "Listing meets catalogue guidelines"}, token=admin)
        call("Public: listing detail", "GET", f"/api/v1/public/services/{listing_id}")

    # ---------- buyer journey ----------
    buyer = login(b1["email"], PASSWORD, "BUYER") if reg_b1 else None
    rfq = None
    if buyer:
        call("Buyer: profile", "GET", "/api/v1/buyers/me/profile", token=buyer)
        call("Buyer: dashboard", "GET", "/api/v1/buyer/dashboard", token=buyer)
        rfq = call("Buyer: create & publish RFQ", "POST", "/api/v1/buyer/rfqs", {
            "title": "DPR for pickle unit expansion",
            "categoryId": category_id,
            "subcategoryId": subcategory_id,
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
            "expectedStartDate": (date.today() + timedelta(days=10)).isoformat(),
            "submissionDeadline": (datetime.now() + timedelta(days=7)).replace(microsecond=0).isoformat(),
            "distributionMode": "SELECTED",
            "sellerIds": [reg_s1["userId"]] if reg_s1 else [],
            "publish": True,
        }, token=buyer)

    rfq_id = id_of(rfq)
    quotation = None
    if rfq_id and seller:
        call("Seller: invited RFQs", "GET", "/api/v1/seller/rfqs?page=0&size=10", token=seller)
        call("Seller: RFQ detail", "GET", f"/api/v1/seller/rfqs/{rfq_id}", token=seller)
        quotation = call("Seller: submit quotation", "POST", f"/api/v1/seller/rfqs/{rfq_id}/quotations", {
            "technicalProposal": "We will conduct a site visit, gather data and prepare a bank-ready DPR in 3 weeks.",
            "methodology": "Kick-off call, site visit, two review cycles",
            "scopeAccepted": True,
            "deliverables": "DPR document, CMA data, projected financials",
            "timelineDays": 21,
            "teamComposition": "1 CA, 1 industry analyst",
            "relevantExperience": "40+ DPRs for food processing units",
            "professionalFee": 45000,
            "taxPercent": 18,
            "otherExpenses": 5000,
            "paymentMilestones": "50% advance, 50% on delivery",
            "validUntil": (date.today() + timedelta(days=30)).isoformat(),
            "items": [
                {"description": "DPR preparation", "quantity": 1, "unit": "lot", "unitPrice": 40000},
                {"description": "CMA data", "quantity": 1, "unit": "lot", "unitPrice": 5000},
            ],
            "submit": True,
        }, token=seller)
    if rfq_id and buyer:
        call("Buyer: quotations for RFQ", "GET", f"/api/v1/buyer/rfqs/{rfq_id}/quotations", token=buyer)
        call("Buyer: comparison", "GET", f"/api/v1/buyer/rfqs/{rfq_id}/comparison", token=buyer)
    if quotation and buyer:
        call("Buyer: shortlist quotation", "POST", f"/api/v1/buyer/quotations/{id_of(quotation)}/shortlist",
             {"shortlisted": True}, token=buyer)
        call("Buyer: accept quotation", "POST", f"/api/v1/buyer/quotations/{id_of(quotation)}/accept", {
            "remarks": "Selected based on experience and price",
            "contractValue": 53100,
            "startDate": (date.today() + timedelta(days=10)).isoformat(),
            "expectedEndDate": (date.today() + timedelta(days=31)).isoformat(),
            "confirmed": True,
        }, token=buyer)
        call("Buyer: engagements", "GET", "/api/v1/buyer/engagements?page=0&size=10", token=buyer)

    # ---------- admin-created users can log in with the temporary password ----------
    if created_s2:
        login(s2["email"], created_s2["temporaryPassword"], "SELLER")
    if created_b2:
        login(b2["email"], created_b2["temporaryPassword"], "BUYER")

    call("Admin: procurement stats", "GET", "/api/v1/admin/procurement/stats", token=admin)

    onboarded = [
        ("Seller (self-registered)", s1["email"], PASSWORD, reg_s1),
        ("Buyer (self-registered)", b1["email"], PASSWORD, reg_b1),
        ("Seller (admin-onboarded)", s2["email"], (created_s2 or {}).get("temporaryPassword"), created_s2),
        ("Buyer (admin-onboarded)", b2["email"], (created_b2 or {}).get("temporaryPassword"), created_b2),
    ]
    write_report(onboarded, meta)


def write_report(onboarded, meta):
    passed = sum(1 for r in results if r[4])
    lines = [f"# bds-backend API smoke test — payloads", "",
             f"- Target: `{BASE}`", f"- Run: `{RUN}` at {datetime.now():%Y-%m-%d %H:%M}",
             f"- Result: **{passed}/{len(results)} passed**", "",
             "## Onboarded accounts", "", "| Account | Email | Password | userId | referenceNo |", "|---|---|---|---|---|"]
    for label, email, pwd, info in onboarded:
        info = info or {}
        lines.append(f"| {label} | {email} | `{pwd if is_local() else '<redacted>'}` | {info.get('userId', '—')} | {info.get('referenceNo', '—')} |")
        if not is_local():
            print(f"  account: {label}  {email}  {pwd}")
    lines += ["", "## Summary", "", "| # | Result | Status | Method | Path | Step |", "|---|---|---|---|---|---|"]
    for i, (name, method, path, status, ok) in enumerate(results, 1):
        lines.append(f"| {i} | {'PASS' if ok else 'FAIL'} | {status} | {method} | `{path}` | {name} |")
    lines += ["", "## Request / response payloads", ""]
    for i, (name, method, path, body, status, parsed, ok, authed) in enumerate(log, 1):
        lines += [f"### {i}. {name}", "", f"`{method} {path}`" + ("  — `Authorization: Bearer <token>`" if authed else ""), ""]
        if body is not None:
            lines += ["Request:", "```json", short(redact(body)), "```", ""]
        lines += [f"Response `{status}`:", "```json", short(redact(parsed)) if parsed is not None else "(empty)", "```", ""]
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"\n{passed}/{len(results)} passed. Payloads written to {OUT}")


if __name__ == "__main__":
    main()
    sys.exit(0 if all(r[4] for r in results) else 1)
