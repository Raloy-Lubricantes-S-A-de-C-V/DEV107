package com.example.deviceappend.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.databinding.FragmentLoginBinding
import com.example.deviceappend.ui.home.HomeFragment
import com.example.deviceappend.core.LoginRepository
import com.example.deviceappend.core.session.SessionManager

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(LoginRepository(SessionManager(requireContext())))
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.btnLogin.setOnClickListener {
            val user = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            if (user.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.login(user, pass)
            } else {
                Toast.makeText(context, "Ingrese credenciales válidas", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ForgotPasswordFragment(), true)
        }

        binding.tvCreateAccount.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(RegisterFragment(), true)
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.user.message, Toast.LENGTH_LONG).show()
                    (activity as? MainActivity)?.replaceFragment(HomeFragment())
                }
                is LoginState.RequirePasswordChange -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Requiere actualizar contraseña temporal", Toast.LENGTH_LONG).show()
                    (activity as? MainActivity)?.replaceFragment(ChangePasswordFragment(), true)
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}