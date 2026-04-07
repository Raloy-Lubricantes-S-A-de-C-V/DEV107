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

fun Fragment.checkconnect(
    rootView: View,
    mensaje: String = "Validando seguridad...",
    onSuccess: suspend CoroutineScope.() -> Unit
) {
    val sessionManager = SessionManager(requireContext())

    if (sessionManager.getToken().isNullOrEmpty()) {
        Log.e("SecurityCheck", "Acceso denegado: No hay sesión.")
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
        val api = RetrofitClient.instance

        withContext(Dispatchers.IO) {
            for (intento in 1..3) {
                try {
                    val response = api.checkDatabaseConnectivity()
                    if (response.isSuccessful) {
                        conectado = true
                        break
                    }
                } catch (e: Exception) {
                    Log.e("SecurityCheck", "Intento $intento al API fallido: ${e.message}")
                }
                if (intento < 3) delay(1500)
            }
        }

        overlay?.visibility = View.GONE

        if (conectado) {
            rootView.visibility = View.VISIBLE
            onSuccess()
        } else {
            Log.e("SecurityCheck", "Fallo de conexión al API. Abortando y limpiando sesión.")
            rootView.visibility = View.GONE
            (activity as? MainActivity)?.logout()
        }
    }
}