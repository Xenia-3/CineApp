package com.example.cineapp

import android.os.Bundle
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import android.view.View
import retrofit2.Call
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class FragmentoInicio : Fragment(R.layout.pantalla_inicio) {
    private val requests = mutableListOf<Call<*>>()
    private var nowPlayingIds = emptySet<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cinema = requireContext().getSharedPreferences("CinemaSelection", Context.MODE_PRIVATE)
        requireView().findViewById<TextView>(R.id.tvSelectedCinema).text = cinema.getString("cinemaName", "")

        requireView().findViewById<RecyclerView>(R.id.rvMovies).layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        requireView().findViewById<RecyclerView>(R.id.rvRecomendadas).layoutManager = LinearLayoutManager(requireContext())
        val genresCall = ClienteTmdb.api.genres()
        requests.add(genresCall)
        ClienteTmdb.request(genresCall, { response ->
            if (isAdded && this.view != null) {
                ClienteTmdb.genreNames = response.genres.associate { it.id to it.name }
                loadMovies("now_playing")
            }
        }, { showError(it) })


    }

    private fun loadMovies(category: String) {
        val call = ClienteTmdb.api.movies(category)
        requests.add(call)
        ClienteTmdb.request(call, { response ->
            if (isAdded && this.view != null) {
                val results = if (category == "upcoming") {
                    response.results.distinctBy { it.id }
                        .sortedBy { it.id in nowPlayingIds }
                        .take(6)
                } else response.results
                val movies = results.map { it.toMovie() }
                when (category) {
                    "now_playing" -> {
                        nowPlayingIds = movies.map { it.id }.toSet()
                        requireView().findViewById<RecyclerView>(R.id.rvMovies).adapter = AdaptadorPeliculas(movies)
                        loadMovies("upcoming")
                    }
                    "upcoming" -> {
                        (requireActivity() as ActividadPrincipal).setUpcomingMovies(movies.map { it.id })
                        requireView().findViewById<RecyclerView>(R.id.rvRecomendadas).adapter = AdaptadorPeliculasVertical(movies, upcoming = true)
                        requireView().findViewById<View>(R.id.tvUpcomingEmpty).visibility =
                            if (movies.isEmpty()) View.VISIBLE else View.GONE

                    }
                }
                if (movies.isEmpty()) showError("No se encontraron películas en esta sección")
            }
        }, { showError(it) })
    }

    private fun showError(message: String) {
        if (isAdded && this.view != null) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        requests.forEach { it.cancel() }
        super.onDestroyView()
    }

}
