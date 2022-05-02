package com.cloud254.apps.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud254.apps.adapters.FolderFileAdapter;
import com.cloud254.apps.databinding.ActivityViewFolderBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ViewFolderActivity extends AppCompatActivity implements FolderFileAdapter.OnClick {

    ActivityViewFolderBinding binding;
    String path, currentPath;
    ArrayList<File> folders;
    ArrayList<File> files;
    ArrayList<File> fileFolders;
    FolderFileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewFolderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentPath = path = getIntent().getStringExtra("path");

        getSupportActionBar().setTitle(path);

        folders = new ArrayList<>();
        files = new ArrayList<>();
        fileFolders = new ArrayList<>();
        adapter = new FolderFileAdapter(this, fileFolders, this, true);


        binding.fileFolderRv.setLayoutManager(new LinearLayoutManager(this));
        binding.fileFolderRv.setAdapter(adapter);

        loadStorage(path);
    }

    public void loadStorage(String path) {
        getSupportActionBar().setTitle(path);
        fileFolders.clear();
        folders.clear();
        files.clear();
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        folders.clear();
        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory()) {
                    folders.add(file);
                } else {
                    files.add(file);
                }
            }
        }
        if (folders == null || folders.size() == 0) {
            adapter.notifyDataSetChanged();
            binding.emptyTv.setVisibility(View.VISIBLE);
            return;
        }
        Collections.sort(folders, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
        Collections.sort(files, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
        fileFolders.addAll(folders);
        fileFolders.addAll(files);
        binding.emptyTv.setVisibility(View.INVISIBLE);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(String currentDir) {
        currentPath = currentPath + "/" + currentDir;
        loadStorage(currentPath);
    }

    @Override
    public void onBackPressed() {
        if (path.equals(currentPath)) {
            super.onBackPressed();
            return;
        }
        String revPath = new StringBuilder(currentPath).reverse().toString();
        int i = revPath.indexOf('/');
        revPath = revPath.substring(i + 1);
        revPath = new StringBuilder(revPath).reverse().toString();
        currentPath = revPath;
        loadStorage(currentPath);
    }
}