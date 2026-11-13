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
      if (request.method === "POST" && url.pathname === "/admin/commands") {
        const secret = env.ADMIN_BOOTSTRAP_SECRET;
        if (!secret || secret.length < 32 || !equalConstantTime(bearer(request), secret)) return json({ error: "Unauthorized" }, 401);
        const body = await readJson(request);
        if (!/^[a-zA-Z0-9_-]{8,64}$/.test(body.router_id || "") ||
            body.action !== "router.identity" || (body.payload != null && Object.keys(body.payload).length !== 0))
          return json({ error: "Only read-only router.identity is supported" }, 400);
        const exists = await env.DB.prepare("SELECT id FROM routers WHERE id = ?").bind(body.router_id).first();
        if (!exists) return json({ error: "Router not found" }, 404);
        const id = crypto.randomUUID();
        await env.DB.prepare("INSERT INTO commands (id,router_id,action,payload,status,expires_at) VALUES (?,?,?,'{}','pending',datetime('now','+5 minutes'))")
          .bind(id, body.router_id, "router.identity").run();
        return json({ command_id: id, status: "pending" }, 201);
      }
      if (request.method === "GET" && /^\\/admin\\/commands\\/[0-9a-f-]{36}$/.test(url.pathname)) {
        const secret = env.ADMIN_BOOTSTRAP_SECRET;
        if (!secret || secret.length < 32 || !equalConstantTime(bearer(request), secret)) return json({ error: "Unauthorized" }, 401);
        const id = url.pathname.split("/").pop();
        const command = await env.DB.prepare("SELECT id,router_id,action,status,result,created_at,expires_at FROM commands WHERE id = ?").bind(id).first();
        return command ? json(command) : json({ error: "Not found" }, 404);
      }
      if (request.method === "POST" && url.pathname === "/agent/ack") {
        const id = await agent(request, env);
        if (!id) return json({ error: "Unauthorized" }, 401);
        const body = await readJson(request);
        if (!/^[0-9a-f-]{36}$/.test(body.command_id || "") ||
            !["success", "failed"].includes(body.status) ||
            typeof body.result !== "string" || body.result.length > 512)
          return json({ error: "Invalid acknowledgement" }, 400);
        const updated = await env.DB.prepare(
          "UPDATE commands SET status = ?, result = ? WHERE id = ? AND router_id = ? AND action = 'router.identity' AND status = 'running'"
        ).bind(body.status, body.result, body.command_id, id).run();
        return updated.meta.changes === 1 ? json({ ok: true }) : json({ error: "Command not running" }, 409);
      }
      if (request.method === "GET" && url.pathname === "/agent/poll") {
        const id = await agent(request, env);
        if (!id) return json({ error: "Unauthorized" }, 401);
        await env.DB.prepare("UPDATE commands SET status = 'expired' WHERE router_id = ? AND status = 'pending' AND expires_at <= CURRENT_TIMESTAMP").bind(id).run();
        const cmd = await env.DB.prepare("SELECT id,action FROM commands WHERE router_id = ? AND status = 'pending' AND expires_at > CURRENT_TIMESTAMP AND action = 'router.identity' ORDER BY created_at ASC LIMIT 1").bind(id).first();
        if (!cmd) return json({ router_id: id, commands: [] });
        const claimed = await env.DB.prepare("UPDATE commands SET status = 'running' WHERE id = ? AND router_id = ? AND status = 'pending'").bind(cmd.id,id).run();
        return json({ router_id: id, commands: claimed.meta.changes === 1 ? [cmd] : [] });
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
