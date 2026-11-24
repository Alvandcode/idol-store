package com.idol.store.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.R
import com.idol.store.data.AppContentState
import com.idol.store.data.DownloadState
import com.idol.store.data.StoreApp
import com.idol.store.data.downloadKey

/** اسکلت اصلی استور با نوار پایین به سبک گوگل‌پلی */
@Composable
fun StoreTabs(vm: StoreViewModel, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_store)) },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_search)) },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_categories)) },
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_you)) },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> HomeTab(vm, onLogout, Modifier.padding(padding))
            1 -> SearchTab(vm, Modifier.padding(padding))
            2 -> CategoriesTab(vm, Modifier.padding(padding))
            else -> YouTab(vm, onLogout, Modifier.padding(padding))
        }
    }
}

// ---------------- استور (خانه) ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTab(vm: StoreViewModel, onLogout: () -> Unit, modifier: Modifier) {
    val contentMap by vm.content.collectAsState()
    var selectedCat by rememberSaveable { mutableStateOf("همه") }
    val categories = vm.catalog?.categories.orEmpty()
    val apps = vm.catalog?.apps.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    if (vm.isAdmin) {
                        IconButton(onClick = { vm.adminMode = true }) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = stringResource(R.string.admin_panel))
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.logout))
                    }
                },
            )
        },
        modifier = modifier,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // چیپ‌های دسته‌بندی (مثل نوار بالای گوگل‌پلی)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCat == "همه",
                        onClick = { selectedCat = "همه" },
                        label = { Text(stringResource(R.string.all_chip)) },
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCat == cat,
                        onClick = { selectedCat = cat },
                        label = { Text(cat) },
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
                        val recent = vm.recentApps
                        if (recent.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    stringResource(R.string.recent_updates),
                                    showViewAll = recent.size > 4,
                                ) { vm.openCategory = "__recent__" }
                            }
                            items(recent.take(4), key = { "r_${it.id}" }) { app ->
                                AppRowItem(vm, contentMap, app)
                            }
                        }
                        categories.forEach { cat ->
                            val list = apps.filter { it.category == cat }
                            if (list.isNotEmpty()) {
                                item(key = "h_$cat") {
                                    SectionHeader(cat, showViewAll = list.size > 4) {
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
private fun AppRowItem(vm: StoreViewModel, contentMap: Map<String, AppContentState>, app: StoreApp) {
    val states by vm.downloadStates.collectAsState()
    val state = contentMap[app.id] ?: AppContentState.Loading
    val latest = (state as? AppContentState.Ready)?.releases?.firstOrNull()
    val key = latest?.let { downloadKey(app.id, it.tagName) }

    AppRow(
        app = app,
        state = state,
        downloadState = key?.let { states[it] } ?: DownloadState.Idle,
        installedVersion = vm.installedVersionCode(app.packageName),
        onClick = { vm.selectedApp = app },
        onDownload = {
            if (latest != null) (state as? AppContentState.Ready)?.let {
                vm.download(app, it.releases.first().tagName, it.universalApk)
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

@Composable
private fun SectionHeader(title: String, showViewAll: Boolean, onViewAll: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (showViewAll) {
            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.view_all))
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (vm.query.isNotEmpty()) {
                    IconButton(onClick = { vm.query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
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

// ---------------- دسته‌ها ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesTab(vm: StoreViewModel, modifier: Modifier) {
    val categories = vm.catalog?.categories.orEmpty()
    val apps = vm.catalog?.apps.orEmpty()

    Column(modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.categories_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp),
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(categories) { cat ->
                    val count = apps.count { it.category == cat }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { vm.openCategory = cat },
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Category, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(cat, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.apps_count_fmt, count),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- شما ----------------

@Composable
private fun YouTab(vm: StoreViewModel, onLogout: () -> Unit, modifier: Modifier) {
    val contentMap by vm.content.collectAsState()
    val context = LocalContext.current
    val apps = vm.catalog?.apps.orEmpty()
    val installed = apps.filter { vm.installedVersionCode(it.packageName) != null }
    var themeMode by rememberSaveable { mutableStateOf(Prefs.themeMode.name) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.you_github_account),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        vm.currentLogin ?: "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.you_stats_fmt, apps.size, installed.size),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onLogout) { Text(stringResource(R.string.logout_account)) }
                }
            }
        }

        // تنظیمات تم
        item {
            Text(
                stringResource(R.string.settings_theme),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val options = listOf(
                ThemeMode.DARK to stringResource(R.string.theme_dark),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
            )
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = themeMode == mode.name,
                        onClick = {
                            themeMode = mode.name
                            Prefs.setTheme(context, mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
        }

        // تنظیمات زبان
        item {
            Text(
                stringResource(R.string.settings_language),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
            )
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

        item {
            Text(
                stringResource(R.string.you_installed_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
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
        else -> key
    }
    val list = when (key) {
        "__recent__" -> vm.recentApps
        "__other__" -> vm.catalog?.apps.orEmpty().filter { it.category.isNullOrBlank() }
        else -> vm.appsIn(key)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
