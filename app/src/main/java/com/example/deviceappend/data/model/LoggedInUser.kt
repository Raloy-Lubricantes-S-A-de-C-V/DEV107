package com.example.deviceappend.data.model

/**
 * Modelo de datos para el usuario autenticado.
 */
data class LoggedInUser(
    val userId: String,
    val displayName: String,
    val message: String = "Login exitoso" // Mensaje dinámico del API
)