package com.idol.store.data

import android.content.Context
import android.provider.Settings
import android.os.Build
import com.idol.store.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

/**
 * رجیستری کاربران عادی (بدون سرور):
 *
 * - اثر انگشت دستگاه (fingerprint) از ANDROID_ID + مشخصات دستگاه ساخته می‌شود.
 * - توکنِ هر (ایمیل + دستگاه) به‌صورت قطعی (deterministic) از هش + راز برنامه ساخته می‌شود:
 *       token = SHA-256( "v1|" + emailHash + "|" + deviceHash + "|" + SECRET )
 *   پس توکن فقط روی همان دستگاهی که با آن ساخته شده معتبر است.
 * - درخواست ثبت‌نام به‌صورت یک «کد» (شامل هش ایمیل + هش دستگاه + هش توکن) در قالب Issue
 *   در ریپوی کاتالوگ باز می‌شود و یک GitHub Action آن را به users.json اضافه می‌کند.
 * - قوانین انحصاری (که Action و اپ هر دو رعایت می‌کنند):
 *       ۱) هر دستگاه فقط با یک ایمیل می‌تواند ثبت شود (دستگاهِ قبلاً ثبت‌شده با ایمیل دیگر رد می‌شود)
 *       ۲) هر ایمیل فقط روی یک دستگاه معتبر است
 * - ایمیل خام هیچ‌وقت جایی ذخیره/منتشر نمی‌شود؛ فقط هش آن (با راز برنامه).
 */
object UserRegistry {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class UserRecord(
        /** emailHash */
        val e: String,
        /** deviceHash */
        val d: String,
        /** tokenHash */
        val t: String,
        /** تاریخ ثبت */
        val at: String = "",
    )

    // ---------- اثر انگشت دستگاه ----------

    /** شناسه پایدار دستگاه برای این امضای اپ (ANDROID_ID + خانواده دستگاه) */
    fun deviceFingerprint(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Exception) {
            ""
        }
        val raw = "$androidId|${Build.FINGERPRINT.take(64)}|${Build.MODEL}"
        return sha256(raw)
    }

    private fun deviceHash(context: Context): String =
        sha256("d|" + deviceFingerprint(context) + "|" + BuildConfig.REG_SECRET)

    fun emailHash(email: String): String =
        sha256("e|" + email.trim().lowercase() + "|" + BuildConfig.REG_SECRET)

    /** توکن دستگاه‌محور برای (ایمیل، این دستگاه) */
    fun tokenFor(context: Context, email: String): String =
        sha256("t|" + emailHash(email) + "|" + deviceHash(context) + "|" + BuildConfig.REG_SECRET)

    private fun tokenHashOf(token: String): String = sha256("th|" + token)

    private fun sha256(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isValidEmail(email: String): Boolean =
        Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())

    // ---------- کد ثبت‌نام (برای Issue) ----------

    /** کدی که در Issue ثبت‌نام قرار می‌گیرد؛ ایمیل و مشخصات خام داخلش نیست، فقط هش */
    fun registrationCode(context: Context, email: String): String {
        val payload = json.encodeToString(
            RegistrationPayload.serializer(),
            RegistrationPayload(
                v = 1,
                e = emailHash(email),
                d = deviceHash(context),
                t = tokenHashOf(tokenFor(context, email)),
            ),
        )
        val b64 = android.util.Base64.encodeToString(
            payload.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP,
        )
        return "IDOLREG1.$b64"
    }

    @Serializable
    data class RegistrationPayload(val v: Int, val e: String, val d: String, val t: String)

    // ---------- خواندن رجیستری و اعتبارسنجی ----------

    sealed interface AuthResult {
        /** ثبت‌نام معتبر؛ ورود موفق */
        data class Success(val email: String) : AuthResult

        /** دستگاه قبلاً با ایمیل دیگری ثبت شده — ورود و ثبت‌نام جدید ممنوع */
        data object DeviceBoundToOtherEmail : AuthResult

        /** ایمیل قبلاً روی دستگاه دیگری ثبت شده */
        data object EmailBoundToOtherDevice : AuthResult

        /** توکن با مقدار محاسبه‌شده از دستگاه نمی‌خواند */
        data object InvalidToken : AuthResult

        /** این (ایمیل + دستگاه) در رجیستری نیست */
        data object NotRegistered : AuthResult
    }

    /** خواندن users.json از کاتالوگ (عمومی؛ raw) */
    suspend fun fetchRegistry(): List<UserRecord> = withContext(Dispatchers.IO) {
        val url = BuildConfig.USER_REGISTRY_URL
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (resp.code == 404) return@use emptyList()
            if (!resp.isSuccessful) throw GitHubApiException(resp.code)
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) emptyList()
            else json.decodeFromString<List<UserRecord>>(body)
        }
    }

    /**
     * بررسی ورود کاربر عادی:
     * ۱) توکن واردشده باید با توکن محاسبه‌شده از (ایمیل، این دستگاه) یکی باشد
     * ۲) باید رکورد (ایمیل، دستگاه، توکن) در رجیستری باشد
     * ۳) اگر دستگاه با ایمیل دیگری ثبت شده → رد قطعی
     */
    suspend fun checkLogin(context: Context, email: String, enteredToken: String): AuthResult =
        withContext(Dispatchers.IO) {
            val trimmed = email.trim().lowercase()
            if (!isValidEmail(trimmed)) return@withContext AuthResult.NotRegistered

            val expectedToken = tokenFor(context, trimmed)
            if (!enteredToken.trim().equals(expectedToken, ignoreCase = true)) {
                return@withContext AuthResult.InvalidToken
            }

            val registry = try {
                fetchRegistry()
            } catch (_: Exception) {
                return@withContext AuthResult.NotRegistered
            }

            val eHash = emailHash(trimmed)
            val dHash = deviceHash(context)
            val tHash = tokenHashOf(expectedToken)

            // قانون ۱: این دستگاه قبلاً با ایمیل دیگری ثبت شده؟
            if (registry.any { it.d == dHash && it.e != eHash }) {
                return@withContext AuthResult.DeviceBoundToOtherEmail
            }

            val record = registry.firstOrNull { it.e == eHash }
            return@withContext when {
                record == null -> AuthResult.NotRegistered
                record.d != dHash -> AuthResult.EmailBoundToOtherDevice
                record.t != tHash -> AuthResult.InvalidToken
                else -> AuthResult.Success(trimmed)
            }
        }

    /** آیا این دستگاه اصلاً (با هر ایمیلی) در رجیستری ثبت شده؟ (برای صفحه ثبت‌نام) */
    suspend fun isDeviceRegistered(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dHash = deviceHash(context)
            fetchRegistry().any { it.d == dHash }
        } catch (_: Exception) {
            false
        }
    }
}
