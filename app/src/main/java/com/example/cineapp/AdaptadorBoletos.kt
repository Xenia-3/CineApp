package com.example.cineapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class AdaptadorBoletos(private val tickets: List<Boleto>) : RecyclerView.Adapter<AdaptadorBoletos.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.imgTicket)
        val title: TextView = view.findViewById(R.id.tvTicketTitle)
        val details: TextView = view.findViewById(R.id.tvTicketDetails)
        val total: TextView = view.findViewById(R.id.tvTicketTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.elemento_boleto, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val ticket = tickets[position]
        val details = "${ticket.cinema}\n${ticket.date} · ${ticket.time}\n${ticket.format}\n" +
            "Asientos: ${ticket.seats.joinToString(", ")}"
        val total = String.format(Locale.US, "Total: $%.2f", ticket.totalCents / 100.0)
        holder.title.text = ticket.title
        holder.details.text = details
        holder.total.text = total
        Glide.with(holder.itemView).load(ticket.poster.ifEmpty { null })
            .placeholder(R.color.card_dark).error(R.color.card_dark).into(holder.poster)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            context.startActivity(ActividadDetalleBoleto.createIntent(context, ticket.purchaseId))
        }
    }

    override fun getItemCount() = tickets.size
}
