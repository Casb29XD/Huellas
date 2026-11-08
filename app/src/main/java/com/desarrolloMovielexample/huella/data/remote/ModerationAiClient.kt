package com.desarrolloMovielexample.huella.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Cliente de la Edge Function moderate-ai, con timeout de lectura largo (el modelo puede tardar).
 * ponytail: sin refresh de token en 401; añadir el authenticator de RetrofitClient si molesta.
 */
object ModerationAiClient {
    private interface Api {
        @POST("functions/v1/moderate-ai")
        suspend fun moderateAi(@Body body: JsonObject): Response<JsonObject>
    }

    private val http: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("apikey", SupabaseConfig.publishableKey)
                .header("Accept", "application/json")
            SupabaseSession.accessToken?.let { builder.header("Authorization", "Bearer $it") }
            chain.proceed(builder.build())
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // el modelo puede tardar
        .build()

    @Volatile
    private var cached: Pair<String, Api>? = null

    private val api: Api
        get() {
            SupabaseConfig.requireConfigured()
            val url = SupabaseConfig.url
            cached?.takeIf { it.first == url }?.let { return it.second }
            return Retrofit.Builder()
                .baseUrl("$url/")
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
                .also { cached = url to it }
        }

    /** Edge Function moderate-ai. Devuelve null si responde status=not_configured; lanza si falla. */
    suspend fun analyze(publicationId: String): AiReview? {
        val response = api.moderateAi(JsonObject().apply { addProperty("publication_id", publicationId) })
        val json = response.body()
            ?: runCatching { JsonParser.parseString(response.errorBody()?.string()).asJsonObject }.getOrNull()
        if (json?.str("status") == "not_configured") return null
        val review = json?.get("review")?.takeIf { it.isJsonObject }?.asJsonObject
        if (!response.isSuccessful || review == null) {
            error(json?.str("error") ?: json?.str("message") ?: "No pudimos analizar con IA (${response.code()})")
        }
        return AiReview(
            verdict = review.str("verdict").orEmpty(),
            risk = review.get("risk")?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0,
            reasons = review.get("reasons")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeUnless { e -> e.isJsonNull }?.asString }.orEmpty(),
            suggestedReason = review.str("suggested_reason"),
        )
    }

    private fun JsonObject.str(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString
}

/** Sugerencia de la IA: verdict APROBAR | RECHAZAR | REVISAR, risk 0..1. Solo orienta al moderador. */
data class AiReview(
    val verdict: String,
    val risk: Double,
    val reasons: List<String>,
    val suggestedReason: String?,
)
