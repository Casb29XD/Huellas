package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.BuildConfig

/** URL y publishable key de Supabase: vienen de app/.env (BuildConfig) y las pruebas pueden sobreescribirlas. */
object SupabaseConfig {
    @Volatile
    var url: String = BuildConfig.SUPABASE_URL.trimEnd('/')
        private set

    @Volatile
    var publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        private set

    val isConfigured: Boolean get() = url.isNotBlank() && publishableKey.isNotBlank()

    fun override(url: String, key: String) {
        this.url = url.trimEnd('/')
        publishableKey = key
    }

    fun requireConfigured() {
        check(isConfigured) { "Configura SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY en app/.env" }
    }
}
