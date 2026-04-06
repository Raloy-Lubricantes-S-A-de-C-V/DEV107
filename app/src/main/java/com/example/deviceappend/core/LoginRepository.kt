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
                val appAuth = api.autenticateApp(AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P"))
                if (!appAuth.isSuccessful) return@withContext Result.failure(Exception("Fallo App Auth"))
                sessionManager.saveToken(appAuth.body()?.data?.key ?: "")

                // FASE 2: Login de Usuario Final
                val userAuth = api.loginUser(UserLoginRequest(email, pass))
                if (userAuth.isSuccessful && userAuth.body()?.data != null) {
                    val userToken = userAuth.body()!!.data!!.key

                    // FASE 3: Consultar Permisos (Error corregido: api en lugar de ap1)
                    val sysAdminRes = api.checkIsSysAdmin(CheckSysAdminRequest(user = email))
                    val isSystemAdmin = sysAdminRes.body()?.is_sys ?: false

                    // FASE 4: Persistencia
                    val internalUid = email.hashCode()
                    sessionManager.saveSession(internalUid, email, isSystemAdmin)
                    sessionManager.saveToken(userToken)

                    Result.success(LoggedInUser(internalUid.toString(), email))
                } else {
                    Result.failure(Exception("Credenciales inválidas"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}