// moderate-ai: segunda opinión de IA (DeepSeek) para la cola de moderación. Ver docs/MODERACION_IA.md.
// POST {"publication_id"} con el JWT de un moderador. Guarda el veredicto en publication_ai_reviews.
import { createClient } from "jsr:@supabase/supabase-js@2";

const VERDICTS = ["APROBAR", "RECHAZAR", "REVISAR"];
const REASONS = ["Venta de animales", "Contenido falso", "Fotos inadecuadas", "Duplicado", "Otro"];

const SYSTEM_PROMPT = `Eres moderador de "Huellas", una app colombiana de adopción, hogares temporales, mascotas perdidas/encontradas y servicios veterinarios.
Evalúa la publicación que recibes según estas reglas:
- Prohibida la venta de animales (precios, "se vende", cuotas, "entrego por X", razas a cambio de dinero).
- Prohibido el contenido falso o engañoso (estafas, datos incoherentes, pedir dinero por adelantado).
- Prohibidas las fotos inadecuadas (solo ves la URL y la etiqueta de la foto; si no puedes juzgarla, no la uses como motivo).
- Prohibidos los duplicados (compara con las otras publicaciones del mismo autor que se incluyen).
- Prohibido cualquier indicio de maltrato animal.
Responde SOLO un objeto JSON con esta forma exacta:
{"verdict":"APROBAR|RECHAZAR|REVISAR","risk":0.0,"reasons":["motivo breve en español"],"suggested_reason":"Venta de animales|Contenido falso|Fotos inadecuadas|Duplicado|Otro"}
risk va de 0 (seguro) a 1 (viola las reglas). Usa REVISAR si dudas. suggested_reason es null si el veredicto es APROBAR.`;

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method !== "POST") return json(405, { status: "error", message: "Usa POST" });

  const authHeader = req.headers.get("Authorization") ?? "";
  const url = Deno.env.get("SUPABASE_URL")!;
  const userClient = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: { user }, error: authError } = await userClient.auth.getUser(authHeader.replace(/^Bearer\s+/i, ""));
  if (authError || !user) return json(401, { status: "error", message: "JWT inválido o ausente" });

  const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: profile } = await admin.from("profiles").select("is_moderator").eq("id", user.id).maybeSingle();
  if (profile?.is_moderator !== true) return json(403, { status: "error", message: "Solo moderadores" });

  let publicationId: unknown;
  try {
    publicationId = (await req.json()).publication_id;
  } catch { /* cuerpo no JSON: se trata como falta de publication_id */ }
  if (typeof publicationId !== "string" || !publicationId) {
    return json(400, { status: "error", message: "Falta publication_id" });
  }

  const { data: pub, error: pubError } = await admin.from("publications")
    .select("id, author_id, category, title, species, breed, size, age, sex, city, neighborhood, description, image_url, photo_label, phone, place, services, status, created_at")
    .eq("id", publicationId).maybeSingle();
  if (pubError || !pub) return json(404, { status: "error", message: "Publicación no encontrada" });

  const apiKey = Deno.env.get("DEEPSEEK_API_KEY") ?? "";
  if (!apiKey) {
    return json(200, {
      status: "not_configured",
      message: "Configura DEEPSEEK_API_KEY con: supabase secrets set DEEPSEEK_API_KEY=...",
    });
  }
  const model = Deno.env.get("DEEPSEEK_MODEL") || "deepseek-chat";

  // Contexto para detectar duplicados: últimas publicaciones del mismo autor.
  const { data: others } = await admin.from("publications")
    .select("title, description, created_at").eq("author_id", pub.author_id).neq("id", pub.id)
    .order("created_at", { ascending: false }).limit(10);

  let content: string;
  try {
    const upstream = await fetch("https://api.deepseek.com/chat/completions", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${apiKey}` },
      signal: AbortSignal.timeout(20_000),
      body: JSON.stringify({
        model,
        temperature: 0,
        response_format: { type: "json_object" },
        messages: [
          { role: "system", content: SYSTEM_PROMPT },
          { role: "user", content: JSON.stringify({ publicacion: pub, otras_del_autor: others ?? [] }) },
        ],
      }),
    });
    if (!upstream.ok) {
      return json(502, { status: "error", message: `DeepSeek respondió ${upstream.status}: ${(await upstream.text()).slice(0, 300)}` });
    }
    content = (await upstream.json()).choices?.[0]?.message?.content ?? "";
  } catch (e) {
    const timeout = e instanceof DOMException && e.name === "TimeoutError";
    return json(502, { status: "error", message: timeout ? "DeepSeek no respondió en 20 s" : `Error llamando a DeepSeek: ${e}` });
  }

  let out: Record<string, unknown>;
  try {
    out = JSON.parse(content);
  } catch {
    return json(502, { status: "error", message: "DeepSeek devolvió JSON inválido" });
  }

  // Normaliza para que nunca viole los CHECK de la tabla.
  const verdict = VERDICTS.includes(out.verdict as string) ? out.verdict as string : "REVISAR";
  const risk = Math.min(1, Math.max(0, Number(out.risk) || 0));
  const reasons = Array.isArray(out.reasons) ? out.reasons.map(String).slice(0, 10) : [];
  const suggested = verdict === "APROBAR" ? null
    : REASONS.includes(out.suggested_reason as string) ? out.suggested_reason as string : "Otro";

  const { data: review, error: insertError } = await admin.from("publication_ai_reviews")
    .insert({ publication_id: pub.id, verdict, risk, reasons, suggested_reason: suggested, model })
    .select().single();
  if (insertError) return json(500, { status: "error", message: `No se guardó la revisión: ${insertError.message}` });

  return json(200, { status: "ok", review });
});
