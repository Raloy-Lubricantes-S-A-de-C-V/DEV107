package com.example.deviceappend.ui.login

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.*
import com.example.deviceappend.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private var leaderList: List<UserListItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        cargarLideres()

        binding.btnRegister.setOnClickListener { ejecutarRegistro() }
    }

    private fun cargarLideres() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.listUsers()
                if (response.isSuccessful && response.body()?.error == false) {
                    leaderList = response.body()!!.data.filter { it.lider == 1 }
                    val names = leaderList.map { it.name }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvLeader.setAdapter(adapter)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fallo al cargar líderes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ejecutarRegistro() {
        val name = binding.etFullName.text.toString().trim()
        val mail = binding.etRegisterEmail.text.toString().trim()
        val selectedLeader = binding.actvLeader.text.toString()
        val leaderId = leaderList.find { it.name == selectedLeader }?.id ?: -1

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
                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}