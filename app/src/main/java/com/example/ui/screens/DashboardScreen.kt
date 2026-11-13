package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.routeros.ConnectionStatus
import com.example.ui.MikrotechViewModel
import com.example.ui.components.CommercialSignatureCard
import com.example.ui.components.LuxuryCard
import com.example.ui.components.StatCard
import com.example.ui.components.formatAriary
import com.example.ui.components.formatBytes
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: MikrotechViewModel,
    onNavigate: (String) -> Unit,
    onOpenCreateTicket: () -> Unit,
    onOpenBatchTicket: () -> Unit,
    onOpenCreatePppoe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routerResource by viewModel.routerResource.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val hotspotUsers by viewModel.hotspotUsers.collectAsState()
    val hotspotActive by viewModel.hotspotActive.collectAsState()
    val pppActive by viewModel.pppActive.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val thisMonthRevenue by viewModel.thisMonthRevenue.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    val allSales by viewModel.allSales.collectAsState()

    val totalActiveClients = hotspotActive.size + pppActive.size
    val activeTicketsCount = hotspotUsers.count { !it.disabled && it.uptime != "0s" }
    val availableTicketsCount = hotspotUsers.count { it.uptime == "0s" }
    val expiredTicketsCount = hotspotUsers.count { it.disabled }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MikrotechBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(MikrotechBlue, Color(0xFF102B5B), Color(0xFF07152C))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = MikrotechCyan, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MIKROTECH 301", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("CENTRE DE CONTRÔLE", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                            Surface(
                                color = if (connectionStatus == ConnectionStatus.CONNECTED) MikrotechGreen.copy(alpha = 0.2f) else MikrotechOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    if (connectionStatus == ConnectionStatus.CONNECTED) "● EN LIGNE" else "● HORS LIGNE",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    color = if (connectionStatus == ConnectionStatus.CONNECTED) MikrotechGreen else MikrotechOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text("Votre réseau, en temps réel.", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "${routerResource.boardName}  •  RouterOS ${routerResource.version}",
                            color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$totalActiveClients", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text("Clients en ligne", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${routerResource.cpuLoad}%", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text("CPU MikroTik", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.refreshAll() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualiser les données", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("ACCÈS RAPIDE", fontWeight = FontWeight.Bold, color = MikrotechTextPrimary, fontSize = 13.sp, letterSpacing = 1.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardQuickAction("Ticket", Icons.Default.AddCircle, MikrotechCyan, Modifier.weight(1f), onOpenCreateTicket)
                DashboardQuickAction("Lot", Icons.Default.ConfirmationNumber, MikrotechPurple, Modifier.weight(1f), onOpenBatchTicket)
                DashboardQuickAction("PPPoE", Icons.Default.PersonAdd, MikrotechGreen, Modifier.weight(1f), onOpenCreatePppoe)
                DashboardQuickAction("Clients", Icons.Default.Groups, MikrotechOrange, Modifier.weight(1f)) { onNavigate("clients") }
            }
        }

        // Status banner only if disconnected
        if (connectionStatus == ConnectionStatus.DISCONNECTED || connectionStatus == ConnectionStatus.ERROR) {
            item {
                Surface(
                    color = MikrotechOrange.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechOrange, MikrotechYellow)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = MikrotechOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AUCUN ROUTEUR CONNECTÉ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MikrotechOrange
                                )
                                Text(
                                    text = "Connectez-vous au Wi-Fi du MikroTik (192.168.88.1)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigate("routers") },
                            colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Détecter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 1: Financial & Commercial Overview
        item {
            Text(
                text = "PERFORMANCES COMMERCIALES (ARIARY)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Ventes Aujourd'hui",
                    value = formatAriary(todayRevenue),
                    subtitle = "${todaySales.size} vente(s)",
                    icon = Icons.Default.Paid,
                    iconColor = MikrotechGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("revenue") }
                )

                StatCard(
                    title = "Revenus du Mois",
                    value = formatAriary(thisMonthRevenue),
                    subtitle = "Novembre 2026",
                    icon = Icons.Default.TrendingUp,
                    iconColor = MikrotechBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("revenue") }
                )
            }
        }

        // Section 2: Clients & Tickets
        item {
            Text(
                text = "CLIENTS ET BILLETERIE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Actifs",
                    value = "$totalActiveClients",
                    subtitle = "${hotspotActive.size} Hotspot • ${pppActive.size} PPPoE",
                    icon = Icons.Default.SignalCellularAlt,
                    iconColor = MikrotechCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("clients") }
                )

                StatCard(
                    title = "Tickets Disponibles",
                    value = "$availableTicketsCount",
                    subtitle = "$activeTicketsCount actifs / $expiredTicketsCount expirés",
                    icon = Icons.Default.ConfirmationNumber,
                    iconColor = MikrotechPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("tickets") }
                )
            }
        }

        // Section 3: Hardware & Router Telemetry
        item {
            Text(
                text = "SANTÉ DU ROUTEUR MIKROTIK",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            LuxuryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = routerResource.boardName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MikrotechTextPrimary
                        )
                        Text(
                            text = "${routerResource.version} • Uptime : ${routerResource.uptime}",
                            fontSize = 12.sp,
                            color = MikrotechTextSecondary
                        )
                    }

                    Surface(
                        color = MikrotechSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MikrotechCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${routerResource.cpuFrequency} MHz",
                                fontSize = 11.sp,
                                color = MikrotechTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CPU Load bar
                val cpuPercent = routerResource.cpuLoad
                val cpuColor = when {
                    cpuPercent > 80 -> MikrotechRed
                    cpuPercent > 50 -> MikrotechOrange
                    else -> MikrotechGreen
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Charge Processeur (CPU)", fontSize = 12.sp, color = MikrotechTextSecondary)
                    Text("$cpuPercent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cpuColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (cpuPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = cpuColor,
                    trackColor = MikrotechSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Memory RAM usage bar
                val memPercent = routerResource.memoryUsagePercent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mémoire RAM", fontSize = 12.sp, color = MikrotechTextSecondary)
                    Text(
                        text = "$memPercent% (${formatBytes(routerResource.totalMemory - routerResource.freeMemory)} / ${formatBytes(routerResource.totalMemory)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MikrotechCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (memPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MikrotechCyan,
                    trackColor = MikrotechSurfaceVariant
                )
            }
        }

        // Section 4: Revenue History 7 Days Chart
        item {
            Text(
                text = "HISTORIQUE FINANCIER (7 DERNIERS JOURS)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            LuxuryCard {
                Text(
                    text = "Ventes quotidiennes (Ariary)",
                    fontSize = 13.sp,
                    color = MikrotechTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))

                val sampleDays = listOf(
                    "J-6" to 12000L,
                    "J-5" to 18500L,
                    "J-4" to 25000L,
                    "J-3" to 15000L,
                    "J-2" to 32000L,
                    "Hier" to 28000L,
                    "Auj" to (todayRevenue.coerceAtLeast(1500L))
                )
                val maxVal = sampleDays.maxOfOrNull { it.second } ?: 1L

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / (sampleDays.size * 2f)
                        val step = size.width / sampleDays.size

                        for (i in sampleDays.indices) {
                            val (day, amount) = sampleDays[i]
                            val barHeight = ((amount.toFloat() / maxVal.toFloat()) * (size.height - 24.dp.toPx())).coerceAtLeast(6.dp.toPx())
                            val x = (i * step) + (step - barWidth) / 2f
                            val y = size.height - 20.dp.toPx() - barHeight

                            // Gradient bar
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(MikrotechCyan, MikrotechBlue)
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }

                    // Labels below bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (item in sampleDays) {
                            Text(
                                text = item.first,
                                fontSize = 10.sp,
                                color = MikrotechTextMuted
                            )
                        }
                    }
                }
            }
        }

        // Section 5: Quick Actions
        item {
            Text(
                text = "ACTIONS RAPIDES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MikrotechCyan,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        title = "Nouveau Ticket",
                        icon = Icons.Default.AddCircle,
                        color = MikrotechCyan,
                        onClick = onOpenCreateTicket,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = "Générer un Lot",
                        icon = Icons.Default.LibraryAdd,
                        color = MikrotechBlue,
                        onClick = onOpenBatchTicket,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        title = "Nouveau Client PPPoE",
                        icon = Icons.Default.PersonAdd,
                        color = MikrotechGreen,
                        onClick = onOpenCreatePppoe,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = "Profils Hotspot",
                        icon = Icons.Default.Speed,
                        color = MikrotechPurple,
                        onClick = { onNavigate("hotspot_profiles") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        title = "Comptabilité & Ventes",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = MikrotechOrange,
                        onClick = { onNavigate("revenue") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = "Routeurs MikroTik",
                        icon = Icons.Default.Router,
                        color = MikrotechTextPrimary,
                        onClick = { onNavigate("routers") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            CommercialSignatureCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), color.copy(alpha = 0.4f)))),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardQuickAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MikrotechSurfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 15.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Text(title, color = MikrotechTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
