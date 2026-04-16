package com.example.deviceappend.ui.login

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.RecoveryEmailRequest
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private var currentSalt: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        // Ocultar menú superior en la pantalla de recuperación de contraseña
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).supportActionBar?.hide()

        binding.btnSendEmail.setOnClickListener {
            hideKeyboard()
            sendEmailFlow()
        }

        binding.btnVerifyCode.setOnClickListener {
            val input = binding.etVerificationCode.text.toString().trim()
            if (input == currentSalt && input.isNotEmpty()) {
                val session = SessionManager(requireContext())
                session.saveUsername(binding.etRecoveryEmail.text.toString().trim())
                (activity as? MainActivity)?.replaceFragment(ChangePasswordFragment(), true)
            } else {
                Toast.makeText(context, "Código incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnResendEmail.setOnClickListener { sendEmailFlow() }
    }

    private fun sendEmailFlow() {
        val email = binding.etRecoveryEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.etRecoveryEmail.error = "Ingrese su correo"
            return
        }
        currentSalt = (10000..99999).random().toString()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.sendRecoveryEmail(RecoveryEmailRequest(email, currentSalt))
                if (res.isSuccessful) {
                    binding.cardValidation.visibility = View.VISIBLE
                    binding.btnSendEmail.isEnabled = false
                    Toast.makeText(context, "Código enviado exitosamente", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restaurar menú superior al salir
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).supportActionBar?.show()
        _binding = null
    }
}