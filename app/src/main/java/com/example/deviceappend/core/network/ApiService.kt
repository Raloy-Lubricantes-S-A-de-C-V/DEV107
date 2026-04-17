package com.example.deviceappend.core.network

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class AuthAppRequest(@SerializedName("username") val username: String, @SerializedName("password") val password: String)
data class UserLoginRequest(@SerializedName("username") val username: String, @SerializedName("password") val password: String)
data class UserProfile(
    @SerializedName("id") val id: Int?, @SerializedName("user") val user: String?, @SerializedName("name") val name: String?,
    @SerializedName("lider") val lider: Int?, @SerializedName("sys") val sys: Int?, @SerializedName("admin") val admin: Int?,
    @SerializedName("normal") val normal: Int?, @SerializedName("require_password_change") val requirePasswordChange: Boolean? = false
)
data class AuthData(@SerializedName("error") val error: Boolean?, @SerializedName("msj") val msj: String?, @SerializedName("key") val key: String?, @SerializedName("profile") val profile: UserProfile?)
data class AuthResponse(@SerializedName("status") val status: Int, @SerializedName("data") val data: AuthData?)
data class CheckSysAdminRequest(@SerializedName("user") val user: String)
data class CheckSysAdminResponse(@SerializedName("is_sys") val is_sys: Boolean)
data class UserListItem(@SerializedName("id") val id: Int, @SerializedName("name") val name: String, @SerializedName("user") val user: String?, @SerializedName("lider") val lider: Int)
data class UserListResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: List<UserListItem>)
data class RegisterRequest(@SerializedName("name") val name: String, @SerializedName("mail") val mail: String, @SerializedName("parent_id") val parent_id: Int, @SerializedName("code") val code: Int)
data class RegisterResponse(@SerializedName("error") val error: Boolean, @SerializedName("id_request") val id_request: Int? = null, @SerializedName("msj") val msj: String)
data class UpdatePasswordRequest(@SerializedName("username") val username: String, @SerializedName("hash") val hash: String)
data class RecoveryEmailRequest(@SerializedName("email") val email: String, @SerializedName("salt") val salt: String)
data class NewTechnicianWebhookRequest(@SerializedName("email") val email: String, @SerializedName("mensaje") val mensaje: String, @SerializedName("asunto") val asunto: String? = null)

data class Empresa(
    @SerializedName("id") val id: Int, @SerializedName("cveempresa") val cveempresa: String?, @SerializedName("descripcio") val descripcio: String?,
    @SerializedName("calle") val calle: String?, @SerializedName("noextint") val noextint: String?, @SerializedName("colonia") val colonia: String?,
    @SerializedName("codpostal") val codpostal: Double?, @SerializedName("poblacion") val poblacion: String?, @SerializedName("cveentfed") val cveentfed: String?, @SerializedName("rfc") val rfc: String?
)
data class EmpresaListResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: List<Empresa>)
data class EmpresaRequest(
    @SerializedName("cveempresa") val cveempresa: String, @SerializedName("descripcio") val descripcio: String, @SerializedName("calle") val calle: String,
    @SerializedName("noextint") val noextint: String, @SerializedName("colonia") val colonia: String, @SerializedName("codpostal") val codpostal: Int?,
    @SerializedName("poblacion") val poblacion: String, @SerializedName("cveentfed") val cveentfed: String, @SerializedName("rfc") val rfc: String
)
data class EmpresaResponse(@SerializedName("error") val error: Boolean, @SerializedName("id") val id: Any? = null, @SerializedName("msj") val msj: String? = null)
data class TecnicoConEmpresas(@SerializedName("id_usuario") val idUsuario: Int, @SerializedName("nombre") val nombre: String?, @SerializedName("empresas") val empresas: List<Int>?)
data class TecnicosListResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: List<TecnicoConEmpresas>)
data class AsignarEmpresasRequest(@SerializedName("empresas") val empresas: List<Int>)

data class Prospecto(
    @SerializedName("id") val id: Int, @SerializedName("name") val name: String?, @SerializedName("mail") val mail: String?,
    @SerializedName("create_day") val create_day: String?, @SerializedName("view") val view: Int, @SerializedName("acepted") val acepted: Int,
    @SerializedName("declined") val declined: Int, @SerializedName("open") val open: Int, @SerializedName("code") val code: Int?, @SerializedName("parent_id") val parent_id: Int?
)
data class ProspectoListResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: List<Prospecto>)
data class AprobarProspectoRequest(
    @SerializedName("name") val name: String, @SerializedName("mail") val mail: String, @SerializedName("parent_id") val parent_id: Int,
    @SerializedName("sys") val sys: Int, @SerializedName("admin") val admin: Int, @SerializedName("normal") val normal: Int,
    @SerializedName("lider") val lider: Int, @SerializedName("empresas") val empresas: List<Int>
)
data class Notificacion(val id: Int, val modulo: String, val descripcion: String, val isRead: Boolean, val prospectoId: Int)

data class DelsipEmpresa(@SerializedName("cveempresa") val cveempresa: String)
data class DelsipTestResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: List<DelsipEmpresa>)
data class DelsipImageData(@SerializedName("filename") val filename: String?, @SerializedName("image_base64") val imageBase64: String?)
data class DelsipImageResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: DelsipImageData?, @SerializedName("msj") val msj: String?)

data class EmployeeData(
    @SerializedName("nomina") val nomina: String?, @SerializedName("nombre") val nombre: String?, @SerializedName("paterno") val paterno: String?,
    @SerializedName("materno") val materno: String?, @SerializedName("curp") val curp: String?, @SerializedName("sexo") val sexo: String?,
    @SerializedName("fecha_nacimiento") val fecha_nacimiento: String?, @SerializedName("edad") val edad: Int?, @SerializedName("foto_base64") val foto_base64: String?
)
data class EmployeeResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: EmployeeData?)

data class AiPhotoRequest(@SerializedName("image_base64") val image_base64: String)
data class CandidateSearchRequest(@SerializedName("sexo") val sexo: String, @SerializedName("edad_min") val edad_min: Int, @SerializedName("edad_max") val edad_max: Int)
data class CandidateSearchResponse(@SerializedName("error") val error: Boolean, @SerializedName("data") val data: List<Map<String, String>>)

// ==========================================
// NUEVOS MODELOS PARA EVALUACIÓN 1 vs 1 (N8N)
// ==========================================
data class CompareFacesRequest(
    @SerializedName("foto_capturada") val fotoCapturada: String,
    @SerializedName("foto_candidato") val fotoCandidato: String,
    @SerializedName("nomina_candidato") val nominaCandidato: String
)

data class CompareFacesResponse(
    @SerializedName("match") val match: Boolean,
    @SerializedName("nomina") val nomina: String?
)

data class SaveEnrollmentRequest(
    @SerializedName("modulo") val modulo: String, @SerializedName("correo") val correo: String, @SerializedName("tipo_registro") val tipo_registro: String,
    @SerializedName("nomina") val nomina: String?, @SerializedName("datos_extra") val datos_extra: Map<String, String>,
    @SerializedName("fotografia_base64") val fotografia_base64: String?, @SerializedName("huella_digital") val huella_digital: String?
)

interface ApiService {
    @GET("check-connectivity") suspend fun checkDatabaseConnectivity(): Response<Map<String, Any>>
    @GET("users/list") suspend fun listUsers(): Response<UserListResponse>
    @POST("register-request") suspend fun registerNewUser(@Body request: RegisterRequest): Response<RegisterResponse>
    @POST("authenticate") suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>
    @POST("user-login") suspend fun loginUser(@Body request: UserLoginRequest): Response<AuthResponse>
    @POST("rol/source/is_sys") suspend fun checkIsSysAdmin(@Body request: CheckSysAdminRequest): Response<CheckSysAdminResponse>
    @GET("AYd34kWfLfPRY05vO") suspend fun getPasswordHash(@Query("password") plainPassword: String): Response<Map<String, String>>
    @POST("update-password") suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<Map<String, Any>>
    @POST("https://n8n.raloy.com.mx/webhook/kioskoti-recuperacion-contrase%C3%B1a") suspend fun sendRecoveryEmail(@Body request: RecoveryEmailRequest): Response<Unit>
    @POST("https://n8n.raloy.com.mx/webhook/nuevo-tecnico") suspend fun sendNewTechnicianWebhook(@Body request: NewTechnicianWebhookRequest): Response<Unit>

    @GET("empresas") suspend fun getEmpresas(): Response<EmpresaListResponse>
    @POST("empresas") suspend fun createEmpresa(@Body request: EmpresaRequest): Response<EmpresaResponse>
    @PUT("empresas/{id}") suspend fun updateEmpresa(@Path("id") id: Int, @Body request: EmpresaRequest): Response<EmpresaResponse>

    @GET("tecnicos/empresas") suspend fun getTecnicosConEmpresas(): Response<TecnicosListResponse>
    @PUT("tecnicos/{id_usuario}/empresas") suspend fun asignarEmpresasATecnico(@Path("id_usuario") idUsuario: Int, @Body request: AsignarEmpresasRequest): Response<EmpresaResponse>

    @GET("prospectos") suspend fun getProspectos(): Response<ProspectoListResponse>
    @PUT("prospectos/{id}/visto") suspend fun marcarProspectoVisto(@Path("id") id: Int): Response<Map<String, Any>>
    @PUT("prospectos/{id}/declinar") suspend fun declinarProspecto(@Path("id") id: Int): Response<Map<String, Any>>
    @POST("prospectos/{id}/aprobar") suspend fun aprobarProspecto(@Path("id") id: Int, @Body req: AprobarProspectoRequest): Response<Map<String, Any>>

    @GET("delsip/test") suspend fun testDelsipConnection(): Response<DelsipTestResponse>
    @GET("delsip/testimage") suspend fun testDelsipImage(@Query("nomina") nomina: String): Response<DelsipImageResponse>

    @GET("delsip/empleado/{nomina}") suspend fun getEmployeeByNomina(@Path("nomina") nomina: String): Response<EmployeeResponse>
    @POST("https://n8n.raloy.com.mx/webhook/ai-analizar-foto") suspend fun analyzePhotoAi(@Body req: AiPhotoRequest): Response<JsonObject>
    @POST("delsip/candidatos") suspend fun searchCandidates(@Body req: CandidateSearchRequest): Response<CandidateSearchResponse>

    // NUEVO WEBHOOK DE COMPARACIÓN 1 a 1 (El que crearás en N8N)
    @POST("https://n8n.raloy.com.mx/webhook/ai-comparar-rostros")
    suspend fun compareFacesAi(@Body req: CompareFacesRequest): Response<CompareFacesResponse>

    @POST("enrolamiento/guardar") suspend fun saveEnrollment(@Body req: SaveEnrollmentRequest): Response<Map<String, Any>>
}