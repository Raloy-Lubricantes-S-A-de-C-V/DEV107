package com.example.deviceappend.core.network

import retrofit2.Response
import retrofit2.http.*

// Modelos para Autenticación
data class AuthAppRequest(val username: String, val password: String)
data class UserLoginRequest(val username: String, val password: String)
data class AuthResponse(val status: String, val data: AuthData?)
data class AuthData(val key: String)

// NUEVO: Modelos para consulta de permisos (Indispensables para el repositorio)
data class CheckSysAdminRequest(val user: String)
data class CheckSysAdminResponse(val is_sys: Boolean)

// Modelos para actualización y recuperación
data class UpdatePasswordRequest(val username: String, val hash: String)
data class RecoveryEmailRequest(val email: String, val salt: String)

interface ApiService {
    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

    // GESTIÓN DE ROLES: Método que faltaba y causaba el error de referencia
    @POST("rol/source/is_sys")
    suspend fun checkIsSysAdmin(@Body request: CheckSysAdminRequest): Response<CheckSysAdminResponse>

    @GET("AYd34kWfLfPRY05vO")
    suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>

    @POST("update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>

    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("user-login")
    suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>

    @POST("password-recovery")
    suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>
}