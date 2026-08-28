package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CivicIncident
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun OfficerScreen(
    operationalQueue: List<CivicIncident>,
    officerName: String,
    onIncidentClick: (String) -> Unit,
    onAssignSelf: (String) -> Unit,
    onSubmitResolution: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var resolvingIncidentId by remember { mutableStateOf<String?>(null) }
    var resolutionNotes by remember { mutableStateOf("") }

    val criticalCount = operationalQueue.count { it.priority == Priority.CRITICAL }
    val inProgressCount = operationalQueue.count { it.status == IncidentStatus.IN_PROGRESS }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Operational Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CivicNavyDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CivicBlueDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = officerName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Field Operations Desk",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (criticalCount > 0) PriorityCritical else CivicTeal)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (criticalCount > 0) "$criticalCount URGENT" else "SYSTEM STABLE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(title = "Queue Items", value = "${operationalQueue.size}", modifier = Modifier.weight(1f))
                    MetricBox(title = "In Progress", value = "$inProgressCount", modifier = Modifier.weight(1f))
                    MetricBox(title = "Critical SLA", value = "$criticalCount", modifier = Modifier.weight(1f))
                }
            }
        }

        // Section Title: "What needs my attention RIGHT NOW?"
        Text(
            text = "PRIORITIZED OPERATIONAL QUEUE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CivicNavyMedium,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("officer_queue_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(operationalQueue, key = { it.id }) { incident ->
                OfficerIncidentActionCard(
                    incident = incident,
                    onCardClick = { onIncidentClick(incident.id) },
                    onAssignSelf = { onAssignSelf(incident.id) },
                    onOpenResolution = { resolvingIncidentId = incident.id }
                )
            }
        }
    }

    // Resolution Submission Dialog
    resolvingIncidentId?.let { incId ->
        Dialog(onDismissRequest = { resolvingIncidentId = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Submit Resolution for $incId",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Provide work completion details for the citizen verification record:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("officer_resolution_input"),
                        placeholder = { Text("e.g. Cold-mix asphalt applied and compacted. Debris cleared...") },
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { resolvingIncidentId = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSubmitResolution(incId, resolutionNotes.ifBlank { "Repair completed by field operations unit." })
                                resolvingIncidentId = null
                                resolutionNotes = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusResolved),
                            modifier = Modifier.testTag("officer_confirm_resolution_button")
                        ) {
                            Text("Complete & Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun OfficerIncidentActionCard(
    incident: CivicIncident,
    onCardClick: () -> Unit,
    onAssignSelf: () -> Unit,
    onOpenResolution: () -> Unit
) {
    val remainingHours = ((incident.slaDeadline - System.currentTimeMillis()) / (3600 * 1000L)).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (incident.priority == Priority.CRITICAL) PriorityCritical else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${incident.id} • ${incident.category.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityBadge(priority = incident.priority)
                    StatusBadge(status = incident.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = incident.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${incident.address} • SLA: ${remainingHours}h remaining",
                fontSize = 12.sp,
                color = if (remainingHours <= 12) PriorityHigh else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons for Field Officer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (incident.status != IncidentStatus.IN_PROGRESS && incident.status != IncidentStatus.RESOLVED) {
                    Button(
                        onClick = onAssignSelf,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicNavyDark)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Assign & Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (incident.status == IncidentStatus.IN_PROGRESS) {
                    Button(
                        onClick = onOpenResolution,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusResolved)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Submit Resolution", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
