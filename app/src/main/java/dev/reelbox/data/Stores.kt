package dev.reelbox.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.core.content.edit

@Serializable
data class SavedItem(
    val id: Int,
    val type: String,
    val title: String,
    val posterPath: String? = null,
    val year: String = "",
    val rating: Double = 0.0,
)

/** Watchlist persisted as a small JSON blob. No database needed for a few hundred titles. */
class WatchlistStore(context: Context) {

    private val prefs = context.getSharedPreferences("reelbox_watchlist", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<SavedItem>> = _items.asStateFlow()

    fun contains(id: Int, type: String): Boolean =
        _items.value.any { it.id == id && it.type == type }

    fun toggle(item: SavedItem) {
        val current = _items.value
        val next = if (current.any { it.id == item.id && it.type == item.type }) {
            current.filterNot { it.id == item.id && it.type == item.type }
        } else {
            listOf(item) + current
        }
        persist(next)
    }

    fun remove(id: Int, type: String) {
        persist(_items.value.filterNot { it.id == id && it.type == type })
    }

    fun clear() = persist(emptyList())

    private fun persist(list: List<SavedItem>) {
        prefs.edit { putString(KEY, json.encodeToString(list)) }
        _items.value = list
    }

    private fun load(): List<SavedItem> =
        runCatching {
            val raw = prefs.getString(KEY, null) ?: return emptyList()
            json.decodeFromString<List<SavedItem>>(raw)
        }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "items"
    }
}

/** Tiny settings store: streaming region used by the "Where to watch" section. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("reelbox_settings", Context.MODE_PRIVATE)

    private val _region = MutableStateFlow(prefs.getString(KEY_REGION, "GH") ?: "GH")
    val region: StateFlow<String> = _region.asStateFlow()

    fun setRegion(code: String) {
        prefs.edit { putString(KEY_REGION, code.uppercase()) }
        _region.value = code.uppercase()
    }

    private companion object {
        const val KEY_REGION = "region"
    }
}

/** Simple manual DI. One place to build the network stack. */
object Graph {

    private var initialized = false

    lateinit var repository: TmdbRepository
        private set

    lateinit var watchlist: WatchlistStore
        private set

    lateinit var settings: SettingsStore
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            repository = TmdbRepository(TmdbClient.create())
            watchlist = WatchlistStore(context.applicationContext)
            settings = SettingsStore(context.applicationContext)
            initialized = true
        }
    }
}
