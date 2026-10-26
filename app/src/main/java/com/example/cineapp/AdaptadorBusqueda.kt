package com.example.cineapp


import com.bumptech.glide.Glide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdaptadorBusqueda(private val movieList: List<Pelicula>) : RecyclerView.Adapter<AdaptadorBusqueda.SearchViewHolder>() {

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgVertical: ImageView = itemView.findViewById(R.id.imgVertical)
        val tvTitleVertical: TextView = itemView.findViewById(R.id.tvTitleVertical)
        val tvGenreVertical: TextView = itemView.findViewById(R.id.tvGenreVertical)
        val tvRatingVertical: TextView = itemView.findViewById(R.id.tvRatingVertical)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.elemento_pelicula_vertical, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val movie = movieList[position]
        holder.tvTitleVertical.text = movie.title
        holder.tvGenreVertical.text = movie.genre
        holder.tvRatingVertical.text = movie.rating

        Glide.with(holder.itemView).load(movie.imageUrl.ifEmpty { null })
            .placeholder(R.drawable.icono_aplicacion_fondo)
            .error(R.drawable.icono_aplicacion_fondo)
            .into(holder.imgVertical)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            (context as ActividadPrincipal).showMovie(movie.id)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }
}
