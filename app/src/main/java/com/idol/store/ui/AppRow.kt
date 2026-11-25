package com.idol.store.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

/** استخراج versionCode عددی از تگ برای مقایسه با نسخه نصب‌شده */
fun versionCodeFromTag(tag: String): Int =
    Regex("(\\d+)").find(tag.replace(".", " "))?.groupValues?.get(1)?.toIntOrNull() ?: 0

/** محاسبه وضعیت نصب از روی versionCode نصب‌شده و تگ آخرین ریلیز */
fun installStatusOf(installedVersion: Int?, latestTag: String?): InstallStatus = when {
    installedVersion == null -> InstallStatus.NOT_INSTALLED
    latestTag == null -> InstallStatus.UP_TO_DATE
    installedVersion >= versionCodeFromTag(latestTag) -> InstallStatus.UP_TO_DATE
    else -> InstallStatus.UPDATE_AVAILABLE
}

/** ردیف اپ به سبک گوگل‌پلی: آیکون + نام + توضیح دوخطی + چیپ‌ها + دکمه دانلود */
@Composable
fun AppRow(
    app: StoreApp,
    state: AppContentState,
    downloadState: DownloadState,
    installedVersion: Int?,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    showButton: Boolean = true,
    installStatus: InstallStatus = InstallStatus.NOT_INSTALLED,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = app.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (installedVersion != null) {
                        Spacer(Modifier.width(6.dp))
                        InstalledBadge()
                    }
                }
                val desc = app.description ?: app.developer ?: "—"
                Text(
                    desc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
                when (state) {
                    is AppContentState.Ready ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.version_fmt, state.releases.first().tagName),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.starred == true) {
                                Spacer(Modifier.width(8.dp))
                                Text("★", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    is AppContentState.Loading -> Text(
                        stringResource(R.string.fetching),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    is AppContentState.Error -> Text(
                        state.message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showButton) {
                Spacer(Modifier.width(8.dp))
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
}

@Composable
fun InstalledBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) { Text(stringResource(R.string.btn_installed)) }

            // نصب است اما قدیمی → دکمه به‌روزرسانی
            InstallStatus.UPDATE_AVAILABLE ->
                Button(
                    onClick = onDownload,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) { Text(stringResource(R.string.btn_update)) }

            // نصب نیست → دکمه دانلود
            InstallStatus.NOT_INSTALLED ->
                FilledTonalButton(
                    onClick = onDownload,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) { Text(stringResource(R.string.download)) }
        }

        is DownloadState.Downloading ->
            OutlinedButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.progress_fmt, state.progress))
            }

        is DownloadState.Ready ->
            Button(
                onClick = onInstall,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) { Text(stringResource(R.string.install)) }

        is DownloadState.Failed ->
            OutlinedButton(
                onClick = onDownload,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) { Text(stringResource(R.string.retry)) }
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
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
