package com.example.cineapp

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FragmentoHorarios : Fragment(R.layout.pantalla_horarios) {
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var selectedFormat: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.btnBackShow).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<TextView>(R.id.tvMovieName).text = requireArguments().getString("title")
        Glide.with(this).load(requireArguments().getString("image"))
            .placeholder(R.color.card_dark).error(R.color.card_dark)
            .into(view.findViewById<ImageView>(R.id.imgMini))
        val cinema = requireContext().getSharedPreferences("CinemaSelection", Context.MODE_PRIVATE)
        val cinemaName = cinema.getString("cinemaName", "")
        view.findViewById<TextView>(R.id.tvSelectedCinema).text = cinemaName
        val datesLayout = view.findViewById<LinearLayout>(R.id.layoutDates)
        val continueButton = view.findViewById<Button>(R.id.btnContinue)
        val dateCards = mutableListOf<Pair<View, String>>()
        val timeButtons = mutableListOf<Chip>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.forLanguageTag("es-SV"))
        val today = Calendar.getInstance()
        val screeningFormat = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US).apply { isLenient = false }
        fun isFutureTime(time: String): Boolean {
            val date = selectedDate ?: return false
            return screeningFormat.parse("$date $time")?.after(java.util.Date()) == true
        }
        selectedDate = null
        selectedTime = null
        selectedFormat = null

        fun updateSelection() {
            if (selectedTime != null && !isFutureTime(selectedTime!!)) {
                selectedTime = null
                selectedFormat = null
            }
            dateCards.forEach { (card, date) ->
                card.isSelected = date == selectedDate
                card.findViewById<TextView>(R.id.tvDateDay).setTextColor(requireContext().getColor(
                    if (card.isSelected) R.color.white else R.color.text_gray
                ))
            }
            timeButtons.forEach { chip ->
                chip.isEnabled = isFutureTime(chip.text.toString())
                chip.alpha = if (chip.isEnabled) 1f else 0.4f
                chip.isSelected = chip.text.toString() == selectedTime && chip.tag == selectedFormat
                chip.chipBackgroundColor = ColorStateList.valueOf(requireContext().getColor(
                    if (chip.isSelected) R.color.accent_red else R.color.card_dark
                ))
            }
            continueButton.isEnabled = selectedDate != null && selectedTime != null
        }

        for (offset in 0..6) {
            val date = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val value = dateFormat.format(date.time)
            val day = when (offset) {
                0 -> "Hoy"
                1 -> "Mañana"
                else -> dayFormat.format(date.time).replaceFirstChar { it.uppercase() }.trimEnd('.')
            }
            val card = layoutInflater.inflate(R.layout.elemento_fecha_funcion, datesLayout, false)
            card.findViewById<TextView>(R.id.tvDateDay).text = day
            card.findViewById<TextView>(R.id.tvDateNumber).text = date.get(Calendar.DAY_OF_MONTH).toString()
            card.contentDescription = "$day, $value"
            card.isSaveEnabled = false
            card.setOnClickListener {
                selectedDate = value
                updateSelection()
            }
            datesLayout.addView(card)
            dateCards.add(card to value)
            if (value == savedInstanceState?.getString("date")) selectedDate = value
        }

        val showtimes = listOf(
            Horario(cinemaName.orEmpty(), "2D ESPAÑOL", listOf("12:00 PM", "2:30 PM", "5:00 PM", "7:30 PM")),
            Horario(cinemaName.orEmpty(), "3D SUBTITULADA", listOf("1:10 PM", "7:45 PM"))
        )
        showtimes.forEachIndexed { index, showtime ->
            val group = view.findViewById<ChipGroup>(if (index == 0) R.id.times2d else R.id.times3d)
            showtime.times.forEach { time ->
                val chip = Chip(requireContext()).apply {
                    text = time
                    tag = showtime.format
                    isCheckable = false
                    isSaveEnabled = false
                    setTextColor(requireContext().getColor(R.color.white))
                    textSize = 13f
                    chipStrokeColor = ColorStateList.valueOf(requireContext().getColor(R.color.accent_red))
                    chipStrokeWidth = resources.displayMetrics.density
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(8 * resources.displayMetrics.density).build()
                    setEnsureMinTouchTargetSize(true)
                    contentDescription = "$time, ${showtime.format}"
                }
                chip.setOnClickListener {
                    selectedTime = time
                    selectedFormat = showtime.format
                    updateSelection()
                }
                group.addView(chip)
                timeButtons.add(chip)
                if (time == savedInstanceState?.getString("time") &&
                    showtime.format == savedInstanceState.getString("format")) {
                    selectedTime = time
                    selectedFormat = showtime.format
                }
            }
        }
        updateSelection()
        continueButton.setOnClickListener {
            if (selectedDate == null || selectedTime == null || !isFutureTime(selectedTime!!)) {
                updateSelection()
                Toast.makeText(requireContext(), "Selecciona una fecha y un horario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selection = Bundle(requireArguments()).apply {
                putString("cinemaId", cinema.getString("cinemaId", ""))
                putString("cinemaName", cinemaName)
                putString("date", selectedDate)
                putString("time", selectedTime)
                putString("format", selectedFormat)
            }
            (requireActivity() as ActividadPrincipal).showSeats(selection)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("date", selectedDate)
        outState.putString("time", selectedTime)
        outState.putString("format", selectedFormat)
        super.onSaveInstanceState(outState)
    }
}
