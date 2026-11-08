package com.desarrolloMovielexample.huella.e2e

import com.desarrolloMovielexample.huella.data.remote.RetrofitClient
import com.desarrolloMovielexample.huella.data.remote.SupabaseConfig
import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assume

/**
 * Entorno para pruebas contra un Supabase real (por defecto el proyecto hosted; ver docs/PRUEBAS.md).
 * Lee SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY (o SUPABASE_ANON_KEY) y SUPABASE_SERVICE_ROLE_KEY; si falta alguna, la prueba se salta.
 * La BD es compartida: cada prueba crea sus propios usuarios con [createUser] (correos e2e+<nanoTime>@huella.test)
 * y cada clase llama a [cleanup] en su @AfterClass, que borra los usuarios (y por cascada sus filas).
 */
object E2eEnv {
    const val PASSWORD = "E2e-Test-1234!"

    data class Resp(val code: Int, val body: String) {
        val ok get() = code in 200..299
        fun json(): JsonObject = JsonParser.parseString(body).asJsonObject
        fun rows(): List<JsonObject> = JsonParser.parseString(body).asJsonArray.map { it.asJsonObject }
    }

    data class TestUser(val id: String, val email: String, val name: String) {
        /** access_token de una sesión real (password grant). */
        val token: String by lazy { login(email, PASSWORD).first }
    }

    private fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
    private val http = OkHttpClient()
    private val createdUsers = mutableListOf<String>()

    val url: String get() = env("SUPABASE_URL")!!.trimEnd('/')
    val key: String get() = (env("SUPABASE_PUBLISHABLE_KEY") ?: env("SUPABASE_ANON_KEY"))!!
    private val serviceKey: String get() = env("SUPABASE_SERVICE_ROLE_KEY")!!

    /** Salta la prueba si no hay Supabase configurado; si lo hay, apunta la app a él. */
    fun assumeConfigured() {
        val missing = buildList {
            if (env("SUPABASE_URL") == null) add("SUPABASE_URL")
            if (env("SUPABASE_PUBLISHABLE_KEY") == null && env("SUPABASE_ANON_KEY") == null) add("SUPABASE_PUBLISHABLE_KEY")
            if (env("SUPABASE_SERVICE_ROLE_KEY") == null) add("SUPABASE_SERVICE_ROLE_KEY")
        }
        Assume.assumeTrue("Faltan variables $missing: ver docs/PRUEBAS.md", missing.isEmpty())
        SupabaseConfig.override(url, key)
    }

    /** SupabaseApiService real (el mismo RetrofitClient de la app). */
    fun api(): SupabaseApiService {
        assumeConfigured()
        return RetrofitClient.supabaseApi
    }

    fun uniqueEmail() = "e2e+${System.nanoTime()}@huella.test"

    /** Petición cruda a Supabase con la publishable key; `token` va como Bearer si se pasa. */
    fun rest(
        path: String,
        method: String = "GET",
        body: String? = null,
        token: String? = null,
        apikey: String = key,
        contentType: String = "application/json",
        headers: Map<String, String> = emptyMap(),
    ): Resp = send(path, method, body?.toRequestBody(contentType.toMediaType()), token, apikey, headers)

    /** POST binario (subidas a storage) con la publishable key y el token del usuario si se pasa. */
    fun restBytes(path: String, bytes: ByteArray, contentType: String, token: String? = null): Resp =
        send(path, "POST", bytes.toRequestBody(contentType.toMediaType()), token, key, emptyMap())

    private fun send(path: String, method: String, body: RequestBody?, token: String?, apikey: String, headers: Map<String, String>): Resp {
        assumeConfigured()
        val req = Request.Builder()
            .url("$url/${path.trimStart('/')}")
            .method(method, body)
            .header("apikey", apikey)
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        return http.newCall(req).execute().use { Resp(it.code, it.body?.string().orEmpty()) }
    }

    /** POST rest/v1/rpc/<fn> con el token del usuario (o anónimo si es null). */
    fun rpc(fn: String, body: String = "{}", token: String? = null): Resp = rest("rest/v1/rpc/$fn", "POST", body, token)

    /** Sesión de un usuario: devuelve (access_token, user id). */
    fun login(email: String, password: String = PASSWORD): Pair<String, String> {
        val r = rest("auth/v1/token?grant_type=password", "POST", """{"email":"$email","password":"$password"}""")
        check(r.ok) { "Login de $email falló (${r.code}): ${r.body}" }
        val json = r.json()
        return json.get("access_token").asString to json.getAsJsonObject("user").get("id").asString
    }

    /** Como service_role (salta RLS) para preparar datos y verificar el estado real de la BD. */
    fun admin(path: String, method: String = "GET", body: String? = null, contentType: String = "application/json"): Resp =
        rest(path, method, body, token = serviceKey, apikey = serviceKey, contentType = contentType,
            headers = mapOf("Prefer" to "return=representation"))

    /** Usuario confirmado creado con la Admin API (sin correo); se borra en [cleanup]. */
    fun createUser(name: String = "E2E", city: String = "Medellín"): TestUser {
        val email = uniqueEmail()
        val r = admin(
            "auth/v1/admin/users", "POST",
            """{"email":"$email","password":"$PASSWORD","email_confirm":true,"user_metadata":{"name":"$name","city":"$city"}}""",
        )
        check(r.ok) { "Admin API no creó $email (${r.code}): ${r.body}" }
        return TestUser(track(r.json().get("id").asString), email, name)
    }

    /** Registra un usuario creado por otra vía (p. ej. signup público) para borrarlo en [cleanup]. */
    fun track(userId: String): String = userId.also { synchronized(createdUsers) { createdUsers += it } }

    /** Marca al usuario de prueba como moderador (service_role: RLS y triggers de perfil no aplican). */
    fun makeModerator(user: TestUser) {
        val r = admin("rest/v1/profiles?id=eq.${user.id}", "PATCH", """{"is_moderator":true}""")
        check(r.ok && r.rows().size == 1) { "No se pudo hacer moderador a ${user.email} (${r.code}): ${r.body}" }
    }

    /** Borra los usuarios creados por la clase (DELETE admin/users/<id>; profiles y demás caen por ON DELETE CASCADE). */
    fun cleanup() {
        val ids = synchronized(createdUsers) { createdUsers.toList().also { createdUsers.clear() } }
        val failed = ids.map { it to admin("auth/v1/admin/users/$it", "DELETE") }.filterNot { it.second.ok }
        check(failed.isEmpty()) { "No se borraron usuarios e2e: ${failed.map { "${it.first} ${it.second.code} ${it.second.body}" }}" }
    }
}
