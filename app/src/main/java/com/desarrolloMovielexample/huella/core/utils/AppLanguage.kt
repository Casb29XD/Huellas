package com.desarrolloMovielexample.huella.core.utils

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Persists the app language while allowing SYSTEM to follow the device language. */
object AppLanguage {
    const val SYSTEM = "system"
    const val SPANISH = "es"
    const val ENGLISH = "en"

    private const val PREFERENCES = "huella_language"
    private const val LANGUAGE_KEY = "language"

    fun selected(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val systemSelection = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!systemSelection.isEmpty) {
                val language = systemSelection[0].language
                if (language in setOf(SPANISH, ENGLISH)) return language
            }
        }
        return context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, SYSTEM)
            ?.takeIf { it in setOf(SYSTEM, SPANISH, ENGLISH) }
            ?: SYSTEM
    }

    fun set(context: Context, language: String) {
        require(language in setOf(SYSTEM, SPANISH, ENGLISH))
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, language)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = if (language == SYSTEM) LocaleList.getEmptyLocaleList()
            else LocaleList.forLanguageTags(language)
            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
        }
    }

    /** Applies the saved locale before Activity resources are created on Android 10–12. */
    fun wrapBaseContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Locale.setDefault(base.resources.configuration.locales[0])
            return base
        }
        val language = selected(base)
        if (language == SYSTEM) {
            Locale.setDefault(base.resources.configuration.locales[0])
            return base
        }

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    fun findActivity(context: Context): android.app.Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is android.app.Activity) return current
            current = current.baseContext
        }
        return current as? android.app.Activity
    }
}
