package com.example.deviceappend.core

import com.example.deviceappend.data.model.LoggedInUser
import com.example.deviceappend.core.network.*
import com.example.deviceappend.core.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepository(private val sessionManager: SessionManager) {

    suspend fun login(email: String, pass: String): Result<LoggedInUser> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance

                val appAuth = api.autenticateApp(AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P"))
                if (!appAuth.isSuccessful) return@withContext Result.failure(Exception("Fallo App Auth (HTTP ${appAuth.code()})"))
                sessionManager.saveToken(appAuth.body()?.data?.key ?: "")

                val userAuth = api.loginUser(UserLoginRequest(email, pass))
                val authData = userAuth.body()?.data

                if (userAuth.isSuccessful && authData != null && authData.error == false) {
                    val userToken = authData.key ?: ""
                    val profile = authData.profile
                    val msj = authData.msj ?: "Sesión iniciada correctamente"

                    // VALIDACIÓN DE CONTRASEÑA TEMPORAL
                    if (profile?.requirePasswordChange == true) {
                        sessionManager.saveUsername(email) // Guardamos email para el update
                        return@withContext Result.success(LoggedInUser("0", email, "REQUIRE_CHANGE"))
                    }

                    if (profile != null) {
                        sessionManager.saveUserProfile(profile)
                    }
                    sessionManager.saveToken(userToken)

                    Result.success(LoggedInUser(profile?.id?.toString() ?: "", profile?.name ?: email, msj))
                } else {
                    Result.failure(Exception(authData?.msj ?: "Credenciales inválidas"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}