package com.example.deviceappend.ui.wizard

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.databinding.FragmentWizardBinding
import com.example.deviceappend.utils.verificarConexionYEjecutar

class WizardFragment : Fragment(R.layout.fragment_wizard) {

    private val viewModel: WizardViewModel by viewModels()
    private var _binding: FragmentWizardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWizardBinding.bind(view)

        // 1. Registro del Menú (Para ver el botón Home)
        setupMenu()

        // 2. Verificación de conexión obligatoria al iniciar el Wizard
        verificarConexionYEjecutar("Preparando validación...") {
            setupUI()
        }
    }

    private fun setupUI() {
        binding.btnNext.setOnClickListener {
            // Lógica para avanzar en los pasos del Wizard
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                // Aquí NO ocultamos action_home, para que sea visible
                menu.findItem(R.id.action_home)?.isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // La acción la maneja MainActivity.kt
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}