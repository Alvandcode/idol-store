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
import com.idol.store.data.AuthManager
import com.idol.store.data.DeviceFlowStart
import com.idol.store.data.ExchangeResult
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
                val start = AuthManager.startDeviceFlow()
                flow = start
                deviceCode = start.userCode
                verificationUri = start.verificationUri
                busy = false
                polling = true
                pollLoop(start)
            } catch (e: Exception) {
                error = e.message
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
                    error = result.message
                    return
                }
            }
        }
        error = "زمان اعتبار کد به پایان رسید — دوباره تلاش کنید"
    }

    fun loginWithToken(token: String) {
        if (token.isBlank()) {
            error = "توکن را وارد کنید"
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
                error = e.message
            } finally {
                busy = false
            }
        }
    }
}
