
package com.cloud.apps.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.cloud.apps.activities.ConnectivityActivity;
import com.cloud.apps.databinding.FragmentDashboardBinding;
import com.cloud.apps.utils.Consts;

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

        binding.a1Ll.setOnClickListener(v -> Toast.makeText(context, "Under development", Toast.LENGTH_SHORT).show());
        binding.a2Ll.setOnClickListener(v -> Toast.makeText(context, "Under development", Toast.LENGTH_SHORT).show());
        binding.a3Ll.setOnClickListener(v -> Toast.makeText(context, "Under development", Toast.LENGTH_SHORT).show());

        Consts.LAST_SYNC_TIME.observe((LifecycleOwner) context, s -> binding.lastSyncTv.setText(s.isEmpty() ? "Last Sync: Press Backup Icon" : "Last Synced: " + s));

        return binding.getRoot();
    }

}