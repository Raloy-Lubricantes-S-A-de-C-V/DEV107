package com.example.deviceappend.ui.login

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.*
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private var leaderList: List<UserListItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        setupRegisterMenu()
        autenticarYListar()

        binding.btnRegister.setOnClickListener { ejecutarRegistro() }
    }

    private fun autenticarYListar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.instance
                val session = SessionManager(requireContext())

                // PASO 1: Iniciar Sesión de App para obtener Token
                val authRes = api.autenticateApp(AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P"))

                if (authRes.isSuccessful && authRes.body()?.data != null) {
                    session.saveToken(authRes.body()!!.data!!.key)
                    Log.d("DEBUG_REGISTRO", "Token inyectado, llamando a users/list...")
                    cargarLideres()
                } else {
                    Log.e("DEBUG_REGISTRO", "Error 404: Verifica que el endpoint sea /api/v1/users/list")
                }
            } catch (e: Exception) {
                Log.e("DEBUG_REGISTRO", "Excepción en flujo: ${e.message}")
            }
        }
    }

    private suspend fun cargarLideres() {
        try {
            val response = RetrofitClient.instance.listUsers()
            if (response.isSuccessful && response.body()?.error == false) {
                val allUsers = response.body()!!.data
                leaderList = allUsers.filter { it.lider == 1 }

                val names = leaderList.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                binding.actvLeader.setAdapter(adapter)
                binding.actvLeader.setText("", false)
            }
        } catch (e: Exception) {
            Log.e("DEBUG_REGISTRO", "Error al procesar lista: ${e.message}")
        }
    }

    private fun ejecutarRegistro() {
        val name = binding.etFullName.text.toString().trim()
        val mail = binding.etRegisterEmail.text.toString().trim()
        val leaderName = binding.actvLeader.text.toString()
        val leaderId = leaderList.find { it.name == leaderName }?.id ?: -1

        if (name.isEmpty() || mail.isEmpty() || leaderId == -1) {
            Toast.makeText(context, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = RegisterRequest(name, mail, leaderId, (1000..9999).random())
                val response = RetrofitClient.instance.registerNewUser(request)
                if (response.isSuccessful && response.body()?.error == false) {
                    Toast.makeText(context, "Solicitud enviada correctamente", Toast.LENGTH_LONG).show()
                    (activity as? MainActivity)?.replaceFragment(LoginFragment())
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fallo de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRegisterMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_home)?.isVisible = false
                menu.findItem(R.id.action_logout)?.isVisible = false
                menu.findItem(R.id.action_back_to_login)?.isVisible = true
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}