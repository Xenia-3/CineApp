package com.example.cineapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShowtimeAdapter(private val showtimeList: List<Showtime>) : RecyclerView.Adapter<ShowtimeAdapter.ShowtimeViewHolder>() {

    class ShowtimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCinema: TextView = itemView.findViewById(R.id.tvCinema)
        val tvFormat: TextView = itemView.findViewById(R.id.tvFormat)
        val layoutTimes: LinearLayout = itemView.findViewById(R.id.layoutTimes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowtimeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_showtime, parent, false)
        return ShowtimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowtimeViewHolder, position: Int) {
        val showtime = showtimeList[position]
        holder.tvCinema.text = showtime.cinema
        holder.tvFormat.text = showtime.format

        // Limpiamos el contenedor para no duplicar botones
        holder.layoutTimes.removeAllViews()

        // Creamos los botones de horas dinámicamente
        showtime.times.forEachIndexed { index, time ->
            val tvTime = TextView(holder.itemView.context)
            tvTime.text = time
            tvTime.textSize = 14f
            tvTime.setPadding(40, 20, 40, 20) // padding interno

            // Alternamos colores: el primero rojo, los demás grises (como en tu diseño)
            if (index == 0) {
                tvTime.setBackgroundColor(android.graphics.Color.parseColor("#E50914")) // Rojo
                tvTime.setTextColor(android.graphics.Color.WHITE)
            } else {
                tvTime.setBackgroundColor(android.graphics.Color.parseColor("#333333")) // Gris
                tvTime.setTextColor(android.graphics.Color.WHITE)
            }

            // Margen entre botones
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) params.marginStart = 16

            tvTime.layoutParams = params
            holder.layoutTimes.addView(tvTime)
        }
    }

    override fun getItemCount(): Int {
        return showtimeList.size
    }
}