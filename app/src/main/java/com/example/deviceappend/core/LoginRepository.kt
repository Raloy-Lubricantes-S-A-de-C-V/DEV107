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

                // FASE 1: Autenticación de la Aplicación
                val appAuthRequest = AuthAppRequest(
                    username = "app-movile-001",
                    password = "Zsh4cvz4tvGyQa56P"
                )
                val appResponse = api.autenticateApp(appAuthRequest)

                if (!appResponse.isSuccessful || appResponse.body()?.data == null) {
                    return@withContext Result.failure(Exception("Fallo en autenticación de aplicación"))
                }

                sessionManager.saveToken(appResponse.body()!!.data!!.key)

                // FASE 2: Login de Usuario Final
                val userRequest = UserLoginRequest(username = email, password = pass)
                val userResponse = api.loginUser(userRequest)

                if (userResponse.isSuccessful && userResponse.body()?.data != null) {
                    val userToken = userResponse.body()!!.data!!.key

                    // FASE 3: Consultar Permisos (is_sys)
                    // CORRECCIÓN: Se cambió 'ap1' por 'api' para coincidir con la variable declarada
                    val sysAdminResponse = api.checkIsSysAdmin(CheckSysAdminRequest(user = email))
                    val isSystemAdmin = sysAdminResponse.body()?.is_sys ?: false

                    // FASE 4: Persistencia Segura Final
                    val internalUid = email.hashCode()
                    sessionManager.saveSession(internalUid, email, isSystemAdmin)
                    sessionManager.saveToken(userToken)

                    Result.success(LoggedInUser(internalUid.toString(), email))
                } else {
                    Result.failure(Exception("Credenciales de usuario inválidas"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}