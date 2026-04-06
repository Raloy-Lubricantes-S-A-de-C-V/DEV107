package com.example.deviceappend.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deviceappend.core.LoginRepository
import com.example.deviceappend.data.model.LoggedInUser
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(user: String, pass: String) {
        _loginState.value = LoginState.Loading

        // Se eliminó la validación local de "123" que activaba RequirePasswordChange.
        // Ahora el flujo siempre consulta directamente a la API de Raloy.
        viewModelScope.launch {
            val result = repository.login(user, pass)
            result.onSuccess { userModel ->
                _loginState.value = LoginState.Success(userModel)
            }.onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "Error de conexión")
            }
        }
    }

    class Factory(private val repository: LoginRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(repository) as T
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    object RequirePasswordChange : LoginState()
    data class Success(val user: LoggedInUser) : LoginState()
    data class Error(val message: String) : LoginState()
}