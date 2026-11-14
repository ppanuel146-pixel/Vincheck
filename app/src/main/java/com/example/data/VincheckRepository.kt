package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.InventoryItem
import com.example.data.models.Intervention
import com.example.data.models.UserProfile
import com.example.data.models.Vehicle
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VincheckRepository(val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Usuario no autenticado en Firebase.")
  }

  // --- USER PROFILE ---
  suspend fun getUserProfile(userId: String): UserProfile? {
    return try {
      val doc = db.collection("users").document(userId).get().await()
      doc.toObject(UserProfile::class.java)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error fetching user profile", e)
      null
    }
  }

  suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      val payload = mutableMapOf<String, Any>(
        "userId" to uid,
        "email" to profile.email,
        "displayName" to profile.displayName,
        "role" to profile.role,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (profile.workshopName.isNotBlank()) {
        payload["workshopName"] = profile.workshopName
      }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error saving user profile", e)
      Result.failure(e)
    }
  }

  suspend fun updateUserRole(role: String, workshopName: String? = null): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid)
      val updates = mutableMapOf<String, Any>(
        "role" to role,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (!workshopName.isNullOrBlank()) {
        updates["workshopName"] = workshopName
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error updating user role", e)
      Result.failure(e)
    }
  }

  // --- VEHICLES (Keyed by 17-char VIN) ---
  fun observeVehiclesForOwner(ownerId: String): Flow<List<Vehicle>> = flow {
    emitAll(
      db.collection("vehicles")
        .whereEqualTo("ownerId", ownerId)
        .snapshots()
        .map { snapshot -> snapshot.toObjects(Vehicle::class.java) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          Log.e("VincheckRepo", "Error observing owner vehicles: ${error.message}", error)
          throw error
        }
    )
  }

  suspend fun getVehicleByVin(vin: String): Vehicle? {
    return try {
      val cleanVin = vin.uppercase().trim()
      if (cleanVin.length != 17) return null
      val doc = db.collection("vehicles").document(cleanVin).get().await()
      if (doc.exists()) {
        doc.toObject(Vehicle::class.java)
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error getting vehicle by VIN", e)
      null
    }
  }

  suspend fun registerVehicle(vehicle: Vehicle): Result<String> {
    return try {
      val uid = requireUserId()
      val cleanVin = vehicle.vin.uppercase().trim()
      val cleanPlate = vehicle.plate.uppercase().trim()
      val docRef = db.collection("vehicles").document(cleanVin)
      val payload = mapOf(
        "vin" to cleanVin,
        "plate" to cleanPlate,
        "brand" to vehicle.brand.trim(),
        "model" to vehicle.model.trim(),
        "year" to vehicle.year,
        "mileage" to vehicle.mileage,
        "ownerId" to uid,
        "servicingWorkshopIds" to emptyList<String>(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error registering vehicle", e)
      Result.failure(e)
    }
  }

  suspend fun updateVehicleMileage(vin: String, newMileage: Int): Result<Unit> {
    return try {
      val cleanVin = vin.uppercase().trim()
      db.collection("vehicles").document(cleanVin).update(
        mapOf(
          "mileage" to newMileage,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error updating vehicle mileage", e)
      Result.failure(e)
    }
  }

  suspend fun recordWorkshopServiceOnVehicle(vin: String, newMileage: Int): Result<Unit> {
    return try {
      val uid = requireUserId()
      val cleanVin = vin.uppercase().trim()
      val docRef = db.collection("vehicles").document(cleanVin)
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) return Result.success(Unit)
      val currentMileage = (snapshot.getLong("mileage") ?: 0L).toInt()
      val targetMileage = maxOf(currentMileage, newMileage)
      docRef.update(
        mapOf(
          "mileage" to targetMileage,
          "servicingWorkshopIds" to FieldValue.arrayUnion(uid),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error recording workshop service on vehicle", e)
      Result.failure(e)
    }
  }

  suspend fun deleteVehicle(vin: String): Result<Unit> {
    return try {
      val cleanVin = vin.uppercase().trim()
      db.collection("vehicles").document(cleanVin).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error deleting vehicle", e)
      Result.failure(e)
    }
  }

  // --- INTERVENTIONS ---
  fun observeInterventionsForOwner(ownerId: String): Flow<List<Intervention>> = flow {
    emitAll(
      db.collection("interventions")
        .whereEqualTo("ownerId", ownerId)
        .snapshots()
        .map { snapshot -> snapshot.toObjects(Intervention::class.java).sortedByDescending { it.date } }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          Log.e("VincheckRepo", "Error observing owner interventions", error)
          throw error
        }
    )
  }

  fun observeInterventionsForWorkshop(workshopId: String): Flow<List<Intervention>> = flow {
    emitAll(
      db.collection("interventions")
        .whereEqualTo("workshopId", workshopId)
        .snapshots()
        .map { snapshot -> snapshot.toObjects(Intervention::class.java).sortedByDescending { it.date } }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          Log.e("VincheckRepo", "Error observing workshop interventions", error)
          throw error
        }
    )
  }

  suspend fun createIntervention(intervention: Intervention): Result<String> {
    return try {
      val uid = requireUserId()
      val cleanVin = intervention.vin.uppercase().trim()
      // Resolver el ownerId real del vehículo si existe en la nube
      val existingVehicle = getVehicleByVin(cleanVin)
      val resolvedOwnerId = existingVehicle?.ownerId?.takeIf { it.isNotBlank() }
        ?: intervention.ownerId.ifBlank { uid }

      val docRef = db.collection("interventions").document()
      val payload = mutableMapOf<String, Any>(
        "vin" to cleanVin,
        "plate" to (existingVehicle?.plate?.takeIf { it.isNotBlank() } ?: intervention.plate.uppercase().trim()),
        "workshopId" to uid,
        "workshopName" to intervention.workshopName.ifBlank { "Taller Mecánico" },
        "ownerId" to resolvedOwnerId,
        "date" to intervention.date,
        "mileage" to intervention.mileage,
        "workDone" to intervention.workDone,
        "partsUsed" to intervention.partsUsed,
        "hasPhoto" to intervention.hasPhoto,
        "hasSignature" to intervention.hasSignature,
        "signatureDisclaimer" to intervention.signatureDisclaimer,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (intervention.photoBase64.isNotBlank()) {
        payload["photoBase64"] = intervention.photoBase64
      }
      if (intervention.photoGps.isNotBlank()) {
        payload["photoGps"] = intervention.photoGps
      }
      if (intervention.photoTimestamp.isNotBlank()) {
        payload["photoTimestamp"] = intervention.photoTimestamp
      }
      if (intervention.signatureBase64.isNotBlank()) {
        payload["signatureBase64"] = intervention.signatureBase64
      }

      docRef.set(payload).await()

      // Sincronizar kilometraje y taller atendiente en el documento del vehículo
      if (existingVehicle != null) {
        recordWorkshopServiceOnVehicle(cleanVin, intervention.mileage)
      }

      Result.success(docRef.id)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error creating intervention", e)
      Result.failure(e)
    }
  }

  suspend fun updateInterventionEvidence(
    interventionId: String,
    photoBase64: String? = null,
    gps: String? = null,
    photoTimestamp: String? = null,
    signatureBase64: String? = null
  ): Result<Unit> {
    return try {
      val docRef = db.collection("interventions").document(interventionId)
      val updates = mutableMapOf<String, Any>(
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (photoBase64 != null) {
        updates["photoBase64"] = photoBase64
        updates["hasPhoto"] = true
        if (gps != null) updates["photoGps"] = gps
        if (photoTimestamp != null) updates["photoTimestamp"] = photoTimestamp
      }
      if (signatureBase64 != null) {
        updates["signatureBase64"] = signatureBase64
        updates["hasSignature"] = true
        updates["signatureDisclaimer"] = "Trazo capturado en el dispositivo, no una firma electrónica certificada"
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error updating evidence", e)
      Result.failure(e)
    }
  }

  // --- WORKSHOP INVENTORY ---
  fun observeInventory(workshopId: String): Flow<List<InventoryItem>> = flow {
    emitAll(
      db.collection("inventory")
        .whereEqualTo("workshopId", workshopId)
        .snapshots()
        .map { snapshot -> snapshot.toObjects(InventoryItem::class.java).sortedBy { it.name } }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          Log.e("VincheckRepo", "Error observing inventory", error)
          throw error
        }
    )
  }

  suspend fun addInventoryItem(item: InventoryItem): Result<String> {
    return try {
      val uid = requireUserId()
      val docRef = db.collection("inventory").document()
      val payload = mapOf(
        "workshopId" to uid,
        "partNumber" to item.partNumber.trim().uppercase(),
        "name" to item.name.trim(),
        "category" to item.category.trim(),
        "stock" to item.stock,
        "minStock" to item.minStock,
        "unitPrice" to item.unitPrice,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error adding inventory item", e)
      Result.failure(e)
    }
  }

  suspend fun updateInventoryStock(itemId: String, newStock: Int): Result<Unit> {
    return try {
      val docRef = db.collection("inventory").document(itemId)
      docRef.update(
        mapOf(
          "stock" to newStock.coerceAtLeast(0),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error updating inventory stock", e)
      Result.failure(e)
    }
  }

  suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
    return try {
      db.collection("inventory").document(itemId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("VincheckRepo", "Error deleting inventory item", e)
      Result.failure(e)
    }
  }
}

private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
  flow.collect { value -> emit(value) }
}
