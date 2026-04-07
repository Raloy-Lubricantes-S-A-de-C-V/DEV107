package com.example.deviceappend.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.deviceappend.R
import com.example.deviceappend.utils.checkconnect
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.example.deviceappend.MainActivity

class ScannerFragment : Fragment(R.layout.fragment_scanner) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura el menú para regresar
        setupMenu()

        // Protege el fragmento con checkconnect()
        checkconnect {
            // Lógica de cámara irá aquí.
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                // En Scanner sí mostramos Home
                menu.findItem(R.id.action_home)?.isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}