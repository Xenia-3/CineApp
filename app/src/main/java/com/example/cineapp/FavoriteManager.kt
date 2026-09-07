package com.example.cineapp

import android.content.Context

class FavoriteManager(context: Context) {
    private val prefs = context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)

    // Guardar favorito (Guardamos el título, género e imagen)
    fun saveFavorite(movie: Movie) {
        val editor = prefs.edit()
        // Guardamos los datos en formato de texto separado por "|"
        editor.putString(movie.title, "${movie.genre}|${movie.imageUrl}|${movie.rating}")
        editor.apply()
    }

    // Leer todos los favoritos
    fun getFavorites(): List<Movie> {
        val allEntries = prefs.all
        val movies = mutableListOf<Movie>()
        for ((title, value) in allEntries) {
            val parts = value.toString().split("|")
            if (parts.size == 3) {
                movies.add(Movie(title, parts[0], parts[1], parts[2]))
            }
        }
        return movies
    }

    // Quitar de favoritos
    fun removeFavorite(title: String) {
        val editor = prefs.edit()
        editor.remove(title)
        editor.apply()
    }

    // Verificar si ya es favorito
    fun isFavorite(title: String): Boolean {
        return prefs.contains(title)
    }
}