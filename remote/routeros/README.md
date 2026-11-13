# RouterOS v7 read-only polling agent — laboratory build

**Not production ready. Do not install on customer routers.** This script is deliberately limited to `router_status`; it cannot create tickets, modify users or execute arbitrary RouterOS instructions.

## Prerequisites
- Test MikroTik hAP ac² running RouterOS v7 (7.15+ recommended for JSON deserialize/serialize).
- Correct router clock, working DNS, outbound HTTPS (TCP 443).
- A test Cloudflare Worker deployment and **test-only** token. Never put a production admin token into RouterOS.
- Back up the router and inspect the script before running. Use a restricted RouterOS account where possible.

## Manual laboratory test
1. Deploy the Worker in a **non-production** Cloudflare environment and set `ADMIN_API_TOKEN` as a Worker secret. The current shared-token design is not safe for real customers.
2. In a private RouterOS terminal session set:
   ```routeros
   :global mkRemoteBase "https://YOUR-WORKER.workers.dev"
   :global mkRemoteRouterId "router-test-0001"
   :global mkRemoteToken "YOUR-TEST-TOKEN-AT-LEAST-16-CHARS"
   ```
3. Review and import `agent-v7.rsc` **only on the test router**; alternatively paste its contents into the terminal. The script runs once and does not install a scheduler.
4. Queue a `router_status` job via authenticated `POST /v1/routers/router-test-0001/queue`.
5. Run the agent again and query `GET /v1/routers/router-test-0001/result/{jobId}`. Expected: `status: online`, uptime, RouterOS version.
6. Queue `list_hotspot` or `list_pppoe`: the agent must ignore these; they are **not implemented**.

## Known limitations / security blockers
- Current Worker uses a **single shared bearer token** across routes. This is lab-only; replace with per-router credentials and owner-authenticated queue access before commercial use.
- Global variables may be visible to privileged RouterOS users and in exports. Never expose token in logs, screenshots or backups.
- JSON syntax and `/tool fetch` behavior require validation on the target RouterOS release; no hardware test has been performed.
- A repeated queued job may be delivered more than once if upload fails; current action is read-only. Add leases, expiry, idempotency and retry policy before enabling writes.
- `check-certificate=yes` requires valid trusted TLS certificate chain and correct time.
- RouterOS globals set in an interactive session may not be available to a scheduled script running as another user. Future production agent needs a secure, explicitly documented configuration and permission model.
- There is no scheduler, automatic enrollment, APK integration or multi-tenant authentication yet.

## Next implementation
Add per-router token verification, owner authorization, enrollment and revocation, bounded queue TTL, test harness, then scheduled polling and Android local/remote selection.
