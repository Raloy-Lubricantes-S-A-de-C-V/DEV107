package com.example.myapplication.data.network

import android.content.Context
import com.example.myapplication.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp que garantiza la seguridad en cada petición.
 * - Inyecta el Token de autorización (Bearer).
 * - Configura encodings para evitar errores de serialización.
 * - Asegura que el Content-Type sea siempre JSON.
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken()

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 1. Inyección de Token (Si existe sesión activa)
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // 2. Headers Técnicos de Compatibilidad
        // "Accept-Encoding: identity" evita que Retrofit intente descomprimir
        // respuestas que el servidor de Raloy ya envía en formato plano.
        requestBuilder.addHeader("Accept-Encoding", "identity")

        // Aseguramos que el servidor siempre reciba y devuelva JSON
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")

        // 3. Identificador de Dispositivo (Opcional para logs en n8n/Mongo)
        requestBuilder.addHeader("X-App-Source", "Android-AssetManager-V1")

        val request = requestBuilder.build()

        // Proceder con la petición modificada
        return chain.proceed(request)
    }
}