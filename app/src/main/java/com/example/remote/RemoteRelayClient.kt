package com.example.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Experimental HTTPS client for MIKROTECK 301 PRO remote mode.
 *
 * SECURITY: Never put ADMIN_API_TOKEN or a router agent token in an Android APK.
 * [accessToken] must come from a future server-issued, user-scoped login session.
 * The current Cloudflare Worker does NOT support customer login yet; this client
 * must remain disabled in production until that authentication is implemented.
 */
class RemoteRelayClient(
    private val accessToken: String,
    private val baseUrl: String = "https://mikroteck-remote-relay.je0515356.workers.dev",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    init {
        require(baseUrl.startsWith("https://")) { "HTTPS is required" }
        require(accessToken.isNotBlank()) { "Missing user access token" }
    }

    private fun routerPath(routerId: String): String {
        require(Regex("[a-zA-Z0-9_-]{8,80}").matches(routerId)) { "Invalid router ID" }
        return "${baseUrl.trimEnd('/')}/v1/routers/$routerId"
    }

    suspend fun requestStatus(routerId: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("action", "router_status").toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url("${routerPath(routerId)}/queue")
            .header("Authorization", "Bearer $accessToken")
            .post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Remote queue HTTP ${response.code}")
            val json = JSONObject(response.body?.string() ?: throw IOException("Empty response"))
            json.getString("id")
        }
    }

    suspend fun getStatusResult(routerId: String, jobId: String): JSONObject =
        withContext(Dispatchers.IO) {
            require(Regex("[0-9a-fA-F-]{36}").matches(jobId)) { "Invalid job ID" }
            val request = Request.Builder()
                .url("${routerPath(routerId)}/result/$jobId")
                .header("Authorization", "Bearer $accessToken")
                .get().build()
            client.newCall(request).execute().use { response ->
                if (response.code == 202) return@withContext JSONObject().put("status", "pending")
                if (!response.isSuccessful) throw IOException("Remote result HTTP ${response.code}")
                JSONObject(response.body?.string() ?: throw IOException("Empty response"))
            }
        }
}
