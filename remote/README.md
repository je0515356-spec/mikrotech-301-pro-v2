# MIKROTECK 301 PRO — Remote Control V1 (development)

This branch introduces an **unreleased proof of concept**, not a production-ready remote administration service.

## Goal
One APK per customer, lifetime license bound to one router, automatic local/remote selection, no VPN app and no VPS. Starlink CGNAT is compatible with outbound HTTPS polling.

## Protocol prototype
- `POST /v1/routers/{routerId}/queue` with `{"action":"router_status"}`
- `GET /v1/routers/{routerId}/next`
- `POST /v1/routers/{routerId}/result` with `{"id":"...","result":{}}`
- `GET /v1/routers/{routerId}/result/{jobId}`

The worker accepts only three read-only action names. It does **not** expose generic RouterOS commands. Jobs and results are stored in a Durable Object.

## Required before connecting real customers
1. Replace prototype shared ADMIN_API_TOKEN with distinct router credentials and verified per-customer Supabase JWT + ownership checks.
2. Add pairing, credential rotation/revocation, signed short-lived job leases, replay protection, TTL and queue limits.
3. Build a RouterOS v7 HTTPS polling agent, TLS certificate validation, safe output encoding and constrained RouterOS commands.
4. Integrate Android API client and local-to-remote fallback; show explicit connectivity state.
5. Implement license recovery when phone changes; enforce one active router per license server-side.
6. Rate-limit and audit requests; minimize collected personal information.
7. Test with a real hAP ac² and Starlink, then CI-build and sign the APK.

**Never deploy this prototype with a production MikroTik password or customer data.** The single shared token is intentionally a development-only placeholder. RouterOS scripts cannot be assumed to support all JSON operations without testing.
