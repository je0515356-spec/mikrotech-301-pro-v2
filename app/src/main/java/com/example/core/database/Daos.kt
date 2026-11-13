package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RouterDao {
    @Query("SELECT * FROM routers ORDER BY id DESC")
    fun getAllRouters(): Flow<List<RouterEntity>>

    @Query("SELECT * FROM routers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultRouter(): RouterEntity?

    @Query("SELECT * FROM routers WHERE id = :id LIMIT 1")
    suspend fun getRouterById(id: Long): RouterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: RouterEntity): Long

    @Update
    suspend fun updateRouter(router: RouterEntity)

    @Delete
    suspend fun deleteRouter(router: RouterEntity)

    @Query("UPDATE routers SET isDefault = 0")
    suspend fun clearDefaultRouters()

    @Query("UPDATE routers SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultRouter(id: Long)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE routerId = :routerId ORDER BY timestamp DESC")
    fun getSalesByRouter(routerId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getSalesSince(sinceTimestamp: Long): Flow<List<SaleEntity>>

    @Query("SELECT SUM(amountAriary) FROM sales WHERE status = 'COMPLETED' AND timestamp >= :sinceTimestamp")
    fun getTotalRevenueSince(sinceTimestamp: Long): Flow<Long?>

    @Query("SELECT SUM(amountAriary) FROM sales WHERE status = 'COMPLETED'")
    fun getTotalRevenueAllTime(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("UPDATE sales SET status = :newStatus, notes = :notes WHERE id = :saleId")
    suspend fun updateSaleStatus(saleId: Long, newStatus: String, notes: String)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)
}

@Dao
interface LocalTicketDao {
    @Query("SELECT * FROM local_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<LocalTicketEntity>>

    @Query("SELECT * FROM local_tickets WHERE status = :status ORDER BY createdAt DESC")
    fun getTicketsByStatus(status: String): Flow<List<LocalTicketEntity>>

    @Query("SELECT * FROM local_tickets WHERE username = :username LIMIT 1")
    suspend fun getTicketByUsername(username: String): LocalTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<LocalTicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: LocalTicketEntity): Long

    @Update
    suspend fun updateTicket(ticket: LocalTicketEntity)

    @Query("UPDATE local_tickets SET status = :status, soldAt = :soldAt WHERE id = :id")
    suspend fun markTicketSold(id: Long, status: String = "SOLD", soldAt: Long = System.currentTimeMillis())

    @Query("UPDATE local_tickets SET status = :status WHERE id = :id")
    suspend fun updateTicketStatus(id: Long, status: String)

    @Delete
    suspend fun deleteTicket(ticket: LocalTicketEntity)

    @Query("DELETE FROM local_tickets WHERE id = :id")
    suspend fun deleteTicketById(id: Long)
}

@Dao
interface PppoeCommercialDao {
    @Query("SELECT * FROM pppoe_commercial ORDER BY nextRenewalDate ASC")
    fun getAllPppoeClients(): Flow<List<PppoeCommercialEntity>>

    @Query("SELECT * FROM pppoe_commercial WHERE username = :username LIMIT 1")
    suspend fun getPppoeClient(username: String): PppoeCommercialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(client: PppoeCommercialEntity)

    @Delete
    suspend fun delete(client: PppoeCommercialEntity)

    @Query("DELETE FROM pppoe_commercial WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}

@Dao
interface CaptivePortalDao {
    @Query("SELECT * FROM captive_portal_config WHERE id = 1 LIMIT 1")
    fun getPortalConfig(): Flow<CaptivePortalConfigEntity?>

    @Query("SELECT * FROM captive_portal_config WHERE id = 1 LIMIT 1")
    suspend fun getPortalConfigOnce(): CaptivePortalConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePortalConfig(config: CaptivePortalConfigEntity)
}

@Dao
interface LicenseDao {
    @Query("SELECT * FROM license_info WHERE id = 1 LIMIT 1")
    fun getLicenseInfo(): Flow<LicenseEntity?>

    @Query("SELECT * FROM license_info WHERE id = 1 LIMIT 1")
    suspend fun getLicenseInfoOnce(): LicenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLicense(license: LicenseEntity)
}
