package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.data.model.LoggedInUser
import com.example.myapplication.data.network.AuthAppRequest
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

/**
 * Repository que gestiona la autenticación híbrida:
 * 1. Validación XML-RPC contra Odoo (DelSIP).
 * 2. Autenticación REST para obtener Token de sesión de la API Raloy.
 */
class LoginRepository(private val sessionManager: SessionManager) {

    private val TAG = "LoginRepository"

    suspend fun login(username: String, pass: String): Result<LoggedInUser> {
        return withContext(Dispatchers.IO) {
            try {
                // --- FASE 1: VALIDACIÓN ODOO (XML-RPC) ---
                // Se valida contra el servidor especificado en tu script de Python (10.150.4.155)
                val uid = authenticateOdoo(username, pass)

                if (uid <= 0) {
                    return@withContext Result.failure(Exception("Credenciales de Odoo incorrectas"))
                }

                // --- FASE 2: AUTENTICACIÓN API REST RALOY ---
                // Obtenemos el Bearer Token necesario para MongoDB y n8n
                val authRequest = AuthAppRequest(
                    username = "app-movile-001",
                    password = "Zsh4cvz4tvGyQa56P"
                )

                val response = RetrofitClient.instance.autenticateApp(authRequest)

                if (response.isSuccessful && response.body()?.data != null) {
                    val token = response.body()!!.data!!.key

                    // --- FASE 3: PERSISTENCIA SEGURA ---
                    sessionManager.saveSession(uid, username)
                    sessionManager.saveToken(token)

                    Log.d(TAG, "Login exitoso para: $username con UID: $uid")
                    Result.success(LoggedInUser(uid.toString(), username))
                } else {
                    Result.failure(Exception("Error al obtener Token de la API Raloy"))
                }

            } catch (e: Exception) {
                Log.error(TAG, "Error en proceso de Login: ${e.message}")
                Result.failure(e)
            }
        }
    }


    fun logout() {
        sessionManager.clearSession()
    }
}