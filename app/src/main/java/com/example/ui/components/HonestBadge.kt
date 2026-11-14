package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BadgeType {
  DEMO_EXAMPLE,      // Amarillo ámbar
  NO_REAL_READINGS,  // Naranja advertencia
  OBD_DISCONNECTED,  // Slate / Gris neutro
  OBD_PASSIVE_CONNECTED, // Verde clínico
  EVIDENCE_ATTACHED, // Verde
  EVIDENCE_MISSING,  // Ámbar
  SIGNATURE_SIGNED,  // Verde
  SIGNATURE_PENDING  // Ámbar
}

@Composable
fun HonestBadge(
  type: BadgeType,
  modifier: Modifier = Modifier,
  customText: String? = null
) {
  val (bgColor, borderColor, textColor, text, icon) = when (type) {
    BadgeType.DEMO_EXAMPLE -> Tuple5(
      Color(0xFFFEF3C7),
      Color(0xFFF59E0B),
      Color(0xFF92400E),
      customText ?: "Ejemplo / Demostración",
      Icons.Default.Info
    )
    BadgeType.NO_REAL_READINGS -> Tuple5(
      Color(0xFFFFEDD5),
      Color(0xFFF97316),
      Color(0xFF9A3412),
      customText ?: "No son lecturas reales",
      Icons.Default.Warning
    )
    BadgeType.OBD_DISCONNECTED -> Tuple5(
      Color(0xFFF1F5F9),
      Color(0xFF94A3B8),
      Color(0xFF475569),
      customText ?: "Sin lectura OBD-II",
      Icons.Default.Info
    )
    BadgeType.OBD_PASSIVE_CONNECTED -> Tuple5(
      Color(0xFFDCFCE7),
      Color(0xFF22C55E),
      Color(0xFF166534),
      customText ?: "Módulo OBD-II Pasivo Conectado",
      Icons.Default.CheckCircle
    )
    BadgeType.EVIDENCE_ATTACHED -> Tuple5(
      Color(0xFFDCFCE7),
      Color(0xFF16A34A),
      Color(0xFF14532D),
      customText ?: "Evidencia fotográfica adjunta",
      Icons.Default.CheckCircle
    )
    BadgeType.EVIDENCE_MISSING -> Tuple5(
      Color(0xFFFEE2E2),
      Color(0xFFEF4444),
      Color(0xFF991B1B),
      customText ?: "Sin evidencia fotográfica",
      Icons.Default.Warning
    )
    BadgeType.SIGNATURE_SIGNED -> Tuple5(
      Color(0xFFDCFCE7),
      Color(0xFF16A34A),
      Color(0xFF14532D),
      customText ?: "Firma registrada",
      Icons.Default.CheckCircle
    )
    BadgeType.SIGNATURE_PENDING -> Tuple5(
      Color(0xFFFEF3C7),
      Color(0xFFD97706),
      Color(0xFF78350F),
      customText ?: "Pendiente de firma",
      Icons.Default.Info
    )
  }

  Box(
    modifier = modifier
      .background(bgColor, RoundedCornerShape(6.dp))
      .border(1.dp, borderColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = text,
        tint = textColor,
        modifier = Modifier.size(13.dp)
      )
      Text(
        text = " $text",
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

private data class Tuple5<A, B, C, D, E>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
  val fifth: E
)
