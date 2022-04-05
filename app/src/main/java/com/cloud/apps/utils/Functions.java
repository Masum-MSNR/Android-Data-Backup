package com.cloud.apps.utils;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.mutableLogSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
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

    public static void saveLog(OutputStreamWriter out, String newLog) {
        Log.v("Log", newLog);
//        Consts.LOG += newLog + "\n";
//        try {
//            out.append(newLog);
//            out.append("\n");
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void saveNewLog(Context context, String newLog) {
        TreeSet<String> logSet = new TreeSet<>(mutableLogSet.getValue());
        logSet.add(newLog);
        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet("logs", logSet);
        editor.apply();
        mutableLogSet.postValue(logSet);
    }

//    public static String getLog() {
//        File root = new File(Environment.getExternalStorageDirectory(), "Demo Log");
//        if (!root.exists()) {
//            root.mkdirs();
//        }
//        File file = new File(root, "test_log.txt");
//        StringBuilder text = new StringBuilder();
//
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(file));
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                text.append(line);
//                text.append('\n');
//            }
//            br.close();
//        } catch (IOException ignored) {
//        }
//
//        return text.toString();
//    }

//    public static void iniLogFile() {
//        java.io.File root = new java.io.File(Environment.getExternalStorageDirectory(), "Demo Log");
//        if (!root.exists()) {
//            root.mkdirs();
//        }
//        java.io.File file = new java.io.File(root, "test_log.txt");
//
//        FileOutputStream fOut = null;
//        try {
//            fOut = new FileOutputStream(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        Consts.out = new OutputStreamWriter(fOut);
//    }

    public static String convertedTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss", Locale.getDefault());
//        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(time));
    }
}
