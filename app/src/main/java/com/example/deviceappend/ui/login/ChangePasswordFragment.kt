package com.example.deviceappend.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.AuthAppRequest
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.network.UpdatePasswordRequest
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.databinding.FragmentChangePasswordBinding
import kotlinx.coroutines.launch

class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChangePasswordBinding.bind(view)

        binding.btnUpdatePassword.setOnClickListener {
            val pass1 = binding.etNewPassword.text.toString()
            val pass2 = binding.etConfirmPassword.text.toString()

            if (pass1 == pass2 && pass1.isNotEmpty()) {
                if (pass1.length >= 8) {
                    ejecutarCambioDeContrasena(pass1)
                } else {
                    Toast.makeText(context, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ejecutarCambioDeContrasena(nuevaClave: String) {
        val session = SessionManager(requireContext())
        val email = session.getUsername() ?: ""
        val api = RetrofitClient.instance

        lifecycleScope.launch {
            try {
                // PASO 1: Obtener el Hash seguro
                val hashRes = api.getPasswordHash(nuevaClave)
                val hashGenerado = hashRes.body()?.get("hash")

                if (hashRes.isSuccessful && hashGenerado != null) {

                    // PASO 2: Re-autenticar App para obtener token fresco antes del update
                    val appAuth = api.autenticateApp(AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P"))

                    if (appAuth.isSuccessful && appAuth.body()?.data != null) {
                        // Guardamos el nuevo token para que el AuthInterceptor lo use en la siguiente llamada
                        session.saveToken(appAuth.body()!!.data!!.key)

                        // PASO 3: Mandar el Hash obtenido al endpoint update-password
                        val updateRes = api.updatePassword(UpdatePasswordRequest(email, hashGenerado))

                        if (updateRes.isSuccessful) {
                            Toast.makeText(context, "Contraseña actualizada. Inicie sesión.", Toast.LENGTH_LONG).show()
                            (activity as? MainActivity)?.logout()
                        } else {
                            Toast.makeText(context, "Error al actualizar en servidor", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Fallo de autenticación de seguridad", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error al generar hash", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}