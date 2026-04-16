package com.example.deviceappend.ui.prospectos

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.*
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.utils.checkconnect
import com.example.deviceappend.utils.hideLoader
import com.example.deviceappend.utils.showLoader
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ProspectosFragment : Fragment(R.layout.fragment_prospectos) {

    private lateinit var sessionManager: SessionManager
    private lateinit var rvProspectos: RecyclerView
    private lateinit var rvEmpresasCheck: RecyclerView
    private lateinit var actvLider: AutoCompleteTextView

    private lateinit var etName: EditText
    private lateinit var etMail: EditText
    private lateinit var cbSys: CheckBox
    private lateinit var cbAdmin: CheckBox
    private lateinit var cbNormal: CheckBox
    private lateinit var cbLider: CheckBox
    private lateinit var btnAprobar: MaterialButton
    private lateinit var btnDeclinar: MaterialButton

    private var allProspectos: List<Prospecto> = emptyList()
    private var allEmpresas: List<Empresa> = emptyList()
    private var allLideres: List<UserListItem> = emptyList()

    private var prospectoId: Int? = null
    private var selectedLiderId: Int = 0
    private val selectedEmpresasIds = mutableSetOf<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupMenu()
        bindViews(view)

        // 1. CORTAFUEGOS Y LOADERS AL ENTRAR AL MÓDULO
        checkconnect(view, "Cargando prospectos...") {
            loadData()
            setupClickListeners()
        }
    }

    private fun bindViews(view: View) {
        rvProspectos = view.findViewById(R.id.rvProspectos)
        rvEmpresasCheck = view.findViewById(R.id.rvEmpresasCheck)
        actvLider = view.findViewById(R.id.actvLider)
        etName = view.findViewById(R.id.etName)
        etMail = view.findViewById(R.id.etMail)
        cbSys = view.findViewById(R.id.cbSys)
        cbAdmin = view.findViewById(R.id.cbAdmin)
        cbNormal = view.findViewById(R.id.cbNormal)
        cbLider = view.findViewById(R.id.cbLider)
        btnAprobar = view.findViewById(R.id.btnAprobar)
        btnDeclinar = view.findViewById(R.id.btnDeclinar)

        rvProspectos.layoutManager = LinearLayoutManager(context)
        rvEmpresasCheck.layoutManager = LinearLayoutManager(context)
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoader("Sincronizando...")
            try {
                val api = RetrofitClient.instance
                val reqP = async { api.getProspectos() }
                val reqE = async { api.getEmpresas() }
                val reqU = async { api.listUsers() }

                val resP = reqP.await()
                if (resP.isSuccessful) allProspectos = resP.body()?.data ?: emptyList()

                val resE = reqE.await()
                if (resE.isSuccessful) allEmpresas = resE.body()?.data ?: emptyList()

                val resU = reqU.await()
                if (resU.isSuccessful) {
                    allLideres = resU.body()?.data?.filter { it.lider == 1 } ?: emptyList()
                }

                setupDropdown()
                rvEmpresasCheck.adapter = EmpresasCheckAdapter(allEmpresas, selectedEmpresasIds)
                rvProspectos.adapter = ProspectosAdapter(allProspectos) { prospecto ->
                    selectProspecto(prospecto)
                }
            } catch (e: Exception) {
                Log.e("Prospectos", e.message.toString())
            } finally {
                hideLoader()
            }
        }
    }

    private fun setupDropdown() {
        val names = allLideres.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
        actvLider.setAdapter(adapter)
        actvLider.setOnItemClickListener { _, _, position, _ ->
            selectedLiderId = allLideres.find { it.name == adapter.getItem(position) }?.id ?: 0
        }
    }

    private fun selectProspecto(p: Prospecto) {
        if (p.open != 1) {
            Toast.makeText(context, "Solo se pueden modificar prospectos abiertos", Toast.LENGTH_SHORT).show()
            return
        }

        prospectoId = p.id

        if (p.view == 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                RetrofitClient.instance.marcarProspectoVisto(p.id)
                loadData()
            }
        }

        etName.setText(p.name)
        etMail.setText(p.mail)
        selectedEmpresasIds.clear()
        rvEmpresasCheck.adapter?.notifyDataSetChanged()

        val l = allLideres.find { it.id == p.parent_id }
        if (l != null) {
            actvLider.setText(l.name, false)
            selectedLiderId = l.id
        }

        if (!sessionManager.isSys()) {
            cbSys.visibility = View.GONE
            cbLider.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnAprobar.setOnClickListener {
            if (prospectoId == null) {
                Toast.makeText(context, "Seleccione un prospecto OPEN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = AprobarProspectoRequest(
                name = etName.text.toString(),
                mail = etMail.text.toString(),
                parent_id = selectedLiderId,
                sys = if (cbSys.isChecked) 1 else 0,
                admin = if (cbAdmin.isChecked) 1 else 0,
                normal = if (cbNormal.isChecked) 1 else 0,
                lider = if (cbLider.isChecked) 1 else 0,
                empresas = selectedEmpresasIds.toList()
            )

            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("Creando rol y enviando correo...")
                try {
                    val res = RetrofitClient.instance.aprobarProspecto(prospectoId!!, req)
                    if (res.isSuccessful) {
                        val p = allProspectos.find { it.id == prospectoId }
                        val msj = "Hola ${p?.name},\n\n" +
                                "Tu solicitud para ingresar al sistema Raloy Asset Manager ha sido aprobada.\n\n" +
                                "Usuario: ${p?.mail}\n" +
                                "Contraseña Temporal: ${p?.code}\n\n" +
                                "Ingresa a la app. Se te solicitará crear tu nueva contraseña."
                        try {
                            RetrofitClient.instance.sendNewTechnicianWebhook(NewTechnicianWebhookRequest(p?.mail ?: "", msj, "Acceso a Raloy Asset Manager"))
                        } catch (e: Exception) {}

                        Toast.makeText(context, "Prospecto Aprobado", Toast.LENGTH_LONG).show()
                        clearForm()
                        loadData()
                    } else {
                        Toast.makeText(context, "Error en servidor", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    hideLoader()
                }
            }
        }

        btnDeclinar.setOnClickListener {
            if (prospectoId == null) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("Declinando prospecto...")
                try {
                    val res = RetrofitClient.instance.declinarProspecto(prospectoId!!)
                    if (res.isSuccessful) {
                        Toast.makeText(context, "Prospecto Declinado", Toast.LENGTH_SHORT).show()
                        clearForm()
                        loadData()
                    }
                } catch (e: Exception) {}
                finally { hideLoader() }
            }
        }
    }

    private fun clearForm() {
        prospectoId = null
        etName.setText("")
        etMail.setText("")
        actvLider.setText("", false)
        selectedEmpresasIds.clear()
        rvEmpresasCheck.adapter?.notifyDataSetChanged()
        cbSys.isChecked = false
        cbAdmin.isChecked = false
        cbLider.isChecked = false
        cbNormal.isChecked = true
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_home)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true
                menu.findItem(R.id.action_notifications)?.isVisible = false
                menu.findItem(R.id.action_modules)?.isVisible = false
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    inner class ProspectosAdapter(
        private val list: List<Prospecto>,
        private val onClick: (Prospecto) -> Unit
    ) : RecyclerView.Adapter<ProspectosAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombre)
            val tvMail: TextView = v.findViewById(R.id.tvMail)
            val tvFecha: TextView = v.findViewById(R.id.tvFecha)
            val tvEstado: TextView = v.findViewById(R.id.tvEstado)
            val viewInd: View = v.findViewById(R.id.viewIndicador)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_prospecto, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = list[position]
            holder.tvNombre.text = p.name
            holder.tvMail.text = p.mail
            holder.tvFecha.text = "Fecha de Solicitud: ${p.create_day ?: "N/A"}"

            var estado = "DESCONOCIDO"
            var color = Color.GRAY

            if (p.acepted == 1) {
                estado = "ACEPTADO"
                color = Color.parseColor("#4CAF50") // Verde
            } else if (p.declined == 1) {
                estado = "DECLINADO"
                color = Color.parseColor("#F44336") // Rojo
            } else if (p.open == 1) {
                if (p.view == 1) {
                    estado = "VISTO (OPEN)"
                    color = Color.parseColor("#2196F3") // Azul
                } else {
                    estado = "NUEVO (OPEN)"
                    color = Color.parseColor("#FFC107") // Amarillo
                }
            }

            holder.tvEstado.text = "ESTADO: $estado"
            holder.tvEstado.setTextColor(color)
            holder.viewInd.setBackgroundColor(color)

            holder.itemView.setOnClickListener { onClick(p) }
        }
        override fun getItemCount() = list.size
    }

    inner class EmpresasCheckAdapter(
        private val empresas: List<Empresa>,
        private val selectedIds: MutableSet<Int>
    ) : RecyclerView.Adapter<EmpresasCheckAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.cbEmpresa)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_empresa_check, parent, false))
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val empresa = empresas[position]
            holder.checkBox.text = "${empresa.cveempresa?.trim()} - ${empresa.descripcio?.trim()}"
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = selectedIds.contains(empresa.id)
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIds.add(empresa.id) else selectedIds.remove(empresa.id)
            }
        }
        override fun getItemCount() = empresas.size
    }
}