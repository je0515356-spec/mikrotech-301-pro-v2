package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.RouterEntity
import com.example.data.routeros.ConnectionStatus
import com.example.ui.MikrotechViewModel
import com.example.ui.components.LuxuryCard
import com.example.ui.theme.*

@Composable
fun WelcomeSplashScreen(
    viewModel: MikrotechViewModel,
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val defaultRouter by viewModel.defaultRouter.collectAsState()
    val discoveredIps by viewModel.discoveredIps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showPairingForm by remember { mutableStateOf(defaultRouter == null) }
    var routerName by remember { mutableStateOf("MikroTik Principal") }
    var host by remember { mutableStateOf("192.168.88.1") }
    var portText by remember { mutableStateOf("8728") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberRouter by remember { mutableStateOf(true) }
    var showWelcomeIntro by remember { mutableStateOf(true) }

    // Smooth one-time brand reveal; connection controls remain immediately usable.
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val brandScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.82f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "brandScale"
    )
    val brandAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "brandAlpha"
    )

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    if (showWelcomeIntro) {
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "introPulse"
        )
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF050A1D), Color(0xFF10123A), Color(0xFF180A35))))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .scale(pulse)
                        .border(
                            BorderStroke(3.dp, Brush.sweepGradient(listOf(Color(0xFF17D9FF), Color(0xFFAC32FF), Color(0xFF176CFF), Color(0xFF17D9FF)))),
                            CircleShape
                        )
                        .background(Color(0xFF172354).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Router, contentDescription = "MIKROTECH 301", tint = Color(0xFF25D9FF), modifier = Modifier.size(85.dp))
                }
                Text("MIKROTECH 301", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                Text("ULTIMATE PRO", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC368FF), letterSpacing = 3.sp)
                Text("Votre réseau. Votre business. Votre contrôle.", fontSize = 13.sp, color = Color(0xFFBAC8E9), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntroFeature(Icons.Default.Wifi, "Hotspot")
                    IntroFeature(Icons.Default.Security, "Sécurité")
                    IntroFeature(Icons.Default.Analytics, "Statistiques")
                }
            }
            Button(
                onClick = { showWelcomeIntro = false },
                modifier = Modifier.fillMaxWidth().height(56.dp).align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6548F5)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("CONTINUER", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Grand Logo MIKROTECH 301
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(brandScale)
                .alpha(brandAlpha)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MikrotechCyan.copy(alpha = 0.25f),
                            MikrotechBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    BorderStroke(
                        2.5.dp,
                        Brush.linearGradient(listOf(MikrotechCyan, MikrotechBlue))
                    ),
                    RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Router,
                contentDescription = "Logo MIKROTECH 301",
                tint = MikrotechCyan,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "MIKROTECH 301",
            modifier = Modifier.alpha(brandAlpha),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MikrotechCyan
        )

        Text(
            text = "ULTIMATE PRO V2",
            modifier = Modifier.alpha(brandAlpha),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            color = MikrotechBlue
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "\"Votre réseau. Votre business. Votre contrôle.\"",
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Commercial Badge & Direct Contact
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MikrotechBlue.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MikrotechBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Édition Commerciale • Madagascar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MikrotechBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = MikrotechGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MikrotechGreen.copy(alpha = 0.3f)),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0382817100"))
                    context.startActivity(intent)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MikrotechGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "038 28 171 00",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Router Section
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MikrotechCyan,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(radarAngle)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONNEXION ROUTEUR MIKROTIK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MikrotechCyan,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = if (connectionStatus == ConnectionStatus.CONNECTED) MikrotechGreen.copy(alpha = 0.15f) else MikrotechOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (connectionStatus == ConnectionStatus.CONNECTED) "En Ligne" else "Détection Wi-Fi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connectionStatus == ConnectionStatus.CONNECTED) MikrotechGreen else MikrotechOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // State A: A router is already memorized
            if (defaultRouter != null && !showPairingForm) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MikrotechCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = defaultRouter?.name ?: "MikroTik Principal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "IP : ${defaultRouter?.host} • Port ${defaultRouter?.port} (${defaultRouter?.apiType})",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MikrotechGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Ce routeur est mémorisé pour se connecter automatiquement.",
                            fontSize = 11.sp,
                            color = MikrotechGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (connectionStatus != ConnectionStatus.CONNECTED) {
                            viewModel.connectMemorizedRouter()
                        }
                        onEnterApp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikrotechCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_enter_dashboard")
                ) {
                    Text(
                        text = "ACCÉDER AU TABLEAU DE BORD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showPairingForm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Changer ou détecter un autre routeur", fontSize = 12.sp, color = MikrotechTextSecondary)
                }
            } else {
                // State B: Detection & pairing form
                Text(
                    text = "Connectez votre téléphone au Wi-Fi du routeur MikroTik, puis validez la connexion.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Discovered IPs chips
                if (discoveredIps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Routeur(s) détecté(s) sur le réseau local :",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MikrotechGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        discoveredIps.forEach { ip ->
                            Surface(
                                color = MikrotechGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MikrotechGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { host = ip }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Router, contentDescription = null, tint = MikrotechGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(ip, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MikrotechGreen)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = routerName,
                    onValueChange = { routerName = it },
                    label = { Text("Nom du Routeur") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Adresse IP") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter { c -> c.isDigit() } },
                        label = { Text("Port API") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Utilisateur") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberRouter,
                        onCheckedChange = { rememberRouter = it },
                        colors = CheckboxDefaults.colors(checkedColor = MikrotechCyan)
                    )
                    Text(
                        text = "Mémoriser ce routeur (connexion auto au démarrage)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MikrotechCyan)
                    }
                } else {
                    Button(
                        onClick = {
                            val p = portText.toIntOrNull() ?: 8728
                            val apiType = if (p in listOf(80, 443, 8080)) "REST" else "BINARY"
                            viewModel.connectRouter(
                                name = routerName,
                                host = host.trim(),
                                port = p,
                                username = username.trim(),
                                password = password,
                                apiType = apiType,
                                useTls = p == 443
                            )
                            onEnterApp()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikrotechGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_connect_memorize_router")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONNECTER & MÉMORISER",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.scanLocalMikrotik()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MikrotechCyan),
                        border = BorderStroke(1.dp, MikrotechCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MikrotechCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-scanner le réseau Wi-Fi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onEnterApp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Accéder à l'application sans connecter de routeur", fontSize = 12.sp, color = MikrotechTextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun IntroFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Surface(color = Color(0xFF1A2351), shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF24D8FF), modifier = Modifier.size(22.dp))
            Text(title, color = Color.White, fontSize = 11.sp)
        }
    }
}
