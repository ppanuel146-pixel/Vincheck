package com.example.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatermarkUtils {

  fun applyWatermark(
    original: Bitmap,
    vin: String,
    plate: String,
    gpsText: String
  ): Bitmap {
    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val width = result.width
    val height = result.height

    // Altura del banner inferior para la marca de agua
    val bannerHeight = (height * 0.22f).coerceAtLeast(140f)
    val bannerTop = height - bannerHeight

    // Fondo semi-transparente oscuro
    val bgPaint = Paint().apply {
      color = Color.argb(220, 15, 23, 42) // Slate-900 translúcido
      style = Paint.Style.FILL
    }
    canvas.drawRect(0f, bannerTop, width.toFloat(), height.toFloat(), bgPaint)

    // Borde superior de acento (Azul DUOC / Ámbar)
    val borderPaint = Paint().apply {
      color = Color.rgb(217, 119, 6) // Amber-600
      strokeWidth = 6f
    }
    canvas.drawLine(0f, bannerTop, width.toFloat(), bannerTop, borderPaint)

    // Configuración de texto
    val textSize = (bannerHeight * 0.17f).coerceIn(16f, 32f)

    val textPaintHeader = Paint().apply {
      color = Color.rgb(245, 158, 11) // Amber-500
      this.textSize = textSize * 1.05f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }

    val textPaintBody = Paint().apply {
      color = Color.WHITE
      this.textSize = textSize
      typeface = Typeface.MONOSPACE
      isAntiAlias = true
    }

    val textPaintFooter = Paint().apply {
      color = Color.rgb(148, 163, 184) // Slate-400
      this.textSize = textSize * 0.85f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
      isAntiAlias = true
    }

    val currentTimestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CL")).format(Date())

    val paddingX = 24f
    var currentY = bannerTop + (textSize * 1.4f)

    // Línea 1: VIN y Patente
    canvas.drawText("EVIDENCIA TÉCNICA | VIN: ${vin.uppercase()} | PATENTE: ${plate.uppercase()}", paddingX, currentY, textPaintHeader)

    // Línea 2: Fecha y hora
    currentY += textSize * 1.35f
    canvas.drawText("CAPTURA: $currentTimestamp", paddingX, currentY, textPaintBody)

    // Línea 3: Coordenadas GPS reales
    currentY += textSize * 1.35f
    val gpsLabel = if (gpsText.isNotBlank()) gpsText else "Ubicación GPS: No disponible"
    canvas.drawText("GPS: $gpsLabel", paddingX, currentY, textPaintBody)

    // Línea 4: Título institucional
    currentY += textSize * 1.25f
    canvas.drawText("Expediente Clínico Digital • Duoc UC San Joaquín", paddingX, currentY, textPaintFooter)

    return result
  }

  fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
  }

  fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
      val clean = if (base64Str.startsWith("data:image")) {
        base64Str.substringAfter(",")
      } else {
        base64Str
      }
      val decoded = Base64.decode(clean, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    } catch (e: Exception) {
      null
    }
  }
}
