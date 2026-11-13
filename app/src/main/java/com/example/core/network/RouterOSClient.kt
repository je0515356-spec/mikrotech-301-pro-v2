package com.example.core.network

import com.example.data.routeros.*

interface RouterOSClient {
    suspend fun testConnection(): Result<RouterResource>
    suspend fun getResource(): Result<RouterResource>
    suspend fun getIdentity(): Result<RouterIdentity>

    // Hotspot Users (Tickets)
    suspend fun getHotspotUsers(): Result<List<HotspotUserModel>>
    suspend fun addHotspotUser(user: HotspotUserModel): Result<String>
    suspend fun updateHotspotUser(id: String, user: HotspotUserModel): Result<Unit>
    suspend fun deleteHotspotUser(id: String): Result<Unit>
    suspend fun resetHotspotUserCounters(id: String): Result<Unit>

    // Hotspot Active
    suspend fun getHotspotActive(): Result<List<HotspotActiveModel>>
    suspend fun removeHotspotActive(id: String): Result<Unit>

    // Hotspot Profiles
    suspend fun getHotspotProfiles(): Result<List<HotspotProfileModel>>
    suspend fun addHotspotProfile(profile: HotspotProfileModel): Result<String>
    suspend fun updateHotspotProfile(id: String, profile: HotspotProfileModel): Result<Unit>
    suspend fun deleteHotspotProfile(id: String): Result<Unit>

    // PPPoE Secrets
    suspend fun getPppSecrets(): Result<List<PppSecretModel>>
    suspend fun addPppSecret(secret: PppSecretModel): Result<String>
    suspend fun updatePppSecret(id: String, secret: PppSecretModel): Result<Unit>
    suspend fun deletePppSecret(id: String): Result<Unit>

    // PPPoE Active
    suspend fun getPppActive(): Result<List<PppActiveModel>>
    suspend fun removePppActive(id: String): Result<Unit>

    // PPPoE Profiles
    suspend fun getPppProfiles(): Result<List<PppProfileModel>>
    suspend fun addPppProfile(profile: PppProfileModel): Result<String>
    suspend fun updatePppProfile(id: String, profile: PppProfileModel): Result<Unit>
    suspend fun deletePppProfile(id: String): Result<Unit>

    // Maintenance
    suspend fun rebootRouter(): Result<Unit>
    suspend fun createBackup(name: String): Result<Unit>

    // RouterOS Updates & Upgrades (Safe, preserve configuration)
    suspend fun checkRouterOSUpdate(): Result<RouterOSUpdateInfo>
    suspend fun installRouterOSUpdate(): Result<Unit>

    // Bouclier Anti-Coupure (Protection Délestage Jirama & Reprise Temps Restant)
    suspend fun getAntiCoupureStatus(): Result<AntiCoupureStatus>
    suspend fun installAntiCoupure(): Result<Unit>
    suspend fun disableAntiCoupure(): Result<Unit>
    suspend fun simulatePowerOutage(): Result<Unit>
}
