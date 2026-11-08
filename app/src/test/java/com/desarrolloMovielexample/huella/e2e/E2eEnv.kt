package com.desarrolloMovielexample.huella.e2e

import com.desarrolloMovielexample.huella.data.remote.RetrofitClient
import com.desarrolloMovielexample.huella.data.remote.SupabaseConfig
import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assume

/**
 * Entorno para pruebas contra un Supabase real (local: `supabase start`).
 * Lee SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY y SUPABASE_SERVICE_ROLE_KEY; si falta alguna, la prueba se salta.
 * Usuarios semilla (supabase/seed.sql).
 */
object E2eEnv {
    val LAURA = "laura@ejemplo.com" to "Laura1234!"
    val CARLOS = "carlos@ejemplo.com" to "Carlos1234!"

    data class Resp(val code: Int, val body: String) {
        val ok get() = code in 200..299
        fun json(): JsonObject = JsonParser.parseString(body).asJsonObject
        fun rows(): List<JsonObject> = JsonParser.parseString(body).asJsonArray.map { it.asJsonObject }
    }

    private fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
    private val http = OkHttpClient()

    val url: String get() = env("SUPABASE_URL")!!.trimEnd('/')
    val key: String get() = env("SUPABASE_PUBLISHABLE_KEY")!!
    private val serviceKey: String get() = env("SUPABASE_SERVICE_ROLE_KEY")!!

    /** Salta la prueba si no hay Supabase configurado; si lo hay, apunta la app a él. */
    fun assumeConfigured() {
        val vars = listOf("SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY", "SUPABASE_SERVICE_ROLE_KEY")
        Assume.assumeTrue("Faltan variables ${vars.filter { env(it) == null }}: corre scripts/test-all.sh", vars.all { env(it) != null })
        SupabaseConfig.override(url, key)
    }

    /** SupabaseApiService real (el mismo RetrofitClient de la app). */
    fun api(): SupabaseApiService {
        assumeConfigured()
        return RetrofitClient.supabaseApi
    }

    /** Petición cruda a Supabase con la publishable key; `token` va como Bearer si se pasa. */
    fun rest(
        path: String,
        method: String = "GET",
        body: String? = null,
        token: String? = null,
        apikey: String = key,
        contentType: String = "application/json",
        headers: Map<String, String> = emptyMap(),
    ): Resp {
        assumeConfigured()
        val req = Request.Builder()
            .url("$url/${path.trimStart('/')}")
            .method(method, body?.toRequestBody(contentType.toMediaType()))
            .header("apikey", apikey)
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        return http.newCall(req).execute().use { Resp(it.code, it.body?.string().orEmpty()) }
    }

    /** Sesión de un usuario: devuelve (access_token, user id). */
    fun login(email: String, password: String): Pair<String, String> {
        val r = rest("auth/v1/token?grant_type=password", "POST", """{"email":"$email","password":"$password"}""")
        check(r.ok) { "Login de $email falló (${r.code}): ${r.body}" }
        val json = r.json()
        return json.get("access_token").asString to json.getAsJsonObject("user").get("id").asString
    }

    fun loginToken(email: String, password: String): String = login(email, password).first

    /** Consulta/escritura como service_role (salta RLS) para verificar el estado real de la BD. */
    fun adminQuery(path: String, method: String = "GET", body: String? = null): Resp =
        rest(path, method, body, token = serviceKey, apikey = serviceKey, headers = mapOf("Prefer" to "return=representation"))
}
