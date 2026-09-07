package com.example.cineapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private val movieList: List<Movie>,
    private val onRemoveClick: (Movie) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavViewHolder>() {

    class FavViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgVertical: ImageView = itemView.findViewById(R.id.imgVertical)
        val tvTitleVertical: TextView = itemView.findViewById(R.id.tvTitleVertical)
        val tvGenreVertical: TextView = itemView.findViewById(R.id.tvGenreVertical)
        val tvRatingVertical: TextView = itemView.findViewById(R.id.tvRatingVertical)
        val btnHeart: ImageView = itemView.findViewById(R.id.imgHeart) // 👈 Ahora sí existe
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_vertical, parent, false)
        return FavViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
        val movie = movieList[position]
        holder.tvTitleVertical.text = movie.title
        holder.tvGenreVertical.text = movie.genre
        holder.tvRatingVertical.text = movie.rating
        holder.btnHeart.setColorFilter(holder.itemView.context.getColor(R.color.accent_red))

        // Cargar la imagen local
        val imageResId = holder.itemView.resources.getIdentifier(
            movie.imageUrl.replace("R.drawable.", ""), "drawable", holder.itemView.context.packageName
        )
        if (imageResId != 0) {
            holder.imgVertical.setImageResource(imageResId)
        } else {
            holder.imgVertical.setImageResource(R.drawable.ic_launcher_background)
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

        // Clic para quitarla de favoritos
        holder.btnHeart.setOnClickListener {
            onRemoveClick(movie)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }
}