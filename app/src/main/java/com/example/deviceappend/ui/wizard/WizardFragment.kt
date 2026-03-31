package com.example.myapplication.ui.wizard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentWizardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WizardFragment : Fragment(R.layout.fragment_wizard) {

    private val viewModel: WizardViewModel by viewModels()
    private var _binding: FragmentWizardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWizardBinding.bind(view)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.step.observe(viewLifecycleOwner) { step ->
            updateUIForStep(step)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WizardState.Loading -> binding.overlayLoading.visibility = View.VISIBLE
                is WizardState.ShowLegalTerms -> mostrarDialogoLegal(state.id)
                is WizardState.Error -> {
                    binding.overlayLoading.visibility = View.GONE
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Atención")
                        .setMessage(state.message)
                        .setPositiveButton("Reintentar", null)
                        .show()
                }
                else -> binding.overlayLoading.visibility = View.GONE
            }
        }
    }

    private fun updateUIForStep(step: Int) {
        binding.tvStepTitle.text = when(step) {
            0 -> "Validación de Identidad (DelSIP)"
            1 -> "Modalidad de Asignación"
            4 -> "Escaneo de Placa (OCR)"
            7 -> "Verificación de Etiqueta (Bits)"
            else -> "Paso $step"
        }
        // Lógica para inflar sub-vistas dinámicamente según el paso
    }

    private fun mostrarDialogoLegal(id: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Responsabilidad Legal (Bit 12)")
            .setMessage("Al aceptar, usted reconoce la custodia del activo $id y las responsabilidades derivadas...")
            .setPositiveButton("ACEPTO") { _, _ -> /* Finalizar en Mongo */ }
            .setCancelable(false)
            .show()
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            // Lógica para capturar foto o procesar datos del paso actual
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}