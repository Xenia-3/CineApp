package com.example.cineapp

data class Pelicula(
    val title: String,
    val genre: String,
    val imageUrl: String,
    val rating: String,
    val id: Int = 0
)
