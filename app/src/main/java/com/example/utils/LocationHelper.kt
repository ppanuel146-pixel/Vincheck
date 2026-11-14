package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Locale

object LocationHelper {

  fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentGpsCoordinates(context: Context): String? {
    if (!hasLocationPermission(context)) {
      return null
    }

    return try {
      val fusedClient = LocationServices.getFusedLocationProviderClient(context)
      val cts = CancellationTokenSource()
      val location = fusedClient.getCurrentLocation(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        cts.token
      ).await() ?: fusedClient.lastLocation.await()

      if (location != null) {
        String.format(Locale.US, "%.5f, %.5f (±%.1fm)", location.latitude, location.longitude, location.accuracy)
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }
}
