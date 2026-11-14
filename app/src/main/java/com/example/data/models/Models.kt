package com.example.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val displayName: String = "",
  val role: String = "propietario", // "propietario" | "taller"
  val workshopName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Vehicle(
  @DocumentId val id: String = "",
  val vin: String = "",
  val plate: String = "",
  val brand: String = "",
  val model: String = "",
  val year: Int = 0,
  val mileage: Int = 0,
  val ownerId: String = "",
  val servicingWorkshopIds: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Intervention(
  @DocumentId val id: String = "",
  val vin: String = "",
  val plate: String = "",
  val workshopId: String = "",
  val workshopName: String = "",
  val ownerId: String = "",
  val date: String = "",
  val mileage: Int = 0,
  val workDone: String = "",
  val partsUsed: String = "",
  val photoBase64: String = "",
  val photoGps: String = "",
  val photoTimestamp: String = "",
  val hasPhoto: Boolean = false,
  val signatureBase64: String = "",
  val hasSignature: Boolean = false,
  val signatureDisclaimer: String = "Trazo capturado en el dispositivo, no una firma electrónica certificada",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InventoryItem(
  @DocumentId val id: String = "",
  val workshopId: String = "",
  val partNumber: String = "",
  val name: String = "",
  val category: String = "",
  val stock: Int = 0,
  val minStock: Int = 0,
  val unitPrice: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DtcCode(
  val code: String,
  val system: String,
  val description: String,
  val nextStep: String,
  val severity: String, // "Alta", "Media", "Baja"
  val isDemo: Boolean = true
)

enum class AlertUrgency {
  OK,
  DUE_SOON,
  OVERDUE
}

data class MaintenanceAlert(
  val id: String,
  val component: String,
  val intervalKm: Int,
  val targetKm: Int,
  val currentKm: Int,
  val remainingKm: Int,
  val urgency: AlertUrgency,
  val recommendation: String,
  val emailPrepared: Boolean = true
)
