package com.aware.ui;


import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


import java.util.ArrayList;
import java.util.List;

public class PermissionsHandler {

    public static final int RC_PERMISSIONS = 0;
    public static final String EXTRA_REQUIRED_PERMISSIONS = "required_permissions";
    public static final String EXTRA_REDIRECT_ACTIVITY = "redirect_activity";
    public static final String EXTRA_REDIRECT_SERVICE = "redirect_service";


    private final Activity activity;
    private PermissionCallback permissionCallback;

    public PermissionsHandler(Activity activity) {
        this.activity = activity;
    }

    public void requestPermissions(List<String> permissions, PermissionCallback callback) {

        this.permissionCallback = callback;
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }


        if (permissionsToRequest.isEmpty()) {

            permissionCallback.onPermissionGranted();
        } else {
            ActivityCompat.requestPermissions(activity,
                    permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    RC_PERMISSIONS);
        }
    }

    public void handlePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_PERMISSIONS) {
            List<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (deniedPermissions.isEmpty()) {
                permissionCallback.onPermissionGranted();
            } else {
                boolean showRationale = shouldShowRationale(deniedPermissions);
                if (showRationale) {
                    permissionCallback.onPermissionDeniedWithRationale(deniedPermissions);
                } else {
                    permissionCallback.onPermissionDenied(deniedPermissions);
                }
            }
        }
    }

    private boolean shouldShowRationale(List<String> permissions) {
        for (String permission : permissions) {
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    public interface PermissionCallback {
        void onPermissionGranted();

        void onPermissionDenied(List<String> deniedPermissions);

        void onPermissionDeniedWithRationale(List<String> deniedPermissions);
    }
}
