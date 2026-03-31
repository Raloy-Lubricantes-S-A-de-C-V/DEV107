package com.example.myapplication.utils

/**
 * BitAlgorithm: Motor de decodificación de etiquetas físicas.
 * Convierte el patrón de 12 bits detectado por la IA en un ID de 10 dígitos.
 */
object BitAlgorithm {

    /**
     * @param bits Cadena de 12 caracteres (ej: "010010101101")
     * Estructura:
     * [0] -> Bit de Ambiente (0=Producción, 1=Desarrollo)
     * [1-10] -> ID Consecutivo (10 bits = hasta 1024 registros)
     * [11] -> Bit 12: Confirmación Legal / Custodia
     */
    fun decode(bits: String): String {
        if (bits.length != 12) return "0000000000"

        // Extraer los 10 bits centrales que conforman el ID
        val binaryId = bits.substring(1, 11)

        // Convertir binario a Decimal
        val decimalId = try {
            binaryId.toInt(2)
        } catch (e: Exception) {
            0
        }

        // Regla de Negocio: Rellenar con ceros a la izquierda hasta 10 dígitos
        // para coincidir con la máscara de la base de datos de Activos
        return decimalId.toString().padStart(10, '0')
    }

    /**
     * Verifica si el Bit 12 (el último de la etiqueta) está activo.
     * Si es '1', el Wizard disparará el diálogo de aceptación legal.
     */
    fun isLegalBitActive(bits: String): Boolean {
        return bits.length == 12 && bits.last() == '1'
    }
}