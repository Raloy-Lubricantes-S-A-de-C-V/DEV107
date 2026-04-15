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
import com.example.deviceappend.utils.checkconnect

class WizardFragment : Fragment(R.layout.fragment_wizard) {

    private val viewModel: WizardViewModel by viewModels()
    private var _binding: FragmentWizardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWizardBinding.bind(view)

        // CORTAFUEGOS
        checkconnect(binding.root, "Preparando enrolamiento...") {
            setupMenu()
            setupUI()
        }
    }

    private fun setupUI() {
        binding.btnNext.setOnClickListener {
            // Lógica
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_logout)?.isVisible = false
                menu.findItem(R.id.action_home)?.isVisible = true
                menu.findItem(R.id.action_modules)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}