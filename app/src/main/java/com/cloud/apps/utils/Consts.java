package com.cloud.apps.utils;

import android.os.Build;

import androidx.lifecycle.MutableLiveData;

import java.io.OutputStreamWriter;
import java.util.TreeSet;

public class Consts {
    public static final String MY_PREFS_NAME = "pref";
    public static final String NOTIFICATION_CHANNEL_ID = "sync";
    public static final String NOTIFICATION_CHANNEL_NAME = "Sync";
    public static final String GLOBAL_KEY = Build.MODEL + "_" + Build.ID;
    public static String ROOT_KEY = Build.MODEL + "_" + Build.ID;
    public static MutableLiveData<String> LAST_SYNC_TIME = new MutableLiveData<>();
    public static MutableLiveData<TreeSet<String>> mutableLogSet;
}
