package com.aware.ui

import android.content.Context
import android.content.Intent

interface PermissionHandler {
    fun getPermissionHandlerIntent(context: Context): Intent
}