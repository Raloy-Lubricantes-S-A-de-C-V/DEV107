package com.example.myapplication.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SessionManager: Gestión de persistencia cifrada con AES256.
 * Protege el Token de la API de Raloy y los datos del técnico en reposo.
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_SUPER_ADMIN = "is_super_admin"
    }

    /**
     * Guarda el Bearer Token obtenido de la API de Raloy.
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /**
     * Registra la sesión del técnico tras validar en Odoo/DelSIP.
     */
    fun saveSession(uid: Int, username: String, isSuperAdmin: Boolean = false) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, uid)
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_SUPER_ADMIN, isSuperAdmin)
            apply()
        }
    }

    fun getUid(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun isSuperAdmin(): Boolean = prefs.getBoolean(KEY_IS_SUPER_ADMIN, false)

    /**
     * Limpia la sesión (Logout). Utilizado por MainActivity y AuthInterceptor
     * si el token expira (401).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}