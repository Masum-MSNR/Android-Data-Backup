
package com.free.filemanagerdemo.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.free.filemanagerdemo.activities.ConnectivityActivity;
import com.free.filemanagerdemo.activities.SelectedFolderListActivity;
import com.free.filemanagerdemo.activities.SettingsActivity;
import com.free.filemanagerdemo.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {


    FragmentDashboardBinding binding;

    Context context;

    public DashboardFragment() {
    }

    public DashboardFragment(Context context) {
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(getLayoutInflater());

        binding.backupLl.setOnClickListener(v -> {
            startActivity(new Intent(context, ConnectivityActivity.class));
        });

        binding.settingsLl.setOnClickListener(v->{
            startActivity(new Intent(context, SettingsActivity.class));
        });

        return binding.getRoot();
    }


}