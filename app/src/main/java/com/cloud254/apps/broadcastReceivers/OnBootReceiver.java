package com.cloud254.apps.broadcastReceivers;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud254.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud254.apps.utils.Functions.setAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
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

        setAlarm(context, Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3, 5)));
    }
}
