package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val HEALTH_URL = "https://mikroteck-remote-relay.je0515356.workers.dev/health"

@Composable
fun RemoteStatusScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Relais non vérifié") }
    var checking by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Contrôle à distance", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cloudflare • diagnostic", style = MaterialTheme.typography.titleMedium)
                Text(status)
                Text("Ce test vérifie uniquement l'accès HTTPS au relais. Il ne confirme PAS la connexion au MikroTik.")
                Text("Le contrôle à distance reste désactivé. Aucune commande n'est envoyée au routeur.")
                Text("Mode Wi-Fi local : inchangé.")
            }
        }
        Button(enabled = !checking, onClick = {
            checking = true
            status = "Vérification HTTPS en cours…"
            scope.launch {
                status = withContext(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
                        val request = Request.Builder().url(HEALTH_URL).get().build()
                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            val json = JSONObject(body)
                            if (response.isSuccessful && json.optString("service") == "mikroteck-relay" &&
                                json.optString("status") == "online") {
                                "Relais Cloudflare accessible via HTTPS. Routeur non vérifié."
                            } else {
                                "Relais non prêt (HTTP ${response.code}). Le Worker doit être redéployé."
                            }
                        }
                    } catch (_: Exception) {
                        "Impossible de joindre le relais. Vérifie Internet et le déploiement Cloudflare."
                    }
                }
                checking = false
            }
        }) { Text(if (checking) "Vérification…" else "Tester le relais Cloudflare") }
        Button(onClick = onBack) { Text("Retour au tableau de bord") }
    }
}
