package com.example.myapplication.ui.wizard

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.utils.BitAlgorithm
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

class WizardViewModel : ViewModel() {

    private val api = RetrofitClient.instance
    private val gemini = GenerativeModel("gemini-1.5-flash", "TU_API_KEY_AQUI")

    private val _step = MutableLiveData<Int>(0)
    val step: LiveData<Int> = _step

    private val _uiState = MutableLiveData<WizardState>()
    val uiState: LiveData<WizardState> = _uiState

    // Datos temporales del proceso
    private var fotoEmpleadoSip: String? = null
    var idRegApartado: String = ""

    // PASO 0: Consulta DelSIP + Match Biométrico
    fun validarIdentidad(nomina: String, fotoCapturada: Bitmap) {
        _uiState.value = WizardState.Loading
        viewModelScope.launch {
            try {
                val response = api.getEmpleadoDelSip(nomina)
                if (response.isSuccessful && response.body() != null) {
                    val empleado = response.body()!!
                    fotoEmpleadoSip = empleado.foto_b64

                    // Ejecutar comparación con Gemini
                    val prompt = "Compara estas dos fotos de rostro. ¿Es la misma persona? Responde solo con un porcentaje de 0 a 100."
                    val result = gemini.generateContent(content {
                        // Enviar foto de base de datos y foto de cámara
                        // (Nota: requiere conversión de B64 a Bitmap omitida por brevedad)
                        image(fotoCapturada)
                        text(prompt)
                    })

                    val score = result.text?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                    if (score >= 80) {
                        _step.value = 1 // Ir a Tipo de Asignación
                        _uiState.value = WizardState.Success
                    } else {
                        _uiState.value = WizardState.Error("Match de identidad fallido: $score%")
                    }
                } else {
                    _uiState.value = WizardState.RequireJefeAuth // Salto a Paso 0.1
                }
            } catch (e: Exception) {
                _uiState.value = WizardState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    // PASO 7: Validación de Bits y Bit 12 Legal
    fun validarEtiquetaFinal(fotoMacro: Bitmap) {
        _uiState.value = WizardState.Loading
        viewModelScope.launch {
            val prompt = "Analiza los 12 cuadros de la etiqueta. Indica cuáles están pintados como una cadena de 1 y 0."
            val response = gemini.generateContent(content {
                image(fotoMacro)
                text(prompt)
            })

            val bitsLeidos = response.text?.filter { it == '1' || it == '0' } ?: ""
            if (bitsLeidos.length == 12) {
                val idDecodificado = BitAlgorithm.decode(bitsLeidos)

                if (bitsLeidos.endsWith("1")) {
                    _uiState.value = WizardState.ShowLegalTerms(idDecodificado)
                } else {
                    finalizarRegistro(idDecodificado)
                }
            }
        }
    }

    private fun finalizarRegistro(id: String) {
        // Lógica para enviar el JSON de 100 campos a MongoDB vía ApiService
    }
}

sealed class WizardState {
    object Loading : WizardState()
    object Success : WizardState()
    object RequireJefeAuth : WizardState()
    data class Error(val message: String) : WizardState()
    data class ShowLegalTerms(val id: String) : WizardState()
}