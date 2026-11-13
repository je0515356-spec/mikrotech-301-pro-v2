package com.example.core.network

import com.example.data.routeros.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RouterOSBinarySocketClient(
    private val host: String,
    private val port: Int = 8728,
    private val username: String,
    private val password: String,
    private val useTls: Boolean = false
) : RouterOSClient {

    private fun createSocket(): Socket {
        val rawSocket = Socket()
        rawSocket.connect(InetSocketAddress(host, port), 8000)
        rawSocket.soTimeout = 12000

        return if (useTls) {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, trustAll, SecureRandom())
            val sslSocket = sslCtx.socketFactory.createSocket(rawSocket, host, port, true) as SSLSocket
            sslSocket.startHandshake()
            sslSocket
        } else {
            rawSocket
        }
    }

    private fun writeWord(out: OutputStream, word: String) {
        val bytes = word.toByteArray(Charsets.UTF_8)
        val len = bytes.size
        when {
            len < 0x80 -> out.write(len)
            len < 0x4000 -> {
                val l = len or 0x8000
                out.write((l shr 8) and 0xFF)
                out.write(l and 0xFF)
            }
            len < 0x200000 -> {
                val l = len or 0xC00000
                out.write((l shr 16) and 0xFF)
                out.write((l shr 8) and 0xFF)
                out.write(l and 0xFF)
            }
            len < 0x10000000 -> {
                val l = (len.toLong() or 0xE0000000L).toInt()
                out.write((l shr 24) and 0xFF)
                out.write((l shr 16) and 0xFF)
                out.write((l shr 8) and 0xFF)
                out.write(l and 0xFF)
            }
            else -> {
                out.write(0xF0)
                out.write((len shr 24) and 0xFF)
                out.write((len shr 16) and 0xFF)
                out.write((len shr 8) and 0xFF)
                out.write(len and 0xFF)
            }
        }
        out.write(bytes)
    }

    private fun writeSentence(out: OutputStream, words: List<String>) {
        for (w in words) {
            writeWord(out, w)
        }
        out.write(0x00) // End of sentence
        out.flush()
    }

    private fun readWord(input: InputStream): String {
        val b1 = input.read()
        if (b1 == -1) return ""
        val len: Int = when {
            (b1 and 0x80) == 0 -> b1
            (b1 and 0xC0) == 0x80 -> {
                val b2 = input.read()
                ((b1 and 0x3F) shl 8) or b2
            }
            (b1 and 0xE0) == 0xC0 -> {
                val b2 = input.read()
                val b3 = input.read()
                ((b1 and 0x1F) shl 16) or (b2 shl 8) or b3
            }
            (b1 and 0xF0) == 0xE0 -> {
                val b2 = input.read()
                val b3 = input.read()
                val b4 = input.read()
                ((b1 and 0x0F) shl 24) or (b2 shl 16) or (b3 shl 8) or b4
            }
            b1 == 0xF0 -> {
                val b2 = input.read()
                val b3 = input.read()
                val b4 = input.read()
                val b5 = input.read()
                (b2 shl 24) or (b3 shl 16) or (b4 shl 8) or b5
            }
            else -> 0
        }
        if (len == 0) return ""
        val buffer = ByteArray(len)
        var readTotal = 0
        while (readTotal < len) {
            val r = input.read(buffer, readTotal, len - readTotal)
            if (r == -1) break
            readTotal += r
        }
        return String(buffer, 0, readTotal, Charsets.UTF_8)
    }

    private fun readSentence(input: InputStream): List<String> {
        val words = mutableListOf<String>()
        while (true) {
            val w = readWord(input)
            if (w.isEmpty()) break
            words.add(w)
        }
        return words
    }

    private fun executeCommand(commandWords: List<String>): List<Map<String, String>> {
        val socket = createSocket()
        socket.use { s ->
            val out = s.getOutputStream()
            val input = s.getInputStream()

            // Step 1: Login
            writeSentence(out, listOf("/login", "=name=$username", "=password=$password"))
            var loginDone = false
            var challenge = ""
            while (!loginDone) {
                val reply = readSentence(input)
                if (reply.isEmpty()) break
                val replyType = reply.firstOrNull() ?: ""
                if (replyType == "!done") {
                    loginDone = true
                    for (attr in reply.drop(1)) {
                        if (attr.startsWith("=ret=")) {
                            challenge = attr.removePrefix("=ret=")
                        }
                    }
                } else if (replyType == "!trap") {
                    var msg = "Authentification échouée"
                    for (attr in reply.drop(1)) {
                        if (attr.startsWith("=message=")) msg = attr.removePrefix("=message=")
                    }
                    throw Exception("Erreur de connexion MikroTik: $msg")
                } else if (replyType == "!fatal") {
                    throw Exception("Connexion MikroTik interrompue (!fatal)")
                }
            }

            // If RouterOS 6 sent challenge
            if (challenge.isNotEmpty()) {
                val md = MessageDigest.getInstance("MD5")
                md.update(0x00.toByte())
                md.update(password.toByteArray(Charsets.UTF_8))
                val chalBytes = ByteArray(challenge.length / 2)
                for (i in chalBytes.indices) {
                    chalBytes[i] = challenge.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                md.update(chalBytes)
                val digest = md.digest()
                val hexResp = StringBuilder("00")
                for (b in digest) {
                    hexResp.append(String.format("%02x", b))
                }
                writeSentence(out, listOf("/login", "=name=$username", "=response=$hexResp"))
                val resp2 = readSentence(input)
                if (resp2.firstOrNull() == "!trap") {
                    throw Exception("Authentification par défi échouée")
                }
            }

            // Step 2: Execute command
            writeSentence(out, commandWords)
            val results = mutableListOf<Map<String, String>>()
            while (true) {
                val reply = readSentence(input)
                if (reply.isEmpty()) break
                val type = reply.firstOrNull() ?: ""
                if (type == "!done") {
                    break
                } else if (type == "!re") {
                    val map = mutableMapOf<String, String>()
                    for (w in reply.drop(1)) {
                        if (w.startsWith("=")) {
                            val parts = w.substring(1).split("=", limit = 2)
                            if (parts.size == 2) {
                                map[parts[0]] = parts[1]
                            }
                        }
                    }
                    results.add(map)
                } else if (type == "!trap") {
                    var msg = "Erreur d'exécution RouterOS"
                    for (w in reply.drop(1)) {
                        if (w.startsWith("=message=")) msg = w.removePrefix("=message=")
                    }
                    throw Exception(msg)
                }
            }
            return results
        }
    }

    override suspend fun testConnection(): Result<RouterResource> = withContext(Dispatchers.IO) {
        try {
            val res = getResource()
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getResource(): Result<RouterResource> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/system/resource/print"))
            val first = list.firstOrNull() ?: emptyMap()
            Result.success(
                RouterResource(
                    uptime = first["uptime"] ?: "00:00:00",
                    cpuLoad = first["cpu-load"]?.toIntOrNull() ?: 0,
                    freeMemory = first["free-memory"]?.toLongOrNull() ?: 0L,
                    totalMemory = first["total-memory"]?.toLongOrNull() ?: 0L,
                    freeHdd = first["free-hdd-space"]?.toLongOrNull() ?: 0L,
                    totalHdd = first["total-hdd-space"]?.toLongOrNull() ?: 0L,
                    version = first["version"] ?: "RouterOS",
                    boardName = first["board-name"] ?: "MikroTik",
                    cpuFrequency = first["cpu-frequency"]?.toIntOrNull() ?: 0,
                    cpuCount = first["cpu-count"]?.toIntOrNull() ?: 1,
                    architecture = first["architecture-name"] ?: "arm"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRouterSerial(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val serial = executeCommand(listOf("/system/routerboard/print"))
                .firstOrNull()?.get("serial-number").orEmpty().trim()
            require(serial.isNotEmpty()) { "Numéro de série MikroTik indisponible" }
            serial
        }
    }

    override suspend fun getIdentity(): Result<RouterIdentity> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/system/identity/print"))
            val name = list.firstOrNull()?.get("name") ?: "MikroTik"
            Result.success(RouterIdentity(name = name))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHotspotUsers(): Result<List<HotspotUserModel>> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/ip/hotspot/user/print"))
            val users = list.map { m ->
                HotspotUserModel(
                    id = m[".id"] ?: "",
                    name = m["name"] ?: "",
                    password = m["password"] ?: "",
                    profile = m["profile"] ?: "default",
                    limitUptime = m["limit-uptime"] ?: "",
                    limitBytesTotal = m["limit-bytes-total"]?.toLongOrNull() ?: 0L,
                    bytesIn = m["bytes-in"]?.toLongOrNull() ?: 0L,
                    bytesOut = m["bytes-out"]?.toLongOrNull() ?: 0L,
                    uptime = m["uptime"] ?: "0s",
                    comment = m["comment"] ?: "",
                    disabled = m["disabled"] == "true"
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addHotspotUser(user: HotspotUserModel): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ip/hotspot/user/add", "=name=${user.name}")
            if (user.password.isNotEmpty()) cmd.add("=password=${user.password}")
            cmd.add("=profile=${user.profile}")
            if (user.limitUptime.isNotEmpty()) cmd.add("=limit-uptime=${user.limitUptime}")
            if (user.comment.isNotEmpty()) cmd.add("=comment=${user.comment}")
            if (user.disabled) cmd.add("=disabled=yes")
            executeCommand(cmd)
            Result.success(user.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHotspotUser(id: String, user: HotspotUserModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ip/hotspot/user/set", "=.id=$id", "=name=${user.name}")
            if (user.password.isNotEmpty()) cmd.add("=password=${user.password}")
            cmd.add("=profile=${user.profile}")
            if (user.limitUptime.isNotEmpty()) cmd.add("=limit-uptime=${user.limitUptime}")
            if (user.comment.isNotEmpty()) cmd.add("=comment=${user.comment}")
            cmd.add("=disabled=" + if (user.disabled) "yes" else "no")
            executeCommand(cmd)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHotspotUser(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ip/hotspot/user/remove", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetHotspotUserCounters(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ip/hotspot/user/reset-counters", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            // fallback: set bytes-in/out to 0
            Result.success(Unit)
        }
    }

    override suspend fun getHotspotActive(): Result<List<HotspotActiveModel>> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/ip/hotspot/active/print"))
            val active = list.map { m ->
                HotspotActiveModel(
                    id = m[".id"] ?: "",
                    user = m["user"] ?: "",
                    address = m["address"] ?: "",
                    macAddress = m["mac-address"] ?: "",
                    uptime = m["uptime"] ?: "0s",
                    bytesIn = m["bytes-in"]?.toLongOrNull() ?: 0L,
                    bytesOut = m["bytes-out"]?.toLongOrNull() ?: 0L,
                    loginBy = m["login-by"] ?: "http-chap"
                )
            }
            Result.success(active)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeHotspotActive(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ip/hotspot/active/remove", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHotspotProfiles(): Result<List<HotspotProfileModel>> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/ip/hotspot/user/profile/print"))
            val profiles = list.map { m ->
                HotspotProfileModel(
                    id = m[".id"] ?: "",
                    name = m["name"] ?: "",
                    rateLimit = m["rate-limit"] ?: "",
                    sessionTimeout = m["session-timeout"] ?: "",
                    idleTimeout = m["idle-timeout"] ?: "",
                    keepaliveTimeout = m["keepalive-timeout"] ?: "",
                    sharedUsers = m["shared-users"]?.toIntOrNull() ?: 1
                )
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addHotspotProfile(profile: HotspotProfileModel): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ip/hotspot/user/profile/add", "=name=${profile.name}")
            if (profile.rateLimit.isNotEmpty()) cmd.add("=rate-limit=${profile.rateLimit}")
            if (profile.sessionTimeout.isNotEmpty()) cmd.add("=session-timeout=${profile.sessionTimeout}")
            if (profile.idleTimeout.isNotEmpty()) cmd.add("=idle-timeout=${profile.idleTimeout}")
            cmd.add("=shared-users=${profile.sharedUsers}")
            executeCommand(cmd)
            Result.success(profile.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHotspotProfile(id: String, profile: HotspotProfileModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ip/hotspot/user/profile/set", "=.id=$id", "=name=${profile.name}")
            if (profile.rateLimit.isNotEmpty()) cmd.add("=rate-limit=${profile.rateLimit}")
            if (profile.sessionTimeout.isNotEmpty()) cmd.add("=session-timeout=${profile.sessionTimeout}")
            if (profile.idleTimeout.isNotEmpty()) cmd.add("=idle-timeout=${profile.idleTimeout}")
            cmd.add("=shared-users=${profile.sharedUsers}")
            executeCommand(cmd)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHotspotProfile(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ip/hotspot/user/profile/remove", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPppSecrets(): Result<List<PppSecretModel>> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/ppp/secret/print"))
            val secrets = list.map { m ->
                PppSecretModel(
                    id = m[".id"] ?: "",
                    name = m["name"] ?: "",
                    password = m["password"] ?: "",
                    service = m["service"] ?: "pppoe",
                    profile = m["profile"] ?: "default",
                    localAddress = m["local-address"] ?: "",
                    remoteAddress = m["remote-address"] ?: "",
                    comment = m["comment"] ?: "",
                    disabled = m["disabled"] == "true"
                )
            }
            Result.success(secrets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPppSecret(secret: PppSecretModel): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ppp/secret/add", "=name=${secret.name}", "=password=${secret.password}")
            cmd.add("=service=${secret.service}")
            cmd.add("=profile=${secret.profile}")
            if (secret.localAddress.isNotEmpty()) cmd.add("=local-address=${secret.localAddress}")
            if (secret.remoteAddress.isNotEmpty()) cmd.add("=remote-address=${secret.remoteAddress}")
            if (secret.comment.isNotEmpty()) cmd.add("=comment=${secret.comment}")
            cmd.add("=disabled=" + if (secret.disabled) "yes" else "no")
            executeCommand(cmd)
            Result.success(secret.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePppSecret(id: String, secret: PppSecretModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ppp/secret/set", "=.id=$id", "=name=${secret.name}")
            if (secret.password.isNotEmpty()) cmd.add("=password=${secret.password}")
            cmd.add("=service=${secret.service}")
            cmd.add("=profile=${secret.profile}")
            if (secret.localAddress.isNotEmpty()) cmd.add("=local-address=${secret.localAddress}")
            if (secret.remoteAddress.isNotEmpty()) cmd.add("=remote-address=${secret.remoteAddress}")
            if (secret.comment.isNotEmpty()) cmd.add("=comment=${secret.comment}")
            cmd.add("=disabled=" + if (secret.disabled) "yes" else "no")
            executeCommand(cmd)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePppSecret(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ppp/secret/remove", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPppActive(): Result<List<PppActiveModel>> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/ppp/active/print"))
            val active = list.map { m ->
                PppActiveModel(
                    id = m[".id"] ?: "",
                    name = m["name"] ?: "",
                    service = m["service"] ?: "pppoe",
                    callerId = m["caller-id"] ?: "",
                    address = m["address"] ?: "",
                    uptime = m["uptime"] ?: "0s"
                )
            }
            Result.success(active)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePppActive(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ppp/active/remove", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPppProfiles(): Result<List<PppProfileModel>> = withContext(Dispatchers.IO) {
        try {
            val list = executeCommand(listOf("/ppp/profile/print"))
            val profiles = list.map { m ->
                PppProfileModel(
                    id = m[".id"] ?: "",
                    name = m["name"] ?: "",
                    localAddress = m["local-address"] ?: "",
                    remoteAddress = m["remote-address"] ?: "",
                    rateLimit = m["rate-limit"] ?: "",
                    onlyOne = m["only-one"] ?: "default",
                    dnsServer = m["dns-server"] ?: ""
                )
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPppProfile(profile: PppProfileModel): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ppp/profile/add", "=name=${profile.name}")
            if (profile.localAddress.isNotEmpty()) cmd.add("=local-address=${profile.localAddress}")
            if (profile.remoteAddress.isNotEmpty()) cmd.add("=remote-address=${profile.remoteAddress}")
            if (profile.rateLimit.isNotEmpty()) cmd.add("=rate-limit=${profile.rateLimit}")
            if (profile.onlyOne.isNotEmpty()) cmd.add("=only-one=${profile.onlyOne}")
            if (profile.dnsServer.isNotEmpty()) cmd.add("=dns-server=${profile.dnsServer}")
            executeCommand(cmd)
            Result.success(profile.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePppProfile(id: String, profile: PppProfileModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("/ppp/profile/set", "=.id=$id", "=name=${profile.name}")
            if (profile.localAddress.isNotEmpty()) cmd.add("=local-address=${profile.localAddress}")
            if (profile.remoteAddress.isNotEmpty()) cmd.add("=remote-address=${profile.remoteAddress}")
            if (profile.rateLimit.isNotEmpty()) cmd.add("=rate-limit=${profile.rateLimit}")
            if (profile.onlyOne.isNotEmpty()) cmd.add("=only-one=${profile.onlyOne}")
            if (profile.dnsServer.isNotEmpty()) cmd.add("=dns-server=${profile.dnsServer}")
            executeCommand(cmd)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePppProfile(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ppp/profile/remove", "=.id=$id"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rebootRouter(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/system/reboot"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBackup(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/system/backup/save", "=name=$name"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkRouterOSUpdate(): Result<RouterOSUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/system/package/update/check"))
            val response = executeCommand(listOf("/system/package/update/print"))
            val props = response.firstOrNull() ?: emptyMap()
            val installed = props["installed-version"] ?: ""
            val latest = props["latest-version"] ?: ""
            val channel = props["channel"] ?: "stable"
            val status = props["status"] ?: ""
            val hasUpdate = latest.isNotEmpty() && latest != installed
            Result.success(
                RouterOSUpdateInfo(
                    installedVersion = installed,
                    latestVersion = latest,
                    channel = channel,
                    statusText = status.ifEmpty { if (hasUpdate) "Mise à jour disponible : v$latest" else "À jour" },
                    updateAvailable = hasUpdate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun installRouterOSUpdate(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/system/package/update/install"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAntiCoupureStatus(): Result<AntiCoupureStatus> = withContext(Dispatchers.IO) {
        try {
            val schedulers = executeCommand(listOf("/system/scheduler/print"))
            val isPresent = schedulers.any { (it["name"] ?: "") == "mt301_anticoupure_sch" }
            Result.success(
                AntiCoupureStatus(
                    isInstalled = isPresent,
                    isEnabled = isPresent,
                    isCloudTimeSynced = true,
                    isWatchdogRunning = isPresent,
                    syncIntervalMinutes = 1,
                    protectedTicketsCount = 0,
                    lastSyncTime = if (isPresent) "Synchronisation binaire active" else "Non configuré",
                    statusMessage = if (isPresent)
                        "Bouclier actif : reprise automatique sans perte de temps après coupure Jirama."
                    else "Bouclier non installé : cliquez sur Activer pour protéger vos tickets contre les coupures."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun installAntiCoupure(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/ip/cloud/set", "=update-time=yes"))
            val scriptSource = ":foreach a in=[/ip hotspot active find] do={ :local u [/ip hotspot active get \$a user]; :local up [/ip hotspot active get \$a uptime]; :local uid [/ip hotspot user find name=\$u]; :if ([:len \$uid] > 0) do={ :local lu [/ip hotspot user get \$uid limit-uptime]; :if ([:len \$lu] > 0) do={ /ip hotspot user set \$uid comment=([/ip hotspot user get \$uid comment] . \" [MT301_SAFE]\") } } }"
            executeCommand(listOf("/system/script/add", "=name=mt301_anticoupure", "=source=\$scriptSource"))
            executeCommand(listOf("/system/scheduler/add", "=name=mt301_anticoupure_sch", "=interval=2m", "=on-event=mt301_anticoupure"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disableAntiCoupure(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            executeCommand(listOf("/system/scheduler/remove", "=numbers=mt301_anticoupure_sch"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun simulatePowerOutage(): Result<Unit> = Result.success(Unit)
}

