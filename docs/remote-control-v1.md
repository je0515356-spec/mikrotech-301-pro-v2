# Remote Control V1 — architecture and delivery checklist

Status: **design only; not yet implemented or validated on hardware**.

## User experience
- Local mode remains unchanged and works without Internet.
- Remote mode uses HTTPS to a backend relay, not a manually enabled Android ZeroTier VPN.
- One customer account can be paired to its own MikroTik router; owner may revoke access.
- Android shows Connected / Offline / Pending and never claims success before RouterOS acknowledges a command.

## Network design
Android app -> HTTPS authenticated relay -> RouterOS 7 polling agent -> RouterOS local operations.
The router makes outbound TLS requests, so no Starlink CGNAT inbound port-forwarding is needed.
A static Cloudflare Pages site is not a relay. Cloudflare Workers plus a durable state store is one possible implementation, subject to deployment and quotas.

## Minimum API contract (proposed)
- POST /v1/routers/enroll: authenticated one-time pairing, router receives its own scoped credentials.
- POST /v1/routers/{id}/commands: authenticated owner queues allowlisted command, returns command id, not immediate success.
- GET /v1/routers/{id}/commands/{commandId}: owner polls status queued/running/succeeded/failed/expired.
- POST /v1/agent/poll: router authenticates and requests pending allowlisted commands.
- POST /v1/agent/results: router reports result, with command id and nonce.
- GET /v1/routers/{id}/health: last-seen and status.

## Security requirements
- TLS, unique credentials per router, credential rotation/revocation.
- Do not expose RouterOS API 8728/8729 publicly or embed router admin passwords in the Android APK.
- Validate user ownership server-side on every request.
- Use narrowly allowlisted operations, not arbitrary RouterOS scripts from Internet clients.
- Commands have expiration, idempotency keys, replay protection, audit logs, rate limits.
- Use least-privilege RouterOS account; redact secrets from logs.
- No remote factory reset or firewall/WAN modifications in V1.

## First commands
- List Hotspot active sessions
- List PPPoE active sessions
- Create Hotspot voucher with validated profile
- Disable/enable a Hotspot user
- Disconnect an active session with explicit confirmation

## Before production
1. Implement and deploy the relay with persistence and authenticated pairing.
2. Implement RouterOS agent and validate compatibility with hAP ax2 RouterOS 7.
3. Add Android Local / Remote selection and real API integration.
4. Add automated contract/security tests and APK CI build.
5. Perform real tests over mobile 4G while router uses Starlink CGNAT, including power outage/reconnection.
6. Document relay hosting costs: a lifetime app license does not imply free lifetime server hosting.

No APK or remote-control success should be announced until all relevant checks pass.
