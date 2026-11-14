package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.data.DemoData
import com.example.data.VincheckRepository
import com.example.data.models.AlertUrgency
import com.example.data.models.Intervention
import com.example.data.models.Vehicle
import com.example.ui.components.BadgeType
import com.example.ui.components.EvidenceViewerDialog
import com.example.ui.components.HonestBadge
import com.example.ui.components.VinQrScannerDialog
import com.example.utils.ClinicalReportHelper
import com.example.utils.ValidationUtils
import com.example.utils.VinDecoder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreen(
  userId: String,
  isDemoMode: Boolean,
  onSignOut: () -> Unit,
  onSwitchToWorkshop: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val repo = remember { VincheckRepository(context) }
  val credentialManager = remember { CredentialManager.create(context) }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Expediente", "Alertas Km", "Módulo OBD-II", "Suscripción & Auto")

  // Vehículos
  val ownerVehiclesFlow = remember(userId, isDemoMode) {
    if (!isDemoMode && userId.isNotBlank()) {
      repo.observeVehiclesForOwner(userId)
    } else null
  }
  val realVehicles by (ownerVehiclesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
  val demoVehiclesList = remember { mutableStateListOf(*DemoData.demoVehicles.toTypedArray()) }
  val currentVehicles = if (isDemoMode) demoVehiclesList else realVehicles

  var selectedVehicleIndex by remember { mutableIntStateOf(0) }
  val selectedVehicle = currentVehicles.getOrNull(selectedVehicleIndex) ?: currentVehicles.firstOrNull()

  // Intervenciones en tiempo real desde Firestore
  val ownerInterventionsFlow = remember(userId, isDemoMode) {
    if (!isDemoMode && userId.isNotBlank()) {
      repo.observeInterventionsForOwner(userId)
    } else null
  }
  val realInterventions by (ownerInterventionsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
  val demoInterventionsList = remember { mutableStateListOf(*DemoData.demoInterventions.toTypedArray()) }

  val displayedInterventions = if (isDemoMode) {
    demoInterventionsList.filter { selectedVehicle == null || it.vin == selectedVehicle.vin }
  } else {
    realInterventions.filter { selectedVehicle == null || it.vin == selectedVehicle.vin }
  }

  var inspectedIntervention by remember { mutableStateOf<Intervention?>(null) }
  var showMileageDialog by remember { mutableStateOf(false) }
  var showVinDecodeDialog by remember { mutableStateOf(false) }

  val currentAlerts = remember(selectedVehicle?.mileage) {
    DemoData.calculateMaintenanceAlerts(selectedVehicle?.mileage ?: 45000)
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
                text = "• Espacio Propietario",
                fontSize = 13.sp,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(
              text = if (isDemoMode) "Modo Demostración Académica (Duoc UC)" else "Sincronizado en tiempo real con Firestore",
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
            onClick = {
              ClinicalReportHelper.shareClinicalRecord(
                context = context,
                vehicle = selectedVehicle,
                interventions = displayedInterventions,
                alerts = currentAlerts,
                isDemoMode = isDemoMode
              )
            },
            modifier = Modifier.testTag("share_record_button")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Compartir expediente clínico",
              tint = Color(0xFF38BDF8)
            )
          }
          IconButton(
            onClick = onSwitchToWorkshop,
            modifier = Modifier.testTag("switch_to_workshop_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Cambiar a Taller",
              tint = Color(0xFF60A5FA)
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
            modifier = Modifier.testTag("sign_out_button")
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
      // VEHICLE SELECTOR CARD
      VehicleHeaderCard(
        vehicle = selectedVehicle,
        totalVehicles = currentVehicles.size,
        onNextVehicle = {
          if (currentVehicles.isNotEmpty()) {
            selectedVehicleIndex = (selectedVehicleIndex + 1) % currentVehicles.size
          }
        },
        onEditMileage = {
          if (selectedVehicle != null) showMileageDialog = true
        },
        onShowVinDetails = {
          if (selectedVehicle != null) showVinDecodeDialog = true
        }
      )

      // PESTAÑAS
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
          0 -> ExpedienteTabContent(
            interventions = displayedInterventions,
            vehicle = selectedVehicle,
            alertsCount = currentAlerts.count { it.urgency != AlertUrgency.OK },
            isDemoMode = isDemoMode,
            onInspect = { inspectedIntervention = it },
            onShareRecord = {
              ClinicalReportHelper.shareClinicalRecord(
                context = context,
                vehicle = selectedVehicle,
                interventions = displayedInterventions,
                alerts = currentAlerts,
                isDemoMode = isDemoMode
              )
            }
          )
          1 -> AlertasKmTabContent(
            vehicle = selectedVehicle,
            isDemoMode = isDemoMode,
            onEditMileage = {
              if (selectedVehicle != null) showMileageDialog = true
            }
          )
          2 -> Obd2TabContent()
          3 -> SuscripcionYVehiculoTabContent(
            userId = userId,
            isDemoMode = isDemoMode,
            vehicles = currentVehicles,
            selectedIndex = selectedVehicleIndex,
            repo = repo,
            onSelectVehicle = { idx ->
              selectedVehicleIndex = idx
              selectedTabIndex = 0
            },
            onVehicleCreated = { newVeh ->
              if (isDemoMode) {
                demoVehiclesList.add(newVeh)
              }
              selectedVehicleIndex = (currentVehicles.size).coerceAtLeast(0)
              Toast.makeText(context, "Vehículo registrado en el expediente", Toast.LENGTH_SHORT).show()
            },
            onDeleteVehicle = { veh ->
              if (isDemoMode) {
                demoVehiclesList.removeAll { it.vin == veh.vin }
                selectedVehicleIndex = 0
              } else {
                coroutineScope.launch { repo.deleteVehicle(veh.vin) }
              }
              Toast.makeText(context, "Vehículo retirado de la lista", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }

  // DIALOG PARA VER EVIDENCIA Y FIRMA
  inspectedIntervention?.let { intervention ->
    EvidenceViewerDialog(
      intervention = intervention,
      onDismiss = { inspectedIntervention = null }
    )
  }

  // DIALOG PARA ACTUALIZAR KILOMETRAJE REAL
  if (showMileageDialog && selectedVehicle != null) {
    UpdateMileageDialog(
      currentMileage = selectedVehicle.mileage,
      onDismiss = { showMileageDialog = false },
      onConfirm = { newKm ->
        if (isDemoMode) {
          val idx = demoVehiclesList.indexOfFirst { it.vin == selectedVehicle.vin }
          if (idx != -1) {
            demoVehiclesList[idx] = demoVehiclesList[idx].copy(mileage = newKm)
          }
          Toast.makeText(context, "Odómetro actualizado a $newKm km", Toast.LENGTH_SHORT).show()
        } else {
          coroutineScope.launch {
            repo.updateVehicleMileage(selectedVehicle.vin, newKm)
            Toast.makeText(context, "Odómetro sincronizado en Firestore", Toast.LENGTH_SHORT).show()
          }
        }
        showMileageDialog = false
      }
    )
  }

  // DIALOG DECODIFICACIÓN ISO 3779 DEL VIN
  if (showVinDecodeDialog && selectedVehicle != null) {
    VinDecodeDialog(
      vehicle = selectedVehicle,
      onDismiss = { showVinDecodeDialog = false }
    )
  }
}

@Composable
private fun VehicleHeaderCard(
  vehicle: Vehicle?,
  totalVehicles: Int,
  onNextVehicle: () -> Unit,
  onEditMileage: () -> Unit,
  onShowVinDetails: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(0.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xFF334155))
          .clickable(enabled = vehicle != null) { onShowVinDetails() },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.DirectionsCar,
          contentDescription = "Ver decodificación VIN",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(26.dp)
        )
      }

      Spacer(modifier = Modifier.size(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        if (vehicle != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${vehicle.brand} ${vehicle.model} (${vehicle.year})",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Decodificación ISO 3779",
              tint = Color(0xFF94A3B8),
              modifier = Modifier
                .size(16.dp)
                .clickable { onShowVinDetails() }
            )
          }
          Text(
            text = "Patente: ${vehicle.plate}  |  VIN: ${vehicle.vin}",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clickable { onEditMileage() }
              .padding(top = 2.dp)
          ) {
            Text(
              text = "Kilometraje registrado: ${vehicle.mileage} km",
              color = Color(0xFF38BDF8),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Actualizar odómetro",
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(12.dp)
            )
          }
        } else {
          Text(
            text = "Sin vehículos registrados",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "Registre su patente y VIN en la pestaña 'Suscripción & Auto'",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
          )
        }
      }

      if (totalVehicles > 1) {
        IconButton(onClick = onNextVehicle) {
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Siguiente vehículo",
            tint = Color(0xFFCBD5E1)
          )
        }
      }
    }
  }
}

@Composable
private fun ExpedienteTabContent(
  interventions: List<Intervention>,
  vehicle: Vehicle?,
  alertsCount: Int,
  isDemoMode: Boolean,
  onInspect: (Intervention) -> Unit,
  onShareRecord: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var filterMode by remember { mutableStateOf("Todas") }
  val filterOptions = listOf("Todas", "Con Evidencia", "Pendientes")

  val filtered = remember(interventions, searchQuery, filterMode) {
    interventions.filter { item ->
      val matchesText = searchQuery.isBlank() ||
        item.workDone.contains(searchQuery, ignoreCase = true) ||
        item.workshopName.contains(searchQuery, ignoreCase = true) ||
        item.partsUsed.contains(searchQuery, ignoreCase = true)
      val matchesFilter = when (filterMode) {
        "Con Evidencia" -> item.hasPhoto && item.hasSignature
        "Pendientes" -> !item.hasPhoto || !item.hasSignature
        else -> true
      }
      matchesText && matchesFilter
    }
  }

  val verifiedCount = interventions.count { it.hasPhoto && it.hasSignature }

  Column(modifier = Modifier.fillMaxSize()) {
    // Resumen clínico rápido
    if (vehicle != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ClinicalStatChip(
          label = "Servicios",
          value = "${interventions.size}",
          accent = Color(0xFF2563EB),
          modifier = Modifier.weight(1f)
        )
        ClinicalStatChip(
          label = "Con Auditoría",
          value = "$verifiedCount/${interventions.size}",
          accent = Color(0xFF16A34A),
          modifier = Modifier.weight(1f)
        )
        ClinicalStatChip(
          label = "Alertas Km",
          value = "$alertsCount",
          accent = if (alertsCount > 0) Color(0xFFD97706) else Color(0xFF16A34A),
          modifier = Modifier.weight(1f)
        )
      }
    }

    if (interventions.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "No hay intervenciones clínicas registradas para este VIN",
          fontWeight = FontWeight.Bold,
          color = Color(0xFF334155),
          fontSize = 15.sp,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Cuando el taller registre un servicio para el VIN ${vehicle?.vin ?: "del vehículo"}, aparecerá automáticamente aquí en tiempo real.",
          fontSize = 12.sp,
          color = Color(0xFF64748B),
          textAlign = TextAlign.Center
        )
      }
    } else {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Filtrar historial por taller, trabajo o repuesto...", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          filterOptions.forEach { option ->
            FilterChip(
              selected = filterMode == option,
              onClick = { filterMode = option },
              label = { Text(option, fontSize = 11.sp) }
            )
          }
          OutlinedButton(
            onClick = onShareRecord,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Exportar Ficha", fontSize = 11.sp)
          }
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp)
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
              text = "Historial Clínico Automotriz (${filtered.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color(0xFF0F172A)
            )
            if (isDemoMode) {
              HonestBadge(type = BadgeType.DEMO_EXAMPLE, customText = "Datos Demo")
            } else {
              Text(
                text = "Firestore Sincronizado",
                fontSize = 11.sp,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        items(filtered, key = { it.id.ifBlank { it.date + it.mileage } }) { intervention ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .clickable { onInspect(intervention) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = intervention.date,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Color(0xFF0F172A)
                )
                Text(
                  text = "${intervention.mileage} km",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = Color(0xFF2563EB)
                )
              }

              Text(
                text = "Taller: ${intervention.workshopName}",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = intervention.workDone,
                fontSize = 13.sp,
                color = Color(0xFF1E293B),
                lineHeight = 17.sp
              )

              if (intervention.partsUsed.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Repuestos: ${intervention.partsUsed}",
                  fontSize = 11.sp,
                  color = Color(0xFF475569)
                )
              }

              Spacer(modifier = Modifier.height(10.dp))
              HorizontalDivider(color = Color(0xFFF1F5F9))
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  if (intervention.hasPhoto) {
                    HonestBadge(type = BadgeType.EVIDENCE_ATTACHED, customText = "Foto con GPS")
                  } else {
                    HonestBadge(type = BadgeType.EVIDENCE_MISSING, customText = "Sin foto")
                  }

                  if (intervention.hasSignature) {
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
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ClinicalStatChip(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = accent
      )
      Text(
        text = label,
        fontSize = 10.sp,
        color = Color(0xFF64748B),
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
private fun AlertasKmTabContent(
  vehicle: Vehicle?,
  isDemoMode: Boolean,
  onEditMileage: () -> Unit
) {
  val context = LocalContext.current
  val km = vehicle?.mileage ?: 45000
  val alerts = remember(km) { DemoData.calculateMaintenanceAlerts(km) }
  val userEmail = remember { Firebase.auth.currentUser?.email ?: "ppanuel146@gmail.com" }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // INFO CARD DE KILOMETRAJE REAL
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.size(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Mantenimiento Preventivo por Uso Real",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color(0xFF1E3A8A)
            )
            Text(
              text = "Cálculo basado en odómetro de $km km (sin estimación de tiempo). Alertas vinculadas a notificación push y respaldo a correo electrónico institucional.",
              fontSize = 11.sp,
              color = Color(0xFF1E40AF),
              lineHeight = 15.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onEditMileage,
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Actualizar Km", fontSize = 12.sp)
          }

          Button(
            onClick = {
              ClinicalReportHelper.composeMaintenanceEmailAlert(
                context = context,
                recipientEmail = userEmail,
                vehicle = vehicle,
                alerts = alerts
              )
            },
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("send_email_alert_button")
          ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Correo Alerta", fontSize = 12.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Estado de Componentes de Desgaste:",
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      color = Color(0xFF0F172A)
    )

    Spacer(modifier = Modifier.height(8.dp))

    alerts.forEach { alert ->
      val (urgencyColor, urgencyBg, urgencyText) = when (alert.urgency) {
        AlertUrgency.OVERDUE -> Triple(Color(0xFFDC2626), Color(0xFFFEE2E2), "Vencido / Requiere Atención")
        AlertUrgency.DUE_SOON -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "Próximo a Vencer")
        AlertUrgency.OK -> Triple(Color(0xFF16A34A), Color(0xFFDCFCE7), "Al Día")
      }

      val progress = ((alert.intervalKm - alert.remainingKm).toFloat() / alert.intervalKm.toFloat())
        .coerceIn(0.05f, 1f)

      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 5.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = alert.component,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color(0xFF0F172A),
              modifier = Modifier.weight(1f)
            )

            Box(
              modifier = Modifier
                .background(urgencyBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = urgencyText,
                color = urgencyColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          LinearProgressIndicator(
            progress = { progress },
            color = urgencyColor,
            trackColor = Color(0xFFE2E8F0),
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Intervalo: cada ${alert.intervalKm} km | Próximo cambio a los ${alert.targetKm} km",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
          )

          Text(
            text = if (alert.remainingKm > 0) "Restan: ${alert.remainingKm} km de servicio" else "Excedido por: ${-alert.remainingKm} km",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = urgencyColor
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = alert.recommendation,
            fontSize = 11.sp,
            color = Color(0xFF334155),
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Email,
              contentDescription = "Correo",
              tint = Color(0xFF64748B),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
              text = "Alerta vinculada a email ($userEmail) y notificación push",
              fontSize = 10.sp,
              color = Color(0xFF64748B)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Obd2TabContent() {
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
    // BANNER EXPLICATIVO DE LECTURA PASIVA NO INVASIVA
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
            text = "Módulo OBD-II (Lectura Pasiva)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          HonestBadge(type = BadgeType.OBD_DISCONNECTED)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Principio de Funcionamiento No Invasivo:",
          color = Color(0xFFF59E0B),
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        )

        Text(
          text = "El hardware implementa un sniffer de bus CAN en modo escucha pasiva. Esto garantiza que el puerto OBD-II queda 100% libre para el escáner del taller mecánico durante diagnósticos activos, sin generar conflictos de bus ni bloquear peticiones OBD de diagnóstico profundo.",
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Interruptor de Hardware: Manual y físico en el arnés de conexión (solo lectura pasiva en la app).",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // AVISO HONESTO DE LECTURAS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Diagnóstico DTC de Referencia",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color(0xFF0F172A)
      )
      HonestBadge(type = BadgeType.NO_REAL_READINGS)
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = dtcSearch,
      onValueChange = { dtcSearch = it },
      placeholder = { Text("Buscar código DTC (ej: P0300, P0420, ABS)...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    // MICROCOPIA TÉCNICA DE LOS DTC CON DESCRIPCIÓN Y SIGUIENTE PASO
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
            HonestBadge(type = BadgeType.DEMO_EXAMPLE, customText = "Código de Ejemplo")
          }

          Text(
            text = "Sistema: ${dtc.system}",
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold
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
              .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
              .padding(8.dp)
          ) {
            Column {
              Text(
                text = "Siguiente paso de diagnóstico clínico:",
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

@Composable
private fun SuscripcionYVehiculoTabContent(
  userId: String,
  isDemoMode: Boolean,
  vehicles: List<Vehicle>,
  selectedIndex: Int,
  repo: VincheckRepository,
  onSelectVehicle: (Int) -> Unit,
  onVehicleCreated: (Vehicle) -> Unit,
  onDeleteVehicle: (Vehicle) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var plateInput by remember { mutableStateOf("") }
  var vinInput by remember { mutableStateOf("") }
  var brandInput by remember { mutableStateOf("") }
  var modelInput by remember { mutableStateOf("") }
  var yearInput by remember { mutableStateOf("2021") }
  var mileageInput by remember { mutableStateOf("45000") }

  var plateError by remember { mutableStateOf<String?>(null) }
  var vinError by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  var showOwnerVinScanner by remember { mutableStateOf(false) }

  val vinPreview = remember(vinInput) { VinDecoder.decode(vinInput) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // VEHÍCULOS REGISTRADOS
    if (vehicles.isNotEmpty()) {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Mis Vehículos Registrados (${vehicles.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF0F172A)
          )
          Spacer(modifier = Modifier.height(8.dp))
          vehicles.forEachIndexed { index, veh ->
            val isSelected = index == selectedIndex
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(
                  1.dp,
                  if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                  RoundedCornerShape(8.dp)
                )
                .background(
                  if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                  RoundedCornerShape(8.dp)
                )
                .clickable { onSelectVehicle(index) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${veh.brand} ${veh.model} (${veh.year})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color(0xFF0F172A)
                )
                Text(
                  text = "${veh.plate} • ${veh.vin} • ${veh.mileage} km",
                  fontSize = 11.sp,
                  color = Color(0xFF64748B)
                )
              }
              IconButton(onClick = { onDeleteVehicle(veh) }) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Eliminar vehículo",
                  tint = Color(0xFF94A3B8)
                )
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    // FORMULARIO DE REGISTRO DE VEHÍCULO
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = "Registrar Nuevo Vehículo al Expediente",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF0F172A)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = plateInput,
          onValueChange = {
            plateInput = it.uppercase()
            plateError = if (it.isNotBlank() && !ValidationUtils.isValidChileanPlate(it)) {
              "Formato de patente chilena inválido (ej: BB-BB-10 o AA-10-00)"
            } else null
          },
          label = { Text("Patente chilena (ej: BB-BB-10)") },
          isError = plateError != null,
          supportingText = { plateError?.let { Text(it, color = Color.Red, fontSize = 11.sp) } },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_plate_input"),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = vinInput,
          onValueChange = {
            vinInput = it.uppercase()
            vinError = if (it.isNotBlank() && !ValidationUtils.isValidVin(it)) {
              "El VIN debe tener 17 caracteres alfanuméricos válidos (sin I, O, Q)"
            } else null
          },
          label = { Text("Código VIN (17 caracteres)") },
          isError = vinError != null,
          supportingText = {
            if (vinError != null) {
              Text(vinError ?: "", color = Color.Red, fontSize = 11.sp)
            } else {
              Text("${vinInput.length}/17 caracteres", fontSize = 11.sp)
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_vin_input"),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )

        OutlinedButton(
          onClick = { showOwnerVinScanner = true },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("scan_owner_vin_qr_button")
        ) {
          Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Escanear QR del VIN (CameraX)",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnimatedVisibility(visible = vinPreview != null) {
          vinPreview?.let { info ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                .padding(8.dp)
            ) {
              Text(
                text = "VIN ISO 3779 detectado: ${info.manufacturerHint} • ${info.regionOrCountry} • Año modelo ${info.modelYearHint}",
                fontSize = 11.sp,
                color = Color(0xFF1E3A8A),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = brandInput,
            onValueChange = { brandInput = it },
            label = { Text("Marca (ej: Toyota)") },
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            label = { Text("Modelo (ej: Yaris)") },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = yearInput,
            onValueChange = { yearInput = it },
            label = { Text("Año") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = mileageInput,
            onValueChange = { mileageInput = it },
            label = { Text("Kilometraje (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            val validPlate = ValidationUtils.isValidChileanPlate(plateInput)
            val validVin = ValidationUtils.isValidVin(vinInput)

            if (!validPlate) {
              plateError = "Ingrese una patente chilena válida"
              return@Button
            }
            if (!validVin) {
              vinError = "El VIN debe contener 17 caracteres (sin I, O, Q)"
              return@Button
            }

            val cleanVin = vinInput.uppercase().trim()
            val newVehicle = Vehicle(
              id = cleanVin,
              vin = cleanVin,
              plate = ValidationUtils.formatChileanPlate(plateInput),
              brand = brandInput.ifBlank { vinPreview?.manufacturerHint ?: "Automóvil" },
              model = modelInput.ifBlank { "Estándar" },
              year = yearInput.toIntOrNull() ?: 2021,
              mileage = mileageInput.toIntOrNull() ?: 45000,
              ownerId = userId.ifBlank { "owner_demo" }
            )

            isSubmitting = true
            if (isDemoMode) {
              onVehicleCreated(newVehicle)
              isSubmitting = false
              plateInput = ""
              vinInput = ""
              brandInput = ""
              modelInput = ""
            } else {
              coroutineScope.launch {
                repo.registerVehicle(newVehicle)
                isSubmitting = false
                plateInput = ""
                vinInput = ""
                brandInput = ""
                modelInput = ""
                onVehicleCreated(newVehicle)
              }
            }
          },
          enabled = !isSubmitting,
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("save_vehicle_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
          } else {
            Text("Guardar Vehículo en Expediente")
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ESTRUCTURA DEL MODELO DE SUSCRIPCIÓN (Informe de título)
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Estructura del Modelo de Suscripción",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Diseño de monetización planteado en el proyecto de título 'Sistema Digital de Diagnóstico Automotriz':",
          fontSize = 12.sp,
          color = Color(0xFF475569)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "Plan Propietario (Expediente Clínico Particular)",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = Color(0xFF1E3A8A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "• Acceso continuo al historial clínico por VIN del vehículo\n• Cálculo automatizado de alertas preventivas por kilometraje real\n• Respaldo de órdenes de servicio, fotos periciales y firmas digitales\n• Telemetría pasiva no invasiva vía módulo OBD-II",
              fontSize = 11.sp,
              color = Color(0xFF334155),
              lineHeight = 16.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "Plan Taller / Concesionario Automotriz",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = Color(0xFFD97706)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "• Emisión de certificaciones digitales con validez por VIN\n• Captura de evidencia con marca de agua pericial (GPS + timestamp)\n• Registro de firma digital de conformidad del cliente en dispositivo\n• Gestión centralizada de inventario y trazabilidad de repuestos",
              fontSize = 11.sp,
              color = Color(0xFF334155),
              lineHeight = 16.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
            .padding(10.dp)
        ) {
          Text(
            text = "Aclaración académica del informe: Los valores monetarios definitivos se encuentran pendientes de cotización comercial según volumen de producción de los módulos de telemetría OBD-II.",
            fontSize = 11.sp,
            color = Color(0xFF92400E),
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }

  if (showOwnerVinScanner) {
    VinQrScannerDialog(
      onDismiss = { showOwnerVinScanner = false },
      onVinScanned = { scannedVin ->
        vinInput = scannedVin
        vinError = null
        showOwnerVinScanner = false
        Toast.makeText(context, "VIN escaneado: $scannedVin", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

@Composable
private fun UpdateMileageDialog(
  currentMileage: Int,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit
) {
  var mileageText by remember { mutableStateOf(currentMileage.toString()) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Actualizar Odómetro Real",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Al actualizar el kilometraje se recalculan automáticamente las alertas de mantenimiento preventivo.",
          fontSize = 12.sp,
          color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = mileageText,
          onValueChange = {
            mileageText = it
            errorMsg = null
          },
          label = { Text("Kilometraje actual (km)") },
          isError = errorMsg != null,
          supportingText = { errorMsg?.let { Text(it, color = Color.Red) } },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val parsed = mileageText.toIntOrNull()
              if (parsed == null || parsed < 0) {
                errorMsg = "Ingrese un kilometraje válido"
              } else {
                onConfirm(parsed)
              }
            }
          ) {
            Text("Guardar")
          }
        }
      }
    }
  }
}

@Composable
private fun VinDecodeDialog(
  vehicle: Vehicle,
  onDismiss: () -> Unit
) {
  val decoded = remember(vehicle.vin) { VinDecoder.decode(vehicle.vin) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Decodificación Técnica VIN (ISO 3779)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (decoded != null) {
          VinInfoRow("VIN Completo", decoded.vin)
          VinInfoRow("WMI (Fabricante)", "${decoded.wmi} — ${decoded.manufacturerHint}")
          VinInfoRow("Región / Origen", decoded.regionOrCountry)
          VinInfoRow("VDS (Descriptor)", decoded.vds)
          VinInfoRow("Año Modelo (Pos. 10)", decoded.modelYearHint)
          VinInfoRow("Planta y Serie (VIS)", decoded.plantAndSerial)
        } else {
          Text(
            text = "El VIN registrado (${vehicle.vin}) no cumple con el estándar de 17 caracteres ISO 3779.",
            fontSize = 13.sp,
            color = Color(0xFFDC2626)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onDismiss,
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Cerrar")
        }
      }
    }
  }
}

@Composable
private fun VinInfoRow(label: String, value: String) {
  Column(modifier = Modifier.padding(vertical = 4.dp)) {
    Text(
      text = label,
      fontSize = 11.sp,
      color = Color(0xFF64748B),
      fontWeight = FontWeight.Medium
    )
    Text(
      text = value,
      fontSize = 13.sp,
      color = Color(0xFF0F172A),
      fontWeight = FontWeight.Bold
    )
    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(top = 4.dp))
  }
}
