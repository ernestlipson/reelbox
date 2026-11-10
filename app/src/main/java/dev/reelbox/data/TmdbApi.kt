package dev.reelbox.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("trending/movie/week")
    suspend fun trendingMovies(@Query("page") page: Int): PageResponse<Movie>

    @GET("trending/tv/week")
    suspend fun trendingTv(@Query("page") page: Int): PageResponse<Movie>

    @GET("movie/top_rated")
    suspend fun topRatedMovies(@Query("page") page: Int): PageResponse<Movie>

    @GET("movie/now_playing")
    suspend fun nowPlaying(@Query("page") page: Int): PageResponse<Movie>

    @GET("movie/upcoming")
    suspend fun upcomingMovies(@Query("page") page: Int): PageResponse<Movie>

    @GET("tv/popular")
    suspend fun popularTv(@Query("page") page: Int): PageResponse<Movie>

    @GET("tv/top_rated")
    suspend fun topRatedTv(@Query("page") page: Int): PageResponse<Movie>

    @GET("search/multi")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int,
    ): PageResponse<Movie>

    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String,
    ): MediaDetail

    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String,
    ): MediaDetail

    @GET("movie/{id}/similar")
    suspend fun similarMovies(@Path("id") id: Int): PageResponse<Movie>

    @GET("tv/{id}/similar")
    suspend fun similarTv(@Path("id") id: Int): PageResponse<Movie>
}
