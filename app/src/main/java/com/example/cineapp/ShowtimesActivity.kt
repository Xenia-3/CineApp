package com.example.cineapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShowtimesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showtimes)

        // Configuración de vistas
        val btnBack = findViewById<ImageView>(R.id.btnBackShow)
        val tvMovieName = findViewById<TextView>(R.id.tvMovieName)
        val imgMini = findViewById<ImageView>(R.id.imgMini)
        val rvShowtimes = findViewById<RecyclerView>(R.id.rvShowtimes)

        // Recibir datos
        val title = intent.getStringExtra("title")
        val image = intent.getStringExtra("image")

        tvMovieName.text = title

        // Cargar la imagen local
        val imageResId = resources.getIdentifier(
            image?.replace("R.drawable.", "") ?: "", "drawable", packageName
        )
        if (imageResId != 0) {
            imgMini.setImageResource(imageResId)
        } else {
            imgMini.setImageResource(R.drawable.ic_launcher_background)
        }

        // Botón regresar
        btnBack.setOnClickListener {
            finish()
        }

        // CREAMOS LA LISTA DE DATOS QUE FALTA
        val showtimes = listOf(
            Showtime("Multicinema Plaza mundo appa", "2D ESPAÑOL", listOf("12:00 PM", "3:40 PM", "8:20 PM")),
            Showtime("Multicinema Plaza mundo appa", "3D SUBTITULADA", listOf("1:10 PM", "7:45 PM")),
            Showtime("Cinepolis Galerías", "2D ESPAÑOL", listOf("4:00 PM", "10:00 PM")),
            Showtime("Cinepolis Galerías", "2D SUBTITULADA", listOf("6:00 PM"))
        )

        // Configuramos la lista
        val adapter = ShowtimeAdapter(showtimes)
        rvShowtimes.layoutManager = LinearLayoutManager(this)
        rvShowtimes.adapter = adapter
    }
}