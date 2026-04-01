package com.example.deviceappend.core.network

import android.content.Context
import com.example.deviceappend.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp que garantiza la seguridad en cada petición.
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

        // Headers Técnicos de Compatibilidad
        requestBuilder.addHeader("Accept-Encoding", "identity")
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")
        requestBuilder.addHeader("X-App-Source", "Android-AssetManager-V1")

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}