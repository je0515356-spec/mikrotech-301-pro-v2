package com.example.data.repository

import com.example.core.database.*
import com.example.core.network.RouterOSManager
import com.example.data.routeros.*
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.util.*

class MikrotechRepository(
    private val database: AppDatabase,
    private val routerManager: RouterOSManager
) {
    private val routerDao = database.routerDao()
    private val saleDao = database.saleDao()
    private val localTicketDao = database.localTicketDao()
    private val pppoeCommercialDao = database.pppoeCommercialDao()
    private val captivePortalDao = database.captivePortalDao()
    private val licenseDao = database.licenseDao()

    val connectionStatus = routerManager.connectionStatus
    val activeRouter = routerManager.activeRouter
    val routerResource = routerManager.routerResource
    val routerIdentity = routerManager.routerIdentity
    val errorMessage = routerManager.errorMessage

    // Routers
    fun getAllRouters(): Flow<List<RouterEntity>> = routerDao.getAllRouters()
    suspend fun getDefaultRouter(): RouterEntity? = routerDao.getDefaultRouter()
    suspend fun saveAndSetDefaultRouter(router: RouterEntity): Long {
        routerDao.clearDefaultRouters()
        val routerToSave = router.copy(isDefault = true, lastConnected = System.currentTimeMillis())
        return routerDao.insertRouter(routerToSave)
    }
    suspend fun insertRouter(router: RouterEntity) = routerDao.insertRouter(router)
    suspend fun updateRouter(router: RouterEntity) = routerDao.updateRouter(router)
    suspend fun deleteRouter(router: RouterEntity) = routerDao.deleteRouter(router)
    suspend fun connectToRouter(router: RouterEntity) = routerManager.connectToRouter(router)
    fun switchToDemoMode() = routerManager.switchToDemoMode()
    suspend fun refreshRouterData() = routerManager.refreshData()
    suspend fun discoverLocalMikrotik() = routerManager.discoverLocalMikrotik()

    // Sales & Accounting
    fun getAllSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()
    fun getTodaySales(): Flow<List<SaleEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return saleDao.getSalesSince(calendar.timeInMillis)
    }
    fun getThisMonthSales(): Flow<List<SaleEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return saleDao.getSalesSince(calendar.timeInMillis)
    }
    fun getTodayRevenue(): Flow<Long?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return saleDao.getTotalRevenueSince(calendar.timeInMillis)
    }
    fun getThisMonthRevenue(): Flow<Long?> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return saleDao.getTotalRevenueSince(calendar.timeInMillis)
    }
    suspend fun recordSale(sale: SaleEntity): Long = saleDao.insertSale(sale)
    suspend fun updateSaleStatus(saleId: Long, newStatus: String, notes: String) =
        saleDao.updateSaleStatus(saleId, newStatus, notes)

    // Hotspot Users / Tickets
    suspend fun getHotspotUsers(): Result<List<HotspotUserModel>> =
        routerManager.getClient().getHotspotUsers()

    suspend fun createTicket(
        username: String,
        password: String,
        profile: String,
        uptimeLimit: String,
        dataLimitBytes: Long,
        priceAriary: Long,
        comment: String,
        recordAsSale: Boolean,
        paymentMethod: String = "ESPECES"
    ): Result<String> {
        val user = HotspotUserModel(
            name = username,
            password = password,
            profile = profile,
            limitUptime = uptimeLimit,
            limitBytesTotal = dataLimitBytes,
            comment = comment
        )
        val res = routerManager.getClient().addHotspotUser(user)
        res.onSuccess {
            // Save locally
            val local = LocalTicketEntity(
                username = username,
                password = password,
                profile = profile,
                durationMinutes = parseUptimeToMinutes(uptimeLimit),
                dataLimitBytes = dataLimitBytes,
                priceAriary = priceAriary,
                comment = comment,
                status = if (recordAsSale) "SOLD" else "AVAILABLE",
                soldAt = if (recordAsSale) System.currentTimeMillis() else null,
                isSyncedToRouter = true
            )
            localTicketDao.insertTicket(local)

            if (recordAsSale && priceAriary > 0) {
                saleDao.insertSale(
                    SaleEntity(
                        itemType = "HOTSPOT_TICKET",
                        itemReference = username,
                        planName = profile,
                        amountAriary = priceAriary,
                        paymentMethod = paymentMethod,
                        notes = "Ticket créé et vendu"
                    )
                )
            }
        }
        return res
    }

    suspend fun generateBatchTickets(
        count: Int,
        prefix: String = "mt",
        profile: String,
        uptimeLimit: String,
        dataLimitBytes: Long,
        priceAriary: Long,
        commentPrefix: String = "Lot MIKROTECH 301"
    ): Result<Int> {
        val random = SecureRandom()
        var createdCount = 0
        for (i in 1..count) {
            val userNum = 1000 + random.nextInt(9000)
            val passNum = 1000 + random.nextInt(9000)
            val username = "$prefix$userNum"
            val password = "$passNum"
            val comment = "$commentPrefix - Ar $priceAriary"
            val r = createTicket(
                username = username,
                password = password,
                profile = profile,
                uptimeLimit = uptimeLimit,
                dataLimitBytes = dataLimitBytes,
                priceAriary = priceAriary,
                comment = comment,
                recordAsSale = false
            )
            if (r.isSuccess) {
                createdCount++
            }
        }
        return Result.success(createdCount)
    }

    suspend fun updateHotspotUser(id: String, user: HotspotUserModel): Result<Unit> =
        routerManager.getClient().updateHotspotUser(id, user)

    suspend fun deleteHotspotUser(id: String): Result<Unit> =
        routerManager.getClient().deleteHotspotUser(id)

    suspend fun resetHotspotUserCounters(id: String): Result<Unit> =
        routerManager.getClient().resetHotspotUserCounters(id)

    // Hotspot Active & Profiles
    suspend fun getHotspotActive(): Result<List<HotspotActiveModel>> =
        routerManager.getClient().getHotspotActive()

    suspend fun removeHotspotActive(id: String): Result<Unit> =
        routerManager.getClient().removeHotspotActive(id)

    suspend fun getHotspotProfiles(): Result<List<HotspotProfileModel>> =
        routerManager.getClient().getHotspotProfiles()

    suspend fun addHotspotProfile(profile: HotspotProfileModel): Result<String> =
        routerManager.getClient().addHotspotProfile(profile)

    suspend fun updateHotspotProfile(id: String, profile: HotspotProfileModel): Result<Unit> =
        routerManager.getClient().updateHotspotProfile(id, profile)

    suspend fun deleteHotspotProfile(id: String): Result<Unit> =
        routerManager.getClient().deleteHotspotProfile(id)

    // PPPoE
    suspend fun getPppSecrets(): Result<List<PppSecretModel>> =
        routerManager.getClient().getPppSecrets()

    suspend fun addPppSecret(
        secret: PppSecretModel,
        customerName: String,
        phone: String,
        monthlyPriceAriary: Long,
        paymentMethod: String = "ESPECES"
    ): Result<String> {
        val res = routerManager.getClient().addPppSecret(secret)
        res.onSuccess {
            // Save commercial info
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, 1)
            pppoeCommercialDao.insertOrUpdate(
                PppoeCommercialEntity(
                    username = secret.name,
                    customerName = customerName,
                    phone = phone,
                    monthlyPriceAriary = monthlyPriceAriary,
                    paymentMethod = paymentMethod,
                    nextRenewalDate = cal.timeInMillis
                )
            )
            if (monthlyPriceAriary > 0) {
                saleDao.insertSale(
                    SaleEntity(
                        itemType = "PPPOE_SUBSCRIPTION",
                        itemReference = secret.name,
                        clientName = customerName,
                        clientPhone = phone,
                        planName = secret.profile,
                        amountAriary = monthlyPriceAriary,
                        paymentMethod = paymentMethod,
                        notes = "Souscription abonnement PPPoE 1 mois"
                    )
                )
            }
        }
        return res
    }

    suspend fun updatePppSecret(id: String, secret: PppSecretModel): Result<Unit> =
        routerManager.getClient().updatePppSecret(id, secret)

    suspend fun renewPppoeSubscription(
        username: String,
        monthsToAdd: Int = 1,
        amountAriary: Long,
        paymentMethod: String = "ESPECES"
    ): Result<Unit> {
        val client = pppoeCommercialDao.getPppoeClient(username) ?: return Result.failure(Exception("Abonné non trouvé"))
        val baseTime = if (client.nextRenewalDate > System.currentTimeMillis()) client.nextRenewalDate else System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = baseTime
        cal.add(Calendar.MONTH, monthsToAdd)
        val updated = client.copy(
            lastPaymentDate = System.currentTimeMillis(),
            nextRenewalDate = cal.timeInMillis,
            status = "ACTIVE"
        )
        pppoeCommercialDao.insertOrUpdate(updated)
        if (amountAriary > 0) {
            saleDao.insertSale(
                SaleEntity(
                    itemType = "PPPOE_SUBSCRIPTION",
                    itemReference = username,
                    clientName = client.customerName,
                    clientPhone = client.phone,
                    planName = "Renouvellement +$monthsToAdd Mois",
                    amountAriary = amountAriary,
                    paymentMethod = paymentMethod,
                    notes = "Renouvellement abonnement mensuel"
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun updateSubscriberStatus(username: String, newStatus: String): Result<Unit> {
        val client = pppoeCommercialDao.getPppoeClient(username) ?: return Result.failure(Exception("Abonné non trouvé"))
        pppoeCommercialDao.insertOrUpdate(client.copy(status = newStatus))
        return Result.success(Unit)
    }

    suspend fun seedInitialSubscribersIfEmpty() {
        val existing = pppoeCommercialDao.getPppoeClient("razafy_home")
        if (existing == null) {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000
            pppoeCommercialDao.insertOrUpdate(
                PppoeCommercialEntity(
                    username = "razafy_home",
                    customerName = "Razafindrakoto Jean",
                    phone = "034 11 222 33",
                    address = "Lot IVB 42, Antananarivo",
                    monthlyPriceAriary = 45000L,
                    paymentMethod = "MVOLA",
                    lastPaymentDate = now - (12 * dayMs),
                    nextRenewalDate = now + (18 * dayMs),
                    status = "ACTIVE",
                    notes = "Forfait Fibre Maison 10 Mbps • 14.2 Go consommés / 50 Go"
                )
            )
            pppoeCommercialDao.insertOrUpdate(
                PppoeCommercialEntity(
                    username = "cyber_pro",
                    customerName = "Cyber Ambodivona",
                    phone = "032 55 678 90",
                    address = "Enceinte Cyber, Ambodivona",
                    monthlyPriceAriary = 120000L,
                    paymentMethod = "AIRTEL_MONEY",
                    lastPaymentDate = now - (27 * dayMs),
                    nextRenewalDate = now + (3 * dayMs),
                    status = "EXPIRING_SOON",
                    notes = "Ligne Dédiée 20 Mbps • 84.5 Go consommés / 100 Go"
                )
            )
            pppoeCommercialDao.insertOrUpdate(
                PppoeCommercialEntity(
                    username = "printemps_vpn",
                    customerName = "Hôtel Le Printemps",
                    phone = "033 99 888 77",
                    address = "Boulevard de la Mer, Majunga",
                    monthlyPriceAriary = 80000L,
                    paymentMethod = "ORANGE_MONEY",
                    lastPaymentDate = now - (32 * dayMs),
                    nextRenewalDate = now - (2 * dayMs),
                    status = "EXPIRED",
                    notes = "Abonnement Mensuel Hôtelier • 50 Go consommés (Échéance dépassée)"
                )
            )
        }
    }

    suspend fun deletePppSecret(id: String, username: String): Result<Unit> {
        val res = routerManager.getClient().deletePppSecret(id)
        pppoeCommercialDao.deleteByUsername(username)
        return res
    }

    suspend fun getPppActive(): Result<List<PppActiveModel>> =

        routerManager.getClient().getPppActive()

    suspend fun removePppActive(id: String): Result<Unit> =
        routerManager.getClient().removePppActive(id)

    suspend fun getPppProfiles(): Result<List<PppProfileModel>> =
        routerManager.getClient().getPppProfiles()

    suspend fun addPppProfile(profile: PppProfileModel): Result<String> =
        routerManager.getClient().addPppProfile(profile)

    suspend fun updatePppProfile(id: String, profile: PppProfileModel): Result<Unit> =
        routerManager.getClient().updatePppProfile(id, profile)

    suspend fun deletePppProfile(id: String): Result<Unit> =
        routerManager.getClient().deletePppProfile(id)

    fun getAllPppoeCommercial(): Flow<List<PppoeCommercialEntity>> =
        pppoeCommercialDao.getAllPppoeClients()

    // Captive Portal Config
    fun getPortalConfig(): Flow<CaptivePortalConfigEntity?> = captivePortalDao.getPortalConfig()
    suspend fun savePortalConfig(config: CaptivePortalConfigEntity) = captivePortalDao.savePortalConfig(config)

    // License
    fun getLicenseInfo(): Flow<LicenseEntity?> = licenseDao.getLicenseInfo()
    suspend fun saveLicense(license: LicenseEntity) = licenseDao.saveLicense(license)

    // Router Maintenance
    suspend fun rebootRouter(): Result<Unit> = routerManager.getClient().rebootRouter()
    suspend fun createBackup(name: String): Result<Unit> = routerManager.getClient().createBackup(name)

    // RouterOS Safe Upgrade
    suspend fun checkRouterOSUpdate(): Result<RouterOSUpdateInfo> = routerManager.getClient().checkRouterOSUpdate()
    suspend fun installRouterOSUpdate(): Result<Unit> {
        // Safe upgrade: take an automated backup first to guarantee zero loss of config
        val backupName = "pre_maj_${System.currentTimeMillis() % 100000}"
        routerManager.getClient().createBackup(backupName)
        return routerManager.getClient().installRouterOSUpdate()
    }

    // Bouclier Haute Disponibilité & Anti-Coupure
    suspend fun getAntiCoupureStatus(): Result<AntiCoupureStatus> = routerManager.getClient().getAntiCoupureStatus()
    suspend fun installAntiCoupure(): Result<Unit> = routerManager.getClient().installAntiCoupure()
    suspend fun disableAntiCoupure(): Result<Unit> = routerManager.getClient().disableAntiCoupure()
    suspend fun simulatePowerOutage(): Result<Unit> = routerManager.getClient().simulatePowerOutage()


    private fun parseUptimeToMinutes(uptime: String): Int {
        if (uptime.isEmpty()) return 0
        var total = 0
        val pattern = "(\\d+)([wdhm])".toRegex()
        pattern.findAll(uptime).forEach { match ->
            val v = match.groupValues[1].toIntOrNull() ?: 0
            when (match.groupValues[2]) {
                "w" -> total += v * 7 * 24 * 60
                "d" -> total += v * 24 * 60
                "h" -> total += v * 60
                "m" -> total += v
            }
        }
        return if (total == 0) uptime.toIntOrNull() ?: 0 else total
    }
}
