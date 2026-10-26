package com.example.cineapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActividadDetalleBoleto : AppCompatActivity() {
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_detalle_boleto)
        findViewById<View>(R.id.btnBackTicketDetail).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val purchaseId = intent.getStringExtra(EXTRA_PURCHASE_ID)
        val content = findViewById<View>(R.id.ticketDetailContent)
        content.visibility = View.INVISIBLE
        if (uid == null || purchaseId.isNullOrBlank()) {
            unavailable()
            return
        }
        loadJob = lifecycleScope.launch {
            try {
                val ticket = withContext(Dispatchers.IO) {
                    GestorBoletos(applicationContext).getTickets(uid)
                        .firstOrNull { it.purchaseId == purchaseId }
                }
                if (ticket == null || FirebaseAuth.getInstance().currentUser?.uid != uid) {
                    unavailable()
                    return@launch
                }
                findViewById<TextView>(R.id.tvTicketMovie).text = ticket.title
                findViewById<TextView>(R.id.tvTicketCinema).text = ticket.cinema
                findViewById<TextView>(R.id.tvTicketDateTime).text = "${ticket.date} · ${ticket.time}"
                findViewById<TextView>(R.id.tvTicketSeats).text = ticket.seats.joinToString(", ")
                content.visibility = View.VISIBLE
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(this@ActividadDetalleBoleto, R.string.error_consultar_boleto, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onStop() {
        loadJob?.cancel()
        super.onStop()
    }

    private fun unavailable() {
        Toast.makeText(this, "No se encontró el boleto en tu cuenta", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val EXTRA_PURCHASE_ID = "purchase_id"

        fun createIntent(context: Context, purchaseId: String): Intent =
            Intent(context, ActividadDetalleBoleto::class.java).putExtra(EXTRA_PURCHASE_ID, purchaseId)
    }
}
