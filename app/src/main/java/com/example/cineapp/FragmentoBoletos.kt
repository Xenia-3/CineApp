package com.example.cineapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException


class FragmentoBoletos : Fragment(R.layout.pantalla_boletos) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTickets = requireView().findViewById<RecyclerView>(R.id.rvTickets)

        rvTickets.layoutManager = LinearLayoutManager(requireContext())
        rvTickets.adapter = AdaptadorBoletos(emptyList())

    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val context = requireContext().applicationContext
        requireView().findViewById<TextView>(R.id.tvSubTitle).setText(R.string.cargando_boletos)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tickets = withContext(Dispatchers.IO) { GestorBoletos(context).getTickets(uid) }
                requireView().findViewById<RecyclerView>(R.id.rvTickets).adapter = AdaptadorBoletos(tickets)
                requireView().findViewById<TextView>(R.id.tvSubTitle).text =
                    if (tickets.isEmpty()) "Aún no tienes compras" else "Tus compras: ${tickets.size}"
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                requireView().findViewById<TextView>(R.id.tvSubTitle).setText(R.string.error_consultar_boletos)
                Toast.makeText(context, R.string.error_consultar_boletos, Toast.LENGTH_LONG).show()
            }
        }
    }
}
