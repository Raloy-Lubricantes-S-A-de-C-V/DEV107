package com.example.deviceappend.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class AuthAppRequest(val username: String, val password: String)
data class UserLoginRequest(val username: String, val password: String)

// 1. NUEVA ESTRUCTURA DEL PERFIL DE USUARIO
data class UserProfile(
    @SerializedName("id") val id: Int,
    @SerializedName("user") val user: String,
    @SerializedName("name") val name: String,
    @SerializedName("lider") val lider: Int,
    @SerializedName("sys") val sys: Int,
    @SerializedName("admin") val admin: Int,
    @SerializedName("normal") val normal: Int
)

// 2. NUEVA ESTRUCTURA DE LA RESPUESTA DE LOGIN
data class AuthData(
    @SerializedName("error") val error: Boolean?,
    @SerializedName("msj") val msj: String?,
    @SerializedName("key") val key: String?,
    @SerializedName("profile") val profile: UserProfile?
)

data class AuthResponse(val status: Int, val data: AuthData?)

data class CheckSysAdminRequest(val user: String)
data class CheckSysAdminResponse(val is_sys: Boolean)

data class UserListItem(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("user") val user: String?,
    @SerializedName("lider") val lider: Any?
)

data class UserListResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: List<UserListItem>
)

data class RegisterRequest(val name: String, val mail: String, val parent_id: Int, val code: Int)
data class RegisterResponse(val error: Boolean, val id_request: Int? = null, val msj: String)

data class UpdatePasswordRequest(val username: String, val hash: String)
data class RecoveryEmailRequest(val email: String, val salt: String)

data class NewTechnicianWebhookRequest(
    @SerializedName("email") val email: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("asunto") val asunto: String? = null
)

interface ApiService {
    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

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

    @POST("https://n8n.raloy.com.mx/webhook/nuevo-tecnico")
    suspend fun sendNewTechnicianWebhook(@Body request: NewTechnicianWebhookRequest): Response<Unit>
}