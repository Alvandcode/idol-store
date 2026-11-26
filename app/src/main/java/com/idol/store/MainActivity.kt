package com.idol.store

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.idol.store.data.AuthManager
import com.idol.store.data.EXTRA_OPEN_APP_ID
import com.idol.store.data.UpdateChecker
import com.idol.store.ui.AboutScreen
import com.idol.store.ui.AdminScreen
import com.idol.store.ui.AppDetailScreen
import com.idol.store.ui.AuthViewModel
import com.idol.store.ui.CategoryAppsScreen
import com.idol.store.ui.IdolStoreTheme
import com.idol.store.ui.LoginScreen
import com.idol.store.ui.PrivacyScreen
import com.idol.store.ui.Prefs
import com.idol.store.ui.SecurityGateSheet
import com.idol.store.ui.StoreTabs
import com.idol.store.ui.StoreViewModel
import com.idol.store.ui.TermsScreen

class MainActivity : AppCompatActivity() {

    private var loggedIn by mutableStateOf(false)
    /** وقتی از اعلان آپدیت می‌آییم: صفحه همان اپ باز شود */
    private var deepLinkAppId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.loadTheme(this)
        loggedIn = AuthManager.hasSession(this)
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

/** صفحات اصلی برای انیمیشن گذار */
private sealed interface Screen {
    data object Tabs : Screen
    data object Admin : Screen
    data class Detail(val app: com.idol.store.data.StoreApp) : Screen
    data class Category(val key: String) : Screen
    data class Legal(val page: String) : Screen
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

    // صفحات حقوقی و درباره
    var legalPage by remember { mutableStateOf<String?>(null) }

    // صفحه فعلی برای انیمیشن نرم بین صفحات
    val screen: Screen = when {
        legalPage != null -> Screen.Legal(legalPage!!)
        vm.adminMode -> Screen.Admin
        vm.selectedApp != null -> Screen.Detail(vm.selectedApp!!)
        vm.openCategory != null -> Screen.Category(vm.openCategory!!)
        else -> Screen.Tabs
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            // ورود نرم: بالا آمدن محو با فنر؛ خروج سریع محو
            (slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it / 10 } +
                fadeIn(spring(dampingRatio = 0.85f, stiffness = 380f)))
                .togetherWith(fadeOut(tween(120)))
        },
        label = "screenTransition",
    ) { scr ->
        when (scr) {
            is Screen.Legal -> when (scr.page) {
                "privacy" -> PrivacyScreen(onBack = { legalPage = null })
                "terms" -> TermsScreen(onBack = { legalPage = null })
                else -> AboutScreen(onBack = { legalPage = null })
            }
            is Screen.Admin -> AdminScreen(onBack = { vm.adminMode = false; vm.refresh() })
            is Screen.Detail -> AppDetailScreen(vm = vm, app = scr.app, onBack = { vm.selectedApp = null })
            is Screen.Category -> CategoryAppsScreen(vm = vm, key = scr.key, onBack = { vm.openCategory = null })
            Screen.Tabs -> StoreTabs(vm, onLogout = onLogout, onOpenLegal = { legalPage = it })
        }
    }

    // گیت امنیتی: روی همه‌ی صفحات بالا می‌آید
    SecurityGateSheet(vm)
}
