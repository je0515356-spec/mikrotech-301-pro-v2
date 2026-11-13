#!/usr/bin/env python3
"""LAB ONLY: simulate a RouterOS agent using HTTPS. No MikroTik access.

Set MK_RELAY_URL, MK_ROUTER_ID, MK_ROUTER_TOKEN and (optional) MK_ADMIN_TOKEN.
Never use credentials from a production router.
"""
import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("MK_RELAY_URL", "").rstrip("/")
ROUTER_ID = os.environ.get("MK_ROUTER_ID", "")
TOKEN = os.environ.get("MK_ROUTER_TOKEN", "")

def request(method, path, token, body=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Authorization": "Bearer " + token, "Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=12) as response:
        return response.status, json.load(response)

def main():
    if not BASE.startswith("https://") or not ROUTER_ID or len(TOKEN) < 32:
        sys.exit("Missing HTTPS relay URL, router ID, or >=32-character test token")
    prefix = "/v1/routers/" + ROUTER_ID
    if os.environ.get("MK_ADMIN_TOKEN"):
        code, queued = request("POST", prefix + "/queue",
                               os.environ["MK_ADMIN_TOKEN"], {"action": "router_status"})
        print("Queued read-only test job:", queued.get("id"), "HTTP", code)
    code, response = request("GET", prefix + "/next", TOKEN)
    job = response.get("job")
    if job is None:
        print("No pending job; router-token authentication succeeded (HTTP", code, ")")
        return
    if job.get("action") != "router_status":
        sys.exit("Refusing non-status action; job remains pending")
    job_id = job.get("id")
    if not isinstance(job_id, str) or len(job_id) != 36:
        sys.exit("Invalid job ID")
    code, result = request("POST", prefix + "/result", TOKEN,
                           {"id": job_id, "result": {"status": "simulated",
                            "uptime": "0s", "routerosVersion": "LAB-NO-ROUTER"}})
    print("Simulation complete (HTTP", code, "):", result)
    admin = os.environ.get("MK_ADMIN_TOKEN")
    if admin:
        _, verified = request("GET", prefix + "/result/" + job_id, admin)
        print("Result retrieved by admin:", verified)

if __name__ == "__main__":
    try:
        main()
    except urllib.error.HTTPError as exc:
        sys.exit("Relay HTTP %s: %s" % (exc.code, exc.read().decode()[:250]))
