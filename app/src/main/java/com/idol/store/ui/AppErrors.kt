package com.idol.store.ui

import android.content.Context
import com.idol.store.R
import com.idol.store.data.GitHubApiException
import com.idol.store.data.StoreErrorKind
import com.idol.store.data.StoreException

/** نگاشت استثناها به پیام‌های محلی (فارسی/انگلیسی) */
object AppErrors {

    fun message(context: Context, e: Throwable?): String = when (e) {
        is com.idol.store.data.ClientIdMissingException ->
            context.getString(R.string.login_err_client_id)

        is GitHubApiException -> when (e.code) {
            404 -> context.getString(R.string.err_repo_not_found, e.repo ?: "-")
            403, 429 -> context.getString(R.string.err_rate_limit)
            401 -> context.getString(R.string.err_invalid_token)
            else -> context.getString(R.string.err_github_code, e.code)
        }

        is StoreException -> when (e.kind) {
            StoreErrorKind.INVALID_REPO_URL ->
                context.getString(R.string.err_invalid_repo_url, e.detail ?: "")
            StoreErrorKind.NO_STABLE_RELEASE -> context.getString(R.string.err_no_stable_release)
            StoreErrorKind.NO_UNIVERSAL_APK -> context.getString(R.string.err_no_universal_apk)
        }

        else -> e?.message?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.err_unknown)
    }
}
