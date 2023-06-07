package com.aware.phone

import android.content.Context
import android.content.Intent
import com.aware.phone.ui.Aware_Participant
import com.aware.ui.PermissionHandler
import javax.inject.Inject

class PermissionHandlerImpl @Inject constructor() : PermissionHandler {

    override fun getPermissionHandlerIntent(context: Context): Intent {
        return Intent(context, Aware_Participant::class.java)
    }
}