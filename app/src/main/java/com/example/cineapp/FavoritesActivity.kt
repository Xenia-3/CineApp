package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class FavoritesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val btnBack = findViewById<ImageView>(R.id.btnBackFav)
        val rvFavorites = findViewById<RecyclerView>(R.id.rvFavorites)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Botón regresar
        btnBack.setOnClickListener {
            finish()
        }

        // LEEMOS los favoritos reales guardados
        val favoriteManager = FavoriteManager(this)
        val favorites = favoriteManager.getFavorites()

        val adapter = FavoritesAdapter(favorites) { movie ->
            favoriteManager.removeFavorite(movie.title)
            rvFavorites.adapter = FavoritesAdapter(favoriteManager.getFavorites()) { updatedMovie ->
                favoriteManager.removeFavorite(updatedMovie.title)
                rvFavorites.adapter = FavoritesAdapter(favoriteManager.getFavorites()) { m ->
                    favoriteManager.removeFavorite(m.title)
                    rvFavorites.adapter = FavoritesAdapter(favoriteManager.getFavorites()) { _ -> }
                }
            }
        }
        rvFavorites.layoutManager = LinearLayoutManager(this)
        rvFavorites.adapter = adapter

        // Barra de navegación
        bottomNav.selectedItemId = R.id.nav_favorites

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
                R.id.nav_tickets -> {
                    val intent = Intent(this, TicketsActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_favorites -> true
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