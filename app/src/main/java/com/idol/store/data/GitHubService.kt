package com.idol.store.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.util.Base64 as AndroidBase64

/**
 * سرویس GitHub API:
 * - گرفتن ریلیزهای پایدار یک ریپو
 * - انتخاب APK یونیورسال از بین asset ها
 * - ستاره‌دادن / ستاره‌برداشتن
 */
class GitHubService(private val tokenProvider: () -> String?) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun get(url: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        client.newCall(builder.build()).execute().use { resp ->
            Pair(resp.code, resp.body?.string().orEmpty())
        }
    }

    private suspend fun mutation(method: String, url: String): Int = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        client.newCall(builder.method(method, null).build()).execute().use { it.code }
    }

    /** ریلیزهای پایدار (بدون draft و prerelease) — جدیدترین اول */
    suspend fun stableReleases(repo: String): List<GhRelease> {
        val (code, body) = get("https://api.github.com/repos/$repo/releases?per_page=30")
        return when (code) {
            200 -> json.decodeFromString<List<GhRelease>>(body)
                .filter { !it.draft && !it.prerelease }

            404 -> throw GitHubApiException(404, repo)
            403, 429 -> throw GitHubApiException(403, repo)
            401 -> throw GitHubApiException(401)
            else -> throw GitHubApiException(code, repo)
        }
    }

    suspend fun isStarred(repo: String): Boolean =
        get("https://api.github.com/user/starred/$repo").first == 204

    /**
     * آیا کاربرِ صاحب توکن، دسترسی push به ریپوی کاتالوگ دارد؟
     * منبع حقیقت همین‌جاست: گیت‌هاب خودش تأیید می‌کند — اسم قابل جعل نیست.
     */
    suspend fun hasPushAccess(repo: String, login: String): Boolean {
        val (code, _) = get("https://api.github.com/repos/$repo/collaborators/$login?permission=push")
        return code == 204
    }

    /** اطلاعات کلی ریپو: توضیحات، وب‌سایت، آدرس — برای صفحه اختصاصی هر پروژه */
    suspend fun repoDetails(repo: String): com.idol.store.data.GhRepoInfo {
        val (code, body) = get("https://api.github.com/repos/$repo")
        if (code != 200) throw GitHubApiException(code, repo)
        return json.decodeFromString<com.idol.store.data.GhRepoInfo>(body)
    }

    /** true = ستاره شد، false = ستاره برداشته شد */
    suspend fun setStarred(repo: String, starred: Boolean): Boolean {
        val code = if (starred) mutation("PUT", "https://api.github.com/user/starred/$repo")
        else mutation("DELETE", "https://api.github.com/user/starred/$repo")
        return code == 204
    }

    suspend fun validateToken(token: String): GhUser {
        val (code, body) = get("https://api.github.com/user")
        if (code != 200) throw GitHubApiException(code)
        return json.decodeFromString<GhUser>(body)
    }

    companion object {

        // ---------- پنل مدیریت: خواندن و کامیت کاتالوگ ----------

        @Serializable
        data class GhContents(
            val sha: String? = null,
            val encoding: String? = null,
            val content: String? = null,
        )

        @Serializable
        data class CommitRequest(
            val message: String,
            val content: String,
            val sha: String? = null,
        )

        @Serializable
        data class GhError(val message: String? = null)

        /** (sha، محتوای فعلی فایل) — برای ریپو/فایل ناموجود: (null، null) */
        suspend fun readCatalogFile(repo: String, path: String, token: String?): Pair<String?, String?> {
            val (code, body) = fetch(
                "https://api.github.com/repos/$repo/contents/$path", token
            )
            if (code == 404) return Pair(null, null)
            if (code != 200) throw GitHubApiException(code)
            val parsed = json.decodeFromString<GhContents>(body)
            val decoded = parsed.content
                ?.replace("\n", "")
                ?.let { String(AndroidBase64.decode(it, AndroidBase64.DEFAULT), Charsets.UTF_8) }
            return Pair(parsed.sha, decoded)
        }

        /** کامیت فایل کاتالوگ در ریپو؛ sha جدید را برمی‌گرداند */
        suspend fun commitCatalogFile(
            repo: String,
            path: String,
            message: String,
            content: String,
            token: String,
        ): String {
            val (sha, _) = readCatalogFile(repo, path, token)
            val payload = json.encodeToString(
                CommitRequest(message, AndroidBase64.encodeToString(content.toByteArray(Charsets.UTF_8), AndroidBase64.NO_WRAP), sha)
            )
            val (code, body) = fetch(
                "https://api.github.com/repos/$repo/contents/$path", token, method = "PUT", bodyJson = payload
            )
            if (code !in 200..201) {
                throw GitHubApiException(code)
            }
            return try { json.decodeFromString<GhContents>(body).sha.orEmpty() } catch (_: Exception) { "" }
        }

        private suspend fun fetch(
            url: String,
            token: String?,
            method: String = "GET",
            bodyJson: String? = null,
        ): Pair<Int, String> = withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
            token?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
            if (bodyJson != null) {
                builder.method(method, bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            } else if (method != "GET") {
                builder.method(method, null)
            }
            client.newCall(builder.build()).execute().use { resp ->
                Pair(resp.code, resp.body?.string().orEmpty())
            }
        }

        // ---------- ابزارها ----------

        /**
         * از ورودی کاربر (کاتالوگ) نام owner/repo را استخراج می‌کند.
         * قبول می‌کند: "owner/repo"، آدرس ریپو، صفحه ریلیز، /releases/latest، ...git
         */
        fun repoFromUrl(input: String): String? {
            val s = input.trim().trimEnd('/')
            val urlMatch = Regex("^https?://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)").find(s)
            if (urlMatch != null) {
                return "${urlMatch.groupValues[1]}/${urlMatch.groupValues[2].removeSuffix(".git")}"
            }
            val shortMatch = Regex("^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$").matchEntire(s)
            if (shortMatch != null) return s
            return null
        }

        private val client = OkHttpClient()
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * از بین asset های یک ریلیز، بهترین APK یونیورسال را برمی‌دارد:
         * اولویت: اسم دارای universal > اسم بدون کلیدواژه معماری (arm/x86) > بقیه
         */
        fun pickUniversalApk(assets: List<GhAsset>): GhAsset? {
            val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
            if (apks.isEmpty()) return null
            val abiKeywords = listOf("arm64", "armeabi", "x86_64", "x86", "abi")
            fun score(a: GhAsset): Int = when {
                a.name.contains("universal", ignoreCase = true) -> 3
                abiKeywords.none { a.name.contains(it, ignoreCase = true) } -> 2
                else -> 1
            }
            return apks.sortedWith(
                compareByDescending<GhAsset> { score(it) }.thenByDescending { it.size }
            ).first()
        }
    }
}
