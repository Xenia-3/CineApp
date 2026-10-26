package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call

class FragmentoBusqueda : Fragment(R.layout.pantalla_busqueda) {
    private var movieCall: Call<RespuestaPeliculas>? = null
    private var genreCall: Call<RespuestaGeneros>? = null
    private lateinit var results: RecyclerView
    private lateinit var searchInput: EditText
    private var genresReady = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchInput = requireView().findViewById(R.id.etSearch)
        results = requireView().findViewById(R.id.rvSearchResults)
        results.layoutManager = LinearLayoutManager(requireContext())
        results.adapter = AdaptadorBusqueda(emptyList())
        requireView().findViewById<ImageView>(R.id.btnBackSearch).setOnClickListener {
            (requireActivity() as ActividadPrincipal).showSection(R.id.nav_home)
        }
        searchInput.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE) {
                if (genresReady) searchMovies()
                true
            } else false
        }
        if (ClienteTmdb.genreNames.isNotEmpty()) {
            genresReady = true
            searchMovies()
        } else {
            genreCall = ClienteTmdb.api.genres()
            ClienteTmdb.request(genreCall!!, { response ->
                if (isAdded && this.view != null) {
                    ClienteTmdb.genreNames = response.genres.associate { it.id to it.name }
                    genresReady = true
                    searchMovies()
                }
            }, { showError(it) })
        }
    }

    private fun searchMovies() {
        movieCall?.cancel()
        results.adapter = AdaptadorBusqueda(emptyList())
        val query = searchInput.text.toString().trim()
        movieCall = if (query.isEmpty()) ClienteTmdb.api.movies("now_playing")
            else ClienteTmdb.api.search(query)
        ClienteTmdb.request(movieCall!!, { response ->
            if (isAdded && this.view != null) {
                results.adapter = AdaptadorBusqueda(response.results.map { it.toMovie() })
                if (response.results.isEmpty()) showError("No se encontraron películas")
            }
        }, { showError(it) })
    }

    private fun showError(message: String) {
        if (isAdded && this.view != null) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        movieCall?.cancel()
        genreCall?.cancel()
        super.onDestroyView()
    }
}
