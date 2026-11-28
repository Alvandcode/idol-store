package com.idol.store.ui

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idol.store.BuildConfig
import com.idol.store.R
import com.idol.store.data.AdminAccess
import com.idol.store.data.ApkDownloader
import com.idol.store.data.AuthManager
import com.idol.store.data.Catalog
import com.idol.store.data.CatalogRepository
import com.idol.store.data.GhAsset
import com.idol.store.data.GhRelease
import com.idol.store.data.GitHubService
import com.idol.store.data.InstalledPkgMap
import kotlinx.serialization.encodeToString
import com.idol.store.data.Installer
import com.idol.store.data.SecurityGateStatus
import com.idol.store.data.SecurityRepository
import com.idol.store.data.SecurityResult
import com.idol.store.data.StoreErrorKind
import com.idol.store.data.StoreException
import com.idol.store.data.StoreApp
import com.idol.store.data.downloadKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/** وضعیت گرفتن ریلیزهای هر اپ از گیت‌هاب */

/** درخواست دانلود منتظر نتیجه اسکن و تایید کاربر */
data class SecurityGateRequest(
    val app: com.idol.store.data.StoreApp,
    val tag: String,
    val asset: GhAsset,
)

sealed interface AppContentState {
    data object Loading : AppContentState

    data class Ready(
        val repo: String,
        val releases: List<GhRelease>,
        /** APK یونیورسال آخرین ریلیز پایدار */
        val universalApk: GhAsset,
        val starred: Boolean?,
        /** توضیحات/وب‌سایت ریپو از گیت‌هاب (برای صفحه اختصاصی) */
        val repoInfo: com.idol.store.data.GhRepoInfo? = null,
    ) : AppContentState

    data class Error(val message: String) : AppContentState
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val gh = GitHubService { AuthManager.token(application) }

    var catalog by mutableStateOf<Catalog?>(null)
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var selectedApp by mutableStateOf<StoreApp?>(null)
    /** صفحه فهرست یک دسته (یا "__recent__" برای بروزرسانی‌های اخیر) */
    var openCategory by mutableStateOf<String?>(null)

    /** پنل مدیریت */
    var adminMode by mutableStateOf(false)
    var isAdmin by mutableStateOf(false)
        private set

    /** پروژه‌ای که سازنده از هر صفحه‌ای برای ویرایش انتخاب کرده — مصرف در صفحه پنل */
    var manageRequest by mutableStateOf<com.idol.store.data.StoreApp?>(null)
        private set

    /** خواندن و پاک کردن درخواست ویرایش (یک‌بار مصرف) */
    fun consumeManageRequest(): com.idol.store.data.StoreApp? {
        val r = manageRequest
        manageRequest = null
        return r
    }

    /** ویرایش از هر صفحه: پنل مدیریت با ادیتور همان پروژه باز می‌شود */
    fun requestEditFromAnywhere(app: com.idol.store.data.StoreApp) {
        if (!isAdmin) return
        manageRequest = app
        selectedApp = null
        openCategory = null
        adminMode = true
    }

    /** حذف مستقیم پروژه از کاتالوگ (سازنده/ادمین manage_apps، از هر صفحه‌ای) */
    var catalogSaving by mutableStateOf(false)
        private set

    fun deleteFromCatalog(app: com.idol.store.data.StoreApp, onDone: () -> Unit = {}) {
        val ctx = getApplication<Application>()
        val cat = catalog ?: return
        if (!isAdmin || catalogSaving) return
        viewModelScope.launch {
            catalogSaving = true
            try {
                val updated = cat.copy(apps = cat.apps.filterNot { it.id == app.id })
                val json = kotlinx.serialization.json.Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }.encodeToString(com.idol.store.data.Catalog.serializer(), updated)
                GitHubService.commitCatalogFile(
                    repo = BuildConfig.CATALOG_REPO,
                    path = BuildConfig.CATALOG_PATH,
                    message = ctx.getString(R.string.admin_commit_msg, currentLogin ?: "owner"),
                    content = json,
                    token = AuthManager.token(ctx) ?: "",
                )
                selectedApp = null
                refresh()
            } catch (e: Exception) {
                catalogError = AppErrors.message(ctx, e)
            } finally {
                catalogSaving = false
                onDone()
            }
        }
    }

    var catalogError by mutableStateOf<String?>(null)
        private set

    fun clearCatalogError() { catalogError = null }
    var currentLogin by mutableStateOf<String?>(null)
        private set

    val downloadStates = ApkDownloader.states

    private val _content = MutableStateFlow<Map<String, AppContentState>>(emptyMap())
    val content: StateFlow<Map<String, AppContentState>> = _content.asStateFlow()

    private val gate = Semaphore(6)

    init {
        refresh()
    }

    fun refresh() {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                // کاربر عادی (ایمیل + توکن) یا مدیر گیت‌هاب؟
                val app = getApplication<Application>()
                currentLogin = AuthManager.userEmail(app) ?: AuthManager.login(app)
                if (AuthManager.userEmail(app) == null && currentLogin == null) {
                    AuthManager.token(app)?.let { tok ->
                        try {
                            val user = gh.validateToken(tok)
                            AuthManager.saveSession(app, tok, user.login)
                            currentLogin = user.login
                        } catch (_: Exception) {
                        }
                    }
                }

                val catalog = CatalogRepository.load(getApplication(), forceRefresh = true)
                this@StoreViewModel.catalog = catalog
                // ادمین فقط با نشست گیت‌هاب (نه نشست ایمیل کاربر عادی)
                isAdmin = AuthManager.hasToken(app) &&
                    AdminAccess.isAllowed(catalog, currentLogin, BuildConfig.CATALOG_REPO)
                // دفاع اصلی: تأیید دسترسی push از خود گیت‌هاب (نام کاربری قابل جعل نیست،
                // چون از API گیت‌هاب با توکن واقعی گرفته می‌شود — ولی برای اطمینان کامل،
                // فقط کسی ادمین است که گیت‌هاب دسترسی push او به ریپوی کاتالوگ را تأیید کند)
                if (isAdmin) {
                    isAdmin = try {
                        gh.hasPushAccess(BuildConfig.CATALOG_REPO, currentLogin!!)
                    } catch (_: Exception) {
                        false
                    }
                }
                loadReleases()
            } catch (e: Exception) {
                error = AppErrors.message(getApplication(), e)
            } finally {
                loading = false
            }
        }
    }

    /**
     * stale-while-revalidate: داده‌های آماده حفظ می‌شوند و فقط نبودها Loading می‌گیرند —
     * نتیجه: برگشت به استور فوری است و رفرش صفحه را سفید/اسپینر نمی‌کند.
     */
    private fun loadReleases() {
        val apps = catalog?.apps.orEmpty()
        _content.update { cur ->
            apps.associate { app ->
                val existing = cur[app.id]
                app.id to (if (existing is AppContentState.Ready) existing else AppContentState.Loading as AppContentState)
            }
        }
        apps.forEach { app -> fetchApp(app) }
    }

    fun retryApp(app: StoreApp) {
        _content.update { it + (app.id to AppContentState.Loading as AppContentState) }
        fetchApp(app)
    }

    private fun fetchApp(app: StoreApp) {
        viewModelScope.launch {
            gate.withPermit {
                try {
                    val repo = GitHubService.repoFromUrl(app.github)
                        ?: throw StoreException(StoreErrorKind.INVALID_REPO_URL, app.github)
                    val allReleases = gh.stableReleases(repo)
                    if (allReleases.isEmpty()) throw StoreException(StoreErrorKind.NO_STABLE_RELEASE)
                    // ریلیزهایی که APK قابل نصب روی معماری این دستگاه ندارند، برای این کاربر کنار گذاشته می‌شوند.
                    val releases = allReleases.filter {
                        GitHubService.pickCompatibleApk(it.assets) != null
                    }
                    if (releases.isEmpty()) throw StoreException(StoreErrorKind.NO_UNIVERSAL_APK)
                    val latest = releases.first()
                    val apk = GitHubService.pickCompatibleApk(latest.assets)
                        ?: throw StoreException(StoreErrorKind.NO_UNIVERSAL_APK)
                    // ستاره و اطلاعات ریپو به‌موازات هم گرفته می‌شوند — نه پشت‌سرهم
                    val scope: Pair<Boolean?, com.idol.store.data.GhRepoInfo?> = coroutineScope {
                        val starredD = async { try { gh.isStarred(repo) } catch (_: Exception) { null } }
                        val infoD = async { try { gh.repoDetails(repo) } catch (_: Exception) { null } }
                        Pair(starredD.await(), infoD.await())
                    }
                    _content.update {
                        it + (app.id to AppContentState.Ready(repo, releases, apk, scope.first, scope.second))
                    }
                } catch (e: Exception) {
                    _content.update {
                        it + (app.id to AppContentState.Error(
                            AppErrors.message(getApplication(), e)
                        ))
                    }
                }
            }
        }
    }

    fun toggleStar(app: StoreApp) {
        val current = _content.value[app.id] as? AppContentState.Ready ?: return
        val newStarred = current.starred != true
        viewModelScope.launch {
            val ok = try { gh.setStarred(current.repo, newStarred) } catch (_: Exception) { false }
            if (ok) {
                _content.update {
                    val ready = it[app.id] as? AppContentState.Ready ?: return@update it
                    it + (app.id to ready.copy(starred = newStarred))
                }
            }
        }
    }

    fun download(app: StoreApp, tag: String, asset: GhAsset) {
        val safeId = app.id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        ApkDownloader.start(
            getApplication(),
            key = downloadKey(app.id, tag),
            url = asset.downloadUrl,
            fileName = "$safeId-$tag.apk",
        )
    }

    // ---------- گیت امنیتی: اسکن خودکار قبل از هر دانلود/نصب ----------

    private val _security = MutableStateFlow<Map<String, SecurityResult>>(emptyMap())
    val securityStates: StateFlow<Map<String, SecurityResult>> = _security.asStateFlow()

    private val _securityLoading = MutableStateFlow(false)
    val securityLoading: StateFlow<Boolean> = _securityLoading.asStateFlow()

    /** دانلود در انتظار تایید کاربر پس از دیدن نتیجه اسکن */
    private val _securityGate = MutableStateFlow<SecurityGateRequest?>(null)
    val securityGate: StateFlow<SecurityGateRequest?> = _securityGate.asStateFlow()

    /** لود یک‌باره گزارش اسکن (در باز شدن صفحه اپ و شروع دانلود) */
    fun ensureSecurityLoaded() {
        if (_securityLoading.value || _security.value.isNotEmpty()) return
        _securityLoading.value = true
        viewModelScope.launch {
            try {
                _security.value = SecurityRepository.load().associateBy { it.id }
            } catch (_: Exception) {
            } finally {
                _securityLoading.value = false
            }
        }
    }

    /** به‌جای دانلود مستقیم: اول گیت امنیتی باز می‌شود */
    fun requestDownload(app: StoreApp, tag: String, asset: GhAsset) {
        ensureSecurityLoaded()
        _securityGate.value = SecurityGateRequest(app, tag, asset)
    }

    /** تایید کاربر پس از دیدن نتیجه اسکن → دانلود */
    fun confirmGateDownload() {
        val g = _securityGate.value ?: return
        _securityGate.value = null
        download(g.app, g.tag, g.asset)
    }

    /** کاربر دانلود را رد کرد */
    fun cancelGate() {
        _securityGate.value = null
    }

    fun cancel(key: String) = ApkDownloader.cancel(key)

    fun install(file: File) = Installer.install(getApplication(), file)

    /** اگر اپ با این پکیج‌نیم روی دستگاه نصب باشد، versionCode نصب‌شده را برمی‌گرداند */
    fun installedVersionCode(packageName: String?): Int? {
        if (packageName.isNullOrBlank()) return null
        return try {
            val pm = getApplication<Application>().packageManager
            val info = pm.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
            else @Suppress("DEPRECATION") info.versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * کش اسکن پکیج‌های نصب‌شده — سنگین‌ترین کار (getInstalledPackages) فقط یک‌بار در هر تیک
     * اجرا می‌شود، نه در هر بار ترسیم هر ردیف. این، لگ اسکرول را از بین می‌برد.
     */
    private var pkgSnapshotTick = -1
    private var pkgSnapshot: Map<String, String?> = emptyMap() // lowercase pkg → versionName

    private fun installedPackagesSnapshot(): Map<String, String?> {
        if (pkgSnapshotTick != installTick) {
            pkgSnapshotTick = installTick
            pkgSnapshot = try {
                getApplication<Application>().packageManager
                    .getInstalledPackages(0)
                    .associate { it.packageName.lowercase() to it.versionName }
            } catch (_: Exception) {
                emptyMap()
            }
        }
        return pkgSnapshot
    }

    /** versionName نصب‌شده برای مقایسه دقیق با تگ ریلیز */
    fun installedVersionName(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return installedPackagesSnapshot()[packageName.lowercase()]
    }

    /**
     * شناسایی اپ نصب‌شده با سه لایه:
     * ۱) نگاشت ذخیره‌شده از خود APK دانلودشده (مطمئن‌ترین)
     * ۲) packageName کاتالوگ
     * ۳) اسکن بی‌حساسیت به حروف بین پکیج‌های نصب‌شده
     */
    fun installedVersionName(app: com.idol.store.data.StoreApp?): String? {
        if (app == null) return null
        val map = installedPackagesSnapshot()
        val candidates = listOfNotNull(
            InstalledPkgMap.get(getApplication(), app.id),
            app.packageName?.takeIf { it.isNotBlank() },
            app.id.takeIf { it.isNotBlank() },
        ).filter { it.isNotBlank() }.distinct()
        candidates.forEach { pkg ->
            map[pkg.lowercase()]?.let { return it }
        }
        return null
    }

    /** با هر برگشت به اپ (مثلاً بعد از پایان نصب سیستم) وضعیت نصب دوباره چک می‌شود */
    var installTick by mutableStateOf(0)
        private set

    fun notifyResumed() {
        installTick++
    }

    fun logout(onDone: () -> Unit) {
        AuthManager.clear(getApplication())
        onDone()
    }

    val filteredApps: List<StoreApp>
        get() {
            val apps = catalog?.apps.orEmpty()
            val q = query.trim().lowercase()
            return if (q.isEmpty()) apps else apps.filter {
                it.name.lowercase().contains(q) ||
                    (it.developer ?: "").lowercase().contains(q) ||
                    (it.category ?: "").lowercase().contains(q)
            }
        }

    /** مرتب‌شده بر اساس جدیدترین ریلیز پایدار (برای سکشن بروزرسانی‌های اخیر) */
    val recentApps: List<StoreApp>
        get() {
            val map = _content.value
            return catalog?.apps.orEmpty()
                .filter { map[it.id] is AppContentState.Ready }
                .sortedByDescending { app ->
                    (map[app.id] as? AppContentState.Ready)?.releases?.firstOrNull()?.publishedAt.orEmpty()
                }
        }

    fun appsIn(category: String): List<StoreApp> =
        catalog?.apps.orEmpty().filter { it.category == category }
}
