package com.example.deviceappend.ui.login

import android.os.Bundle
import android.view.*
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
        setupMenu()

        binding.btnSendEmail.setOnClickListener { sendEmailFlow() }
        binding.btnResendEmail.setOnClickListener { sendEmailFlow() }

        binding.btnVerifyCode.setOnClickListener {
            val input = binding.etVerificationCode.text.toString().trim()
            if (input == currentSalt && input.isNotEmpty()) {
                val session = SessionManager(requireContext())
                session.saveSession(0, binding.etRecoveryEmail.text.toString().trim(), false)
                (activity as? MainActivity)?.replaceFragment(ChangePasswordFragment(), true)
            } else {
                Toast.makeText(context, "Código incorrecto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailFlow() {
        val email = binding.etRecoveryEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.etRecoveryEmail.error = "Ingrese su correo"
            return
        }
        currentSalt = (10000..99999).random().toString()

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.sendRecoveryEmail(RecoveryEmailRequest(email, currentSalt))
                if (res.isSuccessful) {
                    binding.cardValidation.visibility = View.VISIBLE
                    binding.btnSendEmail.visibility = View.GONE
                    Toast.makeText(context, "Código enviado exitosamente", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_home)?.isVisible = true
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}