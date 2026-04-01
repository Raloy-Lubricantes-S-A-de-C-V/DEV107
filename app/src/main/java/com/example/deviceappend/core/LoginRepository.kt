package com.example.deviceappend.core

import com.example.deviceappend.data.model.LoggedInUser
import com.example.deviceappend.core.network.AuthAppRequest
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRepository(private val sessionManager: SessionManager) {

    suspend fun login(username: String, pass: String): Result<LoggedInUser> {
        return withContext(Dispatchers.IO) {
            try {
                val authRequest = AuthAppRequest(
                    username = "app-movile-001",
                    password = "Zsh4cvz4tvGyQa56P"
                )

                val response = RetrofitClient.instance.autenticateApp(authRequest)

                if (response.isSuccessful && response.body()?.data != null) {
                    val token = response.body()!!.data!!.key
                    sessionManager.saveSession(username.hashCode(), username, false)
                    sessionManager.saveToken(token)
                    Result.success(LoggedInUser(username.hashCode().toString(), username))
                } else {
                    Result.failure(Exception("Error de autenticación"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}