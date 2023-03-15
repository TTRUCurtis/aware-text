package com.aware.dependencies

import android.content.Context
import com.aware.data.settings.SettingsDao
import com.aware.data.settings.SettingsInitializer
import com.aware.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO


    @Singleton
    @Provides
    fun provideSettings(settingsInitializer: SettingsInitializer, settingsDao: SettingsDao): SettingsRepository {
        return SettingsRepository(settingsInitializer, settingsDao)
    }
}