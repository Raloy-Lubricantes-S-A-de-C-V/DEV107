package com.example.deviceappend.ui.delsip

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.deviceappend.MainActivity
import com.example.deviceappend.R
import com.example.deviceappend.core.network.RetrofitClient
import com.example.deviceappend.core.session.SessionManager
import com.example.deviceappend.utils.checkconnect
import com.example.deviceappend.utils.hideLoader
import com.example.deviceappend.utils.showLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.deviceappend.core.network.AiPhotoRequest
import com.example.deviceappend.core.network.SaveHashRequest // Corregido: Este es el modelo correcto
import kotlinx.coroutines.launch
import org.json.JSONObject // Agregado para extraer el hash de forma segura

class DelsipTestFragment : Fragment(R.layout.fragment_delsip_test) {

    private lateinit var sessionManager: SessionManager
    private lateinit var ivTestImage: ImageView
    private lateinit var etNominaTest: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        if (!sessionManager.isSys()) {
            Toast.makeText(context, "Acceso restringido a administradores de sistema.", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.supportFragmentManager?.popBackStack()
            return
        }

        setupMenu()

        checkconnect(view, "Cargando Diagnóstico DelSIP...") {
            val btnTestDb = view.findViewById<MaterialButton>(R.id.btnTestDb)
            val btnTestFolders = view.findViewById<MaterialButton>(R.id.btnTestFolders)
            val btnFaceIdEnroll = view.findViewById<MaterialButton>(R.id.btnFaceIdEnroll)

            ivTestImage = view.findViewById(R.id.ivTestImage)
            etNominaTest = view.findViewById(R.id.etNominaTest)

            btnTestDb.setOnClickListener {
                testDatabaseConnection()
            }

            btnTestFolders.setOnClickListener {
                testImageExtraction()
            }

            btnFaceIdEnroll.setOnClickListener {
                startFaceIdEnrollment()
            }
        }
    }

    private fun startFaceIdEnrollment() {
        viewLifecycleOwner.lifecycleScope.launch {
            System.out.println(">>> FACEID_DEBUG: Iniciando startFaceIdEnrollment")
            showLoader("Obteniendo lista de nóminas...")
            try {
                System.out.println(">>> FACEID_DEBUG: Llamando a getActiveEmployees()...")
                val response = RetrofitClient.instance.getActiveEmployees()
                System.out.println(">>> FACEID_DEBUG: Respuesta recibida. Código: ${response.code()}")

                if (response.isSuccessful && response.body()?.error == false) {
                    val employees = response.body()!!.data
                    val total = employees.size
                    System.out.println(">>> FACEID_DEBUG: Empleados cargados: $total")

                    var processed = 0
                    for (emp in employees) {
                        processed++
                        val nomina = emp.nomina ?: continue
                        System.out.println(">>> FACEID_DEBUG: [$processed/$total] Procesando nómina: $nomina")

                        // CORRECCIÓN: Quitamos emp.nombre porque la API de empleados activos no lo devuelve
                        showLoader("Procesando $processed de $total\nNómina: $nomina")

                        try {
                            var fotoBase64 = emp.foto_base64

                            // Validamos si la foto vino vacía, la pedimos individualmente (Fallback)
                            if (fotoBase64.isNullOrEmpty() || fotoBase64.length < 100) {
                                val imgRes = RetrofitClient.instance.testDelsipImage(nomina)
                                if (imgRes.isSuccessful) {
                                    fotoBase64 = imgRes.body()?.data?.imageBase64
                                }
                            }

                            if (!fotoBase64.isNullOrEmpty() && fotoBase64.length > 100) {
                                // Limpiamos el Base64 de cualquier etiqueta MIME
                                val cleanBase64 = fotoBase64.replace("\\s+".toRegex(), "").let {
                                    if (it.contains(",")) it.substringAfter(",") else it
                                }

                                // CORRECCIÓN: Nombre de función actualizado a generateFaceIdHashAi
                                val hashRes = RetrofitClient.instance.generateFaceIdHashAi(AiPhotoRequest(cleanBase64))

                                if (hashRes.isSuccessful && hashRes.body() != null) {
                                    val aiData = hashRes.body()!!

                                    // BLINDAJE: Extracción segura del JSON de N8N/Gemini
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

                                    if (hashGenerado.isNotEmpty() && hashGenerado.lowercase() != "false") {
                                        // CORRECCIÓN: Nombre de Request actualizado a SaveHashRequest
                                        val saveRes = RetrofitClient.instance.saveBiometricHash(SaveHashRequest(
                                            nomina = nomina,
                                            edad = emp.edad ?: 0,
                                            biometric_hash = hashGenerado
                                        ))

                                        if (saveRes.isSuccessful) {
                                            Log.i("FACEID_DEBUG", "✅ Huella guardada en Postgres para nómina: $nomina")
                                        } else {
                                            Log.e("FACEID_DEBUG", "❌ Fallo al guardar en BD: ${saveRes.errorBody()?.string()}")
                                        }
                                    }
                                }
                            } else {
                                Log.w("FACEID_DEBUG", "⚠️ Se omitió nómina $nomina porque no tiene fotografía.")
                            }
                        } catch (e: Exception) {
                            System.out.println(">>> FACEID_DEBUG: Error en nómina $nomina: ${e.message}")
                        }
                    }
                    hideLoader()
                    Toast.makeText(context, "✅ Enrolamiento masivo completado.", Toast.LENGTH_LONG).show()
                } else {
                    hideLoader()
                    System.out.println(">>> FACEID_DEBUG: Error API o body nulo")
                    Toast.makeText(context, "❌ Error al obtener lista de empleados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                hideLoader()
                System.out.println(">>> FACEID_DEBUG: EXCEPCIÓN CRÍTICA: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "❌ Error crítico: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testDatabaseConnection() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoader("Probando conexión a SQL Server DelSIP...")
            try {
                val response = RetrofitClient.instance.testDelsipConnection()

                if (response.isSuccessful && response.body()?.error == false) {
                    val data = response.body()?.data
                    val count = data?.size ?: 0
                    Toast.makeText(context, "✅ ¡Conexión exitosa! Se leyeron $count registros.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ Fallo en la conexión (HTTP ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error de red: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                hideLoader()
            }
        }
    }

    private fun testImageExtraction() {
        val nomina = etNominaTest.text.toString().trim()

        if (nomina.isEmpty()) {
            etNominaTest.error = "Escriba una nómina válida"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            showLoader("Buscando e importando fotografía...")
            ivTestImage.visibility = View.GONE

            try {
                val response = RetrofitClient.instance.testDelsipImage(nomina)

                if (response.isSuccessful && response.body()?.error == false) {

                    // CORRECCIÓN EXACTA APLICADA AQUÍ: Extraemos el Base64 desde el sub-objeto
                    val base64Data = response.body()?.data?.imageBase64

                    if (!base64Data.isNullOrEmpty()) {
                        val cleanBase64 = if (base64Data.contains(",")) {
                            base64Data.substringAfter(",")
                        } else {
                            base64Data
                        }

                        val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                        if (decodedImage != null) {
                            ivTestImage.setImageBitmap(decodedImage)
                            ivTestImage.visibility = View.VISIBLE
                            Toast.makeText(context, "✅ Fotografía importada exitosamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ La cadena Base64 no es una imagen válida", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "❌ La API no retornó datos de imagen para esta nómina", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "❌ Nómina no encontrada o Fallo en servidor (HTTP ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error de red: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                hideLoader()
            }
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_menu, menu)
                menu.findItem(R.id.action_home)?.isVisible = true
                menu.findItem(R.id.action_logout)?.isVisible = false
                menu.findItem(R.id.action_notifications)?.isVisible = false
                menu.findItem(R.id.action_modules)?.isVisible = false
            }
            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}