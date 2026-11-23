package com.idol.store.data

import android.content.Context

/**
 * تنظیمات اعلان آپدیت هر اپ + آخرین تگ دیده‌شده برای تشخیص بروزرسانی جدید.
 */
object UpdatePrefs {

    private const val FILE = "update_prefs"
    private const val KEY_NOTIFYING = "notifying_apps"
    private const val KEY_LAST_TAG = "last_tag_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isNotifying(context: Context, appId: String): Boolean =
        prefs(context).getStringSet(KEY_NOTIFYING, emptySet())?.contains(appId) == true

    /** روشن کردن اعلان: تگ فعلی به‌عنوان مبدأ ثبت می‌شود تا بلافاصله اعلان نیاید */
    fun setNotifying(context: Context, appId: String, enabled: Boolean, currentTag: String?) {
        val p = prefs(context)
        val set = p.getStringSet(KEY_NOTIFYING, emptySet()).orEmpty().toMutableSet()
        if (enabled) {
            set.add(appId)
            currentTag?.let { p.edit().putString(KEY_LAST_TAG + appId, it).apply() }
        } else {
            set.remove(appId)
        }
        p.edit().putStringSet(KEY_NOTIFYING, set).apply()
    }

    fun notifyingIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_NOTIFYING, emptySet()).orEmpty()

    fun lastTag(context: Context, appId: String): String? =
        prefs(context).getString(KEY_LAST_TAG + appId, null)

    fun setLastTag(context: Context, appId: String, tag: String) {
        prefs(context).edit().putString(KEY_LAST_TAG + appId, tag).apply()
    }
}
