package com.idol.store.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ساختار فایل کاتالوگ (apps.json).
 * برای هر اپ فقط آدرس ریپو یا صفحه ریلیز گیت‌هاب را می‌دهیم؛
 * خود اپ آخرین ریلیز پایدار و فایل APK یونیورسال را از GitHub API پیدا می‌کند.
 */
@Serializable
data class Catalog(
    val apps: List<StoreApp> = emptyList(),
    /** نام‌های کاربری گیت‌هاب مجاز به پنل مدیریت */
    val admins: List<String> = emptyList(),
    /** دسته‌بندی‌ها (ادمین در پنل می‌سازد) */
    val categories: List<String> = emptyList(),
    val updatedAt: String? = null,
)

@Serializable
data class StoreApp(
    val id: String,
    val name: String,
    val developer: String? = null,
    val category: String? = null,
    val description: String? = null,
    /** توضیح انگلیسی برای حالت زبان انگلیسی (اختیاری) */
    val descriptionEn: String? = null,
    val iconUrl: String? = null,
    val packageName: String? = null,
    /** "owner/repo" یا آدرس کامل ریپو/ریلیز گیت‌هاب */
    val github: String,
    // ---- اطلاعات تماس (اختیاری؛ ادمین وارد می‌کند) ----
    val website: String? = null,
    val email: String? = null,
    val telegram: String? = null,
)

// ---------- DTO های GitHub API ----------

@Serializable
data class GhRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val body: String? = null,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
data class GhAsset(
    val name: String,
    val size: Long = 0,
    @SerialName("browser_download_url") val downloadUrl: String,
)

@Serializable
data class GhUser(
    val login: String,
)

@Serializable
data class GhOwner(
    val login: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class GhRepoInfo(
    val name: String? = null,
    val description: String? = null,
    val homepage: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val owner: GhOwner? = null,
)

/** کلید یکتا برای پیگیری وضعیت دانلود هر ریلیز از هر اپ */
fun downloadKey(appId: String, tag: String): String = "$appId|$tag"

/**
 * تعیین دسترسی مدیریت:
 * - اگر لیست admins پر باشد: فقط اعضای آن
 * - اگر خالی باشد (راه‌اندازی اولیه): فقط مالک ریپوی کاتالوگ
 */
object AdminAccess {
    fun catalogOwner(catalogRepo: String): String? {
        if (catalogRepo.isBlank() || catalogRepo.contains("USERNAME")) return null
        return catalogRepo.trim('/').substringBefore('/')
    }

    fun isAllowed(catalog: Catalog?, login: String?, catalogRepo: String): Boolean {
        if (login.isNullOrBlank()) return false
        val admins = catalog?.admins.orEmpty()
        return if (admins.isNotEmpty()) admins.any { it.equals(login, ignoreCase = true) }
        else catalogOwner(catalogRepo)?.equals(login, ignoreCase = true) == true
    }
}

fun humanSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024)
    else if (mb >= 1) String.format("%.1f MB", mb)
    else String.format("%.0f KB", bytes / 1024.0)
}

fun shortDate(iso: String?): String = if (!iso.isNullOrBlank() && iso.length >= 10) iso.substring(0, 10) else "—"
