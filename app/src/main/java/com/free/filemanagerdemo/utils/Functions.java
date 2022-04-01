package com.free.filemanagerdemo.utils;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Functions {
    public static void setPaths(String key, Set<String> paths, SharedPreferences.Editor editor) {
        editor.putStringSet(key, paths);
        editor.apply();
    }

    public static Set<String> getPaths(SharedPreferences preferences, String key) {
        return preferences.getStringSet(key, new HashSet<>());
    }
}
