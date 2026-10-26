package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import retrofit2.Call

class FragmentoDetallePelicula : Fragment(R.layout.pantalla_detalle_pelicula) {
    private var detailCall: Call<PeliculaTmdb>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireView().findViewById<ImageView>(R.id.btnBack).setOnClickListener { parentFragmentManager.popBackStack() }
        val favoriteButton = requireView().findViewById<ImageView>(R.id.btnFav)
        favoriteButton.isEnabled = false
        val showtimesButton = requireView().findViewById<Button>(R.id.btnShowtimes)
        val upcoming = requireArguments().getBoolean("upcoming")
        showtimesButton.visibility = if (upcoming) View.GONE else View.VISIBLE
        showtimesButton.isEnabled = false
        val id = requireArguments().getInt("movieId")
        if (id <= 0) {
            showError("No se encontró el identificador de la película")
            return
        }
        detailCall = ClienteTmdb.api.detail(id)
        ClienteTmdb.request(detailCall!!, { data ->
            if (isAdded && this.view != null) {
                val movie = data.toMovie()
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                val isUpcoming = data.releaseDate?.takeIf { it.isNotBlank() }?.let { it > today } ?: upcoming
                showtimesButton.visibility = if (isUpcoming) View.GONE else View.VISIBLE
                showtimesButton.isEnabled = !isUpcoming
                showtimesButton.setOnClickListener {
                    if (isUpcoming) return@setOnClickListener
                    (requireActivity() as ActividadPrincipal).showShowtimes(Bundle().apply {
                        putInt("movieId", movie.id)
                        putString("title", movie.title)
                        putString("image", movie.imageUrl)
                    })
                }
                requireView().findViewById<TextView>(R.id.tvTitleDetail).text = movie.title
                val duration = data.runtime?.takeIf { it > 0 }?.let { "$it min" } ?: "Duración no disponible"
                val date = data.releaseDate?.takeIf { it.isNotBlank() } ?: "Fecha no disponible"
                val status = if (isUpcoming) "Próximamente · " else ""
                requireView().findViewById<TextView>(R.id.tvInfo).text = "$status$date · $duration · ${movie.genre}"
                requireView().findViewById<TextView>(R.id.tvDetailRating).text = "⭐ ${movie.rating}"
                requireView().findViewById<TextView>(R.id.tvSynopsis).text =
                    data.overview?.takeIf { it.isNotBlank() } ?: "Sinopsis no disponible en español"
                Glide.with(this).load(movie.imageUrl.ifEmpty { null })
                    .placeholder(R.drawable.icono_aplicacion_fondo)
                    .error(R.drawable.icono_aplicacion_fondo)
                    .into(requireView().findViewById(R.id.imgDetail))
                val favorites = GestorFavoritos(requireContext())
                fun updateHeart() {
                    favoriteButton.setColorFilter(requireContext().getColor(
                        if (favorites.isFavorite(movie.id)) R.color.accent_red else R.color.white
                    ))
                }
                favoriteButton.isEnabled = true
                try {
                    updateHeart()
                } catch (_: Exception) {
                    showError(getString(R.string.error_consultar_favoritos))
                }
                favoriteButton.setOnClickListener {
                    try {
                        if (favorites.isFavorite(movie.id)) favorites.removeFavorite(movie.id)
                        else favorites.saveFavorite(movie)
                        updateHeart()
                    } catch (_: Exception) {
                        showError(getString(R.string.error_cambios_favoritos))
                    }
                }
            }
        }, { showError(it) })
    }

    private fun showError(message: String) {
        if (isAdded && this.view != null) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        detailCall?.cancel()
        super.onDestroyView()
    }
}
