package com.aware.phone.ui;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {


    private static ArrayList<String> REQUIRED_PERMISSIONS = null;

    public static ArrayList<String> getRequiredPermissions() {
        if (REQUIRED_PERMISSIONS == null) {

            REQUIRED_PERMISSIONS = new ArrayList<>();
            REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
            REQUIRED_PERMISSIONS.add(Manifest.permission.CAMERA);
            REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
            REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
            REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
            REQUIRED_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);
            REQUIRED_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
            REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
            REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_SETTINGS);
            REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_STATS);
            REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SMS);
            REQUIRED_PERMISSIONS.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                REQUIRED_PERMISSIONS.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        return REQUIRED_PERMISSIONS;
    }

    public static boolean checkIfHasRequiredPermissions(Context context) {
        for (String p : PermissionUtils.getRequiredPermissions()) {
            if (PermissionChecker.checkSelfPermission(context, p) != PermissionChecker.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
