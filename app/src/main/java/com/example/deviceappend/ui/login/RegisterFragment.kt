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

                val authRes = api.autenticateApp(AuthAppRequest("app-movile-001", "Zsh4cvz4tvGyQa56P"))

                if (authRes.isSuccessful && authRes.body()?.data != null) {
                    session.saveToken(authRes.body()!!.data!!.key)
                    cargarLideres()
                } else {
                    Log.e("DEBUG_REGISTRO", "Fallo Auth: ${authRes.code()}")
                }
            } catch (e: Exception) {
                Log.e("DEBUG_REGISTRO", "Excepción Auth: ${e.message}")
            }
        }
    }

    private suspend fun cargarLideres() {
        try {
            val response = RetrofitClient.instance.listUsers()
            if (response.isSuccessful && response.body()?.error == false) {
                val allUsers = response.body()!!.data

                leaderList = allUsers.filter {
                    val valLider = it.lider.toString().lowercase()
                    valLider == "1" || valLider == "1.0" || valLider == "true"
                }

                val names = leaderList.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                binding.actvLeader.setAdapter(adapter)

                binding.actvLeader.setText("", false)
            }
        } catch (e: Exception) {
            Log.e("DEBUG_REGISTRO", "Error de red al listar: ${e.message}")
        }
    }

    private fun ejecutarRegistro() {
        val name = binding.etFullName.text.toString().trim()
        val mail = binding.etRegisterEmail.text.toString().trim()
        val selectedLeaderName = binding.actvLeader.text.toString()

        val leader = leaderList.find { it.name == selectedLeaderName }
        val leaderId = leader?.id ?: -1

        // CORRECCIÓN: Leemos exactamente el campo 'user' mapeado
        val leaderEmail = leader?.user ?: ""

        Log.d("DEBUG_REGISTRO", "--- DATOS DEL LÍDER ---")
        Log.d("DEBUG_REGISTRO", "ID del Líder a enviar (parent_id): $leaderId")
        Log.d("DEBUG_REGISTRO", "Correo del líder extraído: '$leaderEmail'")

        if (name.isEmpty() || mail.isEmpty() || leaderId == -1) {
            Toast.makeText(context, "Complete todos los campos y seleccione un líder", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Enviamos el registro a la base de datos
                val request = RegisterRequest(name, mail, leaderId, (1000..9999).random())
                val response = RetrofitClient.instance.registerNewUser(request)

                if (response.isSuccessful && response.body()?.error == false) {
                    Log.d("DEBUG_REGISTRO", "Usuario registrado en BD correctamente. Intentando N8N...")

                    // 2. Disparamos el Webhook de notificación a n8n
                    try {
                        if (leaderEmail.isNotEmpty()) {
                            val mensaje = "Hola ${leader?.name},\n\nEl usuario $name con correo $mail ha intentado ingresar como nuevo técnico y te ha seleccionado como su líder.\n\nEs necesario que ingreses a la aplicación Raloy Asset Manager en el módulo de autorización para aceptar su cuenta."

                            val webhookReq = NewTechnicianWebhookRequest(email = leaderEmail, mensaje = mensaje)
                            val n8nResponse = RetrofitClient.instance.sendNewTechnicianWebhook(webhookReq)

                            if (n8nResponse.isSuccessful) {
                                Log.d("DEBUG_REGISTRO", "ÉXITO: N8N respondió con código ${n8nResponse.code()}")
                            } else {
                                Log.e("DEBUG_REGISTRO", "ERROR N8N: Código ${n8nResponse.code()} - Detalle: ${n8nResponse.errorBody()?.string()}")
                            }
                        } else {
                            Log.e("DEBUG_REGISTRO", "OMITIDO N8N: El correo del líder sigue vacío.")
                        }
                    } catch (e: Exception) {
                        Log.e("DEBUG_REGISTRO", "CRASH al enviar webhook a n8n: ${e.message}")
                    }

                    // 3. Finalizamos y redirigimos al Login
                    Toast.makeText(context, "Solicitud enviada correctamente", Toast.LENGTH_LONG).show()
                    (activity as? MainActivity)?.replaceFragment(LoginFragment())
                } else {
                    Toast.makeText(context, "Fallo al enviar registro: ${response.body()?.msj}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red en el servidor de Kiosko", Toast.LENGTH_SHORT).show()
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