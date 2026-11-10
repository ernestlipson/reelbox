package dev.reelbox.data

import dev.reelbox.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object TmdbClient {

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    fun create(): TmdbApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    /** Supports both TMDB auth styles: v4 read token (Bearer) or v3 api_key query param. */
    private object AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            val urlBuilder = original.url.newBuilder().addQueryParameter("language", "en-US")
            if (BuildConfig.TMDB_READ_TOKEN.isBlank() && BuildConfig.TMDB_API_KEY.isNotBlank()) {
                urlBuilder.addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            }

            val builder = original.newBuilder().url(urlBuilder.build())
            if (BuildConfig.TMDB_READ_TOKEN.isNotBlank()) {
                builder.header("Authorization", "Bearer ${BuildConfig.TMDB_READ_TOKEN}")
            }
            return chain.proceed(builder.build())
        }
    }
}
