package com.example.deviceappend.core.network

import retrofit2.Response
import retrofit2.http.*

// Modelos para Autenticación
data class AuthAppRequest(val username: String, val password: String)
data class UserLoginRequest(val username: String, val password: String)
data class AuthResponse(val status: String, val data: AuthData?)
data class AuthData(val key: String)

// Modelos para Roles y Permisos
data class CheckSysAdminRequest(val user: String)
data class CheckSysAdminResponse(val is_sys: Boolean)

// Modelos para Registro de Nuevo Técnico
data class UserListItem(val id: Int, val name: String, val lider: Int)
data class UserListResponse(val error: Boolean, val data: List<UserListItem>)
data class RegisterRequest(val name: String, val mail: String, val parent_id: Int, val code: Int)
data class RegisterResponse(val error: Boolean, val id_request: Int? = null, val msj: String)

// Modelos para Actualización y Recuperación
data class UpdatePasswordRequest(val username: String, val hash: String)
data class RecoveryEmailRequest(val email: String, val salt: String)

interface ApiService {
    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

    // Listado de usuarios líderes para el alta
    @GET("users/list")
    suspend fun listUsers(): Response<UserListResponse>

    // Registro de solicitud de nuevo técnico
    @POST("register-request")
    suspend fun registerNewUser(@Body request: RegisterRequest): Response<RegisterResponse>

    // Consulta de permisos de administrador
    @POST("rol/source/is_sys")
    suspend fun checkIsSysAdmin(@Body request: CheckSysAdminRequest): Response<CheckSysAdminResponse>

    // Obtención de Hash seguro
    @GET("AYd34kWfLfPRY05vO")
    suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>

    // Persistencia de nueva contraseña
    @POST("update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>

    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("user-login")
    suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>

    // Webhook codificado para n8n
    @POST("https://n8n.raloy.com.mx/webhook/kioskoti-recuperacion-contrase%C3%B1a")
    suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>
}