package com.example.utils

object ValidationUtils {

  /**
   * Valida formato de patente chilena:
   * - Nuevo formato (2007 en adelante): 4 letras consonantes + 2 dígitos (ej: BBBB10, BB-BB-10)
   * - Antiguo formato: 2 letras + 4 dígitos (ej: AA1000, AA-10-00)
   */
  fun isValidChileanPlate(plate: String): Boolean {
    val clean = plate.uppercase().replace("-", "").replace(" ", "").trim()
    // Nuevo formato: 4 letras (excluye vocales en patente estándar) + 2 números
    val newFormatRegex = Regex("^[B-DF-HJ-N-P-TV-Z]{4}[0-9]{2}$")
    // Antiguo formato: 2 letras + 4 números
    val oldFormatRegex = Regex("^[A-Z]{2}[0-9]{4}$")
    return clean.matches(newFormatRegex) || clean.matches(oldFormatRegex)
  }

  fun formatChileanPlate(plate: String): String {
    val clean = plate.uppercase().replace("-", "").replace(" ", "").trim()
    return when {
      clean.length == 6 && clean.take(4).all { it.isLetter() } -> {
        "${clean.substring(0, 2)}·${clean.substring(2, 4)}·${clean.substring(4, 6)}"
      }
      clean.length == 6 && clean.take(2).all { it.isLetter() } -> {
        "${clean.substring(0, 2)}·${clean.substring(2, 6)}"
      }
      else -> clean
    }
  }

  /**
   * Valida formato internacional VIN (ISO 3779):
   * 17 caracteres alfanuméricos, excluyendo letras I, O, Q para evitar confusión con 1 y 0.
   */
  fun isValidVin(vin: String): Boolean {
    val clean = vin.uppercase().trim()
    val vinRegex = Regex("^[A-HJ-NPR-Z0-9]{17}$")
    return clean.matches(vinRegex)
  }

  /**
   * Extrae un VIN válido (17 caracteres ISO 3779) desde el texto crudo de un código QR
   * o código de barras automotriz (soporta texto plano, URLs, JSON o prefijo VIN:/I).
   */
  fun extractVinFromQrPayload(rawPayload: String): String? {
    val upper = rawPayload.uppercase().trim()
    if (isValidVin(upper)) return upper

    // Algunos códigos Code 39 de parabrisas anteponen una 'I' de importación (18 caracteres)
    if (upper.length == 18 && upper.startsWith("I")) {
      val trimmed = upper.substring(1)
      if (isValidVin(trimmed)) return trimmed
    }

    // Buscar cualquier token de 17 caracteres ISO 3779 dentro de URLs, JSON o texto multilínea
    val tokenRegex = Regex("(?<![A-Z0-9])[A-HJ-NPR-Z0-9]{17}(?![A-Z0-9])")
    return tokenRegex.find(upper)?.value
  }
}

