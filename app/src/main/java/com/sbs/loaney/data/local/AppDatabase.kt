package com.sbs.loaney.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sbs.loaney.data.local.dao.LoanDao
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.entity.EmiEntity
import com.sbs.loaney.data.local.entity.DepositEntity
import com.sbs.loaney.data.local.dao.BankAccountDao
import com.sbs.loaney.data.local.dao.EmiDao
import com.sbs.loaney.data.local.dao.DepositDao

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LoanEntity::class, PaymentEntity::class, LoanItemEntity::class, BankAccountEntity::class, EmiEntity::class, DepositEntity::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun emiDao(): EmiDao
    abstract fun depositDao(): DepositDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN profilePhotoUri TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN isCard INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN isMfs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN mfsProvider TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN qrCodeUri TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN removedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE loans ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                db.execSQL("ALTER TABLE loans ADD COLUMN email TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE loans ADD COLUMN address TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE loans ADD COLUMN relationshipType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE loans ADD COLUMN witness TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN ownerUid TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN ownerName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN shareId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN sharePermission TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN isSharedIncoming INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `emis` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `itemName` TEXT NOT NULL, 
                        `totalAmount` REAL NOT NULL, 
                        `downPayment` REAL NOT NULL, 
                        `tenureMonths` INTEGER NOT NULL, 
                        `interestRate` REAL NOT NULL, 
                        `monthlyInstallment` REAL NOT NULL, 
                        `startDate` INTEGER NOT NULL, 
                        `dueDateDay` INTEGER NOT NULL, 
                        `associatedBankAccountId` INTEGER, 
                        `paymentsPaid` INTEGER NOT NULL DEFAULT 0, 
                        `isCompleted` INTEGER NOT NULL DEFAULT 0, 
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deposits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `monthlyDeposit` REAL NOT NULL,
                        `principalAmount` REAL NOT NULL,
                        `annualRate` REAL NOT NULL,
                        `tenureMonths` INTEGER NOT NULL,
                        `maturityAmount` REAL NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `maturityDate` INTEGER NOT NULL,
                        `associatedBankAccountId` INTEGER,
                        `isMatured` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deposits ADD COLUMN compoundingsPerYear INTEGER NOT NULL DEFAULT 4")
                db.execSQL("ALTER TABLE deposits ADD COLUMN taxRatePercent REAL NOT NULL DEFAULT 10.0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loaney_database"
                )
                .addMigrations(
                    MIGRATION_3_4, 
                    MIGRATION_4_5, 
                    MIGRATION_5_6, 
                    MIGRATION_6_7, 
                    MIGRATION_7_8, 
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                // No fallbackToDestructiveMigration: this database is the source of truth
                // for the user's loans and savings, and there is no cloud backup to restore
                // from. A missing migration must fail loudly, not silently wipe their data.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
