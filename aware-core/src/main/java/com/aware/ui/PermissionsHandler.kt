package com.aware.ui

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.util.ArrayList

class PermissionsHandler(private val activity: Activity) {
    private var permissionCallback: PermissionCallback? = null
    fun requestPermissions(permissions: List<String>, callback: PermissionCallback?) {
        permissionCallback = callback
        val permissionsToRequest: MutableList<String> = ArrayList()
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isEmpty()) {
            permissionCallback!!.onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                RC_PERMISSIONS
            )
        }
    }

    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == RC_PERMISSIONS) {
            val deniedPermissions: MutableList<String> = ArrayList()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }
            if (deniedPermissions.isEmpty()) {
                permissionCallback!!.onPermissionGranted()
            } else {
                val showRationale = shouldShowRationale(deniedPermissions)
                if (showRationale) {
                    permissionCallback!!.onPermissionDeniedWithRationale(deniedPermissions)
                } else {
                    permissionCallback!!.onPermissionDenied(deniedPermissions)
                }
            }
        }
    }

    private fun shouldShowRationale(permissions: List<String>): Boolean {
        for (permission in permissions) {
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                return true
            }
        }
        return false
    }

    interface PermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied(deniedPermissions: List<String>?)
        fun onPermissionDeniedWithRationale(deniedPermissions: List<String>?)
    }

    companion object {
        const val RC_PERMISSIONS = 112
        const val EXTRA_REQUIRED_PERMISSIONS = "required_permissions"
        const val EXTRA_REDIRECT_ACTIVITY = "redirect_activity"
        const val EXTRA_REDIRECT_SERVICE = "redirect_service"
    }
}