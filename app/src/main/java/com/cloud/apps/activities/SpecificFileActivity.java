package com.cloud.apps.activities;

import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud.apps.adapters.FileAdapter;
import com.cloud.apps.databinding.ActivitySpeceficFileBinding;
import com.cloud.apps.models.SingleFile;
import com.cloud.apps.viewModels.SpecificFileActivityViewModel;

import java.util.ArrayList;

public class SpecificFileActivity extends AppCompatActivity {

    private static final String TAG = "SpecificFileActivity";
    ActivitySpeceficFileBinding binding;
    SpecificFileActivityViewModel viewModel;

    String type;
    ArrayList<SingleFile> files;
    FileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpeceficFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(SpecificFileActivityViewModel.class);
        viewModel.init();
        type = getIntent().getStringExtra("type");
//        type = "image";
        getSupportActionBar().setTitle(type);

        files = new ArrayList<>();
        adapter = new FileAdapter(this, files, type);
        binding.filesRv.setLayoutManager(new LinearLayoutManager(this));
        binding.filesRv.setAdapter(adapter);

        switch (type) {
            case "image":
                viewModel.refresh();
                files.addAll(viewModel.loadImages(Environment.getExternalStorageDirectory().getPath()));
                adapter.notifyItemRangeChanged(0, files.size());
                break;
            case "video":
                viewModel.refresh();
                files.addAll(viewModel.loadVideos(Environment.getExternalStorageDirectory().getPath()));
                adapter.notifyItemRangeChanged(0, files.size());
                break;
            case "document":
                viewModel.refresh();
                files.addAll(viewModel.loadDocuments(Environment.getExternalStorageDirectory().getPath()));
                adapter.notifyItemRangeChanged(0, files.size());
                break;
            case "audio":
                viewModel.refresh();
                files.addAll(viewModel.loadAudios(Environment.getExternalStorageDirectory().getPath()));
                adapter.notifyItemRangeChanged(0, files.size());
                break;
            default:
                finish();
        }
    }


}