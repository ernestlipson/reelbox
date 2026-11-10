package dev.reelbox.data

data class StreamingServer(
    val id: String,
    val name: String,
    val buildUrl: (type: String, tmdbId: Int, season: Int, episode: Int) -> String,
)

object StreamingServers {
    val ALL = listOf(
        StreamingServer(
            id = "vidcore",
            name = "Vidcore",
            buildUrl = { type, id, season, episode ->
                if (type == "tv") "https://vidcore.net/tv/$id/$season/$episode?autoplay=1"
                else "https://vidcore.net/movie/$id?autoplay=1"
            },
        ),
        StreamingServer(
            id = "videasy",
            name = "Videasy",
            buildUrl = { type, id, season, episode ->
                if (type == "tv") "https://player-4aq.pages.dev/embed/tv/$id/$season/$episode?autoplay=1"
                else "https://player-4aq.pages.dev/embed/movie/$id?autoplay=1"
            },
        ),
        StreamingServer(
            id = "peachify",
            name = "Peachify",
            buildUrl = { type, id, season, episode ->
                if (type == "tv") "https://player.vidlove.cc/embed/tv/$id/$season/$episode?autoplay=1"
                else "https://player.vidlove.cc/embed/movie/$id?autoplay=1"
            },
        ),
        StreamingServer(
            id = "vidsrc",
            name = "VidSrc",
            buildUrl = { type, id, season, episode ->
                if (type == "tv") "https://ythd.org/embed/tv/$id/$season/$episode?autoplay=1"
                else "https://ythd.org/embed/movie/$id?autoplay=1"
            },
        ),
        StreamingServer(
            id = "vidify",
            name = "Vidify",
            buildUrl = { type, id, season, episode ->
                if (type == "tv") "https://play.xpass.top/e/tv/$id/$season/$episode?autoplay=1"
                else "https://play.xpass.top/e/movie/$id?autoplay=1"
            },
        ),
        StreamingServer(
            id = "vidgod",
            name = "VidGod",
            buildUrl = { type, id, season, episode ->
                if (type == "tv") "https://cinemaos.tech/player/?id=$id&type=tv&s=$season&e=$episode&autoplay=1"
                else "https://cinemaos.tech/player/?id=$id&autoplay=1"
            },
        ),
    )

    val DEFAULT: StreamingServer = ALL.first()
}
