package com.example.deviceappend.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class AuthAppRequest(val username: String, val password: String)
data class UserLoginRequest(val username: String, val password: String)

data class UserProfile(
    @SerializedName("id") val id: Int?,
    @SerializedName("user") val user: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("lider") val lider: Int?,
    @SerializedName("sys") val sys: Int?,
    @SerializedName("admin") val admin: Int?,
    @SerializedName("normal") val normal: Int?
)

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
    @SerializedName("lider") val lider: Int
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

// ==========================================
// MODELOS PARA EMPRESAS
// ==========================================
data class Empresa(
    @SerializedName("id") val id: Int,
    @SerializedName("cveempresa") val cveempresa: String?,
    @SerializedName("descripcio") val descripcio: String?,
    @SerializedName("calle") val calle: String?,
    @SerializedName("noextint") val noextint: String?,
    @SerializedName("colonia") val colonia: String?,
    @SerializedName("codpostal") val codpostal: Double?,
    @SerializedName("poblacion") val poblacion: String?,
    @SerializedName("cveentfed") val cveentfed: String?,
    @SerializedName("rfc") val rfc: String?
)

data class EmpresaListResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: List<Empresa>
)

data class EmpresaRequest(
    val cveempresa: String,
    val descripcio: String,
    val calle: String,
    val noextint: String,
    val colonia: String,
    val codpostal: Int?,
    val poblacion: String,
    val cveentfed: String,
    val rfc: String
)

data class EmpresaResponse(
    val error: Boolean,
    val id: Any? = null,
    val msj: String? = null
)

interface ApiService {
    // Se quitó el "api/v1/" de todos los endpoints porque RetrofitClient ya lo tiene en su BASE_URL

    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

    @GET("users/list")
    suspend fun listUsers(): Response<UserListResponse>

    @POST("register-request")
    suspend fun registerNewUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("authenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    @POST("user-login")
    suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>

    @POST("rol/source/is_sys")
    suspend fun checkIsSysAdmin(@Body request: CheckSysAdminRequest): Response<CheckSysAdminResponse>

    @GET("AYd34kWfLfPRY05vO")
    suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>

    @POST("update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>

    // Endpoints Externos de Webhook (Estos conservan la URL completa porque van a n8n)
    @POST("https://n8n.raloy.com.mx/webhook/kioskoti-recuperacion-contrase%C3%B1a")
    suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>

    @POST("https://n8n.raloy.com.mx/webhook/nuevo-tecnico")
    suspend fun sendNewTechnicianWebhook(@Body request: NewTechnicianWebhookRequest): Response<Unit>

    // ==========================================
    // ENDPOINTS EMPRESAS
    // ==========================================
    @GET("empresas")
    suspend fun getEmpresas(): Response<EmpresaListResponse>

    @POST("empresas")
    suspend fun createEmpresa(@Body request: EmpresaRequest): Response<EmpresaResponse>

    @PUT("empresas/{id}")
    suspend fun updateEmpresa(@Path("id") id: Int, @Body request: EmpresaRequest): Response<EmpresaResponse>
}