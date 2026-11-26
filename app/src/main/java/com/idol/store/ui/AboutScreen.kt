package com.idol.store.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.BuildConfig
import com.idol.store.R

/** صفحه درباره برنامه: معرفی + نسخه + مشخصات دستگاه و روش نصب */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // هدر گرادیانی با لوگو
        Box(
            Modifier
                .fillMaxWidth()
                .background(HeaderGradient),
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .background(GlowGradient),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // معرفی برنامه
            Text(
                stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
            )

            Spacer(Modifier.height(20.dp))

            // اطلاعات دستگاه
            Text(
                stringResource(R.string.about_device_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoRow(
                    stringResource(R.string.about_version_label),
                    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
                InfoRow(
                    stringResource(R.string.about_cpu_label),
                    Build.SUPPORTED_ABIS.firstOrNull() ?: "—",
                )
                InfoRow(
                    stringResource(R.string.about_device_label),
                    "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                )
                InfoRow(
                    stringResource(R.string.about_android_label),
                    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                )
                InfoRow(
                    stringResource(R.string.about_install_label),
                    installSourceText(context),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "© Alvandcode • Idol Store",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1.4f),
        )
    }
}

/** تشخیص روش نصب از روی منبع نصب پکیج */
@Suppress("DEPRECATION")
private fun installSourceText(context: Context): String = try {
    val pm = context.packageManager
    val source: String? = if (Build.VERSION.SDK_INT >= 30) {
        pm.getInstallSourceInfo(context.packageName).initiatingPackageName
    } else {
        pm.getInstallerPackageName(context.packageName)
    }
    when {
        source == null -> context.getString(R.string.install_sideload)
        source == "com.android.packageinstaller" ||
            source == "com.google.android.packageinstaller" ->
            context.getString(R.string.install_system_installer)
        source.contains("vending") || source.contains("play") ->
            context.getString(R.string.install_play)
        else -> context.getString(R.string.install_other_fmt, source)
    }
} catch (_: Exception) {
    context.getString(R.string.install_sideload)
}
