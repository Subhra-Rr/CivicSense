package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.screens.AdminInsightsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncidentDetailScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.OfficerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.TrustCenterScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.CivicViewModel

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.IconButton
import com.example.ui.screens.AuthDialog

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MAP("Map", Icons.Default.Map),
    REPORT("Report", Icons.Default.AddCircle),
    OPERATIONS("Desk", Icons.AutoMirrored.Filled.Assignment),
    INSIGHTS("Insights", Icons.Default.AutoGraph),
    PROFILE("Activity", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicMainScreen(
    viewModel: CivicViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(NavDestination.HOME) }
    var inIncidentDetail by remember { mutableStateOf(false) }
    var inTrustCenter by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val allIncidents by viewModel.allIncidents.collectAsState()
    val filteredIncidents by viewModel.filteredIncidents.collectAsState()
    val officerQueue by viewModel.officerQueue.collectAsState()
    val myIncidents by viewModel.myIncidents.collectAsState()
    val selectedIncident by viewModel.selectedIncident.collectAsState()
    val selectedTimeline by viewModel.selectedTimeline.collectAsState()
    val selectedReports by viewModel.selectedReports.collectAsState()
    val submissionProgress by viewModel.submissionProgress.collectAsState()
    val civicInsights by viewModel.civicInsights.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val snackBarMsg by viewModel.snackBarMessage.collectAsState()

    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val priorityFilter by viewModel.priorityFilter.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackBarMsg) {
        snackBarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackBarMessage()
        }
    }

    val destinations = when (userRole) {
        UserRole.CITIZEN -> listOf(NavDestination.HOME, NavDestination.REPORT, NavDestination.MAP, NavDestination.PROFILE)
        UserRole.FIELD_OFFICER -> listOf(NavDestination.OPERATIONS, NavDestination.MAP, NavDestination.PROFILE)
        UserRole.CIVIC_ADMIN -> listOf(NavDestination.INSIGHTS, NavDestination.MAP, NavDestination.HOME, NavDestination.PROFILE)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Navigation Rail for Wide Screens
            if (isExpanded && !inIncidentDetail && !inTrustCenter) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CivicNavyDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "CivicSense",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "CivicSense",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    destinations.forEach { dest ->
                        NavigationRailItem(
                            selected = currentScreen == dest,
                            onClick = { currentScreen = dest },
                            icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
                            label = { Text(text = dest.label, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CivicNavyDark,
                                selectedTextColor = CivicNavyDark,
                                indicatorColor = CivicBlueLight,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("rail_item_${dest.name.lowercase()}")
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Main Content Area
            Scaffold(
                topBar = {
                    if (!inIncidentDetail && !inTrustCenter) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (!isExpanded) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(CivicNavyDark),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = "CivicSense",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column {
                                        Text(
                                            text = "CivicSense",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Civic Action System",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showAuthDialog = true },
                                    modifier = Modifier.testTag("topbar_auth_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "User Account",
                                        tint = CivicNavyDark
                                    )
                                }
                                // Quick Role Indicator Pill
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(CivicBlueLight)
                                        .clickable {
                                            val nextRole = when (userRole) {
                                                UserRole.CITIZEN -> UserRole.FIELD_OFFICER
                                                UserRole.FIELD_OFFICER -> UserRole.CIVIC_ADMIN
                                                UserRole.CIVIC_ADMIN -> UserRole.CITIZEN
                                            }
                                            viewModel.setUserRole(nextRole)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = userRole.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CivicBlueDark
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!isExpanded && !inIncidentDetail && !inTrustCenter) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            destinations.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentScreen == dest,
                                    onClick = { currentScreen = dest },
                                    icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
                                    label = { Text(text = dest.label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CivicNavyDark,
                                        selectedTextColor = CivicNavyDark,
                                        indicatorColor = CivicBlueLight,
                                        unselectedIconColor = TextTertiary,
                                        unselectedTextColor = TextTertiary
                                    ),
                                    modifier = Modifier.testTag("nav_item_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        inIncidentDetail -> {
                            IncidentDetailScreen(
                                incident = selectedIncident,
                                timeline = selectedTimeline,
                                citizenReports = selectedReports,
                                onBackClick = { inIncidentDetail = false },
                                onCommunityConfirm = { isStillHappening ->
                                    selectedIncident?.let {
                                        viewModel.confirmCommunityProblem(it.id, isStillHappening)
                                    }
                                },
                                onVerifyResolution = { isSatisfied, notes ->
                                    selectedIncident?.let {
                                        viewModel.verifyResolution(it.id, isSatisfied, notes)
                                    }
                                }
                            )
                        }

                        inTrustCenter -> {
                            TrustCenterScreen(
                                onBackClick = { inTrustCenter = false }
                            )
                        }

                        else -> {
                            when (currentScreen) {
                                NavDestination.HOME -> {
                                    HomeScreen(
                                        incidents = allIncidents,
                                        userRole = userRole,
                                        onReportClick = { currentScreen = NavDestination.REPORT },
                                        onExploreMapClick = { currentScreen = NavDestination.MAP },
                                        onIncidentClick = { id ->
                                            viewModel.selectIncident(id)
                                            inIncidentDetail = true
                                        },
                                        onTrustCenterClick = { inTrustCenter = true },
                                        onRoleChange = { viewModel.setUserRole(it) }
                                    )
                                }

                                NavDestination.REPORT -> {
                                    ReportScreen(
                                        submissionProgress = submissionProgress,
                                        onSubmitReport = { desc, addr, zone ->
                                            viewModel.submitReport(desc, addr, zone)
                                        },
                                        onReportSuccess = { id ->
                                            viewModel.selectIncident(id)
                                            inIncidentDetail = true
                                        },
                                        onResetProgress = { viewModel.resetSubmissionProgress() }
                                    )
                                }

                                NavDestination.MAP -> {
                                    MapScreen(
                                        incidents = allIncidents,
                                        selectedIncident = selectedIncident,
                                        onSelectIncident = { id -> viewModel.selectIncident(id) },
                                        onViewCaseDetail = { id ->
                                            viewModel.selectIncident(id)
                                            inIncidentDetail = true
                                        },
                                        categoryFilter = categoryFilter,
                                        onCategoryFilterChange = { viewModel.categoryFilter.value = it },
                                        priorityFilter = priorityFilter,
                                        onPriorityFilterChange = { viewModel.priorityFilter.value = it }
                                    )
                                }

                                NavDestination.OPERATIONS -> {
                                    OfficerScreen(
                                        operationalQueue = officerQueue,
                                        officerName = currentUserName,
                                        onIncidentClick = { id ->
                                            viewModel.selectIncident(id)
                                            inIncidentDetail = true
                                        },
                                        onAssignSelf = { id -> viewModel.officerAssignSelf(id) },
                                        onSubmitResolution = { id, notes -> viewModel.officerSubmitResolution(id, notes) }
                                    )
                                }

                                NavDestination.INSIGHTS -> {
                                    AdminInsightsScreen(
                                        incidents = allIncidents,
                                        insights = civicInsights
                                    )
                                }

                                NavDestination.PROFILE -> {
                                    ProfileScreen(
                                        userName = currentUserName,
                                        userRole = userRole,
                                        userProfile = currentUser,
                                        myIncidents = myIncidents,
                                        onIncidentClick = { id ->
                                            viewModel.selectIncident(id)
                                            inIncidentDetail = true
                                        },
                                        onRoleChange = { viewModel.setUserRole(it) },
                                        onOpenAuth = { showAuthDialog = true },
                                        onSignOut = { viewModel.signOut() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAuthDialog) {
            AuthDialog(
                onDismiss = { showAuthDialog = false },
                onGoogleSignIn = { viewModel.signInWithGoogle() },
                onEmailSignIn = { email, pass, role -> viewModel.signInWithEmail(email, pass, role) },
                onEmailSignUp = { name, email, pass, role -> viewModel.signUp(name, email, pass, role) }
            )
        }
    }
}
