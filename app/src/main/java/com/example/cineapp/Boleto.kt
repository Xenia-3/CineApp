package com.example.cineapp

import android.os.Bundle

data class Boleto(
    val purchaseId: String,
    val movieId: Int,
    val title: String,
    val poster: String,
    val cinemaId: String,
    val cinema: String,
    val date: String,
    val time: String,
    val format: String,
    val seats: List<String>,
    val totalCents: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SERVICE_FEE_CENTS = 150

        fun fromSelection(selection: Bundle, purchaseId: String) = Boleto(
            purchaseId, selection.getInt("movieId"), selection.getString("title").orEmpty(),
            selection.getString("image").orEmpty(), selection.getString("cinemaId").orEmpty(),
            selection.getString("cinemaName").orEmpty(), selection.getString("date").orEmpty(),
            selection.getString("time").orEmpty(), selection.getString("format").orEmpty(),
            selection.getStringArrayList("seats").orEmpty().sorted(),
            selection.getInt("totalCents") + SERVICE_FEE_CENTS
        )
    }
}
