package com.idol.store.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.idol.store.R
import com.idol.store.data.DownloadState
import com.idol.store.data.GhAsset
import com.idol.store.data.GhRelease
import com.idol.store.data.GitHubService
import com.idol.store.data.StoreApp
import com.idol.store.data.UpdatePrefs
import com.idol.store.data.downloadKey
import com.idol.store.data.humanSize
import com.idol.store.data.shortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(vm: StoreViewModel, app: StoreApp, onBack: () -> Unit) {
    val states by vm.downloadStates.collectAsState()
    val contentMap by vm.content.collectAsState()
    val state = contentMap[app.id] ?: AppContentState.Loading
    val installedVersion = vm.installedVersionCode(app.packageName)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            when (val s = state) {
                is AppContentState.Loading -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }

                is AppContentState.Error -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.retryApp(app) }) { Text(stringResource(R.string.retry)) }
                }

                is AppContentState.Ready -> {
                    val latest = s.releases.first()

                    // هدر هیرو: پس‌زمینه گرادیانی + آیکون برند
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(HeaderGradient),
                        ) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 24.dp)
                                    .size(90.dp)
                                    .background(GlowGradient),
                            )
                        }

                        // هدر: آیکون + نام + توسعه‌دهنده + ستاره
                        Row(
                            Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(
                                model = app.iconUrl,
                                contentDescription = app.name,
                                size = 84.dp,
                                corner = 22.dp,
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.name, style = MaterialTheme.typography.headlineSmall, fontSize = 20.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    app.developer ?: "—",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (installedVersion != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        InstalledBadge()
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.installed_version_fmt, installedVersion),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            // دکمه ستاره
                            FilledTonalIconButton(onClick = { vm.toggleStar(app) }) {
                                Icon(
                                    imageVector = if (s.starred == true) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = stringResource(R.string.star),
                                    tint = if (s.starred == true)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // اعلان بروزرسانی این اپ
                    NotificationToggle(
                        appId = app.id,
                        latestTag = latest.tagName,
                    )

                    Spacer(Modifier.height(16.dp))

                    // توضیحات (از کاتالوگ، وگرنه توضیحات ریپو از گیت‌هاب)
                    SectionTitle(stringResource(R.string.detail_description))
                    Text(
                        app.description ?: s.repoInfo?.description
                            ?: stringResource(R.string.detail_desc_none),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    // اطلاعات تماس سازنده
                    val contacts = buildList {
                        (app.website ?: s.repoInfo?.homepage?.takeIf { it.isNotBlank() })?.let {
                            add(stringResource(R.string.contact_website), it, Icons.Filled.Public)
                        }
                        app.email?.let {
                            add(
                                stringResource(R.string.contact_email),
                                "mailto:$it",
                                Icons.Filled.Email,
                            )
                        }
                        add(
                            stringResource(R.string.contact_github),
                            s.repoInfo?.htmlUrl ?: "https://github.com/${s.repo}",
                            Icons.Filled.Code,
                        )
                        app.telegram?.let {
                            add(stringResource(R.string.contact_telegram), it, Icons.Filled.Send)
                        }
                    }
                    if (contacts.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.detail_contact_title))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            contacts.forEach { c ->
                                FilledTonalButton(onClick = { openUrl(context, c.url) }) {
                                    Icon(c.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(c.label, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    // آخرین ریلیز پایدار
                    SectionTitle(stringResource(R.string.detail_latest_stable))
                    ReleaseCard(
                        release = latest,
                        apk = s.universalApk,
                        publishedAt = shortDate(latest.publishedAt),
                        installStatus = installStatusOf(installedVersion, latest.tagName),
                        downloadState = states[downloadKey(app.id, latest.tagName)] ?: DownloadState.Idle,
                        onDownload = { vm.download(app, latest.tagName, s.universalApk) },
                        onCancel = { vm.cancel(downloadKey(app.id, latest.tagName)) },
                        onInstall = {
                            val key = downloadKey(app.id, latest.tagName)
                            (states[key] as? DownloadState.Ready)?.let { vm.install(it.file) }
                        },
                        onOpenRelease = { openUrl(context, latest.htmlUrl) },
                    )

                    // ریلیزهای قدیمی‌تر پایدار
                    val older = s.releases.drop(1).take(6)
                    if (older.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        SectionTitle(stringResource(R.string.detail_older_releases))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            older.forEach { release ->
                                val apk = GitHubService.pickUniversalApk(release.assets)
                                if (apk != null) {
                                    ReleaseCard(
                                        release = release,
                                        apk = apk,
                                        publishedAt = shortDate(release.publishedAt),
                                        installStatus = installStatusOf(installedVersion, release.tagName),
                                        downloadState = states[downloadKey(app.id, release.tagName)]
                                            ?: DownloadState.Idle,
                                        onDownload = { vm.download(app, release.tagName, apk) },
                                        onCancel = { vm.cancel(downloadKey(app.id, release.tagName)) },
                                        onInstall = {
                                            val key = downloadKey(app.id, release.tagName)
                                            (states[key] as? DownloadState.Ready)?.let { vm.install(it.file) }
                                        },
                                        onOpenRelease = { openUrl(context, release.htmlUrl) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private data class ContactInfo(val label: String, val url: String, val icon: ImageVector)

private fun MutableCollection<ContactInfo>.add(label: String, url: String, icon: ImageVector) {
    add(ContactInfo(label, url, icon))
}

private fun openUrl(context: android.content.Context, url: String?) {
    if (url.isNullOrBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

@Composable
private fun NotificationToggle(appId: String, latestTag: String) {
    val context = LocalContext.current
    var notifyOn by remember(appId) { mutableStateOf(UpdatePrefs.isNotifying(context, appId)) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* نتیجه بعداً اعمال می‌شود؛ اعلان بدون مجوز ارسال نمی‌شود */ }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.notif_toggle_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    stringResource(R.string.notif_toggle_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = notifyOn,
                onCheckedChange = { enabled ->
                    if (enabled && android.os.Build.VERSION.SDK_INT >= 33) {
                        permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    UpdatePrefs.setNotifying(context, appId, enabled, latestTag)
                    notifyOn = enabled
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ReleaseCard(
    release: GhRelease,
    apk: GhAsset,
    publishedAt: String,
    installStatus: InstallStatus,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.version_fmt, release.tagName),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        if (installStatus != InstallStatus.NOT_INSTALLED) {
                            Spacer(Modifier.width(6.dp))
                            InstalledBadge()
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(
                            R.string.release_meta_fmt,
                            apk.name,
                            humanSize(apk.size),
                            publishedAt,
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                DownloadButton(
                    state = downloadState,
                    installStatus = installStatus,
                    onDownload = onDownload,
                    onCancel = onCancel,
                    onInstall = onInstall,
                )
            }
            if (!release.body.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    release.body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
                onClick = onOpenRelease,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.release_page), fontSize = 12.sp)
            }
        }
    }
}
