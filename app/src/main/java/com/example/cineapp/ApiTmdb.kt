package com.example.cineapp

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale

interface ApiTmdb {
    @GET("genre/movie/list")
    fun genres(): Call<RespuestaGeneros>

    @GET("movie/{category}")
    fun movies(@Path("category") category: String): Call<RespuestaPeliculas>

    @GET("search/movie")
    fun search(@Query("query") query: String): Call<RespuestaPeliculas>

    @GET("movie/{id}")
    fun detail(@Path("id") id: Int): Call<PeliculaTmdb>
}

data class Genero(val id: Int, val name: String)
data class RespuestaGeneros(val genres: List<Genero> = emptyList())
data class RespuestaPeliculas(val results: List<PeliculaTmdb> = emptyList())

data class PeliculaTmdb(
    val id: Int = 0,
    val title: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    val genres: List<Genero> = emptyList(),
    val overview: String? = null,
    val runtime: Int? = null,
    @SerializedName("release_date") val releaseDate: String? = null
) {
    fun toMovie(): Pelicula {
        val names = if (genres.isNotEmpty()) genres.map { it.name }
            else genreIds.mapNotNull { ClienteTmdb.genreNames[it] }
        return Pelicula(
            title,
            names.joinToString(" / ").ifEmpty { "Género no disponible" },
            posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: "",
            if (voteCount > 0) String.format(Locale.US, "%.1f/10", voteAverage) else "Sin calificación",
            id
        )
    }
}
