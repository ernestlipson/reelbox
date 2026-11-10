package dev.reelbox.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class HomeRow(val title: String, val items: List<Movie>)

class TmdbRepository(private val api: TmdbApi) {

    companion object {
        const val APPEND = "credits,videos,watch/providers"
    }

    suspend fun home(): List<HomeRow> = coroutineScope {
        val trendingMovies = async { api.trendingMovies(1).results }
        val trendingTv = async { api.trendingTv(1).results }
        val topMovies = async { api.topRatedMovies(1).results }
        val nowPlaying = async { api.nowPlaying(1).results }
        val popularTv = async { api.popularTv(1).results }
        val upcoming = async { api.upcomingMovies(1).results }

        listOf(
            HomeRow("Trending Movies", trendingMovies.await()),
            HomeRow("Trending TV", trendingTv.await()),
            HomeRow("Top Rated Movies", topMovies.await()),
            HomeRow("In Theaters", nowPlaying.await()),
            HomeRow("Popular TV", popularTv.await()),
            HomeRow("Coming Soon", upcoming.await()),
        ).filter { it.items.isNotEmpty() }
    }

    suspend fun search(query: String, page: Int = 1): List<Movie> =
        api.search(query, page).results
            .filter { it.mediaType == null || it.mediaType == "movie" || it.mediaType == "tv" }

    suspend fun detail(type: String, id: Int): MediaDetail =
        if (type == "tv") api.tvDetail(id, APPEND) else api.movieDetail(id, APPEND)

    suspend fun similar(type: String, id: Int): List<Movie> =
        if (type == "tv") api.similarTv(id).results else api.similarMovies(id).results
}
