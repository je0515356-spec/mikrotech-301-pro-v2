package com.example.data.routeros

data class RouterResource(
    val serialNumber: String = "",
    val uptime: String = "00:00:00",
    val cpuLoad: Int = 0,
    val freeMemory: Long = 0L,
    val totalMemory: Long = 0L,
    val freeHdd: Long = 0L,
    val totalHdd: Long = 0L,
    val version: String = "Unknown",
    val boardName: String = "MikroTik",
    val cpuFrequency: Int = 0,
    val cpuCount: Int = 1,
    val architecture: String = "arm"
) {
    val memoryUsagePercent: Int
        get() = if (totalMemory > 0) (((totalMemory - freeMemory) * 100) / totalMemory).toInt() else 0

    val hddUsagePercent: Int
        get() = if (totalHdd > 0) (((totalHdd - freeHdd) * 100) / totalHdd).toInt() else 0
}

data class RouterIdentity(
    val name: String = "MikroTik"
)

data class HotspotUserModel(
    val id: String = "",
    val name: String,
    val password: String = "",
    val profile: String = "default",
    val limitUptime: String = "", // e.g. "1h", "1d", "30d"
    val limitBytesTotal: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val uptime: String = "0s",
    val comment: String = "",
    val disabled: Boolean = false,
    val antiCoupureSavedRemaining: String = ""
) {
    val totalBytesUsed: Long get() = bytesIn + bytesOut

    val remainingUptimeText: String
        get() {
            if (limitUptime.isEmpty()) return "Illimité"
            val limitMins = parseRouterOSUptimeToMinutes(limitUptime)
            val usedMins = parseRouterOSUptimeToMinutes(uptime)
            val remain = (limitMins - usedMins).coerceAtLeast(0)
            return formatMinutesToDisplay(remain)
        }
}

data class HotspotActiveModel(
    val id: String = "",
    val user: String,
    val address: String = "",
    val macAddress: String = "",
    val uptime: String = "0s",
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val loginBy: String = "http-chap"
)

data class HotspotProfileModel(
    val id: String = "",
    val name: String,
    val rateLimit: String = "", // e.g. "2M/5M" (upload/download)
    val sessionTimeout: String = "",
    val idleTimeout: String = "",
    val keepaliveTimeout: String = "",
    val sharedUsers: Int = 1
)

data class PppSecretModel(
    val id: String = "",
    val name: String,
    val password: String = "",
    val service: String = "pppoe",
    val profile: String = "default",
    val localAddress: String = "",
    val remoteAddress: String = "",
    val comment: String = "",
    val disabled: Boolean = false
)

data class PppActiveModel(
    val id: String = "",
    val name: String,
    val service: String = "pppoe",
    val callerId: String = "",
    val address: String = "",
    val uptime: String = "0s"
)

data class PppProfileModel(
    val id: String = "",
    val name: String,
    val localAddress: String = "",
    val remoteAddress: String = "",
    val rateLimit: String = "",
    val onlyOne: String = "default",
    val dnsServer: String = ""
)

data class InterfaceTraffic(
    val name: String,
    val rxBps: Long = 0L,
    val txBps: Long = 0L,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DEMO_MODE,
    ERROR
}

data class RouterOSUpdateInfo(
    val installedVersion: String = "",
    val latestVersion: String = "",
    val channel: String = "stable",
    val statusText: String = "À jour",
    val updateAvailable: Boolean = false
)

data class AntiCoupureStatus(
    val isInstalled: Boolean = true,
    val isEnabled: Boolean = true,
    val isCloudTimeSynced: Boolean = true,
    val isWatchdogRunning: Boolean = true,
    val syncIntervalMinutes: Int = 1,
    val protectedTicketsCount: Int = 0,
    val lastSyncTime: String = "Synchronisé en continu",
    val statusMessage: String = "Bouclier Anti-Coupure actif : les tickets reprennent exactement là où ils s'étaient arrêtés sans recommencer à 1h.",
    val lastPowerOutageSimulated: Boolean = false
)

fun parseRouterOSUptimeToMinutes(uptime: String): Int {
    if (uptime.isEmpty()) return 0
    var total = 0
    val pattern = "(\\d+)([wdhm])".toRegex()
    val matches = pattern.findAll(uptime).toList()
    if (matches.isNotEmpty()) {
        matches.forEach { match ->
            val value = match.groupValues[1].toIntOrNull() ?: 0
            when (match.groupValues[2]) {
                "w" -> total += value * 7 * 24 * 60
                "d" -> total += value * 24 * 60
                "h" -> total += value * 60
                "m" -> total += value
            }
        }
        return total
    }
    val parts = uptime.split(":")
    if (parts.size == 3) {
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }
    return uptime.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
}

fun formatMinutesToDisplay(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val days = minutes / (24 * 60)
    val hours = (minutes % (24 * 60)) / 60
    val mins = minutes % 60
    return buildString {
        if (days > 0) append("${days}j ")
        if (hours > 0) append("${hours}h ")
        if (mins > 0 || (days == 0 && hours == 0)) append("${mins}m")
    }.trim()
}

