package com.idol.store.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.R
import com.idol.store.data.DownloadState
import com.idol.store.data.StoreApp
import com.idol.store.data.downloadKey
import androidx.compose.material.icons.filled.Shield

/** اسکلت اصلی استور با نوار پایین به سبک گوگل‌پلی */
@Composable
fun StoreTabs(vm: StoreViewModel, onLogout: () -> Unit, onOpenLegal: (String) -> Unit = {}) {
    var tab by rememberSaveable { mutableStateOf(0) }

    // با هر بازگشت به اپ (مثلاً بعد از نصب APK) وضعیت نصب دوباره چک می‌شود
    var installTick by rememberSaveable { mutableStateOf(0) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) installTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            IdolBottomBar(
                items = listOf(
                    Icons.Filled.Home to stringResource(R.string.tab_store),
                    Icons.Filled.Search to stringResource(R.string.tab_search),
                    Icons.Filled.Category to stringResource(R.string.tab_categories),
                    Icons.Filled.Person to stringResource(R.string.tab_you),
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        },
    ) { padding ->
        when (tab) {
            0 -> HomeTab(vm, onLogout, Modifier.padding(padding), installTick, onGoToSearch = { tab = 1 })
            1 -> SearchTab(vm, Modifier.padding(padding))
            2 -> CategoriesTab(vm, Modifier.padding(padding))
            else -> YouTab(vm, onLogout, Modifier.padding(padding), installTick, onOpenLegal)
        }
    }
}

/** آیکون برند (لوگوی اپ) در اندازه‌های کوچک */
@Composable
private fun BrandIcon(size: Int) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.26f).dp)),
    )
}

/**
 * نوار پایین به سبک «Tab Bar Interactive»:
 * جزیره شناور با نشانگر لیکوئیدی گرادیان برند که با فنر می‌لغزد،
 * آیکون فعال بالا و بزرگ می‌شود و برچسبش ظاهر می‌گردد.
 */
@Composable
private fun IdolBottomBar(
    items: List<Pair<androidx.compose.ui.graphics.vector.ImageVector, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        val barWidth = maxWidth
        val itemWidth = barWidth / items.size
        val barHeight = 66.dp
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        // لغزش نشانگر با فنر
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * (if (rtl) (items.size - 1 - selected) else selected),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
            label = "navIndicator",
        )
        // کشسگی نشانگر حین حرکت (افکت مایع)
        val indicatorStretch by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
            label = "navStretch",
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(10.dp, CircleShape, spotColor = Color(0xFF22D3EE))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        ) {
            // نشانگر لیکوئیدی گرادیان برند (offset مطلق — خودش آینه نمی‌شود)
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(indicatorX.roundToPx(), 0) }
                    .padding(7.dp)
                    .width((itemWidth - 14.dp) * indicatorStretch)
                    .height(barHeight - 14.dp)
                    .clip(CircleShape)
                    .background(BrandGradient),
            )

            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, (icon, label) ->
                    val sel = i == selected
                    // پرش آیکون فعال با فنر
                    val iconScale by animateFloatAsState(
                        targetValue = if (sel) 1.18f else 1f,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = 640f),
                        label = "navIcon$i",
                    )
                    val iconLift by animateDpAsState(
                        targetValue = if (sel) (-9).dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "navLift$i",
                    )
                    val labelAlpha by animateFloatAsState(
                        targetValue = if (sel) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 380f),
                        label = "navLabel$i",
                    )

                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(i) },
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = iconLift)
                                .scale(iconScale)
                                .size(24.dp),
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 19.dp)
                                .alpha(labelAlpha),
                        )
                    }
                }
            }
        }
    }
}

// ---------------- استور (خانه) ----------------

@Composable
private fun HomeTab(
    vm: StoreViewModel,
    onLogout: () -> Unit,
    modifier: Modifier,
    installTick: Int = 0,
    onGoToSearch: () -> Unit,
) {
    val contentMap by vm.content.collectAsState()
    var selectedCat by rememberSaveable { mutableStateOf("همه") }
    val categories = vm.catalog?.categories.orEmpty()
    val apps = vm.catalog?.apps.orEmpty()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // هدر برند با گلو نئونی
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(140.dp)
                        .background(GlowGradient),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandIcon(34)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 20.sp,
                            color = Color.White,
                        )
                        Text(
                            stringResource(R.string.home_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                    if (vm.isAdmin) {
                        IconButton(onClick = { vm.adminMode = true }) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = stringResource(R.string.admin_panel),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = stringResource(R.string.logout),
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            // نوار جستجو به سبک گوگل‌پلی — کلیک → تب جستجو
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onGoToSearch)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // چیپ‌های دسته‌بندی
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCat == "همه",
                        onClick = { selectedCat = "همه" },
                        label = { Text(stringResource(R.string.all_chip)) },
                        colors = chipColors(),
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCat == cat,
                        onClick = { selectedCat = cat },
                        label = { Text(localizedCategory(cat)) },
                        colors = chipColors(),
                    )
                }
            }

            when {
                vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                vm.error != null -> ErrorView(vm.error!!, onRetry = { vm.refresh() })

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (selectedCat == "همه") {
                        // کاروسل «به‌روزرسانی‌های اخیر» — سبک گوگل‌پلی
                        val recent = vm.recentApps
                        if (recent.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    stringResource(R.string.recent_updates),
                                    showViewAll = recent.size > 4,
                                ) { vm.openCategory = "__recent__" }
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    items(recent.take(8), key = { "rc_${it.id}" }) { app ->
                                        AppCardHorizontal(
                                            app = app,
                                            subtitle = app.developer ?: app.category ?: "—",
                                            onClick = { vm.selectedApp = app },
                                        )
                                    }
                                }
                            }
                        }
                        categories.forEach { cat ->
                            val list = apps.filter { it.category == cat }
                            if (list.isNotEmpty()) {
                                item(key = "h_$cat") {
                                    SectionHeader(localizedCategory(cat), showViewAll = list.size > 4) {
                                        vm.openCategory = cat
                                    }
                                }
                                items(list.take(4), key = { "c${cat}_${it.id}" }) { app ->
                                    AppRowItem(vm, contentMap, app)
                                }
                            }
                        }
                        val others = apps.filter { it.category.isNullOrBlank() }
                        if (others.isNotEmpty()) {
                            item(key = "h_other") {
                                SectionHeader(
                                    stringResource(R.string.other_apps),
                                    showViewAll = others.size > 4,
                                ) { vm.openCategory = "__other__" }
                            }
                            items(others.take(4), key = { "o_${it.id}" }) { app ->
                                AppRowItem(vm, contentMap, app)
                            }
                        }
                    } else {
                        val list = apps.filter { it.category == selectedCat }
                        if (list.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.category_empty),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(list, key = { it.id }) { app ->
                                AppRowItem(vm, contentMap, app)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun AppRowItem(vm: StoreViewModel, contentMap: Map<String, AppContentState>, app: StoreApp) {
    val states by vm.downloadStates.collectAsState()
    val state = contentMap[app.id] ?: AppContentState.Loading
    val installedVersion = vm.installedVersionName(app.packageName)
    val latest = (state as? AppContentState.Ready)?.releases?.firstOrNull()
    val status = installStatusOf(installedVersion, latest?.tagName)
    val key = latest?.let { downloadKey(app.id, it.tagName) }

    AppRow(
        app = app,
        state = state,
        downloadState = key?.let { states[it] } ?: DownloadState.Idle,
        installedVersionName = installedVersion,
        installStatus = status,
        onClick = { vm.selectedApp = app },
        onDownload = {
            if (latest != null) (state as? AppContentState.Ready)?.let {
                vm.requestDownload(app, it.releases.first().tagName, it.universalApk)
            }
        },
        onCancel = { latest?.let { vm.cancel(downloadKey(app.id, it.tagName)) } },
        onInstall = {
            if (key != null) {
                (states[key] as? DownloadState.Ready)?.let { vm.install(it.file) }
            }
        },
    )
}

// ---------------- جستجو ----------------

@Composable
private fun SearchTab(vm: StoreViewModel, modifier: Modifier) {
    val contentMap by vm.content.collectAsState()

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder = {
                Text(
                    stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (vm.query.isNotEmpty()) {
                    IconButton(onClick = { vm.query = "" }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.search_clear),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if (vm.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val results = vm.filteredApps
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (vm.query.isBlank()) stringResource(R.string.search_empty_prompt)
                        else stringResource(R.string.nothing_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(results, key = { it.id }) { app ->
                        AppRowItem(vm, contentMap, app)
                    }
                }
            }
        }
    }
}

// ---------------- دسته‌ها (گرید گرادیانی) ----------------

/** پالت‌های گرادیان برای کاشی‌های دسته‌بندی */
private val CategoryPalettes = listOf(
    listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
    listOf(Color(0xFF10B981), Color(0xFF0EA5E9)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFF22D3EE), Color(0xFF3B82F6)),
    listOf(Color(0xFFF472B6), Color(0xFF8B5CF6)),
)

@Composable
private fun CategoriesTab(vm: StoreViewModel, modifier: Modifier) {
    val categories = vm.catalog?.categories.orEmpty()
    val apps = vm.catalog?.apps.orEmpty()

    Column(modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.categories_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_categories_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(categories) { cat ->
                    val count = apps.count { it.category == cat }
                    val palette = CategoryPalettes[
                        kotlin.math.abs(cat.hashCode()) % CategoryPalettes.size
                    ]
                    Box(
                        Modifier
                            .height(96.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Brush.linearGradient(palette))
                            .clickable { vm.openCategory = cat }
                            .padding(14.dp),
                    ) {
                        Column(Modifier.align(Alignment.BottomStart)) {
                            Text(
                                localizedCategory(cat),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                            )
                            Text(
                                stringResource(R.string.apps_count_fmt, count),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------- شما ----------------

@Composable
private fun YouTab(vm: StoreViewModel, onLogout: () -> Unit, modifier: Modifier, installTick: Int = 0, onOpenLegal: (String) -> Unit = {}) {
    val contentMap by vm.content.collectAsState()
    val context = LocalContext.current
    val apps = vm.catalog?.apps.orEmpty()
    val installed = apps.filter { vm.installedVersionName(it.packageName) != null }
    var themeMode by rememberSaveable { mutableStateOf(Prefs.themeMode.name) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        // بنر پروفایل گرادیانی
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(140.dp)
                        .background(GlowGradient),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // آواتار دایره‌ای با حرف اول
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BrandGradient),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (vm.currentLogin ?: "I").take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.you_github_account),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                        Text(
                            vm.currentLogin ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // چیپ‌های آمار
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatChip(stringResource(R.string.you_stats_apps, apps.size))
                StatChip(stringResource(R.string.you_stats_installed, installed.size))
            }
        }

        // تنظیمات تم
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                val options = listOf(
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = themeMode == mode.name,
                            onClick = {
                                themeMode = mode.name
                                Prefs.setTheme(context, mode)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }

        // تنظیمات زبان
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                            androidx.core.os.LocaleListCompat.forLanguageTags("fa")
                        )
                    }) { Text(stringResource(R.string.lang_fa)) }
                    OutlinedButton(onClick = {
                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                            androidx.core.os.LocaleListCompat.forLanguageTags("en")
                        )
                    }) { Text(stringResource(R.string.lang_en)) }
                }
            }
        }

        // خروج
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.logout_account))
                }
            }
        }

        // صفحات حقوقی و درباره
        item {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegalMiniCard(
                        icon = Icons.Filled.Shield,
                        label = stringResource(R.string.legal_privacy_short),
                        onClick = { onOpenLegal("privacy") },
                        modifier = Modifier.weight(1f),
                    )
                    LegalMiniCard(
                        icon = Icons.Filled.Description,
                        label = stringResource(R.string.legal_terms_short),
                        onClick = { onOpenLegal("terms") },
                        modifier = Modifier.weight(1f),
                    )
                }
                LegalMiniCard(
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.legal_about_title),
                    onClick = { onOpenLegal("about") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // اپ‌های نصب‌شده
        item {
            SectionHeader(stringResource(R.string.you_installed_title))
        }
        if (installed.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.you_no_installed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            items(installed, key = { it.id }) { app ->
                AppRowItem(vm, contentMap, app)
            }
        }
    }
}

// ---------------- فهرست یک دسته ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAppsScreen(vm: StoreViewModel, key: String, onBack: () -> Unit) {
    val contentMap by vm.content.collectAsState()
    val title = when (key) {
        "__recent__" -> stringResource(R.string.recent_updates)
        "__other__" -> stringResource(R.string.other_apps)
        else -> localizedCategory(key)
    }
    val list = when (key) {
        "__recent__" -> vm.recentApps
        "__other__" -> vm.catalog?.apps.orEmpty().filter { it.category.isNullOrBlank() }
        else -> vm.appsIn(key)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (list.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(list, key = { it.id }) { app ->
                    AppRowItem(vm, contentMap, app)
                }
            }
        }
    }
}
