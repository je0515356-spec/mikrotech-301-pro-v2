// MIKROTECK remote relay prototype. Deploy only after configuring secrets and durable storage.
// This intentionally does not accept arbitrary RouterOS commands.
export interface Env {
  ROUTERS: DurableObjectNamespace;
  ADMIN_API_TOKEN: string;
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
    const match = url.pathname.match(/^\/v1\/routers\/([a-zA-Z0-9_-]{8,80})(\/.*)$/);
    if (!match) return json({error:"not_found"},404);
    // Prototype only: a single server token is NOT suitable for commercial multi-tenant access.
    // Replace with per-router credentials and authenticated owner membership before deployment.
    if (!authorized(request,env.ADMIN_API_TOKEN)) return json({error:"unauthorized"},401);
    const id = env.ROUTERS.idFromName(match[1]);
    const target = new URL(request.url);
    target.pathname = match[2];
    return env.ROUTERS.get(id).fetch(new Request(target,request));
  }
};
