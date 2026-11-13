"""Static security contract checks for the experimental Cloudflare relay.

These checks do not replace integration tests or RouterOS hardware tests.
"""
from pathlib import Path
import re
import unittest

SOURCE = (Path(__file__).resolve().parents[1] / "worker" / "src" / "index.ts").read_text(encoding="utf-8")

class RelaySecurityTests(unittest.TestCase):
    def test_router_token_is_distinct_from_admin_token(self):
        self.assertIn("ROUTER_TOKENS?: string", SOURCE)
        self.assertIn("authorized(request,routerToken)", SOURCE)
        self.assertIn("authorized(request,env.ADMIN_API_TOKEN)", SOURCE)

    def test_router_can_only_poll_and_submit(self):
        self.assertIn('path === "/next"', SOURCE)
        self.assertIn('path === "/result"', SOURCE)
        self.assertIn("isRouterRoute", SOURCE)
        self.assertIn("isAdminRoute", SOURCE)

    def test_result_lookup_regex_is_valid(self):
        self.assertIn(r'/^\/result\/[0-9a-f-]{36}$/.test(path)', SOURCE)
        self.assertNotIn(r'/^\\/result\\/', SOURCE)

    def test_rejects_unknown_actions(self):
        self.assertIn("allowed.has(body.action)", SOURCE)

    def test_no_arbitrary_routeros_execution(self):
        self.assertNotIn("eval(", SOURCE)
        self.assertNotIn("new Function(", SOURCE)

if __name__ == "__main__":
    unittest.main()
