// MIKROTECK remote relay prototype. Deploy only after configuring secrets and durable storage.
// This intentionally does not accept arbitrary RouterOS commands.
export interface Env {
  ROUTERS: DurableObjectNamespace;
  ADMIN_API_TOKEN: string;
  ROUTER_TOKENS?: string; // JSON map router ID -> individual test-only bearer token
}
type Action = "list_hotspot" | "list_pppoe" | "router_status";
const allowed = new Set<Action>(["list_hotspot", "list_pppoe", "router_status"]);
const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), {status, headers: {"content-type":"application/json","cache-control":"no-store"}});
function authorized(request: Request, token: string): boolean {
  const provided = request.headers.get("authorization");
  return Boolean(token && provided === `Bearer ${token}`);
}
export class RouterSession {
  private state: DurableObjectState;
  constructor(state: DurableObjectState) { this.state = state; }
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "POST" && url.pathname === "/queue") {
      const body = await request.json() as {action?: Action};
      if (!body.action || !allowed.has(body.action)) return json({error:"unsupported_action"},400);
      const id = crypto.randomUUID();
      await this.state.storage.put(`job:${id}`, {id, action:body.action, createdAt:Date.now()});
      return json({id,status:"queued"},202);
    }
    if (request.method === "GET" && url.pathname === "/next") {
      const entries = await this.state.storage.list<{id:string,action:Action,createdAt:number}>({prefix:"job:",limit:1});
      const first = entries.entries().next();
      return json(first.done ? {job:null} : {job:first.value[1]});
    }
    if (request.method === "POST" && url.pathname === "/result") {
      const body = await request.json() as {id?:string,result?:unknown};
      if (!body.id || !/^[0-9a-f-]{36}$/.test(body.id)) return json({error:"invalid_id"},400);
      const key = `job:${body.id}`;
      if (!await this.state.storage.get(key)) return json({error:"job_not_found"},404);
      await this.state.storage.put(`result:${body.id}`, {result:body.result, completedAt:Date.now()});
      await this.state.storage.delete(key);
      return json({ok:true});
    }
    if (request.method === "GET" && url.pathname.startsWith("/result/")) {
      const id = url.pathname.slice("/result/".length);
      const result = await this.state.storage.get(`result:${id}`);
      return result ? json(result) : json({status:"pending"},202);
    }
    return json({error:"not_found"},404);
  }
}
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") return json({service:"mikroteck-relay",status:"online",routerConnected:false});
    const match = url.pathname.match(/^\/v1\/routers\/([a-zA-Z0-9_-]{8,80})(\/.*)$/);
    if (!match) return json({error:"not_found"},404);
    // Lab transition: administrator queues/reads results; each router may only poll and submit.
    // No customer APK access until owner-scoped authentication is implemented.
    const path = match[2];
    const isRouterRoute = (request.method === "GET" && path === "/next") ||
      (request.method === "POST" && path === "/result");
    const isAdminRoute = (request.method === "POST" && path === "/queue") ||
      (request.method === "GET" && /^\/result\/[0-9a-f-]{36}$/.test(path));
    if (!isRouterRoute && !isAdminRoute) return json({error:"not_found"},404);
    let tokens: Record<string,string> = {};
    if (env.ROUTER_TOKENS) {
      try {
        const parsed: unknown = JSON.parse(env.ROUTER_TOKENS);
        if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return json({error:"invalid_router_configuration"},503);
        tokens = parsed as Record<string,string>;
      } catch { return json({error:"invalid_router_configuration"},503); }
    }
    if (isRouterRoute) {
      const routerToken = tokens[match[1]];
      if (typeof routerToken !== "string" || routerToken.length < 32) return json({error:"router_not_enrolled"},403);
      if (!authorized(request,routerToken)) return json({error:"unauthorized"},401);
    } else {
      if (!authorized(request,env.ADMIN_API_TOKEN)) return json({error:"unauthorized"},401);
    }
    const id = env.ROUTERS.idFromName(match[1]);
    const target = new URL(request.url);
    target.pathname = match[2];
    return env.ROUTERS.get(id).fetch(new Request(target,request));
  }
};
