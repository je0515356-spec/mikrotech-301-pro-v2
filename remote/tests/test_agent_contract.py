"""Static safety contract checks; these do not execute RouterOS."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
AGENT = (ROOT / "routeros" / "agent-v7.rsc").read_text(encoding="utf-8")
WORKER = (ROOT / "worker" / "src" / "index.ts").read_text(encoding="utf-8")

class AgentContractTests(unittest.TestCase):
    def test_tls_verification_enabled(self):
        self.assertGreaterEqual(AGENT.count("check-certificate=yes"), 2)

    def test_only_status_action_executed(self):
        self.assertIn('($action != "router_status")', AGENT)
        self.assertNotIn(":execute", AGENT)
        self.assertNotIn("/ip hotspot user add", AGENT)
        self.assertNotIn("/ppp secret add", AGENT)

    def test_no_scheduler_or_firewall_changes(self):
        self.assertNotIn("/system scheduler add", AGENT)
        self.assertNotIn("/ip firewall", AGENT)

    def test_relay_protocol_paths(self):
        for path in ('"/next"', '"/result"', '"/queue"'):
            self.assertIn(path, WORKER)
        self.assertIn('"/next"', AGENT)
        self.assertIn('"/result"', AGENT)

    def test_no_real_credentials_committed(self):
        self.assertIn("YOUR-TEST-TOKEN", (ROOT / "routeros" / "README.md").read_text())
        self.assertNotIn("sk_live_", AGENT)

if __name__ == "__main__":
    unittest.main()
