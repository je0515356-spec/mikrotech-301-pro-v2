package com.example.core.network

import com.example.data.routeros.*

class DemoRouterOSClient : RouterOSClient {

    private val hotspotUsers = mutableListOf(
        HotspotUserModel("*1", "mt301_0491", "8492", "3H_1000Ar", "3h", 0L, 142000000L, 89000000L, "1h 42m", "Vendu le 11/11 - MVola", false),
        HotspotUserModel("*2", "mt301_8102", "1290", "1H_500Ar", "1h", 524288000L, 340000000L, 120000000L, "45m", "Vendu le 11/11 - Espèces", false),
        HotspotUserModel("*3", "mt301_7734", "9931", "24H_2500Ar", "1d", 0L, 1850000000L, 450000000L, "14h 10m", "Vendu le 10/11 - Orange Money", false),
        HotspotUserModel("*4", "mt301_1109", "4521", "7J_10000Ar", "7d", 0L, 4100000000L, 950000000L, "3d 8h", "Vendu le 08/11 - Airtel Money", false),
        HotspotUserModel("*5", "mt301_9920", "7712", "1H_500Ar", "1h", 0L, 0L, 0L, "0s", "Disponible / Non activé", false),
        HotspotUserModel("*6", "mt301_9921", "3341", "3H_1000Ar", "3h", 0L, 0L, 0L, "0s", "Disponible / Non activé", false)
    )

    private val hotspotActive = mutableListOf(
        HotspotActiveModel("*a1", "mt301_0491", "192.168.88.241", "AC:D1:B8:44:91:02", "1h 42m", 142000000L, 89000000L, "http-chap"),
        HotspotActiveModel("*a2", "mt301_8102", "192.168.88.198", "48:5F:99:32:11:8A", "45m", 340000000L, 120000000L, "http-chap"),
        HotspotActiveModel("*a3", "mt301_7734", "192.168.88.112", "70:3A:CB:90:54:33", "14h 10m", 1850000000L, 450000000L, "http-chap")
    )

    private val hotspotProfiles = mutableListOf(
        HotspotProfileModel("*p1", "1H_500Ar", "1M/3M", "1h", "10m", "5m", 1),
        HotspotProfileModel("*p2", "3H_1000Ar", "2M/5M", "3h", "15m", "5m", 1),
        HotspotProfileModel("*p3", "24H_2500Ar", "3M/8M", "1d", "30m", "10m", 2),
        HotspotProfileModel("*p4", "7J_10000Ar", "4M/10M", "7d", "1h", "15m", 2),
        HotspotProfileModel("*p5", "VIP_Illimite", "10M/25M", "", "", "", 3)
    )

    private val pppSecrets = mutableListOf(
        PppSecretModel("*s1", "cyber_tana", "Sec7@Tana", "pppoe", "PPPOE_10M", "10.10.10.1", "10.10.10.50", "Cybercafé Analakely - Mensuel 120 000 Ar", false),
        PppSecretModel("*s2", "hotel_radama", "Radama#99", "pppoe", "PPPOE_20M", "10.10.10.1", "10.10.10.51", "Hôtel Restaurant - Mensuel 250 000 Ar", false),
        PppSecretModel("*s3", "bureau_compta", "BurCompta1", "pppoe", "PPPOE_5M", "10.10.10.1", "10.10.10.52", "Cabinet comptable - Mensuel 75 000 Ar", false),
        PppSecretModel("*s4", "residence_ivan", "IvanHome2026", "pppoe", "PPPOE_5M", "10.10.10.1", "10.10.10.53", "Résidence privée - Mensuel 70 000 Ar", false)
    )

    private val pppActive = mutableListOf(
        PppActiveModel("*pa1", "cyber_tana", "pppoe", "00:1E:68:55:22:91", "10.10.10.50", "12d 4h"),
        PppActiveModel("*pa2", "hotel_radama", "pppoe", "5C:F9:DD:71:09:44", "10.10.10.51", "5d 18h"),
        PppActiveModel("*pa3", "bureau_compta", "pppoe", "80:2A:A8:14:8B:E0", "10.10.10.52", "2d 9h")
    )

    private val pppProfiles = mutableListOf(
        PppProfileModel("*pp1", "PPPOE_2M", "10.10.10.1", "pool-pppoe", "1M/2M", "yes", "8.8.8.8,1.1.1.1"),
        PppProfileModel("*pp2", "PPPOE_5M", "10.10.10.1", "pool-pppoe", "2M/5M", "yes", "8.8.8.8,1.1.1.1"),
        PppProfileModel("*pp3", "PPPOE_10M", "10.10.10.1", "pool-pppoe", "4M/10M", "yes", "8.8.8.8,1.1.1.1"),
        PppProfileModel("*pp4", "PPPOE_15M", "10.10.10.1", "pool-pppoe", "5M/15M", "yes", "8.8.8.8,1.1.1.1"),
        PppProfileModel("*pp5", "PPPOE_20M", "10.10.10.1", "pool-pppoe", "8M/20M", "yes", "8.8.8.8,1.1.1.1")
    )

    override suspend fun testConnection(): Result<RouterResource> = getResource()

    override suspend fun getResource(): Result<RouterResource> = Result.success(
        RouterResource(
            uptime = "14d 06:28:14",
            cpuLoad = 18,
            freeMemory = 89653248L,
            totalMemory = 134217728L, // 128 MB
            freeHdd = 8388608L,
            totalHdd = 16777216L, // 16 MB flash
            version = "7.15.2 (stable)",
            boardName = "hAP ac² (RBD52G-5HacD2HnD)",
            cpuFrequency = 716,
            cpuCount = 4,
            architecture = "arm"
        )
    )

    override suspend fun getIdentity(): Result<RouterIdentity> = Result.success(RouterIdentity("MIKROTECH-301-DEMO"))

    override suspend fun getHotspotUsers(): Result<List<HotspotUserModel>> = Result.success(hotspotUsers.toList())

    override suspend fun addHotspotUser(user: HotspotUserModel): Result<String> {
        val newId = "*${System.currentTimeMillis() % 10000}"
        hotspotUsers.add(user.copy(id = newId))
        return Result.success(user.name)
    }

    override suspend fun updateHotspotUser(id: String, user: HotspotUserModel): Result<Unit> {
        val idx = hotspotUsers.indexOfFirst { it.id == id || it.name == user.name }
        if (idx != -1) {
            hotspotUsers[idx] = user.copy(id = id)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteHotspotUser(id: String): Result<Unit> {
        hotspotUsers.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun resetHotspotUserCounters(id: String): Result<Unit> {
        val idx = hotspotUsers.indexOfFirst { it.id == id }
        if (idx != -1) {
            val u = hotspotUsers[idx]
            hotspotUsers[idx] = u.copy(bytesIn = 0L, bytesOut = 0L, uptime = "0s")
        }
        return Result.success(Unit)
    }

    override suspend fun getHotspotActive(): Result<List<HotspotActiveModel>> = Result.success(hotspotActive.toList())

    override suspend fun removeHotspotActive(id: String): Result<Unit> {
        hotspotActive.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getHotspotProfiles(): Result<List<HotspotProfileModel>> = Result.success(hotspotProfiles.toList())

    override suspend fun addHotspotProfile(profile: HotspotProfileModel): Result<String> {
        val newId = "*p${System.currentTimeMillis() % 1000}"
        hotspotProfiles.add(profile.copy(id = newId))
        return Result.success(profile.name)
    }

    override suspend fun updateHotspotProfile(id: String, profile: HotspotProfileModel): Result<Unit> {
        val idx = hotspotProfiles.indexOfFirst { it.id == id || it.name == profile.name }
        if (idx != -1) {
            hotspotProfiles[idx] = profile.copy(id = id)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteHotspotProfile(id: String): Result<Unit> {
        hotspotProfiles.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getPppSecrets(): Result<List<PppSecretModel>> = Result.success(pppSecrets.toList())

    override suspend fun addPppSecret(secret: PppSecretModel): Result<String> {
        val newId = "*s${System.currentTimeMillis() % 1000}"
        pppSecrets.add(secret.copy(id = newId))
        return Result.success(secret.name)
    }

    override suspend fun updatePppSecret(id: String, secret: PppSecretModel): Result<Unit> {
        val idx = pppSecrets.indexOfFirst { it.id == id || it.name == secret.name }
        if (idx != -1) {
            pppSecrets[idx] = secret.copy(id = id)
        }
        return Result.success(Unit)
    }

    override suspend fun deletePppSecret(id: String): Result<Unit> {
        pppSecrets.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getPppActive(): Result<List<PppActiveModel>> = Result.success(pppActive.toList())

    override suspend fun removePppActive(id: String): Result<Unit> {
        pppActive.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getPppProfiles(): Result<List<PppProfileModel>> = Result.success(pppProfiles.toList())

    override suspend fun addPppProfile(profile: PppProfileModel): Result<String> {
        val newId = "*pp${System.currentTimeMillis() % 1000}"
        pppProfiles.add(profile.copy(id = newId))
        return Result.success(profile.name)
    }

    override suspend fun updatePppProfile(id: String, profile: PppProfileModel): Result<Unit> {
        val idx = pppProfiles.indexOfFirst { it.id == id || it.name == profile.name }
        if (idx != -1) {
            pppProfiles[idx] = profile.copy(id = id)
        }
        return Result.success(Unit)
    }

    override suspend fun deletePppProfile(id: String): Result<Unit> {
        pppProfiles.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun rebootRouter(): Result<Unit> = Result.success(Unit)

    override suspend fun createBackup(name: String): Result<Unit> = Result.success(Unit)

    private var simulatedVersion = "7.14.3"

    override suspend fun checkRouterOSUpdate(): Result<RouterOSUpdateInfo> {
        val hasUpdate = simulatedVersion != "7.18.2"
        return Result.success(
            RouterOSUpdateInfo(
                installedVersion = simulatedVersion,
                latestVersion = "7.18.2",
                channel = "stable",
                statusText = if (hasUpdate) "Mise à jour disponible : RouterOS v7.18.2 (Stable)" else "Votre routeur est à jour",
                updateAvailable = hasUpdate
            )
        )
    }

    override suspend fun installRouterOSUpdate(): Result<Unit> {
        simulatedVersion = "7.18.2"
        return Result.success(Unit)
    }

    private var antiCoupureEnabled = true
    private var powerOutageSimulatedCount = 0

    override suspend fun getAntiCoupureStatus(): Result<AntiCoupureStatus> {
        val protectedCount = hotspotUsers.count { it.uptime != "0s" && !it.disabled }
        return Result.success(
            AntiCoupureStatus(
                isInstalled = true,
                isEnabled = antiCoupureEnabled,
                isCloudTimeSynced = true,
                isWatchdogRunning = antiCoupureEnabled,
                syncIntervalMinutes = 1,
                protectedTicketsCount = protectedCount,
                lastSyncTime = if (antiCoupureEnabled) "Synchronisé en continu (Jirama Shield)" else "Désactivé",
                statusMessage = if (antiCoupureEnabled)
                    "Bouclier actif : en cas de délestage Jirama, chaque ticket reprendra exactement à son temps restant sans recommencer à 1h."
                else "Bouclier désactivé : risque de remise à zéro du compteur ou ticket offert au retour du courant.",
                lastPowerOutageSimulated = powerOutageSimulatedCount > 0
            )
        )
    }

    override suspend fun installAntiCoupure(): Result<Unit> {
        antiCoupureEnabled = true
        return Result.success(Unit)
    }

    override suspend fun disableAntiCoupure(): Result<Unit> {
        antiCoupureEnabled = false
        return Result.success(Unit)
    }

    override suspend fun simulatePowerOutage(): Result<Unit> {
        powerOutageSimulatedCount++
        // Simule une coupure de courant brutale
        for (i in 0 until hotspotUsers.size) {
            val u = hotspotUsers[i]
            if (u.uptime != "0s" && u.limitUptime.isNotEmpty()) {
                val limitM = parseRouterOSUptimeToMinutes(u.limitUptime)
                val usedM = parseRouterOSUptimeToMinutes(u.uptime)
                val remainM = (limitM - usedM).coerceAtLeast(0)
                if (antiCoupureEnabled) {
                    // Anti-Coupure : conserve exactement le temps restant pour le client
                    hotspotUsers[i] = u.copy(
                        comment = "Sécurisé Anti-Coupure • Reprise exacte : reste ${formatMinutesToDisplay(remainM)}",
                        antiCoupureSavedRemaining = formatMinutesToDisplay(remainM)
                    )
                } else {
                    // Sans Anti-Coupure : reset dramatique à zéro (1h gratuite pour le client, perte pour le vendeur)
                    hotspotUsers[i] = u.copy(
                        uptime = "0s",
                        comment = "ATTENTION : Compteur réinitialisé suite à coupure (Reset à zéro)"
                    )
                }
            }
        }
        return Result.success(Unit)
    }
}

