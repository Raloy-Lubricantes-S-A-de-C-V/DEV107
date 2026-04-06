package com.example.deviceappend.utils

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.RetrofitClient
import kotlinx.coroutines.*

/**
 * Verifica la conexión 3 veces. Si falla, cierra sesión.
 */
fun Fragment.verificarConexionYEjecutar(
    mensaje: String = "Validando conexión...",
    onSuccess: suspend CoroutineScope.() -> Unit
) {
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
                    Log.e("Connectivity", "Intento $intento fallido")
                }
                if (intento < 3) delay(1500)
            }
        }

        overlay?.visibility = View.GONE
        if (conectado) {
            onSuccess()
        } else {
            (activity as? MainActivity)?.logout()
        }
    }
}