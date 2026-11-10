package dev.reelbox.data

import dev.reelbox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Mirrors website/version.json — the single source of truth for the latest release. */
@Serializable
data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = "",
)

/**
 * Reelbox is distributed as an APK from its website, so Play's in-app updates don't apply.
 * Instead the app reads a small JSON file from the site and compares version codes.
 */
object UpdateChecker {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /** Returns the newer release, or null when up to date, offline, or the file is malformed. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(BuildConfig.UPDATE_URL)
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val info = json.decodeFromString<UpdateInfo>(response.body?.string().orEmpty())
                info.takeIf {
                    it.versionCode > BuildConfig.VERSION_CODE && it.apkUrl.startsWith("https://")
                }
            }
        }.getOrNull()
    }
}
