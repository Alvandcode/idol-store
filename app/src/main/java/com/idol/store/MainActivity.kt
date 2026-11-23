package com.idol.store

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.idol.store.data.AuthManager
import com.idol.store.data.EXTRA_OPEN_APP_ID
import com.idol.store.data.UpdateChecker
import com.idol.store.ui.AdminScreen
import com.idol.store.ui.AppDetailScreen
import com.idol.store.ui.AuthViewModel
import com.idol.store.ui.CategoryAppsScreen
import com.idol.store.ui.IdolStoreTheme
import com.idol.store.ui.LoginScreen
import com.idol.store.ui.StoreTabs
import com.idol.store.ui.StoreViewModel

class MainActivity : ComponentActivity() {

    private var loggedIn by mutableStateOf(false)
    /** وقتی از اعلان آپدیت می‌آییم: صفحه همان اپ باز شود */
    private var deepLinkAppId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loggedIn = AuthManager.hasToken(this)
        deepLinkAppId = intent?.getStringExtra(EXTRA_OPEN_APP_ID)
        UpdateChecker.ensureScheduled(this)
        setContent {
            IdolStoreTheme {
                val auth: AuthViewModel = viewModel()
                if (loggedIn || auth.loggedIn) {
                    val store: StoreViewModel = viewModel()
                    StoreApp(
                        vm = store,
                        onLogout = { store.logout { loggedIn = false } },
                        deepLinkAppId = deepLinkAppId,
                        onDeepLinkHandled = { deepLinkAppId = null },
                    )
                } else {
                    LoginScreen(vm = auth, onLoggedIn = { loggedIn = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_APP_ID)?.let { deepLinkAppId = it }
    }
}

@Composable
fun StoreApp(
    vm: StoreViewModel,
    onLogout: () -> Unit,
    deepLinkAppId: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    // باز کردن صفحه اپ مشخص‌شده در اعلان
    LaunchedEffect(deepLinkAppId, vm.catalog) {
        val id = deepLinkAppId ?: return@LaunchedEffect
        val app = vm.catalog?.apps?.firstOrNull { it.id == id }
        if (app != null) {
            vm.adminMode = false
            vm.openCategory = null
            vm.selectedApp = app
            onDeepLinkHandled()
        }
    }

    if (vm.adminMode) {
        AdminScreen(onBack = { vm.adminMode = false; vm.refresh() })
        return
    }

    val selected = vm.selectedApp
    if (selected != null) {
        AppDetailScreen(vm = vm, app = selected, onBack = { vm.selectedApp = null })
        return
    }

    val category = vm.openCategory
    if (category != null) {
        CategoryAppsScreen(vm = vm, key = category, onBack = { vm.openCategory = null })
        return
    }

    StoreTabs(vm, onLogout = onLogout)
}
