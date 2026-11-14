package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.models.AlertUrgency
import com.example.data.models.Intervention
import com.example.data.models.MaintenanceAlert
import com.example.data.models.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicalReportHelper {

  fun shareClinicalRecord(
    context: Context,
    vehicle: Vehicle?,
    interventions: List<Intervention>,
    alerts: List<MaintenanceAlert>,
    isDemoMode: Boolean
  ) {
    val dateNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL")).format(Date())
    val headerDemo = if (isDemoMode) "[REGISTRO DE DEMOSTRACIÓN ACADÉMICA - NO SON DATOS REALES]\n" else ""

    val sb = StringBuilder()
    sb.append(headerDemo)
    sb.append("==========================================\n")
    sb.append("VINcheck — EXPEDIENTE CLÍNICO DIGITAL\n")
    sb.append("Sistema Digital de Diagnóstico Automotriz\n")
    sb.append("Duoc UC • Sede San Joaquín\n")
    sb.append("Emitido: $dateNow\n")
    sb.append("==========================================\n\n")

    if (vehicle != null) {
      sb.append("VEHÍCULO:\n")
      sb.append("• Marca/Modelo: ${vehicle.brand} ${vehicle.model} (${vehicle.year})\n")
      sb.append("• Patente: ${vehicle.plate}\n")
      sb.append("• VIN: ${vehicle.vin}\n")
      sb.append("• Odómetro registrado: ${vehicle.mileage} km\n\n")
    }

    sb.append("HISTORIAL DE INTERVENCIONES (${interventions.size}):\n")
    if (interventions.isEmpty()) {
      sb.append("• Sin intervenciones clínicas registradas.\n")
    } else {
      interventions.forEachIndexed { idx, item ->
        val photoLabel = if (item.hasPhoto) "Sí (${item.photoGps.ifBlank { "GPS N/D" }})" else "No adjunta"
        val signLabel = if (item.hasSignature) "Sí (Trazo en dispositivo)" else "Pendiente"
        sb.append("${idx + 1}. [${item.date}] ${item.mileage} km - ${item.workshopName}\n")
        sb.append("   Trabajo: ${item.workDone}\n")
        if (item.partsUsed.isNotBlank()) {
          sb.append("   Repuestos: ${item.partsUsed}\n")
        }
        sb.append("   Evidencia foto: $photoLabel | Firma: $signLabel\n")
      }
    }

    sb.append("\nALERTAS DE MANTENIMIENTO POR KILOMETRAJE:\n")
    alerts.forEach { alert ->
      val status = when (alert.urgency) {
        AlertUrgency.OVERDUE -> "VENCIDO"
        AlertUrgency.DUE_SOON -> "PRÓXIMO A VENCER"
        AlertUrgency.OK -> "AL DÍA"
      }
      sb.append("• ${alert.component}: [$status] Próximo a los ${alert.targetKm} km (Restan ${alert.remainingKm} km)\n")
    }

    sb.append("\nAclaración técnica: Los trazos de firma capturados en el dispositivo no constituyen firma electrónica certificada.\n")

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Expediente Clínico VINcheck - ${vehicle?.plate ?: "Vehículo"}")
      putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir Expediente Clínico"))
  }

  fun composeMaintenanceEmailAlert(
    context: Context,
    recipientEmail: String,
    vehicle: Vehicle?,
    alerts: List<MaintenanceAlert>
  ) {
    val priorityAlerts = alerts.filter { it.urgency != AlertUrgency.OK }
    val targetList = priorityAlerts.ifEmpty { alerts }

    val subject = "VINcheck Alerta Preventiva: ${vehicle?.brand ?: "Vehículo"} ${vehicle?.plate ?: ""} (${vehicle?.mileage ?: 0} km)"
    val body = buildString {
      append("Estimado/a propietario/a,\n\n")
      append("Se emite el siguiente resumen de mantenimiento preventivo calculado según el kilometraje real de su vehículo:\n\n")
      if (vehicle != null) {
        append("• Vehículo: ${vehicle.brand} ${vehicle.model} (${vehicle.year})\n")
        append("• Patente: ${vehicle.plate}\n")
        append("• VIN: ${vehicle.vin}\n")
        append("• Kilometraje actual: ${vehicle.mileage} km\n\n")
      }
      append("Detalle de componentes evaluados:\n")
      targetList.forEach { alert ->
        val label = when (alert.urgency) {
          AlertUrgency.OVERDUE -> "VENCIDO"
          AlertUrgency.DUE_SOON -> "PRÓXIMO"
          AlertUrgency.OK -> "AL DÍA"
        }
        append("- ${alert.component} [$label]: Meta ${alert.targetKm} km | ${alert.recommendation}\n")
      }
      append("\nAtentamente,\nVINcheck — Sistema Digital de Diagnóstico Automotriz")
    }

    val uri = Uri.parse(
      "mailto:${Uri.encode(recipientEmail)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    try {
      context.startActivity(intent)
    } catch (_: Exception) {
      val fallback = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
      }
      context.startActivity(Intent.createChooser(fallback, "Enviar alerta por correo"))
    }
  }
}
