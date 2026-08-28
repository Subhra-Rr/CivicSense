package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LockOpen
import com.example.data.auth.UserProfile
import com.example.data.model.CivicIncident
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.UserRole
import com.example.ui.components.IncidentCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    incidents: List<CivicIncident>,
    userRole: UserRole,
    currentUser: UserProfile? = null,
    onReportClick: () -> Unit,
    onExploreMapClick: () -> Unit,
    onIncidentClick: (String) -> Unit,
    onTrustCenterClick: () -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onAuthClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeIncidents = incidents.filter { it.status != IncidentStatus.VERIFIED }
    val criticalCount = activeIncidents.count { it.priority == Priority.CRITICAL }
    val highCount = activeIncidents.count { it.priority == Priority.HIGH }
    val mediumCount = activeIncidents.count { it.priority == Priority.MEDIUM }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 0. Interactive Auth & Citizen Identity Bar
            item {
                AuthIdentityBanner(
                    currentUser = currentUser,
                    userRole = userRole,
                    onAuthClick = onAuthClick
                )
            }

            // 1. Hero Section (Public Utility Focus)
            item {
                HeroSection(
                    onReportClick = onReportClick,
                    onExploreClick = onExploreMapClick
                )
            }

            // 2. "Around You" Experience (Section 18)
            item {
                AroundYouSection(
                    activeCount = activeIncidents.size,
                    criticalCount = criticalCount,
                    highCount = highCount,
                    mediumCount = mediumCount,
                    onExploreClick = onExploreMapClick
                )
            }

            // 3. Live Civic Activity Feed (Section 6)
            item {
                LiveActivitySection(
                    incidents = incidents.take(3),
                    onIncidentClick = onIncidentClick
                )
            }

            // 4. How CivicSense Works (Section 7)
            item {
                HowItWorksSection()
            }

            // 5. Recent Active Issues Carousel
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Civic Incidents",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View Map",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CivicBlue,
                            modifier = Modifier.clickable { onExploreMapClick() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            items(activeIncidents.take(4), key = { it.id }) { incident ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    IncidentCard(
                        incident = incident,
                        onClick = { onIncidentClick(incident.id) }
                    )
                }
            }

            // 6. Trust & AI Transparency Card (Section 29)
            item {
                TrustCenterBanner(onTrustCenterClick = onTrustCenterClick)
            }
        }

        // Dominant Floating Action Button for Mobile
        FloatingActionButton(
            onClick = onReportClick,
            containerColor = CivicNavyDark,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_report_problem")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REPORT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun HeroSection(
    onReportClick: () -> Unit,
    onExploreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CivicNavyDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CivicTeal)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CIVIC ACTION SYSTEM",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "See a problem.\nCivicSense can act on it.",
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Report a civic problem with a photo, voice or a few words. CivicSense uses AI to understand what happened, connect it to the right civic workflow and help track it until resolution.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onReportClick,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("hero_button_report"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CivicBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "REPORT A PROBLEM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onExploreClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_button_explore"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "EXPLORE MAP",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AroundYouSection(
    activeCount: Int,
    criticalCount: Int,
    highCount: Int,
    mediumCount: Int,
    onExploreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onExploreClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CivicBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Civic issues around you",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Within 1.5 km",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$activeCount active issues detected in your neighborhood zone",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (criticalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PriorityCriticalBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$criticalCount Critical",
                            color = PriorityCritical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (highCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PriorityHighBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$highCount High",
                            color = PriorityHigh,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (mediumCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PriorityMediumBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$mediumCount Medium",
                            color = PriorityMedium,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveActivitySection(
    incidents: List<CivicIncident>,
    onIncidentClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCardSubtle
        ),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusResolved)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE CIVIC ACTIVITY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CivicNavyMedium,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realistic Live Workflow Events
            LiveActivityRowItem(
                step1 = "Pothole reported (4th & Elm)",
                step2 = "AI analyzed (Safety 82%)",
                step3 = "Public Works crew dispatched",
                onClick = { incidents.firstOrNull()?.let { onIncidentClick(it.id) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LiveActivityRowItem(
                step1 = "Streetlight outage",
                step2 = "3 citizen reports matched",
                step3 = "Grouped into 1 corridor incident",
                onClick = { incidents.getOrNull(2)?.let { onIncidentClick(it.id) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LiveActivityRowItem(
                step1 = "Water main fracture",
                step2 = "Emergency 12h SLA triggered",
                step3 = "Rapid valve crew on site",
                onClick = { incidents.getOrNull(3)?.let { onIncidentClick(it.id) } }
            )
        }
    }
}

@Composable
private fun LiveActivityRowItem(
    step1: String,
    step2: String,
    step3: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step1,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "↓ $step2",
                fontSize = 11.sp,
                color = CivicBlueDark
            )
            Text(
                text = "↓ $step3",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = StatusResolved
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun HowItWorksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "How CivicSense Works",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Simple on the surface. Extremely intelligent underneath.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        val steps = listOf(
            Triple("1. SEE IT", "Find a civic problem on your street or neighborhood.", Icons.Default.Visibility),
            Triple("2. REPORT IT", "Send a photo, voice or description in seconds.", Icons.Default.Add),
            Triple("3. CIVICSENSE UNDERSTANDS", "AI analyzes severity, safety impact and category.", Icons.Default.Psychology),
            Triple("4. CONNECTS IT", "Routes to the exact responsible civic department.", Icons.Default.NotificationsActive),
            Triple("5. FOLLOW ACTION", "Track field officers and live SLA timeline updates.", Icons.Default.LocationOn),
            Triple("6. VERIFY RESULT", "Citizens confirm whether the issue was actually fixed.", Icons.Default.CheckCircle)
        )

        steps.chunked(2).forEach { rowSteps ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSteps.forEach { (title, desc, icon) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = CivicBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustCenterBanner(onTrustCenterClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onTrustCenterClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CivicNavyMedium
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CivicBlueDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trust Center & AI Governance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Learn how CivicSense uses AI, safeguards privacy & maintains human oversight.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AuthIdentityBanner(
    currentUser: UserProfile?,
    userRole: UserRole,
    onAuthClick: () -> Unit
) {
    val isLoggedIn = currentUser != null && currentUser.id != "guest" && currentUser.email.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clickable { onAuthClick() }
            .testTag("auth_identity_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoggedIn) CivicBlueLight.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (isLoggedIn) CivicBlue.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isLoggedIn) CivicNavyDark else CivicBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLoggedIn) Icons.Default.AccountCircle else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isLoggedIn) (currentUser?.name ?: "Civic User") else "Sign In or Register",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLoggedIn) {
                            if (currentUser?.isGoogleUser == true) "Signed in with Google • ${userRole.displayName}"
                            else "${currentUser?.email} • ${userRole.displayName}"
                        } else {
                            "Sync reports, verify issues & earn civic trust"
                        },
                        fontSize = 11.sp,
                        color = if (isLoggedIn) CivicNavyDark else TextSecondary
                    )
                }
            }

            Button(
                onClick = onAuthClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoggedIn) CivicBlue else CivicNavyDark,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_banner_auth_action")
            ) {
                Text(
                    text = if (isLoggedIn) "Account" else "Sign In / Up",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
