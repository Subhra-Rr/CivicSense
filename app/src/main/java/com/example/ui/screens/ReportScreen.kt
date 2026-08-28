package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.repository.ReportSubmissionProgress
import com.example.ui.theme.*

@Composable
fun ReportScreen(
    submissionProgress: ReportSubmissionProgress,
    onSubmitReport: (description: String, address: String, zone: String, imageUrl: String?) -> Unit,
    onReportSuccess: (String) -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("450 Market Street, District 2") }
    var zone by remember { mutableStateOf("Downtown Transit Corridor") }
    var additionalNotes by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    val quickPresets = listOf(
        "Deep pothole causing vehicle tire damage near crosswalk",
        "Large garbage accumulation obstructing sidewalk",
        "Multiple streetlights dark on entire avenue block",
        "Broken water pipe flooding street and sidewalk",
        "Open manhole drain lid missing on road"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            // Header
            Text(
                text = "Report a Problem",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "CivicSense uses AI to analyze your report and connect it directly to the responsible team.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Step 1: What happened?
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "1. WHAT HAPPENED?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavyMedium,
                            letterSpacing = 0.6.sp
                        )

                        // Voice simulation toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRecordingVoice) PriorityCriticalBg else SurfaceCardSubtle)
                                .clickable {
                                    isRecordingVoice = !isRecordingVoice
                                    if (isRecordingVoice) {
                                        description = "There is a deep road pothole with exposed gravel that vehicles are dangerously swerving around."
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isRecordingVoice) PriorityCritical else CivicNavyLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRecordingVoice) "Listening..." else "Voice input",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isRecordingVoice) PriorityCritical else CivicNavyLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_input_description"),
                        placeholder = { Text("Describe the problem in plain words...") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicBlue,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick scenario presets
                    Text(
                        text = "Or tap a quick example:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    quickPresets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCardSubtle)
                                .clickable { description = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "• $preset",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real Photo Evidence Attachment
                    if (selectedPhotoUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusResolvedBg),
                            border = BorderStroke(1.dp, StatusResolved.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusResolved,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Device Photo Evidence Attached",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusResolved
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedPhotoUri = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.05f))
                                ) {
                                    AsyncImage(
                                        model = selectedPhotoUri,
                                        contentDescription = "Uploaded evidence preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Tap to change photo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CivicBlue,
                                        modifier = Modifier.clickable {
                                            photoPickerLauncher.launch("image/*")
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCardSubtle)
                                .clickable {
                                    photoPickerLauncher.launch("image/*")
                                }
                                .padding(12.dp)
                                .testTag("btn_attach_device_photo"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Photo",
                                    tint = CivicBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Attach Photo Evidence",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Select an image directly from your device gallery",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CivicBlueLight,
                                    contentColor = CivicBlueDark
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Browse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Where is it?
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "2. WHERE IS IT?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavyMedium,
                            letterSpacing = 0.6.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CivicBlueLight)
                                .clickable {
                                    address = "7th & Market Blvd Crosswalk"
                                    zone = "Market Transit - Zone 2"
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS",
                                tint = CivicBlueDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Use GPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CivicBlueDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_input_address"),
                        label = { Text("Street Address / Intersection") },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicBlue,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = zone,
                        onValueChange = { zone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Civic District / Zone") },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicBlue,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 3: Anything else?
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. ANYTHING ELSE? (OPTIONAL)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavyMedium,
                        letterSpacing = 0.6.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Landmarks, timing, or safety hazards...") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CivicBlue,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (description.isNotBlank()) {
                        val fullDescription = if (additionalNotes.isNotBlank()) "$description. Landmark notes: $additionalNotes" else description
                        onSubmitReport(fullDescription, address, zone, selectedPhotoUri?.toString())
                    }
                },
                enabled = description.isNotBlank() && submissionProgress is ReportSubmissionProgress.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_report_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CivicNavyDark,
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANALYZE & SUBMIT REPORT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Live Processing Dialog (Section 10)
        when (val progress = submissionProgress) {
            is ReportSubmissionProgress.Analyzing -> {
                Dialog(onDismissRequest = {}) {
                    ProcessingDialogContent(progress = progress)
                }
            }
            is ReportSubmissionProgress.Success -> {
                Dialog(onDismissRequest = {
                    onResetProgress()
                    onReportSuccess(progress.incidentId)
                }) {
                    SuccessDialogContent(
                        success = progress,
                        onDismiss = {
                            onResetProgress()
                            onReportSuccess(progress.incidentId)
                        }
                    )
                }
            }
            is ReportSubmissionProgress.Error -> {
                Dialog(onDismissRequest = { onResetProgress() }) {
                    ErrorDialogContent(
                        error = progress,
                        onDismiss = { onResetProgress() }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun ProcessingDialogContent(progress: ReportSubmissionProgress.Analyzing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CivicNavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = CivicBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = progress.stepName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = progress.stepDetail,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Step Progress tracker
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..progress.totalSteps) {
                    val isDone = i <= progress.stepIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDone) CivicBlue else Color(0xFF334155))
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessDialogContent(
    success: ReportSubmissionProgress.Success,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(StatusResolvedBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusResolved,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (success.isGroupedIntoExisting) "Matched Active Incident" else "Civic Incident Created",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Incident ID: ${success.incidentId}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CivicBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (success.isGroupedIntoExisting)
                    "Your report matched an existing cluster in this zone. It has been grouped to strengthen community urgency."
                else
                    "AI has categorized this under ${success.analysis.category.displayName} and dispatched it to ${success.analysis.department.displayName}.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("success_view_case_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicNavyDark)
            ) {
                Text(text = "VIEW CASE FILE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorDialogContent(
    error: ReportSubmissionProgress.Error,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Notice",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PriorityCritical
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = error.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicNavyDark)
            ) {
                Text(text = "OK")
            }
        }
    }
}
