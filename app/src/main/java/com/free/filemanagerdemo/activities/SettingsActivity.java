package com.free.filemanagerdemo.activities;

import static com.free.filemanagerdemo.utils.Consts.MY_PREFS_NAME;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.appcompat.app.AppCompatActivity;

import com.free.filemanagerdemo.broadcastReceivers.TestReceiver;
import com.free.filemanagerdemo.databinding.ActivitySettingsBinding;

import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity {

    ActivitySettingsBinding binding;
    Calendar calendar;
    TimePickerDialog timePickerDialog;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        editor = preferences.edit();

        calendar = Calendar.getInstance();


        String tempTime = preferences.getString("time", "12:00 AM");

        binding.timeTv.setText(tempTime);

        timePickerDialog = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
            calendar.set(0, 0, 0, selectedHour, selectedMinute);
            String time = DateFormat.format("hh:mm aa", calendar).toString();
            binding.timeTv.setText(time);
            editor.putString("time", time);
            editor.apply();
            PendingIntent pi = PendingIntent.getBroadcast(this, 0,
                    new Intent(this, TestReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        }, Integer.parseInt(tempTime.substring(0, 2)), Integer.parseInt(tempTime.substring(3, 5)), false);

        binding.editIbt.setOnClickListener(v -> {
            timePickerDialog.show();
        });
    }
}