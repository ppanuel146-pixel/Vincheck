package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.VincheckRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OwnerScreen
import com.example.ui.screens.WorkshopScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          VincheckApp()
        }
      }
    }
  }
}

@Composable
fun VincheckApp() {
  val context = LocalContext.current
  val repo = remember { VincheckRepository(context) }
  val coroutineScope = rememberCoroutineScope()

  var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var userRole by remember { mutableStateOf("propietario") } // "propietario" | "taller"

  // Modo Demostración
  var isDemoMode by remember { mutableStateOf(false) }
  var demoRole by remember { mutableStateOf("propietario") }

  DisposableEffect(Unit) {
    val authListener = FirebaseAuth.AuthStateListener { auth ->
      firebaseUser = auth.currentUser
      val user = auth.currentUser
      if (user != null) {
        coroutineScope.launch {
          val profile = repo.getUserProfile(user.uid)
          if (profile != null) {
            userRole = profile.role
          }
        }
      }
    }
    Firebase.auth.addAuthStateListener(authListener)
    onDispose {
      Firebase.auth.removeAuthStateListener(authListener)
    }
  }

  when {
    // Modo Demostración activo
    isDemoMode -> {
      BackHandler {
        isDemoMode = false
      }
      if (demoRole == "propietario") {
        OwnerScreen(
          userId = "demo_owner_uid",
          isDemoMode = true,
          onSignOut = { isDemoMode = false },
          onSwitchToWorkshop = { demoRole = "taller" }
        )
      } else {
        WorkshopScreen(
          userId = "demo_workshop_uid",
          isDemoMode = true,
          onSignOut = { isDemoMode = false },
          onSwitchToOwner = { demoRole = "propietario" }
        )
      }
    }

    // Usuario autenticado con Google
    firebaseUser != null -> {
      val uid = firebaseUser!!.uid
      key(uid) {
        if (userRole == "taller") {
          WorkshopScreen(
            userId = uid,
            isDemoMode = false,
            onSignOut = {
              firebaseUser = null
            },
            onSwitchToOwner = {
              userRole = "propietario"
              coroutineScope.launch { repo.updateUserRole("propietario") }
            }
          )
        } else {
          OwnerScreen(
            userId = uid,
            isDemoMode = false,
            onSignOut = {
              firebaseUser = null
            },
            onSwitchToWorkshop = {
              userRole = "taller"
              coroutineScope.launch { repo.updateUserRole("taller") }
            }
          )
        }
      }
    }

    // Pantalla de inicio de sesión
    else -> {
      AuthScreen(
        onAuthenticated = { role ->
          userRole = role
          firebaseUser = Firebase.auth.currentUser
        },
        onEnterDemo = { selectedDemoRole ->
          demoRole = selectedDemoRole
          isDemoMode = true
        }
      )
    }
  }
}
