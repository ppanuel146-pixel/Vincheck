package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.utils.ValidationUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun VinQrScannerDialog(
  onDismiss: () -> Unit,
  onVinScanned: (String) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasCameraPermission = granted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  var torchEnabled by remember { mutableStateOf(false) }
  var statusFeedback by remember { mutableStateOf<String?>(null) }
  var hasDeliveredResult by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp)
        .testTag("vin_qr_scanner_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Cabecera
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.QrCodeScanner,
              contentDescription = null,
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Escáner QR de VIN (CameraX)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
              Text(
                text = "Lectura óptica ISO 3779 (QR / Code 39 / Code 128)",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_qr_scanner_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cerrar escáner",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasCameraPermission) {
          // Estado sin permiso de cámara
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Permiso de cámara requerido",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Para escanear el código QR del VIN en el parabrisas, marco de puerta o padrón del vehículo, autorice el acceso a la cámara.",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(16.dp))
              Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
              ) {
                Text("Conceder permiso de cámara")
              }
            }
          }
        } else {
          // Vista previa de CameraX + MlKitAnalyzer
          val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
          val barcodeScanner = remember {
            val options = BarcodeScannerOptions.Builder()
              .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417
              )
              .build()
            BarcodeScanning.getClient(options)
          }

          val cameraController = remember(context) {
            LifecycleCameraController(context).apply {
              cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
              setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            }
          }

          DisposableEffect(lifecycleOwner, cameraController, barcodeScanner) {
            cameraController.setImageAnalysisAnalyzer(
              mainExecutor,
              MlKitAnalyzer(
                listOf(barcodeScanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainExecutor
              ) { result: MlKitAnalyzer.Result? ->
                if (hasDeliveredResult || result == null) return@MlKitAnalyzer
                val barcodes = result.getValue(barcodeScanner)
                if (!barcodes.isNullOrEmpty()) {
                  for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    val extractedVin = ValidationUtils.extractVinFromQrPayload(rawValue)
                    if (extractedVin != null) {
                      hasDeliveredResult = true
                      onVinScanned(extractedVin)
                      break
                    } else {
                      statusFeedback = "Lectura '$rawValue' no contiene un VIN válido de 17 caracteres"
                    }
                  }
                }
              }
            )

            try {
              cameraController.bindToLifecycle(lifecycleOwner)
            } catch (e: Exception) {
              Log.e("VinQrScanner", "Error binding CameraX LifecycleCameraController", e)
              statusFeedback = "No se pudo inicializar la cámara en este dispositivo"
            }

            onDispose {
              cameraController.clearImageAnalysisAnalyzer()
              cameraController.unbind()
              barcodeScanner.close()
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(340.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black)
          ) {
            AndroidView(
              factory = { ctx ->
                PreviewView(ctx).apply {
                  layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                  )
                  implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                  scaleType = PreviewView.ScaleType.FILL_CENTER
                  controller = cameraController
                }
              },
              update = { previewView ->
                if (previewView.controller !== cameraController) {
                  previewView.controller = cameraController
                }
              },
              modifier = Modifier.fillMaxSize()
            )

            // Retícula óptica de enfoque VIN
            Canvas(modifier = Modifier.fillMaxSize()) {
              val frameSize = size.minDimension * 0.68f
              val left = (size.width - frameSize) / 2f
              val top = (size.height - frameSize) / 2f
              val cornerLen = frameSize * 0.18f
              val strokePx = 4.dp.toPx()

              // Sombreado sutil exterior
              drawRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset.Zero,
                size = Size(size.width, top)
              )
              drawRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(0f, top + frameSize),
                size = Size(size.width, size.height - (top + frameSize))
              )
              drawRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(0f, top),
                size = Size(left, frameSize)
              )
              drawRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(left + frameSize, top),
                size = Size(size.width - (left + frameSize), frameSize)
              )

              val accent = Color(0xFFF59E0B)
              // Esquina superior izquierda
              drawLine(accent, Offset(left, top), Offset(left + cornerLen, top), strokePx, StrokeCap.Round)
              drawLine(accent, Offset(left, top), Offset(left, top + cornerLen), strokePx, StrokeCap.Round)
              // Esquina superior derecha
              drawLine(accent, Offset(left + frameSize - cornerLen, top), Offset(left + frameSize, top), strokePx, StrokeCap.Round)
              drawLine(accent, Offset(left + frameSize, top), Offset(left + frameSize, top + cornerLen), strokePx, StrokeCap.Round)
              // Esquina inferior izquierda
              drawLine(accent, Offset(left, top + frameSize), Offset(left + cornerLen, top + frameSize), strokePx, StrokeCap.Round)
              drawLine(accent, Offset(left, top + frameSize - cornerLen), Offset(left, top + frameSize), strokePx, StrokeCap.Round)
              // Esquina inferior derecha
              drawLine(accent, Offset(left + frameSize - cornerLen, top + frameSize), Offset(left + frameSize, top + frameSize), strokePx, StrokeCap.Round)
              drawLine(accent, Offset(left + frameSize, top + frameSize - cornerLen), Offset(left + frameSize, top + frameSize), strokePx, StrokeCap.Round)
            }

            // Botón de linterna sobre el visor
            IconButton(
              onClick = {
                torchEnabled = !torchEnabled
                cameraController.enableTorch(torchEnabled)
              },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
            ) {
              Icon(
                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = if (torchEnabled) "Apagar linterna" else "Encender linterna",
                tint = if (torchEnabled) Color(0xFFF59E0B) else Color.White
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = statusFeedback ?: "Apunte la cámara al código QR o código de barras del VIN (parabrisas, marco de puerta o padrón).",
          color = if (statusFeedback != null) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDismiss,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .semantics(mergeDescendants = true) {}
          ) {
            Text("Cancelar")
          }
        }
      }
    }
  }
}
