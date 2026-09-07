package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class TicketsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets)

        val rvTickets = findViewById<RecyclerView>(R.id.rvTickets)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Datos de ejemplo (simulamos que el usuario ya compró estas)
        val tickets = listOf(
            Movie("Dune: Parte Dos", "Acción", "R.drawable.dune", "4.7"),
            Movie("Intensamente 2", "Animación", "R.drawable.intensamente", "4.8")
        )

        // Nota: Usamos el diseño de Item_movie_vertical como base, pero podrías crear uno específico para tickets
        val adapter = MovieVerticalAdapter(tickets)
        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = adapter

        // Barra de navegación
        bottomNav.selectedItemId = R.id.nav_tickets

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_tickets -> true
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