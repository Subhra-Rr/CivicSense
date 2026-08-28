package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.ui.components.CivicMapCanvas
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun MapScreen(
    incidents: List<CivicIncident>,
    selectedIncident: CivicIncident?,
    onSelectIncident: (String) -> Unit,
    onViewCaseDetail: (String) -> Unit,
    categoryFilter: CivicCategory?,
    onCategoryFilterChange: (CivicCategory?) -> Unit,
    priorityFilter: Priority?,
    onPriorityFilterChange: (Priority?) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = incidents.filter { inc ->
        val matchesQuery = searchQuery.isBlank() || 
            inc.title.contains(searchQuery, ignoreCase = true) ||
            inc.address.contains(searchQuery, ignoreCase = true)
        val matchesCat = categoryFilter == null || inc.category == categoryFilter
        val matchesPrio = priorityFilter == null || inc.priority == priorityFilter
        matchesQuery && matchesCat && matchesPrio
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Vector Map Canvas
        CivicMapCanvas(
            incidents = filtered,
            selectedIncident = selectedIncident,
            onIncidentSelected = { onSelectIncident(it.id) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Floating Top Header Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .testTag("map_search_input"),
                placeholder = { Text("Search location, road, or incident ID...") },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.clickable { searchQuery = "" }
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CivicBlue,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // All Chip
                FilterChip(
                    selected = categoryFilter == null && priorityFilter == null,
                    onClick = {
                        onCategoryFilterChange(null)
                        onPriorityFilterChange(null)
                    },
                    label = { Text("All (${incidents.size})") }
                )

                // Critical Priority Filter
                FilterChip(
                    selected = priorityFilter == Priority.CRITICAL,
                    onClick = {
                        onPriorityFilterChange(if (priorityFilter == Priority.CRITICAL) null else Priority.CRITICAL)
                    },
                    label = { Text("Critical Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PriorityCriticalBg,
                        selectedLabelColor = PriorityCritical
                    )
                )

                // Category Chips
                CivicCategory.values().forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = {
                            onCategoryFilterChange(if (categoryFilter == cat) null else cat)
                        },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Corridor Hotspot Alert (Section 17)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CivicNavyDark.copy(alpha = 0.92f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = PriorityHigh,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Corridor Hotspot: 3 infrastructure issues in District 2 & 4",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // 3. Selected Incident Bottom Card (Section 17)
        AnimatedVisibility(
            visible = selectedIncident != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = 72.dp)
        ) {
            selectedIncident?.let { inc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewCaseDetail(inc.id) }
                        .testTag("map_selected_incident_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, CivicBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = inc.id,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CivicBlue
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PriorityBadge(priority = inc.priority)
                                StatusBadge(status = inc.status)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = inc.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = inc.address,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tap to open complete Case File",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CivicNavyMedium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = CivicBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
