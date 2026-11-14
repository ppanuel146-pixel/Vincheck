package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utils.WatermarkUtils

data class LineStroke(val points: List<Offset>)

@Composable
fun SignatureDialog(
  onDismiss: () -> Unit,
  onSignatureCaptured: (base64Signature: String) -> Unit
) {
  val lines = remember { mutableStateListOf<LineStroke>() }
  var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Draw,
            contentDescription = "Firma",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = "Firma Digital de Conformidad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Solicite al cliente que dibuje su firma con el dedo en el recuadro:",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Lienzo táctil para la firma
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
              detectDragGestures(
                onDragStart = { offset ->
                  currentPoints = listOf(offset)
                },
                onDrag = { change, _ ->
                  change.consume()
                  currentPoints = currentPoints + change.position
                },
                onDragEnd = {
                  if (currentPoints.isNotEmpty()) {
                    lines.add(LineStroke(currentPoints))
                    currentPoints = emptyList()
                  }
                },
                onDragCancel = {
                  currentPoints = emptyList()
                }
              )
            }
        ) {
          Canvas(modifier = Modifier.matchParentSize()) {
            // Dibujar líneas guardadas
            for (line in lines) {
              if (line.points.size > 1) {
                val path = Path().apply {
                  moveTo(line.points.first().x, line.points.first().y)
                  for (i in 1 until line.points.size) {
                    lineTo(line.points[i].x, line.points[i].y)
                  }
                }
                drawPath(
                  path = path,
                  color = Color(0xFF0F172A),
                  style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
              }
            }
            // Dibujar trazo en curso
            if (currentPoints.size > 1) {
              val currentPath = Path().apply {
                moveTo(currentPoints.first().x, currentPoints.first().y)
                for (i in 1 until currentPoints.size) {
                  lineTo(currentPoints[i].x, currentPoints[i].y)
                }
              }
              drawPath(
                path = currentPath,
                color = Color(0xFF0F172A),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }

            if (lines.isEmpty() && currentPoints.isEmpty()) {
              // Indicador guía
              drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(40f, size.height * 0.75f),
                end = Offset(size.width - 40f, size.height * 0.75f),
                strokeWidth = 2f
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mensaje de honestidad académica obligatorio
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Aviso legal honesto",
              tint = Color(0xFFB45309),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
              text = "Nota: Este es un trazo capturado en el dispositivo, no una firma electrónica certificada.",
              fontSize = 11.sp,
              color = Color(0xFF92400E),
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              lines.clear()
              currentPoints = emptyList()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
          ) {
            Icon(Icons.Default.Clear, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Limpiar")
          }

          Row {
            OutlinedButton(onClick = onDismiss) {
              Text("Cancelar")
            }
            Spacer(modifier = Modifier.size(8.dp))
            Button(
              onClick = {
                val base64 = renderLinesToBase64(lines)
                onSignatureCaptured(base64)
              },
              enabled = lines.isNotEmpty()
            ) {
              Text("Guardar firma")
            }
          }
        }
      }
    }
  }
}

private fun renderLinesToBase64(lines: List<LineStroke>): String {
  val width = 500
  val height = 220
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  // Fondo blanco limpio
  canvas.drawColor(AndroidColor.WHITE)

  val paint = Paint().apply {
    color = AndroidColor.rgb(15, 23, 42) // Slate-900
    strokeWidth = 5f
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    isAntiAlias = true
  }

  for (line in lines) {
    if (line.points.size > 1) {
      val path = AndroidPath().apply {
        moveTo(line.points.first().x, line.points.first().y)
        for (i in 1 until line.points.size) {
          lineTo(line.points[i].x, line.points[i].y)
        }
      }
      canvas.drawPath(path, paint)
    }
  }

  return WatermarkUtils.bitmapToBase64(bitmap, quality = 85)
}
