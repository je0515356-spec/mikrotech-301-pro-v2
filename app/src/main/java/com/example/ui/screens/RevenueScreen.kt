package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.core.database.SaleEntity
import com.example.ui.MikrotechViewModel
import com.example.ui.components.LuxuryCard
import com.example.ui.components.StatCard
import com.example.ui.components.formatAriary
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RevenueScreen(
    viewModel: MikrotechViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allSales by viewModel.allSales.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val thisMonthRevenue by viewModel.thisMonthRevenue.collectAsState()

    var showAddSaleDialog by remember { mutableStateOf(false) }
    var saleToCancel by remember { mutableStateOf<SaleEntity?>(null) }
    var filterMethod by remember { mutableStateOf<String?>(null) } // null = All

    val filteredSales = remember(allSales, filterMethod) {
        if (filterMethod == null) allSales else allSales.filter { it.paymentMethod == filterMethod }
    }

    val totalAllTime = remember(allSales) {
        allSales.filter { it.status == "COMPLETED" }.sumOf { it.amountAriary }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSaleDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvelle Vente", fontWeight = FontWeight.Bold) },
                containerColor = MikrotechGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_sale")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header stats
            item {
                Text(
                    text = "COMPTABILITÉ & REVENUS (ARIARY)",
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
                        title = "Aujourd'hui",
                        value = formatAriary(todayRevenue),
                        icon = Icons.Default.Today,
                        iconColor = MikrotechGreen,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Ce Mois",
                        value = formatAriary(thisMonthRevenue),
                        icon = Icons.Default.CalendarMonth,
                        iconColor = MikrotechBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                LuxuryCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Historique Ventes",
                                fontSize = 12.sp,
                                color = MikrotechTextSecondary
                            )
                            Text(
                                text = formatAriary(totalAllTime),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MikrotechTextPrimary
                            )
                        }

                        // Export CSV / PDF button
                        Button(
                            onClick = {
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
                                val csv = StringBuilder("ID;Date;Type;Reference;Client;Plan;MontantAr;ModePaiement;Statut\n")
                                for (s in allSales) {
                                    csv.append("${s.id};${dateFormat.format(Date(s.timestamp))};${s.itemType};${s.itemReference};${s.clientName};${s.planName};${s.amountAriary};${s.paymentMethod};${s.status}\n")
                                }

                                val sendIntent = Intent.createChooser(
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, csv.toString())
                                        type = "text/plain"
                                    },
                                    "Exporter Rapport Ventes CSV"
                                )
                                context.startActivity(sendIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MikrotechSurfaceVariant),
                            border = BorderStroke(1.dp, MikrotechCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MikrotechCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 11.sp, color = MikrotechCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Payment Methods Breakdown Filter
            item {
                Text(
                    text = "FILTRER PAR MODE DE PAIEMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechCyan,
                    letterSpacing = 1.sp
                )
            }

            item {
                val methods = listOf(
                    null to "Tous",
                    "ESPECES" to "Espèces",
                    "MVOLA" to "MVola",
                    "ORANGE_MONEY" to "Orange Money",
                    "AIRTEL_MONEY" to "Airtel Money"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methods.forEach { (key, label) ->
                        val isSelected = filterMethod == key
                        Surface(
                            color = if (isSelected) MikrotechCyan.copy(alpha = 0.2f) else MikrotechSurface,
                            border = BorderStroke(1.dp, if (isSelected) MikrotechCyan else MikrotechCardBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { filterMethod = key }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MikrotechCyan else MikrotechTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JOURNAL DES TRANSACTIONS (${filteredSales.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Transactions list
            if (filteredSales.isEmpty()) {
                item {
                    LuxuryCard {
                        Text(
                            text = "Aucune vente enregistrée pour ce filtre.",
                            fontSize = 13.sp,
                            color = MikrotechTextMuted
                        )
                    }
                }
            } else {
                items(filteredSales, key = { it.id }) { sale ->
                    SaleItemRow(
                        sale = sale,
                        onCancel = { saleToCancel = sale }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal: Add Manual Sale
    if (showAddSaleDialog) {
        AddSaleDialog(
            onDismiss = { showAddSaleDialog = false },
            onConfirm = { type, ref, plan, amount, method, client, phone ->
                viewModel.recordManualSale(
                    type = type,
                    reference = ref,
                    plan = plan,
                    amount = amount,
                    paymentMethod = method,
                    customerName = client,
                    phone = phone
                )
                showAddSaleDialog = false
            }
        )
    }

    // Modal: Cancel Sale
    saleToCancel?.let { sale ->
        var cancelReason by remember { mutableStateOf("Erreur de saisie") }

        AlertDialog(
            onDismissRequest = { saleToCancel = null },
            containerColor = MikrotechSurface,
            title = { Text("Annuler cette vente ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Vente #${sale.id} : ${sale.itemReference} - ${formatAriary(sale.amountAriary)} (${sale.paymentMethod}).",
                        color = MikrotechTextSecondary
                    )
                    Text(
                        text = "L'opération restera tracée dans le journal comptable avec le motif ci-dessous.",
                        fontSize = 12.sp,
                        color = MikrotechTextMuted
                    )
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Motif d'annulation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSale(sale.id, cancelReason)
                        saleToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechRed)
                ) {
                    Text("Confirmer Annulation", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToCancel = null }) {
                    Text("Retour", color = MikrotechTextSecondary)
                }
            }
        )
    }
}

@Composable
fun SaleItemRow(
    sale: SaleEntity,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH) }
    val isCancelled = sale.status == "CANCELLED"

    val (methodColor, methodLabel) = when (sale.paymentMethod) {
        "MVOLA" -> Color(0xFFFFB300) to "MVola"
        "ORANGE_MONEY" -> Color(0xFFFF6D00) to "Orange Money"
        "AIRTEL_MONEY" -> Color(0xFFE53935) to "Airtel Money"
        else -> MikrotechGreen to "Espèces"
    }

    LuxuryCard(
        modifier = modifier,
        borderColor = if (isCancelled) MikrotechRed.copy(alpha = 0.5f) else MikrotechCardBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.itemReference,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isCancelled) MikrotechTextMuted else MikrotechTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = methodColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = methodLabel,
                            color = methodColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sale.planName} • ${dateFormat.format(Date(sale.timestamp))}",
                    fontSize = 11.sp,
                    color = MikrotechTextSecondary
                )

                if (sale.clientName.isNotEmpty()) {
                    Text(
                        text = "Client : ${sale.clientName} (${sale.clientPhone})",
                        fontSize = 11.sp,
                        color = MikrotechCyan
                    )
                }

                if (sale.notes.isNotEmpty()) {
                    Text(
                        text = sale.notes,
                        fontSize = 10.sp,
                        color = if (isCancelled) MikrotechRed else MikrotechTextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAriary(sale.amountAriary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isCancelled) MikrotechTextMuted else MikrotechGreen
                )

                if (!isCancelled) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Annuler vente",
                            tint = MikrotechTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MikrotechRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ANNULÉ",
                            color = MikrotechRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddSaleDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, ref: String, plan: String, amount: Long, method: String, client: String, phone: String) -> Unit
) {
    var itemType by remember { mutableStateOf("HOTSPOT_TICKET") }
    var reference by remember { mutableStateOf("mt301_vente") }
    var planName by remember { mutableStateOf("3H_1000Ar") }
    var amountText by remember { mutableStateOf("1000") }
    var paymentMethod by remember { mutableStateOf("ESPECES") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }

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
                    text = "NOUVELLE ENCAISSEMENT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MikrotechGreen
                )

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (itemType == "HOTSPOT_TICKET") MikrotechGreen else MikrotechSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { itemType = "HOTSPOT_TICKET" }
                    ) {
                        Text(
                            text = "Ticket Hotspot",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (itemType == "HOTSPOT_TICKET") Color.White else MikrotechTextSecondary,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        color = if (itemType == "PPPOE_SUBSCRIPTION") MikrotechBlue else MikrotechSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { itemType = "PPPOE_SUBSCRIPTION" }
                    ) {
                        Text(
                            text = "Abonnement PPPoE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (itemType == "PPPOE_SUBSCRIPTION") Color.White else MikrotechTextSecondary,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Code ticket / Identifiant") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechGreen,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text("Nom du forfait / Offre") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechGreen,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
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

                // Client name & phone
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nom du client (facultatif)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechGreen,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Téléphone client (facultatif)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikrotechGreen,
                        unfocusedBorderColor = MikrotechCardBorder,
                        focusedTextColor = MikrotechTextPrimary,
                        unfocusedTextColor = MikrotechTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment mode
                Text("Mode de paiement :", fontSize = 11.sp, color = MikrotechTextSecondary)
                val methods = listOf("ESPECES" to "Espèces", "MVOLA" to "MVola", "ORANGE_MONEY" to "Orange Money", "AIRTEL_MONEY" to "Airtel")
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSel) MikrotechGreen else MikrotechTextSecondary,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                maxLines = 1
                            )
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
                            onConfirm(itemType, reference, planName, amount, paymentMethod, clientName, clientPhone)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen)
                    ) {
                        Text("Enregistrer Vente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
