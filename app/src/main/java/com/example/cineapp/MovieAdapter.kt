package com.example.cineapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MovieAdapter(private val movieList: List<Movie>) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMovie: ImageView = itemView.findViewById(R.id.imgMovie)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvGenre: TextView = itemView.findViewById(R.id.tvGenre)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val layoutRating: LinearLayout = itemView.findViewById(R.id.layoutRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
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

        // Cargar la imagen local con el ID del drawable
        val imageResId = holder.itemView.resources.getIdentifier(
            movie.imageUrl.replace("R.drawable.", ""), "drawable", holder.itemView.context.packageName
        )
        if (imageResId != 0) {
            holder.imgMovie.setImageResource(imageResId)
        } else {
            holder.imgMovie.setImageResource(R.drawable.ic_launcher_background)
        }

        // Clic para ir al detalle
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("title", movie.title)
            intent.putExtra("genre", movie.genre)
            intent.putExtra("image", movie.imageUrl)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }
}