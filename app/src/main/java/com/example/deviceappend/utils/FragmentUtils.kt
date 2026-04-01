package com.example.myapplication.utils

import android.util.Log // CORRECCIÓN: Importar Log de Android
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.network.AuthAppRequest // CORRECCIÓN: Importar el modelo
import com.example.myapplication.data.network.RetrofitClient
import kotlinx.coroutines.*

fun Fragment.ejecutarFlujoSeguro(
    tituloCarga: String,
    onSuccess: suspend CoroutineScope.() -> Unit,
    onError: (String) -> Unit
) {
    val overlay = view?.findViewById<View>(R.id.overlayLoading)
    val tvTitle = view?.findViewById<TextView>(R.id.tvLoadingTitle)

    overlay?.visibility = View.VISIBLE
    tvTitle?.text = tituloCarga

    viewLifecycleOwner.lifecycleScope.launch {
        var sesionValida = false
        val api = RetrofitClient.instance

        withContext(Dispatchers.IO) {
            for (intento in 1..3) {
                try {
                    // CORRECCIÓN: Pasar las credenciales requeridas por ApiService
                    val request = AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P")
                    val response = api.autenticateApp(request)
                    if (response.isSuccessful) {
                        sesionValida = true
                        break
                    }
                } catch (e: Exception) {
                    Log.e("FlujoSeguro", "Intento $intento fallido: ${e.message}")
                }
                delay(500)
            }
        }

        if (sesionValida) {
            try {
                onSuccess()
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            } finally {
                overlay?.visibility = View.GONE
            }
        } else {
            overlay?.visibility = View.GONE
            onError("Sesión expirada.")
        }
    }
}