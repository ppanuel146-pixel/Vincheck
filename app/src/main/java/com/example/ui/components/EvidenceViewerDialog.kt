package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.Intervention
import com.example.utils.WatermarkUtils

@Composable
fun EvidenceViewerDialog(
  intervention: Intervention,
  onDismiss: () -> Unit
) {
  val photoBitmap = remember(intervention.photoBase64) {
    if (intervention.photoBase64.isNotBlank()) {
      WatermarkUtils.base64ToBitmap(intervention.photoBase64)
    } else null
  }

  val signatureBitmap = remember(intervention.signatureBase64) {
    if (intervention.signatureBase64.isNotBlank()) {
      WatermarkUtils.base64ToBitmap(intervention.signatureBase64)
    } else null
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Auditoría de Evidencia y Firma",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "VIN: ${intervention.vin} | ${intervention.date}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECCIÓN FOTOGRAFÍA CON MARCA DE AGUA
        Text(
          text = "Evidencia Fotográfica:",
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (photoBitmap != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
          ) {
            Image(
              bitmap = photoBitmap.asImageBitmap(),
              contentDescription = "Evidencia con marca de agua",
              contentScale = ContentScale.FillWidth,
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "GPS",
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
              text = "GPS: ${intervention.photoGps.ifBlank { "Ubicación no disponible" }}",
              fontSize = 11.sp,
              color = Color(0xFF475569)
            )
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp)
              .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Sin evidencia fotográfica adjunta",
              color = Color(0xFF64748B),
              fontSize = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN FIRMA DIGITAL
        Text(
          text = "Firma Digital de Conformidad:",
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (signatureBitmap != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp)
              .background(Color.White, RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Image(
              bitmap = signatureBitmap.asImageBitmap(),
              contentDescription = "Trazo de firma",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Estado: Firma capturada en dispositivo",
            fontSize = 11.sp,
            color = Color(0xFF16A34A),
            fontWeight = FontWeight.Medium
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(90.dp)
              .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Pendiente de firma del cliente",
              color = Color(0xFF64748B),
              fontSize = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Aclaración académica legal
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Text(
            text = "Aclaración técnica: ${intervention.signatureDisclaimer}",
            fontSize = 11.sp,
            color = Color(0xFF92400E)
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
