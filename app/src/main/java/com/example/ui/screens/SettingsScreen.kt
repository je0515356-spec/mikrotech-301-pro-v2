package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.database.CaptivePortalConfigEntity
import com.example.data.routeros.ConnectionStatus
import com.example.data.routeros.HotspotProfileModel
import com.example.data.routeros.PppProfileModel
import com.example.data.routeros.PppSecretModel
import com.example.ui.MikrotechViewModel
import com.example.ui.components.CommercialSignatureCard
import com.example.ui.components.LuxuryCard
import com.example.ui.components.formatAriary
import com.example.ui.components.formatBytes
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MikrotechViewModel,
    subScreen: String = "menu",
    onSelectSubScreen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (subScreen) {
        "routers" -> RoutersManagementSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        "hotspot_profiles" -> HotspotProfilesSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        "pppoe" -> PppoeManagementSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        "portal" -> TicketCustomizationSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        "maintenance" -> MaintenanceSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        "license" -> LicenseSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        "anticoupure" -> AntiCoupureSubScreen(viewModel, onBack = { onSelectSubScreen("menu") })
        else -> SettingsMenuMain(viewModel, onNavigateTo = onSelectSubScreen, modifier = modifier)
    }
}

@Composable
fun SettingsMenuMain(
    viewModel: MikrotechViewModel,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val routerIdentity by viewModel.routerIdentity.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isLightMode by viewModel.isLightMode.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "GESTIONNAIRE AVANCÉ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            SettingsMenuItem(
                title = if (isLightMode) "Thème Cristal Lumineux (Clair)" else "Thème Cobalt Électrique (Vibrant)",
                subtitle = "Toucher pour basculer entre mode sombre lumineux et mode clair",
                icon = if (isLightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                badge = if (isLightMode) "Mode Clair" else "Mode Sombre",
                badgeColor = if (isLightMode) MikrotechYellow else MikrotechBlue,
                onClick = { viewModel.toggleTheme() }
            )
        }

        item {
            SettingsMenuItem(
                title = "Routeurs MikroTik",
                subtitle = "IP, authentification, scan réseau, multi-routeurs",
                icon = Icons.Default.Router,
                badge = if (connectionStatus == ConnectionStatus.CONNECTED) "Connecté" else "Démo",
                badgeColor = if (connectionStatus == ConnectionStatus.CONNECTED) MikrotechGreen else MikrotechPurple,
                onClick = { onNavigateTo("routers") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Profils Hotspot",
                subtitle = "Gestion des bandes passantes (Kbps / Mbps) et limites",
                icon = Icons.Default.Speed,
                badgeColor = MikrotechCyan,
                onClick = { onNavigateTo("hotspot_profiles") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Gestion Clients & Profils PPPoE",
                subtitle = "Comptes clients, tarifs mensuels, abonnements",
                icon = Icons.Default.SettingsEthernet,
                badgeColor = MikrotechBlue,
                onClick = { onNavigateTo("pppoe") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Personnalisation des Tickets & Reçus",
                subtitle = "Nom de votre établissement, contact pour recharges, adresse, message",
                icon = Icons.Default.ConfirmationNumber,
                badge = "Personnalisé",
                badgeColor = MikrotechCyan,
                onClick = { onNavigateTo("portal") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Maintenance & Mises à Jour RouterOS",
                subtitle = "Mise à niveau sans perte de config, sauvegardes, diagnostic",
                icon = Icons.Default.SystemUpdateAlt,
                badge = "Sans Perte",
                badgeColor = MikrotechGreen,
                onClick = { onNavigateTo("maintenance") }
            )
        }

        item {
            val antiCoupureStatus by viewModel.antiCoupureStatus.collectAsState()
            SettingsMenuItem(
                title = "Protection Haute Disponibilité & Anti-Coupure",
                subtitle = "Persistance du temps restant après coupure électrique, protection horloge RTC",
                icon = Icons.Default.Shield,
                badge = if (antiCoupureStatus.isEnabled) "Actif (Sécurisé)" else "À Activer",
                badgeColor = if (antiCoupureStatus.isEnabled) MikrotechGreen else MikrotechOrange,
                onClick = { onNavigateTo("anticoupure") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Accueil & Détection Routeur",
                subtitle = "Grand logo MIKROTECH 301, scan Wi-Fi et mémorisation",
                icon = Icons.Default.Home,
                badge = "Accueil",
                badgeColor = MikrotechCyan,
                onClick = { viewModel.openWelcomeScreen() }
            )
        }

        item {
            SettingsMenuItem(
                title = "Licence Commerciale & À Propos",
                subtitle = "MIKROTECH 301 Madagascar • 038 28 171 00",
                icon = Icons.Default.VerifiedUser,
                badge = "ULTIMATE",
                badgeColor = MikrotechPurple,
                onClick = { onNavigateTo("license") }
            )
        }

        item {
            SettingsMenuItem(
                title = "Générateur de Licences Client",
                subtitle = "Créer et délivrer des clés STARTER, PRO, ULTIMATE",
                icon = Icons.Default.Key,
                badge = "Outil Vendeur",
                badgeColor = MikrotechGreen,
                onClick = { onNavigateTo("license") }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            CommercialSignatureCard()
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String? = null,
    badgeColor: Color = MikrotechCyan,
    onClick: () -> Unit
) {
    LuxuryCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechTextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MikrotechTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MikrotechTextMuted
                )
            }
        }
    }
}

// Sub-screen 1: Routers Management
@Composable
fun RoutersManagementSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val allRouters by viewModel.allRouters.collectAsState()
    val discoveredIps by viewModel.discoveredIps.collectAsState()

    var name by remember { mutableStateOf("MikroTik hAP ac2") }
    var host by remember { mutableStateOf("192.168.88.1") }
    var portText by remember { mutableStateOf("8728") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var apiType by remember { mutableStateOf("BINARY") } // "BINARY" or "REST"
    var useTls by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MikrotechBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = MikrotechCyan)
            }
            Text(
                text = "CONNEXION MIKROTIK",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechTextPrimary
            )
        }

        // Discovery Button
        Button(
            onClick = { viewModel.scanLocalMikrotik() },
            colors = ButtonDefaults.buttonColors(containerColor = MikrotechSurfaceVariant),
            border = BorderStroke(1.dp, MikrotechCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Radar, contentDescription = null, tint = MikrotechCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rechercher automatiquement sur le réseau local", color = MikrotechCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (discoveredIps.isNotEmpty()) {
            Text("Adresses MikroTik trouvées :", fontSize = 12.sp, color = MikrotechGreen)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                discoveredIps.forEach { ip ->
                    SuggestionChip(
                        onClick = { host = ip },
                        label = { Text(ip, color = MikrotechCyan, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        LuxuryCard {
            Text("Configuration de connexion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du routeur") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Adresse IP ou nom DNS (ex: 192.168.88.1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_router_host")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() } },
                    label = { Text("Port (8728 ou 443)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Protocol selector
                Surface(
                    color = if (apiType == "BINARY") MikrotechBlue else MikrotechSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            apiType = "BINARY"
                            portText = "8728"
                        }
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("API Socket (8728)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (apiType == "BINARY") Color.White else MikrotechTextSecondary)
                        Text("RouterOS 6 & 7", fontSize = 9.sp, color = if (apiType == "BINARY") Color.White else MikrotechTextMuted)
                    }
                }

                Surface(
                    color = if (apiType == "REST") MikrotechBlue else MikrotechSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            apiType = "REST"
                            portText = "443"
                            useTls = true
                        }
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("REST API (443)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (apiType == "REST") Color.White else MikrotechTextSecondary)
                        Text("RouterOS 7.1+", fontSize = 9.sp, color = if (apiType == "REST") Color.White else MikrotechTextMuted)
                    }
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nom d'utilisateur MikroTik") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_router_user")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_router_pass")
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useTls, onCheckedChange = { useTls = it })
                Text("Connexion sécurisée SSL / TLS", color = MikrotechTextPrimary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val p = portText.toIntOrNull() ?: 8728
                    viewModel.connectRouter(name, host.trim(), p, username.trim(), password, apiType, useTls)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("button_connect_mikrotik")
            ) {
                Text("Tester et Enregistrer la connexion", fontWeight = FontWeight.Bold)
            }
        }

        // Demo Mode button
        Button(
            onClick = {
                viewModel.switchToDemoMode()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MikrotechPurple.copy(alpha = 0.2f), contentColor = MikrotechCyan),
            border = BorderStroke(1.dp, MikrotechPurple),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Science, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Activer le Mode Démonstration (Simulateur)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// Sub-screen 2: Hotspot Profiles
@Composable
fun HotspotProfilesSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val profiles by viewModel.hotspotProfiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MikrotechBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = MikrotechCyan)
                }
                Text("PROFILS HOTSPOT", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouveau Profil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(profiles, key = { it.id.ifEmpty { it.name } }) { p ->
                LuxuryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)
                            Text(
                                text = "Débit (RX/TX) : ${p.rateLimit.ifEmpty { "Illimité" }}",
                                fontSize = 12.sp,
                                color = MikrotechTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Session timeout : ${p.sessionTimeout.ifEmpty { "Non défini" }} • Appareils : ${p.sharedUsers}",
                                fontSize = 11.sp,
                                color = MikrotechTextSecondary
                            )
                        }

                        IconButton(onClick = { viewModel.deleteHotspotProfile(p.id, p.name) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MikrotechRed)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHotspotProfileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { prof ->
                viewModel.addHotspotProfile(prof)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddHotspotProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (HotspotProfileModel) -> Unit
) {
    var name by remember { mutableStateOf("5H_1500Ar") }
    var rxRate by remember { mutableStateOf("2M") }
    var txRate by remember { mutableStateOf("5M") }
    var sessionTimeout by remember { mutableStateOf("5h") }
    var sharedUsers by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("NOUVEAU PROFIL HOTSPOT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du profil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Débits : Téléchargement (Download) & Envoi (Upload)", fontSize = 11.sp, color = MikrotechTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rxRate,
                        onValueChange = { rxRate = it },
                        label = { Text("Upload (ex: 2M)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = txRate,
                        onValueChange = { txRate = it },
                        label = { Text("Download (ex: 5M)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sessionTimeout,
                    onValueChange = { sessionTimeout = it },
                    label = { Text("Session Timeout (ex: 1h, 5h, 1d)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Button(
                        onClick = {
                            val rate = if (rxRate.isNotEmpty() && txRate.isNotEmpty()) "$rxRate/$txRate" else ""
                            onConfirm(
                                HotspotProfileModel(
                                    name = name,
                                    rateLimit = rate,
                                    sessionTimeout = sessionTimeout,
                                    sharedUsers = sharedUsers
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue)
                    ) {
                        Text("Créer Profil")
                    }
                }
            }
        }
    }
}

// Sub-screen 3: PPPoE Management
@Composable
fun PppoeManagementSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val pppSecrets by viewModel.pppSecrets.collectAsState()
    val pppCommercial by viewModel.pppoeCommercial.collectAsState()
    val profiles by viewModel.pppProfiles.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MikrotechBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = MikrotechCyan)
                }
                Text("GESTION PPPoE", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter Client", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(pppSecrets, key = { it.id.ifEmpty { it.name } }) { secret ->
                val comm = pppCommercial.find { it.username == secret.name }
                LuxuryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(secret.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MikrotechBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MikrotechBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(secret.profile, color = MikrotechBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            if (comm != null) {
                                Text("Client : ${comm.customerName} • Tél : ${comm.phone}", fontSize = 12.sp, color = MikrotechTextPrimary)
                                Text("Abonnement : ${formatAriary(comm.monthlyPriceAriary)} / mois", fontSize = 11.sp, color = MikrotechGreen, fontWeight = FontWeight.Bold)
                            }
                            if (secret.comment.isNotEmpty()) {
                                Text(secret.comment, fontSize = 10.sp, color = MikrotechTextMuted)
                            }
                        }

                        IconButton(onClick = { viewModel.deletePppSecret(secret.id, secret.name) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MikrotechRed)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPppSecretDialog(
            profiles = profiles.map { it.name },
            onDismiss = { showAddDialog = false },
            onConfirm = { secret, client, phone, amount, method ->
                viewModel.addPppSecret(secret, client, phone, amount, method)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddPppSecretDialog(
    profiles: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (PppSecretModel, String, String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf(profiles.firstOrNull() ?: "PPPOE_10M") }
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var monthlyPriceText by remember { mutableStateOf("100000") }
    var paymentMethod by remember { mutableStateOf("MVOLA") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("NOUVEAU CLIENT PPPoE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MikrotechBlue)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom d'utilisateur PPPoE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nom du client / Entreprise") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone de contact") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = monthlyPriceText,
                    onValueChange = { monthlyPriceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Tarif mensuel (Ariary)") },
                    trailingIcon = { Text("Ar", color = MikrotechCyan, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Button(
                        onClick = {
                            val price = monthlyPriceText.toLongOrNull() ?: 0L
                            val secret = PppSecretModel(
                                name = name.trim(),
                                password = password.trim(),
                                profile = selectedProfile,
                                comment = "Client: $customerName - $phone"
                            )
                            onConfirm(secret, customerName, phone, price, paymentMethod)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

// Sub-screen 4: Captive Portal Customizer
// Sub-screen 4: Ticket & Receipt Customizer
@Composable
fun TicketCustomizationSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val portalConfig by viewModel.captivePortalConfig.collectAsState()

    var zoneName by remember(portalConfig) { mutableStateOf(portalConfig.zoneName) }
    var phone by remember(portalConfig) { mutableStateOf(portalConfig.contactPhone) }
    var whatsapp by remember(portalConfig) { mutableStateOf(portalConfig.whatsapp) }
    var address by remember(portalConfig) { mutableStateOf(portalConfig.commercialAddress) }
    var welcomeText by remember(portalConfig) { mutableStateOf(portalConfig.welcomeText) }
    var ratesDescription by remember(portalConfig) { mutableStateOf(portalConfig.ratesDescription) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MikrotechBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = MikrotechCyan)
            }
            Text("PERSONNALISATION DES TICKETS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
        }

        Text(
            text = "Configurez les coordonnées de votre établissement. Vos clients verront votre propre nom, votre téléphone et votre adresse sur tous les tickets imprimés ou partagés.",
            fontSize = 12.sp,
            color = MikrotechTextSecondary
        )

        // Live Ticket Preview (Thermal 58mm/80mm style)
        Text("Aperçu en direct de votre Ticket Client :", fontSize = 12.sp, color = MikrotechCyan, fontWeight = FontWeight.Bold)
        Surface(
            color = Color(0xFF0A1224),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MikrotechCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = zoneName.ifEmpty { "NOM DE VOTRE ÉTABLISSEMENT" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MikrotechTextPrimary,
                    textAlign = TextAlign.Center
                )
                if (address.isNotEmpty()) {
                    Text(
                        text = "📍 $address",
                        fontSize = 11.sp,
                        color = MikrotechTextSecondary
                    )
                }
                Text(
                    text = welcomeText.ifEmpty { "Connexion Internet Fibre Haut Débit" },
                    fontSize = 10.sp,
                    color = MikrotechTextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MikrotechCardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text("CODE CONNEXION WI-FI", fontSize = 10.sp, color = MikrotechTextSecondary)
                Text(
                    text = "mt8492",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MikrotechCyan
                )

                Text("MOT DE PASSE / PIN : 4192", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Forfait : 1 Heure (500 Ar)", fontSize = 11.sp, color = MikrotechTextSecondary)
                    Text("🛡️ Anti-Coupure Actif", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikrotechGreen)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MikrotechCardBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Assistance & Recharges : ${phone.ifEmpty { "038 28 171 00" }}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechGreen
                )
                Text(
                    text = ratesDescription.ifEmpty { "Recharges disponibles au guichet • Merci de votre visite !" },
                    fontSize = 10.sp,
                    color = MikrotechTextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Propulsé par MIKROTECH 301 Madagascar",
                    fontSize = 9.sp,
                    color = MikrotechTextMuted
                )
            }
        }

        // Form Fields
        Text("Vos Coordonnées Commerciales :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)

        OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("Nom de votre Établissement (Cyber / Hôtel / Wi-Fi)") },
            placeholder = { Text("Ex: CYBER PLUS MAHAJANGA") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikrotechCyan,
                unfocusedBorderColor = MikrotechCardBorder,
                focusedTextColor = MikrotechTextPrimary,
                unfocusedTextColor = MikrotechTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Téléphone Vente de Recharges & Assistance") },
            placeholder = { Text("Ex: 034 11 222 33") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikrotechCyan,
                unfocusedBorderColor = MikrotechCardBorder,
                focusedTextColor = MikrotechTextPrimary,
                unfocusedTextColor = MikrotechTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = whatsapp,
            onValueChange = { whatsapp = it },
            label = { Text("Numéro WhatsApp Commercial") },
            placeholder = { Text("Ex: 034 11 222 33") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikrotechCyan,
                unfocusedBorderColor = MikrotechCardBorder,
                focusedTextColor = MikrotechTextPrimary,
                unfocusedTextColor = MikrotechTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Adresse / Quartier / Ville") },
            placeholder = { Text("Ex: Boulevard de la Corniche, Majunga") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikrotechCyan,
                unfocusedBorderColor = MikrotechCardBorder,
                focusedTextColor = MikrotechTextPrimary,
                unfocusedTextColor = MikrotechTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = welcomeText,
            onValueChange = { welcomeText = it },
            label = { Text("Slogan ou Message d'accueil sur le ticket") },
            placeholder = { Text("Ex: Connexion Internet Fibre Haut Débit") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikrotechCyan,
                unfocusedBorderColor = MikrotechCardBorder,
                focusedTextColor = MikrotechTextPrimary,
                unfocusedTextColor = MikrotechTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ratesDescription,
            onValueChange = { ratesDescription = it },
            label = { Text("Note de bas de ticket") },
            placeholder = { Text("Ex: Recharges disponibles au comptoir. Merci de votre fidélité !") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikrotechCyan,
                unfocusedBorderColor = MikrotechCardBorder,
                focusedTextColor = MikrotechTextPrimary,
                unfocusedTextColor = MikrotechTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.saveCaptivePortalConfig(
                    portalConfig.copy(
                        zoneName = zoneName.trim(),
                        contactPhone = phone.trim(),
                        whatsapp = whatsapp.trim(),
                        commercialAddress = address.trim(),
                        welcomeText = welcomeText.trim(),
                        ratesDescription = ratesDescription.trim()
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enregistrer la Personnalisation des Tickets", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// Sub-screen 5: Maintenance
@Composable
fun MaintenanceSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val routerResource by viewModel.routerResource.collectAsState()
    val updateInfo by viewModel.routerUpdateInfo.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val isInstallingUpdate by viewModel.isInstallingUpdate.collectAsState()

    var showRebootConfirm by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }
    var backupName by remember { mutableStateOf("mt301_backup") }

    LaunchedEffect(Unit) {
        if (updateInfo == null) {
            viewModel.checkForRouterOSUpdates()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MikrotechBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = MikrotechCyan)
            }
            Text("MAINTENANCE & SYSTÈME", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
        }

        // Section 1: Informations Matérielles
        LuxuryCard {
            Text("Informations Matérielles MikroTik", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Modèle : ${routerResource.boardName}", color = MikrotechTextPrimary)
            Text("Version RouterOS : ${routerResource.version}", color = MikrotechTextSecondary)
            Text("Architecture : ${routerResource.architecture}", color = MikrotechTextSecondary)
            Text("Uptime : ${routerResource.uptime}", color = MikrotechTextSecondary)
            Text("Mémoire Flash / Disque : ${formatBytes(routerResource.totalHdd)} (Libre: ${formatBytes(routerResource.freeHdd)})", color = MikrotechTextSecondary)
        }

        // Section 2: Détection & Mise à Jour RouterOS (Sans perte de configuration)
        LuxuryCard(borderColor = if (updateInfo?.updateAvailable == true) MikrotechOrange else MikrotechGreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mise à Jour RouterOS (Sans Perte de Config)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (updateInfo?.updateAvailable == true) MikrotechOrange else MikrotechCyan
                )

                if (updateInfo?.updateAvailable == true) {
                    Surface(
                        color = MikrotechOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "DISPONIBLE",
                            color = MikrotechOrange,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (updateInfo != null) {
                    Surface(
                        color = MikrotechGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "À JOUR",
                            color = MikrotechGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Version actuelle :", fontSize = 12.sp, color = MikrotechTextSecondary)
                Text(routerResource.version, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
            }

            if (updateInfo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dernière version (Canal Stable) :", fontSize = 12.sp, color = MikrotechTextSecondary)
                    Text(
                        "v${updateInfo?.latestVersion}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (updateInfo?.updateAvailable == true) MikrotechGreen else MikrotechTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updateInfo?.statusText ?: "",
                    fontSize = 11.sp,
                    color = if (updateInfo?.updateAvailable == true) MikrotechOrange else MikrotechGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MikrotechGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Garantie Sécurité MIKROTECH 301 : Sauvegarde intégrale automatique effectuée avant la mise à jour. Vos tickets, clients et profils sont 100% conservés sans perte de configuration.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.checkForRouterOSUpdates() },
                    enabled = !isCheckingUpdate && !isInstallingUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MikrotechCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recherche...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MikrotechCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vérifier Mise à Jour", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }

                if (updateInfo?.updateAvailable == true) {
                    Button(
                        onClick = { showUpdateConfirm = true },
                        enabled = !isInstallingUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isInstallingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sauvegarde & MAJ...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mettre à Niveau", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        LuxuryCard {
            Text("Sauvegarde RouterOS (.backup)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = backupName,
                onValueChange = { backupName = it },
                label = { Text("Nom du fichier de sauvegarde") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.backupRouter(backupName) },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Créer Sauvegarde sur le MikroTik", fontWeight = FontWeight.Bold)
            }
        }

        LuxuryCard(borderColor = MikrotechGreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Moteur d'Expiration Persistant RouterOS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Garantit que les tickets (1h, 24h, 30j) sont réellement coupés par le MikroTik même en cas de coupure de courant, de redémarrage ou si l'application Android est fermée.",
                        fontSize = 11.sp,
                        color = MikrotechTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.installAutonomousExpirationScript() },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Vérifier & Activer Moteur Autonome MikroTik", fontWeight = FontWeight.Bold)
            }
        }

        LuxuryCard(borderColor = MikrotechRed.copy(alpha = 0.5f)) {
            Text("Zone Critique : Redémarrage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechRed)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Le redémarrage du MikroTik interrompra momentanément toutes les sessions Hotspot et PPPoE actives.", fontSize = 11.sp, color = MikrotechTextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { showRebootConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Redémarrer le routeur MikroTik", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            containerColor = MikrotechSurface,
            title = { Text("Confirmer le redémarrage ?") },
            text = { Text("Êtes-vous absolument certain de vouloir redémarrer l'équipement MikroTik maintenant ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rebootRouter()
                        showRebootConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed)
                ) {
                    Text("Oui, Redémarrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) { Text("Annuler") }
            }
        )
    }

    if (showUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MikrotechGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mise à niveau RouterOS Sécurisée", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Installer la version officielle RouterOS v${updateInfo?.latestVersion} ?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "• Étape 1 : MIKROTECH 301 crée automatiquement une sauvegarde intégrale de sécurité.\n" +
                        "• Étape 2 : Le routeur télécharge et installe la mise à niveau officielle.\n" +
                        "• Vos clients, tickets actifs, comptes PPPoE et profils restent 100% préservés sans aucune perte de configuration.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateConfirm = false
                        viewModel.installRouterOSUpdateSafe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen)
                ) {
                    Text("Oui, Mettre à Jour (Sans Perte)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = false }) { Text("Annuler") }
            }
        )
    }
}

// Sub-screen 6: License & About
@Composable
fun LicenseSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val license by viewModel.licenseInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MikrotechBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = MikrotechCyan)
            }
            Text("LICENCE COMMERCIALE", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
        }

        LuxuryCard(borderColor = MikrotechPurple) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Formule de Licence", fontSize = 11.sp, color = MikrotechTextSecondary)
                    Text("ULTIMATE PRO", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MikrotechPurple)
                }
                Surface(
                    color = MikrotechGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ACTIVE & VALIDE", color = MikrotechGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MikrotechCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            val clipboardManager = LocalClipboardManager.current
            var showActivationDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ID d'installation Android :", fontSize = 11.sp, color = MikrotechTextSecondary)
                    Text(license.androidInstallId, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MikrotechCyan)
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(license.androidInstallId))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechSurfaceVariant),
                    border = BorderStroke(1.dp, MikrotechCardBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = MikrotechCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copier", fontSize = 11.sp, color = MikrotechCyan)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Clé active : ${license.licenseKey}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MikrotechTextSecondary)
            Text("Formule : ${license.planType} • Perpétuelle & Sans abonnement cloud", fontSize = 11.sp, color = MikrotechGreen)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showActivationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechPurple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.VpnKey, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Saisir / Renouveler une Clé de Licence", fontWeight = FontWeight.Bold)
            }

            if (showActivationDialog) {
                var inputKey by remember { mutableStateOf("") }
                var selectedPlan by remember { mutableStateOf("ULTIMATE PRO") }

                Dialog(onDismissRequest = { showActivationDialog = false }) {
                    Surface(
                        color = MikrotechSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MikrotechPurple),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("ACTIVATION DE LICENCE MIKROTECH", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)
                            Text("Entrez la clé de licence fournie avec votre routeur MikroTik :", fontSize = 12.sp, color = MikrotechTextSecondary)

                            OutlinedTextField(
                                value = inputKey,
                                onValueChange = { inputKey = it.uppercase() },
                                label = { Text("Clé de licence (ex: MT301-XXXX-XXXX)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Formule :", fontSize = 11.sp, color = MikrotechTextSecondary)
                            val plans = listOf("STARTER", "BUSINESS PRO", "ULTIMATE PRO")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                plans.forEach { pl ->
                                    val isSel = selectedPlan == pl
                                    Surface(
                                        color = if (isSel) MikrotechPurple else MikrotechSurfaceVariant,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).clickable { selectedPlan = pl }
                                    ) {
                                        Text(
                                            text = pl,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else MikrotechTextSecondary,
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showActivationDialog = false }) { Text("Annuler") }
                                Button(
                                    onClick = {
                                        if (inputKey.isNotEmpty()) {
                                            viewModel.activateLicense(inputKey, selectedPlan)
                                            showActivationDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechPurple)
                                ) {
                                    Text("Valider la Licence", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Developer License Generator Tool
        var showGeneratorDialog by remember { mutableStateOf(false) }

        LuxuryCard(borderColor = MikrotechCyan) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Générateur de Licences MIKROTECH 301", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Outil réservé au développeur/vendeur pour créer et délivrer les clés de licence officielles aux acheteurs de routeurs.",
                        fontSize = 11.sp,
                        color = MikrotechTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { showGeneratorDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddModerator, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Générer une Clé pour un Client", fontWeight = FontWeight.Bold)
            }

            if (showGeneratorDialog) {
                val context = LocalContext.current
                val clipboardManager = LocalClipboardManager.current
                var masterPin by remember { mutableStateOf("") }
                var showPassword by remember { mutableStateOf(false) }
                var isUnlocked by remember { mutableStateOf(false) }
                var clientInstallId by remember { mutableStateOf("") }
                var genPlan by remember { mutableStateOf("ULTIMATE PRO") }
                var generatedKeyResult by remember { mutableStateOf("") }
                var pinError by remember { mutableStateOf(false) }

                Dialog(onDismissRequest = { showGeneratorDialog = false }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MikrotechCyan),
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("ESPACE DÉVELOPPEUR MIKROTECH 301", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MikrotechCyan)

                            if (!isUnlocked) {
                                Text("Accès sécurisé réservé au propriétaire (038 28 171 00).\nVeuillez saisir votre Mot de passe Maître :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                OutlinedTextField(
                                    value = masterPin,
                                    onValueChange = {
                                        masterPin = it
                                        pinError = false
                                    },
                                    label = { Text("Mot de passe Développeur") },
                                    placeholder = { Text("Saisissez votre mot de passe") },
                                    isError = pinError,
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Afficher/Masquer le mot de passe"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (pinError) {
                                    Text("Mot de passe incorrect. Accès refusé.", color = MikrotechRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showGeneratorDialog = false }) { Text("Annuler") }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val entered = masterPin.trim()
                                            if (entered == "0382817100Eric." || entered == "0382817100Eric") {
                                                isUnlocked = true
                                                pinError = false
                                            } else {
                                                pinError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen)
                                    ) {
                                        Text("Déverrouiller le Générateur", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text("Saisissez l'ID Android ou le numéro de série du MikroTik du client :", fontSize = 11.sp, color = MikrotechGreen)

                                OutlinedTextField(
                                    value = clientInstallId,
                                    onValueChange = { clientInstallId = it },
                                    label = { Text("ID Android ou Serial MikroTik") },
                                    placeholder = { Text("ex: ANDR-88319-MG ou RBD52G") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                            Text("Formule à délivrer :", fontSize = 11.sp, color = MikrotechTextSecondary)
                            val plans = listOf("STARTER", "BUSINESS PRO", "ULTIMATE PRO")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                plans.forEach { pl ->
                                    val isSel = genPlan == pl
                                    Surface(
                                        color = if (isSel) MikrotechBlue else MikrotechSurfaceVariant,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).clickable { genPlan = pl }
                                    ) {
                                        Text(
                                            text = pl,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else MikrotechTextSecondary,
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val tag = when (genPlan) {
                                        "STARTER" -> "STR"
                                        "BUSINESS PRO" -> "PRO"
                                        else -> "ULT"
                                    }
                                    val suffix = (1000 + (System.currentTimeMillis() % 9000)).toString()
                                    val cleanId = clientInstallId.replace(Regex("[^A-Za-z0-9]"), "").takeLast(5).ifEmpty { "MDG" }.uppercase()
                                    generatedKeyResult = "MT301-$tag-$cleanId-$suffix"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Calculer la Clé de Licence", fontWeight = FontWeight.Bold)
                            }

                            if (generatedKeyResult.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFF091122),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MikrotechGreen),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("CLÉ OFFICIELLE GÉNÉRÉE :", fontSize = 10.sp, color = MikrotechTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = generatedKeyResult,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = MikrotechGreen
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(generatedKeyResult))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MikrotechSurfaceVariant),
                                                border = BorderStroke(1.dp, MikrotechCardBorder),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Copier", fontSize = 11.sp, color = MikrotechCyan)
                                            }

                                            Button(
                                                onClick = {
                                                    val shareText = "Voici votre clé de licence MIKROTECH 301 ($genPlan) : $generatedKeyResult\nAssistance : 038 28 171 00"
                                                    val sendIntent = Intent.createChooser(
                                                        Intent().apply {
                                                            action = Intent.ACTION_SEND
                                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                                            type = "text/plain"
                                                        },
                                                        "Envoyer Clé de Licence"
                                                    )
                                                    context.startActivity(sendIntent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Partager", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            }

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showGeneratorDialog = false }) { Text("Fermer") }
                            }
                        }
                    }
                }
            }
        }

        CommercialSignatureCard()

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// Sub-screen 7: Bouclier Anti-Coupure (Délestage Jirama & Reprise Temps Restant)
@Composable
fun AntiCoupureSubScreen(
    viewModel: MikrotechViewModel,
    onBack: () -> Unit
) {
    val antiCoupureStatus by viewModel.antiCoupureStatus.collectAsState()
    val isInstalling by viewModel.isInstallingAntiCoupure.collectAsState()
    val hotspotUsers by viewModel.hotspotUsers.collectAsState()

    val activeUsersWithTime = hotspotUsers.filter { it.uptime != "0s" && !it.disabled }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = MikrotechCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "BOUCLIER ANTI-COUPURE",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechCyan,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Persistance du Temps Restant & Sécurité Horloge RTC",
                    fontSize = 12.sp,
                    color = MikrotechTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Status Card
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (antiCoupureStatus.isEnabled) MikrotechGreen else MikrotechOrange
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            (if (antiCoupureStatus.isEnabled) MikrotechGreen else MikrotechOrange).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (antiCoupureStatus.isEnabled) MikrotechGreen else MikrotechOrange,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (antiCoupureStatus.isEnabled) "BOUCLIER ANTI-COUPURE ACTIF" else "BOUCLIER DÉSACTIVÉ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (antiCoupureStatus.isEnabled) MikrotechGreen else MikrotechOrange
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = antiCoupureStatus.statusMessage,
                        fontSize = 12.sp,
                        color = MikrotechTextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MikrotechCardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Horloge RTC", fontSize = 11.sp, color = MikrotechTextSecondary)
                    Text(
                        text = "Protégée (Cloud/NTP)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MikrotechGreen
                    )
                }
                Column {
                    Text("Cadence Sync", fontSize = 11.sp, color = MikrotechTextSecondary)
                    Text(
                        text = "Toutes les 1 min",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MikrotechCyan
                    )
                }
                Column {
                    Text("Délestage", fontSize = 11.sp, color = MikrotechTextSecondary)
                    Text(
                        text = "Zéro Reset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MikrotechGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explication Pédagogique du Cas Concret
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MikrotechYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COMMENT FONCTIONNE L'ANTI-COUPURE ?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechYellow
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Fonctionnement en cas de coupure de courant inopinée :",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MikrotechTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step 1
            AntiCoupureStepItem(
                stepNumber = "1",
                title = "Vente d'un ticket",
                desc = "Le client achète un ticket d'1 heure (ou 1 jour / 30 jours).",
                color = MikrotechCyan
            )

            // Step 2
            AntiCoupureStepItem(
                stepNumber = "2",
                title = "Consommation normale",
                desc = "Le client surfe pendant 30 minutes. Il lui reste donc exactement 30 minutes.",
                color = MikrotechBlue
            )

            // Step 3
            AntiCoupureStepItem(
                stepNumber = "3",
                title = "Coupure électrique inattendue",
                desc = "L'électricité se coupe subitement. Le routeur s'éteint sans préavis.",
                color = MikrotechOrange
            )

            // Step 4
            AntiCoupureStepItem(
                stepNumber = "4",
                title = "Rétablissement du courant",
                desc = "Le routeur redémarre. Le Bouclier Anti-Coupure recalcule et verrouille le quota du ticket à 30 minutes restantes.",
                color = MikrotechGreen
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MikrotechGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MikrotechGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MikrotechGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RÉSULTAT GARANTI : Le ticket NE RECOMMENCE JAMAIS à 1h (zéro gratuité non voulue pour le vendeur) et le client ne perd rien de son temps payé !",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechGreen,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aperçu des Tickets Protégés en ce moment
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TICKETS ACTIFS EN COURS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechCyan
                )
                Surface(
                    color = MikrotechGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${activeUsersWithTime.size} protégé(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MikrotechGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeUsersWithTime.isEmpty()) {
                Text(
                    text = "Aucun ticket actif actuellement. Dès qu'un client se connecte, son temps est surveillé et sauvegardé en temps réel.",
                    fontSize = 12.sp,
                    color = MikrotechTextSecondary
                )
            } else {
                activeUsersWithTime.take(4).forEach { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(u.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
                            Text("Écoulé: ${u.uptime} / Limite: ${u.limitUptime}", fontSize = 11.sp, color = MikrotechTextSecondary)
                        }
                        Surface(
                            color = MikrotechCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Reste: ${u.remainingUptimeText}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MikrotechCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MikrotechCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3 Piliers techniques
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "3 PROTECTIONS TECHNIQUES ROUTEROS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan
            )
            Spacer(modifier = Modifier.height(10.dp))

            TechnicalPointRow(
                icon = Icons.Default.AccessTime,
                title = "1. Protection Horloge 1970 (NTP & Cloud Time)",
                desc = "Sans pile RTC matérielle (comme hAP ac² ou hAP mini), le routeur risque de redémarrer en 1970. Le bouclier synchronise l'heure immédiatement."
            )
            Spacer(modifier = Modifier.height(8.dp))
            TechnicalPointRow(
                icon = Icons.Default.Save,
                title = "2. Mémorisation Uptime Réel (Live Commit)",
                desc = "Le temps consommé est sauvegardé en tâche de fond de façon optimisée pour protéger l'usure de la flash."
            )
            Spacer(modifier = Modifier.height(8.dp))
            TechnicalPointRow(
                icon = Icons.Default.PlayArrow,
                title = "3. Script @startup de Recalcul Automatique",
                desc = "Au retour du courant, le routeur réajuste le temps restant net de chaque client avant toute reconnexion."
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        if (isInstalling) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MikrotechCyan)
            }
        } else {
            Button(
                onClick = { viewModel.installAntiCoupureShield() },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (antiCoupureStatus.isEnabled) "Actualiser / Réinstaller le Bouclier" else "Activer le Bouclier Anti-Coupure",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { viewModel.simulatePowerOutageTest() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MikrotechYellow),
                border = BorderStroke(1.dp, MikrotechYellow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MikrotechYellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚡ Tester la Reprise Post-Coupure",
                    fontWeight = FontWeight.Bold,
                    color = MikrotechYellow
                )
            }

            if (antiCoupureStatus.isEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = { viewModel.disableAntiCoupureShield() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Désactiver le bouclier (Non recommandé)", color = MikrotechRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        CommercialSignatureCard()
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun AntiCoupureStepItem(
    stepNumber: String,
    title: String,
    desc: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(stepNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            Text(desc, fontSize = 11.sp, color = MikrotechTextSecondary, lineHeight = 15.sp)
        }
    }
}

@Composable
fun TechnicalPointRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = MikrotechCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
            Text(desc, fontSize = 11.sp, color = MikrotechTextSecondary, lineHeight = 15.sp)
        }
    }
}

