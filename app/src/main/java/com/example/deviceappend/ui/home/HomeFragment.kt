package com.example.deviceappend.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.databinding.FragmentHomeBinding
import com.example.deviceappend.ui.empresas.EmpresasFragment
import com.example.deviceappend.ui.tecnicos.TecnicosFragment
import com.example.deviceappend.ui.wizard.WizardFragment
import com.example.deviceappend.utils.checkconnect

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        if (sessionManager.getToken().isNullOrEmpty()) {
            view.visibility = View.GONE
            (activity as? MainActivity)?.logout()
            return
        }

        _binding = FragmentHomeBinding.bind(view)

        // El menú se infla de inmediato
        setupMenu()

        checkconnect(binding.root) {
            setupUI()
        }
    }

    private fun setupUI() {
        val rawName = sessionManager.getName()
        val userName = if (!rawName.isNullOrBlank()) rawName else sessionManager.getUsername() ?: "Técnico"
        binding.tvWelcome.text = "¡Bienvenido,\n$userName!"
        setupClickListeners()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_back_to_login)?.isVisible = false
                menu.findItem(R.id.action_home)?.isVisible = false
                menu.findItem(R.id.action_modules)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true

                // Mostrar Empresas solo a "Sys"
                menu.findItem(R.id.action_empresas)?.isVisible = sessionManager.isSys()

                // Mostrar Técnicos solo a "Admin"
                menu.findItem(R.id.action_tecnicos)?.isVisible = sessionManager.isAdmin()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_tecnicos -> {
                        (activity as? MainActivity)?.replaceFragment(TecnicosFragment(), true)
                        true
                    }
                    R.id.action_empresas -> {
                        (activity as? MainActivity)?.replaceFragment(EmpresasFragment(), true)
                        true
                    }
                    R.id.action_new_scanner -> {
                        (activity as? MainActivity)?.replaceFragment(ScannerFragment(), true)
                        true
                    }
                    R.id.action_new_metrics -> {
                        Toast.makeText(context, "Módulo de Reportes en construcción", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        requireActivity().invalidateOptionsMenu()
    }

    private fun setupClickListeners() {
        val enrolarListener = View.OnClickListener {
            (activity as? MainActivity)?.replaceFragment(WizardFragment(), true)
        }
        binding.btnHelpDesk.setOnClickListener(enrolarListener)
        binding.btnInventi.setOnClickListener(enrolarListener)
        binding.btnPRTG.setOnClickListener(enrolarListener)
        binding.btnProtection.setOnClickListener(enrolarListener)

        binding.btnScanner.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ScannerFragment(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}