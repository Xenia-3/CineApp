package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ActividadPrincipal : AppCompatActivity() {
    private lateinit var navigation: BottomNavigationView
    private var upcomingMovieIds = emptySet<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upcomingMovieIds = savedInstanceState?.getIntArray("upcomingMovieIds")?.toSet() ?: emptySet()
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, ActividadInicioSesion::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }
        val cinema = getSharedPreferences("CinemaSelection", MODE_PRIVATE)
        if (cinema.getString("cinemaId", null) !in listOf("apopa", "soyapango")) {
            startActivity(Intent(this, ActividadSeleccionCine::class.java))
            finish()
            return
        }
        setContentView(R.layout.pantalla_principal)
        navigation = findViewById(R.id.bottomNav)
        navigation.selectedItemId = savedInstanceState?.getInt("section", R.id.nav_home) ?: R.id.nav_home
        navigation.setOnItemSelectedListener { displaySection(it.itemId) }
        navigation.setOnItemReselectedListener {
            if (supportFragmentManager.backStackEntryCount > 0) displaySection(it.itemId)
        }
        if (savedInstanceState == null) {
            displaySection(R.id.nav_home)
        }
    }

    fun showSection(itemId: Int) {
        if (navigation.selectedItemId == itemId) displaySection(itemId)
        else navigation.selectedItemId = itemId
    }

    private fun displaySection(itemId: Int): Boolean {
        val tag = "section_$itemId"
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val screen = existing ?: when (itemId) {
            R.id.nav_home -> FragmentoInicio()
            R.id.nav_search -> FragmentoBusqueda()
            R.id.nav_tickets -> FragmentoBoletos()
            R.id.nav_favorites -> FragmentoFavoritos()
            R.id.nav_profile -> FragmentoPerfil()
            else -> return false
        }
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction().apply {
            supportFragmentManager.fragments.filter { it.isAdded && it != screen }.forEach {
                hide(it)
                setMaxLifecycle(it, Lifecycle.State.STARTED)
            }
            if (screen.isAdded) show(screen) else add(R.id.sectionContent, screen, tag)
            setMaxLifecycle(screen, Lifecycle.State.RESUMED)
            setPrimaryNavigationFragment(screen)
        }.commitNow()
        return true
    }

    fun setUpcomingMovies(ids: List<Int>) {
        upcomingMovieIds = ids.toSet()
    }

    fun showMovie(movieId: Int, upcoming: Boolean? = null) {
        openScreen(FragmentoDetallePelicula().apply {
            arguments = Bundle().apply {
                putInt("movieId", movieId)
                putBoolean("upcoming", upcoming ?: (movieId in upcomingMovieIds))
            }
        })
    }

    fun showShowtimes(movie: Bundle) {
        openScreen(FragmentoHorarios().apply { arguments = movie })
    }

    fun showSeats(selection: Bundle) {
        openScreen(FragmentoAsientos().apply { arguments = selection })
    }

    fun showPayment(selection: Bundle) {
        openScreen(FragmentoPago().apply { arguments = selection })
    }

    private fun openScreen(screen: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            supportFragmentManager.fragments.filter { it.isAdded && !it.isHidden }.forEach {
                hide(it)
                setMaxLifecycle(it, Lifecycle.State.STARTED)
            }
            add(R.id.sectionContent, screen)
            setPrimaryNavigationFragment(screen)
            addToBackStack(null)
        }.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray("upcomingMovieIds", upcomingMovieIds.toIntArray())
        if (::navigation.isInitialized) outState.putInt("section", navigation.selectedItemId)
        super.onSaveInstanceState(outState)
    }
}
