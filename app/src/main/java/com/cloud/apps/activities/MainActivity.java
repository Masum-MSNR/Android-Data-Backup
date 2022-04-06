package com.cloud.apps.activities;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;
import static com.cloud.apps.utils.Consts.mutableLogSet;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.cloud.apps.R;
import com.cloud.apps.databinding.ActivityMainBinding;
import com.cloud.apps.fragments.BlankFragment;
import com.cloud.apps.fragments.DashboardFragment;
import com.cloud.apps.fragments.LogFragment;
import com.cloud.apps.utils.Consts;
import com.cloud.apps.utils.PermissionHandler;
import com.google.android.material.navigation.NavigationBarView;

import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setTitle("Dashboard");

        TreeSet<String> logSet = new TreeSet<>(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getStringSet("logs", new TreeSet<>()));

        mutableLogSet = new MutableLiveData<>(logSet);


        Consts.LAST_SYNC_TIME.setValue(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).getString("last_sync_time", ""));

        binding.bottomNavigation.setOnItemSelectedListener(listener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DashboardFragment(this)).commit();
        checkPermission();
    }

    private void checkPermission() {
        if (!PermissionHandler.checkForPermission(this, permissions)) {
            PermissionHandler.requestPermission(this, permissions, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length == 2 && grantResults[0] == -1 || grantResults[1] == -1) {
            onBackPressed();
        }
    }

    private final NavigationBarView.OnItemSelectedListener listener = item -> {

        Fragment currentFragment;

        if (item.getItemId() == R.id.dashboard) {
            currentFragment = new DashboardFragment(MainActivity.this);
        } else if (item.getItemId() == R.id.log) {
            currentFragment = new LogFragment(MainActivity.this);
        } else {
            currentFragment = new BlankFragment();
        }
        getSupportActionBar().setTitle(item.getTitle());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, currentFragment).commit();
        return true;
    };


}