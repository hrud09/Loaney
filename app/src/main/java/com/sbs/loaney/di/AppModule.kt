package com.sbs.loaney.di

import android.content.Context
import com.sbs.loaney.data.local.AppDatabase
import com.sbs.loaney.data.repository.LoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.data.repository.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideLoanRepository(db: AppDatabase): LoanRepository {
        return LoanRepository(db.loanDao(), db.bankAccountDao())
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context.dataStore)
    }
}
