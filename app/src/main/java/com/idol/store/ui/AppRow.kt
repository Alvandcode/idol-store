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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.idol.store.data.DownloadState
import com.idol.store.data.StoreApp
import com.idol.store.data.downloadKey

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
                                "نسخه ${state.releases.first().tagName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.starred == true) {
                                Spacer(Modifier.width(8.dp))
                                Text("★", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    is AppContentState.Loading -> Text(
                        "در حال دریافت…",
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
            "نصب‌شده",
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
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    when (state) {
        is DownloadState.Idle ->
            FilledTonalButton(
                onClick = onDownload,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) { Text("دانلود") }

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
                Text("٪${state.progress}")
            }

        is DownloadState.Ready ->
            Button(
                onClick = onInstall,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) { Text("نصب") }

        is DownloadState.Failed ->
            OutlinedButton(
                onClick = onDownload,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) { Text("تلاش مجدد") }
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
            "خطا در دریافت اطلاعات",
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
        Button(onClick = onRetry) { Text("تلاش مجدد") }
    }
}
