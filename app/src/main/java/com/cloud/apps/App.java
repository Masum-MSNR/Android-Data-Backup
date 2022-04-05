package com.cloud.apps;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud.apps.utils.Consts.NOTIFICATION_CHANNEL_NAME;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.cloud.apps.broadcastReceivers.TaskReceiver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
//        if (!getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getBoolean("alarm_set", false)) {
            setAlarm();
//        }
    }

    private void setAlarm() {
        Calendar calendar = Calendar.getInstance();
        SharedPreferences preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String time = preferences.getString("time", "12:00 AM");
        DateFormat h12 = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        DateFormat h24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date;
        try {
            date = h12.parse(time);
            time = h24.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(time.substring(3, 5)));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        }
        Intent intent = new Intent(this, TaskReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi), pi);
//        preferences.edit().putBoolean("alarm_set", true).apply();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Sync");
//                channel.enableVibration(true);
//                channel.setVibrationPattern(new long[]{100, 1000, 200, 340});
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
            manager.createNotificationChannel(channel);
        }
    }
}
