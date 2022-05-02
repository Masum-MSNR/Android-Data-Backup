package com.cloud254.apps.utils;

import static com.cloud254.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud254.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud254.apps.utils.Consts.driveAvailAbleStorage;
import static com.cloud254.apps.utils.Consts.drivePercentage;
import static com.cloud254.apps.utils.Consts.mutableLogSet;
import static com.cloud254.apps.utils.Consts.token;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.cloud254.apps.R;
import com.cloud254.apps.broadcastReceivers.TaskReceiver;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
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

    public static long getInterval(int hour, int minute) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        }
        return calendar.getTimeInMillis() - System.currentTimeMillis();
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
        PendingIntent pi = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi), pi);

        String time = DateFormat.format("hh:mm aa", calendar).toString();
        String timeL = DateFormat.format("dd", calendar).toString();
        showNotification(context, time + " " + timeL);
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

    public static void getAbout(Context context, String token) {
        String url = "https://www.googleapis.com/drive/v3/about?fields=storageQuota&access_token=" + token;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject json = new JSONObject(response);
                json = new JSONObject(json.getString("storageQuota"));
                long use = Long.parseLong(json.getString("usageInDrive"));
                long totalSize = 1024 * 1024 * 1024;
                totalSize *= 15;
                drivePercentage.setValue((int) (((float) use / (float) totalSize) * 100));
                driveAvailAbleStorage.setValue(getSize(use) + "/15GB");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            Log.e("Error", error.toString());
        });
        VolleySingleton.getInstance(context).addToRequestQueue(context, stringRequest);
    }

    public static void updateToken(Context context) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(context).getAccount());

        new Thread(() -> {
            try {
                token = credential.getToken();
            } catch (IOException | GoogleAuthException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void showNotification(Context context, String time) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudapp)
                .setContentTitle("Next scheduled time")
                .setContentText(time)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
        m.notify(103, builder.build());
    }
}
