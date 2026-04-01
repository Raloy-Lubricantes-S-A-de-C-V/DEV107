package com.example.deviceappend.utils

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.R
import com.example.deviceappend.core.network.AuthAppRequest
import com.example.deviceappend.core.network.RetrofitClient
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
            try {
                val request = AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P")
                val response = api.autenticateApp(request)
                if (response.isSuccessful) sesionValida = true else sesionValida = false
            } catch (e: Exception) {
                Log.e("FlujoSeguro", "Error: ${e.message}")
            }
        }

        if (sesionValida) {
            try { onSuccess() } catch (e: Exception) { onError("Error: ${e.message}") }
            finally { overlay?.visibility = View.GONE }
        } else {
            overlay?.visibility = View.GONE
            onError("Sesión expirada.")
        }
    }
}