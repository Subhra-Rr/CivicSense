package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.CitizenReport
import com.example.data.model.CivicIncident
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ScoreBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimelineView
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incident: CivicIncident?,
    timeline: List<TimelineEvent>,
    citizenReports: List<CitizenReport>,
    onBackClick: () -> Unit,
    onCommunityConfirm: (Boolean) -> Unit,
    onVerifyResolution: (Boolean, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (incident == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an incident to view details", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val remainingHours = ((incident.slaDeadline - System.currentTimeMillis()) / (3600 * 1000L)).toInt()
    val timeFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    var showReopenDialog by remember { mutableStateOf(false) }
    var reopenNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = incident.id,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = incident.category.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("incident_detail_scroll"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Case Header & Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PriorityBadge(priority = incident.priority)
                                StatusBadge(status = incident.status)
                            }

                            if (incident.status != IncidentStatus.VERIFIED && incident.status != IncidentStatus.RESOLVED) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = if (remainingHours <= 12) PriorityHigh else TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (remainingHours > 0) "${remainingHours}h remaining" else "SLA Due",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remainingHours <= 12) PriorityHigh else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = incident.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = incident.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Attributes table
                        AttributeRow(icon = Icons.Default.LocationOn, label = "Location", value = "${incident.address} (${incident.zone})")
                        Spacer(modifier = Modifier.height(8.dp))
                        AttributeRow(icon = Icons.Default.Business, label = "Department", value = incident.department.displayName)
                        Spacer(modifier = Modifier.height(8.dp))
                        AttributeRow(
                            icon = Icons.Default.Person,
                            label = "Assigned Lead",
                            value = incident.assignedOfficer ?: "Awaiting officer dispatch"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AttributeRow(icon = Icons.Default.AccessTime, label = "Reported", value = timeFormat.format(Date(incident.reportedAt)))
                    }
                }
            }

            // 2. AI Transparency Breakdown (Section 11)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CivicNavyDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Transparency & Scoring",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicNavyDark
                                )
                                Text(
                                    text = "Why this was marked ${incident.priority.label} priority",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ScoreBar(label = "Safety Risk Assessment", score = incident.safetyRiskScore)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreBar(label = "Public Impact & Exposure", score = incident.publicImpactScore)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreBar(label = "Composite Severity Score", score = incident.severityScore)

                        Spacer(modifier = Modifier.height(12.dp))

                        incident.aiSummary?.let { summary ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "AI Rationale: $summary",
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Automated AI classification is verified by municipal operations personnel.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // 3. Community Intelligence & Duplicate Clustering (Section 15)
            if (incident.recurringReportCount > 1 || citizenReports.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = CivicBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${citizenReports.size + incident.recurringReportCount} citizens reported this problem",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "CivicSense grouped matching neighborhood reports into this single actionable case file.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (citizenReports.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                citizenReports.forEach { rep ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceCardSubtle)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "\"${rep.description}\" — ${rep.reporterName}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Community Confirmation Action (Section 16)
            if (incident.status != IncidentStatus.VERIFIED) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Is this problem still happening?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Community feedback strengthens civic priority and informs repair teams.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onCommunityConfirm(true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_yes_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PriorityCritical)
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "YES (${incident.communityConfirmationsYes})", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onCommunityConfirm(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_no_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "NO (${incident.communityConfirmationsNo})", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Citizen Verification Panel (Section 14)
            if (incident.status == IncidentStatus.RESOLVED) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusResolvedBg),
                        border = BorderStroke(1.dp, StatusResolved)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusResolved)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resolution Submitted by Field Team",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusResolved
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            incident.resolutionNotes?.let { notes ->
                                Text(text = "Notes: $notes", fontSize = 13.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            Text(
                                text = "As a citizen, please confirm if the repair meets satisfaction:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onVerifyResolution(true, null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("verify_satisfied_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusResolved)
                                ) {
                                    Text(text = "CONFIRM FIXED", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showReopenDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("verify_reopen_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PriorityCritical)
                                ) {
                                    Text(text = "REOPEN ISSUE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 6. Case Timeline (Section 14)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Incident Action Timeline",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TimelineView(events = timeline)
                    }
                }
            }
        }
    }

    if (showReopenDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showReopenDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Reopen Incident for Review",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Describe why the problem remains unresolved:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reopenNotes,
                        onValueChange = { reopenNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Patch cracked again, debris left behind...") },
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showReopenDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showReopenDialog = false
                                onVerifyResolution(false, reopenNotes)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityCritical)
                        ) {
                            Text("Submit Reopen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
