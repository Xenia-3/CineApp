package com.example.cineapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle

class GestorBoletos(
    context: Context,
    private val helper: AyudanteBaseFavoritos = AyudanteBaseFavoritos.getInstance(context)
) {
    fun savePurchase(uid: String, ticket: Boleto) {
        require(uid.isNotBlank() && ticket.purchaseId.isNotBlank())
        require(ticket.movieId > 0 && ticket.title.isNotBlank() && ticket.cinemaId.isNotBlank())
        require(ticket.date.isNotBlank() && ticket.time.isNotBlank() && ticket.format.isNotBlank())
        require(ticket.seats.isNotEmpty() && ticket.seats.distinct().size == ticket.seats.size)
        require(ticket.seats.all { it.matches(Regex("[A-G][1-8]")) } && ticket.totalCents > 0)
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val owner = db.query("compras", arrayOf("usuario_id"), "compra_id = ?",
                arrayOf(ticket.purchaseId), null, null, null).use {
                if (it.moveToFirst()) it.getString(0) else null
            }
            if (owner != null) {
                check(owner == uid) { "La compra pertenece a otra cuenta" }
                db.setTransactionSuccessful()
                return
            }
            db.insertOrThrow("compras", null, ContentValues().apply {
                put("compra_id", ticket.purchaseId)
                put("usuario_id", uid)
                put("pelicula_id", ticket.movieId)
                put("titulo", ticket.title)
                put("poster", ticket.poster)
                put("cine_id", ticket.cinemaId)
                put("cine", ticket.cinema)
                put("fecha", ticket.date)
                put("hora", ticket.time)
                put("formato", ticket.format)
                put("asientos", ticket.seats.joinToString(","))
                put("total_centavos", ticket.totalCents)
                put("creada", ticket.createdAt)
            })
            for (seat in ticket.seats) {
                try {
                    db.insertOrThrow("asientos_comprados", null, ContentValues().apply {
                        put("compra_id", ticket.purchaseId)
                        put("cine_id", ticket.cinemaId)
                        put("pelicula_id", ticket.movieId)
                        put("fecha", ticket.date)
                        put("hora", ticket.time)
                        put("formato", ticket.format)
                        put("asiento", seat)
                    })
                } catch (error: SQLiteConstraintException) {
                    throw IllegalStateException("El asiento $seat ya está ocupado. Selecciona otro.", error)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getTickets(uid: String): List<Boleto> {
        require(uid.isNotBlank()) { "Inicia sesión para consultar tus tickets" }
        return helper.readableDatabase.query("compras", null, "usuario_id = ?", arrayOf(uid),
            null, null, "creada DESC, rowid DESC").use { cursor ->
            val tickets = mutableListOf<Boleto>()
            while (cursor.moveToNext()) {
                fun text(column: String) = cursor.getString(cursor.getColumnIndexOrThrow(column)).orEmpty()
                tickets.add(Boleto(
                    text("compra_id"), cursor.getInt(cursor.getColumnIndexOrThrow("pelicula_id")),
                    text("titulo"), text("poster"), text("cine_id"), text("cine"), text("fecha"),
                    text("hora"), text("formato"), text("asientos").split(","),
                    cursor.getInt(cursor.getColumnIndexOrThrow("total_centavos")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("creada"))
                ))
            }
            tickets
        }
    }

    fun occupiedSeats(selection: Bundle): Set<String> = helper.readableDatabase.query(
        "asientos_comprados", arrayOf("asiento"),
        "cine_id = ? AND pelicula_id = ? AND fecha = ? AND hora = ? AND formato = ?",
        arrayOf(selection.getString("cinemaId").orEmpty(), selection.getInt("movieId").toString(),
            selection.getString("date").orEmpty(), selection.getString("time").orEmpty(),
            selection.getString("format").orEmpty()), null, null, null
    ).use { cursor ->
        val seats = mutableSetOf<String>()
        while (cursor.moveToNext()) seats.add(cursor.getString(0))
        seats
    }
}
