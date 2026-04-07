package com.example.deviceappend.utils

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import kotlinx.coroutines.*

/**
 * Verifica la sesión local y la conexión a la API (3 intentos).
 * Si algo falla, mata la sesión y redirige al login.
 */
fun Fragment.checkconnect(
    mensaje: String = "Validando conexión...",
    onSuccess: suspend CoroutineScope.() -> Unit
) {
    val sessionManager = SessionManager(requireContext())

    // 1. Verificar si existe una sesión válida localmente
    if (sessionManager.getToken().isNullOrEmpty()) {
        Log.e("CheckConnect", "No hay sesión activa. Redirigiendo a Login.")
        (activity as? MainActivity)?.logout()
        return
    }

    // 2. Preparar la pantalla de carga (Overlay)
    val overlay = activity?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = activity?.findViewById<TextView>(R.id.tvLoadingTitle)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = mensaje

    viewLifecycleOwner.lifecycleScope.launch {
        var conectado = false
        val api = RetrofitClient.instance

        withContext(Dispatchers.IO) {
            // 3. Validar conexión con el API Raloy (Máximo 3 intentos)
            for (intento in 1..3) {
                try {
                    val response = api.checkDatabaseConnectivity()
                    if (response.isSuccessful) {
                        conectado = true
                        break
                    }
                } catch (e: Exception) {
                    Log.e("CheckConnect", "Intento de conexión $intento fallido")
                }
                // Si no es el último intento, esperar 1.5 segundos antes de reintentar
                if (intento < 3) delay(1500)
            }
        }

        // 4. Ocultar pantalla de carga
        overlay?.visibility = View.GONE

        // 5. Decidir qué hacer
        if (conectado) {
            onSuccess() // Mostrar el fragmento normalmente
        } else {
            Log.e("CheckConnect", "Conexión fallida tras 3 intentos. Matando sesión.")
            (activity as? MainActivity)?.logout() // Mata sesión y va a Login
        }
    }
}