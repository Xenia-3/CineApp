package com.example.cineapp


import com.bumptech.glide.Glide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdaptadorPeliculas(private val movieList: List<Pelicula>) : RecyclerView.Adapter<AdaptadorPeliculas.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMovie: ImageView = itemView.findViewById(R.id.imgMovie)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvGenre: TextView = itemView.findViewById(R.id.tvGenre)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val layoutRating: LinearLayout = itemView.findViewById(R.id.layoutRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.elemento_pelicula, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movieList[position]
        holder.tvTitle.text = movie.title
        holder.tvGenre.text = movie.genre
        holder.tvRating.text = movie.rating

        if (movie.rating.isEmpty()) {
            holder.layoutRating.visibility = View.GONE
        } else {
            holder.layoutRating.visibility = View.VISIBLE
        }

        Glide.with(holder.itemView).load(movie.imageUrl.ifEmpty { null })
            .placeholder(R.drawable.icono_aplicacion_fondo)
            .error(R.drawable.icono_aplicacion_fondo)
            .into(holder.imgMovie)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            (context as ActividadPrincipal).showMovie(movie.id, upcoming = false)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }
}
