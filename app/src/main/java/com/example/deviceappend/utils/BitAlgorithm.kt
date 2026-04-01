package com.example.deviceappend.utils // Corregido

object BitAlgorithm {
    fun decode(bits: String): String {
        if (bits.length != 12) return "0000000000"
        val binaryId = bits.substring(1, 11)
        val decimalId = try { binaryId.toInt(2) } catch (e: Exception) { 0 }
        return decimalId.toString().padStart(10, '0')
    }

    fun isLegalBitActive(bits: String): Boolean {
        return bits.length == 12 && bits.last() == '1'
    }
}