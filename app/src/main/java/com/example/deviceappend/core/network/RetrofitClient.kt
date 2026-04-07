package com.example.deviceappend.core.network

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://apir.raloy.com.mx/kioskoit/api/v1/"
    private var retrofit: Retrofit? = null

    fun init(context: Context) {
        if (retrofit == null) {

            // ==========================================
            // LOGGING INTERCEPTOR (MODO DIOS)
            // ==========================================
            // Esto imprimirá en el Logcat de Android Studio toda la petición HTTP
            val logging = HttpLoggingInterceptor { message ->
                Log.d("RetrofitLog", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY // Muestra URL, Headers y Body
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging) // Lo agregamos aquí
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
        }
    }

    val instance: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient debe inicializarse en MainActivity.")
}