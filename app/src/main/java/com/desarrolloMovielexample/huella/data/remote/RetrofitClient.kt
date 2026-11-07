package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.BuildConfig
import com.desarrolloMovielexample.huella.data.remote.api.PokeApiService
import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Retrofit clients for Supabase Auth/PostgREST and the public PokéAPI catalog. */
object RetrofitClient {
    private const val POKE_API_BASE_URL = "https://pokeapi.co/api/v2/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Never log bodies: auth responses contain access and refresh tokens.
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("apikey")
        redactHeader("Authorization")
    }

    private val supabaseClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Accept", "application/json")
            SupabaseSession.accessToken?.let { builder.header("Authorization", "Bearer $it") }
                ?: builder.removeHeader("Authorization")
            val request = builder.build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val supabaseRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SUPABASE_URL.trimEnd('/') + "/")
        .client(supabaseClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val supabaseApi: SupabaseApiService =
        supabaseRetrofit.create(SupabaseApiService::class.java)

    private val pokeClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val pokeRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(POKE_API_BASE_URL)
        .client(pokeClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val pokeApiService: PokeApiService = pokeRetrofit.create(PokeApiService::class.java)
}
