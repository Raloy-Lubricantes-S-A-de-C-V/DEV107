package com.example.deviceappend.core.network

import retrofit2.Response
import retrofit2.http.*

// DTOs para Autenticación
data class AuthAppRequest(val username: String, val password: String)
data class AuthResponse(val status: String, val data: AuthData?)
data class AuthData(val key: String)

interface ApiService {
    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @GET("delsip/empleado/{pin}")
    suspend fun getEmpleadoDelSip(@Path("pin") pin: String): Response<Map<String, Any>>

    // Agrega aquí el resto de tus métodos siguiendo la estructura corregida
}