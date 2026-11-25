package com.idol.store.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idol.store.BuildConfig
import com.idol.store.R
import com.idol.store.data.AuthManager
import com.idol.store.data.DeviceFlowStart
import com.idol.store.data.ExchangeResult
import com.idol.store.data.UserRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    /** وقتی true شد، MainActivity به استور می‌رود */
    var loggedIn by mutableStateOf(false)
        private set

    var busy by mutableStateOf(false)
    var polling by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var deviceCode by mutableStateOf<String?>(null)
    var verificationUri by mutableStateOf<String?>(null)

    private var flow: DeviceFlowStart? = null

    fun openVerificationPage() {
        val app = getApplication<Application>()
        try {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(verificationUri ?: "https://github.com/login/device"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun startDeviceFlow() {
        busy = true
        error = null
        deviceCode = null
        verificationUri = null
        viewModelScope.launch {
            try {
                val s = AuthManager.startDeviceFlow()
                flow = s
                deviceCode = s.userCode
                verificationUri = s.verificationUri
                busy = false
                polling = true
                pollLoop(s)
            } catch (e: Exception) {
                error = AppErrors.message(getApplication(), e)
            } finally {
                busy = false
                polling = false
            }
        }
    }

    private suspend fun pollLoop(start: DeviceFlowStart) {
        var interval = (start.interval.coerceAtLeast(3)).toLong()
        val deadline = System.currentTimeMillis() + 15 * 60 * 1000
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000)
            when (val result = AuthManager.exchangeDeviceCode(BuildConfig.GITHUB_CLIENT_ID, start.deviceCode)) {
                is ExchangeResult.Success -> {
                    val login = try {
                        AuthManager.validateToken(result.token).login
                    } catch (_: Exception) { null }
                    AuthManager.saveSession(getApplication(), result.token, login)
                    loggedIn = true
                    return
                }
                is ExchangeResult.Pending -> Unit
                is ExchangeResult.SlowDown -> interval += 5
                is ExchangeResult.Error -> {
                    error = result.message.ifBlank {
                        getApplication<Application>().getString(R.string.err_exchange_token)
                    }
                    return
                }
            }
        }
        error = getApplication<Application>().getString(R.string.login_code_expired)
    }

    fun loginWithToken(token: String) {
        if (token.isBlank()) {
            error = getApplication<Application>().getString(R.string.login_err_blank_token)
            return
        }
        busy = true
        error = null
        viewModelScope.launch {
            try {
                val user = AuthManager.validateToken(token.trim())
                AuthManager.saveSession(getApplication(), token.trim(), user.login)
                loggedIn = true
            } catch (e: Exception) {
                error = AppErrors.message(getApplication(), e)
            } finally {
                busy = false
            }
        }
    }

    // ---------- ثبت‌نام و ورود کاربر عادی (ایمیل + توکن دستگاه‌محور) ----------

    /** کد ثبت‌نام ساخته‌شده برای نمایش به کاربر */
    var regCode by mutableStateOf<String?>(null)
        private set
    /** توکن محاسبه‌شده برای (ایمیل، این دستگاه) — کاربر باید نگهش دارد */
    var regToken by mutableStateOf<String?>(null)
        private set
    var regEmail by mutableStateOf<String?>(null)
        private set

    /** ساخت کد و توکن ثبت‌نام از روی ایمیل (فقط محلی؛ هیچ درخواستی زده نمی‌شود) */
    fun prepareRegistration(email: String) {
        val app = getApplication<Application>()
        error = null
        if (!UserRegistry.isValidEmail(email)) {
            error = app.getString(R.string.err_user_bad_email)
            return
        }
        val trimmed = email.trim().lowercase()
        regEmail = trimmed
        regToken = UserRegistry.tokenFor(app, trimmed)
        regCode = UserRegistry.registrationCode(app, trimmed)
    }

    fun resetRegistration() {
        regCode = null
        regToken = null
        regEmail = null
        error = null
    }

    /** باز کردن صفحه ساخت Issue ثبت‌نام در گیت‌هاب با بدنه از پیش پرشده */
    fun openRegistrationIssue() {
        val code = regCode ?: return
        val app = getApplication<Application>()
        val title = java.net.URLEncoder.encode("[register] Idol Store", "UTF-8")
        val body = java.net.URLEncoder.encode(
            "```\n$code\n```",
            "UTF-8",
        )
        try {
            app.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://github.com/${BuildConfig.REGISTRY_ISSUE_REPO}" +
                            "/issues/new?title=$title&body=$body"
                    ),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    /** ورود کاربر: ایمیل + توکن → بررسی رجیستری → ذخیره نشست */
    fun loginUser(email: String, token: String) {
        val app = getApplication<Application>()
        if (!UserRegistry.isValidEmail(email)) {
            error = app.getString(R.string.err_user_bad_email)
            return
        }
        if (token.isBlank()) {
            error = app.getString(R.string.err_user_invalid_token)
            return
        }
        busy = true
        error = null
        viewModelScope.launch {
            when (val result = UserRegistry.checkLogin(app, email, token)) {
                is UserRegistry.AuthResult.Success -> {
                    AuthManager.saveUserSession(app, result.email)
                    loggedIn = true
                }
                UserRegistry.AuthResult.DeviceBoundToOtherEmail ->
                    error = app.getString(R.string.err_user_device_bound)
                UserRegistry.AuthResult.EmailBoundToOtherDevice ->
                    error = app.getString(R.string.err_user_email_bound)
                UserRegistry.AuthResult.InvalidToken ->
                    error = app.getString(R.string.err_user_invalid_token)
                UserRegistry.AuthResult.NotRegistered ->
                    error = app.getString(R.string.err_user_not_registered)
            }
            busy = false
        }
    }
}
