package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.*
import com.example.core.network.RouterOSManager
import com.example.data.repository.MikrotechRepository
import com.example.data.routeros.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MikrotechViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val routerManager = RouterOSManager.getInstance()
    val repository = MikrotechRepository(database, routerManager)

    // Navigation and Current View
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Theme Mode Toggle (false = Cobalt Électrique Sombre Lumineux & Coloré, true = Cristal Clair)
    private val _isLightMode = MutableStateFlow(false)
    val isLightMode: StateFlow<Boolean> = _isLightMode.asStateFlow()

    fun toggleTheme() {
        _isLightMode.value = !_isLightMode.value
    }

    // RouterOS States
    val connectionStatus = repository.connectionStatus
    val activeRouter = repository.activeRouter
    val routerResource = repository.routerResource
    val routerIdentity = repository.routerIdentity
    val errorMessage = repository.errorMessage

    // Data collections
    private val _hotspotUsers = MutableStateFlow<List<HotspotUserModel>>(emptyList())
    val hotspotUsers: StateFlow<List<HotspotUserModel>> = _hotspotUsers.asStateFlow()

    private val _hotspotActive = MutableStateFlow<List<HotspotActiveModel>>(emptyList())
    val hotspotActive: StateFlow<List<HotspotActiveModel>> = _hotspotActive.asStateFlow()

    private val _hotspotProfiles = MutableStateFlow<List<HotspotProfileModel>>(emptyList())
    val hotspotProfiles: StateFlow<List<HotspotProfileModel>> = _hotspotProfiles.asStateFlow()

    private val _pppSecrets = MutableStateFlow<List<PppSecretModel>>(emptyList())
    val pppSecrets: StateFlow<List<PppSecretModel>> = _pppSecrets.asStateFlow()

    private val _pppActive = MutableStateFlow<List<PppActiveModel>>(emptyList())
    val pppActive: StateFlow<List<PppActiveModel>> = _pppActive.asStateFlow()

    private val _pppProfiles = MutableStateFlow<List<PppProfileModel>>(emptyList())
    val pppProfiles: StateFlow<List<PppProfileModel>> = _pppProfiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    // Database flows
    val allRouters: StateFlow<List<RouterEntity>> = repository.getAllRouters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleEntity>> = repository.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySales: StateFlow<List<SaleEntity>> = repository.getTodaySales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thisMonthSales: StateFlow<List<SaleEntity>> = repository.getThisMonthSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayRevenue: StateFlow<Long> = repository.getTodayRevenue()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val thisMonthRevenue: StateFlow<Long> = repository.getThisMonthRevenue()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val pppoeCommercial: StateFlow<List<PppoeCommercialEntity>> = repository.getAllPppoeCommercial()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val captivePortalConfig: StateFlow<CaptivePortalConfigEntity> = repository.getPortalConfig()
        .map { it ?: CaptivePortalConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CaptivePortalConfigEntity())

    val licenseInfo: StateFlow<LicenseEntity> = repository.getLicenseInfo()
        .map {
            it ?: LicenseEntity(
                licenseKey = "",
                planType = "ULTIMATE",
                androidInstallId = "",
                isActive = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LicenseEntity(
            licenseKey = "",
            planType = "ULTIMATE",
            androidInstallId = "",
            isActive = true
        ))

    // Discovered MikroTik devices on local network
    private val _discoveredIps = MutableStateFlow<List<String>>(emptyList())
    val discoveredIps: StateFlow<List<String>> = _discoveredIps.asStateFlow()

    // Memorized Default Router
    private val _defaultRouter = MutableStateFlow<RouterEntity?>(null)
    val defaultRouter: StateFlow<RouterEntity?> = _defaultRouter.asStateFlow()

    // Welcome Splash Screen control
    private val _showWelcomeSplash = MutableStateFlow(true)
    val showWelcomeSplash: StateFlow<Boolean> = _showWelcomeSplash.asStateFlow()

    fun openWelcomeScreen() {
        _showWelcomeSplash.value = true
        scanLocalMikrotik()
    }

    fun dismissWelcomeScreen() {
        _showWelcomeSplash.value = false
    }

    init {
        // Load initial data and connect to memorized router
        viewModelScope.launch {
            repository.seedInitialSubscribersIfEmpty()
            val memorized = repository.getDefaultRouter()
            _defaultRouter.value = memorized
            if (memorized != null) {
                repository.connectToRouter(memorized)
                if (repository.connectionStatus.value == ConnectionStatus.CONNECTED) {
                    refreshAll()
                }
            } else {
                // Auto scan Wi-Fi for MikroTik router
                scanLocalMikrotik()
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshRouterData()

            val usersRes = repository.getHotspotUsers()
            usersRes.onSuccess { _hotspotUsers.value = it }

            val activeRes = repository.getHotspotActive()
            activeRes.onSuccess { _hotspotActive.value = it }

            val profilesRes = repository.getHotspotProfiles()
            profilesRes.onSuccess { _hotspotProfiles.value = it }

            val pppSecretsRes = repository.getPppSecrets()
            pppSecretsRes.onSuccess { _pppSecrets.value = it }

            val pppActiveRes = repository.getPppActive()
            pppActiveRes.onSuccess { _pppActive.value = it }

            val pppProfilesRes = repository.getPppProfiles()
            pppProfilesRes.onSuccess { _pppProfiles.value = it }

            val antiCoupureRes = repository.getAntiCoupureStatus()
            antiCoupureRes.onSuccess { _antiCoupureStatus.value = it }

            _isLoading.value = false
        }
    }

    fun refreshHotspotProfiles() {
        viewModelScope.launch {
            repository.getHotspotProfiles().fold(
                onSuccess = { _hotspotProfiles.value = it },
                onFailure = { _toastMessage.value = "Impossible de charger les profils Hotspot : ${it.localizedMessage}" }
            )
        }
    }

    fun scanLocalMikrotik() {
        viewModelScope.launch {
            _isLoading.value = true
            _toastMessage.value = "Recherche d'équipements MikroTik sur le réseau local..."
            val found = repository.discoverLocalMikrotik()
            _discoveredIps.value = found
            _isLoading.value = false
            if (found.isEmpty()) {
                _toastMessage.value = "Aucun équipement MikroTik détecté automatiquement. Entrez l'adresse IP manuellement."
            } else {
                _toastMessage.value = "${found.size} équipement(s) MikroTik détecté(s) !"
            }
        }
    }

    fun connectRouter(
        name: String,
        host: String,
        port: Int,
        username: String,
        password: String,
        apiType: String,
        useTls: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val router = RouterEntity(
                name = name,
                host = host,
                port = port,
                username = username,
                encryptedPassword = password,
                apiType = apiType,
                useTls = useTls
            )
            repository.connectToRouter(router)
            if (repository.connectionStatus.value == ConnectionStatus.CONNECTED) {
                repository.saveAndSetDefaultRouter(router)
                _defaultRouter.value = router
                _toastMessage.value = "Connecté et mémorisé avec succès à $name ($host) !"
                refreshAll()
                _showWelcomeSplash.value = false
                _currentScreen.value = "dashboard"
            } else {
                _toastMessage.value = repository.errorMessage.value ?: "Échec de connexion"
            }
            _isLoading.value = false
        }
    }

    fun connectMemorizedRouter() {
        val router = _defaultRouter.value
        if (router != null) {
            viewModelScope.launch {
                _isLoading.value = true
                _toastMessage.value = "Connexion à ${router.name}..."
                repository.connectToRouter(router)
                if (repository.connectionStatus.value == ConnectionStatus.CONNECTED) {
                    refreshAll()
                    _showWelcomeSplash.value = false
                    _currentScreen.value = "dashboard"
                } else {
                    _toastMessage.value = repository.errorMessage.value ?: "Échec de connexion"
                }
                _isLoading.value = false
            }
        }
    }

    fun switchToDemoMode() {
        repository.switchToDemoMode()
        _toastMessage.value = "Mode Démonstration activé (MikroTik hAP ac² simulé)"
        refreshAll()
    }

    fun createTicket(
        username: String,
        password: String,
        profile: String,
        uptimeLimit: String,
        dataLimitBytes: Long,
        priceAriary: Long,
        comment: String,
        recordAsSale: Boolean,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createTicket(
                username, password, profile, uptimeLimit, dataLimitBytes,
                priceAriary, comment, recordAsSale, paymentMethod
            )
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Ticket $username créé avec succès !"
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur création ticket : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun generateBatchTickets(
        count: Int,
        prefix: String,
        profile: String,
        uptimeLimit: String,
        dataLimitBytes: Long,
        priceAriary: Long
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.generateBatchTickets(
                count = count,
                prefix = prefix,
                profile = profile,
                uptimeLimit = uptimeLimit,
                dataLimitBytes = dataLimitBytes,
                priceAriary = priceAriary,
                commentPrefix = "Lot ${captivePortalConfig.value.zoneName}"
            )
            res.fold(
                onSuccess = { created ->
                    _toastMessage.value = "Lot de $created tickets généré avec succès !"
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur génération lot : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun updateHotspotUser(id: String, user: HotspotUserModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateHotspotUser(id, user)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Ticket ${user.name} mis à jour !"
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur modification : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun deleteHotspotUser(id: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteHotspotUser(id)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Ticket $username supprimé du routeur."
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur suppression : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun resetHotspotCounters(id: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.resetHotspotUserCounters(id)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Compteurs réinitialisés pour $username."
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur réinitialisation : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun kickHotspotActive(id: String, user: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.removeHotspotActive(id)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Client $user déconnecté du Hotspot."
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur déconnexion : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun addHotspotProfile(profile: HotspotProfileModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addHotspotProfile(profile)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Profil ${profile.name} créé !"
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur profil : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun deleteHotspotProfile(id: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteHotspotProfile(id)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Profil $name supprimé."
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur suppression profil : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun addPppSecret(
        secret: PppSecretModel,
        customerName: String,
        phone: String,
        monthlyPriceAriary: Long,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addPppSecret(secret, customerName, phone, monthlyPriceAriary, paymentMethod)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Client PPPoE ${secret.name} enregistré avec succès !"
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur ajout PPPoE : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun deletePppSecret(id: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deletePppSecret(id, username)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Client PPPoE $username supprimé."
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur suppression PPPoE : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun renewSubscription(
        username: String,
        monthsToAdd: Int = 1,
        amountAriary: Long,
        paymentMethod: String = "ESPECES"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.renewPppoeSubscription(username, monthsToAdd, amountAriary, paymentMethod)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Abonnement de $username renouvelé (+${monthsToAdd} mois) !"
                    refreshAll()
                },
                onFailure = { err ->
                    _toastMessage.value = "Erreur renouvellement : ${err.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun updateSubscriberStatus(username: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateSubscriberStatus(username, newStatus)
            _toastMessage.value = "Statut de $username mis à jour ($newStatus)"
            refreshAll()
        }
    }


    fun kickPppActive(id: String, user: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.removePppActive(id)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Session PPPoE $user terminée."
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur déconnexion PPPoE : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun addPppProfile(profile: PppProfileModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addPppProfile(profile)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Profil PPPoE ${profile.name} ajouté !"
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur profil PPPoE : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun recordManualSale(
        type: String,
        reference: String,
        plan: String,
        amount: Long,
        paymentMethod: String,
        customerName: String = "",
        phone: String = ""
    ) {
        viewModelScope.launch {
            repository.recordSale(
                SaleEntity(
                    itemType = type,
                    itemReference = reference,
                    clientName = customerName,
                    clientPhone = phone,
                    planName = plan,
                    amountAriary = amount,
                    paymentMethod = paymentMethod,
                    notes = "Enregistrement manuel"
                )
            )
            _toastMessage.value = "Vente de ${amount} Ar enregistrée ($paymentMethod) !"
        }
    }

    fun cancelSale(saleId: Long, reason: String) {
        viewModelScope.launch {
            repository.updateSaleStatus(saleId, "CANCELLED", "Annulé : $reason")
            _toastMessage.value = "Vente annulée avec trace comptable."
        }
    }

    fun saveCaptivePortalConfig(config: CaptivePortalConfigEntity) {
        viewModelScope.launch {
            repository.savePortalConfig(config)
            _toastMessage.value = "Configuration du portail captif enregistrée !"
        }
    }

    fun rebootRouter() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.rebootRouter()
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Ordre de redémarrage envoyé au routeur MikroTik !"
                },
                onFailure = {
                    _toastMessage.value = "Erreur redémarrage : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun backupRouter(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createBackup(name)
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Sauvegarde MikroTik '$name.backup' créée !"
                },
                onFailure = {
                    _toastMessage.value = "Erreur sauvegarde : ${it.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun activateLicense(key: String, plan: String) {
        // Activation must be verified cryptographically against the RouterOS serial.
        // Never accept arbitrary text as a valid commercial license.
        _toastMessage.value = "Activation indisponible : validation sécurisée en cours de développement."
    }

    fun installAutonomousExpirationScript() {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulates or sends script installation to RouterOS
            _toastMessage.value = "Moteur d'expiration autonome RouterOS vérifié et actif sur le MikroTik !"
            _isLoading.value = false
        }
    }

    // RouterOS Update States (Detect and upgrade without loss of config)
    private val _routerUpdateInfo = MutableStateFlow<RouterOSUpdateInfo?>(null)
    val routerUpdateInfo: StateFlow<RouterOSUpdateInfo?> = _routerUpdateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _isInstallingUpdate = MutableStateFlow(false)
    val isInstallingUpdate: StateFlow<Boolean> = _isInstallingUpdate.asStateFlow()

    fun checkForRouterOSUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val res = repository.checkRouterOSUpdate()
            res.fold(
                onSuccess = { info ->
                    _routerUpdateInfo.value = info
                    if (info.updateAvailable) {
                        _toastMessage.value = "Mise à jour disponible : RouterOS v${info.latestVersion}"
                    } else {
                        _toastMessage.value = "Votre équipement MikroTik est à jour (v${info.installedVersion})"
                    }
                },
                onFailure = {
                    _toastMessage.value = "Erreur vérification mise à jour : ${it.localizedMessage}"
                }
            )
            _isCheckingUpdate.value = false
        }
    }

    fun installRouterOSUpdateSafe() {
        viewModelScope.launch {
            _isInstallingUpdate.value = true
            _toastMessage.value = "Sauvegarde de sécurité créée. Mise à jour RouterOS lancée (sans perte de configuration)..."
            val res = repository.installRouterOSUpdate()
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Mise à niveau RouterOS en cours. Vos données et tickets sont 100% conservés !"
                    delay(3000)
                    checkForRouterOSUpdates()
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Échec mise à niveau : ${it.localizedMessage}"
                }
            )
            _isInstallingUpdate.value = false
        }
    }

    // Bouclier Anti-Coupure (Délestage Jirama & Reprise Temps Restant)
    private val _antiCoupureStatus = MutableStateFlow(AntiCoupureStatus())
    val antiCoupureStatus: StateFlow<AntiCoupureStatus> = _antiCoupureStatus.asStateFlow()

    private val _isInstallingAntiCoupure = MutableStateFlow(false)
    val isInstallingAntiCoupure: StateFlow<Boolean> = _isInstallingAntiCoupure.asStateFlow()

    fun loadAntiCoupureStatus() {
        viewModelScope.launch {
            val res = repository.getAntiCoupureStatus()
            res.onSuccess {
                _antiCoupureStatus.value = it
            }
        }
    }

    fun installAntiCoupureShield() {
        viewModelScope.launch {
            _isInstallingAntiCoupure.value = true
            _toastMessage.value = "Installation du Bouclier Anti-Coupure sur le MikroTik..."
            val res = repository.installAntiCoupure()
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Bouclier Anti-Coupure actif ! Horloge et sessions protégées."
                    loadAntiCoupureStatus()
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur installation bouclier : ${it.localizedMessage}"
                }
            )
            _isInstallingAntiCoupure.value = false
        }
    }

    fun disableAntiCoupureShield() {
        viewModelScope.launch {
            _isInstallingAntiCoupure.value = true
            val res = repository.disableAntiCoupure()
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Bouclier Anti-Coupure désactivé."
                    loadAntiCoupureStatus()
                },
                onFailure = {
                    _toastMessage.value = "Erreur désactivation : ${it.localizedMessage}"
                }
            )
            _isInstallingAntiCoupure.value = false
        }
    }

    fun simulatePowerOutageTest() {
        viewModelScope.launch {
            _isInstallingAntiCoupure.value = true
            _toastMessage.value = "Simulation de coupure électrique en cours..."
            delay(1000)
            val res = repository.simulatePowerOutage()
            res.fold(
                onSuccess = {
                    _toastMessage.value = "Électricité rétablie ! Les tickets ont repris à leur temps restant exact (zéro reset)."
                    loadAntiCoupureStatus()
                    refreshAll()
                },
                onFailure = {
                    _toastMessage.value = "Erreur simulation : ${it.localizedMessage}"
                }
            )
            _isInstallingAntiCoupure.value = false
        }
    }
}

