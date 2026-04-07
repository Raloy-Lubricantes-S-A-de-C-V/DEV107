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
import com.example.deviceappend.ui.wizard.WizardFragment
import com.example.deviceappend.utils.checkconnect

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // (Opcional) Si mantuviste el cortafuegos de FrameLayout del paso anterior
        // if (view is android.widget.FrameLayout) return

        _binding = FragmentHomeBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        // 1. Configuramos el menú de forma SÍNCRONA (fuera del checkconnect)
        setupMenu()

        // 2. CORRECCIÓN AQUÍ: Pasamos 'binding.root' para que el cortafuegos lo pueda ocultar
        checkconnect(binding.root) {
            setupUI()
        }
    }

    private fun setupUI() {
        // Obtenemos el nombre (name) del profile de la sesión en lugar del correo
        val userName = sessionManager.getName() ?: "Usuario"
        binding.tvWelcome.text = "¡Bienvenido,\n$userName!"
        setupClickListeners()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Limpiamos el caché del menú viejo (el del Login)
                menu.clear()

                menuInflater.inflate(R.menu.main_menu, menu)

                // Ocultamos los íconos que no deben ir en Home
                menu.findItem(R.id.action_home)?.isVisible = false
                menu.findItem(R.id.action_back_to_login)?.isVisible = false

                // Forzamos a que SÍ aparezca la hamburguesa y el cerrar sesión
                menu.findItem(R.id.action_modules)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_new_scanner -> {
                        (activity as? MainActivity)?.replaceFragment(ScannerFragment(), true)
                        true
                    }
                    R.id.action_new_metrics -> {
                        Toast.makeText(context, "Módulo de Reportes en construcción", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false // MainActivity maneja Logout
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Obligamos a la barra a redibujarse en ese preciso instante
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

        // Acción para el nuevo botón en la tarjeta del escáner
        binding.btnScanner.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ScannerFragment(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}