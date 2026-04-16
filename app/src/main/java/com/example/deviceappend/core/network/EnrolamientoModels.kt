package com.example.deviceappend.core.network

import com.google.gson.annotations.SerializedName

// ==========================================
// MODELOS EXCLUSIVOS PARA ENROLAMIENTO Y IA
// ==========================================
data class EmployeeData(
    @SerializedName("nomina") val nomina: String?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("paterno") val paterno: String?,
    @SerializedName("materno") val materno: String?,
    @SerializedName("curp") val curp: String?,
    @SerializedName("sexo") val sexo: String?,
    @SerializedName("fecha_nacimiento") val fecha_nacimiento: String?,
    @SerializedName("edad") val edad: Int?,
    @SerializedName("foto_base64") val foto_base64: String?
)

data class EmployeeResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: EmployeeData?
)

data class AiPhotoRequest(
    @SerializedName("image_base64") val image_base64: String
)

data class AiPhotoResponse(
    @SerializedName("sexo") val sexo: String,
    @SerializedName("edad_minima") val edad_minima: Int,
    @SerializedName("edad_maxima") val edad_maxima: Int
)

data class CandidateSearchRequest(
    @SerializedName("sexo") val sexo: String,
    @SerializedName("edad_min") val edad_min: Int,
    @SerializedName("edad_max") val edad_max: Int
)

data class CandidateSearchResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: List<Map<String, String>>
)

data class FaceMatchRequest(
    @SerializedName("original_base64") val original_base64: String,
    @SerializedName("candidates") val candidates: List<Map<String, String>>
)

data class FaceMatchResponse(
    @SerializedName("match_nomina") val match_nomina: String?,
    @SerializedName("fingerprint") val fingerprint: String?
)

data class SaveEnrollmentRequest(
    @SerializedName("modulo") val modulo: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("tipo_registro") val tipo_registro: String,
    @SerializedName("nomina") val nomina: String?,
    @SerializedName("datos_extra") val datos_extra: Map<String, String>,
    @SerializedName("fotografia_base64") val fotografia_base64: String?,
    @SerializedName("huella_digital") val huella_digital: String?
)