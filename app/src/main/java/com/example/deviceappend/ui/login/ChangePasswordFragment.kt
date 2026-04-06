package com.example.deviceappend.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
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
                ejecutarCambioDeContrasena(pass1)
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
                val hashRes = api.getPasswordHash(nuevaClave)
                val hashGenerado = hashRes.body()?.get("hash")

                if (hashRes.isSuccessful && hashGenerado != null) {
                    val updateRes = api.updatePassword(UpdatePasswordRequest(email, hashGenerado))
                    if (updateRes.isSuccessful) {
                        Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_LONG).show()
                        (activity as? MainActivity)?.logout()
                    } else {
                        Toast.makeText(context, "Error al actualizar en servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fallo de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}