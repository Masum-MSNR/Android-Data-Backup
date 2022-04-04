package com.free.filemanagerdemo.adapters;

import static com.free.filemanagerdemo.utils.Consts.ROOT_KEY;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.free.filemanagerdemo.R;
import com.free.filemanagerdemo.databinding.AdapterFullFolderBinding;
import com.free.filemanagerdemo.driveApi.GoogleDriveServiceHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FullFolderAdapter extends RecyclerView.Adapter<FullFolderAdapter.ViewHolder> {

    Context context;
    ArrayList<String> folders;
    GoogleDriveServiceHelper helper;

    public FullFolderAdapter(Context context, ArrayList<String> folders, GoogleDriveServiceHelper helper) {
        this.context = context;
        this.folders = folders;
        this.helper = helper;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_full_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FullFolderAdapter.ViewHolder h, int position) {

        Animation animation = AnimationUtils.loadAnimation(context, R.anim.rotate_animation);

        int p = h.getAdapterPosition();
        String revPath = new StringBuilder(folders.get(p)).reverse().toString();
        int i = revPath.indexOf('/');
        revPath = revPath.substring(0, i);
        revPath = new StringBuilder(revPath).reverse().toString();
        Log.v("path", revPath);
        h.binding.folderNameTv.setText(revPath);
        h.binding.syncIv.startAnimation(animation);

        Log.v(folders.get(p), fileCounter(folders.get(p)) + "");
        helper.uploadFolder(folders.get(p), folders.get(p), ROOT_KEY, fileCounter(folders.get(p)), animation);

        h.itemView.setOnClickListener(v -> {
            h.binding.folderItemsRv.setVisibility(h.binding.folderItemsRv.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        h.binding.folderItemsRv.setLayoutManager(new LinearLayoutManager(context));

        File root = new File(folders.get(p));
        File[] filesAndFolders = root.listFiles();
        ArrayList<File> fileFolders = new ArrayList<>();
        FolderFileAdapter adapter = new FolderFileAdapter(context, fileFolders);
        h.binding.folderItemsRv.setAdapter(adapter);
        if (filesAndFolders != null) {
            fileFolders.addAll(Arrays.asList(filesAndFolders));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        AdapterFullFolderBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = AdapterFullFolderBinding.bind(itemView);
        }
    }

    private int fileCounter(String path) {
        int count = 0;
        File root = new File(path);
        File[] filesAndFolders = root.listFiles();
        for (File file : filesAndFolders) {
            if (file.isDirectory()) {
                count += fileCounter(file.getPath());
            } else
                count++;
        }
        return count;
    }
}