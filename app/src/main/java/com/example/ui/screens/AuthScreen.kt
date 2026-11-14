package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.VincheckRepository
import com.example.data.models.UserProfile
import com.example.ui.components.BadgeType
import com.example.ui.components.HonestBadge
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
  onAuthenticated: (role: String) -> Unit,
  onEnterDemo: (demoRole: String) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var selectedRole by remember { mutableStateOf("propietario") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val repo = remember { VincheckRepository(context) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E293B), // Slate 800
            Color(0xFF0F172A)
          )
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // LOGO & HEADER
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E3A8A)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.DirectionsCar,
          contentDescription = "Logo VINcheck",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "VINcheck",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
      )

      Text(
        text = "Expediente Clínico Digital Automotriz",
        color = Color(0xFF94A3B8),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
      )

      Text(
        text = "Duoc UC • Sede San Joaquín",
        color = Color(0xFFF59E0B),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(28.dp))

      // CARD DE AUTENTICACIÓN REAL
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Seguridad",
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
              text = "Acceso con Rol Institucional",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0F172A)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Seleccione el tipo de cuenta que registrará o vinculará:",
            fontSize = 13.sp,
            color = Color(0xFF475569)
          )

          Spacer(modifier = Modifier.height(12.dp))

          // SELECCIÓN DE ROL: PROPIETARIO
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.5.dp,
                if (selectedRole == "propietario") Color(0xFF2563EB) else Color(0xFFE2E8F0),
                RoundedCornerShape(10.dp)
              )
              .background(
                if (selectedRole == "propietario") Color(0xFFEFF6FF) else Color.Transparent,
                RoundedCornerShape(10.dp)
              )
              .selectable(
                selected = selectedRole == "propietario",
                onClick = { selectedRole = "propietario" }
              )
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = selectedRole == "propietario",
              onClick = { selectedRole = "propietario" }
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column {
              Text(
                text = "Propietario de Vehículo",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
              )
              Text(
                text = "Consulta expediente por VIN, alertas de km y telemetría",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // SELECCIÓN DE ROL: TALLER
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.5.dp,
                if (selectedRole == "taller") Color(0xFF2563EB) else Color(0xFFE2E8F0),
                RoundedCornerShape(10.dp)
              )
              .background(
                if (selectedRole == "taller") Color(0xFFEFF6FF) else Color.Transparent,
                RoundedCornerShape(10.dp)
              )
              .selectable(
                selected = selectedRole == "taller",
                onClick = { selectedRole = "taller" }
              )
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = selectedRole == "taller",
              onClick = { selectedRole = "taller" }
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column {
              Text(
                text = "Taller Mecánico / Servicio",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
              )
              Text(
                text = "Registra intervenciones, evidencia fotográfica, firma e inventario",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
              )
            }
          }

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = errorMessage ?: "",
              color = Color(0xFFDC2626),
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          // BOTÓN GOOGLE SIGN-IN OBLIGATORIO
          Button(
            onClick = {
              isLoading = true
              errorMessage = null
              triggerGoogleSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = coroutineScope,
                selectedRole = selectedRole,
                repo = repo,
                onSuccess = { role ->
                  isLoading = false
                  onAuthenticated(role)
                },
                onError = { err ->
                  isLoading = false
                  errorMessage = err
                },
                onCancelled = {
                  isLoading = false
                }
              )
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("google_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = "Iniciar sesión con Google",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // SECCIÓN MODO DEMOSTRACIÓN (Para evaluación académica en vivo)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
          ) {
            HonestBadge(type = BadgeType.DEMO_EXAMPLE, customText = "Modo Demostración Académica")
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Acceso directo para defensa ante la comisión evaluadora sin requerir cuentas de Google en tiempo real:",
            fontSize = 12.sp,
            color = Color(0xFFCBD5E1),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = { onEnterDemo("propietario") },
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFF59E0B)
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .testTag("demo_owner_button")
            ) {
              Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Demo Propietario", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { onEnterDemo("taller") },
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF60A5FA)
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .testTag("demo_workshop_button")
            ) {
              Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Demo Taller", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun triggerGoogleSignIn(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  selectedRole: String,
  repo: VincheckRepository,
  onSuccess: (role: String) -> Unit,
  onError: (String) -> Unit,
  onCancelled: () -> Unit
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onError("Configuración Web Client ID no encontrada.")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = Firebase.auth.signInWithCredential(authCredential).await()
        val user = authResult.user

        if (user != null) {
          // Consultar perfil existente o crear uno con el rol seleccionado
          val existingProfile = repo.getUserProfile(user.uid)
          val effectiveRole = existingProfile?.role ?: selectedRole

          if (existingProfile == null) {
            val newProfile = UserProfile(
              userId = user.uid,
              email = user.email ?: "",
              displayName = user.displayName ?: "Usuario",
              role = selectedRole,
              workshopName = if (selectedRole == "taller") "Taller Automotriz ${user.displayName ?: ""}" else ""
            )
            repo.saveUserProfile(newProfile)
          }

          onSuccess(effectiveRole)
        } else {
          onError("Error al obtener usuario de Firebase.")
        }
      } else {
        onError("Tipo de credencial no esperado.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Inicio de sesión con Google cancelado: ${e.message}", e)
      onCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Fallo en autenticación con Google", e)
      onError(e.localizedMessage ?: "Error en el inicio de sesión")
    }
  }
}

fun appSignOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onComplete: () -> Unit
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Error al limpiar credenciales: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onComplete()
      }
    }
  }
}
