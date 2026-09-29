import time
import json
import requests

BASE_URL = "http://localhost:8080/api"

# 1. Login
print("Logging in...")
login_res = requests.post(f"{BASE_URL}/auth/login", json={
    "email": "admin@example.com",
    "password": "Password123!"
})
print("Login status:", login_res.status_code)
token = login_res.json()["token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Upload sample expense report
print("\nUploading sample report...")
with open("sample_expense_claim.txt", "rb") as f:
    files = {"file": ("sample_expense_claim.txt", f, "text/plain")}
    upload_res = requests.post(f"{BASE_URL}/reports/upload", headers=headers, files=files)

print("Upload HTTP status:", upload_res.status_code)
print("Upload response body:", upload_res.text)
upload_data = upload_res.json()
report_id = upload_data.get("reportId") or upload_data.get("id")
print(f"Report ID: {report_id}")

# 3. Poll status
print("\nPolling report status at /api/reports/{report_id}/status ...")
final_status = None
for i in range(60):
    time.sleep(2)
    poll_res = requests.get(f"{BASE_URL}/reports/{report_id}/status", headers=headers)
    if poll_res.status_code == 200:
        rep = poll_res.json()
        status = rep.get("status")
        print(f"Poll {i+1}: status = {status}, lineItemCount = {rep.get('lineItemCount')}")
        if status in ("COMPLETE", "FAILED"):
            final_status = rep
            break
    else:
        print(f"Poll {i+1}: HTTP {poll_res.status_code} - {poll_res.text}")

print("\n=== STATUS RESULT ===")
print(json.dumps(final_status, indent=2))

# 4. Fetch AuditRun & Findings
print("\nFetching Audit Report at /api/reports/{report_id}/audit ...")
audit_res = requests.get(f"{BASE_URL}/reports/{report_id}/audit", headers=headers)
print("Audit HTTP status:", audit_res.status_code)
if audit_res.status_code == 200:
    audit_data = audit_res.json()
    print("\n=== RESULTING AUDIT RUN JSON ===")
    print(json.dumps(audit_data, indent=2))
else:
    print("Audit response error:", audit_res.text)
