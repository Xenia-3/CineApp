package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ActividadSeleccionCine : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, ActividadInicioSesion::class.java))
            finish()
            return
        }
        setContentView(R.layout.pantalla_seleccion_cine)
        findViewById<Button>(R.id.btnApopa).setOnClickListener {
            selectCinema("apopa", "Multicinema Plaza Mundo Apopa")
        }
        findViewById<Button>(R.id.btnSoyapango).setOnClickListener {
            selectCinema("soyapango", "Multicinema Plaza Mundo Soyapango")
        }
    }

    private fun selectCinema(id: String, name: String) {
        getSharedPreferences("CinemaSelection", MODE_PRIVATE).edit()
            .putString("cinemaId", id)
            .putString("cinemaName", name)
            .apply()
        startActivity(Intent(this, ActividadPrincipal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

