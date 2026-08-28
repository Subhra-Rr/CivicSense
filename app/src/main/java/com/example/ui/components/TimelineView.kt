package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncidentStatus
import com.example.data.model.TimelineEvent
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineView(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("h:mm a • MMM d", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        events.forEachIndexed { index, event ->
            val isLast = index == events.size - 1
            val nodeColor = when (event.status) {
                IncidentStatus.VERIFIED -> StatusVerified
                IncidentStatus.RESOLVED -> StatusResolved
                IncidentStatus.IN_PROGRESS -> CivicBlue
                IncidentStatus.TRIAGED -> StatusTriaged
                IncidentStatus.REOPENED -> PriorityHigh
                IncidentStatus.REPORTED -> CivicNavyLight
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // Left Column: Node & Connector Line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(nodeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(BorderMedium)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Event Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isLast) 0.dp else 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.title.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = nodeColor,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = timeFormat.format(Date(event.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = event.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${event.actorName} (${event.actorRole})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
