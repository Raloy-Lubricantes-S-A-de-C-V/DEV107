package com.example.myapplication.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Motor de Red Retrofit: Configuración blindada para Raloy.
 * Centraliza la comunicación con DelSIP (SQL), MongoDB y n8n.
 */
object RetrofitClient {

    private const val BASE_URL = "https://apir.raloy.com.mx/kioskorem/api/v1/"
    private var retrofit: Retrofit? = null

    /**
     * Inicialización única desde MainActivity.
     * Requiere el contexto para que el AuthInterceptor acceda a las SharedPreferences Cifradas.
     */
    fun init(context: Context) {
        if (retrofit == null) {

            // 1. Configuración de Logs para depuración en desarrollo
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // 2. Cliente OkHttp Blindado
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(AuthInterceptor(context)) // Inyección de Token AES256
                .connectTimeout(60, TimeUnit.SECONDS)     // Timeout extendido para IA
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            // 3. Convertidor GSON con soporte para campos nulos
            // Evita que la App truene si un campo de la plantilla de 100 campos viene vacío
            val gson: Gson = GsonBuilder()
                .setLenient()
                .serializeNulls()
                .create()

            // 4. Construcción de la instancia Retrofit
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
    }

    /**
     * Instancia singleton del ApiService.
     * Lanza una excepción clara si se intenta usar antes de inicializar.
     */
    val instance: ApiService
        get() {
            return retrofit?.create(ApiService::class.java)
                ?: throw IllegalStateException("RetrofitClient debe inicializarse en MainActivity antes de su uso.")
        }
}