package com.example.cineapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    private lateinit var favoriteManager: FavoriteManager
    private lateinit var movie: Movie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Encontramos las vistas
        val imgDetail = findViewById<ImageView>(R.id.imgDetail)
        val tvTitleDetail = findViewById<TextView>(R.id.tvTitleDetail)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnShowtimes = findViewById<Button>(R.id.btnShowtimes)
        val btnFav = findViewById<ImageView>(R.id.btnFav)

        // Inicializamos el manager de favoritos
        favoriteManager = FavoriteManager(this)

        // Recibimos los datos
        val title = intent.getStringExtra("title")
        val genre = intent.getStringExtra("genre")
        val image = intent.getStringExtra("image")

        movie = Movie(title ?: "", genre ?: "", image ?: "", "4.7")

        // Los ponemos en pantalla
        tvTitleDetail.text = movie.title
        // Cargar la imagen local
        val imageResId = resources.getIdentifier(
            movie.imageUrl.replace("R.drawable.", ""), "drawable", packageName
        )
        if (imageResId != 0) {
            imgDetail.setImageResource(imageResId)
        } else {
            imgDetail.setImageResource(R.drawable.ic_launcher_background)
        }

        // Configurar el estado inicial del corazón
        updateHeartIcon(btnFav)

        // Botón para regresar
        btnBack.setOnClickListener {
            finish()
        }

        // Botón para ir a horarios
        btnShowtimes.setOnClickListener {
            startActivity(android.content.Intent(this@DetailActivity, ShowtimesActivity::class.java).apply {
                putExtra("title", movie.title)
                putExtra("image", movie.imageUrl)
            })
        }

        // Acción del corazón
        btnFav.setOnClickListener {
            if (favoriteManager.isFavorite(movie.title)) {
                favoriteManager.removeFavorite(movie.title)
            } else {
                favoriteManager.saveFavorite(movie)
            }
            updateHeartIcon(btnFav)
        }
    }

    private fun updateHeartIcon(btnFav: ImageView) {
        if (favoriteManager.isFavorite(movie.title)) {
            btnFav.setColorFilter(getColor(R.color.accent_red))
            btnFav.setImageResource(R.drawable.ic_favorite)
        } else {
            btnFav.setColorFilter(getColor(R.color.white))
            btnFav.setImageResource(R.drawable.ic_favorite)
        }
    }
}