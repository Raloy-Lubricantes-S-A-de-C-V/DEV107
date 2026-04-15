package com.example.deviceappend.ui.prospectos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.Notificacion
import com.example.deviceappend.core.network.RetrofitClient
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvNotificaciones)
        rv.layoutManager = LinearLayoutManager(context)

        setupMenu()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.instance.getProspectos()
                if (res.isSuccessful) {
                    val notifs = res.body()?.data?.filter { it.view == 0 && it.open == 1 }?.map {
                        Notificacion(it.id, "Prospectos", "Nueva solicitud de técnico de: ${it.name}", false, it.id)
                    } ?: emptyList()

                    rv.adapter = NotificationsAdapter(notifs) {
                        (activity as? MainActivity)?.replaceFragment(ProspectosFragment(), true)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_home)?.isVisible = true
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    inner class NotificationsAdapter(
        private val list: List<Notificacion>,
        private val onClick: (Notificacion) -> Unit
    ) : RecyclerView.Adapter<NotificationsAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvModulo = v.findViewById<TextView>(R.id.tvModulo)
            val tvDesc = v.findViewById<TextView>(R.id.tvDescripcion)
            val ivDot = v.findViewById<ImageView>(R.id.ivUnreadDot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val n = list[position]
            holder.tvModulo.text = "Módulo: ${n.modulo}"
            holder.tvDesc.text = n.descripcion
            holder.ivDot.visibility = if (n.isRead) View.INVISIBLE else View.VISIBLE
            holder.itemView.setOnClickListener { onClick(n) }
        }

        override fun getItemCount() = list.size
    }
}