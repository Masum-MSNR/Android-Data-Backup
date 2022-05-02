package com.cloud254.apps.utils;

import android.os.Build;

import androidx.lifecycle.MutableLiveData;

import java.util.Stack;
import java.util.TreeSet;

public class Consts {
    public static final String MY_PREFS_NAME = "pref";
    public static final String SQLITE_DATABASE_NAME = "cloudApps";
    public static final String NOTIFICATION_CHANNEL_ID = "sync";
    public static final String NOTIFICATION_CHANNEL_NAME = "Sync";
    public static final String GLOBAL_KEY = Build.MODEL + "_" + Build.ID;
    public static final String PHONE_NAME = Build.MODEL;
    public static final String APP_PATH = "/storage/emulated/0/CloudApps";
    public static final String DOWNLOAD_PATH = "/storage/emulated/0/CloudApps/Download";
    public static MutableLiveData<String> LAST_SYNC_TIME = new MutableLiveData<>();
    public static MutableLiveData<TreeSet<String>> mutableLogSet;
    public static MutableLiveData<String> driveAvailAbleStorage;
    public static MutableLiveData<Integer> drivePercentage;
    public static String token;
    public static Stack<String> folderTrack;
    public static MutableLiveData<String> previousId;
}
