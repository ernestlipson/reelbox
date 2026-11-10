package dev.reelbox.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized Firebase Analytics manager for Reelbox.
 * Safely wraps analytics calls so the app operates smoothly even before
 * Firebase services are initialized or in test environments.
 */
object AppAnalytics {

    private const val TAG = "AppAnalytics"
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            analytics = FirebaseAnalytics.getInstance(context)
            Log.d(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics initialization skipped: ${e.message}")
        }
    }

    /**
     * Log when a screen is viewed in the Jetpack Compose navigation host.
     */
    fun logScreenView(screenName: String, screenClass: String = screenName) {
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }

    /**
     * Log search query submissions.
     */
    fun logSearch(query: String) {
        if (query.isBlank()) return
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SEARCH_TERM, query.take(100))
            }
            fa.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
    }

    /**
     * Log when a user taps a movie or TV show card.
     */
    fun logSelectItem(mediaType: String, id: Int, title: String) {
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, mediaType)
                putString(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
            }
            fa.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }
    }

    /**
     * Log when a title is added to the local watchlist.
     */
    fun logAddToWatchlist(mediaType: String, id: Int, title: String) {
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, mediaType)
                putString(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
            }
            fa.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, bundle)
        }
    }

    /**
     * Log when a title is removed from the local watchlist.
     */
    fun logRemoveFromWatchlist(mediaType: String, id: Int, title: String) {
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, mediaType)
                putString(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
            }
            fa.logEvent("remove_from_watchlist", bundle)
        }
    }

    /**
     * Log when the user clicks an external streaming provider link (e.g., Netflix, Prime Video).
     */
    fun logProviderClick(providerName: String, title: String, mediaType: String) {
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString("provider_name", providerName)
                putString("media_title", title.take(100))
                putString("media_type", mediaType)
            }
            fa.logEvent("streaming_provider_click", bundle)
        }
    }

    /**
     * Log when the user launches video or trailer playback.
     */
    fun logPlayTrailer(title: String, mediaType: String, id: Int) {
        analytics?.let { fa ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, mediaType)
            }
            fa.logEvent("play_trailer", bundle)
        }
    }

    /**
     * Log when the user switches their preferred streaming region.
     */
    fun logRegionChange(regionCode: String) {
        analytics?.let { fa ->
            fa.setUserProperty("selected_region", regionCode)
            val bundle = Bundle().apply {
                putString("region_code", regionCode)
            }
            fa.logEvent("region_changed", bundle)
        }
    }
}
