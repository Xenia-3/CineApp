package com.example.cineapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FragmentoFavoritos : Fragment(R.layout.pantalla_favoritos) {
    private var displayedFavorites: List<Pelicula>? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayedFavorites = null
        requireView().findViewById<ImageView>(R.id.btnBackFav).setOnClickListener {
            (requireActivity() as ActividadPrincipal).showSection(R.id.nav_home)
        }
        requireView().findViewById<RecyclerView>(R.id.rvFavorites).layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        refreshFavorites()
    }

    private fun refreshFavorites() {
        val manager = GestorFavoritos(requireContext())
        val favorites = try {
            manager.getFavorites()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_consultar_favoritos, Toast.LENGTH_LONG).show()
            return
        }
        if (favorites == displayedFavorites) return
        displayedFavorites = favorites
        requireView().findViewById<RecyclerView>(R.id.rvFavorites).adapter =
            AdaptadorFavoritos(favorites) { movie ->
                try {
                    manager.removeFavorite(movie.id)
                    refreshFavorites()
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), R.string.error_cambios_favoritos, Toast.LENGTH_LONG).show()
                }
            }
    }

}
