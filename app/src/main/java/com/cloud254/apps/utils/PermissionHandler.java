package com.cloud254.apps.utils;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionHandler {
    public static void requestPermission(Activity context, String[] permission, int requestCode) {
        if (!checkForPermission(context, permission)) {
            ActivityCompat.requestPermissions(
                    context,
                    permission,
                    requestCode
            );
        }
    }

    public static boolean checkForPermission(Activity context, String[] permission) {
        for (String s : permission) {
            if (ActivityCompat.checkSelfPermission(context, s) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
