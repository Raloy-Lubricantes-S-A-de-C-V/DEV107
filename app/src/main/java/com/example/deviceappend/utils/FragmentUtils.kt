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

// ==========================================
// 1. SISTEMA GLOBAL DE LOADERS VISUALES
// ==========================================
fun Fragment.showLoader(mensaje: String = "Procesando...") {
    activity?.runOnUiThread {
        val overlay = activity?.findViewById<View>(R.id.overlayLoading)
        val tvTitle = activity?.findViewById<TextView>(R.id.tvLoadingTitle)
        tvTitle?.text = mensaje
        overlay?.visibility = View.VISIBLE
    }
}

fun Fragment.hideLoader() {
    activity?.runOnUiThread {
        val overlay = activity?.findViewById<View>(R.id.overlayLoading)
        overlay?.visibility = View.GONE
    }
}

// ==========================================
// 2. CORTAFUEGOS DE SESIÓN Y CONECTIVIDAD
// ==========================================
fun Fragment.checkconnect(
    rootView: View,
    mensaje: String = "Validando seguridad...",
    onSuccess: suspend CoroutineScope.() -> Unit
) {
    val sessionManager = SessionManager(requireContext())

    // 1. Verificación local
    if (sessionManager.getToken().isNullOrEmpty()) {
        Log.e("SecurityCheck", "Acceso denegado: No hay sesión.")
        rootView.visibility = View.GONE
        (activity as? MainActivity)?.logout()
        return
    }

    // 2. Ocultar la vista principal y mostrar Loader
    rootView.visibility = View.INVISIBLE
    showLoader(mensaje)

    viewLifecycleOwner.lifecycleScope.launch {
        var conectado = false
        var ultimoErrorHttp = -1

        val api = RetrofitClient.instance

        withContext(Dispatchers.IO) {
            for (intento in 1..3) {
                try {
                    val response = api.checkDatabaseConnectivity()
                    // Toleramos 404 por si la ruta no está creada en producción, lo que importa es que el server responde
                    if (response.isSuccessful || response.code() == 404) {
                        conectado = true
                        break
                    } else {
                        ultimoErrorHttp = response.code()
                    }
                } catch (e: Exception) {
                    Log.e("SecurityCheck", "Intento $intento fallido: ${e.message}")
                }
                if (intento < 3) delay(1500)
            }
        }

        hideLoader()

        if (conectado) {
            rootView.visibility = View.VISIBLE
            onSuccess()
        } else {
            val msj = if (ultimoErrorHttp != -1) "Fallo Servidor (HTTP $ultimoErrorHttp)" else "Sin conexión a los servidores Raloy"
            Toast.makeText(context, msj, Toast.LENGTH_LONG).show()
            rootView.visibility = View.GONE
            (activity as? MainActivity)?.logout()
        }
    }
}