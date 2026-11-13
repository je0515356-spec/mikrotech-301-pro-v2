package com.example.core.network

import com.example.core.database.RouterEntity
import com.example.data.routeros.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket

class RouterOSManager {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _activeRouter = MutableStateFlow<RouterEntity?>(null)
    val activeRouter: StateFlow<RouterEntity?> = _activeRouter.asStateFlow()

    private val _routerResource = MutableStateFlow(RouterResource())
    val routerResource: StateFlow<RouterResource> = _routerResource.asStateFlow()

    private val _routerIdentity = MutableStateFlow(RouterIdentity("MIKROTECH-301"))
    val routerIdentity: StateFlow<RouterIdentity> = _routerIdentity.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Default to Demo Client initially for immediate testing & offline exploration
    private var currentClient: RouterOSClient = DemoRouterOSClient()

    fun getClient(): RouterOSClient = currentClient

    suspend fun connectToRouter(router: RouterEntity) = withContext(Dispatchers.IO) {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _errorMessage.value = null

        val client = if (router.apiType == "REST") {
            RouterOSRestClient(
                host = router.host,
                port = router.port,
                username = router.username,
                password = router.encryptedPassword,
                useTls = router.useTls
            )
        } else {
            RouterOSBinarySocketClient(
                host = router.host,
                port = router.port,
                username = router.username,
                password = router.encryptedPassword,
                useTls = router.useTls
            )
        }

        val testRes = client.testConnection()
        testRes.fold(
            onSuccess = { res ->
                currentClient = client
                _activeRouter.value = router
                _routerResource.value = res
                _connectionStatus.value = ConnectionStatus.CONNECTED
                val ident = client.getIdentity().getOrDefault(RouterIdentity(router.name))
                _routerIdentity.value = ident
            },
            onFailure = { err ->
                _connectionStatus.value = ConnectionStatus.ERROR
                _errorMessage.value = "Erreur de connexion (${router.host}:${router.port}) : ${err.localizedMessage}"
            }
        )
    }

    fun switchToDemoMode() {
        currentClient = DemoRouterOSClient()
        _activeRouter.value = null
        _connectionStatus.value = ConnectionStatus.DEMO_MODE
        _routerResource.value = RouterResource(
            uptime = "14d 06:28:14",
            cpuLoad = 18,
            freeMemory = 89653248L,
            totalMemory = 134217728L,
            freeHdd = 8388608L,
            totalHdd = 16777216L,
            version = "7.15.2 (stable)",
            boardName = "hAP ac² (RBD52G-5HacD2HnD)",
            cpuFrequency = 716,
            cpuCount = 4,
            architecture = "arm"
        )
        _routerIdentity.value = RouterIdentity("MIKROTECH-301-DEMO")
        _errorMessage.value = null
    }

    suspend fun refreshData() = withContext(Dispatchers.IO) {
        if (_isRefreshing.value) return@withContext
        _isRefreshing.value = true
        try {
            val res = currentClient.getResource()
            res.onSuccess { _routerResource.value = it }
            val ident = currentClient.getIdentity()
            ident.onSuccess { _routerIdentity.value = it }
        } catch (e: Exception) {
            // Keep last known data
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun discoverLocalMikrotik(): List<String> = withContext(Dispatchers.IO) {
        val candidates = listOf("192.168.88.1", "192.168.1.1", "192.168.0.1", "10.0.0.1")
        val found = mutableListOf<String>()
        for (ip in candidates) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(ip, 80), 300)
                s.close()
                found.add(ip)
            } catch (e: Exception) {
                try {
                    val s2 = Socket()
                    s2.connect(InetSocketAddress(ip, 8728), 300)
                    s2.close()
                    found.add(ip)
                } catch (e2: Exception) {
                    // Not reached
                }
            }
        }
        found
    }

    companion object {
        @Volatile
        private var INSTANCE: RouterOSManager? = null

        fun getInstance(): RouterOSManager {
            return INSTANCE ?: synchronized(this) {
                val inst = RouterOSManager()
                INSTANCE = inst
                inst
            }
        }
    }
}
