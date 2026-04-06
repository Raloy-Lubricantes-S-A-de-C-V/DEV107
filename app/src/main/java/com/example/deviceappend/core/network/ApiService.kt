package com.example.deviceappend.core.network

import retrofit2.Response
import retrofit2.http.*

// Modelos existentes...
data class AuthAppRequest(val username: String, val password: String)
data class UserLoginRequest(val username: String, val password: String)
data class AuthResponse(val status: String, val data: AuthData?)
data class AuthData(val key: String)

// Nuevo modelo para actualización de password
data class UpdatePasswordRequest(val username: String, val hash: String)

interface ApiService {

    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

    // 1. Obtener Hash (Paso 1 del cambio)
    @GET("AYd34kWfLfPRY05vO")
    suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>

    // 2. Guardar Hash en DB (Paso 2 del cambio)
    @POST("update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>

    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("user-login")
    suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>
    abstract fun sendRecoveryEmail(recoveryEmailRequest: Any): Any
}