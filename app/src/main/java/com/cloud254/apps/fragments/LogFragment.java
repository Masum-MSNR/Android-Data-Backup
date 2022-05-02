package com.cloud254.apps.fragments;

import static com.cloud254.apps.utils.Consts.mutableLogSet;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud254.apps.adapters.LogAdapter;
import com.cloud254.apps.databinding.FragmentLogBinding;
import com.cloud254.apps.models.LoG;

import java.util.ArrayList;
import java.util.Set;

public class LogFragment extends Fragment {

    FragmentLogBinding binding;

    Context context;

    LogAdapter adapter;
    ArrayList<LoG> loGS;

    public LogFragment(Context context) {
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(getLayoutInflater());

        loGS = new ArrayList<>();


        adapter = new LogAdapter(context, loGS);
        binding.logRv.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true));
        binding.logRv.setAdapter(adapter);

        mutableLogSet.observe((LifecycleOwner) context, logSet -> {
            Set<String> dLogSet = logSet.descendingSet();
            Log.v("Size", String.valueOf(dLogSet.size()));
            binding.detailsTv.setVisibility(dLogSet.size() == 0 ? View.VISIBLE : View.INVISIBLE);
            int i = 0;
            loGS.clear();
            for (String s : dLogSet) {
                LoG loG = new LoG(s);
                loGS.add(loG);
                i++;
                if (i == 20)
                    break;
            }
            adapter.notifyDataSetChanged();
        });

        return binding.getRoot();
    }

}