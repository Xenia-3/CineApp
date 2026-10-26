package com.example.cineapp

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import java.util.Locale

class FragmentoAsientos : Fragment(R.layout.pantalla_asientos) {
    private val rows = linkedMapOf(
        'A' to listOf(3, 4, 5, 6),
        'B' to (1..8).toList(),
        'C' to listOf(1, 2, 6, 7, 8),
        'D' to (1..8).toList(),
        'E' to (1..8).toList(),
        'F' to (1..8).toList(),
        'G' to listOf(3, 4, 5, 6)
    )
    private val selectedSeats = mutableSetOf<String>()
    private val seatViews = linkedMapOf<String, TextView>()
    private var occupiedSeats = emptySet<String>()
    private var occupancyKnown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.btnBackSeats).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val validSeats = rows.flatMap { (row, numbers) -> numbers.map { "$row$it" } }.toSet()
        occupancyKnown = false
        selectedSeats.clear()
        selectedSeats.addAll(savedInstanceState?.getStringArrayList("selectedSeats").orEmpty()
            .filter { it in validSeats })
        seatViews.clear()
        val room = view.findViewById<LinearLayout>(R.id.layoutSeats)
        createSeats(room)
        updateSummary()
        view.findViewById<Button>(R.id.btnContinueSeats).setOnClickListener {
            if (!occupancyKnown) {
                loadOccupiedSeats()
                return@setOnClickListener
            }
            if (!loadOccupiedSeats()) return@setOnClickListener
            if (selectedSeats.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona al menos un asiento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val seats = selectedSeats.sorted()
            val selection = Bundle(requireArguments()).apply {
                putStringArrayList("seats", ArrayList(seats))
                putIntArray("seatPricesCents", seats.map { priceCents(it) }.toIntArray())
                putInt("ticketCount", seats.size)
                putInt("totalCents", seats.sumOf { priceCents(it) })
            }
            (requireActivity() as ActividadPrincipal).showPayment(selection)
        }
    }

    private fun createSeats(room: LinearLayout) {
        for ((letter, numbers) in rows) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
            }
            fun addRowLabel() {
                row.addView(TextView(requireContext()).apply {
                    text = letter.toString()
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(requireContext().getColor(R.color.text_gray))
                }, LinearLayout.LayoutParams(dp(16), LinearLayout.LayoutParams.MATCH_PARENT))
            }
            addRowLabel()
            for (number in 1..8) {
                if (number == 5) row.addView(View(requireContext()), LinearLayout.LayoutParams(dp(12), 1))
                val slot = FrameLayout(requireContext())
                row.addView(slot, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                if (number !in numbers) continue
                val id = "$letter$number"
                val seat = TextView(requireContext()).apply {
                    text = id
                    textSize = 9f
                    gravity = Gravity.CENTER
                    isFocusable = true
                    setTextColor(requireContext().getColor(R.color.white))
                }
                slot.addView(seat, FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER))
                slot.doOnLayout {
                    val size = minOf(dp(30), (it.width - dp(4)).coerceAtLeast(1))
                    seat.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                }
                seat.setOnClickListener {
                    if (occupancyKnown && id !in occupiedSeats) {
                        if (id in selectedSeats) selectedSeats.remove(id)
                        else selectedSeats.add(id)
                        updateSummary()
                    }
                }
                seatViews[id] = seat
            }
            addRowLabel()
            room.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        loadOccupiedSeats()
    }

    private fun loadOccupiedSeats(): Boolean {
        return try {
            val seats = GestorBoletos(requireContext()).occupiedSeats(requireArguments())
            updateOccupiedSeats(seats)
            true
        } catch (_: Exception) {
            occupancyKnown = false
            updateSummary()
            Toast.makeText(requireContext(), R.string.error_consultar_asientos, Toast.LENGTH_LONG).show()
            false
        }
    }

    fun updateOccupiedSeats(seats: Set<String>) {
        occupiedSeats = seats
        occupancyKnown = true
        selectedSeats.removeAll(occupiedSeats)
        if (view != null) updateSummary()
    }

    private fun updateSummary() {
        val seats = selectedSeats.sorted()
        seatViews.forEach { (id, seat) ->
            val occupied = id in occupiedSeats
            seat.isEnabled = occupancyKnown && !occupied
            seat.isSelected = id in selectedSeats
            seat.setBackgroundResource(when {
                occupied -> R.drawable.fondo_asiento_ocupado
                seat.isSelected -> R.drawable.fondo_asiento_seleccionado
                else -> R.drawable.fondo_asiento_disponible
            })
            val state = if (occupied) "Ocupado" else if (seat.isSelected) "Seleccionado" else "Disponible"
            seat.contentDescription = "$id, $state, ${money(priceCents(id))}"
        }
        requireView().findViewById<View>(R.id.layoutSeats).visibility =
            if (occupancyKnown) View.VISIBLE else View.INVISIBLE
        requireView().findViewById<TextView>(R.id.tvSelectedSeats).text =
            if (!occupancyKnown) getString(R.string.asientos_sin_verificar)
            else if (seats.isEmpty()) "Ninguno" else seats.joinToString(", ")
        requireView().findViewById<TextView>(R.id.tvTicketCount).text =
            "${seats.size} ${if (seats.size == 1) "boleto" else "boletos"}"
        requireView().findViewById<TextView>(R.id.tvSeatsTotal).text = money(seats.sumOf { priceCents(it) })
        requireView().findViewById<Button>(R.id.btnContinueSeats).apply {
            setText(if (occupancyKnown) R.string.continuar_asientos else R.string.reintentar_consulta)
            isEnabled = !occupancyKnown || seats.isNotEmpty()
        }
    }

    private fun priceCents(seat: String): Int = when (seat.first()) {
        'A', 'B' -> 400
        'C', 'D' -> 500
        else -> 575
    }

    private fun money(cents: Int) = String.format(Locale.US, "$%.2f", cents / 100.0)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("selectedSeats", ArrayList(selectedSeats))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        seatViews.clear()
        super.onDestroyView()
    }
}
