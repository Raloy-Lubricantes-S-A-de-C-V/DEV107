package com.example.deviceappend.ui.wizard

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R

// ========================================================
// IMPORTACIONES EXPLÍCITAS PARA EVITAR EL ERROR DE CACHÉ
// ========================================================
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.network.SaveEnrollmentRequest
import com.example.deviceappend.core.network.EmployeeData
import com.example.deviceappend.core.network.AiPhotoRequest
import com.example.deviceappend.core.network.CandidateSearchRequest
import com.example.deviceappend.core.network.FaceMatchRequest

import com.example.deviceappend.utils.checkconnect
import com.example.deviceappend.utils.hideLoader
import com.example.deviceappend.utils.showLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class HelpDeskFragment : Fragment(R.layout.fragment_helpdesk) {

    private lateinit var rgMetodo: RadioGroup
    private lateinit var llNomina: LinearLayout
    private lateinit var llFoto: LinearLayout
    private lateinit var llEventual: LinearLayout
    private lateinit var cvResultado: MaterialCardView

    private lateinit var etCorreo: EditText
    private lateinit var ivFotoPreview: ImageView
    private lateinit var btnProcesarIA: MaterialButton
    private lateinit var tvResGenerales: TextView
    private lateinit var ivResFoto: ImageView

    private var currentBase64Photo: String = ""
    private var currentFingerprint: String = ""
    private var currentNomina: String = ""
    private var employeeDataCache: EmployeeData? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                ivFotoPreview.setImageBitmap(it)
                ivFotoPreview.visibility = View.VISIBLE
                btnProcesarIA.visibility = View.VISIBLE
                currentBase64Photo = bitmapToBase64(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        bindViews(view)

        checkconnect(view, "Cargando Módulo HelpDesk...") {
            setupListeners(view)
        }
    }

    private fun bindViews(view: View) {
        rgMetodo = view.findViewById(R.id.rgMetodo)
        llNomina = view.findViewById(R.id.llContainerNomina)
        llFoto = view.findViewById(R.id.llContainerFoto)
        llEventual = view.findViewById(R.id.llContainerEventual)
        cvResultado = view.findViewById(R.id.cvResultado)

        etCorreo = view.findViewById(R.id.etCorreoInstitucional)
        ivFotoPreview = view.findViewById(R.id.ivFotoPreview)
        btnProcesarIA = view.findViewById(R.id.btnProcesarIA)
        tvResGenerales = view.findViewById(R.id.tvResGenerales)
        ivResFoto = view.findViewById(R.id.ivResFoto)
    }

    private fun setupListeners(view: View) {
        rgMetodo.setOnCheckedChangeListener { _, checkedId ->
            llNomina.visibility = View.GONE
            llFoto.visibility = View.GONE
            llEventual.visibility = View.GONE
            cvResultado.visibility = View.GONE

            when (checkedId) {
                R.id.rbNomina -> llNomina.visibility = View.VISIBLE
                R.id.rbFoto -> llFoto.visibility = View.VISIBLE
                R.id.rbEventual -> llEventual.visibility = View.VISIBLE
            }
        }

        view.findViewById<MaterialButton>(R.id.btnTomarFoto).setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(cameraIntent)
        }

        view.findViewById<MaterialButton>(R.id.btnBuscarNomina).setOnClickListener {
            val nomina = view.findViewById<EditText>(R.id.etNominaBusqueda).text.toString()
            if (nomina.isNotEmpty()) fetchEmployeeData(nomina)
        }

        btnProcesarIA.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("Paso 1/3: Analizando con Gemini Vision...")
                try {
                    val api = RetrofitClient.instance

                    val aiRes = api.analyzePhotoAi(AiPhotoRequest(currentBase64Photo))
                    if (!aiRes.isSuccessful) throw Exception("Fallo IA")
                    val aiData = aiRes.body()!!

                    showLoader("Paso 2/3: Buscando candidatos en DelSIP...")
                    val candRes = api.searchCandidates(CandidateSearchRequest(aiData.sexo, aiData.edad_minima, aiData.edad_maxima))
                    if (!candRes.isSuccessful) throw Exception("Fallo Búsqueda")
                    val candidatos = candRes.body()!!.data

                    showLoader("Paso 3/3: Comparación Biométrica de Rostros...")
                    val matchRes = api.matchFaces(FaceMatchRequest(currentBase64Photo, candidatos))
                    if (!matchRes.isSuccessful) throw Exception("Fallo Biometría")
                    val matchData = matchRes.body()!!

                    currentFingerprint = matchData.fingerprint ?: ""
                    currentNomina = matchData.match_nomina ?: ""

                    fetchEmployeeData(currentNomina)

                } catch (e: Exception) {
                    Toast.makeText(context, "Error en procesamiento IA: ${e.message}", Toast.LENGTH_LONG).show()
                    hideLoader()
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btnGuardarAuditoria).setOnClickListener {
            val correo = etCorreo.text.toString()
            if (correo.isEmpty()) {
                Toast.makeText(context, "El correo institucional es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("Registrando Auditoría...")
                try {
                    val req = SaveEnrollmentRequest(
                        modulo = "HelpDesk",
                        correo = correo,
                        tipo_registro = getSelectedMethod(),
                        nomina = currentNomina,
                        datos_extra = mapOf("nombre" to (employeeDataCache?.nombre ?: "Eventual")),
                        fotografia_base64 = currentBase64Photo,
                        huella_digital = currentFingerprint
                    )

                    val res = RetrofitClient.instance.saveEnrollment(req)
                    if (res.isSuccessful) {
                        Toast.makeText(context, "Enrolamiento guardado. Esperando instrucciones de bloque 2...", Toast.LENGTH_LONG).show()
                        cvResultado.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("HelpDesk", e.message.toString())
                } finally {
                    hideLoader()
                }
            }
        }
    }

    private fun fetchEmployeeData(nomina: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoader("Obteniendo Generales...")
            try {
                val res = RetrofitClient.instance.getEmployeeByNomina(nomina)
                if (res.isSuccessful && res.body()?.data != null) {
                    employeeDataCache = res.body()?.data
                    currentNomina = nomina
                    displayResults(employeeDataCache!!)
                } else {
                    Toast.makeText(context, "Nómina no encontrada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {} finally { hideLoader() }
        }
    }

    private fun displayResults(emp: EmployeeData) {
        cvResultado.visibility = View.VISIBLE
        tvResGenerales.text = "Nómina: ${emp.nomina}\nNombre: ${emp.nombre} ${emp.paterno}\nCURP: ${emp.curp}\nEdad: ${emp.edad}"

        if (!emp.foto_base64.isNullOrEmpty()) {
            try {
                val cleanBase64 = if (emp.foto_base64.contains(",")) emp.foto_base64.substringAfter(",") else emp.foto_base64
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ivResFoto.setImageBitmap(bmp)
                currentBase64Photo = cleanBase64
            } catch (e: Exception) {}
        }
    }

    private fun getSelectedMethod(): String {
        return when (rgMetodo.checkedRadioButtonId) {
            R.id.rbNomina -> "NOMINA"
            R.id.rbFoto -> "FOTO"
            R.id.rbEventual -> "EVENTUAL"
            else -> "UNKNOWN"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos)
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_home)?.isVisible = true
                menu.findItem(R.id.action_modules)?.isVisible = false
                menu.findItem(R.id.action_logout)?.isVisible = false
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}