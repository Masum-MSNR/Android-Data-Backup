package com.free.filemanagerdemo.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.free.filemanagerdemo.R;
import com.free.filemanagerdemo.databinding.ActivityMainBinding;
import com.free.filemanagerdemo.fragments.BlankFragment;
import com.free.filemanagerdemo.fragments.DashboardFragment;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setTitle("Dashboard");

        binding.bottomNavigation.setOnItemSelectedListener(listener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DashboardFragment(this)).commit();
    }

    private final NavigationBarView.OnItemSelectedListener listener = item -> {
        Fragment currentFragment;

        if (item.getItemId() == R.id.dashboard) {
            currentFragment = new DashboardFragment(MainActivity.this);
        } else {
            currentFragment = new BlankFragment();
        }
        getSupportActionBar().setTitle(item.getTitle());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, currentFragment).commit();
        return true;
    };

}