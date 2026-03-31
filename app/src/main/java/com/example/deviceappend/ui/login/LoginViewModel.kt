package com.example.myapplication.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LoginRepository
import com.example.myapplication.data.model.LoggedInUser
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(user: String, pass: String) {
        _loginState.value = LoginState.Loading

        // Regla Super Admin: Cambio de clave obligatorio si es la clave por defecto
        if (user == "pjimenezb@raloy.com.mx" && pass == "123") {
            _loginState.value = LoginState.RequirePasswordChange
            return
        }

        viewModelScope.launch {
            val result = repository.login(user, pass)
            result.onSuccess { userModel ->
                _loginState.value = LoginState.Success(userModel)
            }.onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "Error de conexión")
            }
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    object RequirePasswordChange : LoginState()
    data class Success(val user: LoggedInUser) : LoginState()
    data class Error(val message: String) : LoginState()
}