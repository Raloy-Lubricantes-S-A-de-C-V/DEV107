package com.example.myapplication.utils

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.*

/**
 * Ejecuta una operación de red dentro de un entorno seguro.
 * - Muestra un overlay de carga bloqueante.
 * - Realiza hasta 3 intentos de verificación de sesión (Auto-Refresh).
 * - Ejecuta la acción principal solo si la sesión es válida.
 */
fun Fragment.ejecutarFlujoSeguro(
    tituloCarga: String,
    onSuccess: suspend CoroutineScope.() -> Unit,
    onError: (String) -> Unit
) {
    // Referencias a la UI de carga definida en overlay_loading.xml
    val overlay = view?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = view?.findViewById<TextView>(R.id.tvLoadingTitle)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = tituloCarga

    viewLifecycleOwner.lifecycleScope.launch {
        var sesionValida = false
        val api = RetrofitClient.instance

        // Lógica de Re-intento (Módulo de Seguridad 1)
        withContext(Dispatchers.IO) {
            for (intento in 1..3) {
                try {
                    val response = api.autenticateApp( /* Credenciales de refresco */ )
                    if (response.isSuccessful) {
                        sesionValida = true
                        break
                    }
                } catch (e: Exception) {
                    Log.error("FlujoSeguro", "Intento $intento fallido")
                }
                delay(500) // Pausa breve entre reintentos
            }
        }

        if (sesionValida) {
            try {
                onSuccess()
            } catch (e: Exception) {
                onError("Error en la operación: ${e.message}")
            } finally {
                overlay?.visibility = View.GONE
            }
        } else {
            overlay?.visibility = View.GONE
            onError("Sesión expirada. Por favor re-ingrese sus credenciales.")
            // Aquí se dispararía (activity as MainActivity).logout()
        }
    }
}