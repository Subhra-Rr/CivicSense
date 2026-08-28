package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.ui.theme.*

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (priority) {
        Priority.CRITICAL -> PriorityCriticalBg to PriorityCritical
        Priority.HIGH -> PriorityHighBg to PriorityHigh
        Priority.MEDIUM -> PriorityMediumBg to PriorityMedium
        Priority.LOW -> PriorityLowBg to PriorityLow
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = priority.label.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun StatusBadge(
    status: IncidentStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        IncidentStatus.REPORTED -> StatusReportedBg to StatusReported
        IncidentStatus.TRIAGED -> StatusTriagedBg to StatusTriaged
        IncidentStatus.IN_PROGRESS -> StatusInProgressBg to StatusInProgress
        IncidentStatus.RESOLVED -> StatusResolvedBg to StatusResolved
        IncidentStatus.VERIFIED -> StatusVerifiedBg to StatusVerified
        IncidentStatus.REOPENED -> PriorityHighBg to PriorityHigh
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
