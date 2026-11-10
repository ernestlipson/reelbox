package dev.reelbox.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.reelbox.data.Provider

object StreamingApps {

    // Common TMDB provider ID to Android package name mapping
    private val PACKAGE_BY_ID = mapOf(
        8 to "com.netflix.mediaclient",             // Netflix
        9 to "com.amazon.avod.thirdpartyclient",    // Amazon Prime Video
        119 to "com.amazon.avod.thirdpartyclient",  // Amazon Prime Video
        337 to "com.disney.disneyplus",             // Disney+
        1899 to "com.wbd.stream",                   // Max
        384 to "com.wbd.stream",                    // HBO Max
        15 to "com.hulu.plus",                      // Hulu
        531 to "com.cbs.app",                       // Paramount+
        386 to "com.peacocktv.peacockandroid",      // Peacock
        283 to "com.crunchyroll.crunchyroll",       // Crunchyroll
        73 to "com.tubitv",                         // Tubi TV
        300 to "tv.pluto.android",                  // Pluto TV
        3 to "com.google.android.videos",           // Google TV / Play Movies
        192 to "com.google.android.youtube",        // YouTube
        55 to "com.showmax.letterbox",              // Showmax
        7 to "air.com.vudu.air.DownloaderTablet",   // Vudu / Fandango at Home
        380 to "com.britbox.player",                // BritBox
        43 to "com.bydeluxe.d3.android.program.starz", // Starz
        34 to "com.epix.epix",                      // MGM+ / Epix
        632 to "com.epix.epix",                     // MGM+
        230 to "ca.bellmedia.crave",                // Crave
        21 to "au.com.stan.stan",                   // Stan
        381 to "com.canal.android.canal",           // Canal+
        35 to "tv.wuaki",                           // Rakuten TV
        232 to "com.graymatrix.did",                // Zee5
        237 to "com.mslavstream.sonyliv",           // SonyLIV
        220 to "com.jio.media.ondemand",            // JioCinema
        11 to "com.mubi",                           // MUBI
        99 to "com.dramafever.shudder",             // Shudder
        546 to "com.discovery.discoveryplus.android", // Discovery+
        38 to "bbc.iplayer.android",                // BBC iPlayer
        41 to "air.ITVMobilePlayer",                // ITVX
    )

    // Name keyword matching fallback if provider ID varies by country
    private val PACKAGE_BY_KEYWORD = listOf(
        "netflix" to "com.netflix.mediaclient",
        "prime video" to "com.amazon.avod.thirdpartyclient",
        "amazon" to "com.amazon.avod.thirdpartyclient",
        "disney" to "com.disney.disneyplus",
        "max" to "com.wbd.stream",
        "hbo" to "com.wbd.stream",
        "hulu" to "com.hulu.plus",
        "paramount" to "com.cbs.app",
        "peacock" to "com.peacocktv.peacockandroid",
        "crunchyroll" to "com.crunchyroll.crunchyroll",
        "tubi" to "com.tubitv",
        "pluto" to "tv.pluto.android",
        "showmax" to "com.showmax.letterbox",
        "google play" to "com.google.android.videos",
        "google tv" to "com.google.android.videos",
        "youtube" to "com.google.android.youtube",
        "vudu" to "air.com.vudu.air.DownloaderTablet",
        "britbox" to "com.britbox.player",
        "starz" to "com.bydeluxe.d3.android.program.starz",
        "mgm" to "com.epix.epix",
        "crave" to "ca.bellmedia.crave",
        "stan" to "au.com.stan.stan",
        "canal" to "com.canal.android.canal",
        "rakuten" to "tv.wuaki",
        "zee5" to "com.graymatrix.did",
        "sonyliv" to "com.mslavstream.sonyliv",
        "jiocinema" to "com.jio.media.ondemand",
        "mubi" to "com.mubi",
        "shudder" to "com.dramafever.shudder",
        "discovery" to "com.discovery.discoveryplus.android",
        "iplayer" to "bbc.iplayer.android",
        "itvx" to "air.ITVMobilePlayer",
    )

    fun findPackageName(provider: Provider): String? {
        PACKAGE_BY_ID[provider.id]?.let { return it }
        val lowerName = provider.name.lowercase()
        return PACKAGE_BY_KEYWORD.firstOrNull { (keyword, _) ->
            lowerName.contains(keyword)
        }?.second
    }

    /**
     * Opens the streaming provider app if installed on the device.
     * If not installed, directs the user to the app's Google Play Store listing.
     * If the provider is unknown or no app package exists, falls back to the JustWatch URL.
     */
    fun open(context: Context, provider: Provider, fallbackUrl: String?) {
        // Special case: Apple TV has no standard Android phone app (only Android TV or web)
        if ((provider.id in setOf(2, 350)) || provider.name.contains("Apple TV", ignoreCase = true)) {
            val appleTvPackage = "com.apple.atve.androidtv.appletv"
            val launchIntent = context.packageManager.getLaunchIntentForPackage(appleTvPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
            // For phones, tv.apple.com is the best experience
            openWebUrl(context, "https://tv.apple.com")
            return
        }

        val packageName = findPackageName(provider)
        if (packageName != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }

            // App is not installed: try opening Google Play Store app directly
            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // If Play Store app is absent, fallback to browser Play Store link
                openWebUrl(context, "https://play.google.com/store/apps/details?id=$packageName")
                return
            }
        }

        // Fallback to the JustWatch aggregate link if available
        if (!fallbackUrl.isNullOrBlank()) {
            openWebUrl(context, fallbackUrl)
        }
    }

    private fun openWebUrl(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
