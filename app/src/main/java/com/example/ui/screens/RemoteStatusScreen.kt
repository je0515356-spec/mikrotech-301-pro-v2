package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Safe remote-mode landing screen.
 * Remote controls are deliberately not activated until user-scoped
 * authentication and an isolated router-agent test have passed.
 */
@Composable
fun RemoteStatusScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Contrôle à distance", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cloudflare • préparation", style = MaterialTheme.typography.titleMedium)
                Text("Le relais HTTPS est installé. La connexion sécurisée du compte et du routeur n'est pas encore activée.")
                Text("Mode Wi-Fi local : inchangé.")
                Text("Aucune commande distante n'est envoyée depuis cet écran.")
            }
        }
        Button(onClick = onBack) { Text("Retour au tableau de bord") }
    }
}
