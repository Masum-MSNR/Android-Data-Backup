package com.cloud254.apps.broadcastReceivers;

import static android.content.Context.MODE_PRIVATE;
import static com.cloud254.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud254.apps.utils.Consts.NOTIFICATION_CHANNEL_ID;
import static com.cloud254.apps.utils.Functions.setAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cloud254.apps.R;
import com.cloud254.apps.services.UploaderServiceF;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Broadcast Receiver class triggers at the set time even the off is not open.
 */

public class TaskReceiver extends BroadcastReceiver {

    Context context;


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
//        showNotification(context);
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

        if (GoogleSignIn.getLastSignedInAccount(context) == null)
            return;

        intent = new Intent(context, UploaderServiceF.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

    }

//    private static void showNotification(Context context) {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_cloudapp)
//                .setContentTitle("BroadCast receiver called.")
//                .setOnlyAlertOnce(true)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//        NotificationManagerCompat m = NotificationManagerCompat.from(context.getApplicationContext());
//        m.notify(102, builder.build());
//    }
}
