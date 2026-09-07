package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Lista horizontal (En cartelera)
        val rvMovies = findViewById<RecyclerView>(R.id.rvMovies)
        val movies = listOf(
            Movie("Dune: Parte Dos", "Acción/Aventura", "R.drawable.dune", "4.7"),
            Movie("Intensamente 2", "Animación", "R.drawable.intensamente", "4.8"),
            Movie("Civil War", "Drama", "R.drawable.civilwar", "4.5"),
            Movie("Furiosa", "Acción", "R.drawable.furiosa", "4.9"),
            Movie("El Planeta de los Simios", "Ciencia Ficción", "R.drawable.simios", "4.6")
        )
        val adapter = MovieAdapter(movies)
        rvMovies.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMovies.adapter = adapter

        // 2. Lista vertical (Recomendadas)
        val rvRecomendadas = findViewById<RecyclerView>(R.id.rvRecomendadas)
        val recomendadas = listOf(
            Movie("Furiosa: A Mad Max Saga", "Acción", "R.drawable.furiosa", "4.8"),
            Movie("El Planeta de los Simios: Nuevo Reino", "Ciencia Ficción", "R.drawable.simios", "4.9"),
            Movie("Intensamente 2", "Animación", "R.drawable.intensamente", "4.8")
        )
        val adapterVertical = MovieVerticalAdapter(recomendadas)
        rvRecomendadas.layoutManager = LinearLayoutManager(this)
        rvRecomendadas.adapter = adapterVertical

        // 3. CLIC EN LA TARJETA DE GLADIADOR II
        val cardGladiador = findViewById<ConstraintLayout>(R.id.cardGladiador)
        cardGladiador.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("title", "Gladiador II")
            intent.putExtra("genre", "Acción")
            intent.putExtra("image", "R.drawable.gladiador")
            startActivity(intent)
        }

        // 4. Barra de navegación
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_tickets -> {
                    val intent = Intent(this, TicketsActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_favorites -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}