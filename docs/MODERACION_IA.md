# Moderación asistida por IA (DeepSeek)

La Edge Function `moderate-ai` da una segunda opinión sobre una publicación de la cola de moderación.
No modera sola: guarda un veredicto en `public.publication_ai_reviews` y el moderador decide con `moderate_publication`.

## Estado en el hosted

- Función desplegada en el proyecto hosted, **sin** `DEEPSEEK_API_KEY`: responde `200 {"status":"not_configured"}`.
- Tabla `publication_ai_reviews` (migración `20261109150000_revisiones_ia.sql`): solo moderadores la leen; escribe la función con service_role.
- Para activarla, alguien del equipo solo tiene que poner la key (ver abajo).

## Activar

```bash
supabase secrets set DEEPSEEK_API_KEY=sk-... --project-ref <ref>
# opcional, por defecto deepseek-chat
supabase secrets set DEEPSEEK_MODEL=deepseek-chat --project-ref <ref>
```

La key se crea en https://platform.deepseek.com (API keys) y la cuenta necesita saldo prepago.
No hace falta redeplegar: los secrets se leen en cada invocación. Para volver al modo `not_configured`:
`supabase secrets unset DEEPSEEK_API_KEY --project-ref <ref>`.

Desplegar de nuevo tras cambiar el código (sin Docker):

```bash
supabase functions deploy moderate-ai --use-api --project-ref <ref>
```

Local: copia `supabase/functions/.env.example` a `supabase/functions/.env` (ignorado por git) y
`supabase functions serve moderate-ai --env-file supabase/functions/.env`.

## Contrato HTTP

`POST {SUPABASE_URL}/functions/v1/moderate-ai`, headers `apikey: <publishable key>` y
`Authorization: Bearer <access_token del moderador>`, cuerpo `{"publication_id":"<uuid>"}`.

| Caso | Código | Cuerpo |
|---|---|---|
| Sin JWT o JWT inválido | 401 | `{"status":"error","message":...}` |
| Usuario sin `is_moderator` | 403 | `{"status":"error","message":"Solo moderadores"}` |
| Falta `publication_id` / no existe | 400 / 404 | `{"status":"error",...}` |
| Sin `DEEPSEEK_API_KEY` | 200 | `{"status":"not_configured","message":"Configura DEEPSEEK_API_KEY con: ..."}` |
| DeepSeek falla, tarda más de 20 s o devuelve JSON inválido | 502 | `{"status":"error","message":...}` |
| OK | 200 | `{"status":"ok","review":{id,publication_id,verdict,risk,reasons,suggested_reason,model,created_at}}` |

`verdict` ∈ `APROBAR | RECHAZAR | REVISAR`, `risk` ∈ [0,1], `suggested_reason` ∈ `Venta de animales | Contenido falso |
Fotos inadecuadas | Duplicado | Otro` (null si aprueba). La función normaliza la respuesta del modelo para que nunca viole los CHECK.

Qué evalúa el prompt: venta de animales, contenido falso o estafas, fotos inadecuadas, duplicados y maltrato.
Para detectar duplicados envía también las últimas 10 publicaciones del mismo autor.
Limitación: `deepseek-chat` no ve imágenes; solo recibe la URL y la etiqueta de la foto, así que "Fotos inadecuadas"
sigue siendo tarea del moderador humano.

## Uso desde la app (F4)

1. En la tarjeta de la cola de moderación, botón "Analizar con IA" → POST anterior con el token de la sesión.
2. `not_configured` → mensaje "La moderación con IA no está configurada" (no es un error).
3. `ok` → mostrar veredicto, riesgo y motivos; si es `RECHAZAR`, preseleccionar `suggested_reason` en el diálogo de rechazo.
4. Revisiones previas: `GET rest/v1/publication_ai_reviews?publication_id=eq.<id>&order=created_at.desc&limit=1`
   (RLS: solo moderadores).

## Costo aproximado

Cada análisis envía ~1.000–2.000 tokens de entrada y recibe ~150 de salida. Con los precios publicados de
`deepseek-chat` (del orden de 0,3 USD por millón de tokens de entrada y ~1 USD por millón de salida; revisa
https://api-docs.deepseek.com/quick_start/pricing) sale por **menos de 0,001 USD por publicación**:
1.000 análisis ≈ 1 USD. Solo se cobra cuando un moderador pulsa el botón.

## Pruebas

- `app/src/test/.../e2e/ModerateAiE2eTest.kt`: 401 sin JWT, 403 no moderador, 200 `not_configured` con moderador
  (se salta si no hay `SUPABASE_URL`; ver docs/PRUEBAS.md).
- Camino 502: `supabase secrets set DEEPSEEK_API_KEY=sk-falsa`, llamar como moderador (DeepSeek responde 401 → la
  función devuelve 502) y luego `supabase secrets unset DEEPSEEK_API_KEY`.
