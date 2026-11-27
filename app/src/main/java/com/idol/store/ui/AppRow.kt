package com.idol.store.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.R
import com.idol.store.data.DownloadState
import com.idol.store.data.StoreApp
import com.idol.store.data.downloadKey

/** وضعیت نصب اپ روی دستگاه، برای تعیین دکمه */
enum class InstallStatus {
    /** نصب نیست → دکمه دانلود */
    NOT_INSTALLED,
    /** نصب است و جدیدترین نسخه است → دکمه غیرفعال «نصب شده» */
    UP_TO_DATE,
    /** نصب است اما نسخه قدیمی‌تر از آخرین ریلیز است → دکمه «به‌روزرسانی» */
    UPDATE_AVAILABLE,
}

/** استخراج اجزای عددی نسخه از versionName یا تگ: "v2.3.15" → [2,3,15] */
fun parseVersionTuple(v: String?): List<Int> =
    v?.replace(Regex("^[vV]+"), "")
        ?.split('.', '-', '_', ' ')
        ?.mapNotNull { part -> part.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull() }
        ?: emptyList()

/** محاسبه وضعیت نصب با مقایسه versionName نصب‌شده و تگ آخرین ریلیز */
fun installStatusOf(installedVersionName: String?, latestTag: String?): InstallStatus = when {
    installedVersionName == null -> InstallStatus.NOT_INSTALLED
    latestTag == null -> InstallStatus.UP_TO_DATE
    else -> {
        val a = parseVersionTuple(installedVersionName)
        val b = parseVersionTuple(latestTag)
        val n = maxOf(a.size, b.size)
        var cmp = 0
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) {
                cmp = if (x < y) -1 else 1
                break
            }
        }
        if (cmp >= 0) InstallStatus.UP_TO_DATE else InstallStatus.UPDATE_AVAILABLE
    }
}

/** پاک‌سازی متن ریلیز: حذف HTML، مارک‌داون و لینک‌ها؛ فقط متن چنج‌لاگ */
fun cleanReleaseBody(raw: String): String {
    var s = raw
    // کامنت‌های HTML
    s = s.replace(Regex("<!--[\\s\\S]*?-->"), "")
    // خط جدید از br
    s = s.replace(Regex("<br\\s*/?>"), "\n")
    // تگ‌های بلوکی → خط جدید (قبل از حذف همه تگ‌ها)
    s = s.replace(Regex("</?(tr|td|th|table|thead|tbody|p|div|h[1-6]|li|ul|ol|blockquote)\\b[^>]*>"), "\n")
    // تصاویر markdown و HTML
    s = s.replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), "")
    s = s.replace(Regex("<img[^>]*>"), "")
    // لینک‌های markdown → فقط متن لینک
    s = s.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
    // حذف باقی تگ‌ها
    s = s.replace(Regex("<[^>]*>"), "")
    // entityها
    s = s.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
    // بولد/ایتالیک مارک‌داون
    s = s.replace(Regex("\\*\\*([^*]*)\\*\\*"), "$1")
    s = s.replace(Regex("__([^_]+)__"), "$1")
    s = s.replace("*", "")
    s = s.replace("`", "")
    // هدینگ‌های مارک‌داون
    s = s.replace(Regex("(?m)^#{1,6}\\s*"), "")
    // خطوط خالی تکراری
    s = s.replace(Regex("\n{3,}"), "\n\n")
    // فاصله‌های تکراری ناشی از جدول‌ها
    s = s.replace(Regex("[ ]{3,}"), "\n")
    // حذف کامل خطوط خالی: چنج‌لاگ فشرده، خطوط پشت هم و ادامه‌دار
    s = s.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    return s.trim()
}

/** ردیف اپ به سبک گوگل‌پلی: آیکون + نام + توضیح + متا + دکمه */
@Composable
fun AppRow(
    app: StoreApp,
    state: AppContentState,
    downloadState: DownloadState,
    installedVersionName: String?,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    showButton: Boolean = true,
    installStatus: InstallStatus = InstallStatus.NOT_INSTALLED,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            model = app.iconUrl,
            contentDescription = app.name,
            size = 58.dp,
        )

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (installedVersionName != null) {
                    Spacer(Modifier.width(8.dp))
                    InstalledBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            val desc = localizedDescription(app) ?: app.developer ?: "—"
            Text(
                desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            when (state) {
                is AppContentState.Ready ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.version_fmt, state.releases.first().tagName),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.starred == true) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "★",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                is AppContentState.Loading -> Text(
                    stringResource(R.string.fetching),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is AppContentState.Error -> Text(
                    state.message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showButton) {
            Spacer(Modifier.width(10.dp))
            when (state) {
                is AppContentState.Ready -> DownloadButton(
                    state = downloadState,
                    installStatus = installStatus,
                    onDownload = onDownload,
                    onCancel = onCancel,
                    onInstall = onInstall,
                )
                is AppContentState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                is AppContentState.Error -> Unit
            }
        }
    }
}

@Composable
fun InstalledBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    ) {
        Text(
            stringResource(R.string.installed),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** دکمه‌های فشرده نوار اپ — ارتفاع کم و شکل گرد */
private val CompactButtonPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)

@Composable
fun DownloadButton(
    state: DownloadState,
    installStatus: InstallStatus = InstallStatus.NOT_INSTALLED,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    when (state) {
        is DownloadState.Idle -> when (installStatus) {
            // نصب است و جدیدترین نسخه → نیازی به نصب مجدد نیست
            InstallStatus.UP_TO_DATE ->
                OutlinedButton(
                    enabled = false,
                    onClick = {},
                    contentPadding = CompactButtonPadding,
                    modifier = Modifier.heightIn(min = 34.dp),
                ) { Text(stringResource(R.string.btn_installed), style = MaterialTheme.typography.labelMedium) }

            // نصب است اما قدیمی → دکمه به‌روزرسانی
            InstallStatus.UPDATE_AVAILABLE ->
                Button(
                    onClick = onDownload,
                    contentPadding = CompactButtonPadding,
                    modifier = Modifier.heightIn(min = 34.dp),
                ) { Text(stringResource(R.string.btn_update), style = MaterialTheme.typography.labelMedium) }

            // نصب نیست → دکمه دانلود
            InstallStatus.NOT_INSTALLED ->
                FilledTonalButton(
                    onClick = onDownload,
                    contentPadding = CompactButtonPadding,
                    modifier = Modifier.heightIn(min = 34.dp),
                ) { Text(stringResource(R.string.download), style = MaterialTheme.typography.labelMedium) }
        }

        is DownloadState.Downloading ->
            OutlinedButton(
                onClick = onCancel,
                contentPadding = CompactButtonPadding,
                modifier = Modifier.heightIn(min = 34.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.progress_fmt, state.progress),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

        is DownloadState.Ready ->
            Button(
                onClick = onInstall,
                contentPadding = CompactButtonPadding,
                modifier = Modifier.heightIn(min = 34.dp),
            ) { Text(stringResource(R.string.install), style = MaterialTheme.typography.labelMedium) }

        is DownloadState.Failed ->
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    state.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
                OutlinedButton(
                    onClick = onDownload,
                    contentPadding = CompactButtonPadding,
                    modifier = Modifier.heightIn(min = 34.dp),
                ) { Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelMedium) }
            }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.error_loading),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 42.dp),
        ) { Text(stringResource(R.string.retry)) }
    }
}

/** کارت اپ برای کاروسل افقی صفحه اصلی — سبک گوگل‌پلی */
@Composable
fun AppCardHorizontal(
    app: StoreApp,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        AppIcon(
            model = app.iconUrl,
            contentDescription = app.name,
            size = 96.dp,
            corner = 24.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            app.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
