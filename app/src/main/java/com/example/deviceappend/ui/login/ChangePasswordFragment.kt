package com.example.myapplication.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.MainActivity
import com.example.myapplication.R
// Importación corregida para View Binding
import com.example.myapplication.databinding.FragmentChangePasswordBinding

/**
 * Fragmento para el flujo de cambio de contraseña obligatorio (Super Admin).
 */
class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChangePasswordBinding.bind(view)

        binding.btnUpdatePassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (validatePassword(newPass, confirmPass)) {
                // Aquí se realizaría la actualización en el sistema interno
                Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                (activity as MainActivity).replaceFragment(LoginFragment())
            }
        }
    }

    private fun validatePassword(p1: String, p2: String): Boolean {
        if (p1.length < 8) {
            binding.etNewPassword.error = "Mínimo 8 caracteres"
            return false
        }
        if (p1 != p2) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}