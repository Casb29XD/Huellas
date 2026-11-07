package com.desarrolloMovielexample.huella.data.remote

import android.content.Context

/** Holds the current Supabase access token and persists it across app launches. */
object SupabaseSession {
    private const val PREFS = "supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val USER_ID = "user_id"
    private const val REFRESH_TOKEN = "refresh_token"

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var userId: String? = null
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = prefs.getString(ACCESS_TOKEN, null)
        userId = prefs.getString(USER_ID, null)
        refreshToken = prefs.getString(REFRESH_TOKEN, null)
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

    fun clear(context: Context? = null) {
        accessToken = null
        userId = null
        refreshToken = null
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.clear()
            ?.apply()
    }
}
