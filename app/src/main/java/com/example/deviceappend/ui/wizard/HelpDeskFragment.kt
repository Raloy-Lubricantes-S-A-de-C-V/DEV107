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
import java.security.MessageDigest

class HelpDeskFragment : Fragment(R.layout.fragment_helpdesk) {

    // =========================================================================
    // ⚠️ AQUÍ SE INYECTARÁ LA VARIABLE MEDIANTE POWERSHELL ⚠️
    private val MOCK_BASE64_PHOTO = "TU_BASE64_GIGANTE_VA_AQUI"
    // =========================================================================

    private lateinit var sessionManager: SessionManager
    private lateinit var rgMetodo: RadioGroup
    private lateinit var llNomina: LinearLayout
    private lateinit var llCamara: LinearLayout
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

        llNomina = view.findViewById(R.id.llNomina)
        llCamara = view.findViewById(R.id.llCamara)
        llEventual = view.findViewById(R.id.llEventual)
        cvResultado = view.findViewById(R.id.cvResultado)

        etCorreo = view.findViewById(R.id.etCorreo)
        ivFotoPreview = view.findViewById(R.id.ivFotoPreview)
        btnProcesarIA = view.findViewById(R.id.btnProcesarIA)
        btnTestMockPhoto = view.findViewById(R.id.btnTestMockPhoto)
        tvResGenerales = view.findViewById(R.id.tvResGenerales)
        ivResFoto = view.findViewById(R.id.ivResFoto)
    }

    private fun setupListeners(view: View) {
        rgMetodo.setOnCheckedChangeListener { _, checkedId ->
            llNomina.visibility = View.GONE
            llCamara.visibility = View.GONE
            llEventual.visibility = View.GONE
            cvResultado.visibility = View.GONE

            when (checkedId) {
                R.id.rbNomina -> llNomina.visibility = View.VISIBLE
                R.id.rbFoto -> llCamara.visibility = View.VISIBLE
                R.id.rbEventual -> llEventual.visibility = View.VISIBLE
            }
        }

        view.findViewById<MaterialButton>(R.id.btnAbrirCamara).setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        // CLICK NORMAL: Cargar Mock Photo
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

        // ==========================================
        // CLICK MANTENIDO (LONG CLICK): INICIA EL PROCESO MASIVO
        // ==========================================
        btnTestMockPhoto.setOnLongClickListener {
            startMassiveHashGeneration()
            true // Retornamos true para indicar que el long click fue consumido
        }

        view.findViewById<MaterialButton>(R.id.btnBuscarNomina).setOnClickListener {
            val nomina = view.findViewById<EditText>(R.id.etNomina).text.toString()
            if (nomina.isNotEmpty()) fetchEmployeeData(nomina)
        }

        // ==========================================
        // PROCESADOR DE 1 FOTO (EVALUACIÓN 1 x 1 con N8N)
        // ==========================================
        btnProcesarIA.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                showLoader("Paso 1/3: Analizando con Gemini Vision...")
                try {
                    val api = RetrofitClient.instance

                    // 1. Mandamos la foto para sacar Rango de Edad y Sexo
                    val aiRes = api.analyzePhotoAi(AiPhotoRequest(currentBase64Photo))
                    if (!aiRes.isSuccessful) throw Exception("Fallo IA (HTTP ${aiRes.code()})")

                    val aiData = aiRes.body() ?: throw Exception("N8N devolvió cuerpo vacío")

                    var textJson = ""
                    try {
                        if (aiData.has("content")) {
                            textJson = aiData.getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).asJsonObject
                                .get("text").asString
                        } else if (aiData.has("candidates")) {
                            textJson = aiData.getAsJsonArray("candidates")
                                .get(0).asJsonObject
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).asJsonObject
                                .get("text").asString
                        } else if (aiData.has("text")) {
                            textJson = aiData.get("text").asString
                        } else {
                            throw Exception("La respuesta no contiene 'content', 'candidates' ni 'text'")
                        }
                    } catch (e: Exception) {
                        throw Exception("No se pudo leer la estructura del JSON: ${e.message}")
                    }

                    val cleanJson = textJson.replace("```json", "").replace("```", "").trim()
                    val jsonObj = org.json.JSONObject(cleanJson)
                    val sexoDetectado = jsonObj.getString("sexo")
                    val edadMin = jsonObj.getInt("edad_minima")
                    val edadMax = jsonObj.getInt("edad_maxima")

                    showLoader("Paso 2/3: Buscando $sexoDetectado de $edadMin a $edadMax años en DelSIP...")

                    // 2. Buscamos el lote de candidatos que cumplan esas características
                    val candRes = api.searchCandidates(CandidateSearchRequest(sexoDetectado, edadMin, edadMax))
                    if (!candRes.isSuccessful) throw Exception("Fallo Búsqueda SQL")
                    val candidatos = candRes.body()!!.data

                    if (candidatos.isEmpty()) {
                        throw Exception("No se encontraron candidatos con el perfil de $sexoDetectado entre $edadMin y $edadMax años.")
                    }

                    var matchFound = false

                    // 3. CICLO DE COMPARACIÓN BIOMÉTRICA (1 x 1)
                    for ((index, candidato) in candidatos.withIndex()) {
                        showLoader("Paso 3/3: Comparando rostro ${index + 1}/${candidatos.size} con IA...")

                        val fotoCandidatoRaw = candidato["foto"] ?: candidato["foto_base64"] ?: ""
                        val nominaCandidato = candidato["nomina"] ?: ""

                        if (fotoCandidatoRaw.isEmpty()) continue

                        val safeFotoCandidato = fotoCandidatoRaw.replace("\\s+".toRegex(), "").let {
                            if (it.contains(",")) it.substringAfter(",") else it
                        }

                        val req = CompareFacesRequest(
                            fotoCapturada = currentBase64Photo,
                            fotoCandidato = safeFotoCandidato,
                            nominaCandidato = nominaCandidato
                        )

                        val matchRes = api.compareFacesAi(req)

                        if (matchRes.isSuccessful) {
                            val matchData = matchRes.body()!!
                            if (matchData.match) {
                                matchFound = true
                                currentNomina = matchData.nomina ?: nominaCandidato
                                currentFingerprint = generateFingerprint(currentBase64Photo)
                                break
                            }
                        } else {
                            Log.w("HelpDesk", "Fallo al consultar a Gemini sobre nómina $nominaCandidato")
                        }
                    }

                    if (!matchFound) {
                        throw Exception("Ningún candidato de la base de datos es la persona de la fotografía.")
                    }

                    // 5. Jalamos los Generales a la pantalla si encontramos un Match
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
                        huella_digital = currentFingerprint // Huella Algorítmica SHA-256
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

    // ==========================================
    // FUNCIÓN MAESTRA: ENROLAMIENTO MASIVO (LOS 1014 EMPLEADOS)
    // Extrae imagen, Genera Huella con Gemini, Guarda en Postgres
    // ==========================================
    private fun startMassiveHashGeneration() {
        viewLifecycleOwner.lifecycleScope.launch {

            showLoader("Iniciando Enrolamiento Masivo (BD)...")
            try {
                val api = RetrofitClient.instance

                // 1. Obtener la lista de empleados activos (Python ya filtró a los que ya tienen huella)
                val empRes = api.getActiveEmployees()
                if (!empRes.isSuccessful) throw Exception("Fallo HTTP al obtener empleados activos: ${empRes.code()}")

                val empleados = empRes.body()?.data ?: emptyList()
                if (empleados.isEmpty()) {
                    Toast.makeText(context, "¡Todo al día! No hay empleados nuevos por enrolar.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                var successCount = 0

                // 2. Iterar cada empleado
                for ((index, emp) in empleados.withIndex()) {
                    showLoader(">>> Procesando Pendiente [${index + 1}/${empleados.size}]\nNómina: ${emp.nomina}")
                    Log.i("FACEID_DEBUG", ">>> FACEID_DEBUG: [${index + 1}/${empleados.size}] Procesando nómina: ${emp.nomina}")

                    var base64Photo = emp.foto_base64 ?: ""

                    // Fallback: Si no venía la foto, la pedimos directamente
                    if (base64Photo.isEmpty() || base64Photo.length < 100) {
                        try {
                            val imgRes = api.testDelsipImage(emp.nomina)
                            if (imgRes.isSuccessful) {
                                base64Photo = imgRes.body()?.data?.imageBase64 ?: ""
                            }
                        } catch (e: Exception) { Log.e("FACEID_DEBUG", "Sin foto en Delsip: ${emp.nomina}") }
                    }

                    if (base64Photo.isNotEmpty() && base64Photo.length > 100) {
                        val cleanBase64 = base64Photo.replace("\\s+".toRegex(), "").let {
                            if (it.contains(",")) it.substringAfter(",") else it
                        }

                        try {
                            // 3. Enviar la foto al webhook de Gemini
                            val aiRes = api.generateFaceIdHashAi(AiPhotoRequest(cleanBase64))
                            if (aiRes.isSuccessful) {
                                val aiData = aiRes.body() ?: throw Exception("Body vacío N8N en nómina ${emp.nomina}")

                                var hashGenerado = ""
                                if (aiData.has("fingerprint")) {
                                    hashGenerado = aiData.get("fingerprint").asString
                                } else if (aiData.has("content")) {
                                    val text = aiData.getAsJsonObject("content").getAsJsonArray("parts").get(0).asJsonObject.get("text").asString
                                    val cleanText = text.replace("```json", "").replace("```", "").trim()
                                    hashGenerado = org.json.JSONObject(cleanText).optString("fingerprint", "")
                                } else if (aiData.has("text")) {
                                    val text = aiData.get("text").asString
                                    val cleanText = text.replace("```json", "").replace("```", "").trim()
                                    hashGenerado = org.json.JSONObject(cleanText).optString("fingerprint", "")
                                }

                                // 4. GUARDAR EN POSTGRESQL MEDIANTE PYTHON
                                if (hashGenerado.isNotEmpty() && hashGenerado.lowercase() != "false") {
                                    val saveReq = SaveHashRequest(
                                        nomina = emp.nomina,
                                        edad = emp.edad,
                                        biometric_hash = hashGenerado
                                    )
                                    val saveRes = api.saveBiometricHash(saveReq)

                                    if (saveRes.isSuccessful) {
                                        successCount++
                                        Log.i("FACEID_DEBUG", "✅ Huella guardada en Postgres para nómina: ${emp.nomina}")
                                    } else {
                                        // ¡AQUÍ LO TRONAMOS SI LA BD RECHAZA EL GUARDADO!
                                        throw Exception("La base de datos rechazó guardar la nómina ${emp.nomina}. Proceso Abortado.")
                                    }
                                } else {
                                    Log.w("FACEID_DEBUG", "⚠️ N8N/Gemini no devolvió un hash válido para nómina: ${emp.nomina}")
                                }
                            } else {
                                // Lo tronamos si N8N falla
                                throw Exception("El servidor de Inteligencia Artificial falló en la nómina ${emp.nomina}.")
                            }
                        } catch(e: Exception) {
                            // Lo tronamos si ocurre un SocketTimeout o caída de red
                            Log.e("FACEID_DEBUG", "❌ Error Crítico: ${e.message}")
                            throw Exception("Fallo de red o servidor en nómina ${emp.nomina}: ${e.message}")
                        }
                    } else {
                        Log.w("FACEID_DEBUG", "⚠️ Se omitió nómina ${emp.nomina} porque no tiene fotografía.")
                    }
                }

                Toast.makeText(context, "Proceso completado. $successCount huellas guardadas.", Toast.LENGTH_LONG).show()
                Log.i("FACEID_DEBUG", "=== FIN DEL PROCESO MASIVO. Éxitos: $successCount/${empleados.size} ===")

            } catch (e: Exception) {
                // Al "tronar" el proceso arriba, caerá directo aquí, limpiando la pantalla y avisándote el error exacto
                Log.e("FACEID_DEBUG", "Error crítico en proceso masivo:", e)
                Toast.makeText(context, "Detenido por Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                hideLoader()
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

    // ==========================================
    // GENERADOR DE HUELLA DIGITAL BIOMÉTRICA (FALLBACK)
    // ==========================================
    private fun generateFingerprint(base64Str: String): String {
        return try {
            val bytes = base64Str.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "huella_no_generada"
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
}