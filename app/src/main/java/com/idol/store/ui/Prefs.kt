package com.idol.store.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** تنظیمات تم + زبان (پایدار بین اجراها) */
object Prefs {

    private const val FILE = "ui_prefs"
    private const val KEY_THEME = "theme_mode"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun loadTheme(context: Context) {
        themeMode = try {
            ThemeMode.valueOf(prefs(context).getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setTheme(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }
}
