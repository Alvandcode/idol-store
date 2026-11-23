package com.idol.store.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idol.store.BuildConfig
import com.idol.store.data.AdminAccess
import com.idol.store.data.AuthManager
import com.idol.store.data.Catalog
import com.idol.store.data.CatalogRepository
import com.idol.store.data.GitHubService
import com.idol.store.data.StoreApp
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * پنل مدیریت کاتالوگ: افزودن/ویرایش/حذف اپ‌ها و تعیین مدیران،
 * و انتشار با کامیت مستقیم در ریپوی کاتالوگ از طریق GitHub API.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val gh = GitHubService { AuthManager.token(application) }

    var allowed by mutableStateOf(true)
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var globalError by mutableStateOf<String?>(null)
        private set
    var currentLogin by mutableStateOf<String?>(null)
        private set

    var apps by mutableStateOf<List<StoreApp>>(emptyList())
        private set
    var admins by mutableStateOf("")
    var categories by mutableStateOf("")

    /** null = صفحه لیست؛ StoreApp = در حال ویرایش آن اپ */
    var editing by mutableStateOf<StoreApp?>(null)
        private set
    var editingNew by mutableStateOf(false)
        private set

    private var login: String? = null

    init {
        reload()
    }

    fun reload() {
        loading = true
        globalError = null
        message = null
        viewModelScope.launch {
            try {
                // اگر توکن از قبل ذخیره شده ولی نام کاربری ندارد، یک‌بار بگیر
                login = AuthManager.login(getApplication())
                currentLogin = login
                if (login == null) {
                    AuthManager.token(getApplication())?.let { tok ->
                        try {
                            val user = gh.validateToken(tok)
                            AuthManager.saveSession(getApplication(), tok, user.login)
                            login = user.login
                        } catch (_: Exception) {
                        }
                    }
                }

                val catalog = CatalogRepository.load(getApplication(), forceRefresh = true)
                apps = catalog.apps
                admins = catalog.admins.joinToString(", ")
                categories = catalog.categories.joinToString(", ")
                allowed = AdminAccess.isAllowed(catalog, login, BuildConfig.CATALOG_REPO)
                if (!allowed) {
                    globalError = "شما در لیست مدیران کاتالوگ نیستید."
                }
            } catch (e: Exception) {
                globalError = e.message ?: "خطا در بارگذاری"
            } finally {
                loading = false
            }
        }
    }

    fun openEditor(app: StoreApp?) {
        editingNew = app == null
        editing = app ?: StoreApp(id = "", name = "", github = "")
    }

    fun closeEditor() {
        editing = null
        editingNew = false
    }

    /** ذخیره اپ در لیست محلی (هنوز منتشر نشده) */
    fun upsertApp(app: StoreApp) {
        val existingIndex = apps.indexOfFirst { it.id == app.id }
        apps = if (existingIndex >= 0) {
            apps.toMutableList().apply { set(existingIndex, app) }
        } else apps + app
        closeEditor()
        message = "«${app.name}» به لیست اضافه شد — برای اعمال روی گیت‌هاب دکمه «انتشار» را بزن."
    }

    fun deleteApp(app: StoreApp) {
        apps = apps.filterNot { it.id == app.id }
        message = "«${app.name}» حذف شد — برای اعمال روی گیت‌هاب دکمه «انتشار» را بزن."
    }

    /** انتشار نهایی: کامیت apps.json در ریپوی کاتالوگ */
    fun publish() {
        if (saving) return
        if (BuildConfig.CATALOG_REPO.isBlank() || BuildConfig.CATALOG_REPO.contains("USERNAME")) {
            message = "ابتدا CATALOG_REPO را در app/build.gradle.kts تنظیم کنید."
            return
        }
        val token = AuthManager.token(getApplication())
        if (token.isNullOrBlank()) {
            message = "توکن گیت‌هاب یافت نشد — دوباره وارد شوید."
            return
        }
        // دفاع دوم: دسترسی را همین‌جا هم چک می‌کنیم (مهم‌ترین حفاظ، خود گیت‌هاب است)
        if (!AdminAccess.isAllowed(
                Catalog(apps = apps, admins = admins.split(",").map { it.trim() }.filter { it.isNotEmpty() }),
                login,
                BuildConfig.CATALOG_REPO,
            )
        ) {
            message = "شما اجازه انتشار ندارید."
            return
        }

        saving = true
        message = null
        viewModelScope.launch {
            try {
                val catalog = Catalog(
                    apps = apps,
                    admins = admins.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    categories = categories.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    updatedAt = LocalDate.now().toString(),
                )
                val json = kotlinx.serialization.json.Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }.encodeToString(Catalog.serializer(), catalog)

                val loginName = login ?: ""
                val commitMessage = "Idol Store admin ($loginName): update apps.json"
                GitHubService.commitCatalogFile(
                    repo = BuildConfig.CATALOG_REPO,
                    path = BuildConfig.CATALOG_PATH,
                    message = commitMessage,
                    content = json,
                    token = token,
                )
                message = "✅ منتشر شد (کامیت در ${BuildConfig.CATALOG_REPO})"
            } catch (e: Exception) {
                message = "خطا در انتشار: ${e.message}"
            } finally {
                saving = false
            }
        }
    }
}
