package com.idol.store.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * سیستم مدیریت ادمین‌ها — منبع حقیقت: فایل admins.json در ریپوی کاتالوگ.
 * مالک (سازنده) همه دسترسی‌ها را همیشه دارد؛ ادمین‌ها فقط با اجازه صریح مالک.
 */
@Serializable
data class AdminEntry(
    val login: String,
    /** دسترسی‌ها: manage_apps / manage_admins / update_store — خالی = فقط ورود بدون هیچ دسترسی */
    val permissions: List<String> = emptyList(),
    @SerialName("added_at") val addedAt: String? = null,
)

@Serializable
data class AdminRegistry(
    val schemaVersion: Int = 1,
    /** مالک سازنده — همیشه همه دسترسی‌ها، بدون نیاز به کد */
    val owner: String = "",
    val admins: List<AdminEntry> = emptyList(),
)

/** کلیدهای دسترسی */
object AdminPermissions {
    /** افزودن، ویرایش و حذف اپلیکیشن‌های استور */
    const val MANAGE_APPS = "manage_apps"
    /** افزودن/حذف ادمین و تعیین دسترسی (فقط مالک) */
    const val MANAGE_ADMINS = "manage_admins"
    /** انتشار نسخه جدید خود اپ استور (فقط مالک) */
    const val UPDATE_STORE = "update_store"
    val ALL = listOf(MANAGE_APPS, MANAGE_ADMINS, UPDATE_STORE)
}

/**
 * بارگذاری و ذخیره‌ی admins.json از ریپوی کاتالوگ.
 * خواندن عمومی (بدون توکن) — نوشتن فقط مالک.
 */
object AdminRegistryStore {
    const val FILE = "admins.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** null = فایل هنوز ساخته نشده */
    suspend fun load(catalogRepo: String): AdminRegistry? {
        val (sha, content) = GitHubService.readCatalogFile(catalogRepo, FILE, null)
        if (content.isNullOrBlank()) return null
        return try { json.decodeFromString<AdminRegistry>(content) } catch (_: Exception) { null }
    }

    suspend fun save(catalogRepo: String, registry: AdminRegistry, token: String): String =
        GitHubService.commitCatalogFile(
            repo = catalogRepo,
            path = FILE,
            message = "chore: update admin registry ($FILE) [skip ci]",
            content = json.encodeToString(AdminRegistry.serializer(), registry),
            token = token,
        )
}

/**
 * کد ورود روزانه برای ادمین‌ها:
 * فرمول هر دو دستگاه (مالک و ادمین) یکسان محاسبه می‌کند و هر شب‌نامه عوض می‌شود.
 * مالک کد را می‌بیند و به ادمین می‌دهد؛ بدون این کد ادمین وارد نمی‌شود.
 */
object AdminCode {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun today(): String = fmt.format(Date())

    /** کد ۶ رقمی برای ورود امروز */
    fun forLogin(login: String, secret: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${secret.trim()}|${login.trim().lowercase()}|${today()}".toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { String.format("%02x", it) }
        val num = (hex.take(12).toLong(16) and 0x7FFFFFFFL) % 1_000_000L
        return String.format(Locale.US, "%06d", num)
    }
}
