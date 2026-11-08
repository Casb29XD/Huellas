// verify-image: revisa los primeros bytes de cada foto nueva de pet-images y borra la que no es JPEG/PNG/WebP
// (el bucket solo valida el Content-Type declarado). La invoca el trigger verify_pet_image por pg_net con la
// service role; no la llama la app. Ver docs/SEGURIDAD_BACKEND.md.
import { isServiceRole, objectPath, sniffImage } from "./sniff.ts";

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  const url = Deno.env.get("SUPABASE_URL")!;
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  if (req.method !== "POST") return json(405, { status: "error", message: "Usa POST" });
  if (!isServiceRole(req.headers.get("Authorization"))) return json(403, { status: "error", message: "Solo service_role" });

  let name: unknown;
  try {
    name = (await req.json()).name;
  } catch { /* cuerpo no JSON */ }
  const path = objectPath(name);
  if (!path) return json(400, { status: "error", message: "name inválido" });

  const objectUrl = `${url}/storage/v1/object/pet-images/${path}`;
  const auth = { apikey: key, Authorization: `Bearer ${key}` };
  const head = await fetch(objectUrl, { headers: { ...auth, Range: "bytes=0-15" } });
  if (head.status === 400 || head.status === 404) {
    await head.body?.cancel();
    return json(200, { status: "missing" });
  }
  if (!head.ok) return json(502, { status: "error", message: `storage ${head.status}` });
  const first = new Uint8Array(await head.arrayBuffer()).subarray(0, 16);
  if (sniffImage(first)) return json(200, { status: "ok" });

  const del = await fetch(objectUrl, { method: "DELETE", headers: auth });
  await del.body?.cancel();
  return json(del.ok ? 200 : 502, { status: del.ok ? "removed" : "error" });
});
