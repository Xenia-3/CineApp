package com.example.cineapp

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ClienteTmdb {
    var genreNames: Map<Int, String> = emptyMap()

    val api: ApiTmdb by lazy {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val url = chain.request().url().newBuilder()
                .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                .addQueryParameter("language", "es-ES")
                .build()
            chain.proceed(chain.request().newBuilder().url(url).build())
        }.build()
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiTmdb::class.java)
    }

    fun <T> request(call: Call<T>, success: (T) -> Unit, error: (String) -> Unit) {
        if (BuildConfig.TMDB_API_KEY.isBlank()) {
            error("No se pudo cargar la cartelera. Revisa la configuración")
            return
        }
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    success(body)
                } else {
                    error(when (response.code()) {
                        401, 403 -> "No se pudo acceder a la cartelera. Revisa la configuración"
                        404 -> "No se encontró la película"
                        else -> "No se pudo cargar la información de las películas"
                    })
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (!call.isCanceled) error("No se pudieron cargar las películas. Revisa tu conexión")
            }
        })
    }
}
