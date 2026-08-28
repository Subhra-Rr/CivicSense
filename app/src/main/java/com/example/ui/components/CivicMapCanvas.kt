package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.CivicIncident
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.ui.theme.*

@Composable
fun CivicMapCanvas(
    incidents: List<CivicIncident>,
    selectedIncident: CivicIncident?,
    onIncidentSelected: (CivicIncident) -> Unit,
    modifier: Modifier = Modifier
) {
    // Coordinate bounds for mapping (San Francisco demo region)
    val minLat = 37.7600
    val maxLat = 37.7900
    val minLng = -122.4350
    val maxLng = -122.4050

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
            .pointerInput(incidents) {
                detectTapGestures { tapOffset ->
                    // Find nearest incident marker
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()

                    var closest: CivicIncident? = null
                    var closestDist = Float.MAX_VALUE

                    incidents.forEach { inc ->
                        val normX = ((inc.longitude - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.1f, 0.9f)
                        val normY = (1f - ((inc.latitude - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.1f, 0.9f)
                        val markerX = normX * width
                        val markerY = normY * height

                        val dx = tapOffset.x - markerX
                        val dy = tapOffset.y - markerY
                        val dist = dx * dx + dy * dy

                        if (dist < 40 * 40 && dist < closestDist) {
                            closestDist = dist
                            closest = inc
                        }
                    }

                    closest?.let { onIncidentSelected(it) }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw City Grid & Roads
            drawCityGrid(w, h)

            // 2. Draw Hotspot Cluster Radii
            incidents.filter { it.recurringReportCount >= 4 && it.status != IncidentStatus.VERIFIED }.forEach { inc ->
                val normX = ((inc.longitude - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.1f, 0.9f)
                val normY = (1f - ((inc.latitude - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.1f, 0.9f)
                val cx = normX * w
                val cy = normY * h

                drawCircle(
                    color = PriorityCritical.copy(alpha = 0.15f),
                    radius = 48.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = PriorityCritical.copy(alpha = 0.35f),
                    radius = 48.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 3. Draw User Radar Location
            val userCenter = Offset(w * 0.48f, h * 0.52f)
            drawCircle(
                color = CivicBlue.copy(alpha = 0.18f),
                radius = 36.dp.toPx(),
                center = userCenter
            )
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = userCenter
            )
            drawCircle(
                color = CivicBlue,
                radius = 5.dp.toPx(),
                center = userCenter
            )

            // 4. Draw Incident Markers
            incidents.forEach { inc ->
                val normX = ((inc.longitude - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.1f, 0.9f)
                val normY = (1f - ((inc.latitude - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.1f, 0.9f)
                val markerX = normX * w
                val markerY = normY * h

                val isSelected = selectedIncident?.id == inc.id
                val markerColor = when {
                    inc.status == IncidentStatus.VERIFIED -> StatusVerified
                    inc.priority == Priority.CRITICAL -> PriorityCritical
                    inc.priority == Priority.HIGH -> PriorityHigh
                    inc.priority == Priority.MEDIUM -> PriorityMedium
                    else -> PriorityLow
                }

                // Marker Pin Head
                if (isSelected) {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.35f),
                        radius = 20.dp.toPx(),
                        center = Offset(markerX, markerY - 8.dp.toPx())
                    )
                }

                // Shadow
                drawCircle(
                    color = Color(0x33000000),
                    radius = 5.dp.toPx(),
                    center = Offset(markerX, markerY + 2.dp.toPx())
                )

                // Outer Pin Circle
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 12.dp.toPx() else 9.dp.toPx(),
                    center = Offset(markerX, markerY - 6.dp.toPx())
                )

                // Inner Pin Core
                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) 9.dp.toPx() else 6.5.dp.toPx(),
                    center = Offset(markerX, markerY - 6.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawCityGrid(w: Float, h: Float) {
    val roadColor = Color(0xFFCBD5E1)
    val mainAvenueColor = Color(0xFF94A3B8)
    val parkColor = Color(0xFFDCFCE7)
    val waterColor = Color(0xFFBAE6FD)

    // Water channel at top-right
    val waterPath = Path().apply {
        moveTo(w * 0.7f, 0f)
        lineTo(w, 0f)
        lineTo(w, h * 0.35f)
        cubicTo(w * 0.85f, h * 0.28f, w * 0.78f, h * 0.15f, w * 0.7f, 0f)
        close()
    }
    drawPath(waterPath, color = waterColor)

    // Central park patch
    drawRect(
        color = parkColor,
        topLeft = Offset(w * 0.12f, h * 0.65f),
        size = Size(w * 0.28f, h * 0.22f)
    )

    // Horizontal street grid
    for (i in 1..8) {
        val y = h * (i / 9f)
        val strokeWidth = if (i == 3 || i == 6) 3.5.dp.toPx() else 1.5.dp.toPx()
        val color = if (i == 3 || i == 6) mainAvenueColor else roadColor
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = strokeWidth
        )
    }

    // Vertical avenue grid
    for (j in 1..7) {
        val x = w * (j / 8f)
        val strokeWidth = if (j == 2 || j == 5) 3.5.dp.toPx() else 1.5.dp.toPx()
        val color = if (j == 2 || j == 5) mainAvenueColor else roadColor
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = strokeWidth
        )
    }

    // Diagonal arterial boulevard
    drawLine(
        color = mainAvenueColor,
        start = Offset(0f, h * 0.85f),
        end = Offset(w * 0.85f, 0f),
        strokeWidth = 4.dp.toPx()
    )
}
