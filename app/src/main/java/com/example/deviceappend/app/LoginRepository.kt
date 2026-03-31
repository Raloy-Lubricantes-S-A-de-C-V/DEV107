package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.data.model.LoggedInUser
import com.example.myapplication.data.network.AuthAppRequest
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LoginRepository: Gestiona la autenticación interna y roles.
 * Se eliminó la dependencia de Odoo conforme al nuevo diseño.
 */
class LoginRepository(private val sessionManager: SessionManager) {

    private val TAG = "LoginRepository"

    suspend fun login(username: String, pass: String): Result<LoggedInUser> {
        return withContext(Dispatchers.IO) {
            try {
                // --- FASE 1: AUTENTICACIÓN DIRECTA API RALOY ---
                // Se utilizan las credenciales internas del aplicativo
                val authRequest = AuthAppRequest(
                    username = "app-movile-001",
                    password = "Zsh4cvz4tvGyQa56P"
                )

                val response = RetrofitClient.instance.autenticateApp(authRequest)

                if (response.isSuccessful && response.body()?.data != null) {
                    val token = response.body()!!.data!!.key

                    // Simulamos la obtención del UID del sistema interno basado en el login
                    // En un escenario real, este vendría en el body de la respuesta
                    val internalUid = username.hashCode()

                    // --- FASE 2: GESTIÓN DE ROLES ---
                    val isSuperAdmin = username == "pjimenezb@raloy.com.mx"

                    // --- FASE 3: PERSISTENCIA SEGURA ---
                    sessionManager.saveSession(internalUid, username, isSuperAdmin)
                    sessionManager.saveToken(token)

                    Log.d(TAG, "Login interno exitoso: $username (Admin: $isSuperAdmin)")
                    Result.success(LoggedInUser(internalUid.toString(), username))
                } else {
                    Result.failure(Exception("Error de autenticación: Credenciales inválidas"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en el flujo de sesión: ${e.message}")
                Result.failure(e)
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}