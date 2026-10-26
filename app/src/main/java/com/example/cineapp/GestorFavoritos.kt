package com.example.cineapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class GestorFavoritos(
    context: Context,
    private val helper: AyudanteBaseFavoritos = AyudanteBaseFavoritos.getInstance(context)
) {
    private val prefs = context.applicationContext
        .getSharedPreferences("TmdbFavorites", Context.MODE_PRIVATE)

    private fun userId(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("Inicia sesión para consultar o modificar tus favoritos")
        migrateLegacyFavorites(uid)
        return uid
    }

    fun saveFavorite(movie: Pelicula) {
        val uid = userId()
        require(movie.id > 0)
        val inserted = helper.writableDatabase.insertWithOnConflict(
            "favoritos", null, values(uid, movie), SQLiteDatabase.CONFLICT_IGNORE
        )
        check(inserted != -1L || isFavorite(movie.id)) { "No se pudo guardar el favorito" }
    }

    fun getFavorites(): List<Pelicula> {
        val uid = userId()
        return helper.readableDatabase.query(
            "favoritos", arrayOf("pelicula_id", "titulo", "poster", "genero", "calificacion"),
            "usuario_id = ?", arrayOf(uid), null, null, "id ASC"
        ).use { cursor ->
            val movies = mutableListOf<Pelicula>()
            while (cursor.moveToNext()) {
                movies.add(Pelicula(
                    id = cursor.getInt(0),
                    title = cursor.getString(1),
                    imageUrl = cursor.getString(2).orEmpty(),
                    genre = cursor.getString(3).orEmpty(),
                    rating = cursor.getString(4).orEmpty()
                ))
            }
            movies
        }
    }

    fun removeFavorite(id: Int) {
        val uid = userId()
        helper.writableDatabase.delete(
            "favoritos", "usuario_id = ? AND pelicula_id = ?", arrayOf(uid, id.toString())
        )
    }

    fun isFavorite(id: Int): Boolean {
        val uid = userId()
        return helper.readableDatabase.query(
            "favoritos", arrayOf("id"), "usuario_id = ? AND pelicula_id = ?",
            arrayOf(uid, id.toString()), null, null, null, "1"
        ).use { it.moveToFirst() }
    }

    private fun values(uid: String, movie: Pelicula) = ContentValues().apply {
        put("usuario_id", uid)
        put("pelicula_id", movie.id)
        put("titulo", movie.title)
        put("poster", movie.imageUrl)
        put("genero", movie.genre)
        put("calificacion", movie.rating)
    }

    private fun migrateLegacyFavorites(uid: String) = synchronized(helper) {
        if (prefs.getBoolean("sqlite_migrated_v1", false)) return@synchronized
        val gson = Gson()
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            for ((key, value) in prefs.all) {
                if (key.toIntOrNull() == null || value !is String) continue
                val movie = try {
                    gson.fromJson(value, Pelicula::class.java)
                } catch (_: Exception) {
                    null
                }
                if (movie == null || movie.id <= 0 || movie.title.isNullOrBlank()) continue
                val inserted = db.insertWithOnConflict(
                    "favoritos", null, values(uid, movie), SQLiteDatabase.CONFLICT_IGNORE
                )
                check(inserted != -1L || db.query(
                    "favoritos", arrayOf("id"), "usuario_id = ? AND pelicula_id = ?",
                    arrayOf(uid, movie.id.toString()), null, null, null, "1"
                ).use { it.moveToFirst() }) { "No se pudo migrar el favorito" }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        check(prefs.edit().putBoolean("sqlite_migrated_v1", true).commit()) {
            "No se pudo registrar la migracion de favoritos"
        }
    }
}
