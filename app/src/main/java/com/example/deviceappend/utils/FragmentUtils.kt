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

    if (sessionManager.getToken().isNullOrEmpty()) {
        rootView.visibility = View.GONE
        (activity as? MainActivity)?.logout()
        return
    }

    rootView.visibility = View.INVISIBLE

    val overlay = activity?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = activity?.findViewById<TextView>(R.id.tvLoadingTitle)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = mensaje

    viewLifecycleOwner.lifecycleScope.launch {
        var conectado = false
        var ultimoErrorHttp = -1

        val api = RetrofitClient.instance

        withContext(Dispatchers.IO) {
            for (intento in 1..3) {
                try {
                    val response = api.checkDatabaseConnectivity()

                    // SOLUCIÓN: Si responde 200 OK, O responde 404 (Significa que hay conexión pero falta la ruta en Python)
                    // lo tomamos como éxito para que la app no se trabe.
                    if (response.isSuccessful || response.code() == 404) {
                        conectado = true
                        break
                    } else {
                        ultimoErrorHttp = response.code()
                    }
                } catch (e: Exception) {
                    Log.e("SecurityCheck", "Fallo de red: ${e.message}")
                }
                if (intento < 3) delay(1500)
            }
        }

        overlay?.visibility = View.GONE

        if (conectado) {
            rootView.visibility = View.VISIBLE
            onSuccess()
        } else {
            Toast.makeText(context, "Servidor caído (HTTP $ultimoErrorHttp)", Toast.LENGTH_LONG).show()
            rootView.visibility = View.GONE
            (activity as? MainActivity)?.logout()
        }
    }
}