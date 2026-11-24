package com.idol.store.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.idol.store.MainActivity
import com.idol.store.R
import java.util.concurrent.TimeUnit

const val EXTRA_OPEN_APP_ID = "open_app_id"
private const val CHANNEL_ID = "app_updates"
private const val WORK_NAME = "idol_update_check"

/**
 * چک دوره‌ای بروزرسانی اپ‌هایی که کاربر اعلان‌شان را روشن کرده؛
 * به محض انتشار ریلیز پایدار جدید، اعلان می‌دهد.
 */
class UpdateChecker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val notifying = UpdatePrefs.notifyingIds(context)
        if (notifying.isEmpty()) return Result.success()

        val gh = GitHubService { AuthManager.token(context) }
        return try {
            val catalog = CatalogRepository.load(context, forceRefresh = true)
            catalog.apps.filter { it.id in notifying }.forEach { app ->
                try {
                    val repo = GitHubService.repoFromUrl(app.github) ?: return@forEach
                    val latest = gh.stableReleases(repo).firstOrNull() ?: return@forEach
                    val last = UpdatePrefs.lastTag(context, app.id)
                    if (last == null) {
                        UpdatePrefs.setLastTag(context, app.id, latest.tagName)
                    } else if (latest.tagName != last) {
                        showUpdateNotification(context, app, latest.tagName)
                        UpdatePrefs.setLastTag(context, app.id, latest.tagName)
                    }
                } catch (_: Exception) {
                    // این اپ را نادیده بگیر؛ بقیه اپ‌ها بررسی شوند
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {

        /** یک‌بار در شروع اپ ثبت می‌شود */
        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateChecker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun showUpdateNotification(context: Context, app: StoreApp, tag: String) {
            val manager = NotificationManagerCompat.from(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                channel.description = context.getString(R.string.notif_channel_desc)
                manager.createNotificationChannel(channel)
            }

            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_APP_ID, app.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context, app.id.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(context.getString(R.string.notif_title_fmt, app.name))
                .setContentText(context.getString(R.string.notif_text_fmt, tag))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notif_bigtext_fmt, app.name, tag))
                )
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

            try {
                manager.notify(app.id.hashCode(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}
