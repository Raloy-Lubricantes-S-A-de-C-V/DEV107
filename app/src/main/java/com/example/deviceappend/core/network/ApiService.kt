package com.example.deviceappend.core.network

import retrofit2.Response
import retrofit2.http.*

data class AuthAppRequest(val username: String, val password: String)
data class UserLoginRequest(val username: String, val password: String)
data class AuthResponse(val status: String, val data: AuthData?)
data class AuthData(val key: String)
data class UpdatePasswordRequest(val username: String, val hash: String)
data class RecoveryEmailRequest(val email: String, val salt: String)

interface ApiService {
    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

    @GET("AYd34kWfLfPRY05vO")
    suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>

    @POST("update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>

    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("user-login")
    suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>

    // URL codificada para evitar errores con la "ñ" y acentos
    @POST("password-recovery")
    suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>
}