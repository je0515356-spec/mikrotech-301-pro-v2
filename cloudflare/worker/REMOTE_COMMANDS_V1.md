# MIKROTECK remote commands V1 (test router only)

This code is committed to the feature branch but **NOT automatically deployed** to Cloudflare. Existing heartbeat and poll remain compatible. Do not deploy to a production router.

## Server endpoints

- POST /admin/commands — requires ADMIN_BOOTSTRAP_SECRET; JSON: {"router_id":"hap-ac2-test-v2","action":"router.identity","payload":{}}. Only this read-only action is permitted.
- GET /admin/commands/{uuid} — admin-only status lookup.
- GET /agent/poll — router token + X-Router-Id; atomically claims at most one pending identity command and returns {"router_id":"...","commands":[{"id":"uuid","action":"router.identity"}]}.
- POST /agent/ack — router token + X-Router-Id; JSON {"command_id":"uuid","status":"success","result":"short identity"}; only a running command for that same router can be acknowledged.
- Existing GET /health and POST /agent/heartbeat are unchanged.

## Limitations / security

This is a **read-only queue prototype**, not a finished remote control APK. No router-side command executor or Android UI is integrated yet. Admin bootstrap secret must NEVER be placed in an Android APK or client-side JavaScript. Router tokens must remain private and should be rotated if exposed. The old hap-ac2-test token should be revoked from D1 after verifying the new enrollment. For multi-customer release, implement user authentication and router-scoped authorization, replay/timeout recovery, token revocation and rotation, audit logs, rate limiting, and safe command allowlists. Do not enable destructive router actions until end-to-end tests pass.

The D1 schema already created in this project is compatible; no migration is required for these endpoints. A claimed command remains running until ACK; an unacknowledged running command is not automatically retried to avoid accidental duplicate execution.

## Deployment

Copy cloudflare/worker/src/index.js into the existing Worker, retaining D1 binding DB and secret ADMIN_BOOTSTRAP_SECRET. Deploy manually in Cloudflare. Do not paste secrets into GitHub. Test only on hAP ac2 test. Do not touch SERVEUR NASS.
