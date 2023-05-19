package com.aware.phone

import android.content.Context
import android.content.Intent
import com.aware.ui.PermissionHandler
import com.aware.ui.PermissionsHandler
import javax.inject.Inject

class PermissionHandlerImpl @Inject constructor() : PermissionHandler{

    override fun getPermissionHandlerIntent(context: Context): Intent {
        return Intent(context, Aware_Client::class.java)
    }
}