package com.example.deviceappend.core.network

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit // IMPORTANTE: Importamos esto para medir el tiempo

object RetrofitClient {
    private const val BASE_URL = "https://apir.raloy.com.mx/kioskoit/api/v1/"
    private var retrofit: Retrofit? = null

    fun init(context: Context) {
        if (retrofit == null) {

            // ==========================================
            // LOGGING INTERCEPTOR
            // ==========================================
            val logging = HttpLoggingInterceptor { message ->
                Log.d("RetrofitLog", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            // ==========================================
            // EXTENSIÓN DE TIEMPO PARA LA INTELIGENCIA ARTIFICIAL
            // (60 segundos de paciencia para evitar SocketTimeoutException)
            // ==========================================
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // Tiempo máximo para conectar
                .readTimeout(60, TimeUnit.SECONDS)    // Tiempo máximo esperando respuesta (Gemini)
                .writeTimeout(60, TimeUnit.SECONDS)   // Tiempo máximo para subir los datos
                .addInterceptor(logging)
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