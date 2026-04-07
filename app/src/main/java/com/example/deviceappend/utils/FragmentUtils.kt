package com.example.deviceappend.utils

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import kotlinx.coroutines.*

fun Fragment.checkconnect(
    rootView: View,
    mensaje: String = "Validando seguridad...",
    onSuccess: suspend CoroutineScope.() -> Unit
) {
    val sessionManager = SessionManager(requireContext())

    // 1. Verificación local rápida
    if (sessionManager.getToken().isNullOrEmpty()) {
        Log.e("SecurityCheck", "Acceso denegado: No hay sesión.")
        rootView.visibility = View.GONE
        (activity as? MainActivity)?.logout()
        return
    }

    // 2. Ocultamos la vista principal mientras validamos
    rootView.visibility = View.INVISIBLE

    val overlay = activity?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = activity?.findViewById<TextView>(R.id.tvLoadingTitle)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = mensaje

    viewLifecycleOwner.lifecycleScope.launch {
        var conectado = false
        var ultimoErrorHttp = -1 // Variable para capturar el error exacto

        val api = RetrofitClient.instance

        withContext(Dispatchers.IO) {
            for (intento in 1..3) {
                try {
                    val response = api.checkDatabaseConnectivity()

                    if (response.isSuccessful) {
                        conectado = true
                        break // Salimos del ciclo si fue exitoso
                    } else {
                        // Capturamos el código de error (ej. 404)
                        ultimoErrorHttp = response.code()
                        Log.e("SecurityCheck", "Intento $intento: El servidor respondió con HTTP $ultimoErrorHttp")
                    }
                } catch (e: Exception) {
                    Log.e("SecurityCheck", "Intento $intento al API fallido por red: ${e.message}")
                }
                // Esperamos 1.5 segundos antes del siguiente intento
                if (intento < 3) delay(1500)
            }
        }

        // Ocultamos el spinner de carga
        overlay?.visibility = View.GONE

        if (conectado) {
            // Todo OK: Mostramos la interfaz
            rootView.visibility = View.VISIBLE
            onSuccess()
        } else {
            // PREPARAMOS EL MENSAJE DE ERROR EXACTO
            val mensajeError = if (ultimoErrorHttp != -1) {
                "Fallo API: Endpoint check-connectivity retornó HTTP $ultimoErrorHttp"
            } else {
                "Fallo API: El servidor no responde o no hay red."
            }

            Log.e("SecurityCheck", mensajeError)

            // MOSTRAMOS EL ERROR AL USUARIO ANTES DE SACARLO
            Toast.makeText(context, mensajeError, Toast.LENGTH_LONG).show()

            rootView.visibility = View.GONE
            (activity as? MainActivity)?.logout()
        }
    }
}