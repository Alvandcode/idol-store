package com.idol.store.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.idol.store.R
import com.idol.store.data.GitHubService
import com.idol.store.data.StoreApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val vm: AdminViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_panel), fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp),
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = stringResource(R.string.admin_current_account) + " ${vm.currentLogin ?: "—"}",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            when {
                vm.loading -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                !vm.allowed -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "⛔ " + stringResource(R.string.admin_no_access),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }

                vm.editing != null -> AppForm(vm)

                else -> AdminList(vm)
            }
        }
    }
}

@Composable
private fun AdminList(vm: AdminViewModel) {
    var pendingDelete by remember { mutableStateOf<StoreApp?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            FilledTonalButton(
                onClick = { vm.openEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.admin_add_new))
            }
        }

        items(vm.apps, key = { it.id }) { app ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${app.id}  •  ${app.github}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    FilledTonalIconButton(onClick = { vm.openEditor(app) }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = { pendingDelete = app }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.admin_managers_hint),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = vm.admins,
                onValueChange = { vm.admins = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Alvandcode, friend1") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.admin_categories_hint),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
            CategoryEditor(vm)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.publish() },
                enabled = !vm.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (vm.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.admin_publish_btn))
            }
            vm.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            vm.globalError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingDelete?.let { app ->
        ConfirmDeleteDialog(
            app = app,
            onConfirm = {
                vm.deleteApp(app)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun AdminTextField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

/** مدیریت دسته‌ها با چیپ: افزودن با دکمه، حذف با ✕ */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditor(vm: AdminViewModel) {
    var newCat by remember { mutableStateOf("") }
    val cats = vm.categoryList()

    Column {
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (cats.isEmpty()) {
                Text(
                    stringResource(R.string.no_categories_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            cats.forEach { cat ->
                androidx.compose.material3.InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(cat) },
                    trailingIcon = {
                        androidx.compose.material3.Text(
                            "✕",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { vm.removeCategory(cat) }
                                .padding(start = 6.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCat,
                onValueChange = { newCat = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.cat_add_hint)) },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = {
                    vm.addCategory(newCat)
                    newCat = ""
                },
                enabled = newCat.isNotBlank(),
            ) { Text(stringResource(R.string.cat_add_btn)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppForm(vm: AdminViewModel) {
    val editing = vm.editing!!
    var id by rememberSaveable { mutableStateOf(editing.id) }
    var name by rememberSaveable { mutableStateOf(editing.name) }
    var developer by rememberSaveable { mutableStateOf(editing.developer ?: "") }
    var category by rememberSaveable { mutableStateOf(editing.category ?: "") }
    var description by rememberSaveable { mutableStateOf(editing.description ?: "") }
    var iconUrl by rememberSaveable { mutableStateOf(editing.iconUrl ?: "") }
    var packageName by rememberSaveable { mutableStateOf(editing.packageName ?: "") }
    var github by rememberSaveable { mutableStateOf(editing.github) }
    var website by rememberSaveable { mutableStateOf(editing.website ?: "") }
    var email by rememberSaveable { mutableStateOf(editing.email ?: "") }
    var telegram by rememberSaveable { mutableStateOf(editing.telegram ?: "") }
    var formError by remember { mutableStateOf<String?>(null) }
    var quickUrl by remember { mutableStateOf("") }
    var catMenuOpen by remember { mutableStateOf(false) }

    // اعمال نتیجه «دریافت خودکار» روی فیلدها
    androidx.compose.runtime.LaunchedEffect(vm.fetchedApp) {
        vm.fetchedApp?.let { a ->
            id = a.id
            name = a.name
            developer = a.developer ?: ""
            description = a.description ?: ""
            iconUrl = a.iconUrl ?: ""
            github = a.github
            website = a.website ?: ""
            vm.consumeFetched()
            formError = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            if (vm.editingNew) stringResource(R.string.admin_new_app)
            else stringResource(R.string.admin_editing_fmt, editing.name),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(12.dp))

        // ---- کارت افزودن سریع: فقط لینک ریپو → بقیه خودکار ----
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.admin_quick_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.admin_quick_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = quickUrl,
                        onValueChange = { quickUrl = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("github.com/user/repo") },
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.autoFill(quickUrl) },
                        enabled = quickUrl.isNotBlank() && !vm.fetching,
                    ) { Text(stringResource(R.string.admin_fetch)) }
                }
                if (vm.fetching) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.admin_fetching),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                vm.fetchError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                if (vm.fetchedLatestTag != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "✅ " + stringResource(R.string.admin_latest_stable_fmt, vm.fetchedLatestTag!!),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.admin_fetch_filled),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        AdminTextField(stringResource(R.string.form_id) + " *", id) { id = it }
        AdminTextField(stringResource(R.string.form_name) + " *", name) { name = it }
        AdminTextField(stringResource(R.string.form_developer), developer) { developer = it }

        // انتخاب دسته از منوی دسته‌های موجود (یا بدون دسته)
        ExposedDropdownMenuBox(
            expanded = catMenuOpen,
            onExpandedChange = { catMenuOpen = it },
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.form_category_select)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = catMenuOpen,
                onDismissRequest = { catMenuOpen = false },
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(stringResource(R.string.form_no_category)) },
                    onClick = {
                        category = ""
                        catMenuOpen = false
                    },
                )
                vm.categoryList().forEach { cat ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            category = cat
                            catMenuOpen = false
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.form_description)) },
            modifier = Modifier.fillMaxWidth(),
        )
        AdminTextField(stringResource(R.string.form_icon_url), iconUrl) { iconUrl = it }
        AdminTextField(stringResource(R.string.form_package), packageName) { packageName = it }
        AdminTextField(stringResource(R.string.form_github) + " *", github) { github = it }
        AdminTextField(stringResource(R.string.form_website), website) { website = it }
        AdminTextField(stringResource(R.string.form_email), email) { email = it }
        AdminTextField(stringResource(R.string.form_telegram), telegram) { telegram = it }

        formError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    when {
                        id.isBlank() -> formError = context.getString(R.string.form_err_id)
                        name.isBlank() -> formError = context.getString(R.string.form_err_name)
                        GitHubService.repoFromUrl(github) == null ->
                            formError = context.getString(R.string.form_err_github)
                        else -> {
                            formError = null
                            vm.upsertApp(
                                StoreApp(
                                    id = id.trim(),
                                    name = name.trim(),
                                    developer = developer.trim().ifBlank { null },
                                    category = category.trim().ifBlank { null },
                                    description = description.trim().ifBlank { null },
                                    iconUrl = iconUrl.trim().ifBlank { null },
                                    packageName = packageName.trim().ifBlank { null },
                                    github = GitHubService.repoFromUrl(github.trim())!!,
                                    website = website.trim().ifBlank { null },
                                    email = email.trim().ifBlank { null },
                                    telegram = telegram.trim().ifBlank { null },
                                )
                            )
                        }
                    }
                },
            ) { Text(stringResource(R.string.form_save_list)) }
            OutlinedButton(onClick = { vm.closeEditor() }) {
                Text(stringResource(R.string.form_cancel))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.admin_local_note),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConfirmDeleteDialog(
    app: StoreApp,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_dialog_title_fmt, app.name)) },
        text = { Text(stringResource(R.string.delete_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.form_cancel)) }
        },
    )
}
