package com.aware.dependencies

import android.content.Context
import com.aware.data.SettingsRepository
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
    fun provideSettings(@ApplicationContext appContext: Context): SettingsRepository {
        return SettingsRepository(appContext)
    }
}