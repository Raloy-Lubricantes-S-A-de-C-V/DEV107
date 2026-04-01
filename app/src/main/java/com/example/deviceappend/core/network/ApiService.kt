package com.example.myapplication.data.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Modelos de Datos para Intercambio (DTOs)
 */

// 1. Modelo para Autenticación de la App contra el API de Raloy
data class AuthAppRequest(val username: String, val password: String)
data class AuthResponse(val status: String, val data: AuthData?)
data class AuthData(val key: String)

// 2. Modelo de Colaborador (Lectura de DelSIP / SQL Server)
data class DelSipEmployee(
    val empleado_id: String,
    val nombre_completo: String,
    val foto_b64: String?,
    val puesto: String?,
    val empresa_departamento: String?
)

// 3. Modelo de Validación Externa (Jira, PRTG, SOC)
data class ExternalValidationResponse(
    val isValid: Boolean,
    val message: String,
    val details: String?
)

// 4. Modelo para n8n (Autorizaciones de Jefes y Alertas)
data class N8nAlertRequest(
    val id_reg: String,
    val tecnico_correo: String,
    val tipo_alerta: String, // "BIT_12_ACTIVO" o "COLABORADOR_EXTERNO"
    val evidencia_url: String,
    val directivos: List<String> = listOf(
        "alopez@corporativonova.com",
        "aloya@consorcionova.com",
        "pjimenezb@raloy.com.mx",
        "llopezf@2-protection.com"
    )
)

/**
 * Definición de Endpoints
 */
interface ApiService {

    // --- SEGURIDAD ---
    @POST("autenticate")
    suspend fun autenticateApp(@Body request: AuthAppRequest): Response<AuthResponse>

    // --- DELSIP (SQL SERVER READ-ONLY) ---
    @GET("delsip/empleado/{pin}")
    suspend fun getEmpleadoDelSip(@Path("pin") pin: String): Response<DelSipEmployee>

    // --- MONGODB (GESTIÓN DE ACTIVOS) ---

    // Obtener equipo por ID de 10 dígitos y Cuadrante
    @GET("assets/{coleccion}/{id_reg}")
    suspend fun getAsset(
        @Path("coleccion") coleccion: String,
        @Path("id_reg") idReg: String
    ): Response<Map<String, Any>>

    // Registro o Actualización del JSON de 100 campos
    @POST("assets/upsert")
    suspend fun upsertAsset(@Body assetData: Map<String, Any>): Response<Any>

    // --- VALIDACIONES EXTERNAS ---
    @GET("validate/external/{sistema}/{serie}")
    suspend fun validateExternalSystem(
        @Path("sistema") sistema: String, // "jira", "prtg", "soc"
        @Path("serie") serie: String
    ): Response<ExternalValidationResponse>

    // --- AUTOMATIZACIÓN (n8n WEBHOOKS) ---
    @POST("https://n8n.raloy.com.mx/webhook/evidencia-auditoria")
    suspend fun sendN8nAlert(@Body request: N8nAlertRequest): Response<Any>

    // Verificar si el Jefe ya dio el "Procede" para un externo
    @GET("audit/check-permission/{temp_id}")
    suspend fun checkExternalPermission(@Path("temp_id") tempId: String): Response<Map<String, Boolean>>
}