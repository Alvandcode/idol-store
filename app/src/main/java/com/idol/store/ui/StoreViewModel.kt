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
import com.idol.store.data.AdminAccess
import com.idol.store.data.ApkDownloader
import com.idol.store.data.AuthManager
import com.idol.store.data.Catalog
import com.idol.store.data.CatalogRepository
import com.idol.store.data.GhAsset
import com.idol.store.data.GhRelease
import com.idol.store.data.GitHubService
import com.idol.store.data.Installer
import com.idol.store.data.StoreErrorKind
import com.idol.store.data.StoreException
import com.idol.store.data.StoreApp
import com.idol.store.data.downloadKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/** وضعیت گرفتن ریلیزهای هر اپ از گیت‌هاب */
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
                // اگر توکن قبلاً ذخیره شده ولی نام کاربری ندارد، یک‌بار بگیر
                currentLogin = AuthManager.login(getApplication())
                if (currentLogin == null) {
                    AuthManager.token(getApplication())?.let { tok ->
                        try {
                            val user = gh.validateToken(tok)
                            AuthManager.saveSession(getApplication(), tok, user.login)
                            currentLogin = user.login
                        } catch (_: Exception) {
                        }
                    }
                }

                val catalog = CatalogRepository.load(getApplication(), forceRefresh = true)
                this@StoreViewModel.catalog = catalog
                isAdmin = AdminAccess.isAllowed(catalog, currentLogin, BuildConfig.CATALOG_REPO)
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

    /** برای هر اپ، آخرین ریلیز پایدار + APK یونیورسال + وضعیت ستاره را می‌گیرد */
    private fun loadReleases() {
        val apps = catalog?.apps.orEmpty()
        _content.value = apps.associate { it.id to AppContentState.Loading as AppContentState }
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
                    val releases = gh.stableReleases(repo)
                    if (releases.isEmpty()) throw StoreException(StoreErrorKind.NO_STABLE_RELEASE)
                    val latest = releases.first()
                    val apk = GitHubService.pickUniversalApk(latest.assets)
                        ?: throw StoreException(StoreErrorKind.NO_UNIVERSAL_APK)
                    val starred = try { gh.isStarred(repo) } catch (_: Exception) { null }
                    val repoInfo = try { gh.repoDetails(repo) } catch (_: Exception) { null }
                    _content.update {
                        it + (app.id to AppContentState.Ready(repo, releases, apk, starred, repoInfo))
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
