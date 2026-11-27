package com.idol.store.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import com.idol.store.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Int) : DownloadState
    data class Ready(val file: File) : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * دانلود مستقیم APK با نمایش درصد پیشرفت؛ فایل در پوشه اختصاصی اپ ذخیره می‌شود
 * و از طریق FileProvider برای نصب باز می‌گردد.
 */
object ApkDownloader {

    /** تایم‌اوت سخاوتمند: شبکه‌های ناپایدار نباید دانلود را بیندازند */
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val jobs = HashMap<String, Job>()

    private val _states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states: StateFlow<Map<String, DownloadState>> = _states

    fun start(context: Context, key: String, url: String, fileName: String) {
        synchronized(jobs) { if (jobs.containsKey(key)) return }

        _states.update { it + (key to DownloadState.Downloading(0)) }

        val job = scope.launch {
            try {
                val dir = context.getExternalFilesDir("apks") ?: File(context.filesDir, "apks")
                dir.mkdirs()
                val file = File(dir, fileName)

                // ادامه دانلود (Resume): فایل ناقص قبلی اگر باشد از همان‌جا ادامه می‌دهیم
                var resumeFrom = 0L
                if (file.exists() && file.length() > 0L) resumeFrom = file.length()

                val requestBuilder = Request.Builder().url(url)
                if (resumeFrom > 0L) requestBuilder.header("Range", "bytes=$resumeFrom-")
                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    // 416 = فایل قبلاً کامل بوده؛ همان را قبول کن
                    if (code == 416) {
                        _states.update { it + (key to DownloadState.Ready(file)) }
                        return@use
                    }
                    if (code != 200 && code != 206) throw IOException("HTTP $code")
                    // سرور Range را پشتیبانی نمی‌کند → از صفر شروع کن
                    var done = if (code == 206) resumeFrom else 0L
                    if (code == 200) file.delete()
                    val body = response.body ?: throw IOException(context.getString(R.string.err_empty_response))
                    val total = body.contentLength() + done
                    body.byteStream().use { input ->
                        java.io.FileOutputStream(file, code == 206).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            var lastProgress = -1
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) {
                                    val progress = ((done * 100) / total).toInt().coerceIn(0, 100)
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        _states.update {
                                            it + (key to DownloadState.Downloading(progress))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // فایل ناقص را «موفق» جا نزن: اتصال تمیز بسته شده ولی بایت‌ها ناقص‌اند
                    if (total > 0 && done < total) {
                        throw IOException(context.getString(R.string.err_incomplete, done, total))
                    }
                }

                _states.update { it + (key to DownloadState.Ready(file)) }

                // خودترمیمی: پکیج‌نیم واقعی از خود APK خوانده و ذخیره می‌شود
                // تا بعد از نصب، اپ به‌عنوان «نصب‌شده» شناسایی شود (حتی بدون packageName در کاتالوگ)
                InstalledPkgMap.save(context, key.substringBefore('|'), file)
            } catch (e: Exception) {
                _states.update {
                    it + (key to DownloadState.Failed(
                        e.message?.takeIf { m -> m.isNotBlank() }
                            ?: context.getString(R.string.err_unknown)
                    ))
                }
            } finally {
                synchronized(jobs) { jobs.remove(key) }
            }
        }

        synchronized(jobs) { jobs[key] = job }
    }

    fun cancel(key: String) {
        val job = synchronized(jobs) { jobs.remove(key) }
        job?.cancel()
        _states.update { it - key }
    }
}

/**
 * نگاشت appId → نام پکیج واقعی. منبع: پارس خود APK دانلودشده.
 * با این نگاشت، «به‌روز است ✓» حتی وقتی کاتالوگ packageName ندارد هم درست کار می‌کند.
 */
object InstalledPkgMap {

    private const val FILE = "pkg_map"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(context: Context, appId: String, apkFile: File) {
        try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val pkg = info?.packageName?.takeIf { it.isNotBlank() } ?: return
            prefs(context).edit().putString(appId, pkg).apply()
        } catch (_: Exception) {
        }
    }

    fun get(context: Context, appId: String): String? =
        try { prefs(context).getString(appId, null)?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
}
