package com.example.deviceappend.ui.home

import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.example.deviceappend.ui.delsip.DelsipTestFragment
import com.example.deviceappend.ui.empresas.EmpresasFragment
import com.example.deviceappend.ui.prospectos.NotificationsFragment
import com.example.deviceappend.ui.prospectos.ProspectosFragment
import com.example.deviceappend.ui.tecnicos.TecnicosFragment
import com.example.deviceappend.ui.wizard.WizardFragment
import com.example.deviceappend.utils.checkconnect
import kotlinx.coroutines.delay
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

                menu.findItem(R.id.action_logout)?.isVisible = false
                menu.findItem(R.id.action_home)?.isVisible = false

                val notifItem = menu.findItem(R.id.action_notifications)
                notifItem?.isVisible = sessionManager.isAdmin()

                if (sessionManager.isAdmin()) {
                    fetchNotificationsCount(notifItem)
                }

                menu.findItem(R.id.action_modules)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = true

                menu.findItem(R.id.action_empresas)?.isVisible = sessionManager.isSys()
                menu.findItem(R.id.action_tecnicos)?.isVisible = sessionManager.isAdmin()
                menu.findItem(R.id.action_prospectos)?.isVisible = sessionManager.isAdmin()
                // MOSTRAR LA NUEVA OPCIÓN DELSIP
                menu.findItem(R.id.action_delsip_test)?.isVisible = sessionManager.isAdmin() || sessionManager.isSys()
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
                    R.id.action_delsip_test -> {
                        // NAVEGACIÓN AL NUEVO MÓDULO
                        (activity as? MainActivity)?.replaceFragment(DelsipTestFragment(), true)
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

                        val iconDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_popup_reminder)?.mutate()
                        iconDrawable?.setTint(Color.parseColor("#FFC107"))
                        item?.icon = iconDrawable

                        launch {
                            delay(400)
                            val actionView = requireActivity().findViewById<View>(R.id.action_notifications)
                            if (actionView != null) {
                                val animator = ObjectAnimator.ofFloat(actionView, "rotation", 0f, 25f, -25f, 25f, -25f, 0f)
                                animator.duration = 800
                                animator.repeatCount = 2
                                animator.start()
                            }
                        }

                        sendSystemNotification(count)

                    } else {
                        item?.title = "Notificaciones"
                        val iconDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_popup_reminder)?.mutate()
                        iconDrawable?.setTint(Color.WHITE)
                        item?.icon = iconDrawable
                    }
                }
            } catch(e: Exception) {}
        }
    }

    private fun sendSystemNotification(count: Int) {
        val context = requireContext()
        val channelId = "prospectos_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requireActivity().requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Prospectos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre nuevas solicitudes de técnicos"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_user)
            .setContentTitle("Nueva Solicitud de Técnico")
            .setContentText("Tienes $count solicitud(es) de prospectos pendientes de revisión.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
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

        binding.btnScannerColaborador.setOnClickListener {
            Toast.makeText(context, "Módulo de Escaneo de Colaborador en construcción", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}