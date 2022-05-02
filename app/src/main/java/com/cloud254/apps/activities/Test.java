package com.cloud254.apps.activities;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.cloud254.apps.databinding.ActivityTestBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Test extends AppCompatActivity {

    private static final int REQUEST_USAGE_PERMISSION = 542;
    private static final int REQUEST_PHONE_STATE = 54;
    ActivityTestBinding binding;
    long installedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.MONTH, 3);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat format = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss", Locale.getDefault());
        Log.v("Date", String.valueOf(new Date(calendar.getTimeInMillis())));
        installedTime = calendar.getTimeInMillis();

        if (checkUserStatePermission()) {
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_USAGE_PERMISSION);

        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED

            ) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE
                }, REQUEST_PHONE_STATE);
            }
            else getInternetUsage();
        }
    }

    private void getInternetUsage() {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucketWifi, bucketMobile;

        try {

            bucketWifi = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                    "",
                    installedTime,
                    System.currentTimeMillis());

            bucketMobile = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(),
                    installedTime,
                    System.currentTimeMillis());


            Log.d("MOBILE INTERNET UP", bucketMobile.getTxBytes() / (1024f * 1024f) + " MB");
            Log.d("MOBILE INTERNET DOWN", bucketMobile.getRxBytes() / (1024f * 1024f) + " MB");


            Log.d("WIFI INTERNET UP", bucketWifi.getTxBytes() / (1024f * 1024f) + " MB");
            Log.d("WIFI INTERNET DOWN", bucketWifi.getRxBytes() / (1024f * 1024f) + " MB");


        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /*
    we can pass null above Q
     */
    public String getSubscriberId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyManager.getSubscriberId();
        } else {
            return null;
        }
    }
    private boolean checkUserStatePermission() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getPackageName());
        return mode != AppOpsManager.MODE_ALLOWED;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_USAGE_PERMISSION) {
            if (checkUserStatePermission()) {
                Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                getInternetUsage();
            }

        }
    }
}