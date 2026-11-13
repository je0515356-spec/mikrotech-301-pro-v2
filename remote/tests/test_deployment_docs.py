"""Documentation deployment contract checks (no Cloudflare credentials required)."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
DOC = (ROOT / "DEPLOIEMENT_CLOUDFLARE_FR.md").read_text(encoding="utf-8")

class DeploymentDocumentationTests(unittest.TestCase):
    def test_dedicated_worker_does_not_replace_portal(self):
        self.assertIn("mikroteck-remote-relay", DOC)
        self.assertIn("portail captif", DOC.lower())
    def test_requires_secret_not_git(self):
        self.assertIn("wrangler secret put ADMIN_API_TOKEN", DOC)
        self.assertIn("ne pas le mettre dans le dépôt GitHub", DOC)
    def test_no_paid_plan_assumption(self):
        self.assertIn("Durable Objects", DOC)
        self.assertIn("arrêter ici", DOC)
    def test_has_test_without_router(self):
        self.assertIn("Invoke-RestMethod", DOC)
        self.assertIn("router_status", DOC)

if __name__ == "__main__":
    unittest.main()
