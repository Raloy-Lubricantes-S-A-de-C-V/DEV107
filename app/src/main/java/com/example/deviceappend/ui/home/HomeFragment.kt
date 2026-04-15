package com.example.deviceappend.ui.home

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
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.databinding.FragmentHomeBinding
import com.example.deviceappend.ui.empresas.EmpresasFragment
import com.example.deviceappend.ui.prospectos.NotificationsFragment
import com.example.deviceappend.ui.prospectos.ProspectosFragment
import com.example.deviceappend.ui.tecnicos.TecnicosFragment
import com.example.deviceappend.ui.wizard.WizardFragment
import com.example.deviceappend.utils.checkconnect
import kotlinx.coroutines.launch

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
        setupMenu()

        checkconnect(binding.root) { setupUI() }
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

                // Ocultar botones que no van en el Home
                menu.findItem(R.id.action_back_to_login)?.isVisible = false
                menu.findItem(R.id.action_home)?.isVisible = false

                // Buscar la campanita que ya existe en el XML
                val notifItem = menu.findItem(R.id.action_notifications)
                notifItem?.isVisible = sessionManager.isAdmin() // Ocultar campanita si es técnico normal

                // Ejecutar conteo solo si es admin
                if (sessionManager.isAdmin()) {
                    fetchNotificationsCount(notifItem)
                }

                menu.findItem(R.id.action_modules)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true

                // Permisos DENTRO del menú de hamburguesa
                menu.findItem(R.id.action_empresas)?.isVisible = sessionManager.isSys()
                menu.findItem(R.id.action_tecnicos)?.isVisible = sessionManager.isAdmin()
                menu.findItem(R.id.action_prospectos)?.isVisible = sessionManager.isAdmin()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_notifications -> {
                        (activity as? MainActivity)?.replaceFragment(NotificationsFragment(), true)
                        true
                    }
                    R.id.action_prospectos -> {
                        (activity as? MainActivity)?.replaceFragment(ProspectosFragment(), true)
                        true
                    }
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
    }

    private fun fetchNotificationsCount(item: MenuItem?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.getProspectos()
                if (res.isSuccessful && res.body()?.error == false) {
                    val count = res.body()!!.data.count { it.view == 0 && it.open == 1 }
                    if (count > 0) {
                        item?.title = "Notificaciones ($count)"
                        item?.setIcon(android.R.drawable.ic_dialog_alert) // Cambia a icono de alerta
                    } else {
                        item?.setIcon(android.R.drawable.ic_popup_reminder) // Campana normal
                    }
                }
            } catch(e: Exception) {
                // Falla silenciosa si no hay red para no interrumpir el Home
            }
        }
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