package com.idol.store.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.idol.store.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** خطای GitHub API با کد قابل نگاشت به پیام محلی */
class GitHubApiException(
    val code: Int,
    val repo: String? = null,
) : IOException(code.toString())

/** خطاهای سطح استور برای پیام‌های محلی */
enum class StoreErrorKind { INVALID_REPO_URL, NO_STABLE_RELEASE, NO_UNIVERSAL_APK }

class StoreException(val kind: StoreErrorKind, val detail: String? = null) : Exception()

@Serializable
data class DeviceCodeResponse(
    @kotlinx.serialization.SerialName("device_code") val deviceCode: String,
    @kotlinx.serialization.SerialName("user_code") val userCode: String,
    @kotlinx.serialization.SerialName("verification_uri") val verificationUri: String,
    val interval: Int = 5,
)

@Serializable
data class AccessTokenResponse(
    @kotlinx.serialization.SerialName("access_token") val accessToken: String? = null,
    val error: String? = null,
    @kotlinx.serialization.SerialName("error_description") val errorDescription: String? = null,
)

data class DeviceFlowStart(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val interval: Int,
)

sealed interface ExchangeResult {
    data class Success(val token: String) : ExchangeResult
    data object Pending : ExchangeResult
    data object SlowDown : ExchangeResult
    data class Error(val message: String) : ExchangeResult
}

/**
 * مدیریت ورود با حساب گیت‌هاب:
 * 1) Device Flow (کد دستگاه) — نیاز به GITHUB_CLIENT_ID در build.gradle دارد
 * 2) توکن دسترسی شخصی (PAT)
 * توکن به‌صورت رمزنگاری‌شده ذخیره می‌شود.
 */
object AuthManager {

    private const val FILE = "idol_auth"
    private const val KEY = "gh_token"
    private const val KEY_LOGIN = "gh_login"
    private const val SCOPE = "public_repo read:user"

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    // ---------- ذخیره توکن ----------

    private fun prefs(context: Context): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun hasToken(context: Context): Boolean = !token(context).isNullOrBlank()

    fun token(context: Context): String? = prefs(context).getString(KEY, null)

    /** نام کاربری گیت‌هاب صاحب توکن (برای تشخیص ادمین) */
    fun login(context: Context): String? = prefs(context).getString(KEY_LOGIN, null)

    fun saveSession(context: Context, token: String, login: String?) {
        prefs(context).edit().putString(KEY, token).putString(KEY_LOGIN, login).apply()
    }

    fun saveToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY, token).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    // ---------- Device Flow ----------

    suspend fun startDeviceFlow(): DeviceFlowStart = withContext(Dispatchers.IO) {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) {
            throw ClientIdMissingException()
        }
        val form = FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", SCOPE)
            .build()
        val request = Request.Builder()
            .url("https://github.com/login/device/code")
            .header("Accept", "application/json")
            .post(form)
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw GitHubApiException(resp.code)
            val parsed = json.decodeFromString<DeviceCodeResponse>(body)
            DeviceFlowStart(parsed.deviceCode, parsed.userCode, parsed.verificationUri, parsed.interval)
        }
    }

    suspend fun exchangeDeviceCode(clientId: String, deviceCode: String): ExchangeResult =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("client_id", clientId)
                .add("device_code", deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()
            val request = Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .post(form)
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val parsed = try { json.decodeFromString<AccessTokenResponse>(body) } catch (_: Exception) { null }
                when {
                    parsed?.accessToken != null -> ExchangeResult.Success(parsed.accessToken)
                    parsed?.error == "authorization_pending" -> ExchangeResult.Pending
                    parsed?.error == "slow_down" -> ExchangeResult.SlowDown
                    parsed?.errorDescription != null -> ExchangeResult.Error(parsed.errorDescription)
                    else -> ExchangeResult.Error("")
                }
            }
        }

    suspend fun validateToken(token: String): GhUser {
        val (code, body) = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(request).execute().use { Pair(it.code, it.body?.string().orEmpty()) }
        }
        if (code != 200) throw GitHubApiException(code)
        return json.decodeFromString<GhUser>(body)
    }
}

/** Client ID اپ OAuth تنظیم نشده است */
class ClientIdMissingException : IOException()
