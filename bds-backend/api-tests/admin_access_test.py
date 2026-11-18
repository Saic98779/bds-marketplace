"""Checks that administrators can use every seller/buyer endpoint while sellers and buyers stay confined to their own data.

Run smoke_test.py first; this reuses the accounts and records it created.

    python -I admin_access_test.py <seller_email> <buyer_email> <password> [base_url]
"""
import sys
from datetime import date, datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import smoke_test as st  # noqa: E402

ADMIN_HEADER = "X-Act-As-User"


def call(name, method, path, body=None, token=None, expect=(200, 201), act_as=None):
    if act_as is None:
        return st.call(name, method, path, body, token, expect=expect)
    original = st.request.Request

    def with_header(*args, **kwargs):
        req = original(*args, **kwargs)
        req.add_header(ADMIN_HEADER, str(act_as))
        return req

    st.request.Request = with_header
    try:
        return st.call(f"{name} [{ADMIN_HEADER}: {act_as}]", method, path, body, token, expect=expect)
    finally:
        st.request.Request = original


def main():
    seller_email, buyer_email, password = sys.argv[1:4]
    st.BASE = (sys.argv[4] if len(sys.argv) > 4 else "http://localhost:8080").rstrip("/")
    st.OUT = Path(__file__).with_name("admin_access_payloads.md")

    admin = st.login("admin@bdsmarketplace.in", "Admin@12345", "ADMIN")
    seller = st.login(seller_email, password, "SELLER")
    buyer = st.login(buyer_email, password, "BUYER")
    seller_id = call("Seller: me", "GET", "/api/v1/auth/me", token=seller)["id"]
    buyer_id = call("Buyer: me", "GET", "/api/v1/auth/me", token=buyer)["id"]

    # ---------- admin sees all records on seller/buyer endpoints ----------
    for path in ["/api/v1/seller/rfqs", "/api/v1/seller/rfqs/1", "/api/v1/seller/quotations", "/api/v1/seller/quotations/1",
                 "/api/v1/seller/quotations/1/versions", "/api/v1/seller/quotations/1/pdf", "/api/v1/seller/engagements",
                 "/api/v1/seller/engagements/1", "/api/v1/seller/work-orders", "/api/v1/seller/services", "/api/v1/seller/reviews",
                 "/api/v1/buyer/rfqs", "/api/v1/buyer/rfqs/1", "/api/v1/buyer/rfqs/1/quotations", "/api/v1/buyer/rfqs/1/comparison",
                 "/api/v1/buyer/quotations", "/api/v1/buyer/quotations/1", "/api/v1/buyer/quotations/1/versions",
                 "/api/v1/buyer/quotations/1/pdf", "/api/v1/buyer/engagements", "/api/v1/buyer/engagements/1",
                 "/api/v1/buyer/work-orders", "/api/v1/buyer/feedback/questions", "/api/v1/buyer/recommended-sellers?categoryId=1"]:
        call(f"Admin: {path}", "GET", path, token=admin)

    # ---------- "my account" endpoints need a target user ----------
    for path in ["/api/v1/sellers/me/profile", "/api/v1/seller/dashboard", "/api/v1/seller/services/stats", "/api/v1/buyers/me/profile",
                 "/api/v1/buyer/dashboard"]:
        call(f"Admin without target: {path} -> 400", "GET", path, token=admin, expect=(400,))
    for path in ["/api/v1/sellers/me/profile", "/api/v1/sellers/me/verification", "/api/v1/sellers/me/documents",
                 "/api/v1/seller/dashboard", "/api/v1/seller/services/stats", "/api/v1/seller/reviews/summary"]:
        call(f"Admin as seller: {path}", "GET", path, token=admin, act_as=seller_id)
    for path in ["/api/v1/buyers/me/profile", "/api/v1/buyer/dashboard"]:
        call(f"Admin as buyer: {path}", "GET", path, token=admin, act_as=buyer_id)
    call("Admin as wrong role: buyer id on seller profile -> 404", "GET", "/api/v1/sellers/me/profile", token=admin,
         act_as=buyer_id, expect=(404,))

    # ---------- admin performs seller/buyer actions ----------
    rfq = call("Admin creates RFQ on behalf of buyer", "POST", "/api/v1/buyer/rfqs", {
        "title": "Admin-assisted DPR requirement",
        "categoryId": 1,
        "description": "Raised by the administrator on behalf of the purchaser for a food processing DPR.",
        "deliveryMode": "ONLINE",
        "budgetMax": 50000,
        "submissionDeadline": (datetime.now() + timedelta(days=5)).replace(microsecond=0).isoformat(),
        "distributionMode": "SELECTED",
        "sellerIds": [seller_id],
        "publish": True,
    }, token=admin, act_as=buyer_id)
    call("Admin creates RFQ without target -> 400", "POST", "/api/v1/buyer/rfqs", {
        "title": "Missing target", "categoryId": 1, "description": "This request names no purchaser so it must be refused.",
        "submissionDeadline": (datetime.now() + timedelta(days=5)).replace(microsecond=0).isoformat(),
        "distributionMode": "SELECTED", "publish": False,
    }, token=admin, expect=(400,))
    rfq_id = st.id_of(rfq)
    if rfq_id:
        call("Buyer sees the RFQ the admin raised for them", "GET", f"/api/v1/buyer/rfqs/{rfq_id}", token=buyer)
        call("Admin extends the buyer's RFQ deadline", "POST", f"/api/v1/buyer/rfqs/{rfq_id}/extend-deadline", {
            "submissionDeadline": (datetime.now() + timedelta(days=9)).replace(microsecond=0).isoformat(),
            "remarks": "Extended by administrator"}, token=admin)
        quotation = call("Admin submits quotation on behalf of seller", "POST", f"/api/v1/seller/rfqs/{rfq_id}/quotations", {
            "technicalProposal": "Quotation entered by the administrator on behalf of the provider.",
            "deliverables": "DPR document",
            "timelineDays": 20,
            "professionalFee": 40000,
            "taxPercent": 18,
            "validUntil": (date.today() + timedelta(days=30)).isoformat(),
            "submit": True,
        }, token=admin, act_as=seller_id)
        if st.id_of(quotation):
            call("Admin shortlists quotation", "POST", f"/api/v1/buyer/quotations/{st.id_of(quotation)}/shortlist",
                 {"shortlisted": True}, token=admin)
        call("Admin cancels the RFQ", "POST", f"/api/v1/buyer/rfqs/{rfq_id}/cancel", {"remarks": "Admin access test clean-up"}, token=admin)

    # ---------- sellers and buyers are still confined ----------
    call("Seller cannot use buyer endpoints -> 403", "GET", "/api/v1/buyer/rfqs", token=seller, expect=(403,))
    call("Buyer cannot use seller endpoints -> 403", "GET", "/api/v1/seller/quotations", token=buyer, expect=(403,))
    call("Seller cannot use admin endpoints -> 403", "GET", "/api/v1/admin/users", token=seller, expect=(403,))
    call("Seller ignores act-as header (own profile)", "GET", "/api/v1/sellers/me/profile", token=seller, act_as=buyer_id)
    other = st.call("Self-register second buyer", "POST", "/api/v1/auth/otp/send",
                    {"target": f"buyer.other.{st.RUN}@example.com", "purpose": "REGISTRATION"})
    b = st.buyer_payload("Other", other and other.get("devOtp"))
    b["email"] = f"buyer.other.{st.RUN}@example.com"
    reg = st.call("Register second buyer", "POST", "/api/v1/auth/register/buyer", b)
    if reg:
        call("Admin approves second buyer", "POST", f"/api/v1/admin/users/{reg['userId']}/approve", {"remarks": "ok"}, token=admin)
        other_buyer = st.login(b["email"], st.PASSWORD, "BUYER")
        call("Other buyer cannot see first buyer's RFQ -> 404", "GET", "/api/v1/buyer/rfqs/1", token=other_buyer, expect=(404,))
        call("Other buyer's RFQ list excludes others", "GET", "/api/v1/buyer/rfqs", token=other_buyer)

    # ---------- restricted sub-admin now has full access ----------
    sub = call("Admin creates sub-admin with one privilege", "POST", "/api/v1/admin/users/admins", {
        "fullName": "Reports Only Admin", "email": f"subadmin.{st.RUN}@example.com", "designation": "Analyst",
        "permissions": ["REPORT_VIEW"]}, token=admin)
    if sub:
        sub_token = st.login(f"subadmin.{st.RUN}@example.com", sub["temporaryPassword"], "ADMIN")
        for path in ["/api/v1/admin/users", "/api/v1/admin/audit-logs", "/api/v1/admin/settings", "/api/v1/admin/rfqs",
                     "/api/v1/admin/categories", "/api/v1/seller/quotations"]:
            call(f"Sub-admin: {path}", "GET", path, token=sub_token)

    st.write_report([("Seller", seller_email, password, {"userId": seller_id}),
                     ("Buyer", buyer_email, password, {"userId": buyer_id})], None)


if __name__ == "__main__":
    main()
    sys.exit(0 if all(r[4] for r in st.results) else 1)
