package com.example.deviceappend.ui.empresas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.Empresa
import com.example.deviceappend.core.network.EmpresaRequest
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.ui.home.ScannerFragment
import com.example.deviceappend.utils.checkconnect
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EmpresasFragment : Fragment(R.layout.fragment_empresas) {

    private lateinit var sessionManager: SessionManager
    private var editingEmpresaId: Int? = null
    private lateinit var adapter: EmpresasAdapter

    // Vistas
    private lateinit var etCve: TextInputEditText
    private lateinit var etRfc: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var etCalle: TextInputEditText
    private lateinit var etNoExt: TextInputEditText
    private lateinit var etColonia: TextInputEditText
    private lateinit var etCP: TextInputEditText
    private lateinit var etPoblacion: TextInputEditText
    private lateinit var etEntidad: TextInputEditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        if (sessionManager.getToken().isNullOrEmpty()) {
            view.visibility = View.GONE
            (activity as? MainActivity)?.logout()
            return
        }

        if (!sessionManager.isSys()) {
            view.visibility = View.GONE
            Toast.makeText(context, "Acceso denegado: No tienes permisos de sistema", Toast.LENGTH_LONG).show()
            (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
            return
        }

        setupMenu()
        bindViews(view)
        setupRecyclerView(view)

        checkconnect(view) {
            loadEmpresas()
            setupClickListeners()
        }
    }

    private fun bindViews(view: View) {
        etCve = view.findViewById(R.id.etCveEmpresa)
        etRfc = view.findViewById(R.id.etRfc)
        etDesc = view.findViewById(R.id.etDescripcio)
        etCalle = view.findViewById(R.id.etCalle)
        etNoExt = view.findViewById(R.id.etNoExtInt)
        etColonia = view.findViewById(R.id.etColonia)
        etCP = view.findViewById(R.id.etCodPostal)
        etPoblacion = view.findViewById(R.id.etPoblacion)
        etEntidad = view.findViewById(R.id.etCveEntFed)
        btnSubmit = view.findViewById(R.id.btnSubmitEmpresa)
        btnCancel = view.findViewById(R.id.btnCancelEdit)
    }

    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvEmpresas)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = EmpresasAdapter(emptyList()) { empresa ->
            populateForm(empresa)
        }
        rv.adapter = adapter
    }

    private fun loadEmpresas() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.getEmpresas()
                if (res.isSuccessful && res.body()?.error == false) {
                    adapter.updateData(res.body()!!.data)
                } else {
                    val errorMsg = "Error ${res.code()}: ${res.errorBody()?.string() ?: "Desconocido"}"
                    Log.e("Empresas", errorMsg)
                    Toast.makeText(context, "Fallo al listar: Error ${res.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("Empresas", "Excepción de red: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        btnSubmit.setOnClickListener {
            val req = buildRequestFromForm() ?: return@setOnClickListener

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val api = RetrofitClient.instance
                    if (editingEmpresaId == null) {
                        // CREATE
                        val res = api.createEmpresa(req)
                        if (res.isSuccessful && res.body()?.error == false) {
                            Toast.makeText(context, "Empresa Creada Exitosamente", Toast.LENGTH_SHORT).show()
                            clearForm()
                            loadEmpresas()
                        } else {
                            // EXTRAE EL MENSAJE DE ERROR REAL DEL SERVIDOR PARA DEPUBGING
                            val errorMsg = res.errorBody()?.string() ?: res.body()?.id?.toString() ?: "Error desconocido"
                            Log.e("Empresas", "Backend rechazó el POST: $errorMsg")
                            Toast.makeText(context, "Rechazado (400): $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // UPDATE
                        val res = api.updateEmpresa(editingEmpresaId!!, req)
                        if (res.isSuccessful && res.body()?.error == false) {
                            Toast.makeText(context, "Empresa Actualizada", Toast.LENGTH_SHORT).show()
                            clearForm()
                            loadEmpresas()
                        } else {
                            // EXTRAE EL MENSAJE DE ERROR REAL DEL SERVIDOR PARA DEPUBGING
                            val errorMsg = res.errorBody()?.string() ?: res.body()?.msj ?: "Error desconocido"
                            Log.e("Empresas", "Backend rechazó el PUT: $errorMsg")
                            Toast.makeText(context, "Rechazado (400): $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Fallo de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnCancel.setOnClickListener {
            clearForm()
        }
    }

    private fun populateForm(empresa: Empresa) {
        editingEmpresaId = empresa.id

        etCve.setText(empresa.cveempresa?.trim() ?: "")
        etRfc.setText(empresa.rfc?.trim() ?: "")
        etDesc.setText(empresa.descripcio?.trim() ?: "")
        etCalle.setText(empresa.calle?.trim() ?: "")
        etNoExt.setText(empresa.noextint?.trim() ?: "")
        etColonia.setText(empresa.colonia?.trim() ?: "")
        etPoblacion.setText(empresa.poblacion?.trim() ?: "")
        etEntidad.setText(empresa.cveentfed?.trim() ?: "")

        val cp = empresa.codpostal?.toInt()?.toString() ?: ""
        etCP.setText(cp)

        btnSubmit.text = "ACTUALIZAR EMPRESA"
        btnCancel.visibility = View.VISIBLE
    }

    private fun clearForm() {
        editingEmpresaId = null
        etCve.setText("")
        etRfc.setText("")
        etDesc.setText("")
        etCalle.setText("")
        etNoExt.setText("")
        etColonia.setText("")
        etCP.setText("")
        etPoblacion.setText("")
        etEntidad.setText("")

        btnSubmit.text = "CREAR EMPRESA"
        btnCancel.visibility = View.GONE
    }

    private fun buildRequestFromForm(): EmpresaRequest? {
        val cpStr = etCP.text.toString().trim()
        val cpInt = cpStr.toIntOrNull()

        if (cpInt == null) {
            etCP.error = "Código postal inválido"
            return null
        }

        return EmpresaRequest(
            cveempresa = etCve.text.toString().trim(),
            descripcio = etDesc.text.toString().trim(),
            calle = etCalle.text.toString().trim(),
            noextint = etNoExt.text.toString().trim(),
            colonia = etColonia.text.toString().trim(),
            codpostal = cpInt,
            poblacion = etPoblacion.text.toString().trim(),
            cveentfed = etEntidad.text.toString().trim(),
            rfc = etRfc.text.toString().trim()
        )
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)

                menu.findItem(R.id.action_back_to_login)?.isVisible = false
                menu.findItem(R.id.action_empresas)?.isVisible = false

                menu.findItem(R.id.action_home)?.isVisible = true
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
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        requireActivity().invalidateOptionsMenu()
    }
}