package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.routeros.HotspotUserModel
import com.example.ui.MikrotechViewModel
import com.example.ui.components.LuxuryCard
import com.example.ui.components.formatAriary
import com.example.ui.components.formatBytes
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    viewModel: MikrotechViewModel,
    onOpenCreateTicket: () -> Unit,
    onOpenBatchTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hotspotUsers by viewModel.hotspotUsers.collectAsState()
    val profiles by viewModel.hotspotProfiles.collectAsState()
    val portalConfig by viewModel.captivePortalConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tous, 1: Actifs, 2: Disponibles, 3: Expirés

    var viewingTicketForPrint by remember { mutableStateOf<HotspotUserModel?>(null) }
    var editingTicket by remember { mutableStateOf<HotspotUserModel?>(null) }
    var ticketToDelete by remember { mutableStateOf<HotspotUserModel?>(null) }
    var ticketToSell by remember { mutableStateOf<HotspotUserModel?>(null) }
    var ticketToReset by remember { mutableStateOf<HotspotUserModel?>(null) }
    var showCustomizeHeaderDialog by remember { mutableStateOf(false) }

    val filteredList = remember(hotspotUsers, searchQuery, selectedTab) {
        hotspotUsers.filter { user ->
            val matchesQuery = searchQuery.isEmpty() ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.comment.contains(searchQuery, ignoreCase = true) ||
                    user.profile.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                1 -> !user.disabled && user.uptime != "0s" // Actifs
                2 -> user.uptime == "0s" && !user.disabled // Disponibles
                3 -> user.disabled // Expirés ou désactivés
                else -> true
            }
            matchesQuery && matchesTab
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreateTicket,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouveau Ticket", fontWeight = FontWeight.Bold) },
                containerColor = MikrotechBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_create_ticket")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Batch button & Storefront Customization
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un ticket...", fontSize = 13.sp, color = MikrotechTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MikrotechCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = MikrotechTextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechCyan,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedContainerColor = MikrotechSurface,
                        unfocusedContainerColor = MikrotechSurface,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showCustomizeHeaderDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MikrotechSurfaceVariant)
                        .border(1.dp, MikrotechCardBorder, RoundedCornerShape(12.dp))
                        .testTag("customize_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Mon Établissement",
                        tint = MikrotechYellow
                    )
                }

                IconButton(
                    onClick = onOpenBatchTicket,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MikrotechSurfaceVariant)
                        .border(1.dp, MikrotechCardBorder, RoundedCornerShape(12.dp))
                        .testTag("batch_ticket_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryAdd,
                        contentDescription = "Générer un lot",
                        tint = MikrotechCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MikrotechSurface,
                contentColor = MikrotechCyan,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MikrotechCyan
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MikrotechCardBorder, RoundedCornerShape(12.dp))
            ) {
                val tabs = listOf("Tous (${hotspotUsers.size})", "Actifs", "Disponibles", "Expirés")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MikrotechCyan else MikrotechTextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tickets List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ConfirmationNumber,
                            contentDescription = null,
                            tint = MikrotechTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucun ticket trouvé",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MikrotechTextSecondary
                        )
                        Text(
                            text = "Créez un nouveau ticket ou générez un lot",
                            fontSize = 12.sp,
                            color = MikrotechTextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id.ifEmpty { it.name } }) { user ->
                        TicketCardItem(
                            user = user,
                            onPrint = { viewingTicketForPrint = user },
                            onEdit = { editingTicket = user },
                            onDelete = { ticketToDelete = user },
                            onSell = { ticketToSell = user },
                            onReset = { ticketToReset = user },
                            onAntiCoupure = { viewModel.navigateTo("anticoupure") }
                        )
                    }
                }
            }
        }
    }

    // Modal: Visual Printable Ticket Voucher
    viewingTicketForPrint?.let { ticket ->
        VisualTicketPrintDialog(
            ticket = ticket,
            portalConfig = portalConfig,
            onDismiss = { viewingTicketForPrint = null },
            onOpenCustomize = { showCustomizeHeaderDialog = true }
        )
    }

    // Modal: Customize Shop Header
    if (showCustomizeHeaderDialog) {
        CustomizeShopHeaderDialog(
            portalConfig = portalConfig,
            onDismiss = { showCustomizeHeaderDialog = false },
            onSave = { updated ->
                viewModel.saveCaptivePortalConfig(updated)
                showCustomizeHeaderDialog = false
            }
        )
    }

    // Modal: Edit Ticket
    editingTicket?.let { ticket ->
        EditTicketDialog(
            ticket = ticket,
            profiles = profiles.map { it.name },
            onDismiss = { editingTicket = null },
            onSave = { updated ->
                viewModel.updateHotspotUser(ticket.id, updated)
                editingTicket = null
            }
        )
    }

    // Modal: Confirm Delete
    ticketToDelete?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToDelete = null },
            containerColor = MikrotechSurface,
            titleContentColor = MikrotechRed,
            title = { Text("Supprimer le ticket ?") },
            text = {
                Text(
                    text = "Voulez-vous vraiment supprimer définitivement le ticket '${ticket.name}' du routeur MikroTik ? Cette action est irréversible.",
                    color = MikrotechTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHotspotUser(ticket.id, ticket.name)
                        ticketToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed)
                ) {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ticketToDelete = null }) {
                    Text("Annuler", color = MikrotechTextSecondary)
                }
            }
        )
    }

    // Modal: Confirm Reset Counters
    ticketToReset?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToReset = null },
            containerColor = MikrotechSurface,
            title = { Text("Réinitialiser les compteurs ?") },
            text = {
                Text(
                    text = "Réinitialiser le temps d'utilisation et les octets consommés pour '${ticket.name}' ?",
                    color = MikrotechTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetHotspotCounters(ticket.id, ticket.name)
                        ticketToReset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue)
                ) {
                    Text("Réinitialiser", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ticketToReset = null }) {
                    Text("Annuler", color = MikrotechTextSecondary)
                }
            }
        )
    }

    // Modal: Sell Ticket (Cash / Mobile Money)
    ticketToSell?.let { ticket ->
        SellTicketDialog(
            ticket = ticket,
            onDismiss = { ticketToSell = null },
            onConfirmSell = { amount, method ->
                viewModel.recordManualSale(
                    type = "HOTSPOT_TICKET",
                    reference = ticket.name,
                    plan = ticket.profile,
                    amount = amount,
                    paymentMethod = method,
                    customerName = "Guichet"
                )
                ticketToSell = null
            }
        )
    }
}

@Composable
fun TicketCardItem(
    user: HotspotUserModel,
    onPrint: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSell: () -> Unit,
    onReset: () -> Unit,
    onAntiCoupure: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val statusColor = when {
        user.disabled -> MikrotechRed
        user.uptime != "0s" -> MikrotechGreen
        else -> MikrotechPurple
    }
    val statusText = when {
        user.disabled -> "Expiré / Suspendu"
        user.uptime != "0s" -> "Actif (${user.uptime})"
        else -> "Disponible"
    }

    LuxuryCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MikrotechSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCardBorder, MikrotechCyan)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MikrotechCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MikrotechTextPrimary
                        )
                    }
                }

                if (user.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PIN: ${user.password}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MikrotechTextSecondary
                    )
                }
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MikrotechTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(MikrotechSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Imprimer / Partager Voucher", color = MikrotechCyan) },
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = MikrotechCyan) },
                        onClick = {
                            expandedMenu = false
                            onPrint()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Encaisser / Vendre", color = MikrotechGreen) },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null, tint = MikrotechGreen) },
                        onClick = {
                            expandedMenu = false
                            onSell()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Modifier", color = MikrotechTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MikrotechTextPrimary) },
                        onClick = {
                            expandedMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Réinitialiser compteurs", color = MikrotechOrange) },
                        leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MikrotechOrange) },
                        onClick = {
                            expandedMenu = false
                            onReset()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Protection Anti-Coupure", color = MikrotechCyan) },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = MikrotechCyan) },
                        onClick = {
                            expandedMenu = false
                            onAntiCoupure()
                        }
                    )
                    Divider(color = MikrotechCardBorder)
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MikrotechRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MikrotechRed) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info pills row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MikrotechBlue.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = user.profile,
                    color = MikrotechBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (user.limitUptime.isNotEmpty()) {
                Surface(
                    color = MikrotechSurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Durée: ${user.limitUptime}",
                        color = MikrotechTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (user.uptime != "0s" && user.limitUptime.isNotEmpty()) {
                Surface(
                    color = MikrotechCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MikrotechCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Reste: ${user.remainingUptimeText}",
                            color = MikrotechCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (user.comment.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = user.comment,
                fontSize = 11.sp,
                color = MikrotechTextMuted
            )
        }

        if (user.totalBytesUsed > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Consommation: ${formatBytes(user.totalBytesUsed)} (RX: ${formatBytes(user.bytesIn)} / TX: ${formatBytes(user.bytesOut)})",
                    fontSize = 10.sp,
                    color = MikrotechTextSecondary
                )
            }
        }
    }
}

@Composable
fun VisualTicketPrintDialog(
    ticket: HotspotUserModel,
    portalConfig: com.example.core.database.CaptivePortalConfigEntity,
    onDismiss: () -> Unit,
    onOpenCustomize: () -> Unit = {}
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCyan, MikrotechBlue))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TICKET WI-FI OFFICIEL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan,
                        letterSpacing = 1.sp
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MikrotechTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The Ticket Card (Simulated ESC/POS 58mm/80mm voucher design)
                Surface(
                    color = Color(0xFF0A1224),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCardBorder, MikrotechCyan.copy(alpha = 0.5f)))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = portalConfig.zoneName.ifEmpty { "NOM DE VOTRE ÉTABLISSEMENT" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MikrotechTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = onOpenCustomize, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Personnaliser nom établissement", tint = MikrotechCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (portalConfig.commercialAddress.isNotEmpty()) {
                            Text(
                                text = "📍 ${portalConfig.commercialAddress}",
                                fontSize = 11.sp,
                                color = MikrotechTextSecondary
                            )
                        }

                        if (portalConfig.welcomeText.isNotEmpty()) {
                            Text(
                                text = portalConfig.welcomeText,
                                fontSize = 10.sp,
                                color = MikrotechTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MikrotechCardBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Credentials Block
                        Text("IDENTIFIANT CONNEXION", fontSize = 10.sp, color = MikrotechTextSecondary)
                        Text(
                            text = ticket.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MikrotechCyan
                        )

                        if (ticket.password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("MOT DE PASSE / PIN", fontSize = 10.sp, color = MikrotechTextSecondary)
                            Text(
                                text = ticket.password,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Details table
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Forfait:", fontSize = 11.sp, color = MikrotechTextSecondary)
                            Text(ticket.profile, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
                        }
                        if (ticket.limitUptime.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Durée:", fontSize = 11.sp, color = MikrotechTextSecondary)
                                Text(ticket.limitUptime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikrotechTextPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sécurité:", fontSize = 11.sp, color = MikrotechTextSecondary)
                            Text("🛡️ Haute Disponibilité (Zéro Perte)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikrotechGreen)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stylized QR code representation
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(70.dp)) {
                                drawRect(Color.Black, Offset(0f, 0f), Size(20.dp.toPx(), 20.dp.toPx()))
                                drawRect(Color.White, Offset(4.dp.toPx(), 4.dp.toPx()), Size(12.dp.toPx(), 12.dp.toPx()))
                                drawRect(Color.Black, Offset(6.dp.toPx(), 6.dp.toPx()), Size(8.dp.toPx(), 8.dp.toPx()))

                                drawRect(Color.Black, Offset(size.width - 20.dp.toPx(), 0f), Size(20.dp.toPx(), 20.dp.toPx()))
                                drawRect(Color.Black, Offset(0f, size.height - 20.dp.toPx()), Size(20.dp.toPx(), 20.dp.toPx()))

                                drawRect(Color.Black, Offset(size.width / 2 - 4.dp.toPx(), size.height / 2 - 4.dp.toPx()), Size(8.dp.toPx(), 8.dp.toPx()))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Assistance & Recharges : ${portalConfig.contactPhone.ifEmpty { "038 28 171 00" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MikrotechGreen
                        )
                        Text(
                            text = "Assistance & Recharges : ${portalConfig.contactPhone.ifEmpty { "038 28 171 00" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MikrotechGreen
                        )
                        Text(
                            text = "Propulsé par MIKROTECH 301 Madagascar",
                            fontSize = 9.sp,
                            color = MikrotechTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: WhatsApp share & Android share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val contact = portalConfig.contactPhone.ifEmpty { "038 28 171 00" }
                            val addr = if (portalConfig.commercialAddress.isNotEmpty()) "📍 ${portalConfig.commercialAddress}\n" else ""
                            val welc = if (portalConfig.welcomeText.isNotEmpty()) "\n💡 ${portalConfig.welcomeText}\n" else ""
                            val shareText = """
                                🌐 ${portalConfig.zoneName}
                                ${addr}━━━━━━━━━━━━━━━━━━━━
                                🎟️ TICKET WI-FI HAUT DÉBIT
                                👤 Utilisateur : ${ticket.name}
                                🔑 Mot de passe : ${ticket.password}
                                ⏱️ Forfait : ${ticket.profile}
                                🛡️ Haute Disponibilité : Temps net garanti
                                ━━━━━━━━━━━━━━━━━━━━
                                Assistance & Recharges : $contact${welc}
                                Service propulsé par MIKROTECH 301
                            """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(sendIntent)
                            } catch (e: Exception) {
                                val chooser = Intent.createChooser(
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    },
                                    "Partager le ticket"
                                )
                                context.startActivity(chooser)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val addr = if (portalConfig.commercialAddress.isNotEmpty()) "📍 ${portalConfig.commercialAddress}\n" else ""
                            val shareText = """
                                🌐 ${portalConfig.zoneName}
                                ${addr}🎟️ Ticket Wi-Fi :
                                Identifiant : ${ticket.name}
                                Mot de passe : ${ticket.password}
                                Forfait : ${ticket.profile} (${ticket.limitUptime})
                                Assistance & Recharges : ${portalConfig.contactPhone}
                            """.trimIndent()
                            val sendIntent = Intent.createChooser(
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                },
                                "Partager Ticket"
                            )
                            context.startActivity(sendIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditTicketDialog(
    ticket: HotspotUserModel,
    profiles: List<String>,
    onDismiss: () -> Unit,
    onSave: (HotspotUserModel) -> Unit
) {
    var name by remember { mutableStateOf(ticket.name) }
    var password by remember { mutableStateOf(ticket.password) }
    var selectedProfile by remember { mutableStateOf(ticket.profile) }
    var limitUptime by remember { mutableStateOf(ticket.limitUptime) }
    var comment by remember { mutableStateOf(ticket.comment) }
    var disabled by remember { mutableStateOf(ticket.disabled) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCardBorder, MikrotechCyan))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "MODIFIER LE TICKET",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechCyan
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom d'utilisateur") },
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
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
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
                    value = limitUptime,
                    onValueChange = { limitUptime = it },
                    label = { Text("Limite durée (ex: 1h, 3h, 1d)") },
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
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechCyan,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = disabled,
                        onCheckedChange = { disabled = it },
                        colors = CheckboxDefaults.colors(checkedColor = MikrotechRed)
                    )
                    Text("Suspendre / Désactiver ce ticket", color = MikrotechTextPrimary, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = MikrotechTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                ticket.copy(
                                    name = name,
                                    password = password,
                                    profile = selectedProfile,
                                    limitUptime = limitUptime,
                                    comment = comment,
                                    disabled = disabled
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue)
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SellTicketDialog(
    ticket: HotspotUserModel,
    onDismiss: () -> Unit,
    onConfirmSell: (Long, String) -> Unit
) {
    var amountText by remember { mutableStateOf("1000") }
    var selectedPaymentMethod by remember { mutableStateOf("ESPECES") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCardBorder, MikrotechGreen))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ENCAISSER LA VENTE DU TICKET",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechGreen
                )

                Text(
                    text = "Ticket: ${ticket.name} (${ticket.profile})",
                    fontSize = 13.sp,
                    color = MikrotechTextSecondary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Montant encaissé (Ariary)") },
                    trailingIcon = { Text("Ar", color = MikrotechCyan, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechGreen,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mode de paiement :", fontSize = 12.sp, color = MikrotechTextSecondary)

                val methods = listOf(
                    "ESPECES" to "Espèces",
                    "MVOLA" to "MVola",
                    "ORANGE_MONEY" to "Orange Money",
                    "AIRTEL_MONEY" to "Airtel Money"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedPaymentMethod == key) MikrotechSurfaceVariant else Color.Transparent)
                                .clickable { selectedPaymentMethod = key }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == key,
                                onClick = { selectedPaymentMethod = key },
                                colors = RadioButtonDefaults.colors(selectedColor = MikrotechGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = MikrotechTextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = MikrotechTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull() ?: 0L
                            onConfirmSell(amount, selectedPaymentMethod)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen)
                    ) {
                        Text("Confirmer l'encaissement", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizeShopHeaderDialog(
    portalConfig: com.example.core.database.CaptivePortalConfigEntity,
    onDismiss: () -> Unit,
    onSave: (com.example.core.database.CaptivePortalConfigEntity) -> Unit
) {
    var zoneName by remember(portalConfig) { mutableStateOf(portalConfig.zoneName) }
    var phone by remember(portalConfig) { mutableStateOf(portalConfig.contactPhone) }
    var address by remember(portalConfig) { mutableStateOf(portalConfig.commercialAddress) }
    var welcomeText by remember(portalConfig) { mutableStateOf(portalConfig.welcomeText) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechYellow, MikrotechCyan))),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MikrotechYellow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MON ÉTABLISSEMENT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MikrotechYellow
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MikrotechTextMuted)
                    }
                }

                Text(
                    text = "Ce nom et ce numéro apparaîtront sur tous les tickets que vous imprimez ou partagez sur WhatsApp.",
                    fontSize = 11.sp,
                    color = MikrotechTextSecondary
                )

                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Nom de votre Cyber / Hôtel / Commerce") },
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
                    label = { Text("Téléphone pour Recharges & Assistance") },
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
                    label = { Text("Adresse / Quartier") },
                    placeholder = { Text("Ex: Bazar Be, Majunga") },
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
                    label = { Text("Slogan / Message sur le ticket") },
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

                Button(
                    onClick = {
                        onSave(
                            portalConfig.copy(
                                zoneName = zoneName.trim(),
                                contactPhone = phone.trim(),
                                commercialAddress = address.trim(),
                                welcomeText = welcomeText.trim()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enregistrer pour mes tickets", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

