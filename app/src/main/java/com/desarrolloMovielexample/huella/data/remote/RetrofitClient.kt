package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.BuildConfig
import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Retrofit client for Supabase Auth and PostgREST. */
object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("apikey")
        redactHeader("Authorization")
    }

    private val supabaseClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("apikey", SupabaseConfig.publishableKey)
                .header("Accept", "application/json")
            SupabaseSession.accessToken?.let { builder.header("Authorization", "Bearer $it") }
                ?: builder.removeHeader("Authorization")
            val request = builder.build()
            chain.proceed(request)
        }
        // Ítem 40: en release no se registran URLs (ids de usuario, filtros) en logcat.
        .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor) }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cached: Pair<String, SupabaseApiService>? = null

    /** Lanza IllegalStateException si falta app/.env, en vez de crashear al construir Retrofit con URL vacía. */
    val supabaseApi: SupabaseApiService
        get() {
            SupabaseConfig.requireConfigured()
            val url = SupabaseConfig.url
            cached?.takeIf { it.first == url }?.let { return it.second }
            return Retrofit.Builder()
                .baseUrl("$url/")
                .client(supabaseClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseApiService::class.java)
                .also { cached = url to it }
        }
}
