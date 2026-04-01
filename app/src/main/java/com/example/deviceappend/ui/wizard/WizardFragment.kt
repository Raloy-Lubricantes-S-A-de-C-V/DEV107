package com.example.deviceappend.ui.wizard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.deviceappend.R
import com.example.deviceappend.databinding.FragmentWizardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WizardFragment : Fragment(R.layout.fragment_wizard) {

    private val viewModel: WizardViewModel by viewModels()
    private var _binding: FragmentWizardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWizardBinding.bind(view)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.step.observe(viewLifecycleOwner) { step ->
            updateUIForStep(step)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                // ACCESO CORREGIDO: Si el ID en XML es overlayLoading, se usa así:
                is WizardState.Loading -> {
                    binding.overlayLoading.root.visibility = View.VISIBLE
                }
                is WizardState.ShowLegalTerms -> {
                    binding.overlayLoading.root.visibility = View.GONE
                    mostrarDialogoLegal(state.id)
                }
                is WizardState.Error -> {
                    binding.overlayLoading.root.visibility = View.GONE
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Atención")
                        .setMessage(state.message)
                        .setPositiveButton("Reintentar", null)
                        .show()
                }
                else -> {
                    binding.overlayLoading.root.visibility = View.GONE
                }
            }
        }
    }

    private fun updateUIForStep(step: Int) {
        binding.tvStepTitle.text = when(step) {
            0 -> "Validación de Identidad"
            7 -> "Verificación de Etiqueta"
            else -> "Paso $step"
        }
    }

    private fun mostrarDialogoLegal(id: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Responsabilidad Legal")
            .setMessage("Confirmación para el activo $id")
            .setPositiveButton("ACEPTO", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}