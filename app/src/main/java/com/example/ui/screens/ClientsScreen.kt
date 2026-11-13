package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.core.database.PppoeCommercialEntity
import com.example.data.routeros.HotspotActiveModel
import com.example.data.routeros.PppActiveModel
import com.example.data.routeros.PppSecretModel
import com.example.ui.MikrotechViewModel
import com.example.ui.components.LuxuryCard
import com.example.ui.components.formatAriary
import com.example.ui.components.formatBytes
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClientsScreen(
    viewModel: MikrotechViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pppCommercial by viewModel.pppoeCommercial.collectAsState()
    val hotspotActive by viewModel.hotspotActive.collectAsState()
    val pppActive by viewModel.pppActive.collectAsState()
    val pppProfiles by viewModel.pppProfiles.collectAsState()
    val portalConfig by viewModel.captivePortalConfig.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Abonnés & Mois, 1: Hotspot Live, 2: PPPoE Live
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, EXPIRING, EXPIRED

    var showAddSubscriberDialog by remember { mutableStateOf(false) }
    var subscriberToRenew by remember { mutableStateOf<PppoeCommercialEntity?>(null) }
    var hotspotToKick by remember { mutableStateOf<HotspotActiveModel?>(null) }
    var pppToKick by remember { mutableStateOf<PppActiveModel?>(null) }

    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000

    // Filter subscribers
    val filteredSubscribers = remember(pppCommercial, searchQuery, statusFilter) {
        pppCommercial.filter { sub ->
            val matchesQuery = searchQuery.isEmpty() ||
                    sub.customerName.contains(searchQuery, ignoreCase = true) ||
                    sub.phone.contains(searchQuery) ||
                    sub.username.contains(searchQuery, ignoreCase = true) ||
                    sub.address.contains(searchQuery, ignoreCase = true)

            val daysRemaining = ((sub.nextRenewalDate - now) / dayMs).toInt()
            val matchesFilter = when (statusFilter) {
                "ACTIVE" -> daysRemaining > 5 && sub.status != "SUSPENDED"
                "EXPIRING" -> daysRemaining in 0..5 && sub.status != "SUSPENDED"
                "EXPIRED" -> daysRemaining < 0 || sub.status == "EXPIRED"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val activeCount = remember(pppCommercial) {
        pppCommercial.count { ((it.nextRenewalDate - now) / dayMs).toInt() > 5 && it.status != "SUSPENDED" }
    }
    val expiringCount = remember(pppCommercial) {
        pppCommercial.count {
            val d = ((it.nextRenewalDate - now) / dayMs).toInt()
            d in 0..5 && it.status != "SUSPENDED"
        }
    }
    val expiredCount = remember(pppCommercial) {
        pppCommercial.count { ((it.nextRenewalDate - now) / dayMs).toInt() < 0 || it.status == "EXPIRED" }
    }

    val filteredHotspot = remember(hotspotActive, searchQuery) {
        hotspotActive.filter {
            searchQuery.isEmpty() ||
                    it.user.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery) ||
                    it.macAddress.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPpp = remember(pppActive, searchQuery) {
        pppActive.filter {
            searchQuery.isEmpty() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery) ||
                    it.callerId.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Action Bar with Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SUIVI CLIENTS & ABONNÉS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MikrotechCyan,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Gestion par mois, jours restants & consommation",
                    fontSize = 11.sp,
                    color = MikrotechTextSecondary
                )
            }

            Button(
                onClick = { showAddSubscriberDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nouvel Abonné", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher client, téléphone, login, adresse...", fontSize = 12.sp, color = MikrotechTextMuted) },
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Tabs (3 Tabs)
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
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Abonnés (${pppCommercial.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hotspot (${hotspotActive.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsEthernet, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PPPoE (${pppActive.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content
        when (selectedTab) {
            0 -> {
                // Filter chips for subscribers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChipItem(
                        label = "Tous (${pppCommercial.size})",
                        isSelected = statusFilter == "ALL",
                        color = MikrotechCyan,
                        onClick = { statusFilter = "ALL" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipItem(
                        label = "Actifs ($activeCount)",
                        isSelected = statusFilter == "ACTIVE",
                        color = MikrotechGreen,
                        onClick = { statusFilter = "ACTIVE" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipItem(
                        label = "< 5j ($expiringCount)",
                        isSelected = statusFilter == "EXPIRING",
                        color = MikrotechYellow,
                        onClick = { statusFilter = "EXPIRING" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipItem(
                        label = "Expirés ($expiredCount)",
                        isSelected = statusFilter == "EXPIRED",
                        color = MikrotechRed,
                        onClick = { statusFilter = "EXPIRED" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredSubscribers.isEmpty()) {
                    EmptyClientsView(message = "Aucun abonné ne correspond aux critères de recherche.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredSubscribers, key = { it.username }) { sub ->
                            SubscriberCard(
                                sub = sub,
                                portalShopName = portalConfig.zoneName,
                                portalContact = portalConfig.contactPhone,
                                onRenew = { subscriberToRenew = sub },
                                onRelanceWhatsApp = {
                                    sendWhatsAppRelance(context, sub, portalConfig.zoneName, portalConfig.contactPhone)
                                },
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sub.phone}"))
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    viewModel.deletePppSecret("", sub.username)
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                if (filteredHotspot.isEmpty()) {
                    EmptyClientsView(message = "Aucun client Hotspot connecté actuellement.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredHotspot, key = { it.id.ifEmpty { it.user } }) { client ->
                            HotspotClientCard(
                                client = client,
                                onKick = { hotspotToKick = client }
                            )
                        }
                    }
                }
            }

            2 -> {
                if (filteredPpp.isEmpty()) {
                    EmptyClientsView(message = "Aucune session PPPoE active actuellement.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPpp, key = { it.id.ifEmpty { it.name } }) { client ->
                            PppClientCard(
                                client = client,
                                onKick = { pppToKick = client }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Renew Subscriber Dialog (+1 Month / +Custom)
    subscriberToRenew?.let { sub ->
        RenewSubscriberDialog(
            sub = sub,
            onDismiss = { subscriberToRenew = null },
            onConfirmRenew = { months, amount, method ->
                viewModel.renewSubscription(sub.username, months, amount, method)
                subscriberToRenew = null
            }
        )
    }

    // Modal: Add New Subscriber Dialog
    if (showAddSubscriberDialog) {
        AddNewSubscriberDialog(
            profiles = pppProfiles.map { it.name },
            onDismiss = { showAddSubscriberDialog = false },
            onConfirm = { username, password, customerName, phone, address, profile, price, method, months ->
                val secret = PppSecretModel(
                    name = username,
                    password = password,
                    profile = profile,
                    comment = "Abonné: $customerName • Tél: $phone"
                )
                viewModel.addPppSecret(secret, customerName, phone, price, method)
                showAddSubscriberDialog = false
            }
        )
    }

    // Modal: Kick Hotspot client
    hotspotToKick?.let { client ->
        AlertDialog(
            onDismissRequest = { hotspotToKick = null },
            containerColor = MikrotechSurface,
            title = { Text("Déconnecter le client Hotspot ?") },
            text = {
                Text(
                    text = "Voulez-vous forcer la déconnexion de '${client.user}' (${client.address}) ? La session sera immédiatement fermée sur le MikroTik.",
                    color = MikrotechTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.kickHotspotActive(client.id, client.user)
                        hotspotToKick = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed)
                ) {
                    Text("Déconnecter", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { hotspotToKick = null }) {
                    Text("Annuler", color = MikrotechTextSecondary)
                }
            }
        )
    }

    // Modal: Kick PPPoE client
    pppToKick?.let { client ->
        AlertDialog(
            onDismissRequest = { pppToKick = null },
            containerColor = MikrotechSurface,
            title = { Text("Terminer la session PPPoE ?") },
            text = {
                Text(
                    text = "Voulez-vous couper la session PPPoE de '${client.name}' (${client.address}) ?",
                    color = MikrotechTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.kickPppActive(client.id, client.name)
                        pppToKick = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed)
                ) {
                    Text("Couper session", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pppToKick = null }) {
                    Text("Annuler", color = MikrotechTextSecondary)
                }
            }
        )
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else MikrotechSurface,
        border = BorderStroke(1.dp, if (isSelected) color else MikrotechCardBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else MikrotechTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
fun SubscriberCard(
    sub: PppoeCommercialEntity,
    portalShopName: String,
    portalContact: String,
    onRenew: () -> Unit,
    onRelanceWhatsApp: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    val daysRemaining = ((sub.nextRenewalDate - now) / dayMs).toInt()

    val (badgeText, badgeColor) = when {
        sub.status == "SUSPENDED" -> "Suspendu" to MikrotechRed
        daysRemaining > 5 -> "$daysRemaining jours restants" to MikrotechGreen
        daysRemaining in 1..5 -> "⚠️ $daysRemaining j restants (Relance)" to MikrotechYellow
        daysRemaining == 0 -> "⚠️ Expire AUJOURD'HUI" to MikrotechYellow
        else -> "Expiré (-${-daysRemaining}j)" to MikrotechRed
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val renewalDateText = remember(sub.nextRenewalDate) { dateFormat.format(Date(sub.nextRenewalDate)) }

    // Parse consumption info from notes or default calculation
    val (consumedText, progressFraction) = remember(sub.notes, daysRemaining) {
        val totalDays = 30f
        val daysUsed = (30 - daysRemaining).coerceIn(0, 30).toFloat()
        val frac = (daysUsed / totalDays).coerceIn(0f, 1f)
        val noteData = if (sub.notes.isNotEmpty()) sub.notes else "Consommation standard"
        noteData to frac
    }

    LuxuryCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Customer Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MikrotechBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MikrotechBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sub.customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MikrotechTextPrimary
                        )
                        Text(
                            text = "Login : ${sub.username} • Tél : ${sub.phone}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MikrotechCyan
                        )
                    }
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MikrotechCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Details: Monthly Plan & Consommation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Abonnement Mensuel :", fontSize = 10.sp, color = MikrotechTextSecondary)
                    Text(
                        text = "${formatAriary(sub.monthlyPriceAriary)} / mois",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MikrotechGreen
                    )
                    Text("Mode: ${sub.paymentMethod}", fontSize = 10.sp, color = MikrotechTextMuted)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Échéance Prochaine :", fontSize = 10.sp, color = MikrotechTextSecondary)
                    Text(
                        text = renewalDateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechTextPrimary
                    )
                    if (sub.address.isNotEmpty()) {
                        Text(sub.address, fontSize = 10.sp, color = MikrotechTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Consommation déjà consommée + Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Consommation déjà consommée :",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}% du mois",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progressFraction > 0.85f) MikrotechRed else MikrotechCyan
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = if (progressFraction > 0.85f) MikrotechRed else MikrotechCyan,
                    trackColor = MikrotechSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = consumedText,
                    fontSize = 10.sp,
                    color = MikrotechTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Renew + WhatsApp + Call + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onRenew,
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Renouveler +1 Mois", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRelanceWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MikrotechBlue.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Appeler", tint = MikrotechBlue, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MikrotechRed.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MikrotechRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun sendWhatsAppRelance(
    context: Context,
    sub: PppoeCommercialEntity,
    shopName: String,
    shopPhone: String
) {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    val daysRemaining = ((sub.nextRenewalDate - now) / dayMs).toInt()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    val dateStr = dateFormat.format(Date(sub.nextRenewalDate))

    val message = """
        🌐 *${shopName}* — Rappel Abonnement Internet
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Bonjour ${sub.customerName},
        
        Votre abonnement Internet arrive à échéance le *${dateStr}* (${if (daysRemaining > 0) "dans $daysRemaining jours" else "échéance dépassée"}).
        
        • Compte : ${sub.username}
        • Mensualité : ${formatAriary(sub.monthlyPriceAriary)}
        • État actuel : ${sub.notes.ifEmpty { "En cours" }}
        
        Pour renouveler et maintenir votre ligne active sans coupure, merci d'envoyer votre règlement par Mvola / Airtel / Orange Money au :
        📞 *${shopPhone.ifEmpty { "038 28 171 00" }}*
        
        Merci pour votre confiance !
    """.trimIndent()

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://api.whatsapp.com/send?phone=${sub.phone.replace(" ", "")}&text=${Uri.encode(message)}")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Envoyer relance"))
    }
}

@Composable
fun RenewSubscriberDialog(
    sub: PppoeCommercialEntity,
    onDismiss: () -> Unit,
    onConfirmRenew: (months: Int, amount: Long, method: String) -> Unit
) {
    var months by remember { mutableIntStateOf(1) }
    var amountText by remember { mutableStateOf(sub.monthlyPriceAriary.toString()) }
    var paymentMethod by remember { mutableStateOf(sub.paymentMethod) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechGreen, MikrotechCyan))),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RENOUVELER L'ABONNEMENT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechGreen
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MikrotechTextMuted)
                    }
                }

                Text(
                    text = "Abonné : ${sub.customerName} (${sub.username})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechTextPrimary
                )

                // Select duration in months
                Text("Durée du renouvellement :", fontSize = 12.sp, color = MikrotechTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "1 Mois", 2 to "2 Mois", 3 to "3 Mois", 6 to "6 Mois").forEach { (m, label) ->
                        val isSelected = months == m
                        Surface(
                            color = if (isSelected) MikrotechGreen else MikrotechSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    months = m
                                    amountText = (sub.monthlyPriceAriary * m).toString()
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MikrotechTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Montant total perçu (Ariary)") },
                    trailingIcon = { Text("Ar", color = MikrotechGreen, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechGreen,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment method
                Text("Mode de règlement :", fontSize = 12.sp, color = MikrotechTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("MVOLA", "ORANGE_MONEY", "AIRTEL_MONEY", "ESPECES").forEach { m ->
                        val isSelected = paymentMethod == m
                        Surface(
                            color = if (isSelected) MikrotechGreen.copy(alpha = 0.2f) else MikrotechSurfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, MikrotechGreen) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paymentMethod = m }
                        ) {
                            Text(
                                text = m.replace("_", " "),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MikrotechGreen else MikrotechTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        onConfirmRenew(months, amount, paymentMethod)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Valider le Renouvellement & Caisse", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AddNewSubscriberDialog(
    profiles: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (username: String, pass: String, name: String, phone: String, address: String, profile: String, price: Long, method: String, months: Int) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("1234") }
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf(profiles.firstOrNull() ?: "default") }
    var priceText by remember { mutableStateOf("45000") }
    var paymentMethod by remember { mutableStateOf("MVOLA") }
    var months by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCyan, MikrotechBlue))),
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
                    Text(
                        text = "ENREGISTRER UN ABONNÉ MENSUEL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MikrotechTextMuted)
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nom complet du Client / Entreprise") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechCyan,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone (ex: 034...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MikrotechCyan,
                            unfocusedBorderColor = MikrotechCardBorder,
                            focusedTextColor = MikrotechTextPrimary,
                            unfocusedTextColor = MikrotechTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse / Quartier") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MikrotechCyan,
                            unfocusedBorderColor = MikrotechCardBorder,
                            focusedTextColor = MikrotechTextPrimary,
                            unfocusedTextColor = MikrotechTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Identifiant / Login") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MikrotechCyan,
                            unfocusedBorderColor = MikrotechCardBorder,
                            focusedTextColor = MikrotechTextPrimary,
                            unfocusedTextColor = MikrotechTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
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
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Tarif mensuel (Ariary)") },
                    trailingIcon = { Text("Ar", color = MikrotechCyan, fontWeight = FontWeight.Bold) },
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
                        if (username.isNotEmpty() && customerName.isNotEmpty()) {
                            val price = priceText.toLongOrNull() ?: 0L
                            onConfirm(username.trim(), password.trim(), customerName.trim(), phone.trim(), address.trim(), selectedProfile, price, paymentMethod, months)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Créer le compte & Enregistrer Abonnement", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun HotspotClientCard(
    client: HotspotActiveModel,
    onKick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LuxuryCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = client.user,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MikrotechCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MikrotechGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Connecté",
                            color = MikrotechGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "IP: ${client.address} • MAC: ${client.macAddress}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MikrotechTextSecondary
                )
                Text(
                    text = "Session: ${client.uptime} • Consommé: ${formatBytes(client.bytesIn + client.bytesOut)}",
                    fontSize = 10.sp,
                    color = MikrotechTextMuted
                )
            }

            Button(
                onClick = onKick,
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed.copy(alpha = 0.2f), contentColor = MikrotechRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Couper", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PppClientCard(
    client: PppActiveModel,
    onKick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LuxuryCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = client.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MikrotechBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MikrotechBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "PPPoE En Ligne",
                            color = MikrotechBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "IP: ${client.address} • Caller: ${client.callerId.ifEmpty { "Inconnu" }}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MikrotechTextSecondary
                )
                Text(
                    text = "Durée session : ${client.uptime}",
                    fontSize = 10.sp,
                    color = MikrotechTextMuted
                )
            }

            Button(
                onClick = onKick,
                colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed.copy(alpha = 0.2f), contentColor = MikrotechRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Déconnecter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyClientsView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MikrotechTextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MikrotechTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
