package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.data.DemoData
import com.example.data.VincheckRepository
import com.example.data.models.InventoryItem
import com.example.data.models.Intervention
import com.example.data.models.Vehicle
import com.example.ui.components.BadgeType
import com.example.ui.components.EvidenceViewerDialog
import com.example.ui.components.HonestBadge
import com.example.ui.components.SignatureDialog
import com.example.ui.components.VinQrScannerDialog
import com.example.utils.LocationHelper
import com.example.utils.ValidationUtils
import com.example.utils.VinDecoder
import com.example.utils.WatermarkUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopScreen(
  userId: String,
  isDemoMode: Boolean,
  onSignOut: () -> Unit,
  onSwitchToOwner: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val repo = remember { VincheckRepository(context) }
  val credentialManager = remember { CredentialManager.create(context) }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Registrar Servicio", "Historial Taller", "Inventario Nube", "Diagnóstico OBD-II")

  // Intervenciones en tiempo real
  val workshopInterventionsFlow = remember(userId, isDemoMode) {
    if (!isDemoMode && userId.isNotBlank()) {
      repo.observeInterventionsForWorkshop(userId)
    } else null
  }
  val realInterventions by (workshopInterventionsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
  val demoInterventionsList = remember { mutableStateListOf(*DemoData.demoInterventions.toTypedArray()) }
  val displayedInterventions = if (isDemoMode) demoInterventionsList else realInterventions

  // Inventario en tiempo real
  val inventoryFlow = remember(userId, isDemoMode) {
    if (!isDemoMode && userId.isNotBlank()) {
      repo.observeInventory(userId)
    } else null
  }
  val realInventory by (inventoryFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
  val demoInventoryList = remember { mutableStateListOf(*DemoData.demoInventory.toTypedArray()) }
  val displayedInventory = if (isDemoMode) demoInventoryList else realInventory

  var inspectedIntervention by remember { mutableStateOf<Intervention?>(null) }

  // Estado para completar evidencia/firma en intervenciones existentes
  var pendingPhotoTarget by remember { mutableStateOf<Intervention?>(null) }
  var pendingSignatureTarget by remember { mutableStateOf<Intervention?>(null) }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ -> }

  val existingPhotoLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicturePreview()
  ) { rawBitmap ->
    val target = pendingPhotoTarget
    pendingPhotoTarget = null
    if (rawBitmap != null && target != null) {
      coroutineScope.launch {
        val gps = LocationHelper.getCurrentGpsCoordinates(context) ?: "Ubicación GPS: No disponible"
        val marked = WatermarkUtils.applyWatermark(
          original = rawBitmap,
          vin = target.vin,
          plate = target.plate,
          gpsText = gps
        )
        val base64 = WatermarkUtils.bitmapToBase64(marked, quality = 80)
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CL")).format(Date())

        if (isDemoMode) {
          val idx = demoInterventionsList.indexOfFirst { it.id == target.id }
          if (idx != -1) {
            demoInterventionsList[idx] = demoInterventionsList[idx].copy(
              photoBase64 = base64,
              photoGps = gps,
              photoTimestamp = timestamp,
              hasPhoto = true
            )
          }
          Toast.makeText(context, "Evidencia fotográfica agregada", Toast.LENGTH_SHORT).show()
        } else if (target.id.isNotBlank()) {
          repo.updateInterventionEvidence(
            interventionId = target.id,
            photoBase64 = base64,
            gps = gps,
            photoTimestamp = timestamp
          )
          Toast.makeText(context, "Evidencia sincronizada en Firestore", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "VINcheck",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(
                text = "• Espacio del Taller",
                fontSize = 13.sp,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(
              text = if (isDemoMode) "Taller Mecánico San Joaquín (Modo Demo)" else "Taller Conectado a Firestore Enterprise",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
        },
        actions = {
          if (isDemoMode) {
            HonestBadge(type = BadgeType.DEMO_EXAMPLE, customText = "Demo")
            Spacer(modifier = Modifier.size(4.dp))
          }
          IconButton(
            onClick = onSwitchToOwner,
            modifier = Modifier.testTag("switch_to_owner_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Cambiar a Propietario",
              tint = Color(0xFFF59E0B)
            )
          }
          IconButton(
            onClick = {
              if (isDemoMode) {
                onSignOut()
              } else {
                appSignOut(credentialManager, coroutineScope, onSignOut)
              }
            },
            modifier = Modifier.testTag("sign_out_workshop_button")
          ) {
            Icon(
              imageVector = Icons.Default.ExitToApp,
              contentDescription = "Cerrar sesión",
              tint = Color(0xFFEF4444)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(Color(0xFFF8FAFC))
    ) {
      // PESTAÑAS DEL TALLER
      PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.White,
        contentColor = Color(0xFF2563EB)
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTabIndex == index,
            onClick = { selectedTabIndex = index },
            text = {
              Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      // CONTENIDO DE PESTAÑAS
      Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTabIndex) {
          0 -> RegistrarIntervencionTab(
            userId = userId,
            isDemoMode = isDemoMode,
            inventory = displayedInventory,
            repo = repo,
            onInterventionSaved = { newIntervention, consumedItemIds ->
              if (isDemoMode) {
                demoInterventionsList.add(0, newIntervention)
                consumedItemIds.forEach { itemId ->
                  val idx = demoInventoryList.indexOfFirst { it.id == itemId }
                  if (idx != -1 && demoInventoryList[idx].stock > 0) {
                    demoInventoryList[idx] = demoInventoryList[idx].copy(
                      stock = demoInventoryList[idx].stock - 1
                    )
                  }
                }
              } else {
                coroutineScope.launch {
                  consumedItemIds.forEach { itemId ->
                    val item = realInventory.firstOrNull { it.id == itemId }
                    if (item != null && item.stock > 0) {
                      repo.updateInventoryStock(item.id, item.stock - 1)
                    }
                  }
                }
              }
              Toast.makeText(context, "Intervención sincronizada con el Expediente Clínico", Toast.LENGTH_LONG).show()
              selectedTabIndex = 1
            }
          )
          1 -> HistorialTallerTab(
            interventions = displayedInterventions,
            isDemoMode = isDemoMode,
            onInspect = { inspectedIntervention = it },
            onAddPhoto = { item ->
              pendingPhotoTarget = item
              if (!LocationHelper.hasLocationPermission(context)) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
              }
              existingPhotoLauncher.launch(null)
            },
            onAddSignature = { item ->
              pendingSignatureTarget = item
            }
          )
          2 -> InventarioNubeTab(
            userId = userId,
            isDemoMode = isDemoMode,
            inventory = displayedInventory,
            repo = repo,
            onItemAdded = { newItem ->
              if (isDemoMode) {
                demoInventoryList.add(newItem)
              }
              Toast.makeText(context, "Repuesto agregado al inventario", Toast.LENGTH_SHORT).show()
            },
            onStockUpdated = { itemId, newStock ->
              if (isDemoMode) {
                val idx = demoInventoryList.indexOfFirst { it.id == itemId }
                if (idx != -1) {
                  demoInventoryList[idx] = demoInventoryList[idx].copy(stock = newStock)
                }
              }
            },
            onItemDeleted = { itemId ->
              if (isDemoMode) {
                demoInventoryList.removeAll { it.id == itemId }
              } else {
                coroutineScope.launch { repo.deleteInventoryItem(itemId) }
              }
              Toast.makeText(context, "Repuesto eliminado del inventario", Toast.LENGTH_SHORT).show()
            }
          )
          3 -> DiagnosticoTallerTab()
        }
      }
    }
  }

  inspectedIntervention?.let { intervention ->
    EvidenceViewerDialog(
      intervention = intervention,
      onDismiss = { inspectedIntervention = null }
    )
  }

  pendingSignatureTarget?.let { target ->
    SignatureDialog(
      onDismiss = { pendingSignatureTarget = null },
      onSignatureCaptured = { base64 ->
        if (isDemoMode) {
          val idx = demoInterventionsList.indexOfFirst { it.id == target.id }
          if (idx != -1) {
            demoInterventionsList[idx] = demoInterventionsList[idx].copy(
              signatureBase64 = base64,
              hasSignature = true
            )
          }
          Toast.makeText(context, "Firma digital guardada", Toast.LENGTH_SHORT).show()
        } else if (target.id.isNotBlank()) {
          coroutineScope.launch {
            repo.updateInterventionEvidence(
              interventionId = target.id,
              signatureBase64 = base64
            )
            Toast.makeText(context, "Firma sincronizada en Firestore", Toast.LENGTH_SHORT).show()
          }
        }
        pendingSignatureTarget = null
      }
    )
  }
}

@Composable
private fun RegistrarIntervencionTab(
  userId: String,
  isDemoMode: Boolean,
  inventory: List<InventoryItem>,
  repo: VincheckRepository,
  onInterventionSaved: (Intervention, List<String>) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var vinInput by remember { mutableStateOf("1HGCR2F83HA102938") }
  var plateInput by remember { mutableStateOf("BB-BB-10") }
  var mileageInput by remember { mutableStateOf("45200") }
  var dateInput by remember {
    mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date()))
  }
  var workDoneInput by remember { mutableStateOf("Cambio de pastillas de freno cerámicas y fluido DOT 4") }
  var partsUsedInput by remember { mutableStateOf("Pastillas Bosch BOSCH-PAD-01") }

  var resolvedVehicle by remember { mutableStateOf<Vehicle?>(null) }
  var vinLookupAttempted by remember { mutableStateOf(false) }
  var isSearchingVin by remember { mutableStateOf(false) }

  var selectedInventoryIds by remember { mutableStateOf<List<String>>(emptyList()) }
  var showInventoryPicker by remember { mutableStateOf(false) }

  var watermarkedPhotoBase64 by remember { mutableStateOf<String?>(null) }
  var watermarkedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var capturedGps by remember { mutableStateOf<String?>(null) }

  var signatureBase64 by remember { mutableStateOf<String?>(null) }
  var showSignatureDialog by remember { mutableStateOf(false) }

  var isSaving by remember { mutableStateOf(false) }
  var isProcessingPhoto by remember { mutableStateOf(false) }
  var showVinScannerDialog by remember { mutableStateOf(false) }

  val vinBreakdown = remember(vinInput) { VinDecoder.decode(vinInput) }

  fun performVinLookup(targetVin: String) {
    val cleanVin = targetVin.uppercase().trim()
    if (!ValidationUtils.isValidVin(cleanVin)) {
      Toast.makeText(context, "Ingrese un VIN válido de 17 caracteres (sin I, O, Q)", Toast.LENGTH_SHORT).show()
      return
    }
    isSearchingVin = true
    if (isDemoMode) {
      val found = DemoData.demoVehicles.firstOrNull { it.vin == cleanVin }
      resolvedVehicle = found
      if (found != null) {
        plateInput = found.plate
        mileageInput = found.mileage.toString()
      }
      vinLookupAttempted = true
      isSearchingVin = false
    } else {
      coroutineScope.launch {
        val found = repo.getVehicleByVin(cleanVin)
        resolvedVehicle = found
        if (found != null) {
          plateInput = found.plate
          mileageInput = found.mileage.toString()
        }
        vinLookupAttempted = true
        isSearchingVin = false
      }
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ -> }

  val cameraLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicturePreview()
  ) { rawBitmap ->
    if (rawBitmap != null) {
      isProcessingPhoto = true
      coroutineScope.launch {
        val gps = LocationHelper.getCurrentGpsCoordinates(context) ?: "Ubicación GPS: No disponible"
        capturedGps = gps

        val markedBitmap = WatermarkUtils.applyWatermark(
          original = rawBitmap,
          vin = vinInput,
          plate = plateInput,
          gpsText = gps
        )
        watermarkedPhotoBitmap = markedBitmap
        watermarkedPhotoBase64 = WatermarkUtils.bitmapToBase64(markedBitmap, quality = 80)
        isProcessingPhoto = false
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Registro de Intervención Clínica",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF0F172A)
          )
          Text(
            text = "Expediente Nube",
            fontSize = 11.sp,
            color = Color(0xFF16A34A),
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // VIN + BOTÓN DE BÚSQUEDA EN NUBE
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = vinInput,
            onValueChange = {
              vinInput = it.uppercase()
              vinLookupAttempted = false
            },
            label = { Text("VIN del Vehículo (17 caracteres)") },
            modifier = Modifier
              .weight(1f)
              .testTag("intervention_vin_input"),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true
          )

          Button(
            onClick = { performVinLookup(vinInput) },
            enabled = !isSearchingVin,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            modifier = Modifier
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .testTag("lookup_vin_button")
          ) {
            if (isSearchingVin) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Search, contentDescription = "Buscar VIN en nube", modifier = Modifier.size(18.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = { showVinScannerDialog = true },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("scan_vin_qr_button")
        ) {
          Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Escanear código QR del VIN (CameraX)",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }

        // Resultado de búsqueda VIN + Decodificación ISO 3779
        AnimatedVisibility(visible = vinBreakdown != null) {
          vinBreakdown?.let { info ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Column {
                Text(
                  text = "Decodificación ISO 3779: ${info.manufacturerHint} • ${info.regionOrCountry} • Año ${info.modelYearHint}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF1E3A8A)
                )
                if (vinLookupAttempted) {
                  Spacer(modifier = Modifier.height(4.dp))
                  if (resolvedVehicle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.size(4.dp))
                      Text(
                        text = "Expediente vinculado: ${resolvedVehicle?.brand} ${resolvedVehicle?.model} (${resolvedVehicle?.plate}) — Sincroniza directo al propietario.",
                        fontSize = 11.sp,
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.Medium
                      )
                    }
                  } else {
                    Text(
                      text = "Vehículo aún sin propietario vinculado en la nube. Se guardará bajo el VIN para futura vinculación.",
                      fontSize = 11.sp,
                      color = Color(0xFF92400E)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = plateInput,
            onValueChange = { plateInput = it.uppercase() },
            label = { Text("Patente") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
          )
          OutlinedTextField(
            value = mileageInput,
            onValueChange = { mileageInput = it },
            label = { Text("Kilometraje (km)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = dateInput,
          onValueChange = { dateInput = it },
          label = { Text("Fecha de atención (DD/MM/AAAA)") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = workDoneInput,
          onValueChange = { workDoneInput = it },
          label = { Text("Trabajo realizado / Diagnóstico clínico") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = partsUsedInput,
          onValueChange = { partsUsedInput = it },
          label = { Text("Repuestos y fluidos instalados") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Selector rápido de repuestos desde Inventario Nube
        OutlinedButton(
          onClick = { showInventoryPicker = true },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("pick_from_inventory_button")
        ) {
          Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(6.dp))
          Text(
            text = if (selectedInventoryIds.isEmpty()) {
              "Seleccionar repuestos del Inventario (descuenta stock)"
            } else {
              "${selectedInventoryIds.size} repuesto(s) vinculado(s) del inventario"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = Color(0xFFE2E8F0))

        Spacer(modifier = Modifier.height(14.dp))

        // PRIORIDAD 2: BOTÓN REGISTRAR EVIDENCIA FOTOGRÁFICA
        Text(
          text = "Captura Real de Evidencia y Firma",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = Color(0xFF1E3A8A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = {
            if (!LocationHelper.hasLocationPermission(context)) {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            cameraLauncher.launch(null)
          },
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("capture_photo_evidence_button"),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = if (watermarkedPhotoBase64 != null) "Cambiar evidencia fotográfica" else "Registrar evidencia fotográfica (Cámara + Marca de Agua)",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }

        if (isProcessingPhoto) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Superponiendo marca de agua (VIN, Patente, GPS)...", fontSize = 11.sp, color = Color(0xFF64748B))
          }
        }

        watermarkedPhotoBitmap?.let { bitmap ->
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
          ) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = "Evidencia capturada",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color(0xFF16A34A), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("Marca de agua generada", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PRIORIDAD 2: BOTÓN SOLICITAR FIRMA DIGITAL
        OutlinedButton(
          onClick = { showSignatureDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("request_digital_signature_button"),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = if (signatureBase64 != null) "Actualizar firma del cliente" else "Solicitar firma digital (Lienzo en pantalla)",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }

        if (signatureBase64 != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(
              text = "Firma digital del cliente registrada",
              color = Color(0xFF16A34A),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // BOTÓN GUARDAR EN EXPEDIENTE DIGITAL
        Button(
          onClick = {
            if (!ValidationUtils.isValidVin(vinInput)) {
              Toast.makeText(context, "El VIN debe tener 17 caracteres válidos (sin I, O, Q)", Toast.LENGTH_SHORT).show()
              return@Button
            }
            if (workDoneInput.isBlank()) {
              Toast.makeText(context, "Describa el trabajo realizado", Toast.LENGTH_SHORT).show()
              return@Button
            }

            val parsedMileage = mileageInput.toIntOrNull() ?: 45000
            val newIntervention = Intervention(
              id = if (isDemoMode) "int_demo_${System.currentTimeMillis()}" else "",
              vin = vinInput.uppercase().trim(),
              plate = ValidationUtils.formatChileanPlate(plateInput),
              workshopId = userId.ifBlank { "workshop_demo" },
              workshopName = "Taller Mecánico San Joaquín (Duoc UC)",
              ownerId = resolvedVehicle?.ownerId ?: if (isDemoMode) "owner_demo" else "",
              date = dateInput,
              mileage = parsedMileage,
              workDone = workDoneInput.trim(),
              partsUsed = partsUsedInput.trim(),
              photoBase64 = watermarkedPhotoBase64 ?: "",
              photoGps = capturedGps ?: "Ubicación GPS: No disponible",
              photoTimestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CL")).format(Date()),
              hasPhoto = watermarkedPhotoBase64 != null,
              signatureBase64 = signatureBase64 ?: "",
              hasSignature = signatureBase64 != null,
              signatureDisclaimer = "Trazo capturado en el dispositivo, no una firma electrónica certificada"
            )

            isSaving = true
            if (isDemoMode) {
              onInterventionSaved(newIntervention, selectedInventoryIds)
              isSaving = false
            } else {
              coroutineScope.launch {
                val res = repo.createIntervention(newIntervention)
                isSaving = false
                if (res.isSuccess) {
                  onInterventionSaved(newIntervention.copy(id = res.getOrDefault("")), selectedInventoryIds)
                } else {
                  Toast.makeText(context, "Error al guardar en Firestore: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
              }
            }
          },
          enabled = !isSaving,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("save_intervention_cloud_button")
        ) {
          if (isSaving) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
          } else {
            Text("Guardar en Expediente Digital (Nube)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }
      }
    }
  }

  if (showSignatureDialog) {
    SignatureDialog(
      onDismiss = { showSignatureDialog = false },
      onSignatureCaptured = { base64 ->
        signatureBase64 = base64
        showSignatureDialog = false
      }
    )
  }

  if (showInventoryPicker) {
    InventoryPickerDialog(
      inventory = inventory,
      initiallySelected = selectedInventoryIds,
      onDismiss = { showInventoryPicker = false },
      onConfirm = { pickedIds, summaryText ->
        selectedInventoryIds = pickedIds
        if (summaryText.isNotBlank()) {
          partsUsedInput = summaryText
        }
        showInventoryPicker = false
      }
    )
  }

  if (showVinScannerDialog) {
    VinQrScannerDialog(
      onDismiss = { showVinScannerDialog = false },
      onVinScanned = { scannedVin ->
        vinInput = scannedVin
        vinLookupAttempted = false
        showVinScannerDialog = false
        Toast.makeText(context, "VIN escaneado: $scannedVin", Toast.LENGTH_SHORT).show()
        performVinLookup(scannedVin)
      }
    )
  }
}

@Composable
private fun InventoryPickerDialog(
  inventory: List<InventoryItem>,
  initiallySelected: List<String>,
  onDismiss: () -> Unit,
  onConfirm: (selectedIds: List<String>, partsSummary: String) -> Unit
) {
  val selectedSet = remember { mutableStateListOf(*initiallySelected.toTypedArray()) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Text(
          text = "Seleccionar Repuestos del Inventario",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Al guardar la intervención se descontará 1 unidad por cada ítem seleccionado.",
          fontSize = 11.sp,
          color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (inventory.isEmpty()) {
          Text(
            text = "No hay repuestos registrados en el inventario.",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(vertical = 16.dp)
          )
        } else {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp)
              .verticalScroll(rememberScrollState())
          ) {
            inventory.forEach { item ->
              val isChecked = selectedSet.contains(item.id)
              val hasStock = item.stock > 0
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(enabled = hasStock) {
                    if (isChecked) selectedSet.remove(item.id) else selectedSet.add(item.id)
                  }
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { checked ->
                    if (hasStock) {
                      if (checked) selectedSet.add(item.id) else selectedSet.remove(item.id)
                    }
                  },
                  enabled = hasStock
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasStock) Color(0xFF0F172A) else Color(0xFF94A3B8)
                  )
                  Text(
                    text = "SKU: ${item.partNumber} • Stock: ${item.stock} u.",
                    fontSize = 11.sp,
                    color = if (hasStock) Color(0xFF64748B) else Color(0xFFDC2626)
                  )
                }
              }
              HorizontalDivider(color = Color(0xFFF1F5F9))
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss) {
            Text("Cancelar")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val summary = inventory
                .filter { selectedSet.contains(it.id) }
                .joinToString(" + ") { "${it.name} (Ref: ${it.partNumber})" }
              onConfirm(selectedSet.toList(), summary)
            }
          ) {
            Text("Aplicar")
          }
        }
      }
    }
  }
}

@Composable
private fun HistorialTallerTab(
  interventions: List<Intervention>,
  isDemoMode: Boolean,
  onInspect: (Intervention) -> Unit,
  onAddPhoto: (Intervention) -> Unit,
  onAddSignature: (Intervention) -> Unit
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var showHistoryScannerDialog by remember { mutableStateOf(false) }
  val filtered = remember(interventions, searchQuery) {
    if (searchQuery.isBlank()) interventions
    else interventions.filter {
      it.vin.contains(searchQuery, ignoreCase = true) ||
        it.plate.contains(searchQuery, ignoreCase = true) ||
        it.workDone.contains(searchQuery, ignoreCase = true)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Buscar por VIN, patente o trabajo...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        IconButton(
          onClick = { showHistoryScannerDialog = true },
          modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .testTag("scan_history_vin_qr_button")
        ) {
          Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Escanear QR de VIN para buscar historial",
            tint = Color(0xFF2563EB)
          )
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Intervenciones Realizadas (${filtered.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF0F172A)
          )
          if (isDemoMode) {
            HonestBadge(type = BadgeType.DEMO_EXAMPLE, customText = "Demo")
          } else {
            Text("Firestore En Vivo", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
          }
        }
      }

      items(filtered, key = { it.id.ifBlank { it.date + it.mileage + it.vin } }) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onInspect(item) }
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "VIN: ${item.vin}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0F172A)
              )
              Text(
                text = "${item.mileage} km",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF2563EB)
              )
            }

            Text(
              text = "Patente: ${item.plate}  |  Fecha: ${item.date}",
              fontSize = 12.sp,
              color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = item.workDone,
              fontSize = 13.sp,
              color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.hasPhoto) {
                  HonestBadge(type = BadgeType.EVIDENCE_ATTACHED, customText = "Foto con GPS")
                } else {
                  HonestBadge(type = BadgeType.EVIDENCE_MISSING, customText = "Sin foto")
                }

                if (item.hasSignature) {
                  HonestBadge(type = BadgeType.SIGNATURE_SIGNED, customText = "Firmado")
                } else {
                  HonestBadge(type = BadgeType.SIGNATURE_PENDING, customText = "Sin firma")
                }
              }

              Text(
                text = "Ver detalles >",
                fontSize = 11.sp,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.SemiBold
              )
            }

            // Acciones rápidas para completar auditoría pendiente
            if (!item.hasPhoto || !item.hasSignature) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (!item.hasPhoto) {
                  OutlinedButton(
                    onClick = { onAddPhoto(item) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 48.dp)
                  ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjuntar Foto", fontSize = 11.sp)
                  }
                }
                if (!item.hasSignature) {
                  OutlinedButton(
                    onClick = { onAddSignature(item) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 48.dp)
                  ) {
                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Capturar Firma", fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showHistoryScannerDialog) {
    VinQrScannerDialog(
      onDismiss = { showHistoryScannerDialog = false },
      onVinScanned = { scannedVin ->
        searchQuery = scannedVin
        showHistoryScannerDialog = false
        Toast.makeText(context, "Historial filtrado por VIN: $scannedVin", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

@Composable
private fun InventarioNubeTab(
  userId: String,
  isDemoMode: Boolean,
  inventory: List<InventoryItem>,
  repo: VincheckRepository,
  onItemAdded: (InventoryItem) -> Unit,
  onStockUpdated: (itemId: String, newStock: Int) -> Unit,
  onItemDeleted: (itemId: String) -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var showAddDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("Todos") }

  val filterOptions = listOf("Todos", "Stock Crítico", "Frenos", "Filtros", "Lubricantes", "Encendido")

  val filteredInventory = remember(inventory, searchQuery, selectedFilter) {
    inventory.filter { item ->
      val matchesSearch = searchQuery.isBlank() ||
        item.name.contains(searchQuery, ignoreCase = true) ||
        item.partNumber.contains(searchQuery, ignoreCase = true)
      val matchesFilter = when (selectedFilter) {
        "Todos" -> true
        "Stock Crítico" -> item.stock <= item.minStock
        else -> item.category.equals(selectedFilter, ignoreCase = true)
      }
      matchesSearch && matchesFilter
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Inventario de Repuestos del Taller",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = Color(0xFF0F172A)
        )
        Text(
          text = if (isDemoMode) "Persistente en sesión de demostración" else "Persistente en Firestore Cloud Database",
          fontSize = 11.sp,
          color = Color(0xFF16A34A),
          fontWeight = FontWeight.Medium
        )
      }

      Button(
        onClick = { showAddDialog = true },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("add_part_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(4.dp))
        Text("Repuesto", fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Buscar por nombre o SKU...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      filterOptions.forEach { option ->
        FilterChip(
          selected = selectedFilter == option,
          onClick = { selectedFilter = option },
          label = { Text(option, fontSize = 11.sp) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(filteredInventory, key = { it.id.ifBlank { it.partNumber } }) { item ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
              )
              Text(
                text = "SKU: ${item.partNumber}  |  Categoría: ${item.category}",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
              )
              Text(
                text = "$ ${String.format(Locale.US, "%,d", item.unitPrice)} CLP",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2563EB)
              )
              if (item.stock <= item.minStock) {
                Text(
                  text = "Alerta stock bajo (mín: ${item.minStock})",
                  fontSize = 10.sp,
                  color = Color(0xFFDC2626),
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Stock: ${item.stock} u.",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (item.stock <= item.minStock) Color(0xFFDC2626) else Color(0xFF16A34A)
              )

              Spacer(modifier = Modifier.height(4.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                  onClick = {
                    if (item.stock > 0) {
                      val newStock = item.stock - 1
                      onStockUpdated(item.id, newStock)
                      if (!isDemoMode && item.id.isNotBlank()) {
                        coroutineScope.launch { repo.updateInventoryStock(item.id, newStock) }
                      }
                    }
                  },
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                  onClick = {
                    val newStock = item.stock + 1
                    onStockUpdated(item.id, newStock)
                    if (!isDemoMode && item.id.isNotBlank()) {
                      coroutineScope.launch { repo.updateInventoryStock(item.id, newStock) }
                    }
                  },
                  modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                IconButton(
                  onClick = { onItemDeleted(item.id) }
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar repuesto",
                    tint = Color(0xFF94A3B8)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddInventoryDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { partNum, name, cat, stock, minStock, price ->
        val newItem = InventoryItem(
          id = if (isDemoMode) "inv_demo_${System.currentTimeMillis()}" else "",
          workshopId = userId.ifBlank { "workshop_demo" },
          partNumber = partNum,
          name = name,
          category = cat,
          stock = stock,
          minStock = minStock,
          unitPrice = price
        )
        if (isDemoMode) {
          onItemAdded(newItem)
        } else {
          coroutineScope.launch {
            repo.addInventoryItem(newItem)
          }
        }
        showAddDialog = false
      }
    )
  }
}

@Composable
private fun AddInventoryDialog(
  onDismiss: () -> Unit,
  onConfirm: (partNum: String, name: String, cat: String, stock: Int, minStock: Int, price: Int) -> Unit
) {
  var partNumber by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Frenos") }
  var stockStr by remember { mutableStateOf("10") }
  var minStockStr by remember { mutableStateOf("3") }
  var priceStr by remember { mutableStateOf("25000") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = "Agregar Repuesto al Inventario",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = partNumber,
          onValueChange = { partNumber = it.uppercase() },
          label = { Text("Código de Parte / SKU") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre del Repuesto") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Categoría (ej: Frenos, Filtros, Motor)") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = stockStr,
            onValueChange = { stockStr = it },
            label = { Text("Stock") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = minStockStr,
            onValueChange = { minStockStr = it },
            label = { Text("Mínimo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = priceStr,
          onValueChange = { priceStr = it },
          label = { Text("Precio Unitario ($ CLP)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss) {
            Text("Cancelar")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank() && partNumber.isNotBlank()) {
                onConfirm(
                  partNumber.trim().uppercase(),
                  name.trim(),
                  category.trim(),
                  stockStr.toIntOrNull() ?: 1,
                  minStockStr.toIntOrNull() ?: 2,
                  priceStr.toIntOrNull() ?: 10000
                )
              }
            }
          ) {
            Text("Guardar Repuesto")
          }
        }
      }
    }
  }
}

@Composable
private fun DiagnosticoTallerTab() {
  var dtcSearch by remember { mutableStateOf("") }
  val filteredDtc = remember(dtcSearch) {
    if (dtcSearch.isBlank()) DemoData.demoDtcCodes
    else DemoData.demoDtcCodes.filter {
      it.code.contains(dtcSearch, ignoreCase = true) ||
        it.system.contains(dtcSearch, ignoreCase = true) ||
        it.description.contains(dtcSearch, ignoreCase = true)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Compatibilidad con Escáner de Taller",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          HonestBadge(type = BadgeType.OBD_DISCONNECTED)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Lectura Pasiva No Invasiva:",
          color = Color(0xFFF59E0B),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        )

        Text(
          text = "El sistema VINcheck no compite ni bloquea la línea K ni los pines CAN 6 y 14 con mensajes activos de interrogación cuando el taller conecta su escáner automotriz profesional (Launch, Autel, G-Scan). Solo monitoriza de forma pasiva, garantizando compatibilidad total durante diagnósticos mecánicos.",
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Interruptor de Hardware: Manual y físico en el cable de conexión (sin control por software).",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Guía Técnica de Códigos DTC",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color(0xFF0F172A)
      )
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HonestBadge(type = BadgeType.NO_REAL_READINGS)
        HonestBadge(type = BadgeType.DEMO_EXAMPLE, customText = "Guía Duoc UC")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = dtcSearch,
      onValueChange = { dtcSearch = it },
      placeholder = { Text("Buscar código DTC (ej: P0300, U0100, ABS)...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    filteredDtc.forEach { dtc ->
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = dtc.code,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              color = Color(0xFFDC2626)
            )
            Text(
              text = "Severidad: ${dtc.severity}",
              fontSize = 11.sp,
              color = Color(0xFF64748B),
              fontWeight = FontWeight.SemiBold
            )
          }

          Text(
            text = "Sistema: ${dtc.system}",
            fontSize = 11.sp,
            color = Color(0xFF475569)
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = dtc.description,
            fontSize = 12.sp,
            color = Color(0xFF1E293B)
          )

          Spacer(modifier = Modifier.height(6.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
              .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
              .padding(8.dp)
          ) {
            Column {
              Text(
                text = "Paso Siguiente de Diagnóstico Técnico:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
              )
              Text(
                text = dtc.nextStep,
                fontSize = 11.sp,
                color = Color(0xFF334155),
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    }
  }
}
