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
import com.sbs.loaney.data.local.dao.BankAccountDao

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LoanEntity::class, PaymentEntity::class, LoanItemEntity::class, BankAccountEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun bankAccountDao(): BankAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN profilePhotoUri TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loaney_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
