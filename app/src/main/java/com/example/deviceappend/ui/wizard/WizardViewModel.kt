package com.example.deviceappend.ui.wizard

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Se apunta al core para obtener la instancia de red
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.utils.BitAlgorithm
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

    // Métodos de lógica (validarIdentidad, validarEtiquetaFinal, etc.) se mantienen igual
}

sealed class WizardState {
    object Loading : WizardState()
    object Success : WizardState()
    object RequireJefeAuth : WizardState()
    data class Error(val message: String) : WizardState()
    data class ShowLegalTerms(val id: String) : WizardState()
}