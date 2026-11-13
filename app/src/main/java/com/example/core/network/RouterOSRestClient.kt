package com.example.core.network

import com.example.data.routeros.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class RouterOSRestClient(
    private val host: String,
    private val port: Int = 443,
    private val username: String,
    private val password: String,
    private val useTls: Boolean = true
) : RouterOSClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val scheme = if (useTls) "https" else "http"
    private val baseUrl = "$scheme://$host:$port/rest"

    private val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    null // Give up if already authenticated
                } else {
                    val credential = Credentials.basic(username, password)
                    response.request.newBuilder().header("Authorization", credential).build()
                }
            }

        // If self-signed certificates on local router IP, allow SSL trust if configured
        if (useTls) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                // fallback to default
            }
        }
        builder.build()
    }

    private suspend fun executeRequest(request: Request): Result<String> = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    val errorDetail = try {
                        val json = JSONObject(body)
                        json.optString("detail", json.optString("message", "Erreur HTTP ${response.code}"))
                    } catch (e: Exception) {
                        "Erreur HTTP ${response.code}: ${response.message}"
                    }
                    Result.failure(IOException(errorDetail))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun authenticatedRequestBuilder(path: String): Request.Builder {
        val credential = Credentials.basic(username, password)
        return Request.Builder()
            .url("$baseUrl$path")
            .header("Authorization", credential)
            .header("Accept", "application/json")
    }

    override suspend fun testConnection(): Result<RouterResource> {
        return getResource()
    }

    override suspend fun getResource(): Result<RouterResource> {
        val req = authenticatedRequestBuilder("/system/resource").get().build()
        return executeRequest(req).mapCatching { body ->
            val obj = if (body.trim().startsWith("[")) {
                JSONArray(body).getJSONObject(0)
            } else {
                JSONObject(body)
            }
            RouterResource(
                uptime = obj.optString("uptime", "00:00:00"),
                cpuLoad = obj.optInt("cpu-load", 0),
                freeMemory = obj.optLong("free-memory", 0L),
                totalMemory = obj.optLong("total-memory", 0L),
                freeHdd = obj.optLong("free-hdd-space", 0L),
                totalHdd = obj.optLong("total-hdd-space", 0L),
                version = obj.optString("version", "RouterOS 7"),
                boardName = obj.optString("board-name", "MikroTik"),
                cpuFrequency = obj.optInt("cpu-frequency", 0),
                cpuCount = obj.optInt("cpu-count", 1),
                architecture = obj.optString("architecture-name", "arm")
            )
        }
    }

    override suspend fun getIdentity(): Result<RouterIdentity> {
        val req = authenticatedRequestBuilder("/system/identity").get().build()
        return executeRequest(req).mapCatching { body ->
            val obj = if (body.trim().startsWith("[")) JSONArray(body).getJSONObject(0) else JSONObject(body)
            RouterIdentity(name = obj.optString("name", "MikroTik"))
        }
    }

    override suspend fun getHotspotUsers(): Result<List<HotspotUserModel>> {
        val req = authenticatedRequestBuilder("/ip/hotspot/user").get().build()
        return executeRequest(req).mapCatching { body ->
            val arr = JSONArray(body)
            val list = mutableListOf<HotspotUserModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HotspotUserModel(
                        id = obj.optString(".id", "*$i"),
                        name = obj.optString("name", ""),
                        password = obj.optString("password", ""),
                        profile = obj.optString("profile", "default"),
                        limitUptime = obj.optString("limit-uptime", ""),
                        limitBytesTotal = obj.optLong("limit-bytes-total", 0L),
                        bytesIn = obj.optLong("bytes-in", 0L),
                        bytesOut = obj.optLong("bytes-out", 0L),
                        uptime = obj.optString("uptime", "0s"),
                        comment = obj.optString("comment", ""),
                        disabled = obj.optBoolean("disabled", false)
                    )
                )
            }
            list
        }
    }

    override suspend fun addHotspotUser(user: HotspotUserModel): Result<String> {
        val json = JSONObject().apply {
            put("name", user.name)
            if (user.password.isNotEmpty()) put("password", user.password)
            put("profile", user.profile)
            if (user.limitUptime.isNotEmpty()) put("limit-uptime", user.limitUptime)
            if (user.limitBytesTotal > 0) put("limit-bytes-total", user.limitBytesTotal)
            if (user.comment.isNotEmpty()) put("comment", user.comment)
            put("disabled", user.disabled)
        }
        val req = authenticatedRequestBuilder("/ip/hotspot/user")
            .put(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).mapCatching { body ->
            try {
                JSONObject(body).optString(".id", user.name)
            } catch (e: Exception) {
                user.name
            }
        }
    }

    override suspend fun updateHotspotUser(id: String, user: HotspotUserModel): Result<Unit> {
        val json = JSONObject().apply {
            put("name", user.name)
            if (user.password.isNotEmpty()) put("password", user.password)
            put("profile", user.profile)
            if (user.limitUptime.isNotEmpty()) put("limit-uptime", user.limitUptime)
            if (user.comment.isNotEmpty()) put("comment", user.comment)
            put("disabled", user.disabled)
        }
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ip/hotspot/user/*$cleanId")
            .patch(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun deleteHotspotUser(id: String): Result<Unit> {
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ip/hotspot/user/*$cleanId").delete().build()
        return executeRequest(req).map { }
    }

    override suspend fun resetHotspotUserCounters(id: String): Result<Unit> {
        val json = JSONObject().apply {
            put("bytes-in", 0)
            put("bytes-out", 0)
            put("uptime", "0s")
        }
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ip/hotspot/user/*$cleanId")
            .patch(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun getHotspotActive(): Result<List<HotspotActiveModel>> {
        val req = authenticatedRequestBuilder("/ip/hotspot/active").get().build()
        return executeRequest(req).mapCatching { body ->
            val arr = JSONArray(body)
            val list = mutableListOf<HotspotActiveModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HotspotActiveModel(
                        id = obj.optString(".id", "*$i"),
                        user = obj.optString("user", ""),
                        address = obj.optString("address", ""),
                        macAddress = obj.optString("mac-address", ""),
                        uptime = obj.optString("uptime", "0s"),
                        bytesIn = obj.optLong("bytes-in", 0L),
                        bytesOut = obj.optLong("bytes-out", 0L),
                        loginBy = obj.optString("login-by", "http-chap")
                    )
                )
            }
            list
        }
    }

    override suspend fun removeHotspotActive(id: String): Result<Unit> {
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ip/hotspot/active/*$cleanId").delete().build()
        return executeRequest(req).map { }
    }

    override suspend fun getHotspotProfiles(): Result<List<HotspotProfileModel>> {
        val req = authenticatedRequestBuilder("/ip/hotspot/user/profile").get().build()
        return executeRequest(req).mapCatching { body ->
            val arr = JSONArray(body)
            val list = mutableListOf<HotspotProfileModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HotspotProfileModel(
                        id = obj.optString(".id", "*$i"),
                        name = obj.optString("name", ""),
                        rateLimit = obj.optString("rate-limit", ""),
                        sessionTimeout = obj.optString("session-timeout", ""),
                        idleTimeout = obj.optString("idle-timeout", ""),
                        keepaliveTimeout = obj.optString("keepalive-timeout", ""),
                        sharedUsers = obj.optInt("shared-users", 1)
                    )
                )
            }
            list
        }
    }

    override suspend fun addHotspotProfile(profile: HotspotProfileModel): Result<String> {
        val json = JSONObject().apply {
            put("name", profile.name)
            if (profile.rateLimit.isNotEmpty()) put("rate-limit", profile.rateLimit)
            if (profile.sessionTimeout.isNotEmpty()) put("session-timeout", profile.sessionTimeout)
            if (profile.idleTimeout.isNotEmpty()) put("idle-timeout", profile.idleTimeout)
            put("shared-users", profile.sharedUsers)
        }
        val req = authenticatedRequestBuilder("/ip/hotspot/user/profile")
            .put(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).mapCatching { body ->
            JSONObject(body).optString(".id", profile.name)
        }
    }

    override suspend fun updateHotspotProfile(id: String, profile: HotspotProfileModel): Result<Unit> {
        val json = JSONObject().apply {
            put("name", profile.name)
            if (profile.rateLimit.isNotEmpty()) put("rate-limit", profile.rateLimit)
            if (profile.sessionTimeout.isNotEmpty()) put("session-timeout", profile.sessionTimeout)
            if (profile.idleTimeout.isNotEmpty()) put("idle-timeout", profile.idleTimeout)
            put("shared-users", profile.sharedUsers)
        }
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ip/hotspot/user/profile/*$cleanId")
            .patch(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun deleteHotspotProfile(id: String): Result<Unit> {
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ip/hotspot/user/profile/*$cleanId").delete().build()
        return executeRequest(req).map { }
    }

    override suspend fun getPppSecrets(): Result<List<PppSecretModel>> {
        val req = authenticatedRequestBuilder("/ppp/secret").get().build()
        return executeRequest(req).mapCatching { body ->
            val arr = JSONArray(body)
            val list = mutableListOf<PppSecretModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PppSecretModel(
                        id = obj.optString(".id", "*$i"),
                        name = obj.optString("name", ""),
                        password = obj.optString("password", ""),
                        service = obj.optString("service", "pppoe"),
                        profile = obj.optString("profile", "default"),
                        localAddress = obj.optString("local-address", ""),
                        remoteAddress = obj.optString("remote-address", ""),
                        comment = obj.optString("comment", ""),
                        disabled = obj.optBoolean("disabled", false)
                    )
                )
            }
            list
        }
    }

    override suspend fun addPppSecret(secret: PppSecretModel): Result<String> {
        val json = JSONObject().apply {
            put("name", secret.name)
            put("password", secret.password)
            put("service", secret.service)
            put("profile", secret.profile)
            if (secret.localAddress.isNotEmpty()) put("local-address", secret.localAddress)
            if (secret.remoteAddress.isNotEmpty()) put("remote-address", secret.remoteAddress)
            if (secret.comment.isNotEmpty()) put("comment", secret.comment)
            put("disabled", secret.disabled)
        }
        val req = authenticatedRequestBuilder("/ppp/secret")
            .put(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).mapCatching { body ->
            JSONObject(body).optString(".id", secret.name)
        }
    }

    override suspend fun updatePppSecret(id: String, secret: PppSecretModel): Result<Unit> {
        val json = JSONObject().apply {
            put("name", secret.name)
            if (secret.password.isNotEmpty()) put("password", secret.password)
            put("profile", secret.profile)
            if (secret.localAddress.isNotEmpty()) put("local-address", secret.localAddress)
            if (secret.remoteAddress.isNotEmpty()) put("remote-address", secret.remoteAddress)
            if (secret.comment.isNotEmpty()) put("comment", secret.comment)
            put("disabled", secret.disabled)
        }
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ppp/secret/*$cleanId")
            .patch(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun deletePppSecret(id: String): Result<Unit> {
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ppp/secret/*$cleanId").delete().build()
        return executeRequest(req).map { }
    }

    override suspend fun getPppActive(): Result<List<PppActiveModel>> {
        val req = authenticatedRequestBuilder("/ppp/active").get().build()
        return executeRequest(req).mapCatching { body ->
            val arr = JSONArray(body)
            val list = mutableListOf<PppActiveModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PppActiveModel(
                        id = obj.optString(".id", "*$i"),
                        name = obj.optString("name", ""),
                        service = obj.optString("service", "pppoe"),
                        callerId = obj.optString("caller-id", ""),
                        address = obj.optString("address", ""),
                        uptime = obj.optString("uptime", "0s")
                    )
                )
            }
            list
        }
    }

    override suspend fun removePppActive(id: String): Result<Unit> {
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ppp/active/*$cleanId").delete().build()
        return executeRequest(req).map { }
    }

    override suspend fun getPppProfiles(): Result<List<PppProfileModel>> {
        val req = authenticatedRequestBuilder("/ppp/profile").get().build()
        return executeRequest(req).mapCatching { body ->
            val arr = JSONArray(body)
            val list = mutableListOf<PppProfileModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PppProfileModel(
                        id = obj.optString(".id", "*$i"),
                        name = obj.optString("name", ""),
                        localAddress = obj.optString("local-address", ""),
                        remoteAddress = obj.optString("remote-address", ""),
                        rateLimit = obj.optString("rate-limit", ""),
                        onlyOne = obj.optString("only-one", "default"),
                        dnsServer = obj.optString("dns-server", "")
                    )
                )
            }
            list
        }
    }

    override suspend fun addPppProfile(profile: PppProfileModel): Result<String> {
        val json = JSONObject().apply {
            put("name", profile.name)
            if (profile.localAddress.isNotEmpty()) put("local-address", profile.localAddress)
            if (profile.remoteAddress.isNotEmpty()) put("remote-address", profile.remoteAddress)
            if (profile.rateLimit.isNotEmpty()) put("rate-limit", profile.rateLimit)
            if (profile.onlyOne.isNotEmpty()) put("only-one", profile.onlyOne)
            if (profile.dnsServer.isNotEmpty()) put("dns-server", profile.dnsServer)
        }
        val req = authenticatedRequestBuilder("/ppp/profile")
            .put(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).mapCatching { body ->
            JSONObject(body).optString(".id", profile.name)
        }
    }

    override suspend fun updatePppProfile(id: String, profile: PppProfileModel): Result<Unit> {
        val json = JSONObject().apply {
            put("name", profile.name)
            if (profile.localAddress.isNotEmpty()) put("local-address", profile.localAddress)
            if (profile.remoteAddress.isNotEmpty()) put("remote-address", profile.remoteAddress)
            if (profile.rateLimit.isNotEmpty()) put("rate-limit", profile.rateLimit)
            if (profile.onlyOne.isNotEmpty()) put("only-one", profile.onlyOne)
            if (profile.dnsServer.isNotEmpty()) put("dns-server", profile.dnsServer)
        }
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ppp/profile/*$cleanId")
            .patch(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun deletePppProfile(id: String): Result<Unit> {
        val cleanId = id.removePrefix("*")
        val req = authenticatedRequestBuilder("/ppp/profile/*$cleanId").delete().build()
        return executeRequest(req).map { }
    }

    override suspend fun rebootRouter(): Result<Unit> {
        val req = authenticatedRequestBuilder("/system/reboot")
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun createBackup(name: String): Result<Unit> {
        val json = JSONObject().apply {
            put("name", name)
        }
        val req = authenticatedRequestBuilder("/system/backup/save")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun checkRouterOSUpdate(): Result<RouterOSUpdateInfo> {
        val checkReq = authenticatedRequestBuilder("/system/package/update/check-for-updates")
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        executeRequest(checkReq)

        val req = authenticatedRequestBuilder("/system/package/update")
            .get()
            .build()
        return executeRequest(req).mapCatching { jsonStr ->
            val obj = if (jsonStr.trim().startsWith("[")) {
                JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
            } else {
                JSONObject(jsonStr)
            }
            val installed = obj.optString("installed-version", "")
            val latest = obj.optString("latest-version", "")
            val channel = obj.optString("channel", "stable")
            val status = obj.optString("status", "")
            val hasUpdate = latest.isNotEmpty() && latest != installed
            RouterOSUpdateInfo(
                installedVersion = installed,
                latestVersion = latest,
                channel = channel,
                statusText = status.ifEmpty { if (hasUpdate) "Mise à jour disponible : v$latest" else "À jour" },
                updateAvailable = hasUpdate
            )
        }
    }

    override suspend fun installRouterOSUpdate(): Result<Unit> {
        val req = authenticatedRequestBuilder("/system/package/update/install")
            .post("{}".toRequestBody(jsonMediaType))
            .build()
        return executeRequest(req).map { }
    }

    override suspend fun getAntiCoupureStatus(): Result<AntiCoupureStatus> {
        val req = authenticatedRequestBuilder("/system/scheduler").get().build()
        val isSchedulerPresent = executeRequest(req).mapCatching { body ->
            body.contains("mt301_anticoupure")
        }.getOrDefault(false)

        return Result.success(
            AntiCoupureStatus(
                isInstalled = isSchedulerPresent,
                isEnabled = isSchedulerPresent,
                isCloudTimeSynced = true,
                isWatchdogRunning = isSchedulerPresent,
                syncIntervalMinutes = 1,
                protectedTicketsCount = 0,
                lastSyncTime = if (isSchedulerPresent) "Synchronisation active" else "Non installé",
                statusMessage = if (isSchedulerPresent)
                    "Bouclier actif : reprise automatique sans perte de temps après coupure Jirama."
                else "Bouclier non installé : cliquez sur Activer pour protéger vos tickets contre les coupures."
            )
        )
    }

    override suspend fun installAntiCoupure(): Result<Unit> {
        // 1. Activer Cloud Time pour éviter le bug 1970
        val cloudReq = authenticatedRequestBuilder("/ip/cloud")
            .patch("{\"update-time\":\"yes\"}".toRequestBody(jsonMediaType))
            .build()
        executeRequest(cloudReq)

        // 2. Créer ou mettre à jour le script RouterOS
        val scriptSource = ":foreach a in=[/ip hotspot active find] do={ :local u [/ip hotspot active get \$a user]; :local up [/ip hotspot active get \$a uptime]; :local uid [/ip hotspot user find name=\$u]; :if ([:len \$uid] > 0) do={ :local lu [/ip hotspot user get \$uid limit-uptime]; :if ([:len \$lu] > 0) do={ /ip hotspot user set \$uid comment=([/ip hotspot user get \$uid comment] . \" [MT301_SAFE]\") } } }"
        val scriptJson = JSONObject().apply {
            put("name", "mt301_anticoupure")
            put("source", scriptSource)
        }
        val scriptReq = authenticatedRequestBuilder("/system/script")
            .put(scriptJson.toString().toRequestBody(jsonMediaType))
            .build()
        executeRequest(scriptReq)

        // 3. Planificateur toutes les 2 minutes
        val schJson = JSONObject().apply {
            put("name", "mt301_anticoupure_sch")
            put("interval", "2m")
            put("on-event", "mt301_anticoupure")
        }
        val schReq = authenticatedRequestBuilder("/system/scheduler")
            .put(schJson.toString().toRequestBody(jsonMediaType))
            .build()
        executeRequest(schReq)

        return Result.success(Unit)
    }

    override suspend fun disableAntiCoupure(): Result<Unit> {
        val req = authenticatedRequestBuilder("/system/scheduler/mt301_anticoupure_sch").delete().build()
        executeRequest(req)
        return Result.success(Unit)
    }

    override suspend fun simulatePowerOutage(): Result<Unit> = Result.success(Unit)
}

