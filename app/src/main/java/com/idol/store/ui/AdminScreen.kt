package com.idol.store.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.idol.store.data.GitHubService
import com.idol.store.data.StoreApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val vm: AdminViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پنل مدیریت", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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
            // شناسه ورودی
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = "حساب فعلی: ${vm.currentLogin ?: "—"}",
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
                        "⛔ ${vm.globalError ?: "دسترسی مدیریتی ندارید."}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onBack) { Text("بازگشت به استور") }
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
                Text("افزودن برنامه جدید")
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
                        Icon(Icons.Filled.Edit, contentDescription = "ویرایش", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { pendingDelete = app }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            Text("مدیران (نام کاربری گیت‌هاب، با کاما جدا شود)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = vm.admins,
                onValueChange = { vm.admins = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Alvandcode, friend1") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text("دسته‌بندی‌ها (با کاما جدا شود)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = vm.categories,
                onValueChange = { vm.categories = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابزارها, رسانه, بازی, امنیت") },
                singleLine = true,
            )
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
                Text("انتشار در گیت‌هاب (کامیت apps.json)")
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
private fun AdminTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            if (vm.editingNew) "افزودن برنامه جدید" else "ویرایش «${editing.name}»",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(12.dp))

        AdminTextField("شناسه یکتا (id) *", id) { id = it }
        AdminTextField("نام نمایشی *", name) { name = it }
        AdminTextField("توسعه‌دهنده", developer) { developer = it }
        AdminTextField("دسته", category) { category = it }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("توضیحات") },
            modifier = Modifier.fillMaxWidth(),
        )
        AdminTextField("آدرس آیکون (URL)", iconUrl) { iconUrl = it }
        AdminTextField("نام پکیج (برای تشخیص نصب)", packageName) { packageName = it }
        AdminTextField("ریپو/صفحه ریلیز گیت‌هاب *", github) { github = it }
        AdminTextField("وب‌سایت سازنده (اختیاری)", website) { website = it }
        AdminTextField("ایمیل سازنده (اختیاری)", email) { email = it }
        AdminTextField("لینک کانال تلگرام (اختیاری)", telegram) { telegram = it }
        AdminTextField("دسته (باید از دسته‌های تعریف‌شده باشد)", category) { category = it }

        formError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    when {
                        id.isBlank() -> formError = "شناسه (id) الزامی است"
                        name.isBlank() -> formError = "نام الزامی است"
                        GitHubService.repoFromUrl(github) == null ->
                            formError = "آدرس گیت‌هاب نامعتبر است (مثال: owner/repo)"
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
                                    github = GitHubService.repoFromUrl(github.trim())!!, // بالاتر ولیدیشن شده
                                    website = website.trim().ifBlank { null },
                                    email = email.trim().ifBlank { null },
                                    telegram = telegram.trim().ifBlank { null },
                                )
                            )
                        }
                    }
                },
            ) { Text("ذخیره در لیست") }
            OutlinedButton(onClick = { vm.closeEditor() }) { Text("انصراف") }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "ذخیره فقط در حافظه داخلی است؛ با دکمه «انتشار در گیت‌هاب» به‌صورت کامیت منتشر می‌شود.",
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
        title = { Text("حذف «${app.name}»؟") },
        text = { Text("این اپ از لیست حذف می‌شود؛ تا «انتشار» نزنی روی گیت‌هاب تغییر نمی‌کند.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("حذف") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}
