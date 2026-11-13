# Android remote integration — current status

Added `app/src/main/java/com/example/remote/RemoteRelayClient.kt`:
- HTTPS client using the app's existing OkHttp and coroutine dependencies
- enqueue `router_status`, fetch job results, explicit HTTP error handling
- router/job identifier validation and no embedded secrets
- **not wired into UI yet**, no production login/token issuing mechanism
- the current Worker still requires the admin token to queue and fetch results; never insert that token into an APK

## Required before enabling a remote UI
1. Cloudflare authentication with short-lived customer-scoped tokens and verified ownership of router IDs; Worker must reject unauthorised customers.
2. Durable Object queue limits, expiry, idempotent result handling and observability.
3. Router enrollment and safe agent polling with individual router credentials.
4. End-to-end tests with an isolated lab setup; then backup and controlled read-only test on the production hAP ax².
5. Android UI state handling (local/remote/offline), signed debug build, hardware verification.

**Do not distribute a remote-control APK from this branch as finished.**
