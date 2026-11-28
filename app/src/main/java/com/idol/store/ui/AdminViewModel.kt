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
import com.idol.store.data.AdminEntry
import java.net.URLEncoder
import com.idol.store.data.AdminPermissions
import com.idol.store.data.AdminRegistry
import com.idol.store.data.AdminRegistryStore
import com.idol.store.data.AuthManager
import com.idol.store.data.Catalog
import com.idol.store.data.CatalogRepository
import com.idol.store.data.GitHubService
import com.idol.store.data.StoreApp
import kotlinx.coroutines.launch

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

    // ---------- سیستم نقش‌ها و تایید مالک ----------

    /** مالک سازنده — همه دسترسی‌ها، بدون نیاز به کد */
    var isOwner by mutableStateOf(false)
        private set
    /** دسترسی‌های این ادمین؛ خالی = فقط وارد شده ولی هیچ دسترسی ندارد */
    var permissions by mutableStateOf<Set<String>>(emptySet())
        private set
    /** لاگین دارای push است ولی هنوز در لیست ادمین‌ها نیست → در انتظار تایید مالک */
    var awaitingApproval by mutableStateOf(false)
        private set
    /** درخواست با issue برای مالک ارسال شد */
    var requestSent by mutableStateOf(false)
        private set

    var registry by mutableStateOf<AdminRegistry?>(null)
        private set
    var registrySaving by mutableStateOf(false)
        private set
    var pendingRequests by mutableStateOf<List<Pair<String, Int>>>(emptyList())
        private set

    val canManageApps: Boolean
        get() = allowed && (isOwner || AdminPermissions.MANAGE_APPS in permissions)

    var apps by mutableStateOf<List<StoreApp>>(emptyList())
        private set
    var admins by mutableStateOf("")
    var categories by mutableStateOf("")

    /** نتیجه «دریافت خودکار از گیت‌هاب»: اپ پیش‌پرشده */
    var fetchedApp by mutableStateOf<StoreApp?>(null)
        private set
    /** آخرین نسخه پایدار ریپویی که دریافت خودکار شد (برای نمایش) */
    var fetchedLatestTag by mutableStateOf<String?>(null)
        private set
    var fetching by mutableStateOf(false)
        private set
    var fetchError by mutableStateOf<String?>(null)
        private set

    /** لیست دسته‌ها به‌صورت تمیز از رشته کاما-جدا */
    fun categoryList(): List<String> =
        categories.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun addCategory(name: String) {
        val n = name.trim()
        if (n.isEmpty() || categoryList().any { it.equals(n, ignoreCase = true) }) return
        categories = (categoryList() + n).joinToString(", ")
    }

    fun removeCategory(name: String) {
        categories = categoryList().filterNot { it.equals(name, ignoreCase = true) }.joinToString(", ")
    }

    /** دریافت خودکار اطلاعات پروژه از گیت‌هاب: نام، توضیح، سازنده، آیکون، وب‌سایت، آخرین نسخه */
    fun autoFill(raw: String) {
        val ctx = getApplication<Application>()
        val repo = GitHubService.repoFromUrl(raw)
        if (repo == null) {
            fetchError = ctx.getString(R.string.form_err_github)
            return
        }
        if (fetching) return
        fetching = true
        fetchError = null
        viewModelScope.launch {
            try {
                val info = gh.repoDetails(repo)
                val latest = try {
                    gh.stableReleases(repo).firstOrNull()
                } catch (_: Exception) {
                    null // ریپو هنوز ریلیز ندارد؛ بقیه اطلاعات را می‌گیریم
                }
                // تشخیص خودکار: پروژه بدون APK یونیورسال → اخطار و اضافه نشدن
                val universalApk = latest?.assets?.let { GitHubService.pickUniversalApk(it) }
                if (universalApk == null) {
                    fetchError = ctx.getString(R.string.form_err_no_apk)
                    return@launch
                }
                fetchedApp = StoreApp(
                    id = repo.substringAfter('/').lowercase(),
                    name = info.name ?: repo.substringAfter('/'),
                    developer = info.owner?.login ?: repo.substringBefore('/'),
                    description = info.description,
                    iconUrl = info.owner?.avatarUrl,
                    github = repo,
                    website = info.homepage?.takeIf { it.isNotBlank() },
                )
                fetchedLatestTag = latest?.tagName
            } catch (e: Exception) {
                fetchError = AppErrors.message(ctx, e)
            } finally {
                fetching = false
            }
        }
    }

    /** بعد از اعمال نتیجه در فرم، برای جلوگیری از پرشدن مجدد، پاک می‌شود (تگ برای نمایش می‌ماند) */
    fun consumeFetched() {
        fetchedApp = null
        fetchError = null
    }

    /** آخرین نسخه نمایشی را هم پاک می‌کند (هنگام بستن فرم) */
    fun clearLatestTag() {
        fetchedLatestTag = null
    }

    fun clearFetchError() {
        fetchError = null
    }

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

                // ---------- نقش‌بندی جدید: مالک / ادمین تایید‌شده / در انتظار تایید ----------
                registry = AdminRegistryStore.load(BuildConfig.CATALOG_REPO)
                val reg = registry
                val ownerLogin = reg?.owner?.takeIf { it.isNotBlank() }
                    ?: AdminAccess.catalogOwner(BuildConfig.CATALOG_REPO)
                isOwner = login != null && ownerLogin != null && ownerLogin.equals(login, ignoreCase = true)
                val entry = if (login != null) reg?.admins?.firstOrNull {
                    it.login.equals(login, ignoreCase = true)
                } else null

                // تأیید نهایی از گیت‌هاب: فقط توکنِ دارای دسترسی push واقعی
                val pushOk = if (login != null) try {
                    gh.hasPushAccess(BuildConfig.CATALOG_REPO, login!!)
                } catch (_: Exception) {
                    false
                } else false

                if (isOwner) {
                    // مالک: همه‌چیز، بدون کد
                    permissions = AdminPermissions.ALL.toSet()
                    allowed = pushOk
                } else if (entry != null && pushOk) {
                    // ادمین تاییدشده توسط مالک: ورود همیشگی با دسترسی‌های تعیین‌شده
                    allowed = true
                    awaitingApproval = false
                    permissions = entry.permissions.toSet()
                } else if (pushOk) {
                    // دسترسی push دارد ولی ادمین نیست: در انتظار تایید و تعیین دسترسی مالک
                    allowed = false
                    awaitingApproval = true
                    permissions = emptySet()
                } else {
                    allowed = false
                }
                if (!allowed && !awaitingApproval) {
                    globalError = ctx.getString(R.string.admin_no_access)
                }
            } catch (e: Exception) {
                globalError = AppErrors.message(ctx, e)
            } finally {
                loading = false
            }
        }
    }

    /**
     * لینک تایید برای مالک: باز کردنش یک issue آماده «[admin-approve]» می‌سازد؛
     * ربات کاتالوگ همان را پردازش می‌کند و ادمین را با دسترسی‌ها ثبت می‌کند.
     */
    fun approvalLink(loginName: String, perms: List<String> = listOf(AdminPermissions.MANAGE_APPS)): String {
        val repo = BuildConfig.REGISTRY_ISSUE_REPO
        val title = "[admin-approve] login=$loginName perms=${perms.joinToString(",")}"
        val body = "Approve admin $loginName with: ${perms.joinToString(", ")}\n(درخواست اولیه: [admin-access] $loginName)"
        val q = "?title=" + URLEncoder.encode(title, "UTF-8") +
            "&body=" + URLEncoder.encode(body, "UTF-8")
        return "https://github.com/$repo/issues/new$q"
    }

    /** ارسال درخواست ورود: issue در ریپوی رجیستری → مالک اعلان ایمیلی می‌گیرد */
    fun requestApproval() {
        val l = login ?: return
        viewModelScope.launch {
            try {
                val link = approvalLink(l)
                val (ok, num) = GitHubService.createIssue(
                    repo = BuildConfig.REGISTRY_ISSUE_REPO,
                    title = "[admin-access] $l",
                    body = "Admin login request from device app.\n\nlogin: $l\n\n" +
                        "**--- تایید (برای سازنده) ---**\n" +
                        "لینک تایید با دسترسی «مدیریت اپ»:\n\n$link\n\n" +
                        "بعد از باز کردن لینک و ثبت issue، ربات ادمین را اضافه می‌کند و دستگاه ادمین خودکار وارد می‌شود.",
                    token = AuthManager.token(getApplication()) ?: "",
                )
                requestSent = ok
                if (ok) requestIssueNumber = num
            } catch (_: Exception) {
                requestSent = false
            }
        }
        startApprovalPolling()
    }

    /** در انتظار تایید: هر ۱۵ ثانیه admins.json چک می‌شود؛ با تایید، پنل باز می‌شود */
    private fun startApprovalPolling() {
        if (approvalPolling) return
        approvalPolling = true
        viewModelScope.launch {
            while (awaitingApproval) {
                kotlinx.coroutines.delay(15_000)
                if (!awaitingApproval) break
                try {
                    val reg = AdminRegistryStore.load(BuildConfig.CATALOG_REPO)
                    val entry = reg?.admins?.firstOrNull {
                        it.login.equals(login, ignoreCase = true)
                    }
                    if (entry != null) {
                        // تایید شد: issue درخواست را هم ببند
                        requestIssueNumber?.let { n ->
                            runCatching {
                                GitHubService.closeIssue(
                                    BuildConfig.REGISTRY_ISSUE_REPO,
                                    n,
                                    AuthManager.token(getApplication()) ?: "",
                                )
                            }
                        }
                        requestIssueNumber = null
                        registry = reg
                        isOwner = false
                        permissions = entry.permissions.toSet()
                        awaitingApproval = false
                        allowed = true
                        message = getApplication<Application>().getString(R.string.admin_login_ok)
                    }
                } catch (_: Exception) {
                }
            }
            approvalPolling = false
        }
    }

    private var approvalPolling = false
    var requestIssueNumber by mutableStateOf<Int?>(null)
        private set

    /** درخواست‌های باز ورود ادمین — فقط برای مالک */
    fun loadPendingRequests() {
        if (!isOwner) return
        viewModelScope.launch {
            try {
                pendingRequests = GitHubService.openAdminRequests(
                    BuildConfig.REGISTRY_ISSUE_REPO,
                    AuthManager.token(getApplication()),
                )
            } catch (_: Exception) {
                pendingRequests = emptyList()
            }
        }
    }

    /** افزودن ادمین با دسترسی‌های دلخواه (فقط مالک) — کامیت فوری admins.json */
    fun addAdmin(loginName: String, perms: List<String>) {
        val ctx = getApplication<Application>()
        if (!isOwner || registrySaving || loginName.isBlank()) return
        registrySaving = true
        viewModelScope.launch {
            try {
                val token = AuthManager.token(getApplication()) ?: error("no-token")
                val reg = registry ?: AdminRegistry(owner = AdminAccess.catalogOwner(BuildConfig.CATALOG_REPO) ?: "")
                val updated = if (reg.admins.any { it.login.equals(loginName, ignoreCase = true) }) {
                    reg.copy(admins = reg.admins.map {
                        if (it.login.equals(loginName, ignoreCase = true))
                            it.copy(permissions = perms, addedAt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
                        else it
                    })
                } else {
                    reg.copy(admins = reg.admins + AdminEntry(
                        login = loginName.trim(),
                        permissions = perms,
                        addedAt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                    ))
                }
                AdminRegistryStore.save(BuildConfig.CATALOG_REPO, updated, token)
                registry = updated
                // اگر خود این دستگاه همان ادمین است، دسترسی‌اش را به‌روز کن
                if (login?.equals(loginName, ignoreCase = true) == true) permissions = perms.toSet()
                message = ctx.getString(R.string.admin_registry_saved)
            } catch (e: Exception) {
                message = ctx.getString(R.string.admin_registry_fail, AppErrors.message(ctx, e))
            } finally {
                registrySaving = false
            }
        }
    }

    /** حذف ادمین (فقط مالک) */
    fun removeAdmin(loginName: String) {
        if (!isOwner || registrySaving) return
        val ctx = getApplication<Application>()
        registrySaving = true
        viewModelScope.launch {
            try {
                val token = AuthManager.token(getApplication()) ?: error("no-token")
                val reg = registry ?: return@launch
                val updated = reg.copy(admins = reg.admins.filterNot { it.login.equals(loginName, ignoreCase = true) })
                AdminRegistryStore.save(BuildConfig.CATALOG_REPO, updated, token)
                registry = updated
                message = ctx.getString(R.string.admin_registry_saved)
            } catch (e: Exception) {
                message = ctx.getString(R.string.admin_registry_fail, AppErrors.message(ctx, e))
            } finally {
                registrySaving = false
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
        consumeFetched()
        clearLatestTag()
    }

    /** ذخیره اپ در لیست محلی (هنوز منتشر نشده) — اپ جدید اول برای APK بررسی می‌شود */
    fun upsertApp(app: StoreApp) {
        val ctx = getApplication<Application>()
        val isNew = apps.indexOfFirst { it.id == app.id } < 0
        if (!isNew) {
            doUpsert(app)
            return
        }
        // تشخیص خودکار هنگام افزودن: بدون APK یونیورسال اضافه نمی‌شود
        viewModelScope.launch {
            try {
                val repo = GitHubService.repoFromUrl(app.github)
                val releases = repo?.let { gh.stableReleases(it) }.orEmpty()
                val apk = releases.firstOrNull()?.assets?.let { GitHubService.pickUniversalApk(it) }
                if (apk == null) {
                    message = ctx.getString(R.string.form_err_no_apk)
                } else {
                    doUpsert(app)
                }
            } catch (e: Exception) {
                message = ctx.getString(R.string.form_err_no_apk) + " (" + AppErrors.message(ctx, e) + ")"
            }
        }
    }

    private fun doUpsert(app: StoreApp) {
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
        // دفاع نهایی قبل از هر کامیت: تأیید مستقیم از گیت‌هاب (درون کوروتین)
        saving = true
        message = null
        viewModelScope.launch {
            try {
                val verified = try {
                    gh.hasPushAccess(BuildConfig.CATALOG_REPO, login ?: "")
                } catch (_: Exception) {
                    false
                }
                if (!verified) {
                    message = ctx.getString(R.string.admin_publish_denied_gh)
                    return@launch
                }

                if (!canManageApps) {
                    message = ctx.getString(R.string.admin_publish_denied)
                    return@launch
                }
                // دفاع سوم: رجیستری جدید ادمین‌ها — مالک یا ادمینِ دارای manage_apps
                val regOk = isOwner || registry?.admins?.any {
                    it.login.equals(login, ignoreCase = true) &&
                        AdminPermissions.MANAGE_APPS in it.permissions
                } == true
                if (!regOk) {
                    message = ctx.getString(R.string.admin_publish_denied)
                    return@launch
                }

                val catalog = Catalog(
                    apps = apps,
                    admins = admins.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    categories = categories.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
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
