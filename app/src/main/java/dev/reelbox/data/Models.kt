package dev.reelbox.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A catalog entry. TMDB returns the same shape for movies and TV, so one model covers both. */
@Serializable
data class Movie(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
) {
    val displayTitle: String get() = title?.takeIf { it.isNotBlank() } ?: name.orEmpty()
    val date: String? get() = releaseDate?.takeIf { it.isNotBlank() } ?: firstAirDate
    val year: String get() = date?.take(4).orEmpty()
    val isTv: Boolean get() = mediaType == "tv" || (title.isNullOrBlank() && !name.isNullOrBlank())
}

@Serializable
data class PageResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class Genre(val id: Int = 0, val name: String = "")

@Serializable
data class CastMember(
    val id: Int = 0,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = 0,
)

@Serializable
data class Credits(val cast: List<CastMember> = emptyList())

@Serializable
data class Video(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
)

@Serializable
data class VideoList(val results: List<Video> = emptyList())

@Serializable
data class Provider(
    @SerialName("provider_id") val id: Int = 0,
    @SerialName("provider_name") val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
)

@Serializable
data class RegionProviders(
    val link: String? = null,
    val free: List<Provider> = emptyList(),
    val ads: List<Provider> = emptyList(),
    val flatrate: List<Provider> = emptyList(),
    val rent: List<Provider> = emptyList(),
    val buy: List<Provider> = emptyList(),
)

@Serializable
data class WatchProviders(val results: Map<String, RegionProviders> = emptyMap())

@Serializable
data class MediaDetail(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("number_of_seasons") val seasons: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val tagline: String = "",
    val status: String? = null,
    val genres: List<Genre> = emptyList(),
    val credits: Credits? = null,
    val videos: VideoList? = null,
    @SerialName("watch/providers") val watchProviders: WatchProviders? = null,
) {
    val displayTitle: String get() = title?.takeIf { it.isNotBlank() } ?: name.orEmpty()
    val year: String get() = (releaseDate?.takeIf { it.isNotBlank() } ?: firstAirDate)?.take(4).orEmpty()
    val runtimeLabel: String
        get() {
            val minutes = runtime ?: episodeRunTime.firstOrNull() ?: return ""
            if (minutes <= 0) return ""
            val h = minutes / 60
            val m = minutes % 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
    val trailer: Video?
        get() = videos?.results
            ?.filter { it.site.equals("YouTube", true) && it.key.isNotBlank() }
            ?.sortedByDescending { it.type.equals("Trailer", true) }
            ?.firstOrNull()

    fun providersFor(region: String): RegionProviders? =
        watchProviders?.results?.get(region.uppercase())
}

/** Image URL helper for TMDB's CDN. */
object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p/"

    fun poster(path: String?, width: Int = 342): String? =
        path?.let { "${BASE}w$width$it" }

    fun backdrop(path: String?, width: Int = 780): String? =
        path?.let { "${BASE}w$width$it" }

    fun profile(path: String?, width: Int = 185): String? =
        path?.let { "${BASE}w$width$it" }

    fun providerLogo(path: String?, width: Int = 92): String? =
        path?.let { "${BASE}w$width$it" }
}
