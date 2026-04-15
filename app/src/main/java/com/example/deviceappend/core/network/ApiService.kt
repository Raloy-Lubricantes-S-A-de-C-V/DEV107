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
    @SerializedName("normal") val normal: Int?,
    @SerializedName("require_password_change") val requirePasswordChange: Boolean? = false
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

data class UserListResponse(val error: Boolean, val data: List<UserListItem>)

data class RegisterRequest(val name: String, val mail: String, val parent_id: Int, val code: Int)
data class RegisterResponse(val error: Boolean, val id_request: Int? = null, val msj: String)

data class UpdatePasswordRequest(val username: String, val hash: String)
data class RecoveryEmailRequest(val email: String, val salt: String)

data class NewTechnicianWebhookRequest(
    @SerializedName("email") val email: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("asunto") val asunto: String? = null
)

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

data class EmpresaListResponse(val error: Boolean, val data: List<Empresa>)
data class EmpresaRequest(
    val cveempresa: String, val descripcio: String, val calle: String,
    val noextint: String, val colonia: String, val codpostal: Int?,
    val poblacion: String, val cveentfed: String, val rfc: String
)
data class EmpresaResponse(val error: Boolean, val id: Any? = null, val msj: String? = null)

data class TecnicoConEmpresas(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("empresas") val empresas: List<Int>?
)

data class TecnicosListResponse(val error: Boolean, val data: List<TecnicoConEmpresas>)
data class AsignarEmpresasRequest(val empresas: List<Int>)

data class Prospecto(
    val id: Int, val name: String?, val mail: String?,
    val create_day: String?, val view: Int, val acepted: Int,
    val declined: Int, val open: Int, val code: Int?, val parent_id: Int?
)

data class ProspectoListResponse(val error: Boolean, val data: List<Prospecto>)

data class AprobarProspectoRequest(
    val name: String, val mail: String, val parent_id: Int,
    val sys: Int, val admin: Int, val normal: Int, val lider: Int,
    val empresas: List<Int>
)

data class Notificacion(
    val id: Int, val modulo: String, val descripcion: String,
    val isRead: Boolean, val prospectoId: Int
)

data class DelsipEmpresa(
    @SerializedName("cveempresa") val cveempresa: String
)

data class DelsipTestResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: List<DelsipEmpresa>
)

data class DelsipImage(
    @SerializedName("image_base64") val imageBase64: String,
    @SerializedName("filename") val filename: String
)
data class DelsipImageResponse(
    val error: Boolean,
    val data: DelsipImage?,
    val msj: String?
)

interface ApiService {
    @GET("check-connectivity")
    suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>

    @GET("users/list")
    suspend fun listUsers(): Response<UserListResponse>

    @POST("register-request")
    suspend fun registerNewUser(@Body request: RegisterRequest): Response<RegisterResponse>

    // ==========================================
    // MÉTODO CORRECTO Y ÚNICO DE AUTENTICACIÓN
    // ==========================================
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

    @POST("https://n8n.raloy.com.mx/webhook/kioskoti-recuperacion-contrase%C3%B1a")
    suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>

    @POST("https://n8n.raloy.com.mx/webhook/nuevo-tecnico")
    suspend fun sendNewTechnicianWebhook(@Body request: NewTechnicianWebhookRequest): Response<Unit>

    @GET("empresas")
    suspend fun getEmpresas(): Response<EmpresaListResponse>

    @POST("empresas")
    suspend fun createEmpresa(@Body request: EmpresaRequest): Response<EmpresaResponse>

    @PUT("empresas/{id}")
    suspend fun updateEmpresa(@Path("id") id: Int, @Body request: EmpresaRequest): Response<EmpresaResponse>

    @GET("tecnicos/empresas")
    suspend fun getTecnicosConEmpresas(): Response<TecnicosListResponse>

    @PUT("tecnicos/{id_usuario}/empresas")
    suspend fun asignarEmpresasATecnico(@Path("id_usuario") idUsuario: Int, @Body request: AsignarEmpresasRequest): Response<EmpresaResponse>

    @GET("prospectos")
    suspend fun getProspectos(): Response<ProspectoListResponse>

    @PUT("prospectos/{id}/visto")
    suspend fun marcarProspectoVisto(@Path("id") id: Int): Response<Map<String, Any>>

    @PUT("prospectos/{id}/declinar")
    suspend fun declinarProspecto(@Path("id") id: Int): Response<Map<String, Any>>

    @POST("prospectos/{id}/aprobar")
    suspend fun aprobarProspecto(@Path("id") id: Int, @Body req: AprobarProspectoRequest): Response<Map<String, Any>>

    // ==========================================
    // ENDPOINTS DE PRUEBA DELSIP
    // ==========================================
    @GET("delsip/test")
    suspend fun testDelsipConnection(): Response<DelsipTestResponse>

    @GET("delsip/testimage")
    suspend fun getDelsipImage(@Query("nomina") nomina: String): Response<DelsipImageResponse>
}