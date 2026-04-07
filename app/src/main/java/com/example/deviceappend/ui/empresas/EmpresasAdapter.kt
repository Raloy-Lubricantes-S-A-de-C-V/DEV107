package com.example.deviceappend.ui.empresas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceappend.R
import com.example.deviceappend.core.network.Empresa

class EmpresasAdapter(
    private var empresasList: List<Empresa>,
    private val onUpdateClick: (Empresa) -> Unit
) : RecyclerView.Adapter<EmpresasAdapter.EmpresaViewHolder>() {

    fun updateData(newList: List<Empresa>) {
        empresasList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpresaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_empresa, parent, false)
        return EmpresaViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmpresaViewHolder, position: Int) {
        val empresa = empresasList[position]

        // Limpiamos los espacios en blanco que deja PostgreSQL en columnas CHAR
        val desc = empresa.descripcio?.trim() ?: "Sin descripción"
        val cve = empresa.cveempresa?.trim() ?: "N/A"
        val rfc = empresa.rfc?.trim() ?: "N/A"

        holder.tvNombre.text = desc
        holder.tvDetalles.text = "RFC: $rfc | CVE: $cve"

        holder.btnActualizar.setOnClickListener {
            onUpdateClick(empresa)
        }
    }

    override fun getItemCount(): Int = empresasList.size

    class EmpresaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreEmpresa)
        val tvDetalles: TextView = view.findViewById(R.id.tvDetallesEmpresa)
        val btnActualizar: Button = view.findViewById(R.id.btnActualizar)
    }
}