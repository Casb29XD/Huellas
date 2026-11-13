package com.desarrolloMovielexample.huella.data.remote

import android.content.Context

/** Holds the current Supabase access token and persists it across app launches. */
object SupabaseSession {
    private const val PREFS = "supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val USER_ID = "user_id"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val CODE_VERIFIER = "pkce_code_verifier"
    private const val PASSWORD_RECOVERY_PENDING = "password_recovery_pending"

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var userId: String? = null
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    /** PKCE: verifier del último registro/recuperación; el ?code= del correo no sirve sin él. */
    @Volatile
    var codeVerifier: String? = null
        private set

    @Volatile
    var passwordRecoveryPending: Boolean = false
        private set

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = prefs.getString(ACCESS_TOKEN, null)
        userId = prefs.getString(USER_ID, null)
        refreshToken = prefs.getString(REFRESH_TOKEN, null)
        codeVerifier = prefs.getString(CODE_VERIFIER, null)
        passwordRecoveryPending = prefs.getBoolean(PASSWORD_RECOVERY_PENDING, false)
    }

    fun saveCodeVerifier(context: Context, verifier: String?) {
        codeVerifier = verifier
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CODE_VERIFIER, verifier)
            .apply()
    }

    fun setPasswordRecoveryPending(context: Context, pending: Boolean) {
        passwordRecoveryPending = pending
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PASSWORD_RECOVERY_PENDING, pending)
            .apply()
    }

    fun save(context: Context, token: String, id: String, refresh: String? = refreshToken) {
        accessToken = token
        userId = id
        refreshToken = refresh
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ACCESS_TOKEN, token)
            .putString(USER_ID, id)
            .putString(REFRESH_TOKEN, refresh)
            .apply()
    }

    /** Cierra la sesión; conserva el code verifier para que un enlace de correo pendiente siga sirviendo. */
    fun clear(context: Context? = null) {
        accessToken = null
        userId = null
        refreshToken = null
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(ACCESS_TOKEN)
            ?.remove(USER_ID)
            ?.remove(REFRESH_TOKEN)
            ?.apply()
    }
}
