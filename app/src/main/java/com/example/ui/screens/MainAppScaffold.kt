package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.routeros.PppSecretModel
import com.example.ui.MikrotechViewModel
import com.example.ui.components.MikrotechBottomBar
import com.example.ui.components.MikrotechTopBar
import com.example.ui.theme.MikrotechBackground

@Composable
fun MainAppScaffold(
    viewModel: MikrotechViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val routerResource by viewModel.routerResource.collectAsState()
    val routerIdentity by viewModel.routerIdentity.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val pppProfiles by viewModel.pppProfiles.collectAsState()
    val isLightMode by viewModel.isLightMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateTicketDialog by remember { mutableStateOf(false) }
    var showBatchTicketDialog by remember { mutableStateOf(false) }
    var showCreatePppoeDialog by remember { mutableStateOf(false) }

    // Toast feedback
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val showWelcomeSplash by viewModel.showWelcomeSplash.collectAsState()

    // Back handling
    BackHandler(enabled = showWelcomeSplash) {
        viewModel.dismissWelcomeScreen()
    }

    BackHandler(enabled = !showWelcomeSplash && currentScreen != "dashboard") {
        when {
            currentScreen in listOf("routers", "hotspot_profiles", "pppoe", "portal", "maintenance", "license", "anticoupure") -> {
                viewModel.navigateTo("settings")
            }
            currentScreen != "dashboard" -> {
                viewModel.navigateTo("dashboard")
            }
        }
    }

    if (showWelcomeSplash) {
        WelcomeSplashScreen(
            viewModel = viewModel,
            onEnterApp = { viewModel.dismissWelcomeScreen() }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                MikrotechTopBar(
                    routerName = routerIdentity.name,
                    routerModel = routerResource.boardName,
                    routerVersion = routerResource.version,
                    status = connectionStatus,
                    onRefresh = { viewModel.refreshAll() },
                    onOpenRouters = { viewModel.openWelcomeScreen() },
                    isLightMode = isLightMode,
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            },
            bottomBar = {
                MikrotechBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            when (currentScreen) {
                "dashboard" -> {
                    Column {
                    OutlinedButton(onClick = { viewModel.navigateTo("remote") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Contrôle à distance • Cloudflare")
                    }
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { dest -> viewModel.navigateTo(dest) },
                        onOpenCreateTicket = { showCreateTicketDialog = true },
                        onOpenBatchTicket = { showBatchTicketDialog = true },
                        onOpenCreatePppoe = { showCreatePppoeDialog = true }
                    )
                    }
                }
                "remote" -> {
                    RemoteStatusScreen(onBack = { viewModel.navigateTo("dashboard") })
                }
                "tickets" -> {
                    TicketsScreen(
                        viewModel = viewModel,
                        onOpenCreateTicket = { showCreateTicketDialog = true },
                        onOpenBatchTicket = { showBatchTicketDialog = true }
                    )
                }
                "clients" -> {
                    ClientsScreen(viewModel = viewModel)
                }
                "revenue" -> {
                    RevenueScreen(viewModel = viewModel)
                }
                "settings" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        subScreen = "menu",
                        onSelectSubScreen = { sub -> viewModel.navigateTo(sub) }
                    )
                }
                "routers" -> {
                    RoutersManagementSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                "hotspot_profiles" -> {
                    HotspotProfilesSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                "pppoe" -> {
                    PppoeManagementSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                "portal" -> {
                    TicketCustomizationSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                "maintenance" -> {
                    MaintenanceSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                "license" -> {
                    LicenseSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                "anticoupure" -> {
                    AntiCoupureSubScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("settings") })
                }
                else -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { dest -> viewModel.navigateTo(dest) },
                        onOpenCreateTicket = { showCreateTicketDialog = true },
                        onOpenBatchTicket = { showBatchTicketDialog = true },
                        onOpenCreatePppoe = { showCreatePppoeDialog = true }
                    )
                }
            }
        }
    }

    // Dialog: Create Single Ticket
    if (showCreateTicketDialog) {
        CreateTicketDialog(
            viewModel = viewModel,
            onDismiss = { showCreateTicketDialog = false }
        )
    }

    // Dialog: Batch Ticket
    if (showBatchTicketDialog) {
        BatchTicketDialog(
            viewModel = viewModel,
            onDismiss = { showBatchTicketDialog = false }
        )
    }

    // Dialog: Quick Add PPPoE
    if (showCreatePppoeDialog) {
        AddPppSecretDialog(
            profiles = pppProfiles.map { it.name },
            onDismiss = { showCreatePppoeDialog = false },
            onConfirm = { secret, client, phone, amount, method ->
                viewModel.addPppSecret(secret, client, phone, amount, method)
                showCreatePppoeDialog = false
            }
        )
    }
    }
}

