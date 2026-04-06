package com.example.deviceappend.core.network

import com.google.gson.annotations.SerializedName
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
data class UserListItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("lider") val lider: Int
)
data class UserListResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: List<UserListItem>
)
data class RegisterRequest(val name: String, val mail: String, val parent_id: Int, val code: Int)
data class RegisterResponse(val error: Boolean, val id_request: Int? = null, val msj: String)

interface ApiService {
    // CORRECCIÓN: Se eliminó "api/v1/" de las rutas porque ya está en la BASE_URL
    @GET("users/list")
    suspend fun listUsers(): Response<UserListResponse>

    @POST("register-request")
    suspend fun registerNewUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("user-login")
    suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>

    @POST("rol/source/is_sys")
    suspend fun checkIsSysAdmin(@Body request: CheckSysAdminRequest): Response<CheckSysAdminResponse>

    @GET("AYd34kWfLfPRY05vO")
    suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>

    @POST("update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>

    @POST("https://n8n.raloy.com.mx/webhook/kioskoti-recuperacion-contrase%C3%B1a")
    suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>
}