package com.aware.phone

import com.aware.ui.PermissionHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
abstract class PermissionsModule {
    @Binds
    abstract fun bindPermissionHandler(
        permissionHandlerImpl: PermissionHandlerImpl
    ): PermissionHandler
}