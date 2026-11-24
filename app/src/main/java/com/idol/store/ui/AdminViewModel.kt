package com.idol.store.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idol.store.BuildConfig
import com.idol.store.R
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
            val ctx = getApplication<Application>()
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
                if (allowed && login != null) {
                    // تأیید نهایی از گیت‌هاب: فقط توکنِ دارای دسترسی push واقعی ادمین است
                    allowed = try {
                        gh.hasPushAccess(BuildConfig.CATALOG_REPO, login!!)
                    } catch (_: Exception) {
                        false
                    }
                }
                if (!allowed) {
                    globalError = ctx.getString(R.string.admin_no_access)
                }
            } catch (e: Exception) {
                globalError = AppErrors.message(ctx, e)
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
        val ctx = getApplication<Application>()
        val existingIndex = apps.indexOfFirst { it.id == app.id }
        apps = if (existingIndex >= 0) {
            apps.toMutableList().apply { set(existingIndex, app) }
        } else apps + app
        closeEditor()
        message = ctx.getString(R.string.admin_upsert_fmt, app.name)
    }

    fun deleteApp(app: StoreApp) {
        apps = apps.filterNot { it.id == app.id }
        message = getApplication<Application>().getString(R.string.admin_deleted_fmt, app.name)
    }

    /** انتشار نهایی: کامیت apps.json در ریپوی کاتالوگ */
    fun publish() {
        val ctx = getApplication<Application>()
        if (saving) return
        if (BuildConfig.CATALOG_REPO.isBlank() || BuildConfig.CATALOG_REPO.contains("USERNAME")) {
            message = ctx.getString(R.string.admin_repo_not_set)
            return
        }
        val token = AuthManager.token(getApplication())
        if (token.isNullOrBlank()) {
            message = ctx.getString(R.string.admin_no_token)
            return
        }
        // دفاع نهایی قبل از هر کامیت: تأیید مستقیم از گیت‌هاب
        val verified = try {
            gh.hasPushAccess(BuildConfig.CATALOG_REPO, login ?: "")
        } catch (_: Exception) {
            false
        }
        if (!verified) {
            message = ctx.getString(R.string.admin_publish_denied_gh)
            return
        }
        // دفاع دوم: دسترسی را همین‌جا هم چک می‌کنیم (مهم‌ترین حفاظ، خود گیت‌هاب است)
        if (!AdminAccess.isAllowed(
                Catalog(apps = apps, admins = admins.split(",").map { it.trim() }.filter { it.isNotEmpty() }),
                login,
                BuildConfig.CATALOG_REPO,
            )
        ) {
            message = ctx.getString(R.string.admin_publish_denied)
            return
        }

        saving = true
        message = null
        viewModelScope.launch {
            try {                val catalog = Catalog(
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
                val commitMessage = ctx.getString(R.string.admin_commit_msg, loginName)
                GitHubService.commitCatalogFile(
                    repo = BuildConfig.CATALOG_REPO,
                    path = BuildConfig.CATALOG_PATH,
                    message = commitMessage,
                    content = json,
                    token = token,
                )
                message = ctx.getString(R.string.admin_publish_ok, BuildConfig.CATALOG_REPO)
            } catch (e: Exception) {
                message = ctx.getString(R.string.admin_publish_fail, AppErrors.message(ctx, e))
            } finally {
                saving = false
            }
        }
    }
}
