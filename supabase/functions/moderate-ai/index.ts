// moderate-ai: segunda opinión de IA (DeepSeek) para la cola de moderación. Ver docs/MODERACION_IA.md.
// POST {"publication_id"} con el JWT de un moderador. Guarda el veredicto en publication_ai_reviews.
import { createClient } from "jsr:@supabase/supabase-js@2";
import { normalizeReview, SYSTEM_PROMPT, userMessage } from "./review.ts";

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
          { role: "user", content: userMessage(pub, others ?? []) },
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

  // Normaliza al esquema (CHECK de la tabla) y aplica la heurística local de venta; sigue siendo solo sugerencia.
  const { verdict, risk, reasons, suggested_reason } = normalizeReview(out, pub);

  const { data: review, error: insertError } = await admin.from("publication_ai_reviews")
    .insert({ publication_id: pub.id, verdict, risk, reasons, suggested_reason, model })
    .select().single();
  if (insertError) return json(500, { status: "error", message: `No se guardó la revisión: ${insertError.message}` });

  return json(200, { status: "ok", review });
});
