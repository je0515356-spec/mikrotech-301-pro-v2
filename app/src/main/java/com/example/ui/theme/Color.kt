package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Couleurs Fondamentales Mode Sombre (Cobalt Électrique)
val DarkBackground = Color(0xFF0F1E38)       // Bleu nuit saphir lumineux et aéré
val DarkSurface = Color(0xFF182D50)          // Surface des cartes bleutée et riche
val DarkSurfaceVariant = Color(0xFF233B68)   // Surfaces d'accentuation haute visibilité
val DarkCardBorder = Color(0xFF33538E)       // Bordures contrastées éclatantes
val DarkTextPrimary = Color(0xFFFFFFFF)      // Blanc pur éclatant
val DarkTextSecondary = Color(0xFFB8C8E5)    // Bleu ardoise clair très lisible
val DarkTextMuted = Color(0xFF7E97C2)        // Texte secondaire adouci

// Couleurs Fondamentales Mode Clair (Cristal Lumineux - Haute Visibilité)
val LightBackground = Color(0xFFF1F5F9)      // Fond gris-ardoise très doux et clair
val LightSurface = Color(0xFFFFFFFF)         // Cartes blanches pures
val LightSurfaceVariant = Color(0xFFE2E8F0)  // Gris perle pour éléments secondaires
val LightCardBorder = Color(0xFFCBD5E1)      // Bordure grise nette
val LightTextPrimary = Color(0xFF0F172A)     // Noir ardoise très contrasté pour lisibilité maximale
val LightTextSecondary = Color(0xFF334155)   // Gris foncé lisible pour sous-titres
val LightTextMuted = Color(0xFF64748B)       // Gris moyen contrasté

// Couleurs Vives et Accents Électriques (communs aux deux modes)
val MikrotechBlue = Color(0xFF3884FF)
val MikrotechCyan = Color(0xFF00E5FF)
val MikrotechPurple = Color(0xFF9D65FF)
val MikrotechGreen = Color(0xFF00E676)
val MikrotechRed = Color(0xFFFF4D63)
val MikrotechOrange = Color(0xFFFF9100)
val MikrotechYellow = Color(0xFFFFD600)
val MikrotechPink = Color(0xFFFF4081)

// Accesseurs Dynamiques - S'adaptent AUTOMATIQUEMENT au thème sombre ou clair actif
val MikrotechBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val MikrotechSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val MikrotechSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val MikrotechCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val MikrotechTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val MikrotechTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val MikrotechTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == LightBackground) LightTextMuted else DarkTextMuted

// Alias pour compatibilité
val MikrotechLightBackground = LightBackground
val MikrotechLightSurface = LightSurface
val MikrotechLightSurfaceVariant = LightSurfaceVariant
val MikrotechLightCardBorder = LightCardBorder
val MikrotechLightTextPrimary = LightTextPrimary
val MikrotechLightTextSecondary = LightTextSecondary
val MikrotechLightTextMuted = LightTextMuted
