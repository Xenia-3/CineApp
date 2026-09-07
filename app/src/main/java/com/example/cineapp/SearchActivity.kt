package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: SearchAdapter
    private val allMovies = listOf(
        Movie("Dune: Parte Dos", "Acción", "R.drawable.dune", "4.7"),
        Movie("Intensamente 2", "Animación", "R.drawable.intensamente", "4.8"),
        Movie("Civil War", "Drama", "R.drawable.civilwar", "4.5"),
        Movie("Furiosa", "Acción", "R.drawable.furiosa", "4.9"),
        Movie("El Planeta de los Simios", "Ciencia Ficción", "R.drawable.simios", "4.6"),
        Movie("Gladiador II", "Acción", "R.drawable.gladiador", "4.8")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnBack = findViewById<ImageView>(R.id.btnBackSearch)
        val rvResults = findViewById<RecyclerView>(R.id.rvSearchResults)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Botón de regresar
        btnBack.setOnClickListener {
            finish()
        }

        // Configurar el adaptador con todas las películas al inicio
        adapter = SearchAdapter(allMovies)
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        // Filtrar cuando el usuario escribe
        etSearch.setOnEditorActionListener { _, _, _ ->
            val query = etSearch.text.toString()
            val filteredList = if (query.isEmpty()) {
                allMovies
            } else {
                allMovies.filter { it.title.lowercase().contains(query.lowercase()) }
            }
            adapter = SearchAdapter(filteredList)
            rvResults.adapter = adapter
            true
        }

        // Barra de navegación
        bottomNav.selectedItemId = R.id.nav_search

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_search -> true
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