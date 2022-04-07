package com.cloud.apps.utils;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.mutableLogSet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.Log;

import com.cloud.apps.broadcastReceivers.TaskReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class Functions {
    public static void setPaths(String key, Set<String> paths, SharedPreferences.Editor editor) {
        editor.putStringSet(key, paths);
        editor.apply();
    }

    public static Set<String> getPaths(SharedPreferences preferences, String key) {
        return preferences.getStringSet(key, new HashSet<>());
    }

    public static void setRootId(String key, String rootId, SharedPreferences.Editor editor) {
        editor.putString(key, rootId);
        editor.apply();
    }


    public static void saveNewLog(Context context, String newLog) {
        TreeSet<String> logSet = new TreeSet<>(mutableLogSet.getValue());
        logSet.add(newLog);
        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet("logs", logSet);
        editor.apply();
        mutableLogSet.postValue(logSet);
    }

    public static String convertedTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss", Locale.getDefault());
//        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(time));
    }

    public static String setAlarm(Context context, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        }

        Intent intent = new Intent(context, TaskReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi), pi);

        String time = DateFormat.format("hh:mm aa", calendar).toString();
        Log.v(time, time);
        return time;
    }

    public static String getSize(long size) {
        long nSize = size;
        String bkm = "B";
        if (nSize >= 1024) {
            nSize = Integer.parseInt(String.valueOf(nSize / 1024));
            bkm = "KB";
        }
        if (nSize >= 1024) {
            nSize = Integer.parseInt(String.valueOf(nSize / 1024));
            bkm = "MB";
        }
        if (nSize >= 1024) {
            nSize = Integer.parseInt(String.valueOf(nSize / 1024));
            bkm = "GB";
        }
        return nSize + bkm;
    }
}
