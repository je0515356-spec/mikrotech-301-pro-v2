"""Offline checks for the read-only RouterOS agent; never contacts a real router."""
from pathlib import Path
import unittest

AGENT = (Path(__file__).resolve().parents[1] / "routeros" / "agent-v7.rsc").read_text(encoding="utf-8")
WORKER = (Path(__file__).resolve().parents[1] / "worker" / "src" / "index.ts").read_text(encoding="utf-8")

class RouterAgentContractTests(unittest.TestCase):
    def test_https_and_certificate_validation(self):
        self.assertIn('[:pick $mkRemoteBase 0 8] != "https://"', AGENT)
        self.assertGreaterEqual(AGENT.count("check-certificate=yes"), 2)

    def test_read_only_status_only(self):
        self.assertIn('if ($action != "router_status")', AGENT)
        self.assertIn("/system resource get uptime", AGENT)
        self.assertIn("/system resource get version", AGENT)
        for forbidden in ("/ip hotspot user add", "/ip hotspot user remove",
                          "/ppp secret add", "/ppp secret remove", "/system reset-configuration"):
            self.assertNotIn(forbidden, AGENT)

    def test_agent_and_worker_agree_on_routes(self):
        for route in ('"/next"', '"/result"'):
            self.assertIn(route, WORKER)
            self.assertIn(route, AGENT)

    def test_health_endpoint_is_read_only(self):
        self.assertIn('request.method === "GET" && url.pathname === "/health"', WORKER)
        self.assertIn('routerConnected:false', WORKER)

if __name__ == "__main__":
    unittest.main()
