package com.idol.store.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** گزارش کامل امنیتی منتشرشده در ریپوی کاتالوگ (Idol Security Scanner v2) */
@Serializable
data class SecurityReport(
    val results: List<SecurityResult> = emptyList(),
)

/** نتیجه اسکن یک اپ — فیلدهای سازگار v1 + فیلدهای جدید v2 */
@Serializable
data class SecurityResult(
    val id: String,
    /** مستعار سازگار: no_detection / warning / dangerous / incomplete / not_scanned / scan_error */
    val status: String,
    @SerialName("release_version") val releaseVersion: String? = null,
    val release: SecurityReleaseRef? = null,
    @SerialName("overall_status") val overallStatus: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("scan_timestamp") val scanTimestamp: String? = null,
    @SerialName("scanned_assets") val scannedAssets: List<SecurityAsset> = emptyList(),
    val assets: List<SecurityAsset> = emptyList(),
)

@Serializable
data class SecurityReleaseRef(
    val tag: String? = null,
)

@Serializable
data class SecurityAsset(
    val name: String,
    val sha256: String? = null,
    val status: String? = null,
)

/** وضعیت نهایی گیت امنیتی برای نمایش در UI */
enum class SecurityGateStatus { LOADING, SAFE, WARNING, INCOMPLETE, NOT_SCANNED, DANGEROUS }

/** نسخه بررسی‌شده در گزارش — یا از فیلد v1 یا v2 */
val SecurityResult.scannedTag: String?
    get() = release?.tag ?: releaseVersion

/** گیت امنیتی: هر دانلود باید از این گیت عبور کند */
fun SecurityResult.gateStatusFor(tag: String?): SecurityGateStatus = when {
    // گزارش متعلق به نسخه دیگری است → برای این نسخه معتبر نیست
    scannedTag != null && tag != null && scannedTag != tag -> SecurityGateStatus.NOT_SCANNED
    status == "dangerous" || overallStatus == "DANGEROUS" -> SecurityGateStatus.DANGEROUS
    status == "warning" || overallStatus == "WARNING" || overallStatus == "LOW_RISK" -> SecurityGateStatus.WARNING
    status == "incomplete" || overallStatus == "INCOMPLETE" -> SecurityGateStatus.INCOMPLETE
    status == "scan_error" || overallStatus == "SCAN_ERROR" -> SecurityGateStatus.INCOMPLETE
    status == "no_detection" || overallStatus == "SAFE" -> SecurityGateStatus.SAFE
    else -> SecurityGateStatus.NOT_SCANNED
}

/**
 * خواندن گزارش عمومی اسکن از ریپوی کاتالوگ.
 * نبود یا در دسترس نبودن گزارش هرگز «سالم» تلقی نمی‌شود.
 */
object SecurityRepository {
    private const val REPORT_URL =
        "https://raw.githubusercontent.com/Alvandcode/idol-store-catalog/main/security-results.json"

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<SecurityResult> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(REPORT_URL)
            .header("Cache-Control", "no-cache")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext emptyList()
                json.decodeFromString<SecurityReport>(body).results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
