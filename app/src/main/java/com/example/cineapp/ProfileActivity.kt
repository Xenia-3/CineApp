package com.example.cineapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 👇 LEEMOS EL CORREO GUARDADO
        val prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("email", "No has iniciado sesión")

        // 👇 LO MOSTRAMOS EN LA PANTALLA
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        tvEmail.text = savedEmail

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavProfile)
        bottomNav.selectedItemId = R.id.nav_profile

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
                R.id.nav_favorites -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}