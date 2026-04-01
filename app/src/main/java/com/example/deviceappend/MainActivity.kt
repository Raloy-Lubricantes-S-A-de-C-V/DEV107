package com.example.deviceappend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
// Importaciones ajustadas a la carpeta 'core' que se ve en tu panel lateral
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.ui.login.LoginFragment
import com.example.deviceappend.ui.wizard.WizardFragment

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización de motores de red y sesión
        RetrofitClient.init(applicationContext)
        sessionManager = SessionManager(this)

        if (savedInstanceState == null) {
            checkSessionAndNavigate()
        }
    }

    private fun checkSessionAndNavigate() {
        val userToken = sessionManager.getToken()
        val userId = sessionManager.getUid()

        if (userToken != null && userId != -1) {
            replaceFragment(WizardFragment())
        } else {
            replaceFragment(LoginFragment())
        }
    }

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
}