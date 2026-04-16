package com.example.deviceappend.ui.tecnicos

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.TextView
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
import com.example.deviceappend.core.network.AsignarEmpresasRequest
import com.example.deviceappend.core.network.Empresa
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.network.TecnicoConEmpresas
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.ui.empresas.EmpresasFragment
import com.example.deviceappend.ui.home.ScannerFragment
import com.example.deviceappend.utils.checkconnect
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TecnicosFragment : Fragment(R.layout.fragment_tecnicos) {

    private lateinit var sessionManager: SessionManager

    private lateinit var actvTecnico: AutoCompleteTextView
    private lateinit var rvEmpresasCheck: RecyclerView
    private lateinit var rvTecnicos: RecyclerView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private var allEmpresas: List<Empresa> = emptyList()
    private var allTecnicos: List<TecnicoConEmpresas> = emptyList()

    private var selectedTecnicoId: Int? = null
    private val selectedEmpresasIds = mutableSetOf<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        // Cortafuegos de sesión
        if (sessionManager.getToken().isNullOrEmpty()) {
            view.visibility = View.GONE
            (activity as? MainActivity)?.logout()
            return
        }

        // CORTAFUEGOS DE ROL: SOLO ADMIN PUEDE ENTRAR A ESTE MÓDULO
        if (!sessionManager.isAdmin()) {
            view.visibility = View.GONE
            Toast.makeText(context, "Acceso denegado: Se requiere rol ADMIN", Toast.LENGTH_LONG).show()
            (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
            return
        }

        setupMenu()
        bindViews(view)

        checkconnect(view) {
            loadData()
            setupClickListeners()
        }
    }

    private fun bindViews(view: View) {
        actvTecnico = view.findViewById(R.id.actvTecnico)
        rvEmpresasCheck = view.findViewById(R.id.rvEmpresasCheck)
        rvTecnicos = view.findViewById(R.id.rvTecnicos)
        btnSubmit = view.findViewById(R.id.btnSubmitAsignacion)
        btnCancel = view.findViewById(R.id.btnCancelAsignacion)

        rvEmpresasCheck.layoutManager = LinearLayoutManager(requireContext())
        rvTecnicos.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.instance

                // Ejecutamos ambas peticiones en paralelo para mayor rapidez
                val empresasReq = async { api.getEmpresas() }
                val tecnicosReq = async { api.getTecnicosConEmpresas() }

                val empresasRes = empresasReq.await()
                val tecnicosRes = tecnicosReq.await()

                if (empresasRes.isSuccessful && empresasRes.body()?.error == false) {
                    allEmpresas = empresasRes.body()!!.data
                    setupEmpresasCheckboxes()
                }

                if (tecnicosRes.isSuccessful && tecnicosRes.body()?.error == false) {
                    allTecnicos = tecnicosRes.body()!!.data
                    setupTecnicosDropdown()
                    setupTecnicosList()
                }

            } catch (e: Exception) {
                Log.e("TecnicosFragment", "Error cargando datos: ${e.message}")
                Toast.makeText(context, "Error de red al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEmpresasCheckboxes() {
        rvEmpresasCheck.adapter = EmpresasCheckAdapter(allEmpresas, selectedEmpresasIds)
    }

    private fun setupTecnicosDropdown() {
        val names = allTecnicos.map { it.nombre?.trim() ?: "Desconocido" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
        actvTecnico.setAdapter(adapter)

        actvTecnico.setOnItemClickListener { _, _, position, _ ->
            val selectedName = adapter.getItem(position)
            val tecnico = allTecnicos.find { it.nombre?.trim() == selectedName }
            if (tecnico != null) {
                selectTecnicoForEdit(tecnico)
            }
        }
    }

    private fun setupTecnicosList() {
        rvTecnicos.adapter = TecnicosAdapter(allTecnicos) { tecnico ->
            selectTecnicoForEdit(tecnico)
        }
    }

    private fun selectTecnicoForEdit(tecnico: TecnicoConEmpresas) {
        selectedTecnicoId = tecnico.idUsuario
        actvTecnico.setText(tecnico.nombre?.trim(), false) // false para no re-abrir el dropdown visualmente

        selectedEmpresasIds.clear()
        tecnico.empresas?.let { selectedEmpresasIds.addAll(it) }

        rvEmpresasCheck.adapter?.notifyDataSetChanged() // Refresca los checkboxes para que se tilden

        btnSubmit.text = "ACTUALIZAR ASIGNACIÓN"
        btnCancel.visibility = View.VISIBLE
    }

    private fun clearForm() {
        selectedTecnicoId = null
        actvTecnico.setText("", false)
        selectedEmpresasIds.clear()
        rvEmpresasCheck.adapter?.notifyDataSetChanged()

        btnSubmit.text = "GUARDAR ASIGNACIÓN"
        btnCancel.visibility = View.GONE
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener { clearForm() }

        btnSubmit.setOnClickListener {
            if (selectedTecnicoId == null) {
                Toast.makeText(context, "Por favor, selecciona un técnico primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = AsignarEmpresasRequest(selectedEmpresasIds.toList())

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val res = RetrofitClient.instance.asignarEmpresasATecnico(selectedTecnicoId!!, request)
                    if (res.isSuccessful && res.body()?.error == false) {
                        Toast.makeText(context, "Empresas actualizadas correctamente", Toast.LENGTH_SHORT).show()
                        clearForm()
                        loadData() // Recarga la lista completa inferior para reflejar los cambios
                    } else {
                        val err = res.errorBody()?.string() ?: res.body()?.msj ?: "Error desconocido"
                        Toast.makeText(context, "Fallo (400): $err", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Fallo de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)

                menu.findItem(R.id.action_notifications)?.isVisible = false
                menu.findItem(R.id.action_modules)?.isVisible = false

                menu.findItem(R.id.action_home)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        requireActivity().invalidateOptionsMenu()
    }

    // ==========================================
    // ADAPTADORES INTERNOS DE LAS LISTAS
    // ==========================================

    inner class EmpresasCheckAdapter(
        private val empresas: List<Empresa>,
        private val selectedIds: MutableSet<Int>
    ) : RecyclerView.Adapter<EmpresasCheckAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.cbEmpresa)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_empresa_check, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val empresa = empresas[position]
            holder.checkBox.text = "${empresa.cveempresa?.trim()} - ${empresa.descripcio?.trim()}"

            // Desenlazar listener previo para no disparar eventos cruzados al reciclar vistas
            holder.checkBox.setOnCheckedChangeListener(null)

            // Tildamos o destildamos dependiendo si el ID de la empresa está en el set de seleccionadas
            holder.checkBox.isChecked = selectedIds.contains(empresa.id)

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIds.add(empresa.id)
                } else {
                    selectedIds.remove(empresa.id)
                }
            }
        }

        override fun getItemCount() = empresas.size
    }

    inner class TecnicosAdapter(
        private val tecnicos: List<TecnicoConEmpresas>,
        private val onUpdateClick: (TecnicoConEmpresas) -> Unit
    ) : RecyclerView.Adapter<TecnicosAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.tvNombreTecnico)
            val tvConteo: TextView = view.findViewById(R.id.tvConteoEmpresas)
            val btnUpdate: MaterialButton = view.findViewById(R.id.btnActualizarTecnico)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tecnico, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tecnico = tecnicos[position]
            holder.tvNombre.text = tecnico.nombre?.trim() ?: "Sin nombre"

            val count = tecnico.empresas?.size ?: 0
            holder.tvConteo.text = "Empresas asignadas: $count"

            holder.btnUpdate.setOnClickListener { onUpdateClick(tecnico) }
        }

        override fun getItemCount() = tecnicos.size
    }
}