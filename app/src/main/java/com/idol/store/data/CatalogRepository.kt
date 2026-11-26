package com.idol.store.data

import android.content.Context
import com.idol.store.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * کاتالوگ را از URL ریموت (BuildConfig.CATALOG_URL) می‌خواند؛
 * اگر در دسترس نبود، از نسخه داخلی assets/catalog/apps.json استفاده می‌کند.
 */
object CatalogRepository {

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    private var cached: Catalog? = null

    suspend fun load(context: Context, forceRefresh: Boolean = false): Catalog {
        if (!forceRefresh) cached?.let { return it }
        cached = null

        val remoteUrl = BuildConfig.CATALOG_URL
        if (remoteUrl.isNotBlank() && !remoteUrl.contains("USERNAME")) {
            try {
                withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(remoteUrl).build()).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: throw IOException()
                            cached = json.decodeFromString<Catalog>(body)
                        }
                    }
                }
            } catch (_: Exception) {
                // خطای شبکه یا فرمت اشتباه → برمی‌گردیم روی کاتالوگ داخلی
            }
        }

        if (cached == null) {
            val raw = withContext(Dispatchers.IO) {
                context.assets.open("catalog/apps.json").bufferedReader().use { it.readText() }
            }
            cached = json.decodeFromString<Catalog>(raw)
        }

        return cached!!
    }
}
