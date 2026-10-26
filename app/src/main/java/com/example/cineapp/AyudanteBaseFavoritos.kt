package com.example.cineapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AyudanteBaseFavoritos internal constructor(context: Context, name: String = "cineapp.db") :
    SQLiteOpenHelper(context, name, null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE favoritos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id TEXT NOT NULL,
                pelicula_id INTEGER NOT NULL,
                titulo TEXT NOT NULL,
                poster TEXT,
                genero TEXT,
                calificacion TEXT,
                nota TEXT,
                UNIQUE(usuario_id, pelicula_id)
            )
        """.trimIndent())
        createPurchases(db)
        createProfiles(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createPurchases(db)
        if (oldVersion < 3) db.execSQL("ALTER TABLE favoritos ADD COLUMN nota TEXT")
        if (oldVersion < 4) createProfiles(db)
    }

    private fun createProfiles(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE perfiles (
                usuario_id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                edad INTEGER
            )
        """.trimIndent())
    }

    private fun createPurchases(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE compras (
                compra_id TEXT PRIMARY KEY,
                usuario_id TEXT NOT NULL,
                pelicula_id INTEGER NOT NULL,
                titulo TEXT NOT NULL,
                poster TEXT,
                cine_id TEXT NOT NULL,
                cine TEXT NOT NULL,
                fecha TEXT NOT NULL,
                hora TEXT NOT NULL,
                formato TEXT NOT NULL,
                asientos TEXT NOT NULL,
                total_centavos INTEGER NOT NULL,
                creada INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX compras_usuario ON compras(usuario_id)")
        db.execSQL("""
            CREATE TABLE asientos_comprados (
                compra_id TEXT NOT NULL,
                cine_id TEXT NOT NULL,
                pelicula_id INTEGER NOT NULL,
                fecha TEXT NOT NULL,
                hora TEXT NOT NULL,
                formato TEXT NOT NULL,
                asiento TEXT NOT NULL,
                UNIQUE(cine_id, pelicula_id, fecha, hora, formato, asiento)
            )
        """.trimIndent())
    }

    companion object {
        @Volatile private var instance: AyudanteBaseFavoritos? = null

        fun getInstance(context: Context): AyudanteBaseFavoritos =
            instance ?: synchronized(this) {
                instance ?: AyudanteBaseFavoritos(context.applicationContext).also { instance = it }
            }
    }
}
