package dev.reelbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.reelbox.data.Graph
import dev.reelbox.data.HomeRow
import dev.reelbox.data.MediaDetail
import dev.reelbox.data.Movie
import dev.reelbox.data.SavedItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

internal fun friendlyError(t: Throwable): String = when {
    t is HttpException && t.code() == 401 ->
        "TMDB rejected the API key. Check TMDB_API_KEY / TMDB_READ_TOKEN in local.properties, then rebuild."
    t is HttpException && t.code() == 404 ->
        "That title isn't available."
    t is HttpException ->
        "TMDB returned HTTP ${t.code()}. Try again in a moment."
    t is IOException ->
        "No connection to TMDB. Check your network and try again."
    else ->
        t.message ?: "Something went wrong."
}

// ---------------------------------------------------------------- Home

class HomeViewModel : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val rows: List<HomeRow> = emptyList(),
    ) {
        val hero: List<Movie> get() = rows.firstOrNull()?.items?.take(6).orEmpty()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { Graph.repository.home() }
                .onSuccess { rows -> _state.value = State(loading = false, error = null, rows = rows) }
                .onFailure { e -> _state.value = State(loading = false, error = friendlyError(e), rows = emptyList()) }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { HomeViewModel() } }
    }
}

// -------------------------------------------------------------- Search

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {

    data class State(
        val loading: Boolean = false,
        val error: String? = null,
        val results: List<Movie> = emptyList(),
        val searched: Boolean = false,
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .map { it.trim() }
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { q ->
                    if (q.isBlank()) {
                        _state.value = State()
                        return@collectLatest
                    }
                    _state.value = _state.value.copy(loading = true, error = null)
                    runCatching { Graph.repository.search(q) }
                        .onSuccess { results ->
                            _state.value = State(loading = false, error = null, results = results, searched = true)
                        }
                        .onFailure { e ->
                            _state.value = State(loading = false, error = friendlyError(e), results = emptyList(), searched = true)
                        }
                }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun retry() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { Graph.repository.search(q) }
                .onSuccess { results ->
                    _state.value = State(loading = false, error = null, results = results, searched = true)
                }
                .onFailure { e ->
                    _state.value = State(loading = false, error = friendlyError(e), results = emptyList(), searched = true)
                }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { SearchViewModel() } }
    }
}

// -------------------------------------------------------------- Detail

class DetailViewModel(
    private val type: String,
    private val id: Int,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val detail: MediaDetail? = null,
        val similar: List<Movie> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val detail = Graph.repository.detail(type, id)
                val similar = runCatching { Graph.repository.similar(type, id) }.getOrDefault(emptyList())
                detail to similar
            }.onSuccess { (detail, similar) ->
                _state.value = State(loading = false, error = null, detail = detail, similar = similar)
            }.onFailure { e ->
                _state.value = State(loading = false, error = friendlyError(e))
            }
        }
    }

    fun isSaved(): Boolean = Graph.watchlist.contains(id, type)

    fun toggleSaved() {
        val d = _state.value.detail ?: return
        Graph.watchlist.toggle(
            SavedItem(
                id = d.id,
                type = type,
                title = d.displayTitle,
                posterPath = d.posterPath,
                year = d.year,
                rating = d.voteAverage,
            ),
        )
    }

    companion object {
        fun factory(type: String, id: Int) = viewModelFactory {
            initializer { DetailViewModel(type, id) }
        }
    }
}

// ------------------------------------------------------------ Watchlist

class WatchlistViewModel : ViewModel() {

    val items: StateFlow<List<SavedItem>> = Graph.watchlist.items

    fun remove(item: SavedItem) = Graph.watchlist.remove(item.id, item.type)

    fun clear() = Graph.watchlist.clear()

    companion object {
        val Factory = viewModelFactory { initializer { WatchlistViewModel() } }
    }
}
