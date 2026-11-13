package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routers")
data class RouterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 8728,
    val apiType: String = "REST", // "REST" (RouterOS 7.1+ / HTTPS or HTTP) or "BINARY" (RouterOS 6/7 Socket)
    val useTls: Boolean = false,
    val username: String,
    val encryptedPassword: String, // Encrypted or obfuscated local token
    val model: String = "MikroTik Router",
    val version: String = "RouterOS v7",
    val lastConnected: Long = 0L,
    val isDefault: Boolean = false
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routerId: Long = 0L,
    val itemType: String, // "HOTSPOT_TICKET" or "PPPOE_SUBSCRIPTION"
    val itemReference: String, // Username or Ticket Code
    val clientName: String = "",
    val clientPhone: String = "",
    val planName: String,
    val amountAriary: Long, // Prix en Ariary (Ar)
    val paymentMethod: String, // "ESPECES", "MVOLA", "ORANGE_MONEY", "AIRTEL_MONEY", "AUTRE"
    val operatorName: String = "Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED", // "COMPLETED", "REFUNDED", "CANCELLED"
    val notes: String = ""
)

@Entity(tableName = "local_tickets")
data class LocalTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routerId: Long = 0L,
    val username: String,
    val password: String,
    val profile: String,
    val durationMinutes: Int, // Durée en minutes
    val dataLimitBytes: Long = 0L, // 0 = illimité
    val simultaneousUsers: Int = 1,
    val priceAriary: Long,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val soldAt: Long? = null,
    val status: String = "AVAILABLE", // "AVAILABLE", "SOLD", "EXPIRED", "SUSPENDED"
    val isSyncedToRouter: Boolean = false
)

@Entity(tableName = "pppoe_commercial")
data class PppoeCommercialEntity(
    @PrimaryKey val username: String,
    val routerId: Long = 0L,
    val customerName: String,
    val phone: String,
    val address: String = "",
    val monthlyPriceAriary: Long,
    val paymentMethod: String = "ESPECES",
    val lastPaymentDate: Long = System.currentTimeMillis(),
    val nextRenewalDate: Long,
    val status: String = "ACTIVE", // "ACTIVE", "EXPIRED", "SUSPENDED"
    val notes: String = ""
)

@Entity(tableName = "captive_portal_config")
data class CaptivePortalConfigEntity(
    @PrimaryKey val id: Int = 1,
    val zoneName: String = "MIKROTECH 301 WI-FI ZONE",
    val contactPhone: String = "038 28 171 00",
    val whatsapp: String = "038 28 171 00",
    val facebook: String = "Mikrotech 301 Madagascar",
    val commercialAddress: String = "Madagascar",
    val welcomeText: String = "Bienvenue sur notre réseau Wi-Fi haut débit sécurisé !",
    val bannerHeadline: String = "Connexion Internet Rapide & Stable",
    val themeName: String = "DARK_PREMIUM", // "DARK_PREMIUM", "BUSINESS_BLUE", "CLEAN_LIGHT"
    val ratesDescription: String = "Tickets à partir de 500 Ar. Disponible chez les revendeurs agréés."
)

@Entity(tableName = "license_info")
data class LicenseEntity(
    @PrimaryKey val id: Int = 1,
    val licenseKey: String = "",
    val planType: String = "STARTER", // "STARTER", "BUSINESS_PRO", "ULTIMATE"
    val authorizedRouterSerial: String = "",
    val androidInstallId: String = "",
    val activationDate: Long = 0L,
    val expirationDate: Long = 0L, // 0 = perpétuelle
    val signatureHex: String = "",
    val isActive: Boolean = false
)
