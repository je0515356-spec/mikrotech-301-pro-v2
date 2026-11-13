package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MikrotechViewModel
import com.example.ui.theme.*
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketDialog(
    viewModel: MikrotechViewModel,
    onDismiss: () -> Unit
) {
    val profiles by viewModel.hotspotProfiles.collectAsState()
    val portalConfig by viewModel.captivePortalConfig.collectAsState()
    val random = remember { SecureRandom() }

    fun generateUser() = "mt${1000 + random.nextInt(9000)}"
    fun generatePass() = "${1000 + random.nextInt(9000)}"

    var username by remember { mutableStateOf(generateUser()) }
    var password by remember { mutableStateOf(generatePass()) }
    var selectedProfile by remember {
        mutableStateOf(profiles.firstOrNull()?.name ?: "1H_500Ar")
    }
    var uptimeLimit by remember { mutableStateOf("1h") }
    var priceText by remember { mutableStateOf("500") }
    var comment by remember(portalConfig) {
        mutableStateOf("Ticket ${portalConfig.zoneName} • Tél: ${portalConfig.contactPhone}")
    }
    var recordAsSale by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf("ESPECES") }

    val durationPresets = listOf(
        "1h" to "1 Heure",
        "3h" to "3 Heures",
        "12h" to "12 Heures",
        "1d" to "24 Heures",
        "3d" to "3 Jours",
        "7d" to "7 Jours",
        "30d" to "30 Jours"
    )

    val pricePresets = listOf(500L, 1000L, 2000L, 2500L, 5000L, 10000L)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCardBorder, MikrotechCyan))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOUVEAU TICKET HOTSPOT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan,
                        letterSpacing = 0.5.sp
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MikrotechTextMuted)
                    }
                }

                // Username & Generate
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Identifiant / Nom utilisateur") },
                    trailingIcon = {
                        IconButton(onClick = { username = generateUser() }) {
                            Icon(Icons.Default.Autorenew, contentDescription = "Générer", tint = MikrotechCyan)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechCyan,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ticket_username")
                )

                // Password & Generate
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe / Code PIN") },
                    trailingIcon = {
                        IconButton(onClick = { password = generatePass() }) {
                            Icon(Icons.Default.Autorenew, contentDescription = "Générer", tint = MikrotechCyan)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechCyan,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ticket_password")
                )

                // Profile Selector
                Text("Profil Hotspot :", fontSize = 12.sp, color = MikrotechTextSecondary)
                val availableProfiles = if (profiles.isNotEmpty()) profiles.map { it.name } else listOf("default", "1H_500Ar", "3H_1000Ar", "24H_2500Ar")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableProfiles.take(3).forEach { prof ->
                        val isSelected = selectedProfile == prof
                        Surface(
                            color = if (isSelected) MikrotechBlue else MikrotechSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedProfile = prof }
                        ) {
                            Text(
                                text = prof,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) Color.White else MikrotechTextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Duration Presets
                Text("Durée de validité :", fontSize = 12.sp, color = MikrotechTextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durationPresets.take(4).forEach { (code, label) ->
                            val isSelected = uptimeLimit == code
                            Surface(
                                color = if (isSelected) MikrotechCyan.copy(alpha = 0.2f) else MikrotechSurfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, MikrotechCyan) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { uptimeLimit = code }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MikrotechCyan else MikrotechTextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Price Presets & input
                Text("Prix du ticket (Ariary) :", fontSize = 12.sp, color = MikrotechTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pricePresets.take(4).forEach { p ->
                        val isSelected = priceText == p.toString()
                        Surface(
                            color = if (isSelected) MikrotechGreen.copy(alpha = 0.2f) else MikrotechSurfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, MikrotechGreen) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { priceText = p.toString() }
                        ) {
                            Text(
                                text = "$p Ar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MikrotechGreen else MikrotechTextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Montant personnalisé (Ariary)") },
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

                // Comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire / Réf vendeur") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechCyan,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Record as sale toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = recordAsSale,
                        onCheckedChange = { recordAsSale = it },
                        colors = CheckboxDefaults.colors(checkedColor = MikrotechGreen)
                    )
                    Text("Encaisser immédiatement dans les revenus", color = MikrotechTextPrimary, fontSize = 12.sp)
                }

                if (recordAsSale) {
                    Text("Mode d'encaissement :", fontSize = 11.sp, color = MikrotechTextSecondary)
                    val methods = listOf("ESPECES" to "Espèces", "MVOLA" to "MVola", "ORANGE_MONEY" to "Orange Money")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        methods.forEach { (key, label) ->
                            val isSel = paymentMethod == key
                            Surface(
                                color = if (isSel) MikrotechGreen.copy(alpha = 0.2f) else MikrotechSurfaceVariant,
                                border = if (isSel) BorderStroke(1.dp, MikrotechGreen) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { paymentMethod = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSel) MikrotechGreen else MikrotechTextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Notice Anti-Coupure
                Surface(
                    color = MikrotechGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MikrotechGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MikrotechGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Protection Anti-Coupure : En cas de coupure de courant, le temps restant reprend exactement (aucun reset).",
                            fontSize = 11.sp,
                            color = MikrotechGreen,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Button(
                    onClick = {
                        val price = priceText.toLongOrNull() ?: 0L
                        viewModel.createTicket(
                            username = username.trim(),
                            password = password.trim(),
                            profile = selectedProfile,
                            uptimeLimit = uptimeLimit,
                            dataLimitBytes = 0L,
                            priceAriary = price,
                            comment = comment,
                            recordAsSale = recordAsSale,
                            paymentMethod = paymentMethod
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_create_ticket")
                ) {
                    Text("Créer le ticket sur le MikroTik", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun BatchTicketDialog(
    viewModel: MikrotechViewModel,
    onDismiss: () -> Unit
) {
    val profiles by viewModel.hotspotProfiles.collectAsState()

    var count by remember { mutableIntStateOf(10) }
    var prefix by remember { mutableStateOf("mt") }
    var selectedProfile by remember {
        mutableStateOf(profiles.firstOrNull()?.name ?: "1H_500Ar")
    }
    var uptimeLimit by remember { mutableStateOf("1h") }
    var priceText by remember { mutableStateOf("500") }

    val batchCounts = listOf(10, 20, 50, 100)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MikrotechSurface,
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MikrotechCardBorder, MikrotechBlue))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GÉNÉRER UN LOT DE TICKETS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = MikrotechTextMuted)
                    }
                }

                Text("Quantité à générer :", fontSize = 12.sp, color = MikrotechTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    batchCounts.forEach { c ->
                        val isSel = count == c
                        Surface(
                            color = if (isSel) MikrotechBlue else MikrotechSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { count = c }
                        ) {
                            Text(
                                text = "$c",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSel) Color.White else MikrotechTextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Préfixe identifiant (ex: mt, wifi, zone)") },
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
                    value = uptimeLimit,
                    onValueChange = { uptimeLimit = it },
                    label = { Text("Durée limite (ex: 1h, 3h, 1d)") },
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
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Prix par ticket (Ariary)") },
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

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = MikrotechGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MikrotechGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MikrotechGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tous les tickets bénéficient de la protection Anti-Coupure (Reprise du temps net).",
                            fontSize = 11.sp,
                            color = MikrotechGreen,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val price = priceText.toLongOrNull() ?: 0L
                        viewModel.generateBatchTickets(
                            count = count,
                            prefix = prefix,
                            profile = selectedProfile,
                            uptimeLimit = uptimeLimit,
                            dataLimitBytes = 0L,
                            priceAriary = price
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_batch_tickets")
                ) {
                    Text("Générer $count tickets maintenant", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
