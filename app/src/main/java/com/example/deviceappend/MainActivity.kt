package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import com.example.myapplication.ui.login.LoginFragment
import com.example.myapplication.ui.wizard.WizardFragment

/**
 * MainActivity: Orquestador de Ruteo y Sesión.
 * Se encarga de inicializar los componentes base y decidir el flujo inicial.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicializar Motores de Red y Sesión
        // Retrofit se inicializa con el contexto para el AuthInterceptor
        RetrofitClient.init(applicationContext)
        sessionManager = SessionManager(this)

        // 2. Lógica de Ruteo Inicial (Guard Guardrail)
        if (savedInstanceState == null) {
            checkSessionAndNavigate()
        }
    }

    /**
     * Verifica si existe un UID y Token activo en EncryptedSharedPreferences.
     * Si no hay sesión, fuerza el Login.
     */
    private fun checkSessionAndNavigate() {
        val userToken = sessionManager.getToken()
        val userId = sessionManager.getUid()

        if (userToken != null && userId != -1) {
            // Usuario autenticado -> Ir directamente al Wizard de Enrolamiento
            replaceFragment(WizardFragment())
        } else {
            // Sesión inexistente -> Ir a Login
            replaceFragment(LoginFragment())
        }
    }

    /**
     * Utilidad para intercambio de fragmentos en el contenedor principal.
     */
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    /**
     * Método público para cierre de sesión global desde cualquier módulo.
     */
    fun logout() {
        sessionManager.clearSession()
        replaceFragment(LoginFragment())
    }
}