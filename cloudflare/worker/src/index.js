// MIKROTECK 301 PRO - Cloudflare Worker relay foundation.
// Safe phase: health, admin enrollment, authenticated heartbeat, read-only polling.
// Deploy only after reviewing secrets and database migrations.
// D1 binding: DB; Worker secret: ADMIN_BOOTSTRAP_SECRET.
// Uses existing routers and commands tables; does not execute router commands.

const json = (body, status = 200) => Response.json(body, {
  status,
  headers: { "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" }
});
const bytes = s => new TextEncoder().encode(s);
const hex = data => Array.from(new Uint8Array(data)).map(x => x.toString(16).padStart(2, "0")).join("");
async function sha256(value) {
  return hex(await crypto.subtle.digest("SHA-256", bytes(value)));
}
function equalConstantTime(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  const len = Math.max(a.length, b.length);
  let diff = a.length ^ b.length;
  for (let i = 0; i < len; i++) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}
async function readJson(request) {
  if (!(request.headers.get("content-type") || "").toLowerCase().includes("application/json")) throw new Error("Expected JSON");
  if (Number(request.headers.get("content-length") || 0) > 4096) throw new Error("Payload too large");
  const text = await request.text();
  if (text.length > 4096) throw new Error("Payload too large");
  return JSON.parse(text);
}
function bearer(request) {
  const h = request.headers.get("authorization") || "";
  return h.startsWith("Bearer ") ? h.slice(7) : "";
}
async function agent(request, env) {
  const id = request.headers.get("x-router-id") || "";
  const token = bearer(request);
  if (!/^[a-zA-Z0-9_-]{8,64}$/.test(id) || token.length < 32 || token.length > 256) return null;
  const router = await env.DB.prepare("SELECT id, credential_hash FROM routers WHERE id = ?").bind(id).first();
  if (!router || !equalConstantTime(router.credential_hash, await sha256(token))) return null;
  return router.id;
}
export default {
  async fetch(request, env) {
    try {
      if (!env.DB) return json({ error: "Database unavailable" }, 503);
      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/health") {
        await env.DB.prepare("SELECT 1").first();
        return json({ application: "MIKROTECK 301 PRO", service: "Remote API", status: "online", database: "connected" });
      }
      if (request.method === "POST" && url.pathname === "/admin/routers") {
        const secret = env.ADMIN_BOOTSTRAP_SECRET;
        if (!secret || secret.length < 32 || !equalConstantTime(bearer(request), secret)) return json({ error: "Unauthorized" }, 401);
        const body = await readJson(request);
        if (!/^[a-zA-Z0-9_-]{8,64}$/.test(body.id || "") || typeof body.name !== "string" || body.name.length > 80 ||
            !/^[a-zA-Z0-9_-]{1,64}$/.test(body.owner_id || "") ||
            typeof body.token !== "string" || body.token.length < 32 || body.token.length > 256)
          return json({ error: "Invalid router enrollment" }, 400);
        const hash = await sha256(body.token);
        await env.DB.prepare("INSERT INTO routers (id,name,owner_id,credential_hash) VALUES (?,?,?,?)")
          .bind(body.id, body.name, body.owner_id, hash).run();
        return json({ registered: true, router_id: body.id }, 201);
      }
      if (request.method === "POST" && url.pathname === "/agent/heartbeat") {
        const id = await agent(request, env);
        if (!id) return json({ error: "Unauthorized" }, 401);
        await env.DB.prepare("UPDATE routers SET last_seen = CURRENT_TIMESTAMP WHERE id = ?").bind(id).run();
        return json({ ok: true, router_id: id });
      }
      if (request.method === "GET" && url.pathname === "/agent/poll") {
        const id = await agent(request, env);
        if (!id) return json({ error: "Unauthorized" }, 401);
        // No command execution until authorization, anti-replay and ACK are implemented and tested.
        return json({ router_id: id, commands: [] });
      }
      return json({ error: "Not found" }, 404);
    } catch (e) {
      if (e?.message === "Expected JSON" || e?.message === "Payload too large" || e instanceof SyntaxError)
        return json({ error: "Invalid request" }, 400);
      if (String(e?.message || "").includes("UNIQUE constraint")) return json({ error: "Router already exists" }, 409);
      return json({ error: "Internal server error" }, 500);
    }
  }
};
