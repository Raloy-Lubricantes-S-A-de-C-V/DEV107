package com.example.deviceappend.ui.wizard

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R

import com.example.deviceappend.core.network.*
import com.example.deviceappend.core.session.SessionManager

import com.example.deviceappend.utils.checkconnect
import com.example.deviceappend.utils.hideLoader
import com.example.deviceappend.utils.showLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class HelpDeskFragment : Fragment(R.layout.fragment_helpdesk) {

    // =========================================================================
    // ⚠️ MANTÉN AQUÍ TU VARIABLE GIGANTE GENERADA POR POWERSHELL ⚠️
    private val MOCK_BASE64_PHOTO = "TU_BASE64_GIGANTE_VA_AQUI"
    // =========================================================================

    private lateinit var sessionManager: SessionManager
    private lateinit var rgMetodo: RadioGroup
    private lateinit var llNomina: LinearLayout
    private lateinit var llFoto: LinearLayout
    private lateinit var llEventual: LinearLayout
    private lateinit var cvResultado: MaterialCardView

    private lateinit var etCorreo: EditText
    private lateinit var ivFotoPreview: ImageView
    private lateinit var btnProcesarIA: MaterialButton
    private lateinit var btnTestMockPhoto: MaterialButton
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
                updatePhotoPreview(it)
                currentBase64Photo = bitmapToBase64(it)
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara para esta función", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupMenu()
        bindViews(view)

        // Mostrar botón de prueba solo a usuarios SYS
        if (sessionManager.isSys()) {
            btnTestMockPhoto.visibility = View.VISIBLE
        } else {
            btnTestMockPhoto.visibility = View.GONE
        }

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
        btnTestMockPhoto = view.findViewById(R.id.btnTestMockPhoto)
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

        view.findViewById<MaterialButton>(R.id.btnAbrirCamara).setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        btnTestMockPhoto.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("[TEST] Cargando fotografía mock...")
                try {
                    val safeBase64 = MOCK_BASE64_PHOTO.replace("\\s+".toRegex(), "")
                    currentBase64Photo = safeBase64

                    val imageBytes = Base64.decode(safeBase64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    if (decodedImage != null) {
                        updatePhotoPreview(decodedImage)
                        Toast.makeText(context, "✅ Foto Mock cargada correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "❌ Error al generar imagen Bitmap", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("HelpDeskTest", "Error cargando mock: ${e.message}")
                    Toast.makeText(context, "Fallo Base64: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    hideLoader()
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btnBuscarNomina).setOnClickListener {
            val nomina = view.findViewById<EditText>(R.id.etNominaBusqueda).text.toString()
            if (nomina.isNotEmpty()) fetchEmployeeData(nomina)
        }

        // ==========================================
        // PROCESADOR INTELIGENTE (JSON GSON CORRECTO)
        // ==========================================
        btnProcesarIA.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("Paso 1/3: Analizando con Gemini Vision...")
                try {
                    val api = RetrofitClient.instance

                    val aiRes = api.analyzePhotoAi(AiPhotoRequest(currentBase64Photo))
                    if (!aiRes.isSuccessful) throw Exception("Fallo IA (HTTP ${aiRes.code()})")

                    val aiData = aiRes.body() ?: throw Exception("N8N devolvió cuerpo vacío")

                    // 1. "Escarbamos" la estructura anidada usando los métodos seguros de JsonObject
                    var textJson = ""
                    try {
                        if (aiData.has("candidates")) {
                            textJson = aiData.getAsJsonArray("candidates")
                                .get(0).asJsonObject
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).asJsonObject
                                .get("text").asString
                        } else if (aiData.has("text")) {
                            textJson = aiData.get("text").asString
                        } else {
                            throw Exception("La respuesta no contiene 'candidates' ni 'text'")
                        }
                    } catch (e: Exception) {
                        throw Exception("No se pudo leer la estructura del JSON: ${e.message}")
                    }

                    // 2. Limpiamos la posible notación Markdown que añade la IA
                    val cleanJson = textJson.replace("```json", "").replace("```", "").trim()

                    // 3. Extraemos las variables finales del texto limpio
                    val jsonObj = org.json.JSONObject(cleanJson)
                    val sexoDetectado = jsonObj.getString("sexo")
                    val edadMin = jsonObj.getInt("edad_minima")
                    val edadMax = jsonObj.getInt("edad_maxima")

                    showLoader("Paso 2/3: Buscando candidatos en DelSIP...")
                    val candRes = api.searchCandidates(CandidateSearchRequest(sexoDetectado, edadMin, edadMax))
                    if (!candRes.isSuccessful) throw Exception("Fallo Búsqueda SQL")
                    val candidatos = candRes.body()!!.data

                    showLoader("Paso 3/3: Comparación Biométrica de Rostros...")
                    val matchRes = api.matchFaces(FaceMatchRequest(currentBase64Photo, candidatos))
                    if (!matchRes.isSuccessful) throw Exception("Fallo Biometría")
                    val matchData = matchRes.body()!!

                    currentFingerprint = matchData.fingerprint ?: ""
                    currentNomina = matchData.match_nomina ?: ""

                    fetchEmployeeData(currentNomina)

                } catch (e: Exception) {
                    Log.e("HelpDeskIA", "Error procesando IA", e)
                    Toast.makeText(context, "Error en procesamiento IA: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
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
                        Toast.makeText(context, "Enrolamiento guardado. Esperando instrucciones...", Toast.LENGTH_LONG).show()
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

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(cameraIntent)
    }

    private fun updatePhotoPreview(bitmap: Bitmap) {
        ivFotoPreview.setImageBitmap(bitmap)
        ivFotoPreview.visibility = View.VISIBLE
        btnProcesarIA.visibility = View.VISIBLE
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
                val safeBase64 = cleanBase64.replace("\\s+".toRegex(), "")
                val bytes = Base64.decode(safeBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ivResFoto.setImageBitmap(bmp)
                currentBase64Photo = safeBase64
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
}