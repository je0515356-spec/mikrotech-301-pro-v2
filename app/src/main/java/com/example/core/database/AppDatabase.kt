package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RouterEntity::class,
        SaleEntity::class,
        LocalTicketEntity::class,
        PppoeCommercialEntity::class,
        CaptivePortalConfigEntity::class,
        LicenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routerDao(): RouterDao
    abstract fun saleDao(): SaleDao
    abstract fun localTicketDao(): LocalTicketDao
    abstract fun pppoeCommercialDao(): PppoeCommercialDao
    abstract fun captivePortalDao(): CaptivePortalDao
    abstract fun licenseDao(): LicenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mikrotech301_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
