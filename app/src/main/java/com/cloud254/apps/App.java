
package com.cloud254.apps;

import static com.cloud254.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud254.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud254.apps.utils.Consts.NOTIFICATION_CHANNEL_NAME;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;

import com.cloud254.apps.utils.Functions;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class App extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setAlarm();
    }

    private void setAlarm() {
        SharedPreferences preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String time = preferences.getString("time", "09:00 PM");
        DateFormat h12 = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        DateFormat h24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date;
        try {
            date = h12.parse(time);
            time = h24.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Functions.setAlarm(getApplicationContext(), Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3, 5)));
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Sync");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
            manager.createNotificationChannel(channel);
        }
    }

}
