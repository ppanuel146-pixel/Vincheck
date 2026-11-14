package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.models.InventoryItem
import com.example.data.models.Intervention
import com.example.data.models.UserProfile
import com.example.data.models.Vehicle
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VincheckRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createInventoryItem_authenticatedWorkshop_succeeds() {
    runBlocking {
      val workshopUid = signInTestUser(WORKSHOP_EMAIL)
      val repo = VincheckRepository(firestore)

      val item = InventoryItem(
        partNumber = "BOSCH-PAD-99",
        name = "Pastillas de freno delanteras",
        category = "Frenos",
        stock = 8,
        minStock = 2,
        unitPrice = 32000
      )

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addInventoryItem(item)
      }
      assertTrue(result.isSuccess)
      val docId = result.getOrThrow()
      assertTrue(docId.isNotEmpty())

      val inventoryList = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeInventory(workshopUid).first { items: List<InventoryItem> -> items.isNotEmpty() }
      }
      assertTrue(inventoryList.any { it.partNumber == "BOSCH-PAD-99" })
    }
  }

  @Test
  fun createIntervention_authenticatedWorkshop_resolvesRealOwnerAndUpdatesMileage() {
    runBlocking {
      val ownerUid = signInTestUser(OWNER_EMAIL)
      val ownerRepo = VincheckRepository(firestore)
      val testVin = "1HGCR2F83HA999999"

      // Owner registers vehicle
      val vehicleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo.registerVehicle(
          Vehicle(
            vin = testVin,
            plate = "CC-DD-20",
            brand = "Toyota",
            model = "Corolla",
            year = 2021,
            mileage = 45000
          )
        )
      }
      assertTrue(vehicleResult.isSuccess)

      // Workshop signs in and creates profile with role "taller"
      val workshopUid = signInTestUser(WORKSHOP_EMAIL)
      val workshopRepo = VincheckRepository(firestore)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        workshopRepo.saveUserProfile(
          UserProfile(
            userId = workshopUid,
            email = WORKSHOP_EMAIL,
            displayName = "Taller Duoc",
            role = "taller",
            workshopName = "Taller Duoc San Joaquín"
          )
        )
      }

      // Workshop looks up vehicle by VIN
      val lookedUp = withTimeout(DEFAULT_TIMEOUT_MS) {
        workshopRepo.getVehicleByVin(testVin)
      }
      assertNotNull(lookedUp)
      assertEquals(ownerUid, lookedUp?.ownerId)

      // Workshop creates intervention without needing to hardcode ownerId manually
      val intervention = Intervention(
        vin = testVin,
        plate = "CC-DD-20",
        workshopId = workshopUid,
        workshopName = "Taller Duoc San Joaquín",
        ownerId = "",
        date = "2026-11-12",
        mileage = 50000,
        workDone = "Mantención 50.000 km",
        partsUsed = "Aceite 5W30 sintético + filtro",
        hasPhoto = false,
        hasSignature = false
      )

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        workshopRepo.createIntervention(intervention)
      }
      assertTrue(result.isSuccess)

      // Sign in as owner and verify intervention and updated mileage synced
      signInTestUser(OWNER_EMAIL)
      val ownerInterventions = withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeInterventionsForOwner(ownerUid).first { items: List<Intervention> ->
          items.any { it.vin == testVin }
        }
      }
      assertTrue(ownerInterventions.any { it.vin == testVin })

      val updatedVehicle = withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo.getVehicleByVin(testVin)
      }
      assertEquals(50000, updatedVehicle?.mileage)
    }
  }

  @Test
  fun observeInventory_unauthenticatedUser_failsWithPermissionDenied() {
    runBlocking {
      auth.signOut()
      val repo = VincheckRepository(firestore)

      var caughtException: Exception? = null
      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeInventory("some_uid").first()
        }
      } catch (e: Exception) {
        caughtException = e
      }

      assertNotNull("Expected exception on unauthenticated query", caughtException)
      val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $exceptionMsg"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  private companion object {
    const val WORKSHOP_EMAIL = "taller@vincheck.cl"
    const val OWNER_EMAIL = "propietario@vincheck.cl"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
    const val exceptionMsg = "exception"
  }
}
