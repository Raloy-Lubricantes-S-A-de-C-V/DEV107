package com.example.deviceappend.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.deviceappend.core.network.UserProfile

class SessionManager(context: Context) {

    // ==========================================
    // SEGURIDAD: SharedPreferences Encriptadas (RESTAURADO)
    // ==========================================
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
        private const val KEY_NAME = "name"
        private const val KEY_LIDER = "lider"
        private const val KEY_SYS = "sys"
        private const val KEY_ADMIN = "admin"
        private const val KEY_NORMAL = "normal"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    // ==========================================
    // GUARDADO DEL PERFIL COMPLETO (RESTAURADO)
    // ==========================================
    fun saveUserProfile(profile: UserProfile) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, profile.id ?: -1)
            putString(KEY_USERNAME, profile.user ?: "")
            putString(KEY_NAME, profile.name ?: "")
            putInt(KEY_LIDER, profile.lider ?: 0)
            putInt(KEY_SYS, profile.sys ?: 0)
            putInt(KEY_ADMIN, profile.admin ?: 0)
            putInt(KEY_NORMAL, profile.normal ?: 1)
            apply()
        }
    }

    // ==========================================
    // GETTERS PARA HOME Y OTROS FRAGMENTS (RESTAURADOS)
    // ==========================================
    fun getUid(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getName(): String? = prefs.getString(KEY_NAME, null)

    // ==========================================
    // VERIFICACIÓN DE ROLES (INCLUYE isSys)
    // ==========================================
    fun isSys(): Boolean = prefs.getInt(KEY_SYS, 0) == 1
    fun isLider(): Boolean = prefs.getInt(KEY_LIDER, 0) == 1
    fun isAdmin(): Boolean = prefs.getInt(KEY_ADMIN, 0) == 1
    fun isNormal(): Boolean = prefs.getInt(KEY_NORMAL, 0) == 1

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}