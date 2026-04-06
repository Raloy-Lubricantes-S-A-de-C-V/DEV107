package com.example.deviceappend.ui.home

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.databinding.FragmentHomeBinding
import com.example.deviceappend.ui.wizard.WizardFragment
import com.example.deviceappend.utils.verificarConexionYEjecutar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        verificarConexionYEjecutar {
            setupUI()
            setupMenu()
        }
    }

    private fun setupUI() {
        val user = sessionManager.getUsername() ?: "Técnico"
        binding.tvWelcome.text = "¡Bienvenido, $user!"
        setupClickListeners()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                // OCULTAR el botón Home cuando ya estamos en el fragmento Home
                menu.findItem(R.id.action_home)?.isVisible = false
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false // La MainActivity maneja las acciones del menú
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupClickListeners() {
        val clickListener = View.OnClickListener {
            (activity as? MainActivity)?.replaceFragment(WizardFragment(), true)
        }
        binding.btnHelpDesk.setOnClickListener(clickListener)
        binding.btnInventi.setOnClickListener(clickListener)
        binding.btnPRTG.setOnClickListener(clickListener)
        binding.btnProtection.setOnClickListener(clickListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}